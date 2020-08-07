package network.nerve.dex.util;

import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;

import java.util.Arrays;
import java.util.Comparator;

public class CoinToComparator implements Comparator<CoinTo> {

    private CoinToComparator() {

    }

    private final static CoinToComparator instance = new CoinToComparator();

    public static CoinToComparator getInstance() {
        return instance;
    }

    @Override
    public int compare(CoinTo o1, CoinTo o2) {
        if (o1.getAmount().compareTo(o2.getAmount()) < 0) {
            return -1;
        } else if (o1.getAmount().compareTo(o2.getAmount()) > 0) {
            return 1;
        }
        if (o1.getAssetsChainId() < o2.getAssetsChainId()) {
            return -1;
        } else if (o1.getAssetsChainId() > o2.getAssetsChainId()) {
            return 1;
        }
        if (o1.getAssetsId() < o2.getAssetsId()) {
            return -1;
        } else if (o1.getAssetsId() > o2.getAssetsId()) {
            return 1;
        }
        if (o1.getLockTime() < o2.getLockTime()) {
            return -1;
        } else if (o1.getLockTime() > o2.getLockTime()) {
            return 1;
        }
        return Arrays.compare(o1.getAddress(), o2.getAddress());
    }
}
