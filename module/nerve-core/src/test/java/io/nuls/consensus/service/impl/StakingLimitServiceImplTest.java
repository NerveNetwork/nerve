package io.nuls.consensus.service.impl;

import io.nuls.consensus.model.bo.config.AssetsStakingLimitCfg;
import io.nuls.consensus.model.bo.config.AssetsType;
import io.nuls.common.ConfigBean;
import io.nuls.consensus.service.StakingLimitService;
import io.nuls.consensus.storage.StakingLimitStorageService;
import io.nuls.consensus.model.bo.Chain;
import org.junit.Before;

import java.math.BigDecimal;
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
        ConfigBean config = new ConfigBean();
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