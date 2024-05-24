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

package network.nerve.converter.btc.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Confirm contract upgrade transactiontxdata
 */
public class CheckWithdrawalUsedUTXOData extends BaseNulsData {

    private List<UsedUTXOData> usedUTXODataList;

    public CheckWithdrawalUsedUTXOData() {
    }

    public CheckWithdrawalUsedUTXOData(List<UsedUTXOData> usedUTXODataList) {
        this.usedUTXODataList = usedUTXODataList;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int listSize = usedUTXODataList == null ? 0 : usedUTXODataList.size();
        stream.writeUint16(listSize);
        if(null != usedUTXODataList){
            for(UsedUTXOData utxoData : usedUTXODataList){
                stream.writeNulsData(utxoData);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {

        int listSize = byteBuffer.readUint16();
        if(0 < listSize){
            List<UsedUTXOData> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(byteBuffer.readNulsData(new UsedUTXOData()));
            }
            this.usedUTXODataList = list;
        }
    }

    @Override
    public int size() {
        int size = 0;

        size += SerializeUtils.sizeOfUint16();
        if (null != usedUTXODataList) {
            for(UsedUTXOData utxoData : usedUTXODataList){
                size += SerializeUtils.sizeOfNulsData(utxoData);
            }
        }
        return size;
    }


    public List<UsedUTXOData> getUsedUTXODataList() {
        return usedUTXODataList;
    }

    public void setUsedUTXODataList(List<UsedUTXOData> usedUTXODataList) {
        this.usedUTXODataList = usedUTXODataList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"utxoDataList\":[");
        if (usedUTXODataList != null) {
            for (UsedUTXOData utxo : usedUTXODataList) {
                sb.append(utxo.toString()).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]}");
        return sb.toString();
    }
}
