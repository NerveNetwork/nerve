package io.nuls.account.service;

import io.nuls.account.AccountBootstrap;
import io.nuls.account.config.NulsConfig;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.bo.tx.txdata.Alias;
import io.nuls.account.storage.AliasStorageServiceTest;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.core.inteceptor.ModularServiceMethodInterceptor;
import io.nuls.core.core.ioc.SpringLiteContext;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author: qinyifeng
 * @description:
 * @date: 2018/11/8
 */
public class AliasServiceTest {

    protected static AccountService accountService;
    protected static AliasService aliasService;
    protected int chainId = 2;
    protected String password = "a2678";

    @BeforeClass
    public static void beforeTest() {
        //Initialize configuration
        SpringLiteContext.init("io.nuls.account", new ModularServiceMethodInterceptor());
        AccountBootstrap accountBootstrap = SpringLiteContext.getBean(AccountBootstrap.class);
        //Initialize configuration
        accountBootstrap.initCfg();        //Read the configuration file, store the data in the root directory, initialize and open all table connections in that directory, and place them in the cache
        RocksDBService.init(NulsConfig.DATA_PATH);
        //Start time synchronization thread
//        TimeService.getInstance().start();
        accountService = SpringLiteContext.getBean(AccountService.class);
        aliasService = SpringLiteContext.getBean(AliasService.class);
    }

    /**
     * save the alias
     * <p>
     * Nov.10th 2018
     *
     * @auther EdwardChan
     */
    @Test
    public void saveAliasTest() throws Exception {
        // create account
        Alias alias = AliasStorageServiceTest.createAlias();
        boolean result = aliasService.aliasTxCommit(chainId,alias);
        assertTrue(result);
        Account account = accountService.getAccount(chainId, AddressTool.getStringAddressByBytes(alias.getAddress()));
        assertNotNull(account);
        assertEquals(account.getAlias(),alias.getAlias());
    }
}
