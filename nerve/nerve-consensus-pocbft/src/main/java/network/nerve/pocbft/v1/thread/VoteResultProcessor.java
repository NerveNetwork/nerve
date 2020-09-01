package network.nerve.pocbft.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.v1.RoundController;
import network.nerve.pocbft.v1.VoteController;
import network.nerve.pocbft.v1.entity.BasicRunnable;
import network.nerve.pocbft.v1.entity.VoteStageResult;
import network.nerve.pocbft.v1.entity.VoteSummaryData;
import network.nerve.pocbft.v1.message.VoteMessage;
import network.nerve.pocbft.v1.message.VoteResultMessage;
import network.nerve.pocbft.v1.utils.HashSetDuplicateProcessor;

import java.util.Arrays;
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
                log.info("==========地址不对=from:{}========", resultMessage.getNodeId());
                return;
            }
            boolean result = signature.verifySignature(resultMessage.getHash()).isSuccess();
            if (!result) {
                log.info("==========签名不对=from:{}========", resultMessage.getNodeId());
                return;
            }
            set.add(address);
        }
        double rate = DoubleUtils.div(set.size(), round.getMemberCount()) * 100;
        if (rate <= chain.getConfig().getByzantineRate()) {
            log.info("==========签名数量不足=={}%=from:{}========", rate, resultMessage.getNodeId());
            return;
        }

        if (!duplicateProcessor.insertAndCheck(resultMessage.getBlockHash())) {
            //这里只处理一次就够了
            return;
        }

        chain.getConsensusCache().cacheSignResult(resultMessage);


        if (!chain.isConsonsusNode() || !chain.isNetworkStateOk() || !chain.isSynchronizedHeight()) {
            log.info("通知区块模块，拜占庭验证通过：" + resultMessage.getHeight() + "-" + resultMessage.getBlockHash().toHex());
            CallMethodUtils.noticeByzantineResult(chain, resultMessage.getHeight(), false, resultMessage.getBlockHash(), null);
            return;
        }

        //如果本地还没有收到
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
