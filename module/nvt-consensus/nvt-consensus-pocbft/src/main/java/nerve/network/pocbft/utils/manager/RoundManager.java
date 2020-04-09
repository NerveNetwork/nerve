package nerve.network.pocbft.utils.manager;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.round.MeetingMember;
import nerve.network.pocbft.model.bo.round.MeetingRound;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.po.PunishLogPo;
import nerve.network.pocbft.rpc.call.CallMethodUtils;
import nerve.network.pocbft.utils.enumeration.PunishType;
import nerve.network.pocnetwork.service.ConsensusNetService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * 轮次信息管理类
 * Round Information Management Class
 *
 * @author: Jason
 * 2018/11/14
 */
@Component
public class RoundManager {
    @Autowired
    private AgentManager agentManager;

    @Autowired
    private ConsensusNetService consensusNetService;
    /**
     * 添加轮次信息到轮次列表中
     * Add Round Information to Round List
     *
     * @param chain        chain info
     * @param meetingRound 需添加的轮次信息/round info
     */
    public void addRound(Chain chain, MeetingRound meetingRound) {
        try {
            chain.getRoundLock().lock();
            List<MeetingRound> roundList = chain.getRoundList();
            if (roundList == null) {
                roundList = new ArrayList<>();
            } else {
                rollBackRound(chain, meetingRound.getIndex() - 1);
            }
            if(meetingRound.getIndex() != ConsensusConstant.INIT_ROUND_INDEX){
                MeetingRound preRound = meetingRound.getPreRound();
                if(preRound == null && roundList.size() > 1){
                    preRound = roundList.get(roundList.size() - 1);
                    meetingRound.setPreRound(preRound);
                }
            }
            roundList.add(meetingRound);
            if (roundList.size() > ConsensusConstant.ROUND_CACHE_COUNT) {
                roundList.remove(0);
            }
            //通知共识网络最新共识节点出块地址列表
            consensusNetService.updateConsensusList(chain.getChainId(), meetingRound.getMemberAddressList());
        }finally {
            chain.getRoundLock().unlock();
        }
    }

    /**
     * 回滚本地轮次到指定轮次
     *
     * @param roundIndex 回滚到指定轮次
     * @param chain      链信息
     */
    public void rollBackRound(Chain chain, long roundIndex) {
        List<MeetingRound> roundList = chain.getRoundList();
        for (int index = roundList.size() - 1; index >= 0; index--) {
            if (roundList.get(index).getIndex() > roundIndex) {
                roundList.remove(index);
            } else {
                break;
            }
        }
    }

    /**
     * 清理比指定轮次之后的轮次信息
     * Clean up the wheel number information of the specified chain
     *
     * @param chain chain info
     * @param roundIndex 保留几轮轮次信息/Keep several rounds of information
     * @return boolean
     */
    public boolean clearRound(Chain chain, long roundIndex) {
        List<MeetingRound> roundList = chain.getRoundList();
        MeetingRound round;
        for (int i = roundList.size() - 1; i >= 0; i--) {
            round = roundList.get(i);
            if (round.getIndex() > roundIndex) {
                roundList.remove(i);
            } else{
                break;
            }
        }
        return true;
    }

    /**
     * 获取根据轮次下标获取指定轮次，如果轮次信息不存在则根据时间指定时间计算
     * @param chain                  链信息
     * @param roundIndex             轮次下标
     * @param roundStartTime         轮次开始时间
     * @return                       轮次信息
     * */
    public MeetingRound getRound(Chain chain, long roundIndex, long roundStartTime) throws NulsException{
        try{
            chain.getRoundLock().lock();
            MeetingRound round = getRoundByIndex(chain, roundIndex);
            if(round != null){
                return round;
            }
            BlockHeader startBlockHeader = chain.getNewestHeader();
            if (startBlockHeader.getHeight() != 0L) {
                startBlockHeader = getFirstBlockOfPreRound(chain, roundIndex);
            }
            round = calculationRound(chain, startBlockHeader, roundIndex, roundStartTime);
            return round;
        }finally {
            chain.getRoundLock().unlock();
        }
    }

