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
import io.nuls.core.crypto.HexUtil;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;
import network.nerve.swap.utils.BeanUtilTest;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.TxAssembleUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static network.nerve.swap.constant.SwapConstant.BI_100;
import static network.nerve.swap.constant.SwapConstant.BI_1000;
import static org.junit.Assert.assertNotNull;

/**
 * @author: PierreLuo
 * @date: 2021/5/11
 */
public class StableSwapTokenHandlerTest {

    protected StableAddLiquidityHandlerTest stableAddLiquidityHandlerTest;
    protected StableRemoveLiquidityHandlerTest stableRemoveLiquidityHandlerTest;
    protected StableSwapTradeHandler handler;
    protected StableSwapPairCache stableSwapPairCache;
    protected IPairFactory iPairFactory;
    protected Chain chain;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20;
    protected String address21;
    protected String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    protected String address23 = "TNVTdTSPVnoPACtKgRmQy4s7SG3vm6XyR2Ffv";
    protected String createPairTxHash;
    protected byte[] stablePairAddressBytes;
    protected String stablePairAddress;
    protected int[] decimalsOfCoins;


    @Before
    public void init() {
        stableRemoveLiquidityHandlerTest = new StableRemoveLiquidityHandlerTest();
        stableRemoveLiquidityHandlerTest.init();
        stableAddLiquidityHandlerTest = stableRemoveLiquidityHandlerTest.stableAddLiquidityHandlerTest;
        handler = new StableSwapTradeHandler();
        stableSwapPairCache = stableAddLiquidityHandlerTest.stableSwapPairCache;
        iPairFactory = stableAddLiquidityHandlerTest.iPairFactory;
        chain = stableAddLiquidityHandlerTest.chain;
        token0 = stableAddLiquidityHandlerTest.token0;
        token1 = stableAddLiquidityHandlerTest.token1;
        tokenLP = stableAddLiquidityHandlerTest.tokenLP;
        decimalsOfCoins = stableAddLiquidityHandlerTest.decimalsOfCoins;
        address20 = stableAddLiquidityHandlerTest.address20;
        address21 = stableAddLiquidityHandlerTest.address21;
        createPairTxHash = stableAddLiquidityHandlerTest.createPairTxHash;
        stablePairAddressBytes = stableAddLiquidityHandlerTest.stablePairAddressBytes;
        stablePairAddress = stableAddLiquidityHandlerTest.stablePairAddress;
        BeanUtilTest.setBean(handler, "chainManager", stableAddLiquidityHandlerTest.chainManager);
        BeanUtilTest.setBean(handler, "iPairFactory", iPairFactory);
        SwapContext.AWARD_FEE_SYSTEM_ADDRESS = AddressTool.getAddress(address22);
        SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS = AddressTool.getAddress(address23);
        // Transaction fees0.3%
        SwapContext.FEE_PERMILLAGE_STABLE_SWAP = BigInteger.valueOf(3);
        // Fee extraction50%, give`wrong`liquidity provider
        SwapContext.FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP = BigInteger.valueOf(50);
    }

    @Test
    public void txDataTest() throws Exception {
        StableSwapTradeData txData = new StableSwapTradeData();
        txData.setTo(AddressTool.getAddress("TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz"));
        txData.setTokenOutIndex((byte) 3);
        byte[] bytes = txData.serialize();
        System.out.println(HexUtil.encode(bytes));
        txData = new StableSwapTradeData();
        txData.parse(bytes, 0);
        System.out.println();
        // 0500017fe9a685e43b3124e00fd9c8e4e59158baea634503
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

        items.add(stableAddLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(stableAddLiquidityHandlerTest.getCase1());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(stableRemoveLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase0());
        JunitUtils.execute(items, executer);

        //items.clear();
        //items.add(this.getCase1());
        //JunitUtils.execute(items, executer);

    }

    protected JunitCase getCase0() throws Exception {
        String caseDesc = "normal-First coin transaction";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address20;

        long deadline = System.currentTimeMillis() / 1000 + 300;
        byte[] to = AddressTool.getAddress(address20);
        BigInteger[] amountsIn = new BigInteger[]{BigInteger.valueOf(100_000000000L)};
        NerveToken[] tokensIn = new NerveToken[]{token1};
        byte tokenOutIndex = 0;
        byte[] feeTo = null;
        Transaction tx = TxAssembleUtil.asmbStableSwapTrade(chainId, from,
                amountsIn, tokensIn, tokenOutIndex, feeTo, stablePairAddressBytes, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\tUser transactions: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                StableSwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableSwapTradeBus.class);
                BigInteger coinFee = amountsIn[0].multiply(SwapContext.FEE_PERMILLAGE_STABLE_SWAP).divide(BI_1000);
                BigInteger unLiquidityAwardFee = coinFee.multiply(SwapContext.FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP).divide(BI_100);

                IStablePair pair = iPairFactory.getStablePair(stablePairAddress);
                StableSwapPairDTO dto = BeanUtilTest.getBean(pair, "stableSwapPairDTO", StableSwapPairDTO.class);
                Assert.assertEquals("Pool assets before transaction0", BigInteger.valueOf(25250_000000L), bus.getBalances()[0]);
                Assert.assertEquals("Pool assets before transaction1", BigInteger.ZERO, bus.getBalances()[1]);
                Assert.assertEquals("Pool assets after transaction0", BigInteger.valueOf(25250_000000L).subtract(bus.getAmountOut()), bus.getBalances()[0].add(bus.getChangeBalances()[0]));
                Assert.assertEquals("Pool assets after transaction1", BigInteger.ZERO.add(bus.getAmountsIn()[1]).subtract(bus.getUnLiquidityAwardFees()[1]), bus.getBalances()[1].add(bus.getChangeBalances()[1]));
                Assert.assertEquals("asset0of`wrong`Transaction fees that liquidity providers can reward", BigInteger.ZERO, bus.getUnLiquidityAwardFees()[0]);
                Assert.assertEquals("asset1of`wrong`Transaction fees that liquidity providers can reward", unLiquidityAwardFee, bus.getUnLiquidityAwardFees()[1]);
                Assert.assertEquals("Assets purchased by users0", amountsIn[0].subtract(coinFee).multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[1])), bus.getAmountOut().multiply(BigInteger.TEN.pow(18 - decimalsOfCoins[0])));
                Assert.assertEquals("`wrong`Transaction fees that liquidity providers can reward", unLiquidityAwardFee, bus.getUnLiquidityAwardFees()[1]);
                System.out.println(String.format("\tPost execution pool data: %s", dto.toString()));
                System.out.println(String.format("\tSystem transactions: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[adopt, describe: %s] Test StableSwap-TokenTrade tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));

            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    protected JunitCase getCase1() throws Exception {
        String caseDesc = "abnormal-Coin trading";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address20;

        byte[] to = AddressTool.getAddress(address20);
        BigInteger[] amountsIn = new BigInteger[]{BigInteger.valueOf(10_00000000L)};
        NerveToken[] tokensIn = new NerveToken[]{token1};
        byte tokenOutIndex = 0;
        byte[] feeTo = null;
        Transaction tx = TxAssembleUtil.asmbStableSwapTrade(chainId, from,
                amountsIn, tokensIn, tokenOutIndex, feeTo, stablePairAddressBytes, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\tUser transactions: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertFalse("Expected execution failure", result.isSuccess());
                Assert.assertEquals("transactionhash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("block height", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\tSystem transactions: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[adopt, describe: %s] Test Swap-TokenTrade tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));

            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

}
