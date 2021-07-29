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
package network.nerve.swap.handler.impl.swap.stable;

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
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.impl.StableSwapPairCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.handler.impl.swap.LedgerTempBalanceManagerMock;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.impl.TemporaryPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;
import network.nerve.swap.utils.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author: PierreLuo
 * @date: 2021/5/11
 */
public class StableAddLiquidityHandlerTest {

    protected StableAddLiquidityHandler handler;
    protected IPairFactory iPairFactory;
    protected StableSwapPairCache stableSwapPairCache;
    protected Chain chain;
    protected ChainManager chainManager;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    protected String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";
    protected String createPairTxHash = "c75818aeb1380736810a40a70541373d1cf40e104a0f6d33c68cc441472b3e8f";
    protected byte[] stablePairAddressBytes;
    protected String stablePairAddress;
    protected int[] decimalsOfCoins;

    @Before
    public void init() {
        handler = new StableAddLiquidityHandler();
        iPairFactory = new TemporaryPairFactory();
        stableSwapPairCache = new StableSwapPairCacheImpl();
        SpringLiteContext.putBean(stableSwapPairCache.getClass().getName(), stableSwapPairCache);
        int chainId = 5;
        long blockHeight = 20L;
        token0 = new NerveToken(chainId, 1);
        token1 = new NerveToken(chainId, 2);
        tokenLP = new NerveToken(chainId, 3);
        decimalsOfCoins = new int[]{6, 9};
        stablePairAddressBytes = AddressTool.getAddress(HexUtil.decode(createPairTxHash), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);

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
        StableSwapTempPairManager stableSwapTempPairManager = StableSwapTempPairManager.newInstance(chainId);
        batchInfo.setStableSwapTempPairManager(stableSwapTempPairManager);
        chainManager.getChainMap().put(chainId, chain);

        BeanUtilTest.setBean(handler, "chainManager", chainManager);
        BeanUtilTest.setBean(handler, "iPairFactory", iPairFactory);
        BeanUtilTest.setBean(iPairFactory, "chainManager", chainManager);
        BeanUtilTest.setBean(stableSwapPairCache, "swapStablePairStorageService", new SwapStablePairStorageService() {

            @Override
            public boolean savePair(byte[] address, StableSwapPairPo po) throws Exception {
                return true;
            }

            @Override
            public String getPairAddressByTokenLP(int chainId, NerveToken tokenLP) {
                return null;
            }

            @Override
            public StableSwapPairPo getPair(byte[] address) {
                StableSwapPairPo po = new StableSwapPairPo(address);
                po.setTokenLP(tokenLP);
                po.setCoins(new NerveToken[]{token0, token1});
                po.setDecimalsOfCoins(decimalsOfCoins);
                return po;
            }

            @Override
            public StableSwapPairPo getPair(String address) {
                return this.getPair(AddressTool.getAddress(address));
            }

            @Override
            public boolean delelePair(String address) throws Exception {
                return true;
            }
        });
        BeanUtilTest.setBean(stableSwapPairCache, "swapStablePairBalancesStorageService", new SwapStablePairBalancesStorageService() {

            @Override
            public boolean savePairBalances(String address, StableSwapPairBalancesPo dto) throws Exception {
                return true;
            }

            @Override
            public StableSwapPairBalancesPo getPairBalances(String address) {
                StableSwapPairBalancesPo po = new StableSwapPairBalancesPo(
                        AddressTool.getAddress(address),
                        BigInteger.ZERO,
                        SwapUtils.emptyFillZero(new BigInteger[2]),
                        tempHeader.getTime(),
                        tempHeader.getHeight());
                return po;
            }

            @Override
            public boolean delelePairBalances(String address) throws Exception {
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

        items.clear();
        items.add(getCase1());
        JunitUtils.execute(items, executer);

        //items.clear();
        //items.add(getCase2());
        //JunitUtils.execute(items, executer);

    }

    protected JunitCase getCase0() throws Exception {
        String caseDesc = "正常-首次添加流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address20;
        byte[] fromBytes = AddressTool.getAddress(address20);
        LedgerBalance ledgerBalance0 = tempBalanceManager.getBalance(fromBytes, token0.getChainId(), token0.getAssetId()).getData();
        ledgerBalance0.setBalance(BigInteger.valueOf(20000000_000000L));
        LedgerBalance ledgerBalance1 = tempBalanceManager.getBalance(fromBytes, token1.getChainId(), token1.getAssetId()).getData();
        ledgerBalance1.setBalance(BigInteger.valueOf(20000000_000000000L));

        BigInteger[] amounts = new BigInteger[]{BigInteger.valueOf(30000_000000L), BigInteger.valueOf(20000_000000000L)};
        NerveToken[] tokens = new NerveToken[]{token0, token1};
        long deadline = System.currentTimeMillis() / 1000 + 300;
        byte[] to = AddressTool.getAddress(address21);

        //StableAddLiquidityBus bus = SwapUtils.calStableAddLiquididy(chainId, iPairFactory, stablePairAddress, fromBytes, amounts, to);

        Transaction tx = TxAssembleUtil.asmbStableSwapAddLiquidity(chainId, from, amounts, tokens,
                stablePairAddressBytes, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                BigInteger liquidity = SwapUtils.getCumulativeAmountsOfStableSwap(amounts, decimalsOfCoins);
                StableAddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableAddLiquidityBus.class);

                BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();

                Assert.assertEquals("添加前的池子资产0", BigInteger.ZERO, bus.getBalances()[0]);
                Assert.assertEquals("添加前的池子资产1", BigInteger.ZERO, bus.getBalances()[1]);
                Assert.assertEquals("实际添加的资产0", amounts[0], bus.getRealAmounts()[0]);
                Assert.assertEquals("实际添加的资产1", amounts[1], bus.getRealAmounts()[1]);
                Assert.assertEquals("退回的资产0", BigInteger.ZERO, bus.getRefundAmounts()[0]);
                Assert.assertEquals("退回的资产1", BigInteger.ZERO, bus.getRefundAmounts()[1]);
                Assert.assertEquals("流动性份额", liquidity, bus.getLiquidity());
                Assert.assertEquals("用户获得的流动性份额", toAddressBalanceLP, bus.getLiquidity());

                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-AddLiquidity tx execute! hash: %s, liquidity: %s", junitCase.getKey(), tx.getHash().toHex(), liquidity));


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
        String from = address20;
        byte[] fromBytes = AddressTool.getAddress(address20);

        BigInteger[] amounts = new BigInteger[]{BigInteger.valueOf(300_000000L), BigInteger.valueOf(200_000000000L)};
        NerveToken[] tokens = new NerveToken[]{token0, token1};
        long deadline = System.currentTimeMillis() / 1000 + 300;
        byte[] to = AddressTool.getAddress(address21);
        StableAddLiquidityBus _bus = SwapUtils.calStableAddLiquididy(chainId, iPairFactory, stablePairAddress, fromBytes, amounts, to);

        BigInteger _toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        Transaction tx = TxAssembleUtil.asmbStableSwapAddLiquidity(chainId, from, amounts, tokens,
                stablePairAddressBytes, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                BigInteger liquidity = _bus.getLiquidity();
                StableAddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableAddLiquidityBus.class);

                BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();

                Assert.assertEquals("添加前的池子资产0", BigInteger.valueOf(30000_000000L), bus.getBalances()[0]);
                Assert.assertEquals("添加前的池子资产1", BigInteger.valueOf(20000_000000000L), bus.getBalances()[1]);
                Assert.assertEquals("实际添加的资产0", BigInteger.valueOf(300_000000L), bus.getRealAmounts()[0]);
                Assert.assertEquals("实际添加的资产1", BigInteger.valueOf(200_000000000L), bus.getRealAmounts()[1]);
                Assert.assertEquals("退回的资产0", BigInteger.ZERO, bus.getRefundAmounts()[0]);
                Assert.assertEquals("退回的资产1", BigInteger.ZERO, bus.getRefundAmounts()[1]);
                Assert.assertEquals("流动性份额", liquidity, bus.getLiquidity());
                Assert.assertEquals("用户获得的流动性份额", toAddressBalanceLP.subtract(_toAddressBalanceLP), bus.getLiquidity());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("用户总流动性份额: %s", toAddressBalanceLP));
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-AddLiquidity tx execute! hash: %s, liquidity: %s", junitCase.getKey(), tx.getHash().toHex(), liquidity));


            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase2() throws Exception {
        //TODO pierre 异常case
        String caseDesc = "异常-添加流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address20;

        BigInteger[] amounts = new BigInteger[]{BigInteger.valueOf(300_00000000L), BigInteger.valueOf(200_00000000L)};
        NerveToken[] tokens = new NerveToken[]{token0, token1};
        byte[] to = AddressTool.getAddress(address21);

        Transaction tx = TxAssembleUtil.asmbStableSwapAddLiquidity(chainId, from, amounts, tokens,
                stablePairAddressBytes, to, tempBalanceManager);
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
