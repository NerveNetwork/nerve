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

package network.nerve.converter.model.vo;

import io.nuls.core.crypto.HexUtil;
import network.nerve.converter.btc.txdata.FtUTXOData;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Confirm contract upgrade transactiontxdata
 */
public class WithdrawalUTXOVO {

    private String nerveTxHash;
    private int htgChainId;
    private String currentMultiSignAddress;
    private int currentVirtualBankTotal;
    private long feeRate;
    private List<String> pubs;
    private List<UTXOData> utxoDataList;
    private String script;
    private String ftAddress;
    private List<FtUTXOData> ftUtxoDataList;

    public WithdrawalUTXOVO() {
    }

    public WithdrawalUTXOVO(WithdrawalUTXOTxData txData) {
        this.nerveTxHash = txData.getNerveTxHash();
        this.htgChainId = txData.getHtgChainId();
        this.currentMultiSignAddress = txData.getCurrentMultiSignAddress();
        this.currentVirtualBankTotal = txData.getCurrentVirtualBankTotal();
        this.feeRate = txData.getFeeRate();
        this.pubs = txData.getPubs().stream().map(b -> HexUtil.encode(b)).collect(Collectors.toList());
        this.utxoDataList = txData.getUtxoDataList();
        this.script = txData.getScript();
        this.ftAddress = txData.getFtAddress();
        this.ftUtxoDataList = txData.getFtUtxoDataList();
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getFtAddress() {
        return ftAddress;
    }

    public void setFtAddress(String ftAddress) {
        this.ftAddress = ftAddress;
    }

    public List<FtUTXOData> getFtUtxoDataList() {
        return ftUtxoDataList;
    }

    public void setFtUtxoDataList(List<FtUTXOData> ftUtxoDataList) {
        this.ftUtxoDataList = ftUtxoDataList;
    }

    public int getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(int htgChainId) {
        this.htgChainId = htgChainId;
    }

    public String getCurrentMultiSignAddress() {
        return currentMultiSignAddress;
    }

    public void setCurrentMultiSignAddress(String currentMultiSignAddress) {
        this.currentMultiSignAddress = currentMultiSignAddress;
    }

    public long getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(long feeRate) {
        this.feeRate = feeRate;
    }

    public int getCurrentVirtualBankTotal() {
        return currentVirtualBankTotal;
    }

    public void setCurrentVirtualBankTotal(int currentVirtualBankTotal) {
        this.currentVirtualBankTotal = currentVirtualBankTotal;
    }

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public List<String> getPubs() {
        return pubs;
    }

    public void setPubs(List<String> pubs) {
        this.pubs = pubs;
    }

    public List<UTXOData> getUtxoDataList() {
        return utxoDataList;
    }

    public void setUtxoDataList(List<UTXOData> utxoDataList) {
        this.utxoDataList = utxoDataList;
    }

}
