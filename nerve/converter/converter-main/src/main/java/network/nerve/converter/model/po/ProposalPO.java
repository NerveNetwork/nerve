package network.nerve.converter.model.po;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteStatusEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.utils.VirtualBankUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eva
 */
public class ProposalPO extends BaseNulsData {

    private NulsHash hash;

    private byte type;

    private String content;
    /**
     * Heterogeneous chainchainId
     */
    private int heterogeneousChainId;
    /**
     * Heterogeneous chain original transactionhash
     */
    private String heterogeneousTxHash;
    /**
     * Heterogeneous Chain Contract Address Proposal for Change of Contract Use
     */
    private String heterogeneousMultySignAddress;

    private byte[] address;

    private byte[] nerveHash;

    /**
     * Voting scope type
     */
    private byte voteRangeType;

    /**
     * Block height for voting deadline
     */
    private long voteEndHeight;
    /**
     * When the proposal status is not voting, voting is not allowed
     */
    private byte status;

    /**
     * Number of againsts,1-5Store this field when the type of
     */
    private int againstNumber;
    /**
     * Opponent Address List,1-5Store this field when the type of
     */
    private List<String> againstList;
    /**
     * Number of favors,1-5Store this field when the type of
     */
    private int favorNumber;
    /**
     * Agreer Address List,1-5Store this field when the type of
     */
    private List<String> favorList;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(hash.getBytes());
        stream.writeByte(type);
        stream.writeString(content);
        stream.writeUint16(heterogeneousChainId);
        stream.writeString(heterogeneousTxHash);
        stream.writeString(heterogeneousMultySignAddress);
        stream.writeBytesWithLength(address);
        stream.writeBytesWithLength(nerveHash);
        stream.writeByte(voteRangeType);
        stream.writeUint32(voteEndHeight);
        stream.write(status);

        stream.writeUint32(againstNumber);
        int count = againstList == null ? 0 : againstList.size();
        stream.writeUint16(count);
        if (null != againstList) {
            for (String addr : againstList) {
                stream.writeString(addr);
            }
        }

        stream.writeUint32(favorNumber);
        count = favorList == null ? 0 : favorList.size();
        stream.writeUint16(count);
        if (null != favorList) {
            for (String addr : favorList) {
                stream.writeString(addr);
            }
        }

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.hash = byteBuffer.readHash();
        this.type = byteBuffer.readByte();
        this.content = byteBuffer.readString();
        this.heterogeneousChainId = byteBuffer.readUint16();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.heterogeneousMultySignAddress = byteBuffer.readString();
        this.address = byteBuffer.readByLengthByte();
        this.nerveHash = byteBuffer.readByLengthByte();
        this.voteRangeType = byteBuffer.readByte();
        this.voteEndHeight = byteBuffer.readUint32();
        this.status = byteBuffer.readByte();

        this.againstNumber = (int) byteBuffer.readUint32();
        int count = byteBuffer.readUint16();
        if (0 < count) {
            this.againstList = new ArrayList<>();
            for (int i = 0; i < this.againstNumber; i++) {
                this.againstList.add(byteBuffer.readString());
            }
        }

