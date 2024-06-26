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

package io.nuls.account.tx;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.RpcConstant;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.bo.Chain;
import io.nuls.account.model.bo.tx.AccountBlockExtend;
import io.nuls.account.model.bo.tx.AccountBlockInfo;
import io.nuls.account.model.bo.tx.txdata.AccountBlockData;
import io.nuls.account.model.dto.CoinDTO;
import io.nuls.account.util.AccountTool;
import io.nuls.account.util.LoggerUtil;
import io.nuls.account.util.TxUtil;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.common.ConfigBean;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.I18nUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.model.dto.RpcResultError;
import io.nuls.v2.util.HttpClientUtil;
import io.nuls.v2.util.JsonRpcUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author: Charlie
 * @date: 2019/4/22
 */
public class Transfer implements Runnable {

    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";// 9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";// 477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";// 8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";// 4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";// bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";// ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";// 4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";// 3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";// 27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";// 76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b

    static int chainId = 5;
    static int assetChainId = 5;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";

    private String addressFrom;

    private String addressTo;

    public Transfer(){}

    //public Transfer(String addressFrom, String addressTo) {
    //    this.addressFrom = addressFrom;
    //    this.addressTo = addressTo;
    //}

    @BeforeClass
    public static void initClass() {
        Log.info("init log.");
        I18nUtils.loadLanguage(Transfer.class, "languages", "en");
    }

    @Before
    public void before() throws Exception {
        AddressTool.addPrefix(5, "TNVT");
        //NoUse.mockModule();
        //ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8771");
    }

    @Test
    public void createAccount() throws NulsException {
        Account account = AccountTool.createAccount(1);
        System.out.println(HexUtil.encode(account.getPriKey()));
        System.out.println(HexUtil.encode(account.getPubKey()));
        System.out.println(account.getAddress().toString());
    }

    @Test
    public void createMultiSigAccountTest() throws Exception {
        //create 3 account
        List<Account> accountList = new ArrayList<>();
        accountList.add(AccountTool.createAccount(2));
        accountList.add(AccountTool.createAccount(2));
        accountList.add(AccountTool.createAccount(2));

        Map<String, Object> params = new HashMap<>();
        List<String> pubKeys = new ArrayList<>();
        for (Account account:accountList ) {
            System.out.println(HexUtil.encode(account.getPriKey()));
            pubKeys.add(HexUtil.encode(account.getPubKey()));
        }
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("pubKeys", pubKeys);
        params.put("minSigns", 2);
        //create the multi sign accout
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createMultiSignAccount", params);
        assertNotNull(cmdResp);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_createMultiSignAccount");
        assertNotNull(result);
        String address = (String) result.get("address");
        assertNotNull(address);
        int resultMinSigns = (int) result.get("minSign");
        assertEquals(resultMinSigns,2);
        List<String> resultPubKeys = (List<String>) result.get("pubKeys");
        assertNotNull(resultPubKeys);
        assertEquals(pubKeys.size(),3);
    }

    String fromStr,rpcAddress;
    String fromKey;
    private void setDev() {
        chainId = 5;
        assetChainId = 5;
        assetId = 1;
        fromKey = "4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a";
        // TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(fromKey));
        byte[] addressByPrikey = AddressTool.getAddress(ecKey.getPubKey(), chainId);;
        fromStr = AddressTool.getStringAddressByBytes(addressByPrikey);
    }

    private void setTest() {
        chainId = 5;
        assetChainId = 5;
        assetId = 1;
        fromStr = "TNVTdTSQ32SVZvkY4tM5WHgM1mzRznkiqm2is";
        rpcAddress = "http://beta.api.nerve.network/jsonrpc";
    }

    private void setMain() {
        chainId = 9;
        assetChainId = 9;
        assetId = 1;
        fromStr = "NERVEepb6hz6zSAPu7YkxyM68M6omUkwAJpHmt";
        rpcAddress = "https://api.nerve.network/jsonrpc";
    }

