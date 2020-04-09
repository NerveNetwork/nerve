package nerve.network.pocbft.utils.thread;

import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.utils.ConsensusAwardUtil;

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
