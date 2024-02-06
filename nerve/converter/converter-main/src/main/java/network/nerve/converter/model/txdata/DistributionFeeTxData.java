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
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * subsidy,Distribution fee transaction
 * @author: Loki
 * @date: 2020/3/19
 */
public class DistributionFeeTxData extends BaseNulsData {

    /**
     * Basis for distributing handling feeshash
     * According to this transactionhash,Obtain transactions and transaction types
     * Perform different verification logic based on different transaction types
     * 1.Distribution of withdrawal fees,autograph
     * 2.Distribution of proposal voting fees
     */
    private NulsHash basisTxHash;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(basisTxHash.getBytes());
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.basisTxHash = byteBuffer.readHash();
    }

    @Override
    public int size() {
        return NulsHash.HASH_LENGTH;
    }

    public NulsHash getBasisTxHash() {
        return basisTxHash;
    }

    public void setBasisTxHash(NulsHash basisTxHash) {
        this.basisTxHash = basisTxHash;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\tbasisTxHash: %s", basisTxHash.toHex())).append(lineSeparator);
        return builder.toString();
    }
}
