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
import io.nuls.core.crypto.HexUtil;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.SwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.utils.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static network.nerve.swap.constant.SwapConstant.*;
import static org.junit.Assert.assertNotNull;

/**
 * @author: PierreLuo
 * @date: 2021/5/11
 */
public class SwapTokenHandlerTest {

    protected AddLiquidityHandlerTest addLiquidityHandlerTest;
    protected RemoveLiquidityHandlerTest removeLiquidityHandlerTest;
    protected SwapTradeHandler handler;
    protected IPairFactory iPairFactory;
    protected Chain chain;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20;
    protected String address21;
    protected String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    protected String address23 = "TNVTdTSPVnoPACtKgRmQy4s7SG3vm6XyR2Ffv";

    @Before
    public void init() {
        removeLiquidityHandlerTest = new RemoveLiquidityHandlerTest();
        removeLiquidityHandlerTest.init();
        addLiquidityHandlerTest = removeLiquidityHandlerTest.addLiquidityHandlerTest;
        handler = new SwapTradeHandler();
        iPairFactory = addLiquidityHandlerTest.iPairFactory;
        chain = addLiquidityHandlerTest.chain;
        token0 = addLiquidityHandlerTest.token0;
        token1 = addLiquidityHandlerTest.token1;
        tokenLP = addLiquidityHandlerTest.tokenLP;
        address20 = addLiquidityHandlerTest.address20;
        address21 = addLiquidityHandlerTest.address21;
        BeanUtilTest.setBean(handler, "chainManager", addLiquidityHandlerTest.chainManager);
        BeanUtilTest.setBean(handler, "iPairFactory", iPairFactory);
        BeanUtilTest.setBean(handler, "swapPairCache", addLiquidityHandlerTest.swapPairCache);
        SwapContext.AWARD_FEE_SYSTEM_ADDRESS = AddressTool.getAddress(address22);
        SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS = AddressTool.getAddress(address23);
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

        items.add(addLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(addLiquidityHandlerTest.getCase1());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(removeLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase1());
        JunitUtils.execute(items, executer);

    }

    protected JunitCase getCase0() throws Exception {
        String caseDesc = "normal-First coin transaction";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        long deadline = System.currentTimeMillis() / 1000 + 300;
        String from = address20;
        byte[] to = AddressTool.getAddress(address20);
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, token0, token1);
        BigInteger amountIn = BigInteger.valueOf(10_00000000L);
        NerveToken tokenIn = token0;
        NerveToken[] path = {tokenIn, token1};
        BigInteger[] amountsOut = SwapUtils.getAmountsOut(chainId, iPairFactory, amountIn, path);
        BigInteger amountOutMin = amountsOut[amountsOut.length - 1];
        // The second method of calculationamountOut
        IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
        BigInteger[] reserves = pair.getReserves();
        BigInteger amountOut = SwapUtils.getAmountOut(amountIn, reserves[0], reserves[1], BI_3);
        Assert.assertEquals("Two calculation methodsamountOut", amountOutMin, amountOut);
        System.out.println(String.format("\tThe calculated minimum purchasable amount: %s", amountOutMin));
        byte[] feeTo = null;

        Transaction tx = TxAssembleUtil.asmbSwapTrade(chainId, from,
                amountIn, tokenIn,
                amountOutMin, path, feeTo,
                deadline, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\tUser transactions: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    BigInteger unLiquidityAwardFee = amountIn.divide(BI_1000);
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    SwapPairDTO dto = BeanUtilTest.getBean(pair, "swapPairDTO", SwapPairDTO.class);
                    Assert.assertEquals("Pool assets before transactionA", BigInteger.valueOf(250_00000008L), pairBus.getReserve0());
                    Assert.assertEquals("Pool assets before transactionB", BigInteger.valueOf(125_000001L), pairBus.getReserve1());
                    Assert.assertEquals("Pool assets after transactionA", BigInteger.valueOf(250_00000008L).add(pairBus.getAmountIn()).subtract(pairBus.getUnLiquidityAwardFee()), pairBus.getBalance0());
                    Assert.assertEquals("Pool assets after transactionB", BigInteger.valueOf(125_000001L).subtract(pairBus.getAmountOut()), pairBus.getBalance1());
                    Assert.assertEquals("Assets purchased by usersB", amountOutMin, pairBus.getAmountOut());
                    Assert.assertEquals("`wrong`Transaction fees that liquidity providers can reward", unLiquidityAwardFee, pairBus.getUnLiquidityAwardFee());
                    System.out.println(String.format("\tPost execution pool data: %s", dto.toString()));
                }

                Assert.assertEquals("transactionhash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("block height", header.getHeight(), result.getBlockHeight());

                System.out.println(String.format("\tSystem transactions: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[adopt, describe: %s] Test Swap-TokenTrade tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    protected JunitCase getCase1() throws Exception {
        String caseDesc = "abnormal-Currency transaction timeout";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        long deadline = System.currentTimeMillis() / 1000 + 3;
        // Causing timeout
        TimeUnit.SECONDS.sleep(5);

        header.setTime(System.currentTimeMillis() / 1000);

        String from = address20;
        byte[] to = AddressTool.getAddress(address20);
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, token0, token1);
        BigInteger amountIn = BigInteger.valueOf(10_00000000L);
        NerveToken tokenIn = token0;
        NerveToken[] path = {tokenIn, token1};
        BigInteger[] amountsOut = SwapUtils.getAmountsOut(chainId, iPairFactory, amountIn, path);
        BigInteger amountOutMin = amountsOut[amountsOut.length - 1];
        // The second method of calculationamountOut
        IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
        BigInteger[] reserves = pair.getReserves();
        BigInteger amountOut = SwapUtils.getAmountOut(amountIn, reserves[0], reserves[1], BI_3);
        Assert.assertEquals("Two calculation methodsamountOut", amountOutMin, amountOut);
        System.out.println(String.format("\tThe calculated minimum purchasable amount: %s", amountOutMin));
        byte[] feeTo = null;

        Transaction tx = TxAssembleUtil.asmbSwapTrade(chainId, from,
                amountIn, tokenIn,
                amountOutMin, path, feeTo,
                deadline, to, tempBalanceManager);
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
                System.out.println(String.format("[adopt, describe: %s] Test Swap-TokenTrade tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

}
