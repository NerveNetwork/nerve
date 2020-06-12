package network.nerve.pocbft.utils.thread;

import network.nerve.pocbft.cache.VoteCache;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.vote.VoteResultData;
import network.nerve.pocbft.model.bo.vote.VoteResultItem;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.message.VoteResultMessage;
import network.nerve.pocbft.utils.manager.RoundManager;
import network.nerve.pocbft.utils.manager.VoteManager;

/**
 * 投票结果处理器
 * Voting result processor
 * @author tag
 * */
public class VoteResultProcessor implements Runnable{
    private Chain chain;
    public VoteResultProcessor(Chain chain){
        this.chain = chain;
    }
    private RoundManager roundManager = SpringLiteContext.getBean(RoundManager.class);
    @Override
    public void run() {
        while (true){
            try {
                VoteResultMessage voteResultMessage = VoteCache.VOTE_RESULT_MESSAGE_QUEUE.take();
                //验证投票结果正确性
                VoteResultData voteResultData = voteResultMessage.getVoteResultData();
                VoteResultItem voteInfo = voteResultData.getVoteResultItem();
                if(voteInfo.getVoteStage() == ConsensusConstant.VOTE_STAGE_ONE){
                    chain.getLogger().warn("Receive the first stage voting result information");
                    continue;
                }

                if(VoteCache.CURRENT_BLOCK_VOTE_DATA == null && voteInfo.getHeight() <= chain.getNewestHeader().getHeight()){
                    chain.getLogger().warn("The voting result of this block has been verified in this node");
                    continue;
                }

                /*
                * 当上一轮投票超时是，CONFIRMED_VOTE_RESULT_MAP不会存储上一轮的投票结果，如果存在上一轮的投票结果，则表示上一轮投票在本节点已确认
                * */
                String consensusKey = voteInfo.getConsensusKey();
                boolean isPreviousResult = VoteCache.CURRENT_BLOCK_VOTE_DATA != null && (voteInfo.getRoundIndex() < VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex()
                        || (voteInfo.getRoundIndex() == VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && voteInfo.getPackingIndexOfRound() < VoteCache.CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound())
                        || (voteInfo.getRoundIndex() == VoteCache.CURRENT_BLOCK_VOTE_DATA.getRoundIndex() && voteInfo.getPackingIndexOfRound() == VoteCache.CURRENT_BLOCK_VOTE_DATA.getPackingIndexOfRound()
                            && voteInfo.getVoteRound() < VoteCache.CURRENT_BLOCK_VOTE_DATA.getVoteRound()
                            && VoteCache.CONFIRMED_VOTE_RESULT_MAP.get(consensusKey) != null && VoteCache.CONFIRMED_VOTE_RESULT_MAP.get(consensusKey).get(voteInfo.getVoteRound()) != null));
                if(isPreviousResult){
                    chain.getLogger().warn("It's the result of the previous round");
                    continue;
                }
                /*
                * 接收投票结果又两种方式
                * 1.接收到新区块，首先会进行基础验证，基础验证中会生成并验证轮次，如果验证不通过不会像其他节点获取区块拜占庭验证结果
                * 2.向其他节点获取到本轮次投票结果
                * 以上两种情况都是轮次已经生成验证过的，所以如果接收到轮次不存在的投票结果直接丢弃
                * */
                MeetingRound round = roundManager.getRoundByIndex(chain, voteInfo.getRoundIndex());
                if(round == null){
                    if(voteInfo.getRoundIndex() >= chain.getNewestHeader().getExtendsData().getRoundIndex()){
                        round = roundManager.getRound(chain, voteInfo.getRoundIndex(), voteInfo.getRoundStartTime());
                    }else{
                        chain.getLogger().warn("The voting result round does not exist,roundIndex:{}",voteInfo.getRoundIndex());
                        continue;
                    }
                }
                voteResultData.setResultSuccess(voteResultData.getVoteResultItemList().size() == 1);
                if(voteResultData.isResultSuccess()){
                    voteResultData.setConfirmedEmpty(NulsHash.EMPTY_NULS_HASH.equals(voteInfo.getBlockHash()));
                }
                VoteManager.verifyVoteResult(chain, voteResultData, round);
            }catch (Exception e){
                chain.getLogger().error(e);
            }
        }

    }
}
