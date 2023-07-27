package io.nuls.consensus.service.impl;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.*;
import io.nuls.consensus.model.dto.input.*;
import io.nuls.consensus.model.po.AgentPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.service.MultiSignService;
import io.nuls.consensus.utils.enumeration.DepositTimeType;
import io.nuls.consensus.utils.enumeration.DepositType;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.manager.CoinDataManager;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static io.nuls.consensus.constant.ParameterConstant.*;

/**
 * 多签账户相关交易接口实现类
 * Implementation Class of Multi-Sign Account Related Transaction Interface
 *
 * @author tag
 * 2019/07/25
 */
@Component
public class MultiSignServiceImpl implements MultiSignService {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private CoinDataManager coinDataManager;
    @Autowired
    private AgentManager agentManager;

    @Override
    @SuppressWarnings("unchecked")
    public Result createMultiAgent(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        CreateMultiAgentDTO dto = JSONUtils.map2pojo(params, CreateMultiAgentDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAgentAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }

            Transaction tx = new Transaction(TxType.REGISTER_AGENT);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            Agent agent = new Agent(dto);
            tx.setTxData(agent.serialize());

            int txSignSize = multiSigAccount.getM() * P2PHKSignature.SERIALIZE_LENGTH;
            CoinData coinData = coinDataManager.getCoinData(agent.getAgentAddress(), chain, new BigInteger(dto.getDeposit()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + txSignSize, chain.getConfig().getAgentChainId(), chain.getConfig().getAgentAssetId());
            tx.setCoinData(coinData.serialize());

            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(tx, multiSigAccount, priKey);

            String txStr = RPCUtil.encode(tx.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);

            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result stopMultiAgent(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        StopMultiAgentDTO dto = JSONUtils.map2pojo(params, StopMultiAgentDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }

            Transaction tx = new Transaction(TxType.STOP_AGENT);
            StopAgent stopAgent = new StopAgent();
            stopAgent.setAddress(AddressTool.getAddress(dto.getAddress()));
            List<Agent> agentList = chain.getAgentList();
            Agent agent = null;
            for (Agent a : agentList) {
                if (a.getDelHeight() > 0) {
                    continue;
                }
                if (Arrays.equals(a.getAgentAddress(), AddressTool.getAddress(dto.getAddress()))) {
                    agent = a;
                    break;
                }
            }
            if (agent == null || agent.getDelHeight() > 0) {
                return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
            }
            stopAgent.setCreateTxHash(agent.getTxHash());
            tx.setTxData(stopAgent.serialize());
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            int txSignSize = multiSigAccount.getM() * P2PHKSignature.SERIALIZE_LENGTH;
            CoinData coinData = coinDataManager.getStopAgentCoinData(chain, agent, NulsDateUtils.getCurrentTimeSeconds() + chain.getConfig().getStopAgentLockTime());
            BigInteger fee = TransactionFeeCalculator.getConsensusTxFee(tx.size() + txSignSize + coinData.serialize().length, chain.getConfig().getFeeUnit());
            coinData.getTo().get(0).setAmount(coinData.getTo().get(0).getAmount().subtract(fee));
            tx.setCoinData(coinData.serialize());

            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(tx, multiSigAccount, priKey);

            String txStr = RPCUtil.encode(tx.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);

            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result appendMultiAgentDeposit(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        ChangeMultiAgentDepositDTO dto = JSONUtils.map2pojo(params, ChangeMultiAgentDepositDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }
            Agent agent = agentManager.getValidAgentByAddress(chain, AddressTool.getAddress(dto.getAddress()));
            if (agent == null) {
                return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
            }
            NulsHash agentHash = agent.getTxHash();
            byte[] address = AddressTool.getAddress(dto.getAddress());
            //验证节点是否存在且交易发起者是否为节点创建者
            Result rs = agentManager.creatorValid(chain, agentHash, address);
            if (rs.isFailed()) {
                return rs;
            }
            Transaction tx = new Transaction(TxType.APPEND_AGENT_DEPOSIT);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            ChangeAgentDepositData txData = new ChangeAgentDepositData(address, BigIntegerUtils.stringToBigInteger(dto.getAmount()), agentHash);
            tx.setTxData(txData.serialize());
            CoinData coinData = coinDataManager.getCoinData(address, chain, new BigInteger(dto.getAmount()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + P2PHKSignature.SERIALIZE_LENGTH);
            tx.setCoinData(coinData.serialize());
            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(tx, multiSigAccount, priKey);

            String txStr = RPCUtil.encode(tx.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);

            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result reduceMultiAgentDeposit(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        ChangeMultiAgentDepositDTO dto = JSONUtils.map2pojo(params, ChangeMultiAgentDepositDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }
            Agent agent = agentManager.getValidAgentByAddress(chain, AddressTool.getAddress(dto.getAddress()));
            if (agent == null) {
                return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
            }
            NulsHash agentHash = agent.getTxHash();
            byte[] address = AddressTool.getAddress(dto.getAddress());
            //验证节点是否存在且交易发起者是否为节点创建者
            Result rs = agentManager.creatorValid(chain, agentHash, address);
            if (rs.isFailed()) {
                return rs;
            }
            AgentPo agentPo = (AgentPo) rs.getData();
            BigInteger amount = new BigInteger(dto.getAmount());
            //金额小于允许的最小金额
            BigInteger minReduceAmount = chain.getConfig().getReduceAgentDepositMin();

            if (chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
                minReduceAmount = chain.getConfig().getMinAppendAndExitAmount();
            }

            if (amount.compareTo(minReduceAmount) < 0) {
                chain.getLogger().error("The amount of exit margin is not within the allowed range");
                return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
            }
            BigInteger maxReduceAmount = agentPo.getDeposit().subtract(chain.getConfig().getDepositMin());
            //退出金额大于当前允许退出的最大金额
            if (amount.compareTo(maxReduceAmount) > 0) {
                chain.getLogger().error("Exit amount is greater than the current maximum amount allowed,deposit:{},maxReduceAmount:{},reduceAmount:{}", agentPo.getDeposit(), maxReduceAmount, amount);
                return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
            }
            Transaction tx = new Transaction(TxType.REDUCE_AGENT_DEPOSIT);
            long txTime = NulsDateUtils.getCurrentTimeSeconds();
            tx.setTime(txTime);
            ChangeAgentDepositData txData = new ChangeAgentDepositData(address, amount, agentHash);
            tx.setTxData(txData.serialize());
            CoinData coinData = coinDataManager.getReduceAgentDepositCoinData(address, chain, amount, txTime + chain.getConfig().getReducedDepositLockTime(), tx.size() + P2PHKSignature.SERIALIZE_LENGTH, agentHash);
            tx.setCoinData(coinData.serialize());
            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(tx, multiSigAccount, priKey);
            String txStr = RPCUtil.encode(tx.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);
            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result multiDeposit(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        CreateMultiDepositDTO dto = JSONUtils.map2pojo(params, CreateMultiDepositDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }
            //验证资产是否可以参与stacking
            if (null == chainManager.assetStackingVerify(dto.getAssetChainId(), dto.getAssetId())) {
                chain.getLogger().error("The current asset does not support stacking");
                return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
            }
            //如果为存定期则验证定期类型是否存在
            if (dto.getDepositType() == DepositType.REGULAR.getCode()) {
                DepositTimeType depositTimeType = DepositTimeType.getValue(dto.getTimeType());
                if (depositTimeType == null) {
                    chain.getLogger().error("Recurring delegation type does not exist");
                    return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
                }
            }
            Transaction tx = new Transaction(TxType.DEPOSIT);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            Deposit deposit = new Deposit(dto);
            tx.setTxData(deposit.serialize());
            int txSignSize = multiSigAccount.getM() * P2PHKSignature.SERIALIZE_LENGTH;
            CoinData coinData = coinDataManager.getCoinData(deposit.getAddress(), chain, new BigInteger(dto.getDeposit()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + txSignSize, dto.getAssetChainId(), dto.getAssetId());
            tx.setCoinData(coinData.serialize());
            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(tx, multiSigAccount, priKey);
            String txStr = RPCUtil.encode(tx.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);
            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result multiWithdraw(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        MultiWithdrawDTO dto = JSONUtils.map2pojo(params, MultiWithdrawDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MultiSigAccount multiSigAccount = CallMethodUtils.getMultiSignAccount(dto.getChainId(), dto.getAddress());
            HashMap callResult = null;
            if (StringUtils.isNotBlank(dto.getSignAddress()) && StringUtils.isNotBlank(dto.getPassword())) {
                callResult = CallMethodUtils.getPrivateKey(dto.getChainId(), dto.getSignAddress(), dto.getPassword());
            }

            NulsHash hash = NulsHash.fromHex(dto.getTxHash());
            Transaction depositTransaction = CallMethodUtils.getTransaction(chain, dto.getTxHash());
            if (depositTransaction == null) {
                return Result.getFailed(ConsensusErrorCode.TX_NOT_EXIST);
            }
            CoinData depositCoinData = new CoinData();
            depositCoinData.parse(depositTransaction.getCoinData(), 0);
            Deposit deposit = new Deposit();
            deposit.parse(depositTransaction.getTxData(), 0);
            byte[] address = AddressTool.getAddress(dto.getAddress());
            if (!Arrays.equals(deposit.getAddress(), address)) {
                chain.getLogger().error("The account is not the creator of the entrusted transaction");
                return Result.getFailed(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
            }
            boolean flag = false;
            for (CoinTo to : depositCoinData.getTo()) {
                if (to.getLockTime() == -1L && to.getAmount().compareTo(deposit.getDeposit()) == 0) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
            }
            long time = NulsDateUtils.getCurrentTimeSeconds();
            if (deposit.getDepositType() == DepositType.REGULAR.getCode()) {
                DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
                if (depositTimeType == null) {
                    chain.getLogger().error("Recurring delegation type does not exist");
                    return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
                }
                long periodicTime = depositTransaction.getTime() + depositTimeType.getTime();
                if (time < periodicTime) {
                    chain.getLogger().error("Term commission not due");
                    return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_DUE);
                }
            }
            Transaction cancelDepositTransaction = new Transaction(TxType.CANCEL_DEPOSIT);
            CancelDeposit cancelDeposit = new CancelDeposit();
            cancelDeposit.setAddress(address);
            cancelDeposit.setJoinTxHash(hash);
            cancelDepositTransaction.setTime(time);
            cancelDepositTransaction.setTxData(cancelDeposit.serialize());

            long lockTime = 0;
            if (chain.getChainId() == deposit.getAssetChainId() && chain.getAssetId() == deposit.getAssetId() && chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
                lockTime = cancelDepositTransaction.getTime() + chain.getConfig().getExitStakingLockHours() * 3600;
            }

            CoinData coinData = coinDataManager.getWithdrawCoinData(cancelDeposit.getAddress(), chain, deposit.getDeposit(), lockTime, cancelDepositTransaction.size() + P2PHKSignature.SERIALIZE_LENGTH, deposit.getAssetChainId(), deposit.getAssetId());
            coinData.getFrom().get(0).setNonce(CallMethodUtils.getNonce(hash.getBytes()));
            cancelDepositTransaction.setCoinData(coinData.serialize());

            String priKey = null;
            if (callResult != null && AddressTool.validSignAddress(multiSigAccount.getPubKeyList(), HexUtil.decode((String) callResult.get(PARAM_PUB_KEY)))) {
                priKey = (String) callResult.get(PARAM_PRI_KEY);
            }
            buildMultiSignTransactionSignature(cancelDepositTransaction, multiSigAccount, priKey);
            String txStr = RPCUtil.encode(cancelDepositTransaction.serialize());
            Map<String, Object> result = new HashMap<>(4);
            result.put(PARAM_TX_HASH, cancelDepositTransaction.getHash().toHex());
            result.put(PARAM_TX, txStr);
            result.put(PARAM_COMPLETED, false);
            if (callResult != null && multiSigAccount.getM() == 1) {
                CallMethodUtils.sendTx(chain, txStr);
                result.put(PARAM_COMPLETED, true);
            }
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    private void buildMultiSignTransactionSignature(Transaction transaction, MultiSigAccount multiSigAccount, String priKey) throws NulsException {
        MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
        transactionSignature.setM(multiSigAccount.getM());
        transactionSignature.setPubKeyList(multiSigAccount.getPubKeyList());
        try {
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            if (priKey != null && !priKey.isEmpty()) {
                P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByPriKey(transaction, priKey);
                p2PHKSignatures.add(p2PHKSignature);
            }
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            transaction.setTransactionSignature(transactionSignature.serialize());
        } catch (IOException e) {
            throw new NulsException(ConsensusErrorCode.SERIALIZE_ERROR);
        }
    }
}
