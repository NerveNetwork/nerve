package network.nerve.pocbft.service.impl;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.bo.tx.txdata.ChangeAgentDepositData;
import network.nerve.pocbft.model.bo.tx.txdata.StopAgent;
import network.nerve.pocbft.model.dto.output.AgentDTO;
import network.nerve.pocbft.model.dto.output.ReduceNonceDTO;
import network.nerve.pocbft.model.po.AgentPo;
import network.nerve.pocbft.model.po.VirtualAgentPo;
import network.nerve.pocbft.model.po.nonce.NonceDataPo;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.service.AgentService;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.enumeration.ConsensusStatus;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.basic.Page;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.storage.VirtualAgentStorageService;
import network.nerve.pocbft.model.dto.input.*;
import network.nerve.pocbft.utils.manager.*;
import network.nerve.pocbft.v1.utils.RoundUtils;

import static network.nerve.pocbft.constant.ParameterConstant.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * 共识模块RPC接口实现类
 * Consensus Module RPC Interface Implementation Class
 *
 * @author tag
 * 2018/11/7
 */
@Component
public class AgentServiceImpl implements AgentService {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private CoinDataManager coinDataManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private VirtualAgentStorageService virtualAgentStorageService;

    @Override
    @SuppressWarnings("unchecked")
    public Result createAgent(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        CreateAgentDTO dto = JSONUtils.map2pojo(params, CreateAgentDTO.class);
        try {
            ObjectUtils.canNotEmpty(dto);
            ObjectUtils.canNotEmpty(dto.getChainId(), "chainId can not be null");
            ObjectUtils.canNotEmpty(dto.getAgentAddress(), "agent address can not be null");
            ObjectUtils.canNotEmpty(dto.getDeposit(), "deposit can not be null");
            ObjectUtils.canNotEmpty(dto.getPackingAddress(), "packing address can not be null");
        } catch (RuntimeException e) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }

        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            //1.参数验证
            if (!AddressTool.isNormalAddress(dto.getPackingAddress(), dto.getChainId())) {
                throw new NulsRuntimeException(ConsensusErrorCode.ADDRESS_ERROR);
            }
            //2.账户验证
            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAgentAddress(), dto.getPassword());
            //3.组装创建节点交易
            Transaction tx = new Transaction(TxType.REGISTER_AGENT);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            //3.1.组装共识节点信息
            Agent agent = new Agent(dto);
            tx.setTxData(agent.serialize());
            //3.2.组装coinData
            CoinData coinData = coinDataManager.getCoinData(agent.getAgentAddress(), chain, new BigInteger(dto.getDeposit()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + P2PHKSignature.SERIALIZE_LENGTH);
            tx.setCoinData(coinData.serialize());
            //4.交易签名
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAgentAddress(), dto.getPassword(), priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            CallMethodUtils.sendTx(chain, txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result appendAgentDeposit(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        ChangeAgentDepositDTO dto = JSONUtils.map2pojo(params, ChangeAgentDepositDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        if (!AddressTool.isNormalAddress(dto.getAddress(), dto.getChainId())) {
            return Result.getFailed(ConsensusErrorCode.ADDRESS_ERROR);
        }
        try {
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
            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAddress(), dto.getPassword());
            Transaction tx = new Transaction(TxType.APPEND_AGENT_DEPOSIT);
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            ChangeAgentDepositData txData = new ChangeAgentDepositData(address, BigIntegerUtils.stringToBigInteger(dto.getAmount()), agentHash);
            tx.setTxData(txData.serialize());
            CoinData coinData = coinDataManager.getCoinData(address, chain, new BigInteger(dto.getAmount()), ConsensusConstant.CONSENSUS_LOCK_TIME, tx.size() + P2PHKSignature.SERIALIZE_LENGTH);
            tx.setCoinData(coinData.serialize());
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAddress(), dto.getPassword(), priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            CallMethodUtils.sendTx(chain, txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
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
    public Result reduceAgentDeposit(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        ChangeAgentDepositDTO dto = JSONUtils.map2pojo(params, ChangeAgentDepositDTO.class);
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            Log.error(ConsensusErrorCode.CHAIN_NOT_EXIST.getMsg());
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        if (!AddressTool.isNormalAddress(dto.getAddress(), dto.getChainId())) {
            return Result.getFailed(ConsensusErrorCode.ADDRESS_ERROR);
        }
        try {
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

            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAddress(), dto.getPassword());
            Transaction tx = new Transaction(TxType.REDUCE_AGENT_DEPOSIT);
            long txTime = NulsDateUtils.getCurrentTimeSeconds();
            tx.setTime(txTime);
            ChangeAgentDepositData txData = new ChangeAgentDepositData(address, amount, agentHash);
            tx.setTxData(txData.serialize());
            CoinData coinData = coinDataManager.getReduceAgentDepositCoinData(address, chain, amount, txTime + chain.getConfig().getReducedDepositLockTime(), tx.size() + P2PHKSignature.SERIALIZE_LENGTH, agentHash);
            tx.setCoinData(coinData.serialize());
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAddress(), dto.getPassword(), priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            CallMethodUtils.sendTx(chain, txStr);
            Map<String, Object> result = new HashMap<>(2);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
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
    public Result stopAgent(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        StopAgentDTO dto = JSONUtils.map2pojo(params, StopAgentDTO.class);
        try {
            ObjectUtils.canNotEmpty(dto);
            ObjectUtils.canNotEmpty(dto.getChainId(), "chainId can not be null");
            ObjectUtils.canNotEmpty(dto.getAddress(), "address can not be null");
        } catch (RuntimeException e) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        if (!AddressTool.validAddress(dto.getChainId(), dto.getAddress())) {
            throw new NulsRuntimeException(ConsensusErrorCode.ADDRESS_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(dto.getChainId());
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            HashMap callResult = CallMethodUtils.accountValid(dto.getChainId(), dto.getAddress(), dto.getPassword());
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
            Transaction tx = new Transaction(TxType.STOP_AGENT);
            StopAgent stopAgent = new StopAgent();
            stopAgent.setAddress(AddressTool.getAddress(dto.getAddress()));
            stopAgent.setCreateTxHash(agent.getTxHash());
            tx.setTxData(stopAgent.serialize());
            long txTime = NulsDateUtils.getCurrentTimeSeconds();
            tx.setTime(txTime);
            CoinData coinData = coinDataManager.getStopAgentCoinData(chain, agent, txTime + chain.getConfig().getStopAgentLockTime());
            BigInteger fee = TransactionFeeCalculator.getConsensusTxFee(tx.size() + P2PHKSignature.SERIALIZE_LENGTH + coinData.serialize().length, chain.getConfig().getFeeUnit());
            coinData.getTo().get(0).setAmount(coinData.getTo().get(0).getAmount().subtract(fee));
            tx.setCoinData(coinData.serialize());
            //交易签名
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            CallMethodUtils.transactionSignature(dto.getChainId(), dto.getAddress(), dto.getPassword(), priKey, tx);
            String txStr = RPCUtil.encode(tx.serialize());
            CallMethodUtils.sendTx(chain, txStr);
            Map<String, Object> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
            result.put(PARAM_TX_HASH, tx.getHash().toHex());
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
    public Result getAgentList(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        SearchAllAgentDTO dto = JSONUtils.map2pojo(params, SearchAllAgentDTO.class);
        int pageNumber = dto.getPageNumber();
        int pageSize = dto.getPageSize();
        int chainId = dto.getChainId();
        if (pageNumber == MIN_VALUE) {
            pageNumber = PAGE_NUMBER_INIT_VALUE;
        }
        if (pageSize == MIN_VALUE) {
            pageSize = PAGE_SIZE_INIT_VALUE;
        }
        if (pageNumber < MIN_VALUE || pageSize < MIN_VALUE || pageSize > PAGE_SIZE_MAX_VALUE || chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        List<Agent> agentList = chain.getAgentList();
        List<Agent> handleList = new ArrayList<>();
        String keyword = dto.getKeyWord();
        long startBlockHeight = chain.getBestHeader().getHeight();
        for (Agent agent : agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L) {
                continue;
            }
            if (StringUtils.isNotBlank(keyword)) {
                keyword = keyword.toUpperCase();
                String agentAddress = AddressTool.getStringAddressByBytes(agent.getAgentAddress()).toUpperCase();
                String packingAddress = AddressTool.getStringAddressByBytes(agent.getPackingAddress()).toUpperCase();
                String agentId = agentManager.getAgentId(agent.getTxHash()).toUpperCase();
                //从账户模块获取账户别名
                String agentAlias = CallMethodUtils.getAlias(chain, agentAddress);
                String packingAlias = CallMethodUtils.getAlias(chain, packingAddress);
                boolean b = agentId.contains(keyword);
                b = b || agentAddress.equals(keyword) || packingAddress.equals(keyword);
                if (StringUtils.isNotBlank(agentAlias)) {
                    b = b || agentAlias.toUpperCase().contains(keyword);
                    agent.setAlias(agentAlias);
                }
                if (!b && StringUtils.isNotBlank(packingAlias)) {
                    b = agentAlias.toUpperCase().contains(keyword);
                }
                if (!b) {
                    continue;
                }
            }
            handleList.add(agent);
        }
        int start = pageNumber * pageSize - pageSize;
        Page<AgentDTO> page = new Page<>(pageNumber, pageSize, handleList.size());
        //表示查询的起始位置大于数据总数即查询的该页不存在数据
        if (start >= page.getTotal()) {
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(page);
        }
        agentManager.fillAgentList(chain, handleList);
        List<AgentDTO> resultList = new ArrayList<>();
        for (int i = start; i < handleList.size() && i < (start + pageSize); i++) {
            AgentDTO agentDTO = new AgentDTO(handleList.get(i));
            resultList.add(agentDTO);
        }
        page.setList(resultList);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getAgentBasicList(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        boolean isNewest = true;
        long height = 0;
        if (params.get(PARAM_HEIGHT) != null) {
            isNewest = false;
            height = (Integer) params.get(PARAM_HEIGHT);
        }
        Map<String, Object> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
        result.put(PARAM_LIST, agentManager.getPackBasicAgentList(chain, height, isNewest));
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getAgentInfo(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        SearchAgentDTO dto = JSONUtils.map2pojo(params, SearchAgentDTO.class);
        String agentHash = dto.getAgentHash();
        if (!NulsHash.validHash(agentHash)) {
            return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
        }
        int chainId = dto.getChainId();
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        NulsHash agentHashData = NulsHash.fromHex(agentHash);
        List<Agent> agentList = chain.getAgentList();
        for (Agent agent : agentList) {
            if (agent.getTxHash().equals(agentHashData)) {
                MeetingRound round = RoundUtils.getRoundController().getCurrentRound();
                if (agent.getDelHeight() == -1) {
                    agentManager.fillAgent(agent, round);
                } else {
                    agent.setStatus(0);
                    agent.setCreditVal(0);
                }
                AgentDTO result = new AgentDTO(agent);
                return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
            }
        }
        return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
    }

    /**
     * 获取指定节点状态
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getAgentStatus(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        SearchAgentDTO dto = JSONUtils.map2pojo(params, SearchAgentDTO.class);
        int chainId = dto.getChainId();
        if (dto.getChainId() <= MIN_VALUE || dto.getAgentHash() == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        Map<String, Integer> result = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
        try {
            Agent agent = agentManager.getAgentByHash(chain, NulsHash.fromHex(dto.getAgentHash()));
            if (agent.getDelHeight() > MIN_VALUE) {
                result.put(PARAM_STATUS, 0);
            } else {
                result.put(PARAM_STATUS, 1);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(result);
    }

    @Override
    public Result updateAgentConsensusStatus(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        chain.setConsensusStatus(ConsensusStatus.RUNNING);
        chain.getLogger().info("updateAgentConsensusStatus-修改节点共识状态成功......");
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);
    }

    @Override
    public Result updateAgentStatus(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_STATUS) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        int status = (Integer) params.get(PARAM_STATUS);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        chain.getLogger().info("节点共识状态变更，修改后的状态为，status：{}", status);
        if (status == 1) {
            chain.setSynchronizedHeight(true);
        } else {
            //共识状态变更为不可打包状态，清理当前投票信息，断开共识网络
            chain.setSynchronizedHeight(false);
        }
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);

    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getNodePackingAddress(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CommandConstant.CALL_AC_GET_UNENCRYPTED_ADDRESS_LIST, params);
            List<String> accountAddressList = (List<String>) ((HashMap) ((HashMap) cmdResp.getResponseData()).get(CommandConstant.CALL_AC_GET_UNENCRYPTED_ADDRESS_LIST)).get("list");
            Set<String> packAddressList = agentManager.getPackAddressList(chain, chain.getBestHeader().getHeight());
            String packAddress = null;
            for (String address : packAddressList) {
                if (accountAddressList.contains(address)) {
                    packAddress = address;
                    break;
                }
            }
            Map<String, Object> resultMap = new HashMap<>(2);
            resultMap.put(PARAM_PACK_ADDRESS, packAddress);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
        }
    }

    /**
     * 获取所有节点出块地址/指定N个区块出块指定
     *
     * @param params
     * @return Result
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getAgentAddressList(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put(PARAM_PACK_ADDRESS, agentManager.getPackAddressList(chain, chain.getBestHeader().getHeight()));
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }

    /**
     * 获取当前节点的出块账户信息
     *
     * @param params
     * @return Result
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getPackerInfo(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        try {
            MeetingRound round = RoundUtils.getCurrentRound();
            MeetingMember member = null;
            if (round == null && chain.getBestHeader() != null && chain.getBestHeader().getHeight() != 0) {
                chain.getLogger().info("初始化轮次");
                round = RoundUtils.getRoundController().tempRound();
            }
            if (round != null) {
                member = round.getLocalMember();
            }
            Map<String, Object> resultMap = new HashMap<>(4);
            if (member != null) {
                resultMap.put(PARAM_ADDRESS, AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
                resultMap.put(PARAM_PASSWORD, chain.getConfig().getPassword());
            }
            List<String> packAddressList;
            if (round != null) {
                packAddressList = new ArrayList<>(round.getMemberAddressSet());
            } else {
                packAddressList = chain.getSeedAddressList();
            }
            resultMap.put(PARAM_PACKING_ADDRESS_LIST, packAddressList);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getAgentChangeInfo(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_CURRENT_ROUND) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        BlockExtendsData lastExtendsData = null;
        String lastRoundStr = (String) params.get(PARAM_LAST_ROUND);
        if (lastRoundStr != null) {
            lastExtendsData = new BlockExtendsData(RPCUtil.decode(lastRoundStr));
        }
        String currentRoundStr = (String) params.get(PARAM_CURRENT_ROUND);
        BlockExtendsData currentExtendsData = new BlockExtendsData(RPCUtil.decode(currentRoundStr));
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(RoundUtils.getAgentChangeInfo(chain, lastExtendsData, currentExtendsData));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getReduceDepositNonceList(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_QUIT_ALL) == null || params.get(PARAM_AGENT_HASH) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        int quitAll = (int) params.get(PARAM_QUIT_ALL);
        String agentHash = (String) params.get(PARAM_AGENT_HASH);
        BigInteger depositAmount = BigInteger.ZERO;
        boolean stopAgent = quitAll == 1;
        if (!stopAgent) {
            depositAmount = new BigInteger((String) params.get(PARAM_REDUCE_AMOUNT));
        }
        Map<String, Object> resultMap = new HashMap<>(4);
        List<NonceDataPo> nonceDataList = AgentDepositNonceManager.getNonceDataList(chain, depositAmount, new NulsHash(HexUtil.decode(agentHash)), stopAgent);
        List<ReduceNonceDTO> nonceDTOList = new ArrayList<>();
        for (NonceDataPo nonceDataPo : nonceDataList) {
            nonceDTOList.add(new ReduceNonceDTO(nonceDataPo));
        }
        resultMap.put(PARAM_LIST, nonceDTOList);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result virtualAgentChange(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_HEIGHT) == null || params.get(PARAM_VIRTUAL_AGENT_LIST) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        if (chainId <= MIN_VALUE) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        long height = Long.valueOf(params.get(PARAM_HEIGHT).toString());
        List<String> virtualAgentList = (List<String>) params.get(PARAM_VIRTUAL_AGENT_LIST);
        virtualAgentStorageService.save(new VirtualAgentPo(height, virtualAgentList), height);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);
    }
}
