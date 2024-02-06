package io.nuls.consensus.network.message.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.consensus.network.constant.NetworkCmdConstant;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.network.service.NetworkService;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.model.ConsensusKeys;
import io.nuls.consensus.network.model.ConsensusNet;
import io.nuls.consensus.network.model.message.ConsensusIdentitiesMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class IdentityProcessor extends BasicRunnable {
    private ConsensusNetService consensusNetService;
    private NetworkService networkService;

    public IdentityProcessor(Chain chain) {
        super(chain);
        this.running = true;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                process();
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    private void process() throws Exception {
        if (null == consensusNetService) {
            consensusNetService = SpringLiteContext.getBean(ConsensusNetService.class);
        }
        if (null == networkService) {
            networkService = SpringLiteContext.getBean(NetworkService.class);
        }
        ConsensusIdentitiesMsg message = chain.getConsensusCache().getIdentityMessageQueue().take();
        String nodeId = message.getNodeId();
        int chainId = chain.getChainId();

        String msgHash = message.getMsgHash().toHex();
//        //chain.getLogger().debug("Identity message,msgHash={} recv from node={}", msgHash, nodeId);
        try {
            //Verify signature
            if (!SignatureUtil.validateSignture(message.getConsensusIdentitiesSub().serialize(), message.getSign())) {
                chain.getLogger().error("Identity message,msgHash={} recv from node={} validateSignture false", msgHash, nodeId);
                return;
            }
        } catch (NulsException | IOException e) {
            chain.getLogger().error(e);
            return;
        }
        /*
        Accept identity information, determine if there is your own package, have parsing,1.After parsing, determine if it is in your own connection list. If it exists, jump. If it does not exist, proceed to the third step,meanwhile Broadcast forwarding/ Direct forwarding by ordinary nodes
        */
        ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
        if (null == consensusKeys) {
            //Just need to forward the message
//            //chain.getLogger().debug("=======Not a consensus node, only forwarding{}news", nodeId);
        } else {
            //If the message is signed for the current node, it will be returned directly
            String signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(message.getSign().getPublicKey(), chainId));
            if (signAddress.equals(consensusKeys.getAddress())) {
                return;
            }
            //If decryption is not possible, return directly
            ConsensusNet consensusNet = message.getConsensusIdentitiesSub().getDecryptConsensusNet(chain, consensusKeys.getAddress(), consensusKeys.getPubKey());
            if (null == consensusNet) {
//                chain.getLogger().error("=======Unable to decrypt message, returning！", nodeId);
                return;
            }
            if (StringUtils.isBlank(consensusNet.getAddress())) {
                consensusNet.setAddress(AddressTool.getStringAddressByBytes(AddressTool.getAddress(consensusNet.getPubKey(), chainId)));
            }
            //Unsolved package,Need to determine if the other party has reached a consensus on the node
            ConsensusNet dbConsensusNet = consensusNetService.getConsensusNode(chainId, consensusNet.getAddress());
            if (null == dbConsensusNet) {
                //It should be noted that if the consensus node list does not include that node at this time, it may be misjudged, so it is necessary to ensure that When receiving the message, it already exists in the consensus list.
                chain.getLogger().error("nodeId = {} not in consensus Group", consensusNet.getNodeId());
                return;
            }

            //Maybe there is no public key, update the public key information
            String consensusNetNodeId = consensusNet.getNodeId();

            //Every time a query is made from the network module, it is to avoid a node being disconnected and reconnected, which may cause the local connection cache information to become invalid
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
            //Share all connected consensus information with the counterpart
            networkService.sendShareMessage(chainId, consensusNetNodeId, consensusNet.getPubKey());

            //If it is a new node message, it is required. If it is a receipt information for other linked nodes, it is not required
            if (message.getConsensusIdentitiesSub().isBroadcast()) {
                //Simultaneously share the newly added connection information with other connected nodes
                networkService.sendShareMessageExNode(chainId, consensusNetNodeId, consensusNet);
            }
            //If it is a new node message, it is necessary to receive a receipt of the identity information of this node to the other party（Used for the other node to set this node as a consensus network node）
            if (message.getConsensusIdentitiesSub().isBroadcast()) {
                chain.getLogger().info("begin broadCastIdentityMsg to={} success", nodeId);
                networkService.sendIdentityMessage(chainId, consensusNet.getNodeId(), consensusNet.getPubKey());
            }
        }
        //If it is a new node message, it needs to be forwarded. If it is a receipt information from another linked node, it does not need to be broadcasted
        if (message.getConsensusIdentitiesSub().isBroadcast()) {
//            chain.getLogger().info("begin broadCastIdentityMsg exclude={} success", nodeId);
            networkService.broadCastIdentityMsg(chain, NetworkCmdConstant.POC_IDENTITY_MESSAGE, message.getMsgStr(), nodeId);
        }
    }
}
