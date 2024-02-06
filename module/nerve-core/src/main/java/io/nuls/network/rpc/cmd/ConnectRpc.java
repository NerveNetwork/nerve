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
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.network.constant.CmdConstant;
import io.nuls.network.constant.NetworkErrorCode;
import io.nuls.network.constant.NodeConnectStatusEnum;
import io.nuls.network.manager.BusinessGroupManager;
import io.nuls.network.manager.ConnectionManager;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.utils.IpUtil;
import io.nuls.network.utils.LoggerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lan
 * @description Open peer connection remote call node rpc
 * Direct connectionpeer
 * @create 2019/10/18
 **/
@Component
@NerveCoreCmd(module = ModuleE.NW)
public class ConnectRpc extends BaseCmd {
    @CmdAnnotation(cmd = CmdConstant.CMD_DIRECT_CONNECT_NODES, version = 1.0,
            description = "Connecting nodes")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "nodeId", requestType = @TypeDescriptor(value = String.class), parameterDes = "Node groupID,ip:port")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Unable to complete the return of business connection within the specified timefalse")
    }))
    public Response connectNodes(Map params) {
        NodeGroupManager nodeGroupManager = NodeGroupManager.getInstance();
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            String nodeId = String.valueOf(params.get("nodeId"));
            if (chainId < 0 || StringUtils.isBlank(nodeId)) {
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            }
            NodeGroup nodeGroup = nodeGroupManager.getNodeGroupByChainId(chainId);
            String[] ipPort = IpUtil.changeHostToIp(nodeId);
            if (null == ipPort) {
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            }
            if (nodeGroup.getLocalNetNodeContainer().hadPeerIp(nodeId, ipPort[0])) {
                LoggerUtil.logger(chainId).info("Connected,connected success:{}, had exist.", nodeId);
                rtMap.put("value", true);
                return success(rtMap);
            }
            Node node = new Node(nodeGroup.getMagicNumber(), ipPort[0], Integer.valueOf(ipPort[1]), 0, Node.OUT, false);
            node.setConnectStatus(NodeConnectStatusEnum.CONNECTING);

            node.setRegisterListener(() -> LoggerUtil.logger(chainId).debug("new node {} Register!", node.getId()));

            node.setConnectedListener(() -> {
                LoggerUtil.logger(chainId).debug("connected success:{},iscross={}", node.getId(), node.isCrossConnect());
                connectionManager.nodeClientConnectSuccess(node);
            });

            node.setDisconnectListener(() -> {
                LoggerUtil.logger(node.getNodeGroup().getChainId()).info("connectRpc connected disconnect:{},iscross={}", node.getId(), node.isCrossConnect());
                connectionManager.nodeConnectDisconnect(node);
            });
            if (connectionManager.connection(node)) {
                int times = 0;
                boolean hadConn = false;
                while (!hadConn && times < 10) {
                    LoggerUtil.logger(chainId).info("continues connected node:{}", ipPort[0]);
                    Thread.sleep(100L);
                    hadConn = nodeGroup.getLocalNetNodeContainer().hadPeerIp(nodeId, ipPort[0]);
                    times++;
                }
                LoggerUtil.logger(chainId).info("connected node:{} hadConn={} success", ipPort[0], hadConn);
                rtMap.put("value", hadConn);
                return success(rtMap);
            }
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
        rtMap.put("value", false);
        return success(rtMap);
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_ADD_BUSINESS_GROUP_IPS, version = 1.0,
            description = "Add Connection Business Node Group")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Name"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "Node group identification"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "Newly addedIPgroup")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "correcttrue,errorfalse")
    }))
    public Response addIps(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
        List<String> ips = (List) params.get("ips");
//        LoggerUtil.logger(chainId).info("addIps {}-{} ip={}", module, groupFlag, ips.get(0));
        BusinessGroupManager businessGroupManager = BusinessGroupManager.getInstance();
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            businessGroupManager.addNodes(chainId, module, groupFlag, ips);
            businessGroupManager.printGroupsInfo(chainId, module, groupFlag);
            rtMap.put("value", true);
            return success(rtMap);
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_GET_BUSINESS_GROUP_IPS, version = 1.0,
            description = "Add Connection Business Node Group")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Name"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "Node group identification")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "Business Connection Collection")
    }))
    public Response getBusinessGroupNodes(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
//        LoggerUtil.logger(chainId).info("getBusinessGroupNodes {}-{}", module, groupFlag);
        BusinessGroupManager businessGroupManager = BusinessGroupManager.getInstance();
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            List<String> ips = businessGroupManager.getIps(chainId, module, groupFlag);
//            LoggerUtil.logger(chainId).info("getBusinessGroupNodes ips={}", ips.size());
            rtMap.put("list", ips);
            return success(rtMap);
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
    }


    @CmdAnnotation(cmd = CmdConstant.CMD_REMOVE_BUSINESS_GROUP_IPS, version = 1.0,
            description = "Remove the connected business node group")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Connected ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "Module Name"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "Node group identification"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "Newly addedIPgroup")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "correcttrue,errorfalse")
    }))
    public Response removeIps(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
        List<String> ips = (List) params.get("ips");
        LoggerUtil.logger(chainId).info("---removeIps {}-{} ip={}", module, groupFlag, ips.get(0));
        BusinessGroupManager businessGroupManager = BusinessGroupManager.getInstance();
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            businessGroupManager.removeNodes(chainId, module, groupFlag, ips);
            businessGroupManager.printGroupsInfo(chainId, module, groupFlag);
            rtMap.put("value", true);
            return success(rtMap);
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
    }
}
