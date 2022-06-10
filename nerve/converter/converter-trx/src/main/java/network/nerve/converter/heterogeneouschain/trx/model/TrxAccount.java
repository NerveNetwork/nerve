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
package network.nerve.converter.heterogeneouschain.trx.model;

import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.utils.Numeric;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class TrxAccount extends HtgAccount implements Serializable {

    private int order;

    private String compressedPublicKey;

    private transient int mod;
    private transient int bankSize;

    public TrxAccount() {
    }

    public TrxAccount(int order) {
        this.order = order;
    }

    public static TrxAccount newEmptyAccount(int order) {
        return new TrxAccount(order);
    }

    @Override
    protected boolean validatePubKey(byte[] newPriKey, byte[] orginPubKey) {
        KeyPair pair = new KeyPair(Numeric.toHexString(newPriKey));
        byte[] pubKey = pair.getRawPair().getPublicKey().getEncoded();
        return Arrays.equals(pubKey, orginPubKey);
    }

    public String getCompressedPublicKey() {
        return compressedPublicKey;
    }

    public void setCompressedPublicKey(String compressedPublicKey) {
        this.compressedPublicKey = compressedPublicKey;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(getAddress());
    }

    public int getMod() {
        return mod;
    }

    public void setMod(int mod) {
        this.mod = mod;
    }

    public int getBankSize() {
        return bankSize;
    }

    public void setBankSize(int bankSize) {
        this.bankSize = bankSize;
    }
}
