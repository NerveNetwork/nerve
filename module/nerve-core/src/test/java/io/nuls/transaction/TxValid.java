/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
import io.nuls.base.data.po.BlockHeaderPo;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.token.AccountData;
import io.nuls.transaction.token.AccountDataBetaOne;
import io.nuls.transaction.token.TestJSONObj;
import io.nuls.transaction.tx.Transfer;
import io.nuls.transaction.utils.TxUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static io.nuls.transaction.TestCommonUtil.PASSWORD;
import static io.nuls.transaction.TestCommonUtil.createContract;
import static io.nuls.transaction.utils.LoggerUtil.LOG;
import static org.junit.Assert.assertNotNull;

/**
 * @author: Charlie
 * @date: 2019/3/11
 */
public class TxValid {

    /*
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

    static String address30 = "tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ";
    static String address31 = "tNULSeBaMrbmG67VrTJeZswv4P2uXXKoFMa6RH";
    */

    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";
    static String address30 = "TNVTdTSPRyiWcpbS65NmT5qyGmuqPxuKv8SF4";
    static String address31 = "TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9";

    /**
     * Empty address
     * tNULSeBaMm8Kp5u7WU5xnCJqLe8fRFD49aZQdK
     * tNULSeBaMigwBrvikwVwbhAgAxip8cTScwcaT8
     */
    private Chain chain;
    static int chainId = 5;
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

    private ExecutorService signExecutor = ThreadUtils.createThreadPool(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE, new NulsThreadFactory("THREAD_VERIFIY_BLOCK_TXS_TEST"));

    @Test
    public void importPriKeyTest() {
        // Public key: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//Seed block address tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd
        // Public key: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//Seed block address tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT
        // Public key: 028181b7534e613143befb67e9bd1a0fa95ed71b631873a2005ceef2774b5916df
//        importPriKey("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499", password);//Seed block address tNULSeBaMmShSTVwbU4rHkZjpD98JgFgg6rmhF TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx

        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24 TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1 TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2 TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf
    }
    @Test
    public void transferGo() throws Exception {
        for (int i = 0; i < 2; i++) {
            String hash = createTransfer(address31, address30, new BigInteger("10000000000"));
            //String hash = createCtxTransfer();
            System.out.println("hash:" + hash);
            System.out.println("count:" + (i + 1));
            System.out.println("");
//            Thread.sleep(500L);
        }
    }


    @Test
    public void createAccount() throws Exception{
        // Create an account and print public and private keys
        List<String> list = createAccount(chainId, 5, password);
        for(String a : list){
            getPriKeyByAddress(a);
        }
    }
    /**
     * Mixed issuance of contracts and ordinary transactions
     * @throws Exception
     */
    @Test
    public void transferAndContractPixelTest() throws Exception {
        String code = Files.readString(Path.of("E:\\ContractTest", "pixel.txt"));
        int size = 0;
        for (int i = 0; i < 1000; i++) {
            size++;
            String hash = createTransfer(address21, address29, new BigInteger("100000000"));
            //String hash = createCtxTransfer();
            System.out.println("transfer: " + hash);
            System.out.println("contract: " + createContract(address21, PASSWORD, code, new Object[]{size % 50 + 1}));
//            Thread.sleep(100L);
        }
    }

    @Test
    public void getTxs() throws Exception {
//        String txStr = (String)(getTxCfmClient("27491a5a3df9ee5ac105649f375dc95a27760655edc9c9b314f205e4296b7f4a").get("tx"));
        /*String txStr = (String)(getTxCfmClient("66cd6836ff12ebf3dfafd60f729468441ba5bfd9fc25beb3db3831e87fccb73c").get("tx"));
        Transaction tx = TxUtil.getInstance(txStr, Transaction.class);//The last one
        System.out.println(tx.format());*/

        boolean rs = (Boolean)(getIsTxCfm("02d549187ee5723ba9d0d8266bfd772b74116847c909fe740bd95ecc92639155").get("value"));
        System.out.println(rs);

//        TxUtil.txInformationDebugPrint(tx);
    }

