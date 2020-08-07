package network.nerve.pocbft.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.log.Log;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.v1.RoundController;
import network.nerve.pocbft.v1.VoteController;
import network.nerve.pocbft.v1.entity.BasicRunnable;
import network.nerve.pocbft.v1.entity.VoteResultStageOneQueue;
import network.nerve.pocbft.v1.entity.VoteStageResult;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.utils.CsUtils;

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
//            log.info("收到第一阶段结果：{}-{}-{}-{}: {}",result.getHeight(),result.getRoundIndex(),result.getPackingIndexOfRound(),
//                    result.getVoteRoundIndex(),result.getBlockHash().toHex());
        if (result.getRoundIndex() < chain.getConsensusCache().getLastConfirmedRoundIndex() ||
                (result.getRoundIndex() == chain.getConsensusCache().getLastConfirmedRoundIndex() && result.getPackingIndexOfRound() <= chain.getConsensusCache().getLastConfirmedRoundPackingIndex())) {
            return;
        }
        //修改得到结果标识，判断是否已超时
        chain.getConsensusCache().getBestBlocksVotingContainer().getStage1ResultRecorder()
                .insertAndCheck(result.getHeight() + ConsensusConstant.SEPARATOR + result.getVoteRoundIndex());
        //最要紧的事：赶紧投票第二轮
        MeetingRound round = roundController.getRound(result.getRoundIndex(), result.getRoundStartTime());
        MeetingMember meetingMember = round.getLocalMember();
        if (null == meetingMember) {
            log.info("本地不是共识节点。。。。");
            return;
        }
        String address = AddressTool.getStringAddressByBytes(meetingMember.getAgent().getPackingAddress());

        VoteMessage message = CsUtils.createStageTwoVoteMessage(chain, address, result);
        if (null == message) {
            log.warn("生成第二阶段投票失败！！！");
            return;
        }
        this.voteController.voteStageTwo(message);


    }

}
