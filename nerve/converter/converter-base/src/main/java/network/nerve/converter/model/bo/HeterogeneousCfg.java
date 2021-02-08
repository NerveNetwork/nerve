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

import java.math.BigDecimal;

/**
 * 异构链配置
 * @author: Loki
 * @date: 2020-03-02
 */
public class HeterogeneousCfg {

    private int chainId;

    private int type;

    private String symbol;

    private int decimals;

//    private BigInteger withdrawalSignFeeNvt;
    /**
     *  多签地址
     */
    private String multySignAddress;
    /**
     *  节点启动时默认使用的API服务地址
     */
    private String commonRpcAddress;
    /**
     *  API服务地址
     */
    private String mainRpcAddress;
    /**
     *  默认起始同步高度
     */
    private long defaultStartHeight;
    /**
     *  交易确认高度数
     */
    private int txBlockConfirmations;
    /**
     *  提现交易确认高度数
     */
    private int txBlockConfirmationsOfWithdraw;
    /**
     *  每个虚拟银行节点发送异构链交易的间隔时间（单位：秒）
     */
    private String intervalWaittingSendTransaction;
    /**
     *  API服务地址列表(多个服务地址以逗号隔开，组成一个字符串)
     */
    private String orderRpcAddresses;
    /**
     *  API备用服务地址列表(多个备用服务地址以逗号隔开，组成一个字符串)
     */
    private String standbyRpcAddresses;

    /**
     * 异构链地址初始最小余额, 用于加入虚拟银行验证
     */
    private BigDecimal initialBalance;

    /**
     * 过滤掉的充值地址
     */
    private String filterAddresses;

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

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String getIntervalWaittingSendTransaction() {
        return intervalWaittingSendTransaction;
    }

    public void setIntervalWaittingSendTransaction(String intervalWaittingSendTransaction) {
        this.intervalWaittingSendTransaction = intervalWaittingSendTransaction;
    }

    public String getCommonRpcAddress() {
        return commonRpcAddress;
    }

    public void setCommonRpcAddress(String commonRpcAddress) {
        this.commonRpcAddress = commonRpcAddress;
    }

    public String getMultySignAddress() {
        return multySignAddress;
    }

    public void setMultySignAddress(String multySignAddress) {
        this.multySignAddress = multySignAddress;
    }

    public String getMainRpcAddress() {
        return mainRpcAddress;
    }

    public void setMainRpcAddress(String mainRpcAddress) {
        this.mainRpcAddress = mainRpcAddress;
    }

    public String getOrderRpcAddresses() {
        return orderRpcAddresses;
    }

    public void setOrderRpcAddresses(String orderRpcAddresses) {
        this.orderRpcAddresses = orderRpcAddresses;
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

    public int getTxBlockConfirmationsOfWithdraw() {
        return txBlockConfirmationsOfWithdraw;
    }

    public void setTxBlockConfirmationsOfWithdraw(int txBlockConfirmationsOfWithdraw) {
        this.txBlockConfirmationsOfWithdraw = txBlockConfirmationsOfWithdraw;
    }

    public String getStandbyRpcAddresses() {
        return standbyRpcAddresses;
    }

    public void setStandbyRpcAddresses(String standbyRpcAddresses) {
        this.standbyRpcAddresses = standbyRpcAddresses;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getFilterAddresses() {
        return filterAddresses;
    }

    public void setFilterAddresses(String filterAddresses) {
        this.filterAddresses = filterAddresses;
    }
}
