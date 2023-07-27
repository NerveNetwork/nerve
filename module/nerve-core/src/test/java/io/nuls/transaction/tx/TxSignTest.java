/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.transaction.tx;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.basic.Result;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.JSONUtils;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eva
 */
public class TxSignTest {

    @Test
    public void parse() throws NulsException {
        byte[] singnature = HexUtil.decode("210369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c54630440220165ca414c31d3b22b557f0396ed2f911f3934dacb889820774087592837f67d002203b57efaf1a85f82c811ad28c82d8f33d0eedc434f7641daba2cca4fe15ea97bb2102db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d473045022100cceba4691e3cbc6afce90761517e7bb5b2fddd9fc3cff107d413700a95570a6a022077e44888eb814c11fd2d313bfb2e3c492a4d051e72d5334457b852664ee162ab210308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b473045022100ef27566329def8ce2287679218832647f455e0dffba263a677fdac66844b788a02207e5ad6273daf5cf9d215632eeceecf93f6bb3288e306d57c6b40192e2764802621020c60dd7e0016e174f7ba4fc0333052bade8c890849409de7b6f3d26f0ec64528473045022100932263cfb4138eb2c4862c77c5d7ee12f61cb1ae1c4e6f483a60a9d8ff24cc7202200a7a5781c6077ca4f2af6ce8ef941cbb5ed8f5b77a3344826f987e8b4efd103a");
        TransactionSignature ts = new TransactionSignature();
        ts.parse(singnature,0);
        System.out.println(ts.getSignersCount());
        Set<String> addressSet = new HashSet<>();
        List<P2PHKSignature> p2PHKSignatures;
        p2PHKSignatures = ts.getP2PHKSignatures();
        for (P2PHKSignature signature : p2PHKSignatures) {
            if (signature.getPublicKey() != null && signature.getPublicKey().length != 0) {
                addressSet.add(AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), 9)));
            }
        }
        System.out.println(addressSet);
    }


    public static void main(String[] args) throws Exception{
        main0(args);
    }

    public static void main2(String[] args) throws NulsException {
        System.out.println(new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16).toString());
        // 163888788 888888888888888888
        String txHex = "020012ba1d6200008c0117020001a0829a88221cf34fc8bdf20dc1c94d595a91277602000100d0b632000000000000000000000000000000000000000000000000000000000008000000000000000000011702000174cf6bffe59cae72e122524c672a31f94936e33102000100303031000000000000000000000000000000000000000000000000000000000000000000000000006a2102a94d93b32693989d535d955a112934d563a5489e3ed0da55e6aef6234795f5c3473045022100f31647e6f75c7ad469ae6ee114cb0c3b45a64fdfaa696f57cf60b4fe9f82535d02207fb9c7950e8a08f217c8f2ad9792ec44b1570ae2b13ed86d543c48afe6e83084";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex), 0);
        byte[] singnature = tx.getTransactionSignature();
        TransactionSignature ts = new TransactionSignature();
        ts.parse(singnature,0);
        System.out.println(ts.getSignersCount());

        P2PHKSignature ps = ts.getP2PHKSignatures().get(0);

        System.out.println(HexUtil.encode(ps.getPublicKey()));
        System.out.println(HexUtil.encode(ps.getSignData().getSignBytes()));
        boolean result = ECKey.verify(tx.getHash().getBytes(),ps.getSignData().getSignBytes(),ps.getPublicKey());
        System.out.println(result);
    }

    public static void main0(String[] args) throws NulsException {
        String txHex = "020012ba1d6200008c0117020001a0829a88221cf34fc8bdf20dc1c94d595a91277602000100d0b632000000000000000000000000000000000000000000000000000000000008000000000000000000011702000174cf6bffe59cae72e122524c672a31f94936e33102000100303031000000000000000000000000000000000000000000000000000000000000000000000000006a2102a94d93b32693989d535d955a112934d563a5489e3ed0da55e6aef6234795f5c3473045022100f31647e6f75c7ad469ae6ee114cb0c3b45a64fdfaa696f57cf60b4fe9f82535d02207fb9c7950e8a08f217c8f2ad9792ec44b1570ae2b13ed86d543c48afe6e83084";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex),0);
        System.out.println(tx.getHash());
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(),0);
        for(CoinFrom from :coinData.getFrom()){
            System.out.println("from : "+AddressTool.getStringAddressByBytes(from.getAddress()));
        }
        TransactionSignature sig = new TransactionSignature();
        sig.parse(tx.getTransactionSignature(),0);
        System.out.println("sign : "+AddressTool.getAddressString(sig.getP2PHKSignatures().get(0).getPublicKey(), 2));
        boolean result = SignatureUtil.validateCtxSignture(tx);
        System.out.println(result);
    }

    public static void main1(String[] args) throws NulsException, IOException {
//        String txSign = "2102b2c2595d41d30742d71d48ad728fd909406e3b5920af533ae578ec72647e082a00473045022100d483dad8cb5604bbff2f11ade34af8586aeeae462a05e5ffc50114ec684df8d502203cea35c819a01cbb66df215f1125b600217ca0e970da0683a44ada47f736419d";
//        String txHash = "568533e38c472f9e0740f318038b49ebe6b59817ce374c6816bf645744beb6ed";
//
//        TransactionSignature sign = new TransactionSignature();
//        sign.parse(HexUtil.decode(txSign),0);
//        byte[] pub = sign.getP2PHKSignatures().get(0).getPublicKey();
//        System.out.println(HexUtil.encode(pub));
//        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddress(pub,1)));
//        ECKey ecKey = ECKey.fromPublicOnly(pub);
//        boolean result = ecKey.verify(HexUtil.decode(txHash),sign.getP2PHKSignatures().get(0).getSignData().getSignBytes());
//        System.out.println(result);
//
//        pub = sign.getP2PHKSignatures().get(1).getPublicKey();
//        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddress(pub,1)));
//        ecKey = ECKey.fromPublicOnly(pub);
//         result = ecKey.verify(HexUtil.decode(txHash),sign.getP2PHKSignatures().get(1).getSignData().getSignBytes());
//        System.out.println(result);
//
//
//        for (P2PHKSignature signature : sign.getP2PHKSignatures()) {
//            if (!ECKey.verify(HexUtil.decode(txHash), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
//                System.out.println("bbbbbbbbbb");
//                throw new NulsException(new Exception("Transaction signature error !"));
//            }
//            System.out.println("aaaaaaaaa");
//        }
        JSONUtils.obj2json(new HashMap<>());
        byte[] signData = HexUtil.decode("30440220712d7963a69bb7fa313e202c4fdba5a4f045e50e75994394f13fd5f6d0b9b4f002207200b29f83a6fd311fb415650c7f37b95b68a2e8926d69bb7cf3c3af31e772bd");
        System.out.println("signData length:" + signData.length);
        byte[] pubkey = HexUtil.decode("02b2c2595d41d30742d71d48ad728fd909406e3b5920af533ae578ec72647e082a");
        NulsSignData nulsSignData = new NulsSignData();
        nulsSignData.parse(signData,0);
//        System.out.println(nulsSignData.serialize().length);
//        System.out.println(HexUtil.encode(nulsSignData.serialize()));
//        P2PHKSignature sign = new P2PHKSignature(signData,pubkey);
//        System.out.println(sign.getBytes().length);
        String tx = "02009f32845e066e6f6e6f6e6f008c01170100017c3286ba1b3edcc237a3a9b13080f1bc7e99904001000100a096a6d4e8000000000000000000000000000000000000000000000000000000080000000000000000000117010001d5250c029d54bdefb3504d3b1912681627592a1f010001000010a5d4e800000000000000000000000000000000000000000000000000000000000000000000006921026e80e0b3816b92be5eea85f9c8b8c2f99ea8ef3ef96346ef49ddd40c08a2e207463044022052b211bd932126b0028f1eaa792efd116d5684f153fc177d727526963d9e7bea02201657e55aee9c044cc77d3d8729b06955db68f2c1245a2d6a5c7c1a9402295980";
        NulsByteBuffer byteBuffer = new NulsByteBuffer(HexUtil.decode(tx + HexUtil.encode(signData)));
        Transaction transaction = new Transaction();
        transaction.parse(byteBuffer);
        transaction.getCoinDataInstance();
        System.out.println("time:"+transaction.getTime());
        System.out.println("type:" + transaction.getType());
        System.out.println("remark:" + new String(transaction.getRemark()));
        System.out.println("coindata: " + JSONUtils.obj2json(transaction.getCoinDataInstance()));
        System.out.println("txHash:" + transaction.getHash().toHex());
        System.out.println("verify : " + ECKey.verify(transaction.getHash().getBytes(), signData, pubkey));
    }
}
