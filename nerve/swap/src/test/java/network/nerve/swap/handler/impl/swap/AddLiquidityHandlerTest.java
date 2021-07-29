/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.swap.handler.impl.swap;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.cache.impl.SwapPairCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.AddLiquidityHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.impl.TemporaryPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.manager.SwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.utils.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * @author: PierreLuo
 * @date: 2021/5/11
 */
public class AddLiquidityHandlerTest {

    protected AddLiquidityHandler handler;
    protected IPairFactory iPairFactory;
    protected SwapPairCache swapPairCache;
    protected Chain chain;
    protected ChainManager chainManager;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    protected String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";

    @Before
    public void init() {
        handler = new AddLiquidityHandler();
        iPairFactory = new TemporaryPairFactory();
        swapPairCache = new SwapPairCacheImpl();
        SpringLiteContext.putBean(swapPairCache.getClass().getName(), swapPairCache);
        int chainId = 5;
        long blockHeight = 20L;
        token0 = new NerveToken(chainId, 13);
        token1 = new NerveToken(chainId, 10);
        tokenLP = new NerveToken(chainId, 18);

        chainManager = new ChainManager();
        chain = new Chain();
        ConfigBean cfg = new ConfigBean();
        cfg.setChainId(chainId);
        chain.setConfig(cfg);
        chain.setLogger(LoggerBuilder.getLogger(ModuleE.SW.name, chainId));
        Chain.putCurrentThreadBlockType(0);
        BatchInfo batchInfo = new BatchInfo();
        chain.setBatchInfo(batchInfo);
        // 准备临时余额
        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManagerMock.newInstance(chainId);
        batchInfo.setLedgerTempBalanceManager(tempBalanceManager);
        // 准备当前区块头
        BlockHeader tempHeader = new BlockHeader();
        tempHeader.setHeight(blockHeight);
        tempHeader.setTime(System.currentTimeMillis() / 1000);
        batchInfo.setCurrentBlockHeader(tempHeader);
        // 准备临时交易对
        SwapTempPairManager tempPairManager = SwapTempPairManager.newInstance(chainId);
        batchInfo.setSwapTempPairManager(tempPairManager);
        chainManager.getChainMap().put(chainId, chain);

        BeanUtilTest.setBean(handler, "chainManager", chainManager);
        BeanUtilTest.setBean(handler, "iPairFactory", iPairFactory);
        BeanUtilTest.setBean(iPairFactory, "chainManager", chainManager);
        BeanUtilTest.setBean(swapPairCache, "chainManager", chainManager);
        BeanUtilTest.setBean(swapPairCache, "swapPairStorageService", new SwapPairStorageService() {
            @Override
            public boolean savePair(byte[] address, SwapPairPO po) throws Exception {
                return true;
            }

            @Override
            public String getPairAddressByTokenLP(int chainId, NerveToken tokenLP) {
                return null;
            }

            @Override
            public boolean savePair(String address, SwapPairPO po) throws Exception {
                return true;
            }

            @Override
            public SwapPairPO getPair(byte[] address) {
                SwapPairPO po = new SwapPairPO(address);
                po.setToken0(token0);
                po.setToken1(token1);
                po.setTokenLP(tokenLP);
                return po;
            }

            @Override
            public SwapPairPO getPair(String address) {
                return this.getPair(AddressTool.getAddress(address));
            }

            @Override
            public boolean delelePair(byte[] address) throws Exception {
                return true;
            }

            @Override
            public boolean delelePair(String address) throws Exception {
                return true;
            }
        });
        BeanUtilTest.setBean(swapPairCache, "swapPairReservesStorageService", new SwapPairReservesStorageService() {
            @Override
            public boolean savePairReserves(String address, SwapPairReservesPO dto) throws Exception {
                return true;
            }

            @Override
            public SwapPairReservesPO getPairReserves(String address) {
                SwapPairReservesPO po = new SwapPairReservesPO(AddressTool.getAddress(address),
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        tempHeader.getTime(),
                        tempHeader.getHeight());
                return po;
            }

            @Override
            public boolean delelePairReserves(String address) throws Exception {
                return true;
            }
        });
    }

