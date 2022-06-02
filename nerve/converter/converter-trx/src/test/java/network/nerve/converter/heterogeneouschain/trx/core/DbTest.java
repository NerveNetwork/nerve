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
package network.nerve.converter.heterogeneouschain.trx.core;

import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.HtgBlockHeaderStorageServiceImpl;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxDBConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author: PierreLuo
 * @date: 2022/3/3
 */
public class DbTest {

    static HtgContext htContext = new TrxContext();

    @BeforeClass
    public static void before() {
        Log.info("init");
        htContext.setLogger(Log.BASIC_LOGGER);
        RocksDBService.init("/Users/pierreluo/Nuls/NERVE/data_converter/");
        System.out.println();
    }

    @Test
    public void testBlockDB() throws Exception {
        HtgBlockHeaderStorageServiceImpl blockDB = new HtgBlockHeaderStorageServiceImpl(htContext, TrxDBConstant.DB_TRX);
        System.out.println(JSONUtils.obj2PrettyJson(blockDB.findLatest()));
    }
}
