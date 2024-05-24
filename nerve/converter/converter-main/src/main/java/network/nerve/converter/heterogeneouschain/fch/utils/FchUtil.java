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
package network.nerve.converter.heterogeneouschain.fch.utils;

import fchClass.P2SH;
import io.nuls.core.crypto.HexUtil;
import keyTools.KeyTools;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchUtil {

    public static HtgAccount createAccount(String prikey) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(prikey), true);
        String address = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());
        byte[] pubKey = ecKey.getPubKey();
        HtgAccount account = new HtgAccount();
        account.setAddress(address);
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ecKey.getPrivKeyBytes());
        account.setEncryptedPriKey(new byte[0]);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static HtgAccount createAccountByPubkey(String pubkeyStr) {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode(pubkeyStr));
        byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
        HtgAccount account = new HtgAccount();
        account.setAddress(KeyTools.pubKeyToFchAddr(HexUtil.encode(pubKey)));
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ZERO_BYTES);
        account.setEncryptedPriKey(ZERO_BYTES);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static P2SH genMultiP2sh(List<byte[]> pubKeyList, int m) {
        List<ECKey> keys = new ArrayList();
        Iterator var3 = pubKeyList.iterator();

        byte[] redeemScriptBytes;
        while(var3.hasNext()) {
            redeemScriptBytes = (byte[])var3.next();
            ECKey ecKey = ECKey.fromPublicOnly(redeemScriptBytes);
            keys.add(ecKey);
        }

        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(m, keys);
        redeemScriptBytes = multiSigScript.getProgram();

        try {
            P2SH p2sh = P2SH.parseP2shRedeemScript(javaTools.HexUtil.encode(redeemScriptBytes));
            return p2sh;
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }
    }
}