    @Test
    public void scTestTransfer() throws Exception { //Payment for testing contract
        //pubkey: 02e33fd0d5db4dea80ab73df99d072959467145f5495498495c58527adc9b3585d
        importPriKey("38464747051e948965d667bd24696ffcce8fb680b2faa843273aa2d609a4d40b", password);//tNULSeBaMmcSM2rzbZ4HQzv1ge4KT828W3PFKx
        //pubkey: 02e52522c72c2003f18457b9c23b4d81558cfde3f444099070759a26dd7f72c083
        importPriKey("55eea92427e97fb26ab965087aba1326e44b3251923a8b1c4891eff58da43fc1", password);//tNULSeBaMvTdRV28tTLxn6T6nKShUfMvdEqtFP
        //pubkey: 039141d2063fbe0ee45ccd6a86fb23922b9e0a466b3b8ee1683ef5f3dc036c42a5
        importPriKey("1c47826a81bf7ee46092e4c76c5f6a6fdf5d6a193946270b157c8f9d625baaab", password);//tNULSeBaMjtpBCk5953nc81jr28rvLgsKENcQy
        //pubkey: 033c5b0fd1697b3fc20d56218fa5490cbce117fae9f7928ea783e44bd16b64c0e4
        importPriKey("41f2de5344e238a65b8e4f15afcab46465d641eea020d79761aa356144d1b7df", password);//tNULSeBaMrp9a1QMF3S6gR2JZnmEZq68sqgTLA
        //pubkey: 0357b14a877d2b0c031bde7e8b10a90a77969a00b62870e5f2cbe60ef73ffc265f
        importPriKey("5302cb0d14207abcbf2c2ffcacb2c8de2f6e522bfcfbbf0d9326828b4e1d7203", password);//tNULSeBaMvAj8L9ET1haM7FgWHsBPcNend21fS
        //pubkey: 0250ac9ba03e5a37e3b6ade9c5fdb552099949cb4b1819349f2c7a97a9c6cc0c56
        importPriKey("107c590e2a33df44218dac70177b52c814992b035f2f8b8d529470612a16fc93", password);//tNULSeBaMjhfQ1G2shvFF4QbCZeTJ5b23iTR2h
        //pubkey: 03d87a1fdc06a29fad59e99d8e249210bec188d167b2e7d813d5722d71fcca5582
        importPriKey("5060502d157c03e46ac7dd3ced2e4c456e55cc7b924c94c6203e4c3d7f46a6c4", password);//tNULSeBaMuAQZMiWXmvRhpJzC4aY4DEwqQrucQ
        importPriKey("3fd3381c647612a4cf19eac516d8c582e61eb99d8532ea11d0bae605b542f7ac", password);//tNULSeBaMph1RpK3FAJZcW9QoRa1G4jvC4VSA2
        importPriKey("1b6fffdb9eb61749b7b8466bc7996da9f889e2ca1fd332895ace170914dcf958", password);//tNULSeBaMf9iLnMzHWtvn3JGQ7r55ysbtWDUE1
        importPriKey("368c32b90f810cc4d2f83b374785bda919a5dfda88a1a34c56757107d8b1a28b", password);//tNULSeBaMmc8tip5c4pob9cd41CpnZDYS6beCm
        importPriKey("368c32b90f810cc4d2f83b374785bda919a5dfda88a1a34c56757107d8b1a28b", password);//tNULSeBaMszoF91eiFTb4UWU6q78jsG5jDbd91
        importPriKey("1660e4773adf225f167c1834085afc31aaea52e5b28fb76d251eab6e92c73116", password);//tNULSeBaMg9PdWwZfugBd1NJ7qxNGjWFc2XJtg
//        createTransfer(address20, "tNULSeBaMmcSM2rzbZ4HQzv1ge4KT828W3PFKx", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMvTdRV28tTLxn6T6nKShUfMvdEqtFP", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMjtpBCk5953nc81jr28rvLgsKENcQy", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMrp9a1QMF3S6gR2JZnmEZq68sqgTLA", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMvAj8L9ET1haM7FgWHsBPcNend21fS", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMjhfQ1G2shvFF4QbCZeTJ5b23iTR2h", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMuAQZMiWXmvRhpJzC4aY4DEwqQrucQ", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMph1RpK3FAJZcW9QoRa1G4jvC4VSA2", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMf9iLnMzHWtvn3JGQ7r55ysbtWDUE1", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMmc8tip5c4pob9cd41CpnZDYS6beCm", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMszoF91eiFTb4UWU6q78jsG5jDbd91", new BigInteger("20000000000000"));
//        createTransfer(address20, "tNULSeBaMg9PdWwZfugBd1NJ7qxNGjWFc2XJtg", new BigInteger("20000000000000"));
    }

