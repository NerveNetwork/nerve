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

import network.nerve.converter.btc.txdata.UTXOData;

import java.util.List;

import static network.nerve.converter.utils.ConverterUtil.FEE_RATE_REBUILD;

/**
 * @author: PierreLuo
 * @date: 2024/3/7
 */
public class WithdrawalUTXO {
    private String nerveTxHash;
    private int htgChainId;
    private String currentMultiSignAddress;
    private int currenVirtualBankTotal;
    private long feeRate;
    private List<byte[]> pubs;
    private List<UTXOData> utxoDataList;

    public WithdrawalUTXO() {
    }

    public WithdrawalUTXO(String nerveTxHash, int htgChainId, String currentMultiSignAddress, int currenVirtualBankTotal, long feeRate, List<byte[]> pubs, List<UTXOData> utxoDataList) {
        this.nerveTxHash = nerveTxHash;
        this.htgChainId = htgChainId;
        this.currentMultiSignAddress = currentMultiSignAddress;
        this.currenVirtualBankTotal = currenVirtualBankTotal;
        this.feeRate = feeRate;
        this.pubs = pubs;
        this.utxoDataList = utxoDataList;
    }

    public String getCurrentMultiSignAddress() {
        return currentMultiSignAddress;
    }

    public void setCurrentMultiSignAddress(String currentMultiSignAddress) {
        this.currentMultiSignAddress = currentMultiSignAddress;
    }

    public int getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(int htgChainId) {
        this.htgChainId = htgChainId;
    }

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public int getCurrenVirtualBankTotal() {
        return currenVirtualBankTotal;
    }

    public void setCurrenVirtualBankTotal(int currenVirtualBankTotal) {
        this.currenVirtualBankTotal = currenVirtualBankTotal;
    }

    public long getFeeRate() {
        if (feeRate > FEE_RATE_REBUILD) {
            throw new RuntimeException("UTXO Pause");
        }
        return feeRate;
    }

    public void setFeeRate(long feeRate) {
        this.feeRate = feeRate;
    }

    public List<byte[]> getPubs() {
        return pubs;
    }

    public void setPubs(List<byte[]> pubs) {
        this.pubs = pubs;
    }

    public List<UTXOData> getUtxoDataList() {
        return utxoDataList;
    }

    public void setUtxoDataList(List<UTXOData> utxoDataList) {
        this.utxoDataList = utxoDataList;
    }
}
