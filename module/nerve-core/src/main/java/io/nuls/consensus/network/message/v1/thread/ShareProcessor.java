package io.nuls.consensus.network.message.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.model.ConsensusKeys;
import io.nuls.consensus.network.model.ConsensusNet;
import io.nuls.consensus.network.model.message.ConsensusShareMsg;
import io.nuls.consensus.network.model.message.sub.ConsensusShare;

/**
 * @author Niels
 */
public class ShareProcessor extends BasicRunnable {
    private ConsensusNetService consensusNetService;

    public ShareProcessor(Chain chain) {
        super(chain);
        this.running = true;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                process();
            } catch (Throwable e) {
                log.error(e.getMessage());
            }
        }
    }

    private void process() throws Exception {
        if (null == consensusNetService) {
            consensusNetService = SpringLiteContext.getBean(ConsensusNetService.class);
        }

        ConsensusShareMsg message = chain.getConsensusCache().getShareMessageQueue().take();
        String nodeId = message.getNodeId();

        String msgHash = message.getMsgHash().toHex();
//        //chain.getLogger().debug("Share message,msgHash={} recv from node={}", msgHash, nodeId);
        try {
            //Verify signature
            if (!SignatureUtil.validateSignture(message.getIdentityList(), message.getSign())) {
                chain.getLogger().error("msgHash={} recv from node={} validateSignture false", msgHash, nodeId);
                return;
            }
        } catch (NulsException e) {
            chain.getLogger().error("msgHash={} recv from node={} error", msgHash, nodeId);
            chain.getLogger().error(e);
        }
        ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chain.getChainId());
        if (null == consensusKeys) {
            //Non consensus nodes
//            //chain.getLogger().debug("msgHash={} is not consensus node,drop msg", msgHash);
            return;
        } else {
            ConsensusShare consensusShare = message.getDecryptConsensusShare(chain, consensusKeys.getAddress());
            if (null == consensusShare) {
                return;
            }
            //Unsolved package,Need to determine if the other party has reached a consensus on the node
            ConsensusNet dbConsensusNet = consensusNetService.getConsensusNode(chain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddress(message.getSign().getPublicKey(), chain.getChainId())));
            if (null == dbConsensusNet) {
                //It should be noted that if the consensus node list does not include that node at this time, it may be misjudged, so it is necessary to ensure that When receiving the message, it already exists in the consensus list.
                chain.getLogger().error("nodeId = {} not in consensus Group", nodeId);
                return;
            }
//            chain.getLogger().info("Update consensus network information：");
            //Analyze Sharing address
            for (ConsensusNet consensusNet : consensusShare.getShareList()) {
                if (ArraysTool.arrayEquals(consensusKeys.getPubKey(), consensusNet.getPubKey())) {
                    continue;
                }
                consensusNet.setAddress(AddressTool.getStringAddressByBytes(AddressTool.getAddress(consensusNet.getPubKey(), chain.getChainId())));
//                chain.getLogger().info(consensusNet.getAddress() + ": " + consensusNet.getNodeId() + "(" + consensusNet.getFailTimes() + ")");
                consensusNetService.updateConsensusNode(chain, consensusNet);
            }

        }
    }
}
