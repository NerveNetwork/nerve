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
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.txdata.HeterogeneousMainAssetBindTxData;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@Component("HeterogeneousMainAssetBindV1")
public class HeterogeneousMainAssetBindProcessor implements TransactionProcessor {

    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    @Override
    public int getType() {
        return TxType.HETEROGENEOUS_MAIN_ASSET_BIND;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (converterCoreApi.isProtocol23()) {
            return this.validateP23(chainId, txs, txMap, blockHeader);
        } else {
            return this._validate(chainId, txs, txMap, blockHeader);
        }
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, false);
    }

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            for (Transaction tx : txs) {
                HeterogeneousMainAssetBindTxData txData = new HeterogeneousMainAssetBindTxData();
                txData.parse(tx.getTxData(), 0);
                Integer hChainId = txData.getChainId();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                HeterogeneousAssetInfo mainAsset = docking.getMainAsset();
                // 异构链主资产绑定
                ledgerAssetRegisterHelper.htgMainAssetBind(txData.getNerveAssetChainId(), txData.getNerveAssetId(), hChainId, mainAsset.getAssetId(),
                        mainAsset.getSymbol(), mainAsset.getDecimals(), mainAsset.getSymbol(), mainAsset.getContractAddress());
                chain.getLogger().info("[commit] 异构链主资产绑定, nerveAssetChainId: {}, nerveAssetId: {}, chainId: {}, assetId: {}, symbol: {}, decimals: {}", txData.getNerveAssetChainId(), txData.getNerveAssetId(), hChainId, mainAsset.getAssetId(), mainAsset.getSymbol(), mainAsset.getDecimals());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, false);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            for (Transaction tx : txs) {
                HeterogeneousMainAssetBindTxData txData = new HeterogeneousMainAssetBindTxData();
                txData.parse(tx.getTxData(), 0);
                Integer hChainId = txData.getChainId();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                HeterogeneousAssetInfo info = docking.getMainAsset();
                ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hChainId, info.getAssetId());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    private Map<String, Object> _validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        Map<String, Object> result = null;
        try {
            String errorCode = ConverterErrorCode.DATA_ERROR.getCode();
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();

            for (Transaction tx : txs) {
                // 异构合约主资产注册
                HeterogeneousMainAssetBindTxData txData = new HeterogeneousMainAssetBindTxData();
                txData.parse(tx.getTxData(), 0);
                // 签名验证(种子虚拟银行)
                ConverterSignValidUtil.validateSeedNodeSign(chain, tx);
                // only (nuls & EVM:enuls) (eth & EVM:goerliETH)
                if (!HeterogeneousUtil.checkHeterogeneousMainAssetBind(chainId, txData.getChainId(), txData.getNerveAssetChainId(), txData.getNerveAssetId())) {
                    logger.error("异构链主资产不支持绑定到nerve资产: {}-{}", txData.getNerveAssetChainId(), txData.getNerveAssetId());
                    ErrorCode error = ConverterErrorCode.NO_LONGER_SUPPORTED;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
                    continue;
                }
                Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, txData.getNerveAssetChainId(), txData.getNerveAssetId());
                boolean existNerveAsset = nerveAsset != null;
                if (!existNerveAsset) {
                    logger.error("Nerve资产不存在: {}-{}", txData.getNerveAssetChainId(), txData.getNerveAssetId());
                    ErrorCode error = ConverterErrorCode.ASSET_ID_NOT_EXIST;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
                    continue;
                }

                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getChainId());
                HeterogeneousAssetInfo mainAsset = docking.getMainAsset();
                NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(mainAsset.getChainId(), mainAsset.getAssetId());
                // 新注册，主资产不能存在
                if(nerveAssetInfo != null && !nerveAssetInfo.isEmpty()) {
                    logger.error("异构链主资产已存在");
                    ErrorCode error = ConverterErrorCode.ASSET_EXIST;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
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

    private Map<String, Object> validateP23(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        Map<String, Object> result = null;
        try {
            String errorCode = ConverterErrorCode.DATA_ERROR.getCode();
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();

            for (Transaction tx : txs) {
                // 异构合约主资产注册
                HeterogeneousMainAssetBindTxData txData = new HeterogeneousMainAssetBindTxData();
                txData.parse(tx.getTxData(), 0);
                // 签名验证(种子虚拟银行)
                ConverterSignValidUtil.validateSeedNodeSign(chain, tx);
                // only (nuls & EVM:enuls) (eth & EVM:goerliETH)
                if (!HeterogeneousUtil.checkHeterogeneousMainAssetBind(chainId, txData.getChainId(), txData.getNerveAssetChainId(), txData.getNerveAssetId())) {
                    logger.error("异构链主资产不支持绑定到nerve资产: {}-{}", txData.getNerveAssetChainId(), txData.getNerveAssetId());
                    ErrorCode error = ConverterErrorCode.NO_LONGER_SUPPORTED;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
                    continue;
                }
                Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, txData.getNerveAssetChainId(), txData.getNerveAssetId());
                boolean existNerveAsset = nerveAsset != null;
                if (!existNerveAsset) {
                    logger.error("Nerve资产不存在: {}-{}", txData.getNerveAssetChainId(), txData.getNerveAssetId());
                    ErrorCode error = ConverterErrorCode.ASSET_ID_NOT_EXIST;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
                    continue;
                }

                boolean needCheck = true;
                if (txData.getChainId() == 119 && chainId == 9) {
                    NerveAssetInfo assetInfo = converterCoreApi.getHtgMainAsset(119);
                    if (assetInfo != null && !assetInfo.isEmpty() && (assetInfo.getAssetChainId() != 1 || assetInfo.getAssetId() != 1)) {
                        needCheck = false;
                    }
                }
                if (needCheck) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getChainId());
                    HeterogeneousAssetInfo mainAsset = docking.getMainAsset();
                    NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(mainAsset.getChainId(), mainAsset.getAssetId());
                    // 新注册，主资产不能存在
                    if(nerveAssetInfo != null && !nerveAssetInfo.isEmpty()) {
                        logger.error("异构链主资产已存在");
                        ErrorCode error = ConverterErrorCode.ASSET_EXIST;
                        errorCode = error.getCode();
                        logger.error(error.getMsg());
                        failsList.add(tx);
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

}
