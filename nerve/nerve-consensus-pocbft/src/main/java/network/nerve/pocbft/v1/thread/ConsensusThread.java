package network.nerve.pocbft.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.ConsensusAwardUtil;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.enumeration.ConsensusStatus;
import network.nerve.pocbft.utils.manager.AgentManager;
import network.nerve.pocbft.v1.CsController;
import network.nerve.pocbft.v1.RoundController;
import network.nerve.pocbft.v1.VoteController;
import network.nerve.pocbft.v1.entity.BasicObject;
import network.nerve.pocbft.v1.entity.LocalBlockListener;

/**
 * @author Eva
 */
public class ConsensusThread extends BasicObject implements Runnable {

    /**
     * 共识轮次初始化标识，每次状态变更为可参与共识时需要运行nodeStart方法
     */
    private boolean inited;
    /**
     * 用于查询节点列表
     */
    private AgentManager agentManager;
    private ThreadController threadController;
    private CsController csController;
    private RoundController roundController;
    private VoteController voteController;

    public ConsensusThread(Chain chain) {
        super(chain);
        this.agentManager = SpringLiteContext.getBean(AgentManager.class);
        threadController = new ThreadController(chain);
        roundController = new RoundController(chain);
        voteController = new VoteController(chain, roundController);
        csController = new CsController(chain, roundController, voteController);
        //验证通过第一个区块时的监听，应该进行投票
        LocalBlockListener listener = new LocalBlockListener(chain, voteController);
        chain.getConsensusCache().getBestBlocksVotingContainer().setListener(listener);

        //未来兼容之前的多链、依赖注入方式，增加下面两行
        SpringLiteContext.putBean("roundController", this.roundController);
        SpringLiteContext.putBean("voteController", this.voteController);
    }


    @Override
    public void run() { //共识模块是否已完成启动操作
        while (true) {
            try {
                if (chain.getConsensusStatus() != ConsensusStatus.RUNNING) {
                    sleep("模块启动中");
                    continue;
                }
                //是否同步了最高高度：区块模块调用接口控制
                if (!chain.isSynchronizedHeight()) {
                    sleep("等待区块模块高度同步");
                    continue;
                }
                //本地是否是共识节点
                if (!chain.isConsonsusNode()) {
                    checkNode();
                    if (inited) {
                        chain.getLogger().warn("不再是共识节点");
//                        从参与中，变为不能参与，清理所有数据
                        this.clearCache();
                    }
                    sleep(null);
                    continue;
                }
                if (!inited) {
                    //第一次变为可参与共识，初始化
                    nodeStart();
                }
                if (!chain.isNetworkStateOk()) {
                    sleep("等待共识组网");
                    continue;
                }
                //真正的业务逻辑
                runConsenrunsus();

            } catch (Throwable e) {
                log.error(e);
                try {
                    sleep(null);
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException);
                }
            }
        }
    }

    //执行共识功能：投票、出块、验证
    private void runConsenrunsus() {
        //这里有几种情况：1启动网络从0开始，2重启网络从某个高度，3启动本节点
        //目标是不管哪种情况，都能尽快达成共识（轮次一致）
        this.csController.consensus();
    }

    //睡眠1秒钟
    private void sleep(String reason) throws InterruptedException {
        if (null != reason) {
            log.info(reason + ",sleep~~~");
        }
        Thread.sleep(1000);
    }

    //临时轮次，用于判断是否是共识节点
    private MeetingRound tempRound;

    private boolean checkNode() {
        //检查是否是共识节点
        boolean needInit = null == tempRound;
        boolean timeout = false;
        if (tempRound != null) {
            //结束时间晚于当前时间
            timeout = tempRound.getStartTime() + tempRound.getDelayedSeconds() + tempRound.getMemberCount() * chain.getConfig().getPackingInterval() < NulsDateUtils.getCurrentTimeSeconds();
        }
        //在轮次为空，或者：临时轮次超时时，进行轮次计算
        needInit = needInit || timeout;
        if (needInit) {
            tempRound = roundController.tempRound();
        }
        boolean result = tempRound.getLocalMember() != null;
        chain.setConsonsusNode(result);
        if (result) {
            roundController.switchRound(tempRound, false);
            CallMethodUtils.sendState(chain, true);
        }
        return result;
    }

    /**
     * 启动系统时执行，
     */
    private void nodeStart() {

        if (!chain.isConsonsusNode()) {
            return;
        }
        //如果还没有轮次，就初始化一个
        MeetingRound round = roundController.getCurrentRound();
        if (null == round) {
            round = roundController.initRound();
        }
        if (round.getLocalMember() == null) {
            return;
        }
        chain.getConsensusCache().clear();
        //消息处理线程：基本的消息验证、过滤、转发功能
        this.threadController.execute(new VoteMsgProcessor(chain, voteController));
        //打包处理器
        this.threadController.execute(new PackingProcessor(chain, roundController));
        //第一阶段投票处理器，单独线程处理
        this.threadController.execute(new StageOneProcessor(chain, this.roundController, this.voteController));
        //是共识节点，连接其他共识节点，组核心网络
        ConsensusNetUtil.initConsensusNet(chain, AddressTool.getStringAddressByBytes(round.getLocalMember().getAgent().getPackingAddress()),
                round.getMemberAddressSet());
        ConsensusAwardUtil.onStartConsensusAward(chain);
        inited = true;
        Log.warn("变为共识中状态");
    }


    /**
     * 清理所有共识缓存，变成一个普通节点
     */
    private void clearCache() {
        Log.warn("变为非共识状态");
        chain.getConsensusCache().clear();
        chain.setConsonsusNode(false);
        ConsensusNetUtil.disConnectConsensusNet(chain);
        this.threadController.shutdown();
        inited = false;
    }
}
