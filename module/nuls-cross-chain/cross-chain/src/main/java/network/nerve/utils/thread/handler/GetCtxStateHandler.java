package network.nerve.utils.thread.handler;
import network.nerve.model.bo.Chain;
import network.nerve.model.bo.message.UntreatedMessage;
import network.nerve.utils.TxUtil;

/**
 * 跨链查询跨链交易处理状态
 *
 * @author tag
 * 2019/6/25
 */
public class GetCtxStateHandler implements Runnable {
    private Chain chain;

    public GetCtxStateHandler(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (chain.getGetCtxStateQueue() != null) {
            try {
                UntreatedMessage untreatedMessage = chain.getGetCtxStateQueue().take();
                TxUtil.getCtxState(chain, untreatedMessage.getCacheHash());
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }
}