    /**
     * Delete account
     */
    @Test
    public void removeAccountTest() throws Exception {
        removeAccount("tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp", password);
//        removeAccount(address20, password);
    }
    /**
     * Multiple address transfer
     */
    @Test
    public void mAddressTransfer() throws Exception {
        int count = 9999;
        Log.info("Create a transfer account...");
        List<String> list = createAddress(count);
        //Transfer funds to the newly generated account
        NulsHash hash = null;

        Log.info("Transaction account balance initialization...");
        for (int i = 0; i < count; i++) {
            String address = list.get(i);
            Map transferMap = this.createTransferTx(address23, address, new BigInteger("100000000000"));
            Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                    (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
            Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", RPCUtil.encode(tx.serialize()));
            hash = tx.getHash();
//            Log.debug("hash:" + hash.toHex());
            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
//            Log.debug("count:" + (i + 1));
//            Thread.sleep(1L);
        }
        //sleep30second
        Thread.sleep(30000L);
        Log.info("Create receiving account...");
        List<String> listTo = createAddress(count);

        //Execute one transfer for each newly generated account
        Log.debug("{}", System.currentTimeMillis());
        int countTx = 0;
        Map<String, NulsHash> preHashMap = new HashMap<>();
        for (int x = 0; x < 1000; x++) {
            Log.info("start Transfer {} pen,  * Section {} second", count, x + 1);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                String address = list.get(i);
                String addressTo = listTo.get(i);
                Map transferMap = this.createTransferTx(address, addressTo, new BigInteger("100000"));
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


    private List<String> doAccountsCreateAndGiveMoney(int addrCount, BigInteger amount, String richAddr) throws Exception {
        List<String> list = createAddress(addrCount);
        //Transfer funds to the newly generated account
        NulsHash hash = null;
        Log.info("Transaction account balance initialization...");
        for (int i = 0; i < addrCount; i++) {
            String address = list.get(i);
            Map transferMap = this.createTransferTx(richAddr, address, amount);
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
        return list;
    }

    private long doTrans(Map<String, NulsHash> preHashMap, List<String> list1, List<String> list2, int count) throws Exception {
        long countTx = 0;
        for (int i = 0; i < count; i++) {
            String address = list1.get(i);
            String addressTo = list2.get(i);
            Map transferMap = this.createTransferTx(address, addressTo, new BigInteger("1000000"));
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
        return countTx;

    }

    @Test
    public void mAddressTransferLjs() throws Exception {
        int count = 10000;
        Log.info("Create a transfer account...");
        List<String> list1 = doAccountsCreateAndGiveMoney(count, new BigInteger("500000000"), address21);
        List<String> list2 = doAccountsCreateAndGiveMoney(count, new BigInteger("500000000"), address21);
        //sleep30second
        Thread.sleep(30000L);
        //Execute one transfer for each newly generated account
        Log.debug("{}", System.currentTimeMillis());
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

    @Test
    public void multiThreadingTransfer() throws Exception {
        /** The number of transactions initiated by each thread */
        int txCount = 1000;
        long startTime = System.currentTimeMillis();
        Transfer transfer1 = new Transfer(address21, address22, txCount);
        Thread thread1 = new Thread(transfer1);
        thread1.start();

//        Transfer transfer2 = new Transfer(address26, address22, txCount);
//        Thread thread2 = new Thread(transfer2);
//        thread2.start();
//
//        Transfer transfer3 = new Transfer(address27, address23, txCount);
//        Thread thread3 = new Thread(transfer3);
//        thread3.start();
//
//        Transfer transfer4 = new Transfer(address28, address24, txCount);
//        Thread thread4 = new Thread(transfer4);
//        thread4.start();
//
//        Transfer transfer5 = new Transfer(address29, address24, txCount);
//        Thread thread5 = new Thread(transfer5);
//        thread5.start();

//        Transfer transfer6 = new Transfer(address20, address24, txCount);
//        Thread thread6 = new Thread(transfer6);
//        thread6.start();
//        Log.info("{}Thread execution in progress...", thread6.getName());

        thread1.join();
//        thread2.join();
//        thread3.join();
//        thread4.join();
//        thread5.join();
//        thread6.join();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        Log.info("Full completion time：{}, - total execution time:{} milliseconds,  about≈:{}seconds",
                DateUtils.timeStamp2DateStr(NulsDateUtils.getCurrentTimeMillis()), executionTime, executionTime / 1000);
    }


    /**
     * Set alias
     *
     * @throws Exception
     */
    @Test
    public void aliasTest() throws Exception {
        String alias = "charlie233";
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("password", password);
        params.put("alias", alias);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
        System.out.println("ac_setAlias result:" + JSONUtils.obj2json(cmdResp));
        assertNotNull(cmdResp);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_setAlias");
        String txHash = (String) result.get("txHash");
        Log.debug("alias-txHash:{}", txHash);
        /*****************************************************************/
        for (int i = 0; i < 1; i++) {
            String hash = createTransfer(address26, address20, new BigInteger("1000000000"));
            //String hash = createCtxTransfer();
            System.out.println("hash:" + hash);
            System.out.println("count:" + (i + 1));
            System.out.println("");
//            Thread.sleep(500L);
        }
    }

    @Test
    public void transfer() throws Exception {
        for (int i = 0; i < 1; i++) {
            String hash = createTransfer("tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm", "tNULSeBaNDW6r3RCaxLtBzo4QLPKPdWmDWgxAs", new BigInteger("200000000000000"));
            //String hash = createCtxTransfer();
            System.out.println("hash:" + hash);
            System.out.println("count:" + (i + 1));
            System.out.println("");
//            Thread.sleep(500L);
        }
    }


    @Test
    public void transferLocal() throws Exception {
        NulsHash hash = null;
        for (int i = 0; i < 10000; i++) {
            Map transferMap = this.createTransferTx(address23, address21, new BigInteger("1000000000"));

            Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                    (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
            Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", RPCUtil.encode(tx.serialize()));
            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            hash = tx.getHash();
            System.out.println("hash:" + hash.toHex());
            System.out.println("hash:" + hash);
            System.out.println("count:" + (i + 1));
            System.out.println("");
//            Thread.sleep(500L);
        }
    }


    @Test
    public void getBalance() throws Exception {

        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress(address21), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance));

    }

    @Test
    public void testSign() throws Exception {
        List<Transaction> list = createTx();
        sign(list);
        signMThread(list);

    }

    /**
     * Multithreading
     **/
    private void signMThread(List<Transaction> list) throws Exception {
        //tx_baseValidateTx
        long s1 = System.currentTimeMillis();
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Transaction tx : list) {
            Future<Boolean> res = signExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    //Perform basic validation without confirmation
                    try {
                         /*validateTxSignature(tx,null,chain);
                         return true;*/
                        //Verify only the basic content of a single transaction(TXModule Local Validation)
                        Map<String, Object> params = new HashMap<>();
                        params.put(Constants.VERSION_KEY_STR, "1.0");
                        params.put(Constants.CHAIN_ID, chainId);
                        params.put("tx", RPCUtil.encode(tx.serialize()));
                        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_baseValidateTx", params);
                        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("tx_baseValidateTx");
                        boolean rs = (boolean) result.get("value");
                        return rs;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            futures.add(res);
        }

        try {
            //Multithreaded processing results
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    Log.debug("failed, single tx verify failed");
//                   break;
                }
            }
        } catch (InterruptedException e) {
            Log.debug("batchVerify failed, single tx verify failed");
            Log.error(e);
        } catch (ExecutionException e) {
            Log.debug("batchVerify failed, single tx verify failed");
            Log.error(e);
        }
        Log.debug("{}Multi threaded execution time for transactions:{}", list.size(), System.currentTimeMillis() - s1);
    }

    /**
     * Single threaded
     **/
    private void sign(List<Transaction> list) throws Exception {
        long s1 = System.currentTimeMillis();
        for (Transaction tx : list) {
            //Perform basic validation without confirmation
            try {
                /*validateTxSignature(tx,null,chain);*/
                //Verify only the basic content of a single transaction(TXModule Local Validation)
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.VERSION_KEY_STR, "1.0");
                params.put(Constants.CHAIN_ID, chainId);
                params.put("tx", RPCUtil.encode(tx.serialize()));
                Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_baseValidateTx", params);
                HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("tx_baseValidateTx");
                boolean rs = (boolean) result.get("value");
                if (!rs) {
                    Log.debug("Verification signature failed type:{}, -hash:{}", tx.getType(), tx.getHash().toHex());
                    break;
                }
            } catch (Exception e) {
                Log.debug("Verification signature failed type:{}, -hash:{}", tx.getType(), tx.getHash().toHex());
            }
        }
        Log.debug("{}Single thread execution time for each transaction:{}", list.size(), System.currentTimeMillis() - s1);
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


    /**
     * Multiple address transfer
     * Jian Ge Edition
     */
    @Test
    public void mAddressTransfer2() throws Exception {
        int count = 10000;
        List<String> list = createAddress(count);
        //Transfer funds to the newly generated account
        NulsHash hash = null;
        for (int i = 0; i < count; i++) {
            String address = list.get(i);
            Map transferMap = this.createTransferTx(address23, address, new BigInteger("10000000000"));
            Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                    (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
            Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", RPCUtil.encode(tx.serialize()));
            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx_test", params);
            hash = tx.getHash();
            Log.debug("hash:" + hash.toHex());

            Log.debug("count:" + (i + 1));
            Thread.sleep(1L);
        }

        List<String> listTo = createAddress(count);

        //Execute one transfer for each newly generated account
        Log.debug("{}", System.currentTimeMillis());
        int countTx = 0;
        Map<String, NulsHash> preHashMap = new HashMap<>();
        for (int x = 0; x < 10000; x++) {
            long value = 10000000000L - 1000000 * (x + 1);
            for (int i = 0; i < count; i++) {
                String address = list.get(i);
                String addressTo = listTo.get(i);
                Map transferMap = this.createTransferTx(address, addressTo, new BigInteger("" + value));
                Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                        (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), preHashMap.get(address));
                Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
                params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
                params.put(Constants.CHAIN_ID, chainId);
                params.put("tx", RPCUtil.encode(tx.serialize()));
                HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx_test", params);
                Log.debug("hash:" + tx.getHash().toHex());
                Log.debug("count:" + (i + 1));
                preHashMap.put(address, tx.getHash());
                countTx++;
            }
            Log.debug("***********************");
            removeAccountList(list);
            list = listTo;
            listTo = createAddress(count);
            Thread.sleep(10000L);
        }
        Log.debug("{}", System.currentTimeMillis());
        Log.debug("count:{}", countTx);

    }


    private void removeAccountList(List<String> list) throws Exception {
        for (String address : list) {
            this.removeAccount(address, this.password);
        }
    }

    @Test
    public void mixedTransfer() throws Exception {
        String agentHash = null;
        String depositHash = null;
        String withdrawHash = null;
        String stopAgent = null;
        for (int i = 0; i < 20000; i++) {
            String hash = createTransfer(address25, address21, new BigInteger("10000000000"));
            switch (i) {
                case 0:
                    //Create nodes
                    agentHash = createAgent(address29, address27);
                    break;
                case 4000:
                    //entrust
                    depositHash = deposit(address29, agentHash);
                    break;
                case 16000:
                    //Cancel delegation
                    withdrawHash = withdraw(address29, depositHash);
                    break;
                case 19000:
                    //Stop node
                    stopAgent = stopAgent(address29);
                    break;
            }

            System.out.println("count:" + (i + 1));
            System.out.println("");

        }
    }

    @Test
    public void getTx() throws Exception {
//        getTxCfmClient("31f65fb2cc5e468b203f692291ea94f8559dca30878f9e1648c11601bf0cf7e1");
        String txStr = (String)(getTxCfmClient("0af0e6bb75df48ca1fa397d16bccfc9091e2ac8fa83713e72e30e6c8a7867be0").get("tx"));
        Transaction tx = TxUtil.getInstance(txStr, Transaction.class);//The last one
        txInformationDebugPrint(tx);
    }


    private void getTx(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        //Calling interfaces
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTx", params);
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("tx_getTx"));
        Assert.assertTrue(null != result);
        Log.debug("{}", JSONUtils.obj2PrettyJson(result));
       /* String txStr = (String) result.get("tx");
        LOG.debug("getTx -hash:{}", ((Transaction)TxUtil.getInstanceRpcStr(txStr, Transaction.class)).getHash().toHex());*/
    }


    @Test
    public void createAgentTx() throws Exception {
        createAgent(address20, "tNULSeBaMoRVvrr9noCDWwNNe3ZAbCvRWEPtij");
    }

    private String createAgent(String address, String packing) throws Exception {
        //Assemble and create node transactions
        Map agentTxMap = this.createAgentTx(address, packing);
        //Calling interfaces
        Response cmdResp2 = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", agentTxMap);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get("cs_createAgent"));
        Assert.assertTrue(null != result);
        String hash = (String) result.get("txHash");
        Log.debug("createAgent-txHash:{}", hash);
        return hash;
    }

    /**
     * Delegate node
     */
    @Test
    public void depositToAgent() throws Exception {
        //Assembly commission node transaction
        String hash = deposit(address20, "3f686219f3d2b5eed48c7bbc553a77a0bc5c59bef16f2a7cf7835a157c3c2d96");
    }

    private String deposit(String address, String agentHash) throws Exception {
        Map<String, Object> dpParams = new HashMap<>();
        dpParams.put(Constants.CHAIN_ID, chainId);
        dpParams.put("address", address);
        dpParams.put("password", password);
        dpParams.put("agentHash", agentHash);
        dpParams.put("deposit", 200000 * 100000000L);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
        HashMap dpResult = (HashMap) ((HashMap) dpResp.getResponseData()).get("cs_depositToAgent");
        String txHash = (String) dpResult.get("txHash");
        Log.debug("deposit-txHash:{}", txHash);
        return txHash;
    }

    /**
     * Cancel delegation
     *
     * @throws Exception
     */
    @Test
    public void withdraw() throws Exception {
        String hash = withdraw(address25, "0020b8a42eb4c70196189e607e9434fe09b595d5753711f21819113f40d64a1c82c1");
        Log.debug("withdraw-txHash:{}", hash);
    }

    private String withdraw(String address, String depositHash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        params.put("txHash", depositHash);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdraw", params);
        HashMap dpResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cs_withdraw");
        String hash = (String) dpResult.get("txHash");
        Log.debug("withdraw-txHash:{}", hash);
        return hash;
    }

    @Test
    public void stopAgentTx() throws Exception {
        String hash = stopAgent(address20);
        Log.debug("stopAgent-txHash:{}", hash);
    }

    private String stopAgent(String address) throws Exception {
        Map<String, Object> txMap = new HashMap();
        txMap.put("chainId", chainId);
        txMap.put("address", address);
        txMap.put("password", password);
        //Calling interfaces
        Response cmdResp2 = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", txMap);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get("cs_stopAgent"));
        String txHash = (String) result.get("txHash");
        Log.debug("stopAgent-txHash:{}", txHash);
        return txHash;
    }

    /**
     * Set alias
     *
     * @throws Exception
     */
    @Test
    public void alias() throws Exception {
        String alias = "charlie";
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address29);
        params.put("password", password);
        params.put("alias", alias);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
        System.out.println("ac_setAlias result:" + JSONUtils.obj2json(cmdResp));
        assertNotNull(cmdResp);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_setAlias");
        String txHash = (String) result.get("txHash");
        Log.debug("alias-txHash:{}", txHash);
    }

