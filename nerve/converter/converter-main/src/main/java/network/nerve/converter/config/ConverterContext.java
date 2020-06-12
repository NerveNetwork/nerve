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

package network.nerve.converter.config;

import network.nerve.converter.constant.ConverterConstant;

import java.math.BigInteger;

/**
 * 默认配置项
 * @author: Loki
 * @date: 2020-03-02
 */
public class ConverterContext {
    /**
     * 触发执行虚拟银行变更交易的高度周期 配置
     * 按2秒一个块 大约1天的出块数量
     */
    public static long EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT = ConverterConstant.DAY_BLOCKS;

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
    public static int VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED = 10;

    /**
     * 网络第一次开启节点变更服务的节点总数阈值 默认种子节点的个数
     */
    public static int INITIAL_VIRTUAL_BANK_COUNT = 5;

    /**
     * 是否已经开启节点变更服务
     */
    public static boolean ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = false;

    /**
     * 发提案费用
     */
    public static BigInteger PROPOSAL_PRICE = BigInteger.valueOf(1000000000);
    /**
     * 提案投票时长对应的区块高度
     * 2秒一块 一天出块数为 43,200‬
     * 默认投票时长约10天, 出块数为: 43200 * 10
     */
    public static long PROPOSAL_VOTE_TIME_BLOCKS = ConverterConstant.DAY_BLOCKS * 10L;

    /**
     * 交易补贴手续费
     */
    public static BigInteger DISTRIBUTION_FEE = new BigInteger("10000000000");

    /**
     * 签名拜占庭比例
     */
    public static int BYZANTINERATIO = 66;
}
