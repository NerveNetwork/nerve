package nerve.network.pocbft.message.handler;

import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import nerve.network.pocbft.cache.VoteCache;
import nerve.network.pocbft.constant.CommandConstant;
import nerve.network.pocbft.message.VoteResultMessage;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.utils.LoggerUtil;
import nerve.network.pocbft.utils.manager.ChainManager;

/**
 * 投票结果数据消息处理器
 * Voting result data message processor
 *
 * @author: Jason
 * 2019/10/29
 */
@Component("VoteResultDataHandlerV1")
public class VoteResultHandler implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;
    @Override
    public String getCmd() {
        return CommandConstant.MESSAGE_VOTE_RESULT;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        VoteResultMessage voteResultMessage = RPCUtil.getInstanceRpcStr(message, VoteResultMessage.class);
        if (voteResultMessage == null) {
            chain.getLogger().warn("收到节点：{}返回的投票结果为null");
            return;
        }
        chain.getLogger().debug("收到节点：{}返回的投票轮次投票结果的消息：{}", nodeId, voteResultMessage.getVoteRoundKey());
        voteResultMessage.setNodeId(nodeId);
        VoteCache.VOTE_RESULT_MESSAGE_QUEUE.offer(voteResultMessage);
    }
}
