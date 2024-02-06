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

package network.nerve.converter.model.bo;

import io.nuls.core.crypto.AESEncrypt;
import io.nuls.core.crypto.EncryptedData;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.CryptoException;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.Serializable;

/**
 * Heterogeneous chain account class
 *
 * @author: Mimi
 * @date: 2020-02-26
 */
public abstract class HeterogeneousAccount implements Serializable {
    private static final byte[] ZERO_BYTES = new byte[]{0};

    private String address;

    /**
     * Account public key
     */
    private byte[] pubKey;

    /**
     * Encrypt private key
     */
    private byte[] encryptedPriKey;

    /**
     * Clear text private key
     */
    private byte[] priKey;

    /**
     * Is the account encrypted(Have you ever set a password)
     * Whether the account is encrypted (Whether the password is set)
     */
    private boolean isEncrypted() {
        return getEncryptedPriKey() != null && getEncryptedPriKey().length > 0;
    }

    private boolean isEmptyPriKey() {
        return getPriKey() == null || getPriKey().length <= 1;
    }

    private boolean isEmptyEncryptedPriKey() {
        return getEncryptedPriKey() == null || getEncryptedPriKey().length <= 1;
    }

    /**
     * Verify if the account password is correct
     * Verify that the account password is correct
     */
    public boolean validatePassword(String password) {
        boolean result = FormatValidUtils.validPassword(password);
        if (!result) {
            return false;
        }
        byte[] unencryptedPrivateKey;
        try {
            unencryptedPrivateKey = AESEncrypt.decrypt(this.getEncryptedPriKey(), password);
        } catch (CryptoException e) {
            return false;
        }
        return validatePubKey(unencryptedPrivateKey, getPubKey());
    }


    /**
     * Encrypt account based on password(Set password for account)
     * Password-encrypted account (set password for account)
     */
    public void encrypt(String password) {
        if (isEmptyPriKey()) {
            this.setPriKey(ZERO_BYTES);
            this.setEncryptedPriKey(ZERO_BYTES);
            return;
        }
        if (this.isEncrypted()) {
            return;
        }
        EncryptedData encryptedPrivateKey = AESEncrypt.encrypt(this.priKey, EncryptedData.DEFAULT_IV, new KeyParameter(Sha256Hash.hash(password.getBytes())));
        this.setPriKey(ZERO_BYTES);
        this.setEncryptedPriKey(encryptedPrivateKey.getEncryptedBytes());
    }

    /**
     * According to the decryption account, Including generating account plaintext private keys
     * According to the decryption account, including generating the account plaintext private key
     */
    public boolean decrypt(String password) throws NulsException {
        try {
            if (isEmptyEncryptedPriKey()) {
                this.setPriKey(ZERO_BYTES);
                this.setEncryptedPriKey(ZERO_BYTES);
                return true;
            }
            byte[] unencryptedPrivateKey = AESEncrypt.decrypt(this.getEncryptedPriKey(), password);

            // Generate a public key from decrypted plaintext and verify it with the original public key
            if (!validatePubKey(unencryptedPrivateKey, getPubKey())) {
                return false;
            }
            this.setPriKey(unencryptedPrivateKey);
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
        }
        return true;
    }

    protected abstract boolean validatePubKey(byte[] newPriKey, byte[] orginPubKey);

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public byte[] getEncryptedPriKey() {
        return encryptedPriKey;
    }

    public void setEncryptedPriKey(byte[] encryptedPriKey) {
        this.encryptedPriKey = encryptedPriKey;
    }

    public byte[] getPriKey() {
        return priKey;
    }

    public void setPriKey(byte[] priKey) {
        this.priKey = priKey;
    }
}
