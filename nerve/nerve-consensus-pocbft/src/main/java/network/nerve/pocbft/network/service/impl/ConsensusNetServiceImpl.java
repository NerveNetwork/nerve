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
package network.nerve.pocbft.network.service.impl;
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

import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Request;
import io.nuls.core.thread.ThreadUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.constant.NetworkCmdConstant;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.model.ConsensusNetGroup;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.network.service.NetworkService;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.manager.ThreadManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description
 */
@Component
public class ConsensusNetServiceImpl implements ConsensusNetService {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ThreadManager threadManager;
    @Autowired
    NetworkService networkService;
    /**
     * 共识网络信息
     * key:链ID
     * value:链对应的共识网络组节点信息
     */
    static Map<Integer, ConsensusNetGroup> GROUPS_MAP = new ConcurrentHashMap<>();

    static Map<Integer, Boolean> NETTHREAD_MAP = new ConcurrentHashMap<>();
    /**
     * 自身公私钥信息
     * key：链ID
     * value：链指定的本节点信息
     */
    static Map<Integer, ConsensusKeys> CONSENSUSKEYS_MAP = new ConcurrentHashMap<>();

    static Map<String, Integer> LATEST_CONSENSUS_IPS_MAP = new ConcurrentHashMap<>();

    static final short ADD_CONSENSUS = 1;
    static final short DEL_CONSENSUS = 2;

