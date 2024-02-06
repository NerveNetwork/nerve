package io.nuls.consensus.v1.message.handler;

import io.nuls.base.protocol.MessageProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.v1.utils.VoteMessageObjManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.consensus.constant.CommandConstant;
import io.nuls.consensus.v1.message.VoteMessage;


/**
 * Voting Message Processor
 * Voting message processor
 *
 * @author tag
 * 2019/10/28
 */
@Component("VoteHandlerV1")
public class VoteHandler implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;

    @Override
    public String getCmd() {
        return CommandConstant.MESSAGE_VOTE;
    }

    @Override
    public void process(int chainId, String nodeId, String msg) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        if (!chain.isConsonsusNode() || !chain.isSynchronizedHeight() || !chain.isNetworkStateOk()) {
//            chain.getLogger().info("Discard, local is not a consensus node");
            return;
        }
        VoteMessage message = VoteMessageObjManager.getInstance(chain,msg);
        if (message == null) {
            chain.getLogger().info("Discard, parsing failed");
            message.clear();
            return;
        }
        if (message.getHeight() <= chain.getBestHeader().getHeight()) {
            //Only handle voting for the next block
//            chain.getLogger().info("The received height is incorrect：{},Local height:{}", message.getHeight(), chain.getBestHeader().getHeight());
            message.clear();
            return;
        }
//        chain.getLogger().info("Received vote：{}-{}-{}-{}-{},Current height：{},blockhash:{},from:{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(),
//                message.getVoteRoundIndex(), message.getVoteStage(), chain.getBestHeader().getHeight(),
//                message.getBlockHash().toHex(), message.getAddress(chain));
        message.setSendNode(nodeId);
        message.setRawData(msg);
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }
}
