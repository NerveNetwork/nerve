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
package network.nerve.converter.heterogeneouschain.eth.context;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.model.bo.HeterogeneousCfg;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_GAS_LIMIT_OF_USDT;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthContext implements Serializable {

    /**
     * 当 importAccountByPriKey 或者 importAccountByKeystore 被调用时，覆写这个地址作为虚拟银行管理员地址
     */
    public static String ADMIN_ADDRESS;
    public static String ADMIN_ADDRESS_PASSWORD;

    /**
     * 待确认交易队列
     */
    public static LinkedBlockingDeque<EthUnconfirmedTxPo> UNCONFIRMED_TX_QUEUE = new LinkedBlockingDeque<>();

    public static int NERVE_CHAINID = 1;
    public static String MULTY_SIGN_ADDRESS;
    public static List<String> RPC_ADDRESS_LIST = new ArrayList<>();
    /**
     * 日志实例
     */
    private static NulsLogger logger;
    private static HeterogeneousCfg config;
    private static IConverterCoreApi converterCoreApi;

    public static IConverterCoreApi getConverterCoreApi() {
        return converterCoreApi;
    }

    public static void setConverterCoreApi(IConverterCoreApi converterCoreApi) {
        EthContext.converterCoreApi = converterCoreApi;
    }

    public static HeterogeneousCfg getConfig() {
        return config;
    }

    public static void setConfig(HeterogeneousCfg config) {
        EthContext.config = config;
    }

    public static void setLogger(NulsLogger logger) {
        EthContext.logger = logger;
    }

    public static NulsLogger logger() {
        return logger;
    }

    public static CountDownLatch INIT_UNCONFIRMEDTX_QUEUE_LATCH = new CountDownLatch(1);

    private static BigInteger MAX_ETH_GAS_PRICE = BigInteger.valueOf(300L).multiply(BigInteger.TEN.pow(9));
    private static BigInteger ETH_GAS_PRICE;

    public static BigInteger getEthGasPrice() {
        int time = 0;
        while (ETH_GAS_PRICE == null) {
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
        if (ETH_GAS_PRICE == null) {
            ETH_GAS_PRICE = BigInteger.valueOf(100L).multiply(BigInteger.TEN.pow(9));
        }
        return ETH_GAS_PRICE;
    }

    public static void setEthGasPrice(BigInteger ethGasPrice) {
        if (ethGasPrice != null) {
            if (ethGasPrice.compareTo(MAX_ETH_GAS_PRICE) > 0) {
                ethGasPrice = MAX_ETH_GAS_PRICE;
            }
            ETH_GAS_PRICE = ethGasPrice;
        }
    }

    public static BigDecimal getFee() {
        return new BigDecimal(getEthGasPrice().multiply(ETH_GAS_LIMIT_OF_USDT)).divide(BigDecimal.TEN.pow(18));
    }
}
