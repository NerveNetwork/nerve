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

package network.nerve.converter.btc.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Confirm contract upgrade transactiontxdata
 */
public class WithdrawalUTXORebuildPO extends BaseNulsData {

    private long baseFeeRate;
    private Set<String> nerveTxHashSet;

    public WithdrawalUTXORebuildPO() {
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(baseFeeRate);
        int size = nerveTxHashSet == null ? 0 : nerveTxHashSet.size();
        stream.writeUint16(size);
        if(null != nerveTxHashSet){
            for(String hash : nerveTxHashSet){
                stream.write(HexUtil.decode(hash));
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.baseFeeRate = byteBuffer.readUint32();
        int size = byteBuffer.readUint16();
        if(0 < size){
            this.nerveTxHashSet = new HashSet<>();
            for(int i = 0; i< size; i++){
                nerveTxHashSet.add(HexUtil.encode(byteBuffer.readBytes(NulsHash.HASH_LENGTH)));
            }
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint16();
        if (nerveTxHashSet != null) {
            size += NulsHash.HASH_LENGTH * nerveTxHashSet.size();
        }
        return size;
    }

    public long getBaseFeeRate() {
        return baseFeeRate;
    }

    public void setBaseFeeRate(long baseFeeRate) {
        this.baseFeeRate = baseFeeRate;
    }

    public Set<String> getNerveTxHashSet() {
        return nerveTxHashSet;
    }

    public void setNerveTxHashSet(Set<String> nerveTxHashSet) {
        this.nerveTxHashSet = nerveTxHashSet;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"baseFeeRate\":")
                .append(baseFeeRate);
        sb.append(",\"nerveTxHashSet\":[");
        if (nerveTxHashSet != null) {
            for (String hash : nerveTxHashSet) {
                sb.append('\"').append(hash).append('\"').append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]}");
        return sb.toString();
    }
}
