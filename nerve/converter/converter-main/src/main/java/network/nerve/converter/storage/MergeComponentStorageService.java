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

package network.nerve.converter.storage;

import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.MergedComponentCallPO;

/**
 * @author: Loki
 * @date: 2020/7/30
 */
public interface MergeComponentStorageService {

    /**
     * 记录调用异构链时合并的交易
     * key为合并交易列表的第一个交易hash, 用于异构链识别
     *
     * @param chain
     * @param hash
     * @param po
     * @return
     */
    boolean saveMergeComponentCall(Chain chain, String hash, MergedComponentCallPO po);

    /**
     * 根据合并时的hash 来获取合并的各独立交易
     *
     * @param chain
     * @param hash
     * @return
     */
    MergedComponentCallPO findMergedTx(Chain chain, String hash);

    /**
     * 根据合并的成员hash 查询key的hash
     *
     * @param chain
     * @param hash
     * @return
     */
    String getMergedTxKeyByMember(Chain chain, String hash);


    boolean removeMergedTx(Chain chain, String hash) throws Exception;
}
