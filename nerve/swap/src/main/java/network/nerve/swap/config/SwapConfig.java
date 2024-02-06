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
     * Withdrawal of black hole public key(Share the same public key with setting aliases)
     */
    private String blackHolePublicKey;

    /**
     * System receiving address public key for handling fee rewards
     */
    private String awardFeeSystemAddressPublicKey;
    /**
     * System receiving address public key for handling fee rewards(protocol17take effect)
     */
    private String awardFeeSystemAddressPublicKeyProtocol17;
    /**
     * Destruction address public key for handling fee rewards
     */
    private String awardFeeDestructionAddressPublicKey;
    /**
     * polymerizationstableCombining
     */
    private String stablePairAddressInitialSet;

    public String getAwardFeeSystemAddressPublicKeyProtocol17() {
        return awardFeeSystemAddressPublicKeyProtocol17;
    }

    public void setAwardFeeSystemAddressPublicKeyProtocol17(String awardFeeSystemAddressPublicKeyProtocol17) {
        this.awardFeeSystemAddressPublicKeyProtocol17 = awardFeeSystemAddressPublicKeyProtocol17;
    }

    public String getStablePairAddressInitialSet() {
        return stablePairAddressInitialSet;
    }

    public void setStablePairAddressInitialSet(String stablePairAddressInitialSet) {
        this.stablePairAddressInitialSet = stablePairAddressInitialSet;
    }

    /**
     * Network for reading files[Resave all transaction to address relationships]
     */
    private boolean allPairRelationMainNet;

    public boolean isAllPairRelationMainNet() {
        return allPairRelationMainNet;
    }

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
