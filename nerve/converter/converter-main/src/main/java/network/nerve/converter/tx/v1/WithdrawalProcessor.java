/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.*;

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
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    /**
     * 提现异构链与资产是否有效
     * 账户是否有足够提现金额（账本验证）
     * 验证签名
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
     * @return
     */
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
                WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                if (StringUtils.isBlank(txData.getHeterogeneousAddress())) {
                    failsList.add(tx);
                    // 异构链地址不能为空
                    errorCode = ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL.getMsg());
                    continue;
                }
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null == coinData.getFrom() || null == coinData.getTo()) {
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("提现coinData组装错误, from/to is null. txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }
                String withdrawalFromInfo = null;
                int txFromSize = coinData.getFrom().size();
                if (txFromSize == COIN_SIZE_1) {
                    // 只有一个from表示 提现NVT
                    CoinFrom coinFrom = coinData.getFrom().get(0);
                    if (coinFrom.getAssetsChainId() != chain.getConfig().getChainId()
                            || coinFrom.getAssetsId() != chain.getConfig().getAssetId()) {
                        failsList.add(tx);
                        // 手续费不存在
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error("提现coinData组装错误(手续费不存在), 只有一个from 但不是主资产. txhash:{}, fromChainId:{}, fromAssetId:{}, {}",
                                hash,
                                coinFrom.getAssetsChainId(),
                                coinFrom.getAssetsId(),
                                ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                    // 临时记录提现资产信息,稍后比对
                    withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                } else if (txFromSize == COIN_SIZE_2) {
                    boolean hasCurrentAsset = false;
                    for (CoinFrom coinFrom : coinData.getFrom()) {
                        // 判断是手续费
                        if (coinFrom.getAssetsChainId() == chain.getConfig().getChainId()
                                && coinFrom.getAssetsId() == chain.getConfig().getAssetId()) {
                            hasCurrentAsset = true;
                        } else {
                            // 临时记录提现资产信息,稍后比对
                            withdrawalFromInfo = coinFrom.getAssetsChainId() + "-" + coinFrom.getAssetsId() + "-" + coinFrom.getAmount().toString();
                        }
                    }
                    if (!hasCurrentAsset) {
                        failsList.add(tx);
                        // 手续费不存在
                        errorCode = ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST.getMsg());
                        continue;
                    }
                } else {
                    failsList.add(tx);
                    // 提现coin from size 组装错误
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("提现coinData组装错误, from size:{}, txhash:{}, {}", txFromSize, hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                if (coinData.getTo().size() != COIN_SIZE_2) {
                    failsList.add(tx);
                    // 提现coin from size 组装错误
                    errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                    log.error("提现coinData组装错误, to size:{}, txhash:{}, {}", coinData.getTo().size(), hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                    continue;
                }

                BigInteger feeTo = BigInteger.ZERO;
                String withdrawalToInfo = null;
                for (CoinTo coinTo : coinData.getTo()) {
                    // 补贴手续费收集分发地址
                    byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                    // 销毁转出的链内资产黑洞
                    byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
                    // 该to是当前链的主资产
                    boolean currentChainAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinTo.getAssetsId() == chain.getConfig().getAssetId();
                    if (currentChainAsset && Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                        // 组装的补贴手续费的coinTo
                        feeTo = coinTo.getAmount();
                    } else if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                        // 提现资产的coinTo
                        withdrawalToInfo = coinTo.getAssetsChainId() + "-" + coinTo.getAssetsId() + "-" + coinTo.getAmount().toString();
                        HeterogeneousAssetInfo heterogeneousAssetInfo =
                                heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(coinTo.getAssetsChainId(), coinTo.getAssetsId());
                        if (null == heterogeneousAssetInfo) {
                            failsList.add(tx);
                            // 异构链资产不存在
                            errorCode = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getCode();
                            log.error("不支持该资产提现, txhash:{}, AssetChainId:{}, AssetId:{}", hash, coinTo.getAssetsChainId(), coinTo.getAssetsId(), ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg());
                            continue;
                        }
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
                        boolean rs = docking.validateAddress(txData.getHeterogeneousAddress());
                        if (!rs) {
                            failsList.add(tx);
                            // 异构链地址错误
                            errorCode = ConverterErrorCode.ADDRESS_ERROR.getCode();
                            log.error("{},提现异构链地址错误, HeterogeneousAddress:{}",
                                    ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND.getMsg(),
                                    txData.getHeterogeneousAddress());
                            continue;
                        }
                    } else {
                        failsList.add(tx);
                        // 提现to组装错误
                        errorCode = ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getCode();
                        log.error("提现coinData组装错误, txhash:{}, {}", hash, ConverterErrorCode.WITHDRAWAL_COIN_SIZE_ERROR.getMsg());
                        continue;
                    }
                }
                if (BigIntegerUtils.isLessThan(feeTo, ConverterContext.DISTRIBUTION_FEE)) {
                    failsList.add(tx);
                    // 提现补贴手续费不足
                    errorCode = ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getCode();
                    log.error(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE.getMsg());
                    log.error("提现交易补贴手续费不足. txHash:{}, distribution_fee:{}, coinTo_amount:",
                            hash, ConverterContext.DISTRIBUTION_FEE, feeTo);
                    continue;
                }

                if (StringUtils.isBlank(withdrawalFromInfo) || (txFromSize == COIN_SIZE_2 && !withdrawalFromInfo.equals(withdrawalToInfo))) {
                    failsList.add(tx);
                    // 提现资产金额错误
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

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        // 当区块出块正常运行状态时（非区块同步模式），才执行
        if (syncStatus == SyncStatusEnum.RUNNING.value()) {
            Chain chain = chainManager.getChain(chainId);
            try {
                boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
                if (isCurrentDirector) {
                    for (Transaction tx : txs) {
                        //放入类似队列处理机制 准备通知异构链组件执行提现
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setCurrentDirector(true);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        pendingPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        chain.getPendingTxQueue().offer(pendingPO);
                        chain.getLogger().info("[commit] 提现交易 hash:{}", tx.getHash().toHex());
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
        //提现无业务回滚
        return true;
    }
}
