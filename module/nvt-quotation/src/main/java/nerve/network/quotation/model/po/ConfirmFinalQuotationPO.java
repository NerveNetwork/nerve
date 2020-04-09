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

package nerve.network.quotation.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * 喂价最终上链业务数据存储结构
 * @author: Chino
 * @date: 2020-02-19
 */
public class ConfirmFinalQuotationPO extends BaseNulsData {

    private String anchorToken;

    /**
     * 确认区块时间的日期字符串
     */
    private String date;

    private double price;

    private String txHash;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.anchorToken);
        stream.writeString(this.date);
        stream.writeDouble(this.price);
        stream.writeString(this.txHash);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.anchorToken = byteBuffer.readString();
        this.date = byteBuffer.readString();
        this.price = byteBuffer.readDouble();
        this.txHash = byteBuffer.readString();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.anchorToken);
        size += SerializeUtils.sizeOfString(this.date);
        size += SerializeUtils.sizeOfDouble(this.price);
        size += SerializeUtils.sizeOfString(this.txHash);
        return size;
    }

    public String getAnchorToken() {
        return anchorToken;
    }

    public void setAnchorToken(String anchorToken) {
        this.anchorToken = anchorToken;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