    private static final String ID = "id";
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String DEFAULT_ID = "1";
    private static final String JSONRPC_VERSION = "2.0";
    static final int TIMEOUT_MILLIS = 5000;

    protected static RpcResult request(String requestURL, String method, List<Object> params) {
        RpcResult rpcResult;
        try {
            Map<String, Object> map = new HashMap<>(8);
            map.put(ID, DEFAULT_ID);
            map.put(JSONRPC, JSONRPC_VERSION);
            map.put(METHOD, method);
            map.put(PARAMS, params);
            String resultStr = post(requestURL, map);
            rpcResult = JSONUtils.json2pojo(resultStr, RpcResult.class);
        } catch (Exception e) {
            Log.error(e);
            rpcResult = RpcResult.failed(new RpcResultError(CommonCodeConstanst.DATA_ERROR.getCode(), e.getMessage(), null));
        }
        return rpcResult;
    }

    private static void setPostParams(HttpPost httpPost, Map<String, Object> params) throws Exception {
        //设置请求参数
        String json = JSONUtils.obj2json(params);
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");//发送json需要设置contentType
        httpPost.setEntity(entity);
    }

    private static String post(String url, Map<String, Object> params) throws Exception {
        CloseableHttpResponse response = null;
        try {
            HttpPost httppost = new HttpPost(url);
            HttpHost proxy = new HttpHost("127.0.0.1", 1087);
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT_MILLIS).setProxy(proxy)
                    .setSocketTimeout(TIMEOUT_MILLIS).setConnectTimeout(TIMEOUT_MILLIS).build();
            httppost.setConfig(requestConfig);
            setPostParams(httppost, params);
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            response = httpClient.execute(httppost,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();

            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void accountBlockMultiSignTest() throws Exception {
        setMain();
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);

        Transaction tx = new Transaction();
        tx.setType(TxType.BLOCK_ACCOUNT);
        CoinData coinData = new CoinData();

        byte[] from = AddressTool.getAddress(fromStr);

        byte[] nonce;
        RpcResult request = request(rpcAddress, "getAccountBalance", List.of(chainId, assetChainId, assetId, fromStr));
        Map result = (Map) request.getResult();
        String nonceStr = (String) result.get("nonce");
        if(null == nonceStr){
            nonce = HexUtil.decode("0000000000000000");
        } else {
            nonce = HexUtil.decode(nonceStr);
        }
        coinData.addFrom(new CoinFrom(from, assetChainId, assetId, new BigDecimal("0").movePointRight(8).toBigInteger(), nonce, (byte) 0));
        coinData.addTo(new CoinTo(from, assetChainId, assetId, BigInteger.ZERO, (byte) 0));
        tx.setCoinData(coinData.serialize());
        AccountBlockData data = new AccountBlockData();
        //File file = new File("/Users/pierreluo/Nuls/account_new_nerve");
        //List<String> list = IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8.name());
        //System.out.println("read length: " + list.size());
        //Set<String> set = list.stream().map(a -> a.trim()).collect(Collectors.toSet());
        //System.out.println("deduplication length: " + set.size());
        //data.setAddresses(set.toArray(new String[set.size()]));
        data.setAddresses(new String[]{
                //"NERVEepb6DAjxbCKZZ4sEoytDbRNhvz4ZEk98s"
                "NERVEepb6xT575oWK38pVy2xVV4dxrHP53aaLV"
        });
        tx.setTxData(data.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        System.out.println(String.format("Transaction size: %s", tx.size()));

        String[] pubkeys = new String[]{
                "0225a6a872a4110c9b9c9a71bfdbe896e04bc83bb9fe38e27f3e18957d9b2a25ad",
                "029f8ab66d157ddfd12d89986833eb2a8d6dc0d92c87da12225d02690583ae1020",
                "02784d89575c16f9407c7218f8ca6c6a80d44023cd37796fc5458cbce1ede88adb",
                "020aee2c9cde73f50c5e2eef756b92aeb138bc3cda3438b31a68b56f16004bebf8",
                "02b2e32f94116d2364af6f06ae9af7f58824b0d3a57fca9170b1a36b665aa93195"};
        List<String> pubkeyList = Arrays.asList(pubkeys);
        List<byte[]> collect = pubkeyList.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
        transactionSignature.setM((byte) 3);
        transactionSignature.setPubKeyList(collect);
        tx.setTransactionSignature(transactionSignature.serialize());

        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add("???");
        for (String pri : priKeyList) {
            ECKey eckey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(pri)));
            P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx, eckey);
            p2PHKSignatures.add(p2PHKSignature);
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        }
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    @Test
    public void accountBlockMultiSignProtocol19Test() throws Exception {
        setTest();
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);

