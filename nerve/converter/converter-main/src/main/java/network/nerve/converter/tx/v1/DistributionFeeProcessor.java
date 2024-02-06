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

/**
 * @author: Loki
 * @date: 2020/3/19
 */

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.DistributionFeePO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.DistributionFeeTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.DistributionFeeStorageService;
import network.nerve.converter.storage.VirtualBankAllHistoryStorageService;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.*;

@Component("DistributionFeeV1")
public class DistributionFeeProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private DistributionFeeStorageService distributionFeeStorageService;
    @Autowired
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    @Override
    public int getType() {
        return TxType.DISTRIBUTION_FEE;
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
                DistributionFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), DistributionFeeTxData.class);
                NulsHash basisTxHash = txData.getBasisTxHash();
                String originalHash = basisTxHash.toHex();
                if (setDuplicate.contains(originalHash)) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                // Verify if rewards are issued repeatedly
                DistributionFeePO po = distributionFeeStorageService.findByBasisTxHash(chain, basisTxHash);
                if (null != po) {
                    // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out,This transaction is a duplicate confirmed withdrawal transaction
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DISTRIBUTION_FEE_IS_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.DISTRIBUTION_FEE_IS_DUPLICATION.getMsg());
                    continue;
                }

                // Obtain original transaction
                Transaction basisTx = TransactionCall.getConfirmedTx(chain, basisTxHash);
                if (null == basisTx) {
                    failsList.add(tx);
                    // NerveThe original transaction does not exist
                    errorCode = ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
                    continue;
                }

                // Based on the original transaction To verify
                switch (basisTx.getType()) {
                    case TxType.WITHDRAWAL:
                        String eCode = validWithdrawalDistribution(chain, tx, basisTx, blockHeader);
                        if (null != eCode) {
                            failsList.add(tx);
                            errorCode = eCode;
                            continue;
                        }
                        break;
                    case TxType.PROPOSAL:
                        eCode = validProposalDistribution(chain, tx, basisTx, blockHeader);
                        if (null != eCode) {
                            failsList.add(tx);
                            errorCode = eCode;
                            continue;
                        }
                        break;
                    default:
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

    /**
     * Distribution of transaction fee subsidies for verifying withdrawal transactions
     * If passed, return empty, Failure to return error code
     *
     * @param chain
     * @param tx
     * @param basisTx
     * @return
     */
    private String validWithdrawalDistribution(Chain chain, Transaction tx, Transaction basisTx, BlockHeader blockHeader) {
        // Obtain withdrawal confirmation transactions Distribution fee address
        ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basisTx.getHash());
        return validDistributionFeeAddress(chain, tx, basisTx, po.getListDistributionFee(), blockHeader, false);
    }

    /**
     * Verify subsidy renewal address
     *
     * @param chain
     * @param tx
     * @param basisTx
     * @param listDistributionFee
     * @return
     */
    private String validDistributionFeeAddress(Chain chain, Transaction tx, Transaction basisTx, List<HeterogeneousAddress> listDistributionFee, BlockHeader blockHeader, boolean isProposal) {
        if (null == listDistributionFee || listDistributionFee.isEmpty()) {
            chain.getLogger().error(ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY.getMsg());
            return ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY.getCode();
        }
        // List of addresses for confirming the handling fees to be paid for withdrawal transactions
        List<byte[]> listBasisTxRewardAddressBytes = new ArrayList<>();
        for (HeterogeneousAddress addr : listDistributionFee) {
            String address = chain.getDirectorRewardAddress(addr);
            if (StringUtils.isBlank(address)) {
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listBasisTxRewardAddressBytes.add(AddressTool.getAddress(address));
        }
        CoinData coinData;
        try {
            coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return e.getErrorCode().getCode();
        }

        // List of addresses for receiving transaction fees for subsidy transactions
        List<byte[]> listDistributionTxRewardAddressBytes = new ArrayList<>();
        // calculate How much commission is subsidized for each node
        BigInteger count = BigInteger.valueOf(listBasisTxRewardAddressBytes.size());

        long height = chain.getLatestBasicBlock().getHeight();
        if (null != blockHeader) {
            height = blockHeader.getHeight();
        }
        WithdrawalTotalFeeInfo distributionFeeInfo;
        BigInteger distributionFee;
        try {
            // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
            distributionFeeInfo = assembleTxService.calculateFee(chain, height, basisTx, isProposal);
            distributionFee = distributionFeeInfo.getFee();
        } catch (NulsException e) {
            return e.getErrorCode().getCode();
        }

        BigInteger amount = distributionFee.divide(count);
        // Through raw data(Confirm the list in the transaction),Calculate assemblycointoData for
        Map<String, BigInteger> coinToOriginalMap = assembleTxService.calculateDistributionFeeCoinToAmount(listBasisTxRewardAddressBytes, amount);

        if (coinToOriginalMap.size() != coinData.getTo().size()) {
            chain.getLogger().error(ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getMsg());
            return ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
        }

        Coin feeCoin = distributionFeeInfo.getFeeCoin();
        for (CoinTo coinTo : coinData.getTo()) {
            // protocol15: Add verificationfeeCoin
            if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
                if (distributionFeeInfo.isNvtAsset()) {
                    if (coinTo.getAssetsChainId() != chain.getConfig().getChainId()
                            || coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                        chain.getLogger().error("{}, coinTo:{}-{}, need: {}-{}", ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getMsg(), coinTo.getAssetsChainId(), coinTo.getAssetsId(), chain.getConfig().getChainId(), chain.getConfig().getAssetId());
                        return ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getCode();
                    }
                } else {
                    if (coinTo.getAssetsChainId() != feeCoin.getAssetsChainId()
                            || coinTo.getAssetsId() != feeCoin.getAssetsId()) {
                        chain.getLogger().error("{}, coinTo:{}-{}, need: {}-{}", ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getMsg(), coinTo.getAssetsChainId(), coinTo.getAssetsId(), feeCoin.getAssetsChainId(), feeCoin.getAssetsId());
                        return ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getCode();
                    }
                }
            }
            BigInteger originalAmount = coinToOriginalMap.get(AddressTool.getStringAddressByBytes(coinTo.getAddress()));
            if (!BigIntegerUtils.isEqual(coinTo.getAmount(), originalAmount)) {
                chain.getLogger().error("{}, coinToAmount:{}, originalAmount:{}, amount:{}", ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getMsg(), coinTo.getAmount(), originalAmount);
                return ConverterErrorCode.DISTRIBUTION_FEE_ERROR.getCode();
            }
            listDistributionTxRewardAddressBytes.add(coinTo.getAddress());
        }

        for (byte[] addrBasisBytes : listBasisTxRewardAddressBytes) {
            boolean hit = false;
            for (byte[] addrDistributionBytes : listDistributionTxRewardAddressBytes) {
                if (Arrays.equals(addrBasisBytes, addrDistributionBytes)) {
                    hit = true;
                }
            }
            if (!hit) {
                chain.getLogger().error(ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getMsg());
                return ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH.getCode();
            }
        }
        return null;
    }


    /**
     * Distribution of transaction fee subsidies for verifying proposal voting transactions
     * If passed, return empty, Failure to return error code
     *
     * @param chain
     * @param tx      Current subsidy handling fee transactions
     * @param basisTx Execute proposal confirmation transaction
     * @return
     */
    private String validProposalDistribution(Chain chain, Transaction tx, Transaction basisTx, BlockHeader blockHeader) {
        ConfirmProposalTxData txData;
        ProposalExeBusinessData businessData;
        try {
            txData = ConverterUtil.getInstance(basisTx.getTxData(), ConfirmProposalTxData.class);
            businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
        } catch (NulsException e) {
            chain.getLogger().error(ConverterErrorCode.DESERIALIZE_ERROR.getMsg());
            return ConverterErrorCode.DESERIALIZE_ERROR.getCode();
        }

        // Verify proposal type
        if (txData.getType() != ProposalTypeEnum.UPGRADE.value() &&
                txData.getType() == ProposalTypeEnum.EXPELLED.value() &&
                txData.getType() != ProposalTypeEnum.REFUND.value()) {
            chain.getLogger().error("The execution of proposal types does not require subsidy handling fees. ProposalTxHash:{}, type:{}",
                    businessData.getProposalTxHash().toHex(), txData.getType());
            return ConverterErrorCode.PROPOSAL_TYPE_ERROR.getCode();
        }
        return validDistributionFeeAddress(chain, tx, basisTx, businessData.getListDistributionFee(), blockHeader, true);
    }


    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    private boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus,
                           boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                DistributionFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), DistributionFeeTxData.class);
                boolean rs = distributionFeeStorageService.save(chain, new DistributionFeePO(txData.getBasisTxHash(), tx.getHash()));
                if (!rs) {
                    chain.getLogger().error("[commit] Save distribution fee failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[commit] Subsidy handling fee transaction hash:{}, basisTxHash:{}", tx.getHash().toHex(), txData.getBasisTxHash().toHex());
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
                DistributionFeeTxData txData = ConverterUtil.getInstance(tx.getTxData(), DistributionFeeTxData.class);
                boolean rs = distributionFeeStorageService.deleteByBasisTxHash(chain, txData.getBasisTxHash());
                if (!rs) {
                    chain.getLogger().error("[rollback] remove distribution fee failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                chain.getLogger().info("[rollback] Subsidy handling fee transaction hash:{}", tx.getHash().toHex());
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
