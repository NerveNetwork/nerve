package io.nuls.block.rpc.call;

import io.nuls.base.protocol.ModuleHelper;
import io.nuls.block.manager.ContextManager;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.common.NerveCoreResponseMessageProcessor;

import java.util.HashMap;
import java.util.Map;

public class CrossChainCall {

    /**
     * 批量保存交易
     *
     * @param chainId 链Id/chain id
     * @param height
     * @param blockHeader
     * @return
     */
    public static void heightNotice(int chainId, long height, String blockHeader) {
        if (!ModuleHelper.isSupportCrossChain()) {
            return;
        }
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            Map<String, Object> params = new HashMap<>(4);
//            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("height", height);
            params.put("blockHeader", blockHeader);
            NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CC.abbr, "newBlockHeight", params);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * 节点同步状态变更通知跨链模块
     *
     * @param chainId 链Id/chain id
     * @param status  1-工作,0-等待
     * @return
     */
    public static boolean notice(int chainId, int status) {
        if (!ModuleHelper.isSupportCrossChain()) {
            return true;
        }
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("status", status);
            return NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CC.abbr, "syncStatusUpdate", params).isSuccess();
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }
}
