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

package network.nerve.converter.core.business;

import io.nuls.core.exception.NulsException;
import network.nerve.converter.model.bo.Chain;

/**
 * @author: Loki
 * @date: 2020/3/18
 */
public interface HeterogeneousService {

    /**
     * 判断是否需要组装当前网络的主资产补贴异构链交易手续费
     * 异构链是合约类型,并且提现资产不是异构链主资产,才收取当前网络主资产作为手续费补贴
     * @param heterogeneousChainId
     * @param heterogeneousAssetId
     * @return true 要组装补贴手续费
     */
    boolean isAssembleCurrentAssetFee(int heterogeneousChainId, int heterogeneousAssetId) throws NulsException;

    /**
     * 缓存并持久化异构链正在执行虚拟银行变更交易 状态
     * @param chain
     * @param status
     * @return
     */
    boolean saveExeHeterogeneousChangeBankStatus(Chain chain, Boolean status);

    /**
     * 持久化正在执行取消节点银行资格的提案 状态
     * @param chain
     * @param status
     * @return
     */
    boolean saveExeDisqualifyBankProposalStatus(Chain chain, Boolean status);

    /**
     * 是否正在重置异构链(合约) 状态
     * @param chain
     * @param status
     * @return
     */
    boolean saveResetVirtualBankStatus(Chain chain, Boolean status);


    void checkRetryParse(Chain chain, int heterogeneousChainId, String heterogeneousTxHash) throws NulsException;
    void cancelHtgTx(Chain chain, int heterogeneousChainId, String heterogeneousAddress, String nonce, String priceGwei) throws NulsException;
}
