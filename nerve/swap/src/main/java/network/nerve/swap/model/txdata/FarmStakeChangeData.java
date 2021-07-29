package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 包括stake和withdraw两种操作
 *
 * @author Niels
 */
public class FarmStakeChangeData extends BaseNulsData {
    private NulsHash farmHash;
    private BigInteger amount;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(farmHash.getBytes());
        stream.writeBigInteger(amount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.farmHash = byteBuffer.readHash();
        this.amount = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        return NulsHash.HASH_LENGTH + SerializeUtils.sizeOfBigInteger();
    }

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }
}