    @Override
    public boolean netStatusChange(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            return group.statusChange(chain);
        }
        return false;
    }

    @Override
    public boolean reCalConsensusNet(Chain chain, List<String> ips) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            //发送消息获取到的网络信息进行状态更新处理
            LATEST_CONSENSUS_IPS_MAP.clear();
            for (String ip : ips) {
                LATEST_CONSENSUS_IPS_MAP.put(ip, 1);
            }
            group.reCalConsensusNet(LATEST_CONSENSUS_IPS_MAP);
        }
        return true;
    }

    @Override
    public boolean getNetStatus(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            return group.isAvailable();
        }
        return false;
    }

    @Override
    public List<ConsensusNet> getHadConnConsensusNetList(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            return group.getConsensusHadConnectNetList();
        }
        return null;
    }

    @Override
    public List<ConsensusNet> getAllConsensusNetList(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            return group.getAllConsensusConnectNetList();
        }
        return null;
    }

    @Override
    public List<ConsensusNet> getUnConnectConsensusNetList(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group) {
            return group.getUnConnectConsensusNetList();
        }
        return null;
    }

    @Override
    public boolean reShareSelf(Chain chain) {
        ConsensusNetGroup group = GROUPS_MAP.get(chain.getChainId());
        if (null != group && group.getGroup().size() > 0) {
            return group.hadNullNodeId();
        }
        return false;
    }

    @Override
    public boolean allConnected(Chain chain) {
        return GROUPS_MAP.get(chain.getChainId()).isAllConnected();
    }

    @Override
    public boolean createConsensusNetwork(int chainId, byte[] selfPubKey, byte[] selfPrivKey, List<byte[]> consensusSeedPubKeyList, Set<String> consensusAddrList) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (selfPubKey == null || selfPrivKey == null) {
            chain.getLogger().error("=======================createConsensusNetwork error. self Pub or priv is null");
            return false;
        }
        chain.getLogger().info("开始组建共识网络");
        ConsensusNetGroup group = GROUPS_MAP.computeIfAbsent(chainId, val -> new ConsensusNetGroup(chainId));


        ConsensusKeys consensusKeys = new ConsensusKeys(selfPubKey, selfPrivKey, chainId);
        String nodeId = networkService.getSelfNodeId(chainId);
        //chain.getLogger().debug("=======================createConsensusNetwork,self nodeId:{}", nodeId);
        ConsensusNet selfConsensusNet = new ConsensusNet();
        selfConsensusNet.setPubKey(consensusKeys.getPubKey());
        selfConsensusNet.setNodeId(nodeId);
        ConsensusIdentitiesMsg consensusIdentitiesMsg = new ConsensusIdentitiesMsg(selfConsensusNet);
        consensusIdentitiesMsg.getConsensusIdentitiesSub().setBroadcast(true);
        if (null != consensusSeedPubKeyList) {
            for (byte[] pubKey : consensusSeedPubKeyList) {
                if (!ArraysTool.arrayEquals(pubKey, selfPubKey) && pubKey.length > 0) {
                    ConsensusNet consensusNet = new ConsensusNet(pubKey, null, chainId);
                    group.addConsensus(consensusNet);
                    try {
                        consensusIdentitiesMsg.addEncryptNodes(pubKey);
                    } catch (Exception e) {
                        chain.getLogger().error(e);
                        return false;
                    }
                }
            }
        }
        String selfAddr = consensusKeys.getAddress();
        if (null != consensusAddrList) {
            for (String addr : consensusAddrList) {
                if (!addr.equals(selfAddr) && StringUtils.isNotBlank(addr) && null == group.getConsensusNet(addr)) {
                    ConsensusNet consensusNet = new ConsensusNet(addr, null);
                    group.addConsensus(consensusNet);
                }
            }
        }
        GROUPS_MAP.put(chainId, group);
        CONSENSUSKEYS_MAP.put(chainId, consensusKeys);
        //广播身份消息
        try {
            if (StringUtils.isNotBlank(selfConsensusNet.getNodeId())) {
                consensusIdentitiesMsg.signDatas(consensusKeys.getPrivKey());
                networkService.broadCastIdentityMsg(chain, NetworkCmdConstant.POC_IDENTITY_MESSAGE,
                        HexUtil.encode(consensusIdentitiesMsg.serialize()), null);
            }
        } catch (IOException e) {
            chain.getLogger().error(e);
            return false;
        }
        if (null == NETTHREAD_MAP.get(chainId)) {
            NETTHREAD_MAP.put(chainId, true);
            threadManager.createConsensusNetThread(chain);
        } else if (!NETTHREAD_MAP.get(chainId)) {
            NETTHREAD_MAP.put(chainId, true);
        }
        return true;
    }

    @Override
    public boolean updateConsensusList(int chainId, Set<String> consensusAddrList) {

        if (null == consensusAddrList) {
            return true;
        }

        ThreadUtils.asynExecuteRunnable(new Runnable() {
            @Override
            public void run() {
                Chain chain = chainManager.getChainMap().get(chainId);
                ConsensusKeys consensusKeys = getSelfConsensusKeys(chainId);
                if (null == consensusKeys) {
                    return;
                }
                String selfAddr = consensusKeys.getAddress();
                ConsensusNetGroup consensusNetGroup = GROUPS_MAP.get(chainId);
                Map<String, Integer> map = new HashMap();
                for (String addr : consensusAddrList) {
                    if (StringUtils.isNotBlank(addr) && !addr.equals(selfAddr)) {
                        ConsensusNet consensusNet = consensusNetGroup.getConsensusNet(addr);
                        if (null == consensusNet) {
                            consensusNet = new ConsensusNet(addr, null);
                            consensusNetGroup.addConsensus(consensusNet);
                        }
                        map.put(addr, 1);
                    }
                }
                Map<String, ConsensusNet> groupMap = consensusNetGroup.getGroup();
                List<String> removeList = new ArrayList<>();
                for (Map.Entry<String, ConsensusNet> entry : groupMap.entrySet()) {
                    if (null == map.get(entry.getValue().getAddress())) {
                        removeList.add(entry.getValue().getAddress());
                    }
                }
                for (String removeAddr : removeList) {
                    //移除共识
                    String nodeId = consensusNetGroup.removeConsensus(removeAddr);
                    chain.getLogger().info("remove node={} from consensus net", nodeId);
                    if (null != nodeId) {
                        List<String> ips = new ArrayList<>();
                        ips.add(nodeId.split(":")[0]);
                        networkService.removeIps(chainId, NetworkCmdConstant.NW_GROUP_FLAG, ips);
                    }
                }
            }
        });

        return true;
    }

    @Override
    public void cleanConsensusNetwork(int chainId) {
        Chain chain = chainManager.getChainMap().get(chainId);
        chain.getLogger().info("清理共识网络连接");
        //发送移除消息
        if (!GROUPS_MAP.isEmpty()) {
            networkService.sendDisConnectMessage(chain, GROUPS_MAP.get(chainId).getConsensusNetIps());
        }
        //移除自身信息
        GROUPS_MAP.remove(chainId);
        CONSENSUSKEYS_MAP.remove(chainId);
        NETTHREAD_MAP.put(chainId, false);
    }

    @Override
    public ConsensusKeys getSelfConsensusKeys(int chainId) {
        return CONSENSUSKEYS_MAP.get(chainId);
    }

    @Override
    public ConsensusNet getConsensusNode(int chainId, String address) {
        ConsensusNetGroup consensusNetGroup = GROUPS_MAP.get(chainId);
        if (null == consensusNetGroup) {
            return null;
        }
        return consensusNetGroup.getConsensusNet(address);
    }


    @Override
    public boolean updateConsensusNode(int chainId, ConsensusNet consensusNet, boolean isConnect) {
        ConsensusNetGroup consensusNetGroup = GROUPS_MAP.get(chainId);
        ConsensusNet consensusNet1 = consensusNetGroup.getConsensusNet(consensusNet.getPubKey());
        consensusNet1.setNodeId(consensusNet.getNodeId());
        consensusNet1.setPubKey(consensusNet.getPubKey());
        consensusNet1.setHadConnect(isConnect);
        consensusNet1.setFailTimes(0);
        return true;
    }

    @Override
    public boolean updateConsensusNode(Chain chain, ConsensusNet consensusNet) {

        ConsensusNetGroup consensusNetGroup = GROUPS_MAP.get(chain.getChainId());
        consensusNetGroup.updateAddrNodeMap(consensusNet.getAddress(), consensusNet.getNodeId());
        ConsensusNet consensusNet1 = consensusNetGroup.getConsensusNet(consensusNet.getPubKey());
        if (null == consensusNet1) {
            return false;
        } else {
            if (StringUtils.isBlank(consensusNet1.getNodeId())) {
                //chain.getLogger().debug("update ip peer={}", consensusNet.getNodeId());
                consensusNet1.setNodeId(consensusNet.getNodeId());
            }
            if (null == consensusNet1.getPubKey()) {
                //chain.getLogger().debug("update pub peer={}", consensusNet.getNodeId());
                consensusNet1.setPubKey(consensusNet.getPubKey());
            }
        }
        return true;
    }

    @Override
    public boolean disConnNode(Chain chain, byte[] pubKey) {
        ConsensusNetGroup consensusNetGroup = GROUPS_MAP.get(chain.getChainId());
        ConsensusNet consensusNet1 = consensusNetGroup.getConsensusNet(pubKey);
        if (null == consensusNet1) {
            return false;
        }
        //通知网络层
        //清除对应公钥的共识IP
        if (StringUtils.isBlank(consensusNet1.getNodeId())) {
            return false;
        }
        List<String> ips = new ArrayList<>();
        ips.add(consensusNet1.getNodeId().split(":")[0]);
        Log.info("remove node:" + consensusNet1.getNodeId() + ", " + consensusNet1.getAddress());
        networkService.removeIps(chain.getChainId(), NetworkCmdConstant.NW_GROUP_FLAG, ips);
        consensusNet1.setNodeId(null);
        return true;
    }

    /**
     * 广播共识消息返回已发送连接列表
     *
     * @param chainId
     * @param cmd
     * @param messageBodyHex
     * @return
     */
    @Override
    public List<String> broadCastConsensusNet(int chainId, String cmd, String messageBodyHex, String excludeNodes) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(6);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            if (GROUPS_MAP.get(chainId) == null) {
                return null;
            }
            params.put("ips", GROUPS_MAP.get(chainId).getConsensusNetIps());
            params.put("messageBody", messageBodyHex);
            params.put("command", cmd);
            params.put("isForward",false);
            params.put("excludeNodes", excludeNodes);

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params);
            //chain.getLogger().debug("broadcast: " + cmd + ", success:" + response.isSuccess());
            if (response.isSuccess()) {
                List<String> ips = (List) ((Map) ((Map) response.getResponseData()).get(NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET)).get("list");
                return ips;
            }
        } catch (Exception e) {
            chain.getLogger().error("", e);
        }
        return null;
    }

    /**
     * 异步广播共识消息
     *
     * @param chainId
     * @param cmd
     * @param messageBodyHex
     * @return
     */
    @Override
    public boolean broadCastConsensusNetHalfSync(int chainId, String cmd, String messageBodyHex, String excludeNodes) {
        ConsensusNetGroup group = GROUPS_MAP.get(chainId);
        if (null == group) {
            return false;
        }

        List<String> ips = group.getConsensusNetIps();
        int size = ips.size() / 2;
        if (ips.size() > 20) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(ips.remove(new Random().nextInt(ips.size())));
            }
            ips = list;
        }
        return realBroadCastConsensusNetSync(chainId, cmd, messageBodyHex, excludeNodes, ips);
    }

    public boolean broadCastConsensusNetSync(int chainId, String cmd, String messageBodyHex, String excludeNodes) {
        ConsensusNetGroup group = GROUPS_MAP.get(chainId);
        if (null == group) {
            return false;
        }
        return realBroadCastConsensusNetSync(chainId, cmd, messageBodyHex, excludeNodes, group.getConsensusNetIps());
    }

    private boolean realBroadCastConsensusNetSync(int chainId, String cmd, String messageBodyHex, String excludeNodes, List<String> ips) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(6);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            if (GROUPS_MAP.get(chainId) == null) {
                return false;
            }
            params.put("ips", ips);
            params.put("messageBody", messageBodyHex);
            params.put("command", cmd);
            params.put("excludeNodes", excludeNodes);

            Request request = MessageUtil.newRequest(NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params, Constants.BOOLEAN_FALSE, Constants.ZERO, Constants.ZERO);
            String messageId = ResponseMessageProcessor.requestOnly(ModuleE.NW.abbr, request);
            return messageId.equals("0") ? false : true;
        } catch (Exception e) {
            chain.getLogger().error("", e);
        }
        return true;
    }

}
