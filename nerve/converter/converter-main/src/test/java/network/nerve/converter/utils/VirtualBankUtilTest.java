package network.nerve.converter.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.TxSubsequentProcessKeyListPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.VoteProposalTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class VirtualBankUtilTest {

    @Test
    public void bankOrderTest() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aa", 1 + 30);
        map.put("bb", 2);
        map.put("cc", 3);
        map.put("dd", 4);
        map.put("ee", 5);
        map.put("aa5", 6 + 30);
        map.put("bb5", 7);
        map.put("cc5", 8);
        map.put("dd5", 9 + 30);
        map.put("ee5", 10);
        map.put("aa10", 11);
        map.put("bb10", 12);
        map.put("cc10", 13);
        map.put("dd10", 14);
        map.put("ee10", 15);
        List<String> temp = List.of("aa", "bb", "cc", "dd", "ee");
        for (String t : temp) {
            System.out.println(String.format("key: %s, value: %s", t, map.get(t)));
        }
        for (String t : temp) {
            String key = t + 5;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }
        for (String t : temp) {
            String key = t + 10;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }

        int seed = 14;
        int bankSize = map.size();
        int mod = seed % bankSize + 1;
        map.entrySet().stream().forEach(e -> {
            int bankOrder = e.getValue();
            if (bankOrder < mod) {
                bankOrder += bankSize - (mod - 1);
            } else {
                bankOrder -= mod - 1;
            }
            e.setValue(bankOrder);
        });
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        for (String t : temp) {
            System.out.println(String.format("key: %s, value: %s", t, map.get(t)));
        }
        for (String t : temp) {
            String key = t + 5;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }
        for (String t : temp) {
            String key = t + 10;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }
        List<Map.Entry<String, Integer>> list = new ArrayList(map.entrySet());
        list.sort(ConverterUtil.CHANGE_SORT);
        int i = 1;
        for (Map.Entry<String, Integer> entry : list) {
            map.put(entry.getKey(), i++);
        }
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        for (String t : temp) {
            System.out.println(String.format("key: %s, value: %s", t, map.get(t)));
        }
        for (String t : temp) {
            String key = t + 5;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }
        for (String t : temp) {
            String key = t + 10;
            System.out.println(String.format("key: %s, value: %s", key, map.get(key)));
        }
    }

    @Test
    public void test() {

        List<VirtualBankDirector> list = new ArrayList<>();
        list.add(newInstance(5, "eee"));
        list.add(newInstance(1, "aaa"));
        list.add(newInstance(3, "ccc"));
        list.add(newInstance(2, "bbb"));
        list.add(newInstance(4, "ddd"));
        Collections.sort(list, VirtualBankDirectorSort.getInstance());
        list.stream().forEach(v -> {
            System.out.println(String.format("order: %s, hash: %s", v.getOrder(), v.getAgentHash()));
        });
    }

    private VirtualBankDirector newInstance(int order, String hash) {
        VirtualBankDirector director = new VirtualBankDirector();
        director.setOrder(order);
        director.setAgentHash(hash);
        return director;
    }

    @Test
    public void sleepTest() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void testCV_DATA() throws Exception {
        RocksDBService.init("/Users/pierreluo/IdeaProjects/nerve-network/logs/cv_data03/");
        TxSubsequentProcessKeyListPO listPO = ConverterDBUtil.getModel("cv_pending_9", "PENDING_TX_ALL".getBytes(StandardCharsets.UTF_8), TxSubsequentProcessKeyListPO.class);
        List<TxSubsequentProcessPO> list = new ArrayList<>();
        if(null == listPO || null == listPO.getListTxHash()){
            return;
        }
        for (String txHash : listPO.getListTxHash()) {
            list.add(ConverterDBUtil.getModel("cv_pending_9", txHash.getBytes(StandardCharsets.UTF_8), TxSubsequentProcessPO.class));
        }
        for (TxSubsequentProcessPO po : list) {
            System.out.println(po.getTx().format(WithdrawalTxData.class));
        }
    }

    @Test
    public void serializeTxTest() throws Exception {
//        NulsHash proposalTxHash = NulsHash.fromHex("11560840cc33331f745ed0d62cda6420bbceeb55c1ca6888102c62069a2a5463");
//        String address = "NERVEepb6exLKu4eJkHCekXicn7YpDa3hzHGvt";
//        List<byte[]> pubKeyList = new ArrayList<>();
//        String pubkeys = "02aeddccc442bb3b4cd1a50efbf41aa4681d213b998e68295675e9dd149cdb72b4,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38";
//        String arr[] = pubkeys.split(",");
//        for(String pub:arr){
//            pubKeyList.add(HexUtil.decode(pub));
//        }
//        String nonce = "2411ef0cf84f1ce0";

//        NulsHash proposalTxHash = NulsHash.fromHex("11560840cc33331f745ed0d62cda6420bbceeb55c1ca6888102c62069a2a5463");
//        String address = "NERVEepb6eKxoK6ZmKhhDK4oZkuXnUMxZfCta4";
//        List<byte[]> pubKeyList = new ArrayList<>();
//        String pubkeys = "035b6fef36e032382a06ededf20ec7c522215cb114e46e6c828617680d7d376b00,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38";
//        String arr[] = pubkeys.split(",");
//        for(String pub:arr){
//            pubKeyList.add(HexUtil.decode(pub));
//        }
//        String nonce = "c830cd416d60903f";

//        NulsHash proposalTxHash = NulsHash.fromHex("11560840cc33331f745ed0d62cda6420bbceeb55c1ca6888102c62069a2a5463");
//        String address = "NERVEepb6bT6dAfEtY9Z4c38Khawdr4LKZNais";
//        List<byte[]> pubKeyList = new ArrayList<>();
//        String pubkeys = "027d1ae118dd1d5ab0c416dc78ee0268d38f3a8c8cee2641a03f1d851b1c8405b9,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38";
//        String arr[] = pubkeys.split(",");
//        for(String pub:arr){
//            pubKeyList.add(HexUtil.decode(pub));
//        }
//        String nonce = "97120e657a76bb38";

//        NulsHash proposalTxHash = NulsHash.fromHex("11560840cc33331f745ed0d62cda6420bbceeb55c1ca6888102c62069a2a5463");
//        String address = "NERVEepb6nsuYD48jW2Hq6W9ob1aTpZ3LiNGvk";
//        List<byte[]> pubKeyList = new ArrayList<>();
//        String pubkeys = "02376148f0332ca5bafc89f55777308f0d042290222fc0826ab16f40e2d39d17ba,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38";
//        String arr[] = pubkeys.split(",");
//        for(String pub:arr){
//            pubKeyList.add(HexUtil.decode(pub));
//        }
//        String nonce = "75c643fb277e8077";
//
        NulsHash proposalTxHash = NulsHash.fromHex("11560840cc33331f745ed0d62cda6420bbceeb55c1ca6888102c62069a2a5463");
        String address = "NERVEepb6bt22V1LgaWavLNPEyTKbayCD58xo4";
        List<byte[]> pubKeyList = new ArrayList<>();
        String pubkeys = "03501d23db2f62863d89631168d427d3cc5f8b4a28fbb643e26695ae6cd5fdcc4b,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38";
        String arr[] = pubkeys.split(",");
        for(String pub:arr){
            pubKeyList.add(HexUtil.decode(pub));
        }
        String nonce = "dd41c40a11f0e927";

        VoteProposalTxData voteProposalTxData = new VoteProposalTxData(proposalTxHash, (byte)1);
        Transaction tx = new Transaction(TxType.VOTE_PROPOSAL);
        tx.setTxData(voteProposalTxData.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);

        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                9,
                1,
                TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES,
                HexUtil.decode(nonce),
                (byte) 0);
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);

        List<CoinTo> tos = new ArrayList<>();
        CoinTo coinTo = new CoinTo(
                AddressTool.getAddress(address),
                9,
                1,
                BigInteger.ZERO);
        tos.add(coinTo);

        coinData.setFrom(froms);
        coinData.setTo(tos);
        tx.setCoinData(coinData.serialize());

        MultiSignTxSignature signature = new MultiSignTxSignature();
        signature.setM((byte)3);
        signature.setPubKeyList(pubKeyList);
        tx.setTransactionSignature(signature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }
}