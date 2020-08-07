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

package network.nerve.converter.core.business.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.MessageService;
import network.nerve.converter.core.validator.ProposalVerifier;
import network.nerve.converter.enums.ByzantineStateEnum;
import network.nerve.converter.message.BroadcastHashSignMessage;
import network.nerve.converter.message.GetTxMessage;
import network.nerve.converter.message.NewTxMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.PendingCheckTx;
import network.nerve.converter.model.bo.UntreatedMessage;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.ConverterSignUtil;
import network.nerve.converter.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void newHashSign(Chain chain, String nodeId, BroadcastHashSignMessage message) {
        int chainId = chain.getChainId();
        NulsHash hash = message.getHash();
        P2PHKSignature p2PHKSignature = message.getP2PHKSignature();
        if (null == hash || null == p2PHKSignature || null == p2PHKSignature.getPublicKey() || null == p2PHKSignature.getSignData()) {
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        LoggerUtil.LOG.debug("收到节点[{}]新交易签名 hash: {}, 签名地址:{}", nodeId, hash.toHex(),
                AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chainId)));
        TransactionPO txPO = txStorageService.get(chain, hash);
        if (null == txPO) {
            //如果为第一次收到该交易,暂存该消息并向广播过来的节点获取完整交易
            UntreatedMessage untreatedMessage = new UntreatedMessage(chainId, nodeId, message, hash);
            chain.getFutureMessageMap().putIfAbsent(hash, new ArrayList<>());
            chain.getFutureMessageMap().get(hash).add(untreatedMessage);
            LoggerUtil.LOG.info("当前节点还未确认该交易，缓存签名消息 hash:{}", hash.toHex());

            if(TxType.PROPOSAL == message.getType()){
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
            LoggerUtil.LOG.debug("交易在本节点已经处理完成,Hash:{}\n\n", hash.toHex());
            return;
        }
        try {
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
        if(null == txPO){
            chain.getLogger().error("当前节点不存在该交易,Hash:{}", nativeHex);
            return;
        }
        NewTxMessage newTxMessage = new NewTxMessage();
        newTxMessage.setTx(txPO.getTx());
        //把完整交易发送给请求节点
        if(!NetWorkCall.sendToNode(chain, newTxMessage, nodeId, ConverterCmdConstant.NEW_TX_MESSAGE)){
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
            LoggerUtil.LOG.debug("已收到并处理过该交易,Hash:{}\n\n", localHashHex);
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
        if(null != listMsg){
            for(UntreatedMessage msg : listMsg){
                chain.getSignMessageByzantineQueue().offer(msg);
            }
            // 清空缓存的签名
            chain.getFutureMessageMap().remove(localHash);
        }
        if(TxType.PROPOSAL == message.getTx().getType()){
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

}
