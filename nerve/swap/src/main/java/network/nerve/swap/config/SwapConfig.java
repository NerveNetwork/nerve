package network.nerve.swap.config;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;

import java.io.File;

/**
 * Transaction module setting
 *
 * @author: Loki
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.SWAP)
public class SwapConfig extends ConfigBean implements ModuleConfig {

    private static final long serialVersionUID = 1L;

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
     * 提现黑洞公钥(与设置别名共用一个公钥)
     */
    private String blackHolePublicKey;

    /**
     * 手续费奖励的系统接收地址公钥
     */
    private String awardFeeSystemAddressPublicKey;
    /**
     * 手续费奖励的销毁地址公钥
     */
    private String awardFeeDestructionAddressPublicKey;

    public String getAwardFeeDestructionAddressPublicKey() {
        return awardFeeDestructionAddressPublicKey;
    }

    public void setAwardFeeDestructionAddressPublicKey(String awardFeeDestructionAddressPublicKey) {
        this.awardFeeDestructionAddressPublicKey = awardFeeDestructionAddressPublicKey;
    }

    public String getAwardFeeSystemAddressPublicKey() {
        return awardFeeSystemAddressPublicKey;
    }

    public void setAwardFeeSystemAddressPublicKey(String awardFeeSystemAddressPublicKey) {
        this.awardFeeSystemAddressPublicKey = awardFeeSystemAddressPublicKey;
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

    public String getBlackHolePublicKey() {
        return blackHolePublicKey;
    }

    public void setBlackHolePublicKey(String blackHolePublicKey) {
        this.blackHolePublicKey = blackHolePublicKey;
    }

    public String getPathRoot() {
        return dataPath + File.separator + ModuleE.SW.name;
    }
}
