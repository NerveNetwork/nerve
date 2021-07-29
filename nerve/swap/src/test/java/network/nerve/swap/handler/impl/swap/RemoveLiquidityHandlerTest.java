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
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.RemoveLiquidityHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.SwapPairDTO;
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
public class RemoveLiquidityHandlerTest {

    protected AddLiquidityHandlerTest addLiquidityHandlerTest;
    protected RemoveLiquidityHandler handler;
    protected IPairFactory iPairFactory;
    protected Chain chain;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20;
    protected String address21;

    @Before
    public void init() {
        addLiquidityHandlerTest = new AddLiquidityHandlerTest();
        addLiquidityHandlerTest.init();
        handler = new RemoveLiquidityHandler();
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
        BeanUtilTest.setBean(handler, "ledgerAssetCache", new LedgerAssetCache() {
            @Override
            public LedgerAssetDTO getLedgerAsset(int chainId, int assetId) {
                String key = chainId + "-" + assetId;
                return new LedgerAssetDTO(chainId, assetId, "symbol_" + key, "name_" + key, 0);
            }

            @Override
            public LedgerAssetDTO getLedgerAsset(NerveToken token) {
                if (token == null) {
                    return null;
                }
                return getLedgerAsset(token.getChainId(), token.getAssetId());
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

        items.add(addLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(addLiquidityHandlerTest.getCase1());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase1());
        JunitUtils.execute(items, executer);

    }

    protected JunitCase getCase0() throws Exception {
        String caseDesc = "正常-首次移除流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        long deadline = System.currentTimeMillis() / 1000 + 300;
        String from = address21;
        byte[] to = AddressTool.getAddress(address21);
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, token0, token1);

        BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        BigInteger amountLP = toAddressBalanceLP.divide(BigInteger.valueOf(2));
        // 计算预期保护值
        RemoveLiquidityBus bus = SwapUtils.calRemoveLiquidityBusiness(chainId, iPairFactory, pairAddress, amountLP,
                token0, token1, BigInteger.ZERO, BigInteger.ZERO);
        BigInteger amountAMin = bus.getAmount0();
        BigInteger amountBMin = bus.getAmount1();
        System.out.println(String.format("\t交易前计算移除流动性的数据结果: %s", bus.toString()));

        Transaction tx = TxAssembleUtil.asmbSwapRemoveLiquidity(chainId, from,
                amountLP, tokenLP,
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
                RemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), RemoveLiquidityBus.class);
                BigInteger _toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
                BigInteger _toAddressBalanceToken0 = tempBalanceManager.getBalance(to, token0.getChainId(), token0.getAssetId()).getData().getBalance();
                BigInteger _toAddressBalanceToken1 = tempBalanceManager.getBalance(to, token1.getChainId(), token1.getAssetId()).getData().getBalance();
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairAddress));
                SwapPairDTO dto = BeanUtilTest.getBean(pair, "swapPairDTO", SwapPairDTO.class);
                Assert.assertEquals("移除前的池子资产A", BigInteger.valueOf(500_00000000L), bus.getReserve0());
                Assert.assertEquals("移除前的池子资产B", BigInteger.valueOf(250_000000L), bus.getReserve1());
                Assert.assertEquals("赎回的资产A", amountAMin, bus.getAmount0());
                Assert.assertEquals("赎回的资产B", amountBMin, bus.getAmount1());
                Assert.assertEquals("用户余额tokenA", amountAMin, _toAddressBalanceToken0);
                Assert.assertEquals("用户余额tokenB", amountBMin, _toAddressBalanceToken1);
                Assert.assertEquals("用户移除的流动性份额", amountLP, toAddressBalanceLP.subtract(_toAddressBalanceLP));
                Assert.assertEquals("池子剩余流动性份额", pair.totalSupply(), toAddressBalanceLP.subtract(amountLP));
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t执行后池子数据: %s", dto.toString()));
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test Swap-RemoveLiquidity tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    protected JunitCase getCase1() throws Exception {
        String caseDesc = "异常-移除流动性超时";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        long deadline = System.currentTimeMillis() / 1000 + 3;
        // 造成超时
        TimeUnit.SECONDS.sleep(5);
        header.setTime(System.currentTimeMillis() / 1000);

        String from = address21;
        byte[] to = AddressTool.getAddress(address21);
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, token0, token1);

        BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        BigInteger amountLP = toAddressBalanceLP;
        // 计算预期保护值
        RemoveLiquidityBus bus = SwapUtils.calRemoveLiquidityBusiness(chainId, iPairFactory, pairAddress, amountLP,
                token0, token1, BigInteger.ZERO, BigInteger.ZERO);
        BigInteger amountAMin = bus.getAmount0();
        BigInteger amountBMin = bus.getAmount1();
        System.out.println(String.format("\t交易前计算移除流动性的数据结果: %s", bus.toString()));

        Transaction tx = TxAssembleUtil.asmbSwapRemoveLiquidity(chainId, from,
                amountLP, tokenLP,
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
                System.out.println(String.format("[通过, 描述: %s] Test Swap-RemoveLiquidity tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

}
