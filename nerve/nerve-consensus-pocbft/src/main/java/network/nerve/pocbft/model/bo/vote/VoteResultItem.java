package network.nerve.pocbft.model.bo.vote;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.*;
import io.nuls.core.constant.ToolsConstant;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.message.VoteMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 投票最终结果缓存
 * Voting final result cache
 *
 * @author tag
 * 2019/10/28
 */
public class VoteResultItem extends BaseNulsData {
    /**
     * 投票的区块高度
     * */
    private long height;

    /**
     * 本阶段投票开始时间
     * */
    private long time;

    /**
     * 区块所在轮次
     * */
    private long roundIndex;

    /**
     * 区块出块下标
     * */
    private int packingIndexOfRound;

    /**
     * 共识轮次开始时间
     * */
    private long roundStartTime;

    /**
     * 第几轮投票,一个区块确认可能会进入多轮投票，如果第一轮所有共识节点未达成一致会进入第二轮投票
     * */
    private byte voteRound = 1;

    /**
     * 第几阶段投票，每轮投票分两个阶段，第一阶段是对区块签名投票，第二阶段是对第一阶段结果投票
     * */
    private byte voteStage = 1;

    /**
     * 区块hash
     * */
    private NulsHash blockHash;

    /**
     * 恶意分叉时，传递证据
     */
    private BlockHeader firstHeader;
    private BlockHeader secondHeader;

    /**
     * 投票结果签名列表
     * */
    private List<byte[]> signatureList;


    public VoteResultItem(){}


    public VoteResultItem(VoteMessage voteMessage){
        this.height = voteMessage.getHeight();
        this.roundIndex = voteMessage.getRoundIndex();
        this.packingIndexOfRound = voteMessage.getPackingIndexOfRound();
        this.time = voteMessage.getTime();
        this.voteRound = voteMessage.getVoteRound();
        this.voteStage = voteMessage.getVoteStage();
        this.blockHash = voteMessage.getBlockHash();
        this.firstHeader = voteMessage.getFirstHeader();
        this.secondHeader = voteMessage.getSecondHeader();
        this.roundStartTime = voteMessage.getRoundStartTime();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(height);
        stream.writeUint32(time);
        stream.writeUint32(roundIndex);
        stream.writeUint16(packingIndexOfRound);
        stream.writeUint32(roundStartTime);
        stream.write(voteRound);
        stream.writeByte(voteStage);
        stream.write(blockHash.getBytes());
        stream.writeNulsData(firstHeader);
        stream.writeNulsData(secondHeader);
        int signCount = signatureList == null ? 0 : signatureList.size();
        stream.writeVarInt(signCount);
        if (null != signatureList) {
            for (byte[] signature : signatureList) {
                stream.writeBytesWithLength(signature);
            }
        }
    }

    public byte[] serializeForDigest() throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            int size = size();
            bos = new UnsafeByteArrayOutputStream(size);
            NulsOutputStreamBuffer buffer = new NulsOutputStreamBuffer(bos);
            if (size == 0) {
                bos.write(ToolsConstant.PLACE_HOLDER);
            } else {
                buffer.writeInt64(height);
                buffer.writeUint32(time);
                buffer.writeUint32(roundIndex);
                buffer.writeUint16(packingIndexOfRound);
                buffer.write(voteRound);
                buffer.writeByte(voteStage);
                buffer.write(blockHash.getBytes());
                buffer.writeNulsData(firstHeader);
                buffer.writeNulsData(secondHeader);
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
        this.time = byteBuffer.readUint32();
        this.roundIndex = byteBuffer.readUint32();
        this.packingIndexOfRound = byteBuffer.readUint16();
        this.roundStartTime = byteBuffer.readUint32();
        this.voteRound = byteBuffer.readByte();
        this.voteStage = byteBuffer.readByte();
        this.blockHash = byteBuffer.readHash();
        this.firstHeader = byteBuffer.readNulsData(new BlockHeader());
        this.secondHeader = byteBuffer.readNulsData(new BlockHeader());
        int signCount = (int) byteBuffer.readVarInt();
        if(signCount > 0){
            List<byte[]> signatureList = new ArrayList<>();
            for (int i = 0; i < signCount; i++) {
                signatureList.add(byteBuffer.readByLengthByte());
            }
            this.signatureList = signatureList;
        }
    }

    @Override
    public int size() {
        int size = 20;
        size += 36;
        size += SerializeUtils.sizeOfNulsData(firstHeader);
        size += SerializeUtils.sizeOfNulsData(secondHeader);
        size += SerializeUtils.sizeOfVarInt(signatureList == null ? 0 : signatureList.size());
        if (null != signatureList) {
            for (byte[] signature : signatureList) {
                size += SerializeUtils.sizeOfBytes(signature);
            }
        }
        return size;
    }

    public byte getVoteRound() {
        return voteRound;
    }

    public void setVoteRound(byte voteRound) {
        this.voteRound = voteRound;
    }

    public byte getVoteStage() {
        return voteStage;
    }

    public void setVoteStage(byte voteStage) {
        this.voteStage = voteStage;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(NulsHash blockHash) {
        this.blockHash = blockHash;
    }


    public BlockHeader getFirstHeader() {
        return firstHeader;
    }

    public void setFirstHeader(BlockHeader firstHeader) {
        this.firstHeader = firstHeader;
    }

    public BlockHeader getSecondHeader() {
        return secondHeader;
    }

    public void setSecondHeader(BlockHeader secondHeader) {
        this.secondHeader = secondHeader;
    }

    public List<byte[]> getSignatureList() {
        return signatureList;
    }

    public void setSignatureList(List<byte[]> signatureList) {
        this.signatureList = signatureList;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    public boolean isConfirmedEmpty(){
        return NulsHash.EMPTY_NULS_HASH.equals(blockHash);
    }

    public String getConsensusKey(){
        return roundIndex + ConsensusConstant.SEPARATOR + packingIndexOfRound;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    @Override
    public String toString(){
        byte result = 0;
        if(firstHeader != null){
            result = 1;
        }else if(blockHash != null){
            if(blockHash.equals(NulsHash.EMPTY_NULS_HASH)){
                result = 2;
            }
        }
        return "roundIndex:"+roundIndex+",packIndexOfRound:"+packingIndexOfRound+",voteRound:" + voteRound + ",voteResult:" + result +"(0：确认正常区块，1分叉，2：空块)";
    }
}
