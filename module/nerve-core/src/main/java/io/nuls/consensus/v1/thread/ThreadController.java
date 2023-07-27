package io.nuls.consensus.v1.thread;

import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.BasicRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Eva
 */
public class ThreadController extends BasicObject {

    /**
     * 专用线程池
     */
    private ThreadPoolExecutor threadPool;
    private List<BasicRunnable> list = new ArrayList<>();

    public ThreadController(Chain chain) {
        super(chain);
        init();
    }

    public void execute(BasicRunnable runnable) {
        list.add(runnable);
        threadPool.execute(runnable);
    }

    public void shutdown() {
        if (threadPool != null) {
            for (BasicRunnable runnable : list) {
                runnable.stop();
            }
            threadPool.shutdown();
            list.clear();
            this.init();
        }
    }

    private void init() {
        this.threadPool = ThreadUtils.createThreadPool(3, 100, new NulsThreadFactory("pocbft" + chain.getChainId()));
    }
}
