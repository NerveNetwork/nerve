package network.nerve.swap.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.model.NerveToken;

import java.io.IOException;

/**
 * @author Niels
 */
public class SwapPairPO extends BaseNulsData {
    private byte[] address;
    private int lpAssetId;
    private NerveToken token0;
    private NerveToken token1;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {

    }

    @Override
    public int size() {
        return 0;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public int getLpAssetId() {
        return lpAssetId;
    }

    public void setLpAssetId(int lpAssetId) {
        this.lpAssetId = lpAssetId;
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
