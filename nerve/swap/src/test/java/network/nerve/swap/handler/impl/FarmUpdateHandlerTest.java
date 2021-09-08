package network.nerve.swap.handler.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
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
import network.nerve.swap.handler.SwapHandlerConstraints;
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
import network.nerve.swap.model.txdata.FarmUpdateData;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.FarmStakeHelper;
import network.nerve.swap.tx.v1.helpers.FarmUpdateTxHelper;
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
public class FarmUpdateHandlerTest {
    private FarmStakeHandler stakeHandler;
    private FarmWithdrawHandler withdrawHandler;
    private FarmUpdateHandler updateHandler;
    private FarmCacheImpl farmCacher;
    private Chain chain;
    private static String prikeyHex = "2d57d35e370cffda76cada1f478aa877292be096be9a548c799e3f826564c2aa";
    private FarmUserInfoStorageService userInfoStorageService;
    private FarmStorageService farmStorageService;

    @Before
    public void init() {
        stakeHandler = new FarmStakeHandler();
        stakeHandler.setHelper(new FarmStakeHelper());


        withdrawHandler = new FarmWithdrawHandler();
        withdrawHandler.setHelper(new FarmWithdrawHelper());
        updateHandler = new FarmUpdateHandler();
        updateHandler.setHelper(new FarmUpdateTxHelper());

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
        this.farmCacher = new FarmCacheImpl();
        stakeHandler.setChainManager(chainManager);
        stakeHandler.setFarmCacher(farmCacher);
        stakeHandler.getHelper().setFarmCacher(farmCacher);

        withdrawHandler.setChainManager(chainManager);
        withdrawHandler.setFarmCacher(farmCacher);
        withdrawHandler.getHelper().setFarmCacher(farmCacher);


        updateHandler.setChainManager(chainManager);
        updateHandler.setFarmCacher(farmCacher);
        updateHandler.getHelper().setFarmCacher(farmCacher);
        this.farmStorageService = new FarmStorageService() {
            private FarmPoolPO po;
            @Override
            public FarmPoolPO save(int chainId, FarmPoolPO po) {
                this.po = po;
                return po;
            }

            @Override
            public boolean delete(int chainId, byte[] hash) {
                return true;
            }

            @Override
            public List<FarmPoolPO> getList(int chainId) {
                List<FarmPoolPO> list = new ArrayList<>();
                list.add(po);
                return list;
            }
        };
        stakeHandler.getHelper().setStorageService(farmStorageService);
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
        stakeHandler.setUserInfoStorageService(this.userInfoStorageService);

        withdrawHandler.setUserInfoStorageService(userInfoStorageService);
        withdrawHandler.getHelper().setUserInfoStorageService(userInfoStorageService);
    }

