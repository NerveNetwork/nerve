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
 *//*

package network.nerve.quotation.converter.heterogeneouschain.bchutxo;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.PeerNode;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.parse.JSONUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.SchnorrSignature;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet4Params;
import org.bitcoinj.params.TestNet4Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static network.nerve.quotation.converter.heterogeneouschain.bchutxo.BchUtxoUtil.DustInSatoshi;

*/
/**
 * @author: PierreLuo
 * @date: 2024/7/8
 *//*

public class BchUtxoTransferTest {

    Map<String, Object> pMap;
    String fromPriKey;
    String fromPubKey;
    ECKey fromPubECKey;
    String fromAddress;
    NetworkParameters params;

    List<String> pris;
    List<byte[]> pubs;
    List<ECKey> pubECKeys;
    byte[] priKeyBytesA;
    byte[] priKeyBytesB;
    byte[] priKeyBytesC;
    ECKey ecKeyA;
    ECKey ecKeyB;
    ECKey ecKeyC;
    String pubkeyA;
    String pubkeyB;
    String pubkeyC;
    String addrA;
    String addrB;
    String addrC;
    String packageAddressPrivateKeyZP;
    String packageAddressPrivateKeyNE;
    String packageAddressPrivateKeyHF;
    String multisigAddress;
    boolean mainnet;

    private String protocol = "http";
    private String host = "192.168.5.138";
    private String port = "18332";
    private String user = "cobble";
    private String password = "asdf1234";
    private String auth_scheme = "Basic";

    private BtcdClient client;

    void initRpc() {
        client = new BtcdClientImpl(makeCurrentProperties());
    }

    Properties makeCurrentProperties() {
        Properties nodeConfig = new Properties();
        nodeConfig.put(NodeProperties.RPC_PROTOCOL.getKey(), protocol);
        nodeConfig.put(NodeProperties.RPC_HOST.getKey(), host);
        nodeConfig.put(NodeProperties.RPC_PORT.getKey(), port);
        nodeConfig.put(NodeProperties.RPC_USER.getKey(), user);
        nodeConfig.put(NodeProperties.RPC_PASSWORD.getKey(), password);
        nodeConfig.put(NodeProperties.HTTP_AUTH_SCHEME.getKey(), auth_scheme);
        return nodeConfig;
    }

    @Before
    public void before() {
        initRpc();
        //pubkeyA: 03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9
        //pubkeyB: 024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75
        //pubkeyC: 02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded
        //pubkey996: 039087cc88855da54458e231c50cb76300334abe46553d3c7a76bc8af0852b87f0
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

            priKeyBytesA = HexUtil.decode(pris.get(0));
            priKeyBytesB = HexUtil.decode(pris.get(1));
            priKeyBytesC = HexUtil.decode(pris.get(2));

            ecKeyA = ECKey.fromPrivate(priKeyBytesA);
            ecKeyB = ECKey.fromPrivate(priKeyBytesB);
            ecKeyC = ECKey.fromPrivate(priKeyBytesC);

            pubkeyA = ecKeyA.getPublicKeyAsHex();
            pubkeyB = ecKeyB.getPublicKeyAsHex();
            pubkeyC = ecKeyC.getPublicKeyAsHex();

            fromPriKey = pMap.get(evmFrom.toLowerCase()).toString();
            fromPubKey = ECKey.fromPrivate(HexUtil.decode(fromPriKey)).getPublicKeyAsHex();

            System.out.println(String.format("pubkeyA: %s", pubkeyA));
            System.out.println(String.format("pubkeyB: %s", pubkeyB));
            System.out.println(String.format("pubkeyC: %s", pubkeyC));
            System.out.println(String.format("pubkey996: %s", fromPubKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
        fromPubECKey = ECKey.fromPublicOnly(HexUtil.decode(fromPubKey));
    }

    private void initAddr() {
        String oldA, oldB, oldC, oldMulti;
        String legacyAddr = fromPubECKey.toAddress(params).toString();
        fromAddress = CashAddressFactory.create().getFromBase58(params, legacyAddr).toString();
        addrA = CashAddressFactory.create().getFromBase58(params, oldA = ecKeyA.toAddress(params).toString()).toString();
        addrB = CashAddressFactory.create().getFromBase58(params, oldB = ecKeyB.toAddress(params).toString()).toString();
        addrC = CashAddressFactory.create().getFromBase58(params, oldC = ecKeyC.toAddress(params).toString()).toString();
        oldMulti = BchUtxoUtil.multiAddr(pubs, BchUtxoUtil.getByzantineCount(pubs.size()), mainnet);
        multisigAddress = CashAddressFactory.create().getFromBase58(params, oldMulti).toString();
        System.out.println(String.format("testnet legacyAddr996: %s, address996: %s", legacyAddr, fromAddress));
        System.out.println(String.format("testnet legacyAddrA: %s, addressA: %s", oldA, addrA));
        System.out.println(String.format("testnet legacyAddrB: %s, addressB: %s", oldB, addrB));
        System.out.println(String.format("testnet legacyAddrC: %s, addressC: %s", oldC, addrC));
        System.out.println(String.format("testnet oldMulti: %s, multisigAddress: %s", oldMulti, multisigAddress));
    }

    private void setTestnet() {
        //testnet legacyAddr996: mmLahgkWGHQSKszCDcZXPooWoRuYhQPpCF, address996: bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n
        //testnet legacyAddrA: mpkGu7LBSLf799X91wAZhSyT6hAb4XiTLG, addressA: bchtest:qpjnuyw6dfl4wwu2ehtwemv82sh20j0vkgc76mg7k2
        //testnet legacyAddrB: mqkFNyzmfVm22a5PJc7HszQsBeZo5GohTv, addressB: bchtest:qpcrfm2m8jk05v7f9zh5l3x3xlc3qhr7v5ggjmausl
        //testnet legacyAddrC: n3NP5XXet1mtBCSsWZcUAj2Px4jRJ9K1Ur, addressC: bchtest:qrhm8dj44cr3qf3h83hsl2mp3ttu83w68s4yjx7kyt
        //testnet oldMulti: 2N2EfstdVXkxSzxomSGbvwVvKJhvvSkveby, multisigAddress: bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w
        try {
            mainnet = false;
            params = TestNet4Params.get();
            initAddr();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMainnet() {
        try {
            mainnet = true;
            params = MainNetParams.get();
            initAddr();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConverterAddr() {
        setTestnet();
        boolean validCashAddr = Address.isValidCashAddr(params, fromAddress);
        System.out.println(validCashAddr);
        setMainnet();
        validCashAddr = Address.isValidCashAddr(params, fromAddress);
        System.out.println(validCashAddr);
    }

    @Test
    public void testAddr() {
        setTestnet();
        String legacyAddressP2PKH = "mtyGixaKTmm4TZp1GGc1kRo9kmo1seB92z";
        String newAddr = CashAddressFactory.create().getFromBase58(params, legacyAddressP2PKH).toString();
        System.out.println(String.format("old: %s, new: %s", legacyAddressP2PKH, newAddr));
    }

    @Test
    public void createTransferTransaction() {
        setTestnet();
        long feeRate = 1;
        String pri = fromPriKey;
        byte[] priBytes = HexUtil.decode(pri);
        //String to = "bchtest:qzfe84vndnmekw6gpdf0jefv9u4lqnf8qqh6vlk2ep";
        String to = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        //long amount = new BigDecimal("0.001").movePointRight(8).longValue();
        long amount = 12000;

        List<SendTo> outputs = new ArrayList<>();
        SendTo sendTo = new SendTo();
        sendTo.setFid(to);
        sendTo.setAmount(amount);
        outputs.add(sendTo);

        List<UTXOData> inputs = new ArrayList<>(List.of(
                new UTXOData(
                "5a2b2f13f7956da91f419383611a967c9b1587cc2a58c8eebc3c20e2373f554a",
                0,
                BigInteger.valueOf(1015000L))
        ));

        Object[] datas = BchUtxoUtil.calcFeeAndUTXO(inputs, amount, feeRate, 0);
        long fee = (long) datas[0];
        List<UTXOData> spendingCashes = (List<UTXOData>) datas[1];

        String signedTx = BchUtxoUtil.createTransactionSign(params, fromAddress, spendingCashes, priBytes, outputs, null);
        System.out.println(signedTx);
    }

    @Test
    public void testSignType() throws Exception {
        System.out.println(Transaction.SigHash.ALL.value | Transaction.SigHash.FORKID.value);
        System.out.println(Transaction.SigHash.NONE.value | Transaction.SigHash.FORKID.value);
        System.out.println(Transaction.SigHash.SINGLE.value | Transaction.SigHash.FORKID.value);
    }

    @Test
    public void testBroadcastTx() throws Exception {

    }

    void broadcast(Transaction _tx) {
        try {
            List<PeerNode> peerInfo = client.getPeerInfo();

            // Broadcast tx
            PeerGroup pGroup = new PeerGroup(params);
            int minConnections = 3;
            pGroup.startAsync();
            peerInfo.stream().forEach(p -> System.out.println(p.getAddr()));
            peerInfo.stream().forEach(p -> {
                try {
                    if (!p.getAddr().startsWith("[")) {
                        pGroup.addAddress(InetAddress.getByName(p.getAddr().split(":")[0]));
                    }
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            });
            Transaction tx = pGroup.broadcastTransaction(_tx, minConnections, true).broadcast().get();
            System.out.println("txId: " + tx.getTxId());

        } catch (ScriptException ex) {
            throw new RuntimeException(ex);
        } catch (VerificationException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }
    }

    */
