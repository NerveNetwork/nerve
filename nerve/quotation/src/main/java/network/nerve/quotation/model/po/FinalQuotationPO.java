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

package network.nerve.quotation.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * The final quotation that can be used after calculation
 * @author: Loki
 * @date: 2019/11/27
 */
public class FinalQuotationPO extends BaseNulsData {

    private String token;
    private double price;
    /**
     * Release time Second level
     * time stamp/1000
     */
    private long launchTime;
    /**
     * The quotation time may be earlier than the release time Second level
     * time stamp/1000
     */
    private long quotationTime;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(token);
        stream.writeDouble(price);
        stream.writeUint32(launchTime);
        stream.writeUint32(quotationTime);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.token = byteBuffer.readString();
        this.price = byteBuffer.readDouble();
        this.launchTime = byteBuffer.readUint32();
        this.quotationTime = byteBuffer.readUint32();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.token);
        size += SerializeUtils.sizeOfDouble(this.price);
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        return size;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(long launchTime) {
        this.launchTime = launchTime;
    }

    public long getQuotationTime() {
        return quotationTime;
    }

    public void setQuotationTime(long quotationTime) {
        this.quotationTime = quotationTime;
    }
    
}
