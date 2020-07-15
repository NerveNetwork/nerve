/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.network.rpc.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.network.constant.CmdConstant;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.constant.NetworkContext;
import io.nuls.network.constant.NetworkErrorCode;
import io.nuls.network.manager.BusinessGroupManager;
import io.nuls.network.manager.MessageManager;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.manager.handler.MessageHandlerFactory;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.message.base.MessageHeader;
import io.nuls.network.utils.LoggerUtil;

import java.util.*;

/**
 * @author lan
 * @description 消息远程调用
 * 模块消息处理器注册
 * 发送消息调用
 * @date 2018/11/12
 **/
@Component
public class MessageRpc extends BaseCmd {

    private MessageHandlerFactory messageHandlerFactory = MessageHandlerFactory.getInstance();

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_PROTOCOL_REGISTER, version = 1.0,
            description = "模块协议指令注册")
    @Parameters(value = {
            @Parameter(parameterName = "role", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块角色名称"),
            @Parameter(parameterName = "protocolCmds", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "注册指令列表")
    })
    @ResponseData(description = "无特定返回值，没有错误即成功")
    public Response protocolRegister(Map params) {
        String role = String.valueOf(params.get("role"));
        try {
            /*
             * 如果外部模块修改了调用注册信息，进行重启，则清理缓存信息，并重新注册
             * clear cache protocolRoleHandler
             */
            messageHandlerFactory.clearCacheProtocolRoleHandlerMap(role);
            List<String> protocolCmds = (List<String>) params.get("protocolCmds");
            for (String cmd : protocolCmds) {
                messageHandlerFactory.addProtocolRoleHandlerMap(cmd, CmdPriority.DEFAULT, role);
            }
            Log.info("----------------------------new message register---------------------------");
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(role, e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        return success();
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_PROTOCOL_PRIORITY_REGISTER, version = 1.0,
            description = "模块协议指令注册，带有优先级参数")
    @Parameters(value = {
            @Parameter(parameterName = "role", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块角色名称"),
            @Parameter(parameterName = "protocolCmds", requestType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "cmd", valueType = String.class, description = "协议指令名称,12byte"),
                    @Key(name = "priority", valueType = String.class, description = "优先级,3个等级,HIGH,DEFAULT,LOWER")
            }), parameterDes = "注册指令列表")
    })
    @ResponseData(description = "无特定返回值，没有错误即成功")
    public Response protocolRegisterWithPriority(Map params) {
        String role = String.valueOf(params.get("role"));
        try {
            /*
             * 如果外部模块修改了调用注册信息，进行重启，则清理缓存信息，并重新注册
             * clear cache protocolRoleHandler
             */
            messageHandlerFactory.clearCacheProtocolRoleHandlerMap(role);
            List<Map<String, Object>> protocolCmds = (List<Map<String, Object>>) params.get("protocolCmds");
            for (Map<String, Object> cmdMap : protocolCmds) {
                String cmd = (String) cmdMap.get("cmd");
                String priority = cmdMap.get("priority") == null ? "DEFAULT" : cmdMap.get("priority").toString();
                messageHandlerFactory.addProtocolRoleHandlerMap(cmd, CmdPriority.valueOf(priority), role);
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(role, e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        return success();
    }

    /**
     * nw_broadcast
     * 外部广播接收
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_NW_BROADCAST, version = 1.0,
            description = "广播消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "排除peer节点Id，用逗号分割"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息体Hex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息协议指令"),
            @Parameter(parameterName = "isCross", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "是否是跨链"),
            @Parameter(parameterName = "percent", requestType = @TypeDescriptor(value = int.class), parameterDes = "广播发送比例,不填写,默认100"),

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "一个节点都没发送出去时返回false")
    }))
    public Response broadcast(Map params) {
        Map<String, Object> rtMap = new HashMap<>();
        rtMap.put("value", true);
        int percent = NetworkConstant.FULL_BROADCAST_PERCENT;
        try {
            int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
            String excludeNodes = String.valueOf(params.get("excludeNodes"));
            String messageBodyStr = String.valueOf(params.get("messageBody"));
            byte[] messageBody = RPCUtil.decode(messageBodyStr);
            String cmd = String.valueOf(params.get("command"));
            Object percentParam = params.get("percent");
            if (null != percentParam) {
                percent = Integer.valueOf(String.valueOf(percentParam));
            }
            MessageManager messageManager = MessageManager.getInstance();
            NodeGroup nodeGroup = NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
            if (null == nodeGroup) {
                LoggerUtil.COMMON_LOG.error("chain={} is not exist!", chainId);
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            }
            long magicNumber = nodeGroup.getMagicNumber();
            long checksum = messageManager.getCheckSum(messageBody);
            MessageHeader header = new MessageHeader(cmd, magicNumber, checksum, messageBody.length);
            byte[] headerByte = header.serialize();
            byte[] message = new byte[headerByte.length + messageBody.length];
            boolean isCross = false;
            if (null != params.get("isCross")) {
                isCross = Boolean.valueOf(params.get("isCross").toString());
            }
            System.arraycopy(headerByte, 0, message, 0, headerByte.length);
            System.arraycopy(messageBody, 0, message, headerByte.length, messageBody.length);
            Collection<Node> nodesCollection = nodeGroup.getAvailableNodes(isCross);
            excludeNodes = NetworkConstant.COMMA + excludeNodes + NetworkConstant.COMMA;
            List<Node> nodes = new ArrayList<>();
            for (Node node : nodesCollection) {
                if (!excludeNodes.contains(NetworkConstant.COMMA + node.getId() + NetworkConstant.COMMA)) {
                    nodes.add(node);
                }
            }
            if (0 == nodes.size()) {
                LoggerUtil.logger(chainId).error("broadCast fail peer number=0  nodesCollection={} cmd={}", nodesCollection.size(), cmd);
                rtMap.put("value", false);
            } else {
                messageManager.broadcastToNodes(message, cmd, nodes, true, percent);
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        return success(rtMap);
    }

    /**
     * nw_broadcast
     * 外部广播接收
     */
    @CmdAnnotation(cmd = CmdConstant.NW_BROADCAST_JOIN_CONSENSUS, version = 1.0,
            description = "广播消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "排除peer节点Id，用逗号分割"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息体Hex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息协议指令"),
            @Parameter(parameterName = "isCross", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "是否是跨链"),
            @Parameter(parameterName = "percent", requestType = @TypeDescriptor(value = int.class), parameterDes = "广播发送比例,不填写,默认100"),

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "一个节点都没发送出去时返回false")
    }))
    public Response broadcastJoinConsensusMessage(Map params) {
        NetworkContext.isConsensusNode = true;
        return broadcast(params);
    }


    /**
     * nw_broadcast
     * 外部广播接收
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_NW_SEND_BY_IPS, version = 1.0,
            description = "广播消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "发送的节点Ip集合"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "排除peer节点Id，用逗号分割"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息体Hex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息协议指令")

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "可发送连接集合")
    }))
    public Response sendByIps(Map params) {
        List<String> rtList = new ArrayList<>();
        Map<String, Object> rtMap = new HashMap<>();
        int percent = NetworkConstant.FULL_BROADCAST_PERCENT;
        String excludeNodes = "";
        Map<String, Integer> excludeNodesMap = new HashMap<>();
        if (null != params.get("excludeNodes")) {
            excludeNodes = String.valueOf(params.get("excludeNodes"));
            String[] excludeNodeArray = excludeNodes.split(NetworkConstant.COMMA);
            for (String excludeNode : excludeNodeArray) {
                excludeNodesMap.put(excludeNode.split(NetworkConstant.COLON)[0], 1);
            }
        }

        try {
            int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
            List<String> ips = (List) (params.get("ips"));
            Map<String, Integer> ipsMap = new HashMap<>();
            for (String ip : ips) {
                ipsMap.put(ip, 1);
            }
            String messageBodyStr = String.valueOf(params.get("messageBody"));
            byte[] messageBody = RPCUtil.decode(messageBodyStr);
            String cmd = String.valueOf(params.get("command"));
            Object percentParam = params.get("percent");
            if (null != percentParam) {
                percent = Integer.valueOf(String.valueOf(percentParam));
            }
            MessageManager messageManager = MessageManager.getInstance();
            NodeGroup nodeGroup = NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
            if (null == nodeGroup) {
                LoggerUtil.logger(chainId).error("chain is not exist!");
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            }
            long magicNumber = nodeGroup.getMagicNumber();
            long checksum = messageManager.getCheckSum(messageBody);
            MessageHeader header = new MessageHeader(cmd, magicNumber, checksum, messageBody.length);
            byte[] headerByte = header.serialize();
            byte[] message = new byte[headerByte.length + messageBody.length];
            System.arraycopy(headerByte, 0, message, 0, headerByte.length);
            System.arraycopy(messageBody, 0, message, headerByte.length, messageBody.length);
            Collection<Node> nodesCollection = nodeGroup.getAvailableNodes(false);
            List<Node> nodes = new ArrayList<>();
            for (Node node : nodesCollection) {
                if (null != ipsMap.get(node.getIp())) {
                    if (excludeNodesMap.get(node.getIp()) == null) {
                        nodes.add(node);
                    }
                    rtList.add(node.getIp());
                }
            }
            if (nodes.size() > 0) {
                NetworkEventResult result = messageManager.broadcastToNodes(message, cmd, nodes, false, percent);
                if(nodes.size()>20&&!result.isSuccess()){
                    System.out.println();
                }
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        rtMap.put("list", rtList);
        return success(rtMap);
    }

    /**
     * nw_sendPeersMsg
     */

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_SEND_PEERS_MSG, version = 1.0,
            description = "向指定节点发送消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "nodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "指定发送peer节点Id，用逗号拼接的字符串"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息体Hex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息协议指令")
    })
    @ResponseData(description = "无特定返回值，没有错误即成功")
    public Response sendPeersMsg(Map params) {
        try {
            int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
            String nodes = String.valueOf(params.get("nodes"));
            String messageBodyStr = String.valueOf(params.get("messageBody"));
            byte[] messageBody = RPCUtil.decode(messageBodyStr);
            String cmd = String.valueOf(params.get("command"));
            MessageManager messageManager = MessageManager.getInstance();
            NodeGroupManager nodeGroupManager = NodeGroupManager.getInstance();
            NodeGroup nodeGroup = nodeGroupManager.getNodeGroupByChainId(chainId);
            long magicNumber = nodeGroup.getMagicNumber();
            long checksum = messageManager.getCheckSum(messageBody);
            MessageHeader header = new MessageHeader(cmd, magicNumber, checksum, messageBody.length);
            byte[] headerByte = header.serialize();
            byte[] message = new byte[headerByte.length + messageBody.length];
            System.arraycopy(headerByte, 0, message, 0, headerByte.length);
            System.arraycopy(messageBody, 0, message, headerByte.length, messageBody.length);
            String[] nodeIds = nodes.split(",");
            List<Node> nodesList = new ArrayList<>();
            for (String nodeId : nodeIds) {
                Node availableNode = nodeGroup.getAvailableNode(nodeId);
                if (null != availableNode) {
                    nodesList.add(availableNode);
                } else {
                    LoggerUtil.logger(chainId).error("cmd={},node = {} is not available!", cmd, nodeId);
                }
            }
            if (nodesList.size() > 0) {
                messageManager.broadcastToNodes(message, cmd, nodesList, true, NetworkConstant.FULL_BROADCAST_PERCENT);
            } else {
                LoggerUtil.logger(chainId).error("cmd={},send peer number=0", cmd);
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        return success();
    }

    /**
     * nw_sendPeersMsg
     */

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_SEND_BY_GROUP_FLAG, version = 1.0,
            description = "向指定节点发送消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块名称"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "节点组标识"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息体Hex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "消息协议指令")

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "可发送连接集合")
    }))
    public Response sendByGroupIps(Map params) {
        List<String> rtList = new ArrayList<>();
        Map<String, Object> rtMap = new HashMap<>();
        int percent = NetworkConstant.FULL_BROADCAST_PERCENT;
        try {
            int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
            String module = String.valueOf(params.get("module"));
            String groupFlag = String.valueOf(params.get("groupFlag"));
            String excludeNodes = String.valueOf(params.get("excludeNodes"));
            BusinessGroupManager businessGroupManager = BusinessGroupManager.getInstance();
            Map<String, String> ipsMap = businessGroupManager.getIpsMap(chainId, module, groupFlag);
            if (ipsMap == null) {
                LoggerUtil.COMMON_LOG.info("未连接到其他节点");
                rtMap.put("list", rtList);
                return success(rtMap);
            }
            String messageBodyStr = String.valueOf(params.get("messageBody"));
            byte[] messageBody = RPCUtil.decode(messageBodyStr);
            String cmd = String.valueOf(params.get("command"));
            Object percentParam = params.get("percent");
            if (null != percentParam) {
                percent = Integer.valueOf(String.valueOf(percentParam));
            }
            MessageManager messageManager = MessageManager.getInstance();
            NodeGroup nodeGroup = NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
            if (null == nodeGroup) {
                LoggerUtil.logger(chainId).error("chain is not exist!");
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            }
            long magicNumber = nodeGroup.getMagicNumber();
            long checksum = messageManager.getCheckSum(messageBody);
            MessageHeader header = new MessageHeader(cmd, magicNumber, checksum, messageBody.length);
            byte[] headerByte = header.serialize();
            byte[] message = new byte[headerByte.length + messageBody.length];
            System.arraycopy(headerByte, 0, message, 0, headerByte.length);
            System.arraycopy(messageBody, 0, message, headerByte.length, messageBody.length);
            Collection<Node> nodesCollection = nodeGroup.getAvailableNodes(false);
            List<Node> nodes = new ArrayList<>();
            excludeNodes = NetworkConstant.COMMA + excludeNodes + NetworkConstant.COMMA;
            for (Node node : nodesCollection) {
                if (null != ipsMap.get(node.getIp()) && !excludeNodes.contains(NetworkConstant.COMMA + node.getId() + NetworkConstant.COMMA)) {
                    nodes.add(node);
                    rtList.add(node.getIp());
                }
            }
            if (nodes.size() > 0) {
                LoggerUtil.COMMON_LOG.info("=====sendByGroupIps nodes = {},cmd={}", nodes.size(), cmd);
                messageManager.broadcastToNodes(message, cmd, nodes, true, percent);
            } else {
                LoggerUtil.COMMON_LOG.info("=====sendByGroupIps nodes = 0,cmd={}", cmd);
            }
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        rtMap.put("list", rtList);
        return success(rtMap);
    }
}