/**
     024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75:
     2124aee2dee0580f39b5efb9eb22fdb748e4b655f7962b53dba77ae0322cab40d5652b2890980c14708eabe6aa170fa78750f265ce8a657814fa7ed68e972ec3
     03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9:
     15d201d4fef0f82deacbbe43e83a886561eb665725f05b37a7c1768d110ab1e05ca0de0ca24dab3c85b05ac768db26e5a159cddddd028d27c5ab711b5be05b57
     02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded:
     187c4065e9afddbfedc41655540df35b64fdefd5ec9442461da5fa9a130a4bedc940b33951a6f657ee5e952877da9c27e68022978546ff16ca7d33aceda9fd47
     signedTx: 020000000186d6562cd60d6da678c87806147fc4b51cefd5b725ce8c34962636bc196ed2e000000000f000412124aee2dee0580f39b5efb9eb22fdb748e4b655f7962b53dba77ae0322cab40d5652b2890980c14708eabe6aa170fa78750f265ce8a657814fa7ed68e972ec3414115d201d4fef0f82deacbbe43e83a886561eb665725f05b37a7c1768d110ab1e05ca0de0ca24dab3c85b05ac768db26e5a159cddddd028d27c5ab711b5be05b57414c695221024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba752102c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded2103b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd953aeffffffff02d0070000000000001976a9143fda920e686292be324b438d6509123ecd8e1e9f88aca92500000000000017a914629e53e9fbf2329531f12cfc892c962010c29e1d8700000000

     *//*

    */
