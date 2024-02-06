package io.nuls.consensus.v1.entity;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.v1.message.VoteMessage;
import io.nuls.consensus.v1.utils.HashSetDuplicateProcessor;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eva
 */
public class BestBlocksVotingContainer {

    /**
     * Voted records
     */
    private HashSetDuplicateProcessor<String> localStage1VotedRecorder = new HashSetDuplicateProcessor<>(1024);
    /**
     * Obtained result record
     */
    private HashSetDuplicateProcessor<String> stage1ResultRecorder = new HashSetDuplicateProcessor<>(1024);

    private long currentHeight;
    private int currentVoteRoundIndex;
    private NulsHash votedBlockHash;
    private long votedHeight;

    private Map<NulsHash, BlockHeader> map = new HashMap<>();

    private Lock lock = new ReentrantLock();

    private LocalBlockListener listener;

    public void addBlock(Chain chain, BlockHeader header) {
        lock.lock();
        try {
            if (currentHeight < header.getHeight() || NulsHash.EMPTY_NULS_HASH.equals(this.votedBlockHash)) {
                this.clear(true);
                this.currentHeight = header.getHeight();
                this.votedBlockHash = header.getHash();
                if (null != this.listener) {
                    this.listener.onChange(header);
                }
            } else {
                chain.getLogger().info("This block does not vote：" + header.getHash().toHex());
            }
            map.put(header.getHash(), header);
        } finally {
            lock.unlock();
        }
    }

    public BlockHeader calcNextVotingItem(Chain chain, long votingHeight, long roundIndex, int packingIndexOfRound, long roundStartTime) {
        if (votingHeight == votedHeight && !NulsHash.EMPTY_NULS_HASH.equals(votedBlockHash)) {
            return map.get(votedBlockHash);
        }
        List<NulsHash> list = new ArrayList<>();
        list.addAll(map.keySet());
        list.sort(new Comparator<NulsHash>() {
            @Override
            public int compare(NulsHash o1, NulsHash o2) {
                return Arrays.compare(o1.getBytes(), o2.getBytes());
            }
        });
        for (int i = 0; i < map.size(); i++) {
            BlockHeader header = map.get(list.get(0));
            if (header.getHeight() != votingHeight) {
                continue;
            }

            BlockExtendsData data = header.getExtendsData();
            //If the round is confirmed, then the block must not match the round either
            if (roundIndex == data.getRoundIndex() && packingIndexOfRound == data.getPackingIndexOfRound()
                    && roundStartTime == data.getRoundStartTime()) {
                return header;
//            } else {
//                chain.getLogger().info("The block is not suitable for this vote：{}", header.getHash().toHex());
            }

        }
        return null;
    }

    public void clear(boolean self) {
        this.votedBlockHash = NulsHash.EMPTY_NULS_HASH;
        if (self) {
            map.clear();
        }
    }

    public void clearRecorder() {
        this.getLocalStage1VotedRecorder().clear();
        this.getStage1ResultRecorder().clear();
    }

    public Map<NulsHash, BlockHeader> getMap() {
        return map;
    }

    public void setListener(LocalBlockListener listener) {
        this.listener = listener;
    }

    public boolean isExist(NulsHash blockHash) {
        return this.map.containsKey(blockHash);
    }

    public HashSetDuplicateProcessor<String> getLocalStage1VotedRecorder() {
        return localStage1VotedRecorder;
    }


    public HashSetDuplicateProcessor<String> getStage1ResultRecorder() {
        return stage1ResultRecorder;
    }

    public void votedStage2(VoteMessage message) {
        this.votedBlockHash = message.getBlockHash();
        this.votedHeight = message.getHeight();
    }

    public int getCurrentVoteRoundIndex() {
        return currentVoteRoundIndex;
    }

    public void setCurrentVoteRoundIndex(int currentVoteRoundIndex) {
        this.currentVoteRoundIndex = currentVoteRoundIndex;
    }
}
