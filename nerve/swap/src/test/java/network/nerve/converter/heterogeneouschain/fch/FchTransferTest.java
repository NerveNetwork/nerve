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
        String to = "FMRAEWxRTeYJPBkvroPtdvTFsBWSGfXUHW";
        //long amount = new BigDecimal("0.001").movePointRight(8).longValue();
        long amount = 200000000L;

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
        String txHash = "c7040c7fdbbe8f445614df57a5f7499d72ddb9b680b9ed5c350db2f97c4d6140";
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
    public void opReturnInfoTest() throws Exception {
        String cashIdOpReturn = "4c53736434b79e9b87fbe3988689ba989b29642bbe5f35c245d44b8b41f8fea5";//267536901400
        ApipClient client = BlockchainAPIs.opReturnByIdsPost(urlHead, new String[]{cashIdOpReturn}, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("opReturn info:\n" + client.getResponseBodyStr());
        Map<String, OpReturn> opReturnMap = ApipDataGetter.getOpReturnMap(client.getResponseBody().getData());
        OpReturn opReturn = opReturnMap.get(cashIdOpReturn);
        System.out.println(HexUtil.encode(opReturn.getOpReturn().getBytes(StandardCharsets.UTF_8)));
        System.out.println(opReturnMap.size());
        //0500017ab79bd5c00354e7d4f346749ad7d2c1f8bae031fd102700000000000000
        RechargeData rechargeData = new RechargeData();
        rechargeData.parse(HexUtil.decode(opReturn.getOpReturn()), 0);
        System.out.println(rechargeData);
    }

    @Test
    public void ttt() throws Exception {
        String s = "0900012fcb129a81c09781ca49d2f05ed369bf6123bd75ff3cf8555458000000002437396534363239642d316639372d346635342d623035612d3365633337613137666239390000000000";
        RechargeData rechargeData = new RechargeData();
        rechargeData.parse(HexUtil.decode(s), 0);
        System.out.println(rechargeData);
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

    Object[] baseDataForChange() throws IOException {
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

    Object[] baseDataForWithdraw() throws IOException {
        List<String> newPubs = new ArrayList<>();

        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");

        String utxoJson = "[{\"cashId\":\"cb225f82bd590cabb7d639d602d15b12c13276413a07b88917015b0b8ed3665c\",\"issuer\":\"35nAXxa7CtTk1dRZGYga3cBfn7mHRB4qS8\",\"birthIndex\":2,\"type\":\"P2SH\",\"owner\":\"35nAXxa7CtTk1dRZGYga3cBfn7mHRB4qS8\",\"value\":880176141282,\"lockScript\":\"a9142cd9c54dac21876831a03321a094e24329bb5dc087\",\"birthTxId\":\"f91d564a5eee5470c41344db32d1f62cbe5cb0231242f741e382e36a34c554e0\",\"birthTxIndex\":1,\"birthBlockId\":\"000000000000019f41d536a2d50fff134b3d66cfb2e39fafa7e65c2b46d8db7e\",\"birthTime\":1724142942,\"birthHeight\":2385047,\"cd\":105621,\"valid\":true,\"lastTime\":1724142942}]";
        List<Map> dataList = JSONUtils.json2list(utxoJson, Map.class);
        List<Cash> cashList = ApipDataGetter.getCashList(dataList);

        List<byte[]> newPubEcKeys = newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());
        P2SH newP2SH = FchUtil.genMultiP2sh(newPubEcKeys, 10, true);
        String toAddress = "FG18H73Tgy1aAqgk3RTwuFZjZzyRLYUzK6";
        int n = newPubEcKeys.size(), m = 10;
        long feeRate = 1;
        long amount = new BigDecimal("3729").movePointRight(8).longValue();
        String msg = "";
        //Make raw tx
        byte[] rawTx = (byte[]) FchUtil.createMultiSignRawTxBase(
                newPubEcKeys.stream().map(bb -> ECKey.fromPublicOnly(bb)).collect(Collectors.toList()),
                cashList, toAddress, amount, msg, m, n, feeRate, false, null)[0];
        return new Object[]{rawTx, newP2SH, cashList};
    }

    @Test
    public void signDataForMultiTransferTest() throws Exception {
        List<String> priList = new ArrayList<>();
        priList.add("b");
        priList.add("a");
        priList.add("9");
        priList.add("8");
        priList.add("8");

        Object[] baseData = this.baseDataForWithdraw();
        int i = 0;
        byte[] rawTx = (byte[]) baseData[i++];
        //System.out.println(String.format("rawTx: %s", HexUtil.encode(rawTx)));
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
        Object[] baseData = this.baseDataForWithdraw();
        int i = 0;
        byte[] rawTx = (byte[]) baseData[i++];
        P2SH p2sh = (P2SH) baseData[i++];
        List<Cash> cashList = (List<Cash>) baseData[i];

        String signatureDataL = "2246436d763438625834616b444c6955665a46416b66326a73325862436275475a4279014037479d9ce532a214fffe53eda3cebd0bb6b1a409731cdbcf9dcfedab33bb81d0b837a486b66fe53e97e4f27e31d1a0f8ccd423da944784da0d9990212c1fc3f7, 2246466854674133324c7a475133347a616f524656626d6173345873737173457446530140ac336d502c30d3c01f1da73986768871a2aaae758e1a95650cf1a7899a70042e7cd80f8df50760dbf7ba7dad46b58d62a8d231f4853d691d99f6283c47230bcd, 22464a345565615961457a5563547a4e476e39324642364d707137744c794d464365680140df4464c17a42d81b46fbc647a9669269fb6f1c40d7bdfd1c67972ebff7f70b3e90449f42445880ad2b223b9ab0f4f404f579c6f418b6c31d7dd0317273b8d44e, 22464b45536d5a6e5446396a61785058367671594c566869674b766b696d387a42326e01400a9da255f5ac485ad8df9b902922f6b3d7b78aacfe92e1bf96ee387dfdadd27130e22322f2d99e0ec3529722b987b22196bce46900156573a4b59573e635c1ce, 2246466e6d4d51356e4234325132325150364755454c5677684738777a4b526155575801403b5d4a2d3d86124ac07ffc7ad7b122a3352eb0900547eaf8964d5d72540ef4f98c1d35a365baaae04b1bb5bebb135040fd6b8ea6c602ddd09b221edddb76b75b";
        String signatureDataN = "22464b4474636131773954674e4b71374632346957434c39665a4b5a553741435a6a5601403149d8135edae68e4d7f68af7956a54ac68828049079d249d2655b33e8591f199a325fc7dfd01700afd573f1a8132c1eafd137faac5af533ea25a3cf0e75d693, 22464167665270566755674b6252507253757967654a717441684c4b525451706f4d6b0140845080d135d9e653863d920a8a408a42aab5290dcdd6b05cc197f840a322d156da4b399ab3d724c05dd2c9124f2cd4cce88df7d637ef1f698b5704aa5c033385, 224650656b554351756e646a4b565a5262503334384a534c365444546734754468417701403f2dc7198daa216531dfa4cbfe044a433a68bf16ebceef16fbb32922fd76deba638a73305d4e971b55d84256d21d95b508230bc91c1ef8a3f04fddbf2594f371, 224652423878386f32506b3842786551427648356768644a337768654343345064365001406050e488ab724cc01dee08a03708a9065791edf4d6c855df7c19fce05293084cf737399c3e1a96a444f9acafbf1592e961c139e1650ade2ac82c63bb2464a2a4, 224652624b75526a3951654131474145766e6947556975394d66345a4474644d6262410140bd8f249176482a85ac76ae5270f904234f0ad41b5e247ac82454ddc54903f989cb64086d1374f80eba46b0eb76dd97973613ffab71f2bdbb64893ecb62364c91";
        String signatureData = signatureDataL + "," + signatureDataN;
        Map<String, List<byte[]>> signatures = new HashMap<>();
        //System.out.println(String.format("rawTx: %s", HexUtil.encode(rawTx)));
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
    public void testParseMultiSignTx() throws Exception {
        String signatureData = "028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70640e39454864197fac9497479b5249662ca50f3405b1da488cfd5748702769300782ad559e2d63fd5947aa701dfb7337e9d39a357b099248b587600063cf66c086d406338703df07fa80234e2c86b9c707d08939295cfd3cf76b950420514da93e76a49e7b5b7203ccc16cb2ef7af651c2e5600df18c5943612a9c0a01ca1c1098a0e40b7b5a1e50d1de55bfcd4b2e60eca908516a00b9489f0f23ec4680f3398c52110ce350300c8a923efc31262b09c5623882bde3ce7b5d54c83c76fdfce17ed10934079693b5755b6bcda90d6ee47ba7f521538ed126fbf8337daf94021655071a2566dbe1373d3bf292bad57977dbc1bcdbce138b3f800953bcebf29eac1be2265cb402f30464a6d5ab7bd680d18c97e34066d8e6d25bd8fc51c04838d9b4e7846cfe2b0cd109ad8147346bc45437ca9b24671c7e32d673e2f8b833d88c397b4c90d7a4096f284b515e69ead86264a39a2a66d0b4aeeee5e5c1408b4bef107b9cc844905de89384aa39f3bdd85cc103329a0d582543439d4849f33db39eff887780e07b5,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b406407bbfc2283937f73f0350509424ae6707217a341bc346b0fafbe7805b16198dbdd83487f240d43d12b6f8d12d48054ee4562075043a95e8f011e8f6b850deafd6403b5718b7aed29af99036553fe5e6194b5b05f76b4986dd4d0ef94eb8c24bdc26b5ae8722ae2f2389ac6b5b7044eebef5465760d9081e7808a29af603d72b6d2c4062b71099a34aadaf93750af735f3d9aa64f773543e577e731b1dacb566d4aa426dfa48931bb44bce54feb0d431965f96df5d0ee88c56ffd71e04097936f9c2a8406c1bdcbce9c8a4bc96bc519e72ffb17768c654842d916215e9f41c0527e013608459d881760bb3a40b0fa0e05bbc8faccbcd05d194eeb22af1975e91f63f290d40fd16f1116010d4d87efdc6266130467f04422961f0fadbb50b93a9bbb549b1d85bb4c225e389dc4d4dff852c1aca5bc4959bdec9b68a1825a75cf620a8a70f5140b2fd40eba37fa638363f1bfe16107c9c702dd77c6a8e7c6b6da5c43464c7dc370c3666b258188e57262c4f671cff25a8fced6b4a1a2a7f54b65fbd0aacbeea63,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40640243368c9d1d0f961b2c6bc441b80da390cf64f3888921297d1a30aad5ffafa802de108db63041c230a819978b28339f1a4509bc7aa310305300c2804936b60f4408a2e50a645e79f573fff5da55c42185bd6e4d02abd19cf96af1b0f59da7903738c57b8cd0f6b3bde24afdb70aa7b1c7d4b2b60792e4ca5283fe720653da24d6040a136a7c860bce28cc62626fbc97203a9c58ca8c018004decbfa25b83dadf138decba2c7e3123730896280a5eef7247ac0e941f85404756794f70d6e924554bad404f12f0c2ed5797c5e9f0aa22d91b8cdf9ace7882cbc014a6b7b53c1a6e7bc84b4f421456a15ea65327068a34563eae547b7b99ff44370bfde0d11b69cc0af73640af812f47dca7648768b135fbde345ad244d7c676d73b4e12f350204a777ad512d11a1f072f077365bfeb61cb856f0c285789878e61d29e92d9facaf824b3d0d640e0e5e12d61055a38302bb6a495d705b0ae482974f803a640be4bec85612507a2aebe201e9d81db38b2814f6b0849c6b7288bec18d083801be895ae150748320c,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d064090f021b6e92b0ef309d1084fb5bce69f6bc079d1a4f29aea38a5f55c29162aca01a93af132b535624ede24f208af13e9c1c58b2535f4b8328d3e55448169318d40c9573e3406b7ee7a89b5dce26fe8f6623d2e7f6241d420b03c6367b0abba01f8b94523e34c1fb95d06af19e9e473647f4c55017c962fbf2fcad7f001e02c2b91405d511d7fcd4664606ff702e27c4d7a5f3e84ca6fc32e599974a97bbfbd9e0059093c560f6f5da88e5209e13df61b565018211f920785fd9259095ba33aa605a740793cb3f4ee89f9ac8baa9f289b410d13b645fc4a8313bd273a221a163310737c2114285f9f120e78af8986c2a842bb0fe98c4dc748b30243d26fc6d05d37028f40fedd1d4e5444575d28d8acfb461962d8f4f0261f7100bcc7bfb91aa182a9a710b0567e589476a412d8165605f8508e19816bdf11412b003b668b370f6783757840cd0a7a9e96d1f7efb048bb8ac7d56191b8d23bd98cf9d840af324dfda7a32ac58ed2d4f8a85d0da7b21583af600905eb0a7ab26dd77070599d5104225f7c1959,02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e30490640a7c8f5be2364c625e93d3e48d3117ae843b8376e861e16ad01ef16f9859e4486420cff5f664ab0b647ab5f17e7acb53c9d82eb8d23bb80a406b9b9a404e3fcb54021e64424e8f2b1bc41c2202088ce0011c8e0c1caf2ae9cdb0c66eadb7fbb7047f3ae8f38e864ae0b25bdb6f1b7e039f180fbebe6bce613cdc862b54af0fb5f7e404c5b7f9412002992b7dcb64a0b3796782e7b1018ee04237a7e0be410e384478c7ba5f189e19461c76c9613d35c344c8fc8fccf002866987c27ca6ffa6248f6c8403ff467491d6878debff9cdfbd3303fc9e9b71726e88093300b1fc9be15571eb496de947d7ff32beb4b9a7adde38dd9841d47767fdb22a8d5c63263e6b44e847a40a2c26e5ff25eb60521bdc4e18a3f9c655db6ea657db179afcb2ed46ce74842a990913939488eaa4988fc9b8743b0ff5aabf01cfe10b9d972dfadee88e5eccd01409fb7db65de79818456f7112da94a0196b05d5a0b3323e7d97856d0cc5a13a8940a3d5ff57f7908c78b52213413c5d4d72efbbf86e9f1ddf1a0ce737cc29c68fc,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f006401375c31310882e7595ddaeee0562643565d4b35d701e34792edd2c30f9c0e6a81e568a79fc7ccedb9f436d65a5de00ad369c54a35f3f29eecba05a7ca9eedc004033551fc7198032d4675699f27c441ae7de5724bb574c0f7a39c71e26053de485025ad0dda842142a1031f0847a1d9a5cb806e20e4916c54175cc08e67c9b7f5240fa964b36a37832e2af9af06b21045ba6a4859d644ddd604094f1d9ab6e5de9d1566d9bc21f47af93186dc385c350805a9e5257f84182d02f591da2667d71e639406c1441ace8de7c331a8db787df76dc5dc399bb26d2ca65540d16d56e371a9ef057897c98adb6758d47d16ee934031f1a4c9bc3ce7db4c54028ed13700e97600f40ea338aafecd4916930edc7a703036417e7cd4b42ffd71f18fd2143be3b189e52decea95f7cf65ffa9633fcf73da83784f173b0bc4d2db2cea69db2607891093b401ad6c5ebd4ab32a8e8b749ce48a487f8f7106be148e38658ce75eff9d36f992cecaa7dcbd01fb4297ffbdfd2c0bc6afa2407e7dac09fc9a49a5cec9d5b8f0683,02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a0306405296ab32fad97fab7a13952a7de0e5320f1f1f0ad3fc12c8a5ee01b3758e1ce04fc312fd7be966d2b04c7329b0b72aa14dc75fc54796e34f44c53dfce93a4c22408d2371023f4ee1ffc687e78dca924ba0aa8508b653f1fb46668d8a95077f5529448ab81abd8ea3c63ff40682b914d9dc3db7356ff64b5656c63013154c821db140133da8f5d5598ec122e9b4d49ec35c1e593846553dc96e15721ca7947c19a41628c2e7c99bb1384afd1204d9920bc6906ee30d7f0ebcc052e629f14a43e59b4a40bc83a8acc08658e129ea10465fce617fb4ba4a495c1b47c336f898843ffd4628c14a4a26cf661d29e86a3b35b38750042c41dce0d934b3ad73a7a5bfdd6a5ca340b691d2629a8005772fd557b0731de6af00ca1a4c725247bb6e4f12eff6eb2f9dc02bd13a0806b19ef48ccdede333b2018eecd83419d3bb638f874e16831aa81b402c0615ef0e01ffd26e6cbc9268b43814b80b5bbde077c1d0495a92434d8baeefd85947f3d85403a717fbacf142396214ed30dd634aa90042f43682f8dd6ec573,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee210640cb6e91eff93387dec1da4df27038dec5e70f4edfb306159b159496c1f932e58a3c3b298fc98afadde447646e61567f08d6041905cfbbd209494d57c2463bd6a44088d4d7495a609366e36f7a28500fe81de6689843685d74ba998a9f25c06a2817d09ae0640e868d55efd8abe17b73293cb6d79cf8700643253f67c8da0cfcb6c54096a9443e26b6dd7a69e1c6ce7f9bd240746b3c2bbccb08b3db6e1f64dd0b6d118045b83ba2a85c5d40900c08ae62226d41ea3bc64432e0e1e26b15cf7f50bcaf40aaa92bb31808ecf355600dbbac64b76be63687f696b075c45c9fbc0f45300f4409a7f654713350c5d7b35f38f69a51ff9c25ab899d798e5afabd430958229d6e402db0b5789bb77596805a70f0714e3019f7b8470d56834d256edb8a5414bed5096fedbb6447d2bc2471c34677cadf6540f41ea80f766b4ce871189a764357adf2402fdd4f098e82313ba7bf2b63b81a8b08dabe90162ba174e7afad1802d742ab7867be1815c70a6734f03cc2d7dfa197ed5faf314f191748d3fa2080a28b8df72a,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd06405beeecbde9cce0dd2ff123b28f845bc21c7e604ad24daacfa4838e615e9a2d03be338bf3472bd532a422c9b8b36188c60a0b66b9bf8303d59b3beb2dbfda5867406d7f169242f3ec76096837b7d8ad64b2e859da930ff18c88811c5a70e6271864c6cb836165b4d3696b7594088ee96aa84be32e5ee5ba095b48916a15843faecd408edf68002a6de66b94d80eae3cb16f0fa39b8f9409ed7dafc1d5a7b396767deeebaa846e0ebbb770e0271325f310b5bbd7cdca157872b017dc706433e08324d540bacebe265d53451da2fc8edfafd505c0105a3029ae3be2a9b90779e3d8f8c43bb693acef66671900e34be013010981efdaf847bc81af14ba90d3d43ae14661ee40881fa02a0c8ca60f1b77888ee860f2a150a6fb623c1f153c9879c166747bd6727925256480421c23c71fdc73391ab56945022ef86e4fc977439ce758f7d02b51406fd8ff0eae39c63d3f37a74b628900d9166491a0e343b7422e421b46f8b8e4a2b42cdbd6ab4f4a0e6ee042de89bad52bfd1f41da649ac7f1c57e6a0c23766139,039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d9800640e9b323cb4005ac70447159933d97ad91af605e3717baf84b38363c333ce6712f5e6bca30fe16d4f40d298171728c097cea66cfc3a8de383e7e954eae9d6a66e240a1d4c76f8ce6c9abcea5eff000b421e5506384558cf91e31e7bd30047586d5e96de58fd70b5df9fb40310c123c555379af77925f6071689506562441958665db40816bfdec3f9e26077204ac784d669a43a784983829f75915707c2691e8f204f270d9638b5b6b860652c032a631f0c3d5e09ce7fd13e6b3fe32f0d02f5b07f99740ba0f15218239ab40e0905da49ce7a9c9f560e46256388e5136aae9512103fe1b0b89e4df6dddeb576bd4d1e0eeabbfd122150a78fdfad2b1499d1727d3fbcd7040162f983e8d8f2e1d73982deb3c6df7318188b73a2d67c6a6c9c70acb40b812e02c37b2513b68736bfdbaaa8b03333a75aa0939d34bb0781a4e098af984f25e6840e9dd99aa452074abd9e9617747f7f3f4b0b7adbc115194a02ee59ce4cd3fd0b9fc255f8ff8b36695323a1e1609461386ce2a1fdeb74c33ff43ca9efec55bd373";
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(KeyTools.pubKeyToFchAddr(signDataObj.getPubkey()), signDataObj.getSignatures());
        }
        System.out.println();
    }
    @Test
    public void testCreateMultiSignTx() throws Exception {
        String signatureData = "039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d98014404a602ba6bf3484ef7a14b979736c6743008d76382227de224b6e88fb250ea5aa22c99812c028db2809cff53445f80c3bb0dac02f4c26a27e5acb0f628db7083e4070368e35394b3b91f01a899ba719b5ebe20a667088ae06e74ed623963b547d521054241c7ad89b89c026edec57e61be6ddc24b272fe16d9b8932e4a79eb41c54404f2c2a990458227e4e7553da32682633ca0d0c4486a2e08d986a6e680d1ac23caa9121c7ab506ee7646f9e812762d6b1c717f12895dd57fafd10de0a2d6cafe9409ae61532723a63bbfcd189990b215caeec3be1f5e03d0e075b40bb5830d3ee6a280f0b3ecb47b90dd6e06fdba9e2a3fa8aa4a0fa69ed96bb1328aa9a72e87ef1400ddfdc2853aa7bbefd3dd09a84ed6cc6322ad4acc8177995808efd27ed33062d9d125022413edb6ff785614e9ef70eba1879c5b21f9d663cfee16f7e3ef2de054012293ee18d6f7dc1711dac32592cd6e9e1d21e64bd92953124df6701ee8c7bd82ca1d9ec335e88bbeee3be12d667ecbfe3e03aea8214e6842a459315e148fbad403fd10457ed789d7e60ca0a9f85856fc5417d75787a84b7e097cbc69befb127075c002446563808998f9af1ce03e6c4729e25d4e04f24be9acd1b9a7cbf4006e74007895908157190756e07abfb48fcd92065bacc608406cc1b0bf3878f23d132b8eddae73f429347cc0916991dd2337d8de867728d1b0543f915bb371fec99cdb54032aa4a27d92bf75bcd05d0527f01ead397b136c305051957317395c31c64cb2cc3152e1165e28a9d78eac27e4e280a07c5055769cee171d18dbd0545d32437f0403d55e7f690b8cb2bf73514b797188f9fc9698f9a344b57b62343f5841578c5d5626f379a766e7615e7ab0e1c45e8cfa3344c264c9ead05b8df0e4d910344cae6405dd75e9a560456c28f4845cdf8fd7059af27b785a3ae46a18489ab48e65e6451ff570bb8f617caab98f1be0b354bc2d621fdb14873ecb65e40072bf218ad4ea440d93dffa6551a333389573c7ab7eb90df2a01fa207cb83344a1899293006cfb9cd59e5fb3aec4482f3a028bac02de1b4c6529fcb14b4ded97b10530e7390a06cf40c0a0593e0ce1bc199f875d4355146631908e65946e49ab7d86219119ef18086ddc141cf61804e18e7b3fdbd0548c068ed96ac22d09bba98ed3d1714e36aa478540ea091c935d6e0359154d1594a8775384058a3f87571fca58cb9483d6b190a84557d1cc2d1a513ca9bf7d232a58880454ca495a0757d8f0dedf3f56b1390df094406c00e7008fa09e3c90d1855345f5e47d9cb6c1944f571b26094d3a2587ae6c71e4e1ed0baf0b6404750b5fbf0d69d83c1ea8cbe43b4d36421714d5ec84ab54eb40a9d18b93b935adc1f8c743d8bb874e0eb9ea0d18d8e15a2b9e302506fcb08ca5e358aa8bad91be0a21de6bf7e6d8c94fe91d555cf87504cc9fefc387f443925a405d5c73907bfe1b11138d1dbd6455be3d2aedf41f5729930a89ffbfe21034e1632e1477ca456722e3d16097c256b63bc711efff375e5c6682f1a40358b7e858044082ed7bcc46ae57747132ec467788f272e079f166cd269c40b46d5d68cf756b7b0506ef54e2d4d1a0a8d89899a0f54ae208866ec1549f5f8ac4ae4b038da6f496404f0a046885fd5aaff6d19b3d4a9db218c237f7af0c75dfdfc520e0eedb2f35f6da7eadde9bdb3a28065b662d13e6fa8fa8a2f0953185a615eadf20b85ed4110340c010a96897a36ba1291db1973ca0ab8515f222b9ef1cd50b80413ea5cb982e1921929497d6ae6683e31900cfeb6e1d9637c1a24a5b5fb2db9cc387e4da489743,028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b71440fc96118591dbf4b4b79031f1aa7afe7be645104d42be045f611eef40df9563c971a5ad1d2a17005a685ceffa92884a42638f8679ca2751fb2bf2e2c8ccaccfa740d98b149437dc5d6d4597ff67a04d9f335dccebc3ad19c6d081a0bae9acea778f6d562fb9432fb19c3043e5ebe5eb97e76729035dce6b52051d61b2c4f23d95a74015286697b4e94ef3cf1d1d4144731712882cbc03d1452646f1b969f893c076f23721325910350c3cbfcb0328b9befd42b4bd4cf52c889568b885a4b7065be6624051f6c032a29268691ececd0a6075653de6558832c84549abbf391e03cde973280caba1a2a7a99f410e3039a34f4d921649779f9187d7cf6d887a86b5d8a87607409db2f872b48b4f6f78932592132dfb6a5e10f8f052ab76bdeaff96cc0c0788c905b0fed5a633a4f50618b44f1275ba56cd0fa54cb7e8605b01fee4d87775eabe4012f09225c1c57d8ba6089ab8af124b4dede54caa59fddd7be3b4557bedba7a020742622d5ae3e2069b00df80ccb17ca89623cfa7d2447f4cf8b239bb2b458bf440baac464ddb4db726bac70b2297ba2d5a8a80cf229909daf3bbdf9fb32d373736e0a36fc1f4e9d0f2c3e0e848025973481c4690105d67d0225bb0bf88aa60b9f140e878565ba48cfcae2230362352e65cb11a125aad40ecef8b60fcfb6df50cc54bfbdfd53445d6bc9d0fb5eff842ca9f547a510166f906557487d164968b2ba78a409543acd7d1f1b362a5a7e81ac59c7acb983a3f1e5fbbdd5e14706ac7db4b6739a948bd6f450ccdb4b2edd524c677894d00e7381e4cf8525658249ad2d497a62d4033094b4d6a7c073fe6863e749f3a0cb7ede19ee2fcc4aabc8cacebcb1b2b3d87fb4598a0ad02548c31728f3b89600d5178e526d0ef8507524cc7162fc23e146d4025dec7ce5a6913e1716cc3b24d062a8f3aa2a9ed9184bf474bcbab10f91ae7a9286edeeb327961cf0027a89eeece21e340b03ca699bee00fefa7b829622a2e94401542b9a10ca6e33dbad0c6ee2d3765f02ec7398bd00ea8ef6a37a011f44330962d7fbf74175bf87f0b091f5932e9afdffd8f3f2aaeeb0d56aa3c769ac8c458b4404e6eb481accc1cebe46348e4fe4cbce32c99bdc671f7558aaed7cc7c692aca17d98dd7eab51cfd7f7f19bc33e3199a45b589935bafd149aaf9d5f2271fc58c8340507da334f01074fccaa285f56c77d0fe45584bab7263340c448456ec2e73b73f55a5578a0cf7d1b688d4960f9b005cd1384dbf69a2584af56b94ceee6f56c1bd401bb52b934d70ca0c8683eaee1afa0890c5f4ea9f47fbbb1c44a304d90ab0c201f1ab7d2c3d0b160d2fb554b6f6109010485fc64aa6205baf87e33ec6a293588740fb324b7aa5733b038f9898ad14c1a75057dbf936be581c4b11c240f4e0cea442c0ffc3aff6701e6bc694f4389ac615c8acd28b702bbdbc8145ba91689a4b440a4069809b51988b4ac061ce3cf02f9323bf62cb3868243f40cb4387bf46f6d454728aaa4a9e693b8bd7ffa3e59b9a84f570da899ab88e7ca12fd1fd0327edd4f74c40e0f87b1db26b7681b625845df7e4a237733959c3c0dc6095a9250cf92aa8b96321e21f1c8749760dc30dd8e886b937162a24d56faf6a07209b48a16b47aac323400965e54ac1ce0aa47672880ae0c82d6af2335d2d417129244c7945d1ac6bd5fd5e142dff4ffe9f8c91717b164ebadfcbbe32da187f9e16745995f623ea4b99d540a6f677a4ef94c52f543f6c097ba052e72c1388816e354ef5b3e854316491b226abccfcba62980ca4766766d4817f5f831944bcea33197a79e7beee48aedb8f17,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b414407379ccfb122d9e66120544d92076cde99f880d9e7712c133fb61f0da2a2cf75f74966705da8acd1a6acccd79f743d2d9ee7a8c3b50cd42659902869fd5ab801940f04dbbcd1d45536a6b7ea26548abf8ddc50bcb4de9810ae274c2d0a0121594b29caa4c19764c1d587c9ae01dd118c70037c7312e5f33bf51f026910f41bbc264402cabe0e54ca0943adfa92f1d27fd644c63cb67096eb38658120ba63972635acae3da1d3f81cb8ea38bf70ce05b63748cf477065a908410580df8c71ab2abbd3240d796fd7b6f15643dc6e030abfa49697ec68ac4d240db99a89a8ccd890c44bec7859ecad3d45f1a0ab2a50a6fda3960c5f35ac870576013523337fefea8a100cd40755185d5aa94d7c04f296501ed7738a6bea9c2ed57f29973ca23c0d46513e5b215c781d63cec2cd9029e1476c3b7132f1c17c6d14523b3557815bf13dec5c87a40aee14db143731a0808dd43c411a9ab1c69839ce6cd7f142544e5b7fee78cd311d8772e23f07050ce14f44b4218405fb016eb7e5da340cd283bd9adde508d69f2405e8bbb68658e62300fb5013dde241664f66aa281f65f349ece83f8bdc953e11638970a683704a8ac989b4701b0cfb3f4fffd720b66a3654574fbdf844bbeb00240f6859f82251a60d5b7c5318c0c1cde4ab6710ba8209815dd70c09700c0f249f004ca0ac25cbece70a1248eb04aac99394ddfd2497c0d09f8befa0ed8bfddc97140b6c990d237e90152d3b5981577e5554bcce6d993a93a8dbed06b80923cf03a43cf17cc58b6c4f055e53e67c34272dbc658e3d26992f0a688197aff9110e1b5f7401d421fbfcdb6229fc6412ae522547eedf28b22d1ff9b9ee714996a21f95b300edd646d72012e51568c0d9d6d7a5af54ca80a078a7caa509dea00345395b1443340b56aea6eea619cabb6dc725d334d7b08f0bf6116552a26df034da69dbf4ccd9987971dab93831d3ece692965fa6d5f018a95a442b3c68e4ff965b6d7104aa3da409d1ac3c7c363fe6116e1214c1f877cbbd86b3c46ee6d636c7f96611a6106f725c73160ff5ddb5ca22ab51e6d15b3fdb40880bf9da213013c8f84768804fb2fe840d01b0440cdbe7d8e515b24c16193d99c9b4329ffec9f286a23cf0b3b49f958eb479af7bc408da07efe3867d7790cdb4086f961e36b418bc2dd6462d90e802864403a4db966ca75994c320366eb9f7985a6d5d254d596c3e98fdb4e97237c2316768492c06d2bfee37ada38e54749ed259abd5406156fa4334e98d108d81dd23467405687c198345ae95ff9a56187d22ae7553a97f32b016284fec4509b1167c5250677513277f91e3ad76f4b5305993517696b0c8370356d7f00122b4ded74d3f28b403d3bbdef9a00d61a961d07d36cd0931c4128c2e208cb5f106a60d9869cf43f703a4357ea56f248354521e53489b83a475037d4cb74045b9a7e54ca37d2b05ef0401932c0e2eb5c2617e17d71cf6fb4f5572424f90bd499390b991e0e37246b96571b238bea1e43bb6d57ca6a48995138c6a695c6098f925e9bb440b3f81305c9cb40420e1e860b84f2a7a04ab2f87fb2adb2294a5af2b77e1514374a5d3adeab3f200abb11b8cac99fbd7ab7f1e45caaf3394defffcc6cfb565fd3e1826a51123f21405ad91b070ce05f2f5ddd225c9f4c5d1f601fe156290ef0e4fbd7307621c15081a3e793a984b3b3f02a68a28d0b03680712f6d047980be207efb75eb41459a4fd40be60741ce9e2f9bef0c11f093009098c73ce9402bbd1247768e6d5087f266d621d5a54f8a6cff894961e4da7e69624ee776de31f87ac950c84ec895478aca11a,02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c629214405111d6db144fcdca2427d20e0e4dcbc90a2453e85c4e2ec703dc47356ea3a40500d7e81d071bd490ab308a9cfd7be45c268fb767457901610365220280510ffb404e710acd768bfd7c63766beedf4539c48f71455182729caafe72277e38275bf181fa4d161d576f15a2d7b7fa53e08669a2ebb4032557adba58561b96be70bd774070c33ecf3f3d5329978ca90054b6f649d79a4981f8624afb2145760e486459e53f4eda4578c54d2aa45446d6d2fd260b756a8e76d2f2c04c3fd1ef4cb325b9ef40483fba80e6938f8c726bc91a6f6afc122f008120af33b9bec920e6102c3f057a5ff972e93db569b1a2276432c3094c4bc6f7692449cf3706ba79cfd01f9c4af240bf2b1370020ad7778089cac2f06471e53ed5e809f53efdd9788c04109e3d145a8da54b929fa1918f16dd87fa581afa9cf9a48f3d471c180734a1c79e9eb6f93340a5b7dcb0bf13edbb8015bcc23cb10bb7967fa7c27b3421e010b5a59e54105a3091760830945919f55a320c76fa3e04d8b7d8d3e57c429514a025fc2c52cf8b944048d37c606c0952010b1023fe7f0bcdec372ccac1fb4da9be8d45a7f32c59a0cf79f0d345548c810dc43959d6dc98763cdf4f7305f3ae7f90a0c9bdb34f843a7f4091cd661fbb1343e122be632b53b9fab708159c168980a9ed6299859d79314d86f9ee2ffbaf6dfa93e53f19da92dd29b271d3497cf4f8578d73823b5496d1d0e34009e4f092f8be312e3fba4b15efd8db242867cd1ce800f5483df672599e68ef5195811300d4af286a5735a6b773ac651ebdc70d9335e1a67dd2ffd59cca296ea0406743c2f196ec0a3ef4ca56b42890b5914432a6d9d6ff4ffb674987775255fd33e1d499ea9c5a327b4aae1f13bd4f48ec13e6cbb16c878ab92afb9f9874d9d3684042bf21768a55dc0486fcf44ba035d6ac8a479e06c4b96fee2a23dca814a4275f369112f97f75a5d6fca88d38d81ab8cb2f8a1e11b5b1798e2f1baa7ada92c79540a96830e82b53488f955d4f2fe0a87cd94a8896ad3ad13bad8d9b27d670537663b3e669a40bfaa7740dc4f7c5a37270eb00c39706bf6b32dec5fa8959f5312d914094a1a787f988df4b716064f83bb847e6073a75034326a2d09c9e3e4e1612c5080abd4b55554bd1a359114e718b9306d2362518837ab4a60cf19c5adaa2bf52db4030dec1e1899bea3d6d15c7c13a40928730ee1b44e4e996b0409a6a84a0ffdd1d8cd34698966126325f7504478ac5513ed85fa885c5e6d68f2742d26d406a186940059bf4ddcfd958f055a41f69ee18ebbb5dd87ca9deacb95b24034f2a435d0fe3bbb558d62387de86972a695b7562862d73d948191cc9bdfcba0ab1eb19c1a1f240be4c9b93e91e3f0e6d9f39fd43554103ebd9092f45a1c5439dc4683291b10886bf8bfbc098e3a0ec904988b13684fc3cf20d7557dbba282eeef6361d4b6d59de40a56be32f8737653df153d3060c3cf5446abe4edae5ea49a6e4a1f0eb6d793b12de589f0d2117b110ed153d6e79aaec4e8d554f35ab7d95f9034667e2cc05299740ac0b662baeb904ae971c9e550b2377c5406ebe68dee53ebca32c81a09bfdd1cfdc59ab3dd7ca1fe35ac7d9241d36bee9a3ab530d49054e393aa6a49c94ea8cd84012a2fb8aa510e8958575cc5ddd27c5e92f6853a2101e3c44f7449eea1d8fbf58a82786989f8d4f79879028e639b5324ac215544bf42a53e371d2bdb1d28d57c140c0a150af75f369125560c308a33e99835df0bfede36710d00029aa35c7e22020e62d61d65f22c7c9cdc2ff6ec85122ae2b852509ee5bb62d1257866e802a1784,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd1440bdace93ebab7dada0e0408518104947bb8259046d2feec5c213fe67dd44ffe64e22504d81b3388334e8e6d2f8b59c6ceb9276f70e30e56982be820fbc0a787e74093da91eb73f47f33891b383857d943ea31c5b16844a6b703f83c7094fb4391cdf181b18fdaa466de906a30c841c2ca8f346fe2339150229f8e2d511a99793a0b4035fef988c93a434d8e071ad1e152f92b04a9d6061c286a78e414d07cee181922d851e753d573a365b40197f368a22d71d5d73ef71f188504c5e8eb7e467e768d40e428358b796bfa65152731a18f074c8079857479c8833e75e49747bf59eb9b21aafb1dba7fa0340d7eba48d44ebad91808bb7a798d0640fcfdb1dcda9effe3bd406d26de7cf11d57c6b33784b69f6a246e10b1590e36a50867481a15b97d05e60bf50baf23c5afef153bbe1b457f1c98dd67b828b0e733dc72563a9e4c5e34b552409eb22ac1fe17c7ee13c19942f917951e5694c887bfbc036c0310de54931f17d1b1603806fe82cbe3742060985217f0e9dc20e2daa0df82fe5cd1e8c53dad0eb240c7cde36dfb97d2078769f9374c73b976cdeb34e5185de30f1f57476211b54d2dd0b994f49fe015c9cc35dbbeaa2443f98154c12efdacde605c43e1416e25ec4740f28313df8a09a9b1d7ee72b87dcebffe4b372332629ce54232bdf2454287d9cad874ae0f82a5ea9eb92a60d813069a902a22ed9cd0fdc697972fa0fa9a848a4b40d38a05c375167da5bf7c78fbfb9613ba66f9626ce46ffc895182850180dbc71d55550ffa175e681cbe39ceb023714841a0e46e1c9093a7192f66907b9003bc784075c2973288a8c49e92c662b73a2ead3e6f19fbecc393aca12f4c17578cf4b5ebc100ae322c443b41cb5c9ecc3d865e2d0f02b146e6551c26905b4d36876ea5c040dc0376ef10ca042851231f732d553d356bb42768f24f03604fe7b17047f31221205647dd2712fa55b703faad20432e78535bb1925e5bb484b0b122374ff038da404636ceba6677006e07753f37fa2f6e0b34f82496cea293b4d0f2c1611b733e8b6ef9c9d5d867ff5e1ab409262310533408c84574b7392b72279063d27873c6ff4023d74b85430308a2ecfa3838abdc992758fd8c3f9ecf4eb43ce166f6af01dfe794b0bd1812930c7d510a1145cf227bf346538ab0547771848909cf971bec926f4031b384907ad71e67029ac6b0041a0954ad093d680006d6dd0f929cbed0f8be4231fa46e14194ff72310a321542e2981952693447e5c09a74a2ee5465bc5c316440cc2b4771d6d63e6a691901b612f5d43e5153bec0bb707e71940b222da23a46f9f99e6635209feae0b6fe306d999cd23401baf3c88a5a5545909e34637aaf9b2a402baea569727a09c016b161ae586ba7534eb49de68c0d095771d73bc610464b8f367aacc763e6d72458221db32f6cdcf614a297c628b6a745fbf50ffc85cd5aad40b99e2fc76e1f8db6f7e5c028fce879173dd2cef83f903fdf6c6c626ec40975dad3466148650b4e10212a6cb9714430c0b941a5d730394264b2b6acc6fa0306eb4058e6b8eb177f044c065b8e53a87032b8191a589abf44762415a6a30c979fe8756bc9e825973f75c32e7e09454b2144f663ae06eb497630008d5f759cbace5cb7400d7711dab2836657ee7af068f1f465050c4c0f551bd9ad710fe27d6331f4b09211fbed28750cecf09f8f07a78458c056c9b04d3459c0340b0cdbe34b126f04b9401a17c0f1f57445687dc7537b4ae8d5582e07b154e522237d16e2c1c41005f98665f85cff337ce73c78dae4945109e3964b9dee4a2018eb067e042383d575563a,0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b1440d324f3fbf8d76fbe08d1e5e2c12a5e2d1ae5e9e51c7c21403a7a609bf818fcfe181649258ab400cc84e678f5c738c787f64ccaaa543a25a1ca7934fcf313cec2407c1e222d32affa110181ae25b8f44feb37277a26c14842935a339c9a40c2208b801df3abe99c5cbeee05838274d88427a0f8cb0489f175f3944fa3f1553c746c4040edc2be4c293753e56aaf719913eab4928a20b087a18290fb451b3300f7cf6fc0b6c9b00baf410ef6f34eeb13d518dd81ddcad6d982af1c2399198d23605859409d793d301ca999afbf79fd6f9353cde476fa8668e2bf96f7f5cb4fc4914601f6f98ab27ca19ed313f1b7aeb00b9c9e003249c8394960ed47f28ccedb3eeab7da403126d6751dfb8011294c612468730b01cb5efc20de339496b38f254b77516967296c7735c6b3e9ecfc4071ef51bd8fb1e47ff1dfae150cafda17299a2cc4d5a6401b3142e029b4c2ad29fdc5495b947037e49aa4511bce388d874e3e7cffaddfb6d0d43b2aaf29fe26a1a1586ab313237ba26310e6d91cf5e66b55fcdccc2b541540d75b1b951c6a109ee2abaf693232510e543f03bae8da0b5f54f488fb97697fd3a423be6e71328ae7fef35eea9d0e5281334454af87bf7857c4ccdae7f3d629c440c3a84cc125a3fd39caeb6dbac2f6524130fc702aee9b971036cf1b2088ead7fb14a20b8dd18d6324fb989e6acff2204ffac93de45682822154da25c9d9cc58b64010d65405a271434037ab20c114ab05bdd5865770b32b982c28ceee4ee3a7c69137a62041f16974143377d2213f5d9f06d614f01bac556fa306abbce290f9c50d406f576da4a441e6535d01db9ecae2b35e15036e689051a88a37a2c8362fb539618ac646d5204debe663765ad340b31a82c47007f43c0db9a274a5784763374cf840b22182f4f51cbb0655c5e025afeb6212265d61d98bd7406c2fdb1c39e5804760b2e868b29420d85d82e8dc261d43b55790523f2beea2d548837c516ee9a49bfe4005409697d18d57895b49544163f8b0c207a309bc391c65f939a6cdfd54d74722ee5c238db4bb9a8931b739ff725bb1f1dacf5790b740d69b3aaef8e2acfd67f240e93c45b315167d34adf5c05cbc7962c0aa30403bb00db1f89298d0261358a7e803f878199725ea3e1c99406f4079c3962d39fae6f81c5fc7ce55fa346918272f406706b1a0ce505040f8766106db8e2116d6393fca43a930272842200067aee612c6e1e825142b1ae71b69de7fb4a2f03d8f06fdae9da38cdbc5e7914628e7d71c4042ba94d9b9fdd0f446cd1b04cadf8fee0daf629065faf8f34d93a8f02627962a542757d645dda13a51a2fe4ed6cb2a7b390f85a21e962bfebf4cca85c5439b774096db42139e26e07ac4314293b44a22f02bb7808e7297bd069aab7c23a85e196ed36da10bb98ecec602add55babec328d447d13acfda0e833e7044485c1ac62b440d6ad665b427f7a6c21dd6ec96203f7e3951964bbfdc5d616759979b0de5bb275c2a4a339877cff75b77b81606f0b1e0856f582993f4afdcbb00253e3daefd44040c0cc65fbd4de921a810891b3dbcea7317b20d9c7413b8f7541b5fe3a90870032dfd757cf34362e69520f89d9010e40f8130836cdc799436ff12b4be218b303b34005c531f73b99c09322c5d3dc0a91a5e0086860ee6bb42dea746ddb6029efc9a34c1fafb4eb2e3734d8e6d3dcb6b7008abe387fbcb4682e6b3dfb8c442a6ef59f40d462ffbe484112e16679fe602cd6d63a1de9ccef48070a07fa8ffa5860cc191e6c4975ff99e78b4a84a8df89a3fa4e24ecf1ff5ec1b01b5e23ebd1e2eef3dd23,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21144060a29a91893e922a2d68b4371d71a5b51fdc743364b45317194e217440883a874c3bef33038d65cb977398db94f4c2781b7c3398f1cff1ab3fd01697fb0728fa4034779cb1e4fa5a670abdd68901f44d22f87bd4b49b993d7b51934f5d8568c6e135ef8399adefc757e1dbd6f9e0ef87ed96761eb19c520c0d24bb606c1f3010c6404494f1ea7375c04c42ad2c9a9abed773d0fdd868c2013b97b1597fde3a882161fe4f34a9959dc19df6c8ac534a06e0542e8a4af57f7e96b914070f41309ed77840eaf77ae21a62f117fe1e2300a24fe79b6d26590edb2faf599f271b833f7a27d873e0903989077a2600c6cdcf0a788950ae947995e9762da8763ebaa46b89246040af9a7d1780666b3418995dfdf6fb9478e7f89093c90806703f01e31576538f25e68dba1a996676962366915bf5c40a276f4e6f1a42219de261c9ac62cc9b9882403927ce32e49640493bb1d15bc13ff7b6f52f6da68e52a3a12664c93063396659c66184035290ed947c7ad731d09a96f52ce07738d6b1d3d334f8287f94f8f05e4014ffcb85c7bad63759ea2fef56846ae7f64d7c24598bb955da6affd48ff595c712bbd4961d8df9a9158feee8af1d72098afb7a867a1c6eab4cd8ccb8eb5163c640533556fc942e4ca22c69ed7eaf0c49aa3f6c84dab0ace1548c80ef9561f80c8dddf4537ad3fa5656a4adcba9f6228163762885ae7bb9d583afeb742c9fb74a7040fbe5cf67b028c757d09be657de337ce0c67e60003f86d1f94725cf427f5ce0ed1b1cda77661c77f06f771904be4212d6a1e6b78f2d83b786c0c5425487462172404b18dad771219ac3b5b4b1719d26be8261219a1d1c8c97c2ee672861d78b9e66445271239791ad30ba8077922c8de423911f88cbf63aa82ae85044855d83978740fb332393e23e289be1f4a6d0d53418950a3ed9cab41e61b65d59fc1238d0cb95b271d529e584ca3f272956921303abf9824d8bb20fb366a238c64e28efa6046c40377dd7712c85e59e555563a369343484add02257ca7032ad744f89abe8dc13b4d45de9f0c2e21270b55fcc18121b0952f946b17c2099f8ce0e348eef388221a340d7ea6941fd190af04a15ecf0fc7f4316c63317b85ff34e2583377523fe0c937b6d49b2132bbcaaa88152b497c9d59f7a1e184d70a5bdb8ba8f87997ad5e46e134011d7a93ad914ab4392255a2aacdfe7c7f7cd28a918d31a7ba120a085cb17c62b53409d5e8cd6ce46a07fc847b837bbc8a1ab92594eb2af85435ead490f72a410401e444386949196d8ea9a55262d1d535976c8f3b9a25f4c1355689656714a759c3a04ea790971af49b2103ecf74d12355a31fa9d81d6a5a8fa49b8aff969cb1d040907bb8a1e72f5d47579f015ccab62f211cc67597b673738ae86d784e6b4c8f6af81d2b8f1f31201b0a3fd34e729a46a41c4e1eb18f89750dbc4eee5c8e61d6184073c299028dfb9f119d93b90dac28263c9a88261d2816ca12d8a01a75782f5dec417d9dd864668eb69945052f0664fe3b5d2637c3de6e41d2a35a183ab6d2c75740fbb3f384e3a65de0545f3fcde0c5a83a82c4e263738178677239a0a8180c077e12d60f2fd65bb9562f34f3f1e82d6f420c7f3225db901ee46f3acaa928cce91f40e5a05da23c64ee0e3ebaa09b7b6daefc9a239c6fc6ab63ec5c194122c175063f5e27238e4a9d2af0442500c28ddd238b7681f2b461d0219d6af94281f0d11a35407d96a12c6ccf6ba857dd540a8e916612d88af0b77420926c3f1d2dbeba278d40e6ae57f8022f313bd4bd9cdc0e4636bbddd11906b75565fbe53ee52154b719b1,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b91440ebae91f0fe922214b61941f7863725b35eefcb445a749fbd2766b7d068ffb65e47e1d2c80945230bcf8c543082237ad197d6357cb0852aa6cba850c27c4982584001aab8008d452707b04ea48d63a4eceb21adb3b67e56b14e75dc207c712feb99b3a95be9edc2547541b911646291989dd8d3aacbe3ab70adc9cf3d98a3bffe4a4070513aee6fc32607dc389813937dc90a9e94506dfa81c2d9fe3498821857dfd77be3673db579467776487520e14e431e8dfd46412062c820ff0a92cdbaef302e4024051324c7b83e42126f62115183a0cbdd66aa1f8357ea59d6423f971bbd7771abeb9db91c156789a7005c3cadb70a3e34dca2d89f5ef3c82c3a46b3f38017b840e638c97b8127c94171b1e6853939c64589a09ff059640258d83881de34698721a7335500b391822e2627bdbd2aeb326170914842c0305c7015284f280eafa3a040934e17102247a7a6e48dd549cc3557603c92bf5ecef8b87bcb55c3f80dde37087b690ca1c2a5a8774db9231ba1a12e7834c0281cac8b5ff6c6b45f162dedc3c840f759953a3de44f41202d9cb018c3233decaa2319da70553cfcfa80d989778bf7add07d39f018052af6e64af2efc97c23633aa9e810b1332588c4aac08438838140c9e8e51114ad544e5df27f808c2e8ac873caec3865980ce6adc59b2ed00f9b36fcadd5ed38826e2713aee90780c3864323496feee4f04d7813ac05aaa1cdc461407a4494f880769b9103b0b249f7886eb7ab2c648143fb823e81c728d9ff6350a8ecb26696c032d8977f457d5c3faaaedb0b728688fec3b8e8bb99180e0bb428a7408ff5873996dfc2218482dea420cf00da5ba4a0f7f8c3b82b6fc43ee58c8f2a0ed57877ab0203cfcbc7ac7cda31e68630c3ff570ac750362ab66fd039228a3efd40dde759d12178e3654b33f66d647ce791be5956aae3a88f83785404de896654b235d76047cea48694e23d6be2667b5813680e2b72991f6a9f2da372426eec4958403bbce359945b4d40ff488d5dfa6ac5530367f991a4aa1ef04b35eee8c7bbc699fc746db7144939737e70be402d741019e17af4474c9f74420f31a44474308dcb409ab128e79a929bf3a5bef505c1b01185e2dfcc9a4a85306d2f008d263c811720016818bf75c220ac81ad0d6e37fa7ed47b81a236b1094ec08b46c36afb4a3568401bdcfcc7f015c582e2fdc56ace9c09bdd2f0a014794cd4a51876106624d7215e28bada96c9413ea37d38b522ac5c0484ed5f307ed8acfefb014700cf3f53260d403703be940943b8f0f52703a59c8e53f6ef8fb23e6c59e3d51db4cf7aafd1b53c547e65e1c4908d907d3b1f8cfc1293f5f67bda1691c0bdd336930d24292744ae40c74f03bfa10935d8f2c11320b6664473b9ca7f1ea86faf3ecf8e886dd3b7a98893651d2f9dcc9142ca98332e923082695312c3b0f08f9182226dc15dccd3a2e3405239096f3af63b8a18edc0e12cf10bfc4eb954efd6bde47eddf83868c5f5806e8d6944bbe773101038bad96342529fa19496aedb1d89d88c6c0822791b8eec0a40d00fce2a8babc794b6cb5797e18e9ab108cd54b8c5b869710e44ef8e6822eb9b44b512283df8e4c12471ca3252fd957e4c36408acdd735f2e9313446e368475940442a32a65230ed4d9b7d7bb131fc74e2fdca7389b798e49bbc64732620a25f7e5d8d9ba71a1b1867b1bb9935fc57fe5feab64209e52a074e6923cf14990fa0f140aa18aa0dbbbe77e3260f4c6c9f859d4cdbeef16a0f1a96970e64bf72140b6fabae86d79910b5b836484d144df994188bd9d65ae2c4bc7122c15a439ae12bda0d,03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c91440a2283f4ccea9199aaa45f7d00e07f74ca7f367ade2fd4eaefb2741cb2b166f458b0f556be1785d8cbec9db840973c3d13a4c3214cf20ddd805d73b69951ae254409e51db9efca06ccf2f5092a2f8aaf0c31f8c80f4a91613623fd9878957c199738114b794bf12f099b96b47a020d9693498c3e6f07d3d8ff7cb460d0edf4cc89e403b712aff711a68eb80835d3680c6f9e4013fbc2bfa6045ffbd60bd13344c08b01126d340989e824b2ff79e1d31f3f540a471b34a2ce53aafa297174ee144bd9c40cc203e2122f3fe96f972b146796a9358647b31b24427cfa057b44fd54b3080461d18fa09d1b3cadb8ee492dce599848b51374ba2620c7ad79f9748b10ba9df1040570fd06befcba4f676dc4e91dfe0c419de158390ed6552f4457f05206ff0473f56aaeec2c36334da423545f49b17be4192740902706d6f58f996c0ef7c4b575e40f5101edb18d039e97914fc79f8731ee3019c8fedd6182fa7c7ac1d153d9068e29a89159d5d164b327939ceca45f9c4348915778fb18639473eafb4a0003d2e1e40850a55eb3e5b6c33b1d83c90231a1a017d7bd142101de43c18548474742f61517d13365022a658298a279ba2b9f62a1b7ad25fe59608c47b12daa2f4b3d0006f40c85c3dc5927bfadb472dfd4308b36a9d3093b106c0d0a21d7cb66febb1c930b4cc94f9b4a390e639e74181f2c07477c80b386bdd78cc6ae7f4eebf13ab79974540c256d1b355190d5dfdf3225b3e95f2bc28a803953c0bf0c83698192a7fdf372bc9a29010ac095be5c51849c4d16f4820f6a06f36f0e3fd31c61af1dc6fd719684063c3543e48ce971deb369e603c73bc6b3185085404e48d74cd7435d937a9b0b2d78702c8c5e4534e191664b81f62e3bbbbaa7c3fd730b42c6dbb6658f039977240e11a3f6bc9e19f40270157de4a782c3a7a162f183eb04bf441b65aac2f966ffbf28325a9a66e41d8632084b8c28a4b9ca8963cede6e0594b51bebf1d63e7d7744017dc081549ed7e414f71bf052f720a5e73847cb1bd64937721a6a37264c3273625fc85c395c3c5a3b2f3981e34f1c931bfbea4b4f48681c3f78dfb27e49b6aff405d469a99042b209a37afe7f0887d65d4c21aab868db745d3cccabab5b16c0d18af1c53a152423be928b23f71fb0e93410f8268a87d44243ad95b4b14130bb4954051355f28478be25e40964489f85df0ba9898d799585cf939413d6d72f613c2001161fe5d9d9428920b68e990195bcd9f339d5156856dfccbe329bda919db8b824048c947a8386fd022e52d540094c5f914efd417e18933e84705d56fedbbb6098102f19852c37101c994a649d89a5b3e335655f64d6177ef5fe94b6201f59beada40a796142fd0fd772a85da30757e677c176a96b67bc1e20f2cb63b324aee73014f6b04d1dc0e14037ab06a1df3f2101134f8ce9c3e3c6fa0ab3a7141a4bc100187407511033be32f1e4c6f4b00e2981d36c402e67b8a1cd1f8159ccaa569cf8af0de7bcc0fbf5038cb15254cbaf986c0f2edafbdde1cf05655f786b59728cf87906c4022f9a4e5d049397658aada8c17d6b2d102bd12d4c92b8af5caf7239cbf83df9d61050e9de11d828fa8bddc1e7c80012ad7b3dfb4aa3661427765e0e7dcd17dc640d6d1498a91143947d26020e6f17624cc071eb3dc0dcc0eaace6b1da3e252caf7619bdb32ac3d7bf8c5232dc145b293bac1c26ca426e6e25b223f5a70a15506264086cfa58793f8ea2cf3ce69f3cf84202d1fca13cf0fefd8a41c0a36aeb9f7a87993ca278a4513b409fa8eea68f5f2663bb9a3844773ca98e34de5a086bd847907,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b80314403081a80b44872191ecf02cbba1116e097a004e0dabb58f9c60ea009be6857d938810b1fa33055c1db78727e03cd48eefcd6e8cc908544a424c4ac2c046382a074001f13091184d66aa4b12d8d1f77d8bcb1699f816002e028a5e5abdb3b15b981cda9caf4cf51d34d03ff99c976afeced5a30d73785affd4f12942fe2eb2fa7acb404c173ad57ec71c15e97010673f4eb024c77e56e7f0113a423264b36a568d5ac7da8fcc81c7218f62a36bf26586687f79e3a64c3129eaadcccb3048f3962b1095405d6a455a532b0c1c1feebbc27ea502f37774b4b255db4820956afdefa268229df0d2af6493ac9a1fdf02a6699b9774604e639f878ae137b692e6a021669f147a408ce1419500b41c827825bfed956a0c2dc91b409c40a5f7780c57e2b7127a4957de26e2f47545f9f2638c5b2157a198fffd64a762abbbf1859122f011187d6222407f75a8147e29ac70518f5ebd85c8b16b4ed664c4264bbf194121c61582d6ef1f54826997ed0b012ff13c9eb28ca940dd69c123ef3c6f752f4fd877f79aee3eae4052c64ca68f50f57b8001f51c2b1b486722477afda4798918740d0a0dbe934b4d78ccb2cd650278a6f4abf171c6c76fb561a5f4916eef487bb48680d5d1e628fe40be98812db8eb11f199193b598870d1788f1023c71c9a37aa52c37aac37250ba2faf3451cfd595798b07cabaf90e33f8829a58a58df0630277bb34fffde94799b40dc180b200bc2037ace95dfd87030cf01b3d4f2fdf31fe24c681f0e211b8ec3546e75e200a7a8f4f933e9772ec43f52cecda5701f10f5f791f34b1a721777a5b0400a3cd554f5c4a384585c0c58b16af2dc034665119f93d1af2cf8c0059071a886fd14c0aaa1a110e58fae00e008f7eaa510eb97a5f54a103ed9f6a14e4c1c401840f531b7e4e000e007433cd793f60302f178d42e8920e7d619e4abfea1c9200c720bcaa5a94fcfaf7dfa4f278439c36c4eaa85f571165fda44aa7a91e9302ac7cc4020de4ad37b05d7f091098e5acc8c15d189c4bfba47507ad8dc87bd8c2baaa0dc20ba2fa3675f28e7513cd6436b690f1460aa83d2d1e7d8a548728574449dbcdd40d29a146ee44983fcf8a49485230500096f7127dc6a2965a9c15bf91dd6ee1695aba9fecb1d572532b3a736578aefe6f916ee0ab1eb9b88464c10faf628ceb63640928d6bb2cee58e51363bb51793b501137f2d9e271bd50892856ce80e55c0ad1b62f6076e5175b486273a4d3fa4a3c127cb12243f525661a8b389ae56acdeac214096990dd2d8503d5fdd42ab3035e4435c3079bf9123827b6c8aa578dc55bc9d870b2242080d44362cf5b798e08cc9e1c62cb6752bef3ab626c31f9772427bffba40ea384a8b541e16c583ccaf7fd6a65fb0a8d58c15e4a54995bf17c95cac615599dbb60b81bc321439d6b5bc454154c41aea8655f435bd473bfbfc6bde116302f4404060aaaf9331ad3ec520c9edd836b93d129c9610ef751ec66bf07c5046c421ada7bc260f3ee4524cc539d62a05ef59eb73f10b2fa00df747ec931f88ae4d56e5403dab4eeff3610e52879919478615a31ea5c1f55dba5780aef59bec691b4c1bf7a5dc04b2c55c41ef42c56516dae4d10dfeb8f24bfbeb614d6c1b96191ce3bb5040f8af3a9db6ea0dc5e572dff46e5c6d8232906b53091dee97bd5b91a63a06d057d78c8ae6863c1750a89cee45d00ab7796a66db3ed10a25e17ab160fb8bcf0488407fdb3c33ec09c5c3b3ae8f5201c4d9fcdc644b97862f1ed9107b090c933529126908a20701961690fb35683f80d7135a7c3b432c0bb8f62b2ee58103f2e8a4e0,02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049144048ceec50fff2c5626829eea30ca6e9794ed7cd3512a15f9ecd0e0ade6843a382bb396f83e832813a0cdbc56b47e65e123cd95c7094601e956458099eff6b60cb40ca2a2cdff21fd3d3462ee1050283f06459da62a5c038a89e5273225d57b246e83f3377caf3e7c40796873ee9135ed570556afe1a64f3f93053515f9785d80a68403c64287d06603fa9cf4ec3656f959bdb93b32a67e5ddb65e9480462cf7d24a9d9d644883292aebf423cf6d4c53d3dc2b351959a1762f5e367218c460e9e2fd9f40b4129cf7cb4f723df5213b83afda5b705f747c85ef174161d25e7617428484688ffa3c8f782dbcf6bbacc09415738dcd32a8a738678232a6e09035d7da83f73240e1ce30a3f1508c2bc4f444acd8ad81ecf5703fe153623b9261b48abb30babaf67b47a36a4aa50b28c05a7297b133b9dfc41047e666a5f872c25cb2bfd51ae4c640b46771a8236c1e7d72999361dbd6d2c6dccc1077a201574dd650c57d9f8877091950e802c47844282f9638d84094670042df64a0fd43f19f4f698ed341161d3e40049c2986dd8aa640ec0cbbee5b04055f25d4fada07ae173dd71421dfde6e8f795d668f7c82d3a1aafe56bd9a116dd4632919267e74297f9139bebfab95212d6a407e2807ea69e83cc9a70c6fc7093bd54f9019b740dfa26ea69755ea45c03dde0c1d06beeb09d56590c939235ba8fb4bab7a44ce68e8571d3b7f7dd27e6e15d339403484c8c54abc9637eb18155ea5caa84ac413b3f61624614886ec2bfd7d87a1df759e04b61831a639a24500d03bff90faa45a4a3d29a2d2ee9784bec0640dd8324077b89b1bc691a6dccf6a7d804c7c6284ee943de0e50ecbe6410e55a7df4537d8164477423951267c51552b2f21a61162718c96beb072794f0d2fe3053e67712d40137c579ba5bed55366312f0f12027ea1a1faacf14d1e131aa8a7203fbb1eb467d672a0ee39ef3eb3422b137684a177407ae401f8acaf1640b1b84783ded2254c4032c360d72379e24d60900181e08b9174be45ff91102288b97cf495127b04f6cdcd63b25dc5ab904343921d4a171faef0d631a01035674a0877a1e3b9543241b540056a5c62993ebcf08cc1428010eb3c9770290216448edfd4165ac589131b49fd60d9e0fe9262993f9dde976cbc0cdd82cb6f2278c5d54325606c1e3b6827c616408e437eb7b5cb4f921907c811fb0c8f3e202623ade96b91e61c3f80076946cdcd4c704206f623ae9d3d4eec9f970be009083a056f9d677aa453a27dfd05dae3f540988f00bc114b62addf765b150277a278ca34fbbb520626ddb86942321454307a08843198bb01aa3a8878f40875318d8f453872430b9fcb571da0f055cf16953440e5e5aee6dc9943342a9dad2837d1bd8711125eee26513eb0fc2a15cd61b90cc4bffd7c113ab21b11000529a9b4dbfc68cb04f4cf4c80fce9a4555e11f49d05ca406d275354faf2fe6dd5d852cfa92ae54e45ca9d1500dde1f81d773053cdc7fe5597b69ed186e40530f1f5c46819c8279921abef83d2ca8137c0f24001ec1ee9cf402c39cd9996b7df5f94ccbf73790cc1a39a500e0be48705b1e273a13489a7a1bec84a5ae07c954b5e02638222ca7fcced6275a42ccf78a06b6438e7908cb83e5e40bdba3b309b2006b6557fe7ca5e3b010817aeec85b24ff62f1f0c590a40f255e814366d585bbc77cc81ab1cd4701061c03d8eef8cc8388407c78d386e17e8ba5a4011b11cf012702262df1c1e7d2c55e4c374e4468c583d2f2b6647d84c928a05bce6441b15072c353cea76528c220fe64320c0e7abcb3bee1ce3b7c88c2d69a8ed,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f01440db40569ebc90c025dba001bbf143fcdff43436c608109fba086ed464a178f371fd7579c7f6eb8f3cac026101706c10c2cd34759fc4eb50a173665ea601fec14b40d65be6b581e345a983a83e0bffbf70a3903f6b249bd4f80a7514a5a35888edfd8ce80a74b9c2f8ca9d56f14a2edb20ac75d9c45d35a2055e7a2899353934bf4a40b48736e28d865f4a494c11009ddc49c8cb8cd0a4ff1e01f72d33f95f8a0fcef79b76fd187602332e3614e0a8d7a1697ce70be58c338729b2bbd94dccc0e4ae60403f14d81c617bd33b4af006dd5a276a6e6b3adfffea035e10dfa77e3cf445b083215c385b8e3b6cebb67518726aeea54c8db193ca9ecd5cb1bc1699816ed515d8406381d79637fd4a354f529a875f430945409fdff4e26f2dc053a7d9cf8225ef6b1e8d271b45ded9e51850c4e1fcf8144ad219f8459c2205e3267981aeaf222b8c404bbcccfc2cc4475c9d3729f75c77f4a7be899e30d0cd2fafa5a8630a02e39a79feb611077e66dffe40ae675e1152c846de351f8f54729ff3c5c21d30e0b95e794079f566952c523cf47a2404623c3ec83c359c2c314bc00efb25e66ee30da5333edd08bbb171d720c68c3ba233aa213d4cb0cf87d29d2fa2f972612f68b413df5c401b9e763aaf9d1b9ed5b0e81b19a884e40183506b2b733ccfd00df517baedf8bba6c4420e0a4eb6e8f922ea0b71b680104750c647931382a7568a149589c0c4944040602f1fda385b79a4f694adb6bff15c597b6376222ab14f60d8b7bad8ec697d02e0d8ad0ecfa69cf910158de3146f49328b8cbffabe13d052b8fe26c8d2814540803e73532c4248698fee2ff5117dcfbefca8b07edb239e8a4e63104d73cebb4f5fba3d75f70777e35bc4fdc02da3da8bae117bdf3cad474bbdd824457c200fcd40e850dcdeb3d0ca41080a4e5f87f24e94b014749bcc11acbf359b4c4113a38a4ef91c3405d0ca56da099d96994c986057f3efbb6a4acb345eea397815294de4d640343ebd6c3bf38371e2f04f2eddb52b9933c900f8ab16ae2ad121f42eb8b400dcdc7bd48a5b2a14da7113694ea0a4cb5a60aa2e13a30520c36cdeb6c60274604940b33285543b7df1bfeb2113daa36ffb551e8058ca973907a123e8d71ffde43467526eb88ad10bbeb62654f5330cf42dcbfb3364d932e7b9a0739c977d0a943957403e4145f2bbdbb678973c4233a6d99c08de4ce8ce70b5436dcd8093e5b8a02c4949340aefc68e4762c95073f80058aba54555f8175508c46a3610e7afe88e0046408d116d503aa351c5899a92d485165079fb8b415e604a80067034acf61377a8c4b8055aeacdf609fa71d001bac9d1b7b6cc6e9c2355820a5bf7024f512472e7b8407c2fb9ec247b4ae82baf2965143a88b737e499a7400db9e1ed6034e684691a66fa7cd296b4d5a673d9554543475ac3e39f213aa8823dfa77a823a0b1cc3e476e40f8ebb0666743ed6be28465af852188bb6fe08c54bc123aa2f604e56c155308ec8069661b8bb05eb579d30cf569145f48e8813621bc399fa0edbea9c633bee42b40c873221603a6d0209b2ea3ca9af2e127fa8072d697bb1612d5bc92fc4e965f390110db4d95d4f2b7b5a10d83366ce403c9f935926876c22ed59480b177ad4b65405ae24d6e3f472cecffc5c4a6a3ff492b14ca205955219f4760ef9302585c41473f746d1f22b98fb396dc6843d9f9565b5abdc236ea902340d0ea141a841c9f01401253a00e5fd47221aef2082994579421253665cd5887e2449b64d600602403ed62e8bc8f438b9db66f3373e773cf4014524cd6797e5792a211672c3219267463,02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a0314405eb12b48652a029040631c2ff2eee0c671bff9fc7cfeb8328e8decbf5707d408d6d0bc27d9b7a5075069c484139b7fe097a6c5206ff2e358d9973ce59bc89cc9402e259c347f06b1fccf631edd1d2da7ecd6229710f4ff7431f223918d5810a6bd6331a2c51613508c287a96856c1da7ed5bf34242b7cf3f95cf1e8e362f1724f8403084fd9f9bc2c1077db0c7ac3c7f80090a5e229e4099821f341dbbe09213c816ff55229bf4964a1c57543e573009cda695fb3cff819f8e3372b949ff8739d78540aefdfeac88492d54d2835cceaeb4e487b139579e05b54a9139e1148e28f43eb5f9efc34038bca685baf691409e18d1d2a87b3b2eed359245f8baec8507c3640340e79eb1c95a7c44725876e14ae3320813e3659d2d3360231cea363b842943ccbf48c3f9489b3c165ac92bbe151e1daf94ed92ab14dd2018859b030c054a07c5d440740dd5cac0ab5b0dc9ec2b64d1607547b5b3ab613e9e323e80d5e96b894bd6c0ac1dc3d941938f90ab4f56d265389e6da1f2b301dfa1a13cb6bf562832719924409b3259163969dc4982355786f3f821705348a4a770998ea42d53b94073c7a08026a81ec22e49fdb254e165a0b856f1fa520809056a0d37c1d064f2ef815e30ae40b470da6797b14947b1c5bdd26fede61f4fdf37d0c76585b8c9b4924e296c14459f74eedf4fc60fa468cd2031c12ddc78cc76054debc325f6e86a2885fe0b61a3405c1fbf2fb78e16b4e3f8739f793e6110649472fa64b19944cd854a3f7fa736a6917a92490a54e6a452de4ab7fccc0ab53bcbe62bd4f41d6478f521c4eefdb47340083c91903e3f84f054829040065e02f04d3f759d033dc6269dba291a2b18792f0b4f4d1ec247b4bd96c9626785dbf619aeb0300b60479933beebb58a9586dc3d40dbf3ee0b08643df78b5a66e45d93df5d059cd06d1c8343619014fa0161e6aaba7b19bfe9f00cc4d94b0daec148910199cc6c2dcb027fba26741372061828ce684091693674c3bcd648dc5f0eb031c75c88de3ef676bbaa6e3eff2a77da5efa77afcd6c2c4a8ba5d02a8fe4dfb0641eb026e1655270b6453f6022b55b3ba985882040711f9f7bb1605c48433ab6bc6910ab40fc3c480be6b6fd5384ab14919c692ab91fd91dc669de68e4ff0eb5a158a9c68135fdc196dce7e853a002ccb76d692e304011b1d802c90fec6ff0b7a6e4055ab2d915aa589daa84f7b763d418e0cab7e2ee3f6b1da58dbe9b612c87c9f6652a5a139e9f48d5888f33faade67096598ba41540c4fa10e0defb564cd5812ea0aeefafc386d56710bbd24621eba992a67cc58b0903c658d31506c9ea0920cede4b415e6656f8e64d4cddfd49894edaf589e7994740e7e88461cb50a109281f64c64285169cbe379f1778da51e7c178f76be9cec4f05ad96137857fc06b8dabebeddd23f1a50e580db3a0c830afa14d0d54c14d9fe8404c661714fd16807d805bd6bdb691fdef063169af720b37de599306ee7c0f735de7365ffc9e49cad6a6490731ee8b69cc7a9cdc207246f6b6c06213f78026286a40fdf848c84306762a839f9f6964bb04172d9b2374d12978b9796c2a44fa9ce85576bace31fabdce2fd1151e2219ec3db4d3ae84f76bc638a75a0733fffc7b9fd04030a82ac20c5b0e821446a91ce1dbc5985c6f2a6071e4849dc386c406783f5ee436b728c49bff66b462b3eda0e5c378c421a4a328f1101082cf7cdf111bb73bd1404c658b905e9cb65acaa35ecc3efdbead2201b394ae75233af42c44416d804052a9d6d6c7fe6da6a72238c7eac159e6db2dff2a048294807a879240c6ffb1bd11,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d14403c7f752613de2fbfe7d1860d0e296b763b2fecdaed94170a7ea338ef1821963ae33fcdade546de7fbd5903ae7b01d59f72d57b07ea6c10ddcf7941374b210f704021f932d90f66a043f777bcb6b6d5f253e02956762e7c2029056cb483a9354cc6c9cfb91ae090047cc3cef0891be45202b731f7fc0e4136f304977b1f3b13394a40e934a19bd3a14bce0d300d3480fa8b4507454187223d61f2ad98e9d447d5dbd0c9cb3128f826e926ee81a2eee30f626525d4907a8b64abefc15d4320dc7a2f3f40479adf58ed7cf2cceda55cbb728de39c9ed02e87c5bc7ab727fa140f81234a017b414ff7e9434ca2609e1ac1c7d96c7cfb19f7613b5bafcf40a0fe1c8ee663d9403801a4998b387e2bb90d380fb4bcd9db04433f9ee32cdd21bddbd75591ec596239da5e5d39274582cb10ba3eade6899f75aaba9d190fbac3dddf85f53bbb48e44056c6dd8a7d3beb26229cfd75b8fcc426d21a039ab009077371192ef160d785912274a1a2d605e56dcd7059646a4bc3d74c957d1ebda5124abb2f16e8c1d93b16407351f55151cd2b717749f9280b5dd1b3b0073811c86739dfe263ae4acb44757410b44b42761a2c3608e7c28599662ba5b9c6cc87a1e67c2e7915a293d988ef7240bbb7d9f1fb4496ad59bdb9194d8e782d72f443b76c2677fcf1d1bdddd2d1b98841a768fce73b8f01cee1ddec71a4845ff917c0c5135314c7bb7184a0759de49b401caae6c320768daf1bd187dacaae5956e595596f39f018555b1c481d9e32e954422f7a9f0c5b615ad39b218e17c989ab7c2ac54f0873882649826e04a3e4a034405763275a4e3bc98ae525e6d4a15f18e5761a501f8e7b7f10ce5de76e4a15a21bab5a0ffe1fd8f66c12269a0d76ef3775862bb291d646bd55ca9600cb68bc77e540236f0f14d883b92ba721a771fc7c3cd2cbbdde57170a22f3bad10aa7e8f0987a7c62bff0960047fc046fe2afb041b59519eacebe295b5877d7596d2ccb8566144014ad408842bd1758ad0f9a076f91bc2375c6d7432d889cb3403177329ef96fc7060515792b720b3442a083d5fdeb7bc2eb8fef272a6944654ab1313b430abbc84056f37bb676e27bf6403eaafbab9004d9f704ed14c979fb885a6b64e54b550b254f8dbf215f1e591bb6b3a179dd50f0110c44b3753273ecb758a642c4135e17854079fea3b86630381a8486909512733edf52e468edf84984771b6222c53fb9b63abd635c67e9e413e879a2c160c52769806d023d45d73b1ee1c7c9f7dd283624fd409d9afc4d1f54268006b0b4bb120751151155c2dca611919581599adee459c585687116899b41f25a5bb84510c6fb6d6d27c8bfd29f27994b3edbb1cea920613a4048009ea4f501407a44fffd401d07d2d3063ec2a67a2a2f5334fcc1d8cf0fc3b02a213882e81eccce51bc284d576806af9a9abb3ff184c0273a84d040b5124489404d08e31f87f48b5d941b3f3419b2dfacee0431a8a67e75eb5fed60c450dab88b7c5721e5245f100b84e431b645bfdcb6481a543adfc5f3ae7bcd8c5b192a0d5b40d417b67a21dd6dd28b3deaba803a62dd62b8e33d628925b0d913ec3a559083db88e211030b13bbce60f522725e904b430c8c864246923a6f5ec011a765b37d89405ac93eebb40d00c0a97cb2c438aab189765d0f642c43fad2f6138309b4b25ec64f979fbff5dccf07f8cd6d64e828044f521c1f359b91b99cbdc52421868ff3c240a75818dbe7a0e8e06489da66517aab3b3689434d53c048214d3f131289befa81ce732360cf72b99ec06dd899483ae1e467320014ed159a01b455f0625ec91ecd,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df41440d4bd937d649906acfee902f0353abf242ab3020193373fc08e2b8d2ca81c2b4df0e436a31a54f411d2f1ba0ae4314ca0138545a4265d7ceb1d14899e2b6d7bcc4024e9dc597767a720e85e8684a3ac825862ef376ecf75dd12ae798ec04b875d3bfd35ccb03ac064278c9a260b3cfa23ffa84e01f65424a1915695dea3dd20bd9940a27c5d2fa1713c804dc91e12491519031811345e8a76a9db84ccb1c37039f05cd673d5fadc4b67b7a32e2d7eaf29d96877a3c0fb8382917e2b3ba98920c0ffe34091a0287aa14a4f899b8969d59e0285a069be4c83cc2eade2aab99a9ed67767f5b9d275b8693f18250cff59fbc0d0c688d2cc885c8bdc3dab204cfe02fde481664001b90cbbed8ef49278688267294556fd38c1588966e0ae41fb765becac5141ef3b6bbb23902a4bb07f98c2f7a6d9bc6366a7d5f6868ca23366854dc754f985c74008a9f828c80d755626657f62885ed4c868661acfc3d1e313035bbb491fb2a5c84a239b9c85655245e460f99af3ea6c66da2aea554574ad75b75419efc1b4946f405b0bcaa1538ea5f9c8b000190062af71fcca6a071ec8fc27fa6121ed98e9b685d89e992a5b932a1ba74857f1530e74a3547b4e1e3872008704910570a89dd9134011dfe5a58711c58c431f5976bc0e78fb0fd4830453c58c6ebc05a74e3c0195e6c9c03eb7e518979d454e21e054f550d75e6d326614fb94fd644ad050691fe9224049d1ca1ec40c646bc47eb0af9b26ee1411e9d64ffd1fe870e4e11aa22f0bdb3d0d5f26cfb93b6c20a5cb8aeae1da7a446decb8dbc518243e27df94a5a40d5eb24008a6d243f9c41f8ce060c034254c6b9eed1a88e5881480d33a4f13919f902f3104eb733c4c47b220da46cd765d4bf663c5c00d5db373d29401e1a6c269c386644017e457983edb6e67115e30af399195a7bd85827fe12b10685ef9c9195a1016e1e98a7eb767603376aa9514e8ec0753580e7962ef334c012592caaec2ca888a5e40f13a214e0ceb836ce224e575342f6dd9f8e60da9322c9f646a959b432c794b9ba38542d54c30dbe1a1200e6b7fb3901cd3e4cd113c570438a9622c765c0e8f87405a3537e909a27cc3e219252b8174926552bbce594d5c7f4d8270a3b79bbf81da08eb38e46c63ac55ef875defc69be16fbde8923b198782c6ef7509e1bb2a7a65402bb91970ce5840b1ecd9b93f14a6af1828f4aaf2b28940dcfebb99c5adab1033e0a16671c7878b6b7fbcf624da02d5b18d9cfbea710f987fcd08838e998602e5407cef1925deaddfee4508707b37b64855573430a2dc49dcb01c095e88cfd9d8659ed2516163a3607d688da23d1e6d769fed7391407eef620b94c439ee3e262fbe40ae4d33492e6ee90965d3f204741d6bc7cfc216dc7bc2f28ce8db6e0a34722c2a111e02a00be66558c61de3a3b0c1bb70a547734c49550f9104ed6fca2f3e25ac400b6fc0f677252282d76942c099c022fea1c2ca00fd0e4c55261e24fbfc8d3c80bf011a35b4e70eedf8590bd4ae9750814d3c21b2f98b69ab6efbc1e16bb97a4b407db92bff37d56789091b10df77e2eec08b2258eb4be62dfa8a848d91d2bfbc469ff0960eb1874ccf4c910070f4503965f6ebb1da21c52ec2c4095760950c578640166ededa81d6cc145549ae7a970747c39f982d106696bb9fe899d4ba840db3190019cdee01c27e0e7ce88d03b69b318b48e391a9701c4c6f181b3789b6a95b5f40fa2dfb76496c15ddb637140371f73eef76e5ea21bc92811c718296a360315d7fae433ec0c7543558dcb5e54d539a0ba787d13f15ca85349fb212b7c5c1ca79ce";
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
        String utxoJson = "[\n" +
                "        {\n" +
                "            \"txid\": \"470e6b05421a54a2b518c6eee900757c5e773b9677386a7034d69de3460ffe71\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"80c64c29d5d9147b6e5036adc8422da92ae5849e0ba29f442fce369a4534cc0d\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"83770c5cefc9f201a7ce419c9d2d7d62b27595daac29a270e1602c8ad804a2af\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"df9c0d9d1109a95d2cb47ebe3a7205e8ee8d1dc96b273331eaae682bce31b569\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"627a27196311dc0a1c8d1b7627a442668537f06294a36d69f4ebcf28749615f3\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 10000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"84804f6b5e3c30f96725ec5a1b1d0325eb5090d092f86dfb7fa5e71b694b1874\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 10000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"140e6ada7b42069308dff1387e556f4437b3cf8530c1e81ae7086f3f01b1541d\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 20000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"3a295ebc27f7c0b5ff9720fa6bda22dd62027015aaf37924cc6fc7e59362faa7\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 20000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"e52c3749be4737b8a3f430971b43cc844e2c2158af0370b540a081e433bd9f0c\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 91959216\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"dc9051ec6af3a29c9fa3fe66c2b313926c96e9fd918947a355a3110e3291b89e\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 100000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"3efbf980e44f1acc25977e87acb4d857d8127c3391157d7838ac0a3d704e5d05\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"c1ee70df4a28c9d25717e32549e6420183c1c092ac2410da3830e1c09eb95d62\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"c37feaaf7757abf08397a003cad0c012cebcfc7c91634b569be51fd6310a2fcd\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 1000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"236c0267f302e9926e2bb143d346e8b54399f4082e0e05b3dd3f6de6645421f6\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 2000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"3d3db84bd77893a0b885c4ef72746c09146cb734322fdb2c0d2b804b40aa778d\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 2000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"2dc3e8ae9853f041717835d4541ec1230f1bc917028e11c79800053322b74099\",\n" +
                "                \"vout\": 2,\n" +
                "                \"amount\": 11053673714\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"d2bce82416405764974ad7483a7592fe87e4e60ed43c1c961e3e14819f757908\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 50000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"c0bc739d81561ae58ef305df10449f0245e0f8b166c16af0e8375b6842deef9b\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 100000000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"4c53736434b79e9b87fbe3988689ba989b29642bbe5f35c245d44b8b41f8fea5\",\n" +
                "                \"vout\": 0,\n" +
                "                \"amount\": 169900000000\n" +
                "        },\n" +
                "        {\n" +
                "            \"txid\": \"21e2108d3d9ab8608843b504bfc91bef98b134356f77da0326f2fbb72a42493c\",\n" +
                "                \"vout\": 1,\n" +
                "                \"amount\": 507276139983\n" +
                "        }\n" +
                "            ]";

        List<Map> list = JSONUtils.json2list(utxoJson, Map.class);
        List<Cash> inputs = list.stream().map(m -> FchUtil.converterUTXOToCash(
                m.get("txid").toString(),
                (int) m.get("vout"),
                Long.parseLong(m.get("amount").toString()))).collect(Collectors.toList());
        String to = "FGJAJnU2iQHgeNU8YpBvY2qS3g7imY8EQJ";
        long amount = 423656725555L;
        String opReturn = "1214bd1d17d8ee66bd30f7c9d4e9c3154446aa8024169ed8bcffcb06fe94d23a";
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

        // Preparing to send withdrawal FCH Transactions,
        // nerveTxHash: 91b32ece19db2914a778790f4d7ad9f67937aa8add67bc6c01ea5b44d78cde24,
        // toAddress: FH1yrZbvSjHJ3jeyTJRNUsawcYyFgh7tbx,
        // value: 100000,
        // assetId: 1,
        // signatureData: 03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff50140cb6bc53f7d858e4e1df3573355d2cb2a35a1808c363c47ae9cd1d950a1dcaa5e2cd9a769c563c73941290bfb938e0e2f4e7a8a9ec4b52702a87953bb5a6d21b2,02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed970834775001400b640ec0e1373745441a442ac11bd09637327a4d9531448047fbe510f42e39c8f9decbf255d38ac055209f0da6a0b0d44533f904aa69085ef45abbe27f655433
        String signatureData = "03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff50140cb6bc53f7d858e4e1df3573355d2cb2a35a1808c363c47ae9cd1d950a1dcaa5e2cd9a769c563c73941290bfb938e0e2f4e7a8a9ec4b52702a87953bb5a6d21b2,02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed970834775001400b640ec0e1373745441a442ac11bd09637327a4d9531448047fbe510f42e39c8f9decbf255d38ac055209f0da6a0b0d44533f904aa69085ef45abbe27f655433";
        Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(KeyTools.pubKeyToFchAddr(signDataObj.getPubkey()), signDataObj.getSignatures());
        }
        List<ECKey> pubEcKeys = new ArrayList<>(List.of(
                ECKey.fromPublicOnly(HexUtil.decode("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750")),
                ECKey.fromPublicOnly(HexUtil.decode("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5")),
                ECKey.fromPublicOnly(HexUtil.decode("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260"))
        ));
        List<Cash> inputs = new ArrayList<>(List.of(FchUtil.converterUTXOToCash(
                "0ab04ed4ba4e98bc57743bd02d26d400a8099dde68323926a2b4f5e351fda183",
                2,
                9999566
        )));
        String to = "FH1yrZbvSjHJ3jeyTJRNUsawcYyFgh7tbx";
        long amount = 100000;
        String opReturn = "91b32ece19db2914a778790f4d7ad9f67937aa8add67bc6c01ea5b44d78cde24";
        int m = 2;
        int n = 3;
        long feeRate = 1;
        boolean useAllUTXO = false;
        Long splitGranularity = 0L;
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
