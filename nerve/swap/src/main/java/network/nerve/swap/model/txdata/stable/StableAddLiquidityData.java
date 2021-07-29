package network.nerve.swap.model.txdata.stable;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author Niels
 */
public class StableAddLiquidityData extends BaseNulsData {

    private byte[] to;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(to);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        return size;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

}
