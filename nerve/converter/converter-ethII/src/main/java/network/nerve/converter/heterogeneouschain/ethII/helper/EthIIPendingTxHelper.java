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
package network.nerve.converter.heterogeneouschain.ethII.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.*;
import static network.nerve.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * @author: Mimi
 * @date: 2020-09-27
 */
@Component
public class EthIIPendingTxHelper {

    @Autowired
    private EthCallBackManager ethCallBackManager;

    public void commitNervePendingDepositTx(EthUnconfirmedTxPo po) {
        String ethTxHash = po.getTxHash();
        try {
            ethCallBackManager.getDepositTxSubmitter().pendingTxSubmit(
                    ethTxHash,
                    po.getBlockHeight(),
                    po.getFrom(),
                    po.getTo(),
                    po.getValue(),
                    po.getTxTime(),
                    po.getDecimals(),
                    po.isIfContractAsset(),
                    po.getContractAddress(),
                    po.getAssetId(),
                    po.getNerveAddress());
        } catch (Exception e) {
            // 交易已存在，移除队列
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("Nerve充值待确认交易已存在，忽略[{}]", ethTxHash);
                return;
            }
            logger().warn("在NERVE网络发出充值待确认交易异常, error: {}", e.getMessage());
        }
    }

    public void commitNervePendingWithdrawTx(String nerveTxHash, String ethTxHash) {
        try {
            ethCallBackManager.getTxConfirmedProcessor().pendingTxOfWithdraw(nerveTxHash, ethTxHash);
        } catch (Exception e) {
            // 交易已存在，等待确认移除
            if (e instanceof NulsException && TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode()) || TX_ALREADY_EXISTS_1.equals(((NulsException) e).getErrorCode())) {
                logger().info("Nerve提现待确认交易[{}]已存在，忽略[{}]", nerveTxHash, ethTxHash);
                return;
            }
            logger().warn("在NERVE网络发出提现待确认交易异常, error: {}", e.getMessage());
        }
    }
}
