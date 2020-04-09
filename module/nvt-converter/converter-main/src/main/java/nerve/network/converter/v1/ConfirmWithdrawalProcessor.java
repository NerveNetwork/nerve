/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.po.ConfirmWithdrawalPO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.txdata.ConfirmWithdrawalTxData;
import nerve.network.converter.model.txdata.WithdrawalTxData;
import nerve.network.converter.rpc.call.TransactionCall;
import nerve.network.converter.storage.ConfirmWithdrawalStorageService;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.utils.ConverterSignValidUtil;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.math.BigInteger;
import java.util.*;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("ConfirmWithdrawalV1")
public class ConfirmWithdrawalProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.CONFIRM_WITHDRAWAL;
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
            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                String originalHash = txData.getWithdrawalTxHash().toHex();
                if(setDuplicate.contains(originalHash)){
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.BLOCK_TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.BLOCK_TX_DUPLICATION.getMsg());
                    continue;
                }
                // 判断该提现交易一否已经有对应的确认提现交易
                ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
                if (null != po) {
                    // 说明该提现交易 已经发出过确认提现交易,本次交易为重复的确认提现交易
                    failsList.add(tx);
                    // Nerve提现交易不存在
                    errorCode = ConverterErrorCode.CFM_IS_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.CFM_IS_DUPLICATION.getMsg());
                    continue;
                }
                //获取提现交易
                Transaction withdrawalTx = TransactionCall.getConfirmedTx(chain, txData.getWithdrawalTxHash());
                if (null == withdrawalTx) {
                    failsList.add(tx);
                    // Nerve提现交易不存在
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
//                CoinTo feeTo = null;
                CoinTo withdrawalTo = null;
                for (CoinTo coinTo : coinData.getTo()) {
                    // 链内手续费
                   /* if (coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinTo.getAssetsId() == chain.getConfig().getAssetId()) {
                        feeTo = coinTo;
                    }*/
                    //提现资产
                    if (coinTo.getAssetsChainId() != chainId) {
                        withdrawalTo = coinTo;
                    }
                }
                //获取异构交易信息
                IHeterogeneousChainDocking HeterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(withdrawalTo.getAssetsChainId());
                if (null == HeterogeneousInterface) {
                    failsList.add(tx);
                    // 提现交易不存在
                    errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getMsg());
                    continue;
                }
                HeterogeneousTransactionInfo info = HeterogeneousInterface.getWithdrawTransaction(txData.getHeterogeneousTxHash());
                if (null == info) {
                    failsList.add(tx);
                    // 异构链提现交易不存在
                    errorCode = ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getMsg());
                    continue;
                }
                // 验证 异构链签名列表
                List<HeterogeneousAddress> listDistributionFee = txData.getListDistributionFee();
                if (null == listDistributionFee || listDistributionFee.isEmpty()) {
                    failsList.add(tx);
                    // 异构链签名列表是空的
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_LIST_EMPTY.getCode();
                    log.error(ConverterErrorCode.DISTRIBUTION_ADDRESS_LIST_EMPTY.getMsg());
                    continue;
                }
                List<HeterogeneousAddress> listSigners = info.getSigners();
                if (!listEquals(listDistributionFee, listSigners)) {
                    failsList.add(tx);
                    // 异构链签名列表不匹配
                    errorCode = ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH.getMsg());
                    continue;
                }

                WithdrawalTxData withdrawalTxData = ConverterUtil.getInstance(withdrawalTx.getTxData(), WithdrawalTxData.class);
                if (!withdrawalTxData.getHeterogeneousAddress().equals(info.getTo())) {
                    failsList.add(tx);
                    // 提现交易确认交易中到账地址与异构确认交易到账地址数据不匹配
                    errorCode = ConverterErrorCode.CFM_WITHDRAWAL_ARRIVE_ADDRESS_MISMATCH.getCode();
                    log.error(ConverterErrorCode.CFM_WITHDRAWAL_ARRIVE_ADDRESS_MISMATCH.getMsg());
                    continue;
                }

                if (txData.getHeterogeneousHeight() != info.getBlockHeight()) {
                    failsList.add(tx);
                    // 提现交易确认交易中height与异构确认交易height数据不匹配
                    errorCode = ConverterErrorCode.CFM_WITHDRAWAL_HEIGHT_MISMATCH.getCode();
                    log.error(ConverterErrorCode.CFM_WITHDRAWAL_HEIGHT_MISMATCH.getMsg());
                    continue;
                }
                // todo 提现金额匹配涉及手续费问题，需异构组件确认
                BigInteger fee = new BigInteger("0");
                BigInteger arrivedAmount = withdrawalTo.getAmount().subtract(fee);
                if (arrivedAmount.compareTo(info.getValue()) != 0) {
                    failsList.add(tx);
                    // 提现交易确认交易中金额与异构确认交易金额数据不匹配
                    errorCode = ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH.getCode();
                    log.error(ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH.getMsg());
                    continue;
                }
                // 签名验证
                try {
                    ConverterSignValidUtil.validateSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }

                setDuplicate.add(originalHash);
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

    private boolean listEquals(List<HeterogeneousAddress> listDistributionFee, List<HeterogeneousAddress> listSigners) {
        if (listSigners.size() != listDistributionFee.size()) {
            return false;
        }
        for (HeterogeneousAddress addressSigner : listSigners) {
            boolean hit = false;
            for (HeterogeneousAddress address : listDistributionFee) {
                if (address.equals(addressSigner)) {
                    hit = true;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
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
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                ConfirmWithdrawalPO po = new ConfirmWithdrawalPO(txData, tx.getHash());
                confirmWithdrawalStorageService.save(chain, po);
                // 更新异构链组件交易状态 // add by pierre at 2020-03-12
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                docking.txConfirmedCompleted(txData.getHeterogeneousTxHash(), blockHeader.getHeight());

                //放入后续处理队列, 可能发起手续费补贴交易
                SignAccountDTO signAccountDTO = virtualBankService.isCurrentDirector(chain);
                if (null != signAccountDTO) {
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                }
                chain.getLogger().debug("[commit] 确认提现交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    private boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                confirmWithdrawalStorageService.deleteByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
                // 更新异构链组件交易状态 // add by pierre at 2020-03-13
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                docking.txConfirmedRollback(txData.getHeterogeneousTxHash());
                chain.getLogger().debug("[rollback] 确认提现交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}
