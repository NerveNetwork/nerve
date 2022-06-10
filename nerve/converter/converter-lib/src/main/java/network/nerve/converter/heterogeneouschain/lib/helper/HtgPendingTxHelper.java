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
package network.nerve.converter.heterogeneouschain.lib.helper;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.model.bo.HeterogeneousAddFeeCrossChainData;
import network.nerve.converter.model.bo.HeterogeneousOneClickCrossChainData;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.BiFunction;


/**
 * @author: Mimi
 * @date: 2020-09-27
 */
public class HtgPendingTxHelper implements BeanInitial {

    private HtgCallBackManager htgCallBackManager;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void commitNervePendingDepositTx(HtgUnconfirmedTxPo po, BiFunction<String, NulsLogger, HeterogeneousOneClickCrossChainData> parseOneClickCrossChainData, BiFunction<String, NulsLogger, HeterogeneousAddFeeCrossChainData> parseAddFeeCrossChainData) {
        String htTxHash = po.getTxHash();
        try {
            // 使用 depositII.extend 来判断，检查是否为跨链追加手续费
            if (this.addFeeCrossChainTx(po, parseAddFeeCrossChainData)) {
                return;
            }
            // 使用 depositII.extend 来判断，检查是否为一键跨链的数据 oneClickCrossChainProcessor
            if (this.commitNervePendingOneClickCrossChainTx(po, parseOneClickCrossChainData)) {
                return;
            }
            ConverterConfig converterConfig = htgContext.getConverterCoreApi().getConverterConfig();
            byte[] withdrawalBlackhole = AddressTool.getAddressByPubKeyStr(converterConfig.getBlackHolePublicKey(), converterConfig.getChainId());
            // 充值的nerve接收地址不能是黑洞地址
            if (Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole)) {
                logger().error("[{}]Deposit Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
                return;
            }
            if (po.isDepositIIMainAndToken()) {
                htgCallBackManager.getDepositTxSubmitter().pendingDepositIITxSubmit(
                        htTxHash,
                        po.getBlockHeight(),
                        po.getFrom(),
                        po.getTo(),
                        po.getValue(),
                        po.getTxTime(),
                        po.getDecimals(),
                        po.getContractAddress(),
                        po.getAssetId(),
                        po.getNerveAddress(),
                        po.getDepositIIMainAssetValue());
            } else {
                htgCallBackManager.getDepositTxSubmitter().pendingTxSubmit(
                        htTxHash,
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
            }
        } catch (Exception e) {
            // 交易已存在，移除队列
            if (e instanceof NulsException &&
                    (HtgConstant.TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || HtgConstant.TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("Nerve充值待确认交易已存在，忽略[{}]", htTxHash);
                return;
            }
            logger().error("在NERVE网络发出充值待确认交易异常", e);
        }
    }

    // P21生效, 使用 depositII.extend 来判断，检查是否为跨链追加手续费
    private boolean addFeeCrossChainTx(HtgUnconfirmedTxPo po, BiFunction<String, NulsLogger, HeterogeneousAddFeeCrossChainData> parseAddFeeCrossChainData) {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return false;
        }
        HeterogeneousAddFeeCrossChainData data = parseAddFeeCrossChainData.apply(po.getDepositIIExtend(), logger());
        if (data == null) {
            return false;
        }
        return true;
    }

    // P21生效, 使用 depositII.extend 来判断，检查是否为一键跨链的数据 oneClickCrossChainProcessor.
    private boolean commitNervePendingOneClickCrossChainTx(HtgUnconfirmedTxPo po, BiFunction<String, NulsLogger, HeterogeneousOneClickCrossChainData> parseOneClickCrossChainData) throws Exception {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return false;
        }
        HeterogeneousOneClickCrossChainData data = parseOneClickCrossChainData.apply(po.getDepositIIExtend(), logger());
        if (data == null) {
            return false;
        }
        ConverterConfig converterConfig = htgContext.getConverterCoreApi().getConverterConfig();
        byte[] withdrawalBlackhole = AddressTool.getAddressByPubKeyStr(converterConfig.getBlackHolePublicKey(), converterConfig.getChainId());
        // 一键跨链的nerve接收地址只能是黑洞地址
        if (!Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole)) {
            logger().error("[{}]OneClickCrossChain Nerve address(only blackHole) error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
            return false;
        }
        BigInteger erc20Value, mainAssetValue;
        Integer erc20Decimals, erc20AssetId;
        if (po.isDepositIIMainAndToken()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = po.getDepositIIMainAssetValue();
        } else if (po.isIfContractAsset()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = BigInteger.ZERO;
        } else {
            erc20Value = BigInteger.ZERO;
            erc20Decimals = 0;
            erc20AssetId = 0;
            mainAssetValue = po.getValue();
        }
        htgCallBackManager.getDepositTxSubmitter().pendingOneClickCrossChainTxSubmit(
                po.getTxHash(),
                po.getBlockHeight(),
                po.getFrom(),
                po.getTo(),
                erc20Value,
                po.getTxTime(),
                erc20Decimals,
                po.getContractAddress(),
                erc20AssetId,
                po.getNerveAddress(),
                mainAssetValue,
                data.getFeeAmount(),
                data.getDesChainId(),
                data.getDesToAddress(),
                data.getTipping(),
                data.getTippingAddress(),
                data.getDesExtend());
        return true;
    }

    public void commitNervePendingWithdrawTx(String nerveTxHash, String htTxHash) {
        try {
            htgCallBackManager.getTxConfirmedProcessor().pendingTxOfWithdraw(nerveTxHash, htTxHash);
        } catch (Exception e) {
            // 交易已存在，等待确认移除
            if (e instanceof NulsException && HtgConstant.TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode()) || HtgConstant.TX_ALREADY_EXISTS_1.equals(((NulsException) e).getErrorCode())) {
                logger().info("Nerve提现待确认交易[{}]已存在，忽略[{}]", nerveTxHash, htTxHash);
                return;
            }
            logger().warn("在NERVE网络发出提现待确认交易异常, error: {}", e.getMessage());
        }
    }
}
