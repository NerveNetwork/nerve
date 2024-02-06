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
import java.util.Map;
import java.util.Set;

import static io.protostuff.ByteString.EMPTY_STRING;

/**
 * @author: PierreLuo
 * @date: 2021/10/28
 */
public class StableCoin {

    private String address;
    private Map<NerveToken, Boolean> groupCoin;
    private Map<NerveToken, Boolean> pauseGroupCoin;

    public StableCoin(String address, Map<NerveToken, Boolean> groupCoin, Map<NerveToken, Boolean> pauseGroupCoin) {
        this.address = address;
        this.groupCoin = groupCoin;
        this.pauseGroupCoin = pauseGroupCoin;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Map<NerveToken, Boolean> getGroupCoin() {
        return groupCoin;
    }

    public void setGroupCoin(Map<NerveToken, Boolean> groupCoin) {
        this.groupCoin = groupCoin;
    }

    public Map<NerveToken, Boolean> getPauseGroupCoin() {
        return pauseGroupCoin;
    }

    public void setPauseGroupCoin(Map<NerveToken, Boolean> pauseGroupCoin) {
        this.pauseGroupCoin = pauseGroupCoin;
    }

    public void clear() {
        this.address = EMPTY_STRING;
        this.groupCoin = Collections.EMPTY_MAP;
        this.pauseGroupCoin = Collections.EMPTY_MAP;
    }
}
