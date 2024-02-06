/*
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



package network.nerve.quotation;

import network.nerve.quotation.heterogeneouschain.BNBWalletApi;
import network.nerve.quotation.heterogeneouschain.ETHWalletApi;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Contract quotation testing
 *
 * @author: Loki
 * @date: 2020/12/3
 */
public class ContractQuotaionTest {

    //WETH-NVT:0x5A3De33Ae548aAe7abaA40Cc25A4edDFE4222b3B
    //WBNB-NVT:0xeE31a8Bb1edb26D8de688e2a1Ca086E5C11f5978
    public static final String ETH_UNI_V2_CONTRACT = "0x5a3de33ae548aae7abaa40cc25a4eddfe4222b3b";
    public static final String ETH_NVT_TOKEN_CONTRACT = "0x7b6f71c8b123b38aa8099e0098bec7fbc35b8a13";
    public static final String ETH_WETH_TOKEN_CONTRACT = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

    public static final String BSC_CAKE_LP_CONTRACT = "0xee31a8bb1edb26d8de688e2a1ca086e5c11f5978";
    public static final String BSC_NVT_TOKEN_CONTRACT = "0xf0e406c49c63abf358030a299c0e00118c4c6ba5";
    public static final String BSC_WETH_TOKEN_CONTRACT = "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c";

    private ETHWalletApi ethWalletApi = new ETHWalletApi();
    private BNBWalletApi bnbWalletApi = new BNBWalletApi();

    @Before
    public void before() {
//        EthContext.rpcAddress = "https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        ethWalletApi.init();
//        BnbContext.rpcAddress = "https://data-seed-prebsc-1-s1.binance.org:8545/";
        bnbWalletApi.init();
    }

    @Test
    public void netEth() throws Exception {
        // calculateETHUnder the network UNI_V2 The price of
//        System.out.println(ethWalletApi.getContractTokenDecimals(ETH_UNI_V2_CONTRACT));
//        System.out.println(ethWalletApi.getContractTokenDecimals(ETH_NVT_TOKEN_CONTRACT));
//        System.out.println(ethWalletApi.getContractTokenDecimals(ETH_WETH_TOKEN_CONTRACT));

        // Chi Zi LiETHQuantity of
        BigInteger wethBalance = ethWalletApi.getERC20Balance(ETH_UNI_V2_CONTRACT, ETH_WETH_TOKEN_CONTRACT);
        System.out.println("wethBalance:" + wethBalance);
        int wethDecimals = ethWalletApi.getContractTokenDecimals(ETH_WETH_TOKEN_CONTRACT);
        System.out.println("wethDecimals:" + wethDecimals);
        BigInteger wethTwice  = wethBalance.multiply(new BigInteger("2"));
        System.out.println("wethTwice:" + wethTwice);
        BigDecimal wethCount = new BigDecimal(wethTwice).movePointLeft(wethDecimals);
        System.out.println("wethCount:" + wethCount);
        BigDecimal wethPrice = wethCount.multiply(new BigDecimal("600"));
        System.out.println("wethPrice:" + wethPrice);
        System.out.println();
        BigInteger totalSupplyV2 = ethWalletApi.totalSupply(ETH_UNI_V2_CONTRACT);
        System.out.println("totalSupplyV2:" + totalSupplyV2);
        int v2Decimals = ethWalletApi.getContractTokenDecimals(ETH_UNI_V2_CONTRACT);
        System.out.println("V2Decimals:" + v2Decimals);
        BigDecimal v2Count =  new BigDecimal(totalSupplyV2).movePointLeft(v2Decimals);
        System.out.println("V2Count:" + v2Count);
        BigDecimal uniV2Price = wethPrice.divide(v2Count, 4, RoundingMode.DOWN);
        System.out.println("uniV2Price:" + uniV2Price);
    }


    @Test
    public void netBsc() throws Exception {
        // calculateETHUnder the network CAKE_LP The price of cakeLp

        // Chi Zi LiETHQuantity of
        BigInteger wethBalance = bnbWalletApi.getERC20Balance(BSC_CAKE_LP_CONTRACT, BSC_WETH_TOKEN_CONTRACT);
        System.out.println("wethBalance:" + wethBalance);
        int wethDecimals = bnbWalletApi.getContractTokenDecimals(BSC_WETH_TOKEN_CONTRACT);
        System.out.println("wethDecimals:" + wethDecimals);
        BigInteger wethTwice  = wethBalance.multiply(new BigInteger("2"));
        System.out.println("wethTwice:" + wethTwice);
        BigDecimal wethCount = new BigDecimal(wethTwice).movePointLeft(wethDecimals);
        System.out.println("wethCount:" + wethCount);
        BigDecimal wethPrice = wethCount.multiply(new BigDecimal("600"));
        System.out.println("wethPrice:" + wethPrice);
        System.out.println();
        BigInteger totalSupplyCakeLp = bnbWalletApi.totalSupply(BSC_CAKE_LP_CONTRACT);
        System.out.println("totalSupplyCakeLp:" + totalSupplyCakeLp);
        int cakeLpDecimals = bnbWalletApi.getContractTokenDecimals(BSC_CAKE_LP_CONTRACT);
        System.out.println("cakeLpDecimals:" + cakeLpDecimals);
        BigDecimal cakeLpCount =  new BigDecimal(totalSupplyCakeLp).movePointLeft(cakeLpDecimals);
        System.out.println("cakeLpCount:" + cakeLpCount);
        BigDecimal cakeLpPrice = wethPrice.divide(cakeLpCount, 4, RoundingMode.DOWN);
        System.out.println("cakeLpPrice:" + cakeLpPrice);
    }

}
