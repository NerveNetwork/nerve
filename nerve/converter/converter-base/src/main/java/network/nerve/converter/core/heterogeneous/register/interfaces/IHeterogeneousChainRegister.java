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

package network.nerve.converter.core.heterogeneous.register.interfaces;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

/**
 * 注册流程如下, Nerve核心将按照如下顺序调用异构链
 * @author: Mimi
 * @date: 2020-03-08
 */
public interface IHeterogeneousChainRegister {
    /**
     * 1. Nerve核心将调用此函数获取异构链的chainId
     */
    int getChainId();
    /**
     * 2. Nerve核心将调用此函数初始化异构链的数据，如初始化配置、DB
     */
    void init(HeterogeneousCfg config, NulsLogger logger) throws Exception;

    /**
     * 3. 获取异构链的基本信息，如chainId、symbol、初始多签地址
     */
    HeterogeneousChainInfo getChainInfo();

    /**
     * 4. 获取异构链实现的Nerve接口规范
     */
    IHeterogeneousChainDocking getDockingImpl();

    /**
     * 5. Nerve核心调用此函数，返回注册信息给异构链
     */
    void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception;
}
