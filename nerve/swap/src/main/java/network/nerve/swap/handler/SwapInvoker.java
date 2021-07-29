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
package network.nerve.swap.handler;

import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.SwapResult;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class SwapInvoker implements ISwapInvoker {

    @Autowired
    private ChainManager chainManager;

    private Map<Integer, ISwapHandler> map = new HashMap<>();

    @Override
    public void registerHandler(ISwapHandler handler) {
        map.put(handler.txType(), handler);
    }

    @Override
    public SwapResult invoke(int chainId, Transaction tx, long blockHeight, long blockTime) throws NulsException {
        ISwapHandler iSwapHandler = map.get(tx.getType());
        if (iSwapHandler == null) {
            throw new NulsException(SwapErrorCode.NULL_PARAMETER);
        }
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        // 缓存高度必须一致
        if (blockHeight != batchInfo.getCurrentBlockHeader().getHeight()) {
            throw new NulsException(SwapErrorCode.BLOCK_HEIGHT_INCONSISTENCY);
        }
        // 刷新临时余额
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        tempBalanceManager.refreshTempBalance(chainId, tx, blockTime);
        // 执行交易业务
        return iSwapHandler.execute(chainId, tx, blockHeight, blockTime);
    }
}
