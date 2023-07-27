package io.nuls.consensus.service.impl;

import io.nuls.consensus.storage.StakingLimitStorageService;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.DoubleUtils;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.config.AssetsStakingLimitCfg;
import io.nuls.consensus.model.bo.config.AssetsType;
import io.nuls.consensus.service.StakingLimitService;

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
        BigDecimal oldAmount = storageService.getStakingAmount(chain,cfg.getKey());
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
        return this.storageService.addStaking(chain,cfg.getKey(),realAmount);
    }

    @Override
    public boolean sub(Chain chain, StackingAsset stackingAsset, BigInteger deposit) {
        AssetsStakingLimitCfg cfg = getCfg(chain, stackingAsset);
        if (null == cfg) {
            return true;
        }
        BigDecimal realAmount = DoubleUtils.div(new BigDecimal(deposit), Math.pow(10, stackingAsset.getDecimal()));
        return this.storageService.exitStaking(chain,cfg.getKey(),realAmount);
    }

    public StakingLimitStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(StakingLimitStorageService storageService) {
        this.storageService = storageService;
    }
}
