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
package network.nerve.pocbft.network.message.v1;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.ConsensusIdentityData;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.network.service.NetworkService;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.pocbft.network.constant.NetworkCmdConstant;
import network.nerve.pocbft.network.service.ConsensusNetService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.pocbft.network.constant.NetworkCmdConstant.POC_IDENTITY_MESSAGE;


/**
 * 处理收到的{@link ConsensusIdentitiesMsg},解析身份
 *
 * @author lanjinsheng
 * @version 1.0
 * @date 2019-10-17
 */
@Component("ConsensusIdentityProcessorV1")
public class ConsensusIdentityProcessor implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;
    Map<String, Integer> MSG_HASH_MAP = new ConcurrentHashMap<>();
    int WARNING_HASH_SIZE = 10000;
    int MAX_HASH_SIZE = 12000;
    Map<String, Integer> MSG_HASH_MAP_TEMP = new ConcurrentHashMap<>();
    @Autowired
    ConsensusNetService consensusNetService;
    @Autowired
    NetworkService networkService;

    @Override
    public String getCmd() {
        return POC_IDENTITY_MESSAGE;
    }

    public String getLast8ByteByNulsHash(NulsHash hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return HexUtil.encode(out);
    }

    public boolean duplicateMsg(ConsensusIdentitiesMsg message) {
        String simpleHash = getLast8ByteByNulsHash(message.getMsgHash());
        if (null != MSG_HASH_MAP.get(simpleHash)) {
            return true;
        }
        if (MSG_HASH_MAP.size() > MAX_HASH_SIZE) {
            MSG_HASH_MAP.clear();
            MSG_HASH_MAP.putAll(MSG_HASH_MAP_TEMP);
            MSG_HASH_MAP_TEMP.clear();
        }
        if (MSG_HASH_MAP.size() > WARNING_HASH_SIZE) {
            MSG_HASH_MAP_TEMP.put(simpleHash, 1);
        }
        MSG_HASH_MAP.put(simpleHash, 1);
        return false;
    }

    @Override
    public void process(int chainId, String nodeId, String msgStr) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (!ConsensusNetUtil.consensusNetStatus && chain.isConsonsusNode()) {
            ConsensusNetUtil.UNTREATED_MESSAGE_SET.add(new ConsensusIdentityData(nodeId, msgStr));
            return;
        }
        ConsensusIdentitiesMsg message = RPCUtil.getInstanceRpcStr(msgStr, ConsensusIdentitiesMsg.class);
        if (message == null || duplicateMsg(message)) {
            return;
        }
        String msgHash = message.getMsgHash().toHex();
//        //chain.getLogger().debug("Identity message,msgHash={} recv from node={}", msgHash, nodeId);
        try {
            //校验签名
            if (!SignatureUtil.validateSignture(message.getConsensusIdentitiesSub().serialize(), message.getSign())) {
                chain.getLogger().error("Identity message,msgHash={} recv from node={} validateSignture false", msgHash, nodeId);
                return;
            }
        } catch (NulsException | IOException e) {
            chain.getLogger().error(e);
            return;
        }
        /*
        接受身份信息，判断是否有自己的包，有解析，1.解析后判断是否在自己连接列表内，存在则跃迁，不存在进行第三步,同时 广播转发/ 普通节点直接转发
        */
        ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
        if (null == consensusKeys) {
            //只需要转发消息
//            //chain.getLogger().debug("=======不是共识节点，只转发{}消息", nodeId);
        } else {
            //如果为当前节点签名消息则直接返回
            String signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(message.getSign().getPublicKey(), chainId));
            if (signAddress.equals(consensusKeys.getAddress())) {
                return;
            }
            //如果无法解密直接返回
            ConsensusNet consensusNet = message.getConsensusIdentitiesSub().getDecryptConsensusNet(consensusKeys.getPrivKey(), consensusKeys.getPubKey());
            if (null == consensusNet) {
//                chain.getLogger().error("=======无法解密消息，返回！", nodeId);
                return;
            }
            if (StringUtils.isBlank(consensusNet.getAddress())) {
                consensusNet.setAddress(AddressTool.getStringAddressByBytes(AddressTool.getAddress(consensusNet.getPubKey(), chainId)));
            }
            //解出的包,需要判断对方是否共识节点
            ConsensusNet dbConsensusNet = consensusNetService.getConsensusNode(chainId, consensusNet.getAddress());
            if (null == dbConsensusNet) {
                //这边需要注意，此时如果共识节点列表里面还没有该节点，可能就会误判，所以必须保障 在收到消息时候，共识列表里已经存在该消息。
                chain.getLogger().error("nodeId = {} not in consensus Group", consensusNet.getNodeId());
                return;
            }

            //可能没公钥，更新下公钥信息
            String consensusNetNodeId = consensusNet.getNodeId();

            //每次从网络模块查询，是为了避免某节点断开然后重连导致本地连接缓存信息失效
            boolean isConnect = networkService.connectPeer(chainId, consensusNetNodeId);
            if (!isConnect) {
                chain.getLogger().warn("connect fail .nodeId = {}", consensusNet.getNodeId());
            } else {
//                    //chain.getLogger().debug("connect {} success", consensusNetNodeId);
                dbConsensusNet.setNodeId(consensusNetNodeId);
                dbConsensusNet.setPubKey(consensusNet.getPubKey());
                dbConsensusNet.setHadConnect(true);
                List<String> ips = new ArrayList<>();
                ips.add(consensusNetNodeId.split(":")[0]);
                networkService.addIps(chainId, NetworkCmdConstant.NW_GROUP_FLAG, ips);
            }
            //分享所有已连接共识信息给对端
            networkService.sendShareMessage(chainId, consensusNetNodeId, consensusNet.getPubKey());

            //如果为新节点消息则需要，如果为其他链接节点的回执信息则不需要
            if (message.getConsensusIdentitiesSub().isBroadcast()) {
                //同时分享新增的连接信息给其他已连接节点
                networkService.sendShareMessageExNode(chainId, consensusNetNodeId, consensusNet);
            }
            //如果为新节点消息则需要将本节点的身份信息回执给对方（用于对方节点将本节点设置为共识网络节点）
            if (message.getConsensusIdentitiesSub().isBroadcast()) {
                chain.getLogger().info("begin broadCastIdentityMsg to={} success", nodeId);
                networkService.sendIdentityMessage(chainId, consensusNet.getNodeId(), consensusNet.getPubKey());
            }
        }
        //如果为新节点消息则需要转发，如果为其他链接节点的回执信息则不需要广播
        if (message.getConsensusIdentitiesSub().isBroadcast()) {
//            chain.getLogger().info("begin broadCastIdentityMsg exclude={} success", nodeId);
            networkService.broadCastIdentityMsg(chain, getCmd(), msgStr, nodeId);
        }
        //chain.getLogger().debug("=====================consensusIdentityProcessor deal end");
    }
}
