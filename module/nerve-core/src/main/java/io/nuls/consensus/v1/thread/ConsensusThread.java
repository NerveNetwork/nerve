package io.nuls.consensus.v1.thread;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.enumeration.ConsensusStatus;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.consensus.v1.CsController;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.VoteController;
import io.nuls.consensus.v1.entity.BasicObject;
import io.nuls.consensus.v1.entity.LocalBlockListener;

/**
 * @author Eva
 */
public class ConsensusThread extends BasicObject implements Runnable {

    /**
     * Consensus round initialization identifier, which needs to be run every time the state changes to be able to participate in consensusnodeStartmethod
     */
    private boolean inited;
    /**
     * Used to query node list
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
        //Verify listening when passing the first block, and vote should be taken
        LocalBlockListener listener = new LocalBlockListener(chain, voteController);
        chain.getConsensusCache().getBestBlocksVotingContainer().setListener(listener);

        //Future compatibility with previous multi chains、Dependency injection method, add the following two lines
        SpringLiteContext.putBean("roundController", this.roundController);
        SpringLiteContext.putBean("voteController", this.voteController);
    }


    @Override
    public void run() { //Has the consensus module completed the startup operation
        while (true) {
            try {
                if (chain.getConsensusStatus() != ConsensusStatus.RUNNING) {
                    sleep("Module startup in progress");
                    continue;
                }
                //Has the highest altitude been synchronized：Block module call interface control
                if (!chain.isSynchronizedHeight()) {
                    sleep("Waiting for highly synchronized block modules");
                    continue;
                }
                //Is the local consensus node
                if (!chain.isConsonsusNode()) {
                    checkNode();
                    if (inited) {
                        chain.getLogger().warn("No longer a consensus node");
//                        From participating to not participating, clean up all data
                        this.clearCache();
                    }
                    sleep("Not a consensus node");
                    continue;
                }
                if (!inited) {
                    //The first time it becomes a consensus that can be participated in, initialization
                    nodeStart();
                }
                if (!chain.isNetworkStateOk()) {
                    sleep("Waiting for consensus networking");
                    continue;
                }
                //True business logic
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

    //Execute consensus function：vote、Chunking、validate
    private void runConsenrunsus() {
        //There are several situations here：1Start network from0Start,2Restart the network from a certain height,3Start this node
        //The goal is to reach consensus as soon as possible in any situation（Consistent rounds）
        this.csController.consensus();
    }

    //sleep1Seconds
    private void sleep(String reason) throws InterruptedException {
        if (null != reason) {
            log.info(reason + ",sleep~~~");
        }
        Thread.sleep(1000);
    }

    //Temporary round, used to determine whether it is a consensus node
    private MeetingRound tempRound;

    private boolean checkNode() {
        //Check if it is a consensus node
        boolean needInit = null == tempRound;
        boolean timeout = false;
        if (tempRound != null) {
            //End time is later than the current time
            timeout = tempRound.getStartTime() + tempRound.getDelayedSeconds() + tempRound.getMemberCount() * chain.getConfig().getPackingInterval() < NulsDateUtils.getCurrentTimeSeconds();
        }
        //When the round is empty, or：When the temporary round exceeds the time limit, perform round calculation
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
     * Execute when starting the system,
     */
    private void nodeStart() {

        if (!chain.isConsonsusNode()) {
            return;
        }
        //If there are no rounds yet, initialize one
        MeetingRound round = roundController.getCurrentRound();
        if (null == round) {
            round = roundController.initRound();
        }
        if (round.getLocalMember() == null) {
            return;
        }
        chain.getConsensusCache().clear();
        //Message processing thread：Basic message verification、filter、Forwarding function
        this.threadController.execute(new VoteMsgProcessor(chain, voteController));
        //Packaging processor
        this.threadController.execute(new PackingProcessor(chain, roundController));
        //The first stage voting processor, processed by a separate thread
        this.threadController.execute(new StageOneProcessor(chain, this.roundController, this.voteController));
        //It is a consensus node that connects other consensus nodes and forms the core network
        ConsensusNetUtil.initConsensusNet(chain, AddressTool.getStringAddressByBytes(round.getLocalMember().getAgent().getPackingAddress()),
                round.getMemberAddressSet());
        ConsensusAwardUtil.onStartConsensusAward(chain);
        inited = true;
        Log.warn("Change to consensus state");
    }


    /**
     * Clean up all consensus caches and become a regular node
     */
    private void clearCache() {
        Log.warn("Change to a non consensus state");
        chain.getConsensusCache().clear();
        chain.setConsonsusNode(false);
        ConsensusNetUtil.disConnectConsensusNet(chain);
        this.threadController.shutdown();
        inited = false;
    }
}
