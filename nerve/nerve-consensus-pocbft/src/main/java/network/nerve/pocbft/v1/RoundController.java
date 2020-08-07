package network.nerve.pocbft.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.po.PunishLogPo;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.enumeration.PunishType;
import network.nerve.pocbft.utils.manager.AgentManager;
import network.nerve.pocbft.v1.cache.RoundCache;
import network.nerve.pocbft.v1.entity.BasicObject;
import network.nerve.pocbft.v1.entity.RoundInitData;
import network.nerve.pocbft.v1.utils.CsUtils;

import java.util.*;

/**
 * @author Eva
 */
public class RoundController extends BasicObject {

    private ConsensusNetService consensusNetService;

    public RoundController(Chain chain) {
        super(chain);
        this.agentManager = SpringLiteContext.getBean(AgentManager.class);
    }

    /**
     * 轮次缓存数据
     */
    public RoundCache roundCache = new RoundCache();

    private AgentManager agentManager;

    /**
     * 根据本地时间计算
     */
    public MeetingRound initRound() {
        log.info("初始化轮次：");
        RoundInitData initData = calcNowInitData();
        MeetingRound round = calcRound(initData, true);
        this.switchRound(round, false);
        return round;
    }

    /**
     * 根据本地时间计算
     */
    public void nextRound(MeetingRound currentRound) {
        log.info("run here");
//        chain.getRoundLock().lock();
//        try {
        RoundInitData initData = calcNextRoundInitData(currentRound);
        MeetingRound round = calcRound(initData, true);
        this.switchRound(round, currentRound.isConfirmed());
//        } finally {
//            chain.getRoundLock().unlock();
//        }
    }

    public MeetingRound tempRound() {
        RoundInitData initData = calcNowInitData();
        return calcRound(initData, false);
    }

    public MeetingRound getCurrentRound() {
        return roundCache.getCurrentRound();
    }

    public void switchRound(MeetingRound round, boolean confirmed) {
//        log.info("切换轮次，轮次的确认状态：" + confirmed);
        round.setConfirmed(confirmed);
        roundCache.switchRound(round);
//        log.info(round.toString());
    }

    /**
     * 根据当前区块，计算一个理想化的初始轮次
     *
     * @param currentRound
     * @return
     */
    private RoundInitData calcNextRoundInitData(MeetingRound currentRound) {
        RoundInitData data = new RoundInitData();
        if (currentRound == null) {
            return calcNowInitData();
        }
        long nextRoundIndex = currentRound.getIndex() + 1;
        long nextStartTime = currentRound.getStartTime() + currentRound.getDelayedSeconds() + currentRound.getMemberCount() * chain.getConfig().getPackingInterval();
        BlockHeader startHeader = CsUtils.GetFirstBlockOfPreRound(chain, nextRoundIndex);
        List<Agent> agentList = agentManager.getPackAgentList(chain, startHeader.getHeight());
        agentList.addAll(chain.getSeedAgentList());

        data.setRoundIndex(nextRoundIndex);
        data.setAgentList(agentList);
        data.setStartHeader(startHeader);
        data.setDelayedSeconds(0L);
        data.setStartTime(nextStartTime);
        return data;
    }

