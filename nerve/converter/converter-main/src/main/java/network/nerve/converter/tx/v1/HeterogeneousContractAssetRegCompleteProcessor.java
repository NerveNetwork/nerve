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
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

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
            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if(coinData != null && coinData.length > 0){
                    // coindata存在数据(coinData应该没有数据)
                    throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
                }
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                String contractAddress = txData.getContractAddress().toLowerCase();
                errorCode = ledgerAssetRegisterHelper.checkHeterogeneousContractAssetReg(chain, tx, contractAddress, txData.getDecimals(), txData.getSymbol(), txData.getChainId(), contractAssetRegSet, bindNewSet, bindRemoveSet, bindOverrideSet, unregisterSet, false);
                if (StringUtils.isNotBlank(errorCode)) {
                    failsList.add(tx);
                    continue;
                }
                // 签名拜占庭验证
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
            Map<Integer, List<HeterogeneousAssetInfo>> group = new HashMap<>();
            for (Transaction tx : txs) {
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
                info.setChainId(txData.getChainId());
                info.setContractAddress(txData.getContractAddress().toLowerCase());
                info.setSymbol(txData.getSymbol());
                info.setDecimals(txData.getDecimals());
                Integer hChainId = txData.getChainId();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                // 异构合约资产注册 OR NERVE资产绑定异构合约资产: 新绑定 / 覆盖绑定
                boolean isBindNew = false, isBindRemove = false, isBindOverride = false, isUnregister = false;
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
                        // NERVE资产绑定异构合约资产: 新绑定
                        docking.saveHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, info.getAssetId(),
                                info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                        isBindNew = true;
                        chain.getLogger().info("[commit] NERVE资产绑定异构链合约资产[新绑定], NERVE asset: {}-{}, 异构链资产信息: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    } else if (BindHeterogeneousContractMode.REMOVE.toString().equals(mode)) {
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        HeterogeneousAssetInfo hAssetInfo = info;
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hChainId, hAssetInfo.getAssetId());
                        isBindRemove = true;
                        chain.getLogger().info("[commit] NERVE资产取消绑定异构链合约资产[取消绑定], NERVE asset: {}-{}, 异构链资产信息: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, hAssetInfo.getAssetId(), hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getContractAddress());
                    } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                        // NERVE资产绑定异构合约资产: 覆盖绑定
                        HeterogeneousAssetInfo hAssetInfo = docking.getAssetByContractAddress(info.getContractAddress());
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hAssetInfo.getChainId(), hAssetInfo.getAssetId());
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, hAssetInfo.getAssetId(),
                                hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getSymbol(), hAssetInfo.getContractAddress());
                        isBindOverride = true;
                        chain.getLogger().info("[commit] NERVE资产绑定异构链合约资产[覆盖绑定], NERVE asset: {}-{}, 异构链资产信息: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, hAssetInfo.getAssetId(), hAssetInfo.getSymbol(), hAssetInfo.getDecimals(), hAssetInfo.getContractAddress());
                    } else if (BindHeterogeneousContractMode.UNREGISTER.toString().equals(mode)) {
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.deleteCrossChainAsset(hChainId, info.getAssetId());
                        isUnregister = true;
                        chain.getLogger().info("[commit] NERVE资产取消注册异构链合约资产[取消注册], NERVE asset: {}-{}, 异构链资产信息: chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}",
                                assetChainId, assetId, hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
                    }

                } while (false);
                // 异构合约资产注册
                if (!isBindNew && !isBindRemove && !isBindOverride && !isUnregister) {
                    docking.saveHeterogeneousAssetInfos(List.of(info));
                    ledgerAssetRegisterHelper.crossChainAssetReg(chainId, hChainId, info.getAssetId(),
                            info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                    chain.getLogger().info("[commit] 异构链合约资产注册, chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}", hChainId, info.getAssetId(), info.getSymbol(), info.getDecimals(), info.getContractAddress());
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
                // 异构合约资产注册 OR NERVE资产绑定异构合约资产: 新绑定 / 覆盖绑定
                boolean isBindNew = false, isBindRemove = false, isBindOverride = false, isUnregister = false;
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
                        // NERVE资产绑定异构合约资产: 新绑定
                        docking.rollbackHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.deleteCrossChainAssetByExistNerveAsset(hChainId, info.getAssetId());
                        isBindNew = true;
                    } else if (BindHeterogeneousContractMode.REMOVE.toString().equals(mode)) {
                        docking.saveHeterogeneousAssetInfos(List.of(info));
                        ledgerAssetRegisterHelper.crossChainAssetRegByExistNerveAsset(assetChainId, assetId, hChainId, info.getAssetId(),
                                info.getSymbol(), info.getDecimals(), info.getSymbol(), info.getContractAddress());
                        isBindRemove = true;
                    } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                        // NERVE资产绑定异构合约资产: 覆盖绑定
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
                    }

                } while (false);
                // 异构合约资产注册
                if (!isBindNew && !isBindRemove && !isBindOverride && !isUnregister) {
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
