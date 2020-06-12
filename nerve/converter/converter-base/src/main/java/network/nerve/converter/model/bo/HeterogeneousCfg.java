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

package network.nerve.converter.model.bo;

/**
 * 异构链配置
 * @author: Loki
 * @date: 2020-03-02
 */
public class HeterogeneousCfg {

    private int chainId;

    private int type;

    private String symbol;

//    private BigInteger withdrawalSignFeeNvt;
    /**
     *  多签地址
     */
    private String multySignAddress;
    /**
     *  API服务地址
     */
    private String rpcAddress;
    /**
     *  默认起始同步高度
     */
    private long defaultStartHeight;
    /**
     *  交易确认高度数
     */
    private int txBlockConfirmations;
    /**
     *  API备用服务地址列表(多个备用服务地址以逗号隔开，组成一个字符串)
     */
    private String standbyRpcAddresses;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

//    public BigInteger getWithdrawalSignFeeNvt() {
//        return withdrawalSignFeeNvt;
//    }
//
//    public void setWithdrawalSignFeeNvt(BigInteger withdrawalSignFeeNvt) {
//        this.withdrawalSignFeeNvt = withdrawalSignFeeNvt;
//    }
//
//    public void setWithdrawalSignFeeNvt(double withdrawalSignFeeNvt) {
//        this.withdrawalSignFeeNvt = new BigDecimal(withdrawalSignFeeNvt).movePointRight(8).toBigInteger();
//    }

    public String getMultySignAddress() {
        return multySignAddress;
    }

    public void setMultySignAddress(String multySignAddress) {
        this.multySignAddress = multySignAddress;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public long getDefaultStartHeight() {
        return defaultStartHeight;
    }

    public void setDefaultStartHeight(long defaultStartHeight) {
        this.defaultStartHeight = defaultStartHeight;
    }

    public int getTxBlockConfirmations() {
        return txBlockConfirmations;
    }

    public void setTxBlockConfirmations(int txBlockConfirmations) {
        this.txBlockConfirmations = txBlockConfirmations;
    }

    public String getStandbyRpcAddresses() {
        return standbyRpcAddresses;
    }

    public void setStandbyRpcAddresses(String standbyRpcAddresses) {
        this.standbyRpcAddresses = standbyRpcAddresses;
    }
}
