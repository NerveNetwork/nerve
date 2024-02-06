package io.nuls.consensus.v1.cache;

import io.nuls.base.data.NulsHash;
import io.nuls.consensus.v1.entity.*;
import io.nuls.consensus.v1.utils.HashSetDuplicateProcessor;
import io.nuls.consensus.v1.message.VoteResultMessage;

import java.util.*;

/**
 * @author Eva
 */
public class ConsensusCache {
    /**
     * The latest block that has passed cache verification
     */
    private BestBlocksVotingContainer bestBlocksVotingContainer = new BestBlocksVotingContainer();

    /**
     * Store newly received itemstou
     * VoteMessageQueue
     */
    private VoteMessageQueue voteMessageQueue = new VoteMessageQueue();
    private BlockHeaderQueue blockHeaderQueue = new BlockHeaderQueue();
    private IdentityMessageQueue identityMessageQueue = new IdentityMessageQueue();
    private IdentityMessageQueue disConnectMessageQueue = new IdentityMessageQueue();
    private ShareMessageQueue shareMessageQueue = new ShareMessageQueue();

    /**
     * The results of the first stage
     */
    private VoteResultStageOneQueue stageOneQueue = new VoteResultStageOneQueue();

    /**
     * The results of the second stage
     */
    private VoteResultStageTwoQueue stageTwoQueue = new VoteResultStageTwoQueue();

    private VoteResultQueue voteResultQueue = new VoteResultQueue();

    /**
     * Cache Recently5The voting results for the second round of confirmation of blocks
     */
    private List<NulsHash> signResultList = new ArrayList<NulsHash>();
    private Map<NulsHash, VoteResultMessage> signResultMap = new HashMap<>();

    /**
     * Notify the outbound thread to proceed with packaging
     */
    private PackingQueue packingQueue = new PackingQueue();

    private long lastConfirmedRoundIndex;
    private int lastConfirmedRoundPackingIndex;

    public long getLastConfirmedRoundIndex() {
        return lastConfirmedRoundIndex;
    }

    public void setLastConfirmed(long lastConfirmedRoundIndex, int lastConfirmedRoundPackingIndex) {
        this.lastConfirmedRoundIndex = lastConfirmedRoundIndex;
        this.lastConfirmedRoundPackingIndex = lastConfirmedRoundPackingIndex;
    }

    public int getLastConfirmedRoundPackingIndex() {
        return lastConfirmedRoundPackingIndex;
    }

    /**
     * Message deduplication
     */
    private HashSetDuplicateProcessor<String> msgDuplicateProcessor = new HashSetDuplicateProcessor<>(2048);

    public HashSetDuplicateProcessor<String> getMsgDuplicateProcessor() {
        return msgDuplicateProcessor;
    }


    public BestBlocksVotingContainer getBestBlocksVotingContainer() {
        return bestBlocksVotingContainer;
    }

    public VoteResultQueue getVoteResultQueue() {
        return voteResultQueue;
    }

    public PackingQueue getPackingQueue() {
        return packingQueue;
    }


    public VoteResultStageOneQueue getStageOneQueue() {
        return stageOneQueue;
    }


    public VoteResultStageTwoQueue getStageTwoQueue() {
        return stageTwoQueue;
    }

    public VoteMessageQueue getVoteMessageQueue() {
        return voteMessageQueue;
    }

    public void setVoteMessageQueue(VoteMessageQueue voteMessageQueue) {
        this.voteMessageQueue = voteMessageQueue;
    }

    public void clear() {
        voteMessageQueue.clear();
        stageOneQueue.clear();
        stageTwoQueue.clear();
    }

    public void cacheSignResult(VoteResultMessage message) {
        signResultMap.put(message.getBlockHash(), message);
//        Log.info("put:" + message.getBlockHash().toHex());
        signResultList.add(message.getBlockHash());
        if (signResultList.size() > 50) {
            NulsHash item = signResultList.remove(0);
            signResultMap.remove(item);
        }
    }

    public VoteResultMessage getVoteResult(NulsHash blockHash) {
//        Log.info("get:" + blockHash.toHex());
        return signResultMap.get(blockHash);
    }

    public IdentityMessageQueue getIdentityMessageQueue() {
        return identityMessageQueue;
    }

    public ShareMessageQueue getShareMessageQueue() {
        return shareMessageQueue;
    }

    public IdentityMessageQueue getDisConnectMessageQueue() {
        return disConnectMessageQueue;
    }

    public BlockHeaderQueue getBlockHeaderQueue() {
        return blockHeaderQueue;
    }
}
