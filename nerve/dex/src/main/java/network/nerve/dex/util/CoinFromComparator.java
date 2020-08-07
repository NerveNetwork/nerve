package network.nerve.dex.util;

import io.nuls.base.data.CoinFrom;

import java.util.Arrays;
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
        int i = Arrays.compare(o1.getNonce(), o2.getNonce());
        if (i != 0) {
            return i;
        }
       return Arrays.compare(o1.getAddress(), o2.getAddress());
    }
}
