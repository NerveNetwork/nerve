package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.LinkedList;

/**
 * 存放交易对的修改记录
 */
public class CoinTradingEditInfoPo extends BaseNulsData {

    private LinkedList<EditCoinTradingPo> editCoinTradingList;

    public CoinTradingEditInfoPo() {
    }

    public CoinTradingEditInfoPo(EditCoinTradingPo editCoinTrading) {
        LinkedList<EditCoinTradingPo> editCoinTradingList = new LinkedList<>();
        editCoinTradingList.add(editCoinTrading);
        this.editCoinTradingList = editCoinTradingList;
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint32();
        for (int i = 0; i < editCoinTradingList.size(); i++) {
            size += editCoinTradingList.get(i).size();
        }
        return size;
    }


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(editCoinTradingList.size());
        for (int i = 0; i < editCoinTradingList.size(); i++) {
            stream.writeNulsData(editCoinTradingList.get(i));
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int size = (int) byteBuffer.readUint32();
        LinkedList<EditCoinTradingPo> editCoinTradingList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            EditCoinTradingPo editCoinTrading = new EditCoinTradingPo();
            byteBuffer.readNulsData(editCoinTrading);
            editCoinTradingList.add(editCoinTrading);
        }
        this.editCoinTradingList = editCoinTradingList;
    }


    public LinkedList<EditCoinTradingPo> getEditCoinTradingList() {
        return editCoinTradingList;
    }

    public void setEditCoinTradingList(LinkedList<EditCoinTradingPo> editCoinTradingList) {
        this.editCoinTradingList = editCoinTradingList;
    }
}
