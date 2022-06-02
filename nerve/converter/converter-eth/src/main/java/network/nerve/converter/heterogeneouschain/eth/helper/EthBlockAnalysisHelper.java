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
package network.nerve.converter.heterogeneouschain.eth.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.helper.interfaces.IEthAnalysisTx;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.util.List;

/**
 * 解析以太坊区块，监听指定地址和指定交易并回调Nerve核心
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class EthBlockAnalysisHelper {

    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;

    /**
     * 解析以太坊区块
     */
    public void analysisEthBlock(EthBlock.Block block, IEthAnalysisTx ethAnalysisTx) throws Exception {
        List<EthBlock.TransactionResult> ethTransactionResults = block.getTransactions();
        long blockHeight = block.getNumber().longValue();
        int size;
        if (ethTransactionResults != null && (size = ethTransactionResults.size()) > 0) {
            long txTime = block.getTimestamp().longValue();
            for (int i = 0; i < size; i++) {
                org.web3j.protocol.core.methods.response.Transaction tx = (org.web3j.protocol.core.methods.response.Transaction) ethTransactionResults.get(i).get();
                ethAnalysisTx.analysisTx(tx, txTime, blockHeight);
            }
        }
        // 保存本地区块
        EthSimpleBlockHeader simpleBlockHeader = new EthSimpleBlockHeader();
        simpleBlockHeader.setHash(block.getHash());
        simpleBlockHeader.setPreHash(block.getParentHash());
        simpleBlockHeader.setHeight(blockHeight);
        simpleBlockHeader.setCreateTime(System.currentTimeMillis());
        ethLocalBlockHelper.saveLocalBlockHeader(simpleBlockHeader);
        // 只保留最近的三十个区块
        ethLocalBlockHelper.deleteByHeight(blockHeight - 30);
        if (blockHeight % 20 == 0) {
            EthContext.logger().info("同步ETH高度[{}]完成", blockHeight);
        } else {
            EthContext.logger().debug("同步ETH高度[{}]完成", blockHeight);
        }
    }



}
