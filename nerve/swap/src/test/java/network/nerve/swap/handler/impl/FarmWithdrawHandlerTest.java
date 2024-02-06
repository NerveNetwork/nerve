package network.nerve.swap.handler.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.impl.FarmCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.FarmTempManager;
import network.nerve.swap.manager.FarmUserInfoTempManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.FarmBus;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.FarmWithdrawHelper;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;
import network.nerve.swap.utils.TxAssembleUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Niels
 */
public class FarmWithdrawHandlerTest {
    private FarmWithdrawHandler handler;
    private FarmCacheImpl farmCacher;
    private Chain chain;
    private static String prikeyHex = "2d57d35e370cffda76cada1f478aa877292be096be9a548c799e3f826564c2aa";
    private FarmUserInfoStorageService userInfoStorageService;

    @Before
    public void init() {
        handler = new FarmWithdrawHandler();
        handler.setHelper(new FarmWithdrawHelper());

        ChainManager chainManager = new ChainManager();
        chain = new Chain();
        chain.setLogger(LoggerBuilder.getLogger(ModuleE.SW.name, 9));
        Chain.putCurrentThreadBlockType(0);
        chain.setBatchInfo(new BatchInfo());
        chain.getBatchInfo().setFarmTempManager(new FarmTempManager());
        chain.getBatchInfo().setFarmUserTempManager(new FarmUserInfoTempManager());
        ConfigBean cfg = new ConfigBean();
        cfg.setChainId(9);
        chain.setConfig(cfg);
        chainManager.getChainMap().put(9, chain);
        handler.setChainManager(chainManager);
        this.farmCacher = new FarmCacheImpl();
        handler.setFarmCacher(farmCacher);
        handler.getHelper().setFarmCacher(farmCacher);
//        handler.getHelper().setLedgerService(new LedgerService() {
//            @Override
//            public boolean existNerveAsset(int chainId, int assetChainId, int assetId) throws NulsException {
//                return true;
//            }
//        });
        handler.getHelper().setStorageService(new FarmStorageService() {


            @Override
            public FarmPoolPO save(int chainId, FarmPoolPO po) {
                return po;
            }

            @Override
            public boolean delete(int chainId, byte[] hash) {
                return true;
            }

            @Override
            public List<FarmPoolPO> getList(int chainId) {
                return new ArrayList<>();
            }
        });
        this.userInfoStorageService = new FarmUserInfoStorageService() {
            private FarmUserInfoPO po;

            @Override
            public FarmUserInfoPO save(int chainId, FarmUserInfoPO po) {
                this.po = po;
                return po;
            }

            @Override
            public boolean delete(int chainId, NulsHash farmHash, byte[] address) {
                return true;
            }

            @Override
            public FarmUserInfoPO load(int chainId, NulsHash farmHash, byte[] address) {
                return loadByKey(chainId, ArraysTool.concatenate(farmHash.getBytes(), address));
            }

            @Override
            public FarmUserInfoPO loadByKey(int chainId, byte[] key) {
                return po;
            }
        };
        handler.setUserInfoStorageService(userInfoStorageService);
        handler.getHelper().setUserInfoStorageService(userInfoStorageService);
    }

