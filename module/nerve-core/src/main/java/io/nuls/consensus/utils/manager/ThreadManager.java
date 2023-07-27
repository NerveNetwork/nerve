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
 * 共识模块任务管理器
 * Consensus Module Task Manager
 *
 * @author tag
 * 2018/11/9
 */
@Component
public class ThreadManager {
    /**
     * 创建一条链的任务
     * The task of creating a chain
     *
     * @param chain chain info
     */
    public void createChainThread(Chain chain) {
        //所有共识核心功能都从这里开始
        chain.getThreadPool().execute(new ConsensusThread(chain));
        chain.getThreadPool().execute(new VoteResultProcessor(chain));
        chain.getThreadPool().execute(new IdentityProcessor(chain));
        chain.getThreadPool().execute(new ShareProcessor(chain));
        chain.getThreadPool().execute(new DisConnectProcessor(chain));
    }

    /**
     * 创建一条链的定时任务
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
        //如果当前时间是一半之前则等到一半时再计算，如果一半之后之后则立即执行，（避免在一半之前停止节点一半之后重启节点这种情况发生）
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
