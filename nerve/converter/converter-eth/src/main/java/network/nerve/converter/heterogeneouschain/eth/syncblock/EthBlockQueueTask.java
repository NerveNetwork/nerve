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
 * The task of downloading blocks through multiple threads（No longer in use）
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
     * Detecting height changes in blocks Query block data
     */
    @Override
    public void run() {
        synchronized (ethWalletApi) {
            try {
                long defaultStartHeight = EthContext.getConfig().getDefaultStartHeight();
                EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
                Long localBlockHeight;
                if (localMax == null) {
                    logger.info("The local block is empty and will start from the default starting height[{}]Start downloadingETHblock", defaultStartHeight);
                    localBlockHeight = defaultStartHeight;
                } else {
                    localBlockHeight = localMax.getHeight();
                }
                // Default starting synchronization height
                // When the latest local height is less than the configured default height, synchronization starts from the default height
                if(localBlockHeight < defaultStartHeight) {
                    localBlockHeight = defaultStartHeight;
                }
                logger.info("Local blocks height: {}", localBlockHeight);
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
                    // Calculate the number of blocks to be downloaded for the current batch（The last batch quantity may be less thanthreadCount）
                    int size;
                    if ((between - i) < threadCount) {
                        size = (int) (between - i + 1);
                    } else {
                        size = threadCount;
                    }
                    int queueSize;
                    while ((queueSize = ETH_BLOCK_QUEUE.size()) > 1000) {
                        logger.info("Download block unprocessed queue greater than1000, waiting10Download in seconds, current queue size: {}", queueSize);
                        TimeUnit.SECONDS.sleep(10);
                    }
                    CountDownLatch countDownLatch = new CountDownLatch(size);
                    for (int t = 0; t < size; t++) {
                        tempHeight = tempHeight + 1;
                        threadPool.submit(new GetBlock(ethWalletApi, tempHeight, countDownLatch));
                    }
                    countDownLatch.await();
                    // Determine if all blocks have been successfully downloaded
                    if (list.size() != size) {
                        logger.warn("This batch did not fully download the blocks, interval: [{} - {}]", tempTempHeight, tempTempHeight + size);
                        // Not all successful, restore batch variables and download blocks for this batch again
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
                logger.info("Successfully downloadedethBlock, height: {}", height);
            } catch (Exception e) {
                logger.error("downloadethBlock failure", e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }


}
