package io.nuls.consensus.v1.utils;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.v1.message.VoteMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class CsUtils {

    public static VoteMessage createStageTwoVoteMessage(Chain chain, String address, VoteStageResult result) {
        VoteMessage message = new VoteMessage();
        message.setHeight(result.getHeight());
        message.setRoundIndex(result.getRoundIndex());
        message.setPackingIndexOfRound(result.getPackingIndexOfRound());
        message.setRoundStartTime(result.getRoundStartTime());
        message.setVoteRoundIndex(result.getVoteRoundIndex());
        message.setVoteStage(ConsensusConstant.VOTE_STAGE_TWO);
        message.setBlockHash(result.getBlockHash());
        byte[] sign;
        try {
            sign = CallMethodUtils.signature(chain, address, message.getHash().getBytes(), Map.of("voteMessage", HexUtil.encode(message.serialize()), "method", "voteMsgSign"));
        } catch (IOException | NulsException e) {
            LoggerUtil.commonLog.error(e);
            return null;
        }
        message.setSign(sign);
        return message;
    }

    public static BlockHeader GetRoundStartHeader(Chain chain, BlockHeader startBlockHeader) {
        BlockExtendsData bestRoundData = startBlockHeader.getExtendsData();
        long bestRoundEndTime = bestRoundData.getRoundEndTime(chain.getConfig().getPackingInterval());
        if (startBlockHeader.getHeight() != 0L) {
            long roundIndex = bestRoundData.getRoundIndex();
            /*
            If the round of packaging for the latest local block has ended, the round index needs to be added1,Then it is necessary to find the first block from the latest local block round to calculate the next round information
            If the latest block in this area has been packaged, the subscription of the round will need to be added 1.
            */
            if (bestRoundData.getConsensusMemberCount() == bestRoundData.getPackingIndexOfRound() || NulsDateUtils.getCurrentTimeSeconds() >= bestRoundEndTime) {
                roundIndex += 1;
            }
            startBlockHeader = GetFirstBlockOfPreRound(chain, roundIndex);
        }
        return startBlockHeader;
    }


    /**
     * Get the first block packaged in the previous round of the specified round
     * Gets the first block packaged in the previous round of the specified round
     *
     * @param chain      chain info
     * @param roundIndex Round index
     */
    public static BlockHeader GetFirstBlockOfPreRound(Chain chain, long roundIndex) {
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
            firstBlockHeader = chain.getBestHeader();
        }
        return firstBlockHeader;
    }
}
