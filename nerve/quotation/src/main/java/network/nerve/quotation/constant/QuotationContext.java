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

    /** 统计报价时, 有效报价节点的数量*/
    public static int effectiveQuotation = 5;
     /**
     * 统计最终报价时, 去掉最高和最低 数据的数量
     * 如果为2 则去掉2个最高和2个最低，共计4条价格数据
     * */
     public static int removeMaxMinCount = 2;
    /**
     * 采集第三方价格时 去掉一个最高价 去掉一个最低价
     */
    public static int enquiryRemoveMaxMinCount = 1;
    /**
     * 记录当天无需再计算最终报价的token
     * 1. 记录当前已提供最终报价的token,以防2次报价
     * 2. 当天都不能再提供最终报价的token, 例如报价节点数不足，没有获取到价格等。
     */
    public static final Set<String> INTRADAY_NEED_NOT_QUOTE_TOKENS = new HashSet<>();

    /** 记录当天已成功组装到交易中的token, 未确认
     *  k: anchorToken, value: 交易hash
     * */
    public static final Map<String, String> NODE_QUOTED_TX_TOKENS_TEMP = new HashMap<>();

    /** 当天已成功组装到交易中的token, 并且已确认*/
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

    /**
     * 日志实例
     */
    private static NulsLogger logger;

    public static void setLogger(NulsLogger logger) {
        QuotationContext.logger = logger;
    }

    public static NulsLogger logger() {
        return logger;
    }
}
