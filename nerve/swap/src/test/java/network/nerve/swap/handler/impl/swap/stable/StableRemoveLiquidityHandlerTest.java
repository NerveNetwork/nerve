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
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.stable.StableRemoveLiquidityHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
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
public class StableRemoveLiquidityHandlerTest {

    protected StableAddLiquidityHandlerTest stableAddLiquidityHandlerTest;
    protected StableRemoveLiquidityHandler handler;
    protected StableSwapPairCache stableSwapPairCache;
    protected IPairFactory iPairFactory;
    protected Chain chain;
    protected NerveToken token0;
    protected NerveToken token1;
    protected NerveToken tokenLP;
    protected String address20;
    protected String address21;
    protected String createPairTxHash;
    protected byte[] stablePairAddressBytes;
    protected String stablePairAddress;


    @Before
    public void init() {
        stableAddLiquidityHandlerTest = new StableAddLiquidityHandlerTest();
        stableAddLiquidityHandlerTest.init();
        handler = new StableRemoveLiquidityHandler();
        iPairFactory = stableAddLiquidityHandlerTest.iPairFactory;
        chain = stableAddLiquidityHandlerTest.chain;
        token0 = stableAddLiquidityHandlerTest.token0;
        token1 = stableAddLiquidityHandlerTest.token1;
        tokenLP = stableAddLiquidityHandlerTest.tokenLP;
        address20 = stableAddLiquidityHandlerTest.address20;
        address21 = stableAddLiquidityHandlerTest.address21;
        createPairTxHash = stableAddLiquidityHandlerTest.createPairTxHash;
        stablePairAddressBytes = stableAddLiquidityHandlerTest.stablePairAddressBytes;
        stablePairAddress = stableAddLiquidityHandlerTest.stablePairAddress;
        BeanUtilTest.setBean(handler, "chainManager", stableAddLiquidityHandlerTest.chainManager);
        BeanUtilTest.setBean(handler, "iPairFactory", iPairFactory);
        BeanUtilTest.setBean(handler, "stableSwapPairCache", stableAddLiquidityHandlerTest.stableSwapPairCache);
        BeanUtilTest.setBean(handler, "ledgerAssetCache", new LedgerAssetCache() {
            @Override
            public LedgerAssetDTO getLedgerAsset(int chainId, int assetChainId, int assetId) {
                String key = assetChainId + "-" + assetId;
                return new LedgerAssetDTO(assetChainId, assetId, "symbol_" + key, "name_" + key, 0);
            }

            @Override
            public LedgerAssetDTO getLedgerAsset(int chainId, NerveToken token) {
                if (token == null) {
                    return null;
                }
                return getLedgerAsset(chainId, token.getChainId(), token.getAssetId());
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

        items.add(stableAddLiquidityHandlerTest.getCase0());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(stableAddLiquidityHandlerTest.getCase1());
        JunitUtils.execute(items, executer);

        items.clear();
        items.add(this.getCase0());
        JunitUtils.execute(items, executer);

        //items.clear();
        //items.add(this.getCase1());
        //JunitUtils.execute(items, executer);

    }

    protected JunitCase getCase0() throws Exception {
        String caseDesc = "正常-首次移除流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address21;

        long deadline = System.currentTimeMillis() / 1000 + 300;
        byte[] to = AddressTool.getAddress(address21);

        BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        BigInteger amountLP = toAddressBalanceLP.multiply(BigInteger.valueOf(2)).divide(BigInteger.valueOf(4));

        Transaction tx = TxAssembleUtil.asmbStableSwapRemoveLiquidity(chainId, from,
                amountLP, tokenLP, new byte[]{1, 0}, stablePairAddressBytes, to, tempBalanceManager);
        tempBalanceManager.refreshTempBalance(chainId, tx, header.getTime());
        System.out.println(String.format("\t用户交易: \n%s", tx.format()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) throws Exception {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                StableRemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableRemoveLiquidityBus.class);
                BigInteger _toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
                BigInteger _toAddressBalanceToken0 = tempBalanceManager.getBalance(to, token0.getChainId(), token0.getAssetId()).getData().getBalance();
                BigInteger _toAddressBalanceToken1 = tempBalanceManager.getBalance(to, token1.getChainId(), token1.getAssetId()).getData().getBalance();

                IStablePair pair = iPairFactory.getStablePair(stablePairAddress);
                StableSwapPairDTO dto = BeanUtilTest.getBean(pair, "stableSwapPairDTO", StableSwapPairDTO.class);
                Assert.assertEquals("移除前的池子资产0", BigInteger.valueOf(30300_000000L), bus.getBalances()[0]);
                Assert.assertEquals("移除前的池子资产1", BigInteger.valueOf(20200_000000000L), bus.getBalances()[1]);
                Assert.assertEquals("赎回的资产0", BigInteger.valueOf(5050_000000L), bus.getAmounts()[0]);
                Assert.assertEquals("赎回的资产1", BigInteger.valueOf(20200_000000000L), bus.getAmounts()[1]);
                Assert.assertEquals("用户余额token0", bus.getAmounts()[0], _toAddressBalanceToken0);
                Assert.assertEquals("用户余额token1", bus.getAmounts()[1], _toAddressBalanceToken1);
                Assert.assertEquals("用户移除的流动性份额", amountLP, toAddressBalanceLP.subtract(_toAddressBalanceLP));
                Assert.assertEquals("池子剩余流动性份额", pair.totalSupply(), toAddressBalanceLP.subtract(amountLP));
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("\t执行后池子数据: %s", dto.toString()));
                System.out.println(String.format("\t系统交易: \n%s", result.getSubTx().format()));
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-RemoveLiquidity tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));

            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

    protected JunitCase getCase1() throws Exception {
        //TODO pierre 异常case
        String caseDesc = "异常-移除流动性";
        System.out.println(String.format("//////////////////////////////////////////////////【%s】//////////////////////////////////////////////////", caseDesc));
        int chainId = chain.getChainId();
        BatchInfo batchInfo = chain.getBatchInfo();
        BlockHeader header = batchInfo.getCurrentBlockHeader();
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        String from = address21;

        byte[] to = AddressTool.getAddress(address21);

        BigInteger toAddressBalanceLP = tempBalanceManager.getBalance(to, tokenLP.getChainId(), tokenLP.getAssetId()).getData().getBalance();
        BigInteger amountLP = toAddressBalanceLP.divide(BigInteger.valueOf(2));

        Transaction tx = TxAssembleUtil.asmbStableSwapRemoveLiquidity(chainId, from,
                amountLP, tokenLP, new byte[]{1, 0}, stablePairAddressBytes, to, tempBalanceManager);
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
                System.out.println(String.format("[通过, 描述: %s] Test Swap-RemoveLiquidity tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase(caseDesc, handler, new Object[]{tx}, null, false, null, callback);
    }

}
