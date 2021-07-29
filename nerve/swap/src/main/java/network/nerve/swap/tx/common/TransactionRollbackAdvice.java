/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
package network.nerve.swap.tx.common;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.CommonAdvice;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.enums.BlockType;
import network.nerve.swap.manager.ChainManager;

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2019-12-01
 */
@Component
public class TransactionRollbackAdvice implements CommonAdvice {

    @Override
    public void begin(int chainId, List<Transaction> txList, BlockHeader blockHeader, int syncStatus) {
        try {
            ChainManager.chainHandle(chainId, BlockType.VERIFY_BLOCK.type());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void end(int chainId, List<Transaction> txList, BlockHeader blockHeader) {
    }
}
