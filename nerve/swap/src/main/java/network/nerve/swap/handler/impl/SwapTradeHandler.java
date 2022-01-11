/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.swap.handler.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static network.nerve.swap.constant.SwapConstant.*;
import static network.nerve.swap.constant.SwapErrorCode.INVALID_PATH;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class SwapTradeHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private SwapHelper swapHelper;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;

    @Override
    public Integer txType() {
        return TxType.SWAP_TRADE;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        if (swapHelper.isSupportProtocol17()) {
            //协议17: 整合稳定币币池后，稳定币币种1:1兑换
            return executeProtocol17(chainId, iPairFactory, tx, blockHeight, blockTime);
        } else {
            return _execute(chainId, tx, blockHeight, blockTime);
        }
    }

    private SwapResult _execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getSwapTradeInfo(chainId, coinData);
            // 提取业务参数
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // 检查firstPairAddress是否和path中的一致
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 2) {
                throw new NulsException(INVALID_PATH);
            }
            if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // 用户卖出的资产数量
            BigInteger amountIn = dto.getAmountIn();

            // 整合计算数据
            SwapTradeBus bus = this._calSwapTradeBusiness(chainId, iPairFactory, amountIn,
                    txData.getTo(), path, txData.getAmountOutMin(), txData.getFeeTo());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this._makeSystemDealTx(bus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            List<TradePairBus> busList = bus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
        } catch (Exception e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
            // 组装系统退还交易
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            NerveToken tokenIn = dto.getTokenIn();
            BigInteger amountIn = dto.getAmountIn();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalanceIn = tempBalanceManager.getBalance(dto.getFirstPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
            Transaction refundTx =
                    refund.newFrom()
                            .setFrom(ledgerBalanceIn, amountIn).endFrom()
                            .newTo()
                            .setToAddress(dto.getUserAddress())
                            .setToAssetsChainId(tokenIn.getChainId())
                            .setToAssetsId(tokenIn.getAssetId())
                            .setToAmount(amountIn).endTo()
                            .build();
            result.setSubTx(refundTx);
            String refundTxStr = SwapUtils.nulsData2Hex(refundTx);
            result.setSubTxStr(refundTxStr);
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, refundTx, blockTime);
        } finally {
            batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        }
        return result;
    }

    private SwapResult executeProtocol17(int chainId, IPairFactory iPairFactory, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getSwapTradeInfo(chainId, coinData);
            // 提取业务参数
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // 检查firstPairAddress是否和path中的一致
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 2) {
                throw new NulsException(INVALID_PATH);
            }
            if (!Arrays.equals(this.calcPairAddressProtocol17(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // 用户卖出的资产数量
            BigInteger amountIn = dto.getAmountIn();

            // 整合计算数据
            SwapTradeBus bus = this.calSwapTradeBusinessProtocol17(chainId, iPairFactory, amountIn,
                    txData.getTo(), path, txData.getAmountOutMin(), txData.getFeeTo());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            result.setSwapTradeBus(bus);
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTxProtocol17(chainId, iPairFactory, bus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            List<TradePairBus> busList = bus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                //协议17: 整合稳定币币池后，稳定币币种1:1兑换
                if (bus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                    // 临时缓存更新应该移动到 swapTradeHandler 的 execute 函数中，防止交易验证流程污染了缓存
                    stableSwapTradeHandler.updateCacheByCombining(iPairFactory, pairBus.getStableSwapTradeBus(), blockHeight, blockTime);
                    continue;
                }
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
        } catch (Exception e) {
            Log.error(e);
            // 装填失败的执行结果
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
            // 组装系统退还交易
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            NerveToken tokenIn = dto.getTokenIn();
            BigInteger amountIn = dto.getAmountIn();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalanceIn = tempBalanceManager.getBalance(dto.getFirstPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
            Transaction refundTx =
                    refund.newFrom()
                            .setFrom(ledgerBalanceIn, amountIn).endFrom()
                            .newTo()
                            .setToAddress(dto.getUserAddress())
                            .setToAssetsChainId(tokenIn.getChainId())
                            .setToAssetsId(tokenIn.getAssetId())
                            .setToAmount(amountIn).endTo()
                            .build();
            result.setSubTx(refundTx);
            String refundTxStr = SwapUtils.nulsData2Hex(refundTx);
            result.setSubTxStr(refundTxStr);
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, refundTx, blockTime);
        } finally {
            batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        }
        return result;
    }

    /**
     * [手续费分配]
     *
     * 交易金额的千分之三手续费, 千分之二给流动性提供者
     * 剩下千分之一，销毁地址占比50%
     * 剩余50%的部分
     *     若在交易中指定了手续费地址，则分70%，剩余30%给系统地址
     *     若在交易中未指定手续费地址，则给系统地址
     */
    private Transaction _makeSystemDealTx(SwapTradeBus bus, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo) {
        List<TradePairBus> busList = bus.getTradePairBuses();
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        NerveToken tokenIn, tokenOut;
        for (TradePairBus pairBus : busList) {
            tokenIn = pairBus.getTokenIn();
            tokenOut = pairBus.getTokenOut();
            BigInteger amountOut = pairBus.getAmountOut();
            LedgerBalance ledgerBalanceOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenOut.getChainId(), tokenOut.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(ledgerBalanceOut, amountOut).endFrom()
                    .newTo()
                    .setToAddress(pairBus.getTo())
                    .setToAssetsChainId(tokenOut.getChainId())
                    .setToAssetsId(tokenOut.getAssetId())
                    .setToAmount(amountOut).endTo();
            /** 计算手续费分配 */
            do {

                LedgerBalance ledgerBalanceFeeOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                // `非`流动性提供者可奖励的交易手续费
                BigInteger unLiquidityAwardFee = pairBus.getUnLiquidityAwardFee();
                if (unLiquidityAwardFee.equals(BigInteger.ZERO)) {
                    break;
                }
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceFeeOut, unLiquidityAwardFee).endFrom();
                // 销毁地址的手续费奖励，占比50%（在`非`流动性提供者可奖励的交易手续费当中）
                BigInteger destructionAwardFee = unLiquidityAwardFee.divide(BI_2);
                // 金额太小时，不足以 divide 2，则全分配给销毁地址
                if (destructionAwardFee.equals(BigInteger.ZERO)) {
                    sysDeal.newTo()
                            .setToAddress(SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS)
                            .setToAssetsChainId(tokenIn.getChainId())
                            .setToAssetsId(tokenIn.getAssetId())
                            .setToAmount(unLiquidityAwardFee).endTo();
                    break;
                }
                sysDeal.newTo()
                        .setToAddress(SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS)
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(destructionAwardFee).endTo();
                // 系统地址的手续费奖励
                BigInteger systemAwardFee;
                // 当前交易的指定地址的手续费奖励
                BigInteger assignAddrAwardFee;
                if (feeTo == null) {
                    // 交易未指定手续费奖励地址，则剩余部分手续费奖励分发给系统地址（在扣除销毁部分后）
                    systemAwardFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                } else {
                    BigInteger tempFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                    // 其中奖励给当前交易指定地址的交易手续费，占比70%（在扣除销毁部分后）
                    assignAddrAwardFee= tempFee.multiply(BI_7).divide(BI_10);
                    // 剩余给系统的交易手续费
                    systemAwardFee  = tempFee.subtract(assignAddrAwardFee);
                    if (!assignAddrAwardFee.equals(BigInteger.ZERO)) {
                        sysDeal.newTo()
                                .setToAddress(feeTo)
                                .setToAssetsChainId(tokenIn.getChainId())
                                .setToAssetsId(tokenIn.getAssetId())
                                .setToAmount(assignAddrAwardFee).endTo();
                    }
                }
                sysDeal.newTo()
                        .setToAddress(SwapContext.AWARD_FEE_SYSTEM_ADDRESS)
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(systemAwardFee).endTo();
            } while (false);
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    public Transaction makeSystemDealTxProtocol17(int chainId, IPairFactory iPairFactory, SwapTradeBus bus, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo) throws Exception {
        List<TradePairBus> busList = bus.getTradePairBuses();
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        NerveToken tokenIn, tokenOut;
        for (TradePairBus pairBus : busList) {
            tokenIn = pairBus.getTokenIn();
            tokenOut = pairBus.getTokenOut();
            // 整合稳定币币池后，稳定币币种1:1兑换
            if (bus.isExistStablePair() && SwapUtils.groupCombining(tokenIn, tokenOut)) {
                StableSwapTradeBus stableSwapTradeBus = stableSwapTradeHandler.tradeByCombining(chainId, iPairFactory, pairBus.getPairAddress(), pairBus.getTo(),
                        tempBalanceManager, pairBus.getTokenIn(), pairBus.getAmountIn(), pairBus.getTokenOut(), sysDeal);
                pairBus.setStableSwapTradeBus(stableSwapTradeBus);
                pairBus.setPreBlockHeight(stableSwapTradeBus.getPreBlockHeight());
                pairBus.setPreBlockTime(stableSwapTradeBus.getPreBlockTime());
                continue;
            }
            BigInteger amountOut = pairBus.getAmountOut();
            LedgerBalance ledgerBalanceOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenOut.getChainId(), tokenOut.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(ledgerBalanceOut, amountOut).endFrom()
                    .newTo()
                    .setToAddress(pairBus.getTo())
                    .setToAssetsChainId(tokenOut.getChainId())
                    .setToAssetsId(tokenOut.getAssetId())
                    .setToAmount(amountOut).endTo();
            /** 计算手续费分配 */
            do {
                LedgerBalance ledgerBalanceFeeOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                // `非`流动性提供者可奖励的交易手续费
                BigInteger unLiquidityAwardFee = pairBus.getUnLiquidityAwardFee();
                if (unLiquidityAwardFee.equals(BigInteger.ZERO)) {
                    break;
                }
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceFeeOut, unLiquidityAwardFee).endFrom();
                // 销毁地址的手续费奖励，占比50%（在`非`流动性提供者可奖励的交易手续费当中）
                BigInteger destructionAwardFee = unLiquidityAwardFee.divide(BI_2);
                // 金额太小时，不足以 divide 2，则全分配给销毁地址
                if (destructionAwardFee.equals(BigInteger.ZERO)) {
                    sysDeal.newTo()
                            .setToAddress(SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS)
                            .setToAssetsChainId(tokenIn.getChainId())
                            .setToAssetsId(tokenIn.getAssetId())
                            .setToAmount(unLiquidityAwardFee).endTo();
                    break;
                }
                sysDeal.newTo()
                        .setToAddress(SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS)
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(destructionAwardFee).endTo();
                // 系统地址的手续费奖励
                BigInteger systemAwardFee;
                // 当前交易的指定地址的手续费奖励
                BigInteger assignAddrAwardFee;
                if (feeTo == null) {
                    // 交易未指定手续费奖励地址，则剩余部分手续费奖励分发给系统地址（在扣除销毁部分后）
                    systemAwardFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                } else {
                    BigInteger tempFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                    // 其中奖励给当前交易指定地址的交易手续费，占比70%（在扣除销毁部分后）
                    assignAddrAwardFee= tempFee.multiply(BI_7).divide(BI_10);
                    // 剩余给系统的交易手续费
                    systemAwardFee  = tempFee.subtract(assignAddrAwardFee);
                    if (!assignAddrAwardFee.equals(BigInteger.ZERO)) {
                        sysDeal.newTo()
                                .setToAddress(feeTo)
                                .setToAssetsChainId(tokenIn.getChainId())
                                .setToAssetsId(tokenIn.getAssetId())
                                .setToAmount(assignAddrAwardFee).endTo();
                    }
                }
                sysDeal.newTo()
                        .setToAddress(SwapContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0)
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(systemAwardFee).endTo();
            } while (false);
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    public SwapTradeDTO getSwapTradeInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 1) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
        }
        CoinTo to = tos.get(0);
        if (to.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_AMOUNT_LOCK_ERROR);
        }
        byte[] firstPairAddress = to.getAddress();

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 1) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        CoinFrom from = froms.get(0);
        byte[] userAddress = from.getAddress();
        BigInteger amountIn = to.getAmount();
        return new SwapTradeDTO(userAddress, firstPairAddress, new NerveToken(to.getAssetsChainId(), to.getAssetsId()), amountIn);
    }

    public SwapTradeBus _calSwapTradeBusiness(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
        if (!AddressTool.validAddress(chainId, _to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        if (feeTo != null && !AddressTool.validAddress(chainId, feeTo)) {
            throw new NulsException(SwapErrorCode.FEE_RECEIVE_ADDRESS_ERROR);
        }
        BigInteger[] amounts = SwapUtils.getAmountsOut(chainId, iPairFactory, _amountIn, path);
        if (amounts[amounts.length - 1].compareTo(amountOutMin) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
        }

        List<TradePairBus> list = new ArrayList<>();
        int length = path.length;
        for (int i = 0; i < length - 1; i++) {
            NerveToken input = path[i];
            NerveToken output = path[i + 1];
            NerveToken[] tokens = SwapUtils.tokenSort(input, output);
            NerveToken token0 = tokens[0];
            BigInteger amountIn = amounts[i];
            // `非`流动性提供者可奖励的交易手续费, 占交易金额的0.1%
            BigInteger unLiquidityAwardFee = amountIn.divide(BI_1000);
            BigInteger amountOut = amounts[i + 1];
            BigInteger amount0Out, amount1Out, amount0In, amount1In, amount0InUnLiquidityAwardFee, amount1InUnLiquidityAwardFee;
            if (input.equals(token0)) {
                amount0In = amountIn;
                amount1In = BigInteger.ZERO;
                amount0InUnLiquidityAwardFee = unLiquidityAwardFee;
                amount1InUnLiquidityAwardFee = BigInteger.ZERO;
                amount0Out = BigInteger.ZERO;
                amount1Out = amountOut;
            } else {
                amount0In = BigInteger.ZERO;
                amount1In = amountIn;
                amount0InUnLiquidityAwardFee = BigInteger.ZERO;
                amount1InUnLiquidityAwardFee = unLiquidityAwardFee;
                amount0Out = amountOut;
                amount1Out = BigInteger.ZERO;
            }
            byte[] to = i < length - 2 ? SwapUtils.getPairAddress(chainId, output, path[i + 2]) : _to;
            IPair pair = iPairFactory.getPair(SwapUtils.getStringPairAddress(chainId, input, output));

            SwapPairPO pairPO = pair.getPair();
            NerveToken _token0 = pairPO.getToken0();
            NerveToken _token1 = pairPO.getToken1();
            BigInteger[] reserves = pair.getReserves();
            BigInteger _reserve0 = reserves[0];
            BigInteger _reserve1 = reserves[1];
            if (amount0Out.compareTo(_reserve0) >= 0 || amount1Out.compareTo(_reserve1) >= 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY);
            }
            if (to.equals(_token0) || to.equals(_token1)) {
                throw new NulsException(SwapErrorCode.INVALID_TO);
            }
            BigInteger balance0 = _reserve0.add(amount0In).subtract(amount0Out);
            BigInteger balance1 = _reserve1.add(amount1In).subtract(amount1Out);
            BigInteger balance0Adjusted = balance0.multiply(BI_1000).subtract(amount0In.multiply(BI_3));
            BigInteger balance1Adjusted = balance1.multiply(BI_1000).subtract(amount1In.multiply(BI_3));
            if (balance0Adjusted.multiply(balance1Adjusted).compareTo(_reserve0.multiply(_reserve1).multiply(BI_1000_000)) < 0) {
                throw new NulsException(SwapErrorCode.K);
            }
            // balance要扣除即时分配出的手续费奖励
            balance0 = balance0.subtract(amount0InUnLiquidityAwardFee);
            balance1 = balance1.subtract(amount1InUnLiquidityAwardFee);
            // 组装业务数据
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, unLiquidityAwardFee, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        return bus;
    }

    public SwapTradeBus calSwapTradeBusinessProtocol17(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
        if (!AddressTool.validAddress(chainId, _to)) {
            throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
        }
        if (feeTo != null && !AddressTool.validAddress(chainId, feeTo)) {
            throw new NulsException(SwapErrorCode.FEE_RECEIVE_ADDRESS_ERROR);
        }
        BigInteger[] amounts = SwapUtils.getAmountsOutByCombining(chainId, iPairFactory, _amountIn, path, SwapContext.stableCoinGroup);
        if (amounts[amounts.length - 1].compareTo(amountOutMin) < 0) {
            throw new NulsException(SwapErrorCode.INSUFFICIENT_OUTPUT_AMOUNT);
        }

        boolean existStablePair = false;
        List<TradePairBus> list = new ArrayList<>();
        int length = path.length;
        NerveToken input, output;
        for (int i = 0; i < length - 1; i++) {
            input = path[i];
            output = path[i + 1];
            BigInteger amountIn = amounts[i];
            // swap trade 的path，计算to地址时，若计算的to地址使用的两个token正好是稳定币池币种的话，就使用稳定币交易池地址
            byte[] to = this.calcAddressOfTradeToProtocol17(chainId, i, _to, path);
            //协议17: 整合稳定币币池后，稳定币币种1:1兑换
            int groupIndex;
            if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(input, output)) != -1) {
                // 组装业务数据
                BigInteger amountOut = SwapUtils.getStableOutAmountByGroupIndex(groupIndex, input, amountIn, output, iPairFactory, SwapContext.stableCoinGroup);
                TradePairBus _bus = new TradePairBus(AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex)), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, input, amountIn, BigInteger.ZERO, output, amountOut, to);
                list.add(_bus);
                existStablePair = true;
                continue;
            }
            NerveToken[] tokens = SwapUtils.tokenSort(input, output);
            NerveToken token0 = tokens[0];
            // `非`流动性提供者可奖励的交易手续费, 占交易金额的0.1%
            BigInteger unLiquidityAwardFee = amountIn.divide(BI_1000);
            BigInteger amountOut = amounts[i + 1];
            BigInteger amount0Out, amount1Out, amount0In, amount1In, amount0InUnLiquidityAwardFee, amount1InUnLiquidityAwardFee;
            if (input.equals(token0)) {
                amount0In = amountIn;
                amount1In = BigInteger.ZERO;
                amount0InUnLiquidityAwardFee = unLiquidityAwardFee;
                amount1InUnLiquidityAwardFee = BigInteger.ZERO;
                amount0Out = BigInteger.ZERO;
                amount1Out = amountOut;
            } else {
                amount0In = BigInteger.ZERO;
                amount1In = amountIn;
                amount0InUnLiquidityAwardFee = BigInteger.ZERO;
                amount1InUnLiquidityAwardFee = unLiquidityAwardFee;
                amount0Out = amountOut;
                amount1Out = BigInteger.ZERO;
            }
            IPair pair = iPairFactory.getPair(SwapUtils.getStringPairAddress(chainId, input, output));

            SwapPairPO pairPO = pair.getPair();
            NerveToken _token0 = pairPO.getToken0();
            NerveToken _token1 = pairPO.getToken1();
            BigInteger[] reserves = pair.getReserves();
            BigInteger _reserve0 = reserves[0];
            BigInteger _reserve1 = reserves[1];
            if (amount0Out.compareTo(_reserve0) >= 0 || amount1Out.compareTo(_reserve1) >= 0) {
                throw new NulsException(SwapErrorCode.INSUFFICIENT_LIQUIDITY);
            }
            if (to.equals(_token0) || to.equals(_token1)) {
                throw new NulsException(SwapErrorCode.INVALID_TO);
            }
            BigInteger balance0 = _reserve0.add(amount0In).subtract(amount0Out);
            BigInteger balance1 = _reserve1.add(amount1In).subtract(amount1Out);
            BigInteger balance0Adjusted = balance0.multiply(BI_1000).subtract(amount0In.multiply(BI_3));
            BigInteger balance1Adjusted = balance1.multiply(BI_1000).subtract(amount1In.multiply(BI_3));
            if (balance0Adjusted.multiply(balance1Adjusted).compareTo(_reserve0.multiply(_reserve1).multiply(BI_1000_000)) < 0) {
                throw new NulsException(SwapErrorCode.K);
            }
            // balance要扣除即时分配出的手续费奖励
            balance0 = balance0.subtract(amount0InUnLiquidityAwardFee);
            balance1 = balance1.subtract(amount1InUnLiquidityAwardFee);
            // 组装业务数据
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, unLiquidityAwardFee, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        bus.setExistStablePair(existStablePair);
        return bus;
    }

    // swap trade 的path，计算to地址时，若计算的to地址使用的两个token正好是稳定币池币种的话，就使用稳定币交易池地址
    private byte[] calcAddressOfTradeToProtocol17(int chainId, int i, byte[] _to, NerveToken[] path) {
        int length = path.length;
        if (i < length - 2) {
            NerveToken token1 = path[i + 1];
            NerveToken token2 = path[i + 2];
            return this.calcPairAddressProtocol17(chainId, token1, token2);
        } else {
            return _to;
        }
    }

    private byte[] calcPairAddressProtocol17(int chainId, NerveToken token1, NerveToken token2) {
        int groupIndex;
        if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(token1, token2)) != -1) {
            return AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex));
        }
        return SwapUtils.getPairAddress(chainId, token1, token2);
    }

}
