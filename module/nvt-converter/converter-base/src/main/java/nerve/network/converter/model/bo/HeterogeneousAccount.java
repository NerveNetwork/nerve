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

package nerve.network.converter.model.bo;

import nerve.network.converter.constant.ConverterErrorCode;
import io.nuls.core.crypto.AESEncrypt;
import io.nuls.core.crypto.EncryptedData;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.CryptoException;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.FormatValidUtils;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.Serializable;

/**
 * 异构链账户类
 *
 * @author: Chino
 * @date: 2020-02-26
 */
public abstract class HeterogeneousAccount implements Serializable {

    private String address;

    /**
     * 账户公钥
     */
    private byte[] pubKey;

    /**
     * 加密私钥
     */
    private byte[] encryptedPriKey;

    /**
     * 明文私钥
     */
    private byte[] priKey;

    /**
     * 账户是否被加密(是否设置过密码)
     * Whether the account is encrypted (Whether the password is set)
     */
    public boolean isEncrypted() {
        return getEncryptedPriKey() != null && getEncryptedPriKey().length > 0;
    }

    /**
     * 验证账户密码是否正确
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
     * 根据密码加密账户(给账户设置密码)
     * Password-encrypted account (set password for account)
     */
    public void encrypt(String password) {
        if (this.isEncrypted()) {
            return;
        }
        EncryptedData encryptedPrivateKey = AESEncrypt.encrypt(this.priKey, EncryptedData.DEFAULT_IV, new KeyParameter(Sha256Hash.hash(password.getBytes())));
        this.setPriKey(new byte[0]);
        this.setEncryptedPriKey(encryptedPrivateKey.getEncryptedBytes());
    }

    /**
     * 根据解密账户, 包括生成账户明文私钥
     * According to the decryption account, including generating the account plaintext private key
     */
    public boolean decrypt(String password) throws NulsException {
        try {
            byte[] unencryptedPrivateKey = AESEncrypt.decrypt(this.getEncryptedPriKey(), password);

            // 解密后的明文生成公钥，与原公钥校验
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
