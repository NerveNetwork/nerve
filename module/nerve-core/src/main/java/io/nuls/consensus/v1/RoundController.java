package io.nuls.consensus.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.v1.cache.RoundCache;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.RoundInitData;
import io.nuls.consensus.v1.utils.CsUtils;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.constant.PocbftConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.po.PunishLogPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.enumeration.PunishType;
import io.nuls.consensus.utils.manager.AgentManager;

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
     * Round caching data
     */
    public RoundCache roundCache = new RoundCache();

    private AgentManager agentManager;

    /**
     * Calculated based on local time
     */
    public MeetingRound initRound() {
        log.debug("Initialize round：");
        RoundInitData initData = calcNowInitData();
        MeetingRound round = calcRound(initData, true);
        this.switchRound(round, false);
        return round;
    }

    /**
     * Calculated based on local time
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
//        log.info("Switching rounds, confirmation status of rounds：" + confirmed);
        round.setConfirmed(confirmed);
        roundCache.switchRound(round);
//        log.info(round.toString());
    }

    /**
     * Calculate an idealized initial round based on the current block
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

        //Let's first determine if this round is already over
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
        //If it's just calculation, the latest block's round, then it's easy to handle
        if (calcRoundIndex == bestRoundData.getRoundIndex()) {
            data.setStartTime(bestRoundData.getRoundStartTime());
            data.setDelayedSeconds(bestHeader.getTime() - bestRoundData.getRoundStartTime() - chain.getConfig().getPackingInterval() * bestRoundData.getPackingIndexOfRound());
            return data;
        }

        long interval = NulsDateUtils.getCurrentTimeSeconds() - bestRoundEndTime;
        long wholeRoundTime = agentList.size() * chain.getConfig().getPackingInterval();
        //The next round of the latest round
        if (interval < wholeRoundTime) {
            data.setStartTime(bestRoundEndTime);
            data.setDelayedSeconds(0L);
            return data;
        }
        //There was a long gap in the middle without any blocks being produced
        long addIndex = interval / wholeRoundTime;
        data.setRoundIndex(data.getRoundIndex() + addIndex);
        data.setStartTime(bestRoundEndTime + addIndex * wholeRoundTime);
        data.setDelayedSeconds(0L);
//        log.info("Calculate the start time of the round, which is earlier than the current one：" + (NulsDateUtils.getCurrentTimeMillis() - data.getStartTime() * 1000) + "ms");
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
//        log.info("======Recalculate=Round====================");
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
        //Necessary pre calculated values
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
        //Notify consensus network of the latest consensus node block address list
        consensusNetService.updateConsensusList(chain.getChainId(), round.getMemberAddressSet());
    }

    /**
     * Calculate the reputation value of nodes
     * Calculating the Node's Credit Value
     *
     * @param chain       chain info
     * @param member      Packaging member objects/packing info
     * @param blockHeader Block head/block header
     * @return double
     */
    private double calcCreditVal(Chain chain, MeetingMember member, BlockHeader blockHeader) throws NulsException {
        BlockExtendsData roundData = blockHeader.getExtendsData();
        long roundStart = roundData.getRoundIndex() - PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain);
        if (roundStart < 0) {
            roundStart = 0;
        }
        /*
        The calculation of reputation value is based on the number of blocks and yellow cards produced by nodes within a limited number of rounds
        Credit value is calculated by limiting the number of blocks and yellow cards of nodes in rounds.
        */
        long blockCount = getBlockCountByAddress(chain, member.getAgent().getPackingAddress(), roundStart, roundData.getRoundIndex() - 1);
        long sumRoundVal = getPunishCountByAddress(chain, member.getAgent().getAgentAddress(), roundStart, roundData.getRoundIndex() - 1, PunishType.YELLOW.getCode());
        double ability = DoubleUtils.div(blockCount, PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain));
        double penalty = DoubleUtils.div(sumRoundVal, PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain));

        return DoubleUtils.round(DoubleUtils.sub(ability, penalty), 4);
    }

    /**
     * Obtain address block quantity
     * Get the number of address blocks
     *
     * @param chain          chain info
     * @param packingAddress Block address
     * @param roundStart     Starting round
     * @param roundEnd       End round
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
     * Obtain the number of red and yellow card penalties obtained from the specified address
     * Get the number of red and yellow card penalties for the specified address
     *
     * @param chain      chain info
     * @param address    address/address
     * @param roundStart Starting round/round start index
     * @param roundEnd   End round/round end index
     * @param code       Red and yellow card identification/Red and yellow logo
     * @return long
     */
    private long getPunishCountByAddress(Chain chain, byte[] address, long roundStart, long roundEnd, int code) throws NulsException {
        long count = 0;
        List<PunishLogPo> punishList = chain.getYellowPunishList();
        if (code == PunishType.RED.getCode()) {
            punishList = chain.getRedPunishList();
        }
        for (int i = punishList.size() - 1; i >= 0; i--) {
            if (count >= PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain)) {
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
         * Each round of punishment may include the punishment record from the previous round, i.e. calculating fromareacha+99When recording the punishment for a round,aIn the punishment of the round, it may be that a certain address is penalizeda-1The wheel did not come out of the block, resulting in100The maximum possible number of wheels101Punishment records, process them here
         */
        if (count > PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain)) {
            return PocbftConstant.getRANGE_OF_CAPACITY_COEFFICIENT(chain);
        }
        return count;
    }

    public void switchPackingIndex(long roundIndex, long roundStartTime, int nextPackingIndex, long nextPackingStartTime) {
        log.debug("run here");
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
        log.debug("run here");
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
            //If the first block maker in the previous round did not produce any blocks, then choose the last block in the previous round
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
