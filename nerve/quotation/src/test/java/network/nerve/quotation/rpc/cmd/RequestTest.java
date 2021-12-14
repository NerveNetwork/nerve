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

package network.nerve.quotation.rpc.cmd;

import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.thread.ThreadUtils;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.ConfigBean;
import network.nerve.quotation.rpc.querier.Querier;
import network.nerve.quotation.rpc.querier.impl.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author: Loki
 * @date: 2020/8/11
 */
public class RequestTest {

    static int chainId = 5;
    static int assetId = 1;
    Chain chain;

    static String NULS_USDT = "NULS-USDT";
    static String NVT_USDT = "NVT-USDT";
    @Before
    public void before() throws Exception {
        chain = new Chain();
        chain.setConfigBean(new ConfigBean(chainId, assetId));
        int chainId = chain.getConfigBean().getChainId();
        NulsLogger logger = LoggerBuilder.getLogger(ModuleE.QU.name, chainId);
        chain.setLogger(logger);
    }

    @Test
    public void test() throws Exception {
        String key = NVT_USDT;
        int i = 0;
        while (i < 1) {
//            Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier1 = new AexQuerier();
//                    querier1.tickerPrice(chain, "https://api.aex.zone/v3/ticker.php", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("aex", runnable);
//            runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier3 = new BitzQuerier();
//                    querier3.tickerPrice(chain, "https://api.bitzspeed.com/", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("bitz", runnable);
//            runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier4 = new DexQuerier();
//                    querier4.tickerPrice(chain, "http://beta.nervedex.com", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("nvt", runnable);
//            runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier5 = new HuobiQuerier();
//                    querier5.tickerPrice(chain, "https://api-aws.huobi.pro", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("huobi", runnable);
            Runnable  runnable = new Runnable() {
                @Override
                public void run() {
                    Querier querier6 = new MxcQuerier();
                    querier6.tickerPrice(chain, "https://www.mxc.com", key);
                }
            };
            ThreadUtils.createAndRunThread("mx", runnable);
//            runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier7 = new OkexQuerier();
//                    querier7.tickerPrice(chain, "https://aws.okex.com", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("ok", runnable);
//
//
//            runnable = new Runnable() {
//                @Override
//                public void run() {
//                    Querier querier2 = new BinanceQuerier();
//                    querier2.tickerPrice(chain, "https://api.binance.com", key);
////                    querier2.tickerPrice(chain, "http://binanceapi.zhoulijun.top", key);
//                }
//            };
//            ThreadUtils.createAndRunThread("ok", runnable);
            i++;
        }


        Thread.sleep(1000000000000L);

    }

}
