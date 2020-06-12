package network.nerve.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

public class TradingOrderCancel extends BaseNulsData {

    //订单交易hash
    private byte[] orderHash;

    @Override
    public int size() {
        return NulsHash.HASH_LENGTH;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(orderHash);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        orderHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
    }


    public byte[] getOrderHash() {
        return orderHash;
    }

    public void setOrderHash(byte[] orderHash) {
        this.orderHash = orderHash;
    }
}
