package io.nuls.transaction.rpc.cmd.pausetemp;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Niels
 */
public class NerveToken extends BaseNulsData {

    private int chainId;
    private int assetId;

    public NerveToken() {
    }

    public NerveToken(int chainId, int assetId) {
        this.chainId = chainId;
        this.assetId = assetId;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(chainId);
        stream.writeUint16(assetId);

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();

    }

    @Override
    public int size() {
        return 4;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) return false;
        NerveToken that = (NerveToken) o;
        return chainId == that.chainId &&
                assetId == that.assetId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chainId, assetId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"chainId\":")
                .append(chainId);
        sb.append(",\"assetId\":")
                .append(assetId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public NerveToken clone() {
        return new NerveToken(chainId, assetId);
    }

    public String str() {
        return chainId + "-" + assetId;
    }
}