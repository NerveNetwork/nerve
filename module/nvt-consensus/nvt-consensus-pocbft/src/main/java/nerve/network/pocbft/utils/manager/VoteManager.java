package nerve.network.pocbft.utils.manager;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import nerve.network.pocbft.cache.VoteCache;
import nerve.network.pocbft.constant.CommandConstant;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.message.GetVoteResultMessage;
import nerve.network.pocbft.message.VoteMessage;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.round.MeetingRound;
import nerve.network.pocbft.model.bo.vote.*;
import nerve.network.pocbft.rpc.call.CallMethodUtils;
import nerve.network.pocbft.rpc.call.NetWorkCall;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static nerve.network.pocbft.cache.VoteCache.*;

/**
 * 投票信息管理类
 * Voting information management
 *
 * @author: Jason
 * 2019/10/28
 */
@Component
public class VoteManager {
    private static Lock switchVoteInfoLock = new ReentrantLock();
    @Autowired
    private static RoundManager roundManager;

    /**
     * 统计投票结果，接收到当前阶段投票信息时
     */
    public static boolean statisticalResult(Chain chain, VoteMessage message, byte voteStage) throws Exception {
        chain.getLogger().debug("投票信息，hash:{},address:{},roundIndex:{},packIndexOfRound:{},voteRound:{},voteStage:{},blockHash:{},height:{}",message.getVoteHash().toHex(),message.getAddress(chain), message.getRoundIndex(), message.getPackingIndexOfRound(), message.getVoteRound(), message.getVoteStage(), message.getBlockHash(),message.getHeight());
        //验证投票是否为共识节点投票
        MeetingRound meetingRound = roundManager.getRound(chain, CURRENT_BLOCK_VOTE_DATA.getRoundIndex(), CURRENT_BLOCK_VOTE_DATA.getRoundStartTime());
        if (!meetingRound.getMemberAddressList().contains(message.getAddress(chain))) {
            chain.getLogger().error("Current voting is not consensus node voting");
            return false;
        }
        //如果不为当前轮次投票信息则直接 返回
        if (message.getRoundIndex() != CURRENT_BLOCK_VOTE_DATA.getRoundIndex() || message.getPackingIndexOfRound() != CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound()
                || message.getVoteStage() < CURRENT_BLOCK_VOTE_DATA.getVoteStage() || message.getHeight() != message.getHeight()) {
            chain.getLogger().error("Voting information error");
            return false;
        }
        //如果当前区块已经确认完成或则当前轮次确认完成则直接返回
        if (CURRENT_BLOCK_VOTE_DATA.isFinished() || CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().isFinished()) {
            chain.getLogger().warn("The current round of voting has been confirmed to be completed,roundIndex:{},packIndexOfRound:{},voteRound:{},voteStage:{}", CURRENT_BLOCK_VOTE_DATA.getRoundIndex(), CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound(), CURRENT_BLOCK_VOTE_DATA.getVoteRound(), CURRENT_BLOCK_VOTE_DATA.getVoteStage());
            return true;
        }
        //处理当前投票轮次投票时间不同的投票
        long currentVoteRoundTime = CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getTime();
        if(message.getTime() != currentVoteRoundTime){
            handleVoteBifurcate(chain, message, currentVoteRoundTime);
            return false;
        }
        VoteStageData stageData = CURRENT_BLOCK_VOTE_DATA.getCurrentVoteRoundStageData(voteStage);
        if(stageData.isFinished()){
            chain.getLogger().info("The result of the voting at this stage has been obtained" );
            return true;
        }
        NulsHash voteHash = message.getVoteHash();
        if (!stageData.getItemMap().containsKey(voteHash)) {
            VoteResultItem voteResultItem = new VoteResultItem(message);
            List<byte[]> signList = new ArrayList<>();
            signList.add(message.getSign());
            stageData.getItemVoteCountMap().put(voteHash, 1);
            voteResultItem.setSignatureList(signList);
            stageData.getItemMap().put(voteHash, voteResultItem);
        } else {
            stageData.getItemMap().get(voteHash).getSignatureList().add(message.getSign());
            int voteCount = stageData.getItemVoteCountMap().get(voteHash);
            voteCount++;
            stageData.getItemVoteCountMap().put(voteHash, voteCount);
        }
        int voteTotalCount = 0;
        int maxRateCount = 0;
        for (Map.Entry<NulsHash, Integer> entry : stageData.getItemVoteCountMap().entrySet()) {
            int value = entry.getValue();
            if (maxRateCount < value) {
                maxRateCount = value;
            }
            voteTotalCount += value;
        }
        //如果收集到的签名数量小于最小拜占庭验证数则直接返回
        if (voteTotalCount < CURRENT_BLOCK_VOTE_DATA.getMinByzantineCount()) {
            return true;
        }
        VoteResultData voteResultData = null;
        if (maxRateCount >= CURRENT_BLOCK_VOTE_DATA.getMinPassCount()) {
            voteResultData = new VoteResultData();
            VoteResultItem item = stageData.getItemMap().get(voteHash);
            voteResultData.getVoteResultItemList().add(item);
            voteResultData.setConfirmedEmpty(item.isConfirmedEmpty());
            voteResultData.setResultSuccess(true);
            chain.getLogger().info("本轮次本阶段投票得到结果，voteStage:{},isConfirmedEmpty:{},totalVoteCount:{},minByzantineCount:{},maxRateCount:{}", voteStage, item.isConfirmedEmpty(),voteTotalCount, CURRENT_BLOCK_VOTE_DATA.getMinByzantineCount(),maxRateCount);
        } else {
            int otherCount = voteTotalCount - maxRateCount;
            if (maxRateCount >= CURRENT_BLOCK_VOTE_DATA.getMinCoverCount() && otherCount >= CURRENT_BLOCK_VOTE_DATA.getMinCoverCount()) {
                voteResultData = new VoteResultData();
                voteResultData.setResultSuccess(false);
                voteResultData.getVoteResultItemList().addAll(stageData.getItemMap().values());
                chain.getLogger().info("本轮次本阶段投票收集失败，voteStage:{},totalVoteCount:{},minByzantineCount:{},maxRateCount:{},otherCount:{}", voteStage,voteTotalCount, CURRENT_BLOCK_VOTE_DATA.getMinByzantineCount(),maxRateCount,otherCount);
            }
        }
        if (voteResultData != null) {
            handleVoteResult(chain, voteResultData);
            stageData.setFinished(true);
        }
        return true;
    }

