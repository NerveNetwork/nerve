/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.swap.context;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.model.bo.StableCoinGroup;

import java.math.BigDecimal;
import java.math.BigInteger;

import static network.nerve.swap.constant.SwapConstant.BI_50;

/**
 * Default configuration items
 * @author: Loki
 * @date: 2020-03-02
 */
public class SwapContext {

    public static long LATEST_BLOCK_HEIGHT = 0L;
    /**
     * Effective height of module protocol
     */
    public static long PROTOCOL_UPGRADE_HEIGHT = 0L;
    /**
     * Black hole public key
     */
    public static byte[] BLACKHOLE_PUBKEY;
    /**
     * Black hole address
     */
    public static byte[] BLACKHOLE_ADDRESS;

    public static NulsLogger logger;
    
    /**
     * System receiving address for handling fee rewards
     */
    public static byte[] AWARD_FEE_SYSTEM_ADDRESS;
    /**
     * System receiving address for handling fee rewards(protocol17take effect)
     */
    public static byte[] AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0;
    /**
     * Destruction address for handling fee rewards
     */
    public static byte[] AWARD_FEE_DESTRUCTION_ADDRESS;

    /**
     * （Constant currency trading）The percentage of handling fees charged in thousands
     */
    public static BigInteger FEE_PERMILLAGE_STABLE_SWAP = BigInteger.ZERO;
    /**
     * （Constant currency trading）Allocation of handling fees to`wrong`Percentage of liquidity providers（Allocate to the system address and the designated receiving address in the transaction, and allocate the remaining portion to the liquidity provider）
     */
    public static BigInteger FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP = BigInteger.ZERO;
    /**
     * （Constant currency trading）System receiving address-Percentage of transaction fees obtained（The remaining portion will be allocated to the designated receiving address of the transaction assembler）
     */
    public static BigInteger FEE_PERCENT_SYSTEM_RECEIVE_STABLE_SWAP = BI_50;
    /**
     * （Constant currency trading）The maximum percentage of handling fees charged（protocol21take effect）
     */
    public static BigDecimal FEE_MAX_PERCENT_STABLE_SWAP = BigDecimal.valueOf(5);

    /**
     * v1.15.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_15_0 = 0L;
    public static long PROTOCOL_1_16_0 = 0L;
    public static long PROTOCOL_1_17_0 = 0L;
    public static long PROTOCOL_1_21_0 = 0L;
    public static long PROTOCOL_1_22_0 = 0L;
    public static long PROTOCOL_1_24_0 = 0L;
    public static long PROTOCOL_1_26_0 = 0L;
    public static long PROTOCOL_1_28_0 = 0L;
    public static long PROTOCOL_1_29_0 = 0L;
    public static long PROTOCOL_1_31_0 = 0L;
    public static long PROTOCOL_1_35_0 = 0L;
    public static long PROTOCOL_1_36_0 = 0L;
    public static String PASSWORD;

    public static StableCoinGroup stableCoinGroup = new StableCoinGroup();
}
