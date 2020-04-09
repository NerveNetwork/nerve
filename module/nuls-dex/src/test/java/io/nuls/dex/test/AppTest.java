package io.nuls.dex.test;

import io.nuls.base.data.Address;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.manager.TradingContainer;
import io.nuls.dex.model.po.TradingOrderPo;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */

    @Test
    public void testTradingContainer() {
        String hashs = "584ae3c9af9a42c4e68fcde0736fce670a913262346ed10f827dfaef75714ebd";
        String address = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
        Address address1 = new Address(address);
        NulsHash hasha = new NulsHash(Hex.decode(hashs));

        TradingContainer container = new TradingContainer();
        LinkedList<TradingOrderPo> buyList = new LinkedList<>();
        container.setBuyOrderList(buyList);
        LinkedList<TradingOrderPo> sellList = new LinkedList<>();
        container.setSellOrderList(sellList);

        Random random = new Random();
        TradingOrderPo po1;
        for (int i = 0; i < 561; i++) {
            po1 = new TradingOrderPo();
            int r = random.nextInt(100);
            po1.setType(DexConstant.TRADING_ORDER_BUY_TYPE);
            po1.setPrice(BigInteger.valueOf(r));
            po1.setAddress(address1.getAddressBytes());
            po1.setAmount(BigInteger.valueOf(i));
            po1.setDealAmount(BigInteger.ZERO);
            po1.setTradingHash(hasha);
            try {
                NulsHash hash = NulsHash.calcHash(po1.serialize());
                po1.setOrderHash(hash);

            } catch (IOException e) {
                e.printStackTrace();
            }
            container.addTradingOrder(po1);
        }
        int size = container.getBuyOrderList().size();
        System.out.println("--------------------------  size:" + size);


        for (int i = 0; i < size; i++) {
            int index = random.nextInt(container.getBuyOrderList().size());
            po1 = container.getBuyOrderList().get(index);

            container.removeTradingOrder(po1);
        }
        System.out.println("--------------------------  size:" + container.getBuyOrderList().size());
    }


    @Test
    public void testTradingContainerUpdate() {
        String hashs = "584ae3c9af9a42c4e68fcde0736fce670a913262346ed10f827dfaef75714ebd";
        String address = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
        Address address1 = new Address(address);
        NulsHash hasha = new NulsHash(Hex.decode(hashs));

        TradingContainer container = new TradingContainer();
        LinkedList<TradingOrderPo> buyList = new LinkedList<>();
        container.setBuyOrderList(buyList);
        LinkedList<TradingOrderPo> sellList = new LinkedList<>();
        container.setSellOrderList(sellList);

        Random random = new Random();
        TradingOrderPo po1;
        for (int i = 0; i < 561; i++) {
            po1 = new TradingOrderPo();
            int r = random.nextInt(100);
            po1.setType(DexConstant.TRADING_ORDER_SELL_TYPE);
            po1.setPrice(BigInteger.valueOf(r));
            po1.setAddress(address1.getAddressBytes());
            po1.setAmount(BigInteger.valueOf(i));
            po1.setDealAmount(BigInteger.ZERO);
            po1.setTradingHash(hasha);
            try {
                NulsHash hash = NulsHash.calcHash(po1.serialize());
                po1.setOrderHash(hash);
            } catch (IOException e) {
                e.printStackTrace();
            }
            container.addTradingOrder(po1);
        }
        int size = container.getSellOrderList().size();
        System.out.println("--------------------------  size:" + size);


        for (int i = 0; i < size; i++) {
            po1 = container.getSellOrderList().get(i).copy();
            po1.setDealAmount(BigInteger.TEN);
            try {
                container.updateTradingOrder(po1);
            } catch (NulsException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < size; i++) {
            po1 = container.getSellOrderList().get(i);
            if (po1.getDealAmount().intValue() != 10) {
                System.out.println(po1.getDealAmount());
            }

        }
        System.out.println("--------------------------  size:" + container.getSellOrderList().size());
    }

    @Test
    public void test() {
//        BigDecimal b = new BigDecimal("71783191279121972");
//        NumberFormat n = new DecimalFormat("00.##########");
//        System.out.println(n.format(b.movePointLeft(8)));
//        BigInteger b = new BigInteger("000010000");
//        System.out.println(b);
//        Map<String,Object> map = new HashMap<>();
//        map.put("a","a");
//        System.out.println(map);

        BigInteger a = BigInteger.valueOf(1000000000);
        BigInteger b = BigInteger.valueOf(20000000000L);
        BigDecimal rate = new BigDecimal(a).divide(new BigDecimal(b),8, RoundingMode.HALF_DOWN);
        System.out.println(rate);
    }
}
