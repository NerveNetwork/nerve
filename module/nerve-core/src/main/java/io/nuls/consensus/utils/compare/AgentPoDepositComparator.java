package io.nuls.consensus.utils.compare;

import io.nuls.consensus.model.po.AgentPo;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 节点委托金额对比工具类
 * Node delegated amount comparison tool class
 *
 * @author tag
 * 2019/11/16
 */
public class AgentPoDepositComparator implements Comparator<AgentPo> {

    private static final AgentPoDepositComparator INSTANCE = new AgentPoDepositComparator();

    public static AgentPoDepositComparator getInstance() {
        return INSTANCE;
    }


    @Override
    public int compare(AgentPo o1, AgentPo o2) {
        //保证金从大到小排序
        int result = o2.getDeposit().compareTo(o1.getDeposit());
        if (result != 0) {
            return result;
        }
        if (o1.getBlockHeight() != o2.getBlockHeight()) {
            return (int) (o1.getBlockHeight() - o2.getBlockHeight());
        }
        result = (int) (o1.getTime() - o2.getTime());
        if (result != 0) {
            return result;
        }
        return Arrays.compare(o1.getHash().getBytes(), o2.getHash().getBytes());
    }
}
