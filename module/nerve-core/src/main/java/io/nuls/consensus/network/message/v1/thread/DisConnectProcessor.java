package io.nuls.consensus.network.message.v1.thread;

import io.nuls.base.signture.SignatureUtil;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.v1.entity.BasicRunnable;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.model.ConsensusKeys;
import io.nuls.consensus.network.model.message.ConsensusIdentitiesMsg;

import java.io.IOException;

/**
 * @author Niels
 */
public class DisConnectProcessor extends BasicRunnable {
    private ConsensusNetService consensusNetService;

    public DisConnectProcessor(Chain chain) {
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
        ConsensusIdentitiesMsg message = chain.getConsensusCache().getDisConnectMessageQueue().take();
        String nodeId = message.getNodeId();
        int chainId = chain.getChainId();
        String msgHash = message.getMsgHash().toHex();
        //chain.getLogger().debug("DisConnect message,msgHash={} recv from node={}", msgHash, nodeId);
        try {
            //Verify signature
            if (!SignatureUtil.validateSignture(message.getConsensusIdentitiesSub().serialize(), message.getSign())) {
                chain.getLogger().error("msgHash={} recv pocDisConn from node={} validateSignture false", msgHash, nodeId);
                return;
            }
        } catch (NulsException e) {
            chain.getLogger().error("msgHash={} recv pocDisConn from node={} error", msgHash, nodeId);
            chain.getLogger().error(e);
        } catch (IOException e) {
            chain.getLogger().error("msgHash={} recv pocDisConn from node={} error", msgHash, nodeId);
            chain.getLogger().error(e);
        }
        //Obtain consensus information from oneself
        ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
        if (null == consensusKeys) {
            //Non consensus nodes, no need to handle
            //chain.getLogger().debug("=======Not a consensus node, not processed{}news", nodeId);
        } else {
            //Obtain the opponent's public key
            byte[] pubKey = message.getSign().getPublicKey();
            //clean upIP
            consensusNetService.disConnNode(chain, pubKey);
        }
    }
}
