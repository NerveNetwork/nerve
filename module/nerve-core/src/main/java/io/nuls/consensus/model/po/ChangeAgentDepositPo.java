package io.nuls.consensus.model.po;

import io.nuls.consensus.model.bo.tx.txdata.ChangeAgentDepositData;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Node margin change
 * Agent deposit change
 *
 * @author tag
 * 2019/10/18
 */
public class ChangeAgentDepositPo extends BaseNulsData {
    private BigInteger amount;
    private NulsHash agentHash;
    private long time;
    private NulsHash txHash;
    private long blockHeight = -1L;

    public ChangeAgentDepositPo(){}

    public ChangeAgentDepositPo(ChangeAgentDepositData data, long time, NulsHash txHash, long blockHeight){
        this.amount = data.getAmount();
        this.agentHash = data.getAgentHash();
        this.time = time;
        this.txHash = txHash;
        this.blockHeight = blockHeight;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(amount);
        stream.write(agentHash.getBytes());
        stream.writeUint48(time);
        stream.write(txHash.getBytes());
        stream.writeVarInt(blockHeight);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.amount = byteBuffer.readBigInteger();
        this.agentHash = byteBuffer.readHash();
        this.time = byteBuffer.readUint48();
        this.txHash = byteBuffer.readHash();
        this.blockHeight = byteBuffer.readVarInt();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint48();
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfVarInt(blockHeight);
        return size;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public NulsHash getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(NulsHash agentHash) {
        this.agentHash = agentHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public NulsHash getTxHash() {
        return txHash;
    }

    public void setTxHash(NulsHash txHash) {
        this.txHash = txHash;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }
}