    private RoundInitData calcNowInitData() {
        RoundInitData data = new RoundInitData();

        BlockHeader bestHeader = chain.getBestHeader();
        BlockExtendsData bestRoundData = bestHeader.getExtendsData();
        long bestRoundEndTime = bestHeader.getTime() + chain.getConfig().getPackingInterval() * (bestRoundData.getConsensusMemberCount() - bestRoundData.getPackingIndexOfRound());


        long calcRoundIndex = bestRoundData.getRoundIndex();

        //先判断这一轮是不是已经完了
        if (bestRoundData.getPackingIndexOfRound() == bestRoundData.getConsensusMemberCount() ||
                bestRoundEndTime < NulsDateUtils.getCurrentTimeSeconds()) {
            calcRoundIndex = bestRoundData.getRoundIndex() + 1;
        }

        BlockHeader startHeader = CsUtils.GetFirstBlockOfPreRound(chain, calcRoundIndex);

        List<Agent> agentList = agentManager.getPackAgentList(chain, startHeader.getHeight());
        agentList.addAll(chain.getSeedAgentList());

        data.setAgentList(agentList);
        data.setStartHeader(startHeader);
        data.setRoundIndex(calcRoundIndex);
        //如果就是计算，最新区块的轮次，那就好办了
        if (calcRoundIndex == bestRoundData.getRoundIndex()) {
            data.setStartTime(bestRoundData.getRoundStartTime());
            data.setDelayedSeconds(bestHeader.getTime() - bestRoundData.getRoundStartTime() - chain.getConfig().getPackingInterval() * bestRoundData.getPackingIndexOfRound());
            return data;
        }

        long interval = NulsDateUtils.getCurrentTimeSeconds() - bestRoundEndTime;
        long wholeRoundTime = agentList.size() * chain.getConfig().getPackingInterval();
        //最新轮的下一轮
        if (interval < wholeRoundTime) {
            data.setStartTime(bestRoundEndTime);
            data.setDelayedSeconds(0L);
            return data;
        }
        //中间空了长时间，没有出块的情况
        long addIndex = interval / wholeRoundTime;
        data.setRoundIndex(data.getRoundIndex() + addIndex);
        data.setStartTime(bestRoundEndTime + addIndex * wholeRoundTime);
        data.setDelayedSeconds(0L);
//        log.info("计算轮次起始时间，早于当前：" + (NulsDateUtils.getCurrentTimeMillis() - data.getStartTime() * 1000) + "ms");
        return data;
    }

    public MeetingRound getRound(long roundIndex, long roundStartTime) {
//        chain.getRoundLock().lock();
//        try {
        MeetingRound round = roundCache.get(roundIndex);
        if (null == round) {
            round = calcRound(roundIndex, roundStartTime, false);
        }
        return round;
//        } finally {
//            chain.getRoundLock().unlock();
//        }
    }

    private MeetingRound calcRound(long roundIndex, long roundStartTime, boolean cache) {
//        log.info("======重新计算=轮次====================");
        RoundInitData initData = new RoundInitData();
        initData.setRoundIndex(roundIndex);
        initData.setStartTime(roundStartTime);

        BlockHeader startHeader = CsUtils.GetFirstBlockOfPreRound(chain, roundIndex);
        initData.setStartHeader(startHeader);

        List<Agent> agentList = agentManager.getPackAgentList(chain, startHeader.getHeight());
        agentList.addAll(chain.getSeedAgentList());
        initData.setAgentList(agentList);

        return calcRound(initData, cache);
    }


