package io.nuls.consensus.utils.compare;

import io.nuls.consensus.model.bo.tx.txdata.Agent;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Node commission amount comparison tool class
 * Node delegated amount comparison tool class
 *
 * @author tag
 * 2019/11/16
 */
public class AgentDepositComparator implements Comparator<Agent> {
    @Override
    public int compare(Agent o1, Agent o2) {
        //Ranking of margin from highest to lowest
        int result = o2.getDeposit().compareTo(o1.getDeposit());
        if(result != 0){
            return result;
        }
        if (o1.getBlockHeight() != o2.getBlockHeight()) {
            return (int) (o1.getBlockHeight() - o2.getBlockHeight());
        }
        result =  (int) (o1.getTime() - o2.getTime());
        if(result!=0){
            return result;
        }
        return Arrays.compare(o1.getTxHash().getBytes(),o2.getTxHash().getBytes());
    }
}
