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
package network.nerve.swap.model.bo;

import network.nerve.swap.model.NerveToken;

import java.util.Collections;
import java.util.Set;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2021/10/28
 */
public class StableCoin {

    private String address;
    private Set<NerveToken> groupCoin;

    public StableCoin(String address, Set<NerveToken> groupCoin) {
        this.address = address;
        this.groupCoin = groupCoin;
    }

    public String getAddress() {
        return address;
    }

    public Set<NerveToken> getGroupCoin() {
        return groupCoin;
    }

    public void setGroupCoin(Set<NerveToken> groupCoin) {
        this.groupCoin = groupCoin;
    }

    public void clear() {
        this.address = EMPTY_STRING;
        this.groupCoin = Collections.EMPTY_SET;
    }
}