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

package network.nerve.converter.model.po;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

/**
 * For a withdrawal transaction, Record the additional handling fee for the transaction Business data
 * @author: Loki
 * @date: 2020/9/29
 */
public class WithdrawalAdditionalFeePO implements Serializable {

    /**
     * Withdrawal transactionshash(Original transaction)/Or propose to return the transaction via the original routehash
     */
    private String basicTxHash;
    /**
     * k:Additional transaction feeshash, v:Additional amount
     */
    private Map<String, BigInteger> mapAdditionalFee;

    public WithdrawalAdditionalFeePO() {
    }

    public WithdrawalAdditionalFeePO(String basicTxHash, Map<String, BigInteger> mapAdditionalFee) {
        this.basicTxHash = basicTxHash;
        this.mapAdditionalFee = mapAdditionalFee;
    }

    public String getBasicTxHash() {
        return basicTxHash;
    }

    public void setBasicTxHash(String basicTxHash) {
        this.basicTxHash = basicTxHash;
    }

    public Map<String, BigInteger> getMapAdditionalFee() {
        return mapAdditionalFee;
    }

    public void setMapAdditionalFee(Map<String, BigInteger> mapAdditionalFee) {
        this.mapAdditionalFee = mapAdditionalFee;
    }
}
