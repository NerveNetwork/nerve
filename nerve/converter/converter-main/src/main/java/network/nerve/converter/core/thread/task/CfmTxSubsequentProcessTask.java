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
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService =
            SpringLiteContext.getBean(HeterogeneousAssetConverterStorageService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
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
                        changeVirtualBankProcessor(pendingPO);
                        break;
                    case TxType.WITHDRAWAL:
                        // 处理提现
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
                    default:
                }

                long currentHeight = pendingPO.getBlockHeader().getHeight();
                // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
                asyncProcessedTxStorageService.saveComponentCall(chain, tx.getHash().toHex(), currentHeight);
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
     * 处理虚拟银行变更异构链部分的业务
     * 变更异构链多签地址/合约成员
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void changeVirtualBankProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        changeVirtualBankAddress(pendingPO, hInterfaces);
    }



    private void initializeHeterogeneousProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
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
            }
        }
    }

    private void heterogeneousContractAssetRegCompleteProcessor(TxSubsequentProcessPO pendingPO) {
        try {
            Transaction tx = pendingPO.getTx();
            assembleTxService.createHeterogeneousContractAssetRegCompleteTx(chain, tx);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 虚拟银行公共地址的变更处理
     *
     * @param pendingPO
     * @param hInterfaces
     * @throws NulsException
     */
    private void changeVirtualBankAddress(TxSubsequentProcessPO pendingPO, List<IHeterogeneousChainDocking> hInterfaces) throws NulsException {
        Transaction tx = pendingPO.getTx();
        // 维护虚拟银行
        ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
        List<byte[]> listInAgents = txData.getInAgents();
        int inCount = 0;
        if (null != listInAgents) {
            inCount = listInAgents.size();
        }
        List<VirtualBankDirector> listOutDirector = pendingPO.getListOutDirector();
        int outCount = 0;
        if (null != listOutDirector) {
            outCount = listOutDirector.size();
        }

        for (IHeterogeneousChainDocking hInterface : hInterfaces) {
            // 组装加入参数
            String[] inAddress = new String[inCount];
            if (null != listInAgents && !listInAgents.isEmpty()) {
                for (int i = 0; i < listInAgents.size(); i++) {
                    byte[] addressBytes = listInAgents.get(i);

                    inAddress[i] = chain.getDirectorHeterogeneousAddrByAgentAddr(AddressTool.getStringAddressByBytes(addressBytes), hInterface.getChainId());
                }
            }
            String[] outAddress = new String[outCount];
            if (null != listOutDirector && !listOutDirector.isEmpty()) {
                for (int i = 0; i < listOutDirector.size(); i++) {
                    VirtualBankDirector director = listOutDirector.get(i);
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                    outAddress[i] = heterogeneousAddress.getAddress();
                }
            }

            List<String> listAgentAddr = new ArrayList<>();
            for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
                String heterogeneousAddress = chain.getDirectorHeterogeneousAddr(director.getSignAddress(), hInterface.getChainId());
                listAgentAddr.add(heterogeneousAddress);
            }
            // 创建新的虚拟银行异构链多签地址/修改合约成员
            hInterface.createOrSignManagerChangesTx(
                    tx.getHash().toHex(),
                    inAddress,
                    outAddress,
                    listAgentAddr.toArray(new String[listAgentAddr.size()]));
        }
    }

    /**
     * 处理提现的交易
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void withdrawalProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        Transaction tx = pendingPO.getTx();
        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        CoinTo coinTo = coinData.getTo().get(0);
        int assetId = coinTo.getAssetsId();
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(assetId);
        BigInteger amount = coinTo.getAmount();
        String heterogeneousAddress = txData.getHeterogeneousAddress();
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
        heterogeneousInterface.createOrSignWithdrawTx(tx.getHash().toHex(), heterogeneousAddress, amount, heterogeneousAssetInfo.getAssetId());
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
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmWithdrawalTxData.class);
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : txData.getListDistributionFee()) {
            String address = chain.getDirectorRewardAddress(addr);
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

    private void confirmProposalDistributionFee(TxSubsequentProcessPO pendingPO) throws NulsException {
        ConfirmProposalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmProposalTxData.class);
        List<HeterogeneousAddress> addressList;
        if(ProposalTypeEnum.UPGRADE.value() == txData.getType()) {
            ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
            addressList = upgradeTxData.getListDistributionFee();
        } else {
            ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
            addressList = businessData.getListDistributionFee();
        }
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : addressList) {
            String address = chain.getDirectorRewardAddress(addr);
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

}
