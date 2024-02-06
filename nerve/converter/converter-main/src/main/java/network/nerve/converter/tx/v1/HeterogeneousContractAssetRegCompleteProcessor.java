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
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.BindHeterogeneousContractMode;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegCompleteTxData;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static network.nerve.converter.constant.ConverterConstant.IN;
import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@Component("HeterogeneousContractAssetRegCompleteV1")
public class HeterogeneousContractAssetRegCompleteProcessor implements TransactionProcessor {

    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;

    @Override
    public int getType() {
        return TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_COMPLETE;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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

            Set<String> contractAssetRegSet = new HashSet<>();
            Set<String> bindNewSet = new HashSet<>();
            Set<String> bindRemoveSet = new HashSet<>();
            Set<String> bindOverrideSet = new HashSet<>();
            Set<String> unregisterSet = new HashSet<>();
            Set<String> pauseSet = new HashSet<>();
            Set<String> resumeSet = new HashSet<>();
            Set<String> stableSwapCoinSet = new HashSet<>();
            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if(coinData != null && coinData.length > 0){
                    // coindataExisting data(coinDataThere should be no data available)
                    throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
                }
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                String contractAddress = addressToLowerCase(txData.getContractAddress());
                errorCode = ledgerAssetRegisterHelper.checkHeterogeneousContractAssetReg(chain, tx, contractAddress, txData.getDecimals(), txData.getSymbol(), txData.getChainId(),
                        contractAssetRegSet, bindNewSet, bindRemoveSet, bindOverrideSet, unregisterSet, pauseSet, resumeSet, stableSwapCoinSet, false);
                if (StringUtils.isNotBlank(errorCode)) {
                    failsList.add(tx);
                    continue;
                }
                // Signature Byzantine Verification
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    logger.error(e.getErrorCode().getMsg());
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
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
                info.setChainId(txData.getChainId());
                info.setContractAddress(addressToLowerCase(txData.getContractAddress()));
                info.setSymbol(txData.getSymbol());
                info.setDecimals(txData.getDecimals());
                Integer hChainId = txData.getChainId();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                // Heterogeneous Contract Asset Registration OR NERVEAsset binding heterogeneous contract assets: New binding / Overwrite binding
                boolean isBindNew = false, isBindRemove = false, isBindOverride = false, isUnregister = false, isPause = false, isResume = false, isStablePause = false;
                do {
                    byte[] remark = tx.getRemark();
                    if (remark == null) {
                        break;
                    }
                    String strRemark = new String(remark, StandardCharsets.UTF_8);
                    if (!strRemark.contains(":")) {
                        break;
                    }
                    String[] modeSplit = strRemark.split(":");
                    String mode = modeSplit[0];
                    String assetContactInfo = modeSplit[1];
                    if (!assetContactInfo.contains("-")) {
                        break;
                    }
                    String[] asset = assetContactInfo.split("-");
                    int assetChainId = Integer.parseInt(asset[0]);
                    int assetId = Integer.parseInt(asset[1]);
                    if (BindHeterogeneousContractMode.NEW.toString().equals(mode)) {
                        // NERVEAsset binding heterogeneous contract assets: New binding
                        docking.saveHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, info.getAssetId(),
                                info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                        isBindNew = true;
                        chain.getLogger().info("[commit] NERVEAsset binding heterogeneous chain contract assets[New binding], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    } else if (BindHeterogeneousContractMode.REMOVE.toString().equals(mode)) {
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        HeterogeneousAssetInfo hAssetInfo = info;
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hChainId, hAssetInfo.getAssetId());
                        isBindRemove = true;
                        chain.getLogger().info("[commit] NERVEUnbind assets to heterogeneous chain contract assets[Unbind], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, hAssetInfo.getAssetId(), hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getContractAddress());
                    } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                        // NERVEAsset binding heterogeneous contract assets: Overwrite binding
                        HeterogeneousAssetInfo hAssetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hAssetInfo.getChainId(), hAssetInfo.getAssetId());
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, hAssetInfo.getAssetId(),
                                hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getSymbol(), hAssetInfo.getContractAddress());
                        isBindOverride = true;
                        chain.getLogger().info("[commit] NERVEAsset binding heterogeneous chain contract assets[Overwrite binding], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, hAssetInfo.getAssetId(), hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getContractAddress());
                    } else if (BindHeterogeneousContractMode.UNREGISTER.toString().equals(mode)) {
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.deleteCrossChainAsset(hChainId, info.getAssetId());
                        isUnregister = true;
                        chain.getLogger().info("[commit] NERVEAsset deregistration of heterogeneous chain contract assets[Cancel registration], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    } else if (BindHeterogeneousContractMode.PAUSE.toString().equals(mode)) {
                        int hAssetId = 1;
                        if (StringUtils.isNotBlank(info.getContractAddress())) {
                            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                            hAssetId = assetInfo.getAssetId();
                        }
                        info.setAssetId(hAssetId);
                        String operationType = modeSplit[2];
                        boolean isIn = IN.equalsIgnoreCase(operationType);
                        if (isIn) {
                            ledgerAssetRegisterHelper.pauseIn(hChainId, hAssetId);
                        } else {
                            ledgerAssetRegisterHelper.pauseOut(hChainId, hAssetId);
                        }
                        isPause = true;
                        chain.getLogger().info("[commit] Heterogeneous Chain Contract Assets[{}suspend], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                isIn ? "Recharge" : "Withdrawal", assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    } else if (BindHeterogeneousContractMode.RESUME.toString().equals(mode)) {
                        int hAssetId = 1;
                        if (StringUtils.isNotBlank(info.getContractAddress())) {
                            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                            hAssetId = assetInfo.getAssetId();
                        }
                        info.setAssetId(hAssetId);
                        String operationType = modeSplit[2];
                        boolean isIn = IN.equalsIgnoreCase(operationType);
                        if (isIn) {
                            ledgerAssetRegisterHelper.resumeIn(hChainId, hAssetId);
                        } else {
                            ledgerAssetRegisterHelper.resumeOut(hChainId, hAssetId);
                        }
                        isResume = true;
                    chain.getLogger().info("[commit] Heterogeneous Chain Contract Assets[{}recovery], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                            isIn ? "Recharge" : "Withdrawal", assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    } else if (BindHeterogeneousContractMode.STABLE_SWAP_COIN_PAUSE.toString().equals(mode)) {
                        String stableAddress = asset[2];
                        String status = asset[3];
                        SwapCall.pauseCoinForStable(chainId, stableAddress, assetChainId, assetId, status);
                        isStablePause = true;
                        chain.getLogger().info("[commit] Multi-Routing-Pool[{}-{}], NERVE asset: {}-{}, Heterogeneous Chain Asset Information: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                stableAddress, status, assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    }

                } while (false);
                // Heterogeneous Contract Asset Registration
                if (!isBindNew && !isBindRemove && !isBindOverride && !isUnregister && !isPause && !isResume && !isStablePause) {
                    docking.saveHeterogeneousAssetInfos(List.of(info));
                    ledgerAssetRegisterHelper.crossChainAssetReg(chainId, hChainId, info.getAssetId(),
                            info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                    chain.getLogger().info("[commit] Heterogeneous Chain Contract Asset Registration, chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}", hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                }
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
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
                info.setChainId(txData.getChainId());
                info.setContractAddress(txData.getContractAddress());
                info.setSymbol(txData.getSymbol());
                info.setDecimals(txData.getDecimals());

                Integer hChainId = txData.getChainId();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                // Heterogeneous Contract Asset Registration OR NERVEAsset binding heterogeneous contract assets: New binding / Overwrite binding
                boolean isBindNew = false, isBindRemove = false, isBindOverride = false, isUnregister = false, isPause = false, isResume = false, isStablePause = false;
                do {
                    byte[] remark = tx.getRemark();
                    if (remark == null) {
                        break;
                    }
                    String strRemark = new String(remark, StandardCharsets.UTF_8);
                    if (!strRemark.contains(":")) {
                        break;
                    }
                    String[] modeSplit = strRemark.split(":");
                    String mode = modeSplit[0];
                    String assetContactInfo = modeSplit[1];
                    if (!assetContactInfo.contains("-")) {
                        break;
                    }
                    String[] asset = assetContactInfo.split("-");
                    int assetChainId = Integer.parseInt(asset[0]);
                    int assetId = Integer.parseInt(asset[1]);
                    if (BindHeterogeneousContractMode.NEW.toString().equals(mode)) {
                        // NERVEAsset binding heterogeneous contract assets: New binding
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hChainId, info.getAssetId());
                        isBindNew = true;
                    } else if (BindHeterogeneousContractMode.REMOVE.toString().equals(mode)) {
                        docking.saveHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, info.getAssetId(),
                                info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                        isBindRemove = true;
                    } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                        // NERVEAsset binding heterogeneous contract assets: Overwrite binding
                        HeterogeneousAssetInfo hAssetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hAssetInfo.getChainId(), hAssetInfo.getAssetId());
                        String oldAssetContactInfo = modeSplit[2];
                        String[] oldAsset = oldAssetContactInfo.split("-");
                        int oldAssetChainId = Integer.parseInt(oldAsset[0]);
                        int oldAssetId = Integer.parseInt(oldAsset[1]);
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(oldAssetChainId, oldAssetId, hChainId, hAssetInfo.getAssetId(),
                                hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getSymbol(), hAssetInfo.getContractAddress());
                        isBindOverride = true;
                    } else if (BindHeterogeneousContractMode.UNREGISTER.toString().equals(mode)) {
                        docking.saveHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.crossChainAssetReg(chainId, hChainId, info.getAssetId(),
                                info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                        isUnregister = true;
                    } else if (BindHeterogeneousContractMode.PAUSE.toString().equals(mode)) {
                        int hAssetId = 1;
                        if (StringUtils.isNotBlank(info.getContractAddress())) {
                            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                            hAssetId = assetInfo.getAssetId();
                        }
                        info.setAssetId(hAssetId);
                        String operationType = modeSplit[2];
                        boolean isIn = IN.equalsIgnoreCase(operationType);
                        if (isIn) {
                            ledgerAssetRegisterHelper.resumeIn(hChainId, hAssetId);
                        } else {
                            ledgerAssetRegisterHelper.resumeOut(hChainId, hAssetId);
                        }

                        isPause = true;
                    } else if (BindHeterogeneousContractMode.RESUME.toString().equals(mode)) {
                        int hAssetId = 1;
                        if (StringUtils.isNotBlank(info.getContractAddress())) {
                            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                            hAssetId = assetInfo.getAssetId();
                        }
                        info.setAssetId(hAssetId);
                        String operationType = modeSplit[2];
                        boolean isIn = IN.equalsIgnoreCase(operationType);
                        if (isIn) {
                            ledgerAssetRegisterHelper.pauseIn(hChainId, hAssetId);
                        } else {
                            ledgerAssetRegisterHelper.pauseOut(hChainId, hAssetId);
                        }
                        isResume = true;
                    } else if (BindHeterogeneousContractMode.STABLE_SWAP_COIN_PAUSE.toString().equals(mode)) {
                        String stableAddress = asset[2];
                        String status = asset[3];
                        if ("pause".equalsIgnoreCase(status)) {
                            status = "RESUME";
                        } else if ("resume".equalsIgnoreCase(status)) {
                            status = "PAUSE";
                        }
                        SwapCall.pauseCoinForStable(chainId, stableAddress, assetChainId, assetId, status);
                        isStablePause = true;
                    }

                } while (false);
                // Heterogeneous Contract Asset Registration
                if (!isBindNew && !isBindRemove && !isBindOverride && !isUnregister && !isPause && !isResume && !isStablePause) {
                    docking.rollbackHeterogeneousAssetInfos(List.of(info));
                    ledgerAssetRegisterHelper.deleteCrossChainAsset(hChainId, info.getAssetId());
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }


}