    private MeetingRound calcRound(RoundInitData initData, boolean cache) {
        MeetingRound round = new MeetingRound();
        round.setIndex(initData.getRoundIndex());
        round.setStartTime(initData.getStartTime());
        List<MeetingMember> memberList = new ArrayList<>();
        Set<String> memberAddressSet = new HashSet<>();
        for (Agent agent : initData.getAgentList()) {
            Agent realAgent;
            if (agent.getPubKey() == null) {
                chain.getUnBlockAgentList().add(agent.getPackingAddressStr());
            }
            realAgent = new Agent();
            try {
                realAgent.parse(agent.serialize(), 0);
                realAgent.setTxHash(agent.getTxHash());
                MeetingMember member = new MeetingMember();
                member.setRoundStartTime(round.getStartTime());
                member.setRoundIndex(round.getIndex());
                member.setAgent(realAgent);
                realAgent.setCreditVal(calcCreditVal(chain, member, initData.getStartHeader()));
                memberList.add(member);
                memberAddressSet.add(AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
            } catch (Exception io) {
                Log.error(io);
                return null;
            }
        }
        round.init(memberList);
        //必要的预计算设值
        round.setMemberAddressSet(memberAddressSet);
        List<byte[]> localAddressList = CallMethodUtils.getEncryptedAddressList(chain);

        round.calcLocalPacker(localAddressList, chain);

        long seconds = NulsDateUtils.getCurrentTimeSeconds() - round.getStartTime();
        if (seconds < 0) {
            seconds = 0;
        }
        long index = seconds / chain.getConfig().getPackingInterval() + 1;
        round.setPackingIndexOfRound((int) index);
        round.setDelayedSeconds(initData.getDelayedSeconds());
        if (cache) {
            roundCache.put(round.getIndex(), round);
        }
        updateConsensusNetList(round);


        return round;
    }

    private void updateConsensusNetList(MeetingRound round) {
        if (null == consensusNetService) {
            consensusNetService = SpringLiteContext.getBean(ConsensusNetService.class);
        }
        //通知共识网络最新共识节点出块地址列表
        consensusNetService.updateConsensusList(chain.getChainId(), round.getMemberAddressSet());
    }

    /**
     * 计算节点的信誉值
     * Calculating the Node's Credit Value
     *
     * @param chain       chain info
     * @param member      打包成员对象/packing info
     * @param blockHeader 区块头/block header
     * @return double
     */
    private double calcCreditVal(Chain chain, MeetingMember member, BlockHeader blockHeader) throws NulsException {
        BlockExtendsData roundData = blockHeader.getExtendsData();
        long roundStart = roundData.getRoundIndex() - ConsensusConstant.RANGE_OF_CAPACITY_COEFFICIENT;
        if (roundStart < 0) {
            roundStart = 0;
        }
        /*
        信誉值计算是通过限定轮次内节点出块数与黄牌数计算出的
        Credit value is calculated by limiting the number of blocks and yellow cards of nodes in rounds.
        */
        long blockCount = getBlockCountByAddress(chain, member.getAgent().getPackingAddress(), roundStart, roundData.getRoundIndex() - 1);
        long sumRoundVal = getPunishCountByAddress(chain, member.getAgent().getAgentAddress(), roundStart, roundData.getRoundIndex() - 1, PunishType.YELLOW.getCode());
        double ability = DoubleUtils.div(blockCount, ConsensusConstant.RANGE_OF_CAPACITY_COEFFICIENT);
        double penalty = DoubleUtils.div(sumRoundVal, ConsensusConstant.RANGE_OF_CAPACITY_COEFFICIENT);

        return DoubleUtils.round(DoubleUtils.sub(ability, penalty), 4);
    }

    /**
     * 获取地址出块数量
     * Get the number of address blocks
     *
     * @param chain          chain info
     * @param packingAddress 出块地址
     * @param roundStart     起始轮次
     * @param roundEnd       结束轮次
     */
    private long getBlockCountByAddress(Chain chain, byte[] packingAddress, long roundStart, long roundEnd) {
        long count = 0;
        List<BlockHeader> blockHeaderList = chain.getBlockHeaderList();
        for (int i = blockHeaderList.size() - 1; i >= 0; i--) {
            BlockHeader blockHeader = blockHeaderList.get(i);
            BlockExtendsData roundData = blockHeader.getExtendsData();
            if (roundData.getRoundIndex() > roundEnd) {
                continue;
            }
            if (roundData.getRoundIndex() < roundStart) {
                break;
            }
            if (Arrays.equals(blockHeader.getPackingAddress(chain.getConfig().getChainId()), packingAddress)) {
                count++;
            }
        }
        return count;
    }


    /**
     * 获取指定地址获得的红黄牌惩罚数量
     * Get the number of red and yellow card penalties for the specified address
     *
     * @param chain      chain info
     * @param address    地址/address
     * @param roundStart 起始轮次/round start index
     * @param roundEnd   结束轮次/round end index
     * @param code       红黄牌标识/Red and yellow logo
     * @return long
     */
    private long getPunishCountByAddress(Chain chain, byte[] address, long roundStart, long roundEnd, int code) throws NulsException {
        long count = 0;
        List<PunishLogPo> punishList = chain.getYellowPunishList();
        if (code == PunishType.RED.getCode()) {
            punishList = chain.getRedPunishList();
        }
        for (int i = punishList.size() - 1; i >= 0; i--) {
            if (count >= ConsensusConstant.CREDIT_MAGIC_NUM) {
                break;
            }
            PunishLogPo punish = punishList.get(i);

            if (punish.getRoundIndex() > roundEnd) {
                continue;
            }
            if (punish.getRoundIndex() < roundStart) {
                break;
            }
            if (Arrays.equals(punish.getAddress(), address)) {
                count++;
            }
        }
        /**
         * 每一轮的惩罚都有可能包含上一轮次的惩罚记录，即计算从a到a+99轮的惩罚记录时，a轮的惩罚中可能是惩罚某个地址在a-1轮未出块，导致100轮最多可能有101个惩罚记录，在这里处理下
         */
        if (count > ConsensusConstant.CREDIT_MAGIC_NUM) {
            return ConsensusConstant.CREDIT_MAGIC_NUM;
        }
        return count;
    }

    public void switchPackingIndex(long roundIndex, long roundStartTime, int nextPackingIndex, long nextPackingStartTime) {
        log.info("run here");
        MeetingRound round = this.getRound(roundIndex, roundStartTime);
        if (round.getStartTime() != roundStartTime) {
            round.setStartTime(roundStartTime);
            round.resetMemberOrder();
        }

        round.setPackingIndexOfRound(nextPackingIndex);
        if (round.getMemberCount() < nextPackingIndex) {
            round = this.getRound(roundIndex + 1, nextPackingStartTime);
            round.setPackingIndexOfRound(1);
        } else {
            long delayedTime = nextPackingStartTime - roundStartTime - (nextPackingIndex - 1) * chain.getConfig().getPackingInterval();
            if (delayedTime > round.getDelayedSeconds()) {
                round.setDelayedSeconds(delayedTime);
            }
        }
        this.switchRound(round, true);
    }

    public MeetingRound getRoundByIndex(long roundIndex, long startTime) {
        log.info("run here");
        MeetingRound round = this.roundCache.getRoundByIndex(roundIndex);
        if (round == null) {
            return getRoundByIndex(roundIndex);
        }
        return round;
    }

    public MeetingRound getRoundByIndex(long roundIndex) {
//        log.info("run here");

        MeetingRound currentRound = getCurrentRound();
        if (null != currentRound && currentRound.getIndex() <= roundIndex) {
            return currentRound;
        }

        MeetingRound round = this.roundCache.getRoundByIndex(roundIndex);
        if (round != null) {
            return round;
        }

        RoundInitData data = new RoundInitData();
        BlockHeader firstBlockHeader = null;
        long startRoundIndex = 0L;
        long startTime = 0L;
        List<BlockHeader> blockHeaderList = chain.getBlockHeaderList();
        for (int i = blockHeaderList.size() - 1; i >= 0; i--) {
            BlockHeader blockHeader = blockHeaderList.get(i);
            long currentRoundIndex = blockHeader.getExtendsData().getRoundIndex();
            if (currentRoundIndex == roundIndex && 0 == startTime) {
                startTime = blockHeader.getExtendsData().getRoundStartTime();
            }
            if (roundIndex <= currentRoundIndex) {
                continue;
            }
            if (startRoundIndex == 0L) {
                startRoundIndex = currentRoundIndex;
            }
            //如果前一轮第一个出块人，没出块，就选择再前一轮最后一个区块
            if (currentRoundIndex < startRoundIndex) {
                firstBlockHeader = blockHeaderList.get(i + 1);
                BlockExtendsData roundData = firstBlockHeader.getExtendsData();
                if (roundData.getPackingIndexOfRound() > 1) {
                    firstBlockHeader = blockHeader;
                }
                break;
            }
        }
        if (startTime == 0) {
            return null;
        }
        if (firstBlockHeader == null) {
            firstBlockHeader = chain.getBestHeader();
        }

        List<Agent> agentList = agentManager.getPackAgentList(chain, firstBlockHeader.getHeight());
        agentList.addAll(chain.getSeedAgentList());
        data.setAgentList(agentList);
        data.setStartHeader(firstBlockHeader);
        data.setRoundIndex(roundIndex);
        data.setStartTime(startTime);
        round = calcRound(data, false);

        return round;
    }
}
