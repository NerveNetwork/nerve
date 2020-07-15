package network.nerve.pocbft.v1.thread;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Block;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.ConsensusAwardUtil;
import network.nerve.pocbft.utils.manager.ConsensusManager;
import network.nerve.pocbft.v1.RoundController;
import network.nerve.pocbft.v1.entity.BasicRunnable;
import network.nerve.pocbft.v1.entity.PackingData;

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
            } catch (Exception e) {
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
        //投票
        chain.getConsensusCache().getBestBlocksVotingContainer().addBlock(chain,block.getHeader());

    }

}