        Transaction tx = new Transaction();
        tx.setType(TxType.BLOCK_ACCOUNT);
        CoinData coinData = new CoinData();

        byte[] from = AddressTool.getAddress(fromStr);

        byte[] nonce;
        RpcResult request = JsonRpcUtil.request(rpcAddress, "getAccountBalance", List.of(chainId, assetChainId, assetId, fromStr));
        Map result = (Map) request.getResult();
        String nonceStr = (String) result.get("nonce");
        if(null == nonceStr){
            nonce = HexUtil.decode("0000000000000000");
        } else {
            nonce = HexUtil.decode(nonceStr);
        }
        coinData.addFrom(new CoinFrom(from, assetChainId, assetId, new BigDecimal("0").movePointRight(8).toBigInteger(), nonce, (byte) 0));
        coinData.addTo(new CoinTo(from, assetChainId, assetId, BigInteger.ZERO, (byte) 0));
        tx.setCoinData(coinData.serialize());
        List<Object[]> blockDatas = new ArrayList<>();
        // Lock List: Address, operation type(1-Join the whitelist 2-Remove whitelist)List of whitelist transaction types
        blockDatas.add(new Object[]{"TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw", 1, List.of(2, 3)});
        blockDatas.add(new Object[]{"TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv", 1, List.of(61)});
        blockDatas.add(new Object[]{"TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD", 1, List.of(71)});
        blockDatas.add(new Object[]{"TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5", 1, List.of()});
        AccountBlockData data = this.makeTxData(blockDatas);

