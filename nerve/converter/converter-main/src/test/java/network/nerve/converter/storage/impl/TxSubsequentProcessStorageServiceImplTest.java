package network.nerve.converter.storage.impl;

import io.nuls.base.data.Transaction;
import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static network.nerve.converter.constant.ConverterDBConstant.DB_PENDING_PREFIX;

public class TxSubsequentProcessStorageServiceImplTest {

    static String filePath = "E:\\RocksDBTest";

    static int chainId = 5;
    static int assetId = 1;
    Chain chain;

    TxSubsequentProcessStorageService txSubsequentProcessStorageService = new TxSubsequentProcessStorageServiceImpl();

    @Test
    public void save() throws Exception {

        for(int i = 0; i< 10; i++) {
            String hex = "e5003e7fe85e00911ef715cec29207455a4f2c9b58864cc3797d5446a5574bad0d1c496ef9c19b07050001f88d93a52edc7437da5e2977d27681f0fb1e6bab0200e1f5050000000000000000000000000000000000000000000000000000000000e1f505000000000000000000000000000000000000000000000000000000001704000102dc3a715ee3d1faa7f81cdea0687292d40c189d058c0117050001f88d93a52edc7437da5e2977d27681f0fb1e6bab05000100a067f7050000000000000000000000000000000000000000000000000000000008bac1b7d8e684c077000117050001f88d93a52edc7437da5e2977d27681f0fb1e6bab0500010000e1f50500000000000000000000000000000000000000000000000000000000feffffffffffffff6a21025ffc3303fdf0e432b46c37a9c18e5e7feef00d68fef70029399c17dca34f117d473045022076d0cb8c87ca2a498d0fa64a0d77e5f0f0c037b0c5353183ac2e9eba7e55da95022100d9ab33b0ffd33c63a72c4026cfe50dbe3b3d2c9183cdd026cf5bdffb4f748f45";
            Transaction tx = ConverterUtil.getInstance(hex, Transaction.class);
            tx.setTime(10000+i);
            TxSubsequentProcessPO po = new TxSubsequentProcessPO();
            po.setTx(tx);
            boolean save = txSubsequentProcessStorageService.save(chain, po);
            System.out.println(save + " - " + (i+1));
        }
    }

    @Test
    public void findAll() {
        List<TxSubsequentProcessPO> all = txSubsequentProcessStorageService.findAll(chain);
        System.out.println(all.size());
    }

    @Before
    public void initTest() {
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId, "utf-8"));
        NulsLogger logger = LoggerBuilder.getLogger(ModuleE.CV.name, chainId);
        chain.setLogger(logger);
        try {
            RocksDBManager.init(filePath);
            if (existTable(DB_PENDING_PREFIX + chainId)) {
                return;
//                destroyTable(DB_PENDING_PREFIX);
            }

            RocksDBManager.createTable(DB_PENDING_PREFIX + chainId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean existTable(String table) {
        String[] tables = RocksDBManager.listTable();
        if (tables != null && Arrays.asList(tables).contains(table)) {
            return true;
        }
        return false;
    }

    public static boolean destroyTable(String table) throws Exception {
        return RocksDBManager.destroyTable(table);
    }
}
