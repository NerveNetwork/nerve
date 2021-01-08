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
package network.nerve.converter.heterogeneouschain.ht.schedules;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.ht.callback.HtCallBackManager;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.core.HtWalletApi;
import network.nerve.converter.heterogeneouschain.ht.helper.HtAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.ht.helper.HtBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.ht.helper.HtCommonHelper;
import network.nerve.converter.heterogeneouschain.ht.helper.HtLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.ht.model.HtSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("htBlockScheduled")
public class HtBlockScheduled implements Runnable {

    @Autowired
    private HtLocalBlockHelper htLocalBlockHelper;
    @Autowired
    private HtWalletApi htWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HtBlockAnalysisHelper htBlockAnalysisHelper;
    @Autowired
    private HtCommonHelper htCommonHelper;
    @Autowired
    private HtAnalysisTxHelper htAnalysisTxHelper;
    @Autowired
    private HtCallBackManager htCallBackManager;

    private boolean firstSync = true;
    private boolean clearDB = false;


    public void run() {
        if (!HtContext.getConverterCoreApi().isRunning()) {
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("忽略同步区块模式");
            }
            return;
        }
        if (!HtContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            try {
                if(!clearDB) {
                    htLocalBlockHelper.deleteAllLocalBlockHeader();
                    clearDB = true;
                }
            } catch (Exception e) {
                HtContext.logger().error(e);
            }
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            }
            return;
        }
        clearDB = false;
        if (LoggerUtil.LOG.isDebugEnabled()) {
            LoggerUtil.LOG.debug("[HT区块解析任务] - 每隔5秒执行一次。");
        }
        try {
            htWalletApi.checkApi(HtContext.getConverterCoreApi().getVirtualBankOrder());
            BigInteger currentGasPrice = htWalletApi.getCurrentGasPrice();
            if (currentGasPrice != null) {
                HtContext.logger().debug("当前Huobi网络的Price: {} Gwei.", new BigDecimal(currentGasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
                HtContext.setEthGasPrice(currentGasPrice);
            }
        } catch (Exception e) {
            HtContext.logger().error("同步HT当前Price失败", e);
        }
        try {
            htCommonHelper.clearHash();
        } catch (Exception e) {
            HtContext.logger().error("清理充值交易hash再次验证的集合失败", e);
        }
        try {

            // 当前HT网络最新的区块
            long blockHeightFromEth = htWalletApi.getBlockHeight();
            // 本地最新的区块
            HtSimpleBlockHeader localMax = htLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 当启动节点时，本地区块为空，将从HT网络最新高度开始同步
                EthBlock.Block block = htWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    HtContext.logger().info("获取不到HT区块，等待下轮执行");
                    return;
                }
                htBlockAnalysisHelper.analysisEthBlock(block, htAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当启动节点时，本地最新高度与HT网络区块高度相差两个区块及以上时，则从HT网络高度开始同步
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                EthBlock.Block block = htWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    HtContext.logger().info("获取不到HT区块，等待下轮执行");
                    return;
                }
                htLocalBlockHelper.deleteAllLocalBlockHeader();
                htBlockAnalysisHelper.analysisEthBlock(block, htAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                HtContext.logger().error("获取HT区块失败");
                return;
            } else if (resultCode == 1) {
                HtContext.logger().error("HT区块分叉");
                htLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                try {
                    EthBlock.Block block = htWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        HtContext.logger().info("获取不到HT区块，等待下轮执行");
                        break;
                    }
                    htBlockAnalysisHelper.analysisEthBlock(block, htAnalysisTxHelper);
                } catch (Exception e) {
                    HtContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            HtContext.logger().error("同步HT区块失败, 错误: {}", e);
        }
    }

    private int checkNewestBlock(HtSimpleBlockHeader max) throws Exception {
        EthBlock.Block block = htWalletApi.getBlockHeaderByHeight(max.getHeight().longValue());
        if (block == null) {
            return 0;
        } else if (block.getHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
