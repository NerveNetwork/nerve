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
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.model.bo.HeterogeneousCfg;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.GWEI_1;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.MAX_HTG_GAS_PRICE;

/**
 * @author: PierreLuo
 * @date: 2021-03-22
 */
public interface HtgContext {

    HeterogeneousCfg getConfig();
    NulsLogger logger();
    IConverterCoreApi getConverterCoreApi();
    List<String> RPC_ADDRESS_LIST();
    List<String> STANDBY_RPC_ADDRESS_LIST();
    String ADMIN_ADDRESS_PUBLIC_KEY();
    String ADMIN_ADDRESS();
    String ADMIN_ADDRESS_PASSWORD();
    String MULTY_SIGN_ADDRESS();
    void SET_ADMIN_ADDRESS_PUBLIC_KEY(String s);
    void SET_ADMIN_ADDRESS(String s);
    void SET_ADMIN_ADDRESS_PASSWORD(String s);
    void SET_MULTY_SIGN_ADDRESS(String s);
    int NERVE_CHAINID();
    int HTG_ASSET_ID();
    byte VERSION();
    LinkedBlockingDeque<HtgWaitingTxPo> WAITING_TX_QUEUE();
    LinkedBlockingDeque<HtgUnconfirmedTxPo> UNCONFIRMED_TX_QUEUE();
    CountDownLatch INIT_WAITING_TX_QUEUE_LATCH();
    CountDownLatch INIT_UNCONFIRMEDTX_QUEUE_LATCH();
    Set<String> FILTER_ACCOUNT_SET();
    HtgDocking DOCKING();
    BigInteger getEthGasPrice();
    void setEthGasPrice(BigInteger b);
    AssetName ASSET_NAME();
    void setAvailableRPC(boolean available);
    boolean isAvailableRPC();
    /**
     * 当前异构链是否支持合约的pending查询
     */
    default boolean supportPendingCall() {
        return true;
    }

    default BigInteger calcGasPrice(BigInteger ethGasPrice, BigInteger currentGasPrice) {
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
        回滚异构网络打包价格，稳定6次后再更新的机制，原因是造成前后端price不一致
        if (HTG_GAS_PRICE == null || HTG_GAS_PRICE.compareTo(ethGasPrice) <= 0) {
            HTG_GAS_PRICE = ethGasPrice;
        } else {
            gasPriceOrders = HtgUtil.sortByInsertionDsc(gasPriceOrders, ethGasPrice);
            if (gasPriceOrders[5] != null) {
                HTG_GAS_PRICE = gasPriceOrders[0];
            }
        }*/
    }
}
