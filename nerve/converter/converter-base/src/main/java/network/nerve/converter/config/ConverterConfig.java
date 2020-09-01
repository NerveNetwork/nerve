package network.nerve.converter.config;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.model.bo.ConfigBean;

import java.io.File;
import java.math.BigInteger;

/**
 * Transaction module setting
 *
 * @author: Loki
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.CONVERTER)
public class ConverterConfig extends ConfigBean implements ModuleConfig {

    /**
     * ROCK DB 数据库文件存储路径
     */
    private String dataPath;
    /**
     * 模块code
     */
    private String moduleCode;
    /**
     * 主链链ID
     */
    private int mainChainId;
    /**
     * 主链主资产ID
     */
    private int mainAssetId;
    /**
     * 触发初始化虚拟银行的区块高度
     */
    private long initVirtualBankHeight;
    /**
     * 手续费汇集分发公钥
     */
    private String feePubkey;

    /**
     * 提现黑洞公钥(与设置别名共用一个公钥)
     */
    private String blackHolePublicKey;
    /**
     * 触发执行虚拟银行变更交易的高度周期 配置
     * 按2秒一个块 大约1天的出块数量
     */
    private long executeChangeVirtualBankPeriodicHeight;
    /**
     * 虚拟银行共识节点总数（非种子节点）
     */
    private int virtualBankAgentCountWithoutSeed;

    /**
     * 发起提案费用
     */
    private BigInteger proposalPrice;
    /**
     * 提案投票持续天数(最终会计算成出块数)
     */
    private int proposalVotingDays;

    private int byzantineRatio;

    private BigInteger distributionFee;

    /**
     * 第一次协议升级高度 提现手续费100
     */
    private long feeEffectiveHeightFirst;

    /**
     * 第二次协议升级高度 提现手续费10
     */
    private long feeEffectiveHeightSecond;

    /**
     * 所有异构链多签地址集合, 格式(以逗号隔开):chainId_1:address_1,chainId_2:address_2
     */
    private String multySignAddressSet;
    /**
     * 是否异构链主网
     */
    private boolean heterogeneousMainNet;

    public boolean isHeterogeneousMainNet() {
        return heterogeneousMainNet;
    }

    public void setHeterogeneousMainNet(boolean heterogeneousMainNet) {
        this.heterogeneousMainNet = heterogeneousMainNet;
    }

    public String getTxDataRoot() {
        return dataPath + File.separator + ModuleE.CV.name;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public int getMainChainId() {
        return mainChainId;
    }

    public void setMainChainId(int mainChainId) {
        this.mainChainId = mainChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }

    public long getExecuteChangeVirtualBankPeriodicHeight() {
        return executeChangeVirtualBankPeriodicHeight;
    }

    public void setExecuteChangeVirtualBankPeriodicHeight(long executeChangeVirtualBankPeriodicHeight) {
        this.executeChangeVirtualBankPeriodicHeight = executeChangeVirtualBankPeriodicHeight;
    }

    public String getFeePubkey() {
        return feePubkey;
    }

    public void setFeePubkey(String feePubkey) {
        this.feePubkey = feePubkey;
    }

    public long getInitVirtualBankHeight() {
        return initVirtualBankHeight;
    }

    public void setInitVirtualBankHeight(long initVirtualBankHeight) {
        this.initVirtualBankHeight = initVirtualBankHeight;
    }

    public int getVirtualBankAgentCountWithoutSeed() {
        return virtualBankAgentCountWithoutSeed;
    }

    public void setVirtualBankAgentCountWithoutSeed(int virtualBankAgentCountWithoutSeed) {
        this.virtualBankAgentCountWithoutSeed = virtualBankAgentCountWithoutSeed;
    }

    public BigInteger getDistributionFee() {
        return distributionFee;
    }

    public void setDistributionFee(BigInteger distributionFee) {
        this.distributionFee = distributionFee;
    }

    public BigInteger getProposalPrice() {
        return proposalPrice;
    }

    public void setProposalPrice(BigInteger proposalPrice) {
        this.proposalPrice = proposalPrice;
    }

    public int getByzantineRatio() {
        return byzantineRatio;
    }

    public void setByzantineRatio(int byzantineRatio) {
        this.byzantineRatio = byzantineRatio;
    }

    public int getProposalVotingDays() {
        return proposalVotingDays;
    }

    public void setProposalVotingDays(int proposalVotingDays) {
        this.proposalVotingDays = proposalVotingDays;
    }

    public String getBlackHolePublicKey() {
        return blackHolePublicKey;
    }

    public void setBlackHolePublicKey(String blackHolePublicKey) {
        this.blackHolePublicKey = blackHolePublicKey;
    }

    public String getMultySignAddressSet() {
        return multySignAddressSet;
    }

    public void setMultySignAddressSet(String multySignAddressSet) {
        this.multySignAddressSet = multySignAddressSet;
    }

    public long getFeeEffectiveHeightFirst() {
        return feeEffectiveHeightFirst;
    }

    public void setFeeEffectiveHeightFirst(long feeEffectiveHeightFirst) {
        this.feeEffectiveHeightFirst = feeEffectiveHeightFirst;
    }

    public long getFeeEffectiveHeightSecond() {
        return feeEffectiveHeightSecond;
    }

    public void setFeeEffectiveHeightSecond(long feeEffectiveHeightSecond) {
        this.feeEffectiveHeightSecond = feeEffectiveHeightSecond;
    }
}
