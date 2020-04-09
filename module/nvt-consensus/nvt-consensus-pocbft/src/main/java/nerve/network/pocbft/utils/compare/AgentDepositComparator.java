package nerve.network.pocbft.utils.compare;

import nerve.network.pocbft.model.bo.tx.txdata.Agent;

import java.util.Comparator;

/**
 * 节点委托金额对比工具类
 * Node delegated amount comparison tool class
 *
 * @author: Jason
 * 2019/11/16
 */
public class AgentDepositComparator implements Comparator<Agent> {
    @Override
    public int compare(Agent o1, Agent o2) {
        //保证金从大到小排序
        int result = o2.getDeposit().compareTo(o1.getDeposit());
        if(result == 0){
            if (o1.getBlockHeight() == o2.getBlockHeight()) {
                return (int) (o1.getTime() - o2.getTime());
            }
            return (int) (o1.getBlockHeight() - o2.getBlockHeight());
        }
        return result;
    }
}
