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
 * @author: Loki
 * @date: 2020-02-28
 */
public enum ProposalTypeEnum {

    //类型 1:退回资金, 2:转到其他账户, 3:冻结账户, 4:解冻账户, 5:撤销银行资格, 6:其他类型, 7:多签合约升级
    REFUND((byte) 1),

    TRANSFER((byte) 2),

    LOCK((byte) 3),

    UNLOCK((byte) 4),

    EXPELLED((byte) 5),

    OTHER((byte) 6),

    UPGRADE((byte) 7);

    private byte value;
    private static Map<Byte, ProposalTypeEnum> map;

    ProposalTypeEnum(byte value) {
        this.value = value;
        putValue(value, this);
    }

    public byte value() {
        return value;
    }

    private static ProposalTypeEnum putValue(byte value, ProposalTypeEnum valueEnum) {
        if (map == null) {
            map = new HashMap<>(8);
        }
        return map.put(value, valueEnum);
    }

    public static ProposalTypeEnum getEnum(byte value) {
        return map.get(value);
    }
}
