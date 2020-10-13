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
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_1;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_2;

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
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService = SpringLiteContext.getBean(AsyncProcessedTxStorageService.class);
    private ExeProposalStorageService exeProposalStorageService = SpringLiteContext.getBean(ExeProposalStorageService.class);
    private ProposalStorageService proposalStorageService = SpringLiteContext.getBean(ProposalStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService = SpringLiteContext.getBean(ConfirmWithdrawalStorageService.class);
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService = SpringLiteContext.getBean(HeterogeneousAssetConverterStorageService.class);
    private ComponentSignStorageService componentSignStorageService = SpringLiteContext.getBean(ComponentSignStorageService.class);
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService = SpringLiteContext.getBean(TxSubsequentProcessStorageService.class);

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
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            refund(pendingPO, proposalPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (!refundByzantine(pendingPO, proposalPO)) {
                                ExeProposalPO po = chain.getExeProposalQueue().poll();
                                chain.getExeProposalQueue().addLast(po);
                                continue;
                            }
                        }
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
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            upgrade(pendingPO, proposalPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (!upgradeByzantine(pendingPO, proposalPO)) {
                                ExeProposalPO po = chain.getExeProposalQueue().poll();
                                chain.getExeProposalQueue().addLast(po);
                                continue;
                            }
                        }
                        break;
                    case WITHDRAW:
                        // 提现提案
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            withdrawProposal(pendingPO, proposalPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            withdrawProposalByzantine(pendingPO, proposalPO);
                        }
                    case OTHER:
                    default:
                        break;
                }
                // 存储已执行成功的交易hash, 执行过的不再执行 (当前节点处于同步区块模式时,也要存该hash, 表示已执行过)
                asyncProcessedTxStorageService.saveProposalExe(chain, hash.toHex());
                chain.getLogger().debug("[异构链待处理队列] 执行成功移除交易, hash:{}", hash.toHex());
                // 并且从持久化库中移除
                exeProposalStorageService.delete(chain, hash.toHex());
                // 执行成功移除队列头部元素
                exeProposalQueue.remove();
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            ExeProposalPO po = chain.getExeProposalQueue().poll();
            chain.getExeProposalQueue().addLast(po);
        }
    }


    /**
     * 提现提案 version2
     *
     * @param pendingPO
     * @param proposalPO
     * @throws NulsException
     */
    private void withdrawProposalByzantine(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        if (pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            return;
        }
        String proposalHash = proposalPO.getHash().toHex();
        if (!hasExecutePermission(proposalHash)) {
            return;
        }
        NulsHash withdrawHash = new NulsHash(proposalPO.getNerveHash());
        Transaction tx = TransactionCall.getConfirmedTx(chain, withdrawHash);
        if (null == tx) {
            chain.getLogger().error("[ExeProposal-withdraw] The withdraw tx not exist. proposalHash:{}, withdrawHash:{}",
                    proposalHash, withdrawHash.toHex());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        ConfirmWithdrawalPO cfmWithdrawalTx = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, withdrawHash);
        if (null != cfmWithdrawalTx) {
            chain.getLogger().error("[ExeProposal-withdraw] The confirmWithdraw tx is confirmed. proposalHash:{}, withdrawHash:{}, cfmWithdrawalTx",
                    proposalHash, withdrawHash.toHex(), cfmWithdrawalTx.getConfirmWithdrawalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
        }
        String txHash = withdrawHash.toHex();
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
                    assetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    if (assetInfo != null) {
                        withdrawCoinTo = coinTo;
                        break;
                    }
                }
            }
            if (null == assetInfo) {
                chain.getLogger().error("[异构链地址签名消息-withdraw] no withdrawCoinTo. hash:{}", tx.getHash().toHex());
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
                    withdrawHash, listSign);
            // 初始化存储签名的对象
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(withdrawHash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // 广播当前节点签名消息
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[withdraw] 调用异构链组件执行签名, 发送签名消息. hash:{}", txHash);

            // (提现)业务交易hash 与提案交易的关系
            this.proposalStorageService.saveExeBusiness(chain, txHash, proposalPO.getHash());
        }

        // 为txSubsequentPO提供一个提现的po
        TxSubsequentProcessPO txSubsequentPO = new TxSubsequentProcessPO();
        txSubsequentPO.setTx(tx);
        txSubsequentPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
        BlockHeader header = new BlockHeader();
        header.setHeight(chain.getLatestBasicBlock().getHeight());
        txSubsequentPO.setBlockHeader(header);
        chain.getPendingTxQueue().offer(txSubsequentPO);
        txSubsequentProcessStorageService.save(chain, txSubsequentPO);
        chain.getLogger().info("[提现提案-放入执行异构链提现task中] hash:{}", txHash);
        // 存储更新后的 compSignPO
        componentSignStorageService.save(chain, compSignPO);
    }

    /**
     * 提现提案
     *
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
     *
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
                heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(withdrawCoinTo.getAssetsChainId(), withdrawCoinTo.getAssetsId());
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
     * 处理合约升级, 拜占庭签名
     * 判断并发送异构链交易
     *
     * @param pendingPO
     * @param proposalPO
     * @return true:需要删除队列的元素(已调用异构链, 或无需, 无权限执行等) false: 放队尾
     * @throws NulsException
     */
    private boolean upgradeByzantine(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash hash = proposalPO.getHash();
        String txHash = hash.toHex();
        // 新的合约多签地址
        int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
        String newMultySignAddress = Numeric.toHexString(proposalPO.getAddress());
        if (pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            return true;
        }
        if (!hasExecutePermission(txHash)) {
            return true;
        }
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
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // 如果当前节点还没签名则触发当前节点签名,存储 并广播
            String signStrData = docking.signUpgradeII(txHash, newMultySignAddress);
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
            chain.getLogger().info("[执行提案-{}] proposalHash:{}",
                    ProposalTypeEnum.UPGRADE, proposalPO.getHash().toHex());
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (!compSignPO.getCompleted()) {
                // 执行调用异构链
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("虚拟银行变更, 调用异构链参数为空");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                String ethTxHash = docking.createOrSignUpgradeTxII(
                        callParm.getTxHash(),
                        callParm.getUpgradeContract(),
                        callParm.getSigned());
                compSignPO.setCompleted(true);
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-upgrade] 调用异构链组件执行合约升级. hash:{}, ethHash:{}", txHash, ethTxHash);
            }
            rs = true;
        }
        // 存储更新后的 compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;

    }

    /**
     * 合约升级
     */
    private void upgrade(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        String hash = proposalPO.getHash().toHex();
        int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
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

        proposalPO.setHeterogeneousMultySignAddress(docking.getCurrentMultySignAddress());
        // 存储提案执行的链内hash
        proposalStorageService.save(chain, proposalPO);
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
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布退回交易");
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
     * 原路退回 version2
     * 根据异构链充值交易hash, 查询充值from地址资产与金额, 再通过异构链发还回去.
     * 判断并发送异构链交易
     *
     * @throws NulsException
     */
    private boolean refundByzantine(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash proposalTxHash = proposalPO.getHash();
        String proposalHash = proposalTxHash.toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("非虚拟银行成员, 或节点处于同步区块模式, 无需发布退回交易");
            return true;
        }
        if (!hasExecutePermission(proposalHash) || !replyAttack(proposalHash, proposalPO.getHeterogeneousTxHash())) {
            return true;
        }
        // 判断是否收到过该消息, 并签了名
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, proposalHash);
        boolean sign = false;
        if (null != compSignPO) {
            if (!compSignPO.getCurrentSigned()) {
                sign = true;
            }
        } else {
            sign = true;
        }
        if (sign) {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(proposalPO.getHeterogeneousChainId());
            HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                    proposalPO.getHeterogeneousChainId(),
                    proposalPO.getHeterogeneousTxHash(),
                    HeterogeneousTxTypeEnum.DEPOSIT,
                    this.heterogeneousDockingManager,
                    docking);
            if (null == info) {
                chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            // 如果当前节点还没签名则触发当前节点签名,存储 并广播
            String signStrData = docking.signWithdrawII(proposalHash, info.getFrom(), info.getValue(), info.getAssetId());
            String currentHaddress = docking.getCurrentSignAddress();
            if (StringUtils.isBlank(currentHaddress)) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            HeterogeneousAddress heterogeneousAddress = new HeterogeneousAddress(proposalPO.getHeterogeneousChainId(), currentHaddress);
            HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
            List<HeterogeneousSign> listSign = new ArrayList<>();
            listSign.add(currentSign);
            ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                    proposalTxHash, listSign);
            // 初始化存储签名的对象
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(proposalTxHash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // 广播当前节点签名消息
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            this.proposalStorageService.saveExeBusiness(chain, proposalHash, proposalTxHash);
            chain.getLogger().info("[执行提案-{}] 调用异构链组件执行签名, 发送签名消息, 提案hash:{}", ProposalTypeEnum.REFUND, proposalHash);
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (!compSignPO.getCompleted()) {
                // 执行调用异构链
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("虚拟银行变更, 调用异构链参数为空");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                String ethTxHash = docking.createOrSignWithdrawTxII(
                        callParm.getTxHash(),
                        callParm.getToAddress(),
                        callParm.getValue(),
                        callParm.getAssetId(),
                        callParm.getSigned());
                compSignPO.setCompleted(true);
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-refund] 调用异构链组件执行原路退回. hash:{}, ethHash:{}", callParm.getTxHash(), ethTxHash);
            }
            rs = true;
        }
        // 存储更新后的 compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;
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
        proposalPO.setNerveHash(tx.getHash().getBytes());
        // 存储提案执行的链内hash
        proposalStorageService.save(chain, proposalPO);
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
            proposalPO.setNerveHash(tx.getHash().getBytes());
            // 存储提案执行的链内hash
            proposalStorageService.save(chain, proposalPO);
            boolean rs = this.proposalStorageService.saveExeBusiness(chain, tx.getHash().toHex(), proposalPO.getHash());
            TransactionCall.newTx(chain, tx);
            chain.getLogger().info("[执行提案-{}] proposalHash:{}, txHash:{}, saveExeBusiness:{}",
                    ProposalTypeEnum.EXPELLED,
                    proposalPO.getHash().toHex(),
                    tx.getHash().toHex(),
                    rs);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (!e.getErrorCode().getCode().equals("cv_0048")
                    && !e.getErrorCode().getCode().equals("cv_0020")
                    && !e.getErrorCode().getCode().equals("tx_0013")) {
                throw e;
            }
            chain.getLogger().warn("[执行提案] 该节点已经不是银行成员, 无需执行变更交易, 提案完成.");
        }

    }
}
