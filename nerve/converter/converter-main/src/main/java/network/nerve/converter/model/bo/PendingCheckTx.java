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

import io.nuls.base.data.NulsHash;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.message.BroadcastHashSignMessage;

import java.util.List;

/**
 * @author: Loki
 * @date: 2020/4/29
 */
public class PendingCheckTx {
    /**
     * 检查次数
     */
    private int checkTimes;

    private NulsHash hash;

    /**
     * 原始交易hash
     */
    private String originalHash;

    /**
     * 交易类型
     */
    private int type;

    /**
     * 异构链hash列表
     */
    private List<HeterogeneousHash> heterogeneousHashList;

    public PendingCheckTx() {
    }
    public PendingCheckTx(BroadcastHashSignMessage msg) {
        this.checkTimes = ConverterConstant.MAX_CHECK_TIMES;
        this.hash = msg.getHash();
        this.originalHash = msg.getOriginalHash();
        this.type = msg.getType();
        this.heterogeneousHashList = msg.getHeterogeneousHashList();
    }

    public int getCheckTimes() {
        return checkTimes;
    }

    public void setCheckTimes(int checkTimes) {
        this.checkTimes = checkTimes;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<HeterogeneousHash> getHeterogeneousHashList() {
        return heterogeneousHashList;
    }

    public void setHeterogeneousHashList(List<HeterogeneousHash> heterogeneousHashList) {
        this.heterogeneousHashList = heterogeneousHashList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PendingCheckTx that = (PendingCheckTx) o;

        if (type != that.type) return false;
        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;
        return originalHash != null ? originalHash.equals(that.originalHash) : that.originalHash == null;
    }

    @Override
    public int hashCode() {
        int result = hash != null ? hash.hashCode() : 0;
        result = 31 * result + (originalHash != null ? originalHash.hashCode() : 0);
        result = 31 * result + type;
        return result;
    }
}
