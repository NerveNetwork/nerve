package network.nerve.pocbft.model.bo.vote;

import io.nuls.base.data.NulsHash;
import network.nerve.pocbft.message.VoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class VoteStageData {
    /**
     * 已投票地址列表(消息去重)
     * */
    private Set<String> haveVotedAccountSet;
    /**
     * hash与VoteResultItem键值对 （避免重复序列化反序列化）
     * key:VoteResultItem的hash
     * value:VoteResultItem
     * */
    private Map<NulsHash, VoteResultItem> itemMap;
    /**
     * 当前投票阶段投票消息（用于切换投票阶段时用，为啥采用map是为了处理切换过程中收到当前阶段的投票信息）
     * key:投票账户
     * value:投票信息
     * */
    private Map<String, VoteMessage> voteMessageMap;
    /**
     * 投票结果与得票率键值对
     * key:投票结果
     * value:得票率
     * */
    private Map<NulsHash, Integer> itemVoteCountMap;
    /**
     * 当前阶段投票完成数据
     * */
    private CompletableFuture<VoteResultData> voteResult;

    private boolean finished;


    public VoteStageData(){
        this.haveVotedAccountSet = new CopyOnWriteArraySet<>();
        this.itemMap = new HashMap<>();
        this.voteMessageMap = new ConcurrentHashMap<>();
        this.itemVoteCountMap = new HashMap<>();
        this.voteResult = new CompletableFuture<>();
        this.finished = false;
    }


    public Set<String> getHaveVotedAccountSet() {
        return haveVotedAccountSet;
    }

    public void setHaveVotedAccountSet(Set<String> haveVotedAccountSet) {
        this.haveVotedAccountSet = haveVotedAccountSet;
    }

    public Map<NulsHash, VoteResultItem> getItemMap() {
        return itemMap;
    }

    public void setItemMap(Map<NulsHash, VoteResultItem> itemMap) {
        this.itemMap = itemMap;
    }

    public Map<NulsHash, Integer> getItemVoteCountMap() {
        return itemVoteCountMap;
    }

    public void setItemVoteCountMap(Map<NulsHash, Integer> itemVoteCountMap) {
        this.itemVoteCountMap = itemVoteCountMap;
    }

    public CompletableFuture<VoteResultData> getVoteResult() {
        return voteResult;
    }

    public void setVoteResult(CompletableFuture<VoteResultData> voteResult) {
        this.voteResult = voteResult;
    }

    public Map<String, VoteMessage> getVoteMessageMap() {
        return voteMessageMap;
    }

    public void setVoteMessageMap(Map<String, VoteMessage> voteMessageMap) {
        this.voteMessageMap = voteMessageMap;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
