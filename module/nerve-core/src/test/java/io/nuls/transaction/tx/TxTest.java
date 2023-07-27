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

package io.nuls.transaction.tx;

import io.nuls.common.ConfigBean;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.utils.TxUtil;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试交易
 * @author: Charlie
 * @date: 2020/6/12
 */
public class TxTest {

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
    static String address30 = "TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9";
    static String address31 = "TNVTdTSPRyiWcpbS65NmT5qyGmuqPxuKv8SF4";

    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;
    static int heterogeneousChainId = 101;
    static int heterogeneousAssetId = 1;

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
    public void createWrongTx() throws Exception {
        Map map = CreateTx.createTransferTx(address30, address25, new BigInteger("1000000000"));
        Transaction tx = CreateTx.assemblyTransaction((List<CoinDTO>) map.get("inputs"), (List<CoinDTO>) map.get("outputs"), (String) map.get("remark"), null, 1591947442L, TxType.QUOTATION);
        sign(tx, address30, password);
        Log.info("{}", tx.format());
        newTx(tx);
    }
    @Test
    public void createTx() throws Exception {
        for (int i = 0; i < 1; i++) {
            Map map = CreateTx.createTransferTx(address30, address25, new BigInteger("2200000000"));
            Transaction tx = CreateTx.assemblyTransaction((List<CoinDTO>) map.get("inputs"), (List<CoinDTO>) map.get("outputs"), (String) map.get("remark"), null, 1593070691L, TxType.TRANSFER);
            Log.info("{}", tx.format());
            newTx(tx);
        }
    }

    private void newTx(Transaction tx) throws Exception {
        Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", RPCUtil.encode(tx.serialize()));
        HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
//        System.out.println(result.get("hash"));
    }

    /**
     * 对交易hash签名(在线)
     * @param tx
     * @param address
     * @param password
     */
    public void sign(Transaction tx, String address, String password) throws Exception {
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        Map<String, Object> params = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        params.put("data", RPCUtil.encode(tx.getHash().getBytes()));
        HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
        String signatureStr = (String) result.get("signature");
        P2PHKSignature signature = new P2PHKSignature();
        signature.parse(new NulsByteBuffer(RPCUtil.decode(signatureStr)));
        p2PHKSignatures.add(signature);
        //交易签名
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
    }

    public static void main(String[] args) throws Exception {
//        AddressTool.addPrefix(5, "TNVT");
        AddressTool.addPrefix(2, "tNULS");
//        String hex = "020029f8b5600000fd160102170500018215c5c461586c87fd268a357087d20112442ad2050002000080c6a47e8d030000000000000000000000000000000000000000000000000008000000000000000000170500014212bc546cc9564c6437bd6b72891b1f4954c78005000100e20afbcb0000000000000000000000000000000000000000000000000000000008619d09a97fa0c4880002170500014212bc546cc9564c6437bd6b72891b1f4954c780050002000080c6a47e8d03000000000000000000000000000000000000000000000000000000000000000000170500018215c5c461586c87fd268a357087d20112442ad2050001004284f9cb000000000000000000000000000000000000000000000000000000000000000000000000692103a7f55e70af5019a6beccfe2c0b5b5d3184507c6e793381e861b01b6e1e8be155463044022036c861f31b24d044ec663923076fc7fe225d020c3f78e04a4b3a08b6cfb5eb2f022046ff09eb30ab63e7a7f513bbb6300236ed607e3936aea7b8faa4dc835f8fb2b7";
        String hex = "0a00a560d9600000d20217020001bc2a13e5cd8f92d1ad063a0e3b9a84a85a28ed12050001004023050600000000000000000000000000000000000000000000000000000000085baac6ee777b11f70017020001bc2a13e5cd8f92d1ad063a0e3b9a84a85a28ed12010001008096980000000000000000000000000000000000000000000000000000000000085baac6ee777b11f7000117050001de04572783335a216deb4035d212242c9efcc13a0500010000e1f50500000000000000000000000000000000000000000000000000000000000000000000000000";
        Transaction tx = TxUtil.getInstance(hex, Transaction.class);
//        TransactionSignature ts = TxUtil.getInstance(tx.getTransactionSignature(), TransactionSignature.class);
//
//        for(P2PHKSignature p : ts.getP2PHKSignatures()){
//            byte[] address = AddressTool.getAddress(p.getPublicKey(), 5);
//            String addr = AddressTool.getStringAddressByBytes(address);
//            System.out.println("签名地址");
//            System.out.println(addr);
//
//        }
        String format = tx.format();
        System.out.println(format);

    }
}