    /**
     * 获取本地最新轮次信息
     * Get the latest local rounds
     *
     * @param chain chain info
     * @return MeetingRound
     */
    public MeetingRound getCurrentRound(Chain chain) {
        List<MeetingRound> roundList = chain.getRoundList();
        chain.getRoundLock().lock();
        try {
            if (roundList == null || roundList.size() == 0) {
                if(chain.getNewestHeader().getHeight() == 0){
                    return null;
                }
                return getRound(chain, chain.getNewestHeader().getExtendsData().getRoundIndex(), chain.getNewestHeader().getExtendsData().getRoundStartTime());
            }
            return roundList.get(roundList.size() - 1);
        } catch (NulsException e){
            chain.getLogger().error(e);
            return null;
        }finally {
            chain.getRoundLock().unlock();
        }
    }

    /**
     * 获取指定下标的轮次信息
     * Get round information for specified Subscripts
     *
     * @param chain      chain info
     * @param roundIndex 轮次下标/round index
     * @return MeetingRound
     */
    public MeetingRound getRoundByIndex(Chain chain, long roundIndex) {
        List<MeetingRound> roundList = chain.getRoundList();
        MeetingRound round;
        for (int i = roundList.size() - 1; i >= 0; i--) {
            round = roundList.get(i);
            if (round.getIndex() == roundIndex) {
                return round;
            } else if (round.getIndex() < roundIndex) {
                break;
            }
        }
        return null;
    }

    /**
     * 获取下一轮的轮次信息
     * Get the next round of round objects
     *
     * @param chain      chain info
     * @param roundData  轮次数据/block extends entity
     * @param isRealTime 是否根据最新时间计算轮次/Whether to calculate rounds based on current time
     * @return MeetingRound
     */
    public MeetingRound getRound(Chain chain, BlockExtendsData roundData, boolean isRealTime) throws Exception {
        chain.getRoundLock().lock();
        try {
            if (isRealTime && roundData == null) {
                return getRoundByRealTime(chain);
            } else if (!isRealTime && roundData == null) {
                return getRoundByNewestBlock(chain);
            } else {
                return getRoundByExpectedRound(chain, roundData);
            }
        } finally {
            chain.getRoundLock().unlock();
        }
    }

