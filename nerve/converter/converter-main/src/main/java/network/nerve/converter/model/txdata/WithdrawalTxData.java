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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.config.ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT;

/**
 * 提现交易txdata
 * @author: Loki
 * @date: 2020-02-17
 */
public class WithdrawalTxData extends BaseNulsData {

    /**
     * 提现到账的对应异构链地址
     */
    private String heterogeneousAddress;

    private int heterogeneousChainId;

    public WithdrawalTxData() {
    }

    public WithdrawalTxData(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public WithdrawalTxData(String heterogeneousAddress, int heterogeneousChainId) {
        this.heterogeneousAddress = heterogeneousAddress;
        this.heterogeneousChainId = heterogeneousChainId;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.heterogeneousAddress);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            stream.writeUint16(this.heterogeneousChainId);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousAddress = byteBuffer.readString();
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            this.heterogeneousChainId = byteBuffer.readUint16();
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(heterogeneousAddress);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            size += SerializeUtils.sizeOfUint16();
        }
        return size;
    }

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousAddress: %s", heterogeneousAddress)).append(lineSeparator);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        }
        return builder.toString();
    }
}
