package io.nuls.crosschain.test;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsSignData;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.crosschain.base.model.ResetChainInfoTransaction;
import io.nuls.crosschain.base.model.bo.txdata.ResetChainInfoData;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxSendTester {

    @Test
    public void test() throws Exception {
        NoUse.mockModule();
        String prikey = "";
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(prikey));
        byte[] address = AddressTool.getAddress(ecKey.getPubKey(), 5);


        ResetChainInfoTransaction tx = new ResetChainInfoTransaction();
        tx.setTime(System.currentTimeMillis() / 1000);
        ResetChainInfoData txData = new ResetChainInfoData();
        txData.setJson("{\n" +
                "            \"chainId\":2,\n" +
                "            \"chainName\":\"nuls2\",\n" +
                "            \"minAvailableNodeNum\":0,\n" +
                "            \"maxSignatureCount\":100,\n" +
                "            \"signatureByzantineRatio\":66,\n" +
                "            \"addressPrefix\":\"tNULS\",\n" +
                "            \"assetInfoList\":[\n" +
                "                {\n" +
                "                    \"assetId\":1,\n" +
                "                    \"symbol\":\"NULS\",\n" +
                "                    \"assetName\":\"\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":8\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":8,\n" +
                "                    \"symbol\":\"T1\",\n" +
                "                    \"assetName\":\"t1\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":9\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":29,\n" +
                "                    \"symbol\":\"BIG\",\n" +
                "                    \"assetName\":\"BIG\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":2\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":31,\n" +
                "                    \"symbol\":\"TTk\",\n" +
                "                    \"assetName\":\"token\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":7\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":33,\n" +
                "                    \"symbol\":\"NNERVENABOXTOMOON\",\n" +
                "                    \"assetName\":\"nnn\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":10\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":40,\n" +
                "                    \"symbol\":\"TAKER\",\n" +
                "                    \"assetName\":\"TakerSwap\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":18\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":58,\n" +
                "                    \"symbol\":\"NABOX\",\n" +
                "                    \"assetName\":\"NABOX\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":18\n" +
                "                },\n" +
                "                {\n" +
                "                    \"assetId\":64,\n" +
                "                    \"symbol\":\"pg\",\n" +
                "                    \"assetName\":\"pigs\",\n" +
                "                    \"usable\":true,\n" +
                "                    \"decimalPlaces\":3\n" +
                "                }\n" +
                "            ],\n" +
                "            \"verifierList\":[\n" +
                "                \"tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp\",\n" +
                "                \"tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe\",\n" +
                "                \"tNULSeBaMmShSTVwbU4rHkZjpD98JgFgg6rmhF\"\n" +
                "            ],\n" +
                "            \"registerTime\":0\n" +
                "        }");
        tx.setTxData(txData.serialize());

        CoinData coinData = new CoinData();
        CoinFrom from = new CoinFrom();
        from.setAddress(address);
        from.setAmount(BigInteger.ZERO);
        from.setAssetsChainId(5);
        from.setAssetsId(1);
        from.setLocked((byte) 0);
        from.setNonce(HexUtil.decode("38beaaea79a72b26"));
        coinData.getFrom().add(from);
        CoinTo to = new CoinTo();
        to.setAddress(address);
        to.setAmount(BigInteger.ZERO);
        to.setAssetsId(1);
        to.setAssetsChainId(5);
        to.setLockTime(0);
        coinData.getTo().add(to);

        tx.setCoinData(coinData.serialize());

        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> list = new ArrayList<>();
        P2PHKSignature sig = new P2PHKSignature();
        sig.setPublicKey(ecKey.getPubKey());
        NulsSignData data = new NulsSignData();
        data.setSignBytes(ecKey.sign(tx.getHash().getBytes()));
        sig.setSignData(data);
        list.add(sig);
        transactionSignature.setP2PHKSignatures(list);
        tx.setTransactionSignature(transactionSignature.serialize());
        Log.info(tx.getHash().toHex());
        Log.info(HexUtil.encode(tx.serialize()));
        sendTx(5, HexUtil.encode(tx.serialize()));
    }

    @SuppressWarnings("unchecked")
    public static void sendTx(int chainId, String tx) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", tx);
        try {
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            if (!cmdResp.isSuccess()) {
                //rollBackUnconfirmTx(chain,tx);
                throw new RuntimeException();
            }
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
