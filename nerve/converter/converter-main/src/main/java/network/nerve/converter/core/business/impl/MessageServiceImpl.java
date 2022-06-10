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
import network.nerve.converter.model.po.ComponentCallParm;
import network.nerve.converter.model.po.ComponentSignByzantinePO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ComponentSignStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.storage.VirtualBankAllHistoryStorageService;
import network.nerve.converter.utils.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        // v15波场协议升级后，特殊处理hash，历史遗留问题，polygon区块回滚导致的交易高度和时间不一致的问题
        if (converterCoreApi.isSupportProtocol15TrxCrossChain() && "e7650127c55c7fa90e8cfded861b9aba0a71e025c318f0e31d53721d864d1e26".equalsIgnoreCase(hash.toHex())) {
            LoggerUtil.LOG.warn("过滤交易hash: {}", hash.toHex());
            return;
        }
        LoggerUtil.LOG.info("收到节点[{}]新交易签名 hash: {}, 签名地址:{}", nodeId, hash.toHex(),
                AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chainId)));
        TransactionPO txPO = txStorageService.get(chain, hash);
        if (null == txPO) {
            try {
                //如果为第一次收到该交易,暂存该消息并向广播过来的节点获取完整交易
                UntreatedMessage untreatedMessage = new UntreatedMessage(chainId, nodeId, message, hash);
                List<UntreatedMessage> untreatedMsgList = chain.getFutureMessageMap().computeIfAbsent(hash, v -> new ArrayList<>());
                untreatedMsgList.add(untreatedMessage);
                LoggerUtil.LOG.info("当前节点还未确认该交易，缓存签名消息 hash:{}", hash.toHex());
            } catch (Exception e) {
                LoggerUtil.LOG.error(e);
            }

            if (TxType.PROPOSAL == message.getType()) {
                // 如果是提案交易, 则需要主动向发送索要交易,非交易发起节点 无法创建该交易
                GetTxMessage msg = new GetTxMessage(hash);
                NetWorkCall.broadcast(chain, msg, null, ConverterCmdConstant.GET_TX_MESSAGE);
                return;
            }
            /**
             * 特殊处理 将交易hash放入 task 一定时间后去获取交易信息,
             * 防止刚加入虚拟银行的节点 异构链同步机制的延迟导致不能创建对应的交易
             */
            PendingCheckTx pendingCheckTx = new PendingCheckTx(message);
            chain.getPendingCheckTxSet().add(pendingCheckTx);
            return;
        }
        //如果该交易已经被打包了，所以不需要再广播该交易的签名
        if (txPO.getStatus() != ByzantineStateEnum.UNTREATED.getStatus() || message.getP2PHKSignature() == null) {
            LoggerUtil.LOG.info("交易在本节点已经处理完成,Hash:{}\n\n", hash.toHex());
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
        LoggerUtil.LOG.info("节点:{},向本节点获取完整的交易，Hash:{}", nodeId, nativeHex);
        TransactionPO txPO = txStorageService.get(chain, hash);
        if (null == txPO) {
            chain.getLogger().error("当前节点不存在该交易,Hash:{}", nativeHex);
            return;
        }
        NewTxMessage newTxMessage = new NewTxMessage();
        newTxMessage.setTx(txPO.getTx());
        //把完整交易发送给请求节点
        if (!NetWorkCall.sendToNode(chain, newTxMessage, nodeId, ConverterCmdConstant.NEW_TX_MESSAGE)) {
            LoggerUtil.LOG.info("发送完整的交易到节点:{}, 失败! Hash:{}\n\n", nodeId, nativeHex);
            return;
        }
        LoggerUtil.LOG.info("将完整的交易发送给链内节点:{}, Hash:{}\n\n", nodeId, nativeHex);
    }

    @Override
    public void receiveTx(Chain chain, String nodeId, NewTxMessage message) {
        Transaction tx = message.getTx();
        NulsHash localHash = tx.getHash();
        String localHashHex = localHash.toHex();
        LoggerUtil.LOG.info("收到链内节点:{}发送过来的完整交易,Hash:{}", nodeId, localHashHex);
        //判断本节点是否已经收到过该交易，如果已收到过直接忽略
        TransactionPO txPO = txStorageService.get(chain, localHash);
        if (txPO != null) {
            LoggerUtil.LOG.info("已收到并处理过该交易,Hash:{}\n\n", localHashHex);
            return;
        }
        // 验证该交易
        try {
            proposalVerifier.validate(chain, tx);
        } catch (NulsException e) {
            chain.getLogger().error("收到完整交易, 验证失败. hash:{}", localHashHex);
            chain.getLogger().error(e);
            return;
        }
        txStorageService.save(chain, new TransactionPO(message.getTx()));
        List<UntreatedMessage> listMsg = chain.getFutureMessageMap().get(localHash);
        if (null != listMsg) {
            for (UntreatedMessage msg : listMsg) {
                chain.getSignMessageByzantineQueue().offer(msg);
            }
            // 清空缓存的签名
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
            LoggerUtil.LOG.debug("[newTx-message] 广播本节点对提案的签名 txhash:{}, 签名地址:{}",
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
            // 已经处理过
            return;
        }
        IDepositTxSubmitter submitter = heterogeneousCallBackManager.createOrGetDepositTxSubmitter(chain.getChainId(), heterogeneousChainId);
        Result result = submitter.validateDepositTx(heterogeneousTxHash);
        if (result.isFailed()) {
            chain.getLogger().error("重新解析异构交易, validateDepositTx 验证失败, {}", result.getErrorCode().getCode());
            return;
        }
        try {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            boolean rs = docking.reAnalysisDepositTx(heterogeneousTxHash);
            if (rs) {
                txStorageService.saveHeterogeneousHash(chain, heterogeneousTxHash);
                NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.CHECK_RETRY_PARSE_MESSAGE);
                chain.getLogger().info("[checkRetryParse 消息处理完成] 异构chainId: {}, 异构hash:{}", heterogeneousChainId, heterogeneousTxHash);
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
                chain.getLogger().info("[cancelHtgTx 消息处理完成] 异构chainId: {}, 异构address:{}, 取消操作的hash:{}", heterogeneousChainId, address, hash);
            } else {
                NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
                chain.getLogger().info("[cancelHtgTx 消息转发完成] 异构chainId: {}, 异构address:{}", heterogeneousChainId, address);
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
        // 收到过该消息
        return existMsg;
    }

    private ComponentSignByzantinePO initCompSignPO(ComponentSignByzantinePO compSignPO, NulsHash hash) {
        // 初始化存储签名的对象
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
        // 获取交易
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[异构链地址签名消息-未查询到该交易], 异构链地址签名消息, 收到节点[{}] txhash:{}, 异构链Id:{}, 签名地址:{}",
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
                            // 判断提案类型是合约升级, 广播的是对应的提案交易hash 和对该hash的签名
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
     * 协议v21 支持一键跨链
     */
    private void componentSignV21(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate) {
        NulsHash hash = message.getHash();
        List<HeterogeneousSign> listSign = message.getListSign();
        if (null == hash || null == listSign || listSign.size() == 0) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        // 获取交易
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[异构链地址签名消息-未查询到该交易], 异构链地址签名消息, 收到节点[{}] txhash:{}, 异构链Id:{}, 签名地址:{}",
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
                            // 判断提案类型是合约升级, 广播的是对应的提案交易hash 和对该hash的签名
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
        // 获取交易
        try {
            Transaction tx = TransactionCall.getConfirmedTx(chain, hash);
            if (null == tx) {
                String ids = "";
                String signAddress = "";
                for (HeterogeneousSign sign : listSign) {
                    ids += sign.getHeterogeneousAddress().getChainId() + " ";
                    signAddress += sign.getHeterogeneousAddress().getAddress() + " ";
                }
                LoggerUtil.LOG.error("[重发虚拟银行变更签名消息-未查询到该交易], 异构链地址签名消息, 收到节点[{}] txhash:{}, 异构链Id:{}, 签名地址:{}",
                        nodeId, hash.toHex(), ids, signAddress);
                return;
            }
            if (tx.getType() != TxType.CHANGE_VIRTUAL_BANK) {
                LoggerUtil.LOG.error("[重发虚拟银行变更签名消息-交易类型错误]-{}", tx.getType());
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
     * 处理虚拟银行变更交易的消息
     * 1.验证签名
     * 2.处理消息, 存储, 转发,
     * 3.计算拜占庭, 如果拜占庭通过则调异构链组件
     *
     * @param chain
     * @param tx
     * @param message
     * @throws NulsException
     */
    private void changeVirtualBankMessageProcess(Chain chain, String nodeId, Transaction tx, ComponentSignMessage message, boolean isCreate) throws NulsException {
        synchronized (objectBankLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            // 判断是否收到过该消息
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            boolean existMessage = isExistMessage(compSignPO, message);
            //if (isExistMessage(compSignPO, message)) {
            //    return;
            //}
            compSignPO = initCompSignPO(compSignPO, hash);
            LoggerUtil.LOG.info("[异构链地址签名消息 - 处理changeVirtualBank], 收到节点[{}]  hash:{}", nodeId, txHash);
            List<IHeterogeneousChainDocking> chainDockings = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
            if (null == chainDockings || chainDockings.isEmpty()) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            int htgChainSize = chainDockings.size();
            // 验证
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            // 缓存变更的虚拟银行在各条链对应的地址
            Map<Integer, String[][]> addressCache = new HashMap<>();
            for (IHeterogeneousChainDocking docking : chainDockings) {
                int hChainId = docking.getChainId();
                // 组装加入参数
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                }

                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                }
                addressCache.put(hChainId, new String[][]{inAddress, outAddress});
            }
            // 验证收到的消息的正确性
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
                        LoggerUtil.LOG.error("[虚拟银行变更签名消息-异构链ID错误], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                                nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // 验证消息签名
                    Boolean msgPass = docking.verifySignManagerChangesII(
                            signAddress,
                            txHash,
                            inAddress,
                            outAddress,
                            1,
                            HexUtil.encode(sign.getSignature()));
                    if (!msgPass) {
                        verifySignManagerChangesII = false;
                        LoggerUtil.LOG.error("[虚拟银行变更签名消息-签名验证失败-changeVirtualBank], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                                nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                }
                if (verifySignManagerChangesII) {
                    // 添加本次收到的消息
                    compSignPO.getListMsg().add(message);
                    // 验证通过, 转发消息
                    NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
                }
            } while (false);
            ComponentSignMessage currentMessage = null;
            /**
             * (签名针对变更交易,为多组件统一业务)
             * 多组件统一标识, 单个组件完成必然多个组件同时完成.
             */
            boolean signed = false;
            boolean completed = false;
            boolean bztPass = false;
            // 当前节点签名
            do {
                if (compSignPO.getCurrentSigned()){
                    break;
                }
                // 如果当前节点没有签过名, 则需要进行签名, 收集, 广播.
                List<HeterogeneousSign> currentSignList = new ArrayList<>();
                // 检查当前节点是否新加入的 则不用签名
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
                // 当前节点是否新加入的 则不用签名
                if (currentJoin) {
                    break;
                }
                // 消息签名
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    /**
                     * 如果当前节点还没签名则触发收集当前节点各个异构链组件签名
                     * 由于虚拟银行变更涉及到所有异构链组件, 所以每个组件都要签名
                     * 并且加入到一个消息中广播.
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
            // 检查是否收集到足够的节点签名
            do {
                boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                if (!byzantinePass) {
                    // 未达到足够的签名
                    break;
                }
                if (compSignPO.getByzantinePass()) {
                    // 已执行过此流程
                    break;
                }
                List<Map<Integer, HeterogeneousSign>> list = compSignPO.getListMsg().stream().map(m -> m.getListSign().stream().collect(Collectors.toMap(hSign -> hSign.getHeterogeneousAddress().getChainId(), Function.identity()))).collect(Collectors.toList());
                int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
                List<ComponentCallParm> callParmsList = new ArrayList<>();
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // 处理消息并拜占庭验证, 更新compSignPO等
                    if (isCreate) {
                        // 创建异构链交易
                        StringBuilder signatureDataBuilder = new StringBuilder();
                        int count = 0;
                        for (Map<Integer, HeterogeneousSign> map : list) {
                            HeterogeneousSign heterogeneousSign = map.get(hChainId);
                            if (heterogeneousSign == null) {
                                LoggerUtil.LOG.warn("[虚拟银行变更签名消息-签名信息不完整-changeVirtualBank], 收到节点[{}], txhash: {}, 异构链Id:{}", nodeId, txHash, hChainId);
                                continue;
                            }
                            signatureDataBuilder.append(HexUtil.encode(heterogeneousSign.getSignature()));
                            count++;
                        }
                        // 达到最小签名数才是有效的调用数据
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
                            chain.getLogger().info("[虚拟银行变更签名消息-拜占庭通过-changeVirtualBank] 存储异构链组件执行虚拟银行变更调用参数. hash:{}, callParm chainId:{}", txHash, callParm.getHeterogeneousId());
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
                // 广播当前节点签名消息
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);

            /*List<HeterogeneousSign> currentSignList = null;
            if (!compSignPO.getCurrentSigned()) {
                // 如果当前节点没有签过名, 则需要进行签名, 收集, 广播.
                currentSignList = new ArrayList<>();
            }
            ComponentSignMessage currentMessage = null;

            *//**
             * (签名针对变更交易,为多组件统一业务)
             * 多组件统一标识, 单个组件完成必然多个组件同时完成.
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
                        // 组装加入参数
                        String[] inAddress = new String[inSize];
                        if (null != txData.getInAgents()) {
                            getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                        }

                        String[] outAddress = new String[outSize];
                        if (null != txData.getOutAgents()) {
                            getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                        }
                        // 验证消息签名
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hInterface.getChainId());
                        Boolean msgPass = docking.verifySignManagerChangesII(
                                signAddress,
                                txHash,
                                inAddress,
                                outAddress,
                                1,
                                HexUtil.encode(sign.getSignature()));
                        if (!msgPass) {
                            LoggerUtil.LOG.error("[异构链地址签名消息-签名验证失败-changeVirtualBank], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                                    nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                            continue;
                        }
                        // 验证通过, 转发消息
                        NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
                        if (!addedMsg) {
                            //先添加本次收到的消息
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
                            // 当前节点是新加入的 则不用签名
                            if (!currentJoin) {
                                *//**
                                 * 如果当前节点还没签名则触发收集当前节点各个异构链组件签名
                                 * 由于虚拟银行变更涉及到所有异构链组件, 所以每个组件都要签名
                                 * 并且加入到一个消息中广播.
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
                        // 处理消息并拜占庭验证, 更新compSignPO等
                        boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                        if (byzantinePass && !compSignPO.getByzantinePass()) {
                            if (isCreate) {
                                // 创建异构链交易
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
                                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-changeVirtualBank] 存储异构链组件执行虚拟银行变更调用参数. hash:{}", txHash);
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
                // 广播当前节点签名消息
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);*/
        }
    }

    /**
     * 获取异构链地址
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
            HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
            if (null == heterogeneousAddress) {
                chain.getLogger().warn("异构链地址签名消息[changeVirtualBank] 没有获取到异构链地址, agentAddress:{}", agentAddress);
                chain.getLogger().warn("(可忽略)获取不到异构链地址: 如果当前节点处理虚拟银行变更交易稍慢(还没有执行完commit, 导致获取不到异构链地址)");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            address[i] = heterogeneousAddress.getAddress();
        }
    }

    /**
     * 处理提现交易的消息
     * 1.验证签名
     * 2.处理消息, 存储, 转发,
     * 3.计算拜占庭, 如果拜占庭通过则调异构链组件
     *
     * @param chain
     * @param tx
     * @param message
     * @throws NulsException
     */
    private void withdrawMessageProcess(Chain chain, String nodeId, Transaction tx, ComponentSignMessage message) throws NulsException {
        synchronized (objectWithdrawLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            // 判断是否收到过该消息
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            int signAddressChainId = sign.getHeterogeneousAddress().getChainId();
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            LoggerUtil.LOG.debug("[异构链地址签名消息-处理withdraw], 收到节点[{}]  hash: {}, 签名地址:{}-{}",
                    nodeId, txHash, signAddressChainId, signAddress);
            // 验证
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
                chain.getLogger().error("[异构链地址签名消息-withdraw] no withdrawCoinTo. hash:{}", tx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
            int heterogeneousChainId = assetInfo.getChainId();
            BigInteger amount = withdrawCoinTo.getAmount();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // 根据交易验证签名正确性
            Boolean msgPass = docking.verifySignWithdrawII(
                    signAddress,
                    txHash,
                    toAddress,
                    amount,
                    assetInfo.getAssetId(),
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[异构链地址签名消息 - 签名验证失败-withdraw], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // 验证通过, 转发消息
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);

            // 处理消息并拜占庭验证, 更新compSignPO等
            if (!compSignPO.getCurrentSigned()) {
                LoggerUtil.LOG.debug("[异构链地址签名消息 执行当前节点签名] txHash:{}", txHash);
                // 如果当前节点还没签名则触发当前节点签名,存储 并广播
                String signStrData = docking.signWithdrawII(txHash, toAddress, amount, assetInfo.getAssetId());
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // 创建异构链交易
                StringBuilder signatureDataBuilder = new StringBuilder();
                // 拼接所有签名
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
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-withdraw] 存储异构链组件执行提现调用参数. hash:{}", txHash);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }


    private void broadcastCurrentSign(Chain chain, NulsHash hash, ComponentSignByzantinePO compSignPO, String signStrData, HeterogeneousAddress heterogeneousAddress, int virtualBankTotal) {
        HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
        List<HeterogeneousSign> listSign = new ArrayList<>();
        listSign.add(currentSign);
        ComponentSignMessage currentMessage = new ComponentSignMessage(virtualBankTotal,
                hash, listSign);
        compSignPO.getListMsg().add(currentMessage);
        compSignPO.setCurrentSigned(true);
        // 广播当前节点签名消息
        NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
    }


    /**
     * 提案原路退回 version2
     *
     * @param chain
     * @param nodeId
     * @param proposalPO
     * @param message
     * @throws NulsException
     */
    private void refundMessageProcess(Chain chain, String nodeId, Transaction tx, ProposalPO proposalPO, ComponentSignMessage message) throws NulsException {
        synchronized (objectRefundLock) {
            NulsHash hash = proposalPO.getHash();
            String txHash = hash.toHex();
            // 判断是否收到过该消息
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, txHash);
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
            LoggerUtil.LOG.info("[异构链地址签名消息-处理refund], 收到节点[{}]  hash: {}, 签名地址:{}-{}",
                    nodeId, txHash, heterogeneousChainId, signAddress);

            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                    heterogeneousChainId,
                    proposalPO.getHeterogeneousTxHash(),
                    HeterogeneousTxTypeEnum.DEPOSIT,
                    this.heterogeneousDockingManager,
                    docking);
            if (null == info) {
                chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            // 如果当前节点还没签名则触发当前节点签名,存储 并广播
            Boolean msgPass = docking.verifySignWithdrawII(
                    signAddress,
                    txHash,
                    info.getFrom(),
                    info.getValue(),
                    info.getAssetId(),
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[异构链地址签名消息 - 签名验证失败-refund], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // 验证通过, 转发消息
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);

            // 处理消息并拜占庭验证, 更新compSignPO等
            if (!compSignPO.getCurrentSigned()) {
                LoggerUtil.LOG.info("[异构链地址签名消息 执行当前节点签名] txHash:{}", txHash);
                // 如果当前节点还没签名则触发当前节点签名,存储 并广播
                String signStrData = docking.signWithdrawII(txHash, info.getFrom(), info.getValue(), info.getAssetId());
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // 创建异构链交易
                StringBuilder signatureDataBuilder = new StringBuilder();
                // 拼接所有签名
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
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-refund] 存储异构链组件执行原路退回调用参数. hash:{}", txHash);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);

        }
    }


    /**
     * 处理异构链合约升级交易的消息
     * 1.验证签名
     * 2.处理消息, 存储, 转发,
     * 3.计算拜占庭, 如果拜占庭通过则调异构链组件
     *
     * @param chain
     * @param proposalPO
     * @param message
     * @throws NulsException
     */
    private void upgradeMessageProcess(Chain chain, String nodeId, Transaction tx, ProposalPO proposalPO, ComponentSignMessage message) throws NulsException {
        synchronized (objectUpgradeLock) {
            NulsHash hash = proposalPO.getHash();
            String txHash = hash.toHex();
            // 判断是否收到过该消息
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            if (isExistMessage(compSignPO, message)) {
                return;
            }
            compSignPO = initCompSignPO(compSignPO, hash);
            HeterogeneousSign sign = message.getListSign().get(0);
            String signAddress = sign.getHeterogeneousAddress().getAddress();
            // 验证消息签名
            int heterogeneousChainId = proposalPO.getHeterogeneousChainId();
            LoggerUtil.LOG.info("[异构链地址签名消息-处理withdraw], 收到节点[{}]  hash: {}, 签名地址:{}-{}",
                    nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress);
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // 新合约多签地址
            // 兼容非以太系地址 update by pierre at 2021/11/16
            String newMultySignAddress = docking.getAddressString(proposalPO.getAddress());
            Boolean msgPass = docking.verifySignUpgradeII(
                    signAddress,
                    txHash,
                    newMultySignAddress,
                    HexUtil.encode(sign.getSignature()));
            if (!msgPass) {
                LoggerUtil.LOG.error("[异构链地址签名消息 - 签名验证失败-upgrade], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                        nodeId, txHash, heterogeneousChainId, signAddress, HexUtil.encode(sign.getSignature()));
                return;
            }
            // 验证通过, 转发消息
            NetWorkCall.broadcast(chain, message, nodeId, ConverterCmdConstant.COMPONENT_SIGN);
            // 处理消息并拜占庭验证, 更新compSignPO等
            if (!compSignPO.getCurrentSigned()) {
                // 如果当前节点还没签名则触发当前节点签名,存储 并广播
                String signStrData = docking.signUpgradeII(txHash, newMultySignAddress);
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                broadcastCurrentSign(chain, hash, compSignPO, signStrData, new HeterogeneousAddress(heterogeneousChainId, currentHaddress), message.getVirtualBankTotal());
            }
            boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO);
            if (byzantinePass && !compSignPO.getByzantinePass()) {
                // 创建异构链交易
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
                // 执行提案后的业务处理
                this.proposalStorageService.saveExeBusiness(chain, proposalPO.getHash().toHex(), proposalPO.getHash());
                proposalPO.setHeterogeneousMultySignAddress(docking.getCurrentMultySignAddress());
                chain.getLogger().info("[异构链地址签名消息-拜占庭通过-upgrade] 存储调用异构链组件执行合约升级调用参数. hash:{}", txHash);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }

    private boolean processComponentSignMsgByzantine(Chain chain, ComponentSignMessage message, ComponentSignByzantinePO compSignPO) {
        return processComponentSignMsgByzantine(chain, message, compSignPO, true);
    }

    private boolean processComponentSignMsgByzantine(Chain chain, ComponentSignMessage message, ComponentSignByzantinePO compSignPO, boolean addMsg) {
        // 添加当前签名消息
        List<ComponentSignMessage> list = compSignPO.getListMsg();
        if (addMsg) {
            list.add(message);
        }
        // 拜占庭验证
        int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
        if (list.size() >= byzantineMinPassCount) {
            // 调试日志
            String signAddress = "";
            for (ComponentSignMessage msg : list) {
                for (HeterogeneousSign sign : msg.getListSign()) {
                    signAddress += sign.getHeterogeneousAddress().getChainId() + ":" + sign.getHeterogeneousAddress().getAddress() + " ";
                }
            }
            LoggerUtil.LOG.info("[异构链地址签名消息 拜占庭通过] 当前签名数:{}, 签名地址:{}", list.size(), signAddress);
            return true;
        }
        LoggerUtil.LOG.info("[异构链地址签名消息 - 暂时未达到拜占庭签名数], txhash: {}, 拜占庭需达到的签名数:{}, 当前签名数:{}, ",
                message.getHash().toHex(), byzantineMinPassCount, list.size());
        return false;
    }

    private void retryVirtualBankMessageProcess(Chain chain, String nodeId, Transaction tx, VirtualBankSignMessage _message, boolean isCreate) throws NulsException {
        synchronized (objectBankLock) {
            NulsHash hash = tx.getHash();
            String txHash = hash.toHex();
            LoggerUtil.LOG.info("[重发虚拟银行变更签名消息 - 处理changeVirtualBank], 收到节点[{}]  hash:{}", nodeId, txHash);
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, hash.toHex());
            // 准备阶段，重置compSignPO
            if (_message.getPrepare() == 1) {
                // 判断是否收到过该消息
                if (compSignPO != null) {
                    componentSignStorageService.delete(chain, hash.toHex());
                    NetWorkCall.broadcast(chain, _message, nodeId, ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
                    LoggerUtil.LOG.info("[重发虚拟银行变更签名消息 - 重置本节点状态], 收到节点[{}]  hash:{}", nodeId, txHash);
                }
                return;
            } else if (_message.getPrepare() != 2) {
                LoggerUtil.LOG.error("[重发虚拟银行变更签名消息 - 消息类型错误], prepareID: {}, 收到节点[{}]  hash:{}", _message.getPrepare(), nodeId, txHash);
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
            // 验证
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            // 缓存变更的虚拟银行在各条链对应的地址
            Map<Integer, String[][]> addressCache = new HashMap<>();
            for (IHeterogeneousChainDocking docking : chainDockings) {
                int hChainId = docking.getChainId();
                // 组装加入参数
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents());
                }

                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents());
                }
                addressCache.put(hChainId, new String[][]{inAddress, outAddress});
            }
            // 验证收到的消息的正确性
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
                        LoggerUtil.LOG.error("[重发虚拟银行变更签名消息-异构链ID错误], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                                nodeId, txHash, sign.getHeterogeneousAddress().getChainId(), signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // 验证消息签名
                    Boolean msgPass = docking.verifySignManagerChangesII(
                            signAddress,
                            txHash,
                            inAddress,
                            outAddress,
                            1,
                            HexUtil.encode(sign.getSignature()));
                    if (!msgPass) {
                        verifySignManagerChangesII = false;
                        LoggerUtil.LOG.error("[重发虚拟银行变更签名消息-签名验证失败-changeVirtualBank], 收到节点[{}], txhash: {}, 异构链Id:{}, 签名地址:{}, 签名hex:{}",
                                nodeId, txHash, hChainId, signAddress, HexUtil.encode(sign.getSignature()));
                        break;
                    }
                }
                if (verifySignManagerChangesII) {
                    // 添加本次收到的消息
                    compSignPO.getListMsg().add(message);
                    // 验证通过, 转发消息
                    NetWorkCall.broadcast(chain, _message, nodeId, ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
                }
            } while (false);

            ComponentSignMessage currentMessage = null;
            /**
             * (签名针对变更交易,为多组件统一业务)
             * 多组件统一标识, 单个组件完成必然多个组件同时完成.
             */
            boolean signed = false;
            boolean completed = false;
            boolean bztPass = false;
            // 当前节点签名
            do {
                if (compSignPO.getCurrentSigned()){
                    break;
                }
                // 如果当前节点没有签过名, 则需要进行签名, 收集, 广播.
                List<HeterogeneousSign> currentSignList = new ArrayList<>();
                // 检查当前节点是否新加入的 则不用签名
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
                // 当前节点是否新加入的 则不用签名
                if (currentJoin) {
                    break;
                }
                // 消息签名
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    /**
                     * 如果当前节点还没签名则触发收集当前节点各个异构链组件签名
                     * 由于虚拟银行变更涉及到所有异构链组件, 所以每个组件都要签名
                     * 并且加入到一个消息中广播.
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
            // 检查是否收集到足够的节点签名
            do {
                boolean byzantinePass = processComponentSignMsgByzantine(chain, message, compSignPO, false);
                if (!byzantinePass) {
                    // 未达到足够的签名
                    break;
                }
                if (compSignPO.getByzantinePass()) {
                    // 已执行过此流程
                    break;
                }
                List<Map<Integer, HeterogeneousSign>> list = compSignPO.getListMsg().stream().map(m -> m.getListSign().stream().collect(Collectors.toMap(hSign -> hSign.getHeterogeneousAddress().getChainId(), Function.identity()))).collect(Collectors.toList());
                int byzantineMinPassCount = VirtualBankUtil.getByzantineCount(chain, message.getVirtualBankTotal());
                List<ComponentCallParm> callParmsList = new ArrayList<>();
                for (IHeterogeneousChainDocking docking : chainDockings) {
                    int hChainId = docking.getChainId();
                    // 组装加入参数
                    String[][] addressData = addressCache.get(hChainId);
                    String[] inAddress = addressData[0];
                    String[] outAddress = addressData[1];
                    // 处理消息并拜占庭验证, 更新compSignPO等
                    if (isCreate) {
                        // 创建异构链交易
                        StringBuilder signatureDataBuilder = new StringBuilder();
                        int count = 0;
                        for (Map<Integer, HeterogeneousSign> map : list) {
                            HeterogeneousSign heterogeneousSign = map.get(hChainId);
                            if (heterogeneousSign == null) {
                                LoggerUtil.LOG.warn("[重发虚拟银行变更签名消息-签名信息不完整-changeVirtualBank], 收到节点[{}], txhash: {}, 异构链Id:{}", nodeId, txHash, hChainId);
                                continue;
                            }
                            signatureDataBuilder.append(HexUtil.encode(heterogeneousSign.getSignature()));
                            count++;
                        }
                        // 达到最小签名数才是有效的调用数据
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
                            chain.getLogger().info("[重发虚拟银行变更签名消息-拜占庭通过-changeVirtualBank] 存储异构链组件执行虚拟银行变更调用参数. hash:{}, callParm chainId:{}", txHash, callParm.getHeterogeneousId());
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
                // 广播当前节点签名消息
                NetWorkCall.broadcast(chain, VirtualBankSignMessage.of(currentMessage, _message.getPrepare()), ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
            }
            // 存储更新后的 compSignPO
            componentSignStorageService.save(chain, compSignPO);
        }
    }

}
