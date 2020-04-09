package nerve.network.pocbft.utils.thread;
import io.nuls.base.RPCUtil;
import nerve.network.pocbft.cache.VoteCache;
import nerve.network.pocbft.constant.CommandConstant;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.message.VoteMessage;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.utils.ConsensusNetUtil;
import nerve.network.pocbft.utils.manager.VoteManager;

import java.util.concurrent.TimeUnit;

/**
 * 第二阶段投票收集器
 * Second stage voting collector
 *
 * @author: Jason
 * 2019/10/31
 */
public class StageOneVoteCollector implements Runnable {
    private Chain chain;
    public StageOneVoteCollector(Chain chain){
        this.chain = chain;
    }

    @Override
    public void run() {
        while (true) {
            try {
                VoteMessage message = VoteCache.CURRENT_ROUND_STAGE_ONE_MESSAGE_QUEUE.take();
                //轮次切换中不处理投票信息
                while (VoteCache.VOTE_HANDOVER){
                    TimeUnit.MILLISECONDS.sleep(50);
                }
                if(VoteManager.statisticalResult(chain, message, ConsensusConstant.VOTE_STAGE_ONE) && !message.isLocal()) {
                    //广播收到的投票信息
                    ConsensusNetUtil.broadcastInConsensus(chain.getChainId(), CommandConstant.MESSAGE_VOTE, RPCUtil.encode(message.serialize()), message.getSendNode());
                }
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }
}
