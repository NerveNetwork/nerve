package network.nerve.pocbft.v1.entity;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import network.nerve.pocbft.v1.message.VoteResultMessage;

/**
 * @author Eva
 */
public class VoteStageResult {

    private long height;
    private long roundIndex;
    private long roundStartTime;
    private int packingIndexOfRound;
    private long voteRoundIndex;
    private byte stage;
    private NulsHash blockHash;


    private VoteResultMessage resultMessage;

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    public VoteResultMessage getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(VoteResultMessage resultMessage) {
        this.resultMessage = resultMessage;
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

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public long getVoteRoundIndex() {
        return voteRoundIndex;
    }

    public void setVoteRoundIndex(long voteRoundIndex) {
        this.voteRoundIndex = voteRoundIndex;
    }

    public byte getStage() {
        return stage;
    }

    public void setStage(byte stage) {
        this.stage = stage;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(NulsHash blockHash) {
        this.blockHash = blockHash;
    }

}
