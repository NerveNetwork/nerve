package nerve.network.pocbft.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinTo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.DoubleUtils;
import nerve.network.ecomomic.nvt.constant.NulsEconomicConstant;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.InflationInfo;
import nerve.network.pocbft.model.bo.config.ConsensusChainConfig;
import nerve.network.pocbft.model.po.AwardSettlePo;
import nerve.network.pocbft.model.po.AwardSettleRecordPo;
import nerve.network.pocbft.storage.AwardSettleRecordService;
import nerve.network.pocbft.utils.manager.AgentManager;
import nerve.network.pocbft.utils.manager.DepositManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ConsensusAwardUtil {
    @Autowired
    private static AgentManager agentManager;

    @Autowired
    private static DepositManager depositManager;

    @Autowired
    private static ConsensusChainConfig config;

    @Autowired
    private static AwardSettleRecordService awardSettleRecordService;

    /**
     * 共识奖励基础信息
     * */
    private static InflationInfo currentInflationInfo = null;

    /**
     * 本地下一次需结算的共识奖励明细
     * */
    public static AwardSettlePo lastestSettleResult;

    /**
     * 从喂价系统获取对应的数量的NVT
     * @param amount            数量
     * @param assetChainId      资产链ID
     * @param assetId           资产ID
     * @param time              交易时间（毫秒）
     * @return                  对应数量的NVT
     * */
    public static BigInteger getRealAmount(BigInteger amount, int assetChainId, int assetId, long time){
        String dateStr = DateUtils.timeStamp2DateStr(time, ConsensusConstant.DATE_FORMAT);
        return getRealAmount(amount, assetChainId, assetId, dateStr);
    }

    /**
     * 从喂价系统获取对应的数量的NVT
     * @param amount            数量
     * @param assetChainId      资产链ID
     * @param assetId           资产ID
     * @param date              交易日期
     * @return                  对应数量的NVT
     * */
    public static BigInteger getRealAmount(BigInteger amount, int assetChainId, int assetId, String date){
        return amount;
    }

    /**
     * 从喂价系统获取对应的数量的NVT
     * @param amount            数量
     * @param assetChainId      资产链ID
     * @param assetId           资产ID
     * @param time              时间（毫秒）
     * @return                  对应数量的NVT
     * */
    public static BigDecimal getRealAmount(BigDecimal amount, int assetChainId, int assetId, long time){
        String date = DateUtils.timeStamp2DateStr(time, ConsensusConstant.DATE_FORMAT);
        return getRealAmount(amount, assetChainId, assetId, date);
    }

    /**
     * 从喂价系统获取对应的数量的NVT
     * @param amount            数量
     * @param assetChainId      资产链ID
     * @param assetId           资产ID
     * @param date              时间
     * @return                  对应数量的NVT
     * */
    public static BigDecimal getRealAmount(BigDecimal amount, int assetChainId, int assetId, String date){
        return amount;
    }

    /**
     * 判断是否需要结算共识奖励
     * @param chain  chain info
     * @param time   block time
     * */
    public static boolean settleConsensusAward(Chain chain, long time){
        if(chain.getNewestHeader() == null || chain.getNewestHeader().getHeight() == 0 || time < chain.getNewestHeader().getTime()){
            return false;
        }
        return !DateUtils.isSameDay(DateUtils.secondsToMillis(time), DateUtils.secondsToMillis(chain.getNewestHeader().getTime()));
    }

    /**
     * 区块验证失败时，如果结算明细是本地定时任务生成的数据则不删除明细否则需删除明细
     * */
    public static void clearSettleDetails(){
        if(lastestSettleResult != null && !lastestSettleResult.isSettled()){
            lastestSettleResult.getSettleDetails().clear();
        }
    }

    /**
     * 保存区块
     * @param chain        chain info
     * @param blockHeader  block header
     *
     * */
    public static void saveAndSwitchSettleRecord(Chain chain, BlockHeader blockHeader){
        //判断当前区块是否为结算奖励区块(当前区块是否为当天第一个区块)，如果为结算奖励区块则保存并切换清算结果
        if(!settleConsensusAward(chain, blockHeader.getTime())){
            return;
        }
        try {
            AwardSettleRecordPo po = awardSettleRecordService.get(chain.getChainId());
            if(po == null){
                po = new AwardSettleRecordPo();
            }
            if(po.getRecordList() == null){
                po.setRecordList(new ArrayList<>());
            }
            //初始化第一天的共识奖励结算信息
            long newStartHeight;
            long newEndHeight = blockHeader.getHeight() - 1;
            if(lastestSettleResult == null){
                chain.getLogger().debug("Initialize the first consensus award settlement date information" );
                newStartHeight = 1;
            }else{
                po.getRecordList().add(lastestSettleResult);
                newStartHeight = lastestSettleResult.getEndHeight() + 1;
            }
            //下一次结算的日期是当前区块前一天
            Date date = new Date(DateUtils.secondsToMillis(blockHeader.getTime()));
            date = DateUtils.dateAdd(date, -1);
            SimpleDateFormat sdf = new SimpleDateFormat(ConsensusConstant.DATE_FORMAT);
            String dateStr = sdf.format(date);
            AwardSettlePo nextSettlePo = new AwardSettlePo(newStartHeight, newEndHeight, dateStr);
            po.setLastestSettleResult(nextSettlePo);
            awardSettleRecordService.save(po, chain.getChainId());
            lastestSettleResult = nextSettlePo;
            chain.getLogger().debug("Consensus award settlement date switch completed,nextSettleDate:{},startHeight:{},endHeight:{}",dateStr,newStartHeight,newEndHeight);
        }catch (Exception e){
            chain.getLogger().error(e);
        }
    }

    /**
     * 保存区块
     * @param chain        chain info
     * @param blockHeader  block header
     * @param preHeader    previous block header
     * */
    public static void rollbackAndSwitchSettleRecord(Chain chain, BlockHeader preHeader, BlockHeader blockHeader){
        //如果回滚掉的区块是结算共识奖励区块，则需要将共识奖励记录回滚
        if(preHeader == null || preHeader.getHeight() == 0 || blockHeader.getTime() < preHeader.getTime()){
            return;
        }
        if(DateUtils.isSameDay(DateUtils.secondsToMillis(preHeader.getTime()), DateUtils.secondsToMillis(blockHeader.getTime()))){
            return;
        }
        try {
            AwardSettleRecordPo po = awardSettleRecordService.get(chain.getChainId());
            lastestSettleResult = po.getRecordList().remove(0);
            awardSettleRecordService.save(po, chain.getChainId());
        }catch (Exception e){
            chain.getLogger().error(e);
        }
    }

    /**
     * 区块打包共识奖励
     * @param chain        链信息
     * @param coinToList   奖励列表
     * @param time         区块时间(毫秒)
     * */
    public static void packConsensusAward(Chain chain, List<CoinTo> coinToList, long time){
        //获取结算日期，为time对应日期两天前(每天十一点半开始计算前一天的共识奖励，明天的第一个块结算今天计算出的共识奖励)
        Date date = new Date(DateUtils.secondsToMillis(time));
        date = DateUtils.dateAdd(date, -2);
        SimpleDateFormat sdf = new SimpleDateFormat(ConsensusConstant.DATE_FORMAT);
        String dateStr = sdf.format(date);

        if(lastestSettleResult == null){
            chain.getLogger().warn("没有共识奖励，区块结算日期：{}",dateStr);
            return;
        }

        //结算日期是当前结算日期之前则直接返回错误（有可能出现前几天没有出块，则需要结算最后未结算的共识奖励）
        if(dateStr.compareTo(lastestSettleResult.getDate()) < 0){
            chain.getLogger().warn("共识奖励结算信息错误，待结算的日期：{}，区块结算日期：{}",lastestSettleResult.getDate(),dateStr);
            return;
        }

        //将预算出的共识奖励打包
        mergeAwardAndFee(coinToList, lastestSettleResult.getSettleDetails());
    }

    /**
     * 每天定时计算前一天的共识奖励
     * @param chain  chain info
     * */
    public static void budgetConsensusAward(Chain chain){
        try {
            //如果为初始化，则从数据库查询最后一次结算明细
            if(lastestSettleResult == null){
                AwardSettleRecordPo recordPo = awardSettleRecordService.get(chain.getChainId());
                if(recordPo == null || recordPo.getLastestSettleResult() == null){
                    chain.getLogger().warn("There is currently no consensus award to be settled");
                    return;
                }else{
                    lastestSettleResult = recordPo.getLastestSettleResult();
                }
            }
            Date date = new Date();
            date = DateUtils.dateAdd(date, -1);
            SimpleDateFormat sdf = new SimpleDateFormat(ConsensusConstant.DATE_FORMAT);
            String dateStr = sdf.format(date);
            if(dateStr.compareTo(lastestSettleResult.getDate()) < 0){
                chain.getLogger().warn("当前共识奖励结算日期小于待结算日期，budgetDate:{},lastestSettleDate:{}",dateStr,lastestSettleResult.getDate());
                return;
            }
            lastestSettleResult.setSettled(true);
            lastestSettleResult.setSettleDetails(settleConsensusAward(chain, lastestSettleResult.getStartHeight(),lastestSettleResult.getEndHeight(), 0, dateStr));
        }catch (Exception e){
            chain.getLogger().error(e);
        }
    }

    /**
     * 同步过程中接收到结算区块时根据区块时间生成对应的共识奖励明细
     * @param chain        链信息
     * @param coinToList   奖励列表
     * @param time         区块时间(毫秒)
     * */
    public static void validBlockConsensusAward(Chain chain, List<CoinTo> coinToList, long time){
        //获取结算日期，为time对应日期两天前(每天十一点半开始计算前一天的共识奖励，明天的第一个块结算今天计算出的共识奖励)
        Date date = new Date(DateUtils.secondsToMillis(time));
        date = DateUtils.dateAdd(date, -2);
        SimpleDateFormat sdf = new SimpleDateFormat(ConsensusConstant.DATE_FORMAT);
        String dateStr = sdf.format(date);

        if(lastestSettleResult == null){
            AwardSettleRecordPo recordPo = awardSettleRecordService.get(chain.getChainId());
            if(recordPo == null || recordPo.getLastestSettleResult() == null){
                chain.getLogger().warn("There is currently no consensus award to be settled");
                return;
            }else{
                lastestSettleResult = recordPo.getLastestSettleResult();
            }
        }

        //结算日期是当前结算日期之前则直接返回错误（有可能出现前几天没有出块，则需要结算最后未结算的共识奖励）
        if(dateStr.compareTo(lastestSettleResult.getDate()) < 0){
            chain.getLogger().warn("共识奖励结算信息错误，待结算的日期：{}，区块结算日期：{}",lastestSettleResult.getDate(),dateStr);
            return;
        }

        if(lastestSettleResult.getSettleDetails() != null && !lastestSettleResult.getSettleDetails().isEmpty()){
            chain.getLogger().info("共识奖励明细已提前在本地计算得到,日期：{}",lastestSettleResult.getDate());
            mergeAwardAndFee(coinToList, lastestSettleResult.getSettleDetails());
            return;
        }

        lastestSettleResult.setSettled(false);
        lastestSettleResult.setSettleDetails(settleConsensusAward(chain, lastestSettleResult.getStartHeight(),lastestSettleResult.getEndHeight(), 0, dateStr));
        mergeAwardAndFee(coinToList, lastestSettleResult.getSettleDetails());
    }



    /**
     * 结算当天共识奖励
     * @param chain        链信息
     * @param startHeight  开始区块高度
     * @param endHeight    结束高度
     * @param lockTime     锁定时间
     * @param date         结算时间
     * */
    private static List<CoinTo> settleConsensusAward(Chain chain, long startHeight, long endHeight, long lockTime, String date){
        chain.getLogger().info("结算指定日期的共识奖励，date:{},startHeight:{},endHeight:{}",date,startHeight,endHeight );
        List<CoinTo> awardDetails = new ArrayList<>();

        if(startHeight >= endHeight){
            return awardDetails;
        }

        //查询当天出块数量，计算当前产生的共识奖励
        BigDecimal totalAward = calcDayConsensusAward(chain, startHeight, endHeight);
        if(totalAward.compareTo(BigDecimal.ZERO) == 0){
            return awardDetails;
        }

        //获取权重明细，根据明细计算各账户奖励明细
        Map<String, BigDecimal> weightDetails = getWeightDetails(chain, endHeight, date);
        if(weightDetails.isEmpty()){
            return awardDetails;
        }

        BigInteger award;
        CoinTo newCoinTo;
        String address;
        BigDecimal weight;
        for (Map.Entry<String, BigDecimal> entry : weightDetails.entrySet()){
            address = entry.getKey();
            weight = entry.getValue();
            award = DoubleUtils.mul(totalAward, weight).toBigInteger();
            newCoinTo = new CoinTo(AddressTool.getAddress(address), chain.getChainId(), chain.getAssetId(), award, lockTime);
            awardDetails.add(newCoinTo);
        }

        return awardDetails;
    }

    /**
     * 合并共识奖励与手续费
     * Merger consensus award and handling fee
     * */
    private static void mergeAwardAndFee(List<CoinTo> feeList, List<CoinTo> awardList){
        if(feeList == null || feeList.isEmpty()){
            feeList = awardList;
        }

        if (awardList == null || awardList.isEmpty()){
            return;
        }

        //如果区块收取了本链主资产手续费则需要合并共识奖励与手续费
        CoinTo coinTo = null;
        for (CoinTo fee : feeList){
            if(fee.getAssetsChainId() == config.getAgentChainId() && fee.getAssetsId() == config.getAgentAssetId()){
                coinTo = fee;
                break;
            }
        }

        if(coinTo == null){
            feeList.addAll(awardList);
        }

        boolean judgeAddress = true;
        for (CoinTo award : awardList){
            if(judgeAddress && Arrays.equals(award.getAddress(), coinTo.getAddress())){
                coinTo.setAmount(coinTo.getAmount().add(award.getAmount()));
                judgeAddress =false;
            }else{
                feeList.add(award);
            }
        }
    }

    /**
     * 获取权重明细
     * @param chain        链信息
     * @param endHeight    结束高度
     * */
    private static Map<String, BigDecimal> getWeightDetails(Chain chain, long endHeight, String date){
        Map<String,BigDecimal> weightDetails = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        BigDecimal totalDeposit = BigDecimal.ZERO;
        Map<String,BigDecimal> depositMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        //获取有效的节点保证金权重
        totalDeposit = agentManager.getAgentDepositByHeight(chain, endHeight, depositMap, totalDeposit);

        //获取有效的委托金权重
        totalDeposit = depositManager.getDepositByHeight(chain, endHeight, depositMap,totalDeposit ,date);

        //计算各账户的权重
        for (Map.Entry<String,BigDecimal> entry:depositMap.entrySet()){
            weightDetails.put(entry.getKey(), DoubleUtils.div(entry.getValue(), totalDeposit).setScale(8, BigDecimal.ROUND_HALF_UP));
        }

        return weightDetails;
    }

    /**
     * 计算当天总的共识奖励
     * Calculate the total consensus Award for the day
     *
     * @return                      本轮总共识奖励
     * */
    private static BigDecimal calcDayConsensusAward(Chain chain, long startHeight, long endHeight){
        if(chain.getConfig().getInflationAmount().compareTo(BigInteger.ZERO) == 0){
            return BigDecimal.ZERO;
        }
        long blockCount = endHeight - startHeight;
        InflationInfo inflationInfo = getInflationInfo(chain, startHeight);
        if(endHeight <= inflationInfo.getEndHeight()){
            return DoubleUtils.mul(new BigDecimal(blockCount), inflationInfo.getAwardUnit());
        }
        BigDecimal totalAll = BigDecimal.ZERO;
        long currentCount;
        while(endHeight > inflationInfo.getEndHeight()){
            currentCount = inflationInfo.getEndHeight() - startHeight + 1;
            totalAll = totalAll.add(DoubleUtils.mul(new BigDecimal(currentCount), inflationInfo.getAwardUnit()));
            chain.getLogger().info("本阶段共识奖励为{}的数量为{}", inflationInfo.getAwardUnit(),currentCount);
            startHeight += currentCount;
            inflationInfo = getInflationInfo(chain, startHeight);
        }
        currentCount = endHeight - startHeight + 1;
        chain.getLogger().info("本阶段奖励为{}的数量为{}", inflationInfo.getAwardUnit(),currentCount);
        totalAll = totalAll.add(DoubleUtils.mul(new BigDecimal(currentCount), inflationInfo.getAwardUnit()));
        return totalAll;
    }

    /**
     * 计算指定高所在的通胀详情
     * Calculate inflation details at specified time points
     *
     * @param chain             链信息
     * @param height            指定高度
     * @return                  指定高度所在的通胀阶段详情
     * */
    private static InflationInfo getInflationInfo(Chain chain, long height){
        InflationInfo inflationInfo = new InflationInfo();

        long initHeight = chain.getConfig().getInitHeight();
        long initEndHeight = initHeight + chain.getConfig().getDeflationHeightInterval();

        if(height < initHeight){
            chain.getLogger().info("当前区块高度小于通缩开始高度");
        }

        if(currentInflationInfo != null && height >= currentInflationInfo.getStartHeight() && height <= currentInflationInfo.getEndHeight()){
            return currentInflationInfo;
        }

        if(height <= initEndHeight){
            inflationInfo.setStartHeight(initHeight);
            inflationInfo.setEndHeight(initEndHeight);
            inflationInfo.setInflationAmount(chain.getConfig().getInflationAmount());
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationInfo.getInflationAmount()), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        }else{
            long differentCount = (height - initEndHeight)/chain.getConfig().getDeflationHeightInterval();
            if((height - initEndHeight)%chain.getConfig().getDeflationHeightInterval() != 0){
                differentCount ++;
            }
            long differentHeight = differentCount*chain.getConfig().getDeflationHeightInterval();
            inflationInfo.setStartHeight(initHeight + differentHeight);
            inflationInfo.setEndHeight(initEndHeight + differentHeight);
            double ratio = DoubleUtils.div(chain.getConfig().getDeflationRatio(), NulsEconomicConstant.VALUE_OF_100, 4);
            BigInteger inflationAmount = DoubleUtils.mul(new BigDecimal(chain.getConfig().getInflationAmount()),BigDecimal.valueOf(Math.pow(ratio, differentCount))).toBigInteger();
            inflationInfo.setInflationAmount(inflationAmount);
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationAmount), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        }
        currentInflationInfo = inflationInfo;
        Log.info("通胀发生改变，当前通胀通胀开始高度{}：，当前阶段通胀结束高度：{},当前阶段通胀总数：{}，当前阶段出块单位奖励：{}", inflationInfo.getStartHeight(),inflationInfo.getEndHeight(),inflationInfo.getInflationAmount(),inflationInfo.getAwardUnit());
        return inflationInfo;
    }
}
