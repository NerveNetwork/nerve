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
package network.nerve.converter.heterogeneouschain.btc.utils;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BtcUtil {

    public static HtgAccount createAccount(String prikey, boolean mainnet) {
        org.bitcoinj.core.ECKey ecKey = org.bitcoinj.core.ECKey.fromPrivate(HexUtil.decode(prikey), true);
        String address = getBtcLegacyAddress(ecKey.getPubKey(), mainnet);
        byte[] pubKey = ecKey.getPubKey();
        HtgAccount account = new HtgAccount();
        account.setAddress(address);
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ecKey.getPrivKeyBytes());
        account.setEncryptedPriKey(new byte[0]);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static String getBtcLegacyAddress(byte[] pubKeyBytes, boolean mainnet) {
        int version = mainnet ? 0 : 111;
        byte[] bytes = SerializeUtils.sha256hash160(pubKeyBytes);
        return Base58.encodeChecked(version, bytes);
    }

    public static HtgAccount createAccountByPubkey(String pubkeyStr, boolean mainnet) {
        org.bitcoinj.core.ECKey ecKey = org.bitcoinj.core.ECKey.fromPublicOnly(HexUtil.decode(pubkeyStr));
        byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
        HtgAccount account = new HtgAccount();
        account.setAddress(getBtcLegacyAddress(pubKey, mainnet));
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ZERO_BYTES);
        account.setEncryptedPriKey(ZERO_BYTES);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }
}
