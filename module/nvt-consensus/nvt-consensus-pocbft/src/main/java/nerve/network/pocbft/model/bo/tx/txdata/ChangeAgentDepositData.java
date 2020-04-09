package nerve.network.pocbft.model.bo.tx.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import java.io.IOException;
import java.math.BigInteger;

/**
 * 节点保证金变更
 * Agent deposit change
 *
 * @author: Jason
 * 2019/10/18
 */
public class ChangeAgentDepositData extends BaseNulsData {
    private byte[] address;
    private BigInteger amount;
    private NulsHash agentHash;

    public ChangeAgentDepositData(){}
    public ChangeAgentDepositData(byte[] address,BigInteger amount,NulsHash agentHash){
        this.address = address;
        this.amount = amount;
        this.agentHash = agentHash;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(address);
        stream.writeBigInteger(amount);
        stream.write(agentHash.getBytes());

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.amount = byteBuffer.readBigInteger();
        this.agentHash = byteBuffer.readHash();
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfBigInteger();
        size += NulsHash.HASH_LENGTH;
        return size;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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
}
