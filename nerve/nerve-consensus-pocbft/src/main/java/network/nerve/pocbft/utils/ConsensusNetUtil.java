package network.nerve.pocbft.utils;

import network.nerve.pocbft.cache.VoteCache;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.ConsensusIdentityData;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.utils.manager.RoundManager;
import network.nerve.pocbft.utils.thread.CacheConsensusIdentityProcessor;
import network.nerve.pocnetwork.service.ConsensusNetService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private static RoundManager roundManager;

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
     * 链刚启动初始化共识轮次和投票轮次
     *
     * @param chain 链信息
     */
    public static boolean initRound(Chain chain, boolean saveRound) {
        if (VoteCache.CURRENT_BLOCK_VOTE_DATA != null) {
            chain.getLogger().info("Current node round initialized");
            return false;
        }
        //如果链刚启动，则初始化轮次为初始轮次
        if (chain.getNewestHeader().getHeight() == 0) {
            try {
                MeetingRound round = roundManager.getRound(chain, ConsensusConstant.INIT_ROUND_INDEX, NulsDateUtils.getCurrentTimeSeconds() + 1);
                if (saveRound) {
                    MeetingMember member = round.getMyMember();
                    if (member != null && member.getPackingIndexOfRound() == 1) {
                        roundManager.addRound(chain, round);
                        VoteCache.initCurrentVoteRound(chain, round.getIndex(), member.getPackingIndexOfRound(), round.getMemberCount(), chain.getNewestHeader().getHeight() + 1, round.getStartTime());
                        return true;
                    }
                }
            } catch (NulsException e) {
                chain.getLogger().error(e);
            }
        } else {
            if (!chain.isCanPacking() || !chain.isNetworkState()) {
                return false;
            }
            if (!initRoundThreadStarted) {
                initRoundThreadStarted = true;
                ThreadUtils.createAndRunThread("round-init", () -> {
                    long height = chain.getNewestHeader().getHeight();
                    try {
                        //等待10秒，如果10秒之后还未收到投票信息且区块高度未增加，则初始化投票信息
                        TimeUnit.SECONDS.sleep(ConsensusConstant.WAIL_INIT_VOTE_ROUND_TIME);
                    } catch (InterruptedException e) {
                        chain.getLogger().error(e);
                    }
                    Set<String> initFailedSet = new HashSet<>();
                    MeetingRound currentRound = null;
                    String packAddress;
                    while (VoteCache.CURRENT_BLOCK_VOTE_DATA == null && chain.isCanPacking() && height == chain.getNewestHeader().getHeight()) {
                        BlockHeader bestBlockHeader = chain.getNewestHeader();
                        //当前时间
                        long currentTime = NulsDateUtils.getCurrentTimeSeconds();
                        //本地最后一个区块打包时间
                        long bestBlockTime = bestBlockHeader.getTime();
                        BlockExtendsData bestRoundData = bestBlockHeader.getExtendsData();
                        long bestBlockRoundIndex = bestRoundData.getRoundIndex();
                        long timeInterval = currentTime - bestBlockTime;
                        long intervalRound = timeInterval / ConsensusConstant.INIT_CONSENSUS_ROUND_TIME + 1;
                        long roundIndex = bestBlockRoundIndex + intervalRound;
                        long roundStartTime = bestBlockTime + intervalRound * ConsensusConstant.INIT_CONSENSUS_ROUND_TIME;
                        long roundInitWaitTime = roundStartTime + ConsensusConstant.INIT_CONSENSUS_ROUND_WAIT_TIME;
                        long roundEndTime = roundStartTime + ConsensusConstant.INIT_CONSENSUS_ROUND_TIME;
                        long remainderTime = timeInterval % ConsensusConstant.INIT_CONSENSUS_ROUND_TIME;
                        try {
                            //避免多个节点出现轮次差
                            if (remainderTime < ConsensusConstant.INIT_CONSENSUS_ROUND_WAIT_TIME) {
                                long sleepTime = roundInitWaitTime - NulsDateUtils.getCurrentTimeSeconds();
                                TimeUnit.SECONDS.sleep(sleepTime);
                                chain.getLogger().info("节点初始化轮次，等待初始轮次，sleepTime:{},remainderTime:{}", sleepTime,remainderTime);
                                continue;
                            }
                            //如果上一轮未初始化完成，则需要把上一轮应该初始化投票轮次的节点排除
                            if (currentRound != null) {
                                for (MeetingMember member : currentRound.getMemberList()) {
                                    packAddress = AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress());
                                    if (!initFailedSet.contains(packAddress)) {
                                        chain.getLogger().warn("出块节点未启动，该节点不再参与投票轮次初始化，packAddress:{}" ,packAddress);
                                        initFailedSet.add(packAddress);
                                        break;
                                    }
                                }
                            }
                            //查找当前当前轮次应该初始化的节点，并初始化投票轮次
                            currentRound = roundManager.getRound(chain, roundIndex, roundStartTime);
                            MeetingMember localMember = currentRound.getMyMember();
                            if (localMember == null) {
                                chain.getLogger().info("当前节点不是共识节点");
                                return;
                            }
                            for (MeetingMember member : currentRound.getMemberList()) {
                                packAddress = AddressTool.getStringAddressByBytes(member.getAgent().getPackingAddress());
                                if (!initFailedSet.contains(packAddress)) {
                                    if (localMember.getPackingIndexOfRound() == member.getPackingIndexOfRound()) {
                                        chain.getLogger().info("初始化投票轮次，bestBlockTime:{},bestBlockRoundIndex:{},currentTime:{},roundIndex:{},roundStartTime:{},packIndex:{}", bestBlockTime, bestBlockRoundIndex, currentTime, roundIndex, roundStartTime, member.getPackingIndexOfRound());
                                        VoteCache.initCurrentVoteRound(chain, currentRound.getIndex(), member.getPackingIndexOfRound(), currentRound.getMemberCount(), chain.getNewestHeader().getHeight() + 1, currentRound.getStartTime() + member.getPackingIndexOfRound() * 2);
                                        return;
                                    }
                                    break;
                                }
                            }
                            TimeUnit.SECONDS.sleep(roundEndTime - NulsDateUtils.getCurrentTimeSeconds());
                        } catch (NulsException | InterruptedException e) {
                            chain.getLogger().error(e);
                        }
                    }
                    initRoundThreadStarted = false;
                }, false);
            }
        }
        return false;
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
        netService.broadCastConsensusNet(chainId, cmd, message, excludeNodes);
    }
}
