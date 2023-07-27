package io.nuls.consensus.utils.compare;
import io.nuls.consensus.model.po.nonce.NonceDataPo;

import java.util.Comparator;

public class NonceDataComparator implements Comparator<NonceDataPo> {
    @Override
    public int compare(NonceDataPo o1, NonceDataPo o2) {
        return o1.getDeposit().compareTo(o2.getDeposit());
    }
}
