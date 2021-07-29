package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.model.NerveToken;

import java.io.IOException;

/**
 * @author Niels
 */
public class CreatePairData extends BaseNulsData {

    private NerveToken token0;
    private NerveToken token1;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(token0.getChainId());
        stream.writeUint16(token0.getAssetId());
        stream.writeUint16(token1.getChainId());
        stream.writeUint16(token1.getAssetId());
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.token0 = new NerveToken(byteBuffer.readUint16(), byteBuffer.readUint16());
        this.token1 = new NerveToken(byteBuffer.readUint16(), byteBuffer.readUint16());
    }

    @Override
    public int size() {
        return 8;
    }

    public NerveToken getToken0() {
        return token0;
    }

    public void setToken0(NerveToken token0) {
        this.token0 = token0;
    }

    public NerveToken getToken1() {
        return token1;
    }

    public void setToken1(NerveToken token1) {
        this.token1 = token1;
    }
}
