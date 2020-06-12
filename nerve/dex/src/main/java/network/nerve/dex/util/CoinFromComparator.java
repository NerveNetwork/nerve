package network.nerve.dex.util;

import io.nuls.base.data.CoinFrom;

import java.util.Comparator;

public class CoinFromComparator implements Comparator<CoinFrom> {

    private CoinFromComparator() {

    }

    private final static CoinFromComparator instance = new CoinFromComparator();

    public static CoinFromComparator getInstance() {
        return instance;
    }

    @Override
    public int compare(CoinFrom o1, CoinFrom o2) {
        if (o1.getAmount().compareTo(o2.getAmount()) < 0) {
            return -1;
        } else if (o1.getAmount().compareTo(o2.getAmount()) > 0) {
            return 1;
        }
        if (o1.getAddress().hashCode() < o2.getAddress().hashCode()) {
            return -1;
        } else if (o1.getAddress().hashCode() > o2.getAddress().hashCode()) {
            return 1;
        }
        return 0;
    }
}
