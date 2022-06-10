/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.db;

import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.storage.impl.EthUnconfirmedTxStorageServiceImpl;
import network.nerve.converter.model.po.StringListPo;
import network.nerve.converter.utils.ConverterDBUtil;
import network.nerve.converter.utils.LoggerUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
public class DBSaveTest {

    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        EthContext.setLogger(LoggerUtil.LOG);
        RocksDBService.init("./data/converter");
        RocksDBService.createTable(EthDBConstant.DB_ETH);
    }

    @Test
    public void testDBModelWrapper() {
        StringListPo po = new StringListPo();
        List<String> list = new ArrayList<>();
        list.add(null);
        po.setCollection(list);
        byte[] bytes = ConverterDBUtil.getModelSerialize(po);
        StringListPo model = ConverterDBUtil.getModel(bytes, StringListPo.class);
        List<String> collection = model.getCollection();
        Set<String> set = new HashSet<>(collection);
        System.out.println(set);
    }

    @Test
    public void ethUnconfirmedTxStorageServiceTest() throws Exception {
        ethUnconfirmedTxStorageService = new EthUnconfirmedTxStorageServiceImpl();
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        po.setTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a551bbaa80");
        po.setFrom("ababab");
        po.setTo("aeeaea");
        ethUnconfirmedTxStorageService.save(po);
        po = new EthUnconfirmedTxPo();
        po.setTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a5aaaaaaaa");
        po.setFrom("ababab");
        po.setTo("aeeaea");
        ethUnconfirmedTxStorageService.save(po);
        po = new EthUnconfirmedTxPo();
        po.setTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a5bbbbbbbb");
        po.setFrom("ababab");
        po.setTo("aeeaea");
        ethUnconfirmedTxStorageService.save(po);

        ethUnconfirmedTxStorageService.deleteByTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a5aaaaaaaa");
        ethUnconfirmedTxStorageService.deleteByTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a5bbbbbbbb");
        ethUnconfirmedTxStorageService.deleteByTxHash("0x2fa5997114ccac0064fb4d2e411a5343021d02bfca8146a74515d8a551bbaa80");
        List<EthUnconfirmedTxPo> list = ethUnconfirmedTxStorageService.findAll();
        System.out.println(list.size());
    }
}
