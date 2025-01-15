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
package network.nerve.converter.heterogeneouschain.fch;

import apipClass.BlockInfo;
import apipClass.Fcdsl;
import apipClass.TxInfo;
import apipClient.ApipClient;
import apipClient.ApipDataGetter;
import apipClient.BlockchainAPIs;
import apipClient.FreeGetAPIs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fcTools.ParseTools;
import fchClass.Cash;
import fchClass.OpReturn;
import fchClass.P2SH;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.io.IoUtils;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.v2.model.Account;
import io.nuls.v2.util.AccountTool;
import javaTools.JsonTools;
import keyTools.KeyTools;
import network.nerve.converter.btc.txdata.RechargeData;
import network.nerve.converter.heterogeneouschain.fch.utils.FchUtil;
import org.bitcoinj.base.VarInt;
import org.bitcoinj.core.Base58;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.junit.Before;
import org.junit.Test;
import txTools.FchTool;
import walletTools.MultiSigData;
import walletTools.SendTo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static txTools.FchTool.*;

/**
 * @author: PierreLuo
 * @date: 2023/12/27
 */
public class FchTransferTest {
    List<String> pris;
    List<byte[]> pubs;
    P2SH p2sh;
    byte[] priKeyBytesA;
    byte[] priKeyBytesB;
    byte[] priKeyBytesC;
    ECKey ecKeyA;
    ECKey ecKeyB;
    ECKey ecKeyC;
    String pubkeyA;
    String pubkeyB;
    String pubkeyC;
    String fidA;
    String fidB;
    String fidC;
    String urlHead = "https://cid.cash/APIP";

    Map<String, Object> pMap;
    String packageAddressPrivateKeyZP;
    String packageAddressPrivateKeyNE;
    String packageAddressPrivateKeyHF;
    String fromPriKey;
    String multisigAddress;
    //byte[] sessionKey = HexUtil.decode("17fd649617d838b514ba8338caf050c4753a51d1a471c11e1ee743329828dd8a");
    //byte[] sessionKey = HexUtil.decode("b3928a1dc649b38fb1f4b21b0afc3def668bad9f335c99db4fc0ec54cac1e655");
    byte[] sessionKey = HexUtil.decode("fb0ceb389f17d6b54891d2d1fcedf992c91e68140afe8202a412a87b6c835392");

