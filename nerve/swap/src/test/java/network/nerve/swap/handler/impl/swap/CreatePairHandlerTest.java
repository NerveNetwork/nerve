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
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.cache.impl.SwapPairCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.handler.ISwapHandler;
import network.nerve.swap.handler.impl.CreatePairHandler;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.manager.SwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.storage.SwapPairStorageService;
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
public class CreatePairHandlerTest {

    private CreatePairHandler createPairHandler;
    private SwapPairCache swapPairCache;
    private Chain chain;
    private NerveToken token0;
    private NerveToken token1;
    private NerveToken tokenLP;

    @Before
    public void init() {
        createPairHandler = new CreatePairHandler();
        swapPairCache = new SwapPairCacheImpl();
        SpringLiteContext.putBean(swapPairCache.getClass().getName(), swapPairCache);
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
        // Prepare temporary balance
        LedgerTempBalanceManager tempBalanceManager = LedgerTempBalanceManager.newInstance(chainId);
        batchInfo.setLedgerTempBalanceManager(tempBalanceManager);
        // Prepare the current block header
        BlockHeader tempHeader = new BlockHeader();
        tempHeader.setHeight(blockHeight);
        tempHeader.setTime(System.currentTimeMillis() / 1000);
        batchInfo.setCurrentBlockHeader(tempHeader);
        // Prepare for temporary transactions
        SwapTempPairManager tempPairManager = SwapTempPairManager.newInstance(chainId);
        batchInfo.setSwapTempPairManager(tempPairManager);
        chainManager.getChainMap().put(chainId, chain);

        BeanUtilTest.setBean(createPairHandler, "chainManager", chainManager);
        BeanUtilTest.setBean(swapPairCache, "chainManager", chainManager);
        BeanUtilTest.setBean(swapPairCache, "swapPairStorageService", new SwapPairStorageService() {
            @Override
            public boolean savePair(byte[] address, SwapPairPO po) throws Exception {
                return true;
            }

            @Override
            public boolean savePair(String address, SwapPairPO po) throws Exception {
                return true;
            }

            @Override
            public String getPairAddressByTokenLP(int chainId, NerveToken tokenLP) {
                return null;
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
    public void swapPairAddress() {
        System.out.println(SwapUtils.getStringPairAddress(5, new NerveToken(5, 1), new NerveToken(5, 6)));
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
        Transaction tx = TxAssembleUtil.asmbSwapPairCreate(chainId, token0, token1);
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertTrue(result.isSuccess());
                String pairAddress = SwapUtils.getStringPairAddress(chainId, token0, token1);
                Assert.assertTrue(chain.getBatchInfo().getSwapTempPairManager().isExist(pairAddress));
                Assert.assertEquals("transactionhash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("block height", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[adopt, describe: %s] Test Swap-CreatePair tx execute! hash: %s", junitCase.getKey(), tx.getHash().toHex()));

            }
        };
        return new JunitCase("Normal creationPair", createPairHandler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase1() throws IOException {
        int chainId = chain.getChainId();
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        Transaction tx = TxAssembleUtil.asmbSwapPairCreate(chainId, token0, token0);
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                Assert.assertEquals("transactionhash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("block height", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[adopt, describe: %s] Test Swap-CreatePair tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase("Exception creationPairUsing the sametokenestablish", createPairHandler, new Object[]{tx}, null, false, null, callback);
    }

    private JunitCase getCase2() throws IOException {
        int chainId = chain.getChainId();
        BlockHeader header = chain.getBatchInfo().getCurrentBlockHeader();
        Transaction tx = TxAssembleUtil.asmbSwapPairCreate(chainId, token0, token1);
        NerveCallback<SwapResult> callback = new NerveCallback<>() {
            @Override
            public void callback(JunitCase junitCase, SwapResult result) {
                assertNotNull(result);
                Assert.assertFalse(result.isSuccess());
                Assert.assertEquals("transactionhash", tx.getHash().toHex(), result.getHash());
                Assert.assertEquals("block height", header.getHeight(), result.getBlockHeight());
                System.out.println(String.format("[adopt, describe: %s] Test Swap-CreatePair tx execute! Error: %s", junitCase.getKey(), result.getErrorMessage()));
            }
        };
        return new JunitCase("Exception creationPair, duplicate creationPair", createPairHandler, new Object[]{tx}, null, false, null, callback);
    }
}
