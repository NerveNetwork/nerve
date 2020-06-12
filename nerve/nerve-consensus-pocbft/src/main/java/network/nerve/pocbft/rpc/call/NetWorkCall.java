package network.nerve.pocbft.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.pocbft.utils.LoggerUtil;
import java.util.HashMap;
import java.util.Map;


/**
 * 与网络模块交互类
 * Interaction class with network module
 * @author tag
 * 2019/4/10
 */
public class NetWorkCall {

    /**
     * 给网络上节点广播消息
     *
     * @param chainId 该消息由那条链处理/chain id
     * @param message
     * @return
     */
    public static boolean broadcast(int chainId, BaseBusinessMessage message, String command, boolean isCross) {
        return broadcast(chainId, message, null, command,isCross);
    }

    /**
     * 给网络上节点广播消息
     *
     * @param chainId 链Id/chain id
     * @param message
     * @param excludeNodes 排除的节点
     * @return
     */
    public static boolean broadcast(int chainId, BaseBusinessMessage message, String excludeNodes, String command,boolean isCross) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("excludeNodes", excludeNodes);
            params.put("messageBody", RPCUtil.encode(message.serialize()));
            params.put("command", command);
            params.put("isCross", isCross);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_broadcast", params);
            if (!cmdResp.isSuccess()) {
                LoggerUtil.commonLog.error("Packing state failed to send!");
                return false;
            }
            return   (boolean)((HashMap) ((HashMap) cmdResp.getResponseData()).get("nw_broadcast")).get("value");
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return false;
        }
    }

    /**
     * 给指定节点发送消息
     *
     * @param chainId 链Id/chain id
     * @param message
     * @param nodeId
     * @return
     */
    public static boolean sendToNode(int chainId, BaseBusinessMessage message, String nodeId, String command) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("nodes", nodeId);
            params.put("messageBody", RPCUtil.encode(message.serialize()));
            params.put("command", command);
            return ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_sendPeersMsg", params).isSuccess();
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return false;
        }
    }
}
