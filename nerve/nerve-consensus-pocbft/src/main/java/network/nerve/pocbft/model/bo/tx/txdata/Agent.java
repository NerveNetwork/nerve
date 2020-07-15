/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package network.nerve.pocbft.model.bo.tx.txdata;


import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.model.dto.input.CreateAgentDTO;
import network.nerve.pocbft.model.po.AgentPo;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 节点信息类
 * Node information class
 *
 * @author tag
 * 2018/11/6
 */
@ApiModel(name = "节点信息")
public class Agent extends BaseNulsData {

    /**
     * 节点地址
     * agent address
     **/
    @ApiModelProperty(description = "节点地址")
    private byte[] agentAddress;

    /**
     * 打包地址
     * packing address
     **/
    @ApiModelProperty(description = "出块地址")
    private byte[] packingAddress;

    /**
     * 奖励地址
     * reward address
     */
    @ApiModelProperty(description = "奖励地址")
    private byte[] rewardAddress;

    /**
     * 保证金
     * deposit
     */
    @ApiModelProperty(description = "保证金")
    private BigInteger deposit;

    /**
     * 创建时间
     * create time
     **/
    @ApiModelProperty(description = "创建时间")
    private transient long time;

    /**
     * 所在区块高度
     * block height
     */
    @ApiModelProperty(description = "所在区块高度")
    private transient long blockHeight = -1L;

    /**
     * 该节点注销所在区块高度
     * Block height where the node logs out
     */
    @ApiModelProperty(description = "节点注销高度")
    private transient long delHeight = -1L;

    /**
     * 0:待共识 unConsensus, 1:共识中 consensus
     */
    @ApiModelProperty(description = "状态，0:待共识 unConsensus, 1:共识中 consensus")
    private transient int status;

    /**
     * 信誉值
     * credit value
     */
    @ApiModelProperty(description = "信誉值")
    private transient double creditVal;

    /**
     * 交易HASH
     * transaction hash
     */
    @ApiModelProperty(description = "创建该节点的交易HASH")
    private transient NulsHash txHash;

    /**
     * 别名不序列化
     * Aliases not serialized
     */
    @ApiModelProperty(description = "节点别名")
    private transient String alias;

    /**
     * 出块地址公钥
     * Aliases not serialized
     */
    @ApiModelProperty(description = "节点别名")
    private transient byte[] pubKey;

    private transient String packingAddressStr;

    public Agent() {
    }

    public Agent(CreateAgentDTO dto) {
        byte[] agentAddress = AddressTool.getAddress(dto.getAgentAddress());
        this.agentAddress = agentAddress;
        this.packingAddress = AddressTool.getAddress(dto.getPackingAddress());
        if (StringUtils.isBlank(dto.getRewardAddress())) {
            this.rewardAddress = agentAddress;
        } else {
            this.rewardAddress = AddressTool.getAddress(dto.getRewardAddress());
        }
        this.deposit = BigIntegerUtils.stringToBigInteger(dto.getDeposit());
    }

    public Agent(AgentPo po) {
        this.agentAddress = po.getAgentAddress();
        this.packingAddress = po.getPackingAddress();
        this.rewardAddress = po.getRewardAddress();
        this.deposit = po.getDeposit();
        this.blockHeight = po.getBlockHeight();
        this.delHeight = po.getDelHeight();
        this.txHash = po.getHash();
        this.time = po.getTime();
        this.pubKey = po.getPubKey();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += this.agentAddress.length;
        size += this.rewardAddress.length;
        size += this.packingAddress.length;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(deposit);
        stream.write(agentAddress);
        stream.write(packingAddress);
        stream.write(rewardAddress);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.deposit = byteBuffer.readBigInteger();
        this.agentAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.packingAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.rewardAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
    }

    public String getPackingAddressStr() {
        if (null == packingAddressStr) {
            packingAddressStr = AddressTool.getStringAddressByBytes(packingAddress);
        }
        return packingAddressStr;
    }

    public void setPackingAddressStr(String packingAddressStr) {
        this.packingAddressStr = packingAddressStr;
    }

    public byte[] getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(byte[] packingAddress) {
        this.packingAddress = packingAddress;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public void setCreditVal(double creditVal) {
        this.creditVal = creditVal;
    }

    public double getCreditVal() {
        return creditVal < 0d ? 0D : this.creditVal;
    }

    public double getRealCreditVal() {
        return this.creditVal;
    }

    public void setTxHash(NulsHash txHash) {
        this.txHash = txHash;
    }

    public NulsHash getTxHash() {
        return txHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getDelHeight() {
        return delHeight;
    }

    public void setDelHeight(long delHeight) {
        this.delHeight = delHeight;
    }

    public byte[] getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(byte[] agentAddress) {
        this.agentAddress = agentAddress;
    }

    public byte[] getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(byte[] rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    @Override
    public Agent clone() throws CloneNotSupportedException {
        return (Agent) super.clone();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }
}
