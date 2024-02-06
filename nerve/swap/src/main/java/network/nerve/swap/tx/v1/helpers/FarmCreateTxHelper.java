package network.nerve.swap.tx.v1.helpers;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.txdata.FarmCreateData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class FarmCreateTxHelper {

    @Autowired
    private FarmCache farmCache;

    @Autowired
    private FarmStorageService storageService;

    @Autowired
    private LedgerAssetCache ledgerAssetCache;


    public ValidaterResult commit(int chainId, Transaction tx) throws NulsException {
        FarmCreateData txData = new FarmCreateData();
        txData.parse(tx.getTxData(), 0);

        FarmPoolPO po = getBean(chainId, tx, txData);

        farmCache.put(tx.getHash(), po);
        storageService.save(chainId, po);
        return ValidaterResult.getSuccess();
    }

    public FarmPoolPO getBean(int chainId, Transaction tx, FarmCreateData txData) throws NulsException {
        FarmPoolPO po = new FarmPoolPO();
        TransactionSignature signature = new TransactionSignature();
        try {
            signature.parse(tx.getTransactionSignature(), 0);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(e.getErrorCode());
        }
        if (signature.getSignersCount() != 1) {
            Log.warn("The number of signatures cannot be greater than 1.");
            throw new NulsException(SwapErrorCode.FARM_SIGNER_COUNT_ERROR);
        }
        byte[] address;
        try {
            address = SwapUtils.getSingleAddressFromTX(tx, chainId,false);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(e.getErrorCode());
        }
        po.setCreatorAddress(address);
        po.setFarmHash(tx.getHash());
        po.setLastRewardBlock(txData.getStartBlockHeight());
        po.setAccSyrupPerShare(BigInteger.ZERO);
        po.setLockedTime(txData.getLockedTime());
        po.setStakeToken(txData.getStakeToken());
        po.setStartBlockHeight(txData.getStartBlockHeight());
        po.setSyrupPerBlock(txData.getSyrupPerBlock());
        po.setSyrupToken(txData.getSyrupToken());
        po.setTotalSyrupAmount(txData.getTotalSyrupAmount());
        po.setSyrupTokenBalance(txData.getTotalSyrupAmount());
        po.setModifiable(txData.isModifiable());
        po.setWithdrawLockTime(txData.getWithdrawLockTime());
        po.setSyrupLockTime(txData.getSyrupLockTime());
        return po;
    }

    public ValidaterResult rollback(int chainId, Transaction tx) {
        farmCache.remove(tx.getHash());
        storageService.delete(chainId, tx.getHash().getBytes());
        return ValidaterResult.getSuccess();
    }

    public ValidaterResult validate(Chain chain, Transaction tx) {

        NulsLogger logger = chain.getLogger();
        FarmCreateData txData = new FarmCreateData();
        try {
            txData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }

        return validateTxData(chain, tx, txData);
    }

    public ValidaterResult validateTxData(Chain chain, Transaction tx, FarmCreateData txData) {
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getChainId();
        if (null == txData || txData.getStakeToken() == null || txData.getStartBlockHeight() < 0 || txData.getSyrupPerBlock() == null || txData.getSyrupToken() == null) {
            logger.warn("Incomplete transaction business basic data");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_TX_DATA_ERROR);
        }
        // validate2Existing assets
        if (ledgerAssetCache.getLedgerAsset(chainId, txData.getStakeToken()) == null) {
            logger.warn("Incorrect type of stake assets");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_STAKE_ASSET_TYPE_ERROR);
        }
        if (ledgerAssetCache.getLedgerAsset(chainId, txData.getSyrupToken()) == null) {
            logger.warn("Incorrect type of syrup assets");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_ASSET_TYPE_ERROR);
        }

        // Verify that the reward amount range for each block is correct
        if (null == txData.getSyrupPerBlock() || txData.getSyrupPerBlock().compareTo(BigInteger.ZERO) <= 0) {
            logger.warn("The number of rewards per block must be greater than 0");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_PER_BLOCK_ERROR);
        }
        if (txData.getTotalSyrupAmount() == null || txData.getTotalSyrupAmount().compareTo(txData.getSyrupPerBlock()) <= 0) {
            logger.warn("The total number of awards must be greater than the number of awards per block");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_TOTAL_SYRUP_ERROR);
        }

        byte[] address;
        try {
            address = SwapUtils.getSingleAddressFromTX(tx, chainId,false);
        } catch (NulsException e) {
            logger.error(e);
            return ValidaterResult.getFailed(e.getErrorCode());
        }
        if (address == null) {
            logger.warn("Incorrect transaction signature");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_SIGNER_COUNT_ERROR);
        }
        //amountandcoindataMaintain consistency
        //Transferred asset address isadminAddress
        //The receiving address isfarmaddress
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
                if (to.getAssetsChainId() != txData.getSyrupToken().getChainId() || to.getAssetsId() != txData.getSyrupToken().getAssetId()) {
                    logger.warn("Incorrect type of syrup assets");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_ASSET_TYPE_ERROR);
                }
                if (to.getAmount().compareTo(txData.getTotalSyrupAmount()) != 0) {
                    logger.warn("Incorrect amount of syrup asset.");
                    return ValidaterResult.getFailed(SwapErrorCode.FARM_SYRUP_DEPOSIT_AMOUNT_ERROR);
                }
                result = true;
            }
        }
        if (!result) {
            logger.warn("Assets must be transferred to the farm address");
            return ValidaterResult.getFailed(SwapErrorCode.FARM_ADDRESS_ERROR);
        }

        return ValidaterResult.getSuccess();
    }

    public void setFarmCacher(FarmCache farmCache) {
        this.farmCache = farmCache;
    }

    public void setStorageService(FarmStorageService storageService) {
        this.storageService = storageService;
    }

    public void setLedgerAssetCache(LedgerAssetCache ledgerAssetCache) {
        this.ledgerAssetCache = ledgerAssetCache;
    }
}
