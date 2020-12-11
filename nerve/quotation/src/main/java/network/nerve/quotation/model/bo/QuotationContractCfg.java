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
     * 区块链网络名称
     */
    private String chain;
    /**
     * 提供价格时的报价key
     */
    private String key;
    /**
     * 交易中的喂价token
     */
    private String anchorToken;
    /**
     * swap token合约地址
     */
    private String swapTokenContractAddress;
    /**
     * 计算价格时基准token(以交易对其中一个token作为计算依据)
     */
    private String baseTokenContractAddress;
    /**
     * 计算价格的基准token的喂价key(用于获取基准token的单价)
     */
    private String baseAnchorToken;

    /** 访问对应网络api的地址 */
    private String rpcAddress;

    /** 开始喂价生效高度*/
    private long effectiveHeight;

    private String calculator;

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
}
