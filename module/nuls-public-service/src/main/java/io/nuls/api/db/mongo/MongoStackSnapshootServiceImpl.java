package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.DepositService;
import io.nuls.api.db.StackSnapshootService;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.model.po.*;
import io.nuls.api.utils.DateUtil;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 16:31
 * @Description: 功能描述
 */
@Component
public class MongoStackSnapshootServiceImpl implements StackSnapshootService {

    private Long lastSnapshootTime;

    public static final String TABLE = DBTableConstant.STACK_SNAPSHOOT_TABLE;

    @Autowired
    MongoDBService mongoDBService;

    @Autowired
    DepositService depositService;

    @Autowired
    SymbolQuotationPriceService symbolPriceService;

    /**
     * 构造快照对象
     * @param chainId
     * @param tx
     * @return
     */
    @Override
    public Optional<StackSnapshootInfo> buildSnapshoot(int chainId, TransactionInfo tx){
        List<CoinToInfo> coinToInfoList = tx.getCoinTos();
        if(lastSnapshootTime == null){
            getLastSnapshoot(chainId).ifPresentOrElse(d->lastSnapshootTime = d.getDay(),()->lastSnapshootTime = 0L);
        }
        //判断是否是每日奖励发放块
        boolean isRewardBlock = false;
        if(lastSnapshootTime == 0){
            Long day = DateUtil.getDayStartTimestampBySecond(tx.getCreateTime());
            //如果当前区块是当天第一分钟内出的块，设置为当前第一个块，也就是奖励发放块
            if(day + 60 * 1000L > DateUtil.timeStampSecondToMillisecond(tx.getCreateTime())){
                isRewardBlock = true;
            }
        }else{
            //如果当前块的出块日期大于最后一次快照时间，说明当前块是今天第一个块，也就是奖励发放块
            if(lastSnapshootTime < DateUtil.getDayStartTimestampBySecond(tx.getCreateTime())){
                isRewardBlock = true;
            }
        }
        if(isRewardBlock){
            StackSnapshootInfo stackSnapshootInfo = new StackSnapshootInfo();
            stackSnapshootInfo.setDay(DateUtil.getDayStartTimestampBySecond(tx.getCreateTime()));
            stackSnapshootInfo.setBlockHeight(tx.getHeight());
            stackSnapshootInfo.setCreateTime(tx.getCreateTime() * 1000L);
            //获取节点委托的总数
            BigInteger agentDepositTotal = depositService.getAgentDepositTotal(chainId,tx.getHeight());
            //获取所有stacking换算成NVT的总数
            BigInteger stackingTotal = depositService.getStackingTotalByNVT(chainId,tx.getHeight());
            BigInteger total = agentDepositTotal.add(stackingTotal);
            stackSnapshootInfo.setStackTotal(total);
            //计算当天发放的所有委托奖励
            BigInteger rewardTotal = coinToInfoList.stream().map(d->d.getAmount()).reduce(BigInteger.ZERO,(d1,d2)->d1.add(d2));
            if(rewardTotal.equals(BigInteger.ZERO) || total.equals(BigInteger.ZERO)){
                stackSnapshootInfo.setBaseInterest(BigInteger.ZERO);
            }else{
                //计算年化基础收益率：
                BigDecimal rate = new BigDecimal(rewardTotal).divide(new BigDecimal(total), MathContext.DECIMAL64).multiply(BigDecimal.valueOf(365),MathContext.DECIMAL64);
                stackSnapshootInfo.setBaseInterest(rate.movePointRight(ApiConstant.RATE_DECIMAL).toBigInteger());
            }
            stackSnapshootInfo.setRewardTotal(rewardTotal);
            return Optional.of(stackSnapshootInfo);
        }else{
            return Optional.empty();
        }
    }


    @Override
    public void save(int chainId,StackSnapshootInfo stackSnapshootInfo) {
        Objects.requireNonNull(stackSnapshootInfo.getDay(),"day can't be null");
        Objects.requireNonNull(stackSnapshootInfo.getBlockHeight(),"blockHeight can't be null");
        stackSnapshootInfo.setCreateTime(System.currentTimeMillis());
        mongoDBService.insertOrUpdate(TABLE + chainId,ID, stackSnapshootInfo);
        //更新缓存的最后一个快照时间
        lastSnapshootTime = stackSnapshootInfo.getDay();
    }

    @Override
    public Optional<StackSnapshootInfo> getLastSnapshoot(int chainId) {
        List<Document> list = mongoDBService.limitQuery(TABLE + chainId, Sorts.descending("_id"),0,1);
        if(list == null || list.isEmpty()){
            return Optional.empty();
        }
        Document doc = list.get(0);
        return Optional.of(DocumentTransferTool.toInfo(doc,ID,StackSnapshootInfo.class));
    }

    @Override
    public List<StackSnapshootInfo> queryList(int chainId,long start, long end) {
        List<Document> list = mongoDBService.query(TABLE + chainId,Filters.and(Filters.gte("_id",start),Filters.lte("_id",end)),Sorts.ascending("_id"));
        return list.stream().map(d-> DocumentTransferTool.toInfo(d,ID,StackSnapshootInfo.class)).collect(Collectors.toList());
    }

    /**
     * 获取合计的收益发放数量
     * @return
     */
    @Override
    public BigInteger queryRewardTotal(int chainId){
        return mongoDBService.aggReturnDoc(DBTableConstant.STACK_SNAPSHOOT_TABLE + chainId,
                new Document("$group", new Document("_id", 0).append("total", new Document("$sum", "$rewardTotal")))
        ).stream().map(d->new BigInteger(d.get("total").toString())).findFirst().orElse(BigInteger.ZERO);
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
