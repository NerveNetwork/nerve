package nerve.network.pocbft.message;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.exception.NulsException;
import nerve.network.pocbft.constant.ConsensusConstant;

import java.io.IOException;

/**
 * 向广播节点获取完整投票结果信息
 * Get complete voting result information from broadcast node
 * @author: Jason
 * 2019/10/28
 */
public class GetVoteResultMessage extends BaseBusinessMessage {
    /**
     * 投票的区块高度
     * */
    private long height;

    /**
     * 区块所在轮次
     * */
    private long roundIndex;

    /**
     * 区块出块下标
     * */
    private int packingIndexOfRound;

    /**
     * 投票轮次
     * */
    private byte voteRound;

    public GetVoteResultMessage(){}

    public GetVoteResultMessage(long height, long roundIndex, int packingIndexOfRound, byte voteRound){
        this.height = height;
        this.roundIndex = roundIndex;
        this.packingIndexOfRound = packingIndexOfRound;
        this.voteRound = voteRound;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(height);
        stream.writeUint32(roundIndex);
        stream.writeUint16(packingIndexOfRound);
        stream.write(voteRound);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.height = byteBuffer.readInt64();
        this.roundIndex = byteBuffer.readUint32();
        this.packingIndexOfRound = byteBuffer.readUint16();
        this.voteRound = byteBuffer.readByte();
    }

    @Override
    public int size() {
        return 15;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
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

    public String getVoteRoundKey(){
        return  roundIndex + ConsensusConstant.SEPARATOR + packingIndexOfRound + ConsensusConstant.SEPARATOR + voteRound;
    }

    public String getConsensusKey(){
        return  roundIndex + ConsensusConstant.SEPARATOR + packingIndexOfRound;
    }

    public byte getVoteRound() {
        return voteRound;
    }

    public void setVoteRound(byte voteRound) {
        this.voteRound = voteRound;
    }
}
