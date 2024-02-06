package io.nuls.consensus.v1.thread;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Block;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.manager.ConsensusManager;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.consensus.v1.entity.PackingData;

/**
 * @author Eva
 */
public class PackingProcessor extends BasicRunnable {
    private final RoundController roundController;

    public PackingProcessor(Chain chain, RoundController roundController) {
        super(chain);
        this.roundController = roundController;
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
    }

    private void doit() throws Exception {

        PackingData result = chain.getConsensusCache().getPackingQueue().take();
        if (result == null) {
            return;
        }

        Block block;
        try {
            long time = result.getPackStartTime();
            boolean settleConsensusAward = ConsensusAwardUtil.settleConsensusAward(chain, time);
//            log.info("=================start make endtime:{}", NulsDateUtils.timeStamp2Str(result.getPackStartTime() * 1000));
            block = ConsensusManager.doPacking(chain, result.getMember(), result.getRound(), time, settleConsensusAward);
//            log.info("=================end make endtime,hash:{}", block.getHeader().getHash());
            if (block == null) {
                return;
            }
            CallMethodUtils.receivePackingBlock(chain.getConfig().getChainId(), RPCUtil.encode(block.serialize()));
        } catch (Exception e) {
            chain.getLogger().error("Packing exception");
            chain.getLogger().error(e);
            return;
        }
        //vote
        chain.getConsensusCache().getBestBlocksVotingContainer().addBlock(chain,block.getHeader());

    }

}
