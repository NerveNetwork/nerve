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

package io.nuls.network.manager.handler.message;

import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.network.constant.NodeConnectStatusEnum;
import io.nuls.network.constant.NodeStatusEnum;
import io.nuls.network.manager.*;
import io.nuls.network.manager.handler.base.BaseMessageHandler;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.dto.BestBlockInfo;
import io.nuls.network.model.message.VerackMessage;
import io.nuls.network.model.message.VersionMessage;
import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.network.model.message.body.VerackMessageBody;
import io.nuls.network.model.message.body.VersionMessageBody;
import io.nuls.network.netty.container.NodesContainer;
import io.nuls.network.rpc.call.BlockRpcService;
import io.nuls.network.rpc.call.impl.BlockRpcServiceImpl;
import io.nuls.network.utils.LoggerUtil;

import java.util.Map;
import java.util.concurrent.*;

/**
 * version message handler
 * client 先发起version消息，server接收到后，
 * 1 是否自连接 2.校验业务连接重叠，连接过载
 *
 * @author lan
 * @date 2018/10/20
 */
public class VersionMessageHandler extends BaseMessageHandler {

    private static VersionMessageHandler instance = new VersionMessageHandler();
    private NodeGroupManager nodeGroupManager = NodeGroupManager.getInstance();
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    public ExecutorService discover = ThreadUtils.createThreadPool(Runtime.getRuntime().availableProcessors(), 20, new NulsThreadFactory("NODE_DISCOVER_MULTI_THREAD"));

    private VersionMessageHandler() {

    }

    public static VersionMessageHandler getInstance() {
        return instance;
    }

    /**
     * 服务器被动连接规则
     * 1. 不超过最大被动连接数
     * 2. 如果自己已经主动连接了对方，不接受对方的被动连接
     * 3. 相同的IP的被动连接不超过n次
     *
     * @param ip
     * @param port
     * @return boolean
     */
    private boolean canConnectIn(int chainId, NodesContainer nodesContainer, int maxInCount, int sameIpMaxCount, String ip, int port) {

        int size = nodesContainer.getConnectedCount(Node.IN);
        if (size >= maxInCount) {
            LoggerUtil.logger(chainId).info("refuse canConnectIn size={},maxInCount={},node={}:{}", size, maxInCount, ip, port);
            return false;
        }

        Map<String, Node> connectedNodes = nodesContainer.getConnectedNodes();

        int sameIpCount = 0;
        for (Node node : connectedNodes.values()) {
            //不会存在两次被动连接都是同一个端口的，即使是同一台服务器
            //if(ip.equals(node.getIp()) && (node.getPort().intValue() == port || node.getType() == Node.OUT)) {
            if (ip.equals(node.getIp()) && node.getType() == Node.OUT) {
                //也可能存在自己连接自己进入这个逻辑
                //这里需要一个机制来判定相互连接时候保留哪个?
                LoggerUtil.logger(chainId).info("refuse canConnectIn ip={},node.getIp()={}, node.getType={}", ip, node.getIp(), node.getType());
                return false;
            }
            if (ip.equals(node.getIp())) {
                sameIpCount++;
            }
            if (sameIpCount >= sameIpMaxCount) {
                LoggerUtil.logger(chainId).info("refuse canConnectIn ip={},sameIpCount={},sameIpMaxCount={}, node.getType={}", ip, sameIpCount, sameIpMaxCount, node.getType());
                return false;
            }
        }

        return true;
    }

