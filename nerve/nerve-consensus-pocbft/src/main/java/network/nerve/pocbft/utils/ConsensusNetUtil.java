package network.nerve.pocbft.utils;

import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.thread.ThreadUtils;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.ConsensusIdentityData;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.thread.CacheConsensusIdentityProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static network.nerve.pocbft.constant.ParameterConstant.PARAM_PRI_KEY;
import static network.nerve.pocbft.constant.ParameterConstant.PARAM_PUB_KEY;

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
            HashMap callResult = CallMethodUtils.accountValid(chain.getChainId(), packAddress, chain.getConfig().getPassword());
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            String pubKey = (String) callResult.get(PARAM_PUB_KEY);
            netService.createConsensusNetwork(chain.getChainId(), HexUtil.decode(pubKey), HexUtil.decode(priKey), chain.getSeedNodePubKeyList(), packAddressList);
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
