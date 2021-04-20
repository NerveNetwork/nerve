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
package network.nerve.converter.heterogeneouschain.ht.dbtest;

import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.ht.constant.HtDBConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author: PierreLuo
 * @date: 2021-03-25
 */
public class DbTest {

    HtgContext htContext = new HtContext();

    @BeforeClass
    public static void before() {
        Log.info("init");
        HtContext.logger = Log.BASIC_LOGGER;
        RocksDBService.init("/Users/pierreluo/IdeaProjects/nerve-network/nerve/converter/converter-htn/src/test/resources/data/converter/");
    }

    @Test
    public void testAccountDB() throws Exception {
        HtgAccountStorageServiceImpl accoutDb = new HtgAccountStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        HtgAccount account = accoutDb.findByAddress("0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03");
        System.out.println(JSONUtils.obj2PrettyJson(account));

        HtgAccount account1 = new HtgAccount();
        account1.setAddress("0x123");
        account1.setOrder(2);
        account1.setCompressedPublicKey("0xabcd");
        accoutDb.save(account1);
        HtgAccount _account = accoutDb.findByAddress("0x123");
        System.out.println(JSONUtils.obj2PrettyJson(_account));
    }

    @Test
    public void testBlockDB() throws Exception {
        HtgBlockHeaderStorageServiceImpl blockDB = new HtgBlockHeaderStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(blockDB.findLatest()));
    }

    @Test
    public void testERC20DB() throws Exception {
        HtgERC20StorageServiceImpl _erc20DB = new HtgERC20StorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(_erc20DB.getMaxAssetId());
        System.out.println(JSONUtils.obj2PrettyJson(_erc20DB.findByAddress("0x04f535663110a392a6504839beed34e019fdb4e0")));
    }

    @Test
    public void testMultySignAddressDB() throws Exception {
        HtgMultiSignAddressHistoryStorageServiceImpl multyDB = new HtgMultiSignAddressHistoryStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(multyDB.findAll()));
    }

    @Test
    public void testTxInvokeInfoDB() throws Exception {
        HtgTxInvokeInfoStorageServiceImpl txInvokeInfoDB = new HtgTxInvokeInfoStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(txInvokeInfoDB.findAllWaitingTxPo()));
    }

    @Test
    public void testTxRelationDB() throws Exception {
        HtgTxRelationStorageServiceImpl txRelationDB = new HtgTxRelationStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(txRelationDB.findEthSendTxPo("0xfac20bde3a1134006504180bc76efece3924dc58b11e0d1bb491c42ce3a394da")));
    }

    @Test
    public void testTxDB() throws Exception {
        HtgTxStorageServiceImpl txDB = new HtgTxStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(txDB.findByTxHash("0xfac20bde3a1134006504180bc76efece3924dc58b11e0d1bb491c42ce3a394da")));
    }

    @Test
    public void testUnconfirmedTxDB() throws Exception {
        HtgUnconfirmedTxStorageServiceImpl unconfirmedTxDB = new HtgUnconfirmedTxStorageServiceImpl(htContext, HtDBConstant.DB_HT);
        System.out.println(JSONUtils.obj2PrettyJson(unconfirmedTxDB.findAll()));
    }
}
