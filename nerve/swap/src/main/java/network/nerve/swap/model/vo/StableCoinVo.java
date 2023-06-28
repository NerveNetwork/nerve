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
package network.nerve.swap.model.vo;

import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2021/10/28
 */
public class StableCoinVo {

    private String address;
    private String lpToken;
    private Map<String, Integer> groupCoin;

    public StableCoinVo(String address, NerveToken lpToken, Set<NerveToken> groupCoinSet) {
        this.address = address;
        this.lpToken = lpToken.str();
        this.groupCoin = new HashMap<>();
        for (NerveToken token : groupCoinSet) {
            this.groupCoin.put(token.str(), 1);
        }
    }

    public StableCoinVo(String address, NerveToken lpToken, NerveToken[] coins) {
        this.address = address;
        this.lpToken = lpToken.str();
        this.groupCoin = new HashMap<>();
        for (NerveToken token : coins) {
            this.groupCoin.put(token.str(), 1);
        }
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLpToken() {
        return lpToken;
    }

    public void setLpToken(String lpToken) {
        this.lpToken = lpToken;
    }

    public Map<String, Integer> getGroupCoin() {
        return groupCoin;
    }

    public void setGroupCoin(Map<String, Integer> groupCoin) {
        this.groupCoin = groupCoin;
    }

}
