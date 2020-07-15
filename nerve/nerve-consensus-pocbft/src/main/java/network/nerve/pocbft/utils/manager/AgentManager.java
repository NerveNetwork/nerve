package network.nerve.pocbft.utils.manager;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.dto.output.AgentBasicDTO;
import network.nerve.pocbft.model.po.AgentPo;
import network.nerve.pocbft.model.po.VirtualAgentPo;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.compare.AgentComparator;
import network.nerve.pocbft.utils.compare.AgentDepositComparator;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.storage.AgentStorageService;
import network.nerve.pocbft.storage.VirtualAgentStorageService;
import network.nerve.pocbft.v1.utils.RoundUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 节点管理类，负责节点的相关操作
 * Node management class, responsible for the operation of the node
 *
 * @author tag
 * 2018/12/5
 */
@Component
public class AgentManager {
    @Autowired
    private AgentStorageService agentStorageService;
    @Autowired
    private AgentDepositManager agentDepositManager;
    @Autowired
    private VirtualAgentStorageService virtualAgentStorageService;

    /**
     * 加载节点信息
     * Initialize node information
     *
     * @param chain 链信息/chain info
     */
    public void loadAgents(Chain chain) throws Exception {
        List<Agent> allAgentList = new ArrayList<>();
        List<AgentPo> poList = this.agentStorageService.getList(chain.getConfig().getChainId());
        for (AgentPo po : poList) {
            Agent agent = new Agent(po);
            allAgentList.add(agent);
        }
        allAgentList.sort(new AgentComparator());
        chain.setAgentList(allAgentList);
    }

    /**
     * 添加指定链节点
     * Adding specified chain nodes
     *
     * @param chain chain info
     * @param agent agent info
     */
    public boolean addAgent(Chain chain, Agent agent) {
        if (!agentStorageService.save(new AgentPo(agent), chain.getConfig().getChainId())) {
            chain.getLogger().error("Agent data save error!");
            return false;
        }
        chain.getAgentList().add(agent);
        return true;
    }

    /**
     * 修改指定链节点
     * Modifying specified chain nodes
     *
     * @param chain     chain info
     * @param realAgent agent info
     */
    public boolean updateAgent(Chain chain, Agent realAgent) {
        if (!agentStorageService.save(new AgentPo(realAgent), chain.getChainId())) {
            chain.getLogger().error("Agent data update error!");
            return false;
        }

        for (Agent agent : chain.getAgentList()) {
            if (agent.getTxHash().equals(realAgent.getTxHash())) {
                agent.setDelHeight(realAgent.getDelHeight());
                agent.setDeposit(realAgent.getDeposit());
                break;
            }
        }

        return true;
    }

    /**
     * 删除指定链节点
     * Delete the specified link node
     *
     * @param chain  chain info
     * @param txHash 创建该节点交易的HASH/Creating the node transaction hash
     */
    public boolean removeAgent(Chain chain, NulsHash txHash) {
        if (!agentStorageService.delete(txHash, chain.getConfig().getChainId())) {
            chain.getLogger().error("Data save error!");
            return false;
        }
        chain.getAgentList().removeIf(s -> s.getTxHash().equals(txHash));
        return true;
    }

