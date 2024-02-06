package io.nuls.consensus.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.model.DoubleUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.consensus.v1.entity.VoteStageResult;
import io.nuls.consensus.v1.message.VoteResultMessage;
import io.nuls.consensus.v1.utils.HashSetDuplicateProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Eva
 */
public class VoteResultProcessor extends BasicRunnable {

    private RoundController roundController;

    private HashSetDuplicateProcessor<NulsHash> duplicateProcessor = new HashSetDuplicateProcessor<>(1024);

    public VoteResultProcessor(Chain chain) {
        super(chain);
    }


    @Override
    public void run() {
        while (this.running) {
            try {
                if (null == roundController) {
                    this.roundController = SpringLiteContext.getBean(RoundController.class);
                }
                doit();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private void doit() throws Exception {

        VoteResultMessage resultMessage = chain.getConsensusCache().getVoteResultQueue().take();
        if (null == resultMessage) {
            return;
        }
        if (resultMessage.getSignList() == null || resultMessage.getSignList().isEmpty()) {
            return;
        }

        if (resultMessage.getHeight() != chain.getBestHeader().getHeight() + 1) {
            return;
        }
        if (resultMessage.getBlockHash() == null || resultMessage.getBlockHash().equals(NulsHash.EMPTY_NULS_HASH)) {
            return;
        }
        if (resultMessage.getVoteStage() != ConsensusConstant.VOTE_STAGE_TWO) {
            return;
        }
        Set<String> set = new HashSet<>();

        MeetingRound round = this.roundController.getRound(resultMessage.getRoundIndex(), resultMessage.getRoundStartTime());
        for (byte[] sign : resultMessage.getSignList()) {
            BlockSignature signature = new BlockSignature();
            signature.parse(sign, 0);
            byte[] addressBytes = AddressTool.getAddress(signature.getPublicKey(), chain.getChainId());
            String address = AddressTool.getStringAddressByBytes(addressBytes);
            if (!round.getMemberAddressSet().contains(address)) {
                log.info("==========The address is incorrect={}========", address);
                return;
            }
            boolean result = signature.verifySignature(resultMessage.getHash()).isSuccess();
            if (!result) {
                log.info("==========The signature is incorrect=from:{}========", resultMessage.getNodeId());
                return;
            }
            set.add(address);
        }
        double rate = DoubleUtils.div(set.size(), round.getMemberCount()) * 100;
        if (rate <= chain.getConfig().getByzantineRate()) {
            log.info("==========Insufficient number of signatures=={}%=from:{}========", rate, resultMessage.getNodeId());
            return;
        }

        if (!duplicateProcessor.insertAndCheck(resultMessage.getBlockHash())) {
            //Just one processing here is enough
            return;
        }

        chain.getConsensusCache().cacheSignResult(resultMessage);


        if (!chain.isConsonsusNode() || !chain.isNetworkStateOk() || !chain.isSynchronizedHeight()) {
            log.info("Notification block module, Byzantine verification passed：" + resultMessage.getHeight() + "-" + resultMessage.getBlockHash().toHex());
            CallMethodUtils.noticeByzantineResult(chain, resultMessage.getHeight(), false, resultMessage.getBlockHash(), null);
            return;
        }

        //If you haven't received it locally yet
        VoteStageResult result = new VoteStageResult();
        result.setResultMessage(resultMessage);
        result.setBlockHash(resultMessage.getBlockHash());
        result.setHeight(resultMessage.getHeight());
        result.setVoteRoundIndex(resultMessage.getVoteRoundIndex());
        result.setStage(ConsensusConstant.VOTE_STAGE_TWO);
        result.setRoundStartTime(resultMessage.getRoundStartTime());
        result.setRoundIndex(resultMessage.getRoundIndex());
        result.setPackingIndexOfRound(resultMessage.getPackingIndexOfRound());
        chain.getConsensusCache().getStageTwoQueue().offer(result);


    }

}