        this.favorNumber = (int) byteBuffer.readUint32();
        count = byteBuffer.readUint16();
        if (0 < count) {
            this.favorList = new ArrayList<>();
            for (int i = 0; i < favorNumber; i++) {
                favorList.add(byteBuffer.readString());
            }
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += 1;
        size += SerializeUtils.sizeOfString(this.content);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += SerializeUtils.sizeOfString(this.heterogeneousMultySignAddress);
        size += SerializeUtils.sizeOfBytes(this.address);
        size += SerializeUtils.sizeOfBytes(this.nerveHash);
        size += 1;
        size += SerializeUtils.sizeOfUint32();
        size += 1;

        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint16();
        if (null != againstList) {
            for (String addr : againstList) {
                size += SerializeUtils.sizeOfString(addr);
            }
        }
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint16();
        if (null != favorList) {
            for (String addr : favorList) {
                size += SerializeUtils.sizeOfString(addr);
            }
        }
        return size;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getNerveHash() {
        return nerveHash;
    }

    public void setNerveHash(byte[] nerveHash) {
        this.nerveHash = nerveHash;
    }

    public byte getVoteRangeType() {
        return voteRangeType;
    }

    public void setVoteRangeType(byte voteRangeType) {
        this.voteRangeType = voteRangeType;
    }

    public long getVoteEndHeight() {
        return voteEndHeight;
    }

    public void setVoteEndHeight(long voteEndHeight) {
        this.voteEndHeight = voteEndHeight;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public int getAgainstNumber() {
        return againstNumber;
    }

    public void setAgainstNumber(int againstNumber) {
        this.againstNumber = againstNumber;
    }

    public int getFavorNumber() {
        return favorNumber;
    }

    public void setFavorNumber(int favorNumber) {
        this.favorNumber = favorNumber;
    }

    public List<String> getAgainstList() {
        return againstList;
    }

    public void setAgainstList(List<String> againstList) {
        this.againstList = againstList;
    }

    public List<String> getFavorList() {
        return favorList;
    }

    public void setFavorList(List<String> favorList) {
        this.favorList = favorList;
    }

    public String getHeterogeneousMultySignAddress() {
        return heterogeneousMultySignAddress;
    }

    public void setHeterogeneousMultySignAddress(String heterogeneousMultySignAddress) {
        this.heterogeneousMultySignAddress = heterogeneousMultySignAddress;
    }

    /**
     * commitWhen processing voting data
     *
     * @param votePO
     * @return
     */
    public void voteCommit(Chain chain, VoteProposalPO votePO) {
        if (votePO.getChoice() == ProposalVoteChoiceEnum.AGAINST.value()) {
            this.againstNumber++;
            if (this.againstList == null) {
                this.againstList = new ArrayList<>();
            }
            this.againstList.add(AddressTool.getStringAddressByBytes(votePO.getAddress()));
        } else if (votePO.getChoice() == ProposalVoteChoiceEnum.FAVOR.value()) {
            this.favorNumber++;
            if (this.favorList == null) {
                this.favorList = new ArrayList<>();
            }
            this.favorList.add(AddressTool.getStringAddressByBytes(votePO.getAddress()));
        } else {
            return;
        }
        if (this.type == ProposalTypeEnum.OTHER.value()) {
            // If it is other types of voting, only the number of votes will be counted
            return;
        }

        //The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (this.favorNumber >= byzantineCount) {
            //adopt Judging that the approvers are all bank nodes and exceed66%Then through
            this.status = ProposalVoteStatusEnum.ADOPTED.value();
            chain.getLogger().info("[voteCommit]Proposal passed by vote. favorNumber:{}, byzantineCount:{}", favorNumber, byzantineCount);

        } else if (this.againstNumber > chain.getMapVirtualBank().size() - byzantineCount) {
            //  Judging that the opponents are all bank nodes and exceed33%Never pass, Set as non voting
            this.status = ProposalVoteStatusEnum.REJECTED.value();
            chain.getLogger().info("[voteCommit]Proposal vote not passed. favorNumber:{}, byzantineCount:{}", favorNumber, byzantineCount);
        }

    }

    /**
     * Processing voting data during rollback
     * @param chain
     * @param choice
     * @param address
     */
    public void voteRollback(Chain chain, byte choice, String address) {
        if (choice == ProposalVoteChoiceEnum.AGAINST.value()) {
            if (this.againstList != null && this.againstList.remove(address)) {
                this.againstNumber--;
            }
        } else if (choice == ProposalVoteChoiceEnum.FAVOR.value()) {
            if (this.favorList != null && this.favorList.remove(address)) {
                this.favorNumber--;
            }
        } else {
            return;
        }
        if (this.type == ProposalTypeEnum.OTHER.value()) {
            // If it is other types of voting, only the number of votes will be counted
            return;
        }
        this.status = ProposalVoteStatusEnum.VOTING.value();
        //The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (this.favorNumber >= byzantineCount) {
            //adopt Judging that the approvers are all bank nodes and exceed66%Then through
            this.status = ProposalVoteStatusEnum.ADOPTED.value();
            chain.getLogger().info("[voteRollback]Proposal passed by vote. favorNumber:{}, byzantineCount:{}", favorNumber, byzantineCount);

        } else if (this.againstNumber > chain.getMapVirtualBank().size() - byzantineCount) {
            //  Judging that the opponents are all bank nodes and exceed33%Never pass, Set as non voting
            this.status = ProposalVoteStatusEnum.REJECTED.value();
            chain.getLogger().info("[voteRollback]Proposal vote not passed. favorNumber:{}, byzantineCount:{}", favorNumber, byzantineCount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProposalPO that = (ProposalPO) o;

        return hash != null ? hash.equals(that.hash) : that.hash == null;
    }

    @Override
    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
