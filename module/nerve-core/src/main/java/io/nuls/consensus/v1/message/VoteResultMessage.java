package io.nuls.consensus.v1.message;

import io.nuls.base.data.NulsHash;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.core.basic.VarInt;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.constant.ToolsConstant;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Block final confirmation result data message
 * Block final confirmation result data message
 *
 * @author tag
 * 2019/10/29
 */
public class VoteResultMessage extends BaseBusinessMessage {

    /**
     * Block height for voting
     */
    private long height;

    /**
     * The round in which the block is located
     */
    private long roundIndex;

    /**
     * Block output index
     */
    private int packingIndexOfRound;

    /**
     * Consensus round start time
     */
    private long roundStartTime;

    /**
     * Which round of voting,A block confirmation may attempt multiple rounds of voting, and if all consensus nodes do not reach a consensus in the first round, it will enter the second round of voting
     */
    private long voteRoundIndex = 1;

    /**
     * What stage of voting is there? Each round of voting is divided into two stages. The first stage is voting on block signatures, and the second stage is voting on the results of the first stage
     */
    private byte voteStage = 1;

    /**
     * blockhash
     */
    private NulsHash blockHash;

    /**
     * Block signature
     */
    private List<byte[]> signList = new ArrayList<>();

    /**
     * Sending nodeID, do not serialize
     */
    private String nodeId;
    private NulsHash voteHash;

    public VoteResultMessage() {
    }

    public VoteResultMessage(Chain chain, List<VoteMessage> list) {
        super();
        if (null == list || list.isEmpty()) {
            return;
        }
        boolean first = true;
//        chain.getLogger().info("Summarize voting results：");
        for (VoteMessage msg : list) {
//            try {
//                chain.getLogger().info("      {}-{}-{}-{}-{}-{}-{}-{}", msg.getHeight(), msg.getRoundIndex(), msg.getPackingIndexOfRound(), msg.getRoundStartTime(),
//                        msg.getVoteRoundIndex(), msg.getVoteStage(), msg.getHash().toHex(), msg.getBlockHash().toHex());
//            } catch (IOException e) {
//                chain.getLogger().error(e);
//            }
            if (first) {
                this.height = msg.getHeight();
                this.blockHash = msg.getBlockHash();
                this.packingIndexOfRound = msg.getPackingIndexOfRound();
                this.roundIndex = msg.getRoundIndex();
                this.voteRoundIndex = msg.getVoteRoundIndex();
                this.roundStartTime = msg.getRoundStartTime();
                this.voteStage = msg.getVoteStage();
                first = false;
            }
            this.signList.add(msg.getSign());
            msg.unlock();
        }

    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(height);
        stream.writeUint32(roundIndex);
        stream.writeUint16(packingIndexOfRound);
        stream.writeUint32(voteRoundIndex);
        stream.writeByte(voteStage);
        stream.write(blockHash.getBytes());
        stream.writeUint32(roundStartTime);
        stream.writeVarInt(signList.size());
        for (byte[] sign : signList) {
            stream.writeBytesWithLength(sign);
        }
    }

    public byte[] serializeForDigest() throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            int size = getNormalFieldsSize();
            bos = new UnsafeByteArrayOutputStream(size);
            NulsOutputStreamBuffer buffer = new NulsOutputStreamBuffer(bos);
            if (size == 0) {
                bos.write(ToolsConstant.PLACE_HOLDER);
            } else {
                buffer.writeInt64(height);
                buffer.writeUint32(roundIndex);
                buffer.writeUint16(packingIndexOfRound);
                buffer.writeUint32(voteRoundIndex);
                buffer.writeByte(voteStage);
                buffer.write(blockHash.getBytes());
                buffer.writeUint32(roundStartTime);
            }
            return bos.toByteArray();
        } finally {
            if (bos != null) {
                bos.close();
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.height = byteBuffer.readInt64();
        this.roundIndex = byteBuffer.readUint32();
        this.packingIndexOfRound = byteBuffer.readUint16();
        this.voteRoundIndex = byteBuffer.readUint32();
        this.voteStage = byteBuffer.readByte();
        this.blockHash = byteBuffer.readHash();
        this.roundStartTime = byteBuffer.readUint32();
        int count = (int) byteBuffer.readVarInt();
        for (int i = 0; i < count; i++) {
            this.signList.add(byteBuffer.readByLengthByte());
        }
    }

    @Override
    public int size() {
        int size = getNormalFieldsSize();
        size += VarInt.sizeOf(signList.size());
        for (byte[] sign : signList) {
            size += SerializeUtils.sizeOfBytes(sign);
        }
        return size;
    }

    private int getNormalFieldsSize() {
        int size = 16;
        size += 36 + 3;
        return size;
    }

    public NulsHash getHash() throws IOException {
        if (voteHash == null) {
            voteHash = NulsHash.calcHash(serializeForDigest());
        }
        return voteHash;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getHeight() {
        return height;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public long getVoteRoundIndex() {
        return voteRoundIndex;
    }

    public byte getVoteStage() {
        return voteStage;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public List<byte[]> getSignList() {
        return signList;
    }

    public String getNodeId() {
        return nodeId;
    }
}
