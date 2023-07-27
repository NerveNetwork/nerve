package io.nuls.consensus.v1.entity;

import io.nuls.core.log.logback.NulsLogger;
import io.nuls.consensus.model.bo.Chain;

/**
 * @author Eva
 */
public abstract class BasicObject {
    protected final Chain chain;
    protected final NulsLogger log;
    public BasicObject(Chain chain) {
        this.chain = chain;
        this.log = chain.getLogger();
    }
}
