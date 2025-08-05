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
    BCH(117, 18),
    GOERLIETH(118, 18),
    ENULS(119, 18),
    KAVA(120, 18),
    ETHW(121, 18),
    REI(122, 18),
    ZK(123, 18),
    EOS(124, 18),
    ZKPOLYGON(125, 18),
    LINEA(126, 18),
    CELO(127, 18),
    ETC(128, 18),
    BASE(129, 18),
    SCROLL(130, 18),
    BRISE(131, 18),
    JANUS(132, 18),
    MANTA(133, 18),
    OKB(134, 18),
    ZETA(135, 18),
    KROMA(136, 18),
    SHM(137, 18),

    BTC(201, 8),
    FCH(202, 8),
    BCHUTXO(203, 8),
    TBC(204, 6),

    MODE(138, 18),
    BLAST(139, 18),
    MERLIN(140, 18),
    PULSE(141, 18),
    MINT(142, 18),
    AKC(143, 18);

    // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
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
