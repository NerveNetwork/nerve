package network.nerve.pocbft.utils.validator.base;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.manager.ConsensusManager;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * 基础验证器
 * @author  tag
 * */
public abstract class BaseValidator {
    /**
     * 基础验证器
     * @param chain    链信息
     * @param tx       交易信息
     * @exception NulsException 数据错误
     * @exception IOException   数据序列化错误
     * @return         验证结果
     * */
    public abstract Result validate(Chain chain, Transaction tx) throws NulsException, IOException;

    /**
     * 创建节点和追加保证金交易CoinData验证
     * Create agent and margin call transaction coinData validation
     *
     * @param chain
     * @param deposit           委托信息/deposit
     * @param coinData          交易的CoinData/CoinData
     * @param address           账户
     * @return boolean
     */
    public Result appendDepositCoinDataValid(Chain chain, BigInteger deposit, CoinData coinData,byte[] address){
        return appendDepositCoinDataValid(chain, deposit, coinData, address, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * 创建节点和追加保证金交易CoinData验证
     * Create agent and margin call transaction coinData validation
     *
     * @param chain
     * @param deposit           委托信息/deposit
     * @param coinData          交易的CoinData/CoinData
     * @param address           账户
     * @param assetChainId      抵押资产链ID
     * @param assetId           抵押资产ID
     * @return boolean
     */
    public Result appendDepositCoinDataValid(Chain chain, BigInteger deposit, CoinData coinData,byte[] address,int assetChainId,int assetId){
        if (coinData == null || coinData.getTo().size() == 0) {
            chain.getLogger().error("CoinData validation failed");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        //To有且只有一条数据,from中可能有两条（当委托金额不是本链主资产时有两条数据）
        if(coinData.getTo().size() != 1){
            chain.getLogger().error("CoinData data is greater than one");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        CoinTo toCoin = coinData.getTo().get(0);
        //from与to中账户必须一样且为节点创建者
        if (!Arrays.equals(address, toCoin.getAddress()) || toCoin.getAssetsChainId() != assetChainId || toCoin.getAssetsId() != assetId) {
            chain.getLogger().error("CoinData asset invalidation");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        for (CoinFrom coinFrom: coinData.getFrom()) {
            if(!Arrays.equals(address, coinFrom.getAddress())){
                chain.getLogger().error("CoinData from corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }
        //委托金额与CoinData中金额不一致
        if (!BigIntegerUtils.isEqual(deposit, toCoin.getAmount())) {
            chain.getLogger().error("CoinData is not equal to the entrusted amount");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        //CoinData锁定时间错误
        if (toCoin.getLockTime() != ConsensusConstant.CONSENSUS_LOCK_TIME) {
            chain.getLogger().error("CoinData lock time error");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * 注销节点和退出保证金交易CoinData验证
     * Verification of cancellation agent and exit margin transaction coinData
     *
     * @param amount       委托信息/deposit
     * @param coinData     交易的CoinData/CoinData
     * @param chain        链信息
     * @param address      账户
     * @param realLockTime 锁定时间
     * @return boolean
     */
    public Result reduceDepositCoinDataValid(Chain chain, BigInteger amount, CoinData coinData, byte[] address, long realLockTime){
        return reduceDepositCoinDataValid(chain, amount, coinData, address, realLockTime, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * 注销节点和退出保证金交易CoinData验证
     * Verification of cancellation agent and exit margin transaction coinData
     *
     * @param amount       委托信息/deposit
     * @param coinData     交易的CoinData/CoinData
     * @param chain        链信息
     * @param address      账户
     * @param realLockTime 锁定时间
     * @param assetChainId 退出资产链ID
     * @param assetId      退出资产ID
     * @return boolean
     */
    public Result reduceDepositCoinDataValid(Chain chain, BigInteger amount, CoinData coinData, byte[] address, long realLockTime,int assetChainId,int assetId){
        if (coinData.getFrom().size() == 0 || coinData.getTo().size() == 0) {
            chain.getLogger().error("CoinData from or to is null");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        BigInteger fromAmount = BigInteger.ZERO;
        //from与to中账户必须一样且为节点创建者,且to中资产必须与txData中资产一致
        for (CoinTo coinTo : coinData.getTo()){
            if(!Arrays.equals(address, coinTo.getAddress()) || coinTo.getAssetsChainId() != assetChainId || coinTo.getAssetsId() != assetId){
                chain.getLogger().error("CoinData to corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }
        for (CoinFrom coinFrom: coinData.getFrom()) {
            if(!Arrays.equals(address, coinFrom.getAddress())){
                chain.getLogger().error("CoinData from corresponding  to assets are different");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            if(coinFrom.getAssetsChainId() == assetChainId && coinFrom.getAssetsId() == assetId){
                fromAmount = fromAmount.add(coinFrom.getAmount());
            }
        }

        //验证锁定时间,除了退出保证金交易外其他解锁交易都只有一条CoinTo数据，退出保证金有可能会有两条一条是退出的保证金，另一条是把剩余的金额重新锁定
        int toSize = coinData.getTo().size();
        if(toSize == 1){
            if (amount.compareTo(fromAmount) != 0) {
                chain.getLogger().error("The amount in txData is inconsistent with the unlocked amount");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            long lockTime = coinData.getTo().get(0).getLockTime();
            if(realLockTime != lockTime){
                chain.getLogger().error("Lock time error! lockTime:{},realLockTime:{}",lockTime,realLockTime);
                return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_LOCK_TIME_ERROR);
            }
        }else if(toSize == 2){
            //验证CoinData中金额，from中的金额必须大于等于txData金额（退出保证金交易from中金额可能会大于txData中的金额，因为可能存在组装from时有退回重新锁定的情况）
            if (amount.compareTo(fromAmount) > 0) {
                chain.getLogger().error("Amount in coinData from is less than that in txData");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            //如果有两条CoinTo则一条锁定时间为realLockTime，另一条被重新永久锁定，锁定金额必须等于from金额 - 退出金额
            boolean unlockSuccess = false;
            boolean lockSuccess = false;
            BigInteger reLockAmount = fromAmount.subtract(amount);
            for (CoinTo to : coinData.getTo()){
                //解锁时间是否正确，解锁金额必须大于to中解锁的金额(因为to中金额会减手续费)
                if(realLockTime == to.getLockTime() && amount.compareTo(to.getAmount()) > 0){
                    unlockSuccess = true;
                    continue;
                }
                //重新锁定金额必须等于From中解锁的金额 - 解锁金额
                if(to.getLockTime() == -1 && reLockAmount.compareTo(to.getAmount()) == 0){
                    lockSuccess = true;
                }
            }
            if(!unlockSuccess || !lockSuccess){
                chain.getLogger().error("CoinTo validation failed");
                return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }else{
            chain.getLogger().error("Wrong number of coinTo");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        return ConsensusNetUtil.getSuccess();
    }

    /**
     * 交易手续费验证
     * @param chain    链信息
     * @param coinData coinData
     * @param tx       交易
     * */
    public Result validFee(Chain chain, CoinData coinData, Transaction tx) throws IOException{
        int size = tx.serialize().length;
        BigInteger fee = TransactionFeeCalculator.getConsensusTxFee(size, chain.getConfig().getFeeUnit());
        if (fee.compareTo(ConsensusManager.getFee(coinData, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId())) > 0) {
            chain.getLogger().error("Insufficient service charge");
            return Result.getFailed(ConsensusErrorCode.FEE_NOT_ENOUGH);
        }
        return ConsensusNetUtil.getSuccess();
    }
}
