/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 确认虚拟银行变更交易
 * 业务数据
 * @author: Loki
 * @date: 2020-03-11
 */
public class ConfirmedChangeVirtualBankTxData extends BaseNulsData {

    /**
     * 虚拟银行变更交易hash
     */
    private NulsHash changeVirtualBankTxHash;

    /**
     * 变更后虚拟银行全体成员节点地址列表
     */
    private List<byte[]> listAgents;

    /**
     * 异构链确认信息
     */
    private List<HeterogeneousConfirmedVirtualBank> listConfirmed;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(changeVirtualBankTxHash.getBytes());
        int agCount = listAgents == null ? 0 : listAgents.size();
        stream.writeUint16(agCount);
        if(null != listAgents){
            for(byte[] addressBytes : listAgents){
                stream.write(addressBytes);
            }
        }
        int count = listConfirmed == null ? 0 : listConfirmed.size();
        stream.writeUint16(count);
        if(null != listConfirmed){
            for(HeterogeneousConfirmedVirtualBank hcvb : listConfirmed){
                stream.writeNulsData(hcvb);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.changeVirtualBankTxHash = byteBuffer.readHash();
        int agCount = byteBuffer.readUint16();
        if(0 < agCount){
            List<byte[]> agents = new ArrayList<>();
            for(int i = 0; i< agCount; i++){
                agents.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
            }
            this.listAgents = agents;
        }

        int count = byteBuffer.readUint16();
        if(0 < count){
            List<HeterogeneousConfirmedVirtualBank> list = new ArrayList<>();
            for(int i = 0; i< count; i++){
                list.add(byteBuffer.readNulsData(new HeterogeneousConfirmedVirtualBank()));
            }
            this.listConfirmed = list;
        }
    }

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        if (null != listAgents) {
            size += Address.ADDRESS_LENGTH * listAgents.size();
        }
        size += SerializeUtils.sizeOfUint16();
        if (null != listConfirmed) {
            for(HeterogeneousConfirmedVirtualBank hcvb : listConfirmed){
                size += SerializeUtils.sizeOfNulsData(hcvb);
            }
        }
        return size;
    }


    public NulsHash getChangeVirtualBankTxHash() {
        return changeVirtualBankTxHash;
    }

    public void setChangeVirtualBankTxHash(NulsHash changeVirtualBankTxHash) {
        this.changeVirtualBankTxHash = changeVirtualBankTxHash;
    }

    public List<byte[]> getListAgents() {
        return listAgents;
    }

    public void setListAgents(List<byte[]> listAgents) {
        this.listAgents = listAgents;
    }

    public List<HeterogeneousConfirmedVirtualBank> getListConfirmed() {
        return listConfirmed;
    }

    public void setListConfirmed(List<HeterogeneousConfirmedVirtualBank> listConfirmed) {
        this.listConfirmed = listConfirmed;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\tchangeVirtualBankTxHash: %s", changeVirtualBankTxHash.toHex())).append(lineSeparator);
        if (listAgents == null) {
            builder.append("\tlistAgents: null").append(lineSeparator);
        } else if (listAgents.size() == 0) {
            builder.append("\tlistAgents: size 0").append(lineSeparator);
        } else {
            builder.append("\tlistAgents:").append(lineSeparator);
            for(byte[] bytes : listAgents){
                builder.append(String.format("\t\t%s", AddressTool.getStringAddressByBytes(bytes))).append(lineSeparator);
            }
        }

        if (listConfirmed == null) {
            builder.append("\tlistConfirmed: null").append(lineSeparator);
        } else if (listConfirmed.size() == 0) {
            builder.append("\tlistConfirmed: size 0").append(lineSeparator);
        } else {
            builder.append("\tlistConfirmed:").append(lineSeparator);
            for (int i = 0; i < listConfirmed.size(); i++) {
                HeterogeneousConfirmedVirtualBank bank = listConfirmed.get(i);
                builder.append(String.format("\t\tbanks-%s:", i)).append(lineSeparator);
                builder.append(String.format("\t\t\theterogeneousChainId: %s", bank.getHeterogeneousChainId())).append(lineSeparator);
                builder.append(String.format("\t\t\theterogeneousAddress: %s", bank.getHeterogeneousAddress())).append(lineSeparator);
                builder.append(String.format("\t\t\theterogeneousTxHash: %s", bank.getHeterogeneousTxHash())).append(lineSeparator);
                builder.append(String.format("\t\t\teffectiveTime: %s", bank.getEffectiveTime())).append(lineSeparator);

                if(null != bank.getSignedHeterogeneousAddress()){
                    builder.append(String.format("\t\t\tsignedHeterogeneousAddress: %s", bank.getEffectiveTime())).append(lineSeparator);
                    for(HeterogeneousAddress address: bank.getSignedHeterogeneousAddress()){
                        builder.append(String.format("\t\t\t\t%s", address.getAddress())).append(lineSeparator);
                    }
                }
                builder.append(lineSeparator);
            }
        }

        return builder.toString();
    }
}
