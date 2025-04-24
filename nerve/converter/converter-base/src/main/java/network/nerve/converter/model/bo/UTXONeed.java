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

import network.nerve.converter.btc.txdata.FtUTXOData;
import network.nerve.converter.btc.txdata.UTXOData;

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2025/4/3
 */
public class UTXONeed {

    private List<UTXOData> utxoDataList;
    private String script;
    private String ftAddress;
    private List<FtUTXOData> ftUTXODataList;

    public UTXONeed(List<UTXOData> utxoDataList) {
        this.utxoDataList = utxoDataList;
    }

    public UTXONeed(List<UTXOData> utxoDataList, List<FtUTXOData> ftUTXODataList) {
        this.utxoDataList = utxoDataList;
        this.ftUTXODataList = ftUTXODataList;
    }

    public UTXONeed(List<UTXOData> utxoDataList, String script, String ftAddress, List<FtUTXOData> ftUTXODataList) {
        this.utxoDataList = utxoDataList;
        this.script = script;
        this.ftAddress = ftAddress;
        this.ftUTXODataList = ftUTXODataList;
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

    public List<UTXOData> getUtxoDataList() {
        return utxoDataList;
    }

    public void setUtxoDataList(List<UTXOData> utxoDataList) {
        this.utxoDataList = utxoDataList;
    }

    public List<FtUTXOData> getFtUTXODataList() {
        return ftUTXODataList;
    }

    public void setFtUTXODataList(List<FtUTXOData> ftUTXODataList) {
        this.ftUTXODataList = ftUTXODataList;
    }
}