    /**
     * 计算指定时间点所在轮次
     * @param chain  链信息
     * @param time   时间点
     * */
    public MeetingRound getRoundByTime(Chain chain, long time) throws Exception {
        int blockHeaderSize = chain.getBlockHeaderList().size();
        for (int index = blockHeaderSize - 1; index >= 0; index--) {
            BlockHeader blockHeader = chain.getBlockHeaderList().get(index);
            if (blockHeader.getTime() <= time) {
                BlockExtendsData blockExtendsData = blockHeader.getExtendsData();
                long roundStartTime = blockExtendsData.getRoundStartTime();
                long roundEndTime = roundStartTime + chain.getConfig().getPackingInterval() * blockExtendsData.getConsensusMemberCount();
                if (roundStartTime <= time) {
                    if (roundEndTime >= time) {
                        return getRound(chain, blockExtendsData, false);
                    } else {
                        int realIndex = index + 1;
                        while (realIndex <= blockHeaderSize - 1) {
                            blockExtendsData.parse(chain.getBlockHeaderList().get(realIndex).getExtend(), 0);
                            roundStartTime = blockExtendsData.getRoundStartTime();
                            roundEndTime = roundStartTime + chain.getConfig().getPackingInterval() * blockExtendsData.getConsensusMemberCount();
                            if (roundStartTime > time) {
                                return null;
                            }
                            if (roundEndTime >= time) {
                                return getRound(chain, blockExtendsData, false);
                            }
                            realIndex++;
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }


    /**
     * 根据时间计算下一轮次信息
     * Calculate the next round of information based on time
     *
     * @param chain chain info
     * @return MeetingRound
     */
    private MeetingRound getRoundByRealTime(Chain chain) throws Exception {
        BlockHeader bestBlockHeader = chain.getNewestHeader();
        BlockHeader startBlockHeader = bestBlockHeader;
        BlockExtendsData bestRoundData = bestBlockHeader.getExtendsData();
        long bestRoundEndTime = bestRoundData.getRoundEndTime(chain.getConfig().getPackingInterval());
        if (startBlockHeader.getHeight() != 0L) {
            long roundIndex = bestRoundData.getRoundIndex();
            /*
            本地最新区块所在轮次已经打包结束，则轮次下标需要加1,则需找到本地最新区块轮次中出的第一个块来计算下一轮的轮次信息
            If the latest block in this area has been packaged, the subscription of the round will need to be added 1.
            */
            if (bestRoundData.getConsensusMemberCount() == bestRoundData.getPackingIndexOfRound() || NulsDateUtils.getCurrentTimeSeconds() >= bestRoundEndTime) {
                roundIndex += 1;
            }
            startBlockHeader = getFirstBlockOfPreRound(chain, roundIndex);
        }
        long nowTime = NulsDateUtils.getCurrentTimeSeconds();
        long index;
        long startTime;
        long packingInterval = chain.getConfig().getPackingInterval();
        /*
        找到需计算的轮次下标及轮次开始时间,如果当前时间<本地最新区块时间，则表示需计算轮次就是本地最新区块轮次
        Find the rounds subscripts to be calculated and the start time of rounds
        */
        if (nowTime < bestRoundEndTime) {
            index = bestRoundData.getRoundIndex();
            startTime = bestRoundData.getRoundStartTime();
        } else {
            long diffTime = nowTime - bestRoundEndTime;
            int consensusMemberCount = bestRoundData.getConsensusMemberCount();
            if (bestBlockHeader.getHeight() == 0) {
                consensusMemberCount = chain.getSeedNodeList().size();
            }
            int diffRoundCount = (int) (diffTime / (consensusMemberCount * packingInterval));
            index = bestRoundData.getRoundIndex() + diffRoundCount + 1;
            startTime = bestRoundEndTime + diffRoundCount * consensusMemberCount * packingInterval;
        }
        return calculationRound(chain, startBlockHeader, index, startTime);
    }

    /**
     * 根据最新区块数据计算下一轮轮次信息
     * Calculate next round information based on the latest block entity
     *
     * @param chain chain info
     * @return MeetingRound
     */
    private MeetingRound getRoundByNewestBlock(Chain chain) throws Exception {
        BlockHeader bestBlockHeader = chain.getNewestHeader();
        BlockExtendsData extendsData = new BlockExtendsData();
        extendsData.parse(bestBlockHeader.getExtend(), 0);
        extendsData.setRoundStartTime(extendsData.getRoundEndTime(chain.getConfig().getPackingInterval()));
        extendsData.setRoundIndex(extendsData.getRoundIndex() + 1);
        return getRoundByExpectedRound(chain, extendsData);
    }

    /**
     * 根据指定区块数据计算所在轮次信息
     * Calculate next round information based on the latest block entity
     *
     * @param chain     chain info
     * @param roundData 区块里的轮次信息/block extends entity
     * @return MeetingRound
     */
    private MeetingRound getRoundByExpectedRound(Chain chain, BlockExtendsData roundData) throws Exception {
        BlockHeader startBlockHeader = chain.getNewestHeader();
        long roundIndex = roundData.getRoundIndex();
        long roundStartTime = roundData.getRoundStartTime();
        if (startBlockHeader.getHeight() != 0L) {
            startBlockHeader = getFirstBlockOfPreRound(chain, roundIndex);
        }
        return calculationRound(chain, startBlockHeader, roundIndex, roundStartTime);
    }

    /**
     * 计算轮次信息
     * Calculate wheel information
     *
     * @param chain            chain info
     * @param startBlockHeader 上一轮次的起始区块/Initial blocks of the last round
     * @param index            轮次下标/round index
     * @param startTime        轮次开始打包时间/start time
     */
    @SuppressWarnings("unchecked")
    private MeetingRound calculationRound(Chain chain, BlockHeader startBlockHeader, long index, long startTime)  throws NulsException{
        MeetingRound round = new MeetingRound();
        round.setIndex(index);
        round.setStartTime(startTime);
        setMemberList(chain, round, startBlockHeader);
        List<byte[]> packingAddressList = CallMethodUtils.getEncryptedAddressList(chain);
        if (!packingAddressList.isEmpty()) {
            round.calcLocalPacker(packingAddressList, chain);
        }
        chain.getLogger().debug("当前轮次为：" + round.getIndex() + ";当前轮次开始打包时间：" + NulsDateUtils.convertDate(new Date(startTime * 1000)));
        chain.getLogger().debug("\ncalculation||index:{},startTime:{},startHeight:{},hash:{}\n" + round.toString() + "\n\n", index, startTime * 1000, startBlockHeader.getHeight(), startBlockHeader.getHash());
        return round;
    }

    /**
     * 设置轮次中打包节点信息
     * Setting Packing Node Information in Rounds
     *
     * @param chain            chain info
     * @param round            轮次信息/round info
     * @param startBlockHeader 上一轮次的起始区块/Initial blocks of the last round
     */
    private void setMemberList(Chain chain, MeetingRound round, BlockHeader startBlockHeader) throws NulsException {
        List<MeetingMember> memberList = new ArrayList<>();
        String seedNodesStr = chain.getConfig().getSeedNodes();
        List<String> seedNodes;
        /*
        种子节点打包信息组装
        Seed node packaging information assembly
        */
        if (StringUtils.isNotBlank(seedNodesStr)) {
            seedNodes = chain.getSeedNodeList();
            for (String address : seedNodes) {
                byte[] addressByte = AddressTool.getAddress(address);
                MeetingMember member = new MeetingMember();
                Agent agent = new Agent();
                agent.setAgentAddress(addressByte);
                agent.setPackingAddress(addressByte);
                agent.setRewardAddress(addressByte);
                agent.setCreditVal(0);
                agent.setDeposit(BigInteger.ZERO);
                member.setRoundStartTime(round.getStartTime());
                member.setAgent(agent);
                member.setRoundIndex(round.getIndex());
                memberList.add(member);
            }
        }
        List<Agent> agentList = agentManager.getPackAgentList(chain, startBlockHeader.getHeight());
        for (Agent agent : agentList) {
            Agent realAgent;
            if(startBlockHeader.getHeight() == chain.getNewestHeader().getHeight()){
                if(agent.getPubKey() == null){
                    chain.getUnBlockAgentList().add(AddressTool.getStringAddressByBytes(agent.getPackingAddress()));
                }
                realAgent = new Agent();
                try {
                    realAgent.parse(agent.serialize(), 0);
                    realAgent.setTxHash(agent.getTxHash());
                } catch (IOException io) {
                    Log.error(io);
                    return;
                }
            }else{
                realAgent = agent;
            }
            MeetingMember member = new MeetingMember();
            member.setRoundStartTime(round.getStartTime());
            member.setRoundIndex(round.getIndex());
            member.setAgent(realAgent);
            realAgent.setCreditVal(calcCreditVal(chain, member, startBlockHeader));
            memberList.add(member);
        }
        round.init(memberList);
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
        /*
        每一轮的惩罚都有可能包含上一轮次的惩罚记录，即计算从a到a+99轮的惩罚记录时，a轮的惩罚中可能是惩罚某个地址在a-1轮未出块，导致100轮最多可能有101个惩罚记录，在这里处理下
        Each round of punishment is likely to contain a rounds punishment record, calculated from a to a + 99 rounds of punishment record,
        a round of punishment is likely to be punished in an address in a - 1 round not out of the blocks,
        lead to round up to 100 May be 101 punishment record, treatment here
        */
        if (count > ConsensusConstant.CREDIT_MAGIC_NUM) {
            return ConsensusConstant.CREDIT_MAGIC_NUM;
        }
        return count;
    }


    /**
     * 获取指定轮次前一轮打包的第一个区块
     * Gets the first block packaged in the previous round of the specified round
     *
     * @param chain      chain info
     * @param roundIndex 轮次下标
     */
    public BlockHeader getFirstBlockOfPreRound(Chain chain, long roundIndex) {
        BlockHeader firstBlockHeader = null;
        long startRoundIndex = 0L;
        List<BlockHeader> blockHeaderList = chain.getBlockHeaderList();
        for (int i = blockHeaderList.size() - 1; i >= 0; i--) {
            BlockHeader blockHeader = blockHeaderList.get(i);
            long currentRoundIndex = blockHeader.getExtendsData().getRoundIndex();
            if (roundIndex > currentRoundIndex) {
                if (startRoundIndex == 0L) {
                    startRoundIndex = currentRoundIndex;
                }
                if (currentRoundIndex < startRoundIndex) {
                    firstBlockHeader = blockHeaderList.get(i + 1);
                    BlockExtendsData roundData = firstBlockHeader.getExtendsData();
                    if (roundData.getPackingIndexOfRound() > 1) {
                        firstBlockHeader = blockHeader;
                    }
                    break;
                }
            }
        }
        if (firstBlockHeader == null) {
            firstBlockHeader = chain.getNewestHeader();
            chain.getLogger().warn("the first block of pre round not found");
        }
        return firstBlockHeader;
    }

    /**
     * 获取指定轮次的前一轮轮次信息
     * Gets the first block packaged in the previous round of the specified round
     *
     * @param chain      chain info
     * @param roundIndex 轮次下标
     */
    public MeetingRound getPreRound(Chain chain, long roundIndex)throws Exception{
        List<BlockHeader> blockHeaderList = chain.getBlockHeaderList();
        BlockHeader blockHeader;
        BlockExtendsData extendsData = null;
        for (int i = blockHeaderList.size() - 1; i >= 0; i--) {
            blockHeader = blockHeaderList.get(i);
            extendsData = blockHeader.getExtendsData();
            if(extendsData.getRoundIndex() < roundIndex){
                break;
            }
        }
        if(extendsData == null){
            return null;
        }
        MeetingRound round = getRoundByIndex(chain, extendsData.getRoundIndex());
        if(round != null){
            return round;
        }
        return getRound(chain,extendsData,false);
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
     * 查询两轮次之间新增的共识节点和注销的共识节点
     * New consensus nodes and unregistered consensus nodes between queries
     *
     * @param chain                  chain
     * @param lastExtendsData        上一轮的轮次信息
     * @param currentExtendsData     本轮轮次信息
     * @return                       两轮次之间节点变化信息
     * */
    public Map<String,List<String>> getAgentChangeInfo(Chain chain, BlockExtendsData lastExtendsData, BlockExtendsData currentExtendsData){
        Map<String, List<String>> resultMap = new HashMap<>(2);
        List<String> registerAgentList;
        List<String> cancelAgentList;
        long lastRoundIndex = -1;
        if(lastExtendsData != null){
            lastRoundIndex = lastExtendsData.getRoundIndex();
        }
        long currentRoundIndex = currentExtendsData.getRoundIndex();
        MeetingRound lastRound = null;
        MeetingRound currentRound;
        try {
            if(lastRoundIndex != -1){
                lastRound = getRoundByIndex(chain, lastRoundIndex);
                if(lastRound == null){
                    lastRound = getRound(chain, lastExtendsData, false);
                }
            }
            currentRound = getRoundByIndex(chain, currentRoundIndex);
            if(currentRound == null){
                currentRound = getRound(chain, currentExtendsData, false);
            }
            registerAgentList = getAgentChangeList(lastRound, currentRound, true);
            cancelAgentList = getAgentChangeList(lastRound, currentRound, false);
        }catch (Exception e){
            chain.getLogger().error(e);
            return null;
        }
        resultMap.put("registerAgentList", registerAgentList);
        resultMap.put("cancelAgentList", cancelAgentList);
        return  resultMap;
    }

    /**
     * 获取两轮次之间新增或减少的节点列表
     * @param lastRound        上一轮
     * @param currentRound     本轮
     * @param isRegister       获取增加节点列表（true）或获取减少的节点列表（false）
     * @return                 节点变化列表
     * */
    private List<String> getAgentChangeList(MeetingRound lastRound, MeetingRound currentRound , boolean isRegister){
        List<String> lastRoundAgentList = new ArrayList<>();
        List<String> currentRoundAgentList  = new ArrayList<>();
        if(lastRound != null){
            for (MeetingMember member:lastRound.getMemberList()) {
                lastRoundAgentList.add(AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
            }
        }
        for (MeetingMember member:currentRound.getMemberList()) {
            currentRoundAgentList.add(AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
        }
        if(isRegister){
            currentRoundAgentList.removeAll(lastRoundAgentList);
            return currentRoundAgentList;
        }else{
            lastRoundAgentList.removeAll(currentRoundAgentList);
            return lastRoundAgentList;
        }
    }
}
