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
        if (swapHelper.isSupportProtocol24()) {
            //protocol24: Customized transaction fees
            return executeProtocol24(chainId, iPairFactory, tx, blockHeight, blockTime);
        } else if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return executeProtocol17(chainId, iPairFactory, tx, blockHeight, blockTime);
        } else {
            return _execute(chainId, tx, blockHeight, blockTime);
        }
    }

    public SwapTradeBus calSwapTradeBusiness(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
        if (swapHelper.isSupportProtocol24()) {
            //protocol24: Customized transaction fees
            return calSwapTradeBusinessProtocol24(chainId, iPairFactory, _amountIn, _to, path, amountOutMin, feeTo);
        } else if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return calSwapTradeBusinessProtocol17(chainId, iPairFactory, _amountIn, _to, path, amountOutMin, feeTo);
        } else {
            return _calSwapTradeBusiness(chainId, iPairFactory, _amountIn, _to, path, amountOutMin, feeTo);
        }
    }

    /**
     * [Fee allocation]
     *
     * Transaction fee of 0.3% of transaction amount, Two thousandths to liquidity providers
     * One thousandth remaining, the proportion of destruction addresses50%
     * surplus50%Part of
     *     If a handling fee address is specified in the transaction, it will be divided into70%, Remaining30%Provide system address
     *     If no fee address is specified in the transaction, provide the system address
     */
    public Transaction makeSystemDealTx(int chainId, IPairFactory iPairFactory, SwapTradeBus bus, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo, byte[] userAddress) throws Exception{
        if (swapHelper.isSupportProtocol24()) {
            //protocol24: Customized transaction fees
            return makeSystemDealTxProtocol24(chainId, iPairFactory, bus, orginTxHash, blockTime, tempBalanceManager, feeTo, userAddress);
        } else if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return makeSystemDealTxProtocol17(chainId, iPairFactory, bus, orginTxHash, blockTime, tempBalanceManager, feeTo);
        } else {
            return _makeSystemDealTx(bus, orginTxHash, blockTime, tempBalanceManager, feeTo);
        }
    }

    private SwapResult _execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getSwapTradeInfo(chainId, coinData);
            // Extract business parameters
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // inspectfirstPairAddressIs it related topathConsistent in
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 2) {
                throw new NulsException(INVALID_PATH);
            }
            if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // Number of assets sold by users
            BigInteger amountIn = dto.getAmountIn();

            // Integrate computing data
            SwapTradeBus bus = this._calSwapTradeBusiness(chainId, iPairFactory, amountIn,
                    txData.getTo(), path, txData.getAmountOutMin(), txData.getFeeTo());
            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // Assembly system transaction
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this._makeSystemDealTx(bus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // Update temporary data
            List<TradePairBus> busList = bus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
        } catch (Exception e) {
            Log.error(e);
            // Execution results of failed loading
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
            // Assembly system return transaction
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
            // Update temporary balance
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
            // Extract business parameters
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // inspectfirstPairAddressIs it related topathConsistent in
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 2) {
                throw new NulsException(INVALID_PATH);
            }
            if (!Arrays.equals(this.calcPairAddressProtocol17(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // Number of assets sold by users
            BigInteger amountIn = dto.getAmountIn();

            // Integrate computing data
            SwapTradeBus bus = this.calSwapTradeBusinessProtocol17(chainId, iPairFactory, amountIn,
                    txData.getTo(), path, txData.getAmountOutMin(), txData.getFeeTo());
            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            result.setSwapTradeBus(bus);
            // Assembly system transaction
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTxProtocol17(chainId, iPairFactory, bus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // Update temporary data
            List<TradePairBus> busList = bus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
                if (bus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                    // Temporary cache updates should be moved to swapTradeHandler of execute In the function, prevent the transaction verification process from contaminating the cache
                    stableSwapTradeHandler.updateCacheByCombining(iPairFactory, pairBus.getStableSwapTradeBus(), blockHeight, blockTime);
                    continue;
                }
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
        } catch (Exception e) {
            Log.error(e);
            // Execution results of failed loading
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
            // Assembly system return transaction
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
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, refundTx, blockTime);
        } finally {
            batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        }
        return result;
    }

    private SwapResult executeProtocol24(int chainId, IPairFactory iPairFactory, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getSwapTradeInfo(chainId, coinData);
            // Extract business parameters
            SwapTradeData txData = new SwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // inspectfirstPairAddressIs it related topathConsistent in
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 2) {
                throw new NulsException(INVALID_PATH);
            }
            if (!Arrays.equals(this.calcPairAddressProtocol17(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // Number of assets sold by users
            BigInteger amountIn = dto.getAmountIn();

            // Integrate computing data
            SwapTradeBus bus = this.calSwapTradeBusinessProtocol24(chainId, iPairFactory, amountIn,
                    txData.getTo(), path, txData.getAmountOutMin(), txData.getFeeTo());
            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            result.setSwapTradeBus(bus);
            // Assembly system transaction
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTxProtocol24(chainId, iPairFactory, bus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo(), dto.getUserAddress());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // Update temporary data
            List<TradePairBus> busList = bus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
                if (bus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                    // Temporary cache updates should be moved to swapTradeHandler of execute In the function, prevent the transaction verification process from contaminating the cache
                    stableSwapTradeHandler.updateCacheByCombining(iPairFactory, pairBus.getStableSwapTradeBus(), blockHeight, blockTime);
                    continue;
                }
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
        } catch (Exception e) {
            Log.error(e);
            // Execution results of failed loading
            result.setTxType(txType());
            result.setSuccess(false);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setErrorMessage(e instanceof NulsException ? ((NulsException) e).format() : e.getMessage());

            if (dto == null) {
                return result;
            }
            // Assembly system return transaction
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
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, refundTx, blockTime);
        } finally {
            batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        }
        return result;
    }



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
            /** Calculate the distribution of handling fees */
            do {

                LedgerBalance ledgerBalanceFeeOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                // `wrong`Transaction fees that liquidity providers can reward
                BigInteger unLiquidityAwardFee = pairBus.getUnLiquidityAwardFee();
                if (unLiquidityAwardFee.equals(BigInteger.ZERO)) {
                    break;
                }
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceFeeOut, unLiquidityAwardFee).endFrom();
                // Reward for handling fees for destroying addresses, percentage50%（stay`wrong`Among the transaction fees that liquidity providers can reward）
                BigInteger destructionAwardFee = unLiquidityAwardFee.divide(BI_2);
                // The amount is too small, not enough divide 2Then all will be allocated to the destruction address
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
                // Reward for system address handling fees
                BigInteger systemAwardFee;
                // Reward for handling fees at the specified address of the current transaction
                BigInteger assignAddrAwardFee;
                if (feeTo == null) {
                    // If the transaction does not specify a fee reward address, the remaining fee reward will be distributed to the system address（After deducting the destroyed portion）
                    systemAwardFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                } else {
                    BigInteger tempFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                    // The percentage of transaction fees awarded to the designated address of the current transaction70%（After deducting the destroyed portion）
                    assignAddrAwardFee= tempFee.multiply(BI_7).divide(BI_10);
                    // Remaining transaction fees to the system
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

    private Transaction makeSystemDealTxProtocol17(int chainId, IPairFactory iPairFactory, SwapTradeBus bus, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo) throws Exception {
        List<TradePairBus> busList = bus.getTradePairBuses();
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        NerveToken tokenIn, tokenOut;
        for (TradePairBus pairBus : busList) {
            tokenIn = pairBus.getTokenIn();
            tokenOut = pairBus.getTokenOut();
            // After integrating the stablecoin pool, stablecoin currencies1:1exchange
            if (bus.isExistStablePair() && SwapUtils.groupCombining(tokenIn, tokenOut)) {
                StableSwapTradeBus stableSwapTradeBus = stableSwapTradeHandler.tradeByCombining(chainId, iPairFactory, pairBus.getPairAddress(), pairBus.getTo(),
                        tempBalanceManager, pairBus.getTokenIn(), pairBus.getAmountIn(), pairBus.getTokenOut(), sysDeal, null);
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
            /** Calculate the distribution of handling fees */
            do {
                LedgerBalance ledgerBalanceFeeOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                // `wrong`Transaction fees that liquidity providers can reward
                BigInteger unLiquidityAwardFee = pairBus.getUnLiquidityAwardFee();
                if (unLiquidityAwardFee.equals(BigInteger.ZERO)) {
                    break;
                }
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceFeeOut, unLiquidityAwardFee).endFrom();
                // Reward for handling fees for destroying addresses, percentage50%（stay`wrong`Among the transaction fees that liquidity providers can reward）
                BigInteger destructionAwardFee = unLiquidityAwardFee.divide(BI_2);
                // The amount is too small, not enough divide 2Then all will be allocated to the destruction address
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
                // Reward for system address handling fees
                BigInteger systemAwardFee;
                // Reward for handling fees at the specified address of the current transaction
                BigInteger assignAddrAwardFee;
                if (feeTo == null) {
                    // If the transaction does not specify a fee reward address, the remaining fee reward will be distributed to the system address（After deducting the destroyed portion）
                    systemAwardFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                } else {
                    BigInteger tempFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                    // The percentage of transaction fees awarded to the designated address of the current transaction70%（After deducting the destroyed portion）
                    assignAddrAwardFee= tempFee.multiply(BI_7).divide(BI_10);
                    // Remaining transaction fees to the system
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

    private Transaction makeSystemDealTxProtocol24(int chainId, IPairFactory iPairFactory, SwapTradeBus bus, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo, byte[] userAddress) throws Exception {
        List<TradePairBus> busList = bus.getTradePairBuses();
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        NerveToken tokenIn, tokenOut;
        for (TradePairBus pairBus : busList) {
            tokenIn = pairBus.getTokenIn();
            tokenOut = pairBus.getTokenOut();
            // After integrating the stablecoin pool, stablecoin currencies1:1exchange
            if (bus.isExistStablePair() && SwapUtils.groupCombining(tokenIn, tokenOut)) {
                StableSwapTradeBus stableSwapTradeBus = stableSwapTradeHandler.tradeByCombining(chainId, iPairFactory, pairBus.getPairAddress(), pairBus.getTo(),
                        tempBalanceManager, pairBus.getTokenIn(), pairBus.getAmountIn(), pairBus.getTokenOut(), sysDeal, userAddress);
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
            /** Calculate the distribution of handling fees */
            do {
                LedgerBalance ledgerBalanceFeeOut = tempBalanceManager.getBalance(pairBus.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                // `wrong`Transaction fees that liquidity providers can reward
                BigInteger unLiquidityAwardFee = pairBus.getUnLiquidityAwardFee();
                if (unLiquidityAwardFee.equals(BigInteger.ZERO)) {
                    break;
                }
                sysDeal.newFrom()
                        .setFrom(ledgerBalanceFeeOut, unLiquidityAwardFee).endFrom();
                // Reward for handling fees for destroying addresses, percentage50%（stay`wrong`Among the transaction fees that liquidity providers can reward）
                BigInteger destructionAwardFee = unLiquidityAwardFee.divide(BI_2);
                // The amount is too small, not enough divide 2Then all will be allocated to the destruction address
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
                // Reward for system address handling fees
                BigInteger systemAwardFee;
                // Reward for handling fees at the specified address of the current transaction
                BigInteger assignAddrAwardFee;
                if (feeTo == null) {
                    // If the transaction does not specify a fee reward address, the remaining fee reward will be distributed to the system address（After deducting the destroyed portion）
                    systemAwardFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                } else {
                    BigInteger tempFee = unLiquidityAwardFee.subtract(destructionAwardFee);
                    // The percentage of transaction fees awarded to the designated address of the current transaction70%（After deducting the destroyed portion）
                    assignAddrAwardFee= tempFee.multiply(BI_7).divide(BI_10);
                    // Remaining transaction fees to the system
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



    private SwapTradeBus _calSwapTradeBusiness(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
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
            // `wrong`Transaction fees that liquidity providers can reward, % of transaction amount0.1%
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
            // balanceTo deduct the immediately allocated handling fee reward
            balance0 = balance0.subtract(amount0InUnLiquidityAwardFee);
            balance1 = balance1.subtract(amount1InUnLiquidityAwardFee);
            // Assembling business data
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, unLiquidityAwardFee, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        return bus;
    }

    private SwapTradeBus calSwapTradeBusinessProtocol17(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
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
            // swap trade ofpath, CalculatetoWhen calculating the address, iftoTwo addresses usedtokenIf it happens to be a stablecoin pool currency, use the stablecoin trading pool address
            byte[] to = this.calcAddressOfTradeToProtocol17(chainId, i, _to, path);
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            int groupIndex;
            if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(input, output)) != -1) {
                // Assembling business data
                BigInteger amountOut = SwapUtils.getStableOutAmountByGroupIndex(groupIndex, input, amountIn, output, iPairFactory, SwapContext.stableCoinGroup);
                TradePairBus _bus = new TradePairBus(AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex)), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, input, amountIn, BigInteger.ZERO, output, amountOut, to);
                list.add(_bus);
                existStablePair = true;
                continue;
            }
            NerveToken[] tokens = SwapUtils.tokenSort(input, output);
            NerveToken token0 = tokens[0];
            // `wrong`Transaction fees that liquidity providers can reward, % of transaction amount0.1%
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
            // balanceTo deduct the immediately allocated handling fee reward
            balance0 = balance0.subtract(amount0InUnLiquidityAwardFee);
            balance1 = balance1.subtract(amount1InUnLiquidityAwardFee);
            // Assembling business data
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, unLiquidityAwardFee, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        bus.setExistStablePair(existStablePair);
        return bus;
    }

    private SwapTradeBus calSwapTradeBusinessProtocol24(int chainId, IPairFactory iPairFactory, BigInteger _amountIn, byte[] _to, NerveToken[] path, BigInteger amountOutMin, byte[] feeTo) throws NulsException {
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
            // swap trade ofpath, CalculatetoWhen calculating the address, iftoTwo addresses usedtokenIf it happens to be a stablecoin pool currency, use the stablecoin trading pool address
            byte[] to = this.calcAddressOfTradeToProtocol17(chainId, i, _to, path);
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            int groupIndex;
            if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(input, output)) != -1) {
                // Assembling business data
                BigInteger amountOut = SwapUtils.getStableOutAmountByGroupIndex(groupIndex, input, amountIn, output, iPairFactory, SwapContext.stableCoinGroup);
                TradePairBus _bus = new TradePairBus(AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex)), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, input, amountIn, BigInteger.ZERO, output, amountOut, to);
                list.add(_bus);
                existStablePair = true;
                continue;
            }
            NerveToken[] tokens = SwapUtils.tokenSort(input, output);
            NerveToken token0 = tokens[0];
            // `wrong`Transaction fees that liquidity providers can reward, % of transaction amount0.1%
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
            BigInteger feeRate = BigInteger.valueOf(pairPO.getFeeRate());// p24
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
            BigInteger balance0Adjusted = balance0.multiply(BI_1000).subtract(amount0In.multiply(feeRate));// p24
            BigInteger balance1Adjusted = balance1.multiply(BI_1000).subtract(amount1In.multiply(feeRate));// p24
            if (balance0Adjusted.multiply(balance1Adjusted).compareTo(_reserve0.multiply(_reserve1).multiply(BI_1000_000)) < 0) {
                throw new NulsException(SwapErrorCode.K);
            }
            // balanceTo deduct the immediately allocated handling fee reward
            balance0 = balance0.subtract(amount0InUnLiquidityAwardFee);
            balance1 = balance1.subtract(amount1InUnLiquidityAwardFee);
            // Assembling business data
            TradePairBus _bus = new TradePairBus(pairPO.getAddress(), balance0, balance1, _reserve0, _reserve1, input, amountIn, unLiquidityAwardFee, output, amountOut, to);
            _bus.setPreBlockHeight(pair.getBlockHeightLast());
            _bus.setPreBlockTime(pair.getBlockTimeLast());
            list.add(_bus);
        }
        SwapTradeBus bus = new SwapTradeBus(list);
        bus.setExistStablePair(existStablePair);
        return bus;
    }

    // swap trade ofpath, CalculatetoWhen calculating the address, iftoTwo addresses usedtokenIf it happens to be a stablecoin pool currency, use the stablecoin trading pool address
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

    public byte[] calcPairAddressProtocol17(int chainId, NerveToken token1, NerveToken token2) {
        int groupIndex;
        if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(token1, token2)) != -1) {
            return AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex));
        }
        return SwapUtils.getPairAddress(chainId, token1, token2);
    }

}
