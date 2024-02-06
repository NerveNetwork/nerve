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

package io.nuls.transaction.service.impl;

import io.nuls.common.ConfigBean;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.service.TxService;

import java.math.BigInteger;
import java.util.*;

/**
 * Mass trading testing class Used forcmdcommand line
 *
 * @author: Charlie
 * @date: 2019/6/29
 */
@Component
public class TransferTestImpl {

    static String address20 = "TNVTdN9iAS9rt6f2Eu8LiYESwyMSt4HJU97o9";
    static String address21 = "TNVTdN9iHMKEpHdAGRzrk8jNucKSPxjem1qT2";
    static String address22 = "TNVTdN9iFDuRZ14Sota2dhU7eyLDS53DSo5Av";
    static String address23 = "TNVTdN9i7JazkRXoEsn5JrXV4rnpnYucVtxxh";
    static String address24 = "TNVTdN9iFTM1Xuz46ZVvbsnqxgNVzFGuhy73c";
    static String address25 = "TNVTdN9i47oDvee9N52NwHkr7uRvQWpowTW9W";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";

    /**
     * Empty address
     * tNULSeBaMm8Kp5u7WU5xnCJqLe8fRFD49aZQdK
     * tNULSeBaMigwBrvikwVwbhAgAxip8cTScwcaT8
     */
    private Chain chain;
    @Autowired
    private ChainManager chainManager;
    static int chainId = 4;
    static int assetChainId = 4;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";
    @Autowired
    TxService txService;

