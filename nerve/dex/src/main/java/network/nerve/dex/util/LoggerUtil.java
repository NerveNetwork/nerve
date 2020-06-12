package network.nerve.dex.util;

import io.nuls.core.log.logback.LoggerBuilder;
import io.nuls.core.log.logback.NulsLogger;

public class LoggerUtil {

    public static NulsLogger dexLog = LoggerBuilder.getLogger("dex");

    public static NulsLogger dexInfoLog = LoggerBuilder.getLogger("dex-info");

    public static NulsLogger dexCoinLog = LoggerBuilder.getLogger("dex-coin");
}
