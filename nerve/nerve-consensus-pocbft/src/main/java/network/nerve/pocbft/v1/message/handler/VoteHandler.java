package network.nerve.pocbft.v1.message.handler;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.utils.VoteMessageObjManager;


/**
 * 投票消息处理器
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
//            chain.getLogger().info("丢弃，本地不是共识节点");
            return;
        }
        VoteMessage message = VoteMessageObjManager.getInstance(msg);
        if (message == null) {
            chain.getLogger().info("丢弃，解析失败");
            message.clear();
            return;
        }
        if (message.getHeight() <= chain.getBestHeader().getHeight()) {
            //只处理下一个区块的投票
//            chain.getLogger().info("收到的高度不对：{},本地高度:{}", message.getHeight(), chain.getBestHeader().getHeight());
            message.clear();
            return;
        }
//        chain.getLogger().info("收到投票：{}-{}-{}-{}-{},当前高度：{}，blockhash:{},from:{}", message.getHeight(), message.getRoundIndex(), message.getPackingIndexOfRound(),
//                message.getVoteRoundIndex(), message.getVoteStage(), chain.getBestHeader().getHeight(),
//                message.getBlockHash().toHex(), message.getAddress(chain));
        message.setSendNode(nodeId);
        message.setRawData(msg);
        chain.getConsensusCache().getVoteMessageQueue().offer(message);
    }
}
