package io.nuls.provider.model.form.consensus;

import io.nuls.provider.model.form.Base;

public class GetReduceNonceForm extends Base {
    private String agentHash;
    private String reduceAmount;
    private int quitAll;

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }

    public String getReduceAmount() {
        return reduceAmount;
    }

    public void setReduceAmount(String reduceAmount) {
        this.reduceAmount = reduceAmount;
    }

    public int getQuitAll() {
        return quitAll;
    }

    public void setQuitAll(int quitAll) {
        this.quitAll = quitAll;
    }
}
