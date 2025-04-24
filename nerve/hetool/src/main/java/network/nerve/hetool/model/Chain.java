package network.nerve.hetool.model;

import io.nuls.core.log.logback.NulsLogger;

public class Chain {
    private ConfigBean config;
    private NulsLogger logger;

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public int getChainId() {
        return config.getChainId();
    }


}
