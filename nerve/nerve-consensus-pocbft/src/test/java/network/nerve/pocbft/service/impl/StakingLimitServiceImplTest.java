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
            public boolean addStaking(String key, BigDecimal Amount) {
                return true;
            }

            @Override
            public boolean exitStaking(String key, BigDecimal Amount) {
                return true;
            }

            @Override
            public BigDecimal getAvailableAmount(String key) {
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

    @Test
    public void testValidate() {
        Object[][] args = new Object[][]{
                new Object[]{"测试非硬顶资产", new StackingAsset(5, 1, "", true, 8, "NVT"), BigInteger.valueOf(100000), true},
                new Object[]{"测试超过的情况", new StackingAsset(5, 2, "", true, 18, "NVT"), new BigInteger("100000000000000000000000"), false},
                new Object[]{"测试超过的情况-不同小数位数", new StackingAsset(5, 3, "", true, 8, "NVT"), new BigInteger("10000000000000"), false},
                new Object[]{"合法的情况", new StackingAsset(5, 3, "", true, 18, "NVT"), new BigInteger("1000000000000000000000"), true},
        };
        for (Object[] item : args) {
            boolean result = this.service.validate(chain, (StackingAsset) item[1], (BigInteger) item[2]);
            assertEquals(result, item[3]);
        }

    }
}