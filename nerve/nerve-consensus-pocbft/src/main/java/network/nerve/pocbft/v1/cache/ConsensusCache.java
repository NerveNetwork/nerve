package network.nerve.pocbft.v1.cache;

import io.nuls.base.data.NulsHash;
import io.nuls.core.log.Log;
import network.nerve.pocbft.v1.entity.*;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;
import network.nerve.pocbft.v1.utils.HashSetDuplicateProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class ConsensusCache {
    /**
     * 缓存验证通过的最新区块
     */
    private BestBlocksVotingContainer bestBlocksVotingContainer = new BestBlocksVotingContainer();

    /**
     * 存放刚收到的tou
     * VoteMessageQueue
     */
    private VoteMessageQueue voteMessageQueue = new VoteMessageQueue();

    /**
     * 第一阶段的结果
     */
    private VoteResultStageOneQueue stageOneQueue = new VoteResultStageOneQueue();

    /**
     * 第二阶段的结果
     */
    private VoteResultStageTwoQueue stageTwoQueue = new VoteResultStageTwoQueue();

    private VoteResultQueue voteResultQueue = new VoteResultQueue();

    /**
     * 缓存最近5个区块的第二轮确认的投票结果
     */
    private List<NulsHash> signResultList = new ArrayList<NulsHash>();
    private Map<NulsHash, VoteResultMessage> signResultMap = new HashMap<>();

    /**
     * 通知出块线程进行打包
     */
    private PackingQueue packingQueue = new PackingQueue();

    /**
     * 消息去重
     */
    private HashSetDuplicateProcessor<String> msgDuplicateProcessor = new HashSetDuplicateProcessor<>(1000);

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

}
