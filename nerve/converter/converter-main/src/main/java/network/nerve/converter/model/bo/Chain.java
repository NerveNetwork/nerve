package network.nerve.converter.model.bo;

import io.nuls.base.data.NulsHash;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Basic data and operational status data of the chain
 * Chain information class
 *
 * @author: Loki
 * @date: 2019/04/16
 */
public class Chain {

    /**
     * Chain basic configuration information
     * Chain Foundation Configuration Information
     */
    private ConfigBean config;

    /**
     * journal
     */
    private NulsLogger logger;

    /**
     * Is the current node a virtual bank node
     */
    private AtomicBoolean currentIsDirector = new AtomicBoolean(false);

    /**
     * Latest block height and other brief information
     */
    private LatestBasicBlock latestBasicBlock = new LatestBasicBlock();

    /**
     * Current virtual bank members
     * K: Packaging address（Signature address）, V:member object
     */
    private Map<String, VirtualBankDirector> mapVirtualBank = new ConcurrentHashMap<>();

    /**
     * Heterogeneous Chain Configuration
     */
    private List<HeterogeneousCfg> listHeterogeneous = new ArrayList<>();

    /**
     * Pending processing
     */
    private LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = new LinkedBlockingDeque<>();

    /**
     * Pending Proposal
     */
    private LinkedBlockingDeque<ExeProposalPO> exeProposalQueue = new LinkedBlockingDeque<>();

    /**
     * Need to check the transactions of the broadcasthash Is the local heterogeneous chain component generated
     */
    private Set<PendingCheckTx> pendingCheckTxSet = new HashSet<>();

    /**
     * Start whether to execute towards heterogeneous components,Register current node information
     */
    private boolean initLocalSignPriKeyToHeterogeneous = false;

    /**
     * Pending signature set
     * First, receive the transaction signature andhash, And this node has not yet created the transaction, Temporarily store transactions firsthashAnd signature -2
     */
    private Map<NulsHash, List<UntreatedMessage>> futureMessageMap = new ConcurrentHashMap<>();

    /**
     * Signature messages signed by heterogeneous chains but not Byzantine processed
     */
//    private Map<NulsHash, List<ComponentSignMessage>> heterogeneousFutureMessageMap = new ConcurrentHashMap<>();


    /**
     * Unprocessed received transaction signature messages -2
     */
    private LinkedBlockingQueue<UntreatedMessage> signMessageByzantineQueue = new LinkedBlockingQueue<>();

    /**
     * Proposals in voting
     */
    private Map<NulsHash, ProposalPO> votingProposalMap = new HashMap<>();

    /**
     * Heterogeneous chain is executing virtual bank change transactions, Suspend the execution of new virtual bank change transactions
     */
    private AtomicBoolean heterogeneousChangeBankExecuting = new AtomicBoolean(false);

    /**
     * Executing proposal to cancel node bank qualification
     */
    private AtomicBoolean exeDisqualifyBankProposal = new AtomicBoolean(false);

    /**
     * Are you resetting heterogeneous chains(contract)
     */
    private AtomicBoolean resetVirtualBank = new AtomicBoolean(false);

    /**
     * The current version of heterogeneous chain components running
     */
    private int currentHeterogeneousVersion = 1;

    private transient Map<String, Integer> withdrawFeeChanges = new ConcurrentHashMap<>();

    public void increaseWithdrawFeeChangeVersion(String withdrawHash) {
        Integer version = withdrawFeeChanges.getOrDefault(withdrawHash, 0);
        version++;
        withdrawFeeChanges.put(withdrawHash, version);
        logger.info("[increase] withdrawHash: {}, current feeChangeVersion: {}", withdrawHash, version);
    }

    public void decreaseWithdrawFeeChangeVersion(String withdrawHash) {
        Integer version = withdrawFeeChanges.getOrDefault(withdrawHash, 0);
        if (version == 0) {
            return;
        }
        version--;
        withdrawFeeChanges.put(withdrawHash, version);
        logger.info("[decrease] withdrawHash: {}, current feeChangeVersion: {}", withdrawHash, version);
    }

    public int getWithdrawFeeChangeVersion(String withdrawHash) {
        return withdrawFeeChanges.getOrDefault(withdrawHash, 0);
    }

    public void clearWithdrawFeeChange(String withdrawHash) {
        withdrawFeeChanges.remove(withdrawHash);
    }


    public int getCurrentHeterogeneousVersion() {
        return currentHeterogeneousVersion;
    }

    public void setCurrentHeterogeneousVersion(int currentHeterogeneousVersion) {
        this.currentHeterogeneousVersion = currentHeterogeneousVersion;
    }

    public AtomicBoolean getResetVirtualBank() {
        return resetVirtualBank;
    }

    public int getChainId() {
        return config.getChainId();
    }

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public LatestBasicBlock getLatestBasicBlock() {
        return latestBasicBlock;
    }

    public Map<String, VirtualBankDirector> getMapVirtualBank() {
        return mapVirtualBank;
    }

    public void setMapVirtualBank(Map<String, VirtualBankDirector> mapVirtualBank) {
        this.mapVirtualBank = mapVirtualBank;
    }

    public List<HeterogeneousCfg> getListHeterogeneous() {
        return listHeterogeneous;
    }

    public void setListHeterogeneous(List<HeterogeneousCfg> listHeterogeneous) {
        this.listHeterogeneous = listHeterogeneous;
    }

    public LinkedBlockingDeque<TxSubsequentProcessPO> getPendingTxQueue() {
        return pendingTxQueue;
    }


    public boolean getInitLocalSignPriKeyToHeterogeneous() {
        return initLocalSignPriKeyToHeterogeneous;
    }

