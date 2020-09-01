package io.nuls.api.task;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.exception.SyncException;
import io.nuls.api.model.po.BlockHeaderInfo;
import io.nuls.api.model.po.BlockInfo;
import io.nuls.api.model.po.SyncInfo;
import io.nuls.api.service.RollbackService;
import io.nuls.api.service.SyncService;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.Result;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SyncBlockTask implements Runnable {

    private int chainId;

    private SyncService syncService;

    private RollbackService rollbackService;

    private SymbolRegService symbolRegService;

    private Lock synclock = new ReentrantLock();

    private File stop_file;

    private boolean syncStatus = true;

    public SyncBlockTask(int chainId) {
        this.chainId = chainId;
        syncService = SpringLiteContext.getBean(SyncService.class);
        rollbackService = SpringLiteContext.getBean(RollbackService.class);
        symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
        String stop_file_name = System.getenv("NERVE_STOP_FILE") ;
        stop_file = new File(stop_file_name);
        if(!stop_file.exists()){
            stop_file = null;
        }
        Log.info("NERVE_STOP_FILE:{}", stop_file_name);
    }

    private String read() throws IOException {
        if(stop_file == null){
            return null;
        }
        BufferedReader in = new BufferedReader(new FileReader(stop_file));
        try {
            String line = in.readLine();
            if (line != null) {
                return line.substring(0, 1);
            }
            return null;
        } finally {
            in.close();
        }
    }

    public void run(int resetCount) {
        if (!ApiContext.isReady) {
            LoggerUtil.commonLog.info("------- ApiModule wait for successful cross-chain networking  --------");
            return;
        }
        if(!syncStatus){
            Log.error("同步处于停止状态，清检查停止原因");
            return ;
        }
//        long start = System.currentTimeMillis();
////        symbolRegService.updateSymbolRegList();
//        long end = System.currentTimeMillis() - start;
//        if(end > 500){
//            Log.warn("同步资产信息耗时：{}",end);
//        }
        //每次同步数据前都查看一下最新的同步信息，如果最新块的数据并没有在一次事务中完全处理，需要对区块数据进行回滚
        //Check the latest synchronization information before each entity synchronization.
        //If the latest block entity is not completely processed in one transaction, you need to roll back the block entity.
        try {
            SyncInfo syncInfo = syncService.getSyncInfo(chainId);
            if (syncInfo != null && !syncInfo.isFinish()) {
                rollbackService.rollbackBlock(chainId, syncInfo.getBestHeight());
            }
        } catch (Exception e) {
            Log.error("回滚数据失败", e);
            if (resetCount < 3) {
                run(++resetCount);
            } else {
                Log.error("连续重试回滚失败，停止模块");
                syncStatus = false;
            }
        }
        boolean running = true;
        while (running) {
            try {
                if(checkStopNotify()){
                    running = syncBlock();
                }else{
                    Log.info("监听到停止信号，停止同步数据");
                    syncStatus = false;
                    break;
                }
            } catch (SyncException e) {
                Log.error("同步数据失败，失败高度：{},失败原因:{}", e.height,e.getErrorCode(),e);
                if (resetCount < 3) {
                    Log.info("进行第{}次重试", resetCount);
                    run(++resetCount);
                } else {
                    Log.error("连续重试同步失败，停止模块");
                    syncStatus = false;
                    break;
                }
            }
        }
    }

    private boolean checkStopNotify(){
        try {
            String val = read();
            if(val == null){
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.warn("读取停止信号文件失败");
            return true;
        }
    }

    @Override
    public void run() {
        if (synclock.tryLock()) {
            run(0);
            synclock.unlock();
        }
    }

    /**
     * 同步逻辑
     * 1.Take the record of the latest block saved from the local
     * 2.According to the height of the latest local block, to synchronize the next block of the wallet (local does not start from the 0th block)
     * 3.After syncing to the latest block, the task ends, waiting for the next 10 seconds, resynchronizing
     * 4.Each synchronization needs to be verified with the previous one. If the verification fails, it means local fork and needs to be rolled back.
     * <p>
     * 1. 从本地取出已保存的最新块的记录
     * 2. 根据本地最新块的高度，去同步钱包的下一个块（本地没有则从第0块开始）
     * 3. 同步到最新块后，任务结束，等待下个10秒，重新同步
     * 4. 每次同步都需要和上一块做连续性验证，如果验证失败，说明本地分叉，需要做回滚处理
     *
     * @return boolean 是否还继续同步
     */
    private boolean syncBlock() throws SyncException {
        BlockHeaderInfo localBestBlockHeader = syncService.getBestBlockHeader(chainId);
        return process(localBestBlockHeader);
    }

    private boolean process(BlockHeaderInfo localBestBlockHeader) throws SyncException {
        long nextHeight = 0;
        if (localBestBlockHeader != null) {
            nextHeight = localBestBlockHeader.getHeight() + 1;
        }
        Result<BlockInfo> result = WalletRpcHandler.getBlockInfo(chainId, nextHeight);
        if (result.isFailed()) {
            throw new SyncException(result.getErrorCode(),nextHeight);
        }
        BlockInfo newBlock = result.getData();
        if (null == newBlock) {
            //终止本轮同步，等待线程下一轮调度
            return false;
        }
        try {
            if (checkBlockContinuity(localBestBlockHeader, newBlock.getHeader())) {
                return syncService.syncNewBlock(chainId, newBlock);
            } else if (localBestBlockHeader != null) {
                return rollbackService.rollbackBlock(chainId, localBestBlockHeader.getHeight());
            }
            return false;
        }catch (Throwable e){
            throw new SyncException(nextHeight,e);
        }

    }

    /**
     * 区块连续性验证
     * Block continuity verification
     *
     * @param localBest
     * @param newest
     * @return
     */
    private boolean checkBlockContinuity(BlockHeaderInfo localBest, BlockHeaderInfo newest) {
        if (localBest == null) {
            if (newest.getHeight() == 0) {
                return true;
            } else {
                return false;
            }
        } else {
            if (newest.getHeight() == localBest.getHeight() + 1) {
                if (newest.getPreHash().equals(localBest.getHash())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }



}
