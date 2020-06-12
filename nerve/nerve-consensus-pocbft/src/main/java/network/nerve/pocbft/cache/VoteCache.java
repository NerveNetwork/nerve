package network.nerve.pocbft.cache;

import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.message.VoteMessage;
import network.nerve.pocbft.message.VoteResultMessage;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.vote.VoteData;
import network.nerve.pocbft.model.bo.vote.VoteResultData;
import network.nerve.pocbft.model.bo.vote.VoteRoundData;
import network.nerve.pocbft.utils.FIFOCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class VoteCache {
    /**
     * 当前确认区块投票信息
     * */
    public static VoteData CURRENT_BLOCK_VOTE_DATA = null;

    /**
     * 上一轮投票是否确认完成，用于控制投票线程的执行
     * */
    public static boolean PRE_ROUND_CONFIRMED = true;

    /**
     * 当前投票轮次是否已完成
     * */
    public static boolean CURRENT_ROUND_FINISH = false;

    /**
     * 接收的当前投票轮次第一阶段消息队列
     * */
    public static final LinkedBlockingQueue<VoteMessage> CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE = new LinkedBlockingQueue<>();

    /**
     * 接收的当前投票轮次第二阶段消息队列
     * */
    public static final LinkedBlockingQueue<VoteMessage> CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE = new LinkedBlockingQueue<>();

    /**
     * 投票结果消息队列
     * */
    public static final LinkedBlockingQueue<VoteResultMessage> VOTE_RESULT_MESSAGE_QUEUE = new LinkedBlockingQueue<>();

    /**
     * 缓存最近N个已确认的最新区块，确认结果
     * key:roundIndex_packingIndexOfRound
     * value:
     *        key:voteRound（区块最终投票结果该值填0）
     *        value:该轮次确认结果
     * */
    public static final FIFOCache<String, Map<Byte, VoteResultData>> CONFIRMED_VOTE_RESULT_MAP = new FIFOCache<>(ConsensusConstant.BLOCK_VOTE_CACHE_COUNT);

    /**
     * 待处理确认数据
     * key:roundIndex_packingIndexOfRound
     * value:投票数据
     * */
    public static final Map<String, VoteData> FUTURE_VOTE_DATA = new ConcurrentHashMap<>();

    /**
     * 投票投票轮次是否已切换
     * 检查未来投票信息时避免向其他节点获取无效投票结果
     * */
    public static boolean VOTE_ROUND_CHANGED = false;

    /**
     * 投票轮次切换标志
     * 切换过程中不处理投票信息
     * */
    public static boolean VOTE_HANDOVER = false;

    /**
     * 投票过程中共识网络是否变更
     * */
    public static boolean CONSENSUS_NET_CHANGE_UNAVAILABLE = false;

    public static synchronized void initCurrentVoteRound(Chain chain, long roundIndex, int packIndex, int agentCount, long height, long roundStartTime){
        if(CURRENT_BLOCK_VOTE_DATA != null){
            return;
        }
        VoteData voteData = new VoteData(roundIndex, packIndex, agentCount, height, roundStartTime);
        VoteRoundData voteRoundData = new VoteRoundData(roundStartTime);
        voteData.getVoteRoundMap().put(ConsensusConstant.VOTE_INIT_ROUND, voteRoundData);
        int byzantineRate = chain.getConfig().getByzantineRate();
        int coverRate = ConsensusConstant.VALUE_OF_ONE_HUNDRED - byzantineRate;
        int byzantineRateCount = agentCount * byzantineRate;
        int minPassCount = byzantineRateCount/ ConsensusConstant.VALUE_OF_ONE_HUNDRED;
        if(byzantineRateCount % ConsensusConstant.VALUE_OF_ONE_HUNDRED > 0){
            minPassCount += 1;
        }
        int coverRateCount = coverRate * agentCount;
        int minCoverCount = coverRate * agentCount / ConsensusConstant.VALUE_OF_ONE_HUNDRED;
        if(coverRateCount % ConsensusConstant.VALUE_OF_ONE_HUNDRED > 0){
            minCoverCount += 1;
        }
        int doubleCoverCount = minCoverCount * 2;
        int minByzantineCount = minPassCount < doubleCoverCount ? minPassCount : doubleCoverCount;
        voteData.setMinByzantineCount(minByzantineCount);
        voteData.setMinPassCount(minPassCount);
        voteData.setMinCoverCount(minCoverCount);
        CURRENT_BLOCK_VOTE_DATA = voteData;
        PRE_ROUND_CONFIRMED = true;
        chain.getLogger().info("本地投票信息初始化完成，{}", voteData.toString());
    }

    public static synchronized void initCurrentVoteRound(Chain chain, int agentCount, VoteMessage voteMessage){
        if(CURRENT_BLOCK_VOTE_DATA != null){
            return;
        }
        VoteData voteData = new VoteData(voteMessage.getRoundIndex(), voteMessage.getPackingIndexOfRound(), agentCount, voteMessage.getHeight(), voteMessage.getRoundStartTime());
        voteData.setVoteRound(voteMessage.getVoteRound());
        voteData.setVoteStage(voteMessage.getVoteStage());
        VoteRoundData voteRoundData = new VoteRoundData(voteMessage.getTime());
        voteData.getVoteRoundMap().put(voteMessage.getVoteRound(), voteRoundData);
        int byzantineRate = chain.getConfig().getByzantineRate();
        int coverRate = ConsensusConstant.VALUE_OF_ONE_HUNDRED - byzantineRate;
        int byzantineRateCount = agentCount * byzantineRate;
        int minPassCount = byzantineRateCount/ ConsensusConstant.VALUE_OF_ONE_HUNDRED;
        if(byzantineRateCount % ConsensusConstant.VALUE_OF_ONE_HUNDRED > 0){
            minPassCount += 1;
        }
        int coverRateCount = coverRate * agentCount;
        int minCoverCount = coverRate * agentCount / ConsensusConstant.VALUE_OF_ONE_HUNDRED;
        if(coverRateCount % ConsensusConstant.VALUE_OF_ONE_HUNDRED > 0){
            minCoverCount += 1;
        }
        int doubleCoverCount = minCoverCount * 2;
        int minByzantineCount = minPassCount < doubleCoverCount ? minPassCount : doubleCoverCount;
        voteData.setMinByzantineCount(minByzantineCount);
        voteData.setMinPassCount(minPassCount);
        voteData.setMinCoverCount(minCoverCount);
        CURRENT_BLOCK_VOTE_DATA = voteData;
        PRE_ROUND_CONFIRMED = true;
        chain.getLogger().info("本地投票信息初始化完成，{}", voteData.toString());
    }

    public static void addVoteResult(Chain chain, String consensusKey, byte voteRound, VoteResultData voteResultData){
        if(voteResultData.getVoteResultItem().getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE){
            return;
        }
        if (!CONFIRMED_VOTE_RESULT_MAP.containsKey(consensusKey)) {
            CONFIRMED_VOTE_RESULT_MAP.put(consensusKey, new HashMap<>(ConsensusConstant.INIT_CAPACITY_2));
        }
        CONFIRMED_VOTE_RESULT_MAP.get(consensusKey).put(voteRound, voteResultData);
        chain.getLogger().info("缓存新的投票结果：{}", voteResultData.getVoteResultItem().toString());
    }

    /**
     * 本地是否已经得到指定投票轮次的投票结果
     * @param consensusKey 共识标志
     * @param voteRound  投票轮次
     * */
    public static boolean hasBeenObtained(String consensusKey, byte voteRound){
        if(!CONFIRMED_VOTE_RESULT_MAP.containsKey(consensusKey)){
            return false;
        }
        Map<Byte, VoteResultData> voteResultDataMap = CONFIRMED_VOTE_RESULT_MAP.get(consensusKey);
        return voteResultDataMap.containsKey(voteRound) || voteResultDataMap.containsKey(ConsensusConstant.FINAL_VOTE_ROUND_SIGN);
    }

    /**
     * 切换当前投票轮次已收到的消息队列
     * @param nextVoteRound           下一轮信息
     * @param time                下一轮时间
     * */
    public static void switchVoteRoundMessage(Chain chain, byte nextVoteRound, long time){
        CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.clear();
        CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.clear();

        //切换投票轮次信息
        CURRENT_BLOCK_VOTE_DATA.setVoteRound(nextVoteRound);
        CURRENT_BLOCK_VOTE_DATA.setVoteStage(ConsensusConstant.VOTE_STAGE_ONE);

        //已添加到消息队列中的投票
        Map<String, VoteMessage> existStageOneMap = null;
        Map<String, VoteMessage> existStageTwoMap = null;
        //判断是否已收到过下一轮的投票信息,如果已存在则判断投票时间是否正确
        VoteRoundData nextVoteRoundData = CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().get(nextVoteRound);
        if(nextVoteRoundData != null){
            if(!CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_ONE).isEmpty()){
                existStageOneMap = new ConcurrentHashMap<>(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_ONE));
                CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.addAll(existStageOneMap.values());
            }
            if(!CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_TWO).isEmpty()){
                existStageTwoMap = new ConcurrentHashMap<>(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_TWO));
                CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.addAll(existStageTwoMap.values());
            }
            //如果投票轮次时间不对，则删除投票轮次信息，但是投票本轮次投票信息任然需要添加到队列中
            if(nextVoteRoundData.getTime() > time){
                chain.getLogger().warn("The time of the next round of voting information currently cached is wrong");
                CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().put(nextVoteRound, new VoteRoundData(time));
                nextVoteRoundData = null;
            }
        }
        if(nextVoteRoundData == null){
            CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().put(nextVoteRound, new VoteRoundData(time));
        }
        //查看切换中途是否收到当前轮次投票信息
        if(existStageOneMap != null && !existStageOneMap.isEmpty()){
            List<VoteMessage> missMessageList = CURRENT_BLOCK_VOTE_DATA.getMissMessage(existStageOneMap.keySet(), ConsensusConstant.VOTE_STAGE_ONE);
            if(missMessageList != null){
                CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.addAll(missMessageList);
            }
        }else{
            if(!CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_ONE).isEmpty()){
                CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.addAll(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_ONE).values());
            }
        }
        if(existStageTwoMap != null && existStageTwoMap.isEmpty()){
            List<VoteMessage> missMessageList = CURRENT_BLOCK_VOTE_DATA.getMissMessage(existStageTwoMap.keySet(), ConsensusConstant.VOTE_STAGE_TWO);
            if(missMessageList != null){
                CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.addAll(missMessageList);
            }
        }else{
            if(!CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_TWO).isEmpty()){
                CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.addAll(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_TWO).values());
            }
        }
        VOTE_ROUND_CHANGED = true;
        chain.getLogger().info("投票轮次切换成功，{}", CURRENT_BLOCK_VOTE_DATA.toString());
    }

    /**
     * 切换当前投票轮次已收到的消息队列
     * @param nextVoteRound           下一轮投票下标
     * @param time                    下一轮时间
     * @param chain                   链信息
     * @param nextVoteData            下一轮投票数据
     * */
    public static void switchVoteDataMessage(Chain chain, byte nextVoteRound, long time, VoteData nextVoteData){
        CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.clear();
        CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.clear();
        //切换投票数据
        CURRENT_BLOCK_VOTE_DATA = nextVoteData;
        if(CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().containsKey(nextVoteRound)){
            CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.addAll(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_ONE).values());
            CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.addAll(CURRENT_BLOCK_VOTE_DATA.getStageVoteMessage(ConsensusConstant.VOTE_STAGE_TWO).values());
        }else{
            VoteRoundData voteRoundData = new VoteRoundData(time);
            CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().put(nextVoteRound, voteRoundData);
        }

        //查看切换中途是否有收到当前区块投票信息
        long currentRoundIndex = CURRENT_BLOCK_VOTE_DATA.getRoundIndex();
        int currentPackIndex = CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound();
        String consensusKey = CURRENT_BLOCK_VOTE_DATA.getConsensusKey();
        VoteData missVoteData = FUTURE_VOTE_DATA.remove(consensusKey);
        if(missVoteData != null){
            for (Map.Entry<Byte, VoteRoundData> entry:missVoteData.getVoteRoundMap().entrySet()) {
                CURRENT_BLOCK_VOTE_DATA.addVoteRoundMessage(chain, entry.getValue(), entry.getKey());
            }
        }
        //删除当前轮次之前的投票结果
        if (FUTURE_VOTE_DATA.size() > 0){
           FUTURE_VOTE_DATA.entrySet().removeIf(entry -> (entry.getValue().getRoundIndex() < currentRoundIndex ||
                   (entry.getValue().getRoundIndex() == currentRoundIndex && entry.getValue().getPackingIndexOfRound() <= currentPackIndex)));
        }
        VOTE_ROUND_CHANGED = true;
        chain.getLogger().info("投票轮次切换成功，{}", CURRENT_BLOCK_VOTE_DATA.toString());
    }

    /**
     * 添加当前轮次之后的投票消息
     * @param chain                 链信息
     * @param voteMessage           投票信息
     * @param nodeId                发送节点
     * */
    public static synchronized void addFutureCache(Chain chain, VoteMessage voteMessage, String nodeId){
        String consensusKey = voteMessage.getConsensusKey();
        VoteData futureVoteData = FUTURE_VOTE_DATA.get(consensusKey);
        if(futureVoteData == null){
            futureVoteData = new VoteData(voteMessage.getRoundIndex(),voteMessage.getPackingIndexOfRound(),CURRENT_BLOCK_VOTE_DATA.getAgentCount(),voteMessage.getHeight(),voteMessage.getRoundStartTime());
            FUTURE_VOTE_DATA.put(consensusKey, futureVoteData);
            chain.getLogger().info("初始化未来区块投票数据，consensusKey:{}",consensusKey);
        }
        futureVoteData.addVoteMessage(chain, voteMessage, nodeId);
    }

    /**
     * 清空当前投票轮次信息（当本节点由共识节点转非共识节点时）
     * */
    public static void clearVote(Chain chain){
        CURRENT_BLOCK_VOTE_DATA = null;
        CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.clear();
        CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.clear();
        FUTURE_VOTE_DATA.clear();
        VOTE_RESULT_MESSAGE_QUEUE.clear();
    }
}
