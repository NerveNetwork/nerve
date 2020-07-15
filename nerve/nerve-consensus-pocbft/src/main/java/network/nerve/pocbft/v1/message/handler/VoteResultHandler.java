package network.nerve.pocbft.v1.message.handler;

import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;

/**
 * 投票结果数据消息处理器
 * Voting result data message processor
 *
 * @author tag
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
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        VoteResultMessage voteResultMessage = RPCUtil.getInstanceRpcStr(message, VoteResultMessage.class);
        if (voteResultMessage == null || voteResultMessage.getSignList() == null || voteResultMessage.getSignList().isEmpty()) {
            chain.getLogger().warn("收到节点：{}返回的投票结果为null");
            return;
        }
        voteResultMessage.setNodeId(nodeId);
//        chain.getLogger().info("收到节点：{}，返回的投票记录，条数：" + voteResultMessage.getSignList().size());
        chain.getConsensusCache().getVoteResultQueue().offer(voteResultMessage);
    }
}
