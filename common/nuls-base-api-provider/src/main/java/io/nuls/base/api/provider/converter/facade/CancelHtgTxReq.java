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

package io.nuls.base.api.provider.converter.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @author: Mimi
 * @date: 2021/5/30
 */
public class CancelHtgTxReq extends BaseReq {

    private int heterogeneousChainId;
    private String heterogeneousAddress;
    private String nonce;
    private String priceGwei;

    public CancelHtgTxReq() {
    }

    public CancelHtgTxReq(int heterogeneousChainId, String heterogeneousAddress, String nonce, String priceGwei) {
        this.heterogeneousChainId = heterogeneousChainId;
        this.heterogeneousAddress = heterogeneousAddress;
        this.nonce = nonce;
        this.priceGwei = priceGwei;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getPriceGwei() {
        return priceGwei;
    }

    public void setPriceGwei(String priceGwei) {
        this.priceGwei = priceGwei;
    }

}
