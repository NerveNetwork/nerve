/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.network.task;

import io.nuls.network.manager.TimeManager;
import io.nuls.network.utils.LoggerUtil;

/**
 * Time service category：Used to synchronize network standard time
 * Time service class:Used to synchronize network standard time.
 *
 * @author vivi & lan
 */
public class TimeTask implements Runnable {
    TimeManager timeManager = TimeManager.getInstance();

    /**
     * Loop calling synchronous network time method
     * Loop call synchronous network time method.
     */
    @Override
    public void run() {
        try {
            long lastTime = System.currentTimeMillis();
            timeManager.syncWebTime();
            while (true) {
                long newTime = System.currentTimeMillis();
                if (Math.abs(newTime - lastTime) > TimeManager.TIME_OFFSET_BOUNDARY) {
                    LoggerUtil.COMMON_LOG.debug("-----local time changed ：{}------", newTime - lastTime);
                    timeManager.syncWebTime();
                } else if (TimeManager.currentTimeMillis() - TimeManager.lastSyncTime > TimeManager.NET_REFRESH_TIME) {
                    //Update network time periodically
                    LoggerUtil.COMMON_LOG.debug("-----TimeManager refresh ntp time------");
                    timeManager.syncWebTime();
                }
                lastTime = newTime;
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    LoggerUtil.COMMON_LOG.error(e);
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error("--------SyncTimeTask error-------", e);
        }
    }
}
