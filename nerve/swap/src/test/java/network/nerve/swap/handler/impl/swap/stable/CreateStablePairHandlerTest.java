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
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.impl.StableSwapPairCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.stable.CreateStablePairHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.impl.TemporaryPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;
import network.nerve.swap.utils.BeanUtilTest;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.SwapUtils;
import network.nerve.swap.utils.TxAssembleUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author: PierreLuo
 * @date: 2021/5/11
 */
public class CreateStablePairHandlerTest {

    private CreateStablePairHandler handler;
    private StableSwapPairCache stableSwapPairCache;
    protected IPairFactory iPairFactory;
    private Chain chain;
    private NerveToken token0;
    private NerveToken token1;
    private NerveToken tokenLP;

    @Before
    public void init() {
        handler = new CreateStablePairHandler();
        iPairFactory = new TemporaryPairFactory();
        stableSwapPairCache = new StableSwapPairCacheImpl();
        SpringLiteContext.putBean(stableSwapPairCache.getClass().getName(), stableSwapPairCache);
        int chainId = 5;
        long blockHeight = 20L;
        token0 = new NerveToken(chainId, 1);
        token1 = new NerveToken(chainId, 2);
        tokenLP = new NerveToken(chainId, 3);

        ChainManager chainManager = new ChainManager();
        chain = new Chain();
        ConfigBean cfg = new ConfigBean();
        cfg.setChainId(chainId);
        chain.setConfig(cfg);
        chain.setLogger(LoggerBuilder.getLogger(ModuleE.SW.name, chainId));
        Chain.putCurrentThreadBlockType(0);
        BatchInfo batchInfo = new BatchInfo();
        chain.setBatchInfo(batchInfo);
        // 准备临时余额
        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(chainId);
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
                po.setDecimalsOfCoins(new int[]{6, 6});
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
    public void executeTest() throws IOException {
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        List<JunitCase> items = new ArrayList<>();

        items.add(getCase0());
        items.add(getCase1());
        items.add(getCase2());

        JunitExecuter<ISwapHandler> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<ISwapHandler> junitCase) {
                return junitCase.getObj().execute(chain.getChainId(), (Transaction) junitCase.getParams()[0], header.getHeight(), header.getTime());
            }
        };
        JunitUtils.execute(items, executer);
    }

    private JunitCase getCase0() throws IOException {
        int chainId = chain.getChainId();
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        Transaction tx = TxAssembleUtil.asmbStableSwapPairCreate(chainId, null, token0, token1);
        System.out.println(String.format("创建交易对的交易hash: %s", tx.getHash().toHex()));
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                byte[] stablePairAddressBytes = AddressTool.getAddress(tx.getHash().getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
                String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
                Assert.assertTrue(chain.getBatchInfo().getStableSwapTempPairManager().isExist(stablePairAddress));
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-CreatePair tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));

            }
        };
        return new JunitCase("正常创建稳定价值Pair", handler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase1() throws IOException {
        int chainId = chain.getChainId();
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        Transaction tx = TxAssembleUtil.asmbStableSwapPairCreate(chainId, null, token0, token0);
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-CreatePair tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase("异常创建稳定价值Pair，使用同一个token创建", handler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase2() throws IOException {
        int chainId = chain.getChainId();
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        Transaction tx = TxAssembleUtil.asmbStableSwapPairCreate(chainId, null, token0, token1);
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                Assert.assertEquals("交易hash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("区块高度", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[通过, 描述: %s] Test StableSwap-CreatePair tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase("异常创建稳定价值Pair，重复创建稳定价值Pair", handler, new Object[]{tx}, null, false, null, callback);
    }
}
