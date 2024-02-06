package io.nuls.consensus.utils.manager;

import io.nuls.common.NerveCoreConfig;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.model.po.DepositPo;
import io.nuls.consensus.storage.DepositStorageService;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.compare.DepositComparator;
import io.nuls.consensus.utils.enumeration.DepositTimeType;
import io.nuls.consensus.utils.enumeration.DepositType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entrusted information management class, responsible for entrusted information related processing
 * Delegated information management category, responsible for delegated information related processing
 *
 * @author tag
 * 2018/12/5
 */
@Component
public class DepositManager {
    @Autowired
    private DepositStorageService depositStorageService;

    @Autowired
    private NerveCoreConfig config;

    @Autowired
    private static ChainManager chainManager;

    /**
     * Initialize delegation information
     * Initialize delegation information
     *
     * @param chain Chain information/chain info
     */
    public void loadDeposits(Chain chain) throws Exception {
        List<Deposit> allDepositList = new ArrayList<>();
        List<DepositPo> poList = depositStorageService.getList(chain.getConfig().getChainId());
        for (DepositPo po : poList) {
            Deposit deposit = new Deposit(po);
            allDepositList.add(deposit);
        }
        allDepositList.sort(new DepositComparator());
        chain.setDepositList(allDepositList);
    }

    /**
     * Add delegate cache
     * Add delegation cache
     *
     * @param chain   chain info
     * @param deposit deposit info
     */
    public boolean addDeposit(Chain chain, Deposit deposit) {
        if (!depositStorageService.save(new DepositPo(deposit), chain.getConfig().getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        chain.getDepositList().add(deposit);
        return true;
    }

    /**
     * Modify delegation cache
     * modify delegation cache
     *
     * @param chain   chain
     * @param deposit deposit info
     */
    public boolean updateDeposit(Chain chain, Deposit deposit) {
        if (!depositStorageService.save(new DepositPo(deposit), chain.getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        for (Deposit oldDeposit : chain.getDepositList()) {
            if (oldDeposit.getTxHash().equals(deposit.getTxHash())) {
                oldDeposit.setDelHeight(deposit.getDelHeight());
                break;
            }
        }
        return true;
    }

    /**
     * Delete delegation information for the specified chain
     * Delete delegate information for a specified chain
     *
     * @param chain  chain nfo
     * @param txHash Create the entrusted transactionHash/Hash to create the delegated transaction
     */
    public boolean removeDeposit(Chain chain, NulsHash txHash) {
        if (!depositStorageService.delete(txHash, chain.getConfig().getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        chain.getDepositList().removeIf(s -> s.getTxHash().equals(txHash));
        return true;
    }

    /**
     * Obtain specified delegation information
     * Get the specified delegation information
     *
     * @param chain  chain nfo
     * @param txHash Create the entrusted transactionHash/Hash to create the delegated transaction
     */
    public Deposit getDeposit(Chain chain, NulsHash txHash) {
        for (Deposit deposit : chain.getDepositList()) {
            if (deposit.getTxHash().equals(txHash)) {
                return deposit;
            }
        }
        return null;
    }

    /**
     * Calculate the commission amount for each account and return the total commission amount
     *
     * @param chain       Chain information
     * @param endHeight   height
     * @param depositMap  entrust
     * @param totalAmount Total entrusted amount
     * @param date        Calculated based on the feeding rate on that day
     */
    public BigDecimal getDepositByHeight(Chain chain, long startHeight, long endHeight, Map<String, BigDecimal> depositMap, BigDecimal totalAmount, String date) throws NulsException {
        BigDecimal realAmount;
        String address;
        List<DepositPo> depositList;
        long depositLockEndTime = Integer.parseInt(date) * ConsensusConstant.ONE_DAY_SECONDS + ConsensusConstant.ONE_DAY_SECONDS + ConsensusConstant.ONE_DAY_SECONDS / 2;
        try {
            //There cannot be cached data here, as there may be new data inserted midway
            depositList = depositStorageService.getList(chain.getConfig().getChainId());
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return totalAmount;
        }
        if (depositList == null || depositList.isEmpty()) {
            return totalAmount;
        }
        for (DepositPo deposit : depositList) {
            //Effective delegation, the delegation height must be smaller than the specified height and the exit delegation height must be greater than the specified height
            if (deposit.getBlockHeight() > endHeight) {
                continue;
            }
            if (deposit.getDelHeight() != -1 && deposit.getDelHeight() <= endHeight) {
                continue;
            }
            if (endHeight > chain.getConfig().getDepositAwardChangeHeight() && deposit.getBlockHeight() > startHeight) {
                continue;
            }
            StringBuilder ss = new StringBuilder();
            ss.append(AddressTool.getStringAddressByBytes(deposit.getAddress()));
            ss.append("-");
            ss.append(deposit.getAssetChainId());
            ss.append("-");
            ss.append(deposit.getAssetId());
            ss.append("-");
            ss.append(deposit.getDeposit().toString());
            realAmount = calcDepositBase(chain, deposit, date, depositLockEndTime, endHeight);
            ss.append("-real:");
            ss.append(realAmount.toString());
            totalAmount = totalAmount.add(realAmount);

            ss.append("-total:");
            ss.append(totalAmount.toString());

            address = AddressTool.getStringAddressByBytes(deposit.getAddress());
            chain.getLogger().info(ss.toString());
            depositMap.merge(address, realAmount, (oldValue, value) -> oldValue.add(value));
        }
        return totalAmount;
    }


    /**
     * Calculate the actual corresponding commissionNVT
     *
     * @param chain   Chain information
     * @param deposit Entrustment information
     * @@param date        Settlement date
     */
    private BigDecimal calcDepositBase(Chain chain, DepositPo deposit, String date, long time, long endHeight) throws NulsException {
        BigDecimal realDeposit = new BigDecimal(deposit.getDeposit());
        double weightSqrt = 1;
        //If the entrusted asset is the main asset of this chain, multiply it by the corresponding base number
        if (deposit.getAssetChainId() == chain.getChainId() && deposit.getAssetId() == chain.getAssetId()) {
            weightSqrt = chain.getConfig().getLocalAssertBase();
        } else {
            StackingAsset stackingAsset = chainManager.getAssetByAsset(deposit.getAssetChainId(), deposit.getAssetId());
            if (stackingAsset.getStopHeight() != 0 && stackingAsset.getStopHeight() < endHeight) {
                //Expired assets will no longer receive returns
                return BigDecimal.ZERO;
            } else {
                realDeposit = ConsensusAwardUtil.getRealAmount(chain.getChainId(), realDeposit, stackingAsset, date);
                weightSqrt = chain.getConfig().getWeight(deposit.getAssetChainId(), deposit.getAssetId());
                //Liquidity Plan Special Handling Code
//            if (deposit.getAssetChainId() == chain.getChainId() && (deposit.getAssetId() == 32 || deposit.getAssetId() == 33) && config.getV1_7_0Height() > endHeight && endHeight < config.getV1_7_0Height() + 30 * 43200) {
                if (weightSqrt == 25 && config.getV1_7_0Height() > endHeight && endHeight < config.getV1_7_0Height() + 30 * 43200) {
                    weightSqrt = weightSqrt * 36;
                }
            }
        }
        //If it is a regular commission, multiply the regular time by the corresponding base number,Calculate the weight based on the current period after the expiration of the regular commission
        if (deposit.getDepositType() == DepositType.REGULAR.getCode()) {
            DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
            if (depositTimeType != null && deposit.getTime() + depositTimeType.getTime() >= time) {
                weightSqrt = weightSqrt * depositTimeType.getWeight();
            }
        }
        realDeposit = DoubleUtils.mul(realDeposit, new BigDecimal(Math.sqrt(weightSqrt)).setScale(4, BigDecimal.ROUND_HALF_UP));
        return realDeposit;
    }
}
