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
import fchClass.Cash;
import fchClass.CashMark;
import fchClass.OpReturn;
import fchClass.P2SH;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.VarInt;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import javaTools.JsonTools;
import keyTools.KeyTools;
import network.nerve.swap.utils.fch.BtcSignData;
import network.nerve.swap.utils.fch.FchUtil;
import network.nerve.swap.utils.fch.WithdrawalUTXOTxData;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author: PierreLuo
 * @date: 2023/12/27
 */
public class FchTransferTest {
    List<String> pris;
    List<byte[]> pubs;
    List<ECKey> pubECKeys;
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
    String fid996;
    String urlHead = "https://cid.cash/APIP";

    Map<String, Object> pMap;
    String packageAddressPrivateKeyZP;
    String packageAddressPrivateKeyNE;
    String packageAddressPrivateKeyHF;
    String fromPriKey;
    String multisigAddress;
    String via = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
    //byte[] sessionKey = HexUtil.decode("17fd649617d838b514ba8338caf050c4753a51d1a471c11e1ee743329828dd8a");
    byte[] sessionKey = HexUtil.decode("b3928a1dc649b38fb1f4b21b0afc3def668bad9f335c99db4fc0ec54cac1e655");

    @Before
    public void before() {
        try {
            String evmFrom = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            String packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
            String packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
            String packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
            packageAddressPrivateKeyZP = pMap.get(packageAddressZP).toString();
            packageAddressPrivateKeyNE = pMap.get(packageAddressNE).toString();
            packageAddressPrivateKeyHF = pMap.get(packageAddressHF).toString();
            fromPriKey = pMap.get(evmFrom.toLowerCase()).toString();
            pris = new ArrayList<>();
            pris.add(packageAddressPrivateKeyZP);
            pris.add(packageAddressPrivateKeyNE);
            pris.add(packageAddressPrivateKeyHF);
            pubs = new ArrayList<>();
            pubECKeys = new ArrayList<>();
            for (String pri : pris) {
                byte[] priBytes = HexUtil.decode(pri);
                ECKey ecKey = ECKey.fromPrivate(priBytes, true);
                pubECKeys.add(ecKey);
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
            fid996 = KeyTools.pubKeyToFchAddr(ECKey.fromPrivate(HexUtil.decode(fromPriKey)).getPubKey());
            System.out.println(String.format("fid996: %s", fid996));
            System.out.println(String.format("fidA: %s", fidA));
            System.out.println(String.format("fidB: %s", fidB));
            System.out.println(String.format("fidC: %s", fidC));
            System.out.println(String.format("pubkeyA: %s", pubkeyA));
            System.out.println(String.format("pubkeyB: %s", pubkeyB));
            System.out.println(String.format("pubkeyC: %s", pubkeyC));
            System.out.println(String.format("multisigAddress: %s", multisigAddress));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("empty dev pris");
        }

    }

    @Test
    public void addrTest() throws Exception {
        byte[] priKey = null;
        ECKey ecKey = ECKey.fromPrivate(priKey);
        byte[] pubKey = ecKey.getPubKey();
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
    public void createTransferTransaction() throws Exception {
        long feeRate = 1;
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        ECKey ecKey = ECKey.fromPrivate(priBytes, true);
        String owner = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());
        String to = fidA;
        //long amount = new BigDecimal("0.001").movePointRight(8).longValue();
        long amount = 546;

        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(new BigDecimal(amount).movePointLeft(8).doubleValue());
        outputs.add(sendTo);

        System.out.println(owner);
        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, owner, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());

        Object[] datas = FchUtil.calcFeeAndUTXO(cashList, amount, feeRate, 0);
        long fee = (long) datas[0];
        List<Cash> spendingCashes = (List<Cash>) datas[1];
        System.out.println(JSONUtils.obj2PrettyJson(spendingCashes));

        String signedTx = FchTool.createTransactionSign(spendingCashes, priBytes, outputs, null);
        System.out.println(signedTx);
        ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        Object data = client.getResponseBody().getData();
        System.out.println(data);
        System.out.println(client.getResponseBodyStr());
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
        // 3BXpnXkAG7SYNxyKyDimcxjkyYQcaaJs5X
        P2SH p2sh = FchUtil.genMultiP2shForTest(pubList, 2);
        System.out.println(String.format("makeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), p2sh.getFid()));
        p2sh = FchUtil.genMultiP2sh(pubList, 2, true);
        System.out.println(String.format("Order makeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), p2sh.getFid()));

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
        p2sh = FchUtil.genMultiP2shForTest(pubList1, 10);
        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), p2sh.getFid()));
        p2sh = FchUtil.genMultiP2sh(pubList1, 10, true);
        System.out.println(String.format("Order makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), p2sh.getFid()));

        pubList1 = new ArrayList<>();
        pubList1.add(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803"));
        pubList1.add(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21"));
        pubList1.add(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4"));
        pubList1.add(HexUtil.decode("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a"));
        pubList1.add(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9"));
        pubList1.add(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b"));
        pubList1.add(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d"));
        pubList1.add(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90"));
        pubList1.add(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"));
        pubList1.add(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980"));
        pubList1.add(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0"));
        pubList1.add(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd"));
        pubList1.add(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4"));
        pubList1.add(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7"));
        pubList1.add(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03"));
        p2sh = FchUtil.genMultiP2shForTest(pubList1, 10);
        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), p2sh.getFid()));
        p2sh = FchUtil.genMultiP2sh(pubList1, 10, true);
        System.out.println(String.format("Order makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), p2sh.getFid()));

    }

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
        byte[] rawTx = FchTool.createMultiSignRawTx(cashList, sendToList, msg, p2sh);

        //Sign raw tx
        byte[] redeemScript = HexUtil.decode(p2sh.getRedeemScript());
        MultiSigData multiSignData = new MultiSigData(rawTx, p2sh, cashList);
        MultiSigData multiSignDataA = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(0)));
        MultiSigData multiSignDataB = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(1)));
        MultiSigData multiSignDataC = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(2)));
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
        String signedTx = FchTool.buildSchnorrMultiSignTx(rawTx, sigAll, p2sh);
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
        String txHash = "228ca97ea7c1871e09f345d8d6f42833e74d091064ff1a9ecd52fbc1f6e4f209";
        ApipClient client = BlockchainAPIs.txByIdsPost(urlHead, new String[]{
                txHash
        }, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("tx info:\n" + client.getResponseBodyStr());
        List<TxInfo> txInfoList = ApipDataGetter.getTxInfoList(client.getResponseBody().getData());
        System.out.println(txInfoList.size());
    }

    @Test
    public void getTxInfoCashIdInfoTest() throws Exception {
        String txHash = "1d7840e548eafd6c11f689fdd9bcab4b899add4f7eb1464ea3e622089d6593ba";
        ApipClient client = BlockchainAPIs.txByIdsPost(urlHead, new String[]{
                txHash
        }, via, sessionKey);
        System.out.println("tx info:\n" + client.getResponseBodyStr());
        List<TxInfo> txInfoList = ApipDataGetter.getTxInfoList(client.getResponseBody().getData());
        TxInfo txInfo = txInfoList.get(0);

        ArrayList<CashMark> inputList = txInfo.getSpentCashes();
        List<String> usedCashIds = new ArrayList<>();
        for (CashMark input : inputList) {
            String inputAddress = input.getOwner();
            System.out.println(inputAddress);
            usedCashIds.add(input.getCashId());
        }
        String[] usedCashIdArray = new String[usedCashIds.size()];
        usedCashIds.toArray(usedCashIdArray);
        Map<String, Cash> usedUTXOs = this.getUTXOsByIds(usedCashIdArray);
        System.out.println(JSONUtils.obj2PrettyJson(usedUTXOs));
    }

    private Map<String, Cash> getUTXOsByIds(String[] cashIds) {
        ApipClient client = BlockchainAPIs.cashByIdsPost(urlHead, cashIds, via, sessionKey);
        System.out.println(client.getResponseBodyStr());
        Object data = client.getResponseBody().getData();
        if (data == null) {
            return null;
        }
        return ApipDataGetter.getCashMap(data);
    }

    @Test
    public void UTXOInfoTest() {
        String cashIdSpent = "b7893dc7e90a64e6372433f50fdba3f878eeab9fccac14fcae32e1068577b4dd";
        String cashIdOpReturn = "d94a05e8e7133e2311a9ca326c00895f6067fd04140dc4eaa056e81e752d4009";
        String cashIdUnspent = "a29115a84dfea4c6845998a5913b924878569fb9c41f755f6626a73cbbd47df5";
        ApipClient client = BlockchainAPIs.cashByIdsPost(urlHead, new String[]{cashIdSpent, cashIdOpReturn, cashIdUnspent}, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
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

        int opReturnLen = 0;
        if (opReturnBytesLen != 0)
            opReturnLen = calcOpReturnLen(opReturnBytesLen);

        return baseLength + inputLength + outputLength + opReturnLen;
    }
    private static int calcOpReturnLen(int opReturnBytesLen) {
        int dataLen;
        if (opReturnBytesLen < 76) {
            dataLen = opReturnBytesLen + 1;
        } else if (opReturnBytesLen < 256) {
            dataLen = opReturnBytesLen + 2;
        } else dataLen = opReturnBytesLen + 3;
        int scriptLen;
        scriptLen = (dataLen + 1) + VarInt.sizeOf(dataLen + 1);
        int amountLen = 8;
        return scriptLen + amountLen;
    }

    @Test
    public void mainnetAddrTest() throws Exception {
        List<String> oldPubs = new ArrayList<>();
        oldPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        oldPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        oldPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        oldPubs.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292");
        oldPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        oldPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        oldPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        oldPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        oldPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        oldPubs.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
        oldPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        oldPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        oldPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        oldPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        oldPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");

        List<String> newPubs = new ArrayList<>();
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");

        String utxoJson = "{\"code\":0,\"message\":\"Success.\",\"got\":2,\"total\":2,\"bestHeight\":2245480,\"data\":[{\"cashId\":\"ec83f4e6682ba98b6e32df476f839d5252ee942dc2238c088541003552802a68\",\"issuer\":\"F8WA7ZWhWM1YWvHe9tLDy7tP9pn3SbsM2u\",\"birthIndex\":0,\"type\":\"P2SH\",\"owner\":\"39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV\",\"value\":200000000,\"lockScript\":\"a9145ac09d3971c2c6afac6cb487dda7e3185b7a3f4c87\",\"birthTxId\":\"18a2c4b9d555555373bc8d5a90b198f08e79979aef24f665c38be6ee92ce0950\",\"birthTxIndex\":1,\"birthBlockId\":\"00000000000000d5f472d7dafb2daec9e9394f9fd9abc8d6d5db9cb5417ceac1\",\"birthTime\":1713499471,\"birthHeight\":2214282,\"spendTime\":0,\"spendHeight\":0,\"spendTxIndex\":0,\"spendIndex\":0,\"cdd\":0,\"cd\":44,\"valid\":true},{\"cashId\":\"093d9e9764df53f0751afbe974f57d286e3bc507e426cb73b0697ba38c89ec56\",\"issuer\":\"FJYN3D7x4yiLF692WUAe7Vfo2nQpYDNrC7\",\"birthIndex\":0,\"type\":\"P2SH\",\"owner\":\"39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV\",\"value\":10000000000000,\"lockScript\":\"a9145ac09d3971c2c6afac6cb487dda7e3185b7a3f4c87\",\"birthTxId\":\"4ab3362b0ef662734be108295cb8fc0a903f19d4ff66f09bfbd87923c80dfbc7\",\"birthTxIndex\":1,\"birthBlockId\":\"000000000000010edfcda0700a55f360d5a6d0d7d2b9b89d00e99248a3d31f47\",\"birthTime\":1713541760,\"birthHeight\":2214973,\"spendTime\":0,\"spendHeight\":0,\"spendTxIndex\":0,\"spendIndex\":0,\"cdd\":0,\"cd\":2100000,\"valid\":true}]}";
        Map utxoMap = JSONUtils.json2map(utxoJson);
        List<Map> dataList = (List<Map>) utxoMap.get("data");
        List<Cash> cashList = ApipDataGetter.getCashList(dataList);

        List<byte[]> oldPubEcKeys = oldPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        List<byte[]> newPubEcKeys = newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        P2SH oldP2SH = FchUtil.genMultiP2sh(oldPubEcKeys, 10, true);
        P2SH newP2SH = FchUtil.genMultiP2sh(newPubEcKeys, 10, true);
        String fromAddress = oldP2SH.getFid();
        String toAddress = newP2SH.getFid();
        System.out.println(String.format("old addr: %s", fromAddress));
        System.out.println(String.format("new addr: %s", toAddress));
    }

    Object[] baseData() throws IOException {
        List<String> oldPubs = new ArrayList<>();
        oldPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        oldPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        oldPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        oldPubs.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292");
        oldPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        oldPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        oldPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        oldPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        oldPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        oldPubs.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
        oldPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        oldPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        oldPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        oldPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        oldPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");

        List<String> newPubs = new ArrayList<>();
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");

        String utxoJson = "{\"code\":0,\"message\":\"Success.\",\"got\":2,\"total\":2,\"bestHeight\":2245480,\"data\":[{\"cashId\":\"ec83f4e6682ba98b6e32df476f839d5252ee942dc2238c088541003552802a68\",\"issuer\":\"F8WA7ZWhWM1YWvHe9tLDy7tP9pn3SbsM2u\",\"birthIndex\":0,\"type\":\"P2SH\",\"owner\":\"39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV\",\"value\":200000000,\"lockScript\":\"a9145ac09d3971c2c6afac6cb487dda7e3185b7a3f4c87\",\"birthTxId\":\"18a2c4b9d555555373bc8d5a90b198f08e79979aef24f665c38be6ee92ce0950\",\"birthTxIndex\":1,\"birthBlockId\":\"00000000000000d5f472d7dafb2daec9e9394f9fd9abc8d6d5db9cb5417ceac1\",\"birthTime\":1713499471,\"birthHeight\":2214282,\"spendTime\":0,\"spendHeight\":0,\"spendTxIndex\":0,\"spendIndex\":0,\"cdd\":0,\"cd\":44,\"valid\":true},{\"cashId\":\"093d9e9764df53f0751afbe974f57d286e3bc507e426cb73b0697ba38c89ec56\",\"issuer\":\"FJYN3D7x4yiLF692WUAe7Vfo2nQpYDNrC7\",\"birthIndex\":0,\"type\":\"P2SH\",\"owner\":\"39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV\",\"value\":10000000000000,\"lockScript\":\"a9145ac09d3971c2c6afac6cb487dda7e3185b7a3f4c87\",\"birthTxId\":\"4ab3362b0ef662734be108295cb8fc0a903f19d4ff66f09bfbd87923c80dfbc7\",\"birthTxIndex\":1,\"birthBlockId\":\"000000000000010edfcda0700a55f360d5a6d0d7d2b9b89d00e99248a3d31f47\",\"birthTime\":1713541760,\"birthHeight\":2214973,\"spendTime\":0,\"spendHeight\":0,\"spendTxIndex\":0,\"spendIndex\":0,\"cdd\":0,\"cd\":2100000,\"valid\":true}]}";
        Map utxoMap = JSONUtils.json2map(utxoJson);
        List<Map> dataList = (List<Map>) utxoMap.get("data");
        List<Cash> cashList = ApipDataGetter.getCashList(dataList);

        List<byte[]> oldPubEcKeys = oldPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        List<byte[]> newPubEcKeys = newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        P2SH oldP2SH = FchUtil.genMultiP2sh(oldPubEcKeys, 10, true);
        P2SH newP2SH = FchUtil.genMultiP2sh(newPubEcKeys, 10, true);
        String toAddress = newP2SH.getFid();
        int n = oldPubEcKeys.size(), m = 10;
        long feeRate = 1;
        long feeSize = FchUtil.calcFeeMultiSign(cashList.size(), 0, 0, m, n);
        long fee = feeSize * feeRate;
        long totalMoney = 0;
        for (Cash utxo : cashList) {
            totalMoney += utxo.getValue();
        }
        long amount = totalMoney - fee;
        String msg = "";
        //Make raw tx
        byte[] rawTx = (byte[]) FchUtil.createMultiSignRawTxBase(
                oldPubEcKeys.stream().map(bb -> ECKey.fromPublicOnly(bb)).collect(Collectors.toList()),
                cashList, toAddress, amount, msg, m, n, 1, true, null)[0];
        return new Object[]{rawTx, oldP2SH, cashList};
    }

    @Test
    public void signDataForMultiTransferTest() throws Exception {
        List<String> priList = new ArrayList<>();
        priList.add("a");
        priList.add("a");
        priList.add("9");
        priList.add("8");
        priList.add("8");

        Object[] baseData = this.baseData();
        int i = 0;
        byte[] rawTx = (byte[]) baseData[i++];
        P2SH p2sh = (P2SH) baseData[i++];
        List<Cash> cashList = (List<Cash>) baseData[i];
        //Sign raw tx
        List<String> signDataList = new ArrayList<>();
        for (String priStr : priList) {
            MultiSigData multiSignDataOne = FchTool.signSchnorrMultiSignTx(new MultiSigData(rawTx, p2sh, cashList), HexUtil.decode(priStr));
            Map.Entry<String, List<byte[]>> entry = multiSignDataOne.getFidSigMap().entrySet().iterator().next();
            FchSignData signData = new FchSignData(entry.getKey(), entry.getValue());
            signDataList.add(HexUtil.encode(signData.serialize()));
        }
        System.out.println(signDataList);
    }

    @Test
    public void createMultiSignTxBySignData() throws Exception {
        Object[] baseData = this.baseData();
        int i = 0;
        byte[] rawTx = (byte[]) baseData[i++];
        P2SH p2sh = (P2SH) baseData[i++];
        List<Cash> cashList = (List<Cash>) baseData[i];

        String signatureDataL = "2246436d763438625834616b444c6955665a46416b66326a73325862436275475a427902401546c827300312dc04aea322addd68c52aeed6e456ed3adf7be1928a4c4a55982f953a494b5d1e2a86554e591bdb9831fa3cf781bf3d57ea19aff9a19cc8765c409579d8cdf7db6dbc0dc47c5c9f1a82a3b9adea8b26e37775ab8599e9394875aff67b56caeb5d45a88e62c5231875dc52309938b60b23d9633c19cdd82bb0a8a5, 2246466854674133324c7a475133347a616f524656626d617334587373717345744653024077a34038c4241e2a20110f61b08e53223944c4686a276ca2966285ff32525ee669062a2818228c503d2abc00cbc786e6a8a6e4abe8d5cdd105a7c07a0e9a67c44050fbd827402b848ca54e92f0d2227a019b4d07d35ea808596b98b53a832a2acca0cdcec810ba3509e1b875c6c3c20db596a7e7b43796c99ba9565d432d9e39d9, 22464a345565615961457a5563547a4e476e39324642364d707137744c794d4643656802408df8a637356e3ac9c18eb988ebfecf3baddfbc772b3fd60c27f268e8a68aa52c98511b80d1cc253e7bb2a8dbeb4c045794109ba18304b28eb541ab97042d653340577f6c2ac4c22650caf452ef14191c49acea82197af3a3bf67c4ce4f7f9a2190d5e7c20f00cbde8a2eca9a97a6bc01e8c0040089b2466c0c1320481425329085, 22464b45536d5a6e5446396a61785058367671594c566869674b766b696d387a42326e0240f919094e23a4be9f84055042020905b1f4cea7daa24e7a6956468c2c28b4fe41448e9a4bb206449f7f3a33d96e2a36df277db550508a0182f624f7418a3b06a2408e6eae3d9609bf20d7f702951d6f8f5298c223ca5e149730e184575c33ca6ca599c297760a01c8d0e49bf5d6a9e1ed0c43ff229c8dd9ae7cf35eb27420680ab2, 2246466e6d4d51356e4234325132325150364755454c5677684738777a4b526155575802406db833dce40aed6f06f80a7cf465d23e34097d95f7f55ee5e014525a39c11a84b09fbd31cf9a413c3af3ca0a58d348658435e7fef98d8914fe480f09232d75d240378fe85104e97cfd5c04ed2ff1057c7279d0071f4fedb3de9029d50ffad9728800e5dda9803fe626da1310234a0c81116cfb5de5e427e7a872a639e41e00956f";
        String signatureDataN = "22464b4474636131773954674e4b71374632346957434c39665a4b5a553741435a6a5602408c42fe7505907ab758fd14ce698112258850d63d0b9bda076aa1ea3ae349868d357c48eb3da2e40ea43c6ac01d200745860b6e62415f103964c41edc2e5fcb9240d5aa58b7470aee4218fabca7d4956864bb78d965938f26c3e2a970e8d9bf56eea108d8d590f002989c2ca39c6a51a93f59e80b5292552a0a22b5659982e6bf99, 22464167665270566755674b6252507253757967654a717441684c4b525451706f4d6b0240ac00aa31ba44fc507a7ca39fe7a8197718dca0b43383fc4b656a60e3d0594b354d36ac44160820c5612da5b7a16d13e2c6d71f488d0e67c41f5d295a89f22af940983c3e572f1753bf943e8b2bb77ceb2e8bb3f39f78add9b8d090780533f9e08f9b3e8a63fb3fb0442b9848bc4c12c3d6325ae5a41640e4fb9d0341aed9a053c8, 224650656b554351756e646a4b565a5262503334384a534c36544454673475446841770240eaac695cddcc42bd1c794c059b1eb98f9209dc4446b9d2bd30be0c6bbd669994f4d7ddf9594e218b95ff91818fa971d511cff01336e0fa612110190fb034d074401f21a9a43d8ef955dea4a5e01ddfe8335cb3bc25cac9e18b4de72aec04b93d3d8ba3fd1a903426c422359dd53be4ad85bd22a38fbecd8c4b60ed2aaa00fdd9be, 224652423878386f32506b3842786551427648356768644a33776865434334506436500240e77c24e11117642b0e1e1de55257ba7cff89ada4c0326f45f0cce0683d9e78f7a418d60b9b7bf675d30c26407ac6446c00dd87866e4eb2dc57170801c6bb445440ba51f038c3cf8707eb52b7f7b951c1649436c9d3accaf43689d21be3567d8f17dcafd5df2f93cd69b0695c0a08d3e9f76f53276288c82188e2355ac0702c66b2, 224652624b75526a3951654131474145766e6947556975394d66345a4474644d626241024070c15eb46cc8d14ec9ccfa9fba2a7bce8c2248b48a168514609aaf7cf666f61c762dd513b5694c52e7d7dddbcc26c1ace1d06d0d19a1c465117a5aa970eb796940b6dc76b3d4c1245ef66bcd77bb9a4ae4d7df25d8484a63ddcb86d595ad47e369202366e8965048916a9a42b8ff10f96487c0bfcc80c4c141950314354804ef50";
        String signatureData = signatureDataL + "," + signatureDataN;
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            FchSignData signDataObj = new FchSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(signDataObj.getAddr(), signDataObj.getSignatures());
        }

        //build signed tx
        String signedTx = FchTool.buildSchnorrMultiSignTx(rawTx, signatures, p2sh);
        System.out.println("signedTx: " + signedTx);
    }

    @Test
    public void testDeposit() throws IOException {
        long feeRate = 1;
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        ECKey ecKey = ECKey.fromPrivate(priBytes, true);
        String owner = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());
        System.out.println(String.format("from: %s", owner));

        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, owner, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        long amount = new BigDecimal("0.8").movePointRight(8).longValue();

        //String feeTo = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        //Double nerveFee = Double.valueOf("0.00002");
        RechargeData rechargeData = new RechargeData();
        rechargeData.setTo(AddressTool.getAddress("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad"));
        rechargeData.setValue(amount);
        //if (nerveFee.doubleValue() > 0) {
        //    rechargeData.setFeeTo(AddressTool.getAddress(feeTo));
        //}
        String opReturn = HexUtil.encode(rechargeData.serialize());

        Object[] datas = FchUtil.calcFeeAndUTXO(cashList, amount, feeRate, opReturn.getBytes(StandardCharsets.UTF_8).length);
        long fee = (long) datas[0];
        List<Cash> spendingCashes = (List<Cash>) datas[1];
        String to = multisigAddress;

        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(new BigDecimal(amount).movePointLeft(8).doubleValue());
        outputs.add(sendTo);

        String signedTx = FchTool.createTransactionSign(spendingCashes, priBytes, outputs, opReturn);
        System.out.println(signedTx);
        ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        System.out.println(client.getResponseBodyStr());
    }


    @Test
    public void testMultisigTx() {
        String from = "338uHAHG2Gs3aufiFz89R4wsvPYH6yYTHv";
        String to = multisigAddress;
        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, from, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        int n = 3, m = 2;
        long feeRate = 1;
        long feeSize = FchUtil.calcFeeMultiSign(cashList.size(), 0, 0, m, n);
        long fee = feeSize * feeRate;
        long totalMoney = 0;
        for (Cash utxo : cashList) {
            totalMoney += utxo.getValue();
        }
        long amount = totalMoney - fee;
        String msg = "";
        //Make raw tx
        Object[] rawTxBase = FchUtil.createMultiSignRawTxBase(
                pubECKeys,
                cashList, to, amount, msg, m, n, feeRate, true, null);
        byte[] rawTx = (byte[]) rawTxBase[0];
        P2SH p2sh = (P2SH) rawTxBase[1];
        List<Cash> realInputs = (List<Cash>) rawTxBase[3];

        //Sign raw tx
        MultiSigData multiSignData = new MultiSigData(rawTx, p2sh, realInputs);
        MultiSigData multiSignDataA = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(0)));
        MultiSigData multiSignDataB = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(1)));
        MultiSigData multiSignDataC = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(2)));
        Map<String, List<byte[]>> sig1 = multiSignDataA.getFidSigMap();
        Map<String, List<byte[]>> sig2 = multiSignDataB.getFidSigMap();
        Map<String, List<byte[]>> sig3 = multiSignDataC.getFidSigMap();
        Map<String, List<byte[]>> sigAll = new HashMap<>();
        sigAll.putAll(sig1);
        sigAll.putAll(sig2);
        sigAll.putAll(sig3);

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<byte[]> sigList = sigAll.get(fid);
            for (byte[] sig : sigList) {
                System.out.println("    " + HexUtil.encode(sig));
            }
        }

        //build signed tx
        String signedTx = FchTool.buildSchnorrMultiSignTx(rawTx, sigAll, p2sh);
        System.out.println("signedTx: " + signedTx);

        ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        System.out.println(client.getResponseBodyStr());

    }

    void setTestnet() {
        pris = new ArrayList<>();
        pris.add(pMap.get("t1").toString());
        pris.add(pMap.get("t2").toString());
        pris.add(pMap.get("t3").toString());
        pubECKeys = pris.stream().map(p -> ECKey.fromPrivate(HexUtil.decode(p))).collect(Collectors.toList());
    }
    @Test
    public void testWithdrawTx() {
        setTestnet();
        String from = "3AgTp9hTvJT6oBBDm8z4KYw46MikedC2PA";
        String to = "3NLqrstiNFrLie6g9H3jMkAe2g9Sp8b8dR";
        ApipClient apipClient = FreeGetAPIs.getCashes(urlHead, from, 0);
        List<Cash> cashList = ApipDataGetter.getCashList(apipClient.getResponseBody().getData());
        int n = pubECKeys.size(), m = FchUtil.getByzantineCount(n);
        long feeRate = 1;
        long total = 0;
        for (Cash cash : cashList) {
            total += cash.getValue();
        }
        long feeSize = FchUtil.calcFeeMultiSign(cashList.size(), 1, 0, m, n);
        long fee = feeSize * feeRate;
        long amount = total - fee;
        String nerveHash = null;
        //Make raw tx
        Object[] rawTxBase = FchUtil.createMultiSignRawTxBase(
                pubECKeys,
                cashList, to, amount, nerveHash, m, n, feeRate, true, null);
        byte[] rawTx = (byte[]) rawTxBase[0];
        P2SH p2sh = (P2SH) rawTxBase[1];
        List<Cash> realInputs = (List<Cash>) rawTxBase[3];

        //Sign raw tx
        MultiSigData multiSignData = new MultiSigData(rawTx, p2sh, realInputs);
        MultiSigData multiSignDataA = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(0)));
        MultiSigData multiSignDataB = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(1)));
        Map<String, List<byte[]>> sig1 = multiSignDataA.getFidSigMap();
        Map<String, List<byte[]>> sig2 = multiSignDataB.getFidSigMap();
        Map<String, List<byte[]>> sigAll = new HashMap<>();
        sigAll.putAll(sig1);
        sigAll.putAll(sig2);
        //MultiSigData multiSignDataC = FchTool.signSchnorrMultiSignTx(multiSignData, HexUtil.decode(pris.get(2)));
        //Map<String, List<byte[]>> sig3 = multiSignDataC.getFidSigMap();
        //sigAll.putAll(sig3);

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<byte[]> sigList = sigAll.get(fid);
            for (byte[] sig : sigList) {
                System.out.println("    " + HexUtil.encode(sig));
            }
        }

        //build signed tx
        String signedTx = FchTool.buildSchnorrMultiSignTx(rawTx, sigAll, p2sh);
        System.out.println("signedTx: " + signedTx);

        //ApipClient client = FreeGetAPIs.broadcast(urlHead, signedTx);
        //System.out.println(client.getResponseBodyStr());

    }

    @Test
    public void testVerifySignCount() throws Exception {
        //String signatureData = "039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d9800140925cc61f1f8d43b7e6a760967a221534820ebc9ad7bb9a7434666f9d21e84fe734c265b836994a424d3fc55f6efa623a5e0a967497d20febadbbe6bdc06b1567,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4014050876d83a9b776740b0dbf4692e0071a6802c6f6e08c626d291a733cb0c9b196b2ec674dc002f106f612a1e556037e6e6d058c8cf0e442107556c23764045a52,028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70140205ca6c79cc1632dfed65383949ce6325a5d048927c8ba9801ffcef12a36b5a86e76fc830fcd56d7c648887e47f554cb6699ebf5f87191cef2922a9df66b698f,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b90140f6ec6d1440ef4933b5d02b380c2ea805118e9b783b386b281c6d02a5c0552f6fdef496cc81599b7003f161454296b4878f50f1225908becf15a430252e039ba1,0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0140c8e675e61d64d76b2cb4239f0d8ff6ce3998bbec462c17feb3863ff0d2eb3278673990b80f84c92a12c51dedb77f2d7e76c1a3d7c19d0b3fa0894e69128234c5,03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c90140cfd454539b667109924b0011ca65af9b064918a0157155c2b2fcb02d4d76ad4d6e9a130a0cd6c445bb1867b48adea9691b69adff6e2201078a1e848741f5c0fd,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0014033dc7674486567488de0d9d56be282ae4d3fea048affc8af8cc449c22a0c249cc10295b46c169ad798f3c19e85350b4819e51fa6f43414f753989b4c90236d4c,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40140753c4b07f32ed45c6957551b119019e6114da8c014f2ec3be4d5725ed4908a7aae7f940c49920f0d260a46ed20acdc4bd58b2af4397077d26ff4674f78942927,02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049014066fad3fc5d9a9f62878f422a9c74ff725edf1ae3c5a7c5882f027950570d5dca7b401659aa1b0f9107c9425b68714e6284d555e0f7395cf1d6e8276f78ba4cd6,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d0140473d1b5027438635faca58eaffe3e7f75b2a0ffd1b1ad19d616d53323f1b73ebfd476178bd504fde813feb8424cc22830bb8af176beb877f08bc18391178711b,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803014057404edc24332bf4c2298b0b39ef2644513460ecf8322d04b1f6915939ab81b8e4c95d9fd8e2325126f957506b59fed99989b44c29d004f4f4bd280c345f448d,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee210140a4db10b037a9da7c37aae3d90d276265346206e667737d210aa20a3574b16d683265f6aabc070d46d4f7d4743d808d92f8c62e4472b5795967d56f5048d1b9ea,02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c62920140030e625b7136831617a6d0c565e2467d2e5934f439ef4e99a2760bb637c4d3647914517d7317f5ef377d6c1ac4214576c38bf9114ba961d6b4523df63e87dd25,02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030140759d41c6bbb7c970c4aebd23ab10da2405fdebc4bb687ea95ed041efe9bede0a87af2321e2346006d8cfc8596b1fa03a6cf0cb785977df939e86e5c9c906eb19,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd014013af53848152735f98f5e79ad3dbb85a5d4770ed0f3f7c1e5083f4e34e2b7a4b51dc936718c4e60ba4d59fee674df606456c96c36b762dcab54d6ddb8f17b686";
        String signatureData = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0140c8e675e61d64d76b2cb4239f0d8ff6ce3998bbec462c17feb3863ff0d2eb3278673990b80f84c92a12c51dedb77f2d7e76c1a3d7c19d0b3fa0894e69128234c5";
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures());
        }
        //pubList1.add(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7"));y
        //pubList1.add(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d"));y
        //pubList1.add(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03"));y
        //pubList1.add(HexUtil.decode("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049"));
        //pubList1.add(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0"));y
        //pubList1.add(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21"));y
        //pubList1.add(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd"));y
        //pubList1.add(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4"));y
        //pubList1.add(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"));y
        //pubList1.add(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9"));y
        //pubList1.add(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b"));y
        //pubList1.add(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4"));y
        //pubList1.add(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803"));y
        //pubList1.add(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980"));y
        //pubList1.add(HexUtil.decode("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292"));
        List<ECKey> pubEcKeys = new ArrayList<>(List.of(
                ECKey.fromPublicOnly(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03")),//y
                ECKey.fromPublicOnly(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7")),//y
                ECKey.fromPublicOnly(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d")),//y
                ECKey.fromPublicOnly(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90")),
                ECKey.fromPublicOnly(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4")),//y
                ECKey.fromPublicOnly(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21")),//y
                ECKey.fromPublicOnly(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b")),//y
                ECKey.fromPublicOnly(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a"))
        ));
        List<Cash> inputs = new ArrayList<>(List.of(FchUtil.converterUTXOToCash(
                "daf04281c943b67f3c5af3c0af5946a369dbefb738defc5684124dbf5d2586ee",
                0,
                23000000
        )));
        String to = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        long amount = 8000000;
        String opReturn = "4d804d5d67a5cc655b2e727d3f25c105ee3e1f44e9bae63825bd9c6d27c8c30b";
        int m = 10;
        int n = 15;
        long feeRate = 1;
        boolean useAllUTXO = false;
        Long splitGranularity = null;
        int signCount = FchUtil.verifyMultiSignCount(
                signatures, pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity
        );
        System.out.println(signCount);
    }

    @Test
    public void testCreateMultiSignTx() throws Exception {
        String signatureData = "039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d9800140925cc61f1f8d43b7e6a760967a221534820ebc9ad7bb9a7434666f9d21e84fe734c265b836994a424d3fc55f6efa623a5e0a967497d20febadbbe6bdc06b1567,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4014050876d83a9b776740b0dbf4692e0071a6802c6f6e08c626d291a733cb0c9b196b2ec674dc002f106f612a1e556037e6e6d058c8cf0e442107556c23764045a52,028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70140205ca6c79cc1632dfed65383949ce6325a5d048927c8ba9801ffcef12a36b5a86e76fc830fcd56d7c648887e47f554cb6699ebf5f87191cef2922a9df66b698f,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b90140f6ec6d1440ef4933b5d02b380c2ea805118e9b783b386b281c6d02a5c0552f6fdef496cc81599b7003f161454296b4878f50f1225908becf15a430252e039ba1,0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0140c8e675e61d64d76b2cb4239f0d8ff6ce3998bbec462c17feb3863ff0d2eb3278673990b80f84c92a12c51dedb77f2d7e76c1a3d7c19d0b3fa0894e69128234c5,03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c90140cfd454539b667109924b0011ca65af9b064918a0157155c2b2fcb02d4d76ad4d6e9a130a0cd6c445bb1867b48adea9691b69adff6e2201078a1e848741f5c0fd,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0014033dc7674486567488de0d9d56be282ae4d3fea048affc8af8cc449c22a0c249cc10295b46c169ad798f3c19e85350b4819e51fa6f43414f753989b4c90236d4c,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40140753c4b07f32ed45c6957551b119019e6114da8c014f2ec3be4d5725ed4908a7aae7f940c49920f0d260a46ed20acdc4bd58b2af4397077d26ff4674f78942927,02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049014066fad3fc5d9a9f62878f422a9c74ff725edf1ae3c5a7c5882f027950570d5dca7b401659aa1b0f9107c9425b68714e6284d555e0f7395cf1d6e8276f78ba4cd6,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d0140473d1b5027438635faca58eaffe3e7f75b2a0ffd1b1ad19d616d53323f1b73ebfd476178bd504fde813feb8424cc22830bb8af176beb877f08bc18391178711b,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803014057404edc24332bf4c2298b0b39ef2644513460ecf8322d04b1f6915939ab81b8e4c95d9fd8e2325126f957506b59fed99989b44c29d004f4f4bd280c345f448d,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee210140a4db10b037a9da7c37aae3d90d276265346206e667737d210aa20a3574b16d683265f6aabc070d46d4f7d4743d808d92f8c62e4472b5795967d56f5048d1b9ea,02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c62920140030e625b7136831617a6d0c565e2467d2e5934f439ef4e99a2760bb637c4d3647914517d7317f5ef377d6c1ac4214576c38bf9114ba961d6b4523df63e87dd25,02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030140759d41c6bbb7c970c4aebd23ab10da2405fdebc4bb687ea95ed041efe9bede0a87af2321e2346006d8cfc8596b1fa03a6cf0cb785977df939e86e5c9c906eb19,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd014013af53848152735f98f5e79ad3dbb85a5d4770ed0f3f7c1e5083f4e34e2b7a4b51dc936718c4e60ba4d59fee674df606456c96c36b762dcab54d6ddb8f17b686";
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(KeyTools.pubKeyToFchAddr(signDataObj.getPubkey()), signDataObj.getSignatures());
        }
        List<ECKey> pubEcKeys = new ArrayList<>(List.of(
                ECKey.fromPublicOnly(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03")),//y
                ECKey.fromPublicOnly(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7")),//y
                ECKey.fromPublicOnly(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0")),//y
                ECKey.fromPublicOnly(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d")),//y
                ECKey.fromPublicOnly(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90")),
                ECKey.fromPublicOnly(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4")),//y
                ECKey.fromPublicOnly(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21")),//y
                ECKey.fromPublicOnly(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b")),//y
                ECKey.fromPublicOnly(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803")),//y
                ECKey.fromPublicOnly(HexUtil.decode("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a"))
        ));
        List<Cash> inputs = new ArrayList<>(List.of(FchUtil.converterUTXOToCash(
                "daf04281c943b67f3c5af3c0af5946a369dbefb738defc5684124dbf5d2586ee",
                0,
                23000000
        )));
        String to = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        long amount = 8000000;
        String opReturn = "4d804d5d67a5cc655b2e727d3f25c105ee3e1f44e9bae63825bd9c6d27c8c30b";
        int m = 10;
        int n = 15;
        long feeRate = 1;
        boolean useAllUTXO = false;
        Long splitGranularity = null;
        String multiSignTx = FchUtil.createMultiSignTx(
                signatures, pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity
        );
        System.out.println(multiSignTx);
    }

    @Test
    public void des() throws NulsException {
        String str = "4d804d5d67a5cc655b2e727d3f25c105ee3e1f44e9bae63825bd9c6d27c8c30bca002233356e41587861374374546b3164525a47596761336342666e376d485242347153380f00010000000f0002893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b402a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b902ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f002db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe9003929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d98003ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c903c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee210308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b80303743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a010040646166303432383163393433623637663363356166336330616635393436613336396462656662373338646566633536383431323464626635643235383665650000c0f35e0100000000000000000000000000000000000000000000000000000000";
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData();
        txData.parse(HexUtil.decode(str), 0);
        System.out.println();
        txData.getPubs().forEach(p -> System.out.println(HexUtil.encode(p)));
    }

    @Test
    public void testCreateTx() throws Exception {
        String signatureData = "024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba7501401611d7c3ad3f48f886249aa4d0f7801448a9a128e244c441d0e29fe32691e0c5f7f1f01a4fdaa880e8101d8b054de66d6c1c02329af125312a253c0bac1b7a0c,02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded014028adc4c9672bee8588a853a18126c6528040dc29c273d7bc30ce980b1594c55f5c95e10a6428e70d0ac3c53883e06c2e9e34b2c55b1c252906fd827e424a5355,03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd901408aa6b8440820157cc7dae938063b2ae1f60e7cb93e44650faf57f955ae0bbaa68f308afa1e2591ca4b28c6be3c1f6dc105cc9351024d057a6b788893fed74ae2,02184978060ada31b7db704789dfa986cd5a6f7ed63f727a66d29598e0186058770140c9a2b956c4b44ea4e8f2c33f0e73ff542d820f0e1a1e0463b982b17fd6053dae1264b1e938dc8b0051bb73bb0f986dd350f11cf00bca2a733ea606dfa45bf1fe";
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures());
        }
        List<ECKey> pubEcKeys = new ArrayList<>(List.of(
                ECKey.fromPublicOnly(HexUtil.decode("02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded")),
                ECKey.fromPublicOnly(HexUtil.decode("02184978060ada31b7db704789dfa986cd5a6f7ed63f727a66d29598e018605877")),
                ECKey.fromPublicOnly(HexUtil.decode("024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75")),
                ECKey.fromPublicOnly(HexUtil.decode("03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9"))
        ));
        List<Cash> inputs = new ArrayList<>(List.of(FchUtil.converterUTXOToCash(
                "f0ea35da22ae8284dd847264a07c65738a52892d170a874203a7fb419ef0b9f9",
                0,
                14865479
        )));
        String to = "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD";
        long amount = 2000000;
        String opReturn = "d5c0ae95b0ae606b1abc9b80a44005011e629c5fd5b7b8514ce9a974899532db";
        int m = 3;
        int n = 4;
        long feeRate = 1;
        boolean useAllUTXO = false;
        Long splitGranularity = 8000000L;
        String multiSignTx = FchUtil.createMultiSignTx(
                signatures, pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity
        );
        System.out.println(multiSignTx);
    }

    @Test
    public void createDevMultisigAddress() {
        // dev
        List<byte[]> pubList = new ArrayList<>();
        pubList.add(HexUtil.decode("02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded"));
        pubList.add(HexUtil.decode("02184978060ada31b7db704789dfa986cd5a6f7ed63f727a66d29598e018605877"));
        pubList.add(HexUtil.decode("024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75"));
        pubList.add(HexUtil.decode("03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9"));
        int m = FchUtil.getByzantineCount(pubList.size());
        P2SH p2sh = FchUtil.genMultiP2shForTest(pubList, m);
        System.out.println(String.format("makeMultiAddr (%s of %s) for testnet: %s", m, pubList.size(), p2sh.getFid()));
        p2sh = FchUtil.genMultiP2sh(pubList, m, true);
        System.out.println(String.format("Order makeMultiAddr (%s of %s) for testnet: %s", m, pubList.size(), p2sh.getFid()));
    }
}
