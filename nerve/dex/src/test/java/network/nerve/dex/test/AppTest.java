package network.nerve.dex.test;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.model.txData.TradingOrder;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AppTest {

    @Test
    public void test() throws NulsException {
        String txData = "cee59fa71914bc1d96fb58197b6be796021b3f40e55cdd6e8df6ad6667a2943a0900013247d4a2637d96d16d891af4709f93822aaa4c1f010000e8890423c78a00000000000000000000000000000000000000000000000000e1f5050000000000000000000000000000000000000000000000000000000017090001b05ea40eab2ddb261857d8ba0dcfca6ca66fca4506";
        TradingOrder order = new TradingOrder();
        order.parse(new NulsByteBuffer(HexUtil.decode(txData)));

        NulsHash hash = new NulsHash(order.getTradingHash());
        System.out.println(hash.toHex());

        System.out.println(order.getPrice());
        System.out.println(order.getAmount());
        BigDecimal price = new BigDecimal(order.getPrice()).movePointLeft(8);
        BigDecimal amount = new BigDecimal(1000000000L);
        amount = amount.divide(price, 8, RoundingMode.DOWN);
        amount = amount.movePointRight(8);
        if (amount.toBigInteger().compareTo(order.getAmount()) < 0) {
            System.out.println(1);
        }
    }
}
