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
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.MessageService;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ComponentCalledPO;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import static network.nerve.converter.constant.ConverterCmdConstant.COMPONENT_SIGN;

/**
 * @author: Loki
 * @date: 2020/9/1
 */
@Component("ComponentSignHandlerV1")
public class ComponentSignHandler implements MessageProcessor {
    @Autowired
    private MessageService messageService;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService;

    @Override
    public String getCmd() {
        return COMPONENT_SIGN;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {

        Chain chain = chainManager.getChain(chainId);
        if (null == chain) {
            String msg = String.format("chain is null. chain id %s, msg:%s", chainId, COMPONENT_SIGN);
            chain.getLogger().error(msg, new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST));
            return;
        }

        boolean currentDirector = VirtualBankUtil.isCurrentDirector(chain);

        ComponentSignMessage componentSignMessage = RPCUtil.getInstanceRpcStr(message, ComponentSignMessage.class);
        if (null == componentSignMessage) {
            chain.getLogger().error("msg is null, msg:{}", COMPONENT_SIGN);
            return;
        }
        String hash = componentSignMessage.getHash().toHex();
        ComponentCalledPO po = asyncProcessedTxStorageService.getComponentCalledPO(chain, hash);
        if(null != po) {
            if (!po.getCommit()) {
                String currentOutHash = asyncProcessedTxStorageService.getCurrentOutHash(chain, hash);
                if (StringUtils.isNotBlank(currentOutHash)) {
                    ComponentCalledPO outPO = asyncProcessedTxStorageService.getComponentCalledPO(chain, currentOutHash);
                    if (null != outPO && po.getHeight() <= outPO.getHeight()) {
                        chain.getLogger().debug("当前非虚拟银行成员节点，参与签名, hash: {}", hash);
                        messageService.componentSign(chain, nodeId, componentSignMessage, false);
                    }
                }
            }
        }
        if(!currentDirector){
            chain.getLogger().debug("当前非虚拟银行成员节点, 不处理消息:{}", COMPONENT_SIGN);
            return;
        }
        messageService.componentSign(chain, nodeId, componentSignMessage, true);
    }
}
