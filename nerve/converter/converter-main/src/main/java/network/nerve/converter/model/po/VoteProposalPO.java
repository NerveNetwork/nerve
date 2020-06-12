package network.nerve.converter.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * @author Niels
 */
public class VoteProposalPO extends BaseNulsData {

    private NulsHash hash;

    private NulsHash proposalTxHash;

    private byte choice;

    private byte[] address;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(hash.getBytes());
        stream.write(choice);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.hash = byteBuffer.readHash();
        this.choice = byteBuffer.readByte();
    }

    @Override
    public int size() {
        int size = 1;
        size += NulsHash.HASH_LENGTH;
        return size;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public NulsHash getProposalTxHash() {
        return proposalTxHash;
    }

    public void setProposalTxHash(NulsHash proposalTxHash) {
        this.proposalTxHash = proposalTxHash;
    }

    public byte getChoice() {
        return choice;
    }

    public void setChoice(byte choice) {
        this.choice = choice;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }
}
