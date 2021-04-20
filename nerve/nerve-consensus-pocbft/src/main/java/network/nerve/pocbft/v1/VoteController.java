package network.nerve.pocbft.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.v1.entity.BasicObject;
import network.nerve.pocbft.v1.entity.BestBlocksVotingContainer;
import network.nerve.pocbft.v1.entity.VoteStageResult;
import network.nerve.pocbft.v1.entity.VoteSummaryData;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;
import network.nerve.pocbft.v1.utils.HashSetDuplicateProcessor;

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
     * key: 高度+轮次信息+区块hash
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
//        log.info("收到投票：{}-({}-{}),vote:{}-{},from:{}-{}", vote.getHeight(), vote.getRoundIndex(), vote.getPackingIndexOfRound(),
//                vote.getVoteRoundIndex(), vote.getVoteStage(), vote.getAddress(chain), vote.getBlockHash().toHex());
        if (chain.getBestHeader().getHeight() >= vote.getHeight()) {
            return;
        }
        VoteSummaryData data = summaryMap.computeIfAbsent(vote.getTargetKey(), val -> new VoteSummaryData(chain));
        MeetingRound round = roundController.getRound(vote.getRoundIndex(), vote.getRoundStartTime());
        //如果不是轮次中的投票，则丢弃
        if (!round.getMemberAddressSet().contains(vote.getAddress(chain))) {
//            log.info("丢弃投票，不是共识成员：" + vote.getHeight() + "-" + vote.getVoteRoundIndex() + "-" + vote.getVoteStage() + ": " + vote.getBlockHash().toHex());
            return;
        }
        data.addVote(vote, round);
        //区块不存在，则获取区块
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
     * 广播投票消息
     *
     * @param message
     */
    public void broadcastVote(VoteMessage message) {
        //   去重
        if (!duplicateProcessor.insertAndCheck(message.getMessageKey())) {
            log.info("重复投票！！！");
            return;
        }
        ConsensusNetUtil.broadcastInConsensus(chain.getChainId(), CommandConstant.MESSAGE_VOTE, message.getRawData(), message.getSendNode());
        log.debug("广播投票：{}-{}-{}-{}-{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
    }

    /**
     * 上一轮没有得到结果，下一轮继续
     *
     * @param votingHeight
     * @param roundIndex
     * @param packingIndexOfRound
     * @param roundStartTime
     * @param voteRoundIndex
     */
    public void startNextVoteRound(long votingHeight, long roundIndex, int packingIndexOfRound,
                                   long roundStartTime, long voteRoundIndex, String address) {
        //根据上次投票记录，进行本次的选择
        VoteMessage message = new VoteMessage();
        message.setVoteRoundIndex(voteRoundIndex);
        message.setRoundStartTime(roundStartTime);
        message.setHeight(votingHeight);
        message.setRoundIndex(roundIndex);
        message.setPackingIndexOfRound(packingIndexOfRound);

        //这里计算可能需要保证，必须是已确认轮次的
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
            sign = CallMethodUtils.signature(chain, address, message.getHash().getBytes());
        } catch (IOException | NulsException e) {
            LoggerUtil.commonLog.error(e);
            return;
        }
        message.setSign(sign);

//        this.votingContainer.votedStage1(message);
        log.debug("超时投票：{}-{}-{}-{}:{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
//        log.info("=========================本地投票：({}-{})" + message.getHeight() + "-" + message.getVoteRoundIndex() + "-" + message.getVoteStage() + ": " + message.getBlockHash().toHex(),
//                message.getRoundIndex(), message.getPackingIndexOfRound());
        //广播
        this.broadcastVote(message);
        //再给本地
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void clearMap() {
        this.summaryMap.clear();
    }

    /**
     * 避免删除其他节点提前投票的数据
     *
     * @param votingHeight
     */
    public void clearMap(long votingHeight) {
        Map<String, VoteSummaryData> temp = summaryMapBak;
        summaryMapBak = summaryMap;
        //把不应该删除的summary放到新的map里
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
     * 第一阶段直接投一个空块，代表轮次中的出块人，没有出块
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
     * 第一阶段，投票给一个区块，尽快广播到全网
     * 给自己节点也一份
     *
     * @param header
     */
    public void doVote(BlockHeader header) {
        BlockExtendsData extendsData = header.getExtendsData();
        MeetingRound round = roundController.getCurrentRound();
        if (round == null || round.getLocalMember() == null) {
            log.debug("本地不是共识节点。");
            return;
        }
        String address = round.getLocalMember().getAgent().getPackingAddressStr();
        realVote(header.getHeight(), extendsData.getRoundIndex(), extendsData.getRoundStartTime(), extendsData.getPackingIndexOfRound(),
                this.votingContainer.getCurrentVoteRoundIndex(), header.getTime(), address, header.getHash());

    }

    private void realVote(long votingHeight, long roundIndex, long roundStartTime, int packingIndexOfRound, long voteRoundIndex, long packEndTime, String address, NulsHash blockHash) {
        //先标记，避免重复投票
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
            sign = CallMethodUtils.signature(chain, address, message.getHash().getBytes());
        } catch (IOException | NulsException e) {
            log.error(e);
            return;
        }
        message.setSign(sign);
//        this.votingContainer.votedStage1(message);
//        log.info("=========================本地投票：({}-{})" + message.getHeight() + "-" + message.getVoteRoundIndex() + "-" + message.getVoteStage() + ": " + message.getBlockHash().toHex(),
//                message.getRoundIndex(), message.getPackingIndexOfRound());
        //广播
        this.broadcastVote(message);
        //再给本地
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void cacheSignResult(VoteStageResult result) {
        chain.getConsensusCache().cacheSignResult(result.getResultMessage());
        //本高度的记录没用了
        this.clearMap(result.getHeight());
    }

    public void voteStageTwo(VoteMessage message) {
        this.votingContainer.votedStage2(message);
        log.debug("投票2轮投票：{}-{}-{}-{}-{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRoundIndex(), message.getBlockHash().toHex());
        //广播
        this.broadcastVote(message);
        //再给本地
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }

    public void clearCache() {
        this.votingContainer.clear(false);
        this.votingContainer.clearRecorder();
    }
}
