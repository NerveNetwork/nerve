package nerve.network.converter.model.bo;

import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.po.VirtualBankTemporaryChangePO;
import io.nuls.core.log.logback.NulsLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 链的基础数据和运行状态数据
 * Chain information class
 *
 * @author: Chino
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
     * 已调用成功异构链组件的交易，防止2次调用
     * 该缓存会持久化, 并且在数量达到一定阈值时, 清理缓存与持久化
     * K: 交易hash, V:区块所在高度
     */
    private Map<String, Long> mapComponentCalledTx = new HashMap<>();

    /**
     * 最新一次高度的共识节点列表
     */
//    private List<AgentBasic> listLatestAgents = new ArrayList<>();

    /**
     * 统计周期内，需要加入和退出虚拟银行的节点列表
     */
    private VirtualBankTemporaryChangePO virtualBankTemporaryChange;


    /**
     * 启动是否执行向异构组件,注册当前节点信息
     */
    private boolean initLocalSignPriKeyToHeterogeneous = false;
    /*
    在本节点还未提交跨链
    private Map<NulsHash, List<UntreatedMessage>> futureMessageMap;


    private LinkedBlockingQueue<UntreatedMessage> signMessageByzantineQueue;

    */


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

    public Map<String, Long> getMapComponentCalledTx() {
        return mapComponentCalledTx;
    }

    public VirtualBankTemporaryChangePO getVirtualBankTemporaryChange() {
        return virtualBankTemporaryChange;
    }


    public boolean getInitLocalSignPriKeyToHeterogeneous() {
        return initLocalSignPriKeyToHeterogeneous;
    }

    public void setInitLocalSignPriKeyToHeterogeneous(boolean initLocalSignPriKeyToHeterogeneous) {
        this.initLocalSignPriKeyToHeterogeneous = initLocalSignPriKeyToHeterogeneous;
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
                count ++;
            }
        }
        return count;
    }


    /**
     * 根据银行节点签名地址和异构链chanId 获取异构链地址
     * @param sginAddress
     * @param HeterogeneousChainId
     * @return
     */
    public String getDirectorHeterogeneousAddr(String sginAddress, int HeterogeneousChainId){
        VirtualBankDirector virtualBankDirector = this.getMapVirtualBank().get(sginAddress);
        HeterogeneousAddress heterogeneousAddress =
                virtualBankDirector.getHeterogeneousAddrMap().get(HeterogeneousChainId);
        return heterogeneousAddress.getAddress();
    }

    /**
     * 根据异构链地址获取对应节点的奖励地址
     * @param heterogeneousAddress
     * @return
     */
    public String getDirectorRewardAddress(HeterogeneousAddress heterogeneousAddress) {
        for(VirtualBankDirector director : this.getMapVirtualBank().values()){
            HeterogeneousAddress address = director.getHeterogeneousAddrMap().get(heterogeneousAddress.getChainId());
            if(address.equals(heterogeneousAddress)){
                return director.getRewardAddress();
            }
        }
        return null;
    }

  /*    public Map<NulsHash, List<UntreatedMessage>> getFutureMessageMap() {
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
    }*/
}
