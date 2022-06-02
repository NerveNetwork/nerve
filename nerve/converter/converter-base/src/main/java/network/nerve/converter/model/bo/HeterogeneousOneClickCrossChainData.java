/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2022/3/22
 */
public class HeterogeneousOneClickCrossChainData {
    private BigInteger feeAmount;
    private int desChainId;
    private String desToAddress;
    private BigInteger tipping;
    private String tippingAddress;
    private String desExtend;

    public HeterogeneousOneClickCrossChainData() {
    }

    public HeterogeneousOneClickCrossChainData(BigInteger feeAmount, int desChainId, String desToAddress, BigInteger tipping, String tippingAddress, String desExtend) {
        this.feeAmount = feeAmount;
        this.desChainId = desChainId;
        this.desToAddress = desToAddress;
        this.tipping = tipping;
        this.tippingAddress = tippingAddress;
        this.desExtend = desExtend;
    }

    public BigInteger getTipping() {
        return tipping;
    }

    public void setTipping(BigInteger tipping) {
        this.tipping = tipping;
    }

    public String getTippingAddress() {
        return tippingAddress;
    }

    public void setTippingAddress(String tippingAddress) {
        this.tippingAddress = tippingAddress;
    }

    public BigInteger getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigInteger feeAmount) {
        this.feeAmount = feeAmount;
    }

    public int getDesChainId() {
        return desChainId;
    }

    public void setDesChainId(int desChainId) {
        this.desChainId = desChainId;
    }

    public String getDesToAddress() {
        return desToAddress;
    }

    public void setDesToAddress(String desToAddress) {
        this.desToAddress = desToAddress;
    }

    public String getDesExtend() {
        return desExtend;
    }

    public void setDesExtend(String desExtend) {
        this.desExtend = desExtend;
    }
}
