package io.nuls.consensus.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.constant.CommandConstant;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.consensus.v1.message.VoteMessage;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.v1.VoteController;

import java.util.Arrays;

/**
 * Through the processing here, releasevoteHandlerThread occupancy of
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
            try {
                doit();
            } catch (Throwable e) {
                log.error(e);
            }
        }
        chain.getLogger().warn("Voting Stop Processing!!!!");
    }

    private void doit() throws Exception {
        VoteMessage vote = chain.getConsensusCache().getVoteMessageQueue().take();
        if (!chain.isConsonsusNode()) {
            chain.getLogger().info("Discard, local is not a consensus node");
            vote.clear();
            return;
        }
        if (vote.getSign() == null) {
            chain.getLogger().info("Discard, no signature");
            //Signature is empty
            vote.clear();
            return;
        }
        if (vote.getHeight() <= chain.getBestHeader().getHeight()) {
            vote.clear();
            return;
        }
        //Local voting
        if (vote.getSendNode() == null) {
            this.controller.addVote(vote);
            chain.getConsensusCache().getMsgDuplicateProcessor().insertAndCheck(vote.getMessageKey());
            vote.clear();
            return;
        }
        BlockSignature signature = new BlockSignature();
        signature.parse(vote.getSign(), 0);
        byte[] addressBytes = AddressTool.getAddress(signature.getPublicKey(), chain.getChainId());
        vote.setAddress(AddressTool.getStringAddressByBytes(addressBytes));

        if (!chain.getConsensusCache().getMsgDuplicateProcessor().insertAndCheck(vote.getMessageKey())) {
            //Repeated voting
//                log.info("Voting duplicate, discard：" + vote.getHeight() + "-{}-{}-{}:{},from:{}", vote.getRoundIndex(),
//                        vote.getPackingIndexOfRound(), vote.getVoteRoundIndex(), vote.getBlockHash().toHex(), vote.getAddress(chain));
            vote.clear();
            return;
        }

        if (vote.getRoundIndex() < chain.getConsensusCache().getLastConfirmedRoundIndex() ||
                (vote.getRoundIndex() == chain.getConsensusCache().getLastConfirmedRoundIndex() && vote.getPackingIndexOfRound() < chain.getConsensusCache().getLastConfirmedRoundPackingIndex())) {
            vote.clear();
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
                //Do not accept voting from non nodes
                log.info("Voting signature address is not a node, discard：" + vote.getHeight() + "={},from:{}", vote.getBlockHash().toHex(), vote.getAddress(chain));
                vote.clear();
                return;
            }
        }

        boolean result = signature.verifySignature(vote.getHash()).isSuccess();
        if (!result) {
            //Incorrect signature
            log.info("Incorrect signature, discard：" + vote.getHeight() + "={},from:{},val={}", vote.getBlockHash().toHex(), vote.getAddress(chain), signature.getSignData().toString());
            vote.clear();
            return;
        }
        //Process voting
        this.controller.addVote(vote);

        if (vote.getRoundStartTime() + chain.getConfig().getPackingInterval() * vote.getPackingIndexOfRound() < NulsDateUtils.getCurrentTimeSeconds() - 120) {
            chain.getLogger().info("===========Do not forward messages anymore,Current queue：{}", chain.getConsensusCache().getVoteMessageQueue().size());
            vote.clear();
            return;
        }

        //Broadcast received voting information--asynchronous
//            chain.getLogger().info("===========Passing voting messages to other nodes");
        ConsensusNetUtil.broadcastInConsensusHalf(chain.getChainId(), CommandConstant.MESSAGE_VOTE, vote.getRawData(), vote.getSendNode());
        vote.clear();
    }

}
