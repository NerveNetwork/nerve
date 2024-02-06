package io.nuls.consensus.utils;

import io.nuls.common.NerveCoreConfig;
import io.nuls.consensus.model.po.AwardSettlePo;
import io.nuls.consensus.storage.AwardSettleRecordService;
import io.nuls.consensus.utils.compare.CoinToComparator;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.manager.DepositManager;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.InflationInfo;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.po.AwardSettleRecordPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinTo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.economic.nuls.constant.NulsEconomicConstant;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.utils.manager.AgentManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Component
public class ConsensusAwardUtil {
    @Autowired
    private static AgentManager agentManager;

    @Autowired
    private static DepositManager depositManager;

    @Autowired
    private static NerveCoreConfig config;

    @Autowired
    private static AwardSettleRecordService awardSettleRecordService;

    @Autowired
    private static ChainManager chainManager;

    /**
     * Basic information on consensus rewards
     */
    private static InflationInfo currentInflationInfo = null;

    /**
     * Details of consensus rewards to be settled next time locally
     */
    private static AwardSettlePo lastestSettleResult;

    /**
     * Obtain the corresponding quantity from the pricing systemNVT
     *
     * @param amount quantity
     * @param time   Transaction time（millisecond）
     * @return Corresponding quantityNVT
     */
    public static BigInteger getRealAmount(int chainId, BigInteger amount, StackingAsset stackingAsset, long time) throws NulsException {
        if (stackingAsset.getChainId() == config.getChainId() && stackingAsset.getAssetId() == config.getAssetId()) {
            return amount;
        }
        String dateStr = NulsDateUtils.timeStamp2DateStr(time, ConsensusConstant.DATE_FORMAT, Locale.UK);
        BigDecimal nvtCount = getRealAmount(chainId, new BigDecimal(amount), stackingAsset, dateStr);
//        BigDecimal nvtVal = DoubleUtils.mul(nvtCount, Math.pow(10, 8));
        return nvtCount.toBigInteger();
    }


    /**
     * Obtain the corresponding quantity from the pricing systemNVT
     *
     * @param amount quantity
     * @param date   time
     * @return Corresponding quantityNVT
     */
    public static BigDecimal getRealAmount(int chainId, BigDecimal amount, StackingAsset stackingAsset, String date) throws NulsException {
        if (stackingAsset.getChainId() == config.getChainId() && stackingAsset.getAssetId() == config.getAssetId()) {
            return amount;
        }
        double price = CallMethodUtils.getRealAmount(chainId, stackingAsset.getOracleKey(), date);
        double nervePrice = CallMethodUtils.getRealAmount(chainId, ConsensusConstant.DEFALT_KEY, date);

        BigDecimal realAmount = DoubleUtils.div(amount, Math.pow(10, stackingAsset.getDecimal()));
        BigDecimal nvtVal = DoubleUtils.div(DoubleUtils.mul(realAmount, price), new BigDecimal(nervePrice));
        return DoubleUtils.mul(nvtVal, Math.pow(10, 8));
    }

    /**
     * Determine whether consensus rewards need to be settled
     *
     * @param chain chain info
     * @param time  block time
     */
    public static boolean settleConsensusAward(Chain chain, long time) {
        if (chain.getBestHeader() == null || chain.getBestHeader().getHeight() == 0 || time < chain.getBestHeader().getTime()) {
            return false;
        }
        //The first block, or the previous block, has not been processed completely
        boolean result = (null != lastestSettleResult && lastestSettleResult.getIssuedCount() > 0 && lastestSettleResult.getIssuedCount() < lastestSettleResult.getSettleDetails().size())
                || !isSameSection(time, chain.getBestHeader().getTime());

        return result;
    }

    private static boolean isSameSection(long time, long time1) {
        return time / ConsensusConstant.ONE_DAY_SECONDS == time1 / ConsensusConstant.ONE_DAY_SECONDS;
    }

    /**
     * When block verification fails, if the settlement details are generated by local scheduled tasks, the details will not be deleted; otherwise, the details need to be deleted
     */
    public static void clearSettleDetails() {
        if (lastestSettleResult != null && !lastestSettleResult.isSettled()) {
            lastestSettleResult.getclearSettleDetails();
        }
    }

