package io.nuls.consensus.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.PackingData;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.rpc.call.CallMethodUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Eva
 */
public class CsController extends BasicObject {
    /**
     * Voting timeout,Default three seconds
     */
    private long VOTE_TIME_OUT = 3000L;

    private long comfirmedHeight = 0;


    private RoundController roundController;
    private VoteController voteController;

    public CsController(Chain chain, RoundController roundController, VoteController voteController) {
        super(chain);
        this.roundController = roundController;
        this.voteController = voteController;
    }

    //There are several situations here：1Start network from0Start,2Restart the network from a certain height,3Start this node/
    //The goal is to reach consensus as soon as possible in any situation（Consistent rounds）
    public void consensus() {
        //Waiting for the previous confirmed block to be saved
        int index = 0;
        while (chain.getBestHeader().getHeight() < comfirmedHeight) {
            try {
                Thread.sleep(100L);
                index++;
                if (index > chain.getConfig().getPackingIntervalMills() / 100L) {
                    index = 0;
                    Log.warn("It's been too long..");
                    return;
                }
            } catch (InterruptedException e) {
                log.error(e);
            }
        }

        log.info("here1111");
        MeetingRound round = roundController.getCurrentRound();
        log.debug(round.toString());


        MeetingMember localMember = round.getLocalMember();
        if (null == localMember) {
            if (chain.isConsonsusNode()) {
                //Real time monitoring
                chain.setConsonsusNode(false);
            }
            log.info("here1112");
            return;
        }

        //If the round is not confirmed, recalculate each time,Because there was no confirmation, calculations were made based on time each time
        if (!round.isConfirmed()) {
            VoteStageResult result = tryIt(round, 0);
            if (result != null) {
                //Obtain the result, modify the correct round, and return
                log.info("tryItObtain results：");
                MeetingRound newRound = roundController.getRound(result.getRoundIndex(), result.getRoundStartTime());
                newRound.setPackingIndexOfRound(result.getPackingIndexOfRound());
                // Calculate whether there is a delay in this round, and if so, calculate and set it in
                checkDelayedTime(newRound);
                this.comfirmedResult(newRound, result);
            }
            log.info("here1113");
            return;
        }

        if (round.getPackingIndexOfRound() > round.getMemberCount()) {
            log.info("Switch to the next round：");
            roundController.nextRound(round);
            return;
        }
        long packStartTime = round.getStartTime() + round.getDelayedSeconds() + chain.getConfig().getPackingInterval() * (round.getPackingIndexOfRound() - 1);
        long packEndTime = packStartTime + chain.getConfig().getPackingInterval();
        //If the round has been confirmed and it happens that the block should be produced locally,Time wise Double the fault tolerance time
        boolean timeOk = (packEndTime * 1000 + VOTE_TIME_OUT) > NulsDateUtils.getCurrentTimeMillis();
        if (localMember.getPackingIndexOfRound() == round.getPackingIndexOfRound() && timeOk) {
            //Local block output, only availableroundIt was only after being confirmed that everyone agreed that the block began to be produced
            PackingData packingData = new PackingData();
            packingData.setMember(localMember);
            packingData.setPackStartTime(packStartTime);
            packingData.setRound(round);
            chain.getConsensusCache().getPackingQueue().offer(packingData);
            log.info("Prepare to produce blocks{}-{},height：{},start time：" + NulsDateUtils.timeStamp2Str(packingData.getPackStartTime() * 1000)
                    , round.getIndex(), localMember.getPackingIndexOfRound(), chain.getBestHeader().getHeight() + 1);
        }
        long wait = 0;
        if (!timeOk) {
            log.info("Time has passed, the next person should come out with a block");
            this.voteController.voteEmptyHash(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getStartTime(), round.getPackingIndexOfRound(), 0, packEndTime, round.getLocalMember().getAgent().getPackingAddressStr());
        } else {
            wait = chain.getConfig().getPackingIntervalMills() + packStartTime * 1000 - NulsDateUtils.getCurrentTimeMillis();
        }
        waitComfirmed(round, wait, 0, localMember.getAgent().getPackingAddressStr());

    }

