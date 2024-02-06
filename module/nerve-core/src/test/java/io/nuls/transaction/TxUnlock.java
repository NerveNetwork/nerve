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

package io.nuls.transaction;

import io.nuls.common.ConfigBean;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

/**
 * Test transfer unlocking
 * @author: Charlie
 * @date: 2020/5/14
 */
public class TxUnlock {

    static String address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";


    private Chain chain;
    static int chainId = 2;
    static int assetChainId = 2;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";


    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId));
    }



    @Test
    public void assemblyLockTransaction() throws Exception {
        Transaction tx = assemblyTransaction(0);
        Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", RPCUtil.encode(tx.serialize()));
        TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
        NulsHash hash = tx.getHash();
        System.out.println("hash:" + hash.toHex());
    }

    /**
     * During testing, it is necessary to lock the transaction Printednonce Fill in to unlockfromin
     * @throws Exception
     */
    @Test
    public void assemblyUnlockTransaction() throws Exception {
        Transaction tx = assemblyTransaction(1);
        Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", RPCUtil.encode(tx.serialize()));
        TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
        NulsHash hash = tx.getHash();
        System.out.println("hash:" + hash.toHex());
    }


    public static Transaction assemblyTransaction(int ide) throws Exception {
        Transaction tx = new Transaction(2);
        tx.setTime(NulsDateUtils.getCurrentTimeMillis() / 1000);
        //assembleCoinDataMiddlecoinFrom、coinTodata
        if (ide == 0) {
            tx.setCoinData(getLock().serialize());
        } else if (ide == 1) {
            tx.setCoinData(getUnlock().serialize());
        }
        //Calculate transaction data summary hash
        byte[] bytes = tx.serializeForHash();
        tx.setHash(NulsHash.calcHash(bytes));
        //establishECKeyUsed for signature
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();

        Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address25);
        params.put("password", password);
        params.put("data", RPCUtil.encode(tx.getHash().getBytes()));
        HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
        String signatureStr = (String) result.get("signature");

        P2PHKSignature signature = new P2PHKSignature();
        signature.parse(new NulsByteBuffer(RPCUtil.decode(signatureStr)));

        p2PHKSignatures.add(signature);
        //Transaction signature
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
        return tx;
    }


    private static CoinData getLock() throws NulsException {
        CoinData coinData = new CoinData();
        List<CoinFrom> coinFroms = new ArrayList<>();
        String address = address25;
        byte[] addressByte = AddressTool.getAddress(address);

        //Check if the corresponding asset balance is sufficient
        BigInteger amount = new BigInteger("99900000000");
        //Query ledger to obtainnoncevalue
        byte[] nonce = getNonceByPreHash(createChain(), address, null);
        System.out.println("lock tx from nonce: " + HexUtil.encode(nonce));
        CoinFrom coinFrom = new CoinFrom(addressByte, assetChainId, assetId, amount.add(new BigInteger("100000")), nonce, (byte) 0);
        coinFroms.add(coinFrom);

        List<CoinTo> coinTos = new ArrayList<>();
        CoinTo coinTo = new CoinTo();
        coinTo.setAddress(addressByte);
        coinTo.setAssetsChainId(assetChainId);
        coinTo.setAssetsId(assetId);
        coinTo.setAmount(amount);
        coinTo.setLockTime(1599448812L);
        coinTos.add(coinTo);
        coinData.setFrom(coinFroms);
        coinData.setTo(coinTos);
        return coinData;
    }

    private static CoinData getUnlock() throws NulsException {
        CoinData coinData = new CoinData();
        List<CoinFrom> coinFroms = new ArrayList<>();
        String address = address25;
        byte[] addressByte = AddressTool.getAddress(address);

        //Check if the corresponding asset balance is sufficient
        BigInteger amount = new BigInteger("99900000000");
        
        /**
         * The locked transaction needs to be filled innonce
         */
        byte[] nonce = HexUtil.decode("67729674f1e105e7");
        CoinFrom coinFrom = new CoinFrom(addressByte, assetChainId, assetId, amount, nonce, (byte) -1);
        coinFroms.add(coinFrom);

        List<CoinTo> coinTos = new ArrayList<>();
        CoinTo coinTo = new CoinTo();
        String addressTo = address26;
        coinTo.setAddress(AddressTool.getAddress(addressTo));
        coinTo.setAssetsChainId(assetChainId);
        coinTo.setAssetsId(assetId);
        coinTo.setAmount(amount.subtract(new BigInteger("100000")));
        coinTos.add(coinTo);

        coinData.setFrom(coinFroms);
        coinData.setTo(coinTos);
        return coinData;
    }



    public static byte[] getNonceByPreHash(Chain chain, String address, NulsHash hash) throws NulsException {
        if (hash == null) {
            return LedgerCall.getNonce(chain, address, assetChainId, assetId);
        }
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        String nonce8BytesStr = HexUtil.encode(out);
        return HexUtil.decode(nonce8BytesStr);
    }

    private static Chain createChain() {
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);
        return chain;
    }

}
