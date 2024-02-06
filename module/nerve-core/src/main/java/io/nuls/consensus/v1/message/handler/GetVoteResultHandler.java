package io.nuls.consensus.v1.message.handler;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.rpc.call.NetWorkCall;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.v1.message.GetVoteResultMessage;
import io.nuls.consensus.v1.message.VoteResultMessage;
import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.consensus.constant.CommandConstant;

/**
 * Get voting result data message processor
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
//        chain.getLogger().info("Received message to obtain voting results：{},source：{}", getVoteResultMessage.getBlockHash().toHex(), nodeId);
        //Query the voting result information from the local cache and return it
        VoteResultMessage voteResultMessage = chain.getConsensusCache().getVoteResult(getVoteResultMessage.getBlockHash());
        if (null == voteResultMessage) {
            chain.getLogger().info("******No content obtained");
            return;
        }
//        chain.getLogger().info("******send-msg*size={},hash={},node={}", voteResultMessage.getSignList().size(), voteResultMessage.getBlockHash().toHex(), nodeId);
        NetWorkCall.sendToNode(chainId, voteResultMessage, nodeId, CommandConstant.MESSAGE_VOTE_RESULT);
    }
}
