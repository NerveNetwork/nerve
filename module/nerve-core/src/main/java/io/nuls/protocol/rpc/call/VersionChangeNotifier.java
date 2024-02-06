package io.nuls.protocol.rpc.call;

import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.Protocol;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.common.NerveCoreRpcCall;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.protocol.manager.ContextManager;
import io.nuls.protocol.model.ProtocolContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionChangeNotifier {

    /**
     * Notify all modules after protocol upgrade
     * @param chainId
     * @param version
     * @return
     */
    public static boolean notify(int chainId, short version) {
        long begin = System.nanoTime();
        List<String> noticedModule = new ArrayList<>();
        noticedModule.add(ModuleE.NC.abbr);
        for (String module : noticedModule) {
            long l = System.nanoTime();
            Map<String, Object> params = new HashMap<>(4);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("protocolVersion", version);
            try {
                NerveCoreRpcCall.requestAndResponse(module, "protocolVersionChange", params);
            } catch (NulsException e) {
                return false;
            }
            long end = System.nanoTime();
            ContextManager.getContext(chainId).getLogger().info("****{} notify time-{}ms", module, (end - l) / 1000000);
        }
        long end = System.nanoTime();
        ContextManager.getContext(chainId).getLogger().info("****total notify time-{}ms", (end - begin) / 1000000);
        return true;
    }

    /**
     * When the protocol version changes,Re register transaction、news
     *
     * @param chainId
     * @param context
     * @param version
     * @return
     */
    public static void reRegister(int chainId, ProtocolContext context, short version) {
        long begin = System.nanoTime();
        List<Map.Entry<String, Protocol>> entries = context.getProtocolMap().get(version);
        if (entries != null) {
            entries.forEach(e -> {
                RegisterHelper.registerMsg(e.getValue(), e.getKey());
                RegisterHelper.registerTx(chainId, e.getValue(), e.getKey());
            });
        }
        long end = System.nanoTime();
        context.getLogger().info("****reRegister time****" + (end - begin));
    }

}
