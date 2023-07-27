package io.nuls.consensus.utils.thread;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.ConsensusAwardUtil;

public class BudgetConsensusAwardTask implements Runnable{
    private Chain chain;

    public BudgetConsensusAwardTask(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        ConsensusAwardUtil.budgetConsensusAward(chain);
    }
}
