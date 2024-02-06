/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.syncblock;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.EthAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthCommonHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author: Mimi
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
    private EthBlockAnalysisHelper ethBlockAnalysisHelper;
    @Autowired
    private EthAnalysisTxHelper ethAnalysisTxHelper;
    @Autowired
    private EthCommonHelper ethCommonHelper;
    @Autowired
    private EthCallBackManager ethCallBackManager;

    private boolean switchBlockSync;
    private boolean initialLoaded = false;
    private boolean firstSync = true;
    private boolean clearDB = false;


    public void run() {
        if (!EthContext.getConverterCoreApi().isRunning()) {
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("Ignoring synchronous block mode");
            }
            return;
        }
        if (!EthContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            try {
                if(!clearDB) {
                    ethLocalBlockHelper.deleteAllLocalBlockHeader();
                    clearDB = true;
                }
            } catch (Exception e) {
                EthContext.logger().error(e);
            }
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("Non virtual bank member, skip this task");
            }
            return;
        }
        clearDB = false;
        if (LoggerUtil.LOG.isDebugEnabled()) {
            LoggerUtil.LOG.debug("[ETHBlock parsing task] - every other20Execute once per second.");
        }
        try {
            ethWalletApi.checkApi(EthContext.getConverterCoreApi().getVirtualBankOrder());
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                EthContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            EthContext.logger().error("synchronizationETHcurrentPricefail", e);
        }
        try {
            ethCommonHelper.clearHash();
        } catch (Exception e) {
            EthContext.logger().error("Clearing recharge transactionshashFailed to revalidate collection", e);
        }
        try {

            // currentETHThe latest blocks in the network
            long blockHeightFromEth = ethWalletApi.getBlockHeight();
            // Latest local blocks
            EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // When starting a node, the local block is empty and will be removed from theETHStarting synchronization at the latest height of the network
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    EthContext.logger().info("Unable to obtainETHBlock, waiting for the next round of execution");
                    return;
                }
                ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // When starting a node, the latest local altitude matchesETHWhen the height of a network block differs by two or more blocks, it will be removed from theETHNetwork height begins to synchronize
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    EthContext.logger().info("Unable to obtainETHBlock, waiting for the next round of execution");
                    return;
                }
                ethLocalBlockHelper.deleteAllLocalBlockHeader();
                ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // Verify if the latest block is correct
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                EthContext.logger().error("obtainETHBlock failure");
                return;
            } else if (resultCode == 1) {
                EthContext.logger().error("ETHBlock fork");
                ethLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * Synchronize and parse data
                 */
                try {
                    EthBlock.Block block = ethWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        EthContext.logger().info("Unable to obtainETHBlock, waiting for the next round of execution");
                        break;
                    }
                    ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                } catch (Exception e) {
                    EthContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            EthContext.logger().error("synchronizationETHBlock failure, error: {}", e);
        }
    }

    @Deprecated
    private void runBackup() {
        if (!EthContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            EthContext.logger().info("Not a virtual bank, skipping `EthBlockScheduled`");
            return;
        }
        EthContext.logger().info("ethBlockScheduled - every other20Execute once per second.");
        try {
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                EthContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            EthContext.logger().error("synchronizationETHcurrentPricefail", e);
        }
        try {
            if (!initialLoaded) {
                syncEthBlockInitial.initialEthBlock();
                initialLoaded = true;
            }

            // Latest local blocks
            EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
            // Default starting synchronization height (Temporarily not used)
            long defaultStartHeight = EthContext.getConfig().getDefaultStartHeight();
            if (localMax == null) {
                EthBlock.Block block = ethWalletApi.getBlockByHeight(defaultStartHeight);
                ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // When the latest local height is less than the configured default height, synchronization starts from the default height
            if (localBlockHeight < defaultStartHeight) {
                ethLocalBlockHelper.deleteAllLocalBlockHeader();
                EthBlock.Block block = ethWalletApi.getBlockByHeight(defaultStartHeight);
                ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                return;
            }

            // Verify if the latest block is correct
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                EthContext.logger().error("Failed to obtain block");
                return;
            } else if (resultCode == 1) {
                EthContext.logger().error("Block fork");
                ethLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            // currentETHThe latest blocks in the network
            long blockHeight = ethWalletApi.getBlockHeight();

            for (int i = 1; i <= blockHeight - localBlockHeight; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * Synchronize and parse data
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
                    ethBlockAnalysisHelper.analysisEthBlock(block, ethAnalysisTxHelper);
                } catch (Exception e) {
                    if (!switchBlockSync && block != null) {
                        EthContext.logger().error("syncHeight error height [{}]", block.getNumber().longValue());
                        EthBlockQueueTask.ETH_BLOCK_QUEUE.offerFirst(block);
                    }
                    EthContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            EthContext.logger().error("synchronizationETHBlock failure, error: {}", e);
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
