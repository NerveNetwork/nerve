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

package io.nuls.base.protocol;

import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;

import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2020/4/3
 */
public interface ModuleTxPackageProcessor {

    /**
     * When packaging transaction modules, If the corresponding transaction is registered packProduceThe logo istrue,
     * It will display all the corresponding flag bits under this moduletrueTransaction, Call the interface as parameters together
     * For example, internal transaction generation and other processing, asdex
     * Return newly generated transactions
     * @param chainId
     * @param txs
     * @param process 0:Calling during packaging 1:Calling when validating a block
     * @param height block height
     * @param blockTime Block time
     *
     * @return Map Return two lists 1.key:newlyList Newly generated transactions, 2.key: rmHashList Original transactions that need to be deletedhash
     */
    default Map<String, List<String>> packProduce(int chainId, List<Transaction> txs, int process, long height, long blockTime) throws NulsException {
        return null;
    }

    /**
     * modulecode
     * @return
     */
    String getModuleCode();
}
