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
 * @author Niels
 */
public class FarmUpdateData extends BaseNulsData {
    private NulsHash farmHash;
    private BigInteger newSyrupPerBlock;
    private short changeType;//0：Increase,1：reduce
    private BigInteger changeTotalSyrupAmount;
    private long withdrawLockTime;
    private long stopHeight;

    private long syrupLockTime;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(farmHash.getBytes());
        stream.writeBigInteger(newSyrupPerBlock);
        stream.writeUint8(changeType);
        stream.writeBigInteger(changeTotalSyrupAmount);
        stream.writeInt64(withdrawLockTime);
        if (stopHeight >= 0) {
            stream.writeInt64(stopHeight);
        }
        if (syrupLockTime > 0) {
            stream.writeInt64(syrupLockTime);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.farmHash = byteBuffer.readHash();
        this.newSyrupPerBlock = byteBuffer.readBigInteger();
        this.changeType = byteBuffer.readUint8();
        this.changeTotalSyrupAmount = byteBuffer.readBigInteger();
        this.withdrawLockTime = byteBuffer.readInt64();
        if (!byteBuffer.isFinished()) {
            this.stopHeight = byteBuffer.readInt64();
        }
        if (!byteBuffer.isFinished()) {
            this.syrupLockTime = byteBuffer.readInt64();
        }
    }

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfUint8();
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfInt64();
        if (stopHeight >= 0) {
            size += SerializeUtils.sizeOfInt64();
        }
        if (syrupLockTime > 0) {
            size += SerializeUtils.sizeOfInt64();
        }
        return size;
    }

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
    }

    public BigInteger getNewSyrupPerBlock() {
        return newSyrupPerBlock;
    }

    public void setNewSyrupPerBlock(BigInteger newSyrupPerBlock) {
        this.newSyrupPerBlock = newSyrupPerBlock;
    }

    public BigInteger getChangeTotalSyrupAmount() {
        return changeTotalSyrupAmount;
    }

    public void setChangeTotalSyrupAmount(BigInteger changeTotalSyrupAmount) {
        this.changeTotalSyrupAmount = changeTotalSyrupAmount;
    }

    public long getWithdrawLockTime() {
        return withdrawLockTime;
    }

    public void setWithdrawLockTime(long withdrawLockTime) {
        this.withdrawLockTime = withdrawLockTime;
    }

    public short getChangeType() {
        return changeType;
    }

    public void setChangeType(short changeType) {
        this.changeType = changeType;
    }

    public long getStopHeight() {
        return stopHeight;
    }

    public void setStopHeight(long stopHeight) {
        this.stopHeight = stopHeight;
    }

    public long getSyrupLockTime() {
        return syrupLockTime;
    }

    public void setSyrupLockTime(long syrupLockTime) {
        this.syrupLockTime = syrupLockTime;
    }
}
