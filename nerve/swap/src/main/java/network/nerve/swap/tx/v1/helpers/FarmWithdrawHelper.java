package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmWithdrawHelper {

    @Autowired
    private FarmCache farmCache;

    @Autowired
    private FarmStorageService storageService;

    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;

    public ValidaterResult validate(Chain chain, Transaction tx, long blockTime) {
        NulsLogger logger = chain.getLogger();
        FarmStakeChangeData data = new FarmStakeChangeData();
        try {
            data.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(SwapErrorCode.DATA_PARSE_ERROR);
        }
        return validateTxData(chain, tx, data, null, blockTime);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmStakeChangeData data, FarmTempManager farmTempManager, long blockTime) {
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
        if (data.getAmount().compareTo(BigInteger.ZERO) < 0) {
            logger.warn("Incorrect stake amount.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_AMOUNT_ERROR);
        }
        byte[] userAddress;
        try {
            userAddress = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), true);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        FarmUserInfoPO user = userInfoStorageService.load(chain.getChainId(), data.getFarmHash(), userAddress);
        if (null == user || user.getAmount().compareTo(data.getAmount()) < 0) {
            logger.warn("Withdraw excess.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_WITHDRAW_EXCESS_ERROR);
        }

        if ((blockTime > 0 && blockTime < farm.getLockedTime()) || (blockTime == 0 && NulsDateUtils.getCurrentTimeSeconds() < farm.getLockedTime())) {
            logger.warn("Farm is still locked.");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_IS_LOCKED_ERROR);
        }

        return ValidaterResult.getSuccess();
    }

    public void setFarmCacher(FarmCache farmCache) {
        this.farmCache = farmCache;
    }

    public void setStorageService(FarmStorageService storageService) {
        this.storageService = storageService;
    }

    public void setUserInfoStorageService(FarmUserInfoStorageService userInfoStorageService) {
        this.userInfoStorageService = userInfoStorageService;
    }
}
