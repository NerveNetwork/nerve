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
package network.nerve.converter.heterogeneouschain.ethII.context;

import network.nerve.converter.heterogeneouschain.ethII.model.EthWaitingTxPo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthIIContext {

    // 初始化配置地址
    public static Set<String> FILTER_ACCOUNT_SET = new HashSet<>();

    /**
     * 等待交易队列，当前节点保存交易的调用参数（交易由某一个管理员发出，按管理员顺序，排在首位的管理员发出交易，若发送失败或者未发出，则由下一顺位发出交易，以此类推）
     */
    public static LinkedBlockingDeque<EthWaitingTxPo> WAITING_TX_QUEUE = new LinkedBlockingDeque<>();

    public static CountDownLatch INIT_WAITING_TX_QUEUE_LATCH = new CountDownLatch(1);


}
