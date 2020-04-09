package nerve.network.pocbft.model.bo.vote;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.message.VoteMessage;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.utils.enumeration.VoteTime;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class VoteData {
    /**
     * 投票轮次
     * */
    private long roundIndex;
    /**
     * 区块出块下标
     * */
    private int packingIndexOfRound;
    /**
     * 区块共识轮次开始时间
     * */
    private long roundStartTime;
    /**
     * 当前共识轮次共识节点数量
     * */
    private int agentCount;
    /**
     * 投票高度
     * */
    private long height;
    /**
     * 可投票区块Hash
     * */
    private NulsHash blockHash = NulsHash.EMPTY_NULS_HASH;
    /**
     * 分叉块1
     * */
    private BlockHeader firstHeader;
    /**
     * 分叉块2
     * */
    private BlockHeader secondHeader;
    /**
     * 当前投票轮次
     * */
    private byte voteRound;
    /**
     * 当前处理阶段，用于控制接收处理第一阶段多余消息
     * */
    private byte voteStage;
    /**
     * 当前确认区块是否已经确认完成，当出现有当前确认之后的区块提前被去人的情况下可以直接通知区块模块该区块已被确认，避免进入投票阶段
     * */
    private boolean finished;
    /**
     * 一个区块投票轮次信息
     * key:轮次
     * value:轮次数据
     * */
    private Map<Byte, VoteRoundData> voteRoundMap;

    /**
     * 拜占庭验证签名最小通过数
     * */
    private int minPassCount;

    /**
     * 最少收集到多少个签名才做拜占庭验证
     * */
    private int minByzantineCount;

    /**
     * 两个结果最小都收集到此数量签名时才出不了结果
     * */
    private int minCoverCount;

    public VoteData(long roundIndex, int packingIndexOfRound, int agentCount, long height, long roundStartTime){
        this.roundIndex = roundIndex;
        this.packingIndexOfRound = packingIndexOfRound;
        this.agentCount = agentCount;
        this.height = height;
        this.roundStartTime = roundStartTime;
        voteRound = ConsensusConstant.VOTE_INIT_ROUND;
        voteStage = ConsensusConstant.VOTE_STAGE_ONE;
        finished = false;
        voteRoundMap = new HashMap<>();
        minPassCount = 0;
        minByzantineCount = 0;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(int agentCount) {
        this.agentCount = agentCount;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(NulsHash blockHash) {
        this.blockHash = blockHash;
    }

    public BlockHeader getFirstHeader() {
        return firstHeader;
    }

    public void setFirstHeader(BlockHeader firstHeader) {
        this.firstHeader = firstHeader;
    }

    public BlockHeader getSecondHeader() {
        return secondHeader;
    }

    public void setSecondHeader(BlockHeader secondHeader) {
        this.secondHeader = secondHeader;
    }

    public Map<Byte, VoteRoundData> getVoteRoundMap() {
        return voteRoundMap;
    }

    public void setVoteRoundMap(Map<Byte, VoteRoundData> voteRoundMap) {
        this.voteRoundMap = voteRoundMap;
    }

    public byte getVoteRound() {
        return voteRound;
    }

    public void setVoteRound(byte voteRound) {
        this.voteRound = voteRound;
    }

    public byte getVoteStage() {
        return voteStage;
    }

    public void setVoteStage(byte voteStage) {
        this.voteStage = voteStage;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public int getMinPassCount() {
        return minPassCount;
    }

    public void setMinPassCount(int minPassCount) {
        this.minPassCount = minPassCount;
    }

    public int getMinCoverCount() {
        return minCoverCount;
    }

    public void setMinCoverCount(int minCoverCount) {
        this.minCoverCount = minCoverCount;
    }

    public int getMinByzantineCount() {
        return minByzantineCount;
    }

    public void setMinByzantineCount(int minByzantineCount) {
        this.minByzantineCount = minByzantineCount;
    }

    public VoteRoundData getCurrentRoundData(){
        return voteRoundMap.get(voteRound);
    }

    public String getConsensusKey(){
        return this.roundIndex + ConsensusConstant.SEPARATOR + this.packingIndexOfRound;
    }

    public VoteRoundData getRoundData(byte voteRound){
        return voteRoundMap.get(voteRound);
    }

    public String getCurrentVoteRoundKey(){
        return this.roundIndex + ConsensusConstant.SEPARATOR + this.packingIndexOfRound + ConsensusConstant.SEPARATOR + this.voteRound ;
    }

    /**
     * 获取当前投票轮次最终投票结果
     * Get the final result of the current voting round
     * */
    public VoteResultData getCurrentRoundVoteResult() throws Exception{
        return voteRoundMap.get(voteRound).getStageTwo().getVoteResult().get();
    }

    /**
     * 获取当前区块最终投票结果（说明该区块被提前确认了,区块最终投票轮次是特殊值0）
     * Get the final voting result of the current block
     * */
    public VoteResultData getFinalResult() throws Exception{
        return voteRoundMap.get(ConsensusConstant.FINAL_VOTE_ROUND_SIGN).getStageTwo().getVoteResult().get();
    }

    /**
     * 获取指定投票轮次，指定阶段的投票结果且指定获取的超时时间
     * @param chain       链信息
     * @param voteRound   投票轮次
     * @param voteStage   投票阶段
     * @param timeOut     获取结果超时时间
     * */
    public VoteResultData getVoteResult(Chain chain, byte voteRound, byte voteStage, long timeOut)throws  Exception{
        if(!voteRoundMap.containsKey(voteRound)){
            chain.getLogger().warn("Voting round does not exist");
            return null;
        }
        if(voteStage == ConsensusConstant.VOTE_STAGE_ONE){
            return voteRoundMap.get(voteRound).getStageOne().getVoteResult().get(timeOut, TimeUnit.MILLISECONDS);
        }else{
            return voteRoundMap.get(voteRound).getStageTwo().getVoteResult().get(timeOut, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 判断指定投票信息相对于本地投票轮次时间
     * @param voteMessage  投票信息
     * @return             投票时机
     * */
    public VoteTime voteTime(VoteMessage voteMessage){
        long roundIndex = voteMessage.getRoundIndex();
        int packingIndexOfRound = voteMessage.getPackingIndexOfRound();
        byte voteRound = voteMessage.getVoteRound();
        byte voteStage = voteMessage.getVoteStage();
        boolean isPrevious = roundIndex < this.roundIndex
                || (roundIndex == this.roundIndex && packingIndexOfRound < this.packingIndexOfRound)
                || (roundIndex == this.roundIndex && packingIndexOfRound == this.packingIndexOfRound && voteRound < this.voteRound)
                || (roundIndex == this.roundIndex && packingIndexOfRound == this.packingIndexOfRound && voteRound == this.voteRound && voteStage < this.voteStage);
        if(isPrevious){
            return VoteTime.PREVIOUS;
        }
        boolean isCurrentRound = roundIndex == this.roundIndex && packingIndexOfRound == this.packingIndexOfRound
                && voteRound == this.voteRound;
        if(isCurrentRound){
            if(voteStage == ConsensusConstant.VOTE_STAGE_ONE){
                return VoteTime.CURRENT_STAGE_ONE;
            }else{
                return VoteTime.CURRENT_STAGE_TWO;
            }
        }
        return VoteTime.FUTURE;
    }

    /**
     * 查看投票地址是否重复并添加地址
     * @param voteRound   投票轮次
     * @param voteStage   投票阶段
     * @param address     投票地址
     * */
    public boolean isRepeatMessage(byte voteRound,byte voteStage, String address){
        VoteRoundData voteRoundData = voteRoundMap.get(voteRound);
        if(voteRoundData == null){
            return false;
        }
        VoteStageData voteStageData = voteRoundData.getVoteStageDate(voteStage);
        if(voteStageData == null){
            return false;
        }
        return !voteStageData.getHaveVotedAccountSet().add(address);
    }

    /**
     * 添加本轮次之后的投票信息
     * @param chain    链信息
     * @param message  投票信息
     * @param nodeId   消息发送节点
     * */
    public void addVoteMessage(Chain chain, VoteMessage message, String nodeId){
        VoteRoundData voteRoundData = voteRoundMap.get(message.getVoteRound());
        if(voteRoundData == null){
            voteRoundData = new VoteRoundData(message.getTime());
            voteRoundMap.put(message.getVoteRound(), voteRoundData);
        }
        voteRoundData.getVoteNodeSet().add(nodeId);
        voteRoundData.addVoteStageData(chain, message);
    }

    /**
     * 添加某一投票轮次的信息
     * @param chain             链信息
     * @param voteRoundData     该轮次投票信息
     * @param voteRound         投票轮次
     * */
    public void addVoteRoundMessage(Chain chain, VoteRoundData voteRoundData, byte voteRound){
        if(!voteRoundMap.containsKey(voteRound)){
            voteRoundMap.put(voteRound, voteRoundData);
            return;
        }
        voteRoundMap.get(voteRound).getVoteNodeSet().addAll(voteRoundData.getVoteNodeSet());
        for (VoteMessage message:voteRoundData.getStageOne().getVoteMessageMap().values()) {
            voteRoundMap.get(voteRound).addVoteStageData(chain, message, ConsensusConstant.VOTE_STAGE_ONE);
        }
        for (VoteMessage message:voteRoundData.getStageTwo().getVoteMessageMap().values()) {
            voteRoundMap.get(voteRound).addVoteStageData(chain, message, ConsensusConstant.VOTE_STAGE_TWO);
        }
    }

    /***
     * 获取当前轮次漏掉未加入队列的投票消息
     * @param existKey    已添加的投票地址
     * @param stage       投票阶段
     */
    public List<VoteMessage> getMissMessage(Set<String> existKey, byte stage){
        return getCurrentRoundData().getMissMessage(existKey, stage);
    }

    /**
     * 获取当前投票轮次指定阶段投票数据
     * @param voteStage  投票阶段
     * */
    public Map<String, VoteMessage> getStageVoteMessage(byte voteStage){
        return getCurrentRoundData().getVoteStageDate(voteStage).getVoteMessageMap();
    }

    /**
     * 获取当前轮次指定投票阶段数据
     * */
    public VoteStageData getCurrentVoteRoundStageData(byte stage){
        return getCurrentRoundData().getVoteStageDate(stage);
    }

    /**
     * 设置指定指定轮次指定阶段投票结果数据
     * */
    public void setVoteResult(Chain chain, byte voteRound, byte voteStage, VoteResultData voteResultData){
        getRoundData(voteRound).getVoteStageDate(voteStage).getVoteResult().complete(voteResultData);
        if(voteStage == ConsensusConstant.VOTE_STAGE_TWO){
            getRoundData(voteRound).setFinished(true);
            if(voteResultData.isResultSuccess()){
               setFinished(true);
            }
        }
        chain.getLogger().debug("设置投票轮次结果，roundIndex:{},packIndex:{},voteRound:{},voteStage:{}",roundIndex,packingIndexOfRound,voteRound,voteStage );
    }

    /**
     * Set the voting block of this round
     * */
    public void setVoteBlock(Chain chain, BlockHeader blockHeader){
        if(blockHeader.getHeight() == height && blockHeader.getExtendsData().getRoundIndex() == roundIndex
                && blockHeader.getExtendsData().getPackingIndexOfRound() == packingIndexOfRound){
            chain.getLogger().debug("收到最新区块，hash:{}，height：{}",blockHeader.getHash().toString(),blockHeader.getHeight());
            //如果当前已经存在投票区块，则表示分叉，分叉则排序去其中之一
            if(firstHeader != null){
                int compareResult = Arrays.compare(blockHeader.getHash().getBytes(), blockHash.getBytes());
                if(compareResult > 0){
                    chain.getLogger().debug("接收到分叉块" );
                    blockHash = blockHeader.getHash();
                    BlockHeader tempHeader = firstHeader;
                    firstHeader = blockHeader;
                    secondHeader = tempHeader;
                }else if(compareResult < 0 ){
                    chain.getLogger().debug("接收到分叉块" );
                    secondHeader = blockHeader;
                }
            }else{
                blockHash = blockHeader.getHash();
                firstHeader = blockHeader;
            }
        }else{
            chain.getLogger().warn("收到不是当前投票轮次的区块，hash:{}",blockHeader.getHash().toString());
        }
    }

    @Override
    public String toString(){
        return "roundIndex:"+roundIndex+",packIndexOfRound:"+packingIndexOfRound+",roundStartTime:"+roundStartTime+",voteRound:" + voteRound +",voteRoundTime:"+getCurrentRoundData().getTime()+ ",voteStage:"+ voteStage +",agentCount:"+ agentCount +",minPassCount:"+minPassCount+",height:"+height+"\n\n";
    }
}
