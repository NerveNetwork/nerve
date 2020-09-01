package network.nerve.pocbft.v1.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.v1.RoundController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class RoundUtils {

    private static RoundController roundController;

    public static RoundController getRoundController() {
        if (null == roundController) {
            try {
                roundController = SpringLiteContext.getBean(RoundController.class);
            } catch (Exception e) {
                throw new NulsRuntimeException(ConsensusErrorCode.FAILED);
            }
        }
        return roundController;
    }

    public static MeetingRound getCurrentRound() {
        if (getRoundController() == null) {
            return null;
        }
        return roundController.getCurrentRound();
    }


    /**
     * 查询两轮次之间新增的共识节点和注销的共识节点
     * New consensus nodes and unregistered consensus nodes between queries
     *
     * @param chain              chain
     * @param lastExtendsData    上一轮的轮次信息
     * @param currentExtendsData 本轮轮次信息
     * @return 两轮次之间节点变化信息
     */
    public static Map<String, List<String>> getAgentChangeInfo(Chain chain, BlockExtendsData lastExtendsData, BlockExtendsData currentExtendsData) {
        Map<String, List<String>> resultMap = new HashMap<>(2);
        List<String> registerAgentList;
        List<String> cancelAgentList;
        long lastRoundIndex = -1;
        if (lastExtendsData != null) {
            lastRoundIndex = lastExtendsData.getRoundIndex();
        }
        long currentRoundIndex = currentExtendsData.getRoundIndex();
        MeetingRound lastRound = null;
        MeetingRound currentRound;
        try {
            if (lastRoundIndex != -1) {
                lastRound = RoundUtils.getRoundController().getRoundByIndex(lastRoundIndex, lastExtendsData.getRoundStartTime());

                if (lastRound != null) {
                    lastRoundIndex = lastRound.getIndex();
                }

            }
            currentRound = RoundUtils.getRoundController().getRoundByIndex(currentRoundIndex, currentExtendsData.getRoundStartTime());

            registerAgentList = getAgentChangeList(lastRound, currentRound, true);
            cancelAgentList = getAgentChangeList(lastRound, currentRound, false);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
        chain.getLogger().debug("获取轮次间共识节点变更信息，lastRound:{},currentRound:{},registerList:{},cancelList{}", lastRoundIndex, currentRound.getIndex(), registerAgentList, cancelAgentList);
        resultMap.put("registerAgentList", registerAgentList);
        resultMap.put("cancelAgentList", cancelAgentList);
        return resultMap;
    }

    /**
     * 获取两轮次之间新增或减少的节点列表
     *
     * @param lastRound    上一轮
     * @param currentRound 本轮
     * @param isRegister   获取增加节点列表（true）或获取减少的节点列表（false）
     * @return 节点变化列表
     */
    private static List<String> getAgentChangeList(MeetingRound lastRound, MeetingRound currentRound, boolean isRegister) {
        List<String> lastRoundAgentList = new ArrayList<>();
        List<String> currentRoundAgentList = new ArrayList<>();
        if (lastRound != null) {
            for (MeetingMember member : lastRound.getMemberList()) {
                lastRoundAgentList.add(AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
            }
        }
        for (MeetingMember member : currentRound.getMemberList()) {
            currentRoundAgentList.add(AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress()));
        }
        if (isRegister) {
            currentRoundAgentList.removeAll(lastRoundAgentList);
            return currentRoundAgentList;
        } else {
            lastRoundAgentList.removeAll(currentRoundAgentList);
            return lastRoundAgentList;
        }
    }
}
