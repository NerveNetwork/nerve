package io.nuls.block.task;

import io.nuls.base.data.Block;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class HeightMonitorTask implements InitializingBean, Runnable {

    private Block lastCheckBlock;

    @Override
    public void afterPropertiesSet() throws NulsException {
        String os = System.getProperty("os.name");
        if (os.equals("Mac OS X")) {
            return;
        }
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(this, 5, 5, TimeUnit.MINUTES);
    }

    public Block getBestBlock() {
        ChainContext context = ContextManager.getContext(1);
        if (null == context) {
            return null;
        }
        return context.getLatestBlock();
    }

    @Override
    public void run() {
        Block block = getBestBlock();
        if (null == block) {
            return;
        }
        if (lastCheckBlock == null) {
            this.lastCheckBlock = block;
            return;
        }
        if(block.getHeader().getHeight()<= lastCheckBlock.getHeader().getHeight()){
            System.out.println("==================================================");
            System.out.println("====================Need Restart==================");
            System.out.println("==================================================");
        }

    }
}
