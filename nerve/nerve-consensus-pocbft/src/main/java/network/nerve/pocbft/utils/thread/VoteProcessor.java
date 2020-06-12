package network.nerve.pocbft.utils.thread;

import network.nerve.pocbft.cache.VoteCache;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.vote.VoteResultData;
import network.nerve.pocbft.model.bo.vote.VoteResultItem;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.ConsensusAwardUtil;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.LoggerUtil;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.message.VoteMessage;
import network.nerve.pocbft.utils.enumeration.ConsensusStatus;
import network.nerve.pocbft.utils.manager.ConsensusManager;
import network.nerve.pocbft.utils.manager.RoundManager;
import network.nerve.pocbft.utils.manager.VoteManager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VoteProcessor implements Runnable {
    private RoundManager roundManager = SpringLiteContext.getBean(RoundManager.class);
    private Chain chain;

    public VoteProcessor(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //等待轮次初始化
                if(VoteCache.CURRENT_BLOCK_VOTE_DATA == null){
                    if(!ConsensusNetUtil.initRound(chain, false)){
                        Thread.sleep(2000L);
                        continue;
                    }
                }

                //等待共识网络组建/成为共识节点/共识网络状态变更
                if (!chain.isNetworkState() || !chain.isPacker() || VoteCache.CONSENSUS_NET_CHANGE_UNAVAILABLE) {
                    Thread.sleep(1000L);
                    continue;
                }

                //等待上一轮投票处理完成
                while (!VoteCache.PRE_ROUND_CONFIRMED) {
                    try {
                        chain.getLogger().debug("等待上一轮投票结束！" );
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        chain.getLogger().error(e);
                    }
                }

                VoteCache.PRE_ROUND_CONFIRMED = false;
                VoteCache.CURRENT_ROUND_FINISH = false;
                /*
                 * 当前区块如果已经提前被确认，
                 * 如果确认的不是空块则直接通知区块模块该区块拜占庭已经完成等待区块被保存
                 * 如果确认的空块这直接voteData
                 * */
                if (VoteCache.CURRENT_BLOCK_VOTE_DATA.isFinished()) {
                    chain.getLogger().info("当前投票轮次已完成，{}", VoteCache.CURRENT_BLOCK_VOTE_DATA.toString());
                    try {
                        VoteCache.CURRENT_ROUND_FINISH = true;
                        VoteResultData stageTwoResult = VoteCache.CURRENT_BLOCK_VOTE_DATA.getFinalResult();
                        if (stageTwoResult.isConfirmedEmpty() || stageTwoResult.getVoteResultItem().getHeight() <= chain.getNewestHeader().getHeight()) {
                            VoteManager.switchVoteData(chain, stageTwoResult, false);
                        } else {
                            VoteManager.noticeByzantineResult(chain,stageTwoResult.getVoteResultItem());
                        }
                        continue;
                    } catch (Exception e) {
                        chain.getLogger().error(e);
                        VoteCache.CURRENT_ROUND_FINISH = true;
                    }
                }
                long time = VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getTime();
                /*
                 * 1.判断当前轮次是否该本节点出块
                 * 1.1.如果该本节点出块，则出块，等待投票结果收集
                 * 1.2.如果不该本节点出块，则等待投票收集结果
                 */
                MeetingRound consensusRound = roundManager.getRound(chain, VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex(), time);
                MeetingMember member = consensusRound.getMyMember();
                if (member == null) {
                    chain.getLogger().warn("Current node is not a consensus node");
                    continue;
                }
                boolean packing = checkConsensusStatus(chain)
                        && member.getRoundIndex() == VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                        && member.getPackingIndexOfRound() == VoteCache.CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound()
                        && VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRound() == ConsensusConstant.VOTE_INIT_ROUND;

                long packEndTime = time + chain.getConfig().getPackingInterval();
                long packEndTimeMills = packEndTime * 1000;
                long currentTime = NulsDateUtils.getCurrentTimeMillis();
                chain.getLogger().info("Block out time information of this round block,,currentTime:{},packEndTimeMills:{}", currentTime,packEndTimeMills);
                if (packing && currentTime < packEndTimeMills) {
                    long packRemainTime = packEndTimeMills - currentTime;
                    //如果打包时间大于2S，则需要等待，出块最大时间为2S(网络重启是有可能出现出块时间大于2S的情况)
                    if(packRemainTime > 2000){
                        long waitTime = packRemainTime - 2000;
                        chain.getLogger().info("出块时间大于2秒，等待打包，等待时间：{}",waitTime );
                        TimeUnit.MILLISECONDS.sleep(waitTime);
                    }
                    chain.getLogger().info("Node begins to block,roundIndex:{},packingIndex:{},currentTime:{},packEndTime:{}", consensusRound.getIndex(), member.getPackingIndexOfRound(),currentTime,packEndTimeMills);
                    //判断是否需要结算共识奖励，当前区块是当天的第一个区块则需要结算昨天的共识奖励
                    packingBlock(chain, consensusRound, member, time, ConsensusAwardUtil.settleConsensusAward(chain, time));
                }
                //如果本轮次投票已完成，则直接进入第二轮投票
                String packAddress = AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress());
                if (VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().isFinished()) {
                    chain.getLogger().debug("当前投票轮次已结束，进入下一轮投票!" );
                    VoteCache.CURRENT_ROUND_FINISH = true;
                    continue;
                }
                //第一阶段投票及结果收集
                VoteResultData stageOneResult = stageOneVote(chain, packing, packEndTimeMills, packAddress);
                //第二阶段投票及结果收集
                stageTwoVote(chain, stageOneResult, packAddress, packEndTime);
            }catch (Exception e) {
                chain.getLogger().error(e);
                VoteCache.CURRENT_ROUND_FINISH = true;
            }
        }
    }

    private void packingBlock(Chain chain, MeetingRound round, MeetingMember member, long time, boolean settleConsensusAward) {
        long start = System.currentTimeMillis();
        Block block;
        try {
            block = ConsensusManager.doPacking(chain, member, round, time, settleConsensusAward);
            if (block == null) {
                return;
            }
            CallMethodUtils.receivePackingBlock(chain.getConfig().getChainId(), RPCUtil.encode(block.serialize()));
        } catch (Exception e) {
            chain.getLogger().error("Packing exception");
            chain.getLogger().error(e);
            return;
        }
        VoteCache.CURRENT_BLOCK_VOTE_DATA.setVoteBlock(chain, block.getHeader());
        chain.getLogger().info("doPacking use:" + (System.currentTimeMillis() - start) + "ms" + "\n\n");
    }

    /**
     * 检查本节点共识状态度
     * @param chain 链信息
     * */
    private boolean checkConsensusStatus(Chain chain) {
        /*
        检查节点状态是否可打包(区块管理模块同步完成之后设置该状态)
        Check whether the node status can be packaged (set up after the block management module completes synchronization)
        */
        if (!chain.isCanPacking()) {
            chain.getLogger().debug("打包状态为不可打包！");
            return false;
        }
        return chain.getConsensusStatus().ordinal() >= ConsensusStatus.RUNNING.ordinal();
    }

    /**
     * 第一阶段投票
     *
     * @param chain       链信息
     * @param isPacker    是否为出块节点
     * @param packEndTime 当前区块打包结束时间
     * @param packAddress 本节点共识账户
     */
    private VoteResultData stageOneVote(Chain chain, boolean isPacker, long packEndTime, String packAddress) {
        if(VoteCache.VOTE_HANDOVER ){
            return null;
        }
        long waitBlockEndTime = packEndTime + ConsensusConstant.VOTE_STAGE_ONE_WAIT_TIME;
        if (!isPacker) {
            while (NulsHash.EMPTY_NULS_HASH.equals(VoteCache.CURRENT_BLOCK_VOTE_DATA.getBlockHash()) && !VoteCache.VOTE_HANDOVER && NulsDateUtils.getCurrentTimeMillis() <= waitBlockEndTime) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    chain.getLogger().error(e);
                }
            }
        }
        //第一阶段投票，如果还没收到区块HASH则投空块，如果收到分叉块则投分叉，否则投正常区块
        chain.getLogger().debug("stage vote one start,startTime:{}，waitBlockEndTime：{}",NulsDateUtils.getCurrentTimeMillis(),waitBlockEndTime);
        VoteMessage stageOneVote = new VoteMessage(VoteCache.CURRENT_BLOCK_VOTE_DATA);
        stageOneVote.setVoteStage(ConsensusConstant.VOTE_STAGE_ONE);
        voteAndBroad(chain, packAddress, stageOneVote);
        VoteResultData stageOneResult;
        try {
            long voteStageOneResultEndTime = packEndTime + ConsensusConstant.VOTE_STAGE_ONE_RESULT_WAIT_TIME - NulsDateUtils.getCurrentTimeMillis();
            if (voteStageOneResultEndTime < ConsensusConstant.WAIT_VOTE_RESULT_MIN_TIME) {
                voteStageOneResultEndTime = ConsensusConstant.WAIT_VOTE_RESULT_MIN_TIME;
            }
            stageOneResult = VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().get(VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRound()).getStageOne().getVoteResult().get(voteStageOneResultEndTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            chain.getLogger().warn("First stage voting collection timeout");
            return null;
        }
        return stageOneResult;
    }

    /**
     * 第二阶段投票
     */
    private void stageTwoVote(Chain chain, VoteResultData stageOneResult, String packAddress, long packEndTime) {
        //如果本轮次投票以结束则直接处理投票结果
        long roundIndex = VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex();
        int packIndex = VoteCache.CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound();
        byte voteRound = VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRound();
        if (!VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().isFinished()) {
            chain.getLogger().debug("进入第二阶段投票，RoundIndex:{},PackIndex:{},VoteRound:{}", roundIndex,packIndex,voteRound);
            VoteCache.CURRENT_BLOCK_VOTE_DATA.setVoteStage(ConsensusConstant.VOTE_STAGE_TWO);
            VoteMessage stageTwoVote = new VoteMessage(VoteCache.CURRENT_BLOCK_VOTE_DATA);
            stageTwoVote.setVoteStage(ConsensusConstant.VOTE_STAGE_TWO);
            if (stageOneResult == null || !stageOneResult.isResultSuccess() || stageOneResult.isConfirmedEmpty()) {
                stageTwoVote.setBlockHash(NulsHash.EMPTY_NULS_HASH);
            } else {
                VoteResultItem voteResultItem = stageOneResult.getVoteResultItem();
                stageTwoVote.setBlockHash(voteResultItem.getBlockHash());
                if (voteResultItem.getFirstHeader() != null && voteResultItem.getSecondHeader() != null) {
                    stageTwoVote.setFirstHeader(voteResultItem.getFirstHeader());
                    stageTwoVote.setSecondHeader(voteResultItem.getSecondHeader());
                }
            }
            voteAndBroad(chain, packAddress, stageTwoVote);
        }
        long voteStageTwoTimeOutSecond = packEndTime + ConsensusConstant.VOTE_STAGE_TWO_TIME_OUT;
        try {
            long waitMilliseconds = voteStageTwoTimeOutSecond * 1000 - NulsDateUtils.getCurrentTimeMillis();
            VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRoundMap().get(VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRound()).getStageTwo().getVoteResult().get(waitMilliseconds, TimeUnit.MILLISECONDS);
            VoteCache.CURRENT_ROUND_FINISH = true;
            chain.getLogger().info("当前投票轮次结束，RoundIndex:{},PackIndex:{},RoundIndex:{}", roundIndex,packIndex,voteRound);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            chain.getLogger().warn("Stage two voting collection timeout");
            VoteCache.CURRENT_ROUND_FINISH = true;
            //如果超时则直接切换投票轮次
            VoteManager.switchVoteRound(chain, roundIndex, packIndex, voteRound, voteStageTwoTimeOutSecond - chain.getConfig().getPackingInterval(), null);
        }
    }

    /**
     * 签名并广播投票消息
     *
     * @param chain   链信息
     * @param address 签名账户地址
     * @param message 投票消息
     */
    private void voteAndBroad(Chain chain, String address, VoteMessage message) {
        //签名
        byte[] sign;
        try {
            sign = CallMethodUtils.signature(chain, address, NulsHash.calcHash(message.serializeForDigest()).getBytes());
        }  catch (IOException | NulsException e) {
            LoggerUtil.commonLog.error(e);
            return;
        }
        message.setSign(sign);
        message.setLocal(true);
        VoteCache.CURRENT_BLOCK_VOTE_DATA.isRepeatMessage(message.getVoteRound(), message.getVoteStage(), message.getAddress(chain));
        if (message.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE) {
            VoteCache.CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.offer(message);
            VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageOne().getHaveVotedAccountSet().add(address);
            VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageOne().getVoteMessageMap().put(address, message);
        } else {
            VoteCache.CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.offer(message);
            VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageTwo().getHaveVotedAccountSet().add(address);
            VoteCache.CURRENT_BLOCK_VOTE_DATA.getCurrentRoundData().getStageTwo().getVoteMessageMap().put(address, message);
        }
        try {
            ConsensusNetUtil.broadcastInConsensus(chain.getChainId(), CommandConstant.MESSAGE_VOTE, RPCUtil.encode(message.serialize()), message.getSendNode());
        }catch (IOException e){
            chain.getLogger().error(e);
        }
    }
}
