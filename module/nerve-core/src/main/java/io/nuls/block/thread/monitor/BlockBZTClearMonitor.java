/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.thread.monitor;

import io.nuls.base.data.NulsHash;
import io.nuls.block.model.BlockSaveTemp;
import io.nuls.block.model.ChainContext;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.util.*;

/**
 * bztVerified block data cleaning monitor
 * Start every fixed time interval
 * If it is found that the cached data exceeds the limit100sizeStart time comparison for data cleaning
 *
 * @author ljs
 * @version 1.0
 * @date 19-12-14 afternoon3:53
 */
public class BlockBZTClearMonitor extends BaseMonitor {
    /**
     * If there are200The blocks that have passed basic verification are2If Byzantium has not been approved within minutes, it needs to be cleared and deleted
     * */
    private static final short MAX_TEMP_SIZE = 200;
    private static final short OVER_TIME_INTERVAL = 120;

    private static final BlockBZTClearMonitor INSTANCE = new BlockBZTClearMonitor();

    public static BlockBZTClearMonitor getInstance() {
        return INSTANCE;
    }

    @Override
    protected void process(int chainId, ChainContext context, NulsLogger commonLog) {
        Map<NulsHash, BlockSaveTemp> blockSaveTempMap = context.getBlockVerifyResult();
        long nowTime = NulsDateUtils.getCurrentTimeSeconds();
        try {
            if(blockSaveTempMap.size() > MAX_TEMP_SIZE){
                blockSaveTempMap.entrySet().removeIf(entry -> (nowTime - entry.getValue().getTime() > OVER_TIME_INTERVAL));
            }
        }catch(Exception e){
            commonLog.error(e);
        }
    }
}
