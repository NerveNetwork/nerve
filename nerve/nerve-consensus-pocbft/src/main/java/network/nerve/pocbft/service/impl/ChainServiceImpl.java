package network.nerve.pocbft.service.impl;

import io.nuls.core.model.DoubleUtils;
import io.nuls.economic.nuls.constant.NulsEconomicConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.config.ChainConfig;
import network.nerve.pocbft.model.bo.config.ConsensusChainConfig;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.bo.tx.txdata.Deposit;
import network.nerve.pocbft.model.dto.input.SearchPunishDTO;
import network.nerve.pocbft.model.po.PunishLogPo;
import network.nerve.pocbft.service.ChainService;
import network.nerve.pocbft.utils.enumeration.DepositTimeType;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.dto.output.*;
import network.nerve.pocbft.utils.manager.*;

import static network.nerve.pocbft.constant.ParameterConstant.*;

import java.io.IOException;
import java.math.BigDecimal;
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
public class ChainServiceImpl implements ChainService {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private RoundManager roundManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private ConsensusChainConfig consensusConfig;

    @Override
    @SuppressWarnings("unchecked")
    public Result isConsensusAgent(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_ADDRESS) == null) {
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
            MeetingRound round;
            if(params.get(PARAM_BLOCK_HEADER) == null){
                round = roundManager.getCurrentRound(chain);
            }else{
                BlockHeader header = new BlockHeader();
                header.parse(RPCUtil.decode((String) params.get(PARAM_BLOCK_HEADER)), 0);
                round = roundManager.getRound(chain, header.getExtendsData().getRoundIndex(), header.getTime());
            }
            String address = (String) params.get(PARAM_ADDRESS);
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(PARAM_RESULT_VALUE, round.getOnlyMember(AddressTool.getAddress(address)) != null);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
        }catch (NulsException e){
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    /**
     * 区块分叉记录
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result addEvidenceRecord(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_BLOCK_HEADER) == null || params.get(PARAM_EVIDENCE_HEADER) == null) {
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
            BlockHeader header = new BlockHeader();
            header.parse(RPCUtil.decode((String) params.get(PARAM_BLOCK_HEADER)), 0);
            BlockHeader evidenceHeader = new BlockHeader();
            evidenceHeader.parse(RPCUtil.decode((String) params.get(PARAM_EVIDENCE_HEADER)), 0);
            chain.getLogger().info("Received new bifurcation evidence:" + header.getHeight());
            punishManager.addEvidenceRecord(chain, header, evidenceHeader);
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(PARAM_RESULT_VALUE, true);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    /**
     * 双花交易记录
     *
     * @param params
     * @return Result
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result doubleSpendRecord(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_BLOCK) == null || params.get(PARAM_TX) == null) {
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
            Block block = new Block();
            block.parse(RPCUtil.decode((String) params.get(PARAM_BLOCK)), 0);
            List<String> txHexList = JSONUtils.json2list((String) params.get(PARAM_TX), String.class);
            List<Transaction> txList = new ArrayList<>();
            for (String txHex : txHexList) {
                Transaction tx = new Transaction();
                tx.parse(RPCUtil.decode(txHex), 0);
                txList.add(tx);
            }
            punishManager.addDoubleSpendRecord(chain, txList, block);
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(PARAM_RESULT_VALUE, true);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(validResult);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            chain.getLogger().error(e);
            return Result.getFailed(ConsensusErrorCode.DATA_PARSE_ERROR);
        }
    }

    /**
     * 获取全网信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getWholeInfo(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        WholeNetConsensusInfoDTO dto = new WholeNetConsensusInfoDTO();
        List<Agent> agentList = chain.getAgentList();
        if (agentList == null) {
            return Result.getFailed(ConsensusErrorCode.DATA_NOT_EXIST);
        }
        List<Agent> handleList = new ArrayList<>();
        //获取本地最新高度
        long startBlockHeight = chain.getNewestHeader().getHeight();
        for (Agent agent : agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            } else if (agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L) {
                continue;
            }
            handleList.add(agent);
        }
        MeetingRound round = roundManager.getCurrentRound(chain);
        BigInteger totalDeposit = BigInteger.ZERO;
        int packingAgentCount = 0;
        if (null != round) {
            for (MeetingMember member : round.getMemberList()) {
                //totalDeposit = totalDeposit.add(member.getAgent().getDeposit().add(member.getAgent().getTotalDeposit()));
                if (member.getAgent() != null) {
                    packingAgentCount++;
                }
            }
        }
        dto.setAgentCount(handleList.size());
        dto.setTotalDeposit(String.valueOf(totalDeposit));
        dto.setConsensusAccountNumber(handleList.size());
        dto.setPackingAgentCount(packingAgentCount);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(dto);
    }

    /**
     * 获取指定账户信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getInfo(Map<String, Object> params) {
        if (params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_ADDRESS) == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        int chainId = (Integer) params.get(PARAM_CHAIN_ID);
        String address = (String) params.get(PARAM_ADDRESS);
        AccountConsensusInfoDTO dto = new AccountConsensusInfoDTO();
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        long startBlockHeight = chain.getNewestHeader().getHeight();
        int agentCount = 0;
        String agentHash = null;
        byte[] addressBytes = AddressTool.getAddress(address);
        List<Agent> agentList = chain.getAgentList();
        for (Agent agent : agentList) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= startBlockHeight) {
                continue;
            } else if (agent.getBlockHeight() > startBlockHeight || agent.getBlockHeight() < 0L) {
                continue;
            }
            if (Arrays.equals(agent.getAgentAddress(), addressBytes)) {
                //一个账户最多只能创建一个共识节点
                agentCount = 1;
                agentHash = agent.getTxHash().toHex();
                break;
            }
        }
        List<Deposit> depositList = chain.getDepositList();
        Set<NulsHash> agentSet = new HashSet<>();
        BigInteger totalDeposit = BigInteger.ZERO;
        for (Deposit deposit : depositList) {
            if (deposit.getDelHeight() != -1L && deposit.getDelHeight() <= startBlockHeight) {
                continue;
            }
            if (deposit.getBlockHeight() > startBlockHeight || deposit.getBlockHeight() < 0L) {
                continue;
            }
            if (!Arrays.equals(deposit.getAddress(), addressBytes)) {
                continue;
            }
            /*agentSet.add(deposit.getAgentHash());*/
            totalDeposit = totalDeposit.add(deposit.getDeposit());
        }
        dto.setAgentCount(agentCount);
        dto.setAgentHash(agentHash);
        dto.setJoinAgentCount(agentSet.size());
        //统计账户奖励金

        dto.setTotalDeposit(String.valueOf(totalDeposit));
        try {
            //从账本模块获取账户可用余额
        } catch (Exception e) {
            chain.getLogger().error(e);
            dto.setUsableBalance(BigIntegerUtils.ZERO);
        }
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(dto);
    }

    /**
     * 获取惩罚信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getPublishList(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        SearchPunishDTO dto = JSONUtils.map2pojo(params, SearchPunishDTO.class);
        int chainId = dto.getChainId();
        String address = dto.getAddress();
        int type = dto.getType();
        if (chainId == 0 || StringUtils.isBlank(address)) {
            return Result.getFailed(ConsensusErrorCode.PARAM_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(ConsensusErrorCode.CHAIN_NOT_EXIST);
        }
        List<PunishLogDTO> yellowPunishList = null;
        List<PunishLogDTO> redPunishList = null;
        int typeOfYellow = 2;
        //查询红牌交易
        if (type != 1) {
            redPunishList = new ArrayList<>();
            for (PunishLogPo po : chain.getRedPunishList()) {
                if (StringUtils.isNotBlank(address) && !ByteUtils.arrayEquals(po.getAddress(), AddressTool.getAddress(address))) {
                    continue;
                }
                redPunishList.add(new PunishLogDTO(po));
            }
        }
        if (type != typeOfYellow) {
            yellowPunishList = new ArrayList<>();
            for (PunishLogPo po : chain.getYellowPunishList()) {
                if (StringUtils.isNotBlank(address) && !ByteUtils.arrayEquals(po.getAddress(), AddressTool.getAddress(address))) {
                    continue;
                }
                yellowPunishList.add(new PunishLogDTO(po));
            }
        }
        Map<String, List<PunishLogDTO>> resultMap = new HashMap<>(2);
        resultMap.put(PARAM_RED_PUNISH, redPunishList);
        resultMap.put(PARAM_YELLOW_PUNISH, yellowPunishList);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }

    /**
     * 获取当前轮次信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getCurrentRoundInfo(Map<String, Object> params) {
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
        MeetingRound round = roundManager.getCurrentRound(chain);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(round);
    }


    /**
     * 获取指定区块轮次
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getRoundMemberList(Map<String, Object> params) {
        if (params == null || params.get(PARAM_CHAIN_ID) == null || params.get(PARAM_EXTEND) == null) {
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
            BlockExtendsData extendsData = new BlockExtendsData(RPCUtil.decode((String) params.get(PARAM_EXTEND)));
            MeetingRound round = roundManager.getRoundByIndex(chain, extendsData.getRoundIndex());
            if (round == null) {
                round = roundManager.getRound(chain, extendsData, false);
            }
            List<String> packAddressList = new ArrayList<>();
            for (MeetingMember meetingMember : round.getMemberList()) {
                packAddressList.add(AddressTool.getStringAddressByBytes(meetingMember.getAgent().getPackingAddress()));
            }
            Map<String, Object> resultMap = new HashMap<>(2);
            resultMap.put(PARAM_PACKING_ADDRESS_LIST, packAddressList);
            return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (Exception e) {
            return Result.getFailed(ConsensusErrorCode.DATA_ERROR);
        }
    }

    /**
     * 获取共模块识配置信息
     *
     * @param params
     * @return Result
     */
    @Override
    @SuppressWarnings("unchecked")
    public Result getConsensusConfig(Map<String, Object> params) {
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
        ChainConfig chainConfig = chain.getConfig();
        Map<String, Object> map = new HashMap<>(8);
        map.put(PARAM_SEED_NODES, chainConfig.getSeedNodes());
        map.put(PARAM_INFLATION_AMOUNT, chainConfig.getInflationAmount());
        map.put(PARAM_AGENT_ASSET_ID, chainConfig.getAgentAssetId());
        map.put(PARAM_AGENT_CHAIN_ID, chainConfig.getAgentChainId());
        map.put(PARAM_AWARD_ASSERT_ID, chainConfig.getAwardAssetId());
        map.put(PARAM_TOTALINFLATIONAMOUNT, chainConfig.getTotalInflationAmount());
        map.put(PARAM_MAIN_ASSERT_BASE,chainConfig.getMainAssertBase());
        map.put(PARAM_LOCAL_ASSERT_BASE,chainConfig.getLocalAssertBase());
        map.put(PARAM_COMMISSION_MIN,chainConfig.getPackDepositMin());
        map.put(PARAM_SEED_COUNT,chain.getSeedNodeList().size());
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getSeedNodeInfo(Map<String, Object> params) {
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
            List<String> packAddressList = chain.getSeedNodeList();
            MeetingRound round = roundManager.getCurrentRound(chain);
            MeetingMember member = null;
            if(round != null){
                member = round.getMyMember();
            }
            Map<String, Object> resultMap = new HashMap<>(4);
            if (member != null) {
                String address = AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress());
                if(packAddressList.contains(address)){
                    resultMap.put(PARAM_ADDRESS, address);
                    resultMap.put(PARAM_PASSWORD, chain.getConfig().getPassword());
                }
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
    public Result getRateAddition(Map<String, Object> params) {
        Map<String, Object> resultMap = new HashMap<>(2);
        if(chainManager.getStackingAssetList() == null || chainManager.getStackingAssetList().isEmpty()){
            resultMap.put(PARAM_LIST, new ArrayList<>());
        }else{
            List<RateAdditionDTO> rateAdditionList = new ArrayList<>();
            double basicRate;
            for (StackingAsset stackingAsset : chainManager.getStackingAssetList()){
                basicRate = getBasicRate(stackingAsset.getChainId(), stackingAsset.getAssetId());
                RateAdditionDTO rateAddition = new RateAdditionDTO(stackingAsset, basicRate);
                rateAddition.setDetailList(getRateAdditionDetail(basicRate));
                rateAdditionList.add(rateAddition);
            }
            resultMap.put(PARAM_LIST, rateAdditionList);
        }
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }


    @Override
    @SuppressWarnings("unchecked")
    public Result getRewardUnit(Map<String, Object> params) {
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
        long height = Long.valueOf(params.get(PARAM_HEIGHT).toString());
        long initHeight = chain.getConfig().getInitHeight();
        long initEndHeight = initHeight + chain.getConfig().getDeflationHeightInterval();
        BigInteger award;
        if(height > initEndHeight){
            long differentCount = (height - initEndHeight)/chain.getConfig().getDeflationHeightInterval();
            if((height - initEndHeight)%chain.getConfig().getDeflationHeightInterval() != 0){
                differentCount ++;
            }
            double ratio = DoubleUtils.div(chain.getConfig().getDeflationRatio(), NulsEconomicConstant.VALUE_OF_100, 4);
            BigInteger inflationAmount = DoubleUtils.mul(new BigDecimal(chain.getConfig().getInflationAmount()),BigDecimal.valueOf(Math.pow(ratio, differentCount))).toBigInteger();
            award = DoubleUtils.div(new BigDecimal(inflationAmount), chain.getConfig().getDeflationHeightInterval()).toBigInteger();
        }else{
            award = DoubleUtils.div(new BigDecimal(chain.getConfig().getInflationAmount()), chain.getConfig().getDeflationHeightInterval()).toBigInteger();
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put(PARAM_RESULT_VALUE, award);
        return Result.getSuccess(ConsensusErrorCode.SUCCESS).setData(resultMap);
    }

    private double getBasicRate(int assetChainId, int assetId){
        if(assetChainId == consensusConfig.getChainId() && assetId == consensusConfig.getAssetId()){
            return consensusConfig.getLocalAssertBase();
        }
        if(assetChainId == consensusConfig.getMainChainId() && assetId == consensusConfig.getMainAssetId()){
            return consensusConfig.getMainAssertBase();
        }
        return 1;
    }

    private List<RateAdditionDetailDTO> getRateAdditionDetail(double basicRate){
        List<RateAdditionDetailDTO> rateAdditionDetailList = new ArrayList<>();
        rateAdditionDetailList.add(new RateAdditionDetailDTO((byte)0, basicRate, null));
        for (DepositTimeType depositTimeType : DepositTimeType.values()){
            rateAdditionDetailList.add(new RateAdditionDetailDTO((byte)1, basicRate, depositTimeType));
        }
        return rateAdditionDetailList;
    }
}
