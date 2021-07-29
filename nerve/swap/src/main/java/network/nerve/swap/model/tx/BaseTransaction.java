/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
package network.nerve.swap.model.tx;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * @author: PierreLuo
 * @date: 2019-03-07
 */
public abstract class BaseTransaction {

    private int txType;
    private CoinFrom coinFrom;
    private CoinTo coinTo;
    private List<CoinFrom> froms = new ArrayList<>();
    private List<CoinTo> tos = new ArrayList<>();
    private long time;
    private byte[] txData;
    private String remark;

    public BaseTransaction newFrom() {
        this.coinFrom = new CoinFrom();
        return this;
    }

    public BaseTransaction setFromAddress(byte[] address) {
        coinFrom.setAddress(address);
        return this;
    }

    public BaseTransaction setFromAssetsChainId(int assetsChainId) {
        coinFrom.setAssetsChainId(assetsChainId);
        return this;
    }

    public BaseTransaction setFromAssetsId(int assetsId) {
        coinFrom.setAssetsId(assetsId);
        return this;
    }

    public BaseTransaction setFromAmount(BigInteger amount) {
        coinFrom.setAmount(amount);
        return this;
    }

    public BaseTransaction setFrom(LedgerBalance balance, BigInteger amount) {
        if (balance.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("balance not enough");
        }
        coinFrom.setAddress(balance.getAddress());
        coinFrom.setAssetsChainId(balance.getAssetsChainId());
        coinFrom.setAssetsId(balance.getAssetsId());
        coinFrom.setNonce(balance.getNonce());
        coinFrom.setAmount(amount);
        return this;
    }

    public BaseTransaction setFromNonce(byte[] nonce) {
        coinFrom.setNonce(nonce);
        return this;
    }

    public BaseTransaction setFromNonce(LedgerTempBalanceManager tempBalanceManager) {
        LedgerBalance ledgerBalance = tempBalanceManager.getBalance(coinFrom.getAddress(), coinFrom.getAssetsChainId(), coinFrom.getAssetsId()).getData();
        coinFrom.setNonce(ledgerBalance.getNonce());
        return this;
    }

    public BaseTransaction setFromLocked(byte locked) {
        coinFrom.setLocked(locked);
        return this;
    }

    public BaseTransaction endFrom() {
        froms.add(coinFrom);
        coinFrom = null;
        return this;
    }

    public BaseTransaction newTo() {
        this.coinTo = new CoinTo();
        return this;
    }

    public BaseTransaction setToAddress(byte[] address) {
        coinTo.setAddress(address);
        return this;
    }

    public BaseTransaction setToAssetsChainId(int assetsChainId) {
        coinTo.setAssetsChainId(assetsChainId);
        return this;
    }

    public BaseTransaction setToAssetsId(int assetsId) {
        coinTo.setAssetsId(assetsId);
        return this;
    }

    public BaseTransaction setToAmount(BigInteger amount) {
        coinTo.setAmount(amount);
        return this;
    }

    public BaseTransaction setToLockTime(long lockTime) {
        coinTo.setLockTime(lockTime);
        return this;
    }

    public BaseTransaction endTo() {
        tos.add(coinTo);
        coinTo = null;
        return this;
    }

    public BaseTransaction setTime(long time) {
        this.time = time;
        return this;
    }

    protected void setTxData(byte[] txData) {
        this.txData = txData;
    }

    protected void setTxType(int txType) {
        this.txType = txType;
    }

    protected abstract BaseTransaction setTxData();

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Transaction build() {
        Transaction tx = new Transaction();
        tx.setType(txType);
        tx.setTime(time);
        tx.setTxData(txData);
        if (StringUtils.isNotBlank(remark)) {
            tx.setRemark(remark.getBytes(StandardCharsets.UTF_8));
        }
        CoinData coinData = new CoinData();
        if (!froms.isEmpty()) {
            coinData.getFrom().addAll(froms);
        }
        if (!tos.isEmpty()) {
            coinData.getTo().addAll(tos);
        }
        tx.setCoinData(SwapUtils.nulsData2HexBytes(coinData));
        return tx;
    }
}
