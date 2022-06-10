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
package network.nerve.converter.core.heterogeneous.callback.interfaces;

import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.model.bo.HeterogeneousAddress;

import java.util.List;

/**
 * 发出异构链上交易后，异构链组件监听交易是否确认打包，打包后
 *
 * @author: Mimi
 * @date: 2020-02-17
 */
public interface ITxConfirmedProcessor {
    /**
     * @param txType           交易类型 - WITHDRAW/CHANGE/UPGRADE 提现/管理员变更/升级
     * @param nerveTxHash      本链交易hash
     * @param txHash           异构链交易hash
     * @param blockHeight      异构链交易确认高度
     * @param txTime           异构链交易时间
     * @param multiSignAddress 当前多签地址
     * @param signers          交易签名地址列表
     */
    void txConfirmed(HeterogeneousChainTxType txType, String nerveTxHash,
                     String txHash, Long blockHeight, Long txTime, String multiSignAddress, List<HeterogeneousAddress> signers) throws Exception;

    /**
     * @param nerveTxHash         本链交易hash
     * @param heterogeneousTxHash 异构链交易hash
     */
    void pendingTxOfWithdraw(String nerveTxHash, String heterogeneousTxHash) throws Exception;
}
