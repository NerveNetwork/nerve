package network.nerve.pocbft.v1.award.po;

import io.nuls.base.data.CoinTo;

import java.util.List;

/**
 * @author Niels
 */
public class AwardItemPo {
    private long key;

    private long startHeight;

    private long endHeight;

    private List<CoinTo> tos;

    private int awaardedCount;



}
