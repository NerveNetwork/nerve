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

package network.nerve.converter.message.handler.v1;

import io.nuls.base.RPCUtil;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.MessageService;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.message.NewTxMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.utils.LoggerUtil;
import network.nerve.converter.utils.VirtualBankUtil;

/**
 * @author: Loki
 * @date: 2020-02-27
 */
@Component("NewTxHandlerV1")
public class NewTxHandler implements MessageProcessor {

    @Autowired
    private MessageService messageService;
    @Autowired
    private ChainManager chainManager;

    @Override
    public String getCmd() {
        return ConverterCmdConstant.NEW_TX_MESSAGE;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {
        Chain chain = chainManager.getChain(chainId);
        if (null == chain) {
            String msg = String.format("chain is null. chain id %s, msg:%s", chainId, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
            LoggerUtil.LOG.error(msg, new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST));
            return;
        }
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            LoggerUtil.LOG.debug("当前非虚拟银行成员节点, 不处理消息:{}", ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
            return;
        }
        NewTxMessage newTxMessage = RPCUtil.getInstanceRpcStr(message, NewTxMessage.class);
        if(null == newTxMessage){
            chain.getLogger().error("msg is null, msg:{}", ConverterCmdConstant.NEW_TX_MESSAGE);
            return;
        }
        messageService.receiveTx(chain, nodeId, newTxMessage);
    }
}
