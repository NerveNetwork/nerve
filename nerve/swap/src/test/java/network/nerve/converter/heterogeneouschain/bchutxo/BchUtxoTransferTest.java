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
package network.nerve.converter.heterogeneouschain.bchutxo;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.fch.RechargeData;
import network.nerve.swap.utils.HttpClientUtil;
import network.nerve.swap.utils.bch.BchUtxoUtil;
import network.nerve.swap.utils.bch.TestNet4ParamsForNerve;
import network.nerve.swap.utils.fch.BtcSignData;
import network.nerve.swap.utils.fch.FchUtil;
import network.nerve.swap.utils.fch.UTXOData;
import org.apache.http.message.BasicHeader;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/7/8
 */
public class BchUtxoTransferTest {
    private static final String ID = "id";
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String DEFAULT_ID = "1";
    private static final String JSONRPC_VERSION = "2.0";
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

    @Before
    public void before() {
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
        oldMulti = BchUtxoUtil.multiAddr(pubs, FchUtil.getByzantineCount(pubs.size()), mainnet);
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
            params = TestNet4ParamsForNerve.get();
            initAddr();
            multisigAddress = "bchtest:pr3gnmqquepj972smd7drztpx8v9tgwujqhp6mmpkz";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMainnet() {
        try {
            mainnet = true;
            params = MainNetParams.get();
            initAddr();
            multisigAddress = "bitcoincash:pp54cej9hyllw9qyvca3u6rt6csnyz46kuhlfqn0r9";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSize() {
        long size = BchUtxoUtil.calcFeeMultiSignWithSplitGranularity(
                4637, 1200, 1, 0L, 1, 32, 2, 3);
        System.out.println(size);
        System.out.println(BchUtxoUtil.calcFeeMultiSign(1,1,32,2,3));
        System.out.println(BchUtxoUtil.calcFeeMultiSignBak(1,1,32,2,3));
        System.out.println(BchUtxoUtil.calcFeeMultiSignBak(1,1,64,2,3));
        //02000000016011e15f0b5cae18a7bc7b1fc0c031efee4bfbdbb1d796271021a576982ae76d01000000fdfd000047304402206cd2e3fc87dfa45026d692ef060005a46426d1ff50a5f2fd9846250faf1c668c022014a466a09a26be2736ff5a202b9af3358b2e9dddf28e6c0744585fc7f3b6844b41483045022100f18c6e47b7d327251486044ee9f28c691636b5b33af5ea28e77257eafde2c755022031b1de0f20716e644507b5d46e34f91e4024a38aa145df57c7db6490262cff51414c695221024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba752102c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded2103b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd953aeffffffff03b0040000000000001976a9143fda920e686292be324b438d6509123ecd8e1e9f88ac0000000000000000426a4039333066336163303137366464313332663932323139326662383330363435646633336436633261623639343663346163366238646564393762333035303138570b00000000000017a914629e53e9fbf2329531f12cfc892c962010c29e1d8700000000
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

        List<UTXOData> inputs = new ArrayList<>(List.of(
                new UTXOData(
                        "5a2b2f13f7956da91f419383611a967c9b1587cc2a58c8eebc3c20e2373f554a",
                        0,
                        BigInteger.valueOf(1015000L))
        ));

        Object[] datas = BchUtxoUtil.calcFeeAndUTXO(inputs, amount, feeRate, 0);
        long fee = (long) datas[0];
        List<UTXOData> spendingCashes = (List<UTXOData>) datas[1];

        String signedTx = BchUtxoUtil.createTransactionSign(params, fromAddress, spendingCashes, priBytes, to, amount, feeRate, null);
        System.out.println(signedTx);
    }

    @Test
    public void testSignType() throws Exception {
        System.out.println(Transaction.SigHash.ALL.value | Transaction.SigHash.FORKID.value);
    }

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
        NetworkParameters networkParameters = TestNet4ParamsForNerve.get();
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

    List<byte[]> parseSignData(byte[] signData) throws NulsException {
        BtcSignData data = new BtcSignData();
        data.parse(signData, 0);
        return data.getSignatures();
    }

    private List<UTXOData> getAccountUtxo(String address) {
        if (mainnet) {
            if (address.startsWith("bitcoincash:")) {
                address = address.substring(12);
            }
            return this.takeUTXOFromOKX(address);
        }
        String url = "http://bchutxo.nerve.network/jsonrpc";
        try {
            Map<String, Object> map = new HashMap<>(8);
            map.put(ID, DEFAULT_ID);
            map.put(JSONRPC, JSONRPC_VERSION);
            map.put(METHOD, "getAddressUTXO");
            map.put(PARAMS, List.of(address));
            String resultStr = HttpClientUtil.post(url, map);
            Map result = JSONUtils.json2map(resultStr);
            List<Map> list = (List<Map>) result.get("result");
            if (list == null || list.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<UTXOData> resultList = new ArrayList<>();
            for (Map utxo : list) {
                resultList.add(new UTXOData(
                        utxo.get("txid").toString(),
                        Integer.parseInt(utxo.get("vout").toString()),
                        new BigInteger(utxo.get("value").toString())
                ));
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    @Test
    public void testUTXO() throws Exception {
        System.out.println(JSONUtils.obj2PrettyJson(this.getAccountUtxo("bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n")));
    }

    @Test
    public void testValidAddr() throws Exception {
        setMainnet();
        String addr;
        boolean valid;
        addr = "bitcoincash:qp5qf769pgn3qm3qz9csfhfyjcfvd936es320ef2z3";
        valid = CashAddress.isValidCashAddr(params, addr);
        System.out.println(valid);
        addr = "qp5qf769pgn3qm3qz9csfhfyjcfvd936es320ef2z3";
        valid = CashAddress.isValidCashAddr(params, addr);
        System.out.println(valid);
    }

    @Test
    public void testDeposit() throws IOException {
        //setTestnet();
        setMainnet();
        long feeRate = 1;
        String pri = "";
        String legacyAddr = ECKey.fromPrivate(HexUtil.decode(pri)).toAddress(params).toString();
        String fromAddress = CashAddressFactory.create().getFromBase58(params, legacyAddr).toString();
        System.out.println(fromAddress);

        byte[] priBytes = HexUtil.decode(pri);
        //String to = "bchtest:qzfe84vndnmekw6gpdf0jefv9u4lqnf8qqh6vlk2ep";
        //String to = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = multisigAddress;
        long amount = new BigDecimal("0.019").movePointRight(8).longValue();
        //long amount = 213000;

        List<UTXOData> inputs = this.getAccountUtxo(fromAddress);
        RechargeData rechargeData = new RechargeData();
        rechargeData.setTo(AddressTool.getAddress("NERVEepb6CwyEWh9mhnmPTJcuWpRzmYvoS7tLm"));
        rechargeData.setValue(amount);
        byte[] opReturn = rechargeData.serialize();

        Object[] datas = BchUtxoUtil.calcFeeAndUTXO(inputs, amount, feeRate, opReturn.length);
        long fee = (long) datas[0];
        System.out.println("calc fee: " + fee);
        List<UTXOData> spendingCashes = (List<UTXOData>) datas[1];
        System.out.println(JSONUtils.obj2PrettyJson(spendingCashes));

        String signedTx = BchUtxoUtil.createTransactionSign(params, fromAddress, spendingCashes, priBytes, to, amount, feeRate, opReturn);
        System.out.println(signedTx);
    }

    List<UTXOData> takeUTXOFromOKX(String address) {
        try {
            String apiKey = "8054293f-de13-4b16-90eb-0b09a8ac90d1";
            String url = "https://www.oklink.com/api/v5/explorer/address/utxo?chainShortName=BCH&address=%s";
            List<BasicHeader> headers = new ArrayList<>();
            headers.add(new BasicHeader("Ok-Access-Key", apiKey));
            String s = HttpClientUtil.get(String.format(url, address), headers);
            Map<String, Object> map = JSONUtils.json2map(s);
            List<Map> data = (List<Map>) map.get("data");
            if (data == null || data.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            Map dataMap = data.get(0);
            List<Map> utxoList = (List<Map>) dataMap.get("utxoList");
            if (utxoList == null || utxoList.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<UTXOData> resultList = utxoList.stream().map(utxo -> new UTXOData(
                    (String) utxo.get("txid"),
                    Integer.parseInt(utxo.get("index").toString()),
                    new BigDecimal(utxo.get("unspentAmount").toString()).movePointRight(8).toBigInteger()
            )).collect(Collectors.toList());
            return resultList;
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }

    @Test
    public void testWithdrawTx() throws SignatureDecodeException, IOException, NulsException {
        setTestnet();
        String from = "bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w";
        String to = "bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n";
        List<UTXOData> inputs = new ArrayList<>(List.of(new UTXOData(
                "9dcc4a41ec9fe860ce4e265954423b877e83e4a6be3100467ffa2cd845d8d373",
                1,
                BigInteger.valueOf(7096)
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

        byte[] sig1 = BchUtxoUtil.createMultiSigTxByOne(ecKeyA, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        byte[] sig2 = BchUtxoUtil.createMultiSigTxByOne(ecKeyB, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        byte[] sig3 = BchUtxoUtil.createMultiSigTxByOne(ecKeyC, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);

        //Sign raw tx
        Map<String, List<byte[]>> sigAll = new HashMap<>();
        sigAll.put(pubkeyA, parseSignData(sig1));
        sigAll.put(pubkeyB, parseSignData(sig2));
        sigAll.put(pubkeyC, parseSignData(sig3));

        for (String fid : sigAll.keySet()) {
            System.out.println(fid + ":");
            List<byte[]> sigList = sigAll.get(fid);
            for (byte[] sig : sigList) {
                System.out.println("    " + HexUtil.encode(sig));
            }
        }

        //build signed tx
        String txHex = BchUtxoUtil.createMultiSigTxByMulti(sigAll, pubECKeys,
                inputs, to, amount, nerveHash, m, n, feeRate, false, null, mainnet);
        System.out.println("signedTx: " + txHex);

    }
}