    private List<Transaction> createTx() throws Exception {
        List<Transaction> list = new ArrayList<>();
        NulsHash hash = null;
        System.out.println("satring......");
        for (int i = 0; i < 1; i++) {
            Map transferMap = this.createTransferTx(address25, address21, new BigInteger("1000000000"));

            Transaction tx = assemblyTransaction((int) transferMap.get(Constants.CHAIN_ID), (List<CoinDTO>) transferMap.get("inputs"),
                    (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
            //String hash = createCtxTransfer();
            hash = tx.getHash();
            list.add(tx);
            System.out.println("hash:" + hash.toHex());
            System.out.println("txHex:" + RPCUtil.encode(tx.serialize()));
        }

        System.out.println(list.size());
        return list;
    }

    /**
     * Check transactions
     */
    @Test
    public void getTxRecord() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address20);//tNULSeBaMk4LBr1y1tsneiHvy5H2Rc3Lns4QuN
        params.put("assetChainId", null);
        params.put("assetId", null);
        params.put("type", null);
        params.put("pageSize", null);
        params.put("pageNumber", null);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTxs", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug("Page<TransactionPO>:{}", JSONUtils.obj2PrettyJson(record));
    }

    /**
     * Check transactions
     */
    private void getTxClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug("{}", JSONUtils.obj2PrettyJson(record));
    }

    /**
     * Check transactions
     */
    private Map<String, Object> getTxCfmClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug(JSONUtils.obj2PrettyJson(record));
        return (Map)record.get("tx_getConfirmedTxClient");
    }

    private Map<String, Object> getIsTxCfm(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_isConfirmed", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug(JSONUtils.obj2PrettyJson(record));
        return (Map)record.get("tx_isConfirmed");
    }

    /**
     * Check blocks
     * Height orhash
     */
    @Test
    public void getBlocByHeight() throws Exception {
        getBlockHeaderPoByHeight(2L);
    }
    private void getBlockHeaderPoByHeight(long param) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("height", param);
        Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, "getBlockHeaderPoByHeight", params);
        Map record = (Map) dpResp.getResponseData();
        Map rs = (Map)record.get("getBlockHeaderPoByHeight");
        BlockHeaderPo blockHeaderPo = new BlockHeaderPo();
        blockHeaderPo.parse(new NulsByteBuffer(RPCUtil.decode((String)rs.get("value"))));
        Log.debug(JSONUtils.obj2PrettyJson(blockHeaderPo));
        for(NulsHash nulsHash : blockHeaderPo.getTxHashList()){
            Log.debug("txHash:{}", nulsHash.toHex());
        }
    }


    @Test
    public void getPriKeyByAddress() throws Exception {
        getPriKeyByAddress("tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL");
    }

    /**
     * Importkeystore
     */
    @Test
    public void importAccountByKeystoreFile() {
        String address = importAccountByKeystoreFile("C:/Users/Administrator/Desktop/2.0Test configuration and content/mainnet.keystore", "nuls1234");
//        String address2 = importAccountByKeystoreFile("C:/Users/Administrator/Desktop/2.0Test configuration and content/testnet.keystore", "nuls1234");
//        String address3 = importAccountByKeystoreFile("C:/Users/Administrator/Desktop/2.0Test configuration and content/tNULSeBaMm8Kp5u7WU5xnCJqLe8fRFD49aZQdK.keystore", "nuls123456");
        Log.info("address:{}", address);
//        Log.info("address:{}", address2);
//        Log.info("address:{}", address3);
    }

    private String createTransfer(String addressFrom, String addressTo, BigInteger amount) throws Exception {
        Map transferMap = this.createTransferTx(addressFrom, addressTo, amount);
        //Calling interfaces
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        if (!cmdResp.isSuccess()) {
            return "fail";
        }
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("ac_transfer"));
        Assert.assertTrue(null != result);
        String hash = (String) result.get("value");
