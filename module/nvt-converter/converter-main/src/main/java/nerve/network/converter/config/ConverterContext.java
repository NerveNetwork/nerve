/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.config;

import java.math.BigInteger;

/**
 * @author: Chino
 * @date: 2020-03-02
 */
public class ConverterContext {
    /**
     * 触发执行虚拟银行变更交易的高度周期 配置
     * 按2秒一个块 大约1天的出块数量
     */
    public static long EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT = 30L * 60L * 24L;

    /**
     * 最近一次变更交易的确认高度
     * (如果没有变化也可能不会发出变更交易，但是检查的高度仍然会记录并计入下一周期的起点高度)
     *
     */
    public static long LATEST_EXECUTE_CHANGE_VIRTUAL_BANK_HEIGHT = 0L;

    /**
     * 手续费汇集分发公钥
     */
    public static byte[] FEE_PUBKEY = null;

    /**
     * 提现黑洞公钥
     */
    public static byte[] WITHDRAWAL_BLACKHOLE_PUBKEY = null;

    /**
     * 触发初始化虚拟银行的区块高度
     */
    public static long INIT_VIRTUAL_BANK_HEIGHT = 10;
    /**
     * 虚拟银行共识节点总数（非种子节点）
     */
    public static int VIRTUAL_BANK_AGENT_NUMBER = 10;

    /**
     * 网络第一次开启节点变更服务的节点总数阈值
     */
    public static int AGENT_COUNT_OF_ENABLE_VIRTUAL_BANK_CHANGES = 20;

    /**
     * 是否已经开启节点变更服务
     */
    public static boolean ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = false;

    //todo 配置文件中增加此字段 10个NVT
    public static BigInteger PROPOSAL_PRICE = BigInteger.valueOf(1000000000);
    /**
     * todo 提案超时时间
     */
    public static long PROPOSAL_TIMEOUT_SECONDS = 30*24*60*60L;

    /**
     * 提现交易补贴手续费
     */
    public static BigInteger WITHDRAWAL_DISTRIBUTION_FEE = new BigInteger("10000000000");
}
