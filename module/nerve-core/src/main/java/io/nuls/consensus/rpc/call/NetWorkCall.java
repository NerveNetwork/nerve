package io.nuls.consensus.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.consensus.constant.NodeEnum;
import io.nuls.consensus.model.dto.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 与网络模块交互类
 * Interaction class with network module
 *
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
        return broadcast(chainId, message, null, command, isCross);
    }

    /**
     * 给网络上节点广播消息
     *
     * @param chainId      链Id/chain id
     * @param message
     * @param excludeNodes 排除的节点
     * @return
     */
    public static boolean broadcast(int chainId, BaseBusinessMessage message, String excludeNodes, String command, boolean isCross) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("excludeNodes", excludeNodes);
            params.put("messageBody", RPCUtil.encode(message.serialize()));
            params.put("command", command);
            params.put("isCross", isCross);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_broadcast", params);
            if (!cmdResp.isSuccess()) {
                LoggerUtil.commonLog.error("Packing state failed to send!");
                return false;
            }
            return (boolean) ((HashMap) ((HashMap) cmdResp.getResponseData()).get("nw_broadcast")).get("value");
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
            return NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_sendPeersMsg", params).isSuccess();
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
            return false;
        }
    }


    /**
     * 根据链ID获取可用节点
     *
     * @param chainId 链Id/chain id
     * @return
     */
    public static List<Node> getAvailableNodes(int chainId) {

        try {
            Map<String, Object> params = new HashMap<>(6);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("state", 1);
            params.put("isCross", false);
            params.put("startPage", 0);
            params.put("pageSize", 0);

            Response response = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_getNodes", params);
            if (!response.isSuccess()) {
                return List.of();
            }
            Map responseData = (Map) response.getResponseData();
            List list = (List) responseData.get("nw_getNodes");
            List<Node> nodes = new ArrayList<>();
            for (Object o : list) {
                Map map = (Map) o;
                Node node = new Node();
                node.setId((String) map.get("nodeId"));
                node.setHeight(Long.parseLong(map.get("blockHeight").toString()));
                String blockHash = (String) map.get("blockHash");
                if (StringUtils.isBlank(blockHash)) {
                    continue;
                }
                node.setHash(NulsHash.fromHex(blockHash));
                node.setNodeEnum(NodeEnum.IDLE);
                nodes.add(node);
            }
            return nodes;
        } catch (Exception e) {
            Log.error("", e);
            return List.of();
        }
    }
}
