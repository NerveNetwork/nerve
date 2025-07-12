package io.nuls.consensus.model.bo;

import io.nuls.common.ConfigBean;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.consensus.model.bo.consensus.Evidence;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.model.po.ChangeAgentDepositPo;
import io.nuls.consensus.model.po.PubKeyPo;
import io.nuls.consensus.model.po.PunishLogPo;
import io.nuls.consensus.utils.enumeration.ConsensusStatus;
import io.nuls.consensus.v1.cache.ConsensusCache;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Chain information class
 * Chain information class
 *
 * @author tag
 * 2018/12/4
 **/
public class Chain {
    private ConsensusCache consensusCache = new ConsensusCache();

    /**
     * Is the consensus network well organized
     * Link to the current consensus network80%The node represents that the consensus network has been assembled
     */
    private boolean networkStateOk;

    /**
     * Chain basic configuration information
     * Chain Foundation Configuration Information
     */
    private ConfigBean config;

    /**
     * running state
     * Chain running state
     */
    private ConsensusStatus consensusStatus;

    /**
     * Block synchronization completed, reaching the latest level
     * Chain packing state
     */
    private boolean synchronizedHeight;

    /**
     * Latest block head
     * The most new block
     */
    private BlockHeader bestHeader;

    /**
     * Node List
     * Agent list
     */
    private List<Agent> agentList;

    /**
     * List of entrusted information
     * Deposit list
     */
    private List<Deposit> depositList;

    /**
     * Yellow Card List
     * Yellow punish list
     */
    private List<PunishLogPo> yellowPunishList;

    /**
     * Red Card List
     * Red punish list
     */
    private List<PunishLogPo> redPunishList;
    /**
     * Is it a consensus node
     * Is it a consensus node
     */
    private boolean consonsusNode;
    /**
     * Record chain block addressPackingAddressEvidence of two different blocks emitted from the same height
     * If the next round is normal, reset to zero, continuity3Wheel will be penalized with a red card
     * Record the address of each chain out block Packing Address, and the same height gives evidence of two different blocks.
     * The next round of normal will be cleared, and three consecutive rounds will be punished by red cards.
     */
    private Map<String, List<Evidence>> evidenceMap;

    /**
     * Save the red card transactions that need to be packaged for this node,When packing nodes, all red card transactions in the set need to be packed and deleted
     * To save the red card transactions that need to be packaged by the node,
     * the node should pack and delete all the red card transactions in the set when packing.
     */
    private List<Transaction> redPunishTransactionList;

    /**
     * Cache information for the last five rounds
     * Round list
     */
    private List<MeetingRound> roundList;

    /**
     * Latest200Wheel block head
     * The latest 200 rounds block
     */
    private List<BlockHeader> blockHeaderList;

    /**
     * Additional margin information list
     * List of additional margin information
     */
    private List<ChangeAgentDepositPo> appendDepositList;

    /**
     * List of reduced margin information
     * Reduced margin information list
     */
    private List<ChangeAgentDepositPo> reduceDepositList;

    private NulsLogger logger;


    /**
     * Seed node list
     * Seed node list
     */
    private List<String> seedAddressList;

    /**
     * Thread pool
     */
    private ThreadPoolExecutor threadPool;

    private List<byte[]> seedNodePubKeyList;
    private List<Agent> seedAgentList;
    /**
     * List of consensus node addresses that have not been blocked in this chain
     */
    private Set<String> unBlockAgentList;

    /**
     * The block address and public key value pairs of nodes in this chain
     */
    private PubKeyPo pubKeyPo;

    public Chain() {
        this.consensusStatus = ConsensusStatus.RUNNING;
        this.synchronizedHeight = false;
        this.agentList = new ArrayList<>();
        this.depositList = new ArrayList<>();
        this.yellowPunishList = new ArrayList<>();
        this.redPunishList = new ArrayList<>();
        this.evidenceMap = new HashMap<>();
        this.redPunishTransactionList = new ArrayList<>();
        this.roundList = new ArrayList<>();
        this.consonsusNode = false;
        this.seedAddressList = new ArrayList<>();
        this.appendDepositList = new ArrayList<>();
        this.reduceDepositList = new ArrayList<>();
        this.networkStateOk = false;
        this.seedNodePubKeyList = new ArrayList<>();
        this.unBlockAgentList = new HashSet<>();
    }


    public int getChainId() {
        return config.getChainId();
    }

    public int getAssetId() {
        return config.getAssetId();
    }

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public ConsensusStatus getConsensusStatus() {
        return consensusStatus;
    }

    public void setConsensusStatus(ConsensusStatus consensusStatus) {
        this.consensusStatus = consensusStatus;
    }

    public boolean isSynchronizedHeight() {
        return synchronizedHeight;
    }

