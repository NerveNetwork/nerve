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
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousAssetInfo;
import nerve.network.converter.model.txdata.HeterogeneousContractAssetRegTxData;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;

import java.util.*;

/**
 * @author: Chino
 * @date: 2020-03-23
 */
@Component("HeterogeneousContractAssetRegV1")
public class HeterogeneousContractAssetRegProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.HETEROGENEOUS_CONTRACT_ASSET_REG;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

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
                HeterogeneousContractAssetRegTxData txData = new HeterogeneousContractAssetRegTxData();
                txData.parse(tx.getTxData(), 0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getChainId());
                HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(txData.getContractAddress());
                // 资产已存在
                if(assetInfo != null) {
                    logger.error("资产已存在");
                    ErrorCode error = ConverterErrorCode.ASSET_EXIST;
                    errorCode = error.getCode();
                    logger.error(error.getMsg());
                    failsList.add(tx);
                    continue;
                }
                // 资产信息验证
                if (!docking.validateHeterogeneousAssetInfoFromNet(txData.getContractAddress(), txData.getSymbol(), txData.getDecimals())) {
                    logger.error("资产信息不匹配");
                    ErrorCode error = ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY;
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
                HeterogeneousContractAssetRegTxData txData = new HeterogeneousContractAssetRegTxData();
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
                docking.saveHeterogeneousAssetInfos(assetInfos);
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
                HeterogeneousContractAssetRegTxData txData = new HeterogeneousContractAssetRegTxData();
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
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}
