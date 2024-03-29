/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.po.sub.FreezeHeightState;
import io.nuls.ledger.model.po.sub.FreezeLockTimeState;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.Ledger information corresponding to user address asset account
 * 2.The persistent object is the one confirmed by the block,Final information：containnonceValue, balance, and freeze information.
 * 3.keyvalue:address-assetChainId-assetId
 *
 * @author lanjinsheng
 */

public class AccountState extends BaseNulsData {

    private byte[] nonce = LedgerConstant.getInitNonceByte();
    /**
     * The processing time of the most recent frozen data in the ledger,Storage seconds
     */
    private long latestUnFreezeTime = 0;
    /**
     * Total account amount outgoing
     * correspondingcoindataInsidecoinfrom Accumulated value
     */
    private BigInteger totalFromAmount = BigInteger.ZERO;

    /**
     * Total account amount recorded
     * correspondingcoindataInsidecointo Accumulated value
     */
    private BigInteger totalToAmount = BigInteger.ZERO;


    /**
     * Assets frozen in accounts(Highly frozen)
     */
    private List<FreezeHeightState> freezeHeightStates = new ArrayList<>();

    /**
     * Assets frozen in accounts(Time freeze)
     */
    private List<FreezeLockTimeState> freezeLockTimeStates = new ArrayList<>();

    /**
     * Assets frozen in accounts(Permanent freeze)
     */
    private transient Map<String, FreezeLockTimeState> permanentLockMap = new HashMap<>();

    public AccountState() {
        super();
    }

    public AccountState(byte[] pNonce) {
        System.arraycopy(pNonce, 0, this.nonce, 0, LedgerConstant.NONCE_LENGHT);
    }

    /**
     * Obtain available account amount（Excluding locked amount）
     *
     * @return BigInteger
     */
    public BigInteger getAvailableAmount() {
        return totalToAmount.subtract(totalFromAmount);
    }

    public void addTotalFromAmount(BigInteger value) {
        totalFromAmount = totalFromAmount.add(value);
    }

    public void subtractTotalFromAmount(BigInteger value) {
        totalFromAmount = totalFromAmount.subtract(value);
    }

    public void addTotalToAmount(BigInteger value) {
        totalToAmount = totalToAmount.add(value);
    }

    /**
     * Obtain the total account amount（Including locked amount）
     *
     * @return BigInteger
     */
    public BigInteger getTotalAmount() {
        return totalToAmount.subtract(totalFromAmount).add(getFreezeTotal());
    }


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(nonce);
        stream.writeUint32(latestUnFreezeTime);
        stream.writeBigInteger(totalFromAmount);
        stream.writeBigInteger(totalToAmount);
        stream.writeUint32(freezeHeightStates.size());
        for (FreezeHeightState heightState : freezeHeightStates) {
            stream.writeNulsData(heightState);
        }
        stream.writeUint32(freezeLockTimeStates.size());
        for (FreezeLockTimeState lockTimeState : freezeLockTimeStates) {
            stream.writeNulsData(lockTimeState);
        }

        int count = permanentLockMap.size();
        stream.writeVarInt(count);
        if (count > 0) {
            for (Map.Entry<String, FreezeLockTimeState> entry : permanentLockMap.entrySet()) {
                stream.writeString(entry.getKey());
                stream.writeNulsData(entry.getValue());
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.nonce = byteBuffer.readBytes(8);
        this.latestUnFreezeTime = byteBuffer.readUint32();
        this.totalFromAmount = byteBuffer.readBigInteger();
        this.totalToAmount = byteBuffer.readBigInteger();
        int freezeHeightCount = (int) byteBuffer.readUint32();
        this.freezeHeightStates = new ArrayList<>(freezeHeightCount);
        for (int i = 0; i < freezeHeightCount; i++) {
            try {
                FreezeHeightState heightState = new FreezeHeightState();
                byteBuffer.readNulsData(heightState);
                this.freezeHeightStates.add(heightState);
            } catch (Exception e) {
                throw new NulsException(e);
            }
        }
        int freezeLockTimeCount = (int) byteBuffer.readUint32();
        this.freezeLockTimeStates = new ArrayList<>(freezeLockTimeCount);
        for (int i = 0; i < freezeLockTimeCount; i++) {
            try {
                FreezeLockTimeState lockTimeState = new FreezeLockTimeState();
                byteBuffer.readNulsData(lockTimeState);
                this.freezeLockTimeStates.add(lockTimeState);
            } catch (Exception e) {
                throw new NulsException(e);
            }
        }

        int count = (int) byteBuffer.readVarInt();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                String key = byteBuffer.readString();
                FreezeLockTimeState timeState = byteBuffer.readNulsData(new FreezeLockTimeState());
                this.permanentLockMap.put(key, timeState);
            }
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += nonce.length;
        size += SerializeUtils.sizeOfUint32();
        //totalFromAmount
        size += SerializeUtils.sizeOfBigInteger();
        //totalToAmount
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfUint32();
        for (FreezeHeightState heightState : freezeHeightStates) {
            size += SerializeUtils.sizeOfNulsData(heightState);
        }
        size += SerializeUtils.sizeOfUint32();
        for (FreezeLockTimeState lockTimeState : freezeLockTimeStates) {
            size += SerializeUtils.sizeOfNulsData(lockTimeState);
        }

        int count = permanentLockMap.size();
        size += SerializeUtils.sizeOfVarInt(count);
        if (count > 0) {
            for (Map.Entry<String, FreezeLockTimeState> entry : permanentLockMap.entrySet()) {
                size += SerializeUtils.sizeOfString(entry.getKey());
                size += entry.getValue().size();
            }
        }
        return size;
    }