    public void setSynchronizedHeight(boolean synchronizedHeight) {
        this.synchronizedHeight = synchronizedHeight;
    }

    public List<Agent> getAgentList() {
        return agentList;
    }

    public void setAgentList(List<Agent> agentList) {
        this.agentList = agentList;
    }

    public List<Deposit> getDepositList() {
        return depositList;
    }

    public void setDepositList(List<Deposit> depositList) {
        this.depositList = depositList;
    }

    public List<PunishLogPo> getYellowPunishList() {
        return yellowPunishList;
    }

    public void setYellowPunishList(List<PunishLogPo> yellowPunishList) {
        this.yellowPunishList = yellowPunishList;
    }

    public List<PunishLogPo> getRedPunishList() {
        return redPunishList;
    }

    public void setRedPunishList(List<PunishLogPo> redPunishList) {
        this.redPunishList = redPunishList;
    }

    public Map<String, List<Evidence>> getEvidenceMap() {
        return evidenceMap;
    }

    public void setEvidenceMap(Map<String, List<Evidence>> evidenceMap) {
        this.evidenceMap = evidenceMap;
    }

    public List<Transaction> getRedPunishTransactionList() {
        return redPunishTransactionList;
    }

    public void setRedPunishTransactionList(List<Transaction> redPunishTransactionList) {
        this.redPunishTransactionList = redPunishTransactionList;
    }

    public List<MeetingRound> getRoundList() {
        return roundList;
    }

    public void setRoundList(List<MeetingRound> roundList) {
        this.roundList = roundList;
    }

    public BlockHeader getBestHeader() {
        return bestHeader;
    }

    public void setBestHeader(BlockHeader bestHeader) {
        this.bestHeader = bestHeader;
    }

    public List<BlockHeader> getBlockHeaderList() {
        return blockHeaderList;
    }

    public void setBlockHeaderList(List<BlockHeader> blockHeaderList) {
        this.blockHeaderList = blockHeaderList;
    }

    public ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    public boolean isConsonsusNode() {
        return consonsusNode;
    }

    public void setConsonsusNode(boolean consonsusNode) {
//        Log.error("-=-=-=-=-Consensus state setting：{}",consonsusNode);
        this.consonsusNode = consonsusNode;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public List<String> getSeedAddressList() {
        return seedAddressList;
    }

    public List<Agent> getSeedAgentList() {
        if (this.seedAgentList == null) {
            List<Agent> list = new ArrayList<>();
            for (int i = 0; i < this.seedAddressList.size(); i++) {
                String address = this.seedAddressList.get(i);
                byte[] addressByte = AddressTool.getAddress(address);
                Agent agent = new Agent();
                agent.setAgentAddress(addressByte);
                agent.setPackingAddress(addressByte);
                agent.setRewardAddress(addressByte);
                agent.setCreditVal(0);
                agent.setDeposit(BigInteger.ZERO);
                agent.setPubKey(this.seedNodePubKeyList.get(i));
                list.add(agent);
            }
            this.seedAgentList = list;
        }
        return this.seedAgentList;
    }

    public void setSeedAddressList(List<String> seedAddressList) {
        this.seedAddressList = seedAddressList;
    }

    public List<ChangeAgentDepositPo> getAppendDepositList() {
        return appendDepositList;
    }

    public void setAppendDepositList(List<ChangeAgentDepositPo> appendDepositList) {
        this.appendDepositList = appendDepositList;
    }

    public List<ChangeAgentDepositPo> getReduceDepositList() {
        return reduceDepositList;
    }

    public void setReduceDepositList(List<ChangeAgentDepositPo> reduceDepositList) {
        this.reduceDepositList = reduceDepositList;
    }

    public boolean isNetworkStateOk() {
        return networkStateOk;
    }

    public void setNetworkStateOk(boolean networkStateOk) {
        this.networkStateOk = networkStateOk;
    }

    public int getConsensusAgentCountMax() {
        return config.getAgentCountMax() - seedAddressList.size();
    }

    public PubKeyPo getPubKeyPo() {
        return pubKeyPo;
    }

    public void setPubKeyPo(PubKeyPo pubKeyPo) {
        this.pubKeyPo = pubKeyPo;
    }

    public List<byte[]> getSeedNodePubKeyList() {
        return seedNodePubKeyList;
    }

    public void setSeedNodePubKeyList(List<byte[]> seedNodePubKeyList) {
        this.seedNodePubKeyList = seedNodePubKeyList;
    }

    public Set<String> getUnBlockAgentList() {
        return unBlockAgentList;
    }

    public void setUnBlockAgentList(Set<String> unBlockAgentList) {
        this.unBlockAgentList = unBlockAgentList;
    }

    public ConsensusCache getConsensusCache() {
        return consensusCache;
    }

    public void setConsensusCache(ConsensusCache consensusCache) {
        this.consensusCache = consensusCache;
    }
}
