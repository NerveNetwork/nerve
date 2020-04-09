package nerve.network.pocbft.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.vote.VoteResultData;
import nerve.network.pocbft.model.bo.vote.VoteResultItem;

import java.io.IOException;

/**
 * 区块最终确认结果数据消息
 * Block final confirmation result data message
 * @author: Jason
 * 2019/10/29
 */
public class VoteResultMessage extends BaseBusinessMessage {
    private VoteResultData voteResultData;

    /**
     * 发送节点ID，不序列化
     * */
    private String nodeId;

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(voteResultData);
        return size;
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(voteResultData);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.voteResultData = byteBuffer.readNulsData(new VoteResultData());
    }

    public VoteResultData getVoteResultData() {
        return voteResultData;
    }

    public void setVoteResultData(VoteResultData voteResultData) {
        this.voteResultData = voteResultData;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getVoteRoundKey(){
        VoteResultItem voteResultItem = voteResultData.getVoteResultItemList().get(0);
        return  voteResultItem.getRoundIndex() + ConsensusConstant.SEPARATOR + voteResultItem.getPackingIndexOfRound()+ ConsensusConstant.SEPARATOR + voteResultItem.getVoteRound();
    }

}
