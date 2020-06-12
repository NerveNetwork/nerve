package network.nerve.pocbft.model.bo.vote;

import com.google.common.collect.Sets;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.message.VoteMessage;
import network.nerve.pocbft.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoteRoundData {
    /**
     * 投票轮次开始时间 上一轮时间+2S（1秒等待出块，1.投票）
     * */
    private long time;

    private VoteStageData stageOne;

    private VoteStageData stageTwo;

    /**
     * 避免在轮次切换过程中收到本轮次多余消息
     * */
    private boolean finished;

    /**
     * 本轮次投过票的节点信息，用于判断是否可以向这些节点获取上一轮投票结果
     * */
    private Set<String> voteNodeSet;

    public VoteRoundData(){
        this.stageOne = new VoteStageData();
        this.stageTwo = new VoteStageData();
        this.voteNodeSet = new HashSet<>();
        finished = false;
    }

    public VoteRoundData(long time){
        this();
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public VoteStageData getStageOne() {
        return stageOne;
    }

    public void setStageOne(VoteStageData stageOne) {
        this.stageOne = stageOne;
    }

    public VoteStageData getStageTwo() {
        return stageTwo;
    }

    public void setStageTwo(VoteStageData stageTwo) {
        this.stageTwo = stageTwo;
    }

    public Set<String> getVoteNodeSet() {
        return voteNodeSet;
    }

    public void setVoteNodeSet(Set<String> voteNodeSet) {
        this.voteNodeSet = voteNodeSet;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public VoteStageData getVoteStageDate(byte voteStage){
        return voteStage == ConsensusConstant.VOTE_STAGE_ONE ? stageOne : stageTwo;
    }

    public void addVoteStageData(Chain chain, VoteMessage message){
        VoteStageData stageData = message.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE ? stageOne : stageTwo;
        if(!stageData.getHaveVotedAccountSet().add(message.getAddress(chain))){
            return;
        }
        stageData.getVoteMessageMap().putIfAbsent(message.getAddress(chain), message);
        chain.getLogger().debug("缓存未来投票信息，VoteRoundKey:{}", message.getMessageKey());
    }

    public void addVoteStageData(Chain chain, VoteMessage message, byte stage){
        VoteStageData stageData = stage == ConsensusConstant.VOTE_STAGE_ONE ? stageOne : stageTwo;
        if(!stageData.getHaveVotedAccountSet().add(message.getAddress(chain))){
            chain.getLogger().debug("Repeated voting" );
            return;
        }
        stageData.getVoteMessageMap().putIfAbsent(message.getAddress(chain), message);
    }

    public List<VoteMessage> getMissMessage(Set<String> existKey, byte stage){
        try {
            VoteStageData stageData = stage == ConsensusConstant.VOTE_STAGE_ONE ? stageOne : stageTwo;
            if(existKey.size() == stageData.getVoteMessageMap().size()){
                return null;
            }
            Set<String> allKey = stageData.getVoteMessageMap().keySet();
            Set<String> differenceSet = Sets.difference(allKey, existKey);
            List<VoteMessage> missList = new ArrayList<>();
            for (String key : differenceSet) {
                missList.add(stageData.getVoteMessageMap().get(key));
            }
            return missList;
        }catch (Exception e){
            LoggerUtil.commonLog.error(e);
            return null;
        }
    }
}