    @Test
    public void executeTest() throws Exception {
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        JunitExecuter<ISwapHandler> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<ISwapHandler> junitCase) {
                return junitCase.getObj().execute(chain.getChainId(), (Transaction) junitCase.getParams()[0], header.getHeight(), header.getTime());
            }
        };
        List<JunitCase> items = new ArrayList<>();

        items.add(getCase0());
        JunitUtils.execute(items, executer);

        //items.clear();
        //items.add(getCase1());
        //JunitUtils.execute(items, executer);
        //
        //items.clear();
        //items.add(getCase2());
        //JunitUtils.execute(items, executer);
    }

    // NULS && USDT
    // 11300000000 / 10e8
    // 100000000 / 10e6
    // 11300000000 / 10e8 * (10e6 / 100000000)
    // (11300000000 * 10e6) / (10e8 * 100000000)
    // 11300000000 / (100000000 * 10e2)
    protected JunitCase getCase0() throws Exception {
        String caseDesc = "正常-首次添加流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        byte[] fromBytes = AddressTool.getAddress(address20);
        LedgerBalance ledgerBalance0 = tempBalanceManager.getBalance(fromBytes, token0.getChainId(), token0.getAssetId()).getData();
        ledgerBalance0.setBalance(BigInteger.valueOf(200000_00000000L));
        LedgerBalance ledgerBalance1 = tempBalanceManager.getBalance(fromBytes, token1.getChainId(), token1.getAssetId()).getData();
        ledgerBalance1.setBalance(BigInteger.valueOf(200000_000000L));

        BigInteger amountA = BigInteger.valueOf(150_00000000L);
        BigInteger amountB = BigInteger.valueOf(100_000000L);
        BigInteger amountAMin = BigInteger.valueOf(200_00000000L);
        BigInteger amountBMin = BigInteger.valueOf(100_000000L);
        long deadline = System.currentTimeMillis() / 1000 + 300;
        String from = address20;
        byte[] to = AddressTool.getAddress(address21);
        Transaction tx = TxAssembleUtil.asmbSwapAddLiquidity(chainId, from,
                amountA, amountB,
                token0, token1,
                amountAMin, amountBMin,
                deadline, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                BigInteger liquidity = amountA.multiply(amountB).sqrt().subtract(SwapConstant.MINIMUM_LIQUIDITY);
                AddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), AddLiquidityBus.class);
                BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();

                Assert.assertEquals("添加前的池子资产A", BigInteger.ZERO, bus.getReserve0());
                Assert.assertEquals("添加前的池子资产B", BigInteger.ZERO, bus.getReserve1());
                Assert.assertEquals("实际添加的资产A", amountA, bus.getRealAddAmount0());
                Assert.assertEquals("实际添加的资产B", amountB, bus.getRealAddAmount1());
                Assert.assertEquals("退回的资产A", BigInteger.ZERO, bus.getRefundAmount0());
                Assert.assertEquals("退回的资产B", BigInteger.ZERO, bus.getRefundAmount1());
                Assert.assertEquals("流动性份额", liquidity, bus.getLiquidity());
                Assert.assertEquals("用户获得的流动性份额", toAddressBalanceLP, bus.getLiquidity());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test Swap-AddLiquidity tx execute! hash: %s, liquidity: %s", junitCase.getKey(), tx.getHash().toHex(), liquidity));

            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    protected JunitCase getCase1() throws Exception {
        String caseDesc = "正常-第二次添加流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        BigInteger amountA = BigInteger.valueOf(300_00000000L);
        BigInteger amountB = BigInteger.valueOf(200_000000L);
        byte[] to = AddressTool.getAddress(address21);
        RealAddLiquidityOrderDTO dto = SwapUtils.calcAddLiquidity(chainId, iPairFactory, token0, token1, amountA, amountB, BigInteger.ZERO, BigInteger.ZERO);

        BigInteger amountAMin = dto.getRealAmountA();
        BigInteger amountBMin = dto.getRealAmountB();
        long deadline = System.currentTimeMillis() / 1000 + 300;
        String from = address20;

        BigInteger _toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        Transaction tx = TxAssembleUtil.asmbSwapAddLiquidity(chainId, from,
                amountA, amountB,
                token0, token1,
                amountAMin, amountBMin,
                deadline, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                BigInteger liquidity = dto.getLiquidity();
                AddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), AddLiquidityBus.class);
                BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();

                Assert.assertEquals("添加前的池子资产A", BigInteger.valueOf(200_00000000L), bus.getReserve0());
                Assert.assertEquals("添加前的池子资产B", BigInteger.valueOf(100_000000L), bus.getReserve1());
                Assert.assertEquals("实际添加的资产A", amountA, bus.getRealAddAmount0());
                Assert.assertEquals("实际添加的资产B", BigInteger.valueOf(150_000000L), bus.getRealAddAmount1());
                Assert.assertEquals("退回的资产A", BigInteger.ZERO, bus.getRefundAmount0());
                Assert.assertEquals("退回的资产B", BigInteger.valueOf(50_000000L), bus.getRefundAmount1());
                Assert.assertEquals("流动性份额", liquidity, bus.getLiquidity());
                Assert.assertEquals("用户获得的流动性份额", toAddressBalanceLP.subtract(_toAddressBalanceLP), bus.getLiquidity());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test Swap-AddLiquidity tx execute! hash: %s, liquidity: %s", junitCase.getKey(), tx.getHash().toHex(), liquidity));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase2() throws Exception {
        String caseDesc = "异常-添加流动性超时";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        BigInteger amountA = BigInteger.valueOf(500_00000000L);
        BigInteger amountB = BigInteger.valueOf(200_000000L);
        byte[] to = AddressTool.getAddress(address21);
        RealAddLiquidityOrderDTO dto = SwapUtils.calcAddLiquidity(chainId, iPairFactory, token0, token1, amountA, amountB, BigInteger.ZERO, BigInteger.ZERO);

        BigInteger amountAMin = dto.getRealAmountA();
        BigInteger amountBMin = dto.getRealAmountB();
        long deadline = System.currentTimeMillis() / 1000 + 3;
        // 造成超时
        TimeUnit.SECONDS.sleep(5);

        header.setTime(System.currentTimeMillis() / 1000);

        String from = address20;

        Transaction tx = TxAssembleUtil.asmbSwapAddLiquidity(chainId, from,
                amountA, amountB,
                token0, token1,
                amountAMin, amountBMin,
                deadline, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertFalse("期望执行失败", result.isSuccess());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test Swap-AddLiquidity tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

}
