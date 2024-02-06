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
import network.nerve.converter.core.api.ConverterCoreApi;
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
 * Additional withdrawal fee transaction
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
    @Autowired
    private TxStorageService txStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    private final int COIN_SIZE_1 = 1;

    @Override
    public int getType() {
        return TxType.WITHDRAWAL_ADDITIONAL_FEE;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (converterCoreApi.isProtocol21()) {
            // protocol21: One click cross chain
            return validateV21(chainId, txs, txMap, blockHeader);
        } else if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
            // protocol15: Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
            return validateV15(chainId, txs, txMap, blockHeader);
        } else if (converterCoreApi.isSupportProtocol13NewValidationOfERC20()) {
            // protocolv1.13
            return validateV13(chainId, txs, txMap, blockHeader);
        }
        return validateV0(chainId, txs, txMap, blockHeader);
    }

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
                // Changes in updated withdrawal fee additions
                chain.increaseWithdrawFeeChangeVersion(basicTxHash);
                // Save transaction
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                BigInteger fee = coinData.getTo().get(0).getAmount();
                po.getMapAdditionalFee().put(tx.getHash().toHex(), fee);
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                chain.getLogger().info("[commit] Additional withdrawal transaction fee transaction hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), basicTxHash);
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
                // Changes in Rollback Withdrawal Fee Addition
                chain.decreaseWithdrawFeeChangeVersion(withdrawalTxHash);
                //Remove the current transactionkv
                po.getMapAdditionalFee().remove(tx.getHash().toHex());
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                chain.getLogger().info("[rollback] Additional withdrawal transaction fee transaction hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), withdrawalTxHash);
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


    private Map<String, Object> validateV0(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("Original withdrawal transaction with additional transaction feeshashNot present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                /**
                 * 1.There can only be onecoinfrom Must benvtasset
                 * cointo Only one The address is the handling fee address
                 * fromaddress == Signature address == Original transactionfromaddress
                 */

                int txFromSize = coinData.getFrom().size();
                if (txFromSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinFromAssembly error, There is and only onefrom " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }
                CoinFrom coinFrom = coinData.getFrom().get(0);
                if (coinFrom.getAssetsChainId() != chain.getConfig().getChainId() || coinFrom.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("Additional trading assets must be in chain primary assets , AssetsChainId:{}, AssetsId:{}",
                            coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                    continue;
                }

                String basicTxHash = txData.getTxHash();
                Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                if (null == basicTx) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
                    // Not a withdrawal transaction
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("txdataThe corresponding transaction is not a withdrawal transaction/Proposal transaction , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() == TxType.WITHDRAWAL) {
                    CoinData withdrawalTxCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
                    byte[] withdrawalTxAddress = withdrawalTxCoinData.getFrom().get(0).getAddress();
                    byte[] sendAddress = coinFrom.getAddress();
                    if (!Arrays.equals(sendAddress, withdrawalTxAddress)) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_UNMATCHED.getCode();
                        chain.getLogger().error("The withdrawal transaction does not match the user of the additional transaction , originalwithdrawalTxHash:{}, withdrawalTxAddress:{}, AdditionalFeeAddress:{} ",
                                basicTxHash,
                                AddressTool.getStringAddressByBytes(withdrawalTxAddress),
                                AddressTool.getStringAddressByBytes(sendAddress));
                        continue;
                    }
                    // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                    ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                    if (null != po) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                        chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                } else if (basicTx.getType() == TxType.PROPOSAL) {
                    ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                    if (null == proposalPO || proposalPO.getType() != REFUND.value()) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
                        chain.getLogger().error("The original proposal transaction for the withdrawal transaction does not exist or is not returned through the original proposal route , originaltxHash:{}, txhash:{}",
                                basicTxHash, hash);
                        continue;
                    }
                    String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
                    if (StringUtils.isNotBlank(confirmProposalHash)) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.PROPOSAL_CONFIRMED.getCode();
                        chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                }

                // validateto Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                int txToSize = coinData.getTo().size();
                if (txToSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinToAssembly error, There is and only oneto " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != chain.getConfig().getChainId() || coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("Additional trading assets must be in chain primary assets , AssetsChainId:{}, AssetsId:{}",
                            coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    continue;
                }

                byte[] toAddress = coinTo.getAddress();
                if (!Arrays.equals(toAddress, withdrawalFeeAddress)) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
                    chain.getLogger().error("Collection address for handling fees and additional transactionstoAddress mismatch, toAddress:{}, withdrawalFeeAddress:{} ",
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

    /**
     * protocolv1.13 Any address can add additional transaction fees for cross chain transfer transactions
     */
    private Map<String, Object> validateV13(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("Original withdrawal transaction with additional transaction feeshashNot present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                /**
                 * 1.There can only be onecoinfrom Must benvtasset
                 * cointo Only one The address is the handling fee address
                 */
                int txFromSize = coinData.getFrom().size();
                if (txFromSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinFromAssembly error, There is and only onefrom " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }
                CoinFrom coinFrom = coinData.getFrom().get(0);
                if (coinFrom.getAssetsChainId() != chain.getConfig().getChainId() || coinFrom.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("Additional trading assets must be in chain primary assets , AssetsChainId:{}, AssetsId:{}",
                            coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                    continue;
                }

                String basicTxHash = txData.getTxHash();
                Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                if (null == basicTx) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
                    // Not a withdrawal transaction
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("txdataThe corresponding transaction is not a withdrawal transaction/Proposal transaction , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() == TxType.WITHDRAWAL) {
                    // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                    ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                    if (null != po) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                        chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                } else if (basicTx.getType() == TxType.PROPOSAL) {
                    ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                    if (null == proposalPO || proposalPO.getType() != REFUND.value()) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
                        chain.getLogger().error("The original proposal transaction for the withdrawal transaction does not exist or is not returned through the original proposal route , originaltxHash:{}, txhash:{}",
                                basicTxHash, hash);
                        continue;
                    }
                    String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
                    if (StringUtils.isNotBlank(confirmProposalHash)) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.PROPOSAL_CONFIRMED.getCode();
                        chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                }

                // validateto Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                int txToSize = coinData.getTo().size();
                if (txToSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinToAssembly error, There is and only oneto " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != chain.getConfig().getChainId() || coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DATA_ERROR.getCode();
                    chain.getLogger().error("Additional trading assets must be in chain primary assets , AssetsChainId:{}, AssetsId:{}",
                            coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    continue;
                }

                byte[] toAddress = coinTo.getAddress();
                if (!Arrays.equals(toAddress, withdrawalFeeAddress)) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
                    chain.getLogger().error("Collection address for handling fees and additional transactionstoAddress mismatch, toAddress:{}, withdrawalFeeAddress:{} ",
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

    /**
     * protocolv1.15 Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
     */
    private Map<String, Object> validateV15(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
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
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("Original withdrawal transaction with additional transaction feeshashNot present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                /**
                 * 1.There can only be onecoinfrom Must benvtThe main asset of an asset or other chain
                 * cointo Only one The address is the handling fee address
                 */
                int txFromSize = coinData.getFrom().size();
                if (txFromSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinFromAssembly error, There is and only onefrom " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                String basicTxHash = txData.getTxHash();
                Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                if (null == basicTx) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
                    // Not a withdrawal transaction
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("txdataThe corresponding transaction is not a withdrawal transaction/Proposal transaction , hash:{}", basicTxHash);
                    continue;
                }
                int feeAssetChainId = chain.getConfig().getChainId();
                int feeAssetId = chain.getConfig().getAssetId();
                if (basicTx.getType() == TxType.WITHDRAWAL) {
                    // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                    ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                    if (null != po) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                        chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                    // Handling fee assets for withdrawal transactionsID
                    CoinData basicCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
                    byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                    Coin feeCoin = null;
                    for (CoinTo coinTo : basicCoinData.getTo()) {
                        if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                            feeCoin = coinTo;
                            break;
                        }
                    }
                    feeAssetChainId = feeCoin.getAssetsChainId();
                    feeAssetId = feeCoin.getAssetsId();
                } else if (basicTx.getType() == TxType.PROPOSAL) {
                    ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                    if (null == proposalPO || proposalPO.getType() != REFUND.value()) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
                        chain.getLogger().error("The original proposal transaction for the withdrawal transaction does not exist or is not returned through the original proposal route , originaltxHash:{}, txhash:{}",
                                basicTxHash, hash);
                        continue;
                    }
                    String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
                    if (StringUtils.isNotBlank(confirmProposalHash)) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.PROPOSAL_CONFIRMED.getCode();
                        chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                }

                // validateto Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                int txToSize = coinData.getTo().size();
                if (txToSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinToAssembly error, There is and only oneto " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != feeAssetChainId || coinTo.getAssetsId() != feeAssetId) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR.getCode();
                    chain.getLogger().error("Additional transaction assets must be consistent with the transaction fee assets for withdrawal transactions, AssetsChainId:{}, AssetsId:{}",
                            coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    continue;
                }

                byte[] toAddress = coinTo.getAddress();
                if (!Arrays.equals(toAddress, withdrawalFeeAddress)) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
                    chain.getLogger().error("Collection address for handling fees and additional transactionstoAddress mismatch, toAddress:{}, withdrawalFeeAddress:{} ",
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

    /**
     * protocolv1.21 One click cross chain
     */
    private Map<String, Object> validateV21(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
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
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error("Original withdrawal transaction with additional transaction feeshashNot present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                /**
                 * 1.There can only be onecoinfrom Must benvtThe main asset of an asset or other chain
                 * cointo Only one The address is the handling fee address
                 */
                int txFromSize = coinData.getFrom().size();
                if (txFromSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinFromAssembly error, There is and only onefrom " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                String basicTxHash = txData.getTxHash();
                Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
                if (null == basicTx) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
                    continue;
                }
                if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN) {
                    // Not a withdrawal transaction
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    chain.getLogger().error("txdataThe corresponding transaction is not a withdrawal transaction/Proposal transaction/One click cross chain , hash:{}", basicTxHash);
                    continue;
                }
                int feeAssetChainId = chain.getConfig().getChainId();
                int feeAssetId = chain.getConfig().getAssetId();
                if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
                    // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                    ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
                    if (null != po) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_CONFIRMED.getCode();
                        chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                    // Handling fee assets for withdrawal transactionsID
                    CoinData basicCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
                    byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                    Coin feeCoin = null;
                    for (CoinTo coinTo : basicCoinData.getTo()) {
                        if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                            feeCoin = coinTo;
                            break;
                        }
                    }
                    feeAssetChainId = feeCoin.getAssetsChainId();
                    feeAssetId = feeCoin.getAssetsId();
                } else if (basicTx.getType() == TxType.PROPOSAL) {
                    ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                    if (null == proposalPO || proposalPO.getType() != REFUND.value()) {
                        failsList.add(tx);
                        errorCode = ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
                        chain.getLogger().error("The original proposal transaction for the withdrawal transaction does not exist or is not returned through the original proposal route , originaltxHash:{}, txhash:{}",
                                basicTxHash, hash);
                        continue;
                    }
                    String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
                    if (StringUtils.isNotBlank(confirmProposalHash)) {
                        // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                        failsList.add(tx);
                        // NerveWithdrawal transaction does not exist
                        errorCode = ConverterErrorCode.PROPOSAL_CONFIRMED.getCode();
                        chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}, hash:{}", basicTxHash, hash);
                        continue;
                    }
                }

                // validateto Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                int txToSize = coinData.getTo().size();
                if (txToSize != COIN_SIZE_1) {
                    failsList.add(tx);
                    // Withdrawal transactionshash
                    errorCode = ConverterErrorCode.DATA_SIZE_ERROR.getCode();
                    log.error("coinToAssembly error, There is and only oneto " + ConverterErrorCode.DATA_SIZE_ERROR.getMsg());
                    continue;
                }

                // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != feeAssetChainId || coinTo.getAssetsId() != feeAssetId) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR.getCode();
                    chain.getLogger().error("Additional transaction assets must be consistent with the transaction fee assets for withdrawal transactions, AssetsChainId:{}, AssetsId:{}",
                            coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    continue;
                }

                byte[] toAddress = coinTo.getAddress();
                if (!Arrays.equals(toAddress, withdrawalFeeAddress)) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
                    chain.getLogger().error("Collection address for handling fees and additional transactionstoAddress mismatch, toAddress:{}, withdrawalFeeAddress:{} ",
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
}
