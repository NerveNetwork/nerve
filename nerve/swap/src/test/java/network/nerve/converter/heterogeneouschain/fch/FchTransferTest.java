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
import fchClass.OpReturn;
import fchClass.P2SH;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.VarInt;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.io.IoUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import javaTools.JsonTools;
import keyTools.KeyTools;
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
    byte[] sessionKey = HexUtil.decode("47a75483f8800d0c36f6e11c7502b7b6f7522713d800790d665b89736f776cbc");

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
            for (String pri : pris) {
                byte[] priBytes = HexUtil.decode(pri);
                ECKey ecKey = ECKey.fromPrivate(priBytes, true);
                pubs.add(ecKey.getPubKey());
            }

            p2sh = FchUtil.genMultiP2sh(pubs, 2);
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
            System.out.println(String.format("fid996: %s", KeyTools.pubKeyToFchAddr(ECKey.fromPrivate(HexUtil.decode(fromPriKey)).getPubKey())));
            System.out.println(String.format("fidA: %s", fidA));
            System.out.println(String.format("fidB: %s", fidB));
            System.out.println(String.format("fidC: %s", fidC));
            System.out.println(String.format("pubkeyA: %s", pubkeyA));
            System.out.println(String.format("pubkeyB: %s", pubkeyB));
            System.out.println(String.format("pubkeyC: %s", pubkeyC));
            System.out.println(String.format("multisigAddress: %s", multisigAddress));
        } catch (Exception e) {
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
        // 3BXpnXkAG7SYNxyKyDimcxjkyYQcaaJs5X
        P2SH p2sh = FchUtil.genMultiP2sh(pubList, 2);
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
        p2sh = FchUtil.genMultiP2sh(pubList1, 10);
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
        String txHash = "5e6c1e6ac94d9bdfcc26cf196d682e4b6e14760686df9740049802c850a05551";
        ApipClient client = BlockchainAPIs.txByIdsPost(urlHead, new String[]{
                txHash,
                "5cda8e77bdbc65d90f18f5ce4d81d0966eaeef13a33ab79f664a27a48f9b06d6",
                "74682f8e142f0b823faf41ddaf7afb1b9a4face199cce99e5cb2d75e3da0be04"
        }, "FBejsS6cJaBrAwPcMjFJYH7iy6Krh2fkRD", sessionKey);
        System.out.println("tx info:\n" + client.getResponseBodyStr());
        List<TxInfo> txInfoList = ApipDataGetter.getTxInfoList(client.getResponseBody().getData());
        System.out.println(txInfoList.size());
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
        byte[] rawTx = FchUtil.createMultiSignRawTx(cashList, toAddress, amount, msg, oldP2SH, m, n, true);
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
}
