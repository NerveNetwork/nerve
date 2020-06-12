package network.nerve.pocbft.utils.compare;

import network.nerve.pocbft.model.po.ChangeAgentDepositPo;

import java.util.Comparator;

/**
 * 追加/退出节点保证金数据比较器
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
