package io.nuls.crosschain.utils.thread.handler;

import io.nuls.crosschain.base.message.BroadCtxSignMessage;
import io.nuls.crosschain.model.bo.message.UntreatedMessage;
import io.nuls.crosschain.utils.MessageUtil;
import io.nuls.crosschain.model.bo.Chain;

/**
 * Byzantine verification processing thread for cross chain transaction signatures broadcasted by intra chain nodes
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
//                chain.getLogger().info("Start monitoring nodes within the chain{}Cross chain transaction signature message broadcasted for signature Byzantine verification,Hash：{}", untreatedMessage.getNodeId(), nativeHex);
                MessageUtil.handleSignMessage(chain, untreatedMessage.getCacheHash(), untreatedMessage.getChainId(), untreatedMessage.getNodeId(),(BroadCtxSignMessage)untreatedMessage.getMessage(), nativeHex);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }
}
