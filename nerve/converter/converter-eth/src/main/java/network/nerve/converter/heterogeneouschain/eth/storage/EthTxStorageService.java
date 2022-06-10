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
package network.nerve.converter.heterogeneouschain.eth.storage;

import network.nerve.converter.heterogeneouschain.eth.model.EthRecoveryDto;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

/**
 * 保存交易信息 - 充值、签名完成的提现、变更
 * @author: Mimi
 * @date: 2020-02-18
 */
public interface EthTxStorageService {


    int save(HeterogeneousTransactionInfo po) throws Exception;

    HeterogeneousTransactionInfo findByTxHash(String txHash);

    void deleteByTxHash(String txHash) throws Exception;

    int saveRecovery(String nerveTxKey, EthRecoveryDto recovery) throws Exception;

    EthRecoveryDto findRecoveryByNerveTxKey(String nerveTxKey);
}
