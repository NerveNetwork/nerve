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

package io.nuls.transaction.tx;

import io.nuls.common.ConfigBean;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.storage.ConfirmedTxStorageService;
import io.nuls.transaction.storage.impl.ConfirmedTxStorageServiceImpl;
import io.nuls.transaction.utils.TxUtil;
import org.junit.Before;

import java.util.List;

/**
 * Data analysis
 *
 * @author: Loki
 * @date: 2020/12/1
 */
public class DateAnalyse {

    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId));
    }

    public static void main(String[] args) throws Exception {
        testAnalyse();
    }

    // Search for transactions based on transaction address and asset datahash
    public static void testAnalyse() throws Exception {
        RocksDBService.init("data/transaction");
        ConfirmedTxStorageService cft = new ConfirmedTxStorageServiceImpl();
        List<TransactionConfirmedPO> allTxs = cft.getAllTxs(chainId);
        System.out.println(allTxs.size());
        System.out.println("start");
        for (TransactionConfirmedPO txPO : allTxs) {
            Transaction tx = txPO.getTx();
            //NERVEeTSPVPWYvNmNhhfuRFbCwra6bZsibRJJA
            CoinData coinData = TxUtil.getCoinData(tx);
            if(null == coinData){
                continue;
            }
            List<CoinTo> toList = coinData.getTo();
            for(CoinTo coinTo : toList){
                if("TNVTdTSPVPWYvNmNhhfuRFbCwra6bZsibRJJA".equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))){
                    if(coinTo.getAssetsChainId() == 2) {
                        System.out.println(tx.getHash().toHex());
                        System.out.println("type:" + tx.getType());
                        break;
                    }
                }
            }
        }
        System.out.println("end");

    }
}
