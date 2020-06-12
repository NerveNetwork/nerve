package network.nerve.pocbft.utils.thread;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.ConsensusAwardUtil;

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
