/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
 * Heterogeneous Chain Configuration
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
     *  Multiple signed addresses
     */
    private String multySignAddress;
    /**
     *  The default usage for node startupAPIService address
     */
    private String commonRpcAddress;
    /**
     *  APIService address
     */
    private String mainRpcAddress;
    /**
     *  Default starting synchronization height
     */
    private long defaultStartHeight;
    /**
     *  Transaction confirmation height
     */
    private int txBlockConfirmations;
    /**
     *  Confirmation height of withdrawal transactions
     */
    private int txBlockConfirmationsOfWithdraw;
    /**
     *  The interval time for each virtual bank node to send heterogeneous chain transactions（unit：second）
     */
    private String intervalWaittingSendTransaction;
    /**
     *  APIService Address List(Multiple service addresses separated by commas form a string)
     */
    private String orderRpcAddresses;
    /**
     *  APIList of backup service addresses(Multiple backup service addresses separated by commas form a string)
     */
    private String standbyRpcAddresses;

    /**
     * Initial minimum balance of heterogeneous chain addresses, Used to join virtual bank verification
     */
    private BigDecimal initialBalance;

    /**
     * Filtered recharge addresses
     */
    private String filterAddresses;

    /**
     * The execution cycle of the task queue
     */
    private int blockQueuePeriod;
    private int confirmTxQueuePeriod;
    private int waitingTxQueuePeriod;

    /**
     * Within heterogeneous chain networkschainId
     */
    private long chainIdOnHtgNetwork;
    /**
     * Effective height of the agreement
     */
    private int protocolVersion;
    /**
     * priceKEY
     */
    private String priceKey;
    private boolean proxy;
    private String httpProxy;

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public String getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(String httpProxy) {
        this.httpProxy = httpProxy;
    }

    public String getPriceKey() {
        return priceKey;
    }

    public void setPriceKey(String priceKey) {
        this.priceKey = priceKey;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public long getChainIdOnHtgNetwork() {
        return chainIdOnHtgNetwork;
    }

    public void setChainIdOnHtgNetwork(long chainIdOnHtgNetwork) {
        this.chainIdOnHtgNetwork = chainIdOnHtgNetwork;
    }

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

    public int getBlockQueuePeriod() {
        return blockQueuePeriod;
    }

    public void setBlockQueuePeriod(int blockQueuePeriod) {
        this.blockQueuePeriod = blockQueuePeriod;
    }

    public int getConfirmTxQueuePeriod() {
        return confirmTxQueuePeriod;
    }

    public void setConfirmTxQueuePeriod(int confirmTxQueuePeriod) {
        this.confirmTxQueuePeriod = confirmTxQueuePeriod;
    }

    public int getWaitingTxQueuePeriod() {
        return waitingTxQueuePeriod;
    }

    public void setWaitingTxQueuePeriod(int waitingTxQueuePeriod) {
        this.waitingTxQueuePeriod = waitingTxQueuePeriod;
    }

    public String getFilterAddresses() {
        return filterAddresses;
    }

    public void setFilterAddresses(String filterAddresses) {
        this.filterAddresses = filterAddresses;
    }
}
