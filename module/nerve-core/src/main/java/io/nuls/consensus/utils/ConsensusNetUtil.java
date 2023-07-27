package io.nuls.consensus.utils;

import io.nuls.consensus.utils.thread.CacheConsensusIdentityProcessor;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.ConsensusIdentityData;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.rpc.call.CallMethodUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 交易工具类
 * Transaction Tool Class
 *
 * @author tag
 * 2019/7/25
 */
@Component
public class ConsensusNetUtil {
    @Autowired
    private static ConsensusNetService netService;

    private static boolean initRoundThreadStarted = false;

    /**
     * 当前节点共识网络是否已初始化完成，如果未初始化完成，则需要缓存之前收到的连接信息
     * ConsensusIdentitiesMsg
     */
    public static boolean consensusNetStatus = false;

    /**
     * 节点初始化完成之前接收到的网络链接消息
     * ConsensusIdentitiesMsg
     */
    public static final Set<ConsensusIdentityData> UNTREATED_MESSAGE_SET = new HashSet<>();


    public static Result getSuccess() {
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);
    }

    /**
     * 初始化共识网络
     *
     * @param chain           链信息
     * @param packAddress     出块地址
     * @param packAddressList 共识节点出块地址列表
     */
    public static void initConsensusNet(Chain chain, String packAddress, Set<String> packAddressList) {
        try {
            String pubKey = CallMethodUtils.getPublicKey(chain.getChainId(), packAddress, chain.getConfig().getPassword());
            netService.createConsensusNetwork(chain.getChainId(), HexUtil.decode(pubKey),  chain.getSeedNodePubKeyList(), packAddressList);
            if (!consensusNetStatus) {
                consensusNetStatus = true;
                ThreadUtils.createAndRunThread("Cache-ConsensusIdentity_message", new CacheConsensusIdentityProcessor(chain), false);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 节点有共识节点变为非共识节点
     *
     * @param chain 链信息
     */
    public static void disConnectConsensusNet(Chain chain) {
        chain.getLogger().info("节点由共识节点变为非共识节点，断开共识网络");
        netService.cleanConsensusNetwork(chain.getChainId());
    }

    /**
     * 共识网络广播消息
     *
     * @param chainId      链ID
     * @param cmd          调用命令
     * @param message      消息
     * @param excludeNodes 排除的节点
     */
    public static void broadcastInConsensus(int chainId, String cmd, String message, String excludeNodes) {
        netService.broadCastConsensusNetSync(chainId, cmd, message, excludeNodes);
    }
    public static void broadcastInConsensusHalf(int chainId, String cmd, String message, String excludeNodes) {
        netService.broadCastConsensusNetHalfSync(chainId, cmd, message, excludeNodes);
    }
}