    public void importPriKeyTest() {
//        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//Seed block address tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//Seed block address tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe
        importPriKey("5396d5015edb6c8e6a4f9ac281d82b57a222fe712f5281a1e256929df78a46f0", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG
        importPriKey("e626c1e1541471d4f6e074762c811e3a5896be14bffabff059e2c54703252160", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD
        importPriKey("8686352e34eade8b2e5104a0931011cc5a67e72fb1d2679d430e944480260dc4", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
        importPriKey("534b684229baad90b8e5ccd4813154209fb5f5d84bd0dc048f0d9e25e432ae07", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
        importPriKey("b3e6ddf3368970f3f0bead71dcc194d84ff654edd22182324fb4d9647dada8f8", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
        importPriKey("0d71c0ac3a6cd924c012efefd283e27e703813871493f122692548ad61ce794a", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn
    }

    /**
     * Multiple address transfer
     */
    public void mAddressTransfer(String addressMoney) throws Exception {
        int count = 10000;
        Log.info("Create a transfer account...");
        List<String> list = createAddress(count);
        //Transfer funds to the newly generated account
        NulsHash hash = null;

        Log.info("Transaction account balance initialization...");
        for (int i = 0; i < count; i++) {
            String address = list.get(i);
            Map transferMap = this.createTransferTx(addressMoney, address, new BigInteger("8000000000"));
            Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                    (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
            Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", RPCUtil.encode(tx.serialize()));
            hash = tx.getHash();
            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
        }
        //sleep30second
        Thread.sleep(30000L);
        Log.info("Create receiving account...");
        List<String> listTo = createAddress(count);

        //Execute one transfer for each newly generated account
        Log.debug("{}", System.currentTimeMillis());
        int countTx = 0;
        Map<String, NulsHash> preHashMap = new HashMap<>();
        for (int x = 0; x < 50; x++) {
            Log.info("start Transfer {} pen,  * Section {} second", count, x + 1);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                String address = list.get(i);
                String addressTo = listTo.get(i);
                Map transferMap = this.createTransferTx(address, addressTo, new BigInteger("20000000"));
//                LoggerUtil.LOG.info("address={},addressTo={}", address, addressTo);
                Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                        (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), preHashMap.get(address));
                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("tx", RPCUtil.encode(tx.serialize()));
//                Log.debug("hash:" + tx.getHash().toHex());
                HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
                preHashMap.put(address, tx.getHash());
                countTx++;
            }
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            Log.info("tx count:{} - execution time:{} milliseconds,  about≈:{}seconds", count, executionTime, executionTime / 1000);
            Log.info("");
        }
        Log.info("Full completion time：{}, - total count:{}",
                DateUtils.timeStamp2DateStr(NulsDateUtils.getCurrentTimeMillis()), countTx);
    }


    public void mAddressTransferLjs(String addressMoney1, String addressMoney2, String amount) throws Exception {
        int count = 10000;
        if (null == amount) {
            amount = "500000000";
        }
        Log.info("Create a transfer account...");
        List<String> list1 = doAccountsCreateAndGiveMoney(count, new BigInteger(amount), addressMoney1);
        List<String> list2 = doAccountsCreateAndGiveMoney(count, new BigInteger(amount), addressMoney2);
        //sleep30second
        Thread.sleep(30000L);
        //Execute one transfer for each newly generated account
        long countTx = 0;
        Map<String, NulsHash> preHashMap = new HashMap<>();
        long x = 0;
        while (true) {
            x++;
            Log.info("start Transfer {} pen,  * Section {} second", countTx, x);
            long startTime = System.currentTimeMillis();
            countTx = countTx + doTrans(preHashMap, list1, list2, count);
            countTx = countTx + doTrans(preHashMap, list2, list1, count);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            Log.info("tx count:{} - execution time:{} milliseconds,  about≈:{}seconds", countTx, executionTime, executionTime / 1000);
        }
    }

    public void transfer(String address) throws Exception {
        while (true) {
            Thread.sleep(10L);
            Map transferMap = this.createTransferTx(address, address, new BigInteger("1000000"));
            //Calling interfaces
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("fail errorcode:{}", cmdResp.getResponseErrorCode());
                throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
            }
            HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("ac_transfer"));
            if (null == result) {
                throw new RuntimeException("result is null");
            }
            String hash = (String) result.get("value");
            chain.getLogger().info("{}", hash);
//            return hash;
        }
    }


    public void importPriKey(String priKey, String pwd) {
        try {
            //Overwrite if account already exists If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> doAccountsCreateAndGiveMoney(int addrCount, BigInteger amount, String richAddr) throws Exception {
        List<String> list = createAddress(addrCount);
        chain = chainManager.getChain(4);
        //Transfer funds to the newly generated account
        NulsHash hash = null;
        Log.info("Transaction account balance initialization...");
        for (int i = 0; i < addrCount; i++) {
            try {
                String address = list.get(i);
                Map transferMap = this.createTransferTx(richAddr, address, amount);
                Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                        (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("tx", RPCUtil.encode(tx.serialize()));
                txService.newTx(chain, tx);
                hash = tx.getHash();
//            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            } catch (Exception e) {
                Log.error(e);
                continue;
            }
        }
        //sleep30second
        Thread.sleep(60000L);
        return list;
    }

    private long doTrans(Map<String, NulsHash> preHashMap, List<String> list1, List<String> list2, int count) throws Exception {
        long countTx = 0;
        chain = chainManager.getChain(4);
        for (int i = 0; i < count; i++) {
            String address = list1.get(i);
            String addressTo = list2.get(i);
            try {
                Map transferMap = this.createTransferTx(address, addressTo, new BigInteger("1000000"));
                Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                        (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), preHashMap.get(address));
                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("tx", RPCUtil.encode(tx.serialize()));
                Thread.sleep(1L);
                txService.newTx(chain, tx);
//            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
                preHashMap.put(address, tx.getHash());
                countTx++;
            } catch (NulsException e) {
                Log.error(e);
                if (e.getErrorCode().getCode().equalsIgnoreCase(TxErrorCode.PAUSE_NEWTX.getCode())) {
                    //Transaction status is incorrect, sleep2sRe execute
                    Thread.sleep(2000L);
                } else if (e.getErrorCode().getCode().equalsIgnoreCase(TxErrorCode.ORPHAN_TX.getCode())) {
                    //Orphan transaction, retrieve from database againnonce
                    preHashMap.remove(address);
                }
                continue;
            } catch (Exception e) {
                Log.error(e);
                continue;
            }
        }
        return countTx;

    }


    private List<String> createAddress(int count) throws Exception {
        List<String> addressList = new ArrayList<>();
        if (100 <= count) {
            int c1 = count / 100;
            for (int i = 0; i < c1; i++) {
                List<String> list = createAccount(chainId, 100, password);
                addressList.addAll(list);
            }
            int c2 = count % 100;
            if (c2 > 0) {
                List<String> list = createAccount(chainId, c2, password);
                addressList.addAll(list);
            }
        } else if (100 > count) {
            List<String> list = createAccount(chainId, count, password);
            addressList.addAll(list);
        }
        return addressList;
    }

    public static List<String> createAccount(int chainId, int count, String password) {
        List<String> accountList = null;
        Response cmdResp = null;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, version);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("count", count);
            params.put("password", password);
            cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params, 60000L);
            accountList = (List<String>) ((HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_createAccount")).get("list");
        } catch (Exception e) {
            Log.error("cmdResp:{}", cmdResp);
            e.printStackTrace();
        }
        return accountList;
    }


    /**
     * Create a regular transfer transaction
     *
     * @return
     */
    private Map createTransferTx(String addressFrom, String addressTo, BigInteger amount) {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();
        CoinDTO inputCoin1 = new CoinDTO();
        inputCoin1.setAddress(addressFrom);
        inputCoin1.setPassword(password);
        inputCoin1.setAssetsChainId(chainId);
        inputCoin1.setAssetsId(assetId);
        inputCoin1.setAmount(new BigInteger("100000").add(amount));
        inputs.add(inputCoin1);

        CoinDTO outputCoin1 = new CoinDTO();
        outputCoin1.setAddress(addressTo);
        outputCoin1.setPassword(password);
        outputCoin1.setAssetsChainId(chainId);
        outputCoin1.setAssetsId(assetId);
        outputCoin1.setAmount(amount);
        outputs.add(outputCoin1);

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);
        return transferMap;
    }

    private Map createTransferTx2(String addressFrom, String addressTo, BigInteger amount) {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();
        CoinDTO inputCoin1 = new CoinDTO();
        inputCoin1.setAddress(addressFrom);
        inputCoin1.setPassword(password);
        inputCoin1.setAssetsChainId(chainId);
        inputCoin1.setAssetsId(assetId);
        inputCoin1.setAmount(new BigInteger("10000000").add(amount).add(amount));
        inputs.add(inputCoin1);

        CoinDTO outputCoin1 = new CoinDTO();
        outputCoin1.setAddress(addressTo);
        outputCoin1.setPassword(password);
        outputCoin1.setAssetsChainId(chainId);
        outputCoin1.setAssetsId(assetId);
        outputCoin1.setAmount(amount);
        CoinDTO outputCoin2 = new CoinDTO();
        outputCoin2.setAddress(addressTo);
        outputCoin2.setPassword(password);
        outputCoin2.setAssetsChainId(chainId);
        outputCoin2.setAssetsId(assetId);
        outputCoin2.setAmount(amount);
        outputs.add(outputCoin1);
        outputs.add(outputCoin2);
        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);
        return transferMap;
    }

    /**
     * Assembly transaction
     */
    private Transaction assemblyTransaction2(int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, String remark, NulsHash hash) throws NulsException {
        Transaction tx = new Transaction(2);
        tx.setTime(NulsDateUtils.getCurrentTimeMillis() / 1000);
        tx.setRemark(StringUtils.bytes(remark));
        try {
            //assembleCoinDataMiddlecoinFrom、coinTodata
            assemblyCoinData2(tx, chainId, fromList, toList, hash);
            //Calculate transaction data summary hash
            byte[] bytes = tx.serializeForHash();
            tx.setHash(NulsHash.calcHash(bytes));
            //establishECKeyUsed for signature
//            List<ECKey> signEcKeys = new ArrayList<>();
            TransactionSignature transactionSignature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            for (CoinDTO from : fromList) {
//                P2PHKSignature p2PHKSignature = AccountCall.signDigest(from.getAddress(), from.getPassword(), tx.getHash().getBytes());

                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("address", from.getAddress());
                params.put("password", password);
                params.put("data", RPCUtil.encode(tx.getHash().getBytes()));
                HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
                String signatureStr = (String) result.get("signature");

                P2PHKSignature signature = new P2PHKSignature(); // TxUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
                signature.parse(new NulsByteBuffer(RPCUtil.decode(signatureStr)));

                p2PHKSignatures.add(signature);
            }
            //Transaction signature
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(transactionSignature.serialize());
            return tx;

        } catch (Exception e) {
        }
        return tx;
    }

    /**
     * Assembly transaction
     */
    private Transaction assemblyTransaction(int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, String remark, NulsHash hash) throws NulsException {
        Transaction tx = new Transaction(2);
        tx.setTime(NulsDateUtils.getCurrentTimeMillis() / 1000);
        tx.setRemark(StringUtils.bytes(remark));
        try {
            //assembleCoinDataMiddlecoinFrom、coinTodata
            assemblyCoinData(tx, chainId, fromList, toList, hash);
            //Calculate transaction data summary hash
            byte[] bytes = tx.serializeForHash();
            tx.setHash(NulsHash.calcHash(bytes));
            //establishECKeyUsed for signature
//            List<ECKey> signEcKeys = new ArrayList<>();
            TransactionSignature transactionSignature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            for (CoinDTO from : fromList) {
//                P2PHKSignature p2PHKSignature = AccountCall.signDigest(from.getAddress(), from.getPassword(), tx.getHash().getBytes());

                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("address", from.getAddress());
                params.put("password", password);
                params.put("data", RPCUtil.encode(tx.getHash().getBytes()));
                HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
                String signatureStr = (String) result.get("signature");

                P2PHKSignature signature = new P2PHKSignature(); // TxUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
                signature.parse(new NulsByteBuffer(RPCUtil.decode(signatureStr)));

                p2PHKSignatures.add(signature);
            }
            //Transaction signature
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(transactionSignature.serialize());
            return tx;

        } catch (Exception e) {
        }
        return tx;
    }

    private Transaction assemblyCoinData2(Transaction tx, int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, NulsHash hash) throws NulsException {
        try {
            //assemblecoinFrom、coinTodata
            List<CoinFrom> coinFromList = assemblyCoinFrom(chainId, fromList, hash);
            List<CoinTo> coinToList = assemblyCoinTo2(chainId, toList);
            //The source address or transfer address is empty
            if (coinFromList.size() == 0 || coinToList.size() == 0) {
                return null;
            }
            //Total transaction size=Transaction data size+Signature data size
            int txSize = tx.size() + getSignatureSize(coinFromList);
            //assemblecoinDatadata
            CoinData coinData = getCoinData(chainId, coinFromList, coinToList, txSize);
            tx.setCoinData(coinData.serialize());
        } catch (Exception e) {
        }
        return tx;
    }

    private Transaction assemblyCoinData(Transaction tx, int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, NulsHash hash) throws NulsException {
        try {
            //assemblecoinFrom、coinTodata
            List<CoinFrom> coinFromList = assemblyCoinFrom(chainId, fromList, hash);
            List<CoinTo> coinToList = assemblyCoinTo(chainId, toList);
            //The source address or transfer address is empty
            if (coinFromList.size() == 0 || coinToList.size() == 0) {
                return null;
            }
            //Total transaction size=Transaction data size+Signature data size
            int txSize = tx.size() + getSignatureSize(coinFromList);
            //assemblecoinDatadata
            CoinData coinData = getCoinData(chainId, coinFromList, coinToList, txSize);
            tx.setCoinData(coinData.serialize());
        } catch (Exception e) {
        }
        return tx;
    }

    /**
     * assemblecoinTodata
     * assembly coinTo data
     * condition：toAll addresses in the must be addresses on the same chain
     *
     * @param listTo Initiator set coinTo
     * @return List<CoinTo>
     * @throws NulsException
     */
    private List<CoinTo> assemblyCoinTo(int chainId, List<CoinDTO> listTo) throws NulsException {
        List<CoinTo> coinTos = new ArrayList<>();
        for (CoinDTO coinDto : listTo) {
            String address = coinDto.getAddress();
            byte[] addressByte = AddressTool.getAddress(address);
            //The transfer transaction transfer address must be a local chain address
            if (!AddressTool.validAddress(chainId, address)) {
                Log.debug("failed");
            }
            //Check if the chain has the asset
            int assetsChainId = coinDto.getAssetsChainId();
            int assetId = coinDto.getAssetsId();
            //Check if the amount is less than0
            BigInteger amount = coinDto.getAmount();
            if (BigIntegerUtils.isLessThan(amount, BigInteger.ZERO)) {
                Log.debug("failed");
            }
            CoinTo coinTo = new CoinTo();
            coinTo.setAddress(addressByte);
            coinTo.setAssetsChainId(assetsChainId);
            coinTo.setAssetsId(assetId);
            coinTo.setAmount(coinDto.getAmount());
            coinTos.add(coinTo);
        }
        return coinTos;
    }

    private List<CoinTo> assemblyCoinTo2(int chainId, List<CoinDTO> listTo) throws NulsException {
        List<CoinTo> coinTos = new ArrayList<>();
        int i = 1;
        for (CoinDTO coinDto : listTo) {
            String address = coinDto.getAddress();
            byte[] addressByte = AddressTool.getAddress(address);
            //The transfer transaction transfer address must be a local chain address
            if (!AddressTool.validAddress(chainId, address)) {
                Log.debug("failed");
            }
            //Check if the chain has the asset
            int assetsChainId = coinDto.getAssetsChainId();
            int assetId = coinDto.getAssetsId();
            //Check if the amount is less than0
            BigInteger amount = coinDto.getAmount();
            if (BigIntegerUtils.isLessThan(amount, BigInteger.ZERO)) {
                Log.debug("failed");
            }
            CoinTo coinTo = new CoinTo();
            coinTo.setAddress(addressByte);
            coinTo.setAssetsChainId(assetsChainId);
            coinTo.setAssetsId(assetId);
            coinTo.setAmount(coinDto.getAmount());
            coinTos.add(coinTo);
            coinTo.setLockTime(NulsDateUtils.getCurrentTimeSeconds() + (i * 300));
            i++;
        }
        return coinTos;
    }

    private CoinData getCoinData(int chainId, List<CoinFrom> listFrom, List<CoinTo> listTo, int txSize) throws NulsException {
        CoinData coinData = new CoinData();
        coinData.setFrom(listFrom);
        coinData.setTo(listTo);
        return coinData;
    }

    /**
     * adoptcoinfromCalculate signature datasize
     * IfcoinfromCalculate only once if there are duplicate addresses；If there are multiple addresses, only calculatemAddressessize
     *
     * @param coinFroms
     * @return
     */
    private int getSignatureSize(List<CoinFrom> coinFroms) {
        int size = 0;
        Set<String> commonAddress = new HashSet<>();
        for (CoinFrom coinFrom : coinFroms) {
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            commonAddress.add(address);
        }
        size += commonAddress.size() * P2PHKSignature.SERIALIZE_LENGTH;
        return size;
    }

    /**
     * assemblecoinFromdata
     * assembly coinFrom data
     *
     * @param listFrom Initiator set coinFrom
     * @return List<CoinFrom>
     * @throws NulsException
     */
    private List<CoinFrom> assemblyCoinFrom(int chainId, List<CoinDTO> listFrom, NulsHash hash) throws NulsException {
        List<CoinFrom> coinFroms = new ArrayList<>();
        for (CoinDTO coinDto : listFrom) {
            String address = coinDto.getAddress();
            byte[] addressByte = AddressTool.getAddress(address);
            //Check if the chain has the asset
            int assetChainId = coinDto.getAssetsChainId();
            int assetId = coinDto.getAssetsId();

            //Check if the corresponding asset balance is sufficient
            BigInteger amount = coinDto.getAmount();
            //Query ledger to obtainnoncevalue
            byte[] nonce = getNonceByPreHash(createChain(), address, hash);
            CoinFrom coinFrom = new CoinFrom(addressByte, assetChainId, assetId, amount, nonce, (byte) 0);
            coinFroms.add(coinFrom);
        }
        return coinFroms;
    }

    public static byte[] getNonceByPreHash(Chain chain, String address, NulsHash hash) throws NulsException {
        if (hash == null) {
            return LedgerCall.getNonce(chain, address, assetChainId, assetId);
        }
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return out;
    }

    private Chain createChain() {
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);
        return chain;
    }


}
