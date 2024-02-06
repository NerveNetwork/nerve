package io.nuls.consensus.utils.thread;

import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.consensus.network.constant.NetworkCmdConstant;
import io.nuls.consensus.network.model.ConsensusNet;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.network.service.NetworkService;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitoring network status
 */
public class NetworkProcessor implements Runnable {
    private ChainManager chainManager = SpringLiteContext.getBean(ChainManager.class);
    private Chain chain;

    public NetworkProcessor(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (true) {
            boolean netStatus;
            //If it is not a consensus node or if the consensus network is not properly assembled, return directly
            if (chain.isSynchronizedHeight()) {
                try {
                    ConsensusNetService consensusNetService = SpringLiteContext.getBean(ConsensusNetService.class);
                    updatePocGroup(consensusNetService, chain);
                    boolean isChange = consensusNetService.netStatusChange(chain);
                    netStatus = consensusNetService.getNetStatus(chain);
                    if (isChange) {
                        //Notification callback
                        chain.getLogger().info("=====Consensus Network State Change={}", netStatus);
                        chainManager.netWorkStateChange(chain, netStatus);
                    }
                    //Not connectedpeer, perform reconnection
                    processConnect(consensusNetService, chain);
                    //If there are unconnected consensus nodes, perform self address re sharing

                    if (!chain.isNetworkStateOk()) {
                        processShareSelf(consensusNetService, chain);
                    }
                } catch (Exception e) {
                    chain.getLogger().error(e);
                }
            }
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                chain.getLogger().error(e);
            }
        }
    }

    public void updatePocGroup(ConsensusNetService consensusNetService, Chain chain) {
        NetworkService networkService = SpringLiteContext.getBean(NetworkService.class);
        //Obtain consensus networks on network modulesIPGroup, update consensus module
        List<String> list = networkService.getPocNodes(chain);
        if (null != list) {
            consensusNetService.reCalConsensusNet(chain, list);
        }
    }

    public void processConnect(ConsensusNetService consensusNetService, Chain chain) {
        NetworkService networkService = SpringLiteContext.getBean(NetworkService.class);
        List<ConsensusNet> unConnList = consensusNetService.getUnConnectConsensusNetList(chain);
        if (null == unConnList) {
            return;
        }
        List<String> ips = new ArrayList<>();
        for (ConsensusNet consensusNet : unConnList) {
            boolean result = networkService.connectPeer(chain.getChainId(), consensusNet.getNodeId());
            if (result) {
                consensusNet.setHadConnect(true);
                ips.add(consensusNet.getNodeId().split(":")[0]);
                //Connection notification
                //chain.getLogger().debug("Connected notification:node = {}",consensusNet.getNodeId());
                //Provide the receipt information of the node linked to this node, which is used by the other node to set this node as a consensus network node
                networkService.sendIdentityMessage(chain.getChainId(), consensusNet.getNodeId(), consensusNet.getPubKey(), true);
            }
        }
        if (ips.size() > 0) {
            networkService.addIps(chain.getChainId(), NetworkCmdConstant.NW_GROUP_FLAG, ips);
        }

    }

    public void processShareSelf(ConsensusNetService consensusNetService, Chain chain) {
        //Broadcast identity messages
        NetworkService networkService = SpringLiteContext.getBean(NetworkService.class);
        if (consensusNetService.reShareSelf(chain)) {
            networkService.broadCastIdentityMsg(chain);
        }

    }
}
