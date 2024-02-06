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
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegPendingTxData;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@Component("HeterogeneousContractAssetRegPendingV1")
public class HeterogeneousContractAssetRegPendingProcessor implements TransactionProcessor {

    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    @Override
    public int getType() {
        return TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING;
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
                // Heterogeneous Contract Asset Registration OR (NERVEAsset binding heterogeneous contract assets: New binding / Overwrite binding / Unbind) OR Unregistration of heterogeneous contract assets OR (Heterogeneous chain asset recharge suspend / recovery)
                HeterogeneousContractAssetRegPendingTxData txData = new HeterogeneousContractAssetRegPendingTxData();
                txData.parse(tx.getTxData(), 0);
                String contractAddress = addressToLowerCase(txData.getContractAddress());
                // Signature verification(Seed Virtual Bank)
                try {
                    ConverterSignValidUtil.validateSeedNodeSign(chain, tx);
                } catch (NulsException e) {
                    errorCode = e.getErrorCode().getCode();
                    failsList.add(tx);
                    continue;
                }
                errorCode = ledgerAssetRegisterHelper.checkHeterogeneousContractAssetReg(chain, tx, contractAddress, txData.getDecimals(), txData.getSymbol(), txData.getChainId(),
                        contractAssetRegSet, bindNewSet, bindRemoveSet, bindOverrideSet, unregisterSet, pauseSet, resumeSet, stableSwapCoinSet, false);
                if (StringUtils.isNotBlank(errorCode)) {
                    failsList.add(tx);
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
        // When the block is in normal operation state（Non block synchronization mode）Only then will it be executed
        if (syncStatus == SyncStatusEnum.RUNNING.value()) {
            Chain chain = chainManager.getChain(chainId);
            try {
                boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
                if (isCurrentDirector) {
                    for (Transaction tx : txs) {
                        //Placing a queue like processing mechanism Prepare to notify heterogeneous chain components to perform contract asset registration
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        chain.getPendingTxQueue().offer(pendingPO);
                        if(chain.getLogger().isDebugEnabled()) {
                            chain.getLogger().info("[commit] Contract asset registration waiting for transaction hash:{}", tx.getHash().toHex());
                        }
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
        return true;
    }

}
