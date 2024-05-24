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

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.base.basic.TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES;
import static network.nerve.converter.config.ConverterContext.FEE_ADDITIONAL_HEIGHT;
import static network.nerve.converter.config.ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT;
import static network.nerve.converter.constant.ConverterConstant.DISTRIBUTION_FEE_10;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component("WithdrawalV1")
public class WithdrawalProcessor implements TransactionProcessor {

    private final int COIN_SIZE_1 = 1;
    private final int COIN_SIZE_2 = 2;

    @Override
    public int getType() {
        return TxType.WITHDRAWAL;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    /**
     * Whether the withdrawal of heterogeneous chains and assets is effective
     * Is there sufficient withdrawal amount in the account（Ledger verification）
     * Verify signature
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (converterCoreApi.isProtocol16()) {
            return validateOfProtocol16(chainId, txs, txMap, blockHeader);
        } else if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
            return validateOfProtocol15(chainId, txs, txMap, blockHeader);
        } else {
            return _validate(chainId, txs, txMap, blockHeader);
        }
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        // When the block is in normal operation state（Non block synchronization mode）Only then will it be executed
        if (syncStatus == SyncStatusEnum.RUNNING.value()) {
            Chain chain = chainManager.getChain(chainId);
            try {
                boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
                if (isCurrentDirector) {
                    for (Transaction tx : txs) {
                        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                        //Placing a queue like processing mechanism Prepare to notify heterogeneous chain components to execute withdrawals
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setCurrentDirector(true);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        pendingPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        if (txData.getHeterogeneousChainId() > 200) {
                            txSubsequentProcessStorageService.saveBackup(chain, pendingPO);
                        }
                        chain.getPendingTxQueue().offer(pendingPO);
                        chain.getLogger().info("[commit] Withdrawal transaction added to pending queue hash:{}", tx.getHash().toHex());
                    }
                }
            } catch (Exception e) {
                chain.getLogger().error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        //Withdrawal without business rollback
        return true;
    }

    private Map<String, Object> _validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                if (StringUtils.isBlank(txData.getHeterogeneousAddress())) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getMsg());
                    continue;
                }
                long height = chain.getLatestBasicBlock().getHeight();
                if(null != blockHeader) {
                    height = blockHeader.getHeight();
                }
                if(height >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
                    if (txData.getHeterogeneousChainId() <= 0) {
                        failsList.add(tx);
                        // Heterogeneous chainidCannot be empty
                        errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                        log.error(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getMsg());
                        continue;
                    }
                }

                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }
                String withdrawalFromInfo = null;
                int txFromSize = coinData.getFrom().size();
                if (txFromSize == COIN_SIZE_1) {
                    // Only onefromexpress WithdrawalNVT
                    CoinFrom coinFrom = coinData.getFrom().get(0);
                    if (coinFrom.getAssetsChainId() != chain.getConfig().getChainId()
                            || coinFrom.getAssetsId() != chain.getConfig().getAssetId()) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error("WithdrawalcoinDataAssembly error(The handling fee does not exist), Only onefrom But not the main asset. txhash:{}, fromChainId:{}, fromAssetId:{}, {}",
                                hash,
                                coinFrom.getAssetsChainId(),
                                coinFrom.getAssetsId(),
                                ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                    // Temporary record of withdrawal asset information,Compare later
                    withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                } else if (txFromSize == COIN_SIZE_2) {
                    boolean hasCurrentAsset = false;
                    for (CoinFrom coinFrom : coinData.getFrom()) {
                        // Judging it as a handling fee
                        if (coinFrom.getAssetsChainId() == chain.getConfig().getChainId()
                                && coinFrom.getAssetsId() == chain.getConfig().getAssetId()) {
                            hasCurrentAsset = true;
                        } else {
                            // Temporary record of withdrawal asset information,Compare later
                            withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                        }
                    }
                    if (!hasCurrentAsset) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getMsg());
                        continue;
                    }
                } else {
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from size:{}, txhash:{}, {}", txFromSize, hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if(coinData.getTo().size() != COIN_SIZE_2){
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, to size:{}, txhash:{}, {}", coinData.getTo().size(), hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                BigInteger feeTo = BigInteger.ZERO;
                String withdrawalToInfo = null;
                // Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                // Destroy the black hole of in chain assets transferred out
                byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
                for (CoinTo coinTo : coinData.getTo()) {
                    // This to It is the main asset of the current chain
                    boolean currentChainAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinTo.getAssetsId() == chain.getConfig().getAssetId();
                    if (currentChainAsset && Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                        // Subsidies for assembly and handling feescoinTo
                        feeTo = coinTo.getAmount();
                    } else if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                        // Withdrawal of assetscoinTo
                        withdrawalToInfo = coinTo.getAssetsChainId() + "-" + coinTo.getAssetsId() + "-" + coinTo.getAmount().toString();
                        HeterogeneousAssetInfo heterogeneousAssetInfo =
                                heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                        if (null == heterogeneousAssetInfo) {
                            failsList.add(tx);
                            // Heterogeneous chain assets do not exist
                            errorCode = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getCode();
                            log.error("The asset withdrawal is not supported, txhash:{}, AssetChainId:{}, AssetId:{}", hash, coinTo.getAssetsChainId(), coinTo.getAssetsId(), ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg());
                            continue;
                        }
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
                        boolean rs = docking.validateAddress(txData.getHeterogeneousAddress());
                        if (!rs) {
                            failsList.add(tx);
                            // Heterogeneous chain address error
                            errorCode = ConverterErrorCode.ADDRESS_ERROR.getCode();
                            log.error("{},Withdrawal of heterogeneous chain address error, HeterogeneousAddress:{}",
                                    ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg(),
                                    txData.getHeterogeneousAddress());
                            continue;
                        }
                    } else {
                        failsList.add(tx);
                        // WithdrawaltoAssembly error
                        errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                        log.error("WithdrawalcoinDataAssembly error, txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                }

                if(height < FEE_ADDITIONAL_HEIGHT){
                    if (BigIntegerUtils.isLessThan(feeTo, DISTRIBUTION_FEE_10)) {
                        failsList.add(tx);
                        // Insufficient withdrawal subsidy handling fees
                        errorCode = ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getCode();
                        log.error(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getMsg());
                        log.error("Insufficient transaction subsidy fees for withdrawal. txHash:{}, distribution_fee:{}, coinTo_amount:{}",
                                hash, DISTRIBUTION_FEE_10, feeTo);
                        continue;
                    }
                } else {
                    if (BigIntegerUtils.isLessThan(feeTo, NORMAL_PRICE_PRE_1024_BYTES)) {
                        failsList.add(tx);
                        // Insufficient withdrawal subsidy handling fees
                        errorCode = ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getCode();
                        log.error(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getMsg());
                        log.error("Insufficient transaction subsidy fees for withdrawal. txHash:{}, distribution_fee_min:{}, coinTo_amount:",
                                hash, NORMAL_PRICE_PRE_1024_BYTES, feeTo);
                        continue;
                    }
                }

                if (StringUtils.isBlank(withdrawalFromInfo) || (txFromSize == COIN_SIZE_2 && !withdrawalFromInfo.equals(withdrawalToInfo))) {
                    failsList.add(tx);
                    // Incorrect withdrawal amount of assets
                    errorCode = ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getMsg());
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

    private Map<String, Object> validateOfProtocol15(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                if (StringUtils.isBlank(txData.getHeterogeneousAddress())) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getMsg());
                    continue;
                }
                int htgChainId = txData.getHeterogeneousChainId();
                if (htgChainId <= 0) {
                    failsList.add(tx);
                    // Heterogeneous chainidCannot be empty
                    errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }
                Coin feeCoin = null;
                String withdrawalFromInfo = null;
                BigInteger feeTo = BigInteger.ZERO;
                String withdrawalToInfo = null;
                // Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                // Destroy the black hole of in chain assets transferred out
                byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
                for (CoinTo coinTo : coinData.getTo()) {
                    // Handling fee assets
                    if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                        // That is not to sayNVT, nor is it the main asset of heterogeneous chain networks
                        if ((coinTo.getAssetsChainId() != chain.getConfig().getChainId() || coinTo.getAssetsId() != chain.getConfig().getAssetId())
                                && !converterCoreApi.isHtgMainAsset(coinTo)) {
                            failsList.add(tx);
                            // The handling fee does not exist
                            errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                            log.error("WithdrawalcoinDataAssembly error(The handling fee does not exist), Only onefrom But not the main asset. txhash:{}, assetChainId:{}, assetId:{}, {}",
                                    hash,
                                    coinTo.getAssetsChainId(),
                                    coinTo.getAssetsId(),
                                    ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                            continue outer;
                        }
                        // Subsidies for assembly and handling feescoinTo
                        feeCoin = coinTo;
                        feeTo = coinTo.getAmount();
                    } else if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                        // Withdrawal of assetscoinTo
                        withdrawalToInfo = coinTo.getAssetsChainId() + "-" + coinTo.getAssetsId() + "-" + coinTo.getAmount().toString();
                        HeterogeneousAssetInfo heterogeneousAssetInfo =
                                heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                        if (null == heterogeneousAssetInfo) {
                            failsList.add(tx);
                            // Heterogeneous chain assets do not exist
                            errorCode = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getCode();
                            log.error("The asset withdrawal is not supported, txhash:{}, AssetChainId:{}, AssetId:{}", hash, coinTo.getAssetsChainId(), coinTo.getAssetsId(), ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg());
                            continue outer;
                        }
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
                        boolean rs = docking.validateAddress(txData.getHeterogeneousAddress());
                        if (!rs) {
                            failsList.add(tx);
                            // Heterogeneous chain address error
                            errorCode = ConverterErrorCode.ADDRESS_ERROR.getCode();
                            log.error("{},Withdrawal of heterogeneous chain address error, HeterogeneousAddress:{}",
                                    ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg(),
                                    txData.getHeterogeneousAddress());
                            continue outer;
                        }
                    } else {
                        failsList.add(tx);
                        // WithdrawaltoAssembly error
                        errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                        log.error("WithdrawalcoinDataAssembly error, txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue outer;
                    }
                }

                int txFromSize = coinData.getFrom().size();
                if (txFromSize == COIN_SIZE_1) {
                    // Only onefromexpress WithdrawalNVT, or other main assets of heterogeneous chain networks
                    CoinFrom coinFrom = coinData.getFrom().get(0);
                    if (coinFrom.getAssetsChainId() != feeCoin.getAssetsChainId()
                            || coinFrom.getAssetsId() != feeCoin.getAssetsId()) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error("WithdrawalcoinDataAssembly error(The handling fee does not exist), Only onefrom But not the main asset. txhash:{}, fromChainId:{}, fromAssetId:{}, {}",
                                hash,
                                coinFrom.getAssetsChainId(),
                                coinFrom.getAssetsId(),
                                ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                    // Temporary record of withdrawal asset information,Compare later
                    withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                } else if (txFromSize == COIN_SIZE_2) {
                    boolean hasFeeAsset = false;
                    for (CoinFrom coinFrom : coinData.getFrom()) {
                        // Judging it as a handling fee
                        if (coinFrom.getAssetsChainId() == feeCoin.getAssetsChainId()
                                && coinFrom.getAssetsId() == feeCoin.getAssetsId()) {
                            hasFeeAsset = true;
                        } else {
                            // Temporary record of withdrawal asset information,Compare later
                            withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                        }
                    }
                    if (!hasFeeAsset) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getMsg());
                        continue;
                    }
                } else {
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from size:{}, txhash:{}, {}", txFromSize, hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if(coinData.getTo().size() != COIN_SIZE_2){
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, to size:{}, txhash:{}, {}", coinData.getTo().size(), hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if (BigIntegerUtils.isLessThan(feeTo, NORMAL_PRICE_PRE_1024_BYTES)) {
                    failsList.add(tx);
                    // Insufficient withdrawal subsidy handling fees
                    errorCode = ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getCode();
                    log.error(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getMsg());
                    log.error("Insufficient transaction subsidy fees for withdrawal. txHash:{}, distribution_fee_min:{}, coinTo_amount:",
                            hash, NORMAL_PRICE_PRE_1024_BYTES, feeTo);
                    continue;
                }

                if (StringUtils.isBlank(withdrawalFromInfo) || (txFromSize == COIN_SIZE_2 && !withdrawalFromInfo.equals(withdrawalToInfo))) {
                    failsList.add(tx);
                    // Incorrect withdrawal amount of assets
                    errorCode = ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getMsg());
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

    private Map<String, Object> validateOfProtocol16(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                if (StringUtils.isBlank(txData.getHeterogeneousAddress())) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getMsg());
                    continue;
                }
                int htgChainId = txData.getHeterogeneousChainId();
                if (htgChainId <= 0) {
                    failsList.add(tx);
                    // Heterogeneous chainidCannot be empty
                    errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }
                Coin feeCoin = null;
                String withdrawalFromInfo = null;
                String withdrawalToInfo = null;
                // Subsidy handling fee collection and distribution address
                byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                // Destroy the black hole of in chain assets transferred out
                byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
                for (CoinTo coinTo : coinData.getTo()) {
                    // Handling fee assets
                    if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                        // That is not to sayNVT, nor is it the main asset of heterogeneous chain networks
                        if ((coinTo.getAssetsChainId() != chain.getConfig().getChainId() || coinTo.getAssetsId() != chain.getConfig().getAssetId())
                                && !converterCoreApi.isHtgMainAsset(coinTo)) {
                            failsList.add(tx);
                            // The handling fee does not exist
                            errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                            log.error("WithdrawalcoinDataAssembly error(The handling fee does not exist), Only onefrom But not the main asset. txhash:{}, assetChainId:{}, assetId:{}, {}",
                                    hash,
                                    coinTo.getAssetsChainId(),
                                    coinTo.getAssetsId(),
                                    ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                            continue outer;
                        }
                        // Subsidies for assembly and handling feescoinTo
                        feeCoin = coinTo;
                    } else if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                        // Withdrawal of assetscoinTo
                        withdrawalToInfo = coinTo.getAssetsChainId() + "-" + coinTo.getAssetsId() + "-" + coinTo.getAmount().toString();
                        HeterogeneousAssetInfo heterogeneousAssetInfo =
                                heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                        if (null == heterogeneousAssetInfo) {
                            failsList.add(tx);
                            // Heterogeneous chain assets do not exist
                            errorCode = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getCode();
                            log.error("The asset withdrawal is not supported, txhash:{}, AssetChainId:{}, AssetId:{}, error: {}", hash, coinTo.getAssetsChainId(), coinTo.getAssetsId(), ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg());
                            continue outer;
                        }
                        // add by pierre at 2022/7/1 Withdrawal suspension mechanism
                        boolean pauseOut = converterCoreApi.isPauseOutHeterogeneousAsset(heterogeneousAssetInfo.getChainId(), heterogeneousAssetInfo.getAssetId());
                        if (pauseOut) {
                            failsList.add(tx);
                            // Heterogeneous chain assets do not exist
                            errorCode = ConverterErrorCode.WITHDRAWAL_PAUSE.getCode();
                            log.error("[Suspend asset withdrawals], txhash:{}, AssetChainId:{}, AssetId:{}, heterogeneousId: {}", hash, coinTo.getAssetsChainId(), coinTo.getAssetsId(), txData.getHeterogeneousChainId());
                            continue outer;
                        }
                        // end code by pierre
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
                        boolean rs = docking.validateAddress(txData.getHeterogeneousAddress());
                        if (!rs) {
                            failsList.add(tx);
                            // Heterogeneous chain address error
                            errorCode = ConverterErrorCode.ADDRESS_ERROR.getCode();
                            log.error("{},Withdrawal of heterogeneous chain address error, HeterogeneousAddress:{}",
                                    ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg(),
                                    txData.getHeterogeneousAddress());
                            continue outer;
                        }
                    } else {
                        failsList.add(tx);
                        // WithdrawaltoAssembly error
                        errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                        log.error("WithdrawalcoinDataAssembly error, txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue outer;
                    }
                }

                int txFromSize = coinData.getFrom().size();
                if (txFromSize == COIN_SIZE_1) {
                    // Only onefromexpress WithdrawalNVT, or other main assets of heterogeneous chain networks
                    CoinFrom coinFrom = coinData.getFrom().get(0);
                    if (coinFrom.getAssetsChainId() != feeCoin.getAssetsChainId()
                            || coinFrom.getAssetsId() != feeCoin.getAssetsId()) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error("WithdrawalcoinDataAssembly error(The handling fee does not exist), Only onefrom But not the main asset. txhash:{}, fromChainId:{}, fromAssetId:{}, {}",
                                hash,
                                coinFrom.getAssetsChainId(),
                                coinFrom.getAssetsId(),
                                ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                    // Temporary record of withdrawal asset information,Compare later
                    withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                } else if (txFromSize == COIN_SIZE_2) {
                    boolean hasFeeAsset = false;
                    for (CoinFrom coinFrom : coinData.getFrom()) {
                        // Judging it as a handling fee
                        if (coinFrom.getAssetsChainId() == feeCoin.getAssetsChainId()
                                && coinFrom.getAssetsId() == feeCoin.getAssetsId()) {
                            hasFeeAsset = true;
                        } else {
                            // Temporary record of withdrawal asset information,Compare later
                            withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                        }
                    }
                    if (!hasFeeAsset) {
                        failsList.add(tx);
                        // The handling fee does not exist
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getMsg());
                        continue;
                    }
                } else {
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, from size:{}, txhash:{}, {}", txFromSize, hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if(coinData.getTo().size() != COIN_SIZE_2){
                    failsList.add(tx);
                    // Withdrawalcoin from size Assembly error
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("WithdrawalcoinDataAssembly error, to size:{}, txhash:{}, {}", coinData.getTo().size(), hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if (StringUtils.isBlank(withdrawalFromInfo) || (txFromSize == COIN_SIZE_2 && !withdrawalFromInfo.equals(withdrawalToInfo))) {
                    failsList.add(tx);
                    // Incorrect withdrawal amount of assets
                    errorCode = ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getCode();
                    log.error(ConverterErrorCode.WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR.getMsg());
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
