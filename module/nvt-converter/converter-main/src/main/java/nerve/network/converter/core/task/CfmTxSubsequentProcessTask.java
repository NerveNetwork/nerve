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

package nerve.network.converter.core.task;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.core.business.HeterogeneousService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.po.SubsequentProcessedTxPO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.txdata.ChangeVirtualBankTxData;
import nerve.network.converter.model.txdata.ConfirmWithdrawalTxData;
import nerve.network.converter.model.txdata.InitializeHeterogeneousTxData;
import nerve.network.converter.model.txdata.WithdrawalTxData;
import nerve.network.converter.rpc.call.TransactionCall;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.storage.TxSubsequentProcessedTxStorageService;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 交易确认后续处理定时任务
 * 处理交易后续的异构链组件的调用,关联生成新的交易等
 *
 * @author: Chino
 * @date: 2020-03-10
 */
public class CfmTxSubsequentProcessTask implements Runnable {
    private Chain chain;

    public CfmTxSubsequentProcessTask(Chain chain) {
        this.chain = chain;
    }

    private HeterogeneousDockingManager heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
    private TxSubsequentProcessedTxStorageService txSubsequentProcessedTxStorageService = SpringLiteContext.getBean(TxSubsequentProcessedTxStorageService.class);
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService = SpringLiteContext.getBean(TxSubsequentProcessStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private HeterogeneousService heterogeneousService = SpringLiteContext.getBean(HeterogeneousService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            while (!pendingTxQueue.isEmpty()) {
                // 只取出,不移除头部元素
                TxSubsequentProcessPO pendingPO = pendingTxQueue.peekFirst();
                Transaction tx = pendingPO.getTx();
                if(chain.getMapComponentCalledTx().containsKey(tx.getHash().toHex())){
                    // 判断已执行过, 从队列中移除, 并从持久库中移除
                    // 执行成功移除队列头部元素
                    pendingTxQueue.remove();
                    // 并且从持久化库中移除
                    txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                    continue;
                }
                // 判断是否已确认
                if (null == TransactionCall.getConfirmedTx(chain, tx.getHash())) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
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
                        // 确认提现交易
                        confirmWithdrawalProcessor(pendingPO);
                        break;
                    case TxType.INITIALIZE_HETEROGENEOUS:
                        // 处理补贴手续费交易
                        initializeHeterogeneousProcessor(pendingPO);
                        break;
                    default:
                }

                long currentHeight = pendingPO.getBlockHeader().getHeight();
                // 缓存已执行成功的交易hash, 执行过的不在执行
                txSubsequentProcessedTxStorageService.save(chain,
                        new SubsequentProcessedTxPO(tx.getHash().toHex(), currentHeight));
                chain.getMapComponentCalledTx().put(tx.getHash().toHex(), currentHeight);
                // 执行成功移除队列头部元素
                pendingTxQueue.remove();
                // 并且从持久化库中移除
                txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                // 清理检查
                checkAndCleanCalledTx(currentHeight);
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 检查并清理已处理过的交易
     *
     * @param currentHeight
     */
    private void checkAndCleanCalledTx(long currentHeight) {
        if (chain.getMapComponentCalledTx().size() < ConverterConstant.START_CLEAN_MAPCOMPONENTCALLED_SIZE_THRESHOLD) {
            return;
        }
        Set<Map.Entry<String, Long>> entrySet = chain.getMapComponentCalledTx().entrySet();
        Iterator<Map.Entry<String, Long>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String key = entry.getKey();
            // 高度大于阈值则进行缓存与DB的删除
            if (currentHeight - ConverterConstant.CLEAN_MAPCOMPONENTCALLED_HEIGHT_THRESHOLD > entry.getValue()) {
                iterator.remove();
                txSubsequentProcessedTxStorageService.delete(chain, key);
            }
        }

    }

    /**
     * 处理虚拟银行变更异构链部分的业务
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void changeVirtualBankProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        createNewHeterogeneousAddress(pendingPO, hInterfaces);
        // 同步模式不需要执行虚拟银行多签/合约成员变更
        if (pendingPO.getSyncStatusEnum().equals(SyncStatusEnum.RUNNING)) {
            changeVirtualBankAddress(pendingPO, hInterfaces);
        }
    }

    /**
     * 为变更的虚拟银行成员创建多签/合约地址等信息
     *
     * @param pendingPO
     * @param hInterfaces
     */
    private void createNewHeterogeneousAddress(TxSubsequentProcessPO pendingPO, List<IHeterogeneousChainDocking> hInterfaces) {
        List<VirtualBankDirector> listInDirector = pendingPO.getListInDirector();
        if (null != listInDirector && !listInDirector.isEmpty()) {
            for (VirtualBankDirector director : listInDirector) {
                for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                    // 为新成员创建异构链多签地址
                    String heterogeneousAddress = hInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                    director.getHeterogeneousAddrMap().put(hInterface.getChainId(),
                            new HeterogeneousAddress(hInterface.getChainId(), heterogeneousAddress));
                }
                // 更新虚拟银行节点信息 todo
//                chain.getMapVirtualBank().put(director.getSignAddress(), director);
            }
        }
    }

