package io.nuls.consensus.v1.message.handler;

import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.consensus.constant.CommandConstant;
import io.nuls.consensus.v1.message.VoteResultMessage;

/**
 * Voting result data message processor
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
            chain.getLogger().warn("Received node：{}The returned voting result isnull");
            return;
        }
        voteResultMessage.setNodeId(nodeId);
//        chain.getLogger().info("Received node：{}Returned voting records, number of entries：" + voteResultMessage.getSignList().size());
        chain.getConsensusCache().getVoteResultQueue().offer(voteResultMessage);
    }
}
