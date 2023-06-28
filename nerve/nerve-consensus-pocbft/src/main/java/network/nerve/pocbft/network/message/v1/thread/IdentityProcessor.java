package network.nerve.pocbft.network.message.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.constant.NetworkCmdConstant;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.network.service.NetworkService;
import network.nerve.pocbft.v1.entity.BasicRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static network.nerve.pocbft.network.constant.NetworkCmdConstant.POC_IDENTITY_MESSAGE;

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
            ConsensusNet consensusNet = message.getConsensusIdentitiesSub().getDecryptConsensusNet(chain, consensusKeys.getAddress(), consensusKeys.getPubKey());
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
            networkService.broadCastIdentityMsg(chain, POC_IDENTITY_MESSAGE, message.getMsgStr(), nodeId);
        }
    }
}
