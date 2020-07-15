package network.nerve.pocbft.v1.entity;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.pocbft.model.bo.Chain;

/**
 * @author Eva
 */
public abstract class BasicRunnable implements Runnable {
    protected final Chain chain;
    protected final NulsLogger log;

    protected boolean running = true;

    public BasicRunnable(Chain chain) {
        this.chain = chain;
        this.log = chain.getLogger();
    }

    public void stop() {
        this.running = false;
    }
}