    /**
     * server recieve handler
     *
     * @param message message
     * @param node    Node
     */
    private void serverRecieveHandler(BaseMessage message, Node node) {
        VersionMessageBody versionBody = (VersionMessageBody) message.getMsgBody();
        NodeGroup nodeGroup = nodeGroupManager.getNodeGroupByMagic(message.getHeader().getMagicNumber());
        String myIp = versionBody.getAddrYou().getIp().getHostAddress();
        //设置magicNumber
        node.setMagicNumber(nodeGroup.getMagicNumber());
        String ip = node.getIp();
        node.setExternalIp(myIp);
        int maxIn;
        NodesContainer nodesContainer = null;
        int sameIpMaxCount = nodeGroup.getSameIpMaxCount(node.isCrossConnect());
        if (node.isCrossConnect()) {
            //是主网本地magic网络，但是连接了跨链节点,主网magicNumber不存在跨链连接
            if (nodeGroup.isMoonGroup()) {
                LoggerUtil.logger(nodeGroup.getChainId()).error("close!!! node={} status={}, version canConnectIn fail..Cross=true, but group is moon net", node.getId(), node.getStatus());
                node.getChannel().close();
                return;
            } else {
                //判断本地是否出块了，还未出块则取消连接
                BlockRpcService blockRpcService = SpringLiteContext.getBean(BlockRpcServiceImpl.class);
                if (nodeGroup.isMoonNode()) {
                    //主网节点，不用判断，主网卫星链不会存在高度0的情况
                } else {
                    //看跨链节点的高度是否不为0
                    if (!nodeGroup.isHadBlockHeigh()) {
                        BestBlockInfo bestBlockInfo = blockRpcService.getBestBlockHeader(nodeGroup.getChainId());
                        if (bestBlockInfo.getBlockHeight() < 1) {
                            LoggerUtil.logger(nodeGroup.getChainId()).error("close!!! node={} status={}, version canConnectIn fail..Cross=true, but blockHeight={}", node.getId(), node.getStatus(), bestBlockInfo.getBlockHeight());
                            node.getChannel().close();
                            return;
                        } else {
                            nodeGroup.setHadBlockHeigh(true);
                        }
                    }
                }
            }
            maxIn = nodeGroup.getMaxCrossIn();
            nodesContainer = nodeGroup.getCrossNodeContainer();
        } else {
            maxIn = nodeGroup.getMaxIn();
            nodesContainer = nodeGroup.getLocalNetNodeContainer();
        }

        if (!canConnectIn(nodeGroup.getChainId(), nodesContainer, maxIn, sameIpMaxCount, node.getIp(), node.getRemotePort())) {
            LoggerUtil.logger(nodeGroup.getChainId()).info("close!!! node={} status={}, version canConnectIn fail...cross={}", node.getId(), node.getStatus(), node.isCrossConnect());
            node.getChannel().close();
            return;
        }

        //判断对方节点是否可以连接，若不能连接，则直接断链，不加入到已连接列表里
        LoggerUtil.logger(nodeGroup.getChainId()).info("----是否检查对方网络节点可连接，reverseCheck:" + versionBody.getReverseCheck());
        if (versionBody.getReverseCheck() != 0) {
            if (!checkNodeCanConnect(node, versionBody.getAddrMe().getPort())) {
                LoggerUtil.logger(nodeGroup.getChainId()).info("close!!! 对方节点服务器连接验证失败！node={} status={}", node.getId(), node.getStatus());
                node.getChannel().close();

                return;
            }
        }

        node.setConnectStatus(NodeConnectStatusEnum.CONNECTED);
        nodesContainer.getConnectedNodes().put(node.getId(), node);
        nodesContainer.markCanuseNodeByIp(ip, NodeStatusEnum.AVAILABLE);
        //监听被动连接的断开
        node.setDisconnectListener(() -> {
            Log.info("~~~~~~~ in node {} disconnect ! status= {}", node.getId(), node.getStatus());

            if (node.isCrossConnect()) {
                nodeGroup.getCrossNodeContainer().getConnectedNodes().remove(node.getId());
                nodeGroup.getCrossNodeContainer().markCanuseNodeByIp(ip, NodeStatusEnum.CONNECTABLE);
            } else {

                nodeGroup.getLocalNetNodeContainer().getConnectedNodes().remove(node.getId());
                nodeGroup.getLocalNetNodeContainer().markCanuseNodeByIp(ip, NodeStatusEnum.CONNECTABLE);
            }
            BusinessGroupManager.getInstance().removeNode(ip);
        });
        //存储需要的信息,协议版本信息，远程跨链端口信息
        node.setVersionProtocolInfos(versionBody.getProtocolVersion(), versionBody.getBlockHeight(), versionBody.getBlockHash());
        //回复version
        VersionMessage versionMessage = MessageFactory.getInstance().buildVersionMessage(node, message.getHeader().getMagicNumber());
        LoggerUtil.logger(nodeGroup.getChainId()).info("rec node={} ver msg success.go response versionMessage..cross={}", node.getId(), node.isCrossConnect());
        LoggerUtil.logger(nodeGroup.getChainId()).info("--------------服务器回执握手连接，ip:" + versionMessage.getMsgBody().getAddrMe().getIp() + ", port:" + versionMessage.getMsgBody().getAddrMe().getPort());
        send(versionMessage, node, true);
    }

//
//    private boolean checkNodeCanConnect(Node node) {
//        Future<Boolean> res = discover.submit(new Callable<Node>() {
//            @Override
//            public Node call() {
//            }
//        });
//
//        try {
//            return res.get().booleanValue();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    private boolean checkNodeCanConnect(Node source, int port) {
        Node node = new Node(source.getNodeGroup().getMagicNumber(), source.getIp(), port, 0, Node.OUT, false);
        if (node == null) {
            return false;
        }
        node.setRemotePort(port);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        node.setConnectStatus(NodeConnectStatusEnum.CONNECTING);
        node.setConnectedListener(() -> {
            //探测可连接后，断开连接
            LoggerUtil.logger(node.getNodeGroup().getChainId()).info("close!!! checkNodeCanConnect node:{},connect success", node.getId());
            node.setConnectStatus(NodeConnectStatusEnum.CONNECTED);
            node.getChannel().close();
        });

