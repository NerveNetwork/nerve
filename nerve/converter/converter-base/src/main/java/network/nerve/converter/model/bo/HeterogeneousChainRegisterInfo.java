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
package network.nerve.converter.model.bo;

import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IHeterogeneousUpgrade;
import network.nerve.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;

/**
 * @author: Mimi
 * @date: 2020-02-17
 */
public class HeterogeneousChainRegisterInfo {
    /**
     * 多签地址
     * 用于充值、提现或人事变更
     */
    private String multiSigAddress;
    /**
     * 充值交易提交器
     * 用于异构链组件解析到监听的交易，发送给Nerve核心组件
     */
    private IDepositTxSubmitter depositTxSubmitter;
    /**
     * 发送的提现或者人事变更交易的状态确认器
     * 用于异构链交易确认后，发送给Nerve核心组件
     */
    private ITxConfirmedProcessor txConfirmedProcessor;
    /**
     * 核心模块向异构链组件提供的查询api
     */
    private IConverterCoreApi converterCoreApi;
    /**
     * 合约多签类型的异构链，合约升级后，异构链组件发送新的流程处理接口给Nerve核心组件
     */
    private IHeterogeneousUpgrade heterogeneousUpgrade;

    public String getMultiSigAddress() {
        return multiSigAddress;
    }

    public void setMultiSigAddress(String multiSigAddress) {
        this.multiSigAddress = multiSigAddress;
    }

    public IDepositTxSubmitter getDepositTxSubmitter() {
        return depositTxSubmitter;
    }

    public void setDepositTxSubmitter(IDepositTxSubmitter depositTxSubmitter) {
        this.depositTxSubmitter = depositTxSubmitter;
    }

    public ITxConfirmedProcessor getTxConfirmedProcessor() {
        return txConfirmedProcessor;
    }

    public void setTxConfirmedProcessor(ITxConfirmedProcessor txConfirmedProcessor) {
        this.txConfirmedProcessor = txConfirmedProcessor;
    }

    public IConverterCoreApi getConverterCoreApi() {
        return converterCoreApi;
    }

    public void setConverterCoreApi(IConverterCoreApi converterCoreApi) {
        this.converterCoreApi = converterCoreApi;
    }

    public IHeterogeneousUpgrade getHeterogeneousUpgrade() {
        return heterogeneousUpgrade;
    }

    public void setHeterogeneousUpgrade(IHeterogeneousUpgrade heterogeneousUpgrade) {
        this.heterogeneousUpgrade = heterogeneousUpgrade;
    }
}
