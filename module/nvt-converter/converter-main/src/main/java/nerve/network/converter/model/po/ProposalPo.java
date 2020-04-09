package nerve.network.converter.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import nerve.network.converter.constant.ProposalConstant;
import nerve.network.converter.model.bo.Chain;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class ProposalPo extends BaseNulsData {

    private NulsHash hash;

    private long time;

    private byte type;

    private byte[] address;

    private String heterogeneousTxHash;

    //    Number of opponents，1-5的类型时存储该字段
    private int opponentsNumber;
    //    Number of approvers，1-5的类型时存储该字段
    private int approversNumber;
    //    同意者地址列表，1-5的类型时存储该字段
    private List<byte[]> approvers;
    //  反对者地址列表，1-5的类型时存储该字段
    private List<byte[]> opponents;

    //

    /**
     * 当提案状态不是可投票状态，则不允许投票
     */
    private byte status = 0;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeByte(type);
        stream.writeBytesWithLength(address);
        stream.writeString(heterogeneousTxHash);
        stream.write(status);
        stream.writeUint32(time);
        stream.writeUint32(approversNumber);
        for (byte[] address : approvers) {
            stream.write(address);
        }
        stream.writeUint32(opponentsNumber);
        for (byte[] address : opponents) {
            stream.write(address);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.type = byteBuffer.readByte();
        this.address = byteBuffer.readByLengthByte();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.status = byteBuffer.readByte();
        this.time = byteBuffer.readUint32();
        this.approversNumber = (int) byteBuffer.readUint32();
        this.approvers = new ArrayList<>();
        for (int i = 0; i < approversNumber; i++) {
            approvers.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
        }

        this.opponentsNumber = (int) byteBuffer.readUint32();
        this.opponents = new ArrayList<>();
        for (int i = 0; i < this.opponentsNumber; i++) {
            this.opponents.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
        }
    }

    @Override
    public int size() {
        int size = 14;
        size += SerializeUtils.sizeOfBytes(this.address);
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += Address.ADDRESS_LENGTH * this.approversNumber;
        size += Address.ADDRESS_LENGTH * this.opponentsNumber;
        return size;
    }

    public int getOpponentsNumber() {
        return opponentsNumber;
    }

    public void setOpponentsNumber(int opponentsNumber) {
        this.opponentsNumber = opponentsNumber;
    }

    public int getApproversNumber() {
        return approversNumber;
    }

    public void setApproversNumber(int approversNumber) {
        this.approversNumber = approversNumber;
    }

    public List<byte[]> getOpponents() {
        return opponents;
    }

    public void setOpponents(List<byte[]> opponents) {
        this.opponents = opponents;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public List<byte[]> getApprovers() {
        return approvers;
    }

    public void setApprovers(List<byte[]> approvers) {
        this.approvers = approvers;
    }

    /**
     * 处理投票数据
     *
     * @param votePo
     * @return
     */
    public void vote(Chain chain, VoteProposalPo votePo) {
        if (votePo.getChoice() == ProposalConstant.VoteChoice.against) {
            this.opponentsNumber++;
        } else if (votePo.getChoice() == ProposalConstant.VoteChoice.favor) {
            this.approversNumber++;
            if (this.approvers == null) {
                this.approvers = new ArrayList<>();
            }
            this.approvers.add(votePo.getAddress());
        } else {
            return;
        }
        if (this.type == ProposalConstant.TYPE_GENERAL) {
            return;
        }
        //更新状态
        //todo 判断同意者都是银行节点，并且超过66%则通过，
        // 判断反对者都是银行节点，并且超过33%则永不通过

    }
}
