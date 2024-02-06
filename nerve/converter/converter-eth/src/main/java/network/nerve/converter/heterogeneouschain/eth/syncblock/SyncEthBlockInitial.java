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
package network.nerve.converter.heterogeneouschain.eth.syncblock;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Launcher for multi-threaded download tasks（No longer in use）
 *
 * @author: Mimi
 * @date: 2019-11-12
 */
@Deprecated
@Component
public class SyncEthBlockInitial {
    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;
    @Autowired
    private ETHWalletApi ethWalletApi;
    private Future<?> initEthBlockFutrue;
    private ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();

    public void initialEthBlock() {
        this.initEthBlockFutrue = singleThreadPool.submit(new EthBlockQueueTask(ethWalletApi, ethLocalBlockHelper));
    }

    public Future<?> getInitEthBlockFutrue() {
        return initEthBlockFutrue;
    }

    public ExecutorService getSingleThreadPool() {
        return singleThreadPool;
    }
}
