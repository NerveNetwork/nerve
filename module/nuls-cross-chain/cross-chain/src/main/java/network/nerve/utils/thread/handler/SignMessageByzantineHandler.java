package network.nerve.utils.thread.handler;

import io.nuls.crosschain.base.message.BroadCtxSignMessage;
import network.nerve.model.bo.Chain;
import network.nerve.model.bo.message.UntreatedMessage;
import network.nerve.utils.MessageUtil;

/**
 * 链内节点广播过来的跨链交易签名拜占庭验证处理线程
 *
 * @author tag
 * 2019/8/8
 */

public class SignMessageByzantineHandler implements Runnable{
    private Chain chain;

    public SignMessageByzantineHandler(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (chain.getSignMessageByzantineQueue() != null) {
            try {
                UntreatedMessage untreatedMessage = chain.getSignMessageByzantineQueue().take();
                String nativeHex = untreatedMessage.getCacheHash().toHex();
                chain.getLogger().debug("开始对链内节点{}广播过来的跨链交易签名消息做签名拜占庭验证,Hash：{}", untreatedMessage.getNodeId(), nativeHex);
                MessageUtil.handleSignMessage(chain, untreatedMessage.getCacheHash(), untreatedMessage.getChainId(), untreatedMessage.getNodeId(),(BroadCtxSignMessage)untreatedMessage.getMessage(), nativeHex);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }
}
