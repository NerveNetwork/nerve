package network.nerve.pocbft.v1.entity;

import io.nuls.base.data.BlockHeader;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;

import java.util.List;

/**
 * @author Eva
 */
public class RoundInitData {

    private long roundIndex;

    private long startTime;

    private List<Agent> agentList;

    //    当前轮次的前一轮的第一个区块头
    private BlockHeader startHeader;
    private long delayedSeconds;

    public BlockHeader getStartHeader() {
        return startHeader;
    }

    public void setStartHeader(BlockHeader startHeader) {
        this.startHeader = startHeader;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<Agent> getAgentList() {
        return agentList;
    }

    public void setAgentList(List<Agent> agentList) {
        this.agentList = agentList;
    }

    public void setDelayedSeconds(long delayedSeconds) {
        this.delayedSeconds = delayedSeconds;
    }

    public long getDelayedSeconds() {
        return delayedSeconds;
    }
}
