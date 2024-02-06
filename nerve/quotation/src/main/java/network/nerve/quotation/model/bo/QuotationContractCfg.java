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

package network.nerve.quotation.model.bo;

/**
 * @author: Loki
 * @date: 2020/12/7
 */
public class QuotationContractCfg {
    /**
     * Blockchain Network Name
     */
    private String chain;
    /**
     * Quotation when providing priceskey
     */
    private String key;
    /**
     * Feed price in transactionstoken
     */
    private String anchorToken;
    /**
     * swap tokenContract address
     */
    private String swapTokenContractAddress;
    /**
     * Benchmark for calculating pricestoken(Trading against one of themtokenAs a basis for calculation)
     */
    private String baseTokenContractAddress;
    /**
     * The benchmark for calculating pricestokenFeed ratekey(Used to obtain benchmarkstokenUnit price of)
     */
    private String baseAnchorToken;

    /** Access the corresponding networkapiAddress of */
    private String rpcAddress;

    /** Effective height of starting feeding*/
    private long effectiveHeight;

    private String calculator;

    private String tokenInfo;
    private String baseTokenInfo;

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain.toUpperCase();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSwapTokenContractAddress() {
        return swapTokenContractAddress;
    }

    public void setSwapTokenContractAddress(String swapTokenContractAddress) {
        this.swapTokenContractAddress = swapTokenContractAddress.toLowerCase();
    }

    public String getBaseTokenContractAddress() {
        return baseTokenContractAddress;
    }

    public void setBaseTokenContractAddress(String baseTokenContractAddress) {
        this.baseTokenContractAddress = baseTokenContractAddress.toLowerCase();
    }

    public String getBaseAnchorToken() {
        return baseAnchorToken;
    }

    public void setBaseAnchorToken(String baseAnchorToken) {
        this.baseAnchorToken = baseAnchorToken;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public String getAnchorToken() {
        return anchorToken;
    }

    public void setAnchorToken(String anchorToken) {
        this.anchorToken = anchorToken;
    }

    public long getEffectiveHeight() {
        return effectiveHeight;
    }

    public void setEffectiveHeight(long effectiveHeight) {
        this.effectiveHeight = effectiveHeight;
    }

    public String getCalculator() {
        return calculator;
    }

    public void setCalculator(String calculator) {
        this.calculator = calculator;
    }

    public String getTokenInfo() {
        return tokenInfo;
    }

    public void setTokenInfo(String tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public String getBaseTokenInfo() {
        return baseTokenInfo;
    }

    public void setBaseTokenInfo(String baseTokenInfo) {
        this.baseTokenInfo = baseTokenInfo;
    }
}
