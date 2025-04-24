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

package network.nerve.converter.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.*;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component("WithdrawalUTXOsV1")
public class WithdrawalUTXOsProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.WITHDRAWAL_UTXO;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;

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
            OUT:
            for (Transaction tx : txs) {
                WithdrawalUTXOTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalUTXOTxData.class);
                String nerveTxHash = txData.getNerveTxHash();
                // deal dirty data on testnet
                /*if ("2f7253ca19967afb90fdd9644e302241688e24117deba03dbba0fcec5b46eee0".equals(nerveTxHash)) {
                    continue;
                }*/
                if (!setDuplicate.add(nerveTxHash)) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHtgChainId());
                // check if rebuild utxo data
                boolean rebuild = false;
                WithdrawalUTXOTxData utxoTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
                if (utxoTxData != null) {
                    rebuild = utxoTxData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD;
                    if (!rebuild) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                        log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                        continue;
                    }
                }
                List<UTXOData> utxoDataList = txData.getUtxoDataList();
                if (!HtgUtil.isEmptyList(utxoDataList)) {
                    for (UTXOData utxo : utxoDataList) {
                        if (!setDuplicate.add(utxo.getTxid() + "-" + utxo.getVout())) {
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                            log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                            continue OUT;
                        }
                        String nerveHash = docking.getBitCoinApi().getNerveHashByLockedUTXO(utxo.getTxid(), utxo.getVout());
                        boolean lockedUTXO = nerveHash != null;
                        if (lockedUTXO) {
                            if (!rebuild || !nerveHash.equals(nerveTxHash)) {
                                failsList.add(tx);
                                errorCode = ConverterErrorCode.WITHDRAWAL_UTXO_LOCKED.getCode();
                                log.error(ConverterErrorCode.WITHDRAWAL_UTXO_LOCKED.getMsg());
                                continue OUT;
                            }
                        }
                    }
                }
                // Signature verification
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
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
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    private boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                WithdrawalUTXOTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalUTXOTxData.class);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHtgChainId());
                docking.getBitCoinApi().saveWithdrawalUTXOs(txData);
                chain.getLogger().info("[commit] withdrawal UTXOs transactions hash: {}, withdrawal hash: {}", tx.getHash().toHex(), txData.getNerveTxHash());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

}
