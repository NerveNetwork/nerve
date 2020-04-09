package io.nuls.dex.context;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DexContext {
    //手续费收取比例
    public static BigDecimal feePropDecimal;
    //销毁地址
    public static byte[] feeAddress;
    //创建币对销毁Nerve数量
    public static BigInteger createTradingAmount;
}
