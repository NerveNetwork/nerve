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

package io.nuls.account.rpc.call;

import io.nuls.account.model.bo.LatestBasicBlock;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.core.constant.SyncStatusEnum;

/**
 * @author: PierreLuo
 * @date: 2022/1/24
 */
public class BlockCall {

    public static LatestBasicBlock getLatestBasicBlock(int chainId, LatestBasicBlock latestBasicBlock) {
        ChainContext context = ContextManager.getContext(chainId);
        if (context == null) {
            return latestBasicBlock;
        }
        int syncStatus = context.getSimpleStatus();
        SyncStatusEnum syncStatusEnum = SyncStatusEnum.getEnum(syncStatus);
        if (null == syncStatusEnum) {
            return latestBasicBlock;
        }
        if (latestBasicBlock == null) {
            latestBasicBlock = new LatestBasicBlock();
        }
        if (null != context.getLatestBlock()) {
            latestBasicBlock.setTime(context.getLatestBlock().getHeader().getTime());
        }
        latestBasicBlock.setHeight(context.getLatestHeight());
        latestBasicBlock.setSyncStatusEnum(syncStatusEnum);
        return latestBasicBlock;
    }


}
