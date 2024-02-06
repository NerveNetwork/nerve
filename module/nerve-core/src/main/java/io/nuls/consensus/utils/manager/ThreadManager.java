package io.nuls.consensus.utils.manager;

import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.message.v1.thread.DisConnectProcessor;
import io.nuls.consensus.network.message.v1.thread.IdentityProcessor;
import io.nuls.consensus.network.message.v1.thread.ShareProcessor;
import io.nuls.consensus.utils.thread.BudgetConsensusAwardTask;
import io.nuls.consensus.utils.thread.NetworkProcessor;
import io.nuls.consensus.v1.thread.ConsensusThread;
import io.nuls.consensus.v1.thread.VoteResultProcessor;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Consensus Module Task Manager
 * Consensus Module Task Manager
 *
 * @author tag
 * 2018/11/9
 */
@Component
public class ThreadManager {
    /**
     * The task of creating a chain
     * The task of creating a chain
     *
     * @param chain chain info
     */
    public void createChainThread(Chain chain) {
        //All consensus core functions start from here
        chain.getThreadPool().execute(new ConsensusThread(chain));
        chain.getThreadPool().execute(new VoteResultProcessor(chain));
        chain.getThreadPool().execute(new IdentityProcessor(chain));
        chain.getThreadPool().execute(new ShareProcessor(chain));
        chain.getThreadPool().execute(new DisConnectProcessor(chain));
    }

    /**
     * Create a scheduled task for a chain
     * The task of creating a chain
     *
     * @param chain chain info
     */
    public void createChainScheduler(Chain chain) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("nerve" + chain.getChainId()));
        BudgetConsensusAwardTask budgetConsensusAwardTask = new BudgetConsensusAwardTask(chain);
        long delayTime;
        long currentTime = NulsDateUtils.getCurrentTimeMillis();
        long timeInDay = currentTime % ConsensusConstant.ONE_DAY_MILLISECONDS;
        //If the current time is halfway through, wait until halfway through to calculate. If halfway through, execute immediately,（Avoid stopping the node before and restarting it after half of the time）
        if (timeInDay <= ConsensusConstant.HALF_DAY_MILLISECONDS) {
            delayTime = ConsensusConstant.HALF_DAY_MILLISECONDS - timeInDay;
        } else {
            delayTime = 0;
        }
        scheduledThreadPoolExecutor.scheduleAtFixedRate(budgetConsensusAwardTask, delayTime, ConsensusConstant.ONE_DAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public void createConsensusNetThread(Chain chain) {
        chain.getThreadPool().execute(new NetworkProcessor(chain));
    }
}
