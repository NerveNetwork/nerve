package io.nuls.consensus.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.VoteController;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.consensus.v1.entity.VoteResultStageOneQueue;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.consensus.v1.message.VoteMessage;
import io.nuls.consensus.v1.utils.CsUtils;

/**
 * @author Eva
 */
public class StageOneProcessor extends BasicRunnable {

    private final RoundController roundController;

    private final VoteController voteController;


    public StageOneProcessor(Chain chain, RoundController roundController, VoteController voteController) {
        super(chain);
        this.roundController = roundController;
        this.voteController = voteController;
    }


    @Override
    public void run() {
        while (this.running) {
            try {
                doit();
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    private void doit() throws InterruptedException {
        VoteResultStageOneQueue queue = chain.getConsensusCache().getStageOneQueue();
        VoteStageResult result = queue.take();
//            log.info("Received the results of the first stage：{}-{}-{}-{}: {}",result.getHeight(),result.getRoundIndex(),result.getPackingIndexOfRound(),
//                    result.getVoteRoundIndex(),result.getBlockHash().toHex());
        if (result.getRoundIndex() < chain.getConsensusCache().getLastConfirmedRoundIndex() ||
                (result.getRoundIndex() == chain.getConsensusCache().getLastConfirmedRoundIndex() && result.getPackingIndexOfRound() <= chain.getConsensusCache().getLastConfirmedRoundPackingIndex())) {
            return;
        }
        //Modify the result identifier to determine if it has timed out
        chain.getConsensusCache().getBestBlocksVotingContainer().getStage1ResultRecorder()
                .insertAndCheck(result.getHeight() + ConsensusConstant.SEPARATOR + result.getVoteRoundIndex());
        //The most important thing：Hurry up and vote for the second round
        MeetingRound round = roundController.getRound(result.getRoundIndex(), result.getRoundStartTime());
        MeetingMember meetingMember = round.getLocalMember();
        if (null == meetingMember) {
            log.info("Local is not a consensus node....");
            return;
        }
        String address = AddressTool.getStringAddressByBytes(meetingMember.getAgent().getPackingAddress());

        VoteMessage message = CsUtils.createStageTwoVoteMessage(chain, address, result);
        if (null == message) {
            log.warn("Generation of second stage voting failed！！！");
            return;
        }
        this.voteController.voteStageTwo(message);


    }

}
