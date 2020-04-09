/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 对提案的表决
 * @author: lichao
 * @date: 2020-03-03
 */
public enum ProposalVoteChoiceEnum {

    //表决提案 0:against反对，1:favor赞成
    AGAINST((byte) 0),

    FAVOR((byte) 1);

    private byte value;
    private static Map<Byte, ProposalVoteChoiceEnum> map;

    ProposalVoteChoiceEnum(byte value) {
        this.value = value;
        putValue(value, this);
    }

    public byte value() {
        return value;
    }

    private static ProposalVoteChoiceEnum putValue(byte value, ProposalVoteChoiceEnum valueEnum) {
        if (map == null) {
            map = new HashMap<>(8);
        }
        return map.put(value, valueEnum);
    }

    public static ProposalVoteChoiceEnum getEnum(byte value) {
        return map.get(value);
    }
}