    /**
     * Save Block
     *
     * @param chain       chain info
     * @param blockHeader block header
     */
    public static void saveAndSwitchSettleRecord(Chain chain, BlockHeader blockHeader) {
        // Determine whether the current block is a settlement reward block(Is the current block the first block of the day)If it is a settlement reward block, save and switch the settlement result
        if (!settleConsensusAward(chain, blockHeader.getTime())) {
            return;
        }
        try {
            AwardSettleRecordPo po = awardSettleRecordService.get(chain.getChainId());
            if (po == null) {
                po = new AwardSettleRecordPo();
            }
            // If only a portion of the rewards have been distributed to the block, update the stored data and wait for the next block
            if (null != lastestSettleResult) {
                int issuedCount = (int) (lastestSettleResult.getIssuedCount() + chain.getConfig().getMaxCoinToOfCoinbase());
                if (null != lastestSettleResult.getSettleDetails() && issuedCount < lastestSettleResult.getSettleDetails().size()) {
                    po.getLastestSettleResult().setIssuedCount(issuedCount);
                    lastestSettleResult.setIssuedCount(issuedCount);
                    awardSettleRecordService.save(po, chain.getChainId());
                    chain.getLogger().info("Partial rewards have been distributed：-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- ");
                    return;
                }
            }
            if (po.getRecordList() == null) {
                po.setRecordList(new ArrayList<>());
            }
            //Initialize consensus reward settlement information for the first day
            long newStartHeight;
            long newEndHeight = blockHeader.getHeight() - 1;
            if (lastestSettleResult == null) {
                //chain.getLogger().debug("Initialize the first consensus award settlement date information");
                newStartHeight = 1;
            } else {
                po.getRecordList().add(lastestSettleResult);
                newStartHeight = lastestSettleResult.getEndHeight() + 1;
            }
            //The next settlement date is the day before the current block
            String dateStr = getDateStr(blockHeader.getTime(), -1);
            AwardSettlePo nextSettlePo = new AwardSettlePo(newStartHeight, newEndHeight, dateStr);
            po.setLastestSettleResult(nextSettlePo);
            chain.getLogger().info("Rewards have been issued!!!!{}-{}", po.getLastestSettleResult().getStartHeight(), po.getLastestSettleResult().getEndHeight());
            awardSettleRecordService.save(po, chain.getChainId());
            lastestSettleResult = nextSettlePo;
            //chain.getLogger().debug("Consensus award settlement date switch completed,nextSettleDate:{},startHeight:{},endHeight:{}", dateStr, newStartHeight, newEndHeight);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    private static String getDateStr(long time, int offset) {
//        Date date = new Date(NulsDateUtils.secondsToMillis(time));
//        date = NulsDateUtils.dateAdd(date, offset);
//        SimpleDateFormat sdf = new SimpleDateFormat(ConsensusConstant.DATE_FORMAT);
//        String dateStr = sdf.format(date);
        return (time / ConsensusConstant.ONE_DAY_SECONDS + offset) + "";
    }

    /**
     * Save Block
     *
     * @param chain       chain info
     * @param blockHeader block header
     * @param preHeader   previous block header
     */
    public static void rollbackAndSwitchSettleRecord(Chain chain, BlockHeader preHeader, BlockHeader blockHeader) {
        //If the rolled back block is a settlement consensus reward block, the consensus reward record needs to be rolled back
        if (preHeader == null || preHeader.getHeight() == 0 || blockHeader.getTime() < preHeader.getTime()) {
            return;
        }
        if (NulsDateUtils.isSameDayBySecond(preHeader.getTime(), blockHeader.getTime())) {
            return;
        }
        try {
            AwardSettleRecordPo po = awardSettleRecordService.get(chain.getChainId());
            lastestSettleResult = po.getRecordList().remove(0);
            awardSettleRecordService.save(po, chain.getChainId());
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * Blockpack consensus reward
     *
     * @param chain      Chain information
     * @param coinToList Reward List
     * @param time       Block time(millisecond)
     */
    public static void packConsensusAward(Chain chain, List<CoinTo> coinToList, long time) {
        //Obtain settlement date fortimeCorresponding date two days ago(Starting from 11:30 am every day, the consensus reward for the previous day is calculated. The first block of tomorrow is used to settle the consensus reward calculated for today)

        String dateStr = getDateStr(time, -2);

        if (lastestSettleResult == null) {
            chain.getLogger().warn("No consensus reward, block settlement date：{}", dateStr);
            return;
        }

        //If the settlement date is before the current settlement date, an error will be returned directly（It is possible that if there is no block generated in the previous few days, the final unsettled consensus reward needs to be settled）
        if (dateStr.compareTo(lastestSettleResult.getDate()) < 0) {
            chain.getLogger().warn("Consensus reward settlement information error, date to be settled：{}, Block settlement date：{}", lastestSettleResult.getDate(), dateStr);
            return;
        }

        // Attempt to block rewards
        List<CoinTo> details = lastestSettleResult.getSettleDetails();
        chain.getLogger().info(lastestSettleResult.getDate() + " packing: " + lastestSettleResult.getStartHeight() + "-" +
                lastestSettleResult.getEndHeight() + "==" + lastestSettleResult.getIssuedCount() + " of " + lastestSettleResult.getSettleDetails().size());
        if (details != null && details.size() > chain.getConfig().getMaxCoinToOfCoinbase()) {
            int toIndex = (int) (lastestSettleResult.getIssuedCount() + chain.getConfig().getMaxCoinToOfCoinbase());
            details = details.subList((int) lastestSettleResult.getIssuedCount(), toIndex > details.size() ? details.size() : toIndex);
        }
        //Package the budgeted consensus rewards
        mergeAwardAndFee(coinToList, details);
    }

    /**
     * Calculate consensus rewards for the previous day at a scheduled time every day
     *
     * @param chain chain info
     */
    public static void budgetConsensusAward(Chain chain) {
        try {
            //If initialized, query the last settlement details from the database
            if (lastestSettleResult == null) {
                AwardSettleRecordPo recordPo = awardSettleRecordService.get(chain.getChainId());
                if (recordPo == null || recordPo.getLastestSettleResult() == null) {
                    chain.getLogger().warn("There is currently no consensus award to be settled");
                    return;
                } else {
                    lastestSettleResult = recordPo.getLastestSettleResult();
                }
            }
            if (lastestSettleResult.isSettled()) {
                return;
            }
            String dateStr = getDateStr(chain.getBestHeader().getTime(), -1);
            if (dateStr.compareTo(lastestSettleResult.getDate()) < 0) {
                chain.getLogger().warn("The current consensus reward settlement date is earlier than the pending settlement date,budgetDate:{},lastestSettleDate:{}", dateStr, lastestSettleResult.getDate());
                return;
            }
            lastestSettleResult.setSettled(true);
            lastestSettleResult.setSettleDetails(settleConsensusAward(chain, lastestSettleResult.getStartHeight(), lastestSettleResult.getEndHeight(), 0, dateStr));
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }


    /**
     * Calculate the consensus reward for the previous day at each startup
     *
     * @param chain chain info
     */
    public static void onStartConsensusAward(Chain chain) {
        try {
            //If initialized, query the last settlement details from the database

            AwardSettleRecordPo recordPo = awardSettleRecordService.get(chain.getChainId());
            if (recordPo == null || recordPo.getLastestSettleResult() == null) {
                chain.getLogger().warn("There is currently no consensus award to be settled");
                return;
            } else {
                lastestSettleResult = recordPo.getLastestSettleResult();
            }
            if (lastestSettleResult.isSettled()) {
                //What has already been calculated will not be counted
                return;
            }

            String dateStr = getDateStr(chain.getBestHeader().getTime(), -1);
            if (dateStr.compareTo(lastestSettleResult.getDate()) < 0) {
                chain.getLogger().warn("The current consensus reward settlement date is earlier than the pending settlement date,budgetDate:{},lastestSettleDate:{}", dateStr, lastestSettleResult.getDate());
                return;
            }
            lastestSettleResult.setSettled(true);
            lastestSettleResult.setSettleDetails(settleConsensusAward(chain, lastestSettleResult.getStartHeight(), lastestSettleResult.getEndHeight(), 0, dateStr));
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * Generate corresponding consensus reward details based on block time when receiving settlement blocks during synchronization process
     *
     * @param chain      Chain information
     * @param coinToList Reward List
     * @param time       Block time(millisecond)
     */
    public static void validBlockConsensusAward(Chain chain, List<CoinTo> coinToList, long time) throws NulsException {
        //Obtain settlement date fortimeCorresponding date two days ago(Starting from 11:30 am every day, the consensus reward for the previous day is calculated. The first block of tomorrow is used to settle the consensus reward calculated for today)

        String dateStr = getDateStr(time, -2);

        if (lastestSettleResult == null) {
            AwardSettleRecordPo recordPo = awardSettleRecordService.get(chain.getChainId());
            if (recordPo == null || recordPo.getLastestSettleResult() == null) {
//                chain.getLogger().warn("There is currently no consensus award to be settled");
                return;
            } else {
                lastestSettleResult = recordPo.getLastestSettleResult();
            }
        }

        //If the settlement date is before the current settlement date, an error will be returned directly（It is possible that if there is no block generated in the previous few days, the final unsettled consensus reward needs to be settled）
        if (dateStr.compareTo(lastestSettleResult.getDate()) < 0) {
            chain.getLogger().warn("Consensus reward settlement information error, date to be settled：{}, Block settlement date：{}", lastestSettleResult.getDate(), dateStr);
            return;
        }

        if (lastestSettleResult.getSettleDetails() != null && !lastestSettleResult.getSettleDetails().isEmpty() && lastestSettleResult.isSettled()) {
            chain.getLogger().info("The consensus reward details have been calculated locally in advance,date：{}", lastestSettleResult.getDate());
        } else {
            lastestSettleResult.setSettled(false);
            lastestSettleResult.setSettleDetails(settleConsensusAward(chain, lastestSettleResult.getStartHeight(), lastestSettleResult.getEndHeight(), 0, dateStr));
        }

        List<CoinTo> details = lastestSettleResult.getSettleDetails();
//        chain.getLogger().info(lastestSettleResult.getDate() + " valid: " + lastestSettleResult.getStartHeight() + "-" + lastestSettleResult.getEndHeight() + "==" + lastestSettleResult.getIssuedCount() + " of " + lastestSettleResult.getSettleDetails().size());
        if (details != null && details.size() > chain.getConfig().getMaxCoinToOfCoinbase()) {
            int toIndex = (int) (lastestSettleResult.getIssuedCount() + chain.getConfig().getMaxCoinToOfCoinbase());
            details = details.subList((int) lastestSettleResult.getIssuedCount(), toIndex >= details.size() ? details.size() : toIndex);
        }
        mergeAwardAndFee(coinToList, details);
    }


    /**
     * Settlement day consensus reward
     *
     * @param chain       Chain information
     * @param startHeight Starting block height
     * @param endHeight   End height
     * @param lockTime    Lock time
     * @param date        Settlement time
     */
    private static List<CoinTo> settleConsensusAward(Chain chain, long startHeight, long endHeight, long lockTime, String date) throws NulsException {
        chain.getLogger().info("Settlement of consensus rewards on specified dates,date:{},startHeight:{},endHeight:{}", date, startHeight, endHeight);
        List<CoinTo> awardDetails = new ArrayList<>();
        if (endHeight <= chain.getConfig().getMinRewardHeight()) {
            return awardDetails;
        }

        if (startHeight >= endHeight || config.getInflationAmount().equals(BigInteger.ZERO)) {
            return awardDetails;
        }

        //Obtain weight details and calculate reward details for each account based on the details
        Map<String, BigDecimal> weightDetails = getWeightDetails(chain, startHeight, endHeight, date);
        if (weightDetails.isEmpty()) {
            return awardDetails;
        }

        //Query the number of blocks generated on the day and calculate the current consensus reward generated
        BigDecimal totalAward = calcDayConsensusAward(chain, startHeight, endHeight);
        if (totalAward.compareTo(BigDecimal.ZERO) == 0) {
            return awardDetails;
        }

        BigInteger award;
        CoinTo newCoinTo;
        String address;
        BigDecimal weight;
        chain.getLogger().info("This calculationtotalAward：" + totalAward.toString());
        for (Map.Entry<String, BigDecimal> entry : weightDetails.entrySet()) {
            address = entry.getKey();
            weight = entry.getValue();
            award = DoubleUtils.mul(totalAward, weight).toBigInteger();
            newCoinTo = new CoinTo(AddressTool.getAddress(address), chain.getChainId(), chain.getAssetId(), award, lockTime);
            awardDetails.add(newCoinTo);
        }
        awardDetails.sort(new CoinToComparator());
        return awardDetails;
    }

    /**
     * Merge consensus rewards and transaction fees
     * Merger consensus award and handling fee
     */
    private static void mergeAwardAndFee(List<CoinTo> feeList, List<CoinTo> awardList) {
        if (awardList == null || awardList.isEmpty()) {
            return;
        }

        if (feeList.isEmpty()) {
            feeList.addAll(awardList);
            return;
        }


        //If the block charges a handling fee for the main asset of this chain, consensus rewards and handling fees need to be merged
        CoinTo coinTo = null;
        for (CoinTo fee : feeList) {
            if (fee.getAssetsChainId() == config.getAgentChainId() && fee.getAssetsId() == config.getAgentAssetId()) {
                coinTo = fee;
                break;
            }
        }

        if (coinTo == null) {
            feeList.addAll(awardList);
            return;
        }

        boolean judgeAddress = true;
        for (CoinTo award : awardList) {
            if (judgeAddress && Arrays.equals(award.getAddress(), coinTo.getAddress())) {
                coinTo.setAmount(coinTo.getAmount().add(award.getAmount()));
                judgeAddress = false;
            } else {
                feeList.add(award);
            }
        }
    }

    /**
     * Obtain weight details
     *
     * @param chain     Chain information
     * @param endHeight End height
     */
    private static Map<String, BigDecimal> getWeightDetails(Chain chain, long startHeight, long endHeight, String date) throws NulsException {
        Map<String, BigDecimal> weightDetails = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        BigDecimal totalDeposit = BigDecimal.ZERO;
        Map<String, BigDecimal> depositMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        //Obtain effective node margin weight
        totalDeposit = agentManager.getAgentDepositByHeight(chain, startHeight, endHeight, depositMap, totalDeposit);

        //Obtain effective commission weight
        totalDeposit = depositManager.getDepositByHeight(chain, startHeight, endHeight, depositMap, totalDeposit, date);

        if (totalDeposit.compareTo(BigDecimal.ZERO) == 0) {
            return weightDetails;
        }

        //Calculate the weight of each account
        for (Map.Entry<String, BigDecimal> entry : depositMap.entrySet()) {
            BigDecimal value = DoubleUtils.div(entry.getValue(), totalDeposit).setScale(8, BigDecimal.ROUND_HALF_UP);
            chain.getLogger().info("Weight calculation：" + entry.getKey() + ":{}", DoubleUtils.getRoundStr(value.doubleValue(), 8));
            weightDetails.put(entry.getKey(), value);
        }

        return weightDetails;
    }

    /**
     * Calculate the total consensus reward for the day
     * Calculate the total consensus Award for the day
     *
     * @return The overall consensus reward for this round
     */
    private static BigDecimal calcDayConsensusAward(Chain chain, long startHeight, long endHeight) {
        if (chain.getConfig().getInflationAmount().compareTo(BigInteger.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        long blockCount = endHeight - startHeight;
        InflationInfo inflationInfo = null;
        if (endHeight <= 4337812) {
            inflationInfo = getInflationInfo(chain, startHeight);
        } else {
            inflationInfo = getInflationInfoNew(chain, startHeight);
        }
        if (endHeight <= inflationInfo.getEndHeight()) {
            return DoubleUtils.mul(new BigDecimal(blockCount), inflationInfo.getAwardUnit());
        }
        BigDecimal totalAll = BigDecimal.ZERO;
        long currentCount;
        while (endHeight > inflationInfo.getEndHeight()) {
            currentCount = inflationInfo.getEndHeight() - startHeight + 1;
            totalAll = totalAll.add(DoubleUtils.mul(new BigDecimal(currentCount), inflationInfo.getAwardUnit()));
            chain.getLogger().info("The consensus reward for this stage is{}The quantity is{}", inflationInfo.getAwardUnit(), currentCount);
            startHeight += currentCount;
            if (endHeight <= 4337812) {
                inflationInfo = getInflationInfo(chain, startHeight);
            } else {
                inflationInfo = getInflationInfoNew(chain, startHeight);
            }
        }
        currentCount = endHeight - startHeight + 1;
        chain.getLogger().info("The rewards for this stage are{}The quantity is{}", inflationInfo.getAwardUnit(), currentCount);
        totalAll = totalAll.add(DoubleUtils.mul(new BigDecimal(currentCount), inflationInfo.getAwardUnit()));
        return totalAll;
    }

    /**
     * Calculate the inflation details for a specified high
     * Calculate inflation details at specified time points
     *
     * @param chain  Chain information
     * @param height Specify height
     * @return Details of the inflation stage at the specified height
     */
    private static InflationInfo getInflationInfo(Chain chain, long height) {
        InflationInfo inflationInfo = new InflationInfo();

        long initHeight = chain.getConfig().getInitHeight();
        long initEndHeight = initHeight + chain.getConfig().getDeflationHeightInterval();

        if (height < initHeight) {
            chain.getLogger().info("The current block height is less than the starting height of deflation");
        }

        if (currentInflationInfo != null && height >= currentInflationInfo.getStartHeight() && height <= currentInflationInfo.getEndHeight()) {
            return currentInflationInfo;
        }

        if (height <= initEndHeight) {
            inflationInfo.setStartHeight(initHeight);
            inflationInfo.setEndHeight(initEndHeight);
            inflationInfo.setInflationAmount(chain.getConfig().getInflationAmount());
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationInfo.getInflationAmount()), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        } else {
            long differentCount = (height - initEndHeight) / chain.getConfig().getDeflationHeightInterval();
            if ((height - initEndHeight) % chain.getConfig().getDeflationHeightInterval() != 0) {
                differentCount++;
            }
            long differentHeight = differentCount * chain.getConfig().getDeflationHeightInterval();
            inflationInfo.setStartHeight(initHeight + differentHeight);
            inflationInfo.setEndHeight(initEndHeight + differentHeight);
            double ratio = DoubleUtils.div(chain.getConfig().getDeflationRatio(), NulsEconomicConstant.VALUE_OF_100, 4);
            BigInteger inflationAmount = DoubleUtils.mul(new BigDecimal(chain.getConfig().getInflationAmount()), BigDecimal.valueOf(Math.pow(ratio, differentCount))).toBigInteger();
            inflationInfo.setInflationAmount(inflationAmount);
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationAmount), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        }
        currentInflationInfo = inflationInfo;
        Log.info("Inflation has changed, and current inflation is beginning to rise{}：At the current stage, the end height of inflation：{},Total inflation at the current stage：{}At the current stage, unit rewards for block production：{}", inflationInfo.getStartHeight(), inflationInfo.getEndHeight(), inflationInfo.getInflationAmount(), inflationInfo.getAwardUnit());
        return inflationInfo;
    }

    private static InflationInfo getInflationInfoNew(Chain chain, long height) {
        InflationInfo inflationInfo = new InflationInfo();

        long initHeight = chain.getConfig().getInitHeight();
        long initEndHeight = initHeight + chain.getConfig().getDeflationHeightInterval();

        if (height < initHeight) {
            chain.getLogger().info("The current block height is less than the starting height of deflation");
        }

        if (currentInflationInfo != null && height >= currentInflationInfo.getStartHeight() && height <= currentInflationInfo.getEndHeight()&&(currentInflationInfo.getStartHeight()!=4320001||currentInflationInfo.getAwardUnit()>20000)) {
            return currentInflationInfo;
        }

        if (height <= initEndHeight) {
            inflationInfo.setStartHeight(initHeight);
            inflationInfo.setEndHeight(initEndHeight);
            inflationInfo.setInflationAmount(chain.getConfig().getInflationAmount());
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationInfo.getInflationAmount()), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        } else {
            long differentCount = (height - initEndHeight) / chain.getConfig().getDeflationHeightInterval();
            if ((height - initEndHeight) % chain.getConfig().getDeflationHeightInterval() != 0) {
                differentCount++;
            }
            long differentHeight = differentCount * chain.getConfig().getDeflationHeightInterval();
            inflationInfo.setStartHeight(initHeight + differentHeight);
            inflationInfo.setEndHeight(initEndHeight + differentHeight);
            double ratio = 1 - chain.getConfig().getDeflationRatio();

            BigInteger inflationAmount = DoubleUtils.mul(new BigDecimal(chain.getConfig().getInflationAmount()), BigDecimal.valueOf(Math.pow(ratio, differentCount))).toBigInteger();
            inflationInfo.setInflationAmount(inflationAmount);
            inflationInfo.setAwardUnit(DoubleUtils.div(new BigDecimal(inflationAmount), chain.getConfig().getDeflationHeightInterval()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
        }
        currentInflationInfo = inflationInfo;
        Log.info("Inflation has changed, and current inflation is beginning to rise{}：At the current stage, the end height of inflation：{},Total inflation at the current stage：{}At the current stage, unit rewards for block production：{}", inflationInfo.getStartHeight(), inflationInfo.getEndHeight(), inflationInfo.getInflationAmount(), inflationInfo.getAwardUnit());
        return inflationInfo;
    }
}
