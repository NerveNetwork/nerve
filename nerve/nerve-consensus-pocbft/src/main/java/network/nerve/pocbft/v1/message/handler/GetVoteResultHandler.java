package network.nerve.pocbft.v1.message.handler;

import io.nuls.base.data.NulsHash;
import network.nerve.pocbft.v1.entity.VoteStageResult;
import network.nerve.pocbft.v1.entity.VoteSummaryData;
import network.nerve.pocbft.v1.message.GetVoteResultMessage;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.rpc.call.NetWorkCall;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.constant.CommandConstant;

import java.util.List;

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
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        GetVoteResultMessage getVoteResultMessage = RPCUtil.getInstanceRpcStr(message, GetVoteResultMessage.class);

        if (getVoteResultMessage == null) {
            return;
        }
//        chain.getLogger().info("收到获取投票结果的消息：{},来源：{}", getVoteResultMessage.getBlockHash().toHex(), nodeId);
        //从本地缓存中查询该投票结果信息并返回
        VoteResultMessage voteResultMessage = chain.getConsensusCache().getVoteResult(getVoteResultMessage.getBlockHash());
        if (null == voteResultMessage) {
            chain.getLogger().info("******没有获取到内容");
            return;
        }
//        chain.getLogger().info("******send-msg*size={},hash={},node={}", voteResultMessage.getSignList().size(), voteResultMessage.getBlockHash().toHex(), nodeId);
        NetWorkCall.sendToNode(chainId, voteResultMessage, nodeId, CommandConstant.MESSAGE_VOTE_RESULT);
    }
}
