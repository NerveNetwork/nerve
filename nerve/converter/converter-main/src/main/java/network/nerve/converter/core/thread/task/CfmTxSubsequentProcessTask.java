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
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.config.ConverterContext.FEE_ADDITIONAL_HEIGHT;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_1;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_2;

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
    private MergeComponentStorageService mergeComponentStorageService = SpringLiteContext.getBean(MergeComponentStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private VirtualBankStorageService virtualBankStorageService = SpringLiteContext.getBean(VirtualBankStorageService.class);
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService = SpringLiteContext.getBean(VirtualBankAllHistoryStorageService.class);
    private VirtualBankService virtualBankService = SpringLiteContext.getBean(VirtualBankService.class);
    private HeterogeneousService heterogeneousService = SpringLiteContext.getBean(HeterogeneousService.class);
    private HeterogeneousAssetHelper heterogeneousAssetHelper =
            SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
    private ComponentSignStorageService componentSignStorageService = SpringLiteContext.getBean(ComponentSignStorageService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            out:
            while (!pendingTxQueue.isEmpty()) {
                // 只取出,不移除头部元素
                TxSubsequentProcessPO pendingPO = pendingTxQueue.peekFirst();
                Transaction tx = pendingPO.getTx();
                if (!pendingPO.getRetry() && null != asyncProcessedTxStorageService.getComponentCall(chain, tx.getHash().toHex())) {
                    // 判断已执行过, 从队列中移除, 并从持久库中移除
                    chain.getLogger().info("[异构链待处理队列] 已执行过,移除交易, hash:{}", tx.getHash().toHex());
                    // 并且从持久化库中移除
                    txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                    // 执行成功移除队列头部元素
                    pendingTxQueue.remove();
                    continue;
                }
                // 判断是否已确认
                if (null == TransactionCall.getConfirmedTx(chain, tx.getHash())) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        chain.getLogger().error("[异构链待处理队列] 交易未确认(移除处理), hash:{}", tx.getHash().toHex());
                        // 并且从持久化库中移除
                        txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                        // 移除
                        pendingTxQueue.remove();
                        continue;
                    }
                    pendingPO.setIsConfirmedVerifyCount(pendingPO.getIsConfirmedVerifyCount() + 1);
                    // 终止本次执行，等待下一次执行再次检查交易是否确认
                    break;
                }
                switch (tx.getType()) {
                    case TxType.CHANGE_VIRTUAL_BANK:
                        // 处理银行变更
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            if (chain.getHeterogeneousChangeBankExecuting().get()) {
                                // 有虚拟银行变更异构链交易正在执行中, 暂停新的异构处理
                                chain.getLogger().info("[Task-CHANGE_VIRTUAL_BANK] pause new change 正在执行虚拟银行变更异构链交易, 暂停新的银行变更异构处理!");
                                break out;
                            }
                            changeVirtualBankProcessor();
                            // 后续已批量处理完成
                            continue;
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (!changeVirtualBankByzantineProcessor(pendingPO)) {
                                break out;
                            }
                        }
                        break;
                    case TxType.WITHDRAWAL:
                        // 处理提现
                        //if (chain.getHeterogeneousChangeBankExecuting().get()) {
                        //    // 有虚拟银行变更异构链交易正在执行中, 暂停新的异构处理
                        //    chain.getLogger().info("[Task-change_virtual_bank] pause withdrawal 正在执行虚拟银行变更异构链交易, 暂停新的提现异构处理!");
                        //    break out;
                        //}
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            withdrawalProcessor(pendingPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (pendingPO.isWithdrawExceedErrorTime(chain.getWithdrawFeeChangeVersion(tx.getHash().toHex()), 10)) {
                                chain.getLogger().warn("[withdraw] 提现手续费不足，重试次数超过限制，暂停处理当前提前任务, txHash: {}", tx.getHash().toHex());
                                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                            }
                            if (!withdrawalByzantineProcessor(pendingPO)) {
                                TxSubsequentProcessPO po = chain.getPendingTxQueue().poll();
                                chain.getPendingTxQueue().addLast(po);
                                continue;
                            }
                        }
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
                        break;
                    default:
                }
                if (chain.getCurrentIsDirector().get()) {
                    // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
                    ComponentCalledPO callPO = new ComponentCalledPO(
                            tx.getHash().toHex(),
                            pendingPO.getBlockHeader().getHeight(),
                            false);
                    asyncProcessedTxStorageService.saveComponentCall(chain, callPO, pendingPO.getCurrentQuit());
                }

                chain.getLogger().info("[异构链待处理队列] 执行成功移除交易, hash:{}", tx.getHash().toHex());
                // 并且从持久化库中移除
                txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                // 执行成功移除队列头部元素
                pendingTxQueue.remove();
            }
        } catch (Exception e) {

            /**
             * 如果队首交易可以变换位置, 则放入队尾,
             * 否则按顺序遍历,扫描一个可以变位置的元素, 放入队首
             */
            try {
                TxSubsequentProcessPO pendingPO = chain.getPendingTxQueue().peekFirst();
                if (null == pendingPO) {
                    return;
                }
                if (TxType.CHANGE_VIRTUAL_BANK != pendingPO.getTx().getType()) {
                    TxSubsequentProcessPO po = chain.getPendingTxQueue().poll();
                    chain.getPendingTxQueue().addLast(po);
                } else {
                    /**
                     * 当前处理的是虚拟银行变更(已抛异常), 就从后面去一个不是虚拟银行变更的交易放到队首
                     */
                    Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
                    TxSubsequentProcessPO toFirstPO = null;
                    while (it.hasNext()) {
                        TxSubsequentProcessPO po = it.next();
                        if (TxType.CHANGE_VIRTUAL_BANK != po.getTx().getType()) {
                            toFirstPO = po;
                            it.remove();
                            break;
                        }
                    }
                    if (null != toFirstPO) {
                        chain.getPendingTxQueue().addFirst(toFirstPO);
                    }
                }
            } catch (Exception ex) {
                chain.getLogger().error(ex);
            }

            if (e instanceof NulsException) {
                if (ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW.equals(((NulsException) e).getErrorCode())) {
                    return;
                }
            }
            chain.getLogger().error(e);
        }
    }

    /**
     * 处理虚拟银行变更业务
     * 通过异构链地址对交易hash签名, 并广播签名
     * 判断并发送异构链交易
     *
     * @param pendingPO
     * @return true:需要删除队列的元素(已调用异构链, 或无需, 无权限执行等) false: 放队尾
     * @throws Exception
     */
    private boolean changeVirtualBankByzantineProcessor(TxSubsequentProcessPO pendingPO) throws Exception {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        if (pendingPO.getCurrentJoin()) {
            chain.getLogger().info("虚拟银行变更, 执行签名[当前节点加入虚拟银行(不签名), 关闭新异构链变更执行(关门)]");
            heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
            return true;
        }
        Transaction tx = pendingPO.getTx();
        NulsHash hash = tx.getHash();
        String txHash = hash.toHex();
        // 判断是否收到过该消息, 并签了名
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, pendingPO.getTx().getHash().toHex());
        boolean sign = false;
        if (null != compSignPO) {
            if (!compSignPO.getCurrentSigned()) {
                sign = true;
            }
        } else {
            sign = true;
        }
        if (sign) {
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            List<HeterogeneousSign> currentSignList = new ArrayList<>();
            for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                int hChainId = hInterface.getChainId();
                // 组装加入参数
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents(), pendingPO);
                }
                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents(), pendingPO);
                }
                // 验证消息签名
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hInterface.getChainId());
                String signStrData = docking.signManagerChangesII(txHash, inAddress, outAddress, 1);
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                HeterogeneousSign currentSign = new HeterogeneousSign(
                        new HeterogeneousAddress(hChainId, currentHaddress),
                        HexUtil.decode(signStrData));
                currentSignList.add(currentSign);
            }
            ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                    hash, currentSignList);
            // 初始化存储签名的对象
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);

            // 广播当前节点签名消息
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);

        }

        boolean rs = false;
        if (compSignPO.getByzantinePass() && !chain.getHeterogeneousChangeBankExecuting().get()) {
            if (!compSignPO.getCompleted()) {
                // 执行调用异构链
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("虚拟银行变更, 调用异构链参数为空");
                    return false;
                }
                for (IHeterogeneousChainDocking docking : hInterfaces) {
                    for (ComponentCallParm callParm : callParmsList) {
                        if (docking.getChainId() == callParm.getHeterogeneousId()) {
                            boolean vaildPass = docking.validateManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    callParm.getSigned());
                            if (!vaildPass) {
                                chain.getLogger().error("[异构链地址签名消息-异构链验证未通过-changeVirtualBank] 调用异构链组件验证未通过. hash:{}, ethHash:{}", txHash);
                                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
                            }
                            break;
                        }
                    }
                }
                for (IHeterogeneousChainDocking docking : hInterfaces) {
                    for (ComponentCallParm callParm : callParmsList) {
                        if (docking.getChainId() == callParm.getHeterogeneousId()) {
                            String ethTxHash = docking.createOrSignManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    callParm.getSigned());
                            chain.getLogger().info("[异构链地址签名消息-拜占庭通过-changeVirtualBank] 调用异构链组件执行虚拟银行变更. hash:{}, ethHash:{}", txHash, ethTxHash);
                            break;
                        }
                    }
                }
            }
            /**
             * 调异构链成功
             * 开启正在处理银行变更异构链交易模式
             */
            heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
            chain.getLogger().info("[开始执行虚拟银行异构链交易, 关闭新异构链变更执行(关门)] hash:{}", txHash);
            // 保留合并机制的存储模式, 等到确认交易时匹配开门
            List<String> hashList = new ArrayList<>();
            hashList.add(txHash);
            mergeComponentStorageService.saveMergeComponentCall(chain, txHash, new MergedComponentCallPO(hashList));
            compSignPO.setCompleted(true);
            rs = true;
        }
        // 存储更新后的 compSignPO
        componentSignStorageService.save(chain, compSignPO);

        // 2020/12/24 虚拟银行变更等待拜占庭过程中,把队列后面交易提到前面来执行,防止无限循环等待
        if (!rs) {
            Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
            TxSubsequentProcessPO toFirstPO = null;
            while (it.hasNext()) {
                TxSubsequentProcessPO po = it.next();
                if (TxType.CHANGE_VIRTUAL_BANK != po.getTx().getType()) {
                    toFirstPO = po;
                    it.remove();
                    break;
                }
            }
            if (null != toFirstPO) {
                chain.getPendingTxQueue().addFirst(toFirstPO);
            }
        }
        return rs;
    }

    /**
     * 根据节点地址列表 和异构链Id, 获取异构链地址
     * 特殊处理, 如果是当前退出的, 则从pendingPO中获取退出者的异构链信息
     *
     * @param chain
     * @param heterogeneousChainId
     * @param address
     * @param list
     * @param pendingPO
     * @throws NulsException
     */
    private void getHeterogeneousAddress(Chain chain, int heterogeneousChainId, String[] address, List<byte[]> list, TxSubsequentProcessPO pendingPO) throws NulsException {
        for (int i = 0; i < list.size(); i++) {
            byte[] bytes = list.get(i);
            String agentAddress = AddressTool.getStringAddressByBytes(bytes);
            String hAddress = null;
            // 如果是当前退出的, 则从pendingPO中获取退出者的异构链信息
            if (pendingPO.getCurrentQuit() && agentAddress.equals(pendingPO.getCurrentQuitDirector().getAgentAddress())) {
                HeterogeneousAddress heterogeneousAddress =
                        pendingPO.getCurrentQuitDirector().getHeterogeneousAddrMap().get(heterogeneousChainId);
                hAddress = heterogeneousAddress.getAddress();
            } else {
                String signAddress = virtualBankAllHistoryStorageService.findSignAddressByAgentAddress(chain, agentAddress);
                if (StringUtils.isNotBlank(signAddress)) {
                    VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                    hAddress = heterogeneousAddress.getAddress();
                }
            }
            if (StringUtils.isBlank(hAddress)) {
                chain.getLogger().error("异构链地址签名消息[changeVirtualBank] 没有获取到异构链地址, agentAddress:{}", agentAddress);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            address[i] = hAddress;
        }
    }


    /**
     * 处理异构链提现
     * 通过异构链地址对交易hash签名, 并广播签名
     * 判断并发送异构链交易
     *
     * @param pendingPO
     * @return true:需要删除队列的元素(已调用异构链, 或无需, 无权限执行等) false: 放队尾
     * @throws NulsException
     */
    private boolean withdrawalByzantineProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return true;
        }
        Transaction tx = pendingPO.getTx();
        NulsHash hash = tx.getHash();
        String txHash = hash.toHex();
        // 判断是否收到过该消息, 并签了名
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, txHash);
        boolean sign = false;
        if (null != compSignPO) {
            if (!compSignPO.getCurrentSigned()) {
                sign = true;
            }
        } else {
            sign = true;
        }
        if (sign) {
            WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
            CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
            HeterogeneousAssetInfo assetInfo = null;
            CoinTo withdrawCoinTo = null;
            byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                    assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    if (assetInfo != null) {
                        withdrawCoinTo = coinTo;
                        break;
                    }
                }
            }
            if (null == assetInfo) {
                chain.getLogger().error("[异构链地址签名消息-withdraw] no withdrawCoinTo. hash:{}", txHash);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
            int heterogeneousChainId = assetInfo.getChainId();
            BigInteger amount = withdrawCoinTo.getAmount();
            String toAddress = txData.getHeterogeneousAddress();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // 如果当前节点还没签名则触发当前节点签名,存储 并广播
            String signStrData = docking.signWithdrawII(txHash, toAddress, amount, assetInfo.getAssetId());
            String currentHaddress = docking.getCurrentSignAddress();
            if (StringUtils.isBlank(currentHaddress)) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            HeterogeneousAddress heterogeneousAddress = new HeterogeneousAddress(heterogeneousChainId, currentHaddress);
            HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
            List<HeterogeneousSign> listSign = new ArrayList<>();
            listSign.add(currentSign);
            ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                    hash, listSign);
            // 初始化存储签名的对象
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // 广播当前节点签名消息
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[withdraw] 调用异构链组件执行签名, 发送签名消息. hash:{}", txHash);
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (pendingPO.getRetry() || !compSignPO.getCompleted()) {
                // 执行调用异构链
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("[withdraw] 调用异构链参数为空");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                if (chain.getLatestBasicBlock().getHeight() >= FEE_ADDITIONAL_HEIGHT) {
                    WithdrawalTotalFeeInfo totalFeeInfo = assembleTxService.calculateWithdrawalTotalFee(chain, tx);
                    BigInteger totalFee = totalFeeInfo.getFee();
                    boolean enoughFeeOfWithdraw;
                    // 修改手续费机制，支持异构链主资产作为手续费
                    if (totalFeeInfo.isNvtAsset()) {
                        // 验证NVT作为手续费
                        enoughFeeOfWithdraw = docking.isEnoughFeeOfWithdraw(new BigDecimal(totalFee), callParm.getAssetId());
                    } else {
                        // 验证异构链主资产作为手续费
                        // 可使用其他异构网络的主资产作为手续费, 比如提现到ETH，支付BNB作为手续费
                        enoughFeeOfWithdraw = docking.isEnoughFeeOfWithdrawByMainAssetProtocol15(totalFeeInfo.getHtgMainAssetName(), new BigDecimal(totalFee), callParm.getAssetId());
                    }
                    if (!enoughFeeOfWithdraw) {
                        // 异常计数
                        pendingPO.increaseWithdrawErrorTime();
                        chain.getLogger().error("[withdraw] 提现手续费计算, 手续费不足以支付提现费用. hash:{}, amount:{}",
                                txHash, callParm.getValue());
                        throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                    }
                }
                String ethTxHash;
                if (pendingPO.getRetry() && docking instanceof HtgDocking) {
                    // 当执行重发提现的机制时，触发提现，不检查执行顺序，当前节点立刻执行
                    HtgDocking htgDocking = (HtgDocking) docking;
                    ethTxHash = htgDocking.createOrSignWithdrawTxII(
                            callParm.getTxHash(),
                            callParm.getToAddress(),
                            callParm.getValue(),
                            callParm.getAssetId(),
                            callParm.getSigned(), false);
                } else {
                    ethTxHash = docking.createOrSignWithdrawTxII(
                            callParm.getTxHash(),
                            callParm.getToAddress(),
                            callParm.getValue(),
                            callParm.getAssetId(),
                            callParm.getSigned());
                }

                compSignPO.setCompleted(true);
                // 提现交易发出成功，清理手续费追加状态
                chain.clearWithdrawFeeChange(txHash);
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-withdraw] 调用异构链组件执行提现. hash:{}, ethHash:{}", txHash, ethTxHash);
            }
            rs = true;
        }
        // 存储更新后的 compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;
    }


    /**
     * 处理虚拟银行变更异构链部分的业务
     * 变更异构链多签地址/合约成员
     *
     * @throws NulsException
     */
    private void changeVirtualBankProcessor() throws Exception {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
            /**
             * 存储对应关系
             * key:合并列表中第一个交易hash, value:所有合并交易hash列表
             */
            List<String> hashList = new ArrayList<>();
            mergeList.forEach(k -> hashList.add(k.getTx().getHash().toHex()));
            mergeComponentStorageService.saveMergeComponentCall(chain, key, new MergedComponentCallPO(hashList));

            try {
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
                    // 删除重新存 虽然key一致 但是合并交易数可能不一致
                    mergeComponentStorageService.removeMergedTx(chain, key);
                    List<String> hashListNew = new ArrayList<>();
                    mergeList.forEach(k -> hashListNew.add(k.getTx().getHash().toHex()));
                    mergeComponentStorageService.saveMergeComponentCall(chain, key, new MergedComponentCallPO(hashListNew));
                    Result<Integer> resultTwo = changeVirtualBankAddress(key, mergeList, hInterfaces);
                    if (resultTwo.isFailed() && resultTwo.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                        chain.getLogger().error("[虚拟银行变更] 二次合并交易 发送扔失败!.");
                        throw new NulsException(resultTwo.getErrorCode());
                    }
                }
            } catch (Exception e) {
                // 删除key
                try {
                    mergeComponentStorageService.removeMergedTx(chain, key);
                } catch (Exception ex) {
                    throw ex;
                }
                throw e;
            }

        }
        // 从队列移除元素
        chain.getPendingTxQueue().removeAll(mergeList);
        for (TxSubsequentProcessPO po : mergeList) {
            String hash = po.getTx().getHash().toHex();
            if (chain.getCurrentIsDirector().get()) {
                // 存储已执行成功的交易hash, 执行过的不再执行.
                ComponentCalledPO callPO = new ComponentCalledPO(
                        hash,
                        po.getBlockHeader().getHeight(),
                        false);
                asyncProcessedTxStorageService.saveComponentCall(chain, callPO, po.getCurrentQuit());

            }
            chain.getLogger().debug("[异构链待处理队列] 执行成功移除交易, hash:{}", hash);
            // 并且从持久化库中移除
            txSubsequentProcessStorageService.delete(chain, hash);
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
     * 虚拟银行公共地址的变更处理
     *
     * @param key
     * @param mergeList
     * @return
     * @throws NulsException
     */
    private Result<Integer> changeVirtualBankAddress(String key, List<TxSubsequentProcessPO> mergeList, List<IHeterogeneousChainDocking> hInterfaces) throws NulsException {
        /*
         * key: 节点, value: in/out 次数(in +1 ;out -1)
         * value 大于 0, 则组装到加入; value 小于 0, 则组装到退出; value 等于 0, 则不需要变更合约
         */
        Map<VirtualBankDirector, Integer> mapAllDirector = new HashMap<>();
        // 合并前原始交易数量
        int originalMergedCount = mergeList.size();
        chain.getLogger().info("[bank-可合并的交易] size:{}", mergeList.size());
        for (TxSubsequentProcessPO pendingPO : mergeList) {
            chain.getLogger().info("----------------------------------------");
            chain.getLogger().info("[bank-可合并的交易] hash:{}", pendingPO.getTx().getHash().toHex());
            for (VirtualBankDirector director : pendingPO.getListInDirector()) {
                mapAllDirector.compute(director, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
                chain.getLogger().info("InAddress: {}", director.getAgentAddress());
            }
            for (VirtualBankDirector director : pendingPO.getListOutDirector()) {
                mapAllDirector.compute(director, (k, v) -> {
                    if (null == v) {
                        return -1;
                    } else {
                        return v - 1;
                    }
                });
                chain.getLogger().info("OutAddress: {}", director.getAgentAddress());
            }
            chain.getLogger().info("----------------------------------------");
        }
        int inCount = 0;
        int outCount = 0;
        for (Integer count : mapAllDirector.values()) {
            if (count < 0) {
                outCount++;
            } else if (count > 0) {
                inCount++;
            }
        }

        for (IHeterogeneousChainDocking hInterface : hInterfaces) {
            // 组装加入参数
            String[] inAddress = new String[inCount];
            int i = 0;
            for (Map.Entry<VirtualBankDirector, Integer> map : mapAllDirector.entrySet()) {
                if (map.getValue() <= 0) {
                    // 小于等于0的不统计, 只统计加入的(大于0的)
                    continue;
                }
                VirtualBankDirector director = map.getKey();
                HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                inAddress[i] = heterogeneousAddress.getAddress();
                i++;
            }

            String[] outAddress = new String[outCount];
            i = 0;
            for (Map.Entry<VirtualBankDirector, Integer> map : mapAllDirector.entrySet()) {
                if (map.getValue() >= 0) {
                    // 大于等于0的不统计, 只统计退出的(小于0的)
                    continue;
                }
                VirtualBankDirector director = map.getKey();
                HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                outAddress[i] = heterogeneousAddress.getAddress();
                i++;
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
                chain.getLogger().info("[开始执行虚拟银行异构链交易, 关闭新异构链变更执行] key:{}", key);
            }
            chain.getLogger().info("[bank-执行变更] key:{}, inAddress:{}, outAddress:{}",
                    key, Arrays.toString(inAddress), Arrays.toString(outAddress));
        }
        return Result.getSuccess(0);
    }


    private void heterogeneousContractAssetRegCompleteProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
                virtualBankStorageService.update(chain, director);
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
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), withdrawCoinTo.getAssetsChainId(), assetId);
        BigInteger amount = withdrawCoinTo.getAmount();
        String heterogeneousAddress = txData.getHeterogeneousAddress();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
        String ethTxHash = docking.createOrSignWithdrawTx(
                tx.getHash().toHex(),
                heterogeneousAddress,
                amount,
                heterogeneousAssetInfo.getAssetId());
        chain.getLogger().info("[withdraw] 调用异构链组件执行提现. hash:{},ethHash:{}", tx.getHash().toHex(), ethTxHash);
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
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmWithdrawalTxData.class);
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : txData.getListDistributionFee()) {
            String address = chain.getDirectorRewardAddress(addr);
            if (StringUtils.isBlank(address)) {
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            assembleTxService.createDistributionFeeTx(chain, txData.getWithdrawalTxHash(), listRewardAddress, pendingPO.getBlockHeader().getTime(), false);
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
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
            if (StringUtils.isBlank(address)) {
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            // basisTxHash 为确认提案交易hash
            assembleTxService.createDistributionFeeTx(chain, pendingPO.getTx().getHash(), listRewardAddress, pendingPO.getBlockHeader().getTime(), true);
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
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
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
            ComponentCalledPO callPO = new ComponentCalledPO(
                    hash,
                    po.getBlockHeader().getHeight(),
                    false);
            asyncProcessedTxStorageService.saveComponentCall(chain, callPO, po.getCurrentQuit());
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