        node.setDisconnectListener(() -> {
            LoggerUtil.logger(node.getNodeGroup().getChainId()).debug("checkNodeCanConnect node:{},disconnect,failCount={}", node.getId(), node.getFailCount());
            node.setChannel(null);
            if (node.getConnectStatus() == NodeConnectStatusEnum.CONNECTED) {
                //探测可连接
                node.setConnectStatus(NodeConnectStatusEnum.DISCONNECT);
                future.complete(true);
            } else {
                node.setConnectStatus(NodeConnectStatusEnum.FAIL);
                future.complete(false);
            }
        });

        boolean result = connectionManager.connection(node);
        if (!result) {
            return false;
        }
        try {
            return future.get();
        } catch (Exception e) {
            LoggerUtil.logger(node.getNodeGroup().getChainId()).error(e);
            return false;
        }
    }

    /**
     * client recieve handler
     *
     * @param message message
     * @param node    Node
     */
    private void clientRecieveHandler(BaseMessage message, Node node) {
        VersionMessageBody versionBody = (VersionMessageBody) message.getMsgBody();
        String myIp = versionBody.getAddrYou().getIp().getHostAddress();
        int myPort = versionBody.getAddrYou().getPort();
        //设置magicNumber
        node.setExternalIp(myIp);
        //client发出version后获得，得到server回复，建立握手
//       Log.debug("VersionMessageHandler Recieve:Client"+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        //存储需要的信息
        node.setVersionProtocolInfos(versionBody.getProtocolVersion(), versionBody.getBlockHeight(), versionBody.getBlockHash());
        node.setConnectStatus(NodeConnectStatusEnum.AVAILABLE);
        node.setFailCount(0);
        node.setConnectTime(TimeManager.currentTimeMillis());
        if (node.isCrossConnect()) {
            node.getNodeGroup().getCrossNodeContainer().setLatestHandshakeSuccTime(TimeManager.currentTimeMillis());
        } else {
            node.getNodeGroup().getLocalNetNodeContainer().setLatestHandshakeSuccTime(TimeManager.currentTimeMillis());
        }
        //client:接收到server端消息，进行verack答复
        VerackMessage verackMessage = MessageFactory.getInstance().buildVerackMessage(node, message.getHeader().getMagicNumber(), VerackMessageBody.VER_SUCCESS);
//        LoggerUtil.logger(node.getNodeGroup().getChainId()).info("rec node={} ver msg success.go response verackMessage..cross={}", node.getId(), node.isCrossConnect());
        MessageManager.getInstance().sendHandlerMsg(verackMessage, node, true);
        if (node.isSeedNode()) {
            //向种子节点请求地址
            MessageManager.getInstance().sendGetAddressMessage(node, false, false, true);
        }
    }

    /**
     * 接收消息处理
     * Receive message processing
     *
     * @param message message
     * @param node    Node
     * @return NetworkEventResult
     */
    @Override
    public NetworkEventResult recieve(BaseMessage message, Node node) {
        int chainId = NodeGroupManager.getInstance().getChainIdByMagicNum(message.getHeader().getMagicNumber());
//        LoggerUtil.logger(chainId).info("VersionMessageHandler recieve:" + (node.isServer() ? "Server" : "Client") + ":" + node.getIp() + ":" + node.getRemotePort() + "==CMD=" + message.getHeader().getCommandStr());
        if (Node.IN == node.getType()) {
            serverRecieveHandler(message, node);
        } else {
            clientRecieveHandler(message, node);
        }
        return NetworkEventResult.getResultSuccess();
    }

    @Override
    public NetworkEventResult send(BaseMessage message, Node node, boolean asyn) {
        int chainId = NodeGroupManager.getInstance().getChainIdByMagicNum(message.getHeader().getMagicNumber());
//        LoggerUtil.logger(chainId).info("VersionMessageHandler send:" + (node.isServer() ? "Server" : "Client") + ":" + node.getIp() + ":" + node.getRemotePort() + "==CMD=" + message.getHeader().getCommandStr());
        VersionMessage versionMessage = (VersionMessage) message;
        if (node.isCrossConnect()) {
            //跨链不需要区块信息，cross chain no request block info
            versionMessage.getMsgBody().setBlockHash("");
            versionMessage.getMsgBody().setBlockHeight(0);
        } else {
            BlockRpcService blockRpcService = SpringLiteContext.getBean(BlockRpcServiceImpl.class);
            BestBlockInfo bestBlockInfo = blockRpcService.getBestBlockHeader(chainId);
            versionMessage.getMsgBody().setBlockHash(bestBlockInfo.getHash());
            versionMessage.getMsgBody().setBlockHeight(bestBlockInfo.getBlockHeight());
        }
        return super.send(message, node, asyn);
    }
}
