package io.nuls.consensus.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.PackingData;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.rpc.call.CallMethodUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Eva
 */
public class CsController extends BasicObject {
    /**
     * 投票超时时间,默认三秒
     */
    private long VOTE_TIME_OUT = 3000L;

    private long comfirmedHeight = 0;


    private RoundController roundController;
    private VoteController voteController;

    public CsController(Chain chain, RoundController roundController, VoteController voteController) {
        super(chain);
        this.roundController = roundController;
        this.voteController = voteController;
    }

    //这里有几种情况：1启动网络从0开始，2重启网络从某个高度，3启动本节点/
    //目标是不管哪种情况，都能尽快达成共识（轮次一致）
    public void consensus() {
        //等待上一个确认的区块保存
        int index = 0;
        while (chain.getBestHeader().getHeight() < comfirmedHeight) {
            try {
                Thread.sleep(100L);
                index++;
                if (index > chain.getConfig().getPackingIntervalMills() / 100L) {
                    index = 0;
                    Log.warn("It's been too long..");
                    return;
                }
            } catch (InterruptedException e) {
                log.error(e);
            }
        }

        log.info("here1111");
        MeetingRound round = roundController.getCurrentRound();
        log.debug(round.toString());


        MeetingMember localMember = round.getLocalMember();
        if (null == localMember) {
            if (chain.isConsonsusNode()) {
                //实时监测
                chain.setConsonsusNode(false);
            }
            log.info("here1112");
            return;
        }

        //如果轮次没有确认，则每次都重新计算,因为没有确认，所以每次都根据时间进行计算
        if (!round.isConfirmed()) {
            VoteStageResult result = tryIt(round, 0);
            if (result != null) {
                //得到结果，修改正确的轮次，返回
                log.info("tryIt得到结果：");
                MeetingRound newRound = roundController.getRound(result.getRoundIndex(), result.getRoundStartTime());
                newRound.setPackingIndexOfRound(result.getPackingIndexOfRound());
                // 计算本轮是否有延迟，有的话计算出来设置进去
                checkDelayedTime(newRound);
                this.comfirmedResult(newRound, result);
            }
            log.info("here1113");
            return;
        }

        if (round.getPackingIndexOfRound() > round.getMemberCount()) {
            log.info("切换下一轮：");
            roundController.nextRound(round);
            return;
        }
        long packStartTime = round.getStartTime() + round.getDelayedSeconds() + chain.getConfig().getPackingInterval() * (round.getPackingIndexOfRound() - 1);
        long packEndTime = packStartTime + chain.getConfig().getPackingInterval();
        //如果轮次已经确认，并且刚好应该本地出块,时间上 增加一倍的容错时间
        boolean timeOk = (packEndTime * 1000 + VOTE_TIME_OUT) > NulsDateUtils.getCurrentTimeMillis();
        if (localMember.getPackingIndexOfRound() == round.getPackingIndexOfRound() && timeOk) {
            //本地出块，只有round被确认是大家都认同的，才开始出块
            PackingData packingData = new PackingData();
            packingData.setMember(localMember);
            packingData.setPackStartTime(packStartTime);
            packingData.setRound(round);
            chain.getConsensusCache().getPackingQueue().offer(packingData);
            log.info("准备出块{}-{},高度：{},开始时间：" + NulsDateUtils.timeStamp2Str(packingData.getPackStartTime() * 1000)
                    , round.getIndex(), localMember.getPackingIndexOfRound(), chain.getBestHeader().getHeight() + 1);
        }
        long wait = 0;
        if (!timeOk) {
            log.info("时间超过了，应该下一个人出块");
            this.voteController.voteEmptyHash(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getStartTime(), round.getPackingIndexOfRound(), 0, packEndTime, round.getLocalMember().getAgent().getPackingAddressStr());
        } else {
            wait = chain.getConfig().getPackingIntervalMills() + packStartTime * 1000 - NulsDateUtils.getCurrentTimeMillis();
        }
        waitComfirmed(round, wait, 0, localMember.getAgent().getPackingAddressStr());

    }

