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
package network.nerve.converter.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
public enum AssetName {
    NVT(9, 8),
    ETH(101, 18),
    BNB(102, 18),
    HT(103, 18),
    OKT(104, 18),
    ONE(105, 18),
    MATIC(106, 18),
    KCS(107, 18),
    TRX(108, 6),
    CRO(109, 18),
    AVAX(110, 18),
    AETH(111, 18),
    FTM(112, 18),
    METIS(113, 18),
    IOTX(114, 18),
    OETH(115, 18),
    KLAY(116, 18),
    BCH(117, 18);

    // 可使用其他异构网络的主资产作为手续费, 比如提现到ETH，支付BNB作为手续费
    private int chainId;
    private int decimals;
    private static Map<Integer, AssetName> map;

    AssetName(int chainId, int decimals) {
        this.chainId = chainId;
        this.decimals = decimals;
        putValue(this.chainId, this);
    }

    public int chainId() {
        return chainId;
    }

    public int decimals() {
        return decimals;
    }

    private static AssetName putValue(int value, AssetName valueEnum) {
        if (map == null) {
            map = new HashMap<>(8);
        }
        return map.put(value, valueEnum);
    }

    public static AssetName getEnum(int value) {
        return map.get(value);
    }
}
