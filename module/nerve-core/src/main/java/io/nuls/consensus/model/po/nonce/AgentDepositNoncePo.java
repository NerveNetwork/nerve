package io.nuls.consensus.model.po.nonce;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AgentDepositNoncePo extends BaseNulsData {
    /**
     * Effective margin trading
     * */
    private List<NonceDataPo> validNonceList;

    /**
     * Invalid margin trading
     * */
    private List<NonceDataPo> invalidNonceList;

    public AgentDepositNoncePo(){}

    public AgentDepositNoncePo(NonceDataPo initPo){
        validNonceList = new ArrayList<>();
        validNonceList.add(initPo);
    }

    public AgentDepositNoncePo(List<NonceDataPo> validNonceList, List<NonceDataPo> invalidNonceList){
        this.validNonceList = validNonceList;
        this.invalidNonceList = invalidNonceList;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int validCount = validNonceList == null ? 0 : validNonceList.size();
        stream.writeVarInt(validCount);
        if (null != validNonceList) {
            for (NonceDataPo po : validNonceList) {
                stream.writeNulsData(po);
            }
        }
        int invalidCount = invalidNonceList == null ? 0 : invalidNonceList.size();
        stream.writeVarInt(invalidCount);
        if (null != invalidNonceList) {
            for (NonceDataPo po : invalidNonceList) {
                stream.writeNulsData(po);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int validCount = (int) byteBuffer.readVarInt();
        if (0 < validCount) {
            List<NonceDataPo> validList = new ArrayList<>();
            for (int i = 0; i < validCount; i++) {
                validList.add(byteBuffer.readNulsData(new NonceDataPo()));
            }
            this.validNonceList = validList;
        }

        int invalidCount = (int) byteBuffer.readVarInt();
        if (0 < invalidCount) {
            List<NonceDataPo> invalidList = new ArrayList<>();
            for (int i = 0; i < invalidCount; i++) {
                invalidList.add(byteBuffer.readNulsData(new NonceDataPo()));
            }
            this.invalidNonceList = invalidList;
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfVarInt(validNonceList == null ? 0 : validNonceList.size());
        if (null != validNonceList) {
            for (NonceDataPo po : validNonceList) {
                size += SerializeUtils.sizeOfNulsData(po);
            }
        }
        size += SerializeUtils.sizeOfVarInt(invalidNonceList == null ? 0 : invalidNonceList.size());
        if (null != invalidNonceList) {
            for (NonceDataPo po : invalidNonceList) {
                size += SerializeUtils.sizeOfNulsData(po);
            }
        }
        return size;
    }

    public List<NonceDataPo> getValidNonceList() {
        return validNonceList;
    }

    public void setValidNonceList(List<NonceDataPo> validNonceList) {
        this.validNonceList = validNonceList;
    }

    public List<NonceDataPo> getInvalidNonceList() {
        return invalidNonceList;
    }

    public void setInvalidNonceList(List<NonceDataPo> invalidNonceList) {
        this.invalidNonceList = invalidNonceList;
    }
}
