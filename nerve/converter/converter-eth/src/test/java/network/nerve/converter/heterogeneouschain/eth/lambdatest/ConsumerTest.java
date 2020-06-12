/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.lambdatest;

import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * @author: Mimi
 * @date: 2020-04-22
 */
public class ConsumerTest {

    private Object version = new Object();
    private int testVersion = 0;
    EthUnconfirmedTxPo po;
    String to;

    @Before
    public void before() {
        po = new EthUnconfirmedTxPo();
        po.setTxHash("0x12abcd..........");
        po.setNerveTxHash("0xnerverhash,,,,,,,,,,");
        to = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    }

    @Test
    public void testSameVersion() throws Exception {
        testVersion = 0;
        this.update(po, update -> update.setTo(to));
    }

    @Test
    public void testNextVersion() throws Exception {
        testVersion = 1;
        this.update(po, update -> update.setTo(to));
    }

    private int update(EthUnconfirmedTxPo po, Consumer<EthUnconfirmedTxPo> update) throws Exception {
        synchronized (version) {
            EthUnconfirmedTxPo current = this.findByTxHash(po.getTxHash());
            if (current == null) {
                return this.save(po);
            }
            if (current.getDbVersion() == po.getDbVersion()) {
                po.setDbVersion(po.getDbVersion() + 1);
                return this.save(po);
            } else {
                update.accept(current);
                current.setDbVersion(current.getDbVersion() + 1);
                return this.save(current);
            }
        }
    }

    private int save(EthUnconfirmedTxPo po) {
        System.out.println(po.toString());
        return 0;
    }

    private EthUnconfirmedTxPo findByTxHash(String txHash) {
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        po.setDbVersion(testVersion);
        return po;
    }
}