/*@Test
    public void testWithdrawTx0_Schnorr() {
        setTestnet();
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        List<UTXOData> inputs = new ArrayList<>(List.of(new UTXOData(
                "e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686",
                0,
                BigInteger.valueOf(12000)
        )));
        int n = pubECKeys.size(), m = BchUtxoUtil.getByzantineCount(n);
        long feeRate = 1;
        long total = 0;
        for (UTXOData cash : inputs) {
            total += cash.getAmount().longValue();
        }
        long feeSize = BchUtxoUtil.calcFeeMultiSign(inputs.size(), 1, 0, m, n);
        long fee = feeSize * feeRate;
        long amount = 2000;
        String nerveHash = null;
        //Make raw tx
        Object[] rawTxBase = BchUtxoUtil.createMultiSignRawTxBase(
                pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        byte[] rawTx = (byte[]) rawTxBase[0];
        Script redeemScript = (Script) rawTxBase[1];
        Transaction tx = (Transaction) rawTxBase[2];
        List<UTXOData> realInputs = (List<UTXOData>) rawTxBase[3];

        //Sign raw tx
        Map<String, List<byte[]>> sig1 = BchUtxoUtil.signSchnorrMultiSignTx(tx, redeemScript, realInputs, HexUtil.decode(pris.get(0)), mainnet);
        Map<String, List<byte[]>> sig2 = BchUtxoUtil.signSchnorrMultiSignTx(tx, redeemScript, realInputs, HexUtil.decode(pris.get(1)), mainnet);
        Map<String, List<byte[]>> sigAll = new HashMap<>();
        sigAll.putAll(sig1);
        sigAll.putAll(sig2);
        Map<String, List<byte[]>> sig3 = BchUtxoUtil.signSchnorrMultiSignTx(tx, redeemScript, realInputs, HexUtil.decode(pris.get(2)), mainnet);
        sigAll.putAll(sig3);

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<byte[]> sigList = sigAll.get(fid);
            for (byte[] sig : sigList) {
                System.out.println("    " + HexUtil.encode(sig));
            }
        }

        //build signed tx
        String signedTx = BchUtxoUtil.buildSchnorrMultiSignTx(pubs, redeemScript, tx, sigAll, m, mainnet);
        System.out.println("signedTx: " + signedTx);

    }

    @Test
    public void testWithdrawTxI_Schnorr() {
        setTestnet();
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        List<UTXOData> inputs = new ArrayList<>(List.of(new UTXOData(
                "e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686",
                0,
                BigInteger.valueOf(12000)
        )));
        int n = pubECKeys.size(), m = BchUtxoUtil.getByzantineCount(n);
        long feeRate = 1;
        long total = 0;
        for (UTXOData cash : inputs) {
            total += cash.getAmount().longValue();
        }
        long feeSize = BchUtxoUtil.calcFeeMultiSign(inputs.size(), 1, 0, m, n);
        long fee = feeSize * feeRate;
        long amount = 2000;

        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet4Params.get();
        Script redeemScript = ScriptBuilder.createRedeemScript(m, pubECKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSignAddr = Address.fromP2SHScript(networkParameters, scriptPubKey).toString();
        multiSignAddr = CashAddressFactory.create().getFromBase58(networkParameters, multiSignAddr).toString();
        System.out.println(String.format("from createMultiSignRawTxBase, multiSignAddr: %s", multiSignAddr));

        Transaction tx = new Transaction(networkParameters);
        tx.addOutput(Coin.valueOf(amount), CashAddressFactory.create().getFromFormattedAddress(networkParameters, to));

        for (UTXOData usingUtxo : inputs) {
            UTXOData input = usingUtxo;
            TransactionOutPoint outPoint = new TransactionOutPoint(networkParameters, (long) input.getVout(), Sha256Hash.wrap(input.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(networkParameters, tx, new byte[0], outPoint, Coin.valueOf(input.getAmount().longValue()));
            tx.addInput(unsignedInput);
        }

        if (amount + fee > total) {
            throw new RuntimeException("input is not enough");
        } else {
            long change = total - amount - fee;
            if (change > DustInSatoshi) {
                Address changeAddress = CashAddressFactory.create().getFromFormattedAddress(networkParameters, multiSignAddr);
                tx.addOutput(Coin.valueOf(change), changeAddress);
            }
        }


        SchnorrSignature s1 = tx.calculateSchnorrSignature(0, ecKeyA, redeemScript.getProgram(), Coin.valueOf(12000), Transaction.SigHash.ALL, false);
        SchnorrSignature s2 = tx.calculateSchnorrSignature(0, ecKeyB, redeemScript.getProgram(), Coin.valueOf(12000), Transaction.SigHash.ALL, false);

        Script script = ScriptBuilder.createSchnorrMultiSigInputScriptBytes(List.of(s1, s2).stream().map(s -> s.encodeToBitcoin()).collect(Collectors.toList()), redeemScript.getProgram());

        tx.getInputs().get(0).setScriptSig(script);

        System.out.println("signedTx: " + HexUtil.encode(tx.bitcoinSerialize()));
        broadcast(tx);

    }*//*


    @Test
    public void testMultiSendTxByECDSA() {
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        UTXOData cash = new UTXOData("e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686", 0, BigInteger.valueOf(12000));
        List<ECKey> pubECKeys = new ArrayList<>();
        pubECKeys.add(ECKey.fromPublicOnly(HexUtil.decode("03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9")));
        pubECKeys.add(ECKey.fromPublicOnly(HexUtil.decode("024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75")));
        pubECKeys.add(ECKey.fromPublicOnly(HexUtil.decode("02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded")));
        int n = 3, m = 2;
        long total = cash.getAmount().longValue(), amount = 2000, fee = 400;
        NetworkParameters networkParameters = TestNet4Params.get();
        Script redeemScript = ScriptBuilder.createRedeemScript(m, pubECKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSignAddr = Address.fromP2SHScript(networkParameters, scriptPubKey).toString();
        multiSignAddr = CashAddressFactory.create().getFromBase58(networkParameters, multiSignAddr).toString();
        System.out.println(String.format("Address.fromP2SHScript, multiSignAddr: %s", multiSignAddr));
        Transaction tx = new Transaction(networkParameters);
        tx.addOutput(Coin.valueOf(amount), CashAddressFactory.create().getFromFormattedAddress(networkParameters, to));
        TransactionOutPoint outPoint = new TransactionOutPoint(networkParameters, (long) cash.getVout(), Sha256Hash.wrap(cash.getTxid()));
        TransactionInput unsignedInput = new TransactionInput(networkParameters, tx, new byte[0], outPoint, Coin.valueOf(cash.getAmount().longValue()));
        tx.addInput(unsignedInput);
        long change = total - amount - fee;
        Address changeAddress = CashAddressFactory.create().getFromFormattedAddress(networkParameters, multiSignAddr);
        tx.addOutput(Coin.valueOf(change), changeAddress);
        TransactionInput input0 = tx.getInputs().get(0);
        TransactionSignature sigA = tx.calculateWitnessSignature(input0.getIndex(), ecKeyA, redeemScript.getProgram(), input0.getValue(), Transaction.SigHash.ALL, false);
        TransactionSignature sigB = tx.calculateWitnessSignature(input0.getIndex(), ecKeyB, redeemScript.getProgram(), input0.getValue(), Transaction.SigHash.ALL, false);
        TransactionSignature sigC = tx.calculateWitnessSignature(input0.getIndex(), ecKeyC, redeemScript.getProgram(), input0.getValue(), Transaction.SigHash.ALL, false);
        Script script = ScriptBuilder.createP2SHMultiSigInputScript(List.of(sigB, sigC), redeemScript);
        tx.getInputs().get(0).setScriptSig(script);
        System.out.println("signedTx: " + HexUtil.encode(tx.bitcoinSerialize()));
    }

    @Test
    public void testWithdrawTxII_ECDSA() throws SignatureDecodeException {
        setTestnet();
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        List<UTXOData> inputs = new ArrayList<>(List.of(new UTXOData(
                "65436a26eba97b91f4b3decb816e3d8a2f71c6f55bd3d0aa0d80eb5c6e28c8e6",
                1,
                BigInteger.valueOf(9600)
        )));
        int n = pubECKeys.size(), m = BchUtxoUtil.getByzantineCount(n);
        long feeRate = 1;
        long total = 0;
        for (UTXOData cash : inputs) {
            total += cash.getAmount().longValue();
        }
        long feeSize = BchUtxoUtil.calcFeeMultiSign(inputs.size(), 1, 0, m, n);
        long fee = feeSize * feeRate;
        long amount = 2000;
        String nerveHash = null;
        //Make raw tx

        List<String> sig1 = BchUtxoUtil.createMultiSigTxByOne(ecKeyA, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        List<String> sig2 = BchUtxoUtil.createMultiSigTxByOne(ecKeyB, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        List<String> sig3 = BchUtxoUtil.createMultiSigTxByOne(ecKeyC, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);

        //Sign raw tx
        Map<String, List<String>> sigAll = new HashMap<>();
        sigAll.put(pubkeyA, sig1);
        sigAll.put(pubkeyB, sig2);
        sigAll.put(pubkeyC, sig3);

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<String> sigList = sigAll.get(fid);
            for (String sig : sigList) {
                System.out.println("    " + sig);
            }
        }

        //build signed tx
        Transaction tx = BchUtxoUtil.createMultiSigTxByMulti(sigAll, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        System.out.println("signedTx: " + HexUtil.encode(tx.bitcoinSerialize()));

    }


    @Test
    public void testWithdrawTxIII_ECDSA() throws SignatureDecodeException {
        setTestnet();
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        List<UTXOData> inputs = new ArrayList<>(List.of(new UTXOData(
                "e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686",
                0,
                BigInteger.valueOf(12000)
        )));
        int n = pubECKeys.size(), m = BchUtxoUtil.getByzantineCount(n);
        long feeRate = 1;
        long total = 0;
        for (UTXOData cash : inputs) {
            total += cash.getAmount().longValue();
        }
        long amount = 2000;
        String nerveHash = null;
        //Make raw tx
        Object[] rawTxBase = BchUtxoUtil.createMultiSignRawTxBase(
                pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        byte[] rawTx = (byte[]) rawTxBase[0];
        Script redeemScript = (Script) rawTxBase[1];
        Transaction tx = (Transaction) rawTxBase[2];
        List<UTXOData> realInputs = (List<UTXOData>) rawTxBase[3];

        //Sha256Hash h1 = tx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false, true);
        //TransactionSignature sig1 = new TransactionSignature(ecKeyA.sign(h1), Transaction.SigHash.ALL, false, true);
        //TransactionSignature sig2 = new TransactionSignature(ecKeyB.sign(h1), Transaction.SigHash.ALL, false, true);
        //TransactionSignature sig3 = new TransactionSignature(ecKeyC.sign(h1), Transaction.SigHash.ALL, false, true);
        TransactionSignature sig1 = tx.calculateSignature(0, ecKeyA, redeemScript, Transaction.SigHash.ALL, false);
        TransactionSignature sig2 = tx.calculateSignature(0, ecKeyB, redeemScript, Transaction.SigHash.ALL, false);
        TransactionSignature sig3 = tx.calculateSignature(0, ecKeyC, redeemScript, Transaction.SigHash.ALL, false);

        boolean boolA = BchUtxoUtil.verifyMultiSigTxByOne(
                ecKeyA,
                List.of(HexUtil.encode(sig1.encodeToDER())),
                pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        boolean boolB = BchUtxoUtil.verifyMultiSigTxByOne(
                ecKeyB,
                List.of(HexUtil.encode(sig2.encodeToDER())),
                pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        boolean boolC = BchUtxoUtil.verifyMultiSigTxByOne(
                ecKeyC,
                List.of(HexUtil.encode(sig3.encodeToDER())),
                pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        System.out.println(boolA);
        System.out.println(boolB);
        System.out.println(boolC);

        List<TransactionSignature> signatures = Arrays.asList(sig2, sig3, sig1);
        List<byte[]> sigs = new ArrayList<>(signatures.size());
        for (TransactionSignature signature : signatures) {
            sigs.add(signature.encodeToBitcoin());
        }
        Script inputScript = ScriptBuilder.createMultiSigInputScriptBytes(sigs, redeemScript.getProgram());
        
        //Script inputScript = ScriptBuilder.createMultiSigInputScript(Arrays.asList(sig1, sig2));
        tx.getInput(0).setScriptSig(inputScript);

        System.out.println("signedTx: " + HexUtil.encode(tx.bitcoinSerialize()));

    }

}
*/
