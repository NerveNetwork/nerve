package network.nerve.constant;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.model.bo.config.ConfigBean;

import java.io.File;
import java.util.Set;

/**
 * 跨链模块配置类
 * @author tag
 * @date 2019-03-26
 * */
@Component
@Configuration(domain = ModuleE.Constant.CROSS_CHAIN)
public class NulsCrossChainConfig extends ConfigBean implements ModuleConfig {

    private String dataPath;

    /** 模块code*/
    private String moduleCode;

    /** 主链链ID*/
    private int mainChainId;

    /** 主链主资产ID*/
    private int mainAssetId;

    /** 语言*/
    private String language;

    /** 编码*/
    private String encoding;

    private int crossCtxType;

    private boolean mainNet;

    /**默认链接到的跨链节点*/
    private String crossSeedIps;
    /**
     * 本链种子节点地址
     */
    private Set<String> seedNodeSet;

    private Long version1_6_0_height;

    public Long getVersion1_6_0_height() {
        return version1_6_0_height;
    }

    public void setVersion1_6_0_height(Long version1_6_0_height) {
        this.version1_6_0_height = version1_6_0_height;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDataFolder() {
        return dataPath + File.separator + ModuleE.CC.name;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public int getCrossCtxType() {
        return crossCtxType;
    }

    public void setCrossCtxType(int crossCtxType) {
        this.crossCtxType = crossCtxType;
    }

    public boolean isMainNet() {
        return mainNet;
    }

    public void setMainNet(boolean mainNet) {
        this.mainNet = mainNet;
    }

    public String getCrossSeedIps() {
        return crossSeedIps;
    }

    public void setCrossSeedIps(String crossSeedIps) {
        this.crossSeedIps = crossSeedIps;
    }

    public Set<String> getSeedNodeSet() {
        return seedNodeSet;
    }

    public void setSeedNodeSet(Set<String> seedNodeSet) {
        this.seedNodeSet = seedNodeSet;
    }
}
