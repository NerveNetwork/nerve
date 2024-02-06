package io.nuls.consensus.utils.validator.base;

import io.nuls.base.data.*;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Basic validator
 *
 * @author tag
 */
public abstract class BaseValidator {
    /**
     * Basic validator
     *
     * @param chain Chain information
     * @param tx    Transaction information
     * @return Verification results
     * @throws NulsException data error
     * @throws IOException   Data serialization error
     */
    public abstract Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException;

    /**
     * Create nodes and margin tradingCoinDatavalidate
     * Create agent and margin call transaction coinData validation
     *
     * @param chain
     * @param deposit  Entrustment information/deposit
     * @param coinData TransactionalCoinData/CoinData
     * @param address  account
     * @return boolean
     */
    public Result appendDepositCoinDataValid(Chain chain, BigInteger deposit, CoinData coinData, byte[] address) {
        return appendDepositCoinDataValid(chain, deposit, coinData, address, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * Create nodes and margin tradingCoinDatavalidate
     * Create agent and margin call transaction coinData validation
     *
     * @param chain
     * @param deposit      Entrustment information/deposit
     * @param coinData     TransactionalCoinData/CoinData
     * @param address      account
     * @param assetChainId Mortgage asset chainID
     * @param assetId      Mortgage assetsID
     * @return boolean
     */
    public Result appendDepositCoinDataValid(Chain chain, BigInteger deposit, CoinData coinData, byte[] address, int assetChainId, int assetId) {
        if (coinData == null || coinData.getTo().size() == 0) {
            chain.getLogger().error("CoinData validation failed");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        //ToThere is only one piece of data available,fromThere may be two in it（When the entrusted amount is not the main asset of this chain, there are two pieces of data）
        if (coinData.getTo().size() != 1) {
            chain.getLogger().error("CoinData data is greater than one");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        CoinTo toCoin = coinData.getTo().get(0);
        //fromRelated totoThe middle account must be the same and must be the node creator
        if (!Arrays.equals(address, toCoin.getAddress()) || toCoin.getAssetsChainId() != assetChainId || toCoin.getAssetsId() != assetId) {
            chain.getLogger().error("CoinData asset invalidation");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        for (CoinFrom coinFrom : coinData.getFrom()) {
            if (!Arrays.equals(address, coinFrom.getAddress())) {
                chain.getLogger().error("CoinData from corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }
        //Entrusted amount andCoinDataInconsistent amount in the middle
        if (!BigIntegerUtils.isEqual(deposit, toCoin.getAmount())) {
            chain.getLogger().error("CoinData is not equal to the entrusted amount");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        //CoinDataLock time error
        if (toCoin.getLockTime() != ConsensusConstant.CONSENSUS_LOCK_TIME) {
            chain.getLogger().error("CoinData lock time error");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * Cancellation of nodes and exit of margin tradingCoinDatavalidate
     * Verification of cancellation agent and exit margin transaction coinData
     *
     * @param amount       Entrustment information/deposit
     * @param coinData     TransactionalCoinData/CoinData
     * @param chain        Chain information
     * @param address      account
     * @param realLockTime Lock time
     * @return boolean
     */
    public Result reduceDepositCoinDataValid(Chain chain, BigInteger amount, CoinData coinData, byte[] address, long realLockTime) {
        return reduceDepositCoinDataValid(chain, amount, coinData, address, realLockTime, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * Cancellation of nodes and exit of margin tradingCoinDatavalidate
     * Verification of cancellation agent and exit margin transaction coinData
     *
     * @param amount       Entrustment information/deposit
     * @param coinData     TransactionalCoinData/CoinData
     * @param chain        Chain information
     * @param address      account
     * @param realLockTime Lock time
     * @param assetChainId Exit the asset chainID
     * @param assetId      Exit assetsID
     * @return boolean
     */
    public Result reduceDepositCoinDataValid(Chain chain, BigInteger amount, CoinData coinData, byte[] address, long realLockTime, int assetChainId, int assetId) {
        if (coinData.getFrom().size() == 0 || coinData.getTo().size() == 0) {
            chain.getLogger().error("CoinData from or to is null");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        BigInteger fromAmount = BigInteger.ZERO;
        //fromRelated totoThe middle account must be the same and must be the node creator,AndtoMedium assets must be associated withtxDataConsistent assets
        for (CoinTo coinTo : coinData.getTo()) {
            if (!Arrays.equals(address, coinTo.getAddress()) || coinTo.getAssetsChainId() != assetChainId || coinTo.getAssetsId() != assetId) {
                chain.getLogger().error("CoinData to corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }
        for (CoinFrom coinFrom : coinData.getFrom()) {
            if (!Arrays.equals(address, coinFrom.getAddress())) {
                chain.getLogger().error("CoinData from corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            if (coinFrom.getAssetsChainId() == assetChainId && coinFrom.getAssetsId() == assetId) {
                fromAmount = fromAmount.add(coinFrom.getAmount());
            }
        }

        //Verify lock time,Except for exiting margin trading, all other unlocked transactions have only one optionCoinToData, there may be two possible ways to exit the margin: one is to exit the margin, and the other is to lock the remaining amount back in
        int toSize = coinData.getTo().size();
        if (toSize == 1) {
            if (amount.compareTo(fromAmount) != 0) {
                chain.getLogger().error("The amount in txData is inconsistent with the unlocked amount");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            long lockTime = coinData.getTo().get(0).getLockTime();
            if (realLockTime != lockTime) {
                chain.getLogger().error("Lock time error! lockTime:{},realLockTime:{}", lockTime, realLockTime);
                return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_LOCK_TIME_ERROR);
            }
        } else if (toSize == 2) {
            //validateCoinDataMedium amount,fromThe amount in must be greater than or equal totxDatamoney（Exit margin tradingfromThe moderate amount may be greater thantxDataThe amount in due to possible assemblyfromSometimes there are situations of returning and relocking）
            if (amount.compareTo(fromAmount) > 0) {
                chain.getLogger().error("Amount in coinData from is less than that in txData");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            //If there are twoCoinToThen a lock time isrealLockTimeAnother item has been permanently locked again, and the locked amount must be equal tofrommoney - Exit amount
            boolean unlockSuccess = false;
            boolean lockSuccess = false;
            BigInteger reLockAmount = fromAmount.subtract(amount);
            for (CoinTo to : coinData.getTo()) {
                //Is the unlocking time correct? The unlocking amount must be greater thantoUnlocked amount in(becausetoMedium amount will reduce handling fees)
                if (realLockTime == to.getLockTime() && amount.compareTo(to.getAmount()) > 0) {
                    unlockSuccess = true;
                    continue;
                }
                //The amount for re locking must be equal toFromUnlocked amount in - Unlocking amount
                if (to.getLockTime() == -1 && reLockAmount.compareTo(to.getAmount()) == 0) {
                    lockSuccess = true;
                }
            }
            if (!unlockSuccess || !lockSuccess) {
                chain.getLogger().error("CoinTo validation failed");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        } else {
            chain.getLogger().error("Wrong number of coinTo");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * Transaction fee verification
     *
     * @param chain    Chain information
     * @param coinData coinData
     * @param tx       transaction
     */
    public Result validFee(Chain chain, CoinData coinData, Transaction tx) throws IOException {
//        int size = tx.serialize().length;
//        BigInteger fee = TransactionFeeCalculator.getConsensusTxFee(size, chain.getConfig().getFeeUnit());
//        ChargeResult result = ConsensusManager.getFee(coinData, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
//        if (fee.compareTo(result.getMainCharge().getFee()) > 0) {
//            chain.getLogger().error("Insufficient service charge");
//            return Result.getFailed(ConsensusErrorCode.FEE_NOT_ENOUGH);
//        }
        return ConsensusNetUtil.getSuccess();
    }
}
