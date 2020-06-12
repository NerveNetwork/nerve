package network.nerve.quotation.util;

import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.quotation.model.bo.Chain;

public class LoggerUtil {

    public static final NulsLogger LOG = LoggerBuilder.getLogger(ModuleE.QU.name);

    public static void init(Chain chain){
        int chainId = chain.getConfigBean().getChainId();
        NulsLogger logger = LoggerBuilder.getLogger(ModuleE.QU.name, chainId);
        chain.setLogger(logger);
    }
}