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

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.WithdrawalAdditionalFeePO;
import network.nerve.converter.model.txdata.WithdrawalAdditionalFeeTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.ProposalExeStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.converter.constant.ConverterConstant.INIT_CAPACITY_8;
import static network.nerve.converter.enums.ProposalTypeEnum.REFUND;

/**
 * 追加提现手续费交易
 *
 * @author: Loki
 * @date: 2020/9/27
 */
@Component("WithdrawalAdditionalFeeV1")
public class WithdrawalAdditionalFeeProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private ProposalExeStorageService proposalExeStorageService;

    private final int COIN_SIZE_1 = 1;

    @Override
    public int getType() {
        return TxType.WITHDRAWAL_ADDITIONAL_FEE;
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
            outer:
            for (Transaction tx : txs) {
                String hash = tx.getHash().toHex();
                WithdrawalAdditionalFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAdditionalFeeTxData.class);
                if (StringUtils.isBlank(txData.getTxHash())) {
                    failsList.add(tx);
                    // 提现交易hash
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("要追加手续费的原始提现交易hash不存在! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("提现coinData组装错误, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                /**
                 * 1.只能有一个coinfrom 必须是nvt资产
                 * cointo 只有一个 地址是手续费地址
                 * from地址 == 签名地址 == 原始交易from地址
                 */

                int txFromSize = coinData.getFrom().size();
                if (txFromSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // 提现交易hash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinFrom组装错误, 有且只有一个from " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }
                CoinFrom coinFrom = coinData.getFrom().get(0);
                if (coinFrom.getAssetsChainId() != chain.getConfig().getChainId() || coinFrom.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("追加交易资产必须是链内主资产 , AssetsChainId:{}, AssetsId:{}",
                            coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                    continue;
                }

                String basicTxHash = txData.getTxHash();
                Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                if (null == basicTx) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("原始交易不存在 , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
                    // 不是提现交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("txdata对应的交易不是提现交易/提案交易 , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() == TxType.WITHDRAWAL) {
                    CoinData withdrawalTxCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
                    byte[] withdrawalTxAddress = withdrawalTxCoinData.getFrom().get(0).getAddress();
                    byte[] sendAddress = coinData.getFrom().get(0).getAddress();
                    if (!Arrays.equals(sendAddress, withdrawalTxAddress)) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_UNMATCHED.getCode();
                        chain.getLogger().error("该提现交易与追加交易用户不匹配 , 原始withdrawalTxHash:{}, withdrawalTxAddress:{}, AdditionalFeeAddress:{} ",
                                basicTxHash,
                                AddressTool.getStringAddressByBytes(withdrawalTxAddress),
                                AddressTool.getStringAddressByBytes(sendAddress));
                        continue;
                    }
                    // 判断该提现交易是否已经有对应的确认提现交易
                    ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                    if (null != po) {
                        // 说明该提现交易 已经发出过确认提现交易, 不能再追加手续费
                        failsList.add(tx);
                        // Nerve提现交易不存在
                        errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                        chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                } else if (basicTx.getType() == TxType.PROPOSAL) {
                    ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                    if (null == proposalPO || proposalPO.getType() != REFUND.value()) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
                        chain.getLogger().error("该提现交易的原始提案交易不存在或不是提案原路退回 , 原始txHash:{}, txhash:{}",
                                basicTxHash, hash);
                        continue;
                    }
                    String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
                    if (StringUtils.isNotBlank(confirmProposalHash)) {
                        // 说明该提现交易 已经发出过确认提现交易, 不能再追加手续费
                        failsList.add(tx);
                        // Nerve提现交易不存在
                        errorCode = ConverterErrorCode.PROPOSAL_CONFIRMED.getCode();
                        chain.getLogger().error("该提案交易已经完成,不能再追加异构链提现手续费, proposalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                }

                // 验证to 补贴手续费收集分发地址
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                int txToSize = coinData.getTo().size();
                if (txToSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // 提现交易hash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinTo组装错误, 有且只有一个to " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != chain.getConfig().getChainId() || coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("追加交易资产必须是链内主资产 , AssetsChainId:{}, AssetsId:{}",
                            coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    continue;
                }

                byte[] toAddress = coinTo.getAddress();
                if (!Arrays.equals(toAddress, withdrawalFeeAddress)) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
                    chain.getLogger().error("手续费收集地址与追加交易to地址不匹配, toAddress:{}, withdrawalFeeAddress:{} ",
                            AddressTool.getStringAddressByBytes(toAddress),
                            AddressTool.getStringAddressByBytes(withdrawalFeeAddress));
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

    @Autowired
    private TxStorageService txStorageService;

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                WithdrawalAdditionalFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAdditionalFeeTxData.class);
                String basicTxHash = txData.getTxHash();
                WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, basicTxHash);
                if (null == po) {
                    po = new WithdrawalAdditionalFeePO(basicTxHash, new HashMap<>(INIT_CAPACITY_8));
                }
                if (null == po.getMapAdditionalFee()) {
                    po.setMapAdditionalFee(new HashMap<>(INIT_CAPACITY_8));
                }
                // 更新提现手续费追加的变化
                chain.increaseWithdrawFeeChangeVersion(basicTxHash);
                // 保存交易
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                BigInteger fee = coinData.getTo().get(0).getAmount();
                po.getMapAdditionalFee().put(tx.getHash().toHex(), fee);
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                chain.getLogger().info("[commit] 追加提现交易手续费交易 hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), basicTxHash);
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

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                WithdrawalAdditionalFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAdditionalFeeTxData.class);
                String withdrawalTxHash = txData.getTxHash();
                WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, withdrawalTxHash);
                if (po == null || null == po.getMapAdditionalFee() || po.getMapAdditionalFee().isEmpty()) {
                    continue;
                }
                // 回滚提现手续费追加的变化
                chain.decreaseWithdrawFeeChangeVersion(withdrawalTxHash);
                //移除当前交易的kv
                po.getMapAdditionalFee().remove(tx.getHash().toHex());
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                chain.getLogger().info("[rollback] 追加提现交易手续费交易 hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), withdrawalTxHash);
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
