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
import java.util.ArrayList;
import java.util.List;

/**
 * Store the sametoken All quotations in days
 * @author: Loki
 * @date: 2019/11/28
 */
public class NodeQuotationWrapperPO extends BaseNulsData {

    private List<NodeQuotationPO> list;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int length = list == null ? 0 : list.size();
        stream.writeVarInt(length);
        if (null != list) {
            for (NodeQuotationPO nodeQuotationPO : list) {
                stream.writeNulsData(nodeQuotationPO);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int length = (int) byteBuffer.readVarInt();
        if (0 < length) {
            List<NodeQuotationPO> nodeQuotationPOList = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                nodeQuotationPOList.add(byteBuffer.readNulsData(new NodeQuotationPO()));
            }
            this.list = nodeQuotationPOList;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(this.list == null ? 0 : this.list.size());
        if (null != this.list) {
            for (NodeQuotationPO nodeQuotationPO : this.list) {
                size += SerializeUtils.sizeOfNulsData(nodeQuotationPO);
            }
        }
        return size;
    }

    public NodeQuotationWrapperPO() {
    }

    public NodeQuotationWrapperPO(List<NodeQuotationPO> list) {
        this.list = list;
    }

    public List<NodeQuotationPO> getList() {
        return list;
    }

    public void setList(List<NodeQuotationPO> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        if(null == this.list){
            return null;
        }
        StringBuilder builder = new StringBuilder();
        list.forEach(k -> builder.append(k.getTxHash()).append(System.lineSeparator()));
        return builder.toString();
    }
}
