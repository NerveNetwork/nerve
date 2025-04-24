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
package network.nerve.converter.heterogeneouschain.tbc.model;

import com.neemre.btcdcli4j.core.domain.RawTransaction;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2025/2/19
 */
public class TbcRawTransaction {
    private RawTransaction tx;
    //private Map<String, RawOutput> preVoutMap;
    private Map<String, Map> preTxMap;
    private FtTransferInfo ftTransferInfo;

    public TbcRawTransaction(RawTransaction tx) {
        this.tx = tx;
        this.preTxMap = new HashMap<>();
    }

    public FtTransferInfo getFtTransferInfo() {
        return ftTransferInfo;
    }

    public void setFtTransferInfo(FtTransferInfo ftTransferInfo) {
        this.ftTransferInfo = ftTransferInfo;
    }

    public RawTransaction getTx() {
        return tx;
    }

    public void setTx(RawTransaction tx) {
        this.tx = tx;
    }

    public Map<String, Map> getPreTxMap() {
        return preTxMap;
    }

    public void setPreTxMap(Map<String, Map> preTxMap) {
        this.preTxMap = preTxMap;
    }

    //public Map<String, RawOutput> getPreVoutMap() {
    //    return preVoutMap;
    //}
    //
    //public void setPreVoutMap(Map<String, RawOutput> preVoutMap) {
    //    this.preVoutMap = preVoutMap;
    //}
}
