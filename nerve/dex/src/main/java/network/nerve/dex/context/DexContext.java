package network.nerve.dex.context;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DexContext {
    //系统收取手续费地址
    public static byte[] sysFeeAddress;
    //系统收取手续费比例
    public static BigDecimal sysFeeScaleDecimal;
    //创建币对销毁Nerve数量
    public static BigInteger createTradingAmount;

    public static long skipHeight;

    public static long priceSkipHeight;

    public static long cancelConfirmSkipHeight;
}