    @Before
    public void before() {
        try {
            String evmFrom = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            String packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
            String packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
            String packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
            packageAddressPrivateKeyZP = pMap.get(packageAddressZP).toString();
            packageAddressPrivateKeyNE = pMap.get(packageAddressNE).toString();
            packageAddressPrivateKeyHF = pMap.get(packageAddressHF).toString();
            fromPriKey = pMap.get(evmFrom.toLowerCase()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        pris = new ArrayList<>();
        pris.add(packageAddressPrivateKeyZP);
        pris.add(packageAddressPrivateKeyNE);
        pris.add(packageAddressPrivateKeyHF);
        pubs = new ArrayList<>();
        for (String pri : pris) {
            byte[] priBytes = HexUtil.decode(pri);
            ECKey ecKey = ECKey.fromPrivate(priBytes, true);
            pubs.add(ecKey.getPubKey());
        }

        p2sh = FchUtil.genMultiP2sh(pubs, 2, true);
        multisigAddress = p2sh.getFid();

        priKeyBytesA = HexUtil.decode(pris.get(0));
        priKeyBytesB = HexUtil.decode(pris.get(1));
        priKeyBytesC = HexUtil.decode(pris.get(2));

        ecKeyA = ECKey.fromPrivate(priKeyBytesA);
        ecKeyB = ECKey.fromPrivate(priKeyBytesB);
        ecKeyC = ECKey.fromPrivate(priKeyBytesC);

        pubkeyA = ecKeyA.getPublicKeyAsHex();
        pubkeyB = ecKeyB.getPublicKeyAsHex();
        pubkeyC = ecKeyC.getPublicKeyAsHex();

        fidA = KeyTools.pubKeyToFchAddr(pubkeyA);
        fidB = KeyTools.pubKeyToFchAddr(pubkeyB);
        fidC = KeyTools.pubKeyToFchAddr(pubkeyC);
        System.out.println(String.format("fidA: %s", fidA));
        System.out.println(String.format("fidB: %s", fidB));
        System.out.println(String.format("fidC: %s", fidC));
        System.out.println(String.format("pubkeyA: %s", pubkeyA));
        System.out.println(String.format("pubkeyB: %s", pubkeyB));
        System.out.println(String.format("pubkeyC: %s", pubkeyC));
        System.out.println(String.format("multisigAddress: %s", multisigAddress));
    }

    @Test
    public void testCalcFeeMultiSignWithSplitGranularity() {
        // long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n
        long size = FchUtil.calcFeeMultiSignWithSplitGranularity(
                45995890, 12000000, 1, 0L, 1, 64, 2, 3);
        System.out.println(size);
        // int inputNum, int outputNum, int opReturnBytesLen, int m, int n
        size = FchUtil.calcFeeMultiSign(1, 1, 64, 2, 3);
        System.out.println(size);
    }
    @Test
    public void addrTest() throws Exception {
        Account account = AccountTool.createAccount(1);
        byte[] priKey = account.getPriKey();
        byte[] pubKey = account.getPubKey();
        System.out.println(String.format("pri: %s, addr: %s", HexUtil.encode(priKey), KeyTools.pubKeyToFchAddr(HexUtil.encode(pubKey))));
        System.out.println(String.format("addr: %s", pubKeyToFchAddr(HexUtil.encode(pubKey))));
        System.out.println(String.format("addr: %s", pubKeyToFchAddr("0327bf08c066cf6fe0d081d66376f4b8fafeb8fad6b85b97e1642117192228a746")));
    }

    @Test
    public void validAddr() {
        System.out.println(KeyTools.isValidFchAddr("2cg5hKp41bh7ePqfvpZePzHoRwkxSVfWZGw1WEfE"));
    }

    public String pubKeyToFchAddr(String pub) {
        byte[] h = SerializeUtils.sha256hash160(HexUtil.decode(pub));
        byte[] prefixForFch = new byte[]{35};
        byte[] hash160WithPrefix = new byte[21];
        System.arraycopy(prefixForFch, 0, hash160WithPrefix, 0, 1);
        System.arraycopy(h, 0, hash160WithPrefix, 1, 20);
        byte[] hashWithPrefix = Sha256Hash.hashTwice(hash160WithPrefix);
        byte[] checkHash = new byte[4];
        System.arraycopy(hashWithPrefix, 0, checkHash, 0, 4);
        byte[] addrRaw = new byte[hash160WithPrefix.length + checkHash.length];
        System.arraycopy(hash160WithPrefix, 0, addrRaw, 0, hash160WithPrefix.length);
        System.arraycopy(checkHash, 0, addrRaw, hash160WithPrefix.length, checkHash.length);
        return Base58.encode(addrRaw);
    }

    /**
     * Ordinary address transfer
     */
    @Test
    public void createTransferTransaction() {
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        ECKey ecKey = ECKey.fromPrivate(priBytes, true);
        String owner = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());
        String to = "338uHAHG2Gs3aufiFz89R4wsvPYH6yYTHv";
        Double amount = 0.001d;
        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(amount);
        outputs.add(sendTo);

        System.out.println(owner);
        List<Cash> inputs = new ArrayList<>();
        Cash cash = new Cash();
        cash.setOwner(owner);
        cash.setValue(8999999781L);
        cash.setBirthIndex(1);
        cash.setBirthTxId("5cda8e77bdbc65d90f18f5ce4d81d0966eaeef13a33ab79f664a27a48f9b06d6");
        inputs.add(cash);
        String sign = FchTool.createTransactionSign(inputs, priBytes, outputs, "");
        System.out.println(sign);
    }

    /**
     * apiPaid
     */
    @Test
    public void payForApiRequest() throws IOException {
        // FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        ECKey ecKey = ECKey.fromPrivate(priBytes, true);
        String owner = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());

        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, owner, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());

        String to = "FUmo2eez6VK2sfGWjek9i9aK5y1mdHSnqv";
        Double amount = 500d;
        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(amount);
        outputs.add(sendTo);

