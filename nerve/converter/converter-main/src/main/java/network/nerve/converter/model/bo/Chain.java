package network.nerve.converter.model.bo;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 链的基础数据和运行状态数据
 * Chain information class
 *
 * @author: Loki
 * @date: 2019/04/16
 */
public class Chain {

    /**
     * 链基础配置信息
     * Chain Foundation Configuration Information
     */
    private ConfigBean config;

    /**
     * 日志
     */
    private NulsLogger logger;

    /**
     * 当前节点是否是虚拟银行节点
     */
    private AtomicBoolean currentIsDirector = new AtomicBoolean(false);

    /**
     * 最新区块高度等简略信息
     */
    private LatestBasicBlock latestBasicBlock = new LatestBasicBlock();

    /**
     * 当前虚拟银行成员
     * K: 打包地址（签名地址）, V:成员对象
     */
    private Map<String, VirtualBankDirector> mapVirtualBank = new ConcurrentHashMap<>();

    /**
     * 异构链配置
     */
    private List<HeterogeneousCfg> listHeterogeneous = new ArrayList<>();

    /**
     * 待处理
     */
    private LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = new LinkedBlockingDeque<>();

    /**
     * 待执行提案
     */
    private LinkedBlockingDeque<ExeProposalPO> exeProposalQueue = new LinkedBlockingDeque<>();

    /**
     * 需要检查广播的交易hash 本地异构链组件是否生成
     */
    private Set<PendingCheckTx> pendingCheckTxSet = new HashSet<>();

    /**
     * 启动是否执行向异构组件,注册当前节点信息
     */
    private boolean initLocalSignPriKeyToHeterogeneous = false;

    /**
     * 待处理签名集合
     * 先收到交易签名和hash, 而本节点还没有创建该交易, 先暂存交易hash和签名 -2
     */
    private Map<NulsHash, List<UntreatedMessage>> futureMessageMap = new ConcurrentHashMap<>();


    /**
     * 未处理的收到的交易签名消息 -2
     */
    private LinkedBlockingQueue<UntreatedMessage> signMessageByzantineQueue = new LinkedBlockingQueue<>();

    /**
     * 投票中的提案
     */
    private Map<NulsHash, ProposalPO> votingProposalMap = new HashMap<>();

    /**
     * 异构链正在执行虚拟银行变更交易, 暂停执行新的虚拟银行变更交易
     */
    private AtomicBoolean heterogeneousChangeBankExecuting = new AtomicBoolean(false);

    /**
     * 正在执行取消节点银行资格的提案
     */
    private AtomicBoolean exeDisqualifyBankProposal = new AtomicBoolean(false);

    /**
     * 是否正在重置异构链(合约)
     */
    private AtomicBoolean resetVirtualBank = new AtomicBoolean(false);

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
     * 根据异构chainId、type获取异构链配置信息
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
     * 根据节点地址获取虚拟银行成员
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
     * 根据节点地址判断该节点是否是虚拟银行节点
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
     * 根据签名地址(打包地址)判断该节点是否是虚拟银行节点
     *
     * @param signAdderss
     * @return
     */
    public boolean isVirtualBankBySignAddr(String signAdderss) {
        return mapVirtualBank.containsKey(signAdderss);
    }

    /**
     * 获取虚拟银行非种子节点的数量
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
     * 根据银行节点地址和异构链chanId 获取异构链地址
     *
     * @param agentAddress
     * @param heterogeneousChainId
     * @return
     */
    public String getDirectorHeterogeneousAddrByAgentAddr(String agentAddress, int heterogeneousChainId) throws NulsException {
        for (VirtualBankDirector director : mapVirtualBank.values()) {
            if (director.getAgentAddress().equals(agentAddress)) {
                HeterogeneousAddress heterogeneousAddress =
                        director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                return heterogeneousAddress.getAddress();
            }
        }
        logger.error("没有获取到虚拟银行节点的异构链地址 " +
                        "can not get heterogeneous address by agent address and heterogeneousChainId. " +
                        "- HeterogeneousChainId:{} agentAddress:{}",
                heterogeneousChainId, agentAddress);
        throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
    }

    /**
     * 根据银行节点签名地址和异构链chanId 获取异构链地址
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
     * 根据异构链地址获取对应节点的奖励地址
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

    public LinkedBlockingQueue<UntreatedMessage> getSignMessageByzantineQueue() {
        return signMessageByzantineQueue;
    }

    public void setSignMessageByzantineQueue(LinkedBlockingQueue<UntreatedMessage> signMessageByzantineQueue) {
        this.signMessageByzantineQueue = signMessageByzantineQueue;
    }
}
