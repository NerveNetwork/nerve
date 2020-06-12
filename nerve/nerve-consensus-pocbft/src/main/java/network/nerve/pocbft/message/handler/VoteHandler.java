package network.nerve.pocbft.message.handler;
import network.nerve.pocbft.cache.VoteCache;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.enumeration.VoteTime;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.manager.RoundManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.message.VoteMessage;

import java.io.IOException;


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
    @Autowired
    private RoundManager roundManager;
    @Override
    public String getCmd() {
        return CommandConstant.MESSAGE_VOTE;
    }

    @Override
    public void process(int chainId, String nodeId, String msg) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist");
            return;
        }
        VoteMessage message = RPCUtil.getInstanceRpcStr(msg, VoteMessage.class);
        if (message == null) {
            return;
        }

        //如果当前投票轮次未空则表示当前节点刚启动需要根据其他节点的投票信息来初始化投票轮次
        if(VoteCache.CURRENT_BLOCK_VOTE_DATA == null && chain.isNetworkState()){
            try {
                if(chain.isCanPacking() && chain.isPacker() && message.getHeight() == chain.getNewestHeader().getHeight() + 1){
                    chain.getLogger().info("收到其他节点广播的投票消息，初始化本地投票信息，messageKey:{}", message.getMessageKey());
                    MeetingRound round = roundManager.getRound(chain, message.getRoundIndex(), message.getRoundStartTime());
                    if(chain.isCanPacking() && round.getMyMember() != null){
                        roundManager.addRound(chain, round);
                        VoteCache.initCurrentVoteRound(chain, round.getMemberCount(), message);
                    }
                }else{
                    return;
                }
            }catch (Exception e){
                chain.getLogger().error(e);
                return;
            }
        }

        VoteTime voteTime;
        try {
            voteTime = VoteCache.CURRENT_BLOCK_VOTE_DATA.voteTime(message);
        }catch (NullPointerException e){
            return;
        }

        //收到之前的投票信息直接忽略
        if(voteTime == VoteTime.PREVIOUS){
            return;
        }
        // 验证签名
        BlockSignature signature = new BlockSignature();
        try {
            if(message.getSign() == null){
                return;
            }
            signature.parse(message.getSign(), 0);
            if (signature.verifySignature(message.getVoteHash()).isFailed()) {
                chain.getLogger().error("Voting signature verification failed");
                return;
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return;
        } catch (IOException e) {
            chain.getLogger().error(e);
            return;
        }

        message.setAddress(AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chainId)));
        message.setSendNode(nodeId);

        if(voteTime == VoteTime.CURRENT_STAGE_ONE){
            boolean isRepeatMessage = VoteCache.CURRENT_BLOCK_VOTE_DATA.isRepeatMessage(message.getVoteRound(), message.getVoteStage(), message.getAddress(chain));
            //判断是否收到过该消息，如果收到过则直接返回
            if(isRepeatMessage){
               return;
            }
            VoteCache.CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.offer(message);
        }else if(voteTime == VoteTime.CURRENT_STAGE_TWO){
            boolean isRepeatMessage = VoteCache.CURRENT_BLOCK_VOTE_DATA.isRepeatMessage(message.getVoteRound(), message.getVoteStage(), message.getAddress(chain));
            //判断是否收到过该消息，如果收到过则直接返回
            if(isRepeatMessage){
                return;
            }
            VoteCache.CURRENT_ROUND_STAGE_TOW_MESSAGE_QUEUE.offer(message);
        }else{
            String consensusKey = message.getConsensusKey();
            //如果为当前确认区块之后轮次的投票信息
            if(consensusKey.equals(VoteCache.CURRENT_BLOCK_VOTE_DATA.getConsensusKey())){
                VoteCache.CURRENT_BLOCK_VOTE_DATA.addVoteMessage(chain, message, nodeId);
            }else{
                VoteCache.addFutureCache(chain, message, nodeId);
            }
        }
    }
}