    /**
     * 收到与当前投票轮次投票但是轮次时间不一致的投票处理逻辑
     * 只会在当前节点收集上一轮投票结果超时，有其他共识节点收集上一轮投票结果失败的情况出现
     *
     * @param chain           链信息
     * @param message         投票信息
     * @param localVoteTime   本地轮次投票时间
     * */
    private static void handleVoteBifurcate(Chain chain, VoteMessage message, long localVoteTime){
        //无效投票直接返回
        if(message.getVoteRound() <= 1 || message.getTime() >= localVoteTime){
            return;
        }
        byte preRound = (byte)(message.getVoteRound() - 1);
        Map<Byte, VoteResultData> currentBlockVoteResult =  CONFIRMED_VOTE_RESULT_MAP.get(message.getConsensusKey());
        //如果本地正常收集到了上一轮投票结果，则该投票为无效数据
        if(currentBlockVoteResult != null && (currentBlockVoteResult.containsKey(preRound) || currentBlockVoteResult.containsKey(ConsensusConstant.FINAL_VOTE_ROUND_SIGN))){
            return;
        }
        byte localFinalResultRound;
        if(currentBlockVoteResult == null){
            localFinalResultRound = 0;
        }else{
            localFinalResultRound = currentBlockVoteResult.keySet().stream().max(Comparator.comparing(Byte::valueOf)).get();
        }
        //向投票节点获取本地最后收集到的投票结果的下一轮投票结果
        chain.getLogger().debug("收到其他节点广播的本轮次不同时间的投票，nodeId:{},voteRound:{}", message.getSendNode(), localFinalResultRound);
        GetVoteResultMessage getVoteResultMessage = new GetVoteResultMessage(message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(), (byte)(localFinalResultRound + 1));
        NetWorkCall.sendToNode(chain.getChainId(), getVoteResultMessage, message.getSendNode(), CommandConstant.MESSAGE_GET_VOTE_RESULT);
    }