    private void initializeHeterogeneousProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        Transaction tx = pendingPO.getTx();
        // 维护虚拟银行
        InitializeHeterogeneousTxData txData = ConverterUtil.getInstance(tx.getTxData(), InitializeHeterogeneousTxData.class);
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            if (!director.getSeedNode()) {
                // 为新成员创建异构链多签地址
                String heterogeneousAddress = heterogeneousInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                director.getHeterogeneousAddrMap().put(heterogeneousInterface.getChainId(),
                        new HeterogeneousAddress(heterogeneousInterface.getChainId(), heterogeneousAddress));
                // 更新虚拟银行节点信息
//                chain.getMapVirtualBank().put(director.getSignAddress(), director);
            }
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
                    inAddress[i] = chain.getDirectorHeterogeneousAddr(AddressTool.getStringAddressByBytes(addressBytes), hInterface.getChainId());
                }
            }
            String[] outAddress = new String[outCount];
            if (null != listOutDirector && !listOutDirector.isEmpty()) {
                for (int i = 0; i < listOutDirector.size(); i++) {
                    VirtualBankDirector director = listOutDirector.get(i);
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                    inAddress[i] = heterogeneousAddress.getAddress();
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
        int assetChainId = coinTo.getAssetsChainId();
        int assetId = coinTo.getAssetsId();
        BigInteger amount = coinTo.getAmount();
        String heterogeneousAddress = txData.getHeterogeneousAddress();
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(assetChainId);
        heterogeneousInterface.createOrSignWithdrawTx(tx.getHash().toHex(), heterogeneousAddress, amount, assetId);
    }


    /**
     * 确认提现交易 后续手续费补贴业务
     * 如果需要进行手续费补贴,则触发手续费补贴交易
     * 补贴交易时间统一为确认提现交易所在区块的时间
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void confirmWithdrawalProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmWithdrawalTxData.class);
        // 判断是否需要组装
        if(!isCreateDistributionFeeTx(txData)){
            return;
        }
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : txData.getListDistributionFee()) {
            String address = chain.getDirectorRewardAddress(addr);
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        assembleTxService.createDistributionFeeTx(chain, txData.getWithdrawalTxHash(), listRewardAddress, pendingPO.getBlockHeader().getTime());
    }

    /**
     * 判断是否需要组装补贴手续费交易
     * @param txData
     * @return
     * @throws NulsException
     */
    private boolean isCreateDistributionFeeTx(ConfirmWithdrawalTxData txData) throws NulsException {
        Transaction withdrawalTx = TransactionCall.getConfirmedTx(chain, txData.getWithdrawalTxHash());
        CoinData coinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        boolean assembleCurrentAssetFee = false;
        for (CoinFrom coinFrom : coinData.getFrom()) {
            if (coinFrom.getAssetsChainId() == txData.getHeterogeneousChainId()) {
                // 如果提现资产是异构链的coin, 判断是否手续本链手续费来补贴
                assembleCurrentAssetFee = heterogeneousService.isAssembleCurrentAssetFee(coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            }
        }
        return assembleCurrentAssetFee;
    }

}
