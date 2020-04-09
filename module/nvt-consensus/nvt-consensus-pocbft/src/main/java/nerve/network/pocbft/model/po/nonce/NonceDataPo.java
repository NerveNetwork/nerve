package nerve.network.pocbft.model.po.nonce;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class NonceDataPo extends BaseNulsData {
    /**
     * 该笔追加保证金金额
     * */
    private BigInteger deposit;
    /**
     * 追加保证金交易交易hash后八位
     * */
    private byte[] nonce;

    public NonceDataPo(){}

    public NonceDataPo(BigInteger deposit, byte[] nonce){
        this.deposit = deposit;
        this.nonce = nonce;
    }



    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(deposit);
        stream.writeBytesWithLength(nonce);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.deposit = byteBuffer.readBigInteger();
        this.nonce = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBytes(nonce);
        return size;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }

        if(obj == null){
            return false;
        }

        if(obj instanceof NonceDataPo){
            return Arrays.equals(this.nonce, ((NonceDataPo) obj).nonce);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        String nonceStr = nonce == null ? null : RPCUtil.encode(nonce);
        result = 31 * result + (nonceStr == null ? 0 : nonceStr.hashCode());
        return result;
    }
}
