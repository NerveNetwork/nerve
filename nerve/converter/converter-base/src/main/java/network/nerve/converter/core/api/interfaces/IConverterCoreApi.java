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
package network.nerve.converter.core.api.interfaces;

import io.nuls.base.data.Transaction;

/**
 * @author: Mimi
 * @date: 2020-05-08
 */
public interface IConverterCoreApi {
    /**
     * 获取Nerve网络当前区块高度
     */
    long getCurrentBlockHeightOnNerve();

    /**
     * 当前节点是否为虚拟银行
     */
    boolean isVirtualBankByCurrentNode();

    /**
     * 获取当前节点加入虚拟银行时的顺序
     */
    int getVirtualBankOrder();

    /**
     * 获取当前虚拟银行成员的数量
     */
    int getVirtualBankSize();

    /**
     * 获取Nerve交易
     */
    Transaction getNerveTx(String hash);
}
