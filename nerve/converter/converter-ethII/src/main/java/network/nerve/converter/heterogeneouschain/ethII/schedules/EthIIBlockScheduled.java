/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.ethII.schedules;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.EthBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthCommonHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.eth.syncblock.SyncEthBlockInitial;
import network.nerve.converter.heterogeneouschain.ethII.helper.EthIIAnalysisTxHelper;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("ethIIBlockScheduled")
public class EthIIBlockScheduled implements Runnable {

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
    private EthCommonHelper ethCommonHelper;
    @Autowired
    private EthIIAnalysisTxHelper ethIIAnalysisTxHelper;
    @Autowired
    private EthCallBackManager ethCallBackManager;

    private boolean firstSync = true;
    private boolean clearDB = false;


    public void run() {
        if (!EthContext.getConverterCoreApi().isRunning()) {
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("忽略同步区块模式");
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
                LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            }
            return;
        }
        clearDB = false;
        if (LoggerUtil.LOG.isDebugEnabled()) {
            LoggerUtil.LOG.debug("[ETH区块解析任务] - 每隔20秒执行一次。");
        }
        try {
            ethWalletApi.checkApi(EthContext.getConverterCoreApi().getVirtualBankOrder());
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                EthContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            EthContext.logger().error("同步ETH当前Price失败", e);
        }
        try {
            ethCommonHelper.clearHash();
        } catch (Exception e) {
            EthContext.logger().error("清理充值交易hash再次验证的集合失败", e);
        }
        try {

            // 当前ETH网络最新的区块
            long blockHeightFromEth = ethWalletApi.getBlockHeight();
            // 本地最新的区块
            EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 当启动节点时，本地区块为空，将从ETH网络最新高度开始同步
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    EthContext.logger().info("获取不到ETH区块，等待下轮执行");
                    return;
                }
                ethBlockAnalysisHelper.analysisEthBlock(block, ethIIAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当启动节点时，本地最新高度与ETH网络区块高度相差两个区块及以上时，则从ETH网络高度开始同步
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                EthBlock.Block block = ethWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    EthContext.logger().info("获取不到ETH区块，等待下轮执行");
                    return;
                }
                ethLocalBlockHelper.deleteAllLocalBlockHeader();
                ethBlockAnalysisHelper.analysisEthBlock(block, ethIIAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                EthContext.logger().error("获取ETH区块失败");
                return;
            } else if (resultCode == 1) {
                EthContext.logger().error("ETH区块分叉");
                ethLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                try {
                    EthBlock.Block block = ethWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        EthContext.logger().info("获取不到ETH区块，等待下轮执行");
                        break;
                    }
                    ethBlockAnalysisHelper.analysisEthBlock(block, ethIIAnalysisTxHelper);
                } catch (Exception e) {
                    EthContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            EthContext.logger().error("同步ETH区块失败, 错误: {}", e);
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
