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
import io.nuls.transaction.model.bo.config.ConfigBean;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.txdata.TradingOrder;
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
        AddressTool.addPrefix(5, "TNVT");
        String hex = "1c00bc7c855f0057050003bc83930d55e1bb7978a29ae1ce81d1cf234b494c6019da215d000000000000000000000000000000000000000000000000000000e07d6195b1f757a06fcb040e29f75e5a03149fc677d88f941e4eb724da82bae8fd16010217050003bc83930d55e1bb7978a29ae1ce81d1cf234b494c050001000092d99c2e0000000000000000000000000000000000000000000000000000000858ebcc79a51a54faff17050003bc83930d55e1bb7978a29ae1ce81d1cf234b494c05000100007a50e5450000000000000000000000000000000000000000000000000000000813c62ac4536ae783ff0217050003bc83930d55e1bb7978a29ae1ce81d1cf234b494c050001006019da215d0000000000000000000000000000000000000000000000000000003c43995f0000000017050003bc83930d55e1bb7978a29ae1ce81d1cf234b494c05000100006c4e6017000000000000000000000000000000000000000000000000000000fffffffffffffffffd3b010203210224d86a584324fc8e92c6dba19c08926a7af77df884deec0d1c3b879a8f50720f2102362c64e15ab653132ec753e4a8c181ef720ec927466a09417a07877824781f572102962c7942851fa2c937be788a18693885276e3d9688b5997d9f02ebf2fef218db2102362c64e15ab653132ec753e4a8c181ef720ec927466a09417a07877824781f574630440220380648689da946e5d0f536a31ed8b80380b7e66c94f5181be582ada7d2fa6e7b022000dd54f6b0f9b92e1cdc50829aa606fb9efe3708065c01f1ff7c597bd644d62e210224d86a584324fc8e92c6dba19c08926a7af77df884deec0d1c3b879a8f50720f473045022100c372a66fbc341f6ff55b8bdaec5b693575bac3ff074835d39dee8fdbb31ca7c60220552cf71f95dd862de380a3cb7345c60a929301468881e2ba56656d1bdeba49be";
        Transaction tx = TxUtil.getInstance(hex, Transaction.class);
        String format = tx.format(TradingOrder.class);
        System.out.println(format);

    }
}
