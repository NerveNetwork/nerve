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

package network.nerve.converter.core.thread.task;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 交易确认后续处理定时任务
 * 处理交易后续的异构链组件的调用,关联生成新的交易等
 *
 * @author: Loki
 * @date: 2020-03-10
 */
public class CfmTxSubsequentProcessTask implements Runnable {
    private Chain chain;

    public CfmTxSubsequentProcessTask(Chain chain) {
        this.chain = chain;
    }

    private HeterogeneousDockingManager heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService = SpringLiteContext.getBean(AsyncProcessedTxStorageService.class);
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService = SpringLiteContext.getBean(TxSubsequentProcessStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private VirtualBankStorageService virtualBankStorageService = SpringLiteContext.getBean(VirtualBankStorageService.class);
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService = SpringLiteContext.getBean(VirtualBankAllHistoryStorageService.class);
    private VirtualBankService virtualBankService = SpringLiteContext.getBean(VirtualBankService.class);
    private HeterogeneousService heterogeneousService = SpringLiteContext.getBean(HeterogeneousService.class);
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService =
            SpringLiteContext.getBean(HeterogeneousAssetConverterStorageService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            out:
            while (!pendingTxQueue.isEmpty()) {
                // 只取出,不移除头部元素
                TxSubsequentProcessPO pendingPO = pendingTxQueue.peekFirst();
                Transaction tx = pendingPO.getTx();
                if (null != asyncProcessedTxStorageService.getComponentCall(chain, tx.getHash().toHex())) {
                    // 判断已执行过, 从队列中移除, 并从持久库中移除
                    // 执行成功移除队列头部元素
                    pendingTxQueue.remove();
                    chain.getLogger().debug("[异构链待处理队列] 判断已执行过移除交易, hash:{}", tx.getHash().toHex());
                    // 并且从持久化库中移除
                    txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                    continue;
                }
                // 判断是否已确认
                if (null == TransactionCall.getConfirmedTx(chain, tx.getHash())) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        // 移除
                        pendingTxQueue.remove();
                        chain.getLogger().debug("[异构链待处理队列] 交易未确认(移除处理), hash:{}", tx.getHash().toHex());
                        // 并且从持久化库中移除
                        txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                        continue;
                    }
                    pendingPO.setIsConfirmedVerifyCount(pendingPO.getIsConfirmedVerifyCount() + 1);
                    // 终止本次执行，等待下一次执行再次检查交易是否确认
                    break;
                }
                switch (tx.getType()) {
                    case TxType.CHANGE_VIRTUAL_BANK:
                        // 处理银行变更
                        if (chain.getHeterogeneousChangeBankExecuting().get()) {
                            // 有虚拟银行变更异构链交易正在执行中, 暂停新的异构处理
                            chain.getLogger().info("[Task-CHANGE_VIRTUAL_BANK]  pause new change 正在执行虚拟银行变更异构链交易, 暂停新的银行变更异构处理!");
                            break out;
                        }
                        changeVirtualBankProcessor();
                        // 后续已批量处理完成
                        continue;
                    case TxType.WITHDRAWAL:
                        // 处理提现
                        if (chain.getHeterogeneousChangeBankExecuting().get()) {
                            // 有虚拟银行变更异构链交易正在执行中, 暂停新的异构处理
                            chain.getLogger().info("[Task-change_virtual_bank] pause withdrawal 正在执行虚拟银行变更异构链交易, 暂停新的提现异构处理!");
                            break out;
                        }

                        withdrawalProcessor(pendingPO);
                        break;
                    case TxType.CONFIRM_WITHDRAWAL:
                        // 确认提现交易 处理补贴手续费交易
                        confirmWithdrawalDistributionFee(pendingPO);
                        break;
                    case TxType.INITIALIZE_HETEROGENEOUS:
                        // 初始化加入新的异构链
                        initializeHeterogeneousProcessor(pendingPO);
                        break;
                    case TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING:
                        // 处理异构链合约资产注册
                        heterogeneousContractAssetRegCompleteProcessor(pendingPO);
                        break;
                    case TxType.CONFIRM_PROPOSAL:
                        // 确认提案补贴手续费
                        confirmProposalDistributionFee(pendingPO);
                        break;
                    case TxType.RESET_HETEROGENEOUS_VIRTUAL_BANK:
                        resetHeterogeneousVirtualBank(pendingPO);
                        // 后续已批量处理完成
                        continue;
                    default:
                }
                if (chain.getCurrentIsDirector().get()) {
                    // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
                    asyncProcessedTxStorageService.saveComponentCall(chain, tx.getHash().toHex());
                }
                // 执行成功移除队列头部元素
                pendingTxQueue.remove();
                chain.getLogger().debug("[异构链待处理队列] 执行成功移除交易, hash:{}", tx.getHash().toHex());
                // 并且从持久化库中移除
                txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 返回 从队列中取出虚拟银行变更交易来进行合并的 交易列表
     *
     * @param count 最多从队列中取几个交易来进行合并, 为null则不限制(满足条件全部取出)
     * @return
     * @throws NulsException
     */
    private List<TxSubsequentProcessPO> getChangeVirtualBankTxs(Integer count) {
        List<TxSubsequentProcessPO> mergeList = new ArrayList<>();
        Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
        int current = 0;
        while (it.hasNext()) {
            TxSubsequentProcessPO po = it.next();
            Transaction tx = po.getTx();
            if (tx.getType() == TxType.CHANGE_VIRTUAL_BANK) {
                mergeList.add(po);
                if (null != count && count == ++current) {
                    break;
                }
            }
        }
        return mergeList;
    }

    /**
     * 处理虚拟银行变更异构链部分的业务
     * 变更异构链多签地址/合约成员
     *
     * @throws NulsException
     */
    private void changeVirtualBankProcessor() throws NulsException {

        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        List<TxSubsequentProcessPO> mergeList = getChangeVirtualBankTxs(null);
        if (mergeList.isEmpty()) {
            chain.getLogger().error("虚拟银行变更调用异构链时, 没有任何合并的交易数据. changeVirtualBank no merged tx list");
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        if (chain.getCurrentIsDirector().get()) {
            String key = mergeList.get(0).getTx().getHash().toHex();
            Result<Integer> result = changeVirtualBankAddress(key, mergeList, hInterfaces);
            if (result.isFailed() && result.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                // 根据返回的交易个数 重新合并组装
                mergeList = getChangeVirtualBankTxs(result.getData());
                if (mergeList.isEmpty()) {
                    chain.getLogger().error("虚拟银行变更调用异构链时, 二次获取没有任何合并的交易数据. changeVirtualBank no merged tx list");
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                String keyTwo = mergeList.get(0).getTx().getHash().toHex();
                if (!keyTwo.equals(key)) {
                    chain.getLogger().error("[虚拟银行变更] 二次合并交易 key不一致!");
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                Result<Integer> resultTwo = changeVirtualBankAddress(key, mergeList, hInterfaces);
                if (resultTwo.isFailed() && resultTwo.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                    chain.getLogger().error("[虚拟银行变更] 二次合并交易 发送扔失败!.");
                    throw new NulsException(resultTwo.getErrorCode());
                }
            }

            /**
             * 存储对应关系
             * key:合并列表中第一个交易hash, value:所有合并交易hash列表
             */
            List<String> hashList = new ArrayList<>();
            mergeList.forEach(k -> hashList.add(k.getTx().getHash().toHex()));
            txSubsequentProcessStorageService.saveMergeComponentCall(chain, key, new MergedComponentCallPO(hashList));
        }
        // 从队列移除元素
        chain.getPendingTxQueue().removeAll(mergeList);
        for (TxSubsequentProcessPO po : mergeList) {
            String hash = po.getTx().getHash().toHex();
            if (chain.getCurrentIsDirector().get()) {
                // 存储已执行成功的交易hash, 执行过的不再执行.
                asyncProcessedTxStorageService.saveComponentCall(chain, hash);
            }
            chain.getLogger().debug("[异构链待处理队列] 执行成功移除交易, hash:{}", hash);
            // 并且从持久化库中移除
            txSubsequentProcessStorageService.delete(chain, hash);
        }
    }

    /**
     * 移除in和out 相同的节点地址
     *
     * @param listAllInDirector
     * @param listAllOutDirector
     */
    public void removeDuplicateData(Set<VirtualBankDirector> listAllInDirector, Set<VirtualBankDirector> listAllOutDirector) {
        Set<VirtualBankDirector> notExit = new HashSet<>(listAllInDirector);
        Set<VirtualBankDirector> duplicate = new HashSet<>(listAllInDirector);
        // 从in移除所有out中重复的元素, 得到in中不在out的中元素
        notExit.removeAll(listAllOutDirector);
        // 从in移除in中不在out的中元素, 得到两个set集合相同的元素
        duplicate.removeAll(notExit);
        //分别移除
        listAllInDirector.removeAll(duplicate);
        listAllOutDirector.removeAll(duplicate);
    }

    /**
     * 虚拟银行公共地址的变更处理
     *
     * @param key
     * @param mergeList
     * @return
     * @throws NulsException
     */
    private Result<Integer> changeVirtualBankAddress(String key, List<TxSubsequentProcessPO> mergeList, List<IHeterogeneousChainDocking> hInterfaces) throws NulsException {
        // 合并发送
        Set<VirtualBankDirector> listAllInDirector = new HashSet<>();
        Set<VirtualBankDirector> listAllOutDirector = new HashSet<>();
        // 合并前原始数量
        int originalMergedCount = mergeList.size();
        chain.getLogger().debug("[bank-可合并的交易] size:{}", mergeList.size());
        for (TxSubsequentProcessPO pendingPO : mergeList) {
            chain.getLogger().debug("----------------------------------------");
            chain.getLogger().debug("[bank-可合并的交易] hash:{}", pendingPO.getTx().getHash().toHex());
            listAllInDirector.addAll(pendingPO.getListInDirector());
            listAllOutDirector.addAll(pendingPO.getListOutDirector());

            pendingPO.getListInDirector().forEach(s -> {
                chain.getLogger().debug("InAddress: {}", s.getAgentAddress());
            });

            pendingPO.getListOutDirector().forEach(s -> {
                chain.getLogger().debug("OutAddress: {}", s.getAgentAddress());
            });
            chain.getLogger().debug("----------------------------------------");
        }
        int inCount = listAllInDirector.size();
        int outCount = listAllOutDirector.size();
        removeDuplicateData(listAllInDirector, listAllOutDirector);

        for (IHeterogeneousChainDocking hInterface : hInterfaces) {
            // 组装加入参数
            String[] inAddress = new String[inCount];
            if (!listAllInDirector.isEmpty()) {
                int i = 0;
                for (VirtualBankDirector director : listAllInDirector) {
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                    inAddress[i] = heterogeneousAddress.getAddress();
                    i++;
                }
            }

            String[] outAddress = new String[outCount];
            if (!listAllOutDirector.isEmpty()) {
                int i = 0;
                for (VirtualBankDirector director : listAllOutDirector) {
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                    outAddress[i] = heterogeneousAddress.getAddress();
                    i++;
                }
            }

            // 创建新的虚拟银行异构链多签地址/修改合约成员
            try {
                String hash = hInterface.createOrSignManagerChangesTx(
                        key,
                        inAddress,
                        outAddress,
                        originalMergedCount);
            } catch (NulsException e) {
                if (!e.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                    throw e;
                }
                try {
                    HeterogeneousChangePendingInfo Info = hInterface.getChangeVirtualBankPendingInfo(key);
                    int orginTxCount = Info.getOrginTxCount();
                    return Result.getFailed(e.getErrorCode()).setData(orginTxCount);
                } catch (Exception ex) {
                    chain.getLogger().error("[虚拟银行变更] 根据合并key获取异构链调用信息异常");
                    ex.printStackTrace();
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
                }
            }
            //  if(StringUtils.isNotBlank(hash))取消判断是因为 达到拜占庭之后, 有节点正常调用会返回空, 导致下面状态没变
            if (null == asyncProcessedTxStorageService.getComponentCall(chain, key)) {
                /**
                 * 调异构链成功
                 * 开启正在处理银行变更异构链交易模式
                 */
                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
            }
            chain.getLogger().debug("[bank-执行变更] key:{}, inAddress:{}, outAddress:{}",
                    key, Arrays.toString(inAddress), Arrays.toString(outAddress));
        }
        return Result.getSuccess(0);
    }


    private void heterogeneousContractAssetRegCompleteProcessor(TxSubsequentProcessPO pendingPO) {
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        try {
            Transaction tx = pendingPO.getTx();
            assembleTxService.createHeterogeneousContractAssetRegCompleteTx(chain, tx);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }


    private void initializeHeterogeneousProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        Transaction tx = pendingPO.getTx();
        // 维护虚拟银行 同步模式同样需要维护
        InitializeHeterogeneousTxData txData = ConverterUtil.getInstance(tx.getTxData(), InitializeHeterogeneousTxData.class);
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            if (!director.getSeedNode()) {
                // 为新成员创建异构链多签地址
                String heterogeneousAddress = heterogeneousInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                director.getHeterogeneousAddrMap().put(heterogeneousInterface.getChainId(),
                        new HeterogeneousAddress(heterogeneousInterface.getChainId(), heterogeneousAddress));
                virtualBankStorageService.save(chain, director);
                virtualBankAllHistoryStorageService.save(chain, director);
            }
        }
    }


    /**
     * 处理提现的交易
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void withdrawalProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        Transaction tx = pendingPO.getTx();
        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        CoinTo withdrawCoinTo = null;
        for (CoinTo coinTo : coinData.getTo()) {
            if (coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                withdrawCoinTo = coinTo;
                break;
            }
        }
        if (null == withdrawCoinTo) {
            chain.getLogger().error("[withdraw] Withdraw transaction cointo data error, no withdrawCoinTo. hash:{}", tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        int assetId = withdrawCoinTo.getAssetsId();
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(assetId);
        BigInteger amount = withdrawCoinTo.getAmount();
        String heterogeneousAddress = txData.getHeterogeneousAddress();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
        docking.createOrSignWithdrawTx(
                tx.getHash().toHex(),
                heterogeneousAddress,
                amount,
                heterogeneousAssetInfo.getAssetId());
    }


    /**
     * 确认提现交易 后续手续费补贴业务
     * 如果需要进行手续费补贴,则触发手续费补贴交易
     * 补贴交易时间统一为确认提现交易所在区块的时间
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void confirmWithdrawalDistributionFee(TxSubsequentProcessPO pendingPO) throws NulsException {
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmWithdrawalTxData.class);
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : txData.getListDistributionFee()) {
            String address = chain.getDirectorRewardAddress(addr);
            if(StringUtils.isBlank(address)){
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            assembleTxService.createDistributionFeeTx(chain, txData.getWithdrawalTxHash(), listRewardAddress, pendingPO.getBlockHeader().getTime());
        } catch (NulsException e) {
            if ("tx_0013".equals(e.getErrorCode().getCode())) {
                chain.getLogger().info("该补贴手续费交易已确认..(原始)提现确认交易txhash:{}", pendingPO.getTx().getHash().toHex());
            }
        }
    }

    /**
     * 补贴执行提案的手续费(例如 提现, 原路退回等)
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void confirmProposalDistributionFee(TxSubsequentProcessPO pendingPO) throws NulsException {
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        ConfirmProposalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmProposalTxData.class);
        List<HeterogeneousAddress> addressList;
        if (ProposalTypeEnum.UPGRADE.value() == txData.getType()) {
            ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
            addressList = upgradeTxData.getListDistributionFee();
        } else {
            ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
            addressList = businessData.getListDistributionFee();
        }
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : addressList) {
            String address = chain.getDirectorRewardAddress(addr);
            if(StringUtils.isBlank(address)){
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            // basisTxHash 为确认提案交易hash
            assembleTxService.createDistributionFeeTx(chain, pendingPO.getTx().getHash(), listRewardAddress, pendingPO.getBlockHeader().getTime());
        } catch (NulsException e) {
            if ("tx_0013".equals(e.getErrorCode().getCode())) {
                chain.getLogger().info("该补贴手续费交易已确认..(原始)提现确认交易txhash:{}", pendingPO.getTx().getHash().toHex());
            }
            chain.getLogger().error(e);
        }
    }

    /**
     * 重置虚拟银行异构链
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void resetHeterogeneousVirtualBank(TxSubsequentProcessPO pendingPO) throws NulsException {
        /**
         * 1.移除所有虚拟银行变更交易 (所有虚拟银行成员都要执行)
         * 2.发起重置异构链交易 (只有种子节点执行)
         */
        List<TxSubsequentProcessPO> changeVirtualBankTxs = getChangeVirtualBankTxs(null);
        // 1.移除队列中已被处理的元素
        chain.getPendingTxQueue().removeAll(changeVirtualBankTxs);
        for (TxSubsequentProcessPO po : changeVirtualBankTxs) {
            String hash = po.getTx().getHash().toHex();
            // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
            asyncProcessedTxStorageService.saveComponentCall(chain, hash);
            chain.getLogger().debug("[异构链待处理队列] 重置虚拟银行异构链 - 执行成功移除交易, hash:{}", hash);
            // 并且从持久化库中移除
            txSubsequentProcessStorageService.delete(chain, hash);
        }

        VirtualBankDirector currentDirector = virtualBankService.getCurrentDirector(chain.getChainId());
        if (null == currentDirector || !currentDirector.getSeedNode()) {
            chain.getLogger().info("[重置异构链交易-当前节点无需执行] Current director is not seed node, heterogeneous reset operation cannot be performed");
            return;
        }

        // 2.
        Transaction tx = pendingPO.getTx();
        ResetVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ResetVirtualBankTxData.class);
        int virtualBankSize = chain.getMapVirtualBank().size();
        String[] allManagers = new String[virtualBankSize];
        List<String> seedManagerList = new ArrayList<>();
        int i = 0;
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(txData.getHeterogeneousChainId());
            allManagers[i] = heterogeneousAddress.getAddress();
            if (director.getSeedNode()) {
                seedManagerList.add(heterogeneousAddress.getAddress());
            }
            i++;
        }
        String[] seedManagers = seedManagerList.toArray(new String[seedManagerList.size()]);

        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        chain.getLogger().debug("[重置虚拟银行异构链调用] 调用参数. hash :{}, seedManagers:{}, allManagers:{}",
                tx.getHash().toHex(), Arrays.toString(seedManagers), Arrays.toString(allManagers));
        String hash = null;
        try {
            hash = docking.forceRecovery(tx.getHash().toHex(), seedManagers, allManagers);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (!e.getErrorCode().getCode().equals("cv_0048")
                    && !e.getErrorCode().getCode().equals("cv_0040")
                    && !e.getErrorCode().getCode().equals("tx_0013")) {
                throw e;
            }
        }
        chain.getLogger().debug("[重置虚拟银行异构链调用] 调用异构链组件成功. heterogeneousChainId :{}, heterogeneousHash:{}",
                txData.getHeterogeneousChainId(), hash);
    }

}
