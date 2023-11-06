package io.nuls.consensus.model.bo.tx.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ParseTxUtils {
    public static void main(String[] args) throws NulsException, IOException {
        Transaction tx = new Transaction();
        tx.setType(TxType.REGISTER_AGENT);
        tx.setTime(System.currentTimeMillis() / 1000+60);
        Agent agent = new Agent();
        agent.setDeposit(BigInteger.valueOf(299000000000000L));
        agent.setAgentAddress(AddressTool.getAddress("NERVEepb6nsuYD48jW2Hq6W9ob1aTpZ3LiNGvk"));
        agent.setPackingAddress(AddressTool.getAddress("NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb"));
        agent.setRewardAddress(agent.getAgentAddress());
        tx.setTxData(agent.serialize());
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        List<CoinTo> tos = new ArrayList<>();
        CoinFrom from = new CoinFrom();
        from.setAmount(BigInteger.valueOf(299000000100000L));
        from.setAddress(agent.getAgentAddress());
        from.setNonce(HexUtil.decode("a07d622dbe1a4012"));
        from.setLocked((byte) 0);
        from.setAssetsChainId(9);
        from.setAssetsId(1);
        froms.add(from);
        coinData.setFrom(froms);
        CoinTo to = new CoinTo();
        to.setAddress(agent.getAgentAddress());
        to.setAssetsId(1);
        to.setAssetsChainId(9);
        to.setLockTime(-1);
        to.setAmount(BigInteger.valueOf(299000000000000L));
        tos.add(to);
        coinData.setTo(tos);
        tx.setCoinData(coinData.serialize());
        MultiSignTxSignature signature = new MultiSignTxSignature();
        signature.setM((byte) 3);
        List<byte[]> publicKeyList = List.of(
                HexUtil.decode("02376148f0332ca5bafc89f55777308f0d042290222fc0826ab16f40e2d39d17ba"),
                HexUtil.decode("03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2"),
                HexUtil.decode("03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4"),
                HexUtil.decode("031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6"),
                HexUtil.decode("022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"));
        signature.setPubKeyList(publicKeyList);
        tx.setTransactionSignature(signature.serialize());
        System.out.println("结果");
        System.out.println(HexUtil.encode(tx.serialize()));
    }

    public static void main1(String[] args) throws NulsException {
        String txHex = "0900ad6c0965003817090003ec13810701ead3fc53567ae6b94e17eb6c7ca81ca8928c709f3c0e6063b3cddbc8c1fc3f4d27cb417be095d5289ea3ff8c07dffb8c0117090003ec13810701ead3fc53567ae6b94e17eb6c7ca81c090001000060b7986c88000000000000000000000000000000000000000000000000000008289ea3ff8c07dffbff0117090003ec13810701ead3fc53567ae6b94e17eb6c7ca81c0900010060d9b5986c8800000000000000000000000000000000000000000000000000002d331d6500000000fd7f0103052102376148f0332ca5bafc89f55777308f0d042290222fc0826ab16f40e2d39d17ba2103d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e22103a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e421031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa621022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c3821022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c3846304402200e0c2e36c29877181672f97b6c8866aca180554992faf8c5671e23c6432ee5f6022067fea8b69dab66c53429847ad990125f30dea8da09c92c4f1874fbb0f9cf0c0a2103a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e44730450221009d0a2826bfafcfba18ff4764dcc094dcfe70d1c0b86a3690b67c7a37e80d3331022007180d4221e3c3c88a3b3c7a634f31ac3ef867efcbccae111f1618ad96e0364f";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(txHex), 0);
//        Agent agent = new Agent();
//        agent.parse(tx.getTxData(), 0);
        MultiSignTxSignature signature = new MultiSignTxSignature();
        signature.parse(tx.getTransactionSignature(), 0);
        System.out.println(signature.getP2PHKSignatures().size());
        for (P2PHKSignature p2PHKSignature : signature.getP2PHKSignatures()) {
            System.out.println("pub: " + HexUtil.encode(p2PHKSignature.getPublicKey()));
        }
        for (byte[] bytes : signature.getPubKeyList()) {
            System.out.println(HexUtil.encode(bytes));
        }
    }


}
