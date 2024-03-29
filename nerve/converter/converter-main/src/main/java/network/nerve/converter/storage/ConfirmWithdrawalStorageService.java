/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
import network.nerve.converter.model.po.ConfirmWithdrawalPO;

/**
 * NERVETransactions that confirm the status of withdrawal transactions in the network Confirmed business data access
 * @author: Loki
 * @date: 2020-03-06
 */
public interface ConfirmWithdrawalStorageService {


    boolean save(Chain chain, ConfirmWithdrawalPO cwPO);

    /**
     * Based on withdrawal transactionshashobtain
     * @param chain
     * @param hash
     * @return
     */
    ConfirmWithdrawalPO findByWithdrawalTxHash(Chain chain, NulsHash hash);

    /**
     * Based on withdrawal transactionshashdelete
     * @param chain
     * @param hash
     * @return
     */
    boolean deleteByWithdrawalTxHash(Chain chain, NulsHash hash);
}
