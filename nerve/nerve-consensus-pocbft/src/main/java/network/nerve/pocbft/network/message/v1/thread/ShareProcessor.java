package network.nerve.pocbft.network.message.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.network.model.message.ConsensusShareMsg;
import network.nerve.pocbft.network.model.message.sub.ConsensusShare;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.v1.entity.BasicRunnable;

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
            //校验签名
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
            //非共识节点
//            //chain.getLogger().debug("msgHash={} is not consensus node,drop msg", msgHash);
            return;
        } else {
            ConsensusShare consensusShare = message.getDecryptConsensusShare(consensusKeys.getPrivKey(), consensusKeys.getPubKey());
            if (null == consensusShare) {
                return;
            }
            //解出的包,需要判断对方是否共识节点
            ConsensusNet dbConsensusNet = consensusNetService.getConsensusNode(chain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddress(message.getSign().getPublicKey(), chain.getChainId())));
            if (null == dbConsensusNet) {
                //这边需要注意，此时如果共识节点列表里面还没有该节点，可能就会误判，所以必须保障 在收到消息时候，共识列表里已经存在该消息。
                chain.getLogger().error("nodeId = {} not in consensus Group", nodeId);
                return;
            }
//            chain.getLogger().info("更新共识网络信息：");
            //进行解析 分享地址
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