    @Test
    public void testExecute() throws IOException {
        //质押NULS获取NVT
        FarmPoolPO po = tempFarm();
        farmCacher.put(po.getFarmHash(), po);

        List<JunitCase> items = new ArrayList<>();

        items.add(getNormalCase());//高度1，质押：10000个
        items.add(getOutAt10001());//高度10001，退出质押5000，应得奖励：100万
        //高度20001，修改每个块奖励为1，退出锁定1年,取出奖励：（1000000000000-500万）
        items.add(changeFarm());
        // 高度30001，领取奖励：应为101万
        items.add(getNormalCase2());
        //高度40001，修改奖励为10
        items.add(changeFarm2());
        //高度50001，退出时间锁定验证
        items.add(getOutAt50001());//高度50001，退出质押4000，应得奖励：11万，锁定多一年


        JunitExecuter<SwapHandlerConstraints> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<SwapHandlerConstraints> junitCase) {
                return junitCase.getObj().execute(9, (Transaction) junitCase.getParams()[0], (long) junitCase.getParams()[1], (long) junitCase.getParams()[2]);
            }
        };
        JunitUtils.execute(items, executer);
    }

    //高度20001，修改每个块奖励为1，退出锁定1年,取出奖励：（1000000000000-500万）
    private JunitCase changeFarm() throws IOException {
        FarmUpdateData txData = new FarmUpdateData();
        txData.setFarmHash(NulsHash.EMPTY_NULS_HASH);
        txData.setChangeType((short) 1);
        txData.setChangeTotalSyrupAmount(BigInteger.valueOf(1000000000000L-5000000));
        txData.setNewSyrupPerBlock(BigInteger.ONE);
        txData.setWithdrawLockTime(365*24*3600);
        Transaction tx1 = TxAssembleUtil.asmbFarmUpdate( txData , HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO po = chain.getBatchInfo().getFarmTempManager().getFarm(NulsHash.EMPTY_NULS_HASH.toHex());
                assertNotNull(result.getSubTx());
                assertEquals(po.getSyrupPerBlock(),BigInteger.ONE);
                assertEquals(po.getAccSyrupPerShare(),BigInteger.valueOf(300000000000000L));
                assertEquals(po.getSyrupTokenBalance(),BigInteger.valueOf(4000000));
                assertEquals(po.getTotalSyrupAmount(),BigInteger.valueOf(5000000));
                assertEquals(po.getWithdrawLockTime(),365*24*3600L);
                //todo金额验证
                CoinData coinData = null;
                try {
                    coinData = result.getSubTx().getCoinDataInstance();
                } catch (NulsException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
                assertEquals(coinData.getFrom().size(), 1);
                for (CoinFrom from : coinData.getFrom()) {
                    if (from.getAssetsChainId() == po.getStakeToken().getChainId() && from.getAssetsId() == po.getStakeToken().getAssetId()) {
                        assertTrue(false);
                    } else if (from.getAssetsChainId() == po.getSyrupToken().getChainId() && from.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        assertEquals(from.getAmount(), txData.getChangeTotalSyrupAmount());
                    }
                }
                farmStorageService.save(9,po);
                System.out.println("[通过]初次update！");

            }
        };
        return new JunitCase("第一次大改", updateHandler, new Object[]{tx1, 20001L, tx1.getTime()}, null, false, null, callback1);
    }
    private JunitCase changeFarm2() throws IOException {
        FarmUpdateData txData = new FarmUpdateData();
        txData.setFarmHash(NulsHash.EMPTY_NULS_HASH);
        txData.setChangeType((short) 0);
        txData.setChangeTotalSyrupAmount(BigInteger.ZERO);
        txData.setNewSyrupPerBlock(BigInteger.TEN);
        txData.setWithdrawLockTime(365*24*3600);
        Transaction tx1 = TxAssembleUtil.asmbFarmUpdate( txData , HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO po = chain.getBatchInfo().getFarmTempManager().getFarm(NulsHash.EMPTY_NULS_HASH.toHex());
                assertNull(result.getSubTx());
                assertEquals(po.getSyrupPerBlock(),BigInteger.TEN);
                assertEquals(po.getAccSyrupPerShare(),BigInteger.valueOf(304000000000000L));
                assertEquals(po.getSyrupTokenBalance(),BigInteger.valueOf(2990000));
                assertEquals(po.getTotalSyrupAmount(),BigInteger.valueOf(5000000));

                farmStorageService.save(9,po);
                System.out.println("[通过]第二次修改！");

            }
        };
        return new JunitCase("第二次修改", updateHandler, new Object[]{tx1, 40001L, tx1.getTime()}, null, false, null, callback1);
    }
    private JunitCase getOutAt10001() throws IOException {
        BigInteger currentAmount = BigInteger.valueOf(10000L);
        BigInteger thisAmount = BigInteger.valueOf(5000L);

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(NulsHash.EMPTY_NULS_HASH, thisAmount, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO po = farmStorageService.getList(9).get(0);
                assertNotNull(result.getSubTx());
                //todo金额验证
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
                        assertEquals(from.getAmount(), thisAmount);
                    } else if (from.getAssetsChainId() == po.getSyrupToken().getChainId() && from.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        reward = BigInteger.valueOf(10000).multiply(po.getSyrupPerBlock()).multiply(SwapConstant.BI_1E12).divide(BigInteger.valueOf(10000)).divide(SwapConstant.BI_1E12).multiply(currentAmount);
                        assertEquals(from.getAmount(), reward);
                    }
                }
                assertNotNull(result.getBusiness());
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertEquals(bus.getAccSyrupPerShareNew().divide(SwapConstant.BI_1E12), reward.divide(currentAmount));
                assertEquals(bus.getLastRewardBlockNew(), 10001);
                assertEquals(bus.getUserAmountNew(), bus.getUserAmountOld().subtract(thisAmount));
                assertEquals(bus.getUserRewardDebtNew(), bus.getAccSyrupPerShareNew().multiply(bus.getUserAmountNew()).divide(SwapConstant.BI_1E12));

                assertEquals(bus.getSyrupBalanceNew(), bus.getSyrupBalanceOld().subtract(reward));
                assertEquals(bus.getStakingBalanceNew(), bus.getStakingBalanceOld().subtract(thisAmount));
                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());
                FarmUserInfoPO user = chain.getBatchInfo().getFarmUserTempManager().getUserInfo(po.getFarmHash(), AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex), 9, BaseConstant.DEFAULT_ADDRESS_TYPE));
                userInfoStorageService.save(9,user);
                farmStorageService.save(9,farm);
                System.out.println("[通过]第一次退出！");

            }
        };
        return new JunitCase("第一次退出", withdrawHandler, new Object[]{tx1, 10001L, tx1.getTime()}, null, false, null, callback1);
    }
    private JunitCase getOutAt50001() throws IOException {
        BigInteger currentAmount = BigInteger.valueOf(5000L);
        BigInteger thisAmount = BigInteger.valueOf(4000L);

        Transaction tx1 = TxAssembleUtil.asmbFarmWithdraw(NulsHash.EMPTY_NULS_HASH, thisAmount, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                FarmPoolPO po = farmStorageService.getList(9).get(0);
                assertNotNull(result.getSubTx());
                //todo金额验证
                CoinData coinData = null;
                try {
                    coinData = result.getSubTx().getCoinDataInstance();
                } catch (NulsException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
                assertEquals(coinData.getFrom().size(), 2);
                BigInteger reward = BigInteger.valueOf(110000L);
                for (CoinTo to : coinData.getTo()) {
                    if (to.getAssetsChainId() == po.getStakeToken().getChainId() && to.getAssetsId() == po.getStakeToken().getAssetId()) {
                        assertEquals(to.getAmount(), thisAmount);
                        assertEquals(to.getLockTime(),tx1.getTime()+365*24*3600);
                    } else if (to.getAssetsChainId() == po.getSyrupToken().getChainId() && to.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        assertEquals(to.getAmount(), reward);
                        assertEquals(to.getLockTime(),0);
                    }
                }
                assertNotNull(result.getBusiness());
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertEquals(bus.getAccSyrupPerShareNew().divide(SwapConstant.BI_1E12),BigInteger.valueOf(302).add(reward.divide(currentAmount)));
                assertEquals(bus.getLastRewardBlockNew(), 50001);
                assertEquals(bus.getUserAmountNew(), bus.getUserAmountOld().subtract(thisAmount));
                assertEquals(bus.getUserRewardDebtNew(), bus.getAccSyrupPerShareNew().multiply(bus.getUserAmountNew()).divide(SwapConstant.BI_1E12));

                assertEquals(bus.getSyrupBalanceNew(), bus.getSyrupBalanceOld().subtract(reward));
                assertEquals(bus.getStakingBalanceNew(), bus.getStakingBalanceOld().subtract(thisAmount));

                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());
                FarmUserInfoPO user = chain.getBatchInfo().getFarmUserTempManager().getUserInfo(po.getFarmHash(), AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex), 9, BaseConstant.DEFAULT_ADDRESS_TYPE));
                userInfoStorageService.save(9,user);
                farmStorageService.save(9,farm);
                System.out.println("[通过]第二次退出！");

            }
        };
        return new JunitCase("第二次退出", withdrawHandler, new Object[]{tx1, 50001L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getNormalCase() throws IOException {
        FarmPoolPO po = farmCacher.get(NulsHash.EMPTY_NULS_HASH);

        BigInteger amount = BigInteger.valueOf(10000L);

        Transaction tx1 = TxAssembleUtil.asmbFarmStake(po.getStakeToken(), po.getFarmHash(), amount, HexUtil.decode(prikeyHex));


        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());

                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());

                assertNotNull(farm);

                assertEquals(farm.getStakeTokenBalance(), amount);

                FarmUserInfoPO user = chain.getBatchInfo().getFarmUserTempManager().getUserInfo(po.getFarmHash(), AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex), 9, BaseConstant.DEFAULT_ADDRESS_TYPE));
                assertNotNull(user);
                assertTrue(user.getAmount().compareTo(amount) == 0);
                assertTrue(user.getRewardDebt().compareTo(BigInteger.ZERO) == 0);


                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertNotNull(bus);
                assertNotNull(bus.getFarmHash());
                assertNotNull(bus.getUserAddress());
                assertTrue(bus.getSyrupBalanceNew().compareTo(bus.getSyrupBalanceOld()) == 0);
                assertTrue(bus.getStakingBalanceNew().subtract(bus.getStakingBalanceOld()).compareTo(amount) == 0);
                assertTrue(bus.getUserAmountNew().subtract(bus.getUserAmountOld()).compareTo(amount) == 0);
                assertTrue(bus.getUserRewardDebtOld().compareTo(bus.getUserRewardDebtNew()) == 0);
                assertTrue(bus.getUserRewardDebtOld().compareTo(BigInteger.ZERO) == 0);
                assertTrue(bus.getAccSyrupPerShareNew().compareTo(BigInteger.ZERO) == 0);

                userInfoStorageService.save(9,user);
                farmStorageService.save(9,farm);
                System.out.println("[通过]初次stake！");
            }
        };
        return new JunitCase("第一次stake", stakeHandler, new Object[]{tx1, 1L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getNormalCase2() throws IOException {
        FarmPoolPO po = farmCacher.get(NulsHash.EMPTY_NULS_HASH);

        BigInteger amount = BigInteger.valueOf(0L);

        Transaction tx1 = TxAssembleUtil.asmbFarmStake(po.getStakeToken(), po.getFarmHash(), amount, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());

                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());

                assertNotNull(farm);

                FarmUserInfoPO user = chain.getBatchInfo().getFarmUserTempManager().getUserInfo(po.getFarmHash(), AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex), 9, BaseConstant.DEFAULT_ADDRESS_TYPE));
                assertNotNull(user);
                assertTrue(user.getAmount().compareTo(BigInteger.valueOf(5000L)) == 0);

                //todo金额验证
                CoinData coinData = null;
                try {
                    coinData = result.getSubTx().getCoinDataInstance();
                } catch (NulsException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
                assertEquals(coinData.getFrom().size(), 1);
                for (CoinFrom from : coinData.getFrom()) {
                    if (from.getAssetsChainId() == po.getStakeToken().getChainId() && from.getAssetsId() == po.getStakeToken().getAssetId()) {
                        assertTrue(false);
                    } else if (from.getAssetsChainId() == po.getSyrupToken().getChainId() && from.getAssetsId() == po.getSyrupToken().getAssetId()) {
                        assertEquals(from.getAmount(), BigInteger.valueOf(1010000L));
                    }
                }
                assertEquals(farm.getAccSyrupPerShare(),BigInteger.valueOf(302000000000000L));

                userInfoStorageService.save(9,user);
                farmStorageService.save(9,farm);
                System.out.println("[通过]领取奖励！");
            }
        };
        return new JunitCase("领取奖励", stakeHandler, new Object[]{tx1, 30001L, tx1.getTime()}, null, false, null, callback1);
    }

    public FarmPoolPO tempFarm() {
        FarmPoolPO po = new FarmPoolPO();
        po.setFarmHash(NulsHash.EMPTY_NULS_HASH);
        po.setAccSyrupPerShare(BigInteger.ZERO);
        po.setSyrupToken(new NerveToken(9, 1));
        po.setLockedTime(1);
        po.setLastRewardBlock(1);
        po.setModifiable(true);
        po.setStakeToken(new NerveToken(1, 1));
        po.setSyrupPerBlock(BigInteger.valueOf(100L));
        po.setSyrupTokenBalance(BigInteger.valueOf(1000000000000L));
        po.setStakeTokenBalance(BigInteger.ZERO);
        po.setTotalSyrupAmount(BigInteger.valueOf(1000000000000L));
        po.setStartBlockHeight(1);
        po.setCreatorAddress(AddressTool.getAddress(ECKey.fromPrivate(HexUtil.decode(prikeyHex)).getPubKey(), 9));

        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(9);
        tempBalanceManager.addTempBalanceForTest(SwapUtils.getFarmAddress(9), BigInteger.valueOf(10000000000000000L), 9, 1);
        tempBalanceManager.addTempBalanceForTest(SwapUtils.getFarmAddress(9), BigInteger.valueOf(10000000000000000L), 1, 1);
        byte[] address = AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex),9,BaseConstant.DEFAULT_ADDRESS_TYPE);
        tempBalanceManager.addTempBalanceForTest(address, BigInteger.valueOf(100000000L), 9, 1);
        tempBalanceManager.addTempBalanceForTest(address, BigInteger.valueOf(100000000L), 1, 1);
        chain.getBatchInfo().setLedgerTempBalanceManager(tempBalanceManager);
        return po;
    }
}