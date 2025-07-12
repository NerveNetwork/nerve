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
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.txdata.GeneralBusTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Additional withdrawal fee transaction
 *
 * @author: Loki
 * @date: 2020/9/27
 */
@Component("CvGeneralBusV1")
public class CvGeneralBusProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;


    @Override
    public int getType() {
        return TxType.GENERAL_BUS;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Map<String, Object> result = null;
        if (!converterCoreApi.isProtocol36()) {
            result = new HashMap<>();
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
            return result;
        }
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            for (Transaction tx : txs) {
                if (converterCoreApi.isProtocol42()) {
                    // Signature verification(Seed Virtual Bank)
                    ConverterSignValidUtil.validateSeedNodeSign(chain, tx);
                }

                String hash = tx.getHash().toHex();
                GeneralBusTxData txData = ConverterUtil.getInstance(tx.getTxData(), GeneralBusTxData.class);
                int dataType = txData.getType();
                if (dataType == 1) {
                    // check unlock utxo from
                    byte[] data = txData.getData();
                    if (data == null || data.length < NulsHash.HASH_LENGTH + 1) {
                        failsList.add(tx);
                        // Withdrawal transactionshash
                        errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                        log.error("Original withdrawal transaction with additional transaction fees hash Not present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                        continue;
                    }
                    String dataHex = HexUtil.encode(data);
                    String basicTxHash = dataHex.substring(0, 64);
                    boolean forceUnlock = Integer.parseInt(dataHex.substring(64, 66)) == 1;
                    int htgChainId = 0;
                    if (dataHex.length() >= 68) {
                        htgChainId = Integer.parseInt(dataHex.substring(66, 68), 16);
                    }
                    Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                    if (null == basicTx) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                        chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                        continue;
                    }
                    if (basicTx.getType() == TxType.WITHDRAWAL) {
                        WithdrawalTxData basicTxData = ConverterUtil.getInstance(basicTx.getTxData(), WithdrawalTxData.class);
                        htgChainId = basicTxData.getHeterogeneousChainId();
                    }
                    if (htgChainId < 200) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                        chain.getLogger().error("The original HETEROGENEOUS_CHAINID_ERROR , hash:{}", basicTxHash);
                        continue;
                    }
                    if (!forceUnlock) {
                        ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                        if (null != po) {
                            // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                            failsList.add(tx);
                            // NerveWithdrawal transaction does not exist
                            errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                            chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                            continue;
                        }
                    }
                } else if (dataType == 2) {
                    if (!converterCoreApi.isProtocol42()) {
                        failsList.add(tx);
                        // Withdrawal transactionshash
                        errorCode = ConverterErrorCode.PROTOCOL_NOT_REACHED.getCode();
                        log.error("On-chain data protocol not reached 42");
                        continue;
                    }
                    // check unlock utxo from
                    byte[] data = txData.getData();
                    if (data == null || data.length < NulsHash.HASH_LENGTH) {
                        failsList.add(tx);
                        // Withdrawal transactionshash
                        errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                        log.error("Original withdrawal transaction with additional transaction fees hash Not present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                        continue;
                    }
                    String dataHex = HexUtil.encode(data);
                    String basicTxHash = dataHex.substring(0, 64);
                    Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                    if (null == basicTx) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                        chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                        continue;
                    }
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
                GeneralBusTxData txData = ConverterUtil.getInstance(tx.getTxData(), GeneralBusTxData.class);
                if (txData.getType() == 1) {
                    // check unlock utxo from
                    byte[] data = txData.getData();
                    String dataHex = HexUtil.encode(data);
                    String basicTxHash = dataHex.substring(0, 64);
                    boolean forceUnlock = Integer.parseInt(dataHex.substring(64, 66)) == 1;
                    int htgChainId = 0;
                    if (dataHex.length() >= 68) {
                        htgChainId = Integer.parseInt(dataHex.substring(66, 68), 16);
                    }
                    Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                    if (basicTx.getType() == TxType.WITHDRAWAL) {
                        WithdrawalTxData basicTxData = ConverterUtil.getInstance(basicTx.getTxData(), WithdrawalTxData.class);
                        htgChainId = basicTxData.getHeterogeneousChainId();
                    }
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(htgChainId);
                    if (docking != null) {
                        docking.getBitCoinApi().checkLockedUTXO(basicTxHash, List.of());
                    }
                } else if (txData.getType() == 2) {
                    byte[] data = txData.getData();
                    String dataHex = HexUtil.encode(data);
                    String basicTxHash = dataHex.substring(0, 64);
                    converterCoreApi.addSkippedTransaction(basicTxHash);
                }
                chain.getLogger().info("[commit] GeneralBus transaction hash:{}, txData:{}", tx.getHash().toHex(), txData);
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        return true;
    }


}
