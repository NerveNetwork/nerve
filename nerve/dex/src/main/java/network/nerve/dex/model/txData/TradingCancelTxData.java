package network.nerve.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TradingCancelTxData extends BaseNulsData {

    private List<CancelDeal> cancelDealList;

    @Override
    public int size() {

        int size = SerializeUtils.sizeOfVarInt(cancelDealList.size());
        for (CancelDeal cancelDeal : cancelDealList) {
            size += SerializeUtils.sizeOfNulsData(cancelDeal);
        }
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeVarInt(cancelDealList.size());
        for (int i = 0; i < cancelDealList.size(); i++) {
            stream.writeNulsData(cancelDealList.get(i));
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {

        int size = (int) byteBuffer.readVarInt();
        List<CancelDeal> cancelDealList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cancelDealList.add(byteBuffer.readNulsData(new CancelDeal()));
        }
        this.cancelDealList = cancelDealList;
    }

    public List<CancelDeal> getCancelDealList() {
        return cancelDealList;
    }

    public void setCancelDealList(List<CancelDeal> cancelDealList) {
        this.cancelDealList = cancelDealList;
    }
}
