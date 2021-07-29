package network.nerve.swap.utils;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class CoinDataMaker {
    private CoinData coinData = new CoinData();

    public CoinDataMaker addFrom(byte[] address, NerveToken token, BigInteger amount, byte[] nonce, byte locked) {
        CoinFrom from = new CoinFrom();
        from.setAddress(address);
        from.setAmount(amount);
        from.setNonce(nonce);
        from.setLocked(locked);
        from.setAssetsChainId(token.getChainId());
        from.setAssetsId(token.getAssetId());
        coinData.getFrom().add(from);
        return this;
    }

    public CoinDataMaker addTo(byte[] address, NerveToken token, BigInteger amount, long lockTime) {
        CoinTo to = new CoinTo();
        to.setAddress(address);
        to.setAssetsChainId(token.getChainId());
        to.setAssetsId(token.getAssetId());
        to.setAmount(amount);
        to.setLockTime(lockTime);
        coinData.getTo().add(to);
        return this;
    }

    public CoinData getCoinData() {
        return coinData;
    }
}
