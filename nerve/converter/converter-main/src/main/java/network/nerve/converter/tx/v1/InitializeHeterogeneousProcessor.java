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
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020/3/23
 */
@Component("InitializeHeterogeneousV1")
public class InitializeHeterogeneousProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private VirtualBankService virtualBankService;

    @Override
    public int getType() {
        return TxType.INITIALIZE_HETEROGENEOUS;
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
            String errorCode = ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode();
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
           /* Set<Integer> setDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if(coinData != null && coinData.length > 0){
                    // coindata存在数据(coinData应该没有数据)
                    throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
                }
                InitializeHeterogeneousTxData txData = ConverterUtil.getInstance(tx.getTxData(), InitializeHeterogeneousTxData.class);
                int heterogeneousChainId = txData.getHeterogeneousChainId();
                // 检查异构链id存在
                IHeterogeneousChainDocking HeterogeneousInterface =
                        heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                if (null == HeterogeneousInterface) {
                    failsList.add(tx);
                    // 异构链不存在
                    errorCode = ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST.getMsg());
                    continue outer;
                }
                if (setDuplicate.contains(heterogeneousChainId)) {
                    // 判断区块内重复初始化异构链交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.HETEROGENEOUS_INIT_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_INIT_DUPLICATION.getMsg());
                    continue;
                }

                List<byte[]> listVirtualBankAgent = new ArrayList<>();
                for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {

                    if (!director.getSeedNode()) {
                        // 判断已确认交易中重复初始化异构链交易
                        if (director.getHeterogeneousAddrMap().containsKey(txData.getHeterogeneousChainId())) {
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.HETEROGENEOUS_HAS_BEEN_INITIALIZED.getCode();
                            log.error(ConverterErrorCode.HETEROGENEOUS_HAS_BEEN_INITIALIZED.getMsg());
                            continue outer;
                        }
                        listVirtualBankAgent.add(AddressTool.getAddress(director.getAgentAddress()));
                    }
                }

                // 验签名
                try {
                    ConverterSignValidUtil.validateVirtualBankSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue outer;
                }
                setDuplicate.add(heterogeneousChainId);
            }*/
            // 暂时关闭该交易
            failsList.addAll(txs);
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
        Chain chain = chainManager.getChain(chainId);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            if (isCurrentDirector) {
                for (Transaction tx : txs) {
                    //放入类似队列处理机制 准备通知异构链组件执行
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                    chain.getLogger().info("[commit] 初始化异构链交易 hash:{}", tx.getHash().toHex());
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
        return false;
    }
}
