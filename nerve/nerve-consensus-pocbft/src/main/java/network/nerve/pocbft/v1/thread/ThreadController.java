package network.nerve.pocbft.v1.thread;

import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.v1.entity.BasicObject;
import network.nerve.pocbft.v1.entity.BasicRunnable;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Eva
 */
public class ThreadController extends BasicObject {

    /**
     * 专用线程池
     */
    private ThreadPoolExecutor threadPool;

    public ThreadController(Chain chain) {
        super(chain);
        init();
    }

    public void execute(BasicRunnable runnable) {
        threadPool.execute(runnable);
    }

    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
            this.init();
        }
    }

    private void init() {
        this.threadPool = ThreadUtils.createThreadPool(3, 100, new NulsThreadFactory("pocbft" + chain.getChainId()));
    }
}
