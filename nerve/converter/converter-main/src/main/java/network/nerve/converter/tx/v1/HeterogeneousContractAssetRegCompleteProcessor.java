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
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegCompleteTxData;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;

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

            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if(coinData != null && coinData.length > 0){
                    // coindata存在数据(coinData应该没有数据)
                    throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
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
                List<HeterogeneousAssetInfo> assetInfos = group.computeIfAbsent(txData.getChainId(), key -> new ArrayList<>());
                HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
                info.setChainId(txData.getChainId());
                info.setContractAddress(txData.getContractAddress().toLowerCase());
                info.setSymbol(txData.getSymbol());
                info.setDecimals(txData.getDecimals());
                assetInfos.add(info);
            }
            Set<Map.Entry<Integer, List<HeterogeneousAssetInfo>>> entries = group.entrySet();
            for(Map.Entry<Integer, List<HeterogeneousAssetInfo>> entry : entries) {
                Integer hChainId = entry.getKey();
                List<HeterogeneousAssetInfo> assetInfos = entry.getValue();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                docking.saveHeterogeneousAssetInfos(assetInfos);
                for(HeterogeneousAssetInfo assetInfo : assetInfos) {
                    chain.getLogger().info("向账本登记异构链资产, chainId: {}, assetId: {}, symbol: {}, decimals: {}, address: {}", hChainId, assetInfo.getAssetId(), assetInfo.getSymbol(), assetInfo.getDecimals(), assetInfo.getContractAddress());
                    ledgerAssetRegisterHelper.crossChainAssetReg(chainId, hChainId, assetInfo.getAssetId(),
                            assetInfo.getSymbol(), assetInfo.getDecimals(), assetInfo.getSymbol(), assetInfo.getContractAddress());
                }
                if(chain.getLogger().isDebugEnabled()) {
                    chain.getLogger().info("[commit] 异构链[{}]合约资产注册完成 count:{}, detail: {}", hChainId, assetInfos.size(), JSONUtils.obj2json(assetInfos));
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
            Map<Integer, List<HeterogeneousAssetInfo>> group = new HashMap<>();
            for (Transaction tx : txs) {
                HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
                txData.parse(tx.getTxData(), 0);
                List<HeterogeneousAssetInfo> assetInfos = group.computeIfAbsent(txData.getChainId(), key -> new ArrayList<>());
                HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
                info.setChainId(txData.getChainId());
                info.setContractAddress(txData.getContractAddress());
                info.setSymbol(txData.getSymbol());
                info.setDecimals(txData.getDecimals());
                assetInfos.add(info);
            }
            Set<Map.Entry<Integer, List<HeterogeneousAssetInfo>>> entries = group.entrySet();
            for(Map.Entry<Integer, List<HeterogeneousAssetInfo>> entry : entries) {
                Integer hChainId = entry.getKey();
                List<HeterogeneousAssetInfo> assetInfos = entry.getValue();
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
                docking.rollbackHeterogeneousAssetInfos(assetInfos);
                for(HeterogeneousAssetInfo assetInfo : assetInfos) {
                    ledgerAssetRegisterHelper.crossChainAssetDelete(hChainId, assetInfo.getAssetId());
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }


}
