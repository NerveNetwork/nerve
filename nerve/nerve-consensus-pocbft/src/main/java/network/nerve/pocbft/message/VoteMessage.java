package network.nerve.pocbft.message;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.vote.VoteData;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.constant.ToolsConstant;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.constant.ConsensusConstant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 投票信息
 * Vote message
 * @author tag
 * 2019/10/28
 */
public class VoteMessage extends BaseBusinessMessage {
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
     * 第几轮投票,一个区块确认可能会尽力多轮投票，如果第一轮所有共识节点未达成一致会进入第二轮投票
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
     * 区块签名
     * */
    private byte[] sign;

    /**
     * 恶意分叉时，传递证据
     */
    private BlockHeader firstHeader;
    private BlockHeader secondHeader;


    /**
     * 非序列化字段
     */
    private String address;

    /**
     * 非序列化字段
     * */
    private NulsHash voteHash;

    /**
     * 非序列化字段
     * */
    private String sendNode;

    /**
     * 是否为自己投的票
     * */
    private boolean local;

    public VoteMessage(){
        this.local = false;
    }

    public VoteMessage(VoteData voteData){
        this(voteData,false);
    }

    public VoteMessage(VoteData voteData, boolean isInit){
        this.height = voteData.getHeight();
        this.roundIndex = voteData.getRoundIndex();
        this.packingIndexOfRound = voteData.getPackingIndexOfRound();
        this.roundStartTime = voteData.getRoundStartTime();
        this.time = voteData.getCurrentRoundData().getTime();
        this.voteRound = voteData.getVoteRound();
        if(!isInit){
            this.blockHash = voteData.getBlockHash();
            if(voteData.getFirstHeader() != null && voteData.getSecondHeader() != null){
                this.firstHeader = voteData.getFirstHeader();
                this.secondHeader = voteData.getSecondHeader();
            }
        }
        this.local = false;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(height);
        stream.writeUint32(time);
        stream.writeUint32(roundIndex);
        stream.writeUint16(packingIndexOfRound);
        stream.write(voteRound);
        stream.writeByte(voteStage);
        stream.write(blockHash.getBytes());
        stream.writeNulsData(firstHeader);
        stream.writeNulsData(secondHeader);
        stream.writeBytesWithLength(sign);
        stream.writeUint32(roundStartTime);
    }

    public byte[] serializeForDigest() throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            int size = size() - SerializeUtils.sizeOfBytes(sign);
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
        this.voteRound = byteBuffer.readByte();
        this.voteStage = byteBuffer.readByte();
        this.blockHash = byteBuffer.readHash();
        this.firstHeader = byteBuffer.readNulsData(new BlockHeader());
        this.secondHeader = byteBuffer.readNulsData(new BlockHeader());
        this.sign = byteBuffer.readByLengthByte();
        this.roundStartTime = byteBuffer.readUint32();

    }

    @Override
    public int size() {
        int size = 20;
        size += 36;
        size += SerializeUtils.sizeOfNulsData(firstHeader);
        size += SerializeUtils.sizeOfNulsData(secondHeader);
        size += SerializeUtils.sizeOfBytes(sign);
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

    public byte[] getSign() {
        return sign;
    }

    public void setSign(byte[] sign) {
        this.sign = sign;
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

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public NulsHash getVoteHash()throws IOException{
        if(voteHash == null){
            voteHash = NulsHash.calcHash(serializeForDigest());
        }
        return voteHash;
    }

    public String getAddress(Chain chain) {
        if (null == address && this.sign != null) {
            BlockSignature bs = new BlockSignature();
            try {
                bs.parse(this.sign, 0);
            } catch (NulsException e) {
                chain.getLogger().error(e);
            }
            this.address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(bs.getPublicKey(), chain.getChainId()));
        }
        return address;
    }

    public String getGeneratedAddress(){
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMessageKey(){
        return ConsensusConstant.SEPARATOR + roundStartTime + ConsensusConstant.SEPARATOR + roundIndex +  ConsensusConstant.SEPARATOR + packingIndexOfRound + ConsensusConstant.SEPARATOR + voteRound + ConsensusConstant.SEPARATOR + voteStage + ConsensusConstant.SEPARATOR + address;
    }

    public String getVoteRoundStageKey(){
        return ConsensusConstant.SEPARATOR + roundIndex + ConsensusConstant.SEPARATOR + packingIndexOfRound + ConsensusConstant.SEPARATOR + voteRound + ConsensusConstant.SEPARATOR + voteStage;
    }

    public String getVoteRoundKey(){
        return  this.roundIndex + ConsensusConstant.SEPARATOR + this.packingIndexOfRound + ConsensusConstant.SEPARATOR + voteRound;
    }

    public String getConsensusKey(){
        return this.roundIndex + ConsensusConstant.SEPARATOR + this.packingIndexOfRound;
    }

    public String getSendNode() {
        return sendNode;
    }

    public void setSendNode(String sendNode) {
        this.sendNode = sendNode;
    }
}
