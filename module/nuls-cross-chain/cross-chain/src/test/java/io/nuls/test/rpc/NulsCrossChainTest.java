package io.nuls.test.rpc;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.crosschain.base.model.dto.input.CoinDTO;
import network.nerve.constant.NulsCrossChainConfig;
import network.nerve.model.bo.Chain;
import network.nerve.model.bo.config.ConfigBean;
import network.nerve.utils.LoggerUtil;
import network.nerve.utils.TxUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NulsCrossChainTest {
    static int assetChainId = 2;
    static int assetId = 1;
    static String version = "1.0";
    static int chainId = 2;
    static String password = "nuls123456";

    static String main_address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String main_address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String main_address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String main_address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String main_address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String main_address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String main_address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String main_address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String main_address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String main_address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";


    static String local_address1 = "8CPcA7kaXSHbWb3GHP7bd5hRLFu8RZv57rY9w";
    static String local_address2 = "8CPcA7kaj56TWAC3Cix64aYCU3XFoNpu1LN1K";
    static String local_address3 = "8CPcA7kaiDAkvVP28GwXR6eP2oDKPcnPnmvLD";
    static String local_address4 = "8CPcA7kaZDdGEzXe8gwQNQg4u4teecArHt9Dy";
    static String local_address5 = "8CPcA7kaW82Eoj9wyLr96g2uBhHtFqD9Vy4yM";
    static String local_address6 = "8CPcA7kaUW98RW3g7erqTNT7b1gyoaqwxFEY3";
    static String local_address7 = "8CPcA7kaZTXgqBR7DYVbsj8yWUD2sZah6kknY";
    static String local_address8 = "8CPcA7kaaw7jvfn93Zrf7vNwtXyRQMY71zdYF";
    static String local_address9 = "8CPcA7kaUvrGb68gYWcceJRY2Mx2KfUTMJmgB";
    static String local_address10 = "8CPcA7kag8NijwHK8eTJVVMGXjfkT3GDAVo7n";

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
    }

    @Test
    public void batchCreateCtx() throws Exception{
        for(int i = 0;i<= 1000 ;i++){
            String hash = createCtx();
            String tx = getTx(hash);
            while (tx == null || tx.isEmpty()){
                Thread.sleep(100);
                tx = getTx(hash);
            }
            Log.info("第{}笔交易，hash{}",i,hash);
        }
    }

    private String getTx(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        //调用接口
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTx", params);
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("tx_getTx"));
        return (String)result.get("tx");
    }

    @SuppressWarnings("unchecked")
    private String createCtx(){
        try{
            List<CoinDTO> fromList = new ArrayList<>();
            List<CoinDTO> toList = new ArrayList<>();
            fromList.add(new CoinDTO("tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm",assetChainId,assetId, BigInteger.valueOf(100000000L),password));
            toList.add(new CoinDTO("GDMcKEW9i43HACuvkRNFLJgV4yQjXZQhASbed",assetChainId,assetId, BigInteger.valueOf(100000000L),password));
            Map paramMap = new HashMap();
            paramMap.put("listFrom", fromList);
            paramMap.put("listTo", toList);
            paramMap.put("chainId", chainId);
            paramMap.put("remark", "transfer test");
            //调用接口
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CC.abbr, "createCrossTx", paramMap);
            if (!cmdResp.isSuccess()) {
                Log.info("接口调用失败！" );
            }
            HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("createCrossTx"));
            String hash = (String) result.get("txHash");
            Log.debug("{}", hash);
            return hash;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws NulsException, IOException {
        String txHex = "0a00f8f9675f0000d20217090001048d27d1429853880559bf6ab7343161269684910100010080dd04b53d000000000000000000000000000000000000000000000000000000089c28b917af00bc250017090001048d27d1429853880559bf6ab7343161269684910900010080c3c9010000000000000000000000000000000000000000000000000000000008b5777b1f7fa6a979000117010001048d27d1429853880559bf6ab73431612696849101000100001a3bb33d000000000000000000000000000000000000000000000000000000000000000000000069210216fd40c36a036c0899c454ce14c34ec8ff790befe6c31e3346f46af2a591951d46304402207a2b5d7022154d0c742bdfb2ad4b4b43ebd04a7a6d02aa24b0c319cfd18344910220575ac62f88eaa9524a1a8ebc11ca6265899538a48554a86043e0e13e8001b87e";
        Transaction transaction = new Transaction();
        transaction.parse(HexUtil.decode(txHex),0);
        Chain chain = new Chain();
        ConfigBean configBean = new ConfigBean();
        configBean.setChainId(9);

        NulsLogger nulsLogger = LoggerUtil.commonLog;
        chain.setConfig(configBean);
        chain.setLogger(nulsLogger);

        NulsCrossChainConfig crossChainConfig = new NulsCrossChainConfig();
        crossChainConfig.setMainAssetId(1);
        crossChainConfig.setMainChainId(1);
        TxUtil.setConfig(crossChainConfig);
//        chain.setch
        NulsHash convertHash = TxUtil.friendConvertToMain(chain, transaction, TxType.CROSS_CHAIN).getHash();
        nulsLogger.info("{}",convertHash.toHex());
    }

}
