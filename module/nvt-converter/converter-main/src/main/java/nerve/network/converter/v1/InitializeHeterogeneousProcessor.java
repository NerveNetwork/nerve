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

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.txdata.InitializeHeterogeneousTxData;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.utils.ConverterSignValidUtil;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.*;

/**
 * @author: Chino
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
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            Set<Integer> setDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
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
                    ConverterSignValidUtil.validateSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue outer;
                }
                setDuplicate.add(heterogeneousChainId);
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
        Chain chain = chainManager.getChain(chainId);
        try {
            SignAccountDTO signAccountDTO = virtualBankService.isCurrentDirector(chain);
            if (null != signAccountDTO) {
                for (Transaction tx : txs) {
                    //放入类似队列处理机制 准备通知异构链组件执行提现
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                    chain.getLogger().debug("[commit] 初始化异构链交易 hash:{}", tx.getHash().toHex());
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
