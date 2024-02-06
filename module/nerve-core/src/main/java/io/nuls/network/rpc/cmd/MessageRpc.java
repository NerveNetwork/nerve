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

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.base.RPCUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
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
 * @description Message remote call
 * Module Message Processor Registration
 * Send Message Call
 * @date 2018/11/12
 **/
@Component
@NerveCoreCmd(module = ModuleE.NW)
public class MessageRpc extends BaseCmd {

    private MessageHandlerFactory messageHandlerFactory = MessageHandlerFactory.getInstance();

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_PROTOCOL_REGISTER, version = 1.0,
            description = "Module Protocol Instruction Registration")
    @Parameters(value = {
            @Parameter(parameterName = "role", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Role Name"),
            @Parameter(parameterName = "protocolCmds", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "Register Instruction List")
    })
    @ResponseData(description = "No specific return value, successful without errors")
    public Response protocolRegister(Map params) {
        String role = String.valueOf(params.get("role"));
        try {
            /*
             * If the external module modifies the call registration information and restarts, clear the cache information and register again
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
            description = "Module protocol instruction registration with priority parameters")
    @Parameters(value = {
            @Parameter(parameterName = "role", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Role Name"),
            @Parameter(parameterName = "protocolCmds", requestType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "cmd", valueType = String.class, description = "Protocol instruction name,12byte"),
                    @Key(name = "priority", valueType = String.class, description = "priority,3Level,HIGH,DEFAULT,LOWER")
            }), parameterDes = "Register Instruction List")
    })
    @ResponseData(description = "No specific return value, successful without errors")
    public Response protocolRegisterWithPriority(Map params) {
        String role = String.valueOf(params.get("role"));
        try {
            /*
             * If the external module modifies the call registration information and restarts, clear the cache information and register again
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
     * External broadcast reception
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_NW_BROADCAST, version = 1.0,
            description = "Broadcast messages")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "eliminatepeernodeId, separated by commas"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message BodyHex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message Protocol Instructions"),
            @Parameter(parameterName = "isCross", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "Is it cross chain"),
            @Parameter(parameterName = "percent", requestType = @TypeDescriptor(value = int.class), parameterDes = "Broadcast transmission ratio,Not filled in,default100"),

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Returned when no node has been sent outfalse")
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
     * External broadcast reception
     */
    @CmdAnnotation(cmd = CmdConstant.NW_BROADCAST_JOIN_CONSENSUS, version = 1.0,
            description = "Broadcast messages")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "eliminatepeernodeId, separated by commas"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message BodyHex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message Protocol Instructions"),
            @Parameter(parameterName = "isCross", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "Is it cross chain"),
            @Parameter(parameterName = "percent", requestType = @TypeDescriptor(value = int.class), parameterDes = "Broadcast transmission ratio,Not filled in,default100"),

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Returned when no node has been sent outfalse")
    }))
    public Response broadcastJoinConsensusMessage(Map params) {
        NetworkContext.isConsensusNode = true;
        return broadcast(params);
    }


    /**
     * nw_broadcast
     * External broadcast reception
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_NW_SEND_BY_IPS, version = 1.0,
            description = "Broadcast messages")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "Sent nodesIpaggregate"),
            @Parameter(parameterName = "excludeNodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "eliminatepeernodeId, separated by commas"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message BodyHex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message Protocol Instructions")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "Can send connection sets")
    }))
    public Response sendByIps(Map params) {
        long time1, time2;
        time1 = System.currentTimeMillis();
        List<String> rtList = new ArrayList<>();
        Map<String, Object> rtMap = new HashMap<>();
        int percent = NetworkConstant.FULL_BROADCAST_PERCENT;
        int chainId = 0;
        String excludeNodes = "";
        List<String> excludeNodesList = new ArrayList<>();
        if (null != params.get("excludeNodes")) {
            excludeNodes = String.valueOf(params.get("excludeNodes"));
            String[] excludeNodeArray = excludeNodes.split(NetworkConstant.COMMA);
            for (String excludeNode : excludeNodeArray) {
                excludeNodesList.add(excludeNode.split(NetworkConstant.COLON)[0]);
            }
        }

        try {
            chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
            List<String> ips = (List) (params.get("ips"));
            Boolean isForward = (Boolean) (params.get("isForward"));
            if (null == isForward) {
                isForward = false;
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
            List<Node> nodesCollection = nodeGroup.getAvailableNodes(false);
            List<Node> nodes = new ArrayList<>();
//            if (ips.size() == 1) {
            for (Node node : nodesCollection) {
                if (!ips.contains(node.getIp())) {
                    continue;
                }
                if (excludeNodesList.contains(node.getIp())) {
                    continue;
                }
                nodes.add(node);
                rtList.add(node.getIp());
            }
//            } else if (isForward) {
//                nodes.addAll(nodesCollection);
//                if (nodes.size() > 15) {
//                    int size = nodes.size() / 3;
//                    List<Node> list = new ArrayList<>();
//                    for (int i = 0; i < size; i++) {
//                        list.add(nodes.remove(new Random().nextInt(nodes.size())));
//                    }
//                    nodes = list;
//                }
//            } else {
//                nodes.addAll(nodesCollection);
//            }

            time2 = System.currentTimeMillis();
//            LoggerUtil.logger(chainId).info("------------sendByIps find Nodes use:{} , node size:{}", (time2 - time1), nodes.size());
            time1 = System.currentTimeMillis();

            if (nodes.size() > 0) {
                NetworkEventResult result = messageManager.broadcastToNodes(message, cmd, nodes, true, percent);
                if (nodes.size() < ips.size() / 2) {
                    LoggerUtil.logger(chainId).info("sendByIps [{}] nodes.size=={},ips:{}", cmd, nodes.size(), ips.size());
                }
            } else {
                LoggerUtil.logger(chainId).info("FALSE sendByIps [{}] nodes.size=={},ips:{}", cmd, nodes.size(), ips.size());
            }
        } catch (Exception e) {
            if (chainId != 0) {
                LoggerUtil.logger(chainId).error("------------sendByIps message error");
                LoggerUtil.logger(chainId).error(e);
            } else {
                LoggerUtil.COMMON_LOG.error("------------sendByIps message error");
                LoggerUtil.COMMON_LOG.error(e);
            }

            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        rtMap.put("list", rtList);
        return success(rtMap);
    }

    /**
     * nw_sendPeersMsg
     */

    @CmdAnnotation(cmd = CmdConstant.CMD_NW_SEND_PEERS_MSG, version = 1.0,
            description = "Send messages to specified nodes")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "nodes", requestType = @TypeDescriptor(value = String.class), parameterDes = "Specify sendingpeernodeIdString concatenated with commas"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message BodyHex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message Protocol Instructions")
    })
    @ResponseData(description = "No specific return value, successful without errors")
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
                }
            }
            //If the nodeidIf it does not exist, search for the sameipNode of
            if (nodesList.isEmpty()) {
                List<String> ips = new ArrayList<>();
                for (String nodeId : nodeIds) {
                    ips.add(nodeId.split(":")[0]);
                }
                List<Node> list = nodeGroup.getAvailableNodes(false);
                for (Node node : list) {
                    if (ips.contains(node.getIp())) {
                        nodesList.add(node);
                    }
                }
            }
            if (nodesList.size() > 0) {
                messageManager.broadcastToNodes(message, cmd, nodesList, false, NetworkConstant.FULL_BROADCAST_PERCENT);
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
            description = "Send messages to specified nodes")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Name"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "Node group identification"),
            @Parameter(parameterName = "messageBody", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message BodyHex"),
            @Parameter(parameterName = "command", requestType = @TypeDescriptor(value = String.class), parameterDes = "Message Protocol Instructions")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "Can send connection sets")
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
                LoggerUtil.COMMON_LOG.info("Not connected to other nodes");
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
//                LoggerUtil.COMMON_LOG.info("=====sendByGroupIps nodes = {},cmd={}", nodes.size(), cmd);
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
