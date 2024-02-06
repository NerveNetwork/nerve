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

package network.nerve.converter.core.heterogeneous.register.interfaces;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

/**
 * The registration process is as follows, NerveThe core will call heterogeneous chains in the following order
 * @author: Mimi
 * @date: 2020-03-08
 */
public interface IHeterogeneousChainRegister {

    /**
     * 1. NerveThe core will call this function to obtain the information of heterogeneous chainschainId
     */
    int getChainId();
    /**
     * 2. NerveThe core will call this function to initialize the data of heterogeneous chains, such as initializing configurations、DB
     */
    String init(HeterogeneousCfg config, NulsLogger logger) throws Exception;

    /**
     * 3. Obtain basic information about heterogeneous chains, such aschainId、symbol、Initial multi signature address
     */
    HeterogeneousChainInfo getChainInfo();

    /**
     * 4. Obtain heterogeneous chain implementationNerveinterface specification
     */
    IHeterogeneousChainDocking getDockingImpl();

    /**
     * 5. NerveThe core calls this function to return registration information to the heterogeneous chain
     */
    void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception;
    /**
     * CORE The order of executing heterogeneous chain registration
     */
    int order();
}
