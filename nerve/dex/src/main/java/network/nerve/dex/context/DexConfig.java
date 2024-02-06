package network.nerve.dex.context;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.core.annotation.Value;
import io.nuls.core.rpc.model.ModuleE;

import java.io.File;

@Component
@Configuration(domain = ModuleE.Constant.DEX)
public class DexConfig implements ModuleConfig {

    /**
     * internationalization
     */
    private String language;

    @Value("dataPath")
    private String dataPath;

    private int chainId;

    private int assetId;

    private String sysFeeAddress;

    private int sysFeeScale;

    private String createTradingAmount;

    private long cancelConfirmSkipHeight;


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDataFolder() {
        return dataPath + File.separator + ModuleE.DX.name;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getCreateTradingAmount() {
        return createTradingAmount;
    }

    public void setCreateTradingAmount(String createTradingAmount) {
        this.createTradingAmount = createTradingAmount;
    }

    public String getSysFeeAddress() {
        return sysFeeAddress;
    }

    public void setSysFeeAddress(String sysFeeAddress) {
        this.sysFeeAddress = sysFeeAddress;
    }

    public int getSysFeeScale() {
        return sysFeeScale;
    }

    public void setSysFeeScale(int sysFeeScale) {
        this.sysFeeScale = sysFeeScale;
    }

    public long getCancelConfirmSkipHeight() {
        return cancelConfirmSkipHeight;
    }

    public void setCancelConfirmSkipHeight(long cancelConfirmSkipHeight) {
        this.cancelConfirmSkipHeight = cancelConfirmSkipHeight;
    }
}
