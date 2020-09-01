package network.nerve.pocbft.v1.entity;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.core.model.DoubleUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.v1.message.GetVoteResultMessage;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class VoteSummaryData extends BasicObject {

    private long height;
    private long roundIndex;
    private long roundStartTime;
    private int packingIndexOfRound;
    private long voteRoundIndex;
    private NulsHash blockHash;


    private Map<String, VoteMessage> stageOneMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_32);
    private Map<String, VoteMessage> stageTwoMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_32);
    private boolean stage1First = true;
    private boolean stage2First = true;

    public VoteSummaryData(Chain chain) {
        super(chain);
    }

    public void addVote(VoteMessage vote, MeetingRound round) {
        if (height == 0) {
            this.height = vote.getHeight();
            this.roundIndex = vote.getRoundIndex();
            this.roundStartTime = vote.getRoundStartTime();
            this.packingIndexOfRound = vote.getPackingIndexOfRound();
            this.voteRoundIndex = vote.getVoteRoundIndex();
            this.blockHash = vote.getBlockHash();
        }
        int count = 0;
        if (vote.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE) {
            stageOneMap.put(vote.getAddress(chain), vote);
            count = stageOneMap.size();
        } else if (vote.getVoteStage() == ConsensusConstant.VOTE_STAGE_TWO) {
            stageTwoMap.put(vote.getAddress(chain), vote);
            count = stageTwoMap.size();
        }
        double result = DoubleUtils.div(count, round.getMemberCount());
//        if (vote.getVoteStage() == 2) {
            log.debug("收到投票({})：{}-{}-{}-{}-{}={}=========={}%", vote.getVoteStage(), this.height, this.roundIndex, this.packingIndexOfRound, this.voteRoundIndex,
                    vote.getBlockHash().toHex(), vote.getAddress(chain), result * 100);
//        }

        if (result * 100 > chain.getConfig().getByzantineRate()) {


            VoteStageResult data = new VoteStageResult();
            data.setBlockHash(vote.getBlockHash());
            data.setHeight(vote.getHeight());
            data.setRoundIndex(vote.getRoundIndex());
            data.setRoundStartTime(vote.getRoundStartTime());
            data.setPackingIndexOfRound(vote.getPackingIndexOfRound());
            data.setStage(vote.getVoteStage());
            data.setVoteRoundIndex(vote.getVoteRoundIndex());
            if (stage1First && vote.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE) {
                stage1First = false;
//                log.info("提交第一阶段投票结果：{}-{}-{}-{}-{}-{}={}=========={}%", this.height, this.roundIndex, this.packingIndexOfRound, this.voteRoundIndex,
//                        vote.getVoteStage(), vote.getBlockHash().toHex(), vote.getAddress(chain), result * 100);

                chain.getConsensusCache().getStageOneQueue().offer(data);

            } else if (stage2First && vote.getVoteStage() == ConsensusConstant.VOTE_STAGE_TWO) {
                stage2First = false;
                data.setResultMessage(new VoteResultMessage(chain, new ArrayList<>(stageTwoMap.values())));
                log.debug("提交2阶段结果：{}-{}-{}-{}-{}-{}={}=========={}%", this.height, this.roundIndex, this.packingIndexOfRound, this.voteRoundIndex,
                        vote.getVoteStage(), vote.getBlockHash().toHex(), vote.getAddress(chain), result * 100);
                chain.getConsensusCache().getStageTwoQueue().offer(data);
            }
        }
    }

    public long getHeight() {
        return height;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public long getVoteRoundIndex() {
        return voteRoundIndex;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public int getStageOneCount() {
        return this.stageOneMap.size();
    }

    public int getStageTwoCount() {
        return this.stageTwoMap.size();
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }


    public String getTargetKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(height);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(roundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(packingIndexOfRound);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(voteRoundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(blockHash.toHex());
        return sb.toString();
    }
}
