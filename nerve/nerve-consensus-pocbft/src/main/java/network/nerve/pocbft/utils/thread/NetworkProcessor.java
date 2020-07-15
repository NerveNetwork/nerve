package network.nerve.pocbft.utils.thread;

import network.nerve.pocbft.model.bo.Chain;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.network.constant.NetworkCmdConstant;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.network.service.NetworkService;

import java.util.ArrayList;
import java.util.List;

/**
 * 监控网络状态
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
            //如果不是共识节点或则共识网络未组好则直接返回
            if (chain.isSynchronizedHeight()) {
                try {
                    ConsensusNetService consensusNetService = SpringLiteContext.getBean(ConsensusNetService.class);
                    updatePocGroup(consensusNetService, chain);
                    boolean isChange = consensusNetService.netStatusChange(chain);
                    netStatus = consensusNetService.getNetStatus(chain);
                    if (isChange) {
                        //通知回调
                        chain.getLogger().info("=====共识网络状态变更={}", netStatus);
                        chainManager.netWorkStateChange(chain, netStatus);
                    }
                    //有未连接peer，进行重连
                    processConnect(consensusNetService, chain);
                    //如果存在未连接的共识节点，进行自身地址重分享
                    processShareSelf(consensusNetService, chain);
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
        //获取网络模块上的共识网络IP组，进行共识模块的更新
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
                //连接通知
                //chain.getLogger().debug("已连接通知:node = {}",consensusNet.getNodeId());
                //给链接到的节点本节点的回执信息，用于对方节点将本节点设置为共识网络节点
                networkService.sendIdentityMessage(chain.getChainId(), consensusNet.getNodeId(), consensusNet.getPubKey(), true);
            }
        }
        if (ips.size() > 0) {
            networkService.addIps(chain.getChainId(), NetworkCmdConstant.NW_GROUP_FLAG, ips);
        }

    }

    public void processShareSelf(ConsensusNetService consensusNetService, Chain chain) {
        //广播身份消息
        NetworkService networkService = SpringLiteContext.getBean(NetworkService.class);
        if (consensusNetService.reShareSelf(chain)) {
            networkService.broadCastIdentityMsg(chain);
        }

    }
}
