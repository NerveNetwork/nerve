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
 * 链信息类
 * Chain information class
 *
 * @author tag
 * 2018/12/4
 **/
public class Chain {
    private ConsensusCache consensusCache = new ConsensusCache();

    /**
     * 共识网络是否组好
     * 链接当前共识网络中80%的节点表示共识网络已组好
     */
    private boolean networkStateOk;

    /**
     * 链基础配置信息
     * Chain Foundation Configuration Information
     */
    private ConfigBean config;

    /**
     * 运行状态
     * Chain running state
     */
    private ConsensusStatus consensusStatus;

    /**
     * 已完成区块同步，达到最新高度
     * Chain packing state
     */
    private boolean synchronizedHeight;

    /**
     * 最新区块头
     * The most new block
     */
    private BlockHeader bestHeader;

    /**
     * 节点列表
     * Agent list
     */
    private List<Agent> agentList;

    /**
     * 委托信息列表
     * Deposit list
     */
    private List<Deposit> depositList;

    /**
     * 黄牌列表
     * Yellow punish list
     */
    private List<PunishLogPo> yellowPunishList;

    /**
     * 红牌列表
     * Red punish list
     */
    private List<PunishLogPo> redPunishList;
    /**
     * 是否为共识节点
     * Is it a consensus node
     */
    private boolean consonsusNode;
    /**
     * 记录链出块地址PackingAddress，同一个高度发出了两个不同的块的证据
     * 下一轮正常则清零， 连续3轮将会被红牌惩罚
     * Record the address of each chain out block Packing Address, and the same height gives evidence of two different blocks.
     * The next round of normal will be cleared, and three consecutive rounds will be punished by red cards.
     */
    private Map<String, List<Evidence>> evidenceMap;

    /**
     * 保存本节点需打包的红牌交易,节点打包时需把该集合中所有红牌交易打包并删除
     * To save the red card transactions that need to be packaged by the node,
     * the node should pack and delete all the red card transactions in the set when packing.
     */
    private List<Transaction> redPunishTransactionList;

    /**
     * 缓存最近五轮轮次信息
     * Round list
     */
    private List<MeetingRound> roundList;

    /**
     * 最新200轮区块头
     * The latest 200 rounds block
     */
    private List<BlockHeader> blockHeaderList;

    /**
     * 追加保证金信息列表
     * List of additional margin information
     */
    private List<ChangeAgentDepositPo> appendDepositList;

    /**
     * 减少保证金信息列表
     * Reduced margin information list
     */
    private List<ChangeAgentDepositPo> reduceDepositList;

    private NulsLogger logger;


    /**
     * 种子节点列表
     * Seed node list
     */
    private List<String> seedAddressList;

    /**
     * 线程池
     */
    private ThreadPoolExecutor threadPool;

    private List<byte[]> seedNodePubKeyList;
    private List<Agent> seedAgentList;
    /**
     * 本链未出过块的共识节点地址列表
     */
    private Set<String> unBlockAgentList;

    /**
     * 本链节点出块地址与公钥键值对
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
        Log.error("-=-=-=-=-共识状态设置：{}",consonsusNode);
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
