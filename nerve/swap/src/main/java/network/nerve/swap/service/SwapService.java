/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.swap.service;

import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
public interface SwapService {

    Result begin(int chainId, long blockHeight, long blockTime, String packingAddress);

    Result invokeOneByOne(int chainId, long blockHeight, long blockTime, Transaction tx);

    Result end(int chainId, long blockHeight);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param tokenA 资产A类型
     * @param tokenB 资产B类型
     * @return
     */
    Result<String> swapCreatePair(int chainId, String address, String password, String tokenA, String tokenB);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amountA 添加的资产A数量
     * @param amountB 添加的资产B数量
     * @param tokenA 资产A类型
     * @param tokenB 资产B类型
     * @param amountAMin 资产A最小添加值
     * @param amountBMin 资产B最小添加值
     * @param deadline 过期时间
     * @param to 流动性份额接收地址
     * @return
     */
    Result<String> swapAddLiquidity(int chainId, String address, String password,
                         BigInteger amountA, BigInteger amountB,
                         String tokenA, String tokenB,
                         BigInteger amountAMin, BigInteger amountBMin,
                         long deadline, String to);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amountLP LP资产数量
     * @param tokenLP LP资产的类型
     * @param tokenA 资产A类型
     * @param tokenB 资产B类型
     * @param amountAMin 资产A最小移除值
     * @param amountBMin 资产B最小移除值
     * @param deadline 过期时间
     * @param to 资产接收地址
     * @return
     */
    Result<String> swapRemoveLiquidity(int chainId, String address, String password,
                        BigInteger amountLP, String tokenLP, String tokenA, String tokenB,
                        BigInteger amountAMin, BigInteger amountBMin,
                        long deadline, String to);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amountIn 卖出的资产数量
     * @param amountOutMin 最小买进的资产数量
     * @param tokenPath 币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]
     * @param feeTo 交易手续费取出一部分给指定的接收地址
     * @param deadline 过期时间
     * @param to 资产接收地址
     * @return
     */
    Result<String> swapTokenTrade(int chainId, String address, String password,
              BigInteger amountIn, BigInteger amountOutMin, String[] tokenPath, String feeTo,
              long deadline, String to);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param coins 创建交易对的资产数组
     * @param symbol LP名称
     * @return
     */
    Result<String> stableSwapCreatePair(int chainId, String address, String password, String[] coins, String symbol);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amounts 添加的资产数量列表
     * @param tokens 添加的资产类型列表
     * @param pairAddress 交易对地址
     * @param deadline 过期时间
     * @param to 流动性份额接收地址
     * @return
     */
    Result<String> stableSwapAddLiquidity(int chainId, String address, String password,
                           BigInteger[] amounts,
                           String[] tokens,
                           String pairAddress,
                           long deadline, String to);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amountLP LP资产数量
     * @param tokenLP LP资产类型
     * @param receiveOrderIndexs 按币种索引顺序接收资产
     * @param pairAddress 交易对地址
     * @param deadline 过期时间
     * @param to 资产接收地址
     * @return
     */
    Result<String> stableSwapRemoveLiquidity(int chainId, String address, String password,
                              BigInteger amountLP, String tokenLP, Integer[] receiveOrderIndexs,
                              String pairAddress,
                              long deadline, String to);

    /**
     * @param chainId
     * @param address 交易账户
     * @param password 账户密码
     * @param amountsIn 卖出的资产数量列表
     * @param tokensIn 卖出的资产类型列表
     * @param tokenOutIndex 买进的资产索引
     * @param feeTo 交易手续费接收地址
     * @param pairAddress 交易对地址
     * @param deadline 过期地址
     * @param to 资产接收地址
     * @param feeTokenStr 手续费资产类型
     * @param feeAmountStr 交易手续费
     * @return
     */
    Result<String> stableSwapTokenTrade(int chainId, String address, String password,
                                        BigInteger[] amountsIn,
                                        String[] tokensIn,
                                        int tokenOutIndex, String feeTo,
                                        String pairAddress,
                                        long deadline, String to, String feeTokenStr, String feeAmountStr);

    /**
     * 根据LP资产获取交易对地址
     */
    Result<String> getPairAddressByTokenLP(int chainId, String tokenLP);

}
