package network.nerve.converter.heterogeneouschain.eth.syncblock;

import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * 多线程下载区块的任务（不再使用）
 *
 * @author: Mimi
 * @date: 2019-08-09
 */
@Deprecated
public class EthBlockQueueTask implements Runnable {

    public static final LinkedBlockingDeque<EthBlock.Block> ETH_BLOCK_QUEUE = new LinkedBlockingDeque<>();

    final Logger logger = LoggerFactory.getLogger(getClass());
    private ETHWalletApi ethWalletApi;
    private EthLocalBlockHelper ethLocalBlockHelper;
    private List<EthBlock.Block> list = new ArrayList<>();

    public EthBlockQueueTask() {
    }

    public EthBlockQueueTask(ETHWalletApi ethWalletApi, EthLocalBlockHelper ethLocalBlockHelper) {
        this.ethWalletApi = ethWalletApi;
        this.ethLocalBlockHelper = ethLocalBlockHelper;
    }

    private void add(EthBlock.Block block) {
        synchronized (list) {
            list.add(block);
        }
    }

    /**
     * 检测区块的高度变化 查询区块数据
     */
    @Override
    public void run() {
        synchronized (ethWalletApi) {
            try {
                long defaultStartHeight = EthContext.getConfig().getDefaultStartHeight();
                EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
                Long localBlockHeight;
                if (localMax == null) {
                    logger.info("本地区块为空，将从默认起始高度[{}]开始下载ETH区块", defaultStartHeight);
                    localBlockHeight = defaultStartHeight;
                } else {
                    localBlockHeight = localMax.getHeight();
                }
                // 默认起始同步高度
                // 当本地最新高度小于配置的默认高度时，则从默认高度开始同步
                if(localBlockHeight < defaultStartHeight) {
                    localBlockHeight = defaultStartHeight;
                }
                logger.info("本地区块 height: {}", localBlockHeight);
                long height = ethWalletApi.getBlockHeight();
                Long between = height - localBlockHeight;
                logger.info("between {}", between);
                Long tempHeight = localBlockHeight;
                int threadCount;
                ExecutorService threadPool;
                if (between == 1) {
                    threadCount = 1;
                    threadPool = Executors.newSingleThreadExecutor();
                } else if (between < 5) {
                    threadCount = 2;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                } else if (between < 10) {
                    threadCount = 5;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                } else {
                    threadCount = 15;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                }
                for (int i = 1; i <= between; i = i + threadCount) {
                    long tempTempHeight = tempHeight;
                    // 计算当前批次要下载的区块数量（最后一个批次数量可能小于threadCount）
                    int size;
                    if ((between - i) < threadCount) {
                        size = (int) (between - i + 1);
                    } else {
                        size = threadCount;
                    }
                    int queueSize;
                    while ((queueSize = ETH_BLOCK_QUEUE.size()) > 1000) {
                        logger.info("下载区块未处理队列大于1000，等待10秒再下载，当前队列大小: {}", queueSize);
                        TimeUnit.SECONDS.sleep(10);
                    }
                    CountDownLatch countDownLatch = new CountDownLatch(size);
                    for (int t = 0; t < size; t++) {
                        tempHeight = tempHeight + 1;
                        threadPool.submit(new GetBlock(ethWalletApi, tempHeight, countDownLatch));
                    }
                    countDownLatch.await();
                    // 判断是否全部成功下载区块
                    if (list.size() != size) {
                        logger.warn("该批次未完整下载区块, 区间: [{} - {}]", tempTempHeight, tempTempHeight + size);
                        // 没有全部成功，还原批次变量，重新下载这个批次的区块
                        i = i - threadCount;
                        tempHeight = tempTempHeight;
                        list.clear();
                        continue;
                    }
                    list.sort(new Comparator<EthBlock.Block>() {
                        @Override
                        public int compare(EthBlock.Block o1, EthBlock.Block o2) {
                            long height1 = o1.getNumber().longValue();
                            long height2 = o2.getNumber().longValue();
                            if (height1 > height2) {
                                return 1;
                            } else if (height1 < height2) {
                                return -1;
                            }
                            return 0;
                        }
                    });
                    list.stream().forEach(block -> ETH_BLOCK_QUEUE.offer(block));
                    list.clear();
                }
                threadPool.shutdown();
            } catch (Exception e) {
                logger.error("syncHeight error ", e);
            }
        }
    }

    class GetBlock implements Runnable {
        private long height;
        private CountDownLatch countDownLatch;
        private ETHWalletApi ethWalletApi;

        public GetBlock(ETHWalletApi ethWalletApi, long height, CountDownLatch countDownLatch) {
            this.ethWalletApi = ethWalletApi;
            this.height = height;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                //logger.info("thread {} get block {}", Thread.currentThread().getName(), height);
                EthBlock.Block block = ethWalletApi.getBlockByHeight(height);
                EthBlockQueueTask.this.add(block);
                logger.info("成功下载eth区块，高度: {}", height);
            } catch (Exception e) {
                logger.error("下载eth区块失败", e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }


}
