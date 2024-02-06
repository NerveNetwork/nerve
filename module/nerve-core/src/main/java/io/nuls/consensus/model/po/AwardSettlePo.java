package io.nuls.consensus.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.CoinTo;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AwardSettlePo extends BaseNulsData {
    /**
     * Starting height
     */
    private long startHeight;
    /**
     * End height
     */
    private long endHeight;
    /**
     * Settlement date
     */
    private String date;
    /**
     * Is it generated for local scheduled tasks? If the data generated for local tasks is verified by settlement blocks, it does not need to be deleted. Otherwise, it needs to be cleared
     */
    private boolean settled;
    /**
     * Settlement details
     */
    private List<CoinTo> settleDetails;

    private long issuedCount;

    public AwardSettlePo() {
    }

    public AwardSettlePo(long startHeight, long endHeight, String date) {
        this.startHeight = startHeight;
        this.endHeight = endHeight;
        this.date = date;
        this.settled = false;
        this.settleDetails = new ArrayList<>();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(startHeight);
        stream.writeInt64(endHeight);
        stream.writeString(date);
        stream.writeBoolean(settled);
        stream.writeUint32(issuedCount);
        int toCount = settleDetails == null ? 0 : settleDetails.size();
        stream.writeVarInt(toCount);
        if (null != settleDetails) {
            for (CoinTo coin : settleDetails) {
                stream.writeNulsData(coin);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        startHeight = byteBuffer.readInt64();
        endHeight = byteBuffer.readInt64();
        date = byteBuffer.readString();
        settled = byteBuffer.readBoolean();
        this.issuedCount = byteBuffer.readUint32();
        int toCount = (int) byteBuffer.readVarInt();
        if (0 < toCount) {
            List<CoinTo> toList = new ArrayList<>();
            for (int i = 0; i < toCount; i++) {
                toList.add(byteBuffer.readNulsData(new CoinTo()));
            }
            this.settleDetails = toList;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(settleDetails == null ? 0 : settleDetails.size());
        if (null != settleDetails) {
            for (CoinTo coin : settleDetails) {
                size += SerializeUtils.sizeOfNulsData(coin);
            }
        }
        size += SerializeUtils.sizeOfString(date);
        size += 21;
        return size;
    }

    public long getStartHeight() {
        return startHeight;
    }

    public long getEndHeight() {
        return endHeight;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public List<CoinTo> getSettleDetails() {
        return settleDetails;
    }

    public void setSettleDetails(List<CoinTo> settleDetails) {
        this.settleDetails = settleDetails;
    }

    public long getIssuedCount() {
        return issuedCount;
    }

    public void setIssuedCount(long issuedCount) {
        this.issuedCount = issuedCount;
    }

    public boolean didPart() {
        return this.getSettleDetails().size() > this.issuedCount && this.issuedCount > 0;
    }

    public void getclearSettleDetails() {
        Log.info("======clear SettleDetails");
        this.getSettleDetails().clear();
    }
}
