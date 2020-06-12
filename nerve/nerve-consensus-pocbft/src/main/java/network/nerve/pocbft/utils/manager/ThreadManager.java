package network.nerve.pocbft.utils.manager;

import network.nerve.pocbft.model.bo.Chain;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.pocbft.constant.ConsensusConstant;
import io.nuls.core.core.annotation.Component;
import network.nerve.pocbft.utils.thread.*;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 共识模块任务管理器
 * Consensus Module Task Manager
 *
 * @author tag
 * 2018/11/9
 * */
@Component
public class ThreadManager {
    /**
     * 创建一条链的任务
     * The task of creating a chain
     *
     * @param chain chain info
     * */
    public void createChainThread(Chain chain){
        /*
        创建链相关的任务
        Chain-related tasks
        */
        chain.getThreadPool().execute(new VoteProcessor(chain));
        chain.getThreadPool().execute(new StageOneVoteCollector(chain));
        chain.getThreadPool().execute(new StageTwoVoteCollector(chain));
        chain.getThreadPool().execute(new VoteResultProcessor(chain));
        chain.getThreadPool().execute(new CheckFutureVoteProcessor(chain));
    }

    /**
     * 创建一条链的定时任务
     * The task of creating a chain
     *
     * @param chain chain info
     * */
    public void createChainScheduler(Chain chain){
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadUtils.createScheduledThreadPool(1,new NulsThreadFactory("nerve" + chain.getChainId()));
        BudgetConsensusAwardTask budgetConsensusAwardTask = new BudgetConsensusAwardTask(chain);
        long delayTime ;
        long currentTime = NulsDateUtils.getCurrentTimeMillis();
        long timeInDay = currentTime % ConsensusConstant.ONE_DAY_MILLISECONDS;
        //如果当前时间是12点之前则等到12点再计算，如果12点之后则立即执行，（避免在12点之前停止节点12点之后重启节点这种情况发生）
        if(timeInDay <= ConsensusConstant.HALF_DAY_MILLISECONDS){
            delayTime = ConsensusConstant.HALF_DAY_MILLISECONDS - timeInDay;
        }else{
            delayTime = 0;
        }
        scheduledThreadPoolExecutor.scheduleAtFixedRate(budgetConsensusAwardTask, delayTime, ConsensusConstant.ONE_DAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public void createConsensusNetThread(Chain chain){
        chain.getThreadPool().execute(new NetworkProcessor(chain));
    }
}
