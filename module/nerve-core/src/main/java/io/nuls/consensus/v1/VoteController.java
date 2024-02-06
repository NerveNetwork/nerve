package io.nuls.consensus.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.BestBlocksVotingContainer;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.consensus.v1.entity.VoteSummaryData;
import io.nuls.consensus.v1.utils.HashSetDuplicateProcessor;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.CommandConstant;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.v1.message.VoteMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eva
 */
public class VoteController extends BasicObject {

    private final RoundController roundController;

    private BestBlocksVotingContainer votingContainer;
    /**
     * key: height+Round information+blockhash
     */
    private Map<String, VoteSummaryData> summaryMap = new ConcurrentHashMap<>();
    private Map<String, VoteSummaryData> summaryMapBak = new ConcurrentHashMap<>();

    public VoteController(Chain chain, RoundController roundController) {
        super(chain);
        this.roundController = roundController;
        this.votingContainer = chain.getConsensusCache().getBestBlocksVotingContainer();
    }

    public void addVote(VoteMessage vote) {
        //TargetKey: height + blockhash+roundIndex +packingIndex
//        log.info("Received vote：{}-({}-{}),vote:{}-{},from:{}-{}", vote.getHeight(), vote.getRoundIndex(), vote.getPackingIndexOfRound(),
//                vote.getVoteRoundIndex(), vote.getVoteStage(), vote.getAddress(chain), vote.getBlockHash().toHex());
        if (chain.getBestHeader().getHeight() >= vote.getHeight()) {
            vote.clear();
            return;
        }
        if (vote.getHeight() - chain.getBestHeader().getHeight() > 2) {
            vote.clear();
            return;
        }
        VoteSummaryData data = summaryMap.computeIfAbsent(vote.getTargetKey(), val -> new VoteSummaryData(chain));
        MeetingRound round = roundController.getRound(vote.getRoundIndex(), vote.getRoundStartTime());
        //If it is not a vote in the round, discard it
        if (!round.getMemberAddressSet().contains(vote.getAddress(chain))) {
//            log.info("Discard voting, not a consensus member：" + vote.getHeight() + "-" + vote.getVoteRoundIndex() + "-" + vote.getVoteStage() + ": " + vote.getBlockHash().toHex());
            vote.clear();
            return;
        }
        data.addVote(vote, round);
        //If the block does not exist, obtain the block
        if (vote.getBlockHash().equals(NulsHash.EMPTY_NULS_HASH)) {
            return;
        }
        boolean exist = chain.getConsensusCache().getBestBlocksVotingContainer().isExist(vote.getBlockHash());
        if (!exist && vote.getSendNode() != null) {
            CallMethodUtils.noticeGetBlock(chain, vote.getHeight(), vote.getSendNode(), vote.getBlockHash(), null);
        }
    }

    private HashSetDuplicateProcessor<String> duplicateProcessor = new HashSetDuplicateProcessor<>(100);

