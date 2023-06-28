package network.nerve.pocbft.network.model;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

public class ShareMsgSignResult extends BaseNulsData {

    private byte[] identityList;

    private byte[] signatrue;

    public ShareMsgSignResult(){}
    public ShareMsgSignResult(byte[] identityList,byte[] signature){
        this.identityList = identityList;
        this.signatrue = signature;
    }

    public byte[] getIdentityList() {
        return identityList;
    }

    public void setIdentityList(byte[] identityList) {
        this.identityList = identityList;
    }

    public byte[] getSignatrue() {
        return signatrue;
    }

    public void setSignatrue(byte[] signatrue) {
        this.signatrue = signatrue;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(identityList);
        stream.writeBytesWithLength(signatrue);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.identityList = byteBuffer.readByLengthByte();
        this.signatrue = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {

        int size = 0;
        size += SerializeUtils.sizeOfBytes(identityList);
        size += SerializeUtils.sizeOfBytes(signatrue);
        return size;
    }
}
