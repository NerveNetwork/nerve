/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.callback;

import nerve.network.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import nerve.network.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Chino
 * @date: 2020-02-17
 */
@Component
public class EthCallBackManager {

    private IDepositTxSubmitter depositTxSubmitter;
    private ITxConfirmedProcessor txConfirmedProcessor;

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
}
