package network.nerve.swap.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author Niels
 */
public class FarmUserInfoPO extends BaseNulsData {
    private NulsHash farmHash;
    private byte[] userAddress;
    private BigInteger amount;
    private BigInteger rewardDebt;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(amount);
        stream.writeBigInteger(rewardDebt);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.amount = byteBuffer.readBigInteger();
        this.rewardDebt = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        return 2 * SerializeUtils.sizeOfBigInteger();
    }

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
    }

    public byte[] getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(byte[] userAddress) {
        this.userAddress = userAddress;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public BigInteger getRewardDebt() {
        return rewardDebt;
    }

    public void setRewardDebt(BigInteger rewardDebt) {
        this.rewardDebt = rewardDebt;
    }
}
