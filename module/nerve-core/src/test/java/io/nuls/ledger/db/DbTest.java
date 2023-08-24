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
package io.nuls.ledger.db;

import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author: PierreLuo
 * @date: 2022/3/3
 */
public class DbTest {


    @BeforeClass
    public static void before() {
        Log.info("init");
        RocksDBService.init("/Users/pierreluo/IdeaProjects/nerve-network/data-35446352-orgin/cross-chain/");
        System.out.println();
    }

    @Test
    public void testBlockDB() throws Exception {
        byte[] stream = RocksDBService.get("chain_block_height", ByteUtils.intToBytes(9));
        long height = ByteUtils.byteToLong(stream);
        System.out.println(height);
    }

    @Test
    public void testCrossChainDB() throws Exception {
        byte[] messageBytes = RocksDBService.get("registered_chain", NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN.getBytes());
        RegisteredChainMessage registeredChainMessage = new RegisteredChainMessage();
        registeredChainMessage.parse(messageBytes,0);
        System.out.println();
    }

    @Test
    public void updateBlockHeightDB() throws Exception {
        RocksDBService.put("chain_block_height", ByteUtils.intToBytes(9), ByteUtils.longToBytes(27208791L));
        byte[] stream = RocksDBService.get("chain_block_height", ByteUtils.intToBytes(9));
        long height = ByteUtils.byteToLong(stream);
        System.out.println(height);
    }
}
