package nerve.network.pocbft.utils.manager;

import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.Chain;
import io.nuls.core.core.annotation.Component;
import nerve.network.pocbft.utils.thread.*;

import java.util.Calendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 共识模块任务管理器
 * Consensus Module Task Manager
 *
 * @author: Jason
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
        Calendar c = Calendar.getInstance();
        long currentTime = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        long executeTime = c.getTimeInMillis();
        long intervalTime = executeTime - currentTime;
        if(intervalTime < 0){
            delayTime = executeTime - currentTime + ConsensusConstant.ONE_DAY_MILLISECONDS;
        }else{
            delayTime = intervalTime;
        }
        scheduledThreadPoolExecutor.scheduleAtFixedRate(budgetConsensusAwardTask, delayTime, ConsensusConstant.ONE_DAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public void createConsensusNetThread(Chain chain){
        chain.getThreadPool().execute(new NetworkProcessor(chain));
    }
}
