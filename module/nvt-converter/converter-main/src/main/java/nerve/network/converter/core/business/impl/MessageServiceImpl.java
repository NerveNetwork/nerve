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

package nerve.network.converter.core.business.impl;

import nerve.network.converter.core.business.MessageService;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.message.BroadcastHashSignMessage;
import nerve.network.converter.message.GetTxMessage;
import nerve.network.converter.message.NewTxMessage;
import nerve.network.converter.storage.TxStorageService;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Chino
 * @date: 2020-02-27
 */
@Component
public class MessageServiceImpl implements MessageService {

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private TxStorageService txStorageService;

    @Override
    public void newHashSign(int chainId, String nodeId, BroadcastHashSignMessage message) {
       /* Chain chain = chainManager.getChainMap().get(chainId);
        if(null == chain){
            LOG.error(new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST));
        }
        NulsHash hash = message.getHash();
        P2PHKSignature p2PHKSignature = message.getP2PHKSignature();
        if(null == hash || null == p2PHKSignature){
            chain.getLogger().error(new NulsException(ConverterErrorCode.NULL_PARAMETER));
        }
        chain.getLogger().debug("收到新交易hash: {}, 签名地址:{}"
                , AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chainId)));
        //如果为第一次收到该交易,暂存该消息并向广播过来的节点获取完整跨链交易
        TransactionPO txPO = txStorageService.get(chain, hash);
        if(null == txPO){
            UntreatedMessage untreatedMessage = new UntreatedMessage(chainId,nodeId,message,hash);
            chain.getFutureMessageMap().putIfAbsent(hash, new ArrayList<>());
            chain.getFutureMessageMap().get(hash).add(untreatedMessage);
            chain.getLogger().info("当前节点还未确认该跨链交易，缓存签名消息");
            return;
        }
        //如果最新区块表中不存在该交易，则表示该交易已经被打包了，所以不需要再广播该交易的签名
        if (txPO.getStatus() != TxStatusEnum.UNCONFIRM.getStatus() || message.getP2PHKSignature() == null) {
            chain.getLogger().debug("跨链交易在本节点已经处理完成,Hash:{}\n\n", nativeHex);
            return;
        }
        try {
            UntreatedMessage untreatedSignMessage = new UntreatedMessage(chainId,nodeId,message,hash);
            chain.getSignMessageByzantineQueue().offer(untreatedSignMessage);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        */
    }

    @Override
    public void getTx(int chainId, String nodeId, GetTxMessage message) {

    }

    @Override
    public void newTx(int chainId, String nodeId, NewTxMessage message) {

    }
}
