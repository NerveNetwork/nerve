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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author Niels
 */
public class TxSignTest {
    public static void main(String[] args) throws NulsException {
        String txHex = "e5007a8bd45e007a07174ccb22d8b990d51a2a9d3e54c43c36afe1c189c47b8c87434b199939e21104000198980dd5d44f66d249c1e16bdf96e392d88309390100e1f505000000000000000000000000000000000000000000000000000000000050d6dc010000000000000000000000000000000000000000000000000000000000d2021704000198980dd5d44f66d249c1e16bdf96e392d8830939040002000050d6dc01000000000000000000000000000000000000000000000000000000082eb33b6db9beaee7001704000198980dd5d44f66d249c1e16bdf96e392d883093904000100a086010000000000000000000000000000000000000000000000000000000000082eb33b6db9beaee700011704000198980dd5d44f66d249c1e16bdf96e392d8830939040002000050d6dc01000000000000000000000000000000000000000000000000000000feffffffffffffff6a2103af80fe5894a4bc170abdad90bbaad38840576136b3fc498a291bb92335c4ad0e473045022100f9166111735ba879bd3072249d66c5505bdfb6d789671444202a3474c6afba3f0220649320415ad42d7261ca21e123b429595c0c711d0a7fefa5bebfa433d1743020";
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
        boolean result = SignatureUtil.validateCtxSignture(tx);
        System.out.println("sign : "+AddressTool.getAddressString(sig.getP2PHKSignatures().get(0).getPublicKey(),4));
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