    public void setInitLocalSignPriKeyToHeterogeneous(boolean initLocalSignPriKeyToHeterogeneous) {
        this.initLocalSignPriKeyToHeterogeneous = initLocalSignPriKeyToHeterogeneous;
    }

    public AtomicBoolean getCurrentIsDirector() {
        return currentIsDirector;
    }

    public Map<NulsHash, ProposalPO> getVotingProposalMap() {
        return votingProposalMap;
    }

    public LinkedBlockingDeque<ExeProposalPO> getExeProposalQueue() {
        return exeProposalQueue;
    }

    public AtomicBoolean getHeterogeneousChangeBankExecuting() {
        return heterogeneousChangeBankExecuting;
    }

    public AtomicBoolean getExeDisqualifyBankProposal() {
        return exeDisqualifyBankProposal;
    }

    /**
     * According to heterogeneitychainId、typeObtain heterogeneous chain configuration information
     *
     * @param chainId
     * @param type
     * @return
     */
    public HeterogeneousCfg getHeterogeneousCfg(int chainId, int type) {
        for (HeterogeneousCfg cfg : listHeterogeneous) {
            if (cfg.getChainId() == chainId && cfg.getType() == type) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Obtain virtual bank members based on node addresses
     * @param agentAddress
     * @return
     */
    public VirtualBankDirector getDirectorByAgent(String agentAddress) {
        for (VirtualBankDirector director : mapVirtualBank.values()) {
            if (director.getAgentAddress().equals(agentAddress)) {
                return director;
            }
        }
        return null;
    }

    /**
     * Determine whether the node is a virtual bank node based on its address
     *
     * @param agentAddress
     * @return
     */
    public boolean isVirtualBankByAgentAddr(String agentAddress) {
        for (VirtualBankDirector director : mapVirtualBank.values()) {
            if (director.getAgentAddress().equals(agentAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Based on the signature address(Packaging address)Determine whether the node is a virtual bank node
     *
     * @param signAdderss
     * @return
     */
    public boolean isVirtualBankBySignAddr(String signAdderss) {
        return mapVirtualBank.containsKey(signAdderss);
    }

    /**
     * Based on the signature address(Packaging address)Determine whether the node is a virtual bank seed node
     *
     * @param signAdderss
     * @return
     */
    public boolean isSeedVirtualBankBySignAddr(String signAdderss) {
        VirtualBankDirector director = mapVirtualBank.get(signAdderss);
        if (director == null) {
            return false;
        }
        return director.getSeedNode();
    }

    /**
     * Obtain the number of non seed nodes for virtual banks
     */
    public int getVirtualBankCountWithoutSeedNode() {
        int count = 0;
        for (VirtualBankDirector director : mapVirtualBank.values()) {
            if (!director.getSeedNode()) {
                count++;
            }
        }
        return count;
    }


    /**
     * Based on bank node addresses and heterogeneous chainschanId Obtain heterogeneous chain addresses
     *
     * @param agentAddress
     * @param heterogeneousChainId
     * @return
     */
    public String getDirectorHeterogeneousAddrByAgentAddr(String agentAddress, int heterogeneousChainId) {
        String address = null;
        for (VirtualBankDirector director : mapVirtualBank.values()) {
            if (director.getAgentAddress().equals(agentAddress)) {
                HeterogeneousAddress heterogeneousAddress =
                        director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                if(null != heterogeneousAddress){
                    address = heterogeneousAddress.getAddress();
                }
            }
        }
        return address;
    }

    /**
     * Based on the signature address of the bank node and the heterogeneous chainchanId Obtain heterogeneous chain addresses
     *
     * @param sginAddress
     * @param heterogeneousChainId
     * @return
     */
    public String getDirectorHeterogeneousAddr(String sginAddress, int heterogeneousChainId) {
        VirtualBankDirector virtualBankDirector = this.getMapVirtualBank().get(sginAddress);
        HeterogeneousAddress heterogeneousAddress =
                virtualBankDirector.getHeterogeneousAddrMap().get(heterogeneousChainId);
        return heterogeneousAddress.getAddress();
    }

    /**
     * Obtain reward addresses for corresponding nodes based on heterogeneous chain addresses
     *
     * @param heterogeneousAddress
     * @return
     */
    public String getDirectorRewardAddress(HeterogeneousAddress heterogeneousAddress) {
        for (VirtualBankDirector director : this.getMapVirtualBank().values()) {
            HeterogeneousAddress address = director.getHeterogeneousAddrMap().get(heterogeneousAddress.getChainId());
            if (address.equals(heterogeneousAddress)) {
                return director.getRewardAddress();
            }
        }
        return null;
    }

    public Set<PendingCheckTx> getPendingCheckTxSet() {
        return pendingCheckTxSet;
    }

    public Map<NulsHash, List<UntreatedMessage>> getFutureMessageMap() {
        return futureMessageMap;
    }

    public void setFutureMessageMap(Map<NulsHash, List<UntreatedMessage>> futureMessageMap) {
        this.futureMessageMap = futureMessageMap;
    }

//    public Map<NulsHash, List<ComponentSignMessage>> getHeterogeneousFutureMessageMap() {
//        return heterogeneousFutureMessageMap;
//    }

    public LinkedBlockingQueue<UntreatedMessage> getSignMessageByzantineQueue() {
        return signMessageByzantineQueue;
    }

    public void setSignMessageByzantineQueue(LinkedBlockingQueue<UntreatedMessage> signMessageByzantineQueue) {
        this.signMessageByzantineQueue = signMessageByzantineQueue;
    }
}
