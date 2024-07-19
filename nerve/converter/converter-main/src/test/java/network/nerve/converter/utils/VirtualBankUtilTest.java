package network.nerve.converter.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.util.JsonRpcUtil;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.TxSubsequentProcessKeyListPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.RechargeTxData;
import network.nerve.converter.model.txdata.VoteProposalTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class VirtualBankUtilTest {

    @Test
    public void balanceOrderTest() throws Exception {
        Map<String, BigDecimal> balanceMap = new HashMap<>();
        balanceMap.put("aaa", new BigDecimal("2.3"));
        balanceMap.put("bbb", new BigDecimal("3.3"));
        balanceMap.put("ccc", new BigDecimal("5.3"));
        balanceMap.put("ddd", new BigDecimal("1.3"));
        balanceMap.put("eee", new BigDecimal("4.3"));
        balanceMap.put("a0", new BigDecimal("0"));
        Map<String, Integer> resultMap = new HashMap<>();
        List<Map.Entry<String, BigDecimal>> list = new ArrayList(balanceMap.entrySet());
        list.sort(ConverterUtil.BALANCE_SORT);
        int i = 1;
        for (Map.Entry<String, BigDecimal> entry : list) {
            resultMap.put(entry.getKey(), i++);
        }
        System.out.println(JSONUtils.obj2PrettyJson(resultMap));
    }

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
    public void recharge2nerve() throws Exception {
        RechargeTxData txData = new RechargeTxData("0x2f05ab966e9f98e67a099ec0886331e93bb386e2c0d119d34affbb50b34c981e");
        txData.setHeterogeneousChainId(103);
        byte[] toAddress = AddressTool.getAddress("NERVEepb6ESTRJAjMX31XREfkzcD4AHNfM9gtG");
        CoinTo coinTo = new CoinTo(
                toAddress,
                1,
                1,
                new BigInteger("60091679746"));
        List<CoinTo> tos = new ArrayList<>();
        tos.add(coinTo);
        CoinData coinData = new CoinData();
        coinData.setTo(tos);
        byte[] coinDataBytes = null;
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.RECHARGE);
        tx.setTxData(txDataBytes);
        tx.setTime(1627397685L);
        tx.setCoinData(coinDataBytes);
        tx.setRemark("0xa3a91082c9e3d4d8fc3544559b9c5d21e35f69d2".getBytes(StandardCharsets.UTF_8));
        System.out.println(tx.getHash().toHex());
    }

    @Test
    public void nonceTest() {
        RpcResult result = JsonRpcUtil.request("https://public.nerve.network/", "getAccountBalance", List.of(9, 9, 1, "NERVEepb6nsuYD48jW2Hq6W9ob1aTpZ3LiNGvk"));
        System.out.println(result);
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
        List<String[]> proposalTxHashList = new ArrayList<>();
        proposalTxHashList.add(new String[]{"Multi_chain_routing_bch_remove_heco",     "9680e3859dbaeaa95a2ccb8d942c9c49290e580fc7f88237691def87c241f52e"});
        proposalTxHashList.add(new String[]{"Multi_chain_routing_bch_remove_okx",     "5bd854b462d0cca1103efce042d60553405b538bc0fcdbabf86a3254f20b6977"});
        //proposalTxHashList.add(new String[]{"Whitelist",     "7d98a8f87cc293fcace1ec4685e8b9bb8e772e8ec981e8b710f7f6dac449e392"});
        //proposalTxHashList.add(new String[]{"Multi chain routing usdc remove oktc",     "f25ccbf66e34f8a72d3a2268b628e24f9ed5889f1039a6b9b78171f03f3858da"});
        //proposalTxHashList.add(new String[]{"Multi chain routing usdt remove oktc",     "f307e85c5a35f9125ff3a0dc271bf247c4daaf88e372652e18d15a4d729aeaab"});
        //proposalTxHashList.add(new String[]{"Multi chain routing eth add blast",     "682d1e8a0b5347af6f9ffabbcde52836527f1346ca3c2a131d4ca054a9e15de5"});

        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"NERVEepb6nsuYD48jW2Hq6W9ob1aTpZ3LiNGvk", "02376148f0332ca5bafc89f55777308f0d042290222fc0826ab16f40e2d39d17ba,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});
        list.add(new String[]{"NERVEepb6gVC8TBCioYHK6PPLKYk9aoXH8eGbV", "02212597815691c838d864c65fb09ba6af5c191ff795bcf0320cebda33dd3c62ae,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});
        list.add(new String[]{"NERVEepb6bT6dAfEtY9Z4c38Khawdr4LKZNais", "027d1ae118dd1d5ab0c416dc78ee0268d38f3a8c8cee2641a03f1d851b1c8405b9,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});
        list.add(new String[]{"NERVEepb6eKxoK6ZmKhhDK4oZkuXnUMxZfCta4", "035b6fef36e032382a06ededf20ec7c522215cb114e46e6c828617680d7d376b00,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});
        list.add(new String[]{"NERVEepb6exLKu4eJkHCekXicn7YpDa3hzHGvt", "02aeddccc442bb3b4cd1a50efbf41aa4681d213b998e68295675e9dd149cdb72b4,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});
        //list.add(new String[]{"NERVEepb6bt22V1LgaWavLNPEyTKbayCD58xo4", "03501d23db2f62863d89631168d427d3cc5f8b4a28fbb643e26695ae6cd5fdcc4b,03d621e65654ff522fa0121b45b9f137e78f0ca27380167b5d0373ec1820e5e9e2,03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4,031e919d04934d4c5018b00a8d6c8964c76281c39b3b580b6d70aa813296c9cfa6,022ed52fef6356f14bd28f4f47b410cd12545a0634a90531aa902316beefcb9c38"});

        Map<String, String> nonceMap = new HashMap<>();
        for (String[] proposalInfo : proposalTxHashList) {
            String chainName = proposalInfo[0];
            String proposalHash = proposalInfo[1];
            NulsHash proposalTxHash = NulsHash.fromHex(proposalHash);
            StringBuilder sb = new StringBuilder();
            for (String[] multiAddress : list) {
                String address = multiAddress[0];
                String pubkeys = multiAddress[1];
                List<byte[]> pubKeyList = new ArrayList<>();
                String arr[] = pubkeys.split(",");
                for(String pub:arr){
                    pubKeyList.add(HexUtil.decode(pub));
                }
                String nonce = nonceMap.get(address);
                if (StringUtils.isBlank(nonce)) {
                    RpcResult result = JsonRpcUtil.request("https://public.nerve.network/", "getAccountBalance", List.of(9, 9, 1, address));
                    nonce = ((Map)result.getResult()).get("nonce").toString();
                }

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
                String hash = tx.getHash().toHex();
                nonce = hash.substring(hash.length() - 16, hash.length());
                nonceMap.put(address, nonce);
                String txInfo = String.format("Initiate Chain: %s, Multiple signed addresses: %s, transactionhash: %s, nextNonce: %s, transactionHex: %s", chainName, address, hash, nonce, HexUtil.encode(tx.serialize()));
                sb.append(txInfo).append("\n");
                System.out.println(txInfo);
                System.out.println();
            }
            IoUtils.writeString(new File("/Users/pierreluo/IdeaProjects/nerve-network/nerve/converter/converter-main/src/test/resources/proposal/" + chainName + ".txt"), sb.toString(), StandardCharsets.UTF_8.name());
        }

    }
}
