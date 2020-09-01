package network.nerve.pocbft.service.impl;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.DoubleUtils;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.config.AssetsStakingLimitCfg;
import network.nerve.pocbft.model.bo.config.AssetsType;
import network.nerve.pocbft.service.StakingLimitService;
import network.nerve.pocbft.storage.StakingLimitStorageService;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Niels
 */
@Component
public class StakingLimitServiceImpl implements StakingLimitService {

    @Autowired
    private StakingLimitStorageService storageService;

    @Override
    public boolean validate(Chain chain, StackingAsset stackingAsset, BigInteger deposit) {
        AssetsStakingLimitCfg cfg = getCfg(chain, stackingAsset);
        if (null == cfg) {
            return true;
        }
        BigDecimal oldAmount = storageService.getAvailableAmount(cfg.getKey());
        BigDecimal realAmount = DoubleUtils.div(new BigDecimal(deposit), Math.pow(10, stackingAsset.getDecimal()));
        if (BigDecimal.valueOf(cfg.getTotalCount()).compareTo(oldAmount.add(realAmount)) < 0) {
            return false;
        }
        return true;
    }

    private AssetsStakingLimitCfg getCfg(Chain chain, StackingAsset stackingAsset) {
        if(null == chain.getConfig().getLimitCfgList()){
            return null;
        }
        for (AssetsStakingLimitCfg cfg : chain.getConfig().getLimitCfgList()) {
            for (AssetsType assetsType : cfg.getAssetsTypeList()) {
                if (stackingAsset.getChainId() == assetsType.getChainId() && stackingAsset.getAssetId() == assetsType.getAssetsId()) {
                    return cfg;
                }
            }
        }
        return null;
    }

    @Override
    public boolean add(Chain chain, StackingAsset stackingAsset, BigInteger deposit) {
        AssetsStakingLimitCfg cfg = getCfg(chain, stackingAsset);
        if (null == cfg) {
            return true;
        }
        BigDecimal realAmount = DoubleUtils.div(new BigDecimal(deposit), Math.pow(10, stackingAsset.getDecimal()));
        return this.storageService.addStaking(cfg.getKey(),realAmount);
    }

    @Override
    public boolean sub(Chain chain, StackingAsset stackingAsset, BigInteger deposit) {
        AssetsStakingLimitCfg cfg = getCfg(chain, stackingAsset);
        if (null == cfg) {
            return true;
        }
        BigDecimal realAmount = DoubleUtils.div(new BigDecimal(deposit), Math.pow(10, stackingAsset.getDecimal()));
        return this.storageService.exitStaking(cfg.getKey(),realAmount);
    }

    public StakingLimitStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(StakingLimitStorageService storageService) {
        this.storageService = storageService;
    }
}
