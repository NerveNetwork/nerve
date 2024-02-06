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
     * ROCK DB Database file storage path
     */
    private String dataPath;
    /**
     * modulecode
     */
    private String moduleCode;
    /**
     * Main chainID
     */
    private int mainChainId;
    /**
     * Main asset of the main chainID
     */
    private int mainAssetId;
    /**
     * Trigger the block height for initializing virtual banks
     */
    private long initVirtualBankHeight;
    /**
     * Collection and distribution of public keys for handling fees
     */
    private String feePubkey;

    /**
     * Withdrawal of black hole public key(Share the same public key with setting aliases)
     */
    private String blackHolePublicKey;
    /**
     * Trigger the high cycle of executing virtual bank change transactions allocation
     * according to2One block per second about1Number of blocks produced per day
     */
    private long executeChangeVirtualBankPeriodicHeight;

    /**
     * Total number of consensus nodes in virtual banks（Include seed node members）
     */
    private int virtualBankAgentTotal;

    // version2Abandoned Due to historydataThere is data available, thereforedbExtracting deserialization requires the existence of this attribute
    @Deprecated
    private int virtualBankAgentCountWithoutSeed;

    /**
     * Proposal initiation fee
     */
    private BigInteger proposalPrice;
    /**
     * Duration of proposal voting(Ultimately, it will be calculated as the number of blocks)
     */
    private int proposalVotingDays;

    private int byzantineRatio;

    private BigInteger distributionFee;

    /**
     * The height of the first protocol upgrade Withdrawal fees100
     */
    private long feeEffectiveHeightFirst;

    /**
     * Second protocol upgrade height Withdrawal fees10
     */
    private long feeEffectiveHeightSecond;

    /**
     * All heterogeneous chain multi signature address sets, format(Separate by commas):chainId_1:address_1,chainId_2:address_2
     */
    private String multySignAddressSet;
    /**
     * Is it a heterogeneous chain main network
     */
    private boolean heterogeneousMainNet;
    /**
     * Directly enable the new process of contract heterogeneous chain
     */
    private boolean newProcessorMode;

    /**
     * Heterogeneous Chain Version2Start initializing virtual bank public key
     */
    private String initVirtualBankPubKeyList;

    public boolean isHeterogeneousMainNet() {
        return heterogeneousMainNet;
    }

    public void setHeterogeneousMainNet(boolean heterogeneousMainNet) {
        this.heterogeneousMainNet = heterogeneousMainNet;
    }

    public boolean isNewProcessorMode() {
        return newProcessorMode;
    }

    public void setNewProcessorMode(boolean newProcessorMode) {
        this.newProcessorMode = newProcessorMode;
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

    public String getInitVirtualBankPubKeyList() {
        return initVirtualBankPubKeyList;
    }

    public void setInitVirtualBankPubKeyList(String initVirtualBankPubKeyList) {
        this.initVirtualBankPubKeyList = initVirtualBankPubKeyList;
    }

    public int getVirtualBankAgentTotal() {
        return virtualBankAgentTotal;
    }

    public void setVirtualBankAgentTotal(int virtualBankAgentTotal) {
        this.virtualBankAgentTotal = virtualBankAgentTotal;
    }

    public int getVirtualBankAgentCountWithoutSeed() {
        return virtualBankAgentCountWithoutSeed;
    }

    public void setVirtualBankAgentCountWithoutSeed(int virtualBankAgentCountWithoutSeed) {
        this.virtualBankAgentCountWithoutSeed = virtualBankAgentCountWithoutSeed;
    }
}
