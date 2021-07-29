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
package network.nerve.converter;

import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.HtgERC20StorageServiceImpl;
import org.junit.Test;

/**
 * @author: PierreLuo
 * @date: 2021/7/21
 */
public class DataTest {

    @Test
    public void test() throws Exception {
        RocksDBService.init("/Users/pierreluo/Nuls/NERVE/cv");
        BnbContext.setLogger(Log.BASIC_LOGGER);
        BnbContext context = new BnbContext();
        HtgERC20StorageServiceImpl service = new HtgERC20StorageServiceImpl(context, BnbDBConstant.DB_BNB);
        HtgERC20Po po = service.findByAddress("0x72755f739b56ef98bda25e2622c63add229dec01");
        System.out.println(JSONUtils.obj2PrettyJson(po));
    }
}
