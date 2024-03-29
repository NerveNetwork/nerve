/*
 * MIT License
 *
 * Copyright (c) 2018-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.transaction.task;

import io.nuls.common.NerveCoreConfig;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.po.TransactionUnconfirmedPO;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.UnconfirmedTxStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.nuls.transaction.constant.TxContext.UNCONFIRMED_TX_EXPIRE_SEC;

/**
 * Unconfirmed transaction clearance mechanism
 */
public class ClearUnconfirmedTxProcessTask implements Runnable {

    private PackablePool packablePool = SpringLiteContext.getBean(PackablePool.class);
    private TxService txService = SpringLiteContext.getBean(TxService.class);
    private UnconfirmedTxStorageService unconfirmedTxStorageService = SpringLiteContext.getBean(UnconfirmedTxStorageService.class);
    private NerveCoreConfig txConfig = SpringLiteContext.getBean(NerveCoreConfig.class);
    private Chain chain;

    public ClearUnconfirmedTxProcessTask(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        try {
            doTask(chain);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    private void doTask(Chain chain) {
        List<byte[]> txKeyList = unconfirmedTxStorageService.getAllTxkeyList(chain.getChainId());
        if (txKeyList == null || txKeyList.size() == 0) {
            return;
        }
        chain.getLogger().info("[UnconfirmedTxProcessTask] unconfirmedTx count: {}", txKeyList.size());
        int count = processUnconfirmedTxs(txKeyList);
        chain.getLogger().info("[UnconfirmedTxProcessTask] Clean expire count: {}", count);
    }

    private boolean processTx(Chain chain, Transaction tx) {
        try {
            txService.clearInvalidTx(chain, tx, false);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }

    /**
     * Filter transactions that expire within a specified time frame
     *
     * @param txKeyList
     * @return expireTxList
     */
    private int processUnconfirmedTxs(List<byte[]> txKeyList) {
        int unconfirmedTxsCount = 0;
        List<byte[]> queryList = new ArrayList<>();
        for (int i = 0; i < txKeyList.size(); i++) {
            queryList.add(txKeyList.get(i));
            if (queryList.size() == 10000) {
                unconfirmedTxsCount += processExpireTxs(queryList);
                queryList.clear();
            }
        }
        if(!queryList.isEmpty()){
            unconfirmedTxsCount += processExpireTxs(queryList);
        }
        return unconfirmedTxsCount;
    }

    public int processExpireTxs(List<byte[]> queryList){
        //Obtain unconfirmed transactions
        List<TransactionUnconfirmedPO> list = unconfirmedTxStorageService.getTransactionUnconfirmedPOList(chain.getChainId(), queryList);

        chain.getLogger().info("[UnconfirmedTxProcessTask] The number of unconfirmed transactions to be processed this time:{}", list.size());
        //Calculate unconfirmed transactions that have exceeded the time limit
        List<Transaction> expireTxList = getExpireTxList(list);
        int count = 0;
        Transaction tx;
        for (int i = 0; i < expireTxList.size(); i++) {
            tx = expireTxList.get(i);
            //If the unconfirmed transaction is not in the pending package pool, it is considered expired dirty data and needs to be cleaned up
            if (!packablePool.exist(chain, tx)) {
                chain.getLogger().info("[UnconfirmedTxProcessTask] --- The transactions that need to be cleared this timehash:{}", tx.getHash().toHex());
                processTx(chain, tx);
                count++;
            }
        }
        return count;
    }

    /**
     * Filter transactions that expire within a specified time frame
     *
     * @param txPOList
     * @return expireTxList
     */
    private List<Transaction> getExpireTxList(List<TransactionUnconfirmedPO> txPOList) {
        List<Transaction> expireTxList = new ArrayList<>();
        long currentTimeSeconds = NulsDateUtils.getCurrentTimeSeconds();
        //Filter transactions that expire within a specified time frame
        List<TransactionUnconfirmedPO> expireTxPOList = txPOList.stream().filter(txPo -> currentTimeSeconds - UNCONFIRMED_TX_EXPIRE_SEC > txPo.getCreateTime()).collect(Collectors.toList());
        expireTxPOList.forEach(txPo -> expireTxList.add(txPo.getTx()));
        chain.getLogger().info("[UnconfirmedTxProcessTask] Expiration time exceeded after this processing{}Seconds of transactions:{}", UNCONFIRMED_TX_EXPIRE_SEC, expireTxList.size());
        return expireTxList;
    }

}
