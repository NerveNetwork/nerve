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
        NulsHash proposalTxHash = NulsHash.fromHex("dd1f44aece842147a001afb62e2fec6f73b443c539c84dabe9bf9f5e34fc494d");
        String address = "TNVTdTSPyT1GGPrbahr9qo7S87dMBatx9NHtP";
        VoteProposalTxData voteProposalTxData = new VoteProposalTxData(proposalTxHash, (byte)1);
        Transaction tx = new Transaction(TxType.VOTE_PROPOSAL);
        tx.setTxData(voteProposalTxData.serialize());
        tx.setTime(System.currentTimeMillis() / 1000);

        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                5,
                1,
                TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES,
                HexUtil.decode("9d848f591514b4ec"),
                (byte) 0);
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);

        List<CoinTo> tos = new ArrayList<>();
        CoinTo coinTo = new CoinTo(
                AddressTool.getAddress(address),
                5,
                1,
                BigInteger.ZERO);
        tos.add(coinTo);

        coinData.setFrom(froms);
        coinData.setTo(tos);
        tx.setCoinData(coinData.serialize());

        MultiSignTxSignature signature = new MultiSignTxSignature();
        signature.setM((byte)2);
        List<byte[]> pubKeyList = new ArrayList<>();
        // 0224d86a584324fc8e92c6dba19c08926a7af77df884deec0d1c3b879a8f50720f,
        // 02362c64e15ab653132ec753e4a8c181ef720ec927466a09417a07877824781f57,
        // 02962c7942851fa2c937be788a18693885276e3d9688b5997d9f02ebf2fef218db
        pubKeyList.add(HexUtil.decode("0224d86a584324fc8e92c6dba19c08926a7af77df884deec0d1c3b879a8f50720f"));
        pubKeyList.add(HexUtil.decode("02362c64e15ab653132ec753e4a8c181ef720ec927466a09417a07877824781f57"));
        pubKeyList.add(HexUtil.decode("02962c7942851fa2c937be788a18693885276e3d9688b5997d9f02ebf2fef218db"));
        signature.setPubKeyList(pubKeyList);
        tx.setTransactionSignature(signature.serialize());
        System.out.println(HexUtil.encode(tx.serialize()));
    }
}