package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmStakeHelper {

    @Autowired
    private FarmCache farmCache;

    @Autowired
    private FarmStorageService storageService;

    public ValidaterResult validate(Chain chain, Transaction tx) {
        NulsLogger logger = chain.getLogger();
        FarmStakeChangeData data = new FarmStakeChangeData();
        try {
            data.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(SwapErrorCode.DATA_PARSE_ERROR);
        }
        return validateTxData(chain, tx, data, null);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmStakeChangeData data, FarmTempManager farmTempManager) {
        NulsLogger logger = chain.getLogger();
        //验证farm是否存在
        FarmPoolPO farm = farmCache.get(data.getFarmHash());
        String farmHash = data.getFarmHash().toHex();
        if (farmTempManager != null && farmTempManager.getFarm(farmHash) != null) {
            farm = farmTempManager.getFarm(farmHash);
        }
        if (farm == null) {
            logger.warn("Farm not exist.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        if (farm.getSyrupTokenBalance().compareTo(BigInteger.ZERO) <= 0) {
            logger.warn("The balance of syrup asset of the farm is not enough.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_BALANCE_NOT_ENOUGH);
        }
        if (data.getAmount().compareTo(BigInteger.ZERO) < 0) {
            logger.warn("Incorrect stake amount.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_AMOUNT_ERROR);
        }
        //地址、资产、金额是否正确
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
                    logger.warn("Transferred in stake asset cannot be locked");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_LOCK_ERROR);
                }
                if (to.getAssetsChainId() != farm.getStakeToken().getChainId() || to.getAssetsId() != farm.getStakeToken().getAssetId()) {
                    logger.warn("Incorrect type of stake asset");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_ASSET_TYPE_ERROR);
                }
                if (to.getAmount().compareTo(data.getAmount()) != 0) {
                    logger.warn("Incorrect stake amount");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_AMOUNT_ERROR);
                }
                result = true;
            }
        }
        if (!result) {
            logger.warn("Assets must be transferred to the farm address");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADDRESS_ERROR);
        }
        byte[] address = null;
        for (CoinFrom from : coinData.getFrom()) {
            if (null == address) {
                address = from.getAddress();
                continue;
            }
            if (!ArraysTool.arrayEquals(from.getAddress(), address)) {
                logger.warn("Transferred in pledged assets cannot be locked");
                return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_ADDRESS_ERROR);
            }
        }
        return ValidaterResult.getSuccess();
    }

    public void setFarmCacher(FarmCache farmCache) {
        this.farmCache = farmCache;
    }

    public void setStorageService(FarmStorageService storageService) {
        this.storageService = storageService;
    }
}
