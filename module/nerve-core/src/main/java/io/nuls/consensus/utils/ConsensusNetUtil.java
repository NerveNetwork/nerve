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
 * Trading tools
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
     * Has the consensus network of the current node been initialized? If it has not been initialized, it is necessary to cache the previously received connection information
     * ConsensusIdentitiesMsg
     */
    public static boolean consensusNetStatus = false;

    /**
     * Network link messages received before node initialization is completed
     * ConsensusIdentitiesMsg
     */
    public static final Set<ConsensusIdentityData> UNTREATED_MESSAGE_SET = new HashSet<>();


    public static Result getSuccess() {
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);
    }

    /**
     * Initialize consensus network
     *
     * @param chain           Chain information
     * @param packAddress     Block address
     * @param packAddressList Consensus node block address list
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
     * Nodes with consensus become non consensus nodes
     *
     * @param chain Chain information
     */
    public static void disConnectConsensusNet(Chain chain) {
        chain.getLogger().info("Node changes from consensus node to non consensus node, disconnecting consensus network");
        netService.cleanConsensusNetwork(chain.getChainId());
    }

    /**
     * Consensus Network Broadcast Message
     *
     * @param chainId      chainID
     * @param cmd          Calling commands
     * @param message      news
     * @param excludeNodes Excluded nodes
     */
    public static void broadcastInConsensus(int chainId, String cmd, String message, String excludeNodes) {
        netService.broadCastConsensusNetSync(chainId, cmd, message, excludeNodes);
    }
    public static void broadcastInConsensusHalf(int chainId, String cmd, String message, String excludeNodes) {
        netService.broadCastConsensusNetHalfSync(chainId, cmd, message, excludeNodes);
    }
}
