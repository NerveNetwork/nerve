/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.config.ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT;

/**
 * 链内充值交易txdata
 *
 * @author: Loki
 * @date: 2020-02-17
 */
public class RechargeTxData extends BaseNulsData {

    /**
     * 异构链充值交易hash / 提案交易hash
     */
    private String originalTxHash;

    private int heterogeneousChainId;

    private String extend;

    public RechargeTxData() {
    }

    public RechargeTxData(String originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public RechargeTxData(String originalTxHash, int heterogeneousChainId) {
        this.originalTxHash = originalTxHash;
        this.heterogeneousChainId = heterogeneousChainId;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.originalTxHash);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            stream.writeUint16(this.heterogeneousChainId);
        }
        if (StringUtils.isNotBlank(extend)) {
            stream.writeString(extend);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.originalTxHash = byteBuffer.readString();
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            this.heterogeneousChainId = byteBuffer.readUint16();
        }
        if (!byteBuffer.isFinished()) {
            this.extend = byteBuffer.readString();
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.originalTxHash);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            size += SerializeUtils.sizeOfUint16();
        }
        if (StringUtils.isNotBlank(extend)) {
            size += SerializeUtils.sizeOfString(extend);
        }
        return size;
    }

    public String getOriginalTxHash() {
        return originalTxHash;
    }

    public void setOriginalTxHash(String originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\toriginalTxHash: %s", originalTxHash)).append(lineSeparator);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        }
        if (StringUtils.isNotBlank(extend)) {
            builder.append(String.format("\textend: %s", extend)).append(lineSeparator);
        }
        return builder.toString();
    }
}
