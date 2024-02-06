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
package io.nuls.consensus.network.model;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.utils.RoundUtils;
import io.nuls.core.log.Log;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.model.StringUtils;
import io.nuls.consensus.model.dto.Node;
import io.nuls.consensus.rpc.call.NetWorkCall;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description
 */
public class ConsensusNetGroup {
    boolean available = false;
    private int chainId;
    /**
     * KEY Using address
     */
    private Map<String, ConsensusNet> group = new ConcurrentHashMap<>();

    //Under the old logic, added plugins
    private Map<String, String> addrNodeMap = new HashMap<>();

    private boolean allConnected = false;

    private int needChangeToFalse = 0;

    private RoundController roundController;

    public ConsensusNetGroup(int chainId) {
        this.chainId = chainId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public Map<String, ConsensusNet> getGroup() {
        return group;
    }

    public void setGroup(Map<String, ConsensusNet> group) {
        this.group = group;
    }

    /**
     * If the current node is connected to all unconnected nodes after startup, it indicates that the consensus network of the node has been successfully established
     * Afterwards, it will no longer broadcast its own node information to other newly added consensus nodes
     */
    public boolean isAllConnected() {
        if (!allConnected) {
            for (ConsensusNet consensusNet : group.values()) {
                if (!consensusNet.isHadConnect()) {
                    return false;
                }
            }
        }
        this.allConnected = true;
        return true;
    }

    public void setAllConnected(boolean allConnected) {
        this.allConnected = allConnected;
    }

    public void addConsensus(ConsensusNet consensusNet) {
        if (StringUtils.isBlank(consensusNet.getAddress())) {
            consensusNet.setAddress(AddressTool.getStringAddressByBytes(AddressTool.getAddress(consensusNet.getPubKey(), chainId)));
        }
        if (StringUtils.isNotBlank(consensusNet.getNodeId())) {
            this.addrNodeMap.put(consensusNet.getAddress(), consensusNet.getNodeId());
        }
        group.put(consensusNet.getAddress(), consensusNet);

    }

    public List<ConsensusNet> getConsensusHadConnectNetList() {
        List<ConsensusNet> list = new ArrayList<>();
        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
            if (null != entry.getValue().getNodeId() && entry.getValue().isHadConnect()) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    public List<ConsensusNet> getAllConsensusConnectNetList() {
        List<ConsensusNet> list = new ArrayList<>();
        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
            if (null != entry.getValue().getPubKey()) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    public List<ConsensusNet> getUnConnectConsensusNetList() {
        List<ConsensusNet> list = new ArrayList<>();
        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
            if (null != entry.getValue().getNodeId() && !entry.getValue().isHadConnect()) {
                //Nodes allowed for reconnection
                if (entry.getValue().getFailTimes() < ConsensusConstant.POC_CONNECT_MAX_FAIL_TIMES) {
                    list.add(entry.getValue());
                }
            }
        }
        return list;
    }

    public boolean hadNullNodeId() {
        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
            if (null == entry.getValue().getNodeId()) {
                return true;
            }
        }
        return false;
    }

    public ConsensusNet getConsensusNet(String address) {
        return group.get(address);
    }

    public ConsensusNet getConsensusNet(byte[] pubKey) {
        String address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(pubKey, chainId));
        return group.get(address);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean statusChange(Chain chain) {
        int total = group.size();
        int hadConnect = 0;
        boolean netAvailable = false;
        List<Node> nodeList = NetWorkCall.getAvailableNodes(chain.getChainId());
        Set<String> connectedIps = getConnectedIps(nodeList);
        if (null == roundController) {
            this.roundController = RoundUtils.getRoundController();
        }
        MeetingRound round = roundController.getCurrentRound();
        if (null == round) {
            round = roundController.tempRound();
        }
        for (String addr : round.getMemberAddressSet()) {
            String nodeId = addrNodeMap.get(addr);
            if (isConnected(nodeId, connectedIps)) {
                hadConnect++;
            }
        }
        int percent = ConsensusConstant.POC_NETWORK_NODE_PERCENT;

        if (total > 0) {
            //Add own nodes
            total = total + 1;
            hadConnect = hadConnect + 1;
            int connectPercent = (hadConnect * 100 / total);
            if (connectPercent >= percent) {
                netAvailable = true;
                needChangeToFalse = 0;
            }
        } else {
            //Single node direct return
            netAvailable = true;
            needChangeToFalse = 0;
        }
        if (netAvailable == available) {
//            chain.getLogger().info("ConsensusNet:{}/{}=={} !availNetNode:{}", hadConnect, total, available, nodeList.size());
            return false;
        } else {
            if (available && needChangeToFalse < 3) {
                needChangeToFalse++;
                return false;
            }
            chain.getLogger().info("net state  change total={} hadConnect={},{}==to=={}", total, hadConnect, available, netAvailable);
            StringBuilder ss = new StringBuilder("network nodes: ");
            for (String ip : connectedIps) {
                ss.append("\n");
                ss.append(ip);
            }
            ss.append("consensus nodes:");

            for (Map.Entry<String, String> entry : addrNodeMap.entrySet()) {
                ss.append("\n");
                ss.append(entry.getValue());
                ss.append(":::");
                ss.append(entry.getKey());
            }
            chain.getLogger().debug(ss.toString());
            available = netAvailable;
            return true;
        }
    }

    private boolean isConnected(String nodeId, Set<String> connectedIps) {
        if (StringUtils.isBlank(nodeId)) {
            return false;
        }
        String nodeIp = nodeId.split(":")[0];
        return connectedIps.contains(nodeIp);
    }

    private Set<String> getConnectedIps(List<Node> nodeList) {
        Set<String> list = new HashSet<>();
        for (Node node : nodeList) {
            list.add(node.getId().split(":")[0]);
        }
        return list;
    }

    public List<String> getConsensusNetIps() {
        List<String> ips = new ArrayList<>();
        List<String> nodeIds = new ArrayList<>(this.addrNodeMap.values());
        for (String val : nodeIds) {
            ips.add(val.split(":")[0]);
        }
//        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
//            if (null != entry.getValue().getNodeId()) {
//                try {
//                    ips.add(entry.getValue().getNodeId().split(":")[0]);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        return ips;
    }

    public void reCalConsensusNet(Map<String, Integer> map) {
        boolean allDisconnect = false;
        if (map.size() == 0) {
            allDisconnect = true;
        }
        for (Map.Entry<String, ConsensusNet> entry : group.entrySet()) {
            if (null != entry.getValue().getNodeId()) {
                try {
                    if (allDisconnect) {
                        //All connections unavailable
                        Log.info("set all nodes disconnect.All connections unavailable");
                        entry.getValue().setHadConnect(false);
                    } else if (null == map.get(entry.getValue().getNodeId().split(":")[0])) {
                        //Part of the connections are unavailable. If the number of attempts exceeds the limit, it will be directly terminatedIP
                        if (entry.getValue().getFailTimes() >= ConsensusConstant.POC_CONNECT_MAX_FAIL_TIMES) {
//                            Log.info("set nodeId null.Exceeded the number of connection failures");
                            entry.getValue().setNodeId(null);
                        } else {
                            entry.getValue().setFailTimes((entry.getValue().getFailTimes() + 1));
                        }
                        entry.getValue().setHadConnect(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String removeConsensus(String address) {
        ConsensusNet consensusNet = group.get(address);
        if (null == consensusNet) {
            return null;
        }
        group.remove(address);
        return consensusNet.getNodeId();
    }

    public void updateAddrNodeMap(String address, String nodeId) {
        if (StringUtils.isBlank(address) || StringUtils.isBlank(nodeId)) {
            return;
        }
        this.addrNodeMap.put(address, nodeId);
    }
}
