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

package network.nerve.converter.core.business.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.MessageService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.management.HeterogeneousCallBackManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.core.validator.ProposalVerifier;
import network.nerve.converter.enums.ByzantineStateEnum;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.message.*;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;

/**
 * @author: Loki
 * @date: 2020-02-27
 */
@Component
public class MessageServiceImpl implements MessageService {

    @Autowired
    private TxStorageService txStorageService;

    @Autowired
    private ProposalVerifier proposalVerifier;

    @Autowired
    private HeterogeneousCallBackManager heterogeneousCallBackManager;

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    @Autowired
    private ComponentSignStorageService componentSignStorageService;
    @Autowired
    private CfmChangeBankStorageService cfmChangeBankStorageService;

    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;

    @Autowired
    private ProposalStorageService proposalStorageService;

    @Autowired
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    private Object objectBankLock = new Object();
    private Object objectWithdrawLock = new Object();
    private Object objectUpgradeLock = new Object();
    private Object objectRefundLock = new Object();

    @Override
    public void newHashSign(Chain chain, String nodeId, BroadcastHashSignMessage message) {
        int chainId = chain.getChainId();
        NulsHash hash = message.getHash();
        P2PHKSignature p2PHKSignature = message.getP2PHKSignature();
        if (null == hash || null == p2PHKSignature || null == p2PHKSignature.getPublicKey() || null == p2PHKSignature.getSignData()) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        // v15Special handling after upgrading the wavefield protocolhashHistorical legacy issues,polygonThe issue of inconsistent transaction height and time caused by block rollback
        if (converterCoreApi.isSupportProtocol15TrxCrossChain() && "e7650127c55c7fa90e8cfded861b9aba0a71e025c318f0e31d53721d864d1e26".equalsIgnoreCase(hash.toHex())) {
            LoggerUtil.LOG.warn("Filter transactionshash: {}", hash.toHex());
            return;
        }
        LoggerUtil.LOG.info("Received node[{}]New transaction signature hash: {}, Signature address:{}, OriginalHash: {}", nodeId, hash.toHex(),
                AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chainId)), message.getOriginalHash());
        TransactionPO txPO = txStorageService.get(chain, hash);
        if (null == txPO) {
            try {
                //If this is the first time the transaction has been received,Temporarily store the message and retrieve the complete transaction from the broadcast node
                UntreatedMessage untreatedMessage = new UntreatedMessage(chainId, nodeId, message, hash);
                List<UntreatedMessage> untreatedMsgList = chain.getFutureMessageMap().computeIfAbsent(hash, v -> new ArrayList<>());
                untreatedMsgList.add(untreatedMessage);
                LoggerUtil.LOG.info("The current node has not yet confirmed the transaction, caching signature messages hash:{}", hash.toHex());
            } catch (Exception e) {
                LoggerUtil.LOG.error(e);
            }

            if (TxType.PROPOSAL == message.getType()) {
                // If it is a proposal transaction, Then it is necessary to actively request transactions from the sender,Non transaction initiating node Unable to create the transaction
                GetTxMessage msg = new GetTxMessage(hash);
                NetWorkCall.broadcast(chain, msg, null, ConverterCmdConstant.GET_TX_MESSAGE);
                return;
            }
            /**
             * Special treatment Transfer the transactionhashInsert task Obtain transaction information after a certain period of time,
             * Prevent nodes that have just joined virtual banks The delay of heterogeneous chain synchronization mechanism prevents the creation of corresponding transactions
             */
            PendingCheckTx pendingCheckTx = new PendingCheckTx(message);
            chain.getPendingCheckTxSet().add(pendingCheckTx);
            return;
        }
        //If the transaction has already been packaged, there is no need to broadcast the signature of the transaction again
        if (txPO.getStatus() != ByzantineStateEnum.UNTREATED.getStatus() || message.getP2PHKSignature() == null) {
            LoggerUtil.LOG.info("The transaction has been processed at this node,Hash:{}\n\n", hash.toHex());
            return;
        }
        try {
            Transaction tx = txPO.getTx();
            if (tx.getType() == TxType.RECHARGE || tx.getType() == TxType.ONE_CLICK_CROSS_CHAIN || tx.getType() == TxType.ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN) {
                CoinData coinData = tx.getCoinDataInstance();
                List<CoinTo> tos = coinData.getTo();
                if (tos == null || tos.isEmpty()) {
                    //chain.getLogger().error("error recharge tx[0], info: {}", tx.format());
                    return;
                }
                for (CoinTo to : tos) {
                    if (!AddressTool.validNormalAddress(to.getAddress(), chainId)) {
                        //chain.getLogger().error("error recharge tx[1], info: {}", tx.format());
                        return;
                    }
                }
            }
            UntreatedMessage untreatedSignMessage = new UntreatedMessage(chainId, nodeId, message, hash);
            chain.getSignMessageByzantineQueue().offer(untreatedSignMessage);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    @Override
    public void getTx(Chain chain, String nodeId, GetTxMessage message) {
        NulsHash hash = message.getHash();
        String nativeHex = hash.toHex();
        LoggerUtil.LOG.info("node:{},Obtain complete transactions from this node,Hash:{}", nodeId, nativeHex);
        TransactionPO txPO = txStorageService.get(chain, hash);
        if (null == txPO) {
            chain.getLogger().error("The transaction does not exist at the current node,Hash:{}", nativeHex);
            return;
        }
        NewTxMessage newTxMessage = new NewTxMessage();
        newTxMessage.setTx(txPO.getTx());
        //Send the complete transaction to the requesting node
        if (!NetWorkCall.sendToNode(chain, newTxMessage, nodeId, ConverterCmdConstant.NEW_TX_MESSAGE)) {
            LoggerUtil.LOG.info("Send complete transaction to node:{}, fail! Hash:{}\n\n", nodeId, nativeHex);
            return;
        }
        LoggerUtil.LOG.info("Send the complete transaction to the in chain nodes:{}, Hash:{}\n\n", nodeId, nativeHex);
    }

    @Override
    public void receiveTx(Chain chain, String nodeId, NewTxMessage message) {
        Transaction tx = message.getTx();
        NulsHash localHash = tx.getHash();
        String localHashHex = localHash.toHex();
        LoggerUtil.LOG.info("Received in chain node:{}The complete transaction sent over,Hash:{}", nodeId, localHashHex);
        //Determine whether this node has received the transaction. If it has, ignore it directly
        TransactionPO txPO = txStorageService.get(chain, localHash);
        if (txPO != null) {
            LoggerUtil.LOG.info("The transaction has been received and processed,Hash:{}\n\n", localHashHex);
            return;
        }
        // Verify the transaction
        try {
            proposalVerifier.validate(chain, tx);
        } catch (NulsException e) {
            chain.getLogger().error("Received complete transaction, Verification failed. hash:{}", localHashHex);
            chain.getLogger().error(e);
            return;
        }
        txStorageService.save(chain, new TransactionPO(message.getTx()));
        List<UntreatedMessage> listMsg = chain.getFutureMessageMap().get(localHash);
        if (null != listMsg) {
            for (UntreatedMessage msg : listMsg) {
                chain.getSignMessageByzantineQueue().offer(msg);
            }
            // Clear cached signatures
            chain.getFutureMessageMap().remove(localHash);
        }
        if (TxType.PROPOSAL == message.getTx().getType()) {
            P2PHKSignature p2PHKSignature;
            try {
                p2PHKSignature = ConverterSignUtil.addSignatureByDirector(chain, message.getTx());
            } catch (NulsException e) {
                chain.getLogger().error("proposal tx sign fail");
                chain.getLogger().error(e);
                return;
            }
            BroadcastHashSignMessage msg = new BroadcastHashSignMessage(tx, p2PHKSignature);
            NetWorkCall.broadcast(chain, msg, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
            LoggerUtil.LOG.debug("[newTx-message] Broadcast the signature of this node on the proposal txhash:{}, Signature address:{}",
                    message.getTx().getHash().toHex(),
                    AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId())));
        }
    }

    @Override
    public void checkRetryParse(Chain chain, String nodeId, CheckRetryParseMessage message) {
        int heterogeneousChainId = message.getHeterogeneousChainId();
        String heterogeneousTxHash = message.getHeterogeneousTxHash();
        if (heterogeneousChainId <= 0 || StringUtils.isBlank(heterogeneousTxHash)) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        String hash = txStorageService.getHeterogeneousHash(chain, heterogeneousTxHash);
        if (StringUtils.isNotBlank(hash)) {
            // Processed
            return;
        }
        IDepositTxSubmitter submitter = heterogeneousCallBackManager.createOrGetDepositTxSubmitter(chain.getChainId(), heterogeneousChainId);
        Result result = submitter.validateDepositTx(heterogeneousTxHash);
        if (result.isFailed()) {
            chain.getLogger().error("Re analyze heterogeneous transactions, validateDepositTx Verification failed, {}", result.getErrorCode().getCode());
            return;
        }
        try {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            boolean rs = docking.reAnalysisDepositTx(heterogeneousTxHash);
            if (rs) {
                txStorageService.saveHeterogeneousHash(chain, heterogeneousTxHash);
                NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.CHECK_RETRY_PARSE_MESSAGE);
                chain.getLogger().info("[checkRetryParse Message processing completed] isomerismchainId: {}, isomerismhash:{}", heterogeneousChainId, heterogeneousTxHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancelHtgTx(Chain chain, String nodeId, CancelHtgTxMessage message) {
        try {
            int heterogeneousChainId = message.getHeterogeneousChainId();
            String address = message.getHeterogeneousAddress();
            String nonce = message.getNonce();
            String priceGwei = message.getPriceGwei();
            if (heterogeneousChainId <= 0
                    || StringUtils.isBlank(address)
                    || StringUtils.isBlank(nonce)
                    || StringUtils.isBlank(priceGwei)
            ) {
                throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (address.equalsIgnoreCase(docking.getCurrentSignAddress())) {
                String hash = docking.cancelHtgTx(nonce, priceGwei);
                chain.getLogger().info("[cancelHtgTx Message processing completed] isomerismchainId: {}, isomerismaddress:{}, Cancel operationhash:{}", heterogeneousChainId, address, hash);
            } else {
                NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
                chain.getLogger().info("[cancelHtgTx Message forwarding completed] isomerismchainId: {}, isomerismaddress:{}", heterogeneousChainId, address);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isExistMessage(ComponentSignByzantinePO compSignPO, ComponentSignMessage message) {
        boolean existMsg = false;
        if (null != compSignPO && null != compSignPO.getListMsg()) {
            out:
            for (ComponentSignMessage uMsg : compSignPO.getListMsg()) {
                for (HeterogeneousSign sign : uMsg.getListSign()) {
                    for (HeterogeneousSign messageSign : message.getListSign()) {
                        if (sign.getHeterogeneousAddress().equals(messageSign.getHeterogeneousAddress())) {
                            existMsg = true;
                            break out;
                        }
                    }
                }
            }
        }
        // Received this message
        return existMsg;
    }

    private ComponentSignByzantinePO initCompSignPO(ComponentSignByzantinePO compSignPO, NulsHash hash) {
        // Initialize the object for storing signatures
        if (null == compSignPO) {
            compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
        } else if (null == compSignPO.getListMsg()) {
            compSignPO.setListMsg(new ArrayList<>());
        }
        return compSignPO;
    }

    @Override
    public void componentSign(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate) {
        if (converterCoreApi.isProtocol21()) {
            this.componentSignV21(chain, nodeId, message, isCreate);
        } else {
            this.componentSignV0(chain, nodeId, message, isCreate);
        }
    }

    private void componentSignV0(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate) {
        NulsHash hash = message.getHash();
        List<HeterogeneousSign> listSign = message.getListSign();
        if (null == hash || null == listSign || listSign.size() == 0) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        // Obtain transactions
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[Heterogeneous chain address signature message-The transaction was not found], Heterogeneous chain address signature message, Received node[{}] txhash:{}, Heterogeneous chainId:{}, Signature address:{}",
                        nodeId, hash.toHex(), ids, signAddress);
                return;
            }
            switch (tx.getType()) {
                case TxType.CHANGE_VIRTUAL_BANK:
                    changeVirtualBankMessageProcess(chain, nodeId, tx, message, isCreate);
                    break;
                case TxType.WITHDRAWAL:
                    withdrawMessageProcess(chain, nodeId, tx, message);
                    break;
                case TxType.PROPOSAL:
                    ProposalPO proposalPO = proposalStorageService.find(chain, hash);
                    ProposalTypeEnum proposalTypeEnum = ProposalTypeEnum.getEnum(proposalPO.getType());
                    switch (proposalTypeEnum) {
                        case REFUND:
                            refundMessageProcess(chain, nodeId, tx, proposalPO, message);
                            break;
                        case UPGRADE:
                            // Determine if the proposal type is contract upgrade, Broadcasting corresponds to proposal transactionshash And for thehashSignature of
                            upgradeMessageProcess(chain, nodeId, tx, proposalPO, message);
                            break;
                        default:
                    }
                    break;
                default:
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * protocolv21 Support one click cross chain
     */
    private void componentSignV21(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate) {
        NulsHash hash = message.getHash();
        if ("a051a43ff30e9ce9693512688a9a9a95bd13c3e2244e6556ea128df3aad859e5".equals(hash.toHex())) {
            return;
        }
        List<HeterogeneousSign> listSign = message.getListSign();
        if (null == hash || null == listSign || listSign.size() == 0) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        // Obtain transactions
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[Heterogeneous chain address signature message-The transaction was not found], Heterogeneous chain address signature message, Received node[{}] txhash:{}, Heterogeneous chainId:{}, Signature address:{}",
                        nodeId, hash.toHex(), ids, signAddress);
                return;
            }
            switch (tx.getType()) {
                case TxType.CHANGE_VIRTUAL_BANK:
                    changeVirtualBankMessageProcess(chain, nodeId, tx, message, isCreate);
                    break;
                case TxType.WITHDRAWAL:
                case TxType.ONE_CLICK_CROSS_CHAIN:
                    withdrawMessageProcess(chain, nodeId, tx, message);
                    break;
                case TxType.PROPOSAL:
                    ProposalPO proposalPO = proposalStorageService.find(chain, hash);
                    ProposalTypeEnum proposalTypeEnum = ProposalTypeEnum.getEnum(proposalPO.getType());
                    switch (proposalTypeEnum) {
                        case REFUND:
                            refundMessageProcess(chain, nodeId, tx, proposalPO, message);
                            break;
                        case UPGRADE:
                            // Determine if the proposal type is contract upgrade, Broadcasting corresponds to proposal transactionshash And for thehashSignature of
                            upgradeMessageProcess(chain, nodeId, tx, proposalPO, message);
                            break;
                        default:
                    }
                    break;
                default:
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    @Override
    public void retryVirtualBankSign(Chain chain, String nodeId, VirtualBankSignMessage message, boolean isCreate) {
        NulsHash hash = message.getHash();
        List<HeterogeneousSign> listSign = message.getListSign();
        if (null == hash || null == listSign || listSign.size() == 0) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        // Obtain transactions
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[Resend virtual bank signature change message-The transaction was not found], Heterogeneous chain address signature message, Received node[{}] txhash:{}, Heterogeneous chainId:{}, Signature address:{}",
                        nodeId, hash.toHex(), ids, signAddress);
                return;
            }
            if (tx.getType() != TxType.CHANGE_VIRTUAL_BANK) {
                LoggerUtil.LOG.error("[Resend virtual bank signature change message-Transaction type error]-{}", tx.getType());
                return;
            }
            this.retryVirtualBankMessageProcess(chain, nodeId, tx, message, isCreate);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * Processing messages for virtual bank change transactions
     * 1.Verify signature
     * 2.Processing messages, storage, transmit,
     * 3.Calculate Byzantium, If Byzantium passes, adjust heterogeneous chain components
     *
     * @param chain
     * @param tx
     * @param message
     * @throws NulsException
     */
    private void changeVirtualBankMessageProcess(Chain chain, String nodeId, Transaction tx, ComponentSignMessage message, boolean isCreate) throws Exception {
        synchronized (objectBankLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            LoggerUtil.LOG.info("[Heterogeneous chain address signature message - handlechangeVirtualBank], Received node[{}]  hash:{}", nodeId, txHash);
            ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, txHash);
            if (po != null) {
                LoggerUtil.LOG.info("[Completed - Heterogeneous chain address signature message - handlechangeVirtualBank], Transaction Completed. TxHash: {}", txHash);
                return;
            }
            // Determine if you have received the message
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            boolean existMessage = isExistMessage(compSignPO, message);
            //if (isExistMessage(compSignPO, message)) {
            //    return;
            //}
            compSignPO = initCompSignPO(compSignPO, hash);
            List<IHeterogeneousChainDocking> chainDockings = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
            if (null == chainDockings || chainDockings.isEmpty()) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            int htgChainSize = chainDockings.size();
            // validate
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            // The virtual bank with cache changes corresponds to the addresses on each chain
            Map<Integer, String[][]> addressCache = new HashMap<>();
            for (IHeterogeneousChainDocking docking : chainDockings) {
                int hChainId = docking.getChainId();
                // Assembly with added parameters
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                }

                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                }
                //if (converterCoreApi.checkChangeP35(txHash)) {
                //    inAddress = converterCoreApi.inChangeP35();
                //    outAddress = converterCoreApi.outChangeP35();
                //}
                addressCache.put(hChainId, new String[][]{inAddress, outAddress});
            }
            // Verify the correctness of the received message
            Map<Integer, IHeterogeneousChainDocking> chainDockingMap = chainDockings.stream().collect(Collectors.toMap(IHeterogeneousChainDocking::getChainId, Function.identity()));
            do {
                if (existMessage) {
                    break;
                }
                boolean verifySignManagerChangesII = true;
                for (HeterogeneousSign sign : message.getListSign()) {
                    String signAddress = sign.getHeterogeneousAddress().getAddress();
                    IHeterogeneousChainDocking docking = chainDockingMap.get(sign.getHeterogeneousAddress().getChainId());
                    if (docking == null) {
                        LoggerUtil.LOG.error("[Virtual Bank Change Signature Message-Heterogeneous chainIDerror], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                                nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // Verify message signature
                    Boolean msgPass = docking.verifySignManagerChangesII(
                            signAddress,
                            txHash,
                            inAddress,
                            outAddress,
                            1,
                            HexUtil.encode(sign.getSignature()));
                    if (!msgPass) {
                        verifySignManagerChangesII = false;
                        LoggerUtil.LOG.error("[Virtual Bank Change Signature Message-Signature verification failed-changeVirtualBank], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                                nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                }
                if (verifySignManagerChangesII) {
                    // Add the message received this time
                    compSignPO.getListMsg().add(message);
                    // Verification passed, relay the message
                    NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
                }
            } while (false);
            ComponentSignMessage currentMessage = null;
            /**
             * (Signature for change transaction,Unified business for multiple components)
             * Unified identification of multiple components, When a single component is completed, multiple components must be completed simultaneously.
             */
            boolean signed = false;
            boolean completed = false;
            boolean bztPass = false;
            // Current node signature
            do {
                if (compSignPO.getCurrentSigned()){
                    break;
                }
                // If the current node has not been signed, Then a signature is required, collect, broadcast.
                List<HeterogeneousSign> currentSignList = new ArrayList<>();
                // Check if the current node has been newly added No need to sign
                boolean currentJoin = false;
                SignAccountDTO packerInfo = ConsensusCall.getPackerInfo(chain);
                VirtualBankDirector director = chain.getMapVirtualBank().get(packerInfo.getAddress());
                List<byte[]> inAgents = txData.getInAgents();
                if (inAgents != null) {
                    for (int i = 0, size = inAgents.size(); i < size; i++) {
                        byte[] bytes = inAgents.get(i);
                        String agentAddress = AddressTool.getStringAddressByBytes(bytes);
                        if (agentAddress.equals(director.getAgentAddress())) {
                            currentJoin = true;
                        }
                    }
                }
                // Is the current node newly joined No need to sign
                if (currentJoin) {
                    break;
                }
                // Message signature
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    /**
                     * If the current node has not yet signed, trigger the collection of signatures from various heterogeneous chain components of the current node
                     * Due to the virtual banking change involving all heterogeneous chain components, So each component needs to be signed
                     * And join the broadcast in a message.
                     */
                    String signStrData = docking.signManagerChangesII(txHash, inAddress, outAddress, 1);
                    String currentHaddress = docking.getCurrentSignAddress();
                    if (StringUtils.isBlank(currentHaddress)) {
                        throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                    }
                    HeterogeneousSign currentSign = new HeterogeneousSign(
                            new HeterogeneousAddress(hChainId, currentHaddress),
                            HexUtil.decode(signStrData));
                    currentSignList.add(currentSign);
                    if (null == currentMessage) {
                        currentMessage = new ComponentSignMessage(message.getVirtualBankTotal(),
                                hash, currentSignList);
                    }
                    if (currentSignList.size() == htgChainSize) {
                        compSignPO.getListMsg().add(currentMessage);
                        signed = true;
                    }
                }
            } while (false);
            // Check if sufficient node signatures have been collected
            do {
                boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                if (!byzantinePass) {
                    // Not enough signatures reached
                    break;
                }
                if (compSignPO.getByzantinePass()) {
                    // This process has already been executed
                    break;
                }
                List<Map<Integer, HeterogeneousSign>> list = compSignPO.getListMsg().stream().map(m -> m.getListSign().stream().collect(Collectors.toMap(hSign -> hSign.getHeterogeneousAddress().getChainId(), Function.identity()))).collect(Collectors.toList());
                int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
                List<ComponentCallParm> callParmsList = new ArrayList<>();
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // Processing messages and Byzantine verification, update compSignPO etc.
                    if (isCreate) {
                        // Create heterogeneous chain transactions
                        StringBuilder signatureDataBuilder = new StringBuilder();
                        int count = 0;
                        for (Map<Integer, HeterogeneousSign> map : list) {
                            HeterogeneousSign heterogeneousSign = map.get(hChainId);
                            if (heterogeneousSign == null) {
                                LoggerUtil.LOG.warn("[Virtual Bank Change Signature Message-Incomplete signature information-changeVirtualBank], Received node[{}], txhash: {}, Heterogeneous chainId:{}", nodeId, txHash, hChainId);
                                continue;
                            }
                            signatureDataBuilder.append(HexUtil.encode(heterogeneousSign.getSignature()));
                            if (hChainId > 200) {
                                // split ',' for the signature of the bitSys'chain, due to the non-fixed length signature
                                signatureDataBuilder.append(",");
                            }
                            count++;
                        }
                        if (hChainId > 200) {
                            signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
                        }
                        // Reaching the minimum number of signatures is the only valid call data
                        if (count >= byzantineMinPassCount) {
                            ComponentCallParm callParm = new ComponentCallParm(
                                    hChainId,
                                    tx.getType(),
                                    txHash,
                                    inAddress,
                                    outAddress,
                                    1,
                                    signatureDataBuilder.toString());
                            callParmsList.add(callParm);
                            chain.getLogger().info("[Virtual Bank Change Signature Message-Byzantine passage-changeVirtualBank] Storage heterogeneous chain components execute virtual bank change call parameters. hash:{}, callParm chainId:{}", txHash, callParm.getHeterogeneousId());
                        }
                    } else {
                        completed = true;
                    }
                }
                if (callParmsList.size() == htgChainSize) {
                    compSignPO.setCallParms(callParmsList);
                    bztPass = true;
                }
            } while (false);
            if (bztPass) {
                compSignPO.setByzantinePass(true);
            }
            if (completed) {
                compSignPO.setCompleted(true);
            }
            if (signed) {
                compSignPO.setCurrentSigned(true);
                // Broadcast current node signature message
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);

            /*List<HeterogeneousSign> currentSignList = null;
            if (!compSignPO.getCurrentSigned()) {
                // If the current node has not been signed, Then a signature is required, collect, broadcast.
                currentSignList = new ArrayList<>();
            }
            ComponentSignMessage currentMessage = null;

            *//**
             * (Signature for change transaction,Unified business for multiple components)
             * Unified identification of multiple components, When a single component is completed, multiple components must be completed simultaneously.
             *//*
            boolean signed = false;
            boolean completed = false;
            boolean bztPass = false;
            boolean addedMsg = false;
            for (IHeterogeneousChainDocking hInterface : chainDockings) {
                for (HeterogeneousSign sign : message.getListSign()) {
                    if (hInterface.getChainId() == sign.getHeterogeneousAddress().getChainId()) {
                        int hChainId = hInterface.getChainId();
                        String signAddress = sign.getHeterogeneousAddress().getAddress();
                        // Assembly with added parameters
                        String[] inAddress = new String[inSize];
                        if (null != txData.getInAgents()) {
                            getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                        }

                        String[] outAddress = new String[outSize];
                        if (null != txData.getOutAgents()) {
                            getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                        }
                        // Verify message signature
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hInterface.getChainId());
                        Boolean msgPass = docking.verifySignManagerChangesII(
                                signAddress,
                                txHash,
                                inAddress,
                                outAddress,
                                1,
                                HexUtil.encode(sign.getSignature()));
                        if (!msgPass) {
                            LoggerUtil.LOG.error("[Heterogeneous chain address signature message-Signature verification failed-changeVirtualBank], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                                    nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                            continue;
                        }
                        // Verification passed, relay the message
                        NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
                        if (!addedMsg) {
                            //Add the message received this time first
                            compSignPO.getListMsg().add(message);
                            addedMsg = true;
                        }
                        if (!compSignPO.getCurrentSigned()) {
                            SignAccountDTO packerInfo = ConsensusCall.getPackerInfo(chain);
                            VirtualBankDirector director = chain.getMapVirtualBank().get(packerInfo.getAddress());
                            boolean currentJoin = false;
                            List<byte[]> inAgents = txData.getInAgents();
                            if (inAgents != null) {
                                for (int i = 0, size = inAgents.size(); i < size; i++) {
                                    byte[] bytes = inAgents.get(i);
                                    String agentAddress = AddressTool.getStringAddressByBytes(bytes);
                                    if (agentAddress.equals(director.getAgentAddress())) {
                                        currentJoin = true;
                                    }
                                }
                            }
                            // The current node is newly added No need to sign
                            if (!currentJoin) {
                                *//**
                                 * If the current node has not yet signed, trigger the collection of signatures from various heterogeneous chain components of the current node
                                 * Due to the virtual banking change involving all heterogeneous chain components, So each component needs to be signed
                                 * And join the broadcast in a message.
                                 *//*
                                String signStrData = docking.signManagerChangesII(txHash, inAddress, outAddress, 1);
                                String currentHaddress = docking.getCurrentSignAddress();
                                if (StringUtils.isBlank(currentHaddress)) {
                                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                                }
                                HeterogeneousSign currentSign = new HeterogeneousSign(
                                        new HeterogeneousAddress(hChainId, currentHaddress),
                                        HexUtil.decode(signStrData));
                                currentSignList.add(currentSign);
                                if (null == currentMessage) {
                                    currentMessage = new ComponentSignMessage(message.getVirtualBankTotal(),
                                            hash, currentSignList);
                                    compSignPO.getListMsg().add(currentMessage);
                                }
                                signed = true;
                            }
                        }
                        // Processing messages and Byzantine verification, updatecompSignPOetc.
                        boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                        if (byzantinePass && !compSignPO.getByzantinePass()) {
                            if (isCreate) {
                                // Create heterogeneous chain transactions
                                StringBuilder signatureDataBuilder = new StringBuilder();
                                for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                                    for (HeterogeneousSign sig : msg.getListSign()) {
                                        if (sig.getHeterogeneousAddress().getChainId() == hChainId) {
                                            signatureDataBuilder.append(HexUtil.encode(sig.getSignature()));
                                        }
                                    }
                                }
                                ComponentCallParm callParm = new ComponentCallParm(
                                        docking.getChainId(),
                                        tx.getType(),
                                        txHash,
                                        inAddress,
                                        outAddress,
                                        1,
                                        signatureDataBuilder.toString());
                                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                                if (null == callParmsList) {
                                    callParmsList = new ArrayList<>();
                                }
                                callParmsList.add(callParm);
                                chain.getLogger().info("[test] callParm chainId:{}, address", callParm.getHeterogeneousId(), callParm.getSignAddress());
                                compSignPO.setCallParms(callParmsList);
                                bztPass = true;
                                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-changeVirtualBank] Storage heterogeneous chain components execute virtual bank change call parameters. hash:{}", txHash);
                            } else {
                                completed = true;
                            }
                        }
                    }
                }
            }
            compSignPO.setByzantinePass(bztPass);
            compSignPO.setCompleted(completed);
            compSignPO.setCurrentSigned(signed);
            if (null != currentSignList && !currentSignList.isEmpty()) {
                compSignPO.setCurrentSigned(true);
                // Broadcast current node signature message
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);*/
        }
    }

    /**
     * Obtain heterogeneous chain addresses
     */
    private void getHeterogeneousAddress(Chain chain, int heterogeneousChainId, String[] address, List<byte[]> list) throws NulsException {
        for (int i = 0; i < list.size(); i++) {
            byte[] bytes = list.get(i);

            String agentAddress = AddressTool.getStringAddressByBytes(bytes);
            String signAddress = virtualBankAllHistoryStorageService.findSignAddressByAgentAddress(chain, agentAddress);
            if (StringUtils.isBlank(signAddress)) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
            if (heterogeneousChainId > 200) {
                // fill in the pubkey on the bitSys'chain
                address[i] = director.getSignAddrPubKey();
            } else {
                HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                if (null == heterogeneousAddress) {
                    chain.getLogger().warn("Heterogeneous chain address signature message[changeVirtualBank] No heterogeneous chain address obtained, agentAddress:{}", agentAddress);
                    chain.getLogger().warn("(Negligible)Unable to obtain heterogeneous chain address: If the current node processes virtual bank change transactions slightly slower(Not yet completedcommit, Causing inability to obtain heterogeneous chain addresses)");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                address[i] = heterogeneousAddress.getAddress();
            }
        }
    }

    /**
     * Message processing withdrawal transactions
     * 1.Verify signature
     * 2.Processing messages, storage, transmit,
     * 3.Calculate Byzantium, If Byzantium passes, adjust heterogeneous chain components
     *
     * @param chain
     * @param tx
     * @param message
     * @throws NulsException
     */
    private void withdrawMessageProcess(Chain chain, String nodeId, Transaction tx, ComponentSignMessage message) throws Exception {
        synchronized (objectWithdrawLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            // Determine if you have received the message
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            int signAddressChainId = sign.getHeterogeneousAddress().getChainId();
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            LoggerUtil.LOG.debug("[Heterogeneous chain address signature message-handlewithdraw], Received node[{}]  hash: {}, Signature address:{}-{}",
                    nodeId, txHash, signAddressChainId, signAddress);
            // validate
            int htgChainId = 0;
            String toAddress = null;
            if (TxType.WITHDRAWAL == tx.getType()) {
                WithdrawalTxData txData1 = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                htgChainId = txData1.getHeterogeneousChainId();
                toAddress = txData1.getHeterogeneousAddress();
            } else if (TxType.ONE_CLICK_CROSS_CHAIN == tx.getType()) {
                OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
                htgChainId = txData.getDesChainId();
                toAddress = txData.getDesToAddress();
            }
            CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
            HeterogeneousAssetInfo assetInfo = null;
            CoinTo withdrawCoinTo = null;
            byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                    assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
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
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // check BTC sys' chains
            if (heterogeneousChainId > 200) {
                withdrawMessageForBTC(chain, docking, tx.getType(), signAddress, hash, toAddress, amount, assetInfo.getAssetId(), HexUtil.encode(sign.getSignature()), nodeId, message, compSignPO);
                return;
            }
            // Verify the correctness of the signature based on the transaction
            Boolean msgPass = docking.verifySignWithdrawII(
                    signAddress,
                    txHash,
                    toAddress,
                    amount,
                    assetInfo.getAssetId(),
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[Heterogeneous chain address signature message - Signature verification failed-withdraw], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // Verification passed, relay the message
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);

            // Processing messages and Byzantine verification, updatecompSignPOetc.
            if (!compSignPO.getCurrentSigned()) {
                LoggerUtil.LOG.debug("[Heterogeneous chain address signature message Execute current node signature] txHash:{}", txHash);
                // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
                String signStrData = docking.signWithdrawII(txHash, toAddress, amount, assetInfo.getAssetId());
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // Create heterogeneous chain transactions
                StringBuilder signatureDataBuilder = new StringBuilder();
                // Splice all signatures
                for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                    signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature()));
                }
                ComponentCallParm callParm = new ComponentCallParm(
                        docking.getChainId(),
                        tx.getType(),
                        txHash,
                        toAddress,
                        amount,
                        assetInfo.getAssetId(),
                        signatureDataBuilder.toString());
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    callParmsList = new ArrayList<>();
                }
                callParmsList.add(callParm);
                compSignPO.setCallParms(callParmsList);
                compSignPO.setByzantinePass(byzantinePass);
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-withdraw] Storage heterogeneous chain component execution withdrawal call parameters. hash:{}", txHash);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }

    private void withdrawMessageForBTC(Chain chain, IHeterogeneousChainDocking docking,
                                       int txType,
                                       String signAddress,
                                       NulsHash hash,
                                       String toAddress,
                                       BigInteger amount,
                                       int assetId,
                                       String signature,
                                       String nodeId,
                                       ComponentSignMessage message,
                                       ComponentSignByzantinePO compSignPO
                                       ) throws Exception {
        int heterogeneousChainId = docking.getChainId();
        String txHash = hash.toHex();
        // Verify the correctness of the signature based on the transaction
        Boolean msgPass = docking.getBitCoinApi().verifySignWithdraw(
                signAddress,
                txHash,
                toAddress,
                amount,
                assetId,
                signature);
        if (!msgPass) {
            LoggerUtil.LOG.error("[Heterogeneous chain address signature message - Signature verification failed-withdraw], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                    nodeId, txHash, heterogeneousChainId, signAddress, signature);
            return;
        }
        // Verification passed, relay the message
        NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);

        // Processing messages and Byzantine verification, updatecompSignPOetc.
        if (!compSignPO.getCurrentSigned()) {
            LoggerUtil.LOG.debug("[Heterogeneous chain address signature message Execute current node signature] txHash:{}", txHash);
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
            String signStrData = docking.getBitCoinApi().signWithdraw(txHash, toAddress, amount, assetId);
            String currentHaddress = docking.getCurrentSignAddress();
            if (StringUtils.isBlank(currentHaddress)) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
        }
        boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
        if (byzantinePass && !compSignPO.getByzantinePass()) {
            // Create heterogeneous chain transactions
            StringBuilder signatureDataBuilder = new StringBuilder();
            // Splice all signatures
            for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature())).append(",");
            }
            signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
            ComponentCallParm callParm = new ComponentCallParm(
                    docking.getChainId(),
                    txType,
                    txHash,
                    toAddress,
                    amount,
                    assetId,
                    signatureDataBuilder.toString());
            List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
            if (null == callParmsList) {
                callParmsList = new ArrayList<>();
            }
            callParmsList.add(callParm);
            compSignPO.setCallParms(callParmsList);
            compSignPO.setByzantinePass(byzantinePass);
            chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-withdraw] Storage heterogeneous chain component execution withdrawal call parameters. hash:{}", txHash);
        }
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);
    }


    private void broadcastCurrentSign(Chain chain, NulsHash hash, ComponentSignByzantinePO compSignPO, String signStrData, HeterogeneousAddress heterogeneousAddress, int virtualBankTotal) {
        HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
        List<HeterogeneousSign> listSign = new ArrayList<>();
        listSign.add(currentSign);
        ComponentSignMessage currentMessage = new ComponentSignMessage(virtualBankTotal,
                hash, listSign);
        compSignPO.getListMsg().add(currentMessage);
        compSignPO.setCurrentSigned(true);
        // Broadcast current node signature message
        NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
    }


    /**
     * Proposal returned by the original route version2
     *
     * @param chain
     * @param nodeId
     * @param proposalPO
     * @param message
     * @throws NulsException
     */
    private void refundMessageProcess(Chain chain, String nodeId, Transaction tx, ProposalPO proposalPO, ComponentSignMessage message) throws Exception {
        synchronized (objectRefundLock) {
            NulsHash hash = proposalPO.getHash();
            String txHash = hash.toHex();
            // Determine if you have received the message
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, txHash);
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
            LoggerUtil.LOG.info("[Heterogeneous chain address signature message-handlerefund], Received node[{}]  hash: {}, Signature address:{}-{}",
                    nodeId, txHash, heterogeneousChainId, signAddress);

            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                    heterogeneousChainId,
                    proposalPO.getHeterogeneousTxHash(),
                    HeterogeneousTxTypeEnum.DEPOSIT,
                    this.heterogeneousDockingManager,
                    docking);
            if (null == info) {
                chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
            Boolean msgPass = docking.verifySignWithdrawII(
                    signAddress,
                    txHash,
                    info.getFrom(),
                    info.getValue(),
                    info.getAssetId(),
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[Heterogeneous chain address signature message - Signature verification failed-refund], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // Verification passed, relay the message
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);

            // Processing messages and Byzantine verification, updatecompSignPOetc.
            if (!compSignPO.getCurrentSigned()) {
                LoggerUtil.LOG.info("[Heterogeneous chain address signature message Execute current node signature] txHash:{}", txHash);
                // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
                String signStrData = docking.signWithdrawII(txHash, info.getFrom(), info.getValue(), info.getAssetId());
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // Create heterogeneous chain transactions
                StringBuilder signatureDataBuilder = new StringBuilder();
                // Splice all signatures
                for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                    signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature()));
                }
                ComponentCallParm callParm = new ComponentCallParm(
                        docking.getChainId(),
                        tx.getType(),
                        proposalPO.getType(),
                        txHash,
                        info.getFrom(),
                        info.getValue(),
                        info.getAssetId(),
                        signatureDataBuilder.toString());
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    callParmsList = new ArrayList<>();
                }
                callParmsList.add(callParm);
                compSignPO.setCallParms(callParmsList);
                compSignPO.setByzantinePass(byzantinePass);
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-refund] Store heterogeneous chain components to execute backhaul call parameters. hash:{}", txHash);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);

        }
    }


    /**
     * Processing messages for upgrading transactions of heterogeneous chain contracts
     * 1.Verify signature
     * 2.Processing messages, storage, transmit,
     * 3.Calculate Byzantium, If Byzantium passes, adjust heterogeneous chain components
     *
     * @param chain
     * @param proposalPO
     * @param message
     * @throws NulsException
     */
    private void upgradeMessageProcess(Chain chain, String nodeId, Transaction tx, ProposalPO proposalPO, ComponentSignMessage message) throws Exception {
        synchronized (objectUpgradeLock) {
            NulsHash hash = proposalPO.getHash();
            String txHash = hash.toHex();
            // Determine if you have received the message
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            // Verify message signature
            int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
            LoggerUtil.LOG.info("[Heterogeneous chain address signature message-handlewithdraw], Received node[{}]  hash: {}, Signature address:{}-{}",
                    nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress);
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // New contract with multiple signed addresses
            // Compatible with non Ethernet addresses update by pierre at 2021/11/16
            String newMultySignAddress = docking.getAddressString(proposalPO.getAddress());
            Boolean msgPass = docking.verifySignUpgradeII(
                    signAddress,
                    txHash,
                    newMultySignAddress,
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[Heterogeneous chain address signature message - Signature verification failed-upgrade], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // Verification passed, relay the message
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
            // Processing messages and Byzantine verification, updatecompSignPOetc.
            if (!compSignPO.getCurrentSigned()) {
                // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
                String signStrData = docking.signUpgradeII(txHash, newMultySignAddress);
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // Create heterogeneous chain transactions
                StringBuilder signatureDataBuilder = new StringBuilder();
                for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                    signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature()));
                }
                ComponentCallParm callParm = new ComponentCallParm(
                        docking.getChainId(),
                        tx.getType(),
                        proposalPO.getType(),
                        txHash,
                        newMultySignAddress,
                        signatureDataBuilder.toString());
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    callParmsList = new ArrayList<>();
                }
                callParmsList.add(callParm);
                compSignPO.setCallParms(callParmsList);
                compSignPO.setByzantinePass(byzantinePass);
                // Business processing after executing proposals
                this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());
                proposalPO.setHeterogeneousMultySignAddress(docking.getCurrentMultySignAddress());
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-upgrade] Store and call heterogeneous chain components to perform contract upgrade call parameters. hash:{}", txHash);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }

    private boolean processComponentSignMsgByzantine(Chain chain, ComponentSignMessage message, ComponentSignByzantinePO compSignPO) {
        return processComponentSignMsgByzantine(chain, message, compSignPO, true);
    }

    private boolean processComponentSignMsgByzantine(Chain chain, ComponentSignMessage message, ComponentSignByzantinePO compSignPO, boolean addMsg) {
        // Add current signature message
        List<ComponentSignMessage> list = compSignPO.getListMsg();
        if (addMsg) {
            list.add(message);
        }
        // Byzantine verification
        int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
        if (list.size() >= byzantineMinPassCount) {
            // Debugging logs
            String signAddress = "";
            for (ComponentSignMessage msg : list) {
                for (HeterogeneousSign sign : msg.getListSign()) {
                    signAddress += sign.getHeterogeneousAddress().getChainId() + ":" + sign.getHeterogeneousAddress().getAddress() + " ";
                }
            }
            LoggerUtil.LOG.info("[Heterogeneous chain address signature message Byzantine passage] Current number of signatures:{}, Signature address:{}", list.size(), signAddress);
            return true;
        }
        LoggerUtil.LOG.info("[Heterogeneous chain address signature message - The number of Byzantine signatures has not yet reached], txhash: {}, The number of signatures that Byzantium needs to reach:{}, Current number of signatures:{}, ",
                message.getHash().toHex(), byzantineMinPassCount, list.size());
        return false;
    }

    String prepareHash = EMPTY_STRING;
    int count = 0;
    private void retryVirtualBankMessageProcess(Chain chain, String nodeId, Transaction tx, VirtualBankSignMessage _message, boolean isCreate) throws Exception {
        synchronized (objectBankLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            LoggerUtil.LOG.info("[Resend virtual bank signature change message - handlechangeVirtualBank], Received node[{}]  hash:{}, prepare: {}", nodeId, txHash, _message.getPrepare());
            ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, txHash);
            if (po != null) {
                LoggerUtil.LOG.info("[Completed - Resend virtual bank signature change message - handlechangeVirtualBank], Transaction Completed. TxHash: {}", txHash);
                return;
            }
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, txHash);
            // Preparation phase, resetcompSignPO
            if (_message.getPrepare() == 1) {
                // Determine if you have received the message
                if (compSignPO != null) {
                    if (!txHash.equals(prepareHash)) {
                        prepareHash = txHash;
                        count = 0;
                    }
                    count++;
                    if (count > 5) {
                        LoggerUtil.LOG.info("[End Reset - Resend virtual bank signature change message - Reset the status of this node], Received node[{}]  hash:{}", nodeId, txHash);
                        return;
                    }
                    componentSignStorageService.delete(chain, txHash);
                    NetWorkCall.broadcast(chain, _message, nodeId, ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
                    LoggerUtil.LOG.info("[Resend virtual bank signature change message - Reset the status of this node], Received node[{}]  hash:{}", nodeId, txHash);
                }
                return;
            } else if (_message.getPrepare() != 2) {
                LoggerUtil.LOG.error("[Resend virtual bank signature change message - Message type error], prepareID: {}, Received node[{}]  hash:{}", _message.getPrepare(), nodeId, txHash);
                return;
            }

            compSignPO = initCompSignPO(compSignPO, hash);
            ComponentSignMessage message = _message.toComponentSignMessage();
            boolean existMessage = isExistMessage(compSignPO, message);

            List<IHeterogeneousChainDocking> chainDockings = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
            if (null == chainDockings || chainDockings.isEmpty()) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            int htgChainSize = chainDockings.size();
            // validate
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            // The virtual bank with cache changes corresponds to the addresses on each chain
            Map<Integer, String[][]> addressCache = new HashMap<>();
            for (IHeterogeneousChainDocking docking : chainDockings) {
                int hChainId = docking.getChainId();
                // Assembly with added parameters
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                }

                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                }
                //if (converterCoreApi.checkChangeP35(txHash)) {
                //    inAddress = converterCoreApi.inChangeP35();
                //    outAddress = converterCoreApi.outChangeP35();
                //}
                addressCache.put(hChainId, new String[][]{inAddress, outAddress});
            }
            // Verify the correctness of the received message
            Map<Integer, IHeterogeneousChainDocking> chainDockingMap = chainDockings.stream().collect(Collectors.toMap(IHeterogeneousChainDocking::getChainId, Function.identity()));
            do {
                if (existMessage) {
                    break;
                }
                boolean verifySignManagerChangesII = true;
                for (HeterogeneousSign sign : message.getListSign()) {
                    String signAddress = sign.getHeterogeneousAddress().getAddress();
                    IHeterogeneousChainDocking docking = chainDockingMap.get(sign.getHeterogeneousAddress().getChainId());
                    if (docking == null) {
                        LoggerUtil.LOG.error("[Resend virtual bank signature change message-Heterogeneous chainIDerror], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                                nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // Verify message signature
                    Boolean msgPass = docking.verifySignManagerChangesII(
                            signAddress,
                            txHash,
                            inAddress,
                            outAddress,
                            1,
                            HexUtil.encode(sign.getSignature()));
                    if (!msgPass) {
                        verifySignManagerChangesII = false;
                        LoggerUtil.LOG.error("[Resend virtual bank signature change message-Signature verification failed-changeVirtualBank], Received node[{}], txhash: {}, Heterogeneous chainId:{}, Signature address:{}, autographhex:{}",
                                nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                }
                if (verifySignManagerChangesII) {
                    // Add the message received this time
                    compSignPO.getListMsg().add(message);
                    // Verification passed, relay the message
                    NetWorkCall.broadcast(chain, _message, nodeId, ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
                }
            } while (false);

            ComponentSignMessage currentMessage = null;
            /**
             * (Signature for change transaction,Unified business for multiple components)
             * Unified identification of multiple components, When a single component is completed, multiple components must be completed simultaneously.
             */
            boolean signed = false;
            boolean completed = false;
            boolean bztPass = false;
            // Current node signature
            do {
                if (compSignPO.getCurrentSigned()){
                    break;
                }
                // If the current node has not been signed, Then a signature is required, collect, broadcast.
                List<HeterogeneousSign> currentSignList = new ArrayList<>();
                // Check if the current node has been newly added No need to sign
                boolean currentJoin = false;
                SignAccountDTO packerInfo = ConsensusCall.getPackerInfo(chain);
                VirtualBankDirector director = chain.getMapVirtualBank().get(packerInfo.getAddress());
                List<byte[]> inAgents = txData.getInAgents();
                if (inAgents != null) {
                    for (int i = 0, size = inAgents.size(); i < size; i++) {
                        byte[] bytes = inAgents.get(i);
                        String agentAddress = AddressTool.getStringAddressByBytes(bytes);
                        if (agentAddress.equals(director.getAgentAddress())) {
                            currentJoin = true;
                        }
                    }
                }
                // Is the current node newly joined No need to sign
                if (currentJoin) {
                    break;
                }
                // Message signature
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    /**
                     * If the current node has not yet signed, trigger the collection of signatures from various heterogeneous chain components of the current node
                     * Due to the virtual banking change involving all heterogeneous chain components, So each component needs to be signed
                     * And join the broadcast in a message.
                     */
                    String signStrData = docking.signManagerChangesII(txHash, inAddress, outAddress, 1);
                    String currentHaddress = docking.getCurrentSignAddress();
                    if (StringUtils.isBlank(currentHaddress)) {
                        throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                    }
                    HeterogeneousSign currentSign = new HeterogeneousSign(
                            new HeterogeneousAddress(hChainId, currentHaddress),
                            HexUtil.decode(signStrData));
                    currentSignList.add(currentSign);
                    if (null == currentMessage) {
                        currentMessage = new ComponentSignMessage(message.getVirtualBankTotal(),
                                hash, currentSignList);
                    }
                    if (currentSignList.size() == htgChainSize) {
                        compSignPO.getListMsg().add(currentMessage);
                        signed = true;
                    }

                }
            } while (false);
            // Check if sufficient node signatures have been collected
            do {
                boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                if (!byzantinePass) {
                    // Not enough signatures reached
                    break;
                }
                if (compSignPO.getByzantinePass()) {
                    // This process has already been executed
                    break;
                }
                List<Map<Integer, HeterogeneousSign>> list = compSignPO.getListMsg().stream().map(m -> m.getListSign().stream().collect(Collectors.toMap(hSign -> hSign.getHeterogeneousAddress().getChainId(), Function.identity()))).collect(Collectors.toList());
                int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
                List<ComponentCallParm> callParmsList = new ArrayList<>();
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // Assembly with added parameters
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // Processing messages and Byzantine verification, updatecompSignPOetc.
                    if (isCreate) {
                        // Create heterogeneous chain transactions
                        StringBuilder signatureDataBuilder = new StringBuilder();
                        int count = 0;
                        for (Map<Integer, HeterogeneousSign> map : list) {
                            HeterogeneousSign heterogeneousSign = map.get(hChainId);
                            if (heterogeneousSign == null) {
                                LoggerUtil.LOG.warn("[Resend virtual bank signature change message-Incomplete signature information-changeVirtualBank], Received node[{}], txhash: {}, Heterogeneous chainId:{}", nodeId, txHash, hChainId);
                                continue;
                            }
                            signatureDataBuilder.append(HexUtil.encode(heterogeneousSign.getSignature()));
                            if (hChainId > 200) {
                                signatureDataBuilder.append(",");
                            }
                            count++;
                        }
                        if (hChainId > 200) {
                            signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
                        }
                        // Reaching the minimum number of signatures is the only valid call data
                        if (count >= byzantineMinPassCount) {
                            ComponentCallParm callParm = new ComponentCallParm(
                                    hChainId,
                                    tx.getType(),
                                    txHash,
                                    inAddress,
                                    outAddress,
                                    1,
                                    signatureDataBuilder.toString());
                            callParmsList.add(callParm);
                            chain.getLogger().info("[Resend virtual bank signature change message-Byzantine passage-changeVirtualBank] Storage heterogeneous chain components execute virtual bank change call parameters. hash:{}, callParm chainId:{}", txHash, callParm.getHeterogeneousId());
                        }
                    } else {
                        completed = true;
                    }
                }
                if (callParmsList.size() == htgChainSize) {
                    compSignPO.setCallParms(callParmsList);
                    bztPass = true;
                }
            } while (false);
            if (bztPass) {
                compSignPO.setByzantinePass(true);
            }
            if (completed) {
                compSignPO.setCompleted(true);
            }
            if (signed) {
                compSignPO.setCurrentSigned(true);
                // Broadcast current node signature message
                NetWorkCall.broadcast(chain, VirtualBankSignMessage.of(currentMessage, _message.getPrepare()), ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }

}
