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

package io.nuls.account.model.bo;

import io.nuls.base.data.BlockHeader;
import io.nuls.core.constant.SyncStatusEnum;

/**
 * 最新区块简略信息
 * @author: Loki
 * @date: 2020/3/16
 */
public class LatestBasicBlock {

    /**
     * 区块高度
     */
    private long height;

    /**
     * 区块时间
     */
    private long time;

    /**
     * 当前节点区块同步模式
     */
    private SyncStatusEnum syncStatusEnum;

    public LatestBasicBlock() {
    }

    public LatestBasicBlock(long height, long time, SyncStatusEnum syncStatusEnum) {
        this.height = height;
        this.time = time;
        this.syncStatusEnum = syncStatusEnum;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public SyncStatusEnum getSyncStatusEnum() {
        return syncStatusEnum;
    }

    public void setSyncStatusEnum(SyncStatusEnum syncStatusEnum) {
        this.syncStatusEnum = syncStatusEnum;
    }

    public BlockHeader toBlockHeader() {
        BlockHeader header = new BlockHeader();
        header.setTime(time);
        header.setHeight(height);
        return header;
    }
}
