/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.storage;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TransactionPO;

/**
 * 交易持久层
 * @author: Loki
 * @date: 2020-02-27
 */
public interface TxStorageService {

    /**
     * 存交易
     * @param chain
     * @param tx
     * @return
     */
    boolean save(Chain chain, TransactionPO tx);

    /**
     * 获取交易
     * @param chain
     * @param hash
     * @return
     */
    TransactionPO get(Chain chain, NulsHash hash);

    /**
     * 获取交易
     * @param chain
     * @param hash
     * @return
     */
    TransactionPO get(Chain chain, String hash);

    /**
     * 删除交易
     * @param chain
     * @param hash
     * @return
     */
    boolean delete(Chain chain, NulsHash hash);

    /**
     * 删除交易
     * @param chain
     * @param hash
     * @return
     */
    boolean delete(Chain chain, String hash);

    /**
     * 存CheckRetryParseMessage收到的异构链交易hash
     * @param chain
     * @param heterogeneousHash
     * @return
     */
    boolean saveHeterogeneousHash(Chain chain, String heterogeneousHash);

    /**
     * 取CheckRetryParseMessage收到的异构链交易hash
     * @param chain
     * @param heterogeneousHash
     * @return
     */
    String getHeterogeneousHash(Chain chain, String heterogeneousHash);
}
