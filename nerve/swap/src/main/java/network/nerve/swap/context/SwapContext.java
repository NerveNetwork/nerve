/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

import java.math.BigInteger;

import static network.nerve.swap.constant.SwapConstant.BI_50;

/**
 * 默认配置项
 * @author: Loki
 * @date: 2020-03-02
 */
public class SwapContext {

    public static long LATEST_BLOCK_HEIGHT = 0L;
    /**
     * 模块协议生效高度
     */
    public static long PROTOCOL_UPGRADE_HEIGHT = 0L;
    /**
     * 黑洞公钥
     */
    public static byte[] BLACKHOLE_PUBKEY;
    /**
     * 黑洞地址
     */
    public static byte[] BLACKHOLE_ADDRESS;

    public static NulsLogger logger;
    
    /**
     * 手续费奖励的系统接收地址
     */
    public static byte[] AWARD_FEE_SYSTEM_ADDRESS;
    /**
     * 手续费奖励的销毁地址
     */
    public static byte[] AWARD_FEE_DESTRUCTION_ADDRESS;

    /**
     * （恒定币交易）手续费收取的千分比例
     */
    public static BigInteger FEE_PERMILLAGE_STABLE_SWAP = BigInteger.ZERO;
    /**
     * （恒定币交易）手续费分配给`非`流动性提供者的百分比例（分配给系统地址和交易中指定的接收地址，剩余部分，则分配给流动性提供者）
     */
    public static BigInteger FEE_PERCENT_ALLOCATION_UN_LIQUIDIDY_STABLE_SWAP = BigInteger.ZERO;
    /**
     * （恒定币交易）系统接收地址-获得的手续费百分比例（剩余部分，则分配给交易组装者指定接收地址）
     */
    public static BigInteger FEE_PERCENT_SYSTEM_RECEIVE_STABLE_SWAP = BI_50;

    /**
     * v1.15.0 协议升级高度
     */
    public static long PROTOCOL_1_15_0 = 0L;
}
