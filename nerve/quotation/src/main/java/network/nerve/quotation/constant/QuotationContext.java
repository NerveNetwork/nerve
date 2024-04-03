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

package network.nerve.quotation.constant;

import io.nuls.core.log.logback.NulsLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public class QuotationContext {

    public static int quoteStartH = 0;
    public static int quoteStartM = 0;
    public static int quoteEndH = 0;
    public static int quoteEndM = 10;

    /** When calculating quotations, The number of valid quotation nodes*/
    public static int effectiveQuotation = 5;
     /**
     * When compiling the final quotation, Remove the highest and lowest values The quantity of data
     * If it is2 Then remove it2Maximum and2Lowest, total4Price data
     * */
     public static int removeMaxMinCount = 2;
    /**
     * When collecting third-party prices Remove a maximum price Remove a minimum price
     */
    public static int enquiryRemoveMaxMinCount = 1;
    /**
     * Record that there is no need to calculate the final quotation on the same daytoken
     * 1. Record the final quotation currently providedtoken,in case2Secondary quotation
     * 2. We cannot provide the final quotation on the same daytoken, For example, insufficient number of quotation nodes and failure to obtain prices.
     */
    public static final Set<String> INTRADAY_NEED_NOT_QUOTE_TOKENS = new HashSet<>();

    /** Record the successful assembly into the transaction on that daytoken, Unconfirmed
     *  k: anchorToken, value: transactionhash
     * */
    public static final Map<String, String> NODE_QUOTED_TX_TOKENS_TEMP = new HashMap<>();

    /** Successfully assembled into the transaction on the same daytoken, And it has been confirmed that*/
    public static final Set<String> NODE_QUOTED_TX_TOKENS_CONFIRMED = new HashSet<>();

    public static long usdtDaiUsdcPaxKeyHeight = 0L;
    public static long bnbKeyHeight = 0L;
    public static long htOkbKeyHeight = 0L;
    public static long oktKeyHeight = 0L;
    public static long oneMaticKcsHeight = 0L;
    public static long trxKeyHeight = 0L;
    public static long protocol16Height = 0L;
    public static long protocol21Height = 0L;
    public static long protocol22Height = 0L;
    public static long protocol24Height = 0L;
    public static long protocol26Height = 0L;
    public static long protocol27Height = 0L;
    public static long protocol29Height = 0L;
    public static long protocol30Height = 0L;
    public static long protocol31Height = 0L;
    public static long protocol34Height = 0L;

    /**
     * Log instance
     */
    private static NulsLogger logger;

    public static void setLogger(NulsLogger logger) {
        QuotationContext.logger = logger;
    }

    public static NulsLogger logger() {
        return logger;
    }
}
