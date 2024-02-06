package io.nuls.consensus.utils.manager;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.po.nonce.NonceDataPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.constant.ParameterConstant;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CoinDataOperational tools
 * CoinData operation tool class
 *
 * @author tag
 * 2018//11/28
 */
@Component
public class CoinDataManager {

    /**
     * assembleCoinData
     * Assemble CoinData
     *
     * @param address  Account address/Account address
     * @param chain    chain info
     * @param amount   money/amount
     * @param lockTime Lock time/lock time
     * @param txSize   Transaction size/transaction size
     * @return AssembledCoinData/Assembled CoinData
     */
    public CoinData getCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize) throws NulsException {
        return getCoinData(address, chain, amount, lockTime, txSize, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * assembleCoinData
     * Assemble CoinData
     *
     * @param address      Account address/Account address
     * @param chain        chain info
     * @param amount       money/amount
     * @param lockTime     Lock time/lock time
     * @param txSize       Transaction size/transaction size
     * @param assetChainId Mortgage asset ownershipChainId
     * @param assetId      Mortgage assetsID
     * @return AssembledCoinData/Assembled CoinData
     */
    public CoinData getCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize, int assetChainId, int assetId) throws NulsException {
        CoinData coinData = new CoinData();
        CoinTo to = new CoinTo(address, assetChainId, assetId, amount, lockTime);
        coinData.addTo(to);
        txSize += to.size();
        //Mortgage asset amount
        Map<String, Object> result = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), assetChainId, assetId);
        if (result == null) {
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        byte[] nonce = RPCUtil.decode((String) result.get(ParameterConstant.PARAM_NONCE));
        BigInteger available = new BigInteger(result.get(ParameterConstant.PARAM_AVAILABLE).toString());
        //Verify if the account balance is sufficient
        CoinFrom from = new CoinFrom(address, assetChainId, assetId, amount, nonce, (byte) 0);
        txSize += from.size();
//        BigInteger fee;
        long feeUnit = chain.getConfig().getFeeUnit();
        int feeAssetChainId = chain.getConfig().getAgentChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (assetChainId == feeAssetChainId && assetId == feeAssetId) {
//            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, feeUnit);
            BigInteger fromAmount = amount;//.add(fee);
            if (BigIntegerUtils.isLessThan(available, fromAmount)) {
                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
            }
            from.setAmount(fromAmount);
            coinData.addFrom(from);
        } else {
            if (BigIntegerUtils.isLessThan(available, amount)) {
                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
            }
            coinData.addFrom(from);

            //Calculate handling fees
//            Map<String, Object> feeResult = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), feeAssetChainId, feeAssetId);
//            if (feeResult == null) {
//                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
//            }
//            byte[] feeNonce = RPCUtil.decode((String) feeResult.get(ParameterConstant.PARAM_NONCE));
//            CoinFrom feeFrom = new CoinFrom(address, feeAssetChainId, feeAssetId, BigInteger.valueOf(feeUnit), feeNonce, (byte) 0);
//            txSize += feeFrom.size();
//            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, chain.getConfig().getFeeUnit());
//            BigInteger feeAvailable = new BigInteger(feeResult.get(ParameterConstant.PARAM_AVAILABLE).toString());
//            if (BigIntegerUtils.isLessThan(feeAvailable, fee)) {
//                throw new NulsException(ConsensusErrorCode.FEE_NOT_ENOUGH);
//            }
//            feeFrom.setAmount(fee);
//            coinData.addFrom(feeFrom);
        }
        return coinData;
    }

    /**
     * Assembly unlocking amountCoinData（frominnonceEmpty）
     * Assemble Coin Data for the amount of unlock (from non CE is empty)
     *
     * @param address   Account address/Account address
     * @param chain     chain info
     * @param amount    money/amount
     * @param lockTime  Lock time/lock time
     * @param txSize    Transaction size/transaction size
     * @param agentHash nodeHASH/agent hash
     * @return AssembledCoinData/Assembled CoinData
     */
    public CoinData getReduceAgentDepositCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize, NulsHash agentHash) throws NulsException {
        int assetChainId = chain.getConfig().getAgentChainId();
        int assetId = chain.getConfig().getAssetId();
        Map<String, Object> balanceMap = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), assetChainId, assetId);
        if (balanceMap == null || balanceMap.isEmpty()) {
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        BigInteger freeze = new BigInteger(balanceMap.get(ParameterConstant.PARAM_PERMANENT_LOCKED).toString());
        if (BigIntegerUtils.isLessThan(freeze, amount)) {
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        List<NonceDataPo> nonceDataList = AgentDepositNonceManager.getNonceDataList(chain, amount, agentHash, false);
        CoinData coinData = new CoinData();
        List<CoinFrom> fromList = new ArrayList<>();
        List<CoinTo> toList = new ArrayList<>();
        BigInteger totalAmount = BigInteger.ZERO;
        for (NonceDataPo po : nonceDataList) {
            CoinFrom from = new CoinFrom(address, assetChainId, assetId, po.getDeposit(), po.getNonce(), (byte) -1);
            totalAmount = totalAmount.add(po.getDeposit());
            fromList.add(from);
        }
        toList.add(new CoinTo(address, assetChainId, assetId, amount, lockTime));
        if (totalAmount.compareTo(amount) > 0) {
            BigInteger reLockAmount = totalAmount.subtract(amount);
            toList.add(new CoinTo(address, assetChainId, assetId, reLockAmount, ConsensusConstant.CONSENSUS_LOCK_TIME));
        }
        coinData.setFrom(fromList);
        coinData.setTo(toList);
        txSize += coinData.size();
        BigInteger fee = TransactionFeeCalculator.getConsensusTxFee(txSize, chain.getConfig().getFeeUnit());
        BigInteger realAmount = toList.get(0).getAmount().subtract(fee);
        toList.get(0).setAmount(realAmount);
        return coinData;
    }

    /**
     * Assembly unlocking amountCoinData（frominnonceEmpty）
     * Assemble Coin Data for the amount of unlock (from non CE is empty)
     *
     * @param address      Account address/Account address
     * @param chain        chain info
     * @param amount       money/amount
     * @param lockTime     Lock time/lock time
     * @param txSize       Transaction size/transaction size
     * @param assetChainId Unlocked asset chainID
     * @param assetId      Unlocked assetsID
     * @return AssembledCoinData/Assembled CoinData
     */
    public CoinData getWithdrawCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize, int assetChainId, int assetId) throws NulsException {
        int feeAssetChainId = chain.getConfig().getAgentChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        Map<String, Object> balanceMap = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), assetChainId, assetId);
        if (balanceMap == null || balanceMap.isEmpty()) {
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        BigInteger freeze = new BigInteger(balanceMap.get(ParameterConstant.PARAM_PERMANENT_LOCKED).toString());
        if (BigIntegerUtils.isLessThan(freeze, amount)) {
            chain.getLogger().error("Lock amount less than exit amount,freeze:{},amount:{}",freeze,amount);
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        CoinData coinData = new CoinData();
        CoinTo to = new CoinTo(address, assetChainId, assetId, amount, lockTime);
        coinData.addTo(to);
        txSize += to.size();
        CoinFrom from = new CoinFrom(address, assetChainId, assetId, amount, (byte) -1);
        coinData.addFrom(from);
        txSize += from.size();
        long feeUnit = chain.getConfig().getFeeUnit();
        BigInteger fee;
        if (assetChainId == feeAssetChainId && assetId == feeAssetId) {
            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, feeUnit);
            BigInteger realToAmount = amount.subtract(fee);
            to.setAmount(realToAmount);
        } else {
            Map<String, Object> feeResult = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), feeAssetChainId, feeAssetId);
            if (feeResult == null) {
                chain.getLogger().error("There is no service charge for main assets of the chain");
                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
            }
            byte[] feeNonce = RPCUtil.decode((String) feeResult.get(ParameterConstant.PARAM_NONCE));
            CoinFrom feeFrom = new CoinFrom(address, feeAssetChainId, feeAssetId, BigInteger.valueOf(feeUnit), feeNonce, (byte) 0);
            txSize += feeFrom.size();
            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, feeUnit);
            BigInteger feeAvailable = new BigInteger(feeResult.get(ParameterConstant.PARAM_AVAILABLE).toString());
            if (BigIntegerUtils.isLessThan(feeAvailable, fee)) {
                chain.getLogger().error("Main assets of the chain are not enough to pay handling charges,feeAvailable:{},fee:{}",feeAvailable,fee);
                throw new NulsException(ConsensusErrorCode.FEE_NOT_ENOUGH);
            }
            feeFrom.setAmount(fee);
            coinData.addFrom(feeFrom);
        }
        return coinData;
    }

    /**
     * Assemble stop nodes based on their addressescoinData
     * Assemble coinData of stop node according to node address
     *
     * @param chain    chain info
     * @param address  agent address/Node address
     * @param lockTime The end point of the lock (lock start time + lock time) is the length of the lock before./Locked end time point(Lock start time point+Lock duration), previously locked for a certain duration
     * @return CoinData
     */
    public CoinData getStopAgentCoinData(Chain chain, byte[] address, long lockTime) throws NulsException {
        List<Agent> agentList = chain.getAgentList();
        for (Agent agent : agentList) {
            if (agent.getDelHeight() > 0) {
                continue;
            }
            if (Arrays.equals(address, agent.getAgentAddress())) {
                return getStopAgentCoinData(chain, agent, lockTime);
            }
        }
        return null;
    }

    /**
     * Assembly nodesCoinDataLock type is time or block height
     * Assembly node CoinData lock type is time or block height
     *
     * @param chain    chain info
     * @param agent    agent info/node
     * @param lockTime lock time/Lock time
     * @return CoinData
     */
    public CoinData getStopAgentCoinData(Chain chain, Agent agent, long lockTime) throws NulsException {
        if (null == agent) {
            return null;
        }
        int agentChainId = chain.getConfig().getAgentChainId();
        int agentAssetId = chain.getConfig().getAgentAssetId();
        NulsHash createTxHash = agent.getTxHash();
        Transaction createAgentTransaction = CallMethodUtils.getTransaction(chain, createTxHash.toHex());
        if (null == createAgentTransaction) {
            chain.getLogger().error("The creation node transaction corresponding to the exit node does not exist");
            throw new NulsRuntimeException(ConsensusErrorCode.TX_NOT_EXIST);
        }
        CoinData coinData = new CoinData();
        List<CoinTo> toList = new ArrayList<>();
        List<CoinFrom> fromList = new ArrayList<>();
        toList.add(new CoinTo(agent.getAgentAddress(), agentChainId, agentAssetId, agent.getDeposit(), lockTime));
        List<NonceDataPo> nonceDataList = AgentDepositNonceManager.getNonceDataList(chain, agent.getDeposit(), createTxHash, true);
        BigInteger totalAmount = BigInteger.ZERO;
        for (NonceDataPo po : nonceDataList) {
            CoinFrom from = new CoinFrom(agent.getAgentAddress(), agentChainId, agentAssetId, po.getDeposit(), po.getNonce(), (byte) -1);
            totalAmount = totalAmount.add(po.getDeposit());
            fromList.add(from);
        }
        if(!totalAmount.equals(agent.getDeposit())){
            chain.getLogger().error("Error unlocking transaction data");
            throw new NulsException(ConsensusErrorCode.AGENT_DEPOSIT_DATA_ERROR);
        }
        if (fromList.isEmpty()) {
            throw new NulsRuntimeException(ConsensusErrorCode.DATA_ERROR);
        }
        coinData.setFrom(fromList);
        coinData.setTo(toList);
        return coinData;
    }
}