    /**
     * Query user frozen amount
     *
     * @return
     */
    public BigInteger getFreezeTotal() {
        BigInteger freeze = BigInteger.ZERO;
        for (FreezeHeightState heightState : freezeHeightStates) {
            freeze = freeze.add(heightState.getAmount());
        }
        for (FreezeLockTimeState lockTimeState : permanentLockMap.values()) {
            freeze = freeze.add(lockTimeState.getAmount());
        }
        for (FreezeLockTimeState lockTimeState : freezeLockTimeStates) {
            freeze = freeze.add(lockTimeState.getAmount());
        }
        return freeze;
    }


    public AccountState deepClone() {
        AccountState orgAccountState = new AccountState();
        orgAccountState.setNonce(ByteUtils.copyOf(this.getNonce(), 8));
        orgAccountState.setLatestUnFreezeTime(this.getLatestUnFreezeTime());
        orgAccountState.setTotalFromAmount(this.getTotalFromAmount());
        orgAccountState.setTotalToAmount(this.getTotalToAmount());
        List<FreezeHeightState> heightStateArrayList = new ArrayList<>();
        heightStateArrayList.addAll(this.getFreezeHeightStates());
        orgAccountState.setFreezeHeightStates(heightStateArrayList);
        List<FreezeLockTimeState> lockTimeStateArrayList = new ArrayList<>();
        lockTimeStateArrayList.addAll(this.getFreezeLockTimeStates());
        orgAccountState.setFreezeLockTimeStates(lockTimeStateArrayList);
        orgAccountState.permanentLockMap.putAll(this.permanentLockMap);
        return orgAccountState;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public long getLatestUnFreezeTime() {
        return latestUnFreezeTime;
    }

    public void setLatestUnFreezeTime(long latestUnFreezeTime) {
        this.latestUnFreezeTime = latestUnFreezeTime;
    }

    public BigInteger getTotalFromAmount() {
        return totalFromAmount;
    }

    public void setTotalFromAmount(BigInteger totalFromAmount) {
        this.totalFromAmount = totalFromAmount;
    }

    public BigInteger getTotalToAmount() {
        return totalToAmount;
    }

    public void setTotalToAmount(BigInteger totalToAmount) {
        this.totalToAmount = totalToAmount;
    }

    public List<FreezeHeightState> getFreezeHeightStates() {
        return freezeHeightStates;
    }

    public void setFreezeHeightStates(List<FreezeHeightState> freezeHeightStates) {
        this.freezeHeightStates = freezeHeightStates;
    }

    public List<FreezeLockTimeState> getFreezeLockTimeStates() {
        return freezeLockTimeStates;
    }

    public void setFreezeLockTimeStates(List<FreezeLockTimeState> freezeLockTimeStates) {
        this.freezeLockTimeStates = freezeLockTimeStates;
    }

    public boolean timeAllow() {
        long now = NulsDateUtils.getCurrentTimeSeconds();
        if ((now - latestUnFreezeTime) > LedgerConstant.TIME_RECALCULATE_FREEZE) {
            return true;
        }
        return false;
    }

    public Map<String, FreezeLockTimeState> getPermanentLockMap() {
        return permanentLockMap;
    }

    public void setPermanentLockMap(Map<String, FreezeLockTimeState> permanentLockMap) {
        this.permanentLockMap = permanentLockMap;
    }


    public boolean isExistPermanentLockMap(String nonce) {
        return permanentLockMap.containsKey(nonce);
    }
}