    //迭代尝试得到最终结果
    //区块验证通过后，需要进行投票
    private void waitComfirmed(MeetingRound round, long packingTimeMills, int voteRoundIndex, String address) {
        //记录当前时间用于重新；
        long time = System.currentTimeMillis() + packingTimeMills;
        if (!chain.isConsonsusNode() || !chain.isSynchronizedHeight() || !chain.isNetworkStateOk()) {
            return;
        }
        //这里如果等待太多次，就算了吧
        if (voteRoundIndex > 5) {
            round.setConfirmed(false);
//            checkRoundTimeout();
            //这里清理缓存的数据
            this.voteController.clearCache();
            this.voteController.clearMap(chain.getBestHeader().getHeight());
            return;
        }
        log.debug("等待达成一致！" + voteRoundIndex);
        this.chain.getConsensusCache().getBestBlocksVotingContainer().setCurrentVoteRoundIndex(voteRoundIndex);

        long realWait = packingTimeMills + VOTE_TIME_OUT - (NulsDateUtils.getCurrentTimeMillis() % 1000);
        VoteStageResult result = chain.getConsensusCache().getStageTwoQueue().poll(realWait, TimeUnit.MILLISECONDS);
        if (null == result) {
            log.debug("超时，得到一个空的结果：wait:" + realWait);
            // 如果之前投了区块，就不应该再投空块了
            //同一个高度的所有数据都缓存起来，包括当前的投票index
            this.voteController.startNextVoteRound(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getPackingIndexOfRound(),
                    round.getStartTime(), voteRoundIndex + 1, address);
            round.setDelayedSeconds(round.getDelayedSeconds() + VOTE_TIME_OUT / 1000);
            this.waitComfirmed(round, 0, voteRoundIndex + 1, address);

        } else if (null != result && !this.comfirmedResult(round, result)) {
            //从等待时间中，去除已经过去的部分
            this.waitComfirmed(round, time - System.currentTimeMillis(), voteRoundIndex + 1, address);
        }
    }

    private boolean comfirmedResult(MeetingRound round, VoteStageResult result) {
        log.info("here1114");
        //这里清理缓存的数据
        this.voteController.clearCache();
        round.setConfirmed(true);
        if (result.getRoundIndex() < chain.getConsensusCache().getLastConfirmedRoundIndex() ||
                (result.getRoundIndex() == chain.getConsensusCache().getLastConfirmedRoundIndex() && result.getPackingIndexOfRound() <= chain.getConsensusCache().getLastConfirmedRoundPackingIndex())) {
            return false;
        }
        log.debug("now:{},result:{}-{}-{}-{}-{}-{}-{}-delay:{}", chain.getBestHeader().getHeight(), result.getHeight(), result.getRoundIndex(), result.getPackingIndexOfRound(), result.getRoundStartTime(),
                result.getBlockHash(), result.getVoteRoundIndex(), result.getStage(), round.getDelayedSeconds());
        chain.getConsensusCache().setLastConfirmed(result.getRoundIndex(), result.getPackingIndexOfRound());
        if (result.getBlockHash().equals(NulsHash.EMPTY_NULS_HASH)) {

            log.debug("下一个节点");

            long packTime = result.getRoundStartTime() + result.getPackingIndexOfRound() * chain.getConfig().getPackingInterval();
            if (result.getRoundIndex() == round.getIndex()) {
                packTime += round.getDelayedSeconds();
            }
            this.roundController.switchPackingIndex(result.getRoundIndex(), result.getRoundStartTime(), result.getPackingIndexOfRound() + 1, packTime);

            return true;
        }

        if (result.getHeight() <= chain.getBestHeader().getHeight()) {
            log.info("重复的投票结果：" + result.getHeight());
            return false;
        }
        if (result.getHeight() > this.comfirmedHeight) {
            this.comfirmedHeight = result.getHeight();
        } else {
            return false;
        }
        //缓存本区块的签名记录
        this.voteController.cacheSignResult(result);

        //先通知区块模块,保存区块时才切换打包人
        log.info("通知区块模块，拜占庭验证通过：" + result.getHeight() + "-" + result.getBlockHash().toHex());
        CallMethodUtils.noticeByzantineResult(chain, result.getHeight(), false, result.getBlockHash(), null);
        log.info("通知区块模块，拜占庭验证通过完成");
        return true;
    }

