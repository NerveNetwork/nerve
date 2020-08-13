package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.AgentService;
import io.nuls.api.db.DepositService;
import io.nuls.api.db.StackSnapshootService;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.*;
import io.nuls.api.service.StackingService;
import io.nuls.api.utils.AgentComparator;
import io.nuls.api.utils.DateUtil;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.consensus.ConsensusProvider;
import io.nuls.base.api.provider.consensus.facade.GetTotalRewardForBlockHeightReq;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.DateUtils;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.nuls.api.utils.LoggerUtil.commonLog;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 16:31
 * @Description: 共识委托快照
 */
@Component
public class MongoStackSnapshootServiceImpl implements StackSnapshootService {

    private Long lastSnapshootTime;

    public static final String TABLE = DBTableConstant.STACK_SNAPSHOOT_TABLE;

    /**
     * 按每小时1800个块计算每天的总通胀量
     */
    public static final int ONE_DAY_BLOCK_COUNT = 1800 * 24;

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    @Autowired
    MongoDBService mongoDBService;

    @Autowired
    DepositService depositService;

    @Autowired
    SymbolQuotationPriceService symbolPriceService;

    @Autowired
    AgentService agentService;

    private BigDecimal getOneBlockRewardForHeight(long blockHeight) {
        return consensusProvider.getTotalRewardForBlockHeight(new GetTotalRewardForBlockHeightReq(blockHeight)).getData();
    }

