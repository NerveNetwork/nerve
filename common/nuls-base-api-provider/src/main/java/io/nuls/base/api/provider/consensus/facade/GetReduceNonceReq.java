package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

public class GetReduceNonceReq  extends BaseReq {
    private String agentHash;
    private int quitAll;
    private String reduceAmount;

    public GetReduceNonceReq(String agentHash, int quitAll, String reduceAmount){
        this.agentHash = agentHash;
        this.quitAll = quitAll;
        this.reduceAmount = reduceAmount;
    }

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }

    public int getQuitAll() {
        return quitAll;
    }

    public void setQuitAll(int quitAll) {
        this.quitAll = quitAll;
    }

    public String getReduceAmount() {
        return reduceAmount;
    }

    public void setReduceAmount(String reduceAmount) {
        this.reduceAmount = reduceAmount;
    }
}
