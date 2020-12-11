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

package network.nerve.quotation.heterogeneouschain.context;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * @author: Loki
 * @date: 2020/11/18
 */
public class EthContext {

    public static String rpcAddress = "http://geth.nerve.network";

    private static BigInteger MAX_ETH_GAS_PRICE = BigInteger.valueOf(300L).multiply(BigInteger.TEN.pow(9));
    public static BigInteger ETH_GAS_PRICE;
    public static BigInteger ETH_GAS_LIMIT_OF_ETH = BigInteger.valueOf(21000L);

    public static BigInteger ETH_GAS_LIMIT_OF_ERC20 = BigInteger.valueOf(60000L);

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
}
