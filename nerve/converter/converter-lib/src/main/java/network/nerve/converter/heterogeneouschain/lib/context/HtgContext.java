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

package network.nerve.converter.heterogeneouschain.lib.context;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.model.bo.HeterogeneousCfg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.GWEI_1;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.MAX_HTG_GAS_PRICE;

/**
 * @author: PierreLuo
 * @date: 2021-03-22
 */
public abstract class HtgContext {

    protected static final BigInteger GAS_LIMIT_OF_WITHDRAW = BigInteger.valueOf(210000L);
    protected static final BigInteger GAS_LIMIT_OF_CHANGE = BigInteger.valueOf(400000L);
    protected static final BigInteger GAS_LIMIT_OF_MAIN_ASSET = BigInteger.valueOf(21000L);
    protected static final BigInteger GAS_LIMIT_OF_ERC20 = BigInteger.valueOf(100000L);
    protected static final BigInteger HTG_ESTIMATE_GAS = BigInteger.valueOf(1000000L);
    protected static final BigInteger BASE_GAS_LIMIT = BigInteger.valueOf(50000L);
    private BigInteger GAS_LIMIT_OF_WITHDRAW_V2;
    private Map<String, Object> cache = new HashMap<>();

    public Map<String, Object> dynamicCache() {
        return cache;
    }

    public abstract int HTG_CHAIN_ID();
    public abstract HeterogeneousCfg getConfig();
    public abstract NulsLogger logger();
    public abstract IConverterCoreApi getConverterCoreApi();
    public abstract void setConverterCoreApi(IConverterCoreApi converterCoreApi);
    public abstract List<String> RPC_ADDRESS_LIST();
    public abstract List<String> STANDBY_RPC_ADDRESS_LIST();
    public abstract String ADMIN_ADDRESS_PUBLIC_KEY();
    public abstract String ADMIN_ADDRESS();
    public abstract String ADMIN_ADDRESS_PASSWORD();
    public abstract String MULTY_SIGN_ADDRESS();
    public abstract void SET_ADMIN_ADDRESS_PUBLIC_KEY(String s);
    public abstract void SET_ADMIN_ADDRESS(String s);
    public abstract void SET_ADMIN_ADDRESS_PASSWORD(String s);
    public abstract void SET_MULTY_SIGN_ADDRESS(String s);
    public abstract void SET_DOCKING(IHeterogeneousChainDocking docking);
    public abstract int NERVE_CHAINID();
    public abstract int HTG_ASSET_ID();
    public abstract byte VERSION();
    public abstract LinkedBlockingDeque<HtgWaitingTxPo> WAITING_TX_QUEUE();
    public abstract LinkedBlockingDeque<HtgUnconfirmedTxPo> UNCONFIRMED_TX_QUEUE();
    public abstract CountDownLatch INIT_WAITING_TX_QUEUE_LATCH();
    public abstract CountDownLatch INIT_UNCONFIRMEDTX_QUEUE_LATCH();
    public abstract Set<String> FILTER_ACCOUNT_SET();
    public abstract IHeterogeneousChainDocking DOCKING();
    public abstract BigInteger getEthGasPrice();
    public abstract void setEthGasPrice(BigInteger b);
    public abstract AssetName ASSET_NAME();
    public abstract void setAvailableRPC(boolean available);
    public abstract boolean isAvailableRPC();
    public abstract void SET_VERSION(byte version);
    public abstract void setLogger(NulsLogger logger);
    public abstract void setConfig(HeterogeneousCfg config);

    public abstract void setNERVE_CHAINID(int NERVE_CHAINID);
    /**
     * Does the current heterogeneous chain support contractspendingquery
     */
    public boolean supportPendingCall() {
        return true;
    }


    public int getByzantineCount(Integer total) {
        return this.getConverterCoreApi().getByzantineCount(total);
    }

    public BigInteger calcGasPrice(BigInteger ethGasPrice, BigInteger currentGasPrice) {
        if (ethGasPrice == null) {
            return currentGasPrice;
        }
        if (ethGasPrice.compareTo(MAX_HTG_GAS_PRICE) > 0) {
            ethGasPrice = MAX_HTG_GAS_PRICE;
        }
        if (ethGasPrice.compareTo(GWEI_1) < 0) {
            return currentGasPrice;
        }
        return ethGasPrice;
        /*
        Rolling back heterogeneous network packaging with stable prices6The mechanism of updating again after the second update is due to the cause of front-end and back-end issuespriceInconsistent
        if (HTG_GAS_PRICE == null || HTG_GAS_PRICE.compareTo(ethGasPrice) <= 0) {
            HTG_GAS_PRICE = ethGasPrice;
        } else {
            gasPriceOrders = HtgUtil.sortByInsertionDsc(gasPriceOrders, ethGasPrice);
            if (gasPriceOrders[5] != null) {
                HTG_GAS_PRICE = gasPriceOrders[0];
            }
        }*/
    }

    public long hashSalt() {
        return getConfig().getChainIdOnHtgNetwork() * 2 + VERSION();
    }

    public BigInteger GET_GAS_LIMIT_OF_WITHDRAW() {
        return GAS_LIMIT_OF_WITHDRAW;
    }

    public BigInteger GET_GAS_LIMIT_OF_CHANGE() {
        return GAS_LIMIT_OF_CHANGE;
    }

    public BigInteger GET_GAS_LIMIT_OF_MAIN_ASSET() {
        return GAS_LIMIT_OF_MAIN_ASSET;
    }

    public BigInteger GET_GAS_LIMIT_OF_ERC20() {
        return GAS_LIMIT_OF_ERC20;
    }

    public BigInteger GET_HTG_ESTIMATE_GAS() {
        return HTG_ESTIMATE_GAS;
    }

    public BigInteger GET_BASE_GAS_LIMIT() {
        return BASE_GAS_LIMIT;
    }

    public BigInteger GAS_LIMIT_OF_WITHDRAW() {
        if (getConverterCoreApi().hasL1FeeOnChain(HTG_CHAIN_ID())) {
            return GET_GAS_LIMIT_OF_WITHDRAW();
        }
        if (GAS_LIMIT_OF_WITHDRAW_V2 == null) {
            GAS_LIMIT_OF_WITHDRAW_V2 = new BigDecimal(GET_GAS_LIMIT_OF_WITHDRAW()).multiply(new BigDecimal("1.2")).toBigInteger();
        }
        return GAS_LIMIT_OF_WITHDRAW_V2;
    }
    public BigInteger GAS_LIMIT_OF_CHANGE() {
        return GET_GAS_LIMIT_OF_CHANGE();
    }
    public BigInteger GAS_LIMIT_OF_MAIN_ASSET() {
        return GET_GAS_LIMIT_OF_MAIN_ASSET();
    }
    public BigInteger GAS_LIMIT_OF_ERC20() {
        return GET_GAS_LIMIT_OF_ERC20();
    }
    public BigInteger HTG_ESTIMATE_GAS() {
        return GET_HTG_ESTIMATE_GAS();
    }
    public BigInteger BASE_GAS_LIMIT() {
        return GET_BASE_GAS_LIMIT();
    }
}
