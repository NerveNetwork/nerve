package io.nuls.consensus.utils.manager;

import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.dto.output.AgentBasicDTO;
import io.nuls.consensus.model.po.AgentPo;
import io.nuls.consensus.model.po.VirtualAgentPo;
import io.nuls.consensus.storage.AgentStorageService;
import io.nuls.consensus.storage.VirtualAgentStorageService;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.compare.AgentComparator;
import io.nuls.consensus.utils.compare.AgentDepositComparator;
import io.nuls.consensus.utils.compare.AgentPoDepositComparator;
import io.nuls.consensus.v1.utils.RoundUtils;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Node management class, responsible for node related operations
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
     * Load node information
     * Initialize node information
     *
     * @param chain Chain information/chain info
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
     * Add specified chain nodes
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
     * Modify specified chain nodes
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
     * Delete specified chain nodes
     * Delete the specified link node
     *
     * @param chain  chain info
     * @param txHash Create transactions for this nodeHASH/Creating the node transaction hash
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
     * Query specified nodes
     * Query specified nodes
     *
     * @param chain  chain info
     * @param txHash Create transactions for this nodeHASH/Creating the node transaction hash
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
     * Query specified nodes
     * Query specified nodes
     *
     * @param chain        chain info
     * @param agentAddress Node address
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
     * Query specified nodes
     * Query specified nodes
     *
     * @param chain        chain info
     * @param agentAddress Node address
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
     * Query specified nodes
     * Query specified nodes
     *
     * @param chain       chain info
     * @param packAddress Node address
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
     * Set node public key
     *
     * @param chain  Chain information
     * @param pubKey Block address public key
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
     * Get nodesid
     * Get agent id
     *
     * @param hash nodeHASH/Agent hash
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
     * Verify if the node exists and if the specified address is the creator
     * Verify that the node exists
     *
     * @param chain     Chain information
     * @param agentHash nodeHash
     * @param address   Creator Address
     * @return Is the creator correct
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
     * Get a list of all nodes that have not been logged out yet(Avoid duplicate account creation when creating nodes)
     * Get a list of nodes that meet block requirements
     *
     * @param chain Chain information
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
     * Get a list of block node addresses at a specified height
     * Get a list of nodes that meet block requirements
     *
     * @param chain  Chain information
     * @param height block height
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
     * Get a list of block nodes at a specified height
     * Get the node list of the specified height out of block
     *
     * @param chain  Chain information
     * @param height Specify block height
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
            if (packAgentList.size() >= chain.getConsensusAgentCountMax()) {
                break;
            }
            if (agent.getDeposit().compareTo(chain.getConfig().getPackDepositMin()) >= 0) {
                packAgentList.add(agent);
            } else {
                break;
            }
        }
        return packAgentList;
    }

    /**
     * Obtain the basic information list of block nodes at a specified height
     * Get the node list of the specified height out of block
     *
     * @param chain  Chain information
     * @param height Specify block height
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
            if (packBasicAgentList.size() >= chain.getConsensusAgentCountMax()) {
                break;
            }
            if (agent.getDeposit().compareTo(chain.getConfig().getPackDepositMin()) >= 0) {
                packBasicAgentList.add(new AgentBasicDTO(agent));
            } else {
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
     * Obtain a list of block nodes at a certain height before the current block
     * Get a list of out of block nodes before the current block
     *
     * @param chain  Chain information
     * @param height Specify block height
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
        //Exit margin trading after obtaining this height
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
        //Additional margin trading after obtaining this height
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
     * Calculate the deposit for each account and return the total deposit
     *
     * @param chain       Chain information
     * @param endHeight   height
     * @param depositMap  entrust
     * @param totalAmount Total entrusted amount
     */
    public BigDecimal getAgentDepositByHeight(Chain chain, long startHeight, long endHeight, Map<String, BigDecimal> depositMap, BigDecimal totalAmount) {
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
            if (agent.getBlockHeight() > endHeight) {
                continue;
            }
            if (agent.getDelHeight() != -1L && agent.getDelHeight() <= endHeight) {
                continue;
            }
            if (endHeight > chain.getConfig().getDepositAwardChangeHeight() && agent.getBlockHeight() > startHeight) {
                continue;
            }
            agentMap.put(agent.getHash(), agent);
        }
        if (agentMap.isEmpty()) {
            return totalAmount;
        }
        //Exit margin trading after obtaining this height
        Map<NulsHash, BigInteger> reduceDepositMap = agentDepositManager.getReduceDepositAfterHeight(chain, endHeight);
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
        //Additional margin trading after obtaining this height
        Map<NulsHash, BigInteger> appendDepositMap = agentDepositManager.getAppendDepositAfterHeight(chain, endHeight);
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

        List<AgentPo> sortAgentList = new ArrayList<>(agentMap.values());
        sortAgentList.sort(AgentPoDepositComparator.getInstance());

//        Map<NulsHash, AgentPo> sortAgentMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_64);
//        agentMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getDeposit())).forEach(e -> sortAgentMap.put(e.getKey(), e.getValue()));
        //Virtual banking weight*4Consensus node weight*3No fast node weights have been released*1
        int maxConsensusAgentCount = chain.getConsensusAgentCountMax();
        List<String> virtualBankList = new ArrayList<>();
        VirtualAgentPo virtualAgentPo = virtualAgentStorageService.get(endHeight);
        if (virtualAgentPo != null) {
            virtualBankList = virtualAgentPo.getVirtualAgentList();
        }
        BigDecimal realDeposit;
        String rewardAddress;
        String agentAddress;
        int count = 1;
        double baseWeight = 1;
        for (AgentPo agentPo : sortAgentList) {
            StringBuilder ss = new StringBuilder();
            ss.append(AddressTool.getStringAddressByBytes(agentPo.getAgentAddress()));
            ss.append("-");
            ss.append(agentPo.getDeposit().toString());
            baseWeight = chain.getConfig().getLocalAssertBase();

            rewardAddress = AddressTool.getStringAddressByBytes(agentPo.getRewardAddress());
            agentAddress = AddressTool.getStringAddressByBytes(agentPo.getAgentAddress());
            //If it is a virtual bank, it will be calculated based on the virtual bank
            realDeposit = new BigDecimal(agentPo.getDeposit());
            if (!virtualBankList.isEmpty() && virtualBankList.contains(agentAddress)) {
                ss.append("-Virtual banking");
                baseWeight = baseWeight * chain.getConfig().getSuperAgentDepositBase();
            } else if (count <= maxConsensusAgentCount) {
                ss.append("-Consensus node");
                baseWeight = baseWeight * chain.getConfig().getAgentDepositBase();
            } else if (count > maxConsensusAgentCount) {
                ss.append("-Subsequent nodes");
                baseWeight = baseWeight * chain.getConfig().getReservegentDepositBase();
            }
            ss.append("-");
            ss.append(baseWeight);
            baseWeight = Math.sqrt(baseWeight);
            BigDecimal weightSqrt = new BigDecimal(baseWeight).setScale(4, BigDecimal.ROUND_HALF_UP);
            realDeposit = DoubleUtils.mul(realDeposit, weightSqrt);
            ss.append("-");
            ss.append(realDeposit.toString());

            totalAmount = totalAmount.add(realDeposit);
            ss.append("-total:");
            ss.append(totalAmount.toString());

            chain.getLogger().info(ss.toString());
            depositMap.merge(rewardAddress, realDeposit, (oldValue, value) -> oldValue.subtract(value));
            count++;
        }
        return totalAmount;
    }
}