    /**
     * Broadcast voting messages
     *
     * @param message
     */
    public void broadcastVote(VoteMessage message) {
        //   Deduplication
        if (!duplicateProcessor.insertAndCheck(message.getMessageKey())) {
            log.info("Repeated voting！！！");
            return;
        }
        ConsensusNetUtil.broadcastInConsensus(chain.getChainId(), CommandConstant.MESSAGE_VOTE, message.getRawData(), message.getSendNode());
        log.debug("Broadcast voting：{}-{}-{}-{}-{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
    }

    /**
     * The previous round did not yield results, the next round will continue
     *
     * @param votingHeight
     * @param roundIndex
     * @param packingIndexOfRound
     * @param roundStartTime
     * @param voteRoundIndex
     */
    public void startNextVoteRound(long votingHeight, long roundIndex, int packingIndexOfRound,
                                   long roundStartTime, long voteRoundIndex, String address) {
        //Based on the last voting record, make this selection
        VoteMessage message = new VoteMessage();
        message.setVoteRoundIndex(voteRoundIndex);
        message.setRoundStartTime(roundStartTime);
        message.setHeight(votingHeight);
        message.setRoundIndex(roundIndex);
        message.setPackingIndexOfRound(packingIndexOfRound);

        //The calculation here may require assurance that it must be a confirmed round
        BlockHeader header = this.votingContainer.calcNextVotingItem(chain, votingHeight, roundIndex, packingIndexOfRound, roundStartTime);
        NulsHash hash = NulsHash.EMPTY_NULS_HASH;
        if (null != header) {
            hash = header.getHash();
            BlockExtendsData data = header.getExtendsData();
            message.setRoundStartTime(data.getRoundStartTime());
            message.setRoundIndex(data.getRoundIndex());
            message.setPackingIndexOfRound(data.getPackingIndexOfRound());
        } else {
            message.setRoundStartTime(roundStartTime);
            message.setRoundIndex(roundIndex);
            message.setPackingIndexOfRound(packingIndexOfRound);
        }
        message.setBlockHash(hash);
        message.setVoteStage(ConsensusConstant.VOTE_STAGE_ONE);

        byte[] sign;
        try {
            sign = CallMethodUtils.signature(chain, address, message.getHash().getBytes(), Map.of("voteMessage", HexUtil.encode(message.serialize()), "method", "voteMsgSign"));
        } catch (IOException | NulsException e) {
            LoggerUtil.commonLog.error(e);
            return;
        }
        message.setSign(sign);

//        this.votingContainer.votedStage1(message);
        log.debug("Overtime voting：{}-{}-{}-{}:{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
//        log.info("=========================Local voting：({}-{})" + message.getHeight() + "-" + message.getVoteRoundIndex() + "-" + message.getVoteStage() + ": " + message.getBlockHash().toHex(),
//                message.getRoundIndex(), message.getPackingIndexOfRound());
        //broadcast
        this.broadcastVote(message);
        //Give it to the local area again
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void clearMap() {
        this.summaryMap.clear();
    }

    /**
     * Avoid deleting data from early voting by other nodes
     *
     * @param votingHeight
     */
    public void clearMap(long votingHeight) {
        Map<String, VoteSummaryData> temp = summaryMapBak;
        summaryMapBak = summaryMap;
        //Remove things that should not be deletedsummaryPut it in a new onemapin
        for (Map.Entry<String, VoteSummaryData> entry : summaryMapBak.entrySet()) {
            VoteSummaryData value = entry.getValue();
            if (value.getHeight() > votingHeight) {
                temp.put(value.getTargetKey(), value);
            }
        }
        this.summaryMap = temp;
        this.summaryMapBak.clear();
    }


    /**
     * In the first stage, directly throw an empty block, representing the person who produced the block in the round, without producing the block
     *
     * @param votingHeight
     * @param roundIndex
     * @param roundStartTime
     * @param packingIndexOfRound
     * @param voteRoundIndex
     * @param packEndTime
     */
    public void voteEmptyHash(long votingHeight, long roundIndex, long roundStartTime, int packingIndexOfRound, long voteRoundIndex, long packEndTime, String address) {
        realVote(votingHeight, roundIndex, roundStartTime, packingIndexOfRound, voteRoundIndex, packEndTime, address, NulsHash.EMPTY_NULS_HASH);
    }

    /**
     * In the first stage, vote for a block and broadcast it to the entire network as soon as possible
     * Give yourself a share of the node as well
     *
     * @param header
     */
    public void doVote(BlockHeader header) {
        BlockExtendsData extendsData = header.getExtendsData();
        MeetingRound round = roundController.getCurrentRound();
        if (round == null || round.getLocalMember() == null) {
            log.debug("Local is not a consensus node.");
            return;
        }
        String address = round.getLocalMember().getAgent().getPackingAddressStr();
        realVote(header.getHeight(), extendsData.getRoundIndex(), extendsData.getRoundStartTime(), extendsData.getPackingIndexOfRound(),
                this.votingContainer.getCurrentVoteRoundIndex(), header.getTime(), address, header.getHash());

    }

    private void realVote(long votingHeight, long roundIndex, long roundStartTime, int packingIndexOfRound, long voteRoundIndex, long packEndTime, String address, NulsHash blockHash) {
        //Mark first to avoid duplicate voting
        this.votingContainer.getLocalStage1VotedRecorder()
                .insertAndCheck(votingHeight + ConsensusConstant.SEPARATOR + voteRoundIndex);
        VoteMessage message = new VoteMessage();
        message.setBlockHash(blockHash);
        message.setVoteStage(ConsensusConstant.VOTE_STAGE_ONE);
        message.setRoundStartTime(roundStartTime);
        message.setPackingIndexOfRound(packingIndexOfRound);
        message.setRoundIndex(roundIndex);
        message.setHeight(votingHeight);
        message.setVoteRoundIndex(voteRoundIndex);
        byte[] sign;
        try {
            sign = CallMethodUtils.signature(chain, address, message.getHash().getBytes(), Map.of("voteMessage", HexUtil.encode(message.serialize()), "method", "voteMsgSign"));
        } catch (IOException | NulsException e) {
            log.error(e);
            return;
        }
        message.setSign(sign);
//        this.votingContainer.votedStage1(message);
//        log.info("=========================Local voting：({}-{})" + message.getHeight() + "-" + message.getVoteRoundIndex() + "-" + message.getVoteStage() + ": " + message.getBlockHash().toHex(),
//                message.getRoundIndex(), message.getPackingIndexOfRound());
        //broadcast
        this.broadcastVote(message);
        //Give it to the local area again
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void cacheSignResult(VoteStageResult result) {
        chain.getConsensusCache().cacheSignResult(result.getResultMessage());
        //The record of this height is useless
        this.clearMap(result.getHeight());
    }

    public void voteStageTwo(VoteMessage message) {
        this.votingContainer.votedStage2(message);
        log.debug("vote2Round voting：{}-{}-{}-{}-{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
        //broadcast
        this.broadcastVote(message);
        //Give it to the local area again
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void clearCache() {
        this.votingContainer.clear(false);
        this.votingContainer.clearRecorder();
    }
}