    //Iterative attempt to obtain final result
    //After block verification is passed, a vote is required
    private void waitComfirmed(MeetingRound round, long packingTimeMills, int voteRoundIndex, String address) {
        //Record the current time for re recording；
        long time = System.currentTimeMillis() + packingTimeMills;
        if (!chain.isConsonsusNode() || !chain.isSynchronizedHeight() || !chain.isNetworkStateOk()) {
            return;
        }
        //If you wait too many times here, forget it
        if (voteRoundIndex > 5) {
            round.setConfirmed(false);
//            checkRoundTimeout();
            //Clear cached data here
            this.voteController.clearCache();
            this.voteController.clearMap(chain.getBestHeader().getHeight());
            return;
        }
        log.debug("Waiting for consensus to be reached！" + voteRoundIndex);
        this.chain.getConsensusCache().getBestBlocksVotingContainer().setCurrentVoteRoundIndex(voteRoundIndex);

        long realWait = packingTimeMills + VOTE_TIME_OUT - (NulsDateUtils.getCurrentTimeMillis() % 1000);
        VoteStageResult result = chain.getConsensusCache().getStageTwoQueue().poll(realWait, TimeUnit.MILLISECONDS);
        if (null == result) {
            log.debug("Time out, get an empty result：wait:" + realWait);
            // If we had invested in blocks before, we shouldn't have invested in empty blocks again
            //All data at the same height are cached, including the current voteindex
            this.voteController.startNextVoteRound(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getPackingIndexOfRound(),
                    round.getStartTime(), voteRoundIndex + 1, address);
            round.setDelayedSeconds(round.getDelayedSeconds() + VOTE_TIME_OUT / 1000);
            this.waitComfirmed(round, 0, voteRoundIndex + 1, address);

        } else if (null != result && !this.comfirmedResult(round, result)) {
            //Remove the parts that have already passed from the waiting time
            this.waitComfirmed(round, time - System.currentTimeMillis(), voteRoundIndex + 1, address);
        }
    }

    private boolean comfirmedResult(MeetingRound round, VoteStageResult result) {
        log.info("here1114");
        //Clear cached data here
        this.voteController.clearCache();
        round.setConfirmed(true);
        if (result.getRoundIndex() < chain.getConsensusCache().getLastConfirmedRoundIndex() ||
                (result.getRoundIndex() == chain.getConsensusCache().getLastConfirmedRoundIndex() && result.getPackingIndexOfRound() <= chain.getConsensusCache().getLastConfirmedRoundPackingIndex())) {
            return false;
        }
        log.debug("now:{},result:{}-{}-{}-{}-{}-{}-{}-delay:{}", chain.getBestHeader().getHeight(), result.getHeight(), result.getRoundIndex(), result.getPackingIndexOfRound(), result.getRoundStartTime(),
                result.getBlockHash(), result.getVoteRoundIndex(), result.getStage(), round.getDelayedSeconds());
        chain.getConsensusCache().setLastConfirmed(result.getRoundIndex(), result.getPackingIndexOfRound());
        if (result.getBlockHash().equals(NulsHash.EMPTY_NULS_HASH)) {

            log.debug("Next node");

            long packTime = result.getRoundStartTime() + result.getPackingIndexOfRound() * chain.getConfig().getPackingInterval();
            if (result.getRoundIndex() == round.getIndex()) {
                packTime += round.getDelayedSeconds();
            }
            this.roundController.switchPackingIndex(result.getRoundIndex(), result.getRoundStartTime(), result.getPackingIndexOfRound() + 1, packTime);

            return true;
        }

        if (result.getHeight() <= chain.getBestHeader().getHeight()) {
            log.info("Repeated voting results：" + result.getHeight());
            return false;
        }
        if (result.getHeight() > this.comfirmedHeight) {
            this.comfirmedHeight = result.getHeight();
        } else {
            return false;
        }
        //Cache signature records for this block
        this.voteController.cacheSignResult(result);

        //Notify the block module first,Switch packagers only when saving blocks
        log.info("Notification block module, Byzantine verification passed：" + result.getHeight() + "-" + result.getBlockHash().toHex());
        CallMethodUtils.noticeByzantineResult(chain, result.getHeight(), false, result.getBlockHash(), null);
        log.info("Notification block module, Byzantine verification completed");
        return true;
    }