    //如果找得到大多数，就迭代持续找下去，找不到大多数，就重试
    private VoteStageResult tryIt(MeetingRound round, int count) {
        if (round == null) {
            round = roundController.tempRound();
        }
        if (round.isConfirmed()) {
            return null;
        }

        //这里清理缓存的数据
        if (count >= 30) {
            this.voteController.clearCache();
            chain.getConsensusCache().clear();
            chain.getConsensusCache().getVoteMessageQueue().clear();
            this.voteController.clearMap();
            count = 0;
        }
        //保证round的时效性
        round = checkRoundByTime(round);
        MeetingMember localMember = round.getLocalMember();
        if (null == localMember) {
            return null;
        }
        log.debug("尝试获取一个确认的轮次，tryIt");
        long packEndTime = round.getStartTime() + round.getDelayedSeconds() + chain.getConfig().getPackingInterval() * round.getPackingIndexOfRound();
        //先表明下自己的态度
//        log.info("投空块：{}-{}-{}/{},roundStart:{}", height, round.getIndex(), round.getPackingIndexOfRound(), round.getMemberCount(), NulsDateUtils.timeStamp2Str(round.getStartTime() * 1000));
        this.voteController.voteEmptyHash(chain.getBestHeader().getHeight() + 1, round.getIndex(), round.getStartTime(), round.getPackingIndexOfRound(), 1,
                packEndTime, localMember.getAgent().getPackingAddressStr());
        long wait = packEndTime * 1000 - NulsDateUtils.getCurrentTimeMillis();
        if (wait < 0) {
            return null;
        }
        VoteStageResult result = chain.getConsensusCache().getStageTwoQueue().poll(wait, TimeUnit.MILLISECONDS);
        if (result == null) {
            log.debug("超时，没得到有效结果:{}ms", wait);
            return this.tryIt(roundController.getCurrentRound(), count + 1);
        }
        return result;
    }

    private boolean checkRoundTimeout() {
        //大于五分钟
        MeetingRound currentRound = roundController.getCurrentRound();
        if (NulsDateUtils.getCurrentTimeSeconds() - currentRound.getStartTime() > 300) {
            voteController.clearCache();
            chain.getConsensusCache().clear();
            MeetingRound round1 = this.roundController.initRound();
            this.roundController.switchRound(round1, true);
            return false;
        }
        return true;
    }

    //如果轮次没有确认，则每次都重新计算,因为没有确认，所以每次都根据时间进行计算
    private MeetingRound checkRoundByTime(MeetingRound round) {
        long roundEndTimeMills = round.getStartTimeMills() + round.getMemberCount() * chain.getConfig().getPackingIntervalMills();
        if (roundEndTimeMills <= NulsDateUtils.getCurrentTimeMillis()) {
            //轮次已过时，就重新根据时间计算
            chain.getLogger().debug("尚未达成一致时，初始化轮次，寻找共同的机会");
            round = roundController.initRound();
        }
        long index = (NulsDateUtils.getCurrentTimeMillis() - round.getStartTimeMills()) / chain.getConfig().getPackingIntervalMills();
        round.setPackingIndexOfRound((int) (index + 1));
        return round;
    }

    // 计算本轮是否有延迟，有的话计算出来设置进去
    private void checkDelayedTime(MeetingRound newRound) {
        // 如果延迟时间不为0，则说明这个轮次之前已经存在，不是初始化出来的，不用管
        if (newRound.getDelayedSeconds() != 0) {
            return;
        }
        // 获取到最新一个确认区块的轮次信息
        BlockHeader bestHeader = chain.getBestHeader();
        if (bestHeader == null) {
            return;
        }
        BlockExtendsData bestRoundData = bestHeader.getExtendsData();
        if (bestRoundData.getRoundIndex() != newRound.getIndex()) {
            // 都跳轮了，这种情况不管了
            return;
        }
        // 计算出最近区块应该出的时间和实际时间，做一个对比，就能算出在这之前有多久的延迟
        long realTime = bestHeader.getTime();
        long expectedTime = bestRoundData.getRoundStartTime() + bestRoundData.getPackingIndexOfRound() * chain.getConfig().getPackingInterval();
        long delayedTime = realTime - expectedTime;
        // 对比最新区块和当前时间，看是否有延迟
        long diffTime = NulsDateUtils.getCurrentTimeSeconds() - realTime;
        if (diffTime > chain.getConfig().getPackingInterval()) {
            long newestDelayedTime = (diffTime - (newRound.getPackingIndexOfRound() - bestRoundData.getPackingIndexOfRound()) * chain.getConfig().getPackingInterval());
            // 这里由于取的当前实际，和实际运行中的节点可能不一致，尽量向实际靠拢，向后取VOTE_TIME_OUT的整数
            delayedTime += Math.ceil((float) newestDelayedTime / (float) VOTE_TIME_OUT) * VOTE_TIME_OUT;
        }
        newRound.setDelayedSeconds(delayedTime);
    }
}
