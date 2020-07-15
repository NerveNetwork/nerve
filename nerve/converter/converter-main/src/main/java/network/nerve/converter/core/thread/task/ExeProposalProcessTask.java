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
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author: Loki
 * @date: 2020/5/15
 */
public class ExeProposalProcessTask implements Runnable {
    private Chain chain;

    public ExeProposalProcessTask(Chain chain) {
        this.chain = chain;
    }

    private HeterogeneousDockingManager heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
    private HeterogeneousChainManager heterogeneousChainManager = SpringLiteContext.getBean(HeterogeneousChainManager.class);
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService = SpringLiteContext.getBean(AsyncProcessedTxStorageService.class);
    private ExeProposalStorageService exeProposalStorageService = SpringLiteContext.getBean(ExeProposalStorageService.class);
    private ProposalStorageService proposalStorageService = SpringLiteContext.getBean(ProposalStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService = SpringLiteContext.getBean(ConfirmWithdrawalStorageService.class);
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService = SpringLiteContext.getBean(HeterogeneousAssetConverterStorageService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<ExeProposalPO> exeProposalQueue = chain.getExeProposalQueue();
            while (!exeProposalQueue.isEmpty()) {
                // 只取出,不移除头部元素
                ExeProposalPO pendingPO = exeProposalQueue.peekFirst();
                NulsHash hash = pendingPO.getProposalTxHash();
                if (null != asyncProcessedTxStorageService.getProposalExe(chain, hash.toHex())) {
                    // 判断已执行过, 从队列中移除, 并从持久库中移除
                    // 执行成功移除队列头部元素
                    exeProposalQueue.remove();
                    chain.getLogger().debug("[提案待执行队列] 判断已执行过移除交易, hash:{}", hash.toHex());
                    // 并且从持久化库中移除
                    exeProposalStorageService.delete(chain, hash.toHex());
                    continue;
                }

                // 判断是否已确认
                if (null == TransactionCall.getConfirmedTx(chain, hash)) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        exeProposalQueue.remove();
                        chain.getLogger().debug("[提案待执行队列] 交易未确认(移除处理), hash:{}", hash.toHex());
                        // 并且从持久化库中移除
                        exeProposalStorageService.delete(chain, hash.toHex());
                        continue;
                    }
                    pendingPO.setIsConfirmedVerifyCount(pendingPO.getIsConfirmedVerifyCount() + 1);
                    // 终止本次执行，等待下一次执行再次检查交易是否确认
                    break;
                }

                ProposalPO proposalPO = proposalStorageService.find(chain, hash);
                ProposalTypeEnum proposalTypeEnum = ProposalTypeEnum.getEnum(proposalPO.getType());
                switch (proposalTypeEnum) {
                    case REFUND:
                        // 异构链原路退回
                        refund(pendingPO, proposalPO);
                        // 记录原始充值hash 保证在提案中只执行一次
                        asyncProcessedTxStorageService.saveProposalExe(chain, proposalPO.getHeterogeneousTxHash());
                        break;
                    case TRANSFER:
                        // 转到其他账户
                        transfer(pendingPO, proposalPO);
                        asyncProcessedTxStorageService.saveProposalExe(chain, proposalPO.getHeterogeneousTxHash());
                        break;
                    case LOCK:
                        // 冻结账户
                        lock(proposalPO.getAddress());
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[执行提案-{}] proposalHash:{}",
                                ProposalTypeEnum.LOCK,
                                proposalPO.getHash().toHex());
                        break;
                    case UNLOCK:
                        // 解冻账户
                        unlock(proposalPO.getAddress());
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[执行提案-{}] proposalHash:{}",
                                ProposalTypeEnum.UNLOCK,
                                proposalPO.getHash().toHex());
                        break;
                    case EXPELLED:
                        // 撤销银行资格
                        if (chain.getResetVirtualBank().get()) {
                            chain.getLogger().warn("正在重置虚拟银行异构链(合约), 提案等待1分钟后执行..");
                            Thread.sleep(60000L);
                            continue;
                        }
                        disqualification(pendingPO, proposalPO);
                        break;
                    case UPGRADE:
                        // 升级合约
                        upgrade(pendingPO, proposalPO);
                        break;
                    case WITHDRAW:
                        // 提现提案
                        withdrawProposal(pendingPO, proposalPO);
                    case OTHER:
                    default:
                        break;
                }

                // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
                asyncProcessedTxStorageService.saveProposalExe(chain, hash.toHex());
                // 执行成功移除队列头部元素
                exeProposalQueue.remove();
                chain.getLogger().debug("[异构链待处理队列] 执行成功移除交易, hash:{}", hash.toHex());
                // 并且从持久化库中移除
                exeProposalStorageService.delete(chain, hash.toHex());
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 提现提案
     * @param pendingPO
     * @param proposalPO
     * @throws NulsException
     */
    private void withdrawProposal(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        if (pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            return;
        }
        String hash = proposalPO.getHash().toHex();
        if (!hasExecutePermission(hash)) {
            return;
        }
        NulsHash withdrawHash = new NulsHash(proposalPO.getNerveHash());
        Transaction withdrawTx = TransactionCall.getConfirmedTx(chain, withdrawHash);
        if (null == withdrawTx) {
            chain.getLogger().error("[ExeProposal-withdraw] The withdraw tx not exist. proposalHash:{}, withdrawHash:{}",
                    hash, withdrawHash.toHex());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        ConfirmWithdrawalPO cfmWithdrawalTx = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, withdrawHash);
        if (null != cfmWithdrawalTx) {
            chain.getLogger().error("[ExeProposal-withdraw] The confirmWithdraw tx is confirmed. proposalHash:{}, withdrawHash:{}, cfmWithdrawalTx",
                    hash, withdrawHash.toHex(), cfmWithdrawalTx.getConfirmWithdrawalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
        }
        exeWithdraw(withdrawTx);
        this.proposalStorageService.saveExeBusiness(chain, HexUtil.encode(proposalPO.getNerveHash()), proposalPO.getHash());
    }

    /**
     * 执行提现调用异构链
     * @param withdrawTx
     * @throws NulsException
     */
    private void exeWithdraw(Transaction withdrawTx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(withdrawTx.getCoinData(), CoinData.class);
        CoinTo withdrawCoinTo = null;
        for (CoinTo coinTo : coinData.getTo()) {
            if (coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                withdrawCoinTo = coinTo;
                break;
            }
        }
        if (null == withdrawCoinTo) {
            chain.getLogger().error("[ExeProposal-withdraw] Withdraw transaction cointo data error, no withdrawCoinTo. hash:{}", withdrawTx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        HeterogeneousAssetInfo assetInfo =
                heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(withdrawCoinTo.getAssetsId());
        if (null == assetInfo) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
        }
        WithdrawalTxData withdrawTxData = ConverterUtil.getInstance(withdrawTx.getTxData(), WithdrawalTxData.class);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
        docking.createOrSignWithdrawTx(
                withdrawTx.getHash().toHex(),
                withdrawTxData.getHeterogeneousAddress(),
                withdrawCoinTo.getAmount(),
                assetInfo.getAssetId());
    }

    /**
     * 合约升级
     */
    private void upgrade(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        String hash = proposalPO.getHash().toHex();
        // 更新合约多签地址
        int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
        String newMultySignAddress = Numeric.toHexString(proposalPO.getAddress());
        heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, newMultySignAddress);
        if (pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            return;
        }
        if (!hasExecutePermission(hash)) {
            return;
        }
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        // 向异构链发起合约升级交易
        String exeTxHash = docking.createOrSignUpgradeTx(hash);
        this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());
        chain.getLogger().info("[执行提案-{}] proposalHash:{}, exeTxHash:{}",
                ProposalTypeEnum.UPGRADE,
                proposalPO.getHash().toHex(),
                exeTxHash);
    }

    /**
     * 执行权限判断
     *
     * @param hash
     * @return
     */
    private boolean hasExecutePermission(String hash) {
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            chain.getLogger().debug("只有虚拟银行才能执行, 非虚拟银行节点不执行, hash:{}", hash);
            return false;
        }
        return true;
    }

    /**
     * 检查异构链业务重放攻击
     * 对于 type:1 原路退回 type:2 充值到其他地址, 同一异构链hash只允许执行一次
     *
     * @param hash
     * @param heterogeneousTxHash
     * @return
     */
    private boolean replyAttack(String hash, String heterogeneousTxHash) {
        if (null != asyncProcessedTxStorageService.getProposalExe(chain, heterogeneousTxHash)) {
            chain.getLogger().error("[提案待执行队列] 提案中该异构链交易已执行过, 提案hash:{} heterogeneousTxHash:{}", hash, heterogeneousTxHash);
            return false;
        }
        return true;
    }

    /**
     * 原路退回
     * 根据异构链充值交易hash, 查询充值from地址资产与金额, 再通过异构链发还回去.
     *
     * @throws NulsException
     */
    private void refund(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash proposalHash = proposalPO.getHash();
        String hash = proposalHash.toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布确认交易");
            return;
        }
        if (!hasExecutePermission(hash) || !replyAttack(hash, proposalPO.getHeterogeneousTxHash())) {
            return;
        }
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(proposalPO.getHeterogeneousChainId());
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                proposalPO.getHeterogeneousChainId(),
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager,
                heterogeneousInterface);
        if (null == info) {
            chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        String exeTxHash = heterogeneousInterface.createOrSignWithdrawTx(hash, info.getFrom(), info.getValue(), info.getAssetId());
        this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());
        chain.getLogger().info("[执行提案-{}] proposalHash:{}, exeTxHash:{}",
                ProposalTypeEnum.REFUND,
                proposalPO.getHash().toHex(),
                exeTxHash);
    }

    /**
     * 转到其他地址
     *
     * @throws NulsException
     */
    private void transfer(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        String hash = proposalPO.getHash().toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布确认交易");
            return;
        }
        if (!hasExecutePermission(hash) || !replyAttack(hash, proposalPO.getHeterogeneousTxHash())) {
            return;
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                proposalPO.getHeterogeneousChainId(),
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }

        RechargeTxDTO rechargeTxDTO = new RechargeTxDTO();
        rechargeTxDTO.setOriginalTxHash(hash);
        rechargeTxDTO.setHeterogeneousChainId(proposalPO.getHeterogeneousChainId());
        rechargeTxDTO.setHeterogeneousAssetId(info.getAssetId());
        rechargeTxDTO.setAmount(info.getValue());
        rechargeTxDTO.setToAddress(AddressTool.getStringAddressByBytes(proposalPO.getAddress()));
        rechargeTxDTO.setTxtime(pendingPO.getTime());
        Transaction tx = assembleTxService.createRechargeTx(chain, rechargeTxDTO);
        chain.getLogger().info("[执行提案-{}] proposalHash:{}, txHash:{}",
                ProposalTypeEnum.TRANSFER,
                proposalPO.getHash().toHex(),
                tx.getHash().toHex());
    }

    private void lock(byte[] address) throws NulsException {
        TransactionCall.lock(chain, address);
    }

    private void unlock(byte[] address) throws NulsException {
        TransactionCall.unlock(chain, address);
    }

    /**
     * 发布(解锁/锁定账户)提案的确认交易
     *
     * @param proposalPO
     * @param pendingPO
     * @throws NulsException
     */
    private void publishProposalConfirmed(ProposalPO proposalPO, ExeProposalPO pendingPO) throws NulsException {
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布确认交易");
            return;
        }
        ProposalExeBusinessData businessData = new ProposalExeBusinessData();
        businessData.setProposalTxHash(proposalPO.getHash());
        businessData.setAddress(proposalPO.getAddress());

        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(proposalPO.getType());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        // 发布提案确认交易
        assembleTxService.createConfirmProposalTx(chain, txData, pendingPO.getTime());
    }


    private void disqualification(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布确认交易");
            return;
        }
        List<byte[]> outList = new ArrayList<>();
        outList.add(proposalPO.getAddress());
        // 创建虚拟银行变更交易
        Transaction tx = null;
        try {
            tx = assembleTxService.assembleChangeVirtualBankTx(chain, null, outList, pendingPO.getHeight(), pendingPO.getTime());
            boolean rs = this.proposalStorageService.saveExeBusiness(chain, tx.getHash().toHex(), proposalPO.getHash());
            TransactionCall.newTx(chain, tx);
            chain.getLogger().info("[执行提案-{}] proposalHash:{}, txHash:{}, saveExeBusiness:{}",
                    ProposalTypeEnum.EXPELLED,
                    proposalPO.getHash().toHex(),
                    tx.getHash().toHex(),
                    rs);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if(!e.getErrorCode().getCode().equals("cv_0048")
                    && !e.getErrorCode().getCode().equals("cv_0020")
                    && !e.getErrorCode().getCode().equals("tx_0013")){
                throw e;
            }
            chain.getLogger().warn("[执行提案] 该节点已经不是银行成员, 无需执行变更交易, 提案完成.");
        }

    }
}
