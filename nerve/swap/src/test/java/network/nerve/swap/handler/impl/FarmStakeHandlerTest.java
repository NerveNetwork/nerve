package network.nerve.swap.handler.impl;

import io.nuls.base.basic.AddressTool;
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
import network.nerve.swap.tx.v1.helpers.FarmStakeHelper;
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
public class FarmStakeHandlerTest {
    private FarmStakeHandler handler;
    private FarmCacheImpl farmCacher;
    private Chain chain;
    private static String prikeyHex = "2d57d35e370cffda76cada1f478aa877292be096be9a548c799e3f826564c2aa";

    @Before
    public void init() {
        handler = new FarmStakeHandler();
        handler.setHelper(new FarmStakeHelper());

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
        handler.setUserInfoStorageService(new FarmUserInfoStorageService() {
            @Override
            public FarmUserInfoPO save(int chainId, FarmUserInfoPO po) {
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
                return null;
            }
        });
    }

    @Test
    public void testExecute() throws IOException {

        List<JunitCase> items = new ArrayList<>();

        items.add(getNormalCase());
        items.add(getCase0());
        items.add(getCase1());
//        items.add(getCase2());
//        items.add(getCase3());


        JunitExecuter<FarmStakeHandler> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<FarmStakeHandler> junitCase) {
                return junitCase.getObj().execute(9, (Transaction) junitCase.getParams()[0], (long) junitCase.getParams()[1], (long) junitCase.getParams()[2]);
            }
        };
        JunitUtils.execute(items, executer);
    }

    private JunitCase getNormalCase() throws IOException {
        FarmPoolPO po = tempFarm();
        farmCacher.put(po.getFarmHash(), po);

        BigInteger amount = BigInteger.valueOf(1000L);

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


                System.out.println("[通过]Test Farm-Stake tx execute: 初次stake！");
            }
        };
        return new JunitCase("第一次stake", handler, new Object[]{tx1, 1L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getCase0() throws IOException {
        FarmPoolPO po = tempFarm();
        farmCacher.put(po.getFarmHash(), po);

        BigInteger amount = BigInteger.valueOf(1000L);

        Transaction tx1 = TxAssembleUtil.asmbFarmStake(po.getStakeToken(), po.getFarmHash(), amount, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws NulsException {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());

                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());

                assertNotNull(farm);
                BigInteger reward = BigInteger.valueOf(10000L);
                assertEquals(farm.getStakeTokenBalance(), amount.multiply(BigInteger.TWO));
                assertNotNull(result.getSubTx());
                assertTrue(result.getSubTx().getCoinDataInstance().getTo().get(0).getAmount().compareTo(reward) == 0);

                FarmUserInfoPO user = chain.getBatchInfo().getFarmUserTempManager().getUserInfo(po.getFarmHash(), AddressTool.getAddressByPrikey(HexUtil.decode(prikeyHex), 9, BaseConstant.DEFAULT_ADDRESS_TYPE));
                assertNotNull(user);
                assertTrue(user.getAmount().compareTo(amount.multiply(BigInteger.TWO)) == 0);
                assertTrue(user.getRewardDebt().compareTo(BigInteger.valueOf(20000)) == 0);


                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertNotNull(bus);
                assertNotNull(bus.getFarmHash());
                assertNotNull(bus.getUserAddress());
                assertTrue(bus.getSyrupBalanceOld().subtract(bus.getSyrupBalanceNew()).compareTo(reward) == 0);
                assertTrue(bus.getStakingBalanceNew().subtract(bus.getStakingBalanceOld()).compareTo(amount) == 0);
                assertTrue(bus.getUserAmountNew().subtract(bus.getUserAmountOld()).compareTo(amount) == 0);
                assertTrue(bus.getUserRewardDebtNew().compareTo(BigInteger.valueOf(20000)) == 0);
                assertTrue(bus.getUserRewardDebtOld().compareTo(BigInteger.ZERO) == 0);

                assertTrue(bus.getAccSyrupPerShareNew().compareTo(BigInteger.TEN.multiply(SwapConstant.BI_1E12)) == 0);
                System.out.println("[通过]Test Farm-Stake tx execute: 二次stake！");
            }
        };
        return new JunitCase("第二次stake", handler, new Object[]{tx1, 101L, tx1.getTime()}, null, false, null, callback1);
    }

    private JunitCase getCase1() throws IOException {
        FarmPoolPO po = tempFarm();
        farmCacher.put(po.getFarmHash(), po);


        Transaction tx1 = TxAssembleUtil.asmbFarmStake(po.getStakeToken(), po.getFarmHash(), BigInteger.ZERO, HexUtil.decode(prikeyHex));
        NerveCallback<SwapResult> callback1 = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws NulsException {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());

                FarmPoolPO farm = chain.getBatchInfo().getFarmTempManager().getFarm(po.getFarmHash().toHex());

                assertNotNull(farm);
                BigInteger reward = BigInteger.valueOf(10000L);
                assertEquals(farm.getStakeTokenBalance(), BigInteger.valueOf(2000L));
                assertNotNull(result.getSubTx());
                assertTrue(result.getSubTx().getCoinDataInstance().getTo().get(0).getAmount().compareTo(reward) == 0);

                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                assertNotNull(bus);
                assertNotNull(bus.getFarmHash());
                assertNotNull(bus.getUserAddress());

                assertEquals(bus.getStakingBalanceOld(), bus.getStakingBalanceNew());
                assertTrue(bus.getSyrupBalanceOld().subtract(bus.getSyrupBalanceNew()).compareTo(reward) == 0);

                System.out.println("[通过]Test Farm-Stake tx execute: 收奖励！");
            }
        };
        return new JunitCase("收奖励", handler, new Object[]{tx1, 201L, tx1.getTime()}, null, false, null, callback1);
    }

    public FarmPoolPO tempFarm() {
        FarmPoolPO po = new FarmPoolPO();
        po.setFarmHash(NulsHash.EMPTY_NULS_HASH);
        po.setAccSyrupPerShare(BigInteger.ZERO);
        po.setSyrupToken(new NerveToken(9, 1));
        po.setLockedTime(1);
        po.setLastRewardBlock(1);
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