package network.nerve.converter.utils;

import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.TxSubsequentProcessKeyListPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VirtualBankUtilTest {

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
}