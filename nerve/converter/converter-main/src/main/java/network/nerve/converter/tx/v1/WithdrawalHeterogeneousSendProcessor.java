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

package network.nerve.converter.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.txdata.WithdrawalHeterogeneousSendTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.*;

/**
 * @author: Loki
 * @date: 2020/9/27
 */
@Component("WithdrawalHeterogeneousSendV1")
public class WithdrawalHeterogeneousSendProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;

    @Override
    public int getType() {
        return TxType.WITHDRAWAL_HETEROGENEOUS_SEND;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            //Check for duplicate transactions within the block business
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                WithdrawalHeterogeneousSendTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalHeterogeneousSendTxData.class);
                String nerveTxHash = txData.getNerveTxHash();
                Transaction withdrawalTx = TransactionCall.getConfirmedTx(chain, nerveTxHash);
                if(null == withdrawalTx){
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("Chain withdrawal transaction does not exist txHash:{}, withdrawalTxHash:{}",
                            tx.getHash().toHex(), nerveTxHash);
                    continue;
                }
                if(!setDuplicate.add(nerveTxHash)){
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("Conflict in block transactions,Business duplication txHash:{}, withdrawalTxHash:{}",
                            tx.getHash().toHex(), nerveTxHash);
                    continue;
                }
                // Signature Byzantine Verification
                try {
                    ConverterSignValidUtil.validateVirtualBankSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            chain.getLogger().error(e);
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                WithdrawalHeterogeneousSendTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalHeterogeneousSendTxData.class);
                chain.getLogger().info("[commit] Withdrawal and publishing to heterogeneous chain network transactions hash:{}, withdrawalTxHash:{}, heterogeneousTxHash:{}",
                        tx.getHash().toHex(), txData.getNerveTxHash(), txData.getHeterogeneousTxHash());
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }
}
