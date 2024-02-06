package network.nerve.dex.context;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DexContext {
    //Address for system fee collection
    public static byte[] sysFeeAddress;
    //System fee collection ratio
    public static BigDecimal sysFeeScaleDecimal;
    //Create a coin pair and destroy itNervequantity
    public static BigInteger createTradingAmount;

    public static long skipHeight;

    public static long priceSkipHeight;

    public static long cancelConfirmSkipHeight;
}
