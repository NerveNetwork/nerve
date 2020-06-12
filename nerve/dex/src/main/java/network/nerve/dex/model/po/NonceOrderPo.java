package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NonceOrderPo extends BaseNulsData {

    private List<NulsHash> orderHashList;

    public NonceOrderPo() {

    }

    public NonceOrderPo(NulsHash nulsHash) {
        orderHashList = new ArrayList<>();
        orderHashList.add(nulsHash);
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfVarInt(orderHashList.size());
        for (int i = 0; i < orderHashList.size(); i++) {
            size += NulsHash.HASH_LENGTH;
        }
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeVarInt(orderHashList.size());
        for (int i = 0; i < orderHashList.size(); i++) {
            stream.write(orderHashList.get(i).getBytes());
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int count = (int) byteBuffer.readVarInt();
        List<NulsHash> orderHashList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orderHashList.add(new NulsHash(byteBuffer.readBytes(NulsHash.HASH_LENGTH)));
        }
        this.orderHashList = orderHashList;
    }

    public List<NulsHash> getOrderHashList() {
        return orderHashList;
    }

    public void setOrderHashList(List<NulsHash> orderHashList) {
        this.orderHashList = orderHashList;
    }
}
