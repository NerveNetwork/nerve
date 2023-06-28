package network.nerve.pocbft.v1.utils;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.v1.entity.VoteStageResult;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;

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
            本地最新区块所在轮次已经打包结束，则轮次下标需要加1,则需找到本地最新区块轮次中出的第一个块来计算下一轮的轮次信息
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
     * 获取指定轮次前一轮打包的第一个区块
     * Gets the first block packaged in the previous round of the specified round
     *
     * @param chain      chain info
     * @param roundIndex 轮次下标
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
