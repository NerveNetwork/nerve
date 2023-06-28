/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.base.signture;


import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.NulsSignData;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * 交易签名工具类
 * Transaction Signature Tool Class
 *
 * @author tag
 * 2018/10/10
 */
@Component
public class SignatureUtil {

    private static final int MAIN_CHAIN_ID = 1;

    /**
     * 验证交易中所有签名正确性
     *
     * @param chainId 当前链ID
     * @param tx      交易
     */
    public static boolean validateTransactionSignture(int chainId, Transaction tx) throws NulsException {
        // 判断硬分叉,需要一个高度
        try {
            if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
                throw new NulsException(new Exception());
            }
            if (!tx.isMultiSignTx()) {
                TransactionSignature transactionSignature = new TransactionSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                if ((transactionSignature.getP2PHKSignatures() == null || transactionSignature.getP2PHKSignatures().size() == 0)) {
                    throw new NulsException(new Exception("Transaction unsigned ！"));
                }
                for (P2PHKSignature signature : transactionSignature.getP2PHKSignatures()) {
                    if (!ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                        throw new NulsException(new Exception("Transaction signature error !"));
                    }
                }

            } else {
                MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                if ((transactionSignature.getP2PHKSignatures() == null || transactionSignature.getP2PHKSignatures().size() == 0)) {
                    throw new NulsException(new Exception("Transaction unsigned ！"));
                }
                List<P2PHKSignature> validSignatures = transactionSignature.getValidSignature();
                int validCount = 0;
                for (P2PHKSignature signature : validSignatures) {
                    if (ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                        validCount++;
                    }
                }
                if (validCount < transactionSignature.getM()) {
                    throw new NulsException(new Exception("Transaction signature error !"));
                }
            }

        } catch (NulsException e) {
            Log.error("TransactionSignature parse error!");
            throw e;
        }
        return true;
    }

    /**
     * 跨链交易验证签名
     *
     * @param tx 交易
     */
    public static boolean ctxSignatureValid(int chainId, Transaction tx) throws NulsException {
        if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
            throw new NulsException(new Exception());
        }
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.parse(tx.getTransactionSignature(), 0);
        if ((transactionSignature.getP2PHKSignatures() == null || transactionSignature.getP2PHKSignatures().size() == 0)) {
            throw new NulsException(new Exception("Transaction unsigned ！"));
        }
        Set<String> fromAddressSet = tx.getCoinDataInstance().getFromAddressList();
        int signCount = tx.getCoinDataInstance().getFromAddressCount();
        int passCount = 0;
        String signAddress;
        for (P2PHKSignature signature : transactionSignature.getP2PHKSignatures()) {
            if (!ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                throw new NulsException(new Exception("Transaction signature error !"));
            }
            signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chainId));
            if (!fromAddressSet.contains(signAddress)) {
                continue;
            }
            fromAddressSet.remove(signAddress);
            passCount++;
            if (passCount >= signCount && fromAddressSet.isEmpty()) {
                break;
            }
        }
        if (passCount < signCount || !fromAddressSet.isEmpty()) {
            throw new NulsException(new Exception("Transaction signature error !"));
        }
        return true;
    }

    /**
     * 跨链交易验证签名
     *
     * @param tx 交易
     */
    public static boolean validateCtxSignture(Transaction tx) throws NulsException {
        if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
            if (tx.getType() == TxType.VERIFIER_INIT || tx.getType() == TxType.VERIFIER_CHANGE) {
                return true;
            }
            return false;
        }
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.parse(tx.getTransactionSignature(), 0);
        for (P2PHKSignature signature : transactionSignature.getP2PHKSignatures()) {
            if (!ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                throw new NulsException(new Exception("Transaction signature error !"));
            }
        }
        return true;
    }

    /**
     * 验证数据签名
     *
     * @param digestBytes
     * @param p2PHKSignature
     * @return
     * @throws NulsException
     */
    public static boolean validateSignture(byte[] digestBytes, P2PHKSignature p2PHKSignature) throws NulsException {
        if (null == p2PHKSignature) {
            throw new NulsException(new Exception("P2PHKSignature is null!"));
        }
        if (ECKey.verify(digestBytes, p2PHKSignature.getSignData().getSignBytes(), p2PHKSignature.getPublicKey())) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws NulsException {
        byte[] hash = HexUtil.decode("02002103200bda89e4116392aa5b939d739e6c9358600c0f8c1790dd4f284591b285de70143130362e35352e3234342e3136303a3137303031210308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260143130362e39312e3230352e3231303a3137303031");
        byte[] bytes = HexUtil.decode("210308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db7819826046304402206964e8b8cfb0b2879cddc837ed849a0a7d50d4e65b16e890e7057e262bc79a8e0220350319ba333d99358aa792f7cd8ec2c2133ed89654dde657284d102019fd5c68");
        P2PHKSignature _sign = new P2PHKSignature();
        _sign.parse(bytes, 0);
        boolean b = ECKey.verify(hash,_sign.getSignData().getSignBytes(),_sign.getPublicKey());
        System.out.println(b);
    }

    /**
     * 判断交易是否存在某地址
     *
     * @param tx 交易
     */
    public static boolean containsAddress(Transaction tx, byte[] address, int chainId) throws NulsException {
        Set<String> addressSet = getAddressFromTX(tx, chainId);
        if (addressSet == null || addressSet.size() == 0) {
            return false;
        }
        return addressSet.contains(AddressTool.getStringAddressByBytes(address));
    }

    /**
     * 获取交易签名地址
     *
     * @param tx 交易
     */
    public static Set<String> getAddressFromTX(Transaction tx, int chainId) throws NulsException {
        Set<String> addressSet = new HashSet<>();
        if (tx.getTransactionSignature() == null || tx.getTransactionSignature().length == 0) {
            return null;
        }
        try {
            List<P2PHKSignature> p2PHKSignatures;
            if (tx.isMultiSignTx()) {
                MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                p2PHKSignatures = transactionSignature.getP2PHKSignatures();
            } else {
                TransactionSignature transactionSignature = new TransactionSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                p2PHKSignatures = transactionSignature.getP2PHKSignatures();
            }

            if ((p2PHKSignatures == null || p2PHKSignatures.size() == 0)) {
                return null;
            }
            for (P2PHKSignature signature : p2PHKSignatures) {
                if (signature.getPublicKey() != null && signature.getPublicKey().length != 0) {
                    addressSet.add(AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chainId)));
                }
            }
        } catch (NulsException e) {
            Log.error("TransactionSignature parse error!");
            throw e;
        }
        return addressSet;
    }

    /**
     * 生成交易TransactionSignture
     *
     * @param tx         交易
     * @param signEckeys 需要生成普通签名的秘钥
     */
    public static void createTransactionSignture(Transaction tx, List<ECKey> signEckeys) throws IOException {
        if (signEckeys == null || signEckeys.size() == 0) {
            Log.error("TransactionSignature signEckeys is null!");
            throw new NullPointerException();
        }
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = null;
        try {
            p2PHKSignatures = createSignaturesByEckey(tx, signEckeys);
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(transactionSignature.serialize());
        } catch (IOException e) {
            Log.error("TransactionSignature serialize error!");
            throw e;
        }
    }

    /**
     * 生成交易多个传统签名（多地址转账可能会用到）
     *
     * @param tx     交易
     * @param eckeys 秘钥列表
     */
    public static List<P2PHKSignature> createSignaturesByEckey(Transaction tx, List<ECKey> eckeys) {
        List<P2PHKSignature> signatures = new ArrayList<>();
        for (ECKey ecKey : eckeys) {
            signatures.add(createSignatureByEckey(tx, ecKey));
        }
        return signatures;
    }

    public static List<P2PHKSignature> createSignaturesByEckey(NulsHash hash, List<ECKey> eckeys) {
        List<P2PHKSignature> signatures = new ArrayList<>();
        for (ECKey ecKey : eckeys) {
            signatures.add(createSignatureByEckey(hash, ecKey));
        }
        return signatures;
    }

    /**
     * 生成交易的签名传统
     *
     * @param tx     交易
     * @param priKey 私钥
     */
    public static P2PHKSignature createSignatureByPriKey(Transaction tx, String priKey) {
        ECKey ecKey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(priKey)));
        P2PHKSignature p2PHKSignature = new P2PHKSignature();
        p2PHKSignature.setPublicKey(ecKey.getPubKey());
        //用当前交易的hash和账户的私钥账户
        p2PHKSignature.setSignData(signDigest(tx.getHash().getBytes(), ecKey));
        return p2PHKSignature;
    }

    /**
     * 生成交易的签名传统
     *
     * @param tx    交易
     * @param ecKey 秘钥
     */
    public static P2PHKSignature createSignatureByEckey(Transaction tx, ECKey ecKey) {
        P2PHKSignature p2PHKSignature = new P2PHKSignature();
        p2PHKSignature.setPublicKey(ecKey.getPubKey());
        //用当前交易的hash和账户的私钥账户
        p2PHKSignature.setSignData(signDigest(tx.getHash().getBytes(), ecKey));
        return p2PHKSignature;
    }


    public static P2PHKSignature createSignatureByEckey(NulsHash hash, ECKey ecKey) {
        P2PHKSignature p2PHKSignature = new P2PHKSignature();
        p2PHKSignature.setPublicKey(ecKey.getPubKey());
        //用当前交易的hash和账户的私钥账户
        p2PHKSignature.setSignData(signDigest(hash.getBytes(), ecKey));
        return p2PHKSignature;
    }

    /**
     * 多重签名脚本签名验证
     *
     * @param digestBytes 验证的签名数据
     * @param signtures   签名列表
     */
    public static boolean validMultiScriptSign(byte[] digestBytes, LinkedList<byte[]> signtures, LinkedList<byte[]> pubkeys) {
        while (signtures.size() > 0) {
            byte[] pubKey = pubkeys.pollFirst();
            if (ECKey.verify(digestBytes, signtures.getFirst(), pubKey)) {
                signtures.pollFirst();
            }
            if (signtures.size() > pubkeys.size()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成交易签名
     *
     * @param digest 需要签名的交易数据
     * @param ecKey  签名的私钥
     */
    public static NulsSignData signDigest(byte[] digest, ECKey ecKey) {
        byte[] signbytes = ecKey.sign(digest);
        NulsSignData nulsSignData = new NulsSignData();
        nulsSignData.setSignBytes(signbytes);
        return nulsSignData;
    }


}
