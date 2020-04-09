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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lan
 * @description Open peer connection remote call node rpc
 * 直接连接peer
 * @create 2019/10/18
 **/
@Component
public class ConnectRpc extends BaseCmd {
    @CmdAnnotation(cmd = CmdConstant.CMD_DIRECT_CONNECT_NODES, version = 1.0,
            description = "连接节点")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "nodeId", requestType = @TypeDescriptor(value = String.class), parameterDes = "节点组ID，ip:port")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "无法规定时间完成业务连接的返回false")
    }))
    public Response connectNodes(Map params) {
        NodeGroupManager nodeGroupManager = NodeGroupManager.getInstance();
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        boolean blCross = Boolean.valueOf(String.valueOf(params.get("isCross")));
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
            if (blCross) {
                //暂时不支持跨链连接
                return failed(NetworkErrorCode.PARAMETER_ERROR);
            } else {
                if (nodeGroup.getLocalNetNodeContainer().hadPeerIp(nodeId, ipPort[0])) {
                    LoggerUtil.logger(chainId).info("connected success:{}, had exist.", nodeId);
                    rtMap.put("value", true);
                    return success(rtMap);
                }
                Node node = new Node(nodeGroup.getMagicNumber(), ipPort[0], Integer.valueOf(ipPort[1]), 0, Node.OUT, blCross);
                node.setConnectStatus(NodeConnectStatusEnum.CONNECTING);

                node.setRegisterListener(() -> LoggerUtil.logger(chainId).debug("new node {} Register!", node.getId()));

                node.setConnectedListener(() -> {
                    LoggerUtil.logger(chainId).debug("connected success:{},iscross={}", node.getId(), node.isCrossConnect());
                    connectionManager.nodeClientConnectSuccess(node);
                });

                node.setDisconnectListener(() -> {
                    LoggerUtil.logger(node.getNodeGroup().getChainId()).debug("connected disconnect:{},iscross={}", node.getId(), node.isCrossConnect());
                    connectionManager.nodeConnectDisconnect(node);
                });
                if (connectionManager.connection(node)) {
                    //等待监听1s后,返回信息TODO:
//                   CompletableFuture<Node> future = new CompletableFuture<>();
//                   Node result =future.get(1000,TimeUnit.MILLISECONDS);
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

            }
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
        rtMap.put("value", false);
        return success(rtMap);
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_ADD_BUSINESS_GROUP_IPS, version = 1.0,
            description = "添加连接业务节点组")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块名称"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "节点组标识"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "新增的IP组")

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "正确true,错误false")
    }))
    public Response addIps(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
        List<String> ips = (List) params.get("ips");
        LoggerUtil.logger(chainId).info("addIps {}-{} ip={}", module, groupFlag, ips.get(0));
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
            description = "添加连接业务节点组")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块名称"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "节点组标识")

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "业务连接集合")
    }))
    public Response getBusinessGroupNodes(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
        LoggerUtil.logger(chainId).info("getBusinessGroupNodes {}-{}", module, groupFlag);
        BusinessGroupManager businessGroupManager = BusinessGroupManager.getInstance();
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            List<String> ips = businessGroupManager.getIps(chainId, module, groupFlag);
            LoggerUtil.logger(chainId).info("getBusinessGroupNodes ips={}", ips.size());
            rtMap.put("list", ips);
            return success(rtMap);
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return failed(e.getMessage());
        }
    }


    @CmdAnnotation(cmd = CmdConstant.CMD_REMOVE_BUSINESS_GROUP_IPS, version = 1.0,
            description = "移除连接业务节点组")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "连接的链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "module", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块名称"),
            @Parameter(parameterName = "groupFlag", requestType = @TypeDescriptor(value = String.class), parameterDes = "节点组标识"),
            @Parameter(parameterName = "ips", requestType = @TypeDescriptor(value = List.class), parameterDes = "新增的IP组")

    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "正确true,错误false")
    }))
    public Response removeIps(Map params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        String module = String.valueOf(params.get("module"));
        String groupFlag = String.valueOf(params.get("groupFlag"));
        List<String> ips = (List) params.get("ips");
        LoggerUtil.logger(chainId).info("remove {}-{} ip={}", module, groupFlag, ips.get(0));
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
