package network.nerve.pocbft.v1.entity;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.core.log.Log;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.utils.HashSetDuplicateProcessor;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eva
 */
public class BestBlocksVotingContainer {

    /**
     * 已投票的记录
     */
    private HashSetDuplicateProcessor<String> localStage1VotedRecorder = new HashSetDuplicateProcessor<>(64);
    /**
     * 已得到结果记录
     */
    private HashSetDuplicateProcessor<String> stage1ResultRecorder = new HashSetDuplicateProcessor<>(64);

    private long currentHeight;
    private int currentVoteRoundIndex;
    private NulsHash votedBlockHash;

    private Map<NulsHash, BlockHeader> map = new HashMap<>();

    private Lock lock = new ReentrantLock();

    private LocalBlockListener listener;

    public void addBlock(Chain chain, BlockHeader header) {
        lock.lock();
        try {
            if (currentHeight < header.getHeight()) {
                this.clear(true);
                currentHeight = header.getHeight();
                if (null != this.listener) {
                    this.listener.onChange(header);
                }
            } else {
                chain.getLogger().info("本区块不投票：" + header.getHash().toHex());
            }
            map.put(header.getHash(), header);
        } finally {
            lock.unlock();
        }
    }

    public BlockHeader calcNextVotingItem(Chain chain, long votingHeight, long roundIndex, int packingIndexOfRound, long roundStartTime) {
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
            //如果轮次是确认的，那么区块也不许和轮次一致
            if (roundIndex == data.getRoundIndex() && packingIndexOfRound == data.getPackingIndexOfRound()
                    && roundStartTime == data.getRoundStartTime()) {
                return header;
            } else {
                chain.getLogger().info("区块不适合本次投票：{}", header.getHash().toHex());
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

    public NulsHash getVotedBlockHash() {
        return votedBlockHash;
    }

    /**
     * 更新当前缓存的正在投票的信息
     *
     * @param message
     */
    public void votedStage1(VoteMessage message) {
        this.votedBlockHash = message.getBlockHash();
    }

    public void votedStage2(VoteMessage message) {
        this.votedBlockHash = message.getBlockHash();
    }

    public int getCurrentVoteRoundIndex() {
        return currentVoteRoundIndex;
    }

    public void setCurrentVoteRoundIndex(int currentVoteRoundIndex) {
        this.currentVoteRoundIndex = currentVoteRoundIndex;
    }
}