    /**
     * 构造快照对象
     * 通过区块出块时间为基础，UTC+0时间每天第一个块的时候，构建前一天的stack数据快照
     * 快照的key为当天的0点0分0秒的时间戳。
     * 快照内容包括共识委托权重的基础年化收益率、当天参与共识委托的资产数量（换算成nvt），当天共发放的收益数量
     * @param chainId
     * @param blockHeaderInfo
     * @return
     */
    @Override
    public Optional<StackSnapshootInfo> buildSnapshoot(int chainId, BlockHeaderInfo blockHeaderInfo) {
        if (blockHeaderInfo.getHeight() == 0) {
            return Optional.empty();
        }
        AtomicInteger totalBlockCount = new AtomicInteger(0);
        if (lastSnapshootTime == null) {
            getLastSnapshoot(chainId).ifPresentOrElse(d -> lastSnapshootTime = d.getDay(), () ->
                    //如果没有查询到上一次快照数据，设置上次的快照时间为前一天的0点。
                lastSnapshootTime = DateUtil.getDayStartTimestampBySecond(blockHeaderInfo.getCreateTime())
            );
        }
        //判断是否是每日第一个块
        boolean isDayFirstBlock = false;
        //如果当前块的出块日期大于最后一次快照时间，说明当前块是今天第一个块，也就是奖励发放块
        if (lastSnapshootTime < DateUtil.getDayStartTimestampBySecond(blockHeaderInfo.getCreateTime())) {
            //如果当前的区块高度不足一天的高度，则计算的总高度为实际出块数量。
            Optional<StackSnapshootInfo> lastSnapshoot = getLastSnapshoot(ApiContext.defaultChainId);
            if(lastSnapshoot.isPresent()){
                totalBlockCount.set((int) ((blockHeaderInfo.getHeight() - lastSnapshoot.get().getBlockHeight())));
            }else{
                totalBlockCount.set((int) blockHeaderInfo.getHeight());
            }
            isDayFirstBlock = true;
        }
        if (isDayFirstBlock) {
            commonLog.info("开始进行委托快照：{}", DateUtils.timeStamp2Str(lastSnapshootTime));
            StackSnapshootInfo stackSnapshootInfo = new StackSnapshootInfo();
            stackSnapshootInfo.setDay(DateUtil.getDayStartTimestampBySecond(blockHeaderInfo.getCreateTime()));
            stackSnapshootInfo.setBlockHeight(blockHeaderInfo.getHeight());
            stackSnapshootInfo.setCreateTime(blockHeaderInfo.getCreateTime() * 1000L);
            //获取节点委托的总数
            List<AgentInfo> agentInfoList = new ArrayList<>(CacheManager.getCache(chainId).getAgentMap().values());
            Collections.sort(agentInfoList, AgentComparator.getInstance());
            int agentCountWithoutSeed = ApiContext.maxAgentCount - ApiContext.seedCount;
            BigDecimal agentDepositTotalWeight = agentInfoList.stream().limit(agentCountWithoutSeed).map(agent -> {
                //节点押金只能用NVT作为抵押资产 NVT的基础权重
                double weight = ApiContext.localAssertBase;
                if (agent.isBankNode()) {
                    //虚拟银行节点权重
                    weight = weight * ApiContext.superAgentDepositBase;
                } else if(BigIntegerUtils.isEqualOrGreaterThan(agent.getDeposit(),ApiContext.minDeposit)) {
                    //普通节点权重
                    weight = weight * ApiContext.agentDepositBase;
                } else {
                    //未参与共识的节点权重
                    weight = weight * ApiContext.reservegentDepositBase;
                }
                weight = Math.sqrt(weight);
                return BigDecimal.valueOf(weight).multiply(new BigDecimal(agent.getDeposit()));
            }).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            agentDepositTotalWeight = agentInfoList.stream().skip(agentCountWithoutSeed).map(agent -> {
                //节点押金只能用NVT作为抵押资产 NVT的基础权重
                double weight = ApiContext.localAssertBase;
                //未参与共识的节点权重
                weight = weight * ApiContext.reservegentDepositBase;
                weight = Math.sqrt(weight);
                return BigDecimal.valueOf(weight).multiply(new BigDecimal(agent.getDeposit()));
            }).reduce(agentDepositTotalWeight,BigDecimal::add);

            BigInteger bankTotalDeposit = agentInfoList.stream().filter(d -> d.isBankNode()).map(d -> d.getDeposit()).reduce(BigInteger::add).orElse(BigInteger.ZERO);
            BigInteger nomalTotalDeposit = agentInfoList.stream().filter(d -> !d.isBankNode()).map(d -> d.getDeposit()).reduce(BigInteger::add).orElse(BigInteger.ZERO);
            commonLog.info("查询到当前节点总数:{}个，虚拟银行总抵押数量:{},普通节点总抵押数:{},占有权重数:{}", agentInfoList.size(), bankTotalDeposit, nomalTotalDeposit, agentDepositTotalWeight.toBigInteger());
            BigDecimal[] stackingData = this.getStackingTotalWeight(chainId, blockHeaderInfo.getHeight());
            BigInteger stackingTotalForNvt = stackingData[0].toBigInteger();
            BigDecimal stackingTotalWeight = stackingData[1];
            BigDecimal totalWeight = agentDepositTotalWeight.add(stackingTotalWeight);
            commonLog.info("本次总权重数为:{}", totalWeight);
            if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
                return Optional.of(stackSnapshootInfo);
            }
            stackSnapshootInfo.setStackTotal(stackingTotalForNvt.add(bankTotalDeposit).add(nomalTotalDeposit));
            //计算当天发放的所有委托奖励
            BigDecimal oneBlockReard = getOneBlockRewardForHeight(blockHeaderInfo.getHeight());
            BigDecimal rewardTotal = oneBlockReard.multiply(BigDecimal.valueOf(totalBlockCount.get()));
            commonLog.info("当日总出块数量:{},应发放收益数量:{}", totalBlockCount.get(), rewardTotal.toBigInteger());
            if (rewardTotal.equals(BigInteger.ZERO) || totalWeight.equals(BigInteger.ZERO)) {
                stackSnapshootInfo.setBaseInterest(BigDecimal.ZERO);
            } else {
                //计算年化基础收益率：
                BigDecimal interst = oneBlockReard.multiply(BigDecimal.valueOf(ONE_DAY_BLOCK_COUNT), MathContext.DECIMAL64).divide(totalWeight, MathContext.DECIMAL64);
                BigDecimal insterst360 = interst.multiply(BigDecimal.valueOf(365), MathContext.DECIMAL64);
                commonLog.info("每一份权重每日可获得:{},预计年化收益率:{}", interst, insterst360);
                stackSnapshootInfo.setBaseInterest(insterst360);
            }
            stackSnapshootInfo.setRewardTotal(rewardTotal.toBigInteger());
            commonLog.info("快照数据构建完成:{}", stackSnapshootInfo);
            return Optional.of(stackSnapshootInfo);
        } else {
            return Optional.empty();
        }
    }



    private BigDecimal[] getStackingTotalWeight(int chainId, long height) {
        List<DepositInfo> depositList = depositService.getDepositList(chainId, height, DepositInfoType.STACKING);
        //获取nvt兑USDT的最新价格
        SymbolPrice nvtUsdtPrice = symbolPriceService.getFreshUsdtPrice(ApiContext.defaultChainId, ApiContext.defaultAssetId);
        StackingService stackingService = SpringLiteContext.getBean(StackingService.class);
        Map<String, BigDecimal> res = new HashMap<>();
        depositList.stream().forEach(d -> {
            String key = d.getAssetChainId() + "-" + d.getAssetId() + "-" + d.getFixedType();
            //抵押的是NVT直接返回数量
            if (d.getAssetChainId() == ApiContext.defaultChainId && d.getAssetId() == ApiContext.defaultAssetId) {
                res.compute(key, (mk, oldVal) -> {
                    if (oldVal != null) {
                        return oldVal.add(new BigDecimal(d.getAmount()));
                    } else {
                        return new BigDecimal(d.getAmount());
                    }
                });
            } else {
                //需要将异构资产换算成NVT
                //获取当前抵押的资产与USDT的汇率
                SymbolPrice symbolPrice = symbolPriceService.getFreshUsdtPrice(d.getAssetChainId(), d.getAssetId());
                //将当前抵押资产转换成NVT
                BigDecimal amount = nvtUsdtPrice.transfer(symbolPrice, new BigDecimal(d.getAmount()).movePointLeft(d.getDecimal()));
                res.compute(key, (mk, oldVal) -> {
                    if (oldVal != null) {
                        return oldVal.add(amount.movePointRight(ApiContext.defaultDecimals));
                    } else {
                        return amount.movePointRight(ApiContext.defaultDecimals);
                    }
                });
            }
        });
        commonLog.info("stacking 数据:");
        res.entrySet().forEach(entry -> {
            commonLog.info("{}:{}", entry.getKey(), entry.getValue());
        });
        BigDecimal stackingTotalForNvt = res.values().stream().reduce(BigDecimal.ZERO, (o1, o2) -> o1.add(o2));
        BigDecimal stackingWeight = res.entrySet().stream().map(entry -> {
            int assetChainId = Integer.parseInt(entry.getKey().split("-")[0]);
            int assetId = Integer.parseInt(entry.getKey().split("-")[1]);
            DepositFixedType depositFixedType = DepositFixedType.valueOf(entry.getKey().split("-")[2]);
            BigDecimal weight = stackingService.getAssetStackingTotalAddition(assetChainId, assetId, depositFixedType).orElseThrow(() -> new RuntimeException("symbol not suport stack"));
            return entry.getValue().multiply(new BigDecimal(Math.sqrt(weight.doubleValue())));
        }).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        commonLog.info("stacking weight :{}", stackingWeight);
        return new BigDecimal[]{stackingTotalForNvt, stackingWeight};
    }


    @Override
    public void save(int chainId, StackSnapshootInfo stackSnapshootInfo) {
        Objects.requireNonNull(stackSnapshootInfo.getDay(), "day can't be null");
        Objects.requireNonNull(stackSnapshootInfo.getBlockHeight(), "blockHeight can't be null");
        stackSnapshootInfo.setCreateTime(System.currentTimeMillis());
        mongoDBService.insertOrUpdate(TABLE + chainId, ID, stackSnapshootInfo);
        //更新缓存的最后一个快照时间
        lastSnapshootTime = stackSnapshootInfo.getDay();
    }

    @Override
    public Optional<StackSnapshootInfo> getLastSnapshoot(int chainId) {
        List<Document> list = mongoDBService.limitQuery(TABLE + chainId, Sorts.descending("_id"), 0, 1);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        Document doc = list.get(0);
        return Optional.of(DocumentTransferTool.toInfo(doc, ID, StackSnapshootInfo.class));
    }

    @Override
    public List<StackSnapshootInfo> queryList(int chainId, long start, long end) {
        List<Document> list = mongoDBService.query(TABLE + chainId, Filters.and(Filters.gte("_id", start), Filters.lte("_id", end + 1000)), Sorts.ascending("_id"));
        return list.stream().map(d -> DocumentTransferTool.toInfo(d, ID, StackSnapshootInfo.class)).collect(Collectors.toList());
    }

    /**
     * 获取合计的收益发放数量
     *
     * @return
     */
    @Override
    public BigInteger queryRewardTotal(int chainId) {
        return mongoDBService.query(TABLE + chainId)
                .stream().map(d->new BigInteger(d.getString("rewardTotal"))).reduce(BigInteger::add).orElse(BigInteger.ZERO);
    }

    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    public void setDepositService(DepositService depositService) {
        this.depositService = depositService;
    }

    public void setSymbolPriceService(SymbolQuotationPriceService symbolPriceService) {
        this.symbolPriceService = symbolPriceService;
    }

    public void setLastSnapshootTime(Long lastSnapshootTime) {
        this.lastSnapshootTime = lastSnapshootTime;
    }


}
