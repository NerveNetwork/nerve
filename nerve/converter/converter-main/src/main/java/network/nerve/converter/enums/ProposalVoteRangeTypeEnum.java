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
 * @author: Loki
 * @date: 2020-03-04
 */
public enum ProposalVoteRangeTypeEnum {
    //type 1:Virtual Bank Node, 2:Consensus node, 3:All coin holders
    BANK((byte) 1),

    AGENT((byte) 2),

    TOKEN_POSSESSOR ((byte) 3);

    private byte value;
    private static Map<Byte, ProposalVoteRangeTypeEnum> map;

    ProposalVoteRangeTypeEnum(byte value) {
        this.value = value;
        putValue(value, this);
    }

    public byte value() {
        return value;
    }

    private static ProposalVoteRangeTypeEnum putValue(byte value, ProposalVoteRangeTypeEnum valueEnum) {
        if (map == null) {
            map = new HashMap<>(8);
        }
        return map.put(value, valueEnum);
    }

    public static ProposalVoteRangeTypeEnum getEnum(byte value) {
        return map.get(value);
    }
}
