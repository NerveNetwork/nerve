package network.nerve.pocbft.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.BlockSignature;
import network.nerve.pocbft.constant.CommandConstant;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.v1.entity.BasicRunnable;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.v1.VoteController;

import java.util.Arrays;

/**
 * 通过这里的处理，释放voteHandler的线程占用
 *
 * @author Eva
 */
public class VoteMsgProcessor extends BasicRunnable {
    private final VoteController controller;


    public VoteMsgProcessor(Chain chain, VoteController voteController) {
        super(chain);
        this.controller = voteController;
    }


    @Override
    public void run() {
        while (this.running) {
            doit();
        }
    }

    private void doit() {
        try {
            VoteMessage vote = chain.getConsensusCache().getVoteMessageQueue().take();
            if (!chain.isConsonsusNode()) {
                chain.getLogger().info("丢弃，本地不是共识节点");
                return;
            }
            if (vote.getSign() == null) {
                chain.getLogger().info("丢弃，没有签名");
                //签名为空
                return;
            }
            if (vote.getHeight() <= chain.getBestHeader().getHeight()) {
                return;
            }
            //本地投票
            if (vote.getSendNode() == null) {
                this.controller.addVote(vote);
                return;
            }
            BlockSignature signature = new BlockSignature();
            signature.parse(vote.getSign(), 0);
            byte[] addressBytes = AddressTool.getAddress(signature.getPublicKey(), chain.getChainId());
            vote.setAddress(AddressTool.getStringAddressByBytes(addressBytes));

            if (!chain.getConsensusCache().getMsgDuplicateProcessor().insertAndCheck(vote.getMessageKey())) {
                //重复投票
//                log.info("投票重复，丢弃：" + vote.getHeight() + "-{}-{}-{}:{},from:{}", vote.getRoundIndex(),
//                        vote.getPackingIndexOfRound(), vote.getVoteRoundIndex(), vote.getBlockHash().toHex(), vote.getAddress(chain));

                return;
            }

            if (!chain.getSeedAddressList().contains(vote.getAddress(chain))) {
                boolean result = false;
                for (Agent agent : chain.getAgentList()) {
                    if (agent.getDelHeight() > 0) {
                        continue;
                    }
                    if (Arrays.equals(agent.getPackingAddress(), addressBytes)) {
                        result = true;
                        break;
                    }
                }
                if (!result) {
                    //不接受非节点的投票
                    log.info("投票签名地址不是节点，丢弃：" + vote.getHeight() + "={},from:{}", vote.getBlockHash().toHex(), vote.getAddress(chain));
                    return;
                }
            }

            boolean result = signature.verifySignature(vote.getHash()).isSuccess();
            if (!result) {
                //签名不正确
                log.info("签名不正确，丢弃：" + vote.getHeight() + "={},from:{}", vote.getBlockHash().toHex(), vote.getAddress(chain));
                return;
            }
            //处理投票
            this.controller.addVote(vote);
            //广播收到的投票信息--异步
//            chain.getLogger().info("===========向其他节点传递投票消息");
            ConsensusNetUtil.broadcastInConsensusHalf(chain.getChainId(), CommandConstant.MESSAGE_VOTE, vote.getRawData(), vote.getSendNode());

        } catch (Exception e) {
            log.error(e);
        }
    }

}
