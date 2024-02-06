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
package network.nerve.converter.core.heterogeneous.task;

import network.nerve.converter.core.api.ConverterCoreApi;

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/11/17
 */
public class HtgWaitingTxInvokeDataHandlerTask implements Runnable{

    private ConverterCoreApi converterCoreApi;

    public HtgWaitingTxInvokeDataHandlerTask(ConverterCoreApi converterCoreApi) {
        this.converterCoreApi = converterCoreApi;
    }

    @Override
    public void run() {
        try {
            List<Runnable> runnables = converterCoreApi.getHtgWaitingTxInvokeDataHandlers();
            if (runnables.isEmpty()) {
                return;
            }
            for (Runnable runnable : runnables) {
                runnable.run();
            }
        } catch (Exception e) {
            converterCoreApi.logger().warn("Heterogeneous chain transaction call data waiting for task execution exception: {}", e.getMessage());
        }
    }
}
