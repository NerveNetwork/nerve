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

package network.nerve.converter.core.thread.task;

import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.converter.message.helper.ByzantineTransactionHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.PendingCheckTx;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.storage.TxStorageService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author: Loki
 * @date: 2020/4/29
 */
public class TxCheckAndCreateProcessTask implements Runnable {

    private Chain chain;

    public TxCheckAndCreateProcessTask(Chain chain) {
        this.chain = chain;
    }
    private ByzantineTransactionHelper byzantineTransactionHelper = SpringLiteContext.getBean(ByzantineTransactionHelper.class);
    private TxStorageService txStorageService = SpringLiteContext.getBean(TxStorageService.class);
    @Override
    public void run() {
        try {
            Set<PendingCheckTx> toRemoveSet = new HashSet<>();
            List<PendingCheckTx> list = new ArrayList<>(chain.getPendingCheckTxSet());
            for(PendingCheckTx checkTx : list){
                // 扫描交易是否存在, 如果存在就移除checkTx
                TransactionPO txPO = txStorageService.get(chain, checkTx.getHash());
                if(null != txPO){
                    chain.getLogger().info("[PendingCheck] 异构链已解析到交易 hash:{}, type:{}",
                            checkTx.getHash().toHex(), checkTx.getType());
                    toRemoveSet.add(checkTx);
                    continue;
                }else if(null == txPO && checkTx.getCheckTimes() <= 0){
                    // 检查次数耗尽, 还是没有交易 则执行创建
                    try {
                        boolean rs = byzantineTransactionHelper.genByzantineTransaction(chain,
                                checkTx.getHash().toHex(),
                                checkTx.getType(),
                                checkTx.getOriginalHash(),
                                checkTx.getHeterogeneousHashList());
                        chain.getLogger().info("[PendingCheck] 异构链没有解析到交易, 调用异构链组件创建交易 hash:{}, type:{}, rs:{}",
                                checkTx.getHash().toHex(), checkTx.getType(), rs);
                        toRemoveSet.add(checkTx);
                        continue;
                    } catch (Exception e) {
                        chain.getLogger().error(e);
                        toRemoveSet.add(checkTx);
                        continue;
                    }
                }
                chain.getLogger().debug("[PendingCheck] 异构链组件没有获取到该交易 hash:{}, type:{}",
                        checkTx.getHash().toHex(), checkTx.getType());
                checkTx.setCheckTimes(checkTx.getCheckTimes() - 1);
            }
            chain.getPendingCheckTxSet().removeAll(toRemoveSet);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }
}
