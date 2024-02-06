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
package network.nerve.converter.heterogeneouschain.bnb.context;

import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.GWEI_10;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class BnbContext extends HtgContext implements Serializable {

    public final int HTG_CHAIN_ID = 102;
    public final int HTG_ASSET_ID = 1;
    public byte VERSION = 2;
    public IHeterogeneousChainDocking DOCKING;
    /**
     * When importAccountByPriKey perhaps importAccountByKeystore When called, overwrite this address as the virtual bank administrator address
     */
    public String ADMIN_ADDRESS;
    public String ADMIN_ADDRESS_PASSWORD;
    public String ADMIN_ADDRESS_PUBLIC_KEY;

    /**
     * Pending confirmation transaction queue
     */
    public LinkedBlockingDeque<HtgUnconfirmedTxPo> UNCONFIRMED_TX_QUEUE = new LinkedBlockingDeque<>();
    public CountDownLatch INIT_UNCONFIRMEDTX_QUEUE_LATCH = new CountDownLatch(1);

    // Initialize configuration address
    public Set<String> FILTER_ACCOUNT_SET = new HashSet<>();

    /**
     * Waiting for the transaction queue, the current node saves the call parameters for the transaction（The transaction is sent out by a certain administrator, and the administrator who ranks first in the order of the administrator sends out the transaction. If the sending fails or is not sent out, the transaction is sent out in the next order, and so on）
     */
    public LinkedBlockingDeque<HtgWaitingTxPo> WAITING_TX_QUEUE = new LinkedBlockingDeque<>();
    public CountDownLatch INIT_WAITING_TX_QUEUE_LATCH = new CountDownLatch(1);

    public int NERVE_CHAINID;
    public String MULTY_SIGN_ADDRESS;
    public List<String> RPC_ADDRESS_LIST = new ArrayList<>();
    public List<String> STANDBY_RPC_ADDRESS_LIST = new ArrayList<>();
    /**
     * Log instance
     */
    public NulsLogger logger;
    public HeterogeneousCfg config;
    public IConverterCoreApi converterCoreApi;

    public Integer intervalWaitting;

    public void setConverterCoreApi(IConverterCoreApi converterCoreApi) {
        this.converterCoreApi = converterCoreApi;
    }

    public void setConfig(HeterogeneousCfg config) {
        this.config = config;
    }

    public int getIntervalWaitting() {
        if (intervalWaitting != null) {
            return intervalWaitting;
        }
        String interval = config.getIntervalWaittingSendTransaction();
        if (StringUtils.isNotBlank(interval)) {
            intervalWaitting = Integer.parseInt(interval);
        } else {
            intervalWaitting = HtgConstant.DEFAULT_INTERVAL_WAITTING;
        }
        return intervalWaitting;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    private BigInteger HTG_GAS_PRICE;

    public BigInteger[] gasPriceOrders = new BigInteger[6];

    @Override
    public void setNERVE_CHAINID(int NERVE_CHAINID) {
        this.NERVE_CHAINID = NERVE_CHAINID;
    }

    @Override
    public IConverterCoreApi getConverterCoreApi() {
        return converterCoreApi;
    }

    @Override
    public int HTG_CHAIN_ID() {
        return HTG_CHAIN_ID;
    }

    @Override
    public HeterogeneousCfg getConfig() {
        return config;
    }

    @Override
    public NulsLogger logger() {
        return logger;
    }

    @Override
    public BigInteger getEthGasPrice() {
        int time = 0;
        while (HTG_GAS_PRICE == null) {
            time++;
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                //do nothing
            }
            if (time == 3) {
                break;
            }
        }
        if (HTG_GAS_PRICE == null) {
            HTG_GAS_PRICE = GWEI_10;
        }
        return HTG_GAS_PRICE;
    }

    @Override
    public void setEthGasPrice(BigInteger ethGasPrice) {
        if (HTG_GAS_PRICE == null) {
            HTG_GAS_PRICE = GWEI_10;
        }
        if (ethGasPrice == null) {
            return;
        }
        HTG_GAS_PRICE = ethGasPrice;
    }

    @Override
    public void SET_DOCKING(IHeterogeneousChainDocking docking) {
        DOCKING = docking;
    }

    @Override
    public List<String> RPC_ADDRESS_LIST() {
        return RPC_ADDRESS_LIST;
    }

    @Override
    public List<String> STANDBY_RPC_ADDRESS_LIST() {
        return STANDBY_RPC_ADDRESS_LIST;
    }

    @Override
    public String ADMIN_ADDRESS_PUBLIC_KEY() {
        return ADMIN_ADDRESS_PUBLIC_KEY;
    }

    @Override
    public String ADMIN_ADDRESS() {
        return ADMIN_ADDRESS;
    }

    @Override
    public String ADMIN_ADDRESS_PASSWORD() {
        return ADMIN_ADDRESS_PASSWORD;
    }

    @Override
    public String MULTY_SIGN_ADDRESS() {
        return MULTY_SIGN_ADDRESS;
    }

    @Override
    public void SET_ADMIN_ADDRESS_PUBLIC_KEY(String s) {
        ADMIN_ADDRESS_PUBLIC_KEY = s;
    }

    @Override
    public void SET_ADMIN_ADDRESS(String s) {
        ADMIN_ADDRESS = s;
    }

    @Override
    public void SET_ADMIN_ADDRESS_PASSWORD(String s) {
        ADMIN_ADDRESS_PASSWORD = s;
    }

    @Override
    public void SET_MULTY_SIGN_ADDRESS(String s) {
        MULTY_SIGN_ADDRESS = s;
    }

    @Override
    public int NERVE_CHAINID() {
        return NERVE_CHAINID;
    }

    @Override
    public int HTG_ASSET_ID() {
        return HTG_ASSET_ID;
    }

    @Override
    public byte VERSION() {
        return VERSION;
    }

    @Override
    public LinkedBlockingDeque<HtgWaitingTxPo> WAITING_TX_QUEUE() {
        return WAITING_TX_QUEUE;
    }

    @Override
    public LinkedBlockingDeque<HtgUnconfirmedTxPo> UNCONFIRMED_TX_QUEUE() {
        return UNCONFIRMED_TX_QUEUE;
    }

    @Override
    public CountDownLatch INIT_WAITING_TX_QUEUE_LATCH() {
        return INIT_WAITING_TX_QUEUE_LATCH;
    }

    @Override
    public CountDownLatch INIT_UNCONFIRMEDTX_QUEUE_LATCH() {
        return INIT_UNCONFIRMEDTX_QUEUE_LATCH;
    }

    @Override
    public Set<String> FILTER_ACCOUNT_SET() {
        return FILTER_ACCOUNT_SET;
    }

    @Override
    public IHeterogeneousChainDocking DOCKING() {
        return DOCKING;
    }

    @Override
    public AssetName ASSET_NAME() {
        return AssetName.BNB;
    }

    private volatile boolean availableRPC = true;

    @Override
    public void setAvailableRPC(boolean available) {
        this.availableRPC = available;
    }

    @Override
    public boolean isAvailableRPC() {
        return availableRPC;
    }

    @Override
    public boolean supportPendingCall() {
        return true;
    }

    @Override
    public void SET_VERSION(byte version) {
        VERSION = version;
    }
}
