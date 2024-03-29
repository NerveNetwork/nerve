package io.nuls.consensus.utils.compare;

import io.nuls.consensus.model.po.ChangeAgentDepositPo;

import java.util.Comparator;

/**
 * Add/Exit node margin data comparator
 * Node Contrast Tool Class
 *
 * @author tag
 * 2019/10/23
 */
public class ChangeDepositComparator implements Comparator<ChangeAgentDepositPo> {

    @Override
    public int compare(ChangeAgentDepositPo o1, ChangeAgentDepositPo o2) {
        if (o1.getBlockHeight() == o2.getBlockHeight()) {
            return (int) (o1.getTime() - o2.getTime());
        }
        return (int) (o1.getBlockHeight() - o2.getBlockHeight());
    }
}
