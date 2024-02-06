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

package network.nerve.converter.core.thread.task;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcTxInfo;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.dto.RechargeTxOfBtcSysDTO;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.config.ConverterContext.FEE_ADDITIONAL_HEIGHT;
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
    private HeterogeneousAssetHelper heterogeneousAssetHelper = SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
    private ComponentSignStorageService componentSignStorageService = SpringLiteContext.getBean(ComponentSignStorageService.class);
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService = SpringLiteContext.getBean(TxSubsequentProcessStorageService.class);
    private RechargeStorageService rechargeStorageService = SpringLiteContext.getBean(RechargeStorageService.class);
    private ProposalExeStorageService proposalExeStorageService = SpringLiteContext.getBean(ProposalExeStorageService.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<ExeProposalPO> exeProposalQueue = chain.getExeProposalQueue();
            while (!exeProposalQueue.isEmpty()) {
                // Only remove,Do not remove head elements
                ExeProposalPO pendingPO = exeProposalQueue.peekFirst();
                NulsHash hash = pendingPO.getProposalTxHash();
                if (null != proposalExeStorageService.find(chain, hash.toHex())
                        || null != asyncProcessedTxStorageService.getProposalExe(chain, hash.toHex())) {
                    // Judging that it has been executed, Remove from queue, And remove it from the persistent library
                    // Successfully removed queue header elements
                    exeProposalQueue.remove();
                    chain.getLogger().debug("[Proposal pending queue] Determine if the removal transaction has been executed, hash:{}", hash.toHex());
                    // And remove it from the persistence library
                    exeProposalStorageService.delete(chain, hash.toHex());
                    continue;
                }

                // Determine if it has been confirmed
                if (null == TransactionCall.getConfirmedTx(chain, hash)) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        exeProposalQueue.remove();
                        chain.getLogger().debug("[Proposal pending queue] Transaction unconfirmed(Remove processing), hash:{}", hash.toHex());
                        // And remove it from the persistence library
                        exeProposalStorageService.delete(chain, hash.toHex());
                        continue;
                    }
                    pendingPO.setIsConfirmedVerifyCount(pendingPO.getIsConfirmedVerifyCount() + 1);
                    // Terminate this execution and wait for the next execution to check if the transaction is confirmed again
                    break;
                }

                ProposalPO proposalPO = proposalStorageService.find(chain, hash);
                ProposalTypeEnum proposalTypeEnum = ProposalTypeEnum.getEnum(proposalPO.getType());
                switch (proposalTypeEnum) {
                    case REFUND:
                        // Heterogeneous chain backtracking
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            refund(pendingPO, proposalPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (!refundByzantine(pendingPO, proposalPO)) {
                                ExeProposalPO po = chain.getExeProposalQueue().poll();
                                chain.getExeProposalQueue().addLast(po);
                                continue;
                            }
                        }
                        // Record the original rechargehash Ensure only one execution in the proposal
                        asyncProcessedTxStorageService.saveProposalExe(chain, proposalPO.getHeterogeneousTxHash());
                        break;
                    case TRANSFER:
                        // Transfer to another account
                        NulsHash rechargeHash;
                        if(null != (rechargeHash = rechargeStorageService.find(chain, hash.toHex()))) {
                            chain.getLogger().info("[proposal-{}] executed, proposalHash:{}, rechargeHash:{}",
                                    ProposalTypeEnum.TRANSFER,
                                    hash.toHex(),
                                    rechargeHash.toHex());
                            break;
                        }
                        transfer(pendingPO, proposalPO);
                        asyncProcessedTxStorageService.saveProposalExe(chain, proposalPO.getHeterogeneousTxHash());
                        break;
                    case LOCK:
                        // Freeze account
                        lock(proposalPO.getAddress());
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Execute proposal-{}] proposalHash:{}",
                                ProposalTypeEnum.LOCK,
                                proposalPO.getHash().toHex());
                        break;
                    case UNLOCK:
                        // Unfreezing accounts
                        unlock(proposalPO.getAddress());
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Execute proposal-{}] proposalHash:{}",
                                ProposalTypeEnum.UNLOCK,
                                proposalPO.getHash().toHex());
                        break;
                    case EXPELLED:
                        // Revocation of bank qualification
                        if (chain.getResetVirtualBank().get()) {
                            chain.getLogger().warn("Resetting Virtual Bank Heterogeneous Chain(contract), Proposal waiting1Execute in minutes..");
                            Thread.sleep(60000L);
                            continue;
                        }
                        disqualification(pendingPO, proposalPO);
                        break;
                    case UPGRADE:
                        // Upgrade contract
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
                        // Withdrawal proposal
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            withdrawProposal(pendingPO, proposalPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            withdrawProposalByzantine(pendingPO, proposalPO);
                        }
                    case ADDCOIN:
                        // swapmodule Stable currency exchange transactions - Add Currency
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Create Proposal Confirmation-{}] proposalHash:{}",
                                ProposalTypeEnum.ADDCOIN,
                                proposalPO.getHash().toHex());
                        break;
                    case REMOVECOIN:
                        // swapmodule Stable currency exchange transactions - Remove Currency
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Create Proposal Confirmation-{}] proposalHash:{}",
                                ProposalTypeEnum.REMOVECOIN,
                                proposalPO.getHash().toHex());
                        break;
                    case MANAGE_STABLE_PAIR_FOR_SWAP_TRADE:
                        // swapmodule Managing stablecoin transactions-Used forSwaptransaction
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Create Proposal Confirmation-{}] proposalHash:{}",
                                ProposalTypeEnum.MANAGE_STABLE_PAIR_FOR_SWAP_TRADE,
                                proposalPO.getHash().toHex());
                        break;
                    case MANAGE_SWAP_PAIR_FEE_RATE:
                        // swapmodule Used forSwapCustomized transaction fees
                        publishProposalConfirmed(proposalPO, pendingPO);
                        chain.getLogger().info("[Create Proposal Confirmation-{}] proposalHash:{}",
                                ProposalTypeEnum.MANAGE_SWAP_PAIR_FEE_RATE,
                                proposalPO.getHash().toHex());
                        break;
                    case OTHER:
                    default:
                        break;
                }
                // Store successfully executed transactionshash, Executed items will no longer be executed (When the current node is in synchronous block mode,We also need to save thishash, Indicates that it has been executed)
                asyncProcessedTxStorageService.saveProposalExe(chain, hash.toHex());
                chain.getLogger().debug("[Heterogeneous chain pending queue] Successfully executed to remove transaction, hash:{}", hash.toHex());
                // And remove it from the persistence library
                exeProposalStorageService.delete(chain, hash.toHex());
                // Successfully removed queue header elements
                exeProposalQueue.remove();
            }
        } catch (Exception e) {
            ExeProposalPO po = chain.getExeProposalQueue().poll();
            chain.getExeProposalQueue().addLast(po);

            if(e instanceof NulsException){
                if(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW.equals(((NulsException) e).getErrorCode())){
                    return;
                }
            }
            chain.getLogger().error(e);
        }
    }


    /**
     * Withdrawal proposal version2
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
        // Determine if you have received the message, And signed it
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
                chain.getLogger().error("[Heterogeneous chain address signature message-withdraw] no withdrawCoinTo. hash:{}", tx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
            int heterogeneousChainId = assetInfo.getChainId();
            BigInteger amount = withdrawCoinTo.getAmount();
            String toAddress = txData.getHeterogeneousAddress();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
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
            // Initialize the object for storing signatures
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(withdrawHash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // Broadcast current node signature message
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[withdraw] Calling heterogeneous chain components to execute signatures, Send signed message. hash:{}", txHash);

            // (Withdrawal)Business transactionshash Relationship with proposed transactions
            this.proposalStorageService.saveExeBusiness(chain, txHash, proposalPO.getHash());
        }

        // bytxSubsequentPOProvide a withdrawal optionpo
        TxSubsequentProcessPO txSubsequentPO = new TxSubsequentProcessPO();
        txSubsequentPO.setTx(tx);
        txSubsequentPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
        BlockHeader header = new BlockHeader();
        header.setHeight(chain.getLatestBasicBlock().getHeight());
        txSubsequentPO.setBlockHeader(header);
        chain.getPendingTxQueue().offer(txSubsequentPO);
        txSubsequentProcessStorageService.save(chain, txSubsequentPO);
        chain.getLogger().info("[Withdrawal proposal-Put and execute heterogeneous chain withdrawalstaskin] hash:{}", txHash);
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);
    }

    /**
     * Withdrawal proposal
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
     * Execute withdrawal calls to heterogeneous chains
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
        WithdrawalTxData withdrawTxData = ConverterUtil.getInstance(withdrawTx.getTxData(), WithdrawalTxData.class);
        HeterogeneousAssetInfo assetInfo =
                heterogeneousAssetHelper.getHeterogeneousAssetInfo(withdrawTxData.getHeterogeneousChainId(), withdrawCoinTo.getAssetsChainId(), withdrawCoinTo.getAssetsId());
        if (null == assetInfo) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
        }

        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
        docking.createOrSignWithdrawTx(
                withdrawTx.getHash().toHex(),
                withdrawTxData.getHeterogeneousAddress(),
                withdrawCoinTo.getAmount(),
                assetInfo.getAssetId());
    }

    /**
     * Handling contract upgrades, Byzantine signature
     * Determine and send heterogeneous chain transactions
     *
     * @param pendingPO
     * @param proposalPO
     * @return true:Need to delete elements from the queue(Heterogeneous chain called, Or no need to, No permission to execute, etc) false: Put the team at the end of the line
     * @throws NulsException
     */
    private boolean upgradeByzantine(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash hash = proposalPO.getHash();
        String txHash = hash.toHex();
        // New contract with multiple signed addresses
        int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
        if (pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            return true;
        }
        if (!hasExecutePermission(txHash)) {
            return true;
        }
        // Determine if you have received the message, And signed it
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
            // Compatible with non Ethernet addresses update by pierre at 2021/11/16
            String newMultySignAddress = docking.getAddressString(proposalPO.getAddress());
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
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
            // Initialize the object for storing signatures
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // Broadcast current node signature message
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[Execute proposal-{}] proposalHash:{}",
                    ProposalTypeEnum.UPGRADE, proposalPO.getHash().toHex());
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (!compSignPO.getCompleted()) {
                // Execute calls to heterogeneous chains
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("Virtual Bank Change, Call heterogeneous chain parameter is empty");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                String ethTxHash = docking.createOrSignUpgradeTxII(
                        callParm.getTxHash(),
                        callParm.getUpgradeContract(),
                        callParm.getSigned());
                compSignPO.setCompleted(true);
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-upgrade] Calling heterogeneous chain components to perform contract upgrades. hash:{}, ethHash:{}", txHash, ethTxHash);
            }
            rs = true;
        }
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;

    }

    /**
     * Contract upgrade
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
        // Initiate contract upgrade transactions to heterogeneous chains
        String exeTxHash = docking.createOrSignUpgradeTx(hash);
        this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());

        proposalPO.setHeterogeneousMultySignAddress(docking.getCurrentMultySignAddress());
        // On chain execution of storage proposalshash
        proposalStorageService.save(chain, proposalPO);
        chain.getLogger().info("[Execute proposal-{}] proposalHash:{}, exeTxHash:{}",
                ProposalTypeEnum.UPGRADE,
                proposalPO.getHash().toHex(),
                exeTxHash);
    }

    /**
     * Execution permission judgment
     *
     * @param hash
     * @return
     */
    private boolean hasExecutePermission(String hash) {
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            chain.getLogger().debug("Only virtual banks can execute, Non virtual banking nodes not executed, hash:{}", hash);
            return false;
        }
        return true;
    }

    /**
     * Check for heterogeneous chain business replay attacks
     * about type:1 Return to the original route type:2 Recharge to another address, Same heterogeneous chainhashOnly allowed to execute once
     *
     * @param hash
     * @param heterogeneousTxHash
     * @return
     */
    private boolean replyAttack(String hash, String heterogeneousTxHash) {
        if(null != rechargeStorageService.find(chain, heterogeneousTxHash)){
            chain.getLogger().error("[replyAttack!! Proposal pending queue] The heterogeneous chain transaction in the proposal has already been executed, proposalhash:{} heterogeneousTxHash:{}", hash, heterogeneousTxHash);
            return false;
        }
        if (null != asyncProcessedTxStorageService.getProposalExe(chain, heterogeneousTxHash)) {
            chain.getLogger().error("[replyAttack!! Proposal pending queue] The heterogeneous chain transaction in the proposal has already been executed, proposalhash:{} heterogeneousTxHash:{}", hash, heterogeneousTxHash);
            return false;
        }
        return true;
    }

    /**
     * Return to the original route
     * According to heterogeneous chain recharge transactionshash, Query rechargefromAddress Assets and Amount, Then send it back through heterogeneous chains.
     *
     * @throws NulsException
     */
    private void refund(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash proposalHash = proposalPO.getHash();
        String hash = proposalHash.toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("Non virtual bank members, Or the node is in synchronous block mode, No need to publish a return transaction");
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
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        String exeTxHash = heterogeneousInterface.createOrSignWithdrawTx(hash, info.getFrom(), info.getValue(), info.getAssetId());
        this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());
        chain.getLogger().info("[Execute proposal-{}] proposalHash:{}, exeTxHash:{}",
                ProposalTypeEnum.REFUND,
                proposalPO.getHash().toHex(),
                exeTxHash);
    }

    /**
     * Return to the original route version2
     * According to heterogeneous chain recharge transactionshash, Query rechargefromAddress Assets and Amount, Then send it back through heterogeneous chains.
     * Determine and send heterogeneous chain transactions
     *
     * @throws NulsException
     */
    private boolean refundByzantine(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        NulsHash proposalTxHash = proposalPO.getHash();
        String proposalHash = proposalTxHash.toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("Non virtual bank members, Or the node is in synchronous block mode, No need to publish a return transaction");
            return true;
        }
        if (!hasExecutePermission(proposalHash) || !replyAttack(proposalHash, proposalPO.getHeterogeneousTxHash())) {
            return true;
        }
        // Determine if you have received the message, And signed it
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
                chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
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
            // Initialize the object for storing signatures
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(proposalTxHash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // Broadcast current node signature message
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[Execute proposal-{}] Calling heterogeneous chain components to execute signatures, Send signed message, proposalhash:{}", ProposalTypeEnum.REFUND, proposalHash);
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (!compSignPO.getCompleted()) {
                // Execute calls to heterogeneous chains
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("Virtual Bank Change, Call heterogeneous chain parameter is empty");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                if (chain.getLatestBasicBlock().getHeight() >= FEE_ADDITIONAL_HEIGHT) {
                    BigInteger totalFee = assembleTxService.calculateRefundTotalFee(chain, proposalHash);
                    boolean enoughFeeOfWithdraw = docking.isEnoughNvtFeeOfWithdraw(new BigDecimal(totalFee), callParm.getAssetId(), callParm.getTxHash());
                    if (!enoughFeeOfWithdraw) {
                        chain.getLogger().error("[withdraw] Withdrawal fee calculation, The handling fee is insufficient to cover the withdrawal fee. amount:{}", callParm.getValue());
                        throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                    }
                }
                String ethTxHash = docking.createOrSignWithdrawTxII(
                        callParm.getTxHash(),
                        callParm.getToAddress(),
                        callParm.getValue(),
                        callParm.getAssetId(),
                        callParm.getSigned());
                compSignPO.setCompleted(true);
                this.proposalStorageService.saveExeBusiness(chain, proposalHash, proposalTxHash);
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-refund] Calling heterogeneous chain components to perform original route rollback. hash:{}, ethHash:{}", callParm.getTxHash(), ethTxHash);
            }
            rs = true;
        }
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;
    }

    /**
     * Go to another address
     *
     * @throws NulsException
     */
    private void transfer(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        String hash = proposalPO.getHash().toHex();
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("Non virtual bank members, Or the node is in synchronous block mode, No need to publish confirmation transactions");
            return;
        }
        if (!hasExecutePermission(hash) || !replyAttack(hash, proposalPO.getHeterogeneousTxHash())) {
            return;
        }
        int htgChainId = proposalPO.getHeterogeneousChainId();
        HeterogeneousTransactionInfo info;
        try {
            IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
            info = docking.getUnverifiedDepositTransaction(proposalPO.getHeterogeneousTxHash());
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
        }
        if (null == info) {
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        Transaction tx;
        if (htgChainId < 200) {
            RechargeTxDTO rechargeTxDTO = new RechargeTxDTO();
            rechargeTxDTO.setOriginalTxHash(hash);
            rechargeTxDTO.setHeterogeneousChainId(htgChainId);
            rechargeTxDTO.setHeterogeneousAssetId(info.getAssetId());
            rechargeTxDTO.setAmount(info.getValue());
            rechargeTxDTO.setToAddress(AddressTool.getStringAddressByBytes(proposalPO.getAddress()));
            rechargeTxDTO.setTxtime(pendingPO.getTime());
            rechargeTxDTO.setExtend(info.getDepositIIExtend());
            if (info.isDepositIIMainAndToken()) {
                // Support simultaneous transfer intokenandmain
                rechargeTxDTO.setDepositII(true);
                rechargeTxDTO.setMainAmount(info.getDepositIIMainAssetValue());
            }
            tx = assembleTxService.createRechargeTx(chain, rechargeTxDTO);
        } else if (htgChainId < 300) {
            BtcTxInfo txInfo = (BtcTxInfo) info;
            RechargeTxOfBtcSysDTO dto = new RechargeTxOfBtcSysDTO();
            dto.setHtgTxHash(hash);
            dto.setHtgFrom(txInfo.getFrom());
            dto.setHtgChainId(htgChainId);
            dto.setHtgAssetId(txInfo.getAssetId());
            dto.setHtgTxTime(pendingPO.getTime());
            dto.setHtgBlockHeight(txInfo.getBlockHeight());
            dto.setTo(AddressTool.getStringAddressByBytes(proposalPO.getAddress()));
            dto.setAmount(txInfo.getValue().add(txInfo.getFee()));
            dto.setExtend(txInfo.getExtend0());
            tx = assembleTxService.createRechargeTxOfBtcSys(chain, dto);
        } else {
            throw new NulsException(ConverterErrorCode.PROPOSAL_EXECUTIVE_FAILED);
        }
        proposalPO.setNerveHash(tx.getHash().getBytes());
        // On chain execution of storage proposalshash
        proposalStorageService.save(chain, proposalPO);
        chain.getLogger().info("[Execute proposal-{}] proposalHash:{}, txHash:{}",
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
     * release(Unlock/Lock account)Confirmation transaction of proposal
     *
     * @param proposalPO
     * @param pendingPO
     * @throws NulsException
     */
    private void publishProposalConfirmed(ProposalPO proposalPO, ExeProposalPO pendingPO) throws NulsException {
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("Non virtual bank members, Or the node is in synchronous block mode, No need to publish confirmation transactions");
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
        // Publish proposal to confirm transaction
        assembleTxService.createConfirmProposalTx(chain, txData, pendingPO.getTime());
    }


    private void disqualification(ExeProposalPO pendingPO, ProposalPO proposalPO) throws NulsException {
        if (!VirtualBankUtil.isCurrentDirector(chain) || pendingPO.getSyncStatusEnum() == SyncStatusEnum.SYNC) {
            chain.getLogger().debug("Non virtual bank members, Or the node is in synchronous block mode, No need to publish confirmation transactions");
            return;
        }
        List<byte[]> outList = new ArrayList<>();
        outList.add(proposalPO.getAddress());
        // Create virtual bank change transaction
        Transaction tx = null;
        try {
            tx = assembleTxService.assembleChangeVirtualBankTx(chain, null, outList, pendingPO.getHeight(), pendingPO.getTime());
            proposalPO.setNerveHash(tx.getHash().getBytes());
            // On chain execution of storage proposalshash
            proposalStorageService.save(chain, proposalPO);
            boolean rs = this.proposalStorageService.saveExeBusiness(chain, tx.getHash().toHex(), proposalPO.getHash());
            TransactionCall.newTx(chain, tx);
            chain.getLogger().info("[Execute proposal-{}] proposalHash:{}, txHash:{}, saveExeBusiness:{}",
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
            chain.getLogger().warn("[Execute proposal] This node is no longer a bank member, No need to execute change transactions, Proposal completed.");
        }

    }
}
