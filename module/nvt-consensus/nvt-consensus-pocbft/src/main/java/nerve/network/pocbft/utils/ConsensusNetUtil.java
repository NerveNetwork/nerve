package nerve.network.pocbft.utils;

import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.pocbft.cache.VoteCache;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.round.MeetingMember;
import nerve.network.pocbft.model.bo.round.MeetingRound;
import nerve.network.pocbft.rpc.call.CallMethodUtils;
import nerve.network.pocbft.utils.manager.RoundManager;
import nerve.network.pocnetwork.service.ConsensusNetService;

import java.util.HashMap;
import java.util.Set;

import static nerve.network.pocbft.constant.ParameterConstant.PARAM_PRI_KEY;
import static nerve.network.pocbft.constant.ParameterConstant.PARAM_PUB_KEY;

/**
 * 交易工具类
 * Transaction Tool Class
 *
 * @author: Jason
 * 2019/7/25
 */
@Component
public class ConsensusNetUtil {
    @Autowired
    private static ConsensusNetService netService;

    @Autowired
    private static RoundManager roundManager;


    public static Result getSuccess() {
        return Result.getSuccess(ConsensusErrorCode.SUCCESS);
    }

    /**
     * 初始化共识网络
     * @param chain            链信息
     * @param packAddress      出块地址
     * @param packAddressList  共识节点出块地址列表
     * */
    public static void initConsensusNet(Chain chain, String packAddress, Set<String> packAddressList){
        try {
            HashMap callResult = CallMethodUtils.accountValid(chain.getChainId(), packAddress, chain.getConfig().getPassword());
            String priKey = (String) callResult.get(PARAM_PRI_KEY);
            String pubKey = (String) callResult.get(PARAM_PUB_KEY);
            netService.createConsensusNetwork(chain.getChainId(), HexUtil.decode(pubKey), HexUtil.decode(priKey), chain.getSeedNodePubKeyList(), packAddressList);
        }catch (Exception e){
            chain.getLogger().error(e);
        }
    }

    /**
     * 节点有共识节点变为非共识节点
     * @param chain 链信息
     * */
    public static void disConnectConsensusNet(Chain chain){
        chain.getLogger().info("节点由共识节点变为非共识节点，断开共识网络" );
        netService.cleanConsensusNetwork(chain.getChainId());
    }

    /**
     * 链刚启动初始化共识轮次和投票轮次
     * @param chain  链信息
     * */
    public static boolean initRound(Chain chain, boolean saveRound){
        if(chain.getNewestHeader().getHeight() == 0 && VoteCache.CURRENT_BLOCK_VOTE_DATA == null){
            try {
                MeetingRound round = roundManager.getRound(chain, ConsensusConstant.INIT_ROUND_INDEX, NulsDateUtils.getCurrentTimeSeconds() + 1);
                if(saveRound){
                    MeetingMember member = round.getMyMember();
                    if(member != null && member.getPackingIndexOfRound() == 1){
                        roundManager.addRound(chain, round);
                        VoteCache.initCurrentVoteRound(chain ,round.getIndex(), member.getPackingIndexOfRound(), round.getMemberCount(), chain.getNewestHeader().getHeight() + 1, round.getStartTime());
                        return true;
                    }
                }
            }catch (NulsException e){
                chain.getLogger().error(e);
            }
        }
        return false;
    }

    /**
     * 共识网络广播消息
     * @param chainId       链ID
     * @param cmd           调用命令
     * @param message       消息
     * @param excludeNodes  排除的节点
     * */
    public static void broadcastInConsensus(int chainId, String cmd, String message, String excludeNodes){
        netService.broadCastConsensusNet(chainId, cmd, message, excludeNodes);
    }
}
