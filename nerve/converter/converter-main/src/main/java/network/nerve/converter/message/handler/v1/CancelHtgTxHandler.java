/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
import network.nerve.converter.message.CancelHtgTxMessage;
import network.nerve.converter.message.CheckRetryParseMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.utils.LoggerUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.converter.constant.ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE;
import static network.nerve.converter.constant.ConverterCmdConstant.CHECK_RETRY_PARSE_MESSAGE;
import static network.nerve.converter.constant.ConverterConstant.MINUTES_5;

/**
 * @author: Mimi
 * @date: 2021/5/30
 */
@Component("CancelHtgTxHandlerV1")
public class CancelHtgTxHandler implements MessageProcessor {

    @Autowired
    private MessageService messageService;
    @Autowired
    private ChainManager chainManager;
    private Map<String, Long> timeOutCache = new ConcurrentHashMap<>();

    @Override
    public String getCmd() {
        return CANCEL_HTG_TX_MESSAGE;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {
        Chain chain = chainManager.getChain(chainId);
        if (null == chain) {
            String msg = String.format("chain is null. chain id %s, msg:%s", chainId, ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
            LoggerUtil.LOG.error(msg, new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST));
            return;
        }
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            LoggerUtil.LOG.debug("当前非虚拟银行成员节点, 不处理消息:{}", ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
            return;
        }
        CancelHtgTxMessage cancelHtgTxMessage = RPCUtil.getInstanceRpcStr(message, CancelHtgTxMessage.class);
        if (null == cancelHtgTxMessage) {
            chain.getLogger().error("msg is null, msg:{}", ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE);
            return;
        }
        // 增加一个接收消息次数限制，限制时长5分钟
        if (hadReceived(cancelHtgTxMessage)) {
            chain.getLogger().info("msg had received, msg symbol:{}, msg:{}", ConverterCmdConstant.CANCEL_HTG_TX_MESSAGE, cancelHtgTxMessage.toKey());
            return;
        }
        messageService.cancelHtgTx(chain, nodeId, cancelHtgTxMessage);
    }

    private synchronized boolean hadReceived(CancelHtgTxMessage cancelHtgTxMessage) {
        String key = cancelHtgTxMessage.toKey();
        Long now = System.currentTimeMillis();
        Long time = timeOutCache.get(key);
        if (time != null && (now - time <= MINUTES_5)) {
            return true;
        }
        timeOutCache.put(key, now);
        return false;
    }
}
