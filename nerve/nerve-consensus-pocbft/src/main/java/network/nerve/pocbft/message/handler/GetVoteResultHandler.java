package network.nerve.pocbft.message.handler;

import network.nerve.pocbft.cache.VoteCache;
import network.nerve.pocbft.message.GetVoteResultMessage;
import network.nerve.pocbft.message.VoteResultMessage;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.vote.VoteResultData;
import network.nerve.pocbft.rpc.call.NetWorkCall;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.constant.ConsensusConstant;

import java.util.Map;

/**
 * 获取投票结果数据消息处理器
 * Get voting result data message processor
 *
 * @author tag
 * 2019/10/29
 */
@Component("GetVoteResultDataHandlerV1")
public class GetVoteResultHandler implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;
    @Override
    public String getCmd() {
        return CommandConstant.MESSAGE_GET_VOTE_RESULT;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        GetVoteResultMessage getVoteResultMessage = RPCUtil.getInstanceRpcStr(message, GetVoteResultMessage.class);
        if (getVoteResultMessage == null) {
            return;
        }
        chain.getLogger().debug("收到节点：{}向本节点获取指定投票轮次投票结果的消息，roundIndex:{},packingIndexOfRound:{},voteRound:{}" ,nodeId, getVoteResultMessage.getRoundIndex(),getVoteResultMessage.getPackingIndexOfRound(), getVoteResultMessage.getVoteRound());
        String consensusKey = getVoteResultMessage.getConsensusKey();
        Map<Byte, VoteResultData> voteResultDataMap = VoteCache.CONFIRMED_VOTE_RESULT_MAP.get(consensusKey);
        if(voteResultDataMap == null || voteResultDataMap.isEmpty()){
            chain.getLogger().warn("本地不存在指定轮次的投票结果缓存，{}",consensusKey);
            return;
        }
        VoteResultData resultData = voteResultDataMap.get(getVoteResultMessage.getVoteRound());
        if(resultData == null){
            resultData = VoteCache.CONFIRMED_VOTE_RESULT_MAP.get(getVoteResultMessage.getConsensusKey()).get(ConsensusConstant.FINAL_VOTE_ROUND_SIGN);
        }
        if (resultData == null){
            chain.getLogger().warn("本地不存在指定的投票结果，roundIndex:{},packingIndexOfRound:{},voteRound:{}", getVoteResultMessage.getRoundIndex(),getVoteResultMessage.getPackingIndexOfRound(), getVoteResultMessage.getVoteRound());
            return;
        }
        //从本地缓存中查询该投票结果信息并返回
        VoteResultMessage voteResultMessage = new VoteResultMessage();
        voteResultMessage.setVoteResultData(resultData);
        NetWorkCall.sendToNode(chainId, voteResultMessage, nodeId, CommandConstant.MESSAGE_VOTE_RESULT);
        chain.getLogger().debug("将投票结果返回给节点：{}，roundIndex:{},packingIndexOfRound:{},voteRound:{}" ,nodeId, getVoteResultMessage.getRoundIndex(),getVoteResultMessage.getPackingIndexOfRound(), getVoteResultMessage.getVoteRound());
    }
}
