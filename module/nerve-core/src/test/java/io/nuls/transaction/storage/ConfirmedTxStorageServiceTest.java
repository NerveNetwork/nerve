package io.nuls.transaction.storage;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.model.StringUtils;
import io.nuls.transaction.TestConstant;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ConfirmedTxStorageServiceTest {

    protected static ConfirmedTxStorageService confirmedTxStorageService;
    protected int chainId = 2;
    protected long height = 100;

    @BeforeClass
    public static void beforeTest() throws Exception {
        //Initialize database configuration file
        //new TransactionBootstrap().initDB();
        //Initialize Context
        SpringLiteContext.init(TestConstant.CONTEXT_PATH);
        confirmedTxStorageService = SpringLiteContext.getBean(ConfirmedTxStorageService.class);
        //Start Chain
        SpringLiteContext.getBean(ChainManager.class).runChain();
    }

    @Test
    public void saveTx() throws Exception {
        Transaction tx = TestConstant.getTransaction2();
        boolean result = confirmedTxStorageService.saveTx(chainId, new TransactionConfirmedPO(tx, 1, (byte)1));
        Assert.assertTrue(result);
    }

    @Test
    public void saveTxList() throws Exception {
        List<TransactionConfirmedPO> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Transaction tx = TestConstant.getTransaction2();
            tx.setRemark(StringUtils.bytes("tx remark" + i));
            list.add(new TransactionConfirmedPO(tx, 1, (byte)1));
        }
        boolean result = confirmedTxStorageService.saveTxList(chainId, list);
        Assert.assertTrue(result);
    }

    /**
     * Batch save of test transactions、Batch Query、Batch deletion、Single query
     *
     * @throws Exception
     */
    @Test
    public void getTxList() throws Exception {
        //test saveTxList
        List<TransactionConfirmedPO> list = new ArrayList<>();
        List<String> hashList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Transaction tx = TestConstant.getTransaction2();
            tx.setRemark(StringUtils.bytes("tx remark" + i));
            list.add(new TransactionConfirmedPO(tx, 1, (byte)1));
            hashList.add(tx.getHash().toHex());
        }
        confirmedTxStorageService.saveTxList(chainId, list);
    }

}
