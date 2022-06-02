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

package network.nerve.converter.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.HeterogeneousSign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 当前虚拟银行异构链地址对交易的签名消息
 * @author: Loki
 * @date: 2020/8/31
 */
public class VirtualBankSignMessage extends BaseBusinessMessage {

    private static Comparator listSignSort = new Comparator<HeterogeneousSign>() {
        @Override
        public int compare(HeterogeneousSign o1, HeterogeneousSign o2) {
            if (o1.getHeterogeneousAddress().getChainId() > o2.getHeterogeneousAddress().getChainId()) {
                return 1;
            } else if (o1.getHeterogeneousAddress().getChainId() < o2.getHeterogeneousAddress().getChainId()) {
                return -1;
            }
            return o1.getHeterogeneousAddress().getAddress().compareTo(o2.getHeterogeneousAddress().getAddress());
        }
    };
    /**
     * 1 - 准备阶段，2 - 非准备，执行阶段
     */
    private int prepare;
    /**
     * 该交易所在区块的虚拟银行成员总数
     * (不算当前加入, 要算当前退出)
     */
    private int virtualBankTotal;

    /**
     * nerve 链内交易hash
     */
    private NulsHash hash;

    /**
     * 每个节点, 签多个异构链
     * 每个异构链地址一个签名
     */
    private List<HeterogeneousSign> listSign;



    public VirtualBankSignMessage() {
    }

    public VirtualBankSignMessage(int prepare, int virtualBankTotal, NulsHash hash, List<HeterogeneousSign> listSign) {
        this.prepare = prepare;
        this.virtualBankTotal = virtualBankTotal;
        this.hash = hash;
        this.listSign = listSign;
    }

    public ComponentSignMessage toComponentSignMessage() {
        ComponentSignMessage message = new ComponentSignMessage(virtualBankTotal, hash, listSign);
        return message;
    }

    public static VirtualBankSignMessage of(ComponentSignMessage message, int prepare) {
        VirtualBankSignMessage msg = new VirtualBankSignMessage(prepare, message.getVirtualBankTotal(), message.getHash(), message.getListSign());
        return msg;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(prepare);
        stream.writeUint16(virtualBankTotal);
        stream.write(hash.getBytes());
        int listSize = listSign == null ? 0 : listSign.size();
        stream.writeUint16(listSize);
        if(null != listSign){
            for(HeterogeneousSign sign : listSign){
                stream.writeNulsData(sign);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer buffer) throws NulsException {
        this.prepare = buffer.readUint16();
        this.virtualBankTotal = buffer.readUint16();
        this.hash = buffer.readHash();
        int listSize = buffer.readUint16();
        if(0 < listSize){
            List<HeterogeneousSign> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(buffer.readNulsData(new HeterogeneousSign()));
            }
            this.listSign = list;
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfInt16();
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        if (null != listSign) {
            for(HeterogeneousSign sign : listSign){
                size += SerializeUtils.sizeOfNulsData(sign);
            }
        }
        return size;
    }

    public int getPrepare() {
        return prepare;
    }

    public void setPrepare(int prepare) {
        this.prepare = prepare;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public List<HeterogeneousSign> getListSign() {
        return listSign;
    }

    public void setListSign(List<HeterogeneousSign> listSign) {
        this.listSign = listSign;
    }

    public int getVirtualBankTotal() {
        return virtualBankTotal;
    }

    public void setVirtualBankTotal(int virtualBankTotal) {
        this.virtualBankTotal = virtualBankTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualBankSignMessage that = (VirtualBankSignMessage) o;

        if (!hash.equals(that.hash)) return false;
        if (prepare != that.prepare) return false;
        if (listSign.size() != that.listSign.size()) return false;
        if (listSign.size() == 1 && !listSign.get(0).equals(that.listSign.get(0))) return false;
        if (listSign.size() > 1) {
            listSign.sort(listSignSort);
            that.listSign.sort(listSignSort);
            if (!listSign.get(0).equals(that.listSign.get(0))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = prepare;
        result = 31 * result + hash.hashCode();
        listSign.sort(listSignSort);
        for (HeterogeneousSign sign : listSign) {
            result = 31 * result + sign.hashCode();
        }
        return result;
    }
}