        tx.setTxData(data.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        System.out.println(String.format("Transaction size: %s", tx.size()));

        String[] pubkeys = new String[]{
                "0225a6a872a4110c9b9c9a71bfdbe896e04bc83bb9fe38e27f3e18957d9b2a25ad",
                "029f8ab66d157ddfd12d89986833eb2a8d6dc0d92c87da12225d02690583ae1020",
                "02784d89575c16f9407c7218f8ca6c6a80d44023cd37796fc5458cbce1ede88adb",
                "020aee2c9cde73f50c5e2eef756b92aeb138bc3cda3438b31a68b56f16004bebf8",
                "02b2e32f94116d2364af6f06ae9af7f58824b0d3a57fca9170b1a36b665aa93195"};
        List<String> pubkeyList = Arrays.asList(pubkeys);
        List<byte[]> collect = pubkeyList.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
        transactionSignature.setM((byte) 3);
        transactionSignature.setPubKeyList(collect);
        tx.setTransactionSignature(transactionSignature.serialize());

        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add("???");
        for (String pri : priKeyList) {
            ECKey eckey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(pri)));
            P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx, eckey);
            p2PHKSignatures.add(p2PHKSignature);
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        }
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    @Test
    public void accountUnBlockMultiSignTest() throws Exception {
        setMain();
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);

        Transaction tx = new Transaction();
        tx.setType(TxType.UNBLOCK_ACCOUNT);
        CoinData coinData = new CoinData();
        byte[] from = AddressTool.getAddress(fromStr);

        byte[] nonce;
        RpcResult request = JsonRpcUtil.request(rpcAddress, "getAccountBalance", List.of(chainId, assetChainId, assetId, fromStr));
        Map result = (Map) request.getResult();
        String nonceStr = (String) result.get("nonce");
        if(null == nonceStr){
            nonce = HexUtil.decode("0000000000000000");
        } else {
            nonce = HexUtil.decode(nonceStr);
        }
        coinData.addFrom(new CoinFrom(from, assetChainId, assetId, new BigDecimal("0").movePointRight(8).toBigInteger(), nonce, (byte) 0));
        coinData.addTo(new CoinTo(from, assetChainId, assetId, BigInteger.ZERO, (byte) 0));
        tx.setCoinData(coinData.serialize());
        AccountBlockData data = new AccountBlockData();
        data.setAddresses(new String[]{
                "NERVEepb6BCfps7sCDBQejzU73YGxTpJhKpa9L"
        });
        tx.setTxData(data.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));

        String[] pubkeys = new String[]{
                "0225a6a872a4110c9b9c9a71bfdbe896e04bc83bb9fe38e27f3e18957d9b2a25ad",
                "029f8ab66d157ddfd12d89986833eb2a8d6dc0d92c87da12225d02690583ae1020",
                "02784d89575c16f9407c7218f8ca6c6a80d44023cd37796fc5458cbce1ede88adb",
                "020aee2c9cde73f50c5e2eef756b92aeb138bc3cda3438b31a68b56f16004bebf8",
                "02b2e32f94116d2364af6f06ae9af7f58824b0d3a57fca9170b1a36b665aa93195"};
        List<String> pubkeyList = Arrays.asList(pubkeys);
        List<byte[]> collect = pubkeyList.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
        transactionSignature.setM((byte) 3);
        transactionSignature.setPubKeyList(collect);
        tx.setTransactionSignature(transactionSignature.serialize());

        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add("???");
        for (String pri : priKeyList) {
            ECKey eckey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(pri)));
            P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx, eckey);
            p2PHKSignatures.add(p2PHKSignature);
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        }
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    @Test
    public void appendSignature() throws Exception {
        setTest();
        String pri = fromKey;
        String txHex = "02007c95ef6103616263008c0117050001e45dc2d5ea7c4216baa277e5a7cee4ef6eb70c3405000100a0b6f07dba020000000000000000000000000000000000000000000000000000082fb49ecf9fd58fc4000117050001b9978dbea4abebb613b6be2d0d66b4179d2511cb050001000030ef7dba02000000000000000000000000000000000000000000000000000000000000000000006921020e19418ed26700b0dba720dcc95483cb4adb1b5f8a103818dab17d5b052318544630440220378c8235b9277bfab4be5d5216198143d2fc3759e4a002184e9463b303c3261b022012bac6e85b2601570cb66f8e2f4d2abb8cb099eb2db6a9aeb34c784af6288d25";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex), 0);
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.parse(tx.getTransactionSignature(), 0);
        List<P2PHKSignature> p2PHKSignatures = transactionSignature.getP2PHKSignatures();
        ECKey eckey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(pri)));
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx, eckey);
        p2PHKSignatures.add(p2PHKSignature);
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    @Test
    public void txMultiSignTest() throws Exception {
        String pri = "???";
        //String filePath = "???";
        //String txHex = IoUtils.readBytesToString(new File(filePath));
        String txHex = "4f00f4f26e6400510200264e45525645657062364361325a69756369715935516b4337736f62376b525457744642597556264e455256456570623635506765393271335855426f74557a57707842326f4d35654268677775008c01170900039c807be394f715e68d2bc25590e005237e22a4f20900010000000000000000000000000000000000000000000000000000000000000000000899f718208f93365d0001170900039c807be394f715e68d2bc25590e005237e22a4f20900010000000000000000000000000000000000000000000000000000000000000000000000000000000000fd16010305210225a6a872a4110c9b9c9a71bfdbe896e04bc83bb9fe38e27f3e18957d9b2a25ad21029f8ab66d157ddfd12d89986833eb2a8d6dc0d92c87da12225d02690583ae10202102784d89575c16f9407c7218f8ca6c6a80d44023cd37796fc5458cbce1ede88adb21020aee2c9cde73f50c5e2eef756b92aeb138bc3cda3438b31a68b56f16004bebf82102b2e32f94116d2364af6f06ae9af7f58824b0d3a57fca9170b1a36b665aa931952102b2e32f94116d2364af6f06ae9af7f58824b0d3a57fca9170b1a36b665aa93195473045022100867811e9fcbdf81d2498f0104fdb9a73bc60f347cd255d958ec25bb0a9a678c102204687a8185ec0b4bfd1ee300b88dd6fdcfdcea8795587b44de6ed32ab02547629";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex), 0);

        MultiSignTxSignature transactionSignature = new MultiSignTxSignature();
        transactionSignature.parse(tx.getTransactionSignature(), 0);

        List<P2PHKSignature> p2PHKSignatures = transactionSignature.getP2PHKSignatures();

        ECKey eckey = ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(pri)));
        P2PHKSignature p2PHKSignature = SignatureUtil.createSignatureByEckey(tx, eckey);
        p2PHKSignatures.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    @Test
    public void getAllBlockAccount() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_getAllBlockAccount", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void getBlockAccountInfo() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw");
        list.add("TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv");
        list.add("TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD");
        list.add("TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5");

        for (String address : list) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_getBlockAccountInfo", params);
            System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        }
    }

    @Test
    public void accountBlockTest() throws Exception {
        setDev();
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);

        Transaction tx = new Transaction();
        tx.setType(TxType.BLOCK_ACCOUNT);
        CoinData coinData = new CoinData();
        byte[] from = AddressTool.getAddress(fromStr);
        byte[] nonce = TxUtil.getBalanceNonce(chain, assetChainId, assetId, from).getNonce();
        if(null == nonce){
            nonce = HexUtil.decode("0000000000000000");
        }
        coinData.addFrom(new CoinFrom(from, assetChainId, assetId, new BigDecimal("0.001").movePointRight(8).toBigInteger(), nonce, (byte) 0));
        coinData.addTo(new CoinTo(from, assetChainId, assetId, BigInteger.ZERO, (byte) 0));
        tx.setCoinData(coinData.serialize());
        List<Object[]> blockDatas = new ArrayList<>();
        blockDatas.add(new Object[]{"TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw", 1, List.of(2, 3)});
        blockDatas.add(new Object[]{"TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv", 1, List.of(61)});
        blockDatas.add(new Object[]{"TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD", 1, List.of(71)});
        blockDatas.add(new Object[]{"TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5", 1, List.of()});
        AccountBlockData data = this.makeTxData(blockDatas);
        tx.setTxData(data.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);

        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        //Obtained based on passwordECKey get ECKey from Password
        ECKey ecKey =  ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(fromKey)));
        byte[] signBytes = SignatureUtil.signDigest(tx.getHash().getBytes(), ecKey).serialize();
        P2PHKSignature signature = new P2PHKSignature(signBytes, ecKey.getPubKey()); // TxUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        p2PHKSignatures.add(signature);
        //Transaction signature
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
        Response response = this.newTx(tx);
        System.out.println(JSONUtils.obj2PrettyJson(response));
    }

    private int[] list2array(List<Integer> list) {
        int[] result = new int[list.size()];
        int i = 0;
        for(Integer a : list) {
            result[i++] = a.intValue();
        }
        return result;
    }
    private AccountBlockData makeTxData(List<Object[]> list) throws Exception {
        List<AccountBlockInfo> infoList = new ArrayList<>();
        String[] addresses = new String[list.size()];
        int i = 0;
        for (Object[] objs : list) {
            String address = (String) objs[0];
            Integer operationType = (Integer) objs[1];
            List<Integer> types = (List<Integer>) objs[2];
            addresses[i++] = address;
            AccountBlockInfo info = new AccountBlockInfo();
            info.setOperationType(operationType);
            info.setTypes(this.list2array(types));
            infoList.add(info);
        }
        AccountBlockData data = new AccountBlockData();
        data.setAddresses(addresses);
        AccountBlockExtend extend = new AccountBlockExtend();
        extend.setInfos(infoList.toArray(new AccountBlockInfo[infoList.size()]));
        data.setExtend(extend.serialize());
        return data;
    }

    @Test
    public void accountUnBlockTest() throws Exception {
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(chainId);
        configBean.setAssetId(assetId);
        chain.setConfig(configBean);

        Transaction tx = new Transaction();
        tx.setType(TxType.UNBLOCK_ACCOUNT);
        CoinData coinData = new CoinData();
        String fromKey = "477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75";
        byte[] from = AddressTool.getAddress("TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz");
        byte[] nonce = TxUtil.getBalanceNonce(chain, assetChainId, assetId, from).getNonce();
        if(null == nonce){
            nonce = HexUtil.decode("0000000000000000");
        }
        coinData.addFrom(new CoinFrom(from, assetChainId, assetId, new BigDecimal("0.001").movePointRight(8).toBigInteger(), nonce, (byte) 0));
        coinData.addTo(new CoinTo(from, assetChainId, assetId, BigInteger.ZERO, (byte) 0));
        tx.setCoinData(coinData.serialize());
        AccountBlockData data = new AccountBlockData();
        data.setAddresses(new String[]{
                "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw"
        });
        tx.setTxData(data.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);

        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        //Obtained based on passwordECKey get ECKey from Password
        ECKey ecKey =  ECKey.fromPrivate(new BigInteger(1, HexUtil.decode(fromKey)));
        byte[] signBytes = SignatureUtil.signDigest(tx.getHash().getBytes(), ecKey).serialize();
        P2PHKSignature signature = new P2PHKSignature(signBytes, ecKey.getPubKey()); // TxUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        p2PHKSignatures.add(signature);
        //Transaction signature
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
        Response response = this.newTx(tx);
        System.out.println(JSONUtils.obj2PrettyJson(response));
    }

    @Test
    public void blockTest() {
        Set<String> nodes = new HashSet<>();
        for (int i=7396000;i<7399782;i++) {
            System.out.println(String.format("load block header: %s", i));
            RpcResult request = JsonRpcUtil.request("https://api.nerve.network/jsonrpc", "getHeaderByHeight", List.of(1, Long.valueOf(i)));
            Map result = (Map) request.getResult();
            String packingAddress = (String) result.get("packingAddress");
            Integer blockVersion = (Integer) result.get("blockVersion");
            if (blockVersion.intValue() < 11) {
                nodes.add(packingAddress);
            }
        }
        nodes.stream().forEach(n -> System.out.println(n));
    }

    @Override
    public void run() {
        try {
            NulsHash hash = null;
            for (int i = 0; i < 1; i++) {
                hash = transfer(hash);
                System.out.println("count:" + (i + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NulsHash transfer(NulsHash hash) throws Exception{
        //Map transferMap = CreateTx.createTransferTx(addressFrom, addressTo, new BigInteger("1000000000"));
        Map transferMap = CreateTx.createAssetsTransferTx(addressFrom, addressTo);
        Transaction tx = CreateTx.assemblyTransaction((List<CoinDTO>) transferMap.get("inputs"),
                (List<CoinDTO>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
        newTx(tx);
        LoggerUtil.LOG.info("hash:" + tx.getHash().toHex());
//        LoggerUtil.LOG.info("count:" + (i + 1));
//        LoggerUtil.LOG.info("");
//        System.out.println("hash:" + hash.toHex());
        return tx.getHash();
    }


    private Response newTx(Transaction tx)  throws Exception{
        Map<String, Object> params = new HashMap<>(AccountConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, RpcConstant.TX_NEW_VERSION);
        params.put(RpcConstant.TX_CHAIN_ID, chainId);
        params.put(RpcConstant.TX_DATA, RPCUtil.encode(tx.serialize()));
        return NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
    }
}