        String opReturn = null;
        String signedTx = FchTool.createTransactionSign(cashList, priBytes, outputs, opReturn);
        System.out.println(signedTx);
        //ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        //System.out.println(client.getResponseBodyStr());
    }

    @Test
    public void getUTXOs() throws JsonProcessingException {
        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        System.out.println(JSONUtils.obj2PrettyJson(cashList));
    }

    /**
     * Ordinary address transfer(With remarks)
     */
    @Test
    public void createTransferTransactionWithRemark() throws IOException {
        // Remarks op_return
        // 402cfbc4d10b8d3d0d0c55043b404714c0da78a5021147c4ba7fe8fd79eed81c
        // c359493a5b897414bda473c3b54c33b77804d2d5f3a4217eefc7e88c11dcbe29
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        ECKey ecKey = ECKey.fromPrivate(priBytes, true);
        String owner = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());

        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, owner, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        Double amount = Double.valueOf("0.5");
        Double fee = Double.valueOf("0.00000500");
        String to = "338uHAHG2Gs3aufiFz89R4wsvPYH6yYTHv";

        String feeTo = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        Double nerveFee = Double.valueOf("0.00002");
        RechargeData rechargeData = new RechargeData();
        rechargeData.setTo(AddressTool.getAddress("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad"));
        rechargeData.setValue(new BigDecimal(amount.toString()).movePointRight(8).toBigInteger().longValue());
        if (nerveFee.doubleValue() > 0) {
            rechargeData.setFeeTo(AddressTool.getAddress(feeTo));
        }
        String opReturn = HexUtil.encode(rechargeData.serialize());

        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(new BigDecimal(amount.toString()).add(new BigDecimal(fee.toString())).doubleValue());
        outputs.add(sendTo);


        String signedTx = FchTool.createTransactionSign(cashList, priBytes, outputs, opReturn);
        System.out.println(signedTx);
        ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        System.out.println(client.getResponseBodyStr());
    }

    @Test
    public void rechargeDataSizeTest() throws IOException {
        RechargeData rechargeData = new RechargeData();
        rechargeData.setTo(AddressTool.getAddress("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad"));
        rechargeData.setValue(10800);
        //rechargeData.setValue(new BigDecimal("122.00123").movePointRight(8).toBigInteger().longValue());
        //rechargeData.setFeeTo(AddressTool.getAddress("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA"));
        //rechargeData.setExtend0("2024-01-25 17:49");
        byte[] bytes = rechargeData.serialize();
        System.out.println(bytes.length);
        System.out.println(HexUtil.encode(bytes));
    }

    /**
     * Create multiple signed addresses
     */
    @Test
    public void createMultisigAddress() {
        // 0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260,
        // 03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5,
        // 02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750
        // testnet
        List<byte[]> pubList = new ArrayList<>();
        pubList.add(HexUtil.decode("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260"));
        pubList.add(HexUtil.decode("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5"));
        pubList.add(HexUtil.decode("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750"));
        P2SH p2sh = FchUtil.genMultiP2shForTest(pubList, 2);
        // 3BXpnXkAG7SYNxyKyDimcxjkyYQcaaJs5X
        System.out.println(String.format("makeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), p2sh.getFid()));
        p2sh = FchUtil.genMultiP2sh(pubList, 2, true);
        // 3BXpnXkAG7SYNxyKyDimcxjkyYQcaaJs5X
        System.out.println(String.format("Ordered makeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), p2sh.getFid()));

        // mainnet
        //0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        //03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9 - NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE
        //03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4 - NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD
        //02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292 - NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB
        //02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        //02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd - NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49
        //02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9
        //02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0 - NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        //028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb
        //02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049 - NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64
        //03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21 - NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63
        //02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9 - NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx
        //023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90 - NERVEepb6G71f2K3mPKrds8Be7KzdiCsM23Ewq
        //035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803 - NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1
        //039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980 - NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt
        List<byte[]> pubList1 = new ArrayList<>();
        pubList1.add(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b"));
        pubList1.add(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9"));
        pubList1.add(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4"));
        pubList1.add(HexUtil.decode("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292"));
        pubList1.add(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d"));
        pubList1.add(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd"));
        pubList1.add(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03"));
        pubList1.add(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0"));
        pubList1.add(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7"));
        pubList1.add(HexUtil.decode("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049"));
        pubList1.add(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21"));
        pubList1.add(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"));
        pubList1.add(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90"));
        pubList1.add(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803"));
        pubList1.add(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980"));
        p2sh = FchUtil.genMultiP2sh(pubList1, 10, true);
        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), p2sh.getFid()));
    }

    //public static P2SH genMultiP2sh(List<byte[]> pubKeyList, int m) {
    //    List<ECKey> keys = new ArrayList();
    //    Iterator var3 = pubKeyList.iterator();
    //
    //    byte[] redeemScriptBytes;
    //    while(var3.hasNext()) {
    //        redeemScriptBytes = (byte[])var3.next();
    //        ECKey ecKey = ECKey.fromPublicOnly(redeemScriptBytes);
    //        keys.add(ecKey);
    //    }
    //    Collections.sort(keys, ECKey.PUBKEY_COMPARATOR);
    //    Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(m, keys);
    //    redeemScriptBytes = multiSigScript.getProgram();
    //
    //    try {
    //        P2SH p2sh = P2SH.parseP2shRedeemScript(javaTools.HexUtil.encode(redeemScriptBytes));
    //        return p2sh;
    //    } catch (Exception var7) {
    //        var7.printStackTrace();
    //        return null;
    //    }
    //}

    /**
     * Get a specific addressUTXO
     */
    @Test
    public void getCashes() {
        String address = "338uHAHG2Gs3aufiFz89R4wsvPYH6yYTHv";
        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, address, 0);
        System.out.println(apipClient.getResponseBodyStr());
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        JsonTools.gsonPrint(cashList);
    }

    @Test
    public void getFidCid() {
        String address = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        ApipClient apipClient = FreeGetAPIs.getFidCid(urlHead, address);
        JsonTools.gsonPrint(apipClient.getResponseBody().getData());
    }

    @Test
    public void getPrice() {
        System.out.println(_getPrice());
    }

    BigDecimal _getPrice() {
        ApipClient apipClient = FreeGetAPIs.getPrices(urlHead);
        Map data = (Map) apipClient.getResponseBody().getData();
        if (data == null) {
            return BigDecimal.ZERO;
        }
        Object balance = data.get("fch/doge");
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.toString());
    }

    @Test
    public void getData() {
        //ApipClient apipClient = FreeGetAPIs.getApps(urlHead, null);
        //JsonTools.gsonPrint(apipClient.getResponseBody().getData());
        //apipClient = FreeGetAPIs.getFreeService(urlHead);
        //JsonTools.gsonPrint(apipClient.getResponseBody().getData());
        //apipClient = FreeGetAPIs.getServices(urlHead, null);
        //JsonTools.gsonPrint(apipClient.getResponseBody().getData());
        ApipClient apipClient = FreeGetAPIs.getTotals(urlHead);
        JsonTools.gsonPrint(apipClient.getResponseBody().getData());
    }

    @Test
    public void getBalanceTest() throws JsonProcessingException {
        //String addr = "FKDtca1w9TgNKq7F24iWCL9fZKZU7ACZjV";
        String addr = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        ApipClient fidCid = FreeGetAPIs.getFidCid(urlHead, addr);
        System.out.println(JSONUtils.obj2PrettyJson(fidCid));
    }

    /**
     * Multiple signing transactions
     */
    @Test
    public void spendMultisigUTXO() {
        String multisigAddress = p2sh.getFid();
        String to = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        Double amount = 0.00001;
        List<SendTo> sendToList = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(amount);
        sendToList.add(sendTo);
        SendTo sendTo1 = new SendTo();
        sendTo1.setFid(multisigAddress);
        sendTo1.setAmount(0.00098);
        sendToList.add(sendTo1);

        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, multisigAddress, 0);
        //System.out.println(apipClient.getResponseBodyStr());
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());

        //String msg = "deposit:NERVEepb69AJjWYALzDshU6V5TQcEGsT45isLx";
        String msg = "";
        //Make raw tx
        byte[] rawTx = createMultiSignRawTx(cashList, sendToList, msg, p2sh);

        //Sign raw tx
        byte[] redeemScript = HexUtil.decode(p2sh.getRedeemScript());
        MultiSigData multiSignData = new MultiSigData(rawTx, p2sh, cashList);
        MultiSigData multiSignDataA = signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(0)));
        MultiSigData multiSignDataB = signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(1)));
        MultiSigData multiSignDataC = signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(2)));
        Map<String, List<byte[]>> sig1 = multiSignDataA.getFidSigMap();
        Map<String, List<byte[]>> sig2 = multiSignDataB.getFidSigMap();
        Map<String, List<byte[]>> sig3 = multiSignDataC.getFidSigMap();
        Map<String, List<byte[]>> sigAll = new HashMap<>();
        sigAll.putAll(sig1);
        sigAll.putAll(sig2);
        sigAll.putAll(sig3);

        System.out.println("Verify sig1:" + FchTool.rawTxSigVerify(rawTx, ecKeyA.getPubKey(), sig1.get(fidA).get(0), 0, cashList.get(0).getValue(), redeemScript));
        System.out.println("Verify sig2:" + FchTool.rawTxSigVerify(rawTx, ecKeyB.getPubKey(), sig2.get(fidB).get(0), 0, cashList.get(0).getValue(), redeemScript));
        System.out.println("Verify sig3:" + FchTool.rawTxSigVerify(rawTx, ecKeyC.getPubKey(), sig3.get(fidC).get(0), 0, cashList.get(0).getValue(), redeemScript));

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<byte[]> sigList = sigAll.get(fid);
            for (byte[] sig : sigList) {
                System.out.println("    " + HexUtil.encode(sig));
            }
        }

        //build signed tx
        String signedTx = buildSchnorrMultiSignTx(rawTx, sigAll, p2sh);
        System.out.println("signedTx: " + signedTx);

        ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        System.out.println(client.getResponseBodyStr());

        //String msC = multiSignDataC.toJson();
        //System.out.println("multiSignDataC: " + msC);
        //MultiSigData multiSignDataD = MultiSigData.fromJson(msC);
        //System.out.println("New: " + multiSignDataD.toJson());
    }

    /**
     * Get a specific addressUTXO
     */
    @Test
    public void getBestBlockTest() {
        ApipClient apipClient = FreeGetAPIs.getBestBlock(urlHead);
        System.out.println(apipClient.getResponseBodyStr());
        Object data = apipClient.getResponseBody().getData();
        Type t = (new TypeToken<BlockInfo>() {
        }).getType();
        Gson gson = new Gson();
        BlockInfo blockInfo = (BlockInfo) gson.fromJson(gson.toJson(data), t);
        System.out.println(JsonTools.getNiceString(blockInfo));
    }

    @Test
    public void blockDetailDataByBlockHashTest() {
        // 1059.99998000
        // 1059.99986000
        //    0.00001
        //sessionKey = HexUtil.decode("b3928a1dc649b38fb1f4b21b0afc3def668bad9f335c99db4fc0ec54cac1e655");
        ApipClient apipClient = BlockchainAPIs.blockByIdsPost(urlHead, new String[]{"000000000000024ae054ab1eaeb529d5f1fed6f86f084e4dc8d696c3f84335d3"}, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("blockDetailData:\n" + apipClient.getResponseBodyStr());
    }

    @Test
    public void blockDetailDataByBlockHeightTest() {
        //byte[] sessionKey = HexUtil.decode("11f2fbcc6f04e9bf40d55603a21ffff4ec156ed948809a492dc43c33d67ebc32");
        ApipClient apipClient = BlockchainAPIs.blockByHeightsPost(urlHead, new String[]{"2080466"}, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("blockDetailData:\n" + apipClient.getResponseBodyStr());
        Map<String, BlockInfo> blockMap = ApipDataGetter.getBlockInfoMap(apipClient.getResponseBody().getData());
        System.out.println(blockMap.size());
        // 49596000000
        // 49595000000
        // 49594000000
        // 49593000000
    }

    @Test
    public void blockDataSearchTest() {
        Fcdsl fcdsl = new Fcdsl();
        //fcdsl.addNewQuery().addNewEquals().addNewFields("height").addNewValues("2045554");

        //byte[] sessionKey = HexUtil.decode("11f2fbcc6f04e9bf40d55603a21ffff4ec156ed948809a492dc43c33d67ebc32");
        ApipClient apipClient = BlockchainAPIs.blockSearchPost(urlHead, fcdsl, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("apipClient:\n" + apipClient.getResponseBodyStr());
    }

    @Test
    public void getTxInfoTest() {
        //BlockchainAPIs.cashByIdsPost()
        String txHash = "314363ec7b9fdfab95209fc43633cd33f0048bde4095c412a465d180f8ca0f67";
        ApipClient client = BlockchainAPIs.txByIdsPost(urlHead, new String[]{
                txHash
        }, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("tx info:\n" + client.getResponseBodyStr());
        List<TxInfo> txInfoList = ApipDataGetter.getTxInfoList(client.getResponseBody().getData());
        System.out.println(txInfoList.size());
    }

    @Test
    public void UTXOInfoTest() {
        // a29115a84dfea4c6845998a5913b924878569fb9c41f755f6626a73cbbd47df5 = sha256x2(birthTxId:08c71a038b51f0832f0edadc21e22652fedc30128ac5e3232fc659c31b91a58b, birthIndex:2)
        String cashIdSpent = "b7893dc7e90a64e6372433f50fdba3f878eeab9fccac14fcae32e1068577b4dd";
        String cashIdOpReturn = "d94a05e8e7133e2311a9ca326c00895f6067fd04140dc4eaa056e81e752d4009";
        String cashIdUnspent = "a29115a84dfea4c6845998a5913b924878569fb9c41f755f6626a73cbbd47df5";
        String[] cashes = new String[]{cashIdSpent, cashIdOpReturn, cashIdUnspent};
        //String[] cashes = new String[]{"0df8c11e343b1000f7795527bf8318f644f8d29b11630281840edf7006b7412a"};
        ApipClient client = BlockchainAPIs.cashByIdsPost(urlHead, cashes, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("utxo info:\n" + client.getResponseBodyStr());
    }

    @Test
    public void CashInfoByTxidVoutTest() {
        String txid = "50e5f5bff70714b2c3f120f3e226111e8a3db3577f27d1c93cbe5979b4ca2814";
        int vout = 0;
        String cashId = ParseTools.calcTxoId(txid, vout);
        String[] cashes = new String[]{cashId};
        ApipClient client = BlockchainAPIs.cashByIdsPost(urlHead, cashes, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("utxo info:\n" + client.getResponseBodyStr());
    }

    @Test
    public void opReturnInfoTest() {
        String cashIdOpReturn = "3829067ab1b932aa903d1ab95f88d4e2564e159a8507574f4694dd3e30afc88e";
        ApipClient client = BlockchainAPIs.opReturnByIdsPost(urlHead, new String[]{cashIdOpReturn}, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("opReturn info:\n" + client.getResponseBodyStr());
        Map<String, OpReturn> opReturnMap = ApipDataGetter.getOpReturnMap(client.getResponseBody().getData());
        OpReturn opReturn = opReturnMap.get(cashIdOpReturn);
        System.out.println(HexUtil.encode(opReturn.getOpReturn().getBytes(StandardCharsets.UTF_8)));
        System.out.println(opReturnMap.size());
        //0500017ab79bd5c00354e7d4f346749ad7d2c1f8bae031fd102700000000000000
    }

    //fee = txSize * (feeRate/1000)*100000000
    public static long calcTxSize(int inputNum, int outputNum, int opReturnBytesLen) {

        long baseLength = 10;
        long inputLength = 141 * (long) inputNum;
        long outputLength = 34 * (long) (outputNum + 1); // Include change output

        int opReturnLen=0;
        if (opReturnBytesLen!=0)
            opReturnLen = calcOpReturnLen(opReturnBytesLen);

        return baseLength + inputLength + outputLength + opReturnLen;
    }

    private static int calcOpReturnLen(int opReturnBytesLen) {
        int dataLen;
        if(opReturnBytesLen <76){
            dataLen = opReturnBytesLen +1;
        }else if(opReturnBytesLen <256){
            dataLen = opReturnBytesLen +2;
        }else dataLen = opReturnBytesLen +3;
        int scriptLen;
        scriptLen = (dataLen + 1) + VarInt.sizeOf(dataLen+1);
        int amountLen = 8;
        return scriptLen + amountLen;
    }
}