    /**
     * 投票结果验证
     * Verification of voting results
     * 1.投票签名正确性验证
     * 2.投票签名拜占庭验证
     *
     * @param chain          链信息
     * @param voteResultData 投票结果数据
     * @param round          轮次信息
     **/
    public static boolean verifyVoteResult(Chain chain, VoteResultData voteResultData, MeetingRound round) {
        //是否为正常确认区块
        boolean isConfirmBlock = voteResultData.getVoteResultItemList().size() == 1;
        //该投票结果是否为当前区块投票结果
        VoteResultItem voteResultBasicInfo = voteResultData.getVoteResultItem();
        int agentCount = round.getMemberCount();
        int byzantineRate = chain.getConfig().getByzantineRate();
        int minPassCount = agentCount * byzantineRate / ConsensusConstant.VALUE_OF_ONE_HUNDRED + 1;
        if (isConfirmBlock) {
            int voteCount = voteResultBasicInfo.getSignatureList().size();
            if (voteCount < minPassCount) {
                chain.getLogger().error("Block winning rate is less than the minimum passing rate");
                return false;
            }
            voteResultData.setResultSuccess(true);
            voteResultData.setConfirmedEmpty(voteResultBasicInfo.isConfirmedEmpty());
        } else {
            int coverRate = ConsensusConstant.VALUE_OF_ONE_HUNDRED - byzantineRate;
            int minCoverCount = coverRate * agentCount / ConsensusConstant.VALUE_OF_ONE_HUNDRED + 1;
            int doubleCoverCount = minCoverCount * 2;
            int minByzantineCount = minPassCount < doubleCoverCount ? minPassCount : doubleCoverCount;

            int voteTotalCount = 0;
            int maxItemCount = 0;
            for (VoteResultItem item : voteResultData.getVoteResultItemList()) {
                int itemCount = item.getSignatureList().size();
                if (itemCount > maxItemCount) {
                    maxItemCount = itemCount;
                }
                voteTotalCount += itemCount;
            }
            int otherCount = voteTotalCount - maxItemCount;
            if (maxItemCount < minByzantineCount || otherCount < minByzantineCount) {
                chain.getLogger().error("Byzantine verification error of voting result data");
                return false;
            }
            voteResultData.setResultSuccess(false);
        }
        if (!verifySignature(chain, voteResultData, round)) {
            return false;
        }
        //如果本节点为共识节点则需要切换投票轮次
        if(CURRENT_BLOCK_VOTE_DATA != null && round.getMyMember() != null){
            chain.getLogger().debug("本节点为共识节点处理投票结果！" );
            handleVoteResult(chain, voteResultData);
        }else{
            chain.getLogger().debug("本节点不为共识节点，缓存投票结果，通知区块模块拜占庭完成！" );
            addVoteResult(chain, voteResultBasicInfo.getConsensusKey(), ConsensusConstant.FINAL_VOTE_ROUND_SIGN, voteResultData);
            noticeByzantineResult(chain, voteResultBasicInfo);
        }
        return true;
    }

