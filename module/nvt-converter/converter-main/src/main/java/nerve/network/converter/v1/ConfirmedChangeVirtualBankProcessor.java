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
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousConfirmedInfo;
import nerve.network.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import nerve.network.converter.model.po.ConfirmedChangeVirtualBankPO;
import nerve.network.converter.model.txdata.ConfirmWithdrawalTxData;
import nerve.network.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import nerve.network.converter.rpc.call.TransactionCall;
import nerve.network.converter.storage.CfmChangeBankStorageService;
import nerve.network.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import nerve.network.converter.utils.ConverterSignValidUtil;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.*;

/**
 * 确认虚拟银行变更交易
 *
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("ConfirmedChangeVirtualBankV1")
public class ConfirmedChangeVirtualBankProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;;

    @Override
    public int getType() {
        return TxType.CONFIRM_CHANGE_VIRTUAL_BANK;
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
            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            outer : for (Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                String originalHash = txData.getChangeVirtualBankTxHash().toHex();
                if(setDuplicate.contains(originalHash)){
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.BLOCK_TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.BLOCK_TX_DUPLICATION.getMsg());
                    continue;
                }
                ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, originalHash);
                if(null != po){
                    // 说明确认交易业务重复,该原始交易已经有一个确认交易 已确认
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.CFM_IS_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.CFM_IS_DUPLICATION.getMsg());
                    continue;
                }

                //获取银行变更交易
                Transaction changeVirtualBankTx = TransactionCall.getConfirmedTx(chain, txData.getChangeVirtualBankTxHash());
                if (null == changeVirtualBankTx) {
                    failsList.add(tx);
                    // Nerve提现交易不存在
                    errorCode = ConverterErrorCode.CHANGE_VIRTUAL_BANK_TX_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.CHANGE_VIRTUAL_BANK_TX_NOT_EXIST.getMsg());
                    continue;
                }

                List<byte[]> agents = txData.getListAgents();
                if(agents.size() != chain.getMapVirtualBank().values().size()){
                    failsList.add(tx);
                    // 虚拟银行列表不一致
                    errorCode = ConverterErrorCode.VIRTUAL_BANK_MISMATCH.getCode();
                    log.error(ConverterErrorCode.VIRTUAL_BANK_MISMATCH.getMsg());
                    continue;
                }
                //判断所有地址是否是当前虚拟银行节点
                for(byte[] addrByte : agents){
                    if(!chain.isVirtualBankByAgentAddr(AddressTool.getStringAddressByBytes(addrByte))){
                        failsList.add(tx);
                        // 虚拟银行列表不一致
                        errorCode = ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getCode();
                        log.error(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getMsg());
                        continue outer;
                    }
                }

                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();

                for(HeterogeneousConfirmedVirtualBank confirmed : listConfirmed) {
                    IHeterogeneousChainDocking HeterogeneousInterface =
                            heterogeneousDockingManager.getHeterogeneousDocking(confirmed.getHeterogeneousChainId());
                    if (null == HeterogeneousInterface) {
                        failsList.add(tx);
                        // 异构链不存在
                        errorCode = ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST.getMsg());
                        continue outer;
                    }
                    HeterogeneousConfirmedInfo info = HeterogeneousInterface.getChangeVirtualBankConfirmedTxInfo(confirmed.getHeterogeneousTxHash());
                    if(null == info) {
                        failsList.add(tx);
                        // 变更交易不存在
                        errorCode = ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getCode();
                        log.error(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getMsg());
                        continue outer;
                    }

                    if(confirmed.getEffectiveTime() != info.getTxTime()){
                        failsList.add(tx);
                        // 异构交易生效时间不匹配
                        errorCode = ConverterErrorCode.HETEROGENEOUS_TX_TIME_MISMATCH.getCode();
                        log.error(ConverterErrorCode.HETEROGENEOUS_TX_TIME_MISMATCH.getMsg());
                        continue outer;
                    }
                    if(!confirmed.getHeterogeneousAddress().equals(info.getMultySignAddress())){
                        failsList.add(tx);
                        // 变更交易数据不匹配
                        errorCode = ConverterErrorCode.VIRTUAL_BANK_MULTIADDRESS_MISMATCH.getCode();
                        log.error(ConverterErrorCode.VIRTUAL_BANK_MULTIADDRESS_MISMATCH.getMsg());
                        continue outer;
                    }
                }
                // 签名验证
                try {
                    ConverterSignValidUtil.validateSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
                setDuplicate.add(originalHash);
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
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    private boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for(Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                cfmChangeBankStorageService.save(chain,
                        new ConfirmedChangeVirtualBankPO(txData.getChangeVirtualBankTxHash(), tx.getHash()));
                // 更新异构链组件交易状态 // add by pierre at 2020-03-12
                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                for(HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                    heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedCompleted(bank.getHeterogeneousTxHash(), blockHeader.getHeight());
                }
                // 移除收集的异构链确认信息 // add by pierre at 2020-03-12
                heterogeneousConfirmedChangeVBStorageService.deleteByTxHash(tx.getHash().toHex());
                chain.getLogger().debug("[commit] 确认虚拟银行变更交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    private boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for(Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                cfmChangeBankStorageService.delete(chain,txData.getChangeVirtualBankTxHash().toHex());
                // 更新异构链组件交易状态 // add by pierre at 2020-03-13
                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                for(HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                    heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedRollback(bank.getHeterogeneousTxHash());
                }
                chain.getLogger().debug("[rollback] 确认虚拟银行变更交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}
