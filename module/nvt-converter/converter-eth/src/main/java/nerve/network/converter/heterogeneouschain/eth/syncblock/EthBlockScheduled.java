/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nerve.network.converter.heterogeneouschain.eth.syncblock;

import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import nerve.network.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * @author: Chino
 * @date: 2020-02-20
 */
@Component("ethBlockScheduled")
public class EthBlockScheduled implements Runnable {

    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private SyncEthBlockInitial syncEthBlockInitial;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthBlockAnalysis ethBlockAnalysis;
    @Autowired
    private EthCallBackManager ethCallBackManager;

    private boolean switchBlockSync;
    private boolean initialLoaded = false;
    private boolean firstSync = true;


    public void run() {
        if(!ethCallBackManager.getTxConfirmedProcessor().isVirtualBankByCurrentNode()) {
            logger().info("Not a virtual bank, skipping `EthBlockScheduled`");
            return;
        }
        logger().info("ethBlockScheduled - 每隔20秒执行一次。");
        try {
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                EthContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            logger().error("同步ETH当前Price失败", e);
        }
        try {

            // 当前ETH网络最新的区块
            long blockHeightFromEth = ethWalletApi.getBlockHeight();
            // 本地最新的区块
            EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 本地区块为空，将从ETH网络最新高度开始同步
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                ethBlockAnalysis.analysisEthBlock(block);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当本地最新高度与ETH网络区块高度相差两个区块及以上时，则从ETH网络高度开始同步
            if(firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                ethLocalBlockHelper.deleteAllLocalBlockHeader();
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                ethBlockAnalysis.analysisEthBlock(block);
                firstSync = false;
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                logger().error("获取区块失败");
                return;
            } else if (resultCode == 1) {
                logger().error("区块分叉");
                ethLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            for (int i = 1; i <= blockHeightFromEth - localBlockHeight; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                try {
                    EthBlock.Block block = ethWalletApi.getBlockByHeight(localBlockHeight);
                    ethBlockAnalysis.analysisEthBlock(block);
                } catch (Exception e) {
                    logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            logger().error("同步ETH区块失败, 错误: {}", e);
        }
    }

    private void runBackup() {
        if(!ethCallBackManager.getTxConfirmedProcessor().isVirtualBankByCurrentNode()) {
            logger().info("Not a virtual bank, skipping `EthBlockScheduled`");
            return;
        }
        logger().info("ethBlockScheduled - 每隔20秒执行一次。");
        try {
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                EthContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            logger().error("同步ETH当前Price失败", e);
        }
        try {
            if (!initialLoaded) {
                syncEthBlockInitial.initialEthBlock();
                initialLoaded = true;
            }

            // 本地最新的区块
            EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
            // 默认起始同步高度 (暂时不使用)
            long defaultStartHeight = EthContext.getConfig().getDefaultStartHeight();
            if (localMax == null) {
                EthBlock.Block block = ethWalletApi.getBlockByHeight(defaultStartHeight);
                ethBlockAnalysis.analysisEthBlock(block);
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当本地最新高度小于配置的默认高度时，则从默认高度开始同步
            if(localBlockHeight < defaultStartHeight) {
                ethLocalBlockHelper.deleteAllLocalBlockHeader();
                EthBlock.Block block = ethWalletApi.getBlockByHeight(defaultStartHeight);
                ethBlockAnalysis.analysisEthBlock(block);
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                logger().error("获取区块失败");
                return;
            } else if (resultCode == 1) {
                logger().error("区块分叉");
                ethLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            // 当前ETH网络最新的区块
            long blockHeight = ethWalletApi.getBlockHeight();

            for (int i = 1; i <= blockHeight - localBlockHeight; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                EthBlock.Block block = null;
                try {
                    if (!switchBlockSync) {
                        LinkedBlockingDeque<EthBlock.Block> queue = EthBlockQueueTask.ETH_BLOCK_QUEUE;
                        block = queue.poll(15, TimeUnit.SECONDS);
                        if (block == null) {
                            if (!syncEthBlockInitial.getInitEthBlockFutrue().isDone()) {
                                return;
                            } else {
                                switchBlockSync = true;
                                block = ethWalletApi.getBlockByHeight(localBlockHeight);
                                syncEthBlockInitial.getSingleThreadPool().shutdown();
                            }
                        }
                    } else {
                        block = ethWalletApi.getBlockByHeight(localBlockHeight);
                    }
                    ethBlockAnalysis.analysisEthBlock(block);
                } catch (Exception e) {
                    if (!switchBlockSync && block != null) {
                        logger().error("syncHeight error height [{}]", block.getNumber().longValue());
                        EthBlockQueueTask.ETH_BLOCK_QUEUE.offerFirst(block);
                    }
                    logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            logger().error("同步ETH区块失败, 错误: {}", e);
        }
    }

    private int checkNewestBlock(EthSimpleBlockHeader max) throws Exception {
        EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(max.getHeight().longValue());
        if (block == null) {
            return 0;
        } else if (block.getHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
