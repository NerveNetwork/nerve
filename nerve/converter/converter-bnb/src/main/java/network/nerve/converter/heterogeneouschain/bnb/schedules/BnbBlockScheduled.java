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
package network.nerve.converter.heterogeneouschain.bnb.schedules;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.bnb.callback.BnbCallBackManager;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbCommonHelper;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("bnbBlockScheduled")
public class BnbBlockScheduled implements Runnable {

    @Autowired
    private BnbLocalBlockHelper bnbLocalBlockHelper;
    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private BnbBlockAnalysisHelper bnbBlockAnalysisHelper;
    @Autowired
    private BnbCommonHelper bnbCommonHelper;
    @Autowired
    private BnbAnalysisTxHelper bnbAnalysisTxHelper;
    @Autowired
    private BnbCallBackManager bnbCallBackManager;

    private boolean firstSync = true;
    private boolean clearDB = false;


    public void run() {
        if (!BnbContext.getConverterCoreApi().isRunning()) {
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("忽略同步区块模式");
            }
            return;
        }
        if (!BnbContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            try {
                if(!clearDB) {
                    bnbLocalBlockHelper.deleteAllLocalBlockHeader();
                    clearDB = true;
                }
            } catch (Exception e) {
                BnbContext.logger().error(e);
            }
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            }
            return;
        }
        clearDB = false;
        if (LoggerUtil.LOG.isDebugEnabled()) {
            LoggerUtil.LOG.debug("[BNB区块解析任务] - 每隔5秒执行一次。");
        }
        try {
            bnbWalletApi.checkApi(BnbContext.getConverterCoreApi().getVirtualBankOrder());
            BigInteger currentGasPrice = bnbWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                BnbContext.logger().debug("当前Binance网络的Price: {} Gwei.", new BigDecimal(currentGasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
                BnbContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            BnbContext.logger().error("同步BNB当前Price失败", e);
        }
        try {
            bnbCommonHelper.clearHash();
        } catch (Exception e) {
            BnbContext.logger().error("清理充值交易hash再次验证的集合失败", e);
        }
        try {

            // 当前BNB网络最新的区块
            long blockHeightFromEth = bnbWalletApi.getBlockHeight();
            // 本地最新的区块
            BnbSimpleBlockHeader localMax = bnbLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 当启动节点时，本地区块为空，将从BNB网络最新高度开始同步
                EthBlock.Block block = bnbWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    BnbContext.logger().info("获取不到BNB区块，等待下轮执行");
                    return;
                }
                bnbBlockAnalysisHelper.analysisEthBlock(block, bnbAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当启动节点时，本地最新高度与BNB网络区块高度相差两个区块及以上时，则从BNB网络高度开始同步
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                EthBlock.Block block = bnbWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    BnbContext.logger().info("获取不到BNB区块，等待下轮执行");
                    return;
                }
                bnbLocalBlockHelper.deleteAllLocalBlockHeader();
                bnbBlockAnalysisHelper.analysisEthBlock(block, bnbAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                BnbContext.logger().error("获取BNB区块失败");
                return;
            } else if (resultCode == 1) {
                BnbContext.logger().error("BNB区块分叉");
                bnbLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                try {
                    EthBlock.Block block = bnbWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        BnbContext.logger().info("获取不到BNB区块，等待下轮执行");
                        break;
                    }
                    bnbBlockAnalysisHelper.analysisEthBlock(block, bnbAnalysisTxHelper);
                } catch (Exception e) {
                    BnbContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            BnbContext.logger().error("同步BNB区块失败, 错误: {}", e);
        }
    }

    private int checkNewestBlock(BnbSimpleBlockHeader max) throws Exception {
        EthBlock.Block block = bnbWalletApi.getBlockHeaderByHeight(max.getHeight().longValue());
        if (block == null) {
            return 0;
        } else if (block.getHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
