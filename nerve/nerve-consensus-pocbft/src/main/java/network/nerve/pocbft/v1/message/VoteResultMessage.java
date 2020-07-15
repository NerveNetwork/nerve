package network.nerve.pocbft.v1.message;

import io.nuls.base.data.NulsHash;
import io.nuls.core.basic.VarInt;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.constant.ToolsConstant;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.model.bo.Chain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 区块最终确认结果数据消息
 * Block final confirmation result data message
 *
 * @author tag
 * 2019/10/29
 */
public class VoteResultMessage extends BaseBusinessMessage {

    /**
     * 投票的区块高度
     */
    private long height;

    /**
     * 区块所在轮次
     */
    private long roundIndex;

    /**
     * 区块出块下标
     */
    private int packingIndexOfRound;

    /**
     * 共识轮次开始时间
     */
    private long roundStartTime;

    /**
     * 第几轮投票,一个区块确认可能会尽力多轮投票，如果第一轮所有共识节点未达成一致会进入第二轮投票
     */
    private long voteRoundIndex = 1;

    /**
     * 第几阶段投票，每轮投票分两个阶段，第一阶段是对区块签名投票，第二阶段是对第一阶段结果投票
     */
    private byte voteStage = 1;

    /**
     * 区块hash
     */
    private NulsHash blockHash;

    /**
     * 区块签名
     */
    private List<byte[]> signList = new ArrayList<>();

    /**
     * 发送节点ID，不序列化
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
//        chain.getLogger().info("汇总投票结果：");
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
