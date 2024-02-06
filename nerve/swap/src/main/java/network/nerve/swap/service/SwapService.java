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
     * @param address Trading account
     * @param password Account password
     * @param tokenA assetAtype
     * @param tokenB assetBtype
     * @return
     */
    Result<String> swapCreatePair(int chainId, String address, String password, String tokenA, String tokenB);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amountA Added assetsAquantity
     * @param amountB Added assetsBquantity
     * @param tokenA assetAtype
     * @param tokenB assetBtype
     * @param amountAMin assetAMinimum added value
     * @param amountBMin assetBMinimum added value
     * @param deadline Expiration time
     * @param to Liquidity share receiving address
     * @return
     */
    Result<String> swapAddLiquidity(int chainId, String address, String password,
                         BigInteger amountA, BigInteger amountB,
                         String tokenA, String tokenB,
                         BigInteger amountAMin, BigInteger amountBMin,
                         long deadline, String to);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amountLP LPAsset quantity
     * @param tokenLP LPTypes of assets
     * @param tokenA assetAtype
     * @param tokenB assetBtype
     * @param amountAMin assetAMinimum removal value
     * @param amountBMin assetBMinimum removal value
     * @param deadline Expiration time
     * @param to Asset receiving address
     * @return
     */
    Result<String> swapRemoveLiquidity(int chainId, String address, String password,
                        BigInteger amountLP, String tokenLP, String tokenA, String tokenB,
                        BigInteger amountAMin, BigInteger amountBMin,
                        long deadline, String to);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amountIn Number of assets sold
     * @param amountOutMin Minimum number of assets to be purchased
     * @param tokenPath Currency exchange asset path, the last asset in the path is the asset that the user wants to buy, such as sellingAbuyB: [A, B] or [A, C, B]
     * @param feeTo Withdraw a portion of the transaction fee to the designated receiving address
     * @param deadline Expiration time
     * @param to Asset receiving address
     * @return
     */
    Result<String> swapTokenTrade(int chainId, String address, String password,
              BigInteger amountIn, BigInteger amountOutMin, String[] tokenPath, String feeTo,
              long deadline, String to);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param coins Create an asset array for transaction pairs
     * @param symbol LPname
     * @return
     */
    Result<String> stableSwapCreatePair(int chainId, String address, String password, String[] coins, String symbol);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amounts List of added asset quantities
     * @param tokens List of added asset types
     * @param pairAddress Transaction to address
     * @param deadline Expiration time
     * @param to Liquidity share receiving address
     * @return
     */
    Result<String> stableSwapAddLiquidity(int chainId, String address, String password,
                           BigInteger[] amounts,
                           String[] tokens,
                           String pairAddress,
                           long deadline, String to);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amountLP LPAsset quantity
     * @param tokenLP LPAsset type
     * @param receiveOrderIndexs Receive assets in currency index order
     * @param pairAddress Transaction to address
     * @param deadline Expiration time
     * @param to Asset receiving address
     * @return
     */
    Result<String> stableSwapRemoveLiquidity(int chainId, String address, String password,
                              BigInteger amountLP, String tokenLP, Integer[] receiveOrderIndexs,
                              String pairAddress,
                              long deadline, String to);

    /**
     * @param chainId
     * @param address Trading account
     * @param password Account password
     * @param amountsIn List of sold assets
     * @param tokensIn List of asset types sold
     * @param tokenOutIndex Index of purchased assets
     * @param feeTo Transaction fee receiving address
     * @param pairAddress Transaction to address
     * @param deadline Expired address
     * @param to Asset receiving address
     * @param feeTokenStr Handling fee asset type
     * @param feeAmountStr Transaction fees
     * @return
     */
    Result<String> stableSwapTokenTrade(int chainId, String address, String password,
                                        BigInteger[] amountsIn,
                                        String[] tokensIn,
                                        int tokenOutIndex, String feeTo,
                                        String pairAddress,
                                        long deadline, String to, String feeTokenStr, String feeAmountStr);

    /**
     * according toLPAsset acquisition transaction against address
     */
    Result<String> getPairAddressByTokenLP(int chainId, String tokenLP);

}