//        Log.debug("{}", hash);
        return hash;
    }

    private Transaction getCreateTransferTx(String addressFrom, String addressTo, BigInteger amount) throws Exception {
        Map transferMap = this.createTransferTx(addressFrom, addressTo, amount);
        //Calling interfaces
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer_tx", transferMap);
        if (!cmdResp.isSuccess()) {
            return null;
        }
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("ac_transfer_tx"));
        Assert.assertTrue(null != result);
        String str = (String) result.get("value");
        Transaction tx = new Transaction();
        tx.parse(new NulsByteBuffer(RPCUtil.decode(str)));
        return tx;
    }

    private void getPriKeyByAddress(String address) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, version);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("password", password);
        params.put("address", address);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_getPriKeyByAddress", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_getPriKeyByAddress");
        String priKey = (String) result.get("priKey");
        String pubKey = (String) result.get("pubKey");
        System.out.println("address:" + address);
        System.out.println("priKey:" +  priKey);
        System.out.println("pubKey:" +  pubKey);
    }

    public static String importAccountByKeystoreFile(String filePath, String password) {
        String address = null;
        try {
            File file = new File(filePath);
            byte[] bytes = copyToByteArray(file);
            String keyStoreStr = new String(bytes, "UTF-8");

            //AccountKeyStoreDto accountKeyStoreDto = JSONUtils.json2pojo(new String(HexUtil.decode(keyStoreHexStr)), AccountKeyStoreDto.class);

            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, version);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("keyStore", RPCUtil.encode(bytes));
            params.put("password", password);
            params.put("overwrite", true);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByKeystore", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByKeystore");
            address = (String) result.get("address");
            //assertEquals(accountList.get(0), address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    public static byte[] copyToByteArray(File in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        InputStream input = null;
        try {
            input = new FileInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            int byteCount = 0;
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = input.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return out.toByteArray();
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
    }

    public static void importPriKey(String priKey, String pwd) {
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

    public void removeAccount(String address, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_removeAccount", params);
        Log.debug("{}", JSONUtils.obj2json(cmdResp.getResponseData()));
    }


    //    @Test
    public void packableTxs() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        long endTime = System.currentTimeMillis() + 10000L;
        System.out.println("endTime: " + endTime);
        params.put("endTimestamp", endTime);
        params.put("maxTxDataSize", 2 * 1024 * 1024L);
        Response response = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_packableTxs", params);
        Assert.assertTrue(null != response.getResponseData());
        Map map = (HashMap) ((HashMap) response.getResponseData()).get("tx_packableTxs");
        Assert.assertTrue(null != map);
        List<String> list = (List) map.get("list");
        Log.debug("packableTxs:");
        for (String s : list) {
            Log.debug(s);
        }
    }

    private List<CoinDTO> createFromCoinDTOList() {
        CoinDTO coinDTO = new CoinDTO();
        coinDTO.setAssetsId(assetId);
        coinDTO.setAssetsChainId(assetChainId);
        coinDTO.setAddress(address25);
        coinDTO.setAmount(new BigInteger("200000000"));
        coinDTO.setPassword(password);

        CoinDTO coinDTO2 = new CoinDTO();
        coinDTO2.setAssetsId(assetId);
        coinDTO2.setAssetsChainId(assetChainId);
        coinDTO2.setAddress(address26);
        coinDTO2.setAmount(new BigInteger("100000000"));
        coinDTO2.setPassword(password);
        List<CoinDTO> listFrom = new ArrayList<>();
        listFrom.add(coinDTO);
        listFrom.add(coinDTO2);
        return listFrom;
    }


    private List<CoinDTO> createToCoinDTOList() {
        CoinDTO coinDTO = new CoinDTO();
        coinDTO.setAssetsId(assetId);
        coinDTO.setAssetsChainId(23);
        coinDTO.setAddress("NULSd6Hgfj4PyqSuYBEJth3zEG32sjYsUGsVA");
        coinDTO.setAmount(new BigInteger("200000000"));

        CoinDTO coinDTO2 = new CoinDTO();
        coinDTO2.setAssetsId(assetId);
        coinDTO2.setAssetsChainId(23);
        coinDTO2.setAddress("NULSd6HggkGBiHrUAL4YGErUFiMb2DkB5QQus");
        coinDTO2.setAmount(new BigInteger("100000000"));
        List<CoinDTO> listTO = new ArrayList<>();
        listTO.add(coinDTO);
        listTO.add(coinDTO2);
        return listTO;
    }


    /**
     * Assembly transaction
     */
    public static Transaction assemblyTransaction(int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, String remark, NulsHash hash) throws NulsException {
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

    private static Transaction assemblyCoinData(Transaction tx, int chainId, List<CoinDTO> fromList, List<CoinDTO> toList, NulsHash hash) throws NulsException {
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

    private static CoinData getCoinData(int chainId, List<CoinFrom> listFrom, List<CoinTo> listTo, int txSize) throws NulsException {
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
    private static int getSignatureSize(List<CoinFrom> coinFroms) {
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
    private static List<CoinFrom> assemblyCoinFrom(int chainId, List<CoinDTO> listFrom, NulsHash hash) throws NulsException {
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

    private static Chain createChain() {
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);
        return chain;
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


    /**
     * assemblecoinTodata
     * assembly coinTo data
     * condition：toAll addresses in the must be addresses on the same chain
     *
     * @param listTo Initiator set coinTo
     * @return List<CoinTo>
     * @throws NulsException
     */
    private static List<CoinTo> assemblyCoinTo(int chainId, List<CoinDTO> listTo) throws NulsException {
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

    /**
     * Create a regular transfer transaction
     *
     * @return
     */
    public static Map createTransferTx(String addressFrom, String addressTo, BigInteger amount) {
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

    //    String txt = txt2String(new File("E:\\remark.txt"));
    public static String txt2String(File file) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));//Construct aBufferedReaderClass to read files
            String s = null;
            while ((s = br.readLine()) != null) {//applyreadLineMethod, read one line at a time
                result.append(System.lineSeparator() + s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * Create nodes
     */
    public Map createAgentTx(String agentAddr, String packingAddr) {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", agentAddr);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("deposit", 20000 * 100000000L);
        params.put("commissionRate", 10);
        params.put("packingAddress", packingAddr);
        params.put("password", password);
        params.put("rewardAddress", agentAddr);
        return params;
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
     * alpah2 Distribute test coins
     **/
    @Test
    public void accountToken() throws Exception {
        TestJSONObj testJSONObj = new TestJSONObj();
        List<AccountData> accountDataList = testJSONObj.readStream();
        createTransfer(address20, address29, new BigInteger("999999900000000"));
        createTransfer(address21, address29, new BigInteger("999999900000000"));
        createTransfer(address22, address29, new BigInteger("999999900000000"));
        createTransfer(address23, address29, new BigInteger("999999900000000"));
        createTransfer(address24, address29, new BigInteger("999999900000000"));
        createTransfer(address25, address29, new BigInteger("999999900000000"));
        createTransfer(address26, address29, new BigInteger("999999900000000"));
        createTransfer(address27, address29, new BigInteger("999999900000000"));
        createTransfer(address28, address29, new BigInteger("999999900000000"));
        Thread.sleep(20000L);
        for (AccountData ac : accountDataList) {
            String hash = createTransfer(address29, ac.getAddress(), new BigInteger(String.valueOf(ac.getTotalBalance())));
            System.out.println(ac.getAddress() + " : " + ac.getTotalBalance() + " : " + hash);
        }
        Thread.sleep(20000L);
        BigInteger balanceTotal = LedgerCall.getBalance(chain, AddressTool.getAddress(address29), assetChainId, assetId);
        createTransfer(address29, "tNULSeBaMfwpGBmn8xuKABPWUbdtsM2cMoinnn", balanceTotal.subtract(new BigInteger("2000000000")));
        Thread.sleep(20000L);
        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress("tNULSeBaMfwpGBmn8xuKABPWUbdtsM2cMoinnn"), assetChainId, assetId);
        System.out.println(balance);

    }


    /**
     * beta1/beta2 Distribute test coins
     **/
    @Test
    public void accountTokenBeta() throws Exception {
        TestJSONObj testJSONObj = new TestJSONObj();
        List<AccountDataBetaOne> accountDataList = testJSONObj.readStreamBetaOne();
        createTransfer(address20, address29, new BigInteger("999999900000000"));
        createTransfer(address21, address29, new BigInteger("999999900000000"));
        createTransfer(address22, address29, new BigInteger("999999900000000"));
        createTransfer(address23, address29, new BigInteger("999999900000000"));
        createTransfer(address24, address29, new BigInteger("999999900000000"));
        createTransfer(address25, address29, new BigInteger("999999900000000"));
        createTransfer(address26, address29, new BigInteger("999999900000000"));
        createTransfer(address27, address29, new BigInteger("999999900000000"));
        createTransfer(address28, address29, new BigInteger("999999900000000"));
        createTransfer(address29, "tNULSeBaMtGu7vkqGzmcLHSPo2nTZQw4j8uzgr", new BigInteger("100000000000000"));
        Thread.sleep(20000L);
        for (AccountDataBetaOne ac : accountDataList) {
            String hash = createTransfer(address29, ac.getAddressV2(), new BigInteger(String.valueOf(ac.getBalance())));
            System.out.println(ac.getAddressV2() + " : " + ac.getBalance() + " : " + hash);
        }
        Thread.sleep(20000L);
        BigInteger balanceTotal = LedgerCall.getBalance(chain, AddressTool.getAddress(address29), assetChainId, assetId);
        String hash = createTransfer(address29, "tNULSeBaMoG1oaW1JZnh6Ly65Ttp6raeTFBfCG", balanceTotal.subtract(new BigInteger("200000000")));
        System.out.println(hash);
        Thread.sleep(20000L);
        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress("tNULSeBaMoG1oaW1JZnh6Ly65Ttp6raeTFBfCG"), assetChainId, assetId);
        System.out.println(balance);
        balance = LedgerCall.getBalance(chain, AddressTool.getAddress("tNULSeBaMh39UuZCPDffqiM6SHqesh5aKnm4LR"), assetChainId, assetId);
        System.out.println(balance);
        balance = LedgerCall.getBalance(chain, AddressTool.getAddress("tNULSeBaMrLc5oxqm7kd5mNZCc7366ojE4QR59"), assetChainId, assetId);
        System.out.println(balance);

    }



    public void txInformationDebugPrint(Transaction tx) {
        if (tx.getType() == 1) {
            return;
        }
        LOG.debug("");
        LOG.debug("**************************************************");
        LOG.debug("Transaction information");
        LOG.debug("type: {}", tx.getType());
        LOG.debug("txHash: {}", tx.getHash().toHex());
        LOG.debug("time: {}", NulsDateUtils.timeStamp2DateStr(tx.getTime()));
        LOG.debug("size: {}B,  -{}KB, -{}MB",
                String.valueOf(tx.getSize()), String.valueOf(tx.getSize() / 1024), String.valueOf(tx.getSize() / 1024 / 1024));
        byte[] remark = tx.getRemark();
        try {
            String remarkStr = remark == null ? "" : new String(tx.getRemark(), "UTF-8");
            LOG.debug("remark: {}", remarkStr);
        } catch (UnsupportedEncodingException e) {
            LOG.error(e);
        }

        CoinData coinData = null;
        try {
            if (tx.getCoinData() != null) {
                coinData = tx.getCoinDataInstance();
            }
        } catch (NulsException e) {
            LOG.error(e);
        }
        if (coinData != null) {
            LOG.debug("coinData:");
            List<CoinFrom> coinFromList = coinData.getFrom();
            if (coinFromList == null) {
                LOG.debug("\tcoinFrom: null");
            } else if (coinFromList.size() == 0) {
                LOG.debug("\tcoinFrom: size 0");
            } else {
                LOG.debug("\tcoinFrom: ");
                for (int i = 0; i < coinFromList.size(); i++) {
                    CoinFrom coinFrom = coinFromList.get(i);
                    LOG.debug("\tFROM_{}:", i);
                    LOG.debug("\taddress: {}", AddressTool.getStringAddressByBytes(coinFrom.getAddress()));
                    LOG.debug("\tamount: {}", coinFrom.getAmount());
                    LOG.debug("\tassetChainId: [{}]", coinFrom.getAssetsChainId());
                    LOG.debug("\tassetId: [{}]", coinFrom.getAssetsId());
                    LOG.debug("\tnonce: {}", HexUtil.encode(coinFrom.getNonce()));
                    LOG.debug("\tlocked(0Ordinary transactions,-1Unlock amount transaction（Exit consensus, exit delegation)): [{}]", coinFrom.getLocked());
                    LOG.debug("");
                }
            }

            List<CoinTo> coinToList = coinData.getTo();
            if (coinToList == null) {
                LOG.debug("\tcoinTo: null");
            } else if (coinToList.size() == 0) {
                LOG.debug("\tcoinTo: size 0");
            } else {
                LOG.debug("\tcoinTo: ");
                for (int i = 0; i < coinToList.size(); i++) {
                    CoinTo coinTo = coinToList.get(i);
                    LOG.debug("\tTO_{}:", i);
                    LOG.debug("\taddress: {}", AddressTool.getStringAddressByBytes(coinTo.getAddress()));
                    LOG.debug("\tamount: {}", coinTo.getAmount());
                    LOG.debug("\tassetChainId: [{}]", coinTo.getAssetsChainId());
                    LOG.debug("\tassetId: [{}]", coinTo.getAssetsId());
                    LOG.debug("\tlocked(Unlocking height or unlocking time,-1To permanently lock): [{}]", coinTo.getLockTime());
                    LOG.debug("");
                }
            }

        } else {
            LOG.debug("coinData: null");
        }
        LOG.debug("**************************************************");
        LOG.debug("");
    }


    public static Map createTransferCVTx(int assetChainIdActive, int assetIdActive, String addressFrom, String addressTo, BigInteger amount) {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();
        CoinDTO inputCoin1 = new CoinDTO();
        inputCoin1.setAddress(addressFrom);
        inputCoin1.setPassword(password);
        inputCoin1.setAssetsChainId(assetChainIdActive);
        inputCoin1.setAssetsId(assetIdActive);
        inputCoin1.setAmount(amount);

        inputs.add(inputCoin1);
        CoinDTO inputCoin2 = new CoinDTO();
        inputCoin2.setAddress(addressFrom);
        inputCoin2.setPassword(password);
        inputCoin2.setAssetsChainId(chainId);
        inputCoin2.setAssetsId(assetId);
        inputCoin2.setAmount(new BigInteger("100000"));
        inputs.add(inputCoin2);
        CoinDTO outputCoin1 = new CoinDTO();
        outputCoin1.setAddress(addressTo);
        outputCoin1.setPassword(password);
        outputCoin1.setAssetsChainId(assetChainIdActive);
        outputCoin1.setAssetsId(assetIdActive);
        outputCoin1.setAmount(amount);
        outputs.add(outputCoin1);

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);
        return transferMap;
    }

    @Test
    public void transferCV() throws Exception {
        Map transferMap = this.createTransferCVTx(
                101,
                1,
                "tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ",
                "tNULSeBaMrQaVh1V7LLvbKa5QSN54bS4sdbXaF",
                new BigInteger("200000000000000000"));
        //Calling interfaces
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("ac_transfer"));
        Assert.assertTrue(null != result);
        String hash = (String) result.get("value");
//        Log.debug("{}", hash);
        System.out.println(hash);
    }
}
