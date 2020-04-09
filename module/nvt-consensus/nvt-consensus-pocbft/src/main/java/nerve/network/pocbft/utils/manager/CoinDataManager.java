package nerve.network.pocbft.utils.manager;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.BigIntegerUtils;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.constant.ParameterConstant;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.po.nonce.NonceDataPo;
import nerve.network.pocbft.rpc.call.CallMethodUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CoinData操作工具类
 * CoinData operation tool class
 *
 * @author: Jason
 * 2018//11/28
 */
@Component
public class CoinDataManager {

    /**
     * 组装CoinData
     * Assemble CoinData
     *
     * @param address  账户地址/Account address
     * @param chain    chain info
     * @param amount   金额/amount
     * @param lockTime 锁定时间/lock time
     * @param txSize   交易大小/transaction size
     * @return 组装的CoinData/Assembled CoinData
     */
    public CoinData getCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize) throws NulsException {
        return getCoinData(address, chain, amount, lockTime, txSize, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
    }

    /**
     * 组装CoinData
     * Assemble CoinData
     *
     * @param address      账户地址/Account address
     * @param chain        chain info
     * @param amount       金额/amount
     * @param lockTime     锁定时间/lock time
     * @param txSize       交易大小/transaction size
     * @param assetChainId 抵押资产所属ChainId
     * @param assetId      抵押资产ID
     * @return 组装的CoinData/Assembled CoinData
     */
    public CoinData getCoinData(byte[] address, Chain chain, BigInteger amount, long lockTime, int txSize, int assetChainId, int assetId) throws NulsException {
        CoinData coinData = new CoinData();
        CoinTo to = new CoinTo(address, assetChainId, assetId, amount, lockTime);
        coinData.addTo(to);
        txSize += to.size();
        //抵押资产金额
        Map<String, Object> result = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), assetChainId, assetId);
        if (result == null) {
            throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
        }
        byte[] nonce = RPCUtil.decode((String) result.get(ParameterConstant.PARAM_NONCE));
        BigInteger available = new BigInteger(result.get(ParameterConstant.PARAM_AVAILABLE).toString());
        //验证账户余额是否足够
        CoinFrom from = new CoinFrom(address, assetChainId, assetId, amount, nonce, (byte) 0);
        txSize += from.size();
        BigInteger fee;
        long feeUnit = chain.getConfig().getFeeUnit();
        int feeAssetChainId = chain.getConfig().getAgentChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (assetChainId == feeAssetChainId && assetId == feeAssetId) {
            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, feeUnit);
            BigInteger fromAmount = amount.add(fee);
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

            //计算手续费
            Map<String, Object> feeResult = CallMethodUtils.getBalanceAndNonce(chain, AddressTool.getStringAddressByBytes(address), feeAssetChainId, feeAssetId);
            if (feeResult == null) {
                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
            }
            byte[] feeNonce = RPCUtil.decode((String) feeResult.get(ParameterConstant.PARAM_NONCE));
            CoinFrom feeFrom = new CoinFrom(address, assetChainId, assetId, BigInteger.valueOf(feeUnit), feeNonce, (byte) 0);
            txSize += feeFrom.size();
            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, chain.getConfig().getFeeUnit());
            BigInteger feeAvailable = new BigInteger(feeResult.get(ParameterConstant.PARAM_AVAILABLE).toString());
            if (BigIntegerUtils.isLessThan(feeAvailable, fee)) {
                throw new NulsException(ConsensusErrorCode.FEE_NOT_ENOUGH);
            }
            feeFrom.setAmount(fee);
            coinData.addFrom(feeFrom);
        }
        return coinData;
    }

    /**
     * 组装解锁金额的CoinData（from中nonce为空）
     * Assemble Coin Data for the amount of unlock (from non CE is empty)
     *
     * @param address   账户地址/Account address
     * @param chain     chain info
     * @param amount    金额/amount
     * @param lockTime  锁定时间/lock time
     * @param txSize    交易大小/transaction size
     * @param agentHash 节点HASH/agent hash
     * @return 组装的CoinData/Assembled CoinData
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
     * 组装解锁金额的CoinData（from中nonce为空）
     * Assemble Coin Data for the amount of unlock (from non CE is empty)
     *
     * @param address      账户地址/Account address
     * @param chain        chain info
     * @param amount       金额/amount
     * @param lockTime     锁定时间/lock time
     * @param txSize       交易大小/transaction size
     * @param assetChainId 解锁的资产链ID
     * @param assetId      解锁的资产ID
     * @return 组装的CoinData/Assembled CoinData
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
                throw new NulsException(ConsensusErrorCode.BANANCE_NOT_ENNOUGH);
            }
            byte[] feeNonce = RPCUtil.decode((String) feeResult.get(ParameterConstant.PARAM_NONCE));
            CoinFrom feeFrom = new CoinFrom(address, feeAssetChainId, feeAssetId, BigInteger.valueOf(feeUnit), feeNonce, (byte) 0);
            txSize += feeFrom.size();
            fee = TransactionFeeCalculator.getConsensusTxFee(txSize, feeUnit);
            BigInteger feeAvailable = new BigInteger(feeResult.get(ParameterConstant.PARAM_AVAILABLE).toString());
            if (BigIntegerUtils.isLessThan(feeAvailable, fee)) {
                throw new NulsException(ConsensusErrorCode.FEE_NOT_ENOUGH);
            }
            feeFrom.setAmount(fee);
            coinData.addFrom(feeFrom);
        }
        return coinData;
    }

    /**
     * 根据节点地址组装停止节点的coinData
     * Assemble coinData of stop node according to node address
     *
     * @param chain    chain info
     * @param address  agent address/节点地址
     * @param lockTime The end point of the lock (lock start time + lock time) is the length of the lock before./锁定的结束时间点(锁定开始时间点+锁定时长)，之前为锁定的时长
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
     * 组装节点CoinData锁定类型为时间或区块高度
     * Assembly node CoinData lock type is time or block height
     *
     * @param chain    chain info
     * @param agent    agent info/节点
     * @param lockTime lock time/锁定时间
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
