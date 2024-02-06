package io.nuls.consensus.v1.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.v1.RoundController;

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
     * Query newly added consensus nodes and logged out consensus nodes between two rounds
     * New consensus nodes and unregistered consensus nodes between queries
     *
     * @param chain              chain
     * @param lastExtendsData    Previous round information
     * @param currentExtendsData Current round information
     * @return Node change information between two rounds
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
        chain.getLogger().debug("Obtain consensus node change information between rounds,lastRound:{},currentRound:{},registerList:{},cancelList{}", lastRoundIndex, currentRound.getIndex(), registerAgentList, cancelAgentList);
        resultMap.put("registerAgentList", registerAgentList);
        resultMap.put("cancelAgentList", cancelAgentList);
        return resultMap;
    }

    /**
     * Obtain a list of newly added or decreased nodes between two rounds
     *
     * @param lastRound    Previous round
     * @param currentRound This round
     * @param isRegister   Obtain the list of added nodes（true）Or obtain a list of reduced nodes（false）
     * @return Node Change List
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
