package io.nuls.consensus.v1.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * Obtain complete voting result information from broadcast nodes
 * Get complete voting result information from broadcast node
 *
 * @author tag
 * 2019/10/28
 */
public class GetVoteResultMessage extends BaseBusinessMessage {
    private NulsHash blockHash;

    public GetVoteResultMessage() {
    }
    public GetVoteResultMessage(NulsHash blockHash) {
        this.blockHash = blockHash;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(this.blockHash.getBytes());
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.blockHash = byteBuffer.readHash();
    }

    @Override
    public int size() {
        return NulsHash.HASH_LENGTH;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    } 
}
