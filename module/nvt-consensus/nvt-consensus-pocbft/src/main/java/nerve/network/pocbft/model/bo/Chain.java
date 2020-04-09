package nerve.network.pocbft.model.bo;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import nerve.network.pocbft.model.bo.config.ChainConfig;
import nerve.network.pocbft.model.bo.consensus.Evidence;
import nerve.network.pocbft.model.bo.round.MeetingRound;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.bo.tx.txdata.Deposit;
import nerve.network.pocbft.model.po.ChangeAgentDepositPo;
import nerve.network.pocbft.model.po.PubKeyPo;
import nerve.network.pocbft.model.po.PunishLogPo;
import nerve.network.pocbft.utils.enumeration.ConsensusStatus;
import io.nuls.core.log.logback.NulsLogger;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 链信息类
 * Chain information class
 *
 * @author: Jason
 * 2018/12/4
 **/
public class Chain {
    /**
     * 是否为共识节点
     * Is it a consensus node
     */
    private boolean packer;

    /**
     * 共识网络是否组好
     * 链接当前共识网络中80%的节点表示共识网络已组好
     * */
    private boolean networkState;

    /**
     * 链基础配置信息
     * Chain Foundation Configuration Information
     */
    private ChainConfig config;

    /**
     * 运行状态
     * Chain running state
     */
    private ConsensusStatus consensusStatus;

    /**
     * 打包状态
     * Chain packing state
     */
    private boolean canPacking;

    /**
     * 最新区块头
     * The most new block
     */
    private BlockHeader newestHeader;

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
     * */
    private List<ChangeAgentDepositPo> appendDepositList;

    /**
     * 减少保证金信息列表
     * Reduced margin information list
     * */
    private List<ChangeAgentDepositPo> reduceDepositList;

    private final Lock roundLock = new ReentrantLock();

    private NulsLogger logger;


    /**
     * 种子节点列表
     * Seed node list
     * */
    private List<String> seedNodeList;

    /**
     * 线程池
     * */
    private ThreadPoolExecutor threadPool;

    private List<byte[]> seedNodePubKeyList;

    /**
     * 本链未出过块的共识节点地址列表
     * */
    private List<String> unBlockAgentList;

    /**
     * 本链节点出块地址与公钥键值对
     * */
    private PubKeyPo pubKeyPo;

    public Chain() {
        this.consensusStatus = ConsensusStatus.RUNNING;
        this.canPacking = false;
        this.agentList = new ArrayList<>();
        this.depositList = new ArrayList<>();
        this.yellowPunishList = new ArrayList<>();
        this.redPunishList = new ArrayList<>();
        this.evidenceMap = new HashMap<>();
        this.redPunishTransactionList = new ArrayList<>();
        this.roundList = new ArrayList<>();
        this.packer = false;
        this.seedNodeList = new ArrayList<>();
        this.appendDepositList = new ArrayList<>();
        this.reduceDepositList = new ArrayList<>();
        this.networkState = false;
        this.seedNodePubKeyList = new ArrayList<>();
        this.unBlockAgentList = new ArrayList<>();
    }


    public int getChainId(){
        return config.getChainId();
    }

    public int getAssetId(){
        return config.getAssetId();
    }

    public ChainConfig getConfig() {
        return config;
    }

    public void setConfig(ChainConfig config) {
        this.config = config;
    }

    public ConsensusStatus getConsensusStatus() {
        return consensusStatus;
    }

    public void setConsensusStatus(ConsensusStatus consensusStatus) {
        this.consensusStatus = consensusStatus;
    }

    public boolean isCanPacking() {
        return canPacking;
    }

    public void setCanPacking(boolean canPacking) {
        this.canPacking = canPacking;
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

    public BlockHeader getNewestHeader() {
        return newestHeader;
    }

    public void setNewestHeader(BlockHeader newestHeader) {
        this.newestHeader = newestHeader;
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

    public Lock getRoundLock() {
        return roundLock;
    }

    public boolean isPacker() {
        return packer;
    }

    public void setPacker(boolean packer) {
        this.packer = packer;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public List<String> getSeedNodeList() {
        return seedNodeList;
    }

    public void setSeedNodeList(List<String> seedNodeList) {
        this.seedNodeList = seedNodeList;
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

    public boolean isNetworkState() {
        return networkState;
    }

    public void setNetworkState(boolean networkState) {
        this.networkState = networkState;
    }

    public int getConsensusAgentCountMax(){
        return config.getAgentCountMax() - seedNodeList.size();
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

    public List<String> getUnBlockAgentList() {
        return unBlockAgentList;
    }

    public void setUnBlockAgentList(List<String> unBlockAgentList) {
        this.unBlockAgentList = unBlockAgentList;
    }
}
