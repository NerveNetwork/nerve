package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.cache.impl.FarmCacheImpl;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.txdata.FarmUpdateData;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmUpdateTxHelper {

    @Autowired
    private FarmCache farmCache;

    public ValidaterResult validate(Chain chain, Transaction tx) {

        NulsLogger logger = chain.getLogger();
        FarmUpdateData txData = new FarmUpdateData();
        try {
            txData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        return validateTxData(chain, tx, txData, null);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmUpdateData txData, FarmTempManager farmTempManager) {
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getChainId();
        if (null == txData || txData.getFarmHash() == null || txData.getWithdrawLockTime() < 0 || txData.getNewSyrupPerBlock() == null) {
            logger.warn("Incomplete transaction business basic data");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_TX_DATA_ERROR);
        }
        // 验证每个区块奖励数额区间正确
        if (null == txData.getNewSyrupPerBlock() || txData.getNewSyrupPerBlock().compareTo(BigInteger.ZERO) <= 0) {
            logger.warn("The number of rewards per block must be greater than 0");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_PER_BLOCK_ERROR);
        }

        if (txData.getChangeType() < 0 || txData.getChangeType() > 1) {
            logger.warn("The total number of awards must be greater than the number of awards per block");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_CHANGE_ERROR);
        }

        if (txData.getChangeTotalSyrupAmount() != null && txData.getChangeTotalSyrupAmount().compareTo(BigInteger.ZERO) < 0) {
            logger.warn("The total number of changes must not be little than 0");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_CHANGE_ERROR);
        }

        byte[] address;
        try {
            address = SwapUtils.getSingleAddressFromTX(tx, chainId, false);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        if (address == null) {
            logger.warn("Incorrect transaction signature");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SIGNER_COUNT_ERROR);
        }
        //验证farm是否存在
        FarmPoolPO farm = farmCache.get(txData.getFarmHash());
        String farmHash = txData.getFarmHash().toHex();
        if (farmTempManager != null && farmTempManager.getFarm(farmHash) != null) {
            farm = farmTempManager.getFarm(farmHash);
        }
        if (farm == null) {
            logger.warn("Farm not exist.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        //既不能修改，也不是追加糖果的交易，不通过
        if (!farm.isModifiable() && !onlyAddSyrup(farm, txData)) {
            logger.warn("The user does not have farm management permission");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_PERMISSION_ERROR);
        }
        if (!ArraysTool.arrayEquals(farm.getCreatorAddress(), address)) {
            logger.warn("The user does not have farm management permission");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_PERMISSION_ERROR);
        }
        if (txData.getChangeType() == 1) {
            if (null == txData.getChangeTotalSyrupAmount() || BigInteger.ZERO.compareTo(txData.getChangeTotalSyrupAmount()) == 0) {
                return ValidaterResult.getSuccess();
            }
            if (txData.getChangeTotalSyrupAmount().compareTo(farm.getSyrupTokenBalance()) > 0) {
                logger.warn("The balance of syrup asset of the farm is not enough.");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_BALANCE_NOT_ENOUGH);
            }
            return ValidaterResult.getSuccess();
        }
        //amount和coindata的保持一致
        //转入资产地址为adminAddress
        //接收地址为farm地址
        CoinData coinData;
        try {
            coinData = tx.getCoinDataInstance();
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        boolean result = false;
        for (CoinTo to : coinData.getTo()) {
            if (ArraysTool.arrayEquals(to.getAddress(), SwapUtils.getFarmAddress(chain.getChainId()))) {
                if (to.getLockTime() != 0) {
                    logger.warn("syrup asset cannot be locked");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_CANNOT_LOCK);
                }
                if (to.getAssetsChainId() != farm.getSyrupToken().getChainId() || to.getAssetsId() != farm.getSyrupToken().getAssetId()) {
                    logger.warn("Incorrect type of syrup assets");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_ASSET_TYPE_ERROR);
                }
                if (to.getAmount().compareTo(txData.getChangeTotalSyrupAmount()) != 0) {
                    logger.warn("Incorrect amount of syrup asset.");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_AMOUNT_ERROR);
                }
                result = true;
            }
        }
        if (coinData.getTo().isEmpty() && BigInteger.ZERO.compareTo(txData.getChangeTotalSyrupAmount()) == 0) {
            result = true;
        }
        if (!result) {
            logger.warn("Assets must be transferred to the farm address");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADDRESS_ERROR);
        }

        return ValidaterResult.getSuccess();
    }

    private boolean onlyAddSyrup(FarmPoolPO farm, FarmUpdateData txData) {
        if (txData.getChangeType() != 0) {
            return false;
        }
        if (null != txData.getNewSyrupPerBlock() && farm.getSyrupPerBlock().compareTo(txData.getNewSyrupPerBlock()) != 0) {
            return false;
        }
        if (txData.getWithdrawLockTime() != farm.getWithdrawLockTime()) {
            return false;
        }
        return true;
    }

    public void setFarmCacher(FarmCacheImpl farmCacher) {
        this.farmCache = farmCacher;
    }
}