    /**
     * 查询指定节点
     * Query specified nodes
     *
     * @param chain  chain info
     * @param txHash 创建该节点交易的HASH/Creating the node transaction hash
     */
    public Agent getAgentByHash(Chain chain, NulsHash txHash) {
        for (Agent agent : chain.getAgentList()) {
            if (txHash.equals(agent.getTxHash())) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 查询指定节点
     * Query specified nodes
     *
     * @param chain        chain info
     * @param agentAddress 节点地址
     */
    public Agent getAgentByAddress(Chain chain, byte[] agentAddress) {
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() > 0) {
                continue;
            }
            if (Arrays.equals(agentAddress, agent.getAgentAddress())) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 查询指定节点
     * Query specified nodes
     *
     * @param chain        chain info
     * @param agentAddress 节点地址
     */
    public Agent getValidAgentByAddress(Chain chain, byte[] agentAddress) {
        for (Agent agent : chain.getAgentList()) {
            if (Arrays.equals(agentAddress, agent.getAgentAddress()) && agent.getDelHeight() == -1) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 查询指定节点
     * Query specified nodes
     *
     * @param chain       chain info
     * @param packAddress 节点地址
     */
    public Agent getAgentByPackAddress(Chain chain, byte[] packAddress) {
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() != -1) {
                continue;
            }
            if (Arrays.equals(packAddress, agent.getPackingAddress())) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 设置节点公钥
     *
     * @param chain  链信息
     * @param pubKey 出块地址公钥
     */
    public void setPubkey(Chain chain, byte[] pubKey) {
        byte[] packAddressByte = AddressTool.getAddress(pubKey, chain.getChainId());
        String packAddress = AddressTool.getStringAddressByBytes(packAddressByte);
        if (chain.getUnBlockAgentList().contains(packAddress)) {
            Agent agent = getAgentByPackAddress(chain, packAddressByte);
            if (agent == null || agent.getPubKey() != null) {
                return;
            }
            agent.setPubKey(pubKey);
            if (!agentStorageService.save(new AgentPo(agent), chain.getChainId())) {
                chain.getLogger().error("Agent pubKey set error!");
                return;
            }
            chain.getUnBlockAgentList().remove(packAddress);
        }
    }

    /**
     * 获取节点id
     * Get agent id
     *
     * @param hash 节点HASH/Agent hash
     * @return String
     */
    public String getAgentId(NulsHash hash) {
        String hashHex = hash.toHex();
        return hashHex.substring(hashHex.length() - 8).toUpperCase();
    }

    public void fillAgentList(Chain chain, List<Agent> agentList) {

        MeetingRound round = RoundUtils.getRoundController().getCurrentRound();
        for (Agent agent : agentList) {
            fillAgent(agent, round);
        }
    }

    public void fillAgent(Agent agent, MeetingRound round) {
        if (round == null) {
            return;
        }
        MeetingMember member = round.getMemberByPackingAddress(agent.getPackingAddress());
        if (null == member) {
            agent.setStatus(0);
            return;
        } else {
            agent.setStatus(1);
        }
        agent.setCreditVal(member.getAgent().getRealCreditVal());
    }

    /**
     * 验证节点是否存在且指定地址是否为创建者
     * Verify that the node exists
     *
     * @param chain     链信息
     * @param agentHash 节点Hash
     * @param address   创建者地址
     * @return 创建者是否正确
     */
    @SuppressWarnings("unchecked")
    public Result creatorValid(Chain chain, NulsHash agentHash, byte[] address) {
        AgentPo agentPo = agentStorageService.get(agentHash, chain.getChainId());
        if (agentPo == null || agentPo.getDelHeight() > 0) {
            chain.getLogger().error("Agent does not exist");
            return Result.getFailed(ConsensusErrorCode.AGENT_NOT_EXIST);
        }
        if (!Arrays.equals(agentPo.getAgentAddress(), address)) {
            chain.getLogger().error("Account is not the agent Creator,agentAddress:{},address:{}", AddressTool.getStringAddressByBytes(agentPo.getAgentAddress()), AddressTool.getStringAddressByBytes(address));
            return Result.getFailed(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
        }
        return ConsensusNetUtil.getSuccess().setData(agentPo);
    }


    /**
     * 获取当前未注销的所有节点列表(创建节点时避免账户重复创建节点)
     * Get a list of nodes that meet block requirements
     *
     * @param chain 链信息
     * @return List<agent>
     **/
    public List<Agent> getAgentList(Chain chain, long height) {
        List<Agent> agentList = new ArrayList<>();
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= height) {
                continue;
            }
            if (agent.getBlockHeight() > height || agent.getBlockHeight() < 0L) {
                continue;
            }
            agentList.add(agent);
        }
        return agentList;
    }

    /**
     * 获取指定高度出块节点地址列表
     * Get a list of nodes that meet block requirements
     *
     * @param chain  链信息
     * @param height 区块高度
     * @return List<agent>
     **/
    public Set<String> getPackAddressList(Chain chain, long height) {
        Set<String> packAddressList = new HashSet<>();
        List<Agent> agentList = getPackAgentList(chain, height);
        agentList.forEach(agent ->
                packAddressList.add(AddressTool.getStringAddressByBytes(agent.getPackingAddress()))
        );
        return packAddressList;
    }

    /**
     * 获取指定高度出块节点列表
     * Get the node list of the specified height out of block
     *
     * @param chain  链信息
     * @param height 指定区块高度
     * @return List<agent>
     **/
    public List<Agent> getPackAgentList(Chain chain, long height) {
        List<Agent> packAgentList = new ArrayList<>();
        boolean isLatestHeight = height == chain.getBestHeader().getHeight();
        List<Agent> agentList;
        if (!isLatestHeight) {
            agentList = getBeforeAgentList(chain, height);
        } else {
            agentList = getValidAgentList(chain);
        }
        agentList.sort(new AgentDepositComparator());
        for (Agent agent : agentList) {
            if (agent.getDeposit().compareTo(chain.getConfig().getPackDepositMin()) >= 0) {
                packAgentList.add(agent);
            } else {
                break;
            }
            if (packAgentList.size() >= chain.getConsensusAgentCountMax()) {
                break;
            }
        }
        return packAgentList;
    }

    /**
     * 获取指定高度出块节点基础信息列表
     * Get the node list of the specified height out of block
     *
     * @param chain  链信息
     * @param height 指定区块高度
     * @return List<agent>
     **/
    public List<AgentBasicDTO> getPackBasicAgentList(Chain chain, long height, boolean isNewest) {
        List<AgentBasicDTO> packBasicAgentList = new ArrayList<>();
        for (int index = 0; index < chain.getSeedAddressList().size(); index++) {
            packBasicAgentList.add(new AgentBasicDTO(chain.getSeedAddressList().get(index), HexUtil.encode(chain.getSeedNodePubKeyList().get(index))));
        }
        List<Agent> agentList;
        if (!isNewest) {
            agentList = getBeforeAgentList(chain, height);
        } else {
            agentList = getValidAgentList(chain);
        }
        agentList.sort(new AgentDepositComparator());
        for (Agent agent : agentList) {
            if (agent.getDeposit().compareTo(chain.getConfig().getPackDepositMin()) >= 0) {
                packBasicAgentList.add(new AgentBasicDTO(agent));
            } else {
                break;
            }
            if (packBasicAgentList.size() >= chain.getConsensusAgentCountMax()) {
                break;
            }
        }
        return packBasicAgentList;
    }

    private List<Agent> getValidAgentList(Chain chain) {
        List<Agent> agentList = new ArrayList<>();
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() != -1L) {
                continue;
            }
            if (agent.getBlockHeight() < 0L) {
                continue;
            }
            agentList.add(agent);
        }
        return agentList;
    }

    /**
     * 获取当前区块之前的某个高度出块节点列表
     * Get a list of out of block nodes before the current block
     *
     * @param chain  链信息
     * @param height 指定区块高度
     * @return List<agent>
     */
    private List<Agent> getBeforeAgentList(Chain chain, long height) {
        Map<NulsHash, Agent> agentMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= height) {
                continue;
            }
            if (agent.getBlockHeight() > height || agent.getBlockHeight() < 0L) {
                continue;
            }
            try {
                agentMap.put(agent.getTxHash(), agent.clone());
            } catch (CloneNotSupportedException e) {
                chain.getLogger().error(e);
            }
        }
        //获取该高度之后的退出保证金交易
        Map<NulsHash, BigInteger> reduceDepositMap = agentDepositManager.getReduceDepositAfterHeight(chain, height);
        BigInteger realDeposit;
        if (!reduceDepositMap.isEmpty()) {
            for (Map.Entry<NulsHash, BigInteger> entry : reduceDepositMap.entrySet()) {
                NulsHash agentHash = entry.getKey();
                if (agentMap.containsKey(agentHash)) {
                    realDeposit = agentMap.get(agentHash).getDeposit().add(entry.getValue());
                    agentMap.get(agentHash).setDeposit(realDeposit);
                }
            }
        }
        //获取该高度之后的追加保证金交易
        Map<NulsHash, BigInteger> appendDepositMap = agentDepositManager.getAppendDepositAfterHeight(chain, height);
        if (!appendDepositMap.isEmpty()) {
            for (Map.Entry<NulsHash, BigInteger> entry : appendDepositMap.entrySet()) {
                NulsHash agentHash = entry.getKey();
                if (agentMap.containsKey(agentHash)) {
                    realDeposit = agentMap.get(agentHash).getDeposit().subtract(entry.getValue());
                    agentMap.get(agentHash).setDeposit(realDeposit);
                }
            }
        }
        return new ArrayList<>(agentMap.values());
    }

    /**
     * 计算个账户的保证金并返回总的保证金
     *
     * @param chain       链信息
     * @param height      高度
     * @param depositMap  委托
     * @param totalAmount 总委托金额
     */
    public BigDecimal getAgentDepositByHeight(Chain chain, long height, Map<String, BigDecimal> depositMap, BigDecimal totalAmount) {
        List<AgentPo> poList;
        try {
            poList = agentStorageService.getList(chain.getConfig().getChainId());
        } catch (NulsException e) {
            return totalAmount;
        }
        if (poList == null || poList.isEmpty()) {
            return totalAmount;
        }
        Map<NulsHash, AgentPo> agentMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_64);
        for (AgentPo agent : poList) {
            if (agent.getBlockHeight() > height) {
                continue;
            }
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= height) {
                continue;
            }
            agentMap.put(agent.getHash(), agent);
        }
        if (agentMap.isEmpty()) {
            return totalAmount;
        }
        //获取该高度之后的退出保证金交易
        Map<NulsHash, BigInteger> reduceDepositMap = agentDepositManager.getReduceDepositAfterHeight(chain, height);
        BigInteger tempDeposit;
        AgentPo po;
        if (!reduceDepositMap.isEmpty()) {
            for (Map.Entry<NulsHash, BigInteger> entry : reduceDepositMap.entrySet()) {
                NulsHash agentHash = entry.getKey();
                if (agentMap.containsKey(agentHash)) {
                    po = agentMap.get(agentHash);
                    tempDeposit = po.getDeposit().add(entry.getValue());
                    po.setDeposit(tempDeposit);
                }
            }
        }
        //获取该高度之后的追加保证金交易
        Map<NulsHash, BigInteger> appendDepositMap = agentDepositManager.getAppendDepositAfterHeight(chain, height);
        if (!appendDepositMap.isEmpty()) {
            for (Map.Entry<NulsHash, BigInteger> entry : appendDepositMap.entrySet()) {
                NulsHash agentHash = entry.getKey();
                if (agentMap.containsKey(agentHash)) {
                    po = agentMap.get(agentHash);
                    tempDeposit = po.getDeposit().subtract(entry.getValue());
                    po.setDeposit(tempDeposit);
                }
            }
        }
        Map<NulsHash, AgentPo> sortAgentMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_64);
        agentMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getDeposit())).forEach(e -> sortAgentMap.put(e.getKey(), e.getValue()));
        //虚拟银行权重*4，共识节点权重*3，未出快节点权重*1
        int maxConsensusAgentCount = chain.getConsensusAgentCountMax();
        List<String> virtualBankList = new ArrayList<>();
        VirtualAgentPo virtualAgentPo = virtualAgentStorageService.get(height);
        if (virtualAgentPo != null) {
            virtualBankList = virtualAgentPo.getVirtualAgentList();
        }
        BigDecimal realDeposit;
        String rewardAddress;
        String agentAddress;
        int count = 1;
        double baseWeight = chain.getConfig().getLocalAssertBase();
        for (AgentPo agentPo : sortAgentMap.values()) {
            rewardAddress = AddressTool.getStringAddressByBytes(agentPo.getRewardAddress());

            agentAddress = AddressTool.getStringAddressByBytes(agentPo.getAgentAddress());
            //如果为虚拟银行则按虚拟银行计算
            realDeposit = new BigDecimal(agentPo.getDeposit());
            if (!virtualBankList.isEmpty() && virtualBankList.contains(agentAddress)) {
                baseWeight = baseWeight * chain.getConfig().getSuperAgentDepositBase();
            } else if (count <= maxConsensusAgentCount) {
                baseWeight = baseWeight * chain.getConfig().getAgentDepositBase();
            } else if (count > maxConsensusAgentCount) {
                baseWeight = baseWeight * chain.getConfig().getReservegentDepositBase();
            }
            baseWeight = Math.sqrt(baseWeight);
            BigDecimal weightSqrt = new BigDecimal(baseWeight).setScale(4, BigDecimal.ROUND_HALF_UP);
            realDeposit = DoubleUtils.mul(realDeposit, weightSqrt);
            totalAmount = totalAmount.add(realDeposit);

            depositMap.merge(rewardAddress, realDeposit, (oldValue, value) -> oldValue.subtract(value));
            count++;
        }
        return totalAmount;
    }
}
