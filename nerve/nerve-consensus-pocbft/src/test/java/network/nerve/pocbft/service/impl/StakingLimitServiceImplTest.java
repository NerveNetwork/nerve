package network.nerve.pocbft.service.impl;

import junit.framework.TestCase;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.config.AssetsStakingLimitCfg;
import network.nerve.pocbft.model.bo.config.AssetsType;
import network.nerve.pocbft.model.bo.config.ChainConfig;
import network.nerve.pocbft.service.StakingLimitService;
import network.nerve.pocbft.storage.StakingLimitStorageService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Niels
 */
public class StakingLimitServiceImplTest {

    private StakingLimitService service;
    private Chain chain;

    @Before
    public void init() {
        StakingLimitServiceImpl impl = new StakingLimitServiceImpl();
        impl.setStorageService(new StakingLimitStorageService() {

            @Override
            public boolean addStaking(Chain chain, String key, BigDecimal Amount) {
                return true;
            }

            @Override
            public boolean exitStaking(Chain chain, String key, BigDecimal Amount) {
                return true;
            }

            @Override
            public BigDecimal getStakingAmount(Chain chain, String key) {
                return new BigDecimal(10000);
            }
        });
        service = impl;
        chain = new Chain();
        ChainConfig config = new ChainConfig();
        chain.setConfig(config);

        List<AssetsStakingLimitCfg> limitCfgs = new ArrayList<>();

        AssetsStakingLimitCfg cfg = new AssetsStakingLimitCfg();
        cfg.setKey("USDs");
        cfg.setTotalCount(20000);
        List<AssetsType> list = new ArrayList<>();
        list.add(new AssetsType(5, 2));
        list.add(new AssetsType(5, 3));
        list.add(new AssetsType(5, 4));
        cfg.setAssetsTypeList(list);

        limitCfgs.add(cfg);

        config.setLimitCfgList(limitCfgs);
    }

}