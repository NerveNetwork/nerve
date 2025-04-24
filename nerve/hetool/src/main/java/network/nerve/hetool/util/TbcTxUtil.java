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
package network.nerve.hetool.util;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.JSONUtils;
import network.nerve.hetool.model.TbcUTXO;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2025/4/1
 */
public class TbcTxUtil {
    public synchronized static Map<String, Object> buildMultiSigTransaction_sendTBC(Context context, String multiSigAddress, String address_to, String amount_tbc, List utxos, String opReturn) throws Exception {
        String format = String.format("contract.MultiSig.buildMultiSigTransaction_sendTBC('%s', '%s', %s, %s, %s)",
                multiSigAddress,
                address_to,
                amount_tbc,
                JSONUtils.obj2json(utxos),
                String.format("contract.buffer().from('88888888%s', 'hex')", opReturn)
        );
        Value va = context.eval("js", format);
        return va.as(Map.class);
    }

    public static String finishMultiSigTransaction_sendTBC(Context context, String txraw, String[][] sigs, String[] pubKeys) throws Exception {
        return _finishMultiSigTransaction_sendTBC(context, txraw, sigs, pubKeys);
    }

    public static String finishMultiSigTransaction_sendTBC(Context context, String txraw, List sigs, List pubKeys) throws Exception {
        return _finishMultiSigTransaction_sendTBC(context, txraw, sigs, pubKeys);
    }

    private synchronized static String _finishMultiSigTransaction_sendTBC(Context context, String txraw, Object sigs, Object pubKeys) throws Exception {
        String format = String.format("contract.MultiSig.finishMultiSigTransaction_sendTBC('%s', %s, %s)",
                txraw,
                JSONUtils.obj2json(sigs),
                JSONUtils.obj2json(pubKeys)
        );
        Value va = context.eval("js", format);
        return va.asString();
    }

    public synchronized static Map<String, Object> buildMultiSigTransaction_transferFT(Context context, String multiSigAddress, String address_to, String tokenId, Map tokenInfo, String transferTokenAmount, TbcUTXO umtxo, List ftutxos,
                                                                          List<String> preTXs, List<String> prepreTxDatas, String contractTx, String privateKeyAny, String opReturn) throws Exception {
        StringBuilder preTXs_ = new StringBuilder("[");
        for (String pre : preTXs) {
            preTXs_.append(String.format("contract.txFromString('%s')", pre)).append(',');
        }
        preTXs_.deleteCharAt(preTXs_.length() - 1);
        preTXs_.append(']');
        String format = String.format("contract.MultiSig.buildMultiSigTransaction_transferFT('%s', '%s', %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                multiSigAddress,
                address_to,
                String.format("contract.getToken('%s', %s)", tokenId, JSONUtils.obj2json(tokenInfo)),
                transferTokenAmount,
                JSONUtils.obj2json(umtxo),
                JSONUtils.obj2json(ftutxos),
                preTXs_,
                JSONUtils.obj2json(prepreTxDatas),
                String.format("contract.txFromString('%s')", contractTx),
                String.format("contract.tbc.PrivateKey.fromString('%s')", privateKeyAny),
                "null",
                String.format("contract.buffer().from('88888888%s', 'hex')", opReturn)
        );
        Value va = context.eval("js", format);
        return va.as(Map.class);
    }

    public static String finishMultiSigTransaction_transferFT(Context context, String txraw, String[][] sigs, String[] pubKeys) throws Exception {
        return _finishMultiSigTransaction_transferFT(context, txraw, sigs, pubKeys);
    }

    public static String finishMultiSigTransaction_transferFT(Context context, String txraw, List sigs, List pubKeys) throws Exception {
        return _finishMultiSigTransaction_transferFT(context, txraw, sigs, pubKeys);
    }

    private synchronized static String _finishMultiSigTransaction_transferFT(Context context, String txraw, Object sigs, Object pubKeys) throws Exception {
        String format = String.format("contract.MultiSig.finishMultiSigTransaction_sendTBC('%s', %s, %s)",
                txraw,
                JSONUtils.obj2json(sigs),
                JSONUtils.obj2json(pubKeys)
        );
        Value va = context.eval("js", format);
        return va.asString();
    }

    public synchronized static String fetchFtPrePreTxData(Context context, String preTXHex, int preTxVout, Map prePreTxs) throws Exception {
        String format = String.format("contract.API.fetchFtPrePreTxDataOffline(%s, %s, %s)",
                String.format("contract.txFromString('%s')", preTXHex),
                preTxVout,
                JSONUtils.obj2json(prePreTxs)
        );
        Value va = context.eval("js", format);
        return va.asString();
    }

    public static byte[] buildFTtransferCode(String code, String multiAddrCombineHash) {
        byte[] codeBuffer = HexUtil.decode(code);
        // 如果接收者是哈希
        if (multiAddrCombineHash.length() != 42) {
            throw new IllegalArgumentException("Invalid multisign hash");
        }
        String hash = multiAddrCombineHash;
        byte[] hashBuffer = HexUtil.decode(hash);
        // 将 hashBuffer 复制到 codeBuffer 的第 1537 字节位置
        System.arraycopy(hashBuffer, 0, codeBuffer, 1537, 21);
        return codeBuffer;
    }
}