    //If you can find the majority, iterate and continue to search. If you can't find the majority, try again
    private VoteStageResult tryIt(MeetingRound round, int count) {
        if (round == null) {
            round = roundController.tempRound();
        }
        if (round.isConfirmed()) {
            return null;
        }

        //Clear cached data here
        if (count >= 30) {
            this.voteController.clearCache();
            chain.getConsensusCache().clear();
            chain.getConsensusCache().getVoteMessageQueue().clear();
            this.voteController.clearMap();
            count = 0;
        }
        //ensureroundTimeliness of
        round = checkRoundByTime(round);
        MeetingMember localMember = round.getLocalMember();
        if (null == localMember) {
            return null;
        }
        log.debug("Attempt to obtain a confirmed round,tryIt");
        long packEndTime = round.getStartTime() + round.getDelayedSeconds() + chain.getConfig().getPackingInterval() * round.getPackingIndexOfRound();
        //First, express your attitude
//        log.info("Throwing empty blocks：{}-{}-{}/{},roundStart:{}", height, round.getIndex(), round.getPackingIndexOfRound(), round.getMemberCount(), NulsDateUtils.timeStamp2Str(round.getStartTime() * 1000));
        this.voteController.voteEmptyHash(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getStartTime(), round.getPackingIndexOfRound(), 1,
                packEndTime, localMember.getAgent().getPackingAddressStr());
        long wait = packEndTime * 1000 - NulsDateUtils.getCurrentTimeMillis();
        if (wait < 0) {
            return null;
        }
        VoteStageResult result = chain.getConsensusCache().getStageTwoQueue().poll(wait, TimeUnit.MILLISECONDS);
        if (result == null) {
            log.debug("Timed out, no valid result obtained:{}ms", wait);
            return this.tryIt(roundController.getCurrentRound(), count + 1);
        }
        return result;
    }

    private boolean checkRoundTimeout() {
        //More than five minutes
        MeetingRound currentRound = roundController.getCurrentRound();
        if (NulsDateUtils.getCurrentTimeSeconds() - currentRound.getStartTime() > 300) {
            voteController.clearCache();
            chain.getConsensusCache().clear();
            MeetingRound round1 = this.roundController.initRound();
            this.roundController.switchRound(round1, true);
            return false;
        }
        return true;
    }

    //If the round is not confirmed, recalculate each time,Because there was no confirmation, calculations were made based on time each time
    private MeetingRound checkRoundByTime(MeetingRound round) {
        long roundEndTimeMills = round.getStartTimeMills() + round.getMemberCount() * chain.getConfig().getPackingIntervalMills();
        if (roundEndTimeMills <= NulsDateUtils.getCurrentTimeMillis()) {
            //The round has expired, so recalculate based on time
            chain.getLogger().debug("When consensus has not yet been reached, initiate the round and seek common opportunities");
            round = roundController.initRound();
        }
        long index = (NulsDateUtils.getCurrentTimeMillis() - round.getStartTimeMills()) / chain.getConfig().getPackingIntervalMills();
        round.setPackingIndexOfRound((int) (index + 1));
        return round;
    }

    // Calculate whether there is a delay in this round, and if so, calculate and set it in
    private void checkDelayedTime(MeetingRound newRound) {
        // If the delay time is not0This indicates that this round already existed before and was not initialized, so there is no need to worry about it
        if (newRound.getDelayedSeconds() != 0) {
            return;
        }
        // Obtain the latest confirmation block's round information
        BlockHeader bestHeader = chain.getBestHeader();
        if (bestHeader == null) {
            return;
        }
        BlockExtendsData bestRoundData = bestHeader.getExtendsData();
        if (bestRoundData.getRoundIndex() != newRound.getIndex()) {
            // We've all jumped the wheels, let's ignore this situation
            return;
        }
        // Calculate the expected and actual time of the recent block, compare them, and then calculate how long the delay is before that
        long realTime = bestHeader.getTime();
        long expectedTime = bestRoundData.getRoundStartTime() + bestRoundData.getPackingIndexOfRound() * chain.getConfig().getPackingInterval();
        long delayedTime = realTime - expectedTime;
        // Compare the latest block with the current time to see if there is any delay
        long diffTime = NulsDateUtils.getCurrentTimeSeconds() - realTime;
        if (diffTime > chain.getConfig().getPackingInterval()) {
            long newestDelayedTime = (diffTime - (newRound.getPackingIndexOfRound() - bestRoundData.getPackingIndexOfRound()) * chain.getConfig().getPackingInterval());
            // Here, due to the inconsistency between the current reality and the actual running nodes, it is advisable to approach reality as closely as possible and retrieve backwardsVOTE_TIME_OUTInteger of
            delayedTime += Math.ceil((float) newestDelayedTime / (float) VOTE_TIME_OUT) * VOTE_TIME_OUT;
        }
        newRound.setDelayedSeconds(delayedTime);
    }
}