    /**
     * 投票结果签名验证
     * Voting result signature verification
     *
     * @param chain          链信息
     * @param voteResultData 投票结果数据
     * @param round          轮次信息
     */
    private static boolean verifySignature(Chain chain, VoteResultData voteResultData, MeetingRound round) {
        Set<String> memberAddressList = round.getMemberAddressList();
        for (VoteResultItem item : voteResultData.getVoteResultItemList()) {
            NulsHash voteHash;
            try {
                voteHash = NulsHash.calcHash(item.serializeForDigest());
            } catch (IOException e) {
                chain.getLogger().error(e);
                return false;
            }
            for (byte[] sign : item.getSignatureList()) {
                BlockSignature signature = new BlockSignature();
                try {
                    signature.parse(sign, 0);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                    return false;
                }
                String address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chain.getChainId()));
                if (!memberAddressList.contains(address)) {
                    chain.getLogger().error("Not a consensus node signature");
                    return false;
                }
                if (signature.verifySignature(voteHash).isFailed()) {
                    chain.getLogger().error("Voting signature verification failed");
                    return false;
                }

            }
        }
        return true;
    }

    /****
     * 停止当前投票（共识状态变更时）
     * @param chain          链信息
     */
    public static void stopVote(Chain chain){
        try {
            switchVoteInfoLock.lock();
            if(CURRENT_BLOCK_VOTE_DATA == null){
                return;
            }
            chain.getLogger().info("共识变更，停止投票");
            try {
                while (!PRE_ROUND_CONFIRMED  && !CURRENT_ROUND_FINISH){
                    CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageTwo().getVoteResult().complete(new VoteResultData());
                    chain.getLogger().debug("等待当前投票轮次结束" );
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            }catch (InterruptedException e){
                chain.getLogger().error(e);
            }
            VoteCache.clearVote(chain);
            PRE_ROUND_CONFIRMED = true;
        } finally {
            switchVoteInfoLock.unlock();
        }
    }


    /**
     * 设置指定阶段投票结果（为啥同步？避免在切换投票轮次的同时有另外的线程来设置投票结果）
     *
     * @param chain          链信息
     * @param voteResultData 投票结果
     */
    private static synchronized void handleVoteResult(Chain chain, VoteResultData voteResultData) {
        byte resultTime = resultTime(voteResultData);
        //如果为之前投票结果，直接忽略
        if(resultTime == ConsensusConstant.PREVIOUS_BLOCK){
            chain.getLogger().warn("The voting result is the result of previous block");
            return;
        }
        if (resultTime == ConsensusConstant.PREVIOUS_ROUND) {
            chain.getLogger().warn("The voting result is the result of previous rounds");
            handlePreRoundResult(chain, voteResultData);
        } else if (resultTime == ConsensusConstant.CURRENT_ROUND) {
            chain.getLogger().debug("处理当前区块，当前投票轮次的投票结果" );
            handleCurrentRoundResult(chain, voteResultData);
        } else if (resultTime == ConsensusConstant.CURRENT_BLOCK) {
            chain.getLogger().debug("处理当前区块，当前投票轮次之后的投票结果" );
            handleCurrentBlockResult(chain, voteResultData);
        } else {
            //如果收到未来区块的投票结果则该区块一定是被正常确认
            if (voteResultData.getVoteResultItemList().size() != 1 && voteResultData.getVoteResultItem().isConfirmedEmpty()) {
                chain.getLogger().warn("Voting result data exception");
                return;
            }
            VoteResultItem voteResultItem = voteResultData.getVoteResultItem();
            chain.getLogger().debug("处理当前区块之后的区块的投票结果，当前投票信息：{}, 投票结果信息:{}",CURRENT_BLOCK_VOTE_DATA.toString(),voteResultItem.toString());
            //缓存投票结果
            String resultConsensusKey = voteResultItem.getConsensusKey();
            addVoteResult(chain, resultConsensusKey, ConsensusConstant.FINAL_VOTE_ROUND_SIGN, voteResultData);
            //通知区块模块区块拜占庭验证完成，等待区块保存，切换voteData
            noticeByzantineResult(chain, voteResultItem);
        }
    }


    /**
     * 处理当前区块之前投票轮次结果
     *
     * @param chain          链信息
     * @param voteResultData 投票结果数据
     */
    private static void handlePreRoundResult(Chain chain, VoteResultData voteResultData) {
        if (CURRENT_BLOCK_VOTE_DATA.isFinished() || CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().isFinished()) {
            chain.getLogger().warn("End of current voting round");
            return;
        }
        VoteResultItem voteInfo = voteResultData.getVoteResultItem();
        byte voteRound = voteInfo.getVoteRound();
        byte voteStage = voteInfo.getVoteStage();
        //如果为第一阶段投票，则直接返回投票结果
        if (voteStage == ConsensusConstant.VOTE_STAGE_ONE) {
            chain.getLogger().debug("收到之前投票轮次第一阶段投票结果不处理" );
            return;
        }
        /*
         * 1.确认空块|当前投票区块高度小于当前最新区块高度，则直接切换voteData
         * 2.确认正常块/分叉，则通知区块模块，区块拜占庭验证成功，等待区块保存的时候切换voteData
         * 3.确认失败，切换投票轮次进入下一轮投票
         * */
        if (voteResultData.isResultSuccess()) {
            addVoteResult(chain, voteInfo.getConsensusKey(), ConsensusConstant.FINAL_VOTE_ROUND_SIGN, voteResultData);
            if (voteResultData.isConfirmedEmpty() || voteInfo.getHeight() <= chain.getNewestHeader().getHeight()) {
                chain.getLogger().debug("之前轮次投票确认空块，切换投票信息" );
                switchVoteData(chain, voteResultData, true);
            } else {
                chain.getLogger().debug("之前轮次投票确认正常块，通知区块模块区块拜占庭完成" );
                noticeByzantineResult(chain, voteResultData.getVoteResultItem());
            }
        } else {
            chain.getLogger().debug("当前轮次投票确认失败，切换投票轮次" );
            addVoteResult(chain, voteInfo.getConsensusKey(), voteRound, voteResultData);
            switchVoteRound(chain, voteResultData);
        }
    }

    /**
     * 处理当前投票轮次结果消息
     *
     * @param chain          链信息
     * @param voteResultData 投票结果数据
     */
    private static void handleCurrentRoundResult(Chain chain, VoteResultData voteResultData) {
        if (CURRENT_BLOCK_VOTE_DATA.isFinished() || CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().isFinished()) {
            chain.getLogger().warn("End of current voting round");
            return;
        }
        VoteResultItem voteInfo = voteResultData.getVoteResultItem();
        long roundIndex = voteInfo.getRoundIndex();
        int packIndex = voteInfo.getPackingIndexOfRound();
        byte voteRound = voteInfo.getVoteRound();
        byte voteStage = voteInfo.getVoteStage();
        //如果为第一阶段投票，则直接返回投票结果
        if (voteStage == ConsensusConstant.VOTE_STAGE_ONE) {
            chain.getLogger().debug("当前轮次第一阶段投票结果处理完成！" );
            completeCurrentResult(chain, roundIndex, packIndex, voteRound, voteStage, voteResultData);
            return;
        }
        /*
         * 1.确认空块|当前投票区块高度小于当前最新区块高度，则直接切换voteData
         * 2.确认正常块/分叉，则通知区块模块，区块拜占庭验证成功，等待区块保存的时候切换voteData
         * 3.确认失败，切换投票轮次进入下一轮投票
         * */
        if (voteResultData.isResultSuccess()) {
            addVoteResult(chain, voteInfo.getConsensusKey(), ConsensusConstant.FINAL_VOTE_ROUND_SIGN, voteResultData);
            if (voteResultData.isConfirmedEmpty() || voteInfo.getHeight() <= chain.getNewestHeader().getHeight()) {
                chain.getLogger().debug("当前轮次投票确认空块，切换投票信息" );
                switchVoteData(chain, voteResultData, true);
            } else {
                chain.getLogger().debug("当前轮次投票确认正常块，通知区块模块区块拜占庭完成" );
                noticeByzantineResult(chain, voteResultData.getVoteResultItem());
            }
        } else {
            chain.getLogger().debug("当前轮次投票确认失败，切换投票轮次" );
            addVoteResult(chain, voteInfo.getConsensusKey(), voteRound, voteResultData);
            switchVoteRound(chain, voteResultData);
        }
    }

    /**
     * 处理当前投票轮次结果消息
     *
     * @param chain          链信息
     * @param voteResultData 投票结果数据
     */
    private static void handleCurrentBlockResult(Chain chain, VoteResultData voteResultData) {
        CURRENT_BLOCK_VOTE_DATA.setVoteResult(chain, voteResultData.getVoteResultItem().getVoteRound(), ConsensusConstant.VOTE_STAGE_TWO, voteResultData);
        VoteResultItem voteInfo = voteResultData.getVoteResultItem();
        byte voteRound = voteInfo.getVoteRound();
        byte voteStage = voteInfo.getVoteStage();
        if(voteStage != ConsensusConstant.VOTE_STAGE_TWO){
            chain.getLogger().warn("Invalid voting result data");
            return;
        }
        if (voteResultData.isResultSuccess()) {
            addVoteResult(chain, voteInfo.getConsensusKey(), ConsensusConstant.FINAL_VOTE_ROUND_SIGN, voteResultData);
            if (voteResultData.isConfirmedEmpty() || voteInfo.getHeight() <= chain.getNewestHeader().getHeight()) {
                switchVoteData(chain, voteResultData, true);
            } else {
                noticeByzantineResult(chain, voteResultData.getVoteResultItem());
            }
        } else {
            addVoteResult(chain, voteInfo.getConsensusKey(), voteRound, voteResultData);
            switchVoteRound(chain, voteResultData);
        }
    }


    /**
     * 区块被正常确认后保存时切换当前确认区块
     *
     * @param chain       链信息
     * @param blockHeader 区块头
     */
    public static void switchBlockVoteData(Chain chain, BlockHeader blockHeader) {
        try {
            switchVoteInfoLock.lock();
            VOTE_HANDOVER = true;
            BlockExtendsData blockExtendsData = blockHeader.getExtendsData();
            long roundIndex = blockExtendsData.getRoundIndex();
            int packIndex = blockExtendsData.getPackingIndexOfRound();
            boolean roundSwitched = roundIndex < CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                    ||  roundIndex== CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && packIndex < CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound();
            if(roundSwitched){
                chain.getLogger().warn("Rounds switched");
                return;
            }
            //保存区块之前，起投票结果一定会先被缓存好了的
            String consensusKey = roundIndex + ConsensusConstant.SEPARATOR + packIndex;
            VoteResultData voteResultData = CONFIRMED_VOTE_RESULT_MAP.get(consensusKey).get(ConsensusConstant.FINAL_VOTE_ROUND_SIGN);
            //设置当前轮次投票结果
            CURRENT_BLOCK_VOTE_DATA.setVoteResult(chain, CURRENT_BLOCK_VOTE_DATA.getVoteRound(), ConsensusConstant.VOTE_STAGE_TWO, voteResultData);
            VoteResultItem voteResultItem = voteResultData.getVoteResultItem();
            int agentCount = blockExtendsData.getConsensusMemberCount();
            int nextPackIndex = packIndex + 1;
            long nextRoundIndex = roundIndex;
            long nextHeight = blockHeader.getHeight() + 1;
            //下一个区块投票开始时间是本区块最后一轮投票时间 + 投票轮次间隔时间
            long nextVoteTime = voteResultItem.getTime() + ConsensusConstant.VOTE_ROUND_INTERVAL_TIME;
            long roundStartTime = voteResultItem.getTime();
            boolean changeRound = false;
            MeetingRound round = null;
            if (packIndex == agentCount) {
                //判断是否有该区块轮次信息，如果没有则生成
                try {
                    nextRoundIndex += 1;
                    round = roundManager.getRound(chain, nextRoundIndex, nextVoteTime);
                    nextPackIndex = ConsensusConstant.INIT_PACING_INDEX;
                    agentCount = round.getMemberCount();
                    roundStartTime = nextVoteTime;
                    changeRound = true;
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                    return;
                }
            }
            try {
                //如果在投票过程中则需等待当前投票轮次投票完成
                while (chain.isNetworkState() && !PRE_ROUND_CONFIRMED && !CURRENT_ROUND_FINISH && chain.isPacker()){
                    chain.getLogger().debug("等待当前投票轮次结束,NetworkState:{},PRE_ROUND_CONFIRMED:{},CURRENT_ROUND_FINISH:{}",chain.isNetworkState(), PRE_ROUND_CONFIRMED, CURRENT_ROUND_FINISH);
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            }catch (InterruptedException e){
                chain.getLogger().error(e);
            }
            //切换投票轮次
            switchVoteData(chain, nextRoundIndex, nextPackIndex, nextVoteTime, agentCount, nextHeight,roundStartTime);
            if (changeRound) {
                roundManager.addRound(chain, round);
                if(round.getMyMember() == null){
                    VoteCache.clearVote(chain);
                }
            }
            PRE_ROUND_CONFIRMED = true;
            chain.getLogger().debug("------------------区块投票信息切换完成-----------------" );
        } finally {
            VOTE_HANDOVER = false;
            switchVoteInfoLock.unlock();
        }
    }

    /**
     * 切换当前确认区块，当前区块被确认为空块或当前投票高度小于本地最新区块高度时调用
     *
     * @param chain             链信息
     * @param voteResultData    投票结果信息
     * @param setResult         是否保存投票结果，是否中断当前投票
     */
    public static void switchVoteData(Chain chain, VoteResultData voteResultData, boolean setResult) {
        try {
            switchVoteInfoLock.lock();
            VOTE_HANDOVER = true;
            VoteResultItem voteInfo = voteResultData.getVoteResultItem();
            long roundIndex = voteInfo.getRoundIndex();
            int packIndex = voteInfo.getPackingIndexOfRound();
            byte voteRound = voteInfo.getVoteRound();

            boolean isValidData = roundIndex == CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                    && packIndex == CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound() &&
                    (voteRound > CURRENT_BLOCK_VOTE_DATA.getVoteRound()
                            || (CONFIRMED_VOTE_RESULT_MAP.get(voteInfo.getConsensusKey()) == null
                            || CONFIRMED_VOTE_RESULT_MAP.get(voteInfo.getConsensusKey()).get(voteInfo.getVoteRound()) == null));

            if(!isValidData){
                chain.getLogger().warn("Voting rounds switched");
                return;
            }

            //设置投票结果，并终止当前投票
            if(setResult){
                CURRENT_BLOCK_VOTE_DATA.setVoteResult(chain, voteRound, ConsensusConstant.VOTE_STAGE_TWO, voteResultData);
                if(voteRound != CURRENT_BLOCK_VOTE_DATA.getVoteRound()){
                    chain.getLogger().debug("如果不是当前投票轮次投票结果，则终止当前投票轮次" );
                    if(CURRENT_BLOCK_VOTE_DATA.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE){
                        CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageOne().getVoteResult().complete(new VoteResultData());
                    }
                    CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageTwo().getVoteResult().complete(new VoteResultData());
                }
            }

            //切换当前投票数据
            long nextRoundIndex = roundIndex;
            int nextPackIndex = packIndex + 1;
            int agentCount = CURRENT_BLOCK_VOTE_DATA.getAgentCount();
            long nextHeight = CURRENT_BLOCK_VOTE_DATA.getHeight();
            boolean changeRound = false;
            MeetingRound newRound = null;
            long nextVoteTime = voteInfo.getTime() + ConsensusConstant.VOTE_ROUND_INTERVAL_TIME;
            long roundStartTime = voteInfo.getTime();
            if (packIndex == agentCount) {
                //切换共识轮次信息，共识轮次开始时间为当前投票轮次时间+2S,修改出块节点数agentCount
                try {
                    newRound = roundManager.getRound(chain, nextRoundIndex + 1, nextVoteTime);
                    agentCount = newRound.getMemberCount();
                    nextRoundIndex = newRound.getIndex();
                    nextPackIndex = ConsensusConstant.INIT_PACING_INDEX;
                    roundStartTime = nextVoteTime;
                    changeRound = true;
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                    return;
                }
            }
            try {
                while (chain.isNetworkState() && !PRE_ROUND_CONFIRMED  && !CURRENT_ROUND_FINISH){
                    chain.getLogger().debug("等待当前投票轮次结束" );
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            }catch (InterruptedException e){
                chain.getLogger().error(e);
            }
            switchVoteData(chain, nextRoundIndex, nextPackIndex, nextVoteTime, agentCount, nextHeight, roundStartTime);
            if (changeRound) {
                roundManager.addRound(chain, newRound);
                if(newRound.getMyMember() == null){
                    VoteCache.clearVote(chain);
                }
            }
            PRE_ROUND_CONFIRMED = true;
            chain.getLogger().debug("------------------投票信息切换完成-----------------" );
        } finally {
            VOTE_HANDOVER = false;
            switchVoteInfoLock.unlock();
        }
    }

    /**
     * 切换voteData
     *
     * @param chain          链信息
     * @param nextRoundIndex 下一个确认区块所在轮次
     * @param nextPackIndex  下一个确认区块出块下标
     * @param nextVoteTime   下一个区块投票开始时间
     * @param agentCount     下一个区块所在轮次共识节点数
     * @param nextHeight     下一个区块高度
     * @param roundStartTime 轮次开始时间
     */
    private static void switchVoteData(Chain chain, long nextRoundIndex, int nextPackIndex, long nextVoteTime, int agentCount, long nextHeight, long roundStartTime) {
        String voteKey = nextRoundIndex + ConsensusConstant.SEPARATOR + nextPackIndex;
        VoteData nextVoteData = null;
        //如果收到过下一个区块的投票信息，则直接从缓存中获取下一个区块的投票信息，并修改节点数量，否则新建下一个区块的投票信息对象
        if (FUTURE_VOTE_DATA.containsKey(voteKey)) {
            nextVoteData = FUTURE_VOTE_DATA.remove(voteKey);
            //如果缓存中投票高度与本地计算的高度不一致，则直接删除缓存中数据
            if (nextVoteData.getHeight() != nextHeight) {
                chain.getLogger().warn("Data exception in cache,nextHeight:{},cacheNextHeight:{}"
                ,nextHeight,nextVoteData.getHeight());
                nextVoteData = null;
            } else {
                //设置投票相关信息
                nextVoteData.setAgentCount(agentCount);
            }
        }
        if (nextVoteData == null) {
            nextVoteData = new VoteData(nextRoundIndex, nextPackIndex, agentCount, nextHeight, roundStartTime);
        }
        if (agentCount != CURRENT_BLOCK_VOTE_DATA.getAgentCount()) {
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
            nextVoteData.setMinPassCount(minPassCount);
            nextVoteData.setMinByzantineCount(minByzantineCount);
            nextVoteData.setMinCoverCount(minCoverCount);
        } else {
            nextVoteData.setMinByzantineCount(CURRENT_BLOCK_VOTE_DATA.getMinByzantineCount());
            nextVoteData.setMinPassCount(CURRENT_BLOCK_VOTE_DATA.getMinPassCount());
            nextVoteData.setMinCoverCount(CURRENT_BLOCK_VOTE_DATA.getMinCoverCount());
        }
        //切换确认区块
        switchVoteDataMessage(chain, ConsensusConstant.VOTE_INIT_ROUND, nextVoteTime, nextVoteData);
    }

    /**
     * 本轮收集投票超时切换投票轮次
     * @param chain            链信息
     * @param roundIndex       当前轮次
     * @param packIndex        打包下标
     * @param voteRound        当前投票轮次
     * @param voteTime         投票时间
     * @param voteResultData   投票结果
     * */
    public static void switchVoteRound(Chain chain, long roundIndex, int packIndex, byte voteRound, long voteTime, VoteResultData voteResultData){
        try {
            switchVoteInfoLock.lock();
            VOTE_HANDOVER = true;
            /*
              1.必须是同一确认区块的不同轮次
              2.切换轮次必须小于等于当前轮次
              3.当前轮次直接切换
              4.小于当前轮次，则投票结果不能为空，本没有该轮次的投票结果
             */
            boolean isValidData = roundIndex == CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && packIndex == CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound();
            if(!isValidData){
                chain.getLogger().warn("Voting rounds switched");
                return;
            }
            String consensusKey = roundIndex + ConsensusConstant.SEPARATOR + packIndex;
            byte localVoteRound = CURRENT_BLOCK_VOTE_DATA.getVoteRound();
            if(voteRound > localVoteRound){
                chain.getLogger().warn("Cannot switch future rounds");
                return;
            }else if(voteRound < localVoteRound){
                if(voteResultData == null){
                    chain.getLogger().warn("The voting result cannot be empty when switching to previous rounds");
                    return;
                }
                if(CONFIRMED_VOTE_RESULT_MAP.get(consensusKey) != null && CONFIRMED_VOTE_RESULT_MAP.get(consensusKey).get(voteRound) != null){
                    chain.getLogger().warn("Normal confirmation block of current node in this voting round");
                    return;
                }
            }
            //设置投票结果，并终止当前投票
            if(voteResultData != null){
                CURRENT_BLOCK_VOTE_DATA.setVoteResult(chain, voteRound, ConsensusConstant.VOTE_STAGE_TWO, voteResultData);
                if(voteRound != CURRENT_BLOCK_VOTE_DATA.getVoteRound()){
                    CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageTwo().getVoteResult().complete(new VoteResultData());
                }
            }
            byte nextVoteRound = (byte) (voteRound + 1);
            long nextVoteTime = voteTime + ConsensusConstant.VOTE_ROUND_INTERVAL_TIME;
            try {
                while (chain.isNetworkState() && !PRE_ROUND_CONFIRMED && !CURRENT_ROUND_FINISH){
                    chain.getLogger().debug("等待当前投票轮次结束" );
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            }catch (InterruptedException e){
                chain.getLogger().error(e);
            }
            //切换当前投票消息队列数据
            VoteCache.switchVoteRoundMessage(chain, nextVoteRound, nextVoteTime);
            PRE_ROUND_CONFIRMED = true;
            chain.getLogger().debug("------------------投票轮次投票信息切换完成-----------------" );
        }finally {
            VOTE_HANDOVER = false;
            switchVoteInfoLock.unlock();
        }
    }

    /**
     * 切换当前区块投票轮次，本轮区块确认失败需进入下一轮投票
     * Switch the current block voting round. If the block confirmation fails in this round, you need to enter the next round of voting
     *
     * @param chain               链信息
     * @param voteResultData      投票结果
     */
    private static void switchVoteRound(Chain chain, VoteResultData voteResultData) {
        try {
            switchVoteInfoLock.lock();
            VoteResultItem voteInfo = voteResultData.getVoteResultItem();
            //当前投票轮次被其他轮次投票结果中断时，当前投票轮次投票结果保存的空
            if(voteInfo == null){
                return;
            }
            long roundIndex = voteInfo.getRoundIndex();
            int packIndex = voteInfo.getPackingIndexOfRound();
            byte voteRound = voteInfo.getVoteRound();
            switchVoteRound(chain, roundIndex, packIndex, voteRound, voteInfo.getTime() , voteResultData);
        } finally {
            switchVoteInfoLock.unlock();
        }
    }

    /**
     * 判断投票结果数据时间点
     * @param voteResultData 投票结果数据
     */
    private static byte resultTime(VoteResultData voteResultData) {
        VoteResultItem voteInfo = voteResultData.getVoteResultItem();
        boolean isPreviousBlock = voteInfo.getRoundIndex() < CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                || (voteInfo.getRoundIndex() == CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && voteInfo.getPackingIndexOfRound() < CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound());
        if (isPreviousBlock) {
            return ConsensusConstant.PREVIOUS_BLOCK;
        }
        if(voteInfo.getRoundIndex() == CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && voteInfo.getPackingIndexOfRound()  == CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound() && voteInfo.getVoteRound() < CURRENT_BLOCK_VOTE_DATA.getVoteRound()){
            return ConsensusConstant.PREVIOUS_ROUND;
        }
        boolean isCurrentBlock = voteInfo.getRoundIndex() == CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && voteInfo.getPackingIndexOfRound()  == CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound();
        boolean isCurrentRound = isCurrentBlock && voteInfo.getVoteRound()  == CURRENT_BLOCK_VOTE_DATA.getVoteRound();
        if (isCurrentRound) {
            return ConsensusConstant.CURRENT_ROUND;
        }
        if (isCurrentBlock) {
            return ConsensusConstant.CURRENT_BLOCK;
        }
        return ConsensusConstant.FUTURE;
    }

    /**
     * 通知区块模块区块拜占庭完成
     *
     * @param voteResultItem 拜占庭结果信息
     */
    public static void noticeByzantineResult(Chain chain, VoteResultItem voteResultItem) {
        chain.getLogger().info("区块拜占庭完成，通知区块模块,blockHeight:{}",voteResultItem.getHeight());
        boolean bifurcate = voteResultItem.getFirstHeader() != null && voteResultItem.getSecondHeader() != null;
        NulsHash firstHash = bifurcate ? voteResultItem.getFirstHeader().getHash() : voteResultItem.getBlockHash();
        NulsHash secondHash = bifurcate ? voteResultItem.getSecondHeader().getHash() : null;
        CallMethodUtils.noticeByzantineResult(chain, voteResultItem.getHeight(), bifurcate, firstHash, secondHash);
    }

    /**
     * 设置投票结果（避免并发篡改数据）
     * Set voting results
     * @param chain       链信息
     * @param roundIndex  轮次信息
     * @param packIndex   打包下标
     * @param voteRound   投票轮次
     * @param voteStage   投票阶段
     * */
    private static void completeCurrentResult(Chain chain, long roundIndex, int packIndex, byte voteRound, byte voteStage, VoteResultData voteResultData){
        try {
            switchVoteInfoLock.lock();
            boolean isValidData = roundIndex == CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                    && packIndex == CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound() && voteRound == CURRENT_BLOCK_VOTE_DATA.getVoteRound()
                    && voteStage == CURRENT_BLOCK_VOTE_DATA.getVoteStage();
            if(!isValidData){
                chain.getLogger().warn("Voting rounds switched");
                return;
            }
            CURRENT_BLOCK_VOTE_DATA.setVoteResult(chain, voteRound, voteStage, voteResultData);
        }finally {
            switchVoteInfoLock.unlock();
        }
    }
}