    @Test
    public void testExecute() throws IOException {
        List<JunitCase> items = new ArrayList<>();

        items.add(getCase1());
        items.add(getNormalCase());
        items.add(getCase2());
        items.add(getCase3());


        JunitExecuter<FarmWithdrawHandler> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<FarmWithdrawHandler> junitCase) {
                return junitCase.getObj().execute(9, (Transaction) junitCase.getParams()[0], (long) junitCase.getParams()[1], (long) junitCase.getParams()[2]);
            }
        };
        JunitUtils.execute(items, executer);
    }

    private JunitCase getCase2() throws IOException {
        FarmPoolPO po = tempFarm();

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(po.getFarmHash(), BigInteger.ONE, HexUtil.decode(prikeyHex));
        BigInteger currentAmount = BigInteger.valueOf(999);
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                assertNotNull(result.getSubTx());
                //todoAmount verification
                CoinData coinData = null;
                try {
                    coinData = result.getSubTx().getCoinDataInstance();
                } catch (NulsException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
                assertEquals(coinData.getFrom().size(), 2);
                assertNotNull(result.getBusiness());
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                BigInteger reward = null;
                for (CoinFrom from : coinData.getFrom()) {
                    if (from.getAssetsChainId() == po.getStakeToken().getChainId() && from.getAssetsId() == po.getStakeToken().getAssetId()) {
                        assertEquals(from.getAmount(), BigInteger.ONE);
                    } else if (from.getAssetsChainId() == po.getSyrupToken().getChainId() && from.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        BigInteger accPerShare = bus.getAccSyrupPerShareOld().add(BigInteger.valueOf(10000).multiply(po.getSyrupPerBlock()).multiply(SwapConstant.BI_1E12).divide(BigInteger.valueOf(9999)));
                        reward = accPerShare.multiply(currentAmount).divide(SwapConstant.BI_1E12).subtract(bus.getUserRewardDebtOld());
                        assertEquals(from.getAmount(), reward);
                    }
                }
                assertEquals(bus.getLastRewardBlockNew(), 20001);
                assertEquals(bus.getUserAmountNew(), bus.getUserAmountOld().subtract(BigInteger.ONE));
                assertEquals(bus.getUserRewardDebtNew(), bus.getAccSyrupPerShareNew().multiply(bus.getUserAmountNew()).divide(SwapConstant.BI_1E12));


                assertEquals(bus.getSyrupBalanceNew(), bus.getSyrupBalanceOld().subtract(reward));
                assertEquals(bus.getStakingBalanceNew(), bus.getStakingBalanceOld().subtract(BigInteger.ONE));
                System.out.println("【adopt】Test Farm-Withdraw tx execute: Second Exit Exit！");

            }
        };
        return new JunitCase("Second Exit Exit", handler, new Object[]{tx1, 20001L, tx1.getTime()}, null, false, null, callback1);
    }
    private JunitCase getCase3() throws IOException {
        FarmPoolPO po = tempFarm();

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(po.getFarmHash(), BigInteger.valueOf(10000), HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                assertNull(result.getSubTx());

                System.out.println("【adopt】Test Farm-Withdraw tx execute: Excess exit！");

            }
        };
        return new JunitCase("Excess exit", handler, new Object[]{tx1, 30001L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getCase1() throws IOException {
        ECKey ecKey = new ECKey();
        FarmPoolPO po = tempFarm();
        po.setLockedTime(System.currentTimeMillis() / 1000 + 10);
        po.setFarmHash(NulsHash.calcHash(new byte[]{1, 1, 1, 1}));
        farmCacher.put(po.getFarmHash(), po);
        BigInteger currentAmount = BigInteger.valueOf(1000L);
        FarmUserInfoPO userPo = new FarmUserInfoPO();
        userPo.setFarmHash(po.getFarmHash());
        userPo.setUserAddress(AddressTool.getAddress(ecKey.getPubKey(), 9));
        userPo.setRewardDebt(BigInteger.ZERO);
        userPo.setAmount(currentAmount);

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(po.getFarmHash(), BigInteger.ONE, ecKey.getPrivKeyBytes());
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                assertNull(result.getSubTx());

                System.out.println("【adopt】Test Farm-Withdraw tx execute: Not unlocked yet！");

            }
        };
        return new JunitCase("Not unlocked yet", handler, new Object[]{tx1, 3001L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getNormalCase() throws IOException {
        FarmPoolPO po = tempFarm();
        farmCacher.put(po.getFarmHash(), po);
        BigInteger currentAmount = BigInteger.valueOf(1000L);
        FarmUserInfoPO userPo = new FarmUserInfoPO();
        userPo.setFarmHash(po.getFarmHash());
        userPo.setUserAddress(AddressTool.getAddress(ECKey.fromPrivate(HexUtil.decode(prikeyHex)).getPubKey(), 9));
        userPo.setRewardDebt(BigInteger.ZERO);
        userPo.setAmount(currentAmount);
        this.userInfoStorageService.save(9, userPo);

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(po.getFarmHash(), BigInteger.ONE, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                assertNotNull(result.getSubTx());
                //todoAmount verification
                CoinData coinData = null;
                try {
                    coinData = result.getSubTx().getCoinDataInstance();
                } catch (NulsException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
                assertEquals(coinData.getFrom().size(), 2);
                BigInteger reward = null;
                for (CoinFrom from : coinData.getFrom()) {
                    if (from.getAssetsChainId() == po.getStakeToken().getChainId() && from.getAssetsId() == po.getStakeToken().getAssetId()) {
                        assertEquals(from.getAmount(), BigInteger.ONE);
                    } else if (from.getAssetsChainId() == po.getSyrupToken().getChainId() && from.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        reward = BigInteger.valueOf(10000).multiply(po.getSyrupPerBlock()).multiply(SwapConstant.BI_1E12).divide(po.getStakeTokenBalance()).divide(SwapConstant.BI_1E12).multiply(currentAmount);
                        assertEquals(from.getAmount(), reward);
                    }
                }
                assertNotNull(result.getBusiness());
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertEquals(bus.getAccSyrupPerShareNew().divide(SwapConstant.BI_1E12), reward.divide(currentAmount));
                assertEquals(bus.getLastRewardBlockNew(), 10001);
                assertEquals(bus.getUserAmountNew(), bus.getUserAmountOld().subtract(BigInteger.ONE));
                assertEquals(bus.getUserRewardDebtNew(), bus.getAccSyrupPerShareNew().multiply(bus.getUserAmountNew()).divide(SwapConstant.BI_1E12));


                assertEquals(bus.getSyrupBalanceNew(), bus.getSyrupBalanceOld().subtract(reward));
                assertEquals(bus.getStakingBalanceNew(), bus.getStakingBalanceOld().subtract(BigInteger.ONE));
                System.out.println("【adopt】Test Farm-Withdraw tx execute: Normal process！");

            }
        };
        return new JunitCase("First timestake", handler, new Object[]{tx1, 10001L, tx1.getTime()}, null, false, null, callback1);
    }

    public FarmPoolPO tempFarm() {
        FarmPoolPO po = new FarmPoolPO();
        po.setFarmHash(NulsHash.EMPTY_NULS_HASH);
        po.setAccSyrupPerShare(BigInteger.ZERO);
        po.setSyrupToken(new NerveToken(9, 1));
        po.setLockedTime(9000);
        po.setLastRewardBlock(1);
        po.setStakeToken(new NerveToken(1, 1));
        po.setSyrupPerBlock(BigInteger.valueOf(1000L));
        po.setSyrupTokenBalance(BigInteger.valueOf(1000000000000L));
        po.setStakeTokenBalance(BigInteger.valueOf(10000L));
        po.setTotalSyrupAmount(BigInteger.valueOf(1000000000000L));
        po.setStartBlockHeight(1);
        po.setCreatorAddress(AddressTool.getAddress(ECKey.fromPrivate(HexUtil.decode(prikeyHex)).getPubKey(), 9));

        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(9);
        tempBalanceManager.addTempBalanceForTest(SwapUtils.getFarmAddress(9), BigInteger.valueOf(10000000000000000L), 9, 1);
        tempBalanceManager.addTempBalanceForTest(SwapUtils.getFarmAddress(9), BigInteger.valueOf(10000000000000000L), 1, 1);
        byte[] address = AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex),9, BaseConstant.DEFAULT_ADDRESS_TYPE);
        tempBalanceManager.addTempBalanceForTest(address, BigInteger.valueOf(100000000L), 9, 1);
        tempBalanceManager.addTempBalanceForTest(address, BigInteger.valueOf(100000000L), 1, 1);
        chain.getBatchInfo().setLedgerTempBalanceManager(tempBalanceManager);
        return po;
    }
}
