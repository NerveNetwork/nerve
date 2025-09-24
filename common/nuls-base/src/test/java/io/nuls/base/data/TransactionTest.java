package io.nuls.base.data;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static io.nuls.base.signture.SignatureUtil.personalHash;

/**
 * @author: Charlie
 * @date: 2018/11/30
 */
public class TransactionTest {

    public static void main(String[] args) throws NulsException {
        String blockHex = "b062957870d479d0319372c95468d9cce5c766fedf94905239e8e9561ea4e71ddf7068ac4dc95259a699c03934e3402194471283ba760e7339782a4b225d187f14cd0160c19a7a000200000014cb510800180006cd01600800010001003c6400002102f9bdb6bf2d5e39cd826cd0712c861185b75b6028c5276df97adeb80706ef30b24630440220032bb2093823192db29d3bee2c481cc0da8757904f31544fcc9ab5d0bc15558d02207e842a7061b2185192c187eff30a367498e3d377516e64b22ae46b77456a2768010014cd0160000046000117090001789670cfe416d9f6c3c34b15530fc90568d4746209000100a086010000000000000000000000000000000000000000000000000000000000000000000000000000020012cd016000008c0117090001fc0c7e1042f22108c5d570f2caeb63857ebc6bb809000100a0c66452bfc601000000000000000000000000000000000000000000000000000847e6e8f43f6dd038000117090001e7dafba5bc339632753b9f9dd2e89f60c193d8090900010000406352bfc6010000000000000000000000000000000000000000000000000000000000000000006b2103d77af848c812639fe177158083c1df98b7b70c9c0f8023742b0890edaed6c778483046022100d1eb71d7b85ae0ea55b87fa6d2c6ad1d19f98c90773b1980f1607914d1b8c05b022100d9cda63aa266006dac80fdc4e8d753a00f9888bd62ba4353fd9d210378e01e91";
        Block block = new Block();
        block.parse(new NulsByteBuffer(HexUtil.decode(blockHex),0));
        Transaction tx = block.getTxs().get(1);
        TransactionSignature ts = new TransactionSignature();
        ts.parse(tx.getTransactionSignature(),0);
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddress(ts.getP2PHKSignatures().get(0).getPublicKey(),9),"NERVE"));

        ECKey ecKey = ECKey.fromPublicOnly(ts.getP2PHKSignatures().get(0).getPublicKey());
        boolean result = ecKey.verify(tx.getHash().getBytes(),ts.getP2PHKSignatures().get(0).getSignData().getSignBytes());
        System.out.println(result);

    }


    private CoinFrom getCoinFrom() throws Exception{
        CoinFrom coinFrom =  new CoinFrom();

        coinFrom.setAddress(AddressTool.getAddress("WSqyJxB1B83MJaAGYoJDnfqZxNc7o3930"));
        coinFrom.setAmount(new BigInteger("2678"));
        coinFrom.setAssetsChainId(1);
        coinFrom.setAssetsId(2);
        coinFrom.setLocked((byte)0);
        byte[] nonce = new byte[8];
        coinFrom.setNonce(nonce);
        System.out.println(JSONUtils.obj2json(coinFrom));
        return coinFrom;
    }

    @Test
    public void validCoinFrom() throws Exception{
        CoinFrom coinFrom = getCoinFrom();
        CoinFrom testCF = new CoinFrom();
        testCF.parse(new NulsByteBuffer(coinFrom.serialize()));
        Assert.assertTrue(Arrays.equals(coinFrom.getAddress(), testCF.getAddress()));
        Assert.assertTrue(Arrays.equals(coinFrom.getNonce(), testCF.getNonce()));
        Assert.assertEquals(coinFrom.getAmount().longValue(), testCF.getAmount().longValue());
        System.out.println(JSONUtils.obj2json(testCF));

    }

    private CoinTo getCoinTo() throws Exception{
        CoinTo coinTo = new CoinTo();
        coinTo.setAddress(AddressTool.getAddress("WSqyJxB1B83MJaAGYoJDnfqZxNc7o3930"));
        coinTo.setAmount(new BigInteger("999"));
        coinTo.setAssetsChainId(1);
        coinTo.setAssetsId(2);
        coinTo.setLockTime(System.currentTimeMillis());
        return coinTo;
    }

    private CoinData getCoinData() throws Exception{
        CoinData coinData = new CoinData();
        CoinFrom coinFrom = getCoinFrom();
        coinData.addFrom(coinFrom);
        CoinTo coinTo = getCoinTo();
        coinData.addTo(coinTo);
        return coinData;
    }

    @Test
    public void serialization() throws Exception{
        Transaction tx = new Transaction();
        tx.setType(10);
        tx.setTime(System.currentTimeMillis()/1000);
        tx.setBlockHeight(100);
        String remark = "Give it a try";
        tx.setRemark(StringUtils.bytes(remark));
        CoinData coinData = getCoinData();

        try {
            tx.setCoinData(coinData.serialize());
            //String hex = HexUtil.encode(tx.serialize());
            String hex = HexUtil.encode(tx.serialize());
            System.out.println(hex);
            Transaction transaction = new Transaction();
            transaction.parse(new NulsByteBuffer(HexUtil.decode(hex)));
            Assert.assertTrue(Arrays.equals(tx.getCoinData(), transaction.getCoinData()));

           /* CoinData cd = new CoinData();
            cd.parse(new NulsByteBuffer(transaction.getCoinData()));

            CoinFrom cf= cd.getFrom().get(0);
            System.out.println(JSONUtils.obj2json(cf));*/
            System.out.println(JSONUtils.obj2json(transaction));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void desTest() throws Exception {
        String hex = "3f00985799682463656563313330382d353164342d346433382d393263652d643666663936373635623737516ebcbd0e00000000000000000000000000000000000000000000000000000000090001f65d5bdd980478ce5005c30d3f8704c3e0ff966a00c4589968050900dc000900220309000d010900c001090001008c0117090001f65d5bdd980478ce5005c30d3f8704c3e0ff966a0900dc001157db96cf9e0700000000000000000000000000000000000000000000000000089fbe0b7e1a66d033000117090004a65c00313ecc3f1398f79c0a44bb5f9507799fe60900dc001157db96cf9e070000000000000000000000000000000000000000000000000000000000000000006b21023a31fd7232e027ef06a6950d4e6a68b1fb24fea4eb3e913a2f35b0da1addaa9d473045022100a3422c3f3245c8e0be928c99810c502ed818bdd9647b83617e94eca89015883a022024eca4ddda0765237ff377fea48caffca61712a7257a87d6dfe310e3ae5a7a9601";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(hex), 0);
        System.out.println(String.format("txHash: %s", tx.getHash().toHex()));
        String transactionSignatureJson = null;
        if(tx.getTransactionSignature() != null) {
            if (!tx.isMultiSignTx()) {
                TransactionSignature ts = new TransactionSignature();
                ts.parse(tx.getTransactionSignature(), 0);
                transactionSignatureJson = ts.toString();
            } else {
                MultiSignTxSignature mts = new MultiSignTxSignature();
                mts.parse(tx.getTransactionSignature(), 0);
                transactionSignatureJson = mts.toString();
            }
            boolean b = SignatureUtil.validateTransactionSigntureForPersonalSign(9, tx);
            System.out.println("verify: " + b);
        }
        System.out.println(transactionSignatureJson);

    }

    @Test
    public void verifyPersonSign() {
        String hash = "34f8b1e2d1931babe91a9209bd2a69e50dab16f1c73a8d267187d601c7cac809";
        byte[] personalHash = personalHash(hash);
        System.out.println("personalHash: " + HexUtil.encode(personalHash));
        String pub = "03c08a1dc38138e28be8af6268da3d3bb58db64fd13c8cd2bc9870ac4ad59787b5";
        String signData = "30450221009785553ceadd66e9de84e4c19af84e537d10e98ab956f144b47220a2241bf455022017962a4de5a5060cc5c983355e5794009f9984949aec755f195ac006437ab7ed";
        boolean verify = ECKey.verify(
                personalHash, HexUtil.decode(signData), HexUtil.decode(pub));
        System.out.println(verify);
    }
}
