package io.nuls.consensus.v1.award;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.consensus.model.bo.Chain;

/**
 * @author Niels
 */
public class AwardProcessor extends BasicRunnable {

    public AwardProcessor(Chain chain) {
        super(chain);
    }

    @Override
    public void run() {
        this.init();
        while (this.running) {
            try {
                this.process();
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    private void init() {

    }

    private void process() throws InterruptedException {
        BlockHeader header = chain.getConsensusCache().getBlockHeaderQueue().take();
        //判断是否 需要计算共识奖励

        //计算共识奖励

        //存储计算结果
    }
}
