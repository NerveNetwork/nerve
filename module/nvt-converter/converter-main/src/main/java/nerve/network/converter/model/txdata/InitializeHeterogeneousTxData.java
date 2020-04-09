/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * 初始化异构链交易
 * 特殊的节点变更交易
 * @author: Chino
 * @date: 2020/3/20
 */
public class InitializeHeterogeneousTxData extends BaseNulsData {

    private int heterogeneousChainId;
    /**
     * 除种子节点外所有虚拟银行成员节点地址
     */
//    private List<byte[]> listDirector;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(heterogeneousChainId);
//        int count = listDirector == null ? 0 : listDirector.size();
//        stream.writeUint16(count);
//        if(null != listDirector){
//            for(byte[] addressBytes : listDirector){
//                stream.write(addressBytes);
//            }
//        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousChainId = byteBuffer.readUint16();
//        int count = byteBuffer.readUint16();
//        if(0 < count){
//            List<byte[]> listDirector = new ArrayList<>();
//            for(int i = 0; i< count; i++){
//                listDirector.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
//            }
//            this.listDirector = listDirector;
//        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
//        size += SerializeUtils.sizeOfUint16();
//        if (null != listDirector) {
//            size += Address.ADDRESS_LENGTH * listDirector.size();
//        }
        return size;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

//    public List<byte[]> getListDirector() {
//        return listDirector;
//    }
//
//    public void setListDirector(List<byte[]> listDirector) {
//        this.listDirector = listDirector;
//    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        return builder.toString();
    }
}
