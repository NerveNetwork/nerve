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
package network.nerve.swap.handler.impl.stable;

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
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.stable.StableSwapTradeDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static network.nerve.swap.constant.SwapConstant.BI_100;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class StableSwapTradeHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapHelper swapHelper;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;

    @Override
    public Integer txType() {
        return TxType.SWAP_TRADE_STABLE_COIN;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        if (swapHelper.isSupportProtocol21()) {
            return this.executeP21(chainId, tx, blockHeight, blockTime);
        } else {
            return this.executeP0(chainId, tx, blockHeight, blockTime);
        }
    }

    private SwapResult executeP0(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        StableSwapTradeDTO dto = null;
        try {
            // 提取业务参数
            StableSwapTradeData txData = new StableSwapTradeData();
            txData.parse(tx.getTxData(), 0);
            byte tokenOutIndex = txData.getTokenOutIndex();
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getStableSwapTradeInfo(chainId, coinData, iPairFactory, tokenOutIndex);

            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken[] coins = pairPo.getCoins();
            // 整合计算数据
            StableSwapTradeBus bus = SwapUtils.calStableSwapTradeBusiness(chainId, iPairFactory, dto.getAmountsIn(), tokenOutIndex, dto.getPairAddress(), txData.getTo(), txData.getFeeTo());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(bus, coins, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            stablePair.update(BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), blockHeight, blockTime);
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
            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken[] coins = pairPo.getCoins();
            int length = coins.length;
            // 组装系统退还交易
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            BigInteger[] amountsIn = dto.getAmountsIn();
            for (int i = 0; i < length; i++) {
                BigInteger amountIn = amountsIn[i];
                if (BigInteger.ZERO.equals(amountIn)) {
                    continue;
                }
                NerveToken tokenIn = coins[i];
                LedgerBalance ledgerBalanceIn = tempBalanceManager.getBalance(dto.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                refund.newFrom()
                        .setFrom(ledgerBalanceIn, amountIn).endFrom()
                        .newTo()
                        .setToAddress(dto.getUserAddress())
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(amountIn).endTo();
            }
            Transaction refundTx = refund.build();
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

    private SwapResult executeP21(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        StableSwapTradeDTO dto = null;
        try {
            // 提取业务参数
            StableSwapTradeData txData = new StableSwapTradeData();
            txData.parse(tx.getTxData(), 0);
            byte tokenOutIndex = txData.getTokenOutIndex();
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getStableSwapTradeInfoP21(chainId, coinData, iPairFactory, tokenOutIndex, txData.getFeeTo());

            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken[] coins = pairPo.getCoins();
            // 整合计算数据
            StableSwapTradeBus bus = SwapUtils.calStableSwapTradeBusinessP21(chainId, iPairFactory, dto.getAmountsIn(), tokenOutIndex, dto.getPairAddress(), txData.getTo(), dto.getFeeTo());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(bus, coins, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            stablePair.update(BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), blockHeight, blockTime);
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
            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken[] coins = pairPo.getCoins();
            int length = coins.length;
            // 组装系统退还交易
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            BigInteger[] amountsIn = dto.getAmountsIn();
            for (int i = 0; i < length; i++) {
                BigInteger amountIn = amountsIn[i];
                if (BigInteger.ZERO.equals(amountIn)) {
                    continue;
                }
                NerveToken tokenIn = coins[i];
                LedgerBalance ledgerBalanceIn = tempBalanceManager.getBalance(dto.getPairAddress(), tokenIn.getChainId(), tokenIn.getAssetId()).getData();
                refund.newFrom()
                        .setFrom(ledgerBalanceIn, amountIn).endFrom()
                        .newTo()
                        .setToAddress(dto.getUserAddress())
                        .setToAssetsChainId(tokenIn.getChainId())
                        .setToAssetsId(tokenIn.getAssetId())
                        .setToAmount(amountIn).endTo();
            }
            Transaction refundTx = refund.build();
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

    private Transaction makeSystemDealTx(StableSwapTradeBus bus, NerveToken[] coins, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo) {
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        this.makeSystemDealTxInner(sysDeal, bus, coins, tempBalanceManager, feeTo);
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    private void makeSystemDealTxInner(SwapSystemDealTransaction sysDeal, StableSwapTradeBus bus, NerveToken[] coins, LedgerTempBalanceManager tempBalanceManager, byte[] feeTo) {
        byte tokenOutIndex = bus.getTokenOutIndex();
        BigInteger amountOut = bus.getAmountOut();
        int length = coins.length;
        BigInteger[] unLiquidityAwardFees = bus.getUnLiquidityAwardFees();
        byte[] pairAddress = bus.getPairAddress();
        byte[] to = bus.getTo();
        // 从池子中流出的币种(用户买进的)
        NerveToken outCoin = coins[tokenOutIndex];
        LedgerBalance outCoinBalance = tempBalanceManager.getBalance(pairAddress, outCoin.getChainId(), outCoin.getAssetId()).getData();
        // 从池子中转移到用户接收地址
        sysDeal.newFrom()
                .setFrom(outCoinBalance, amountOut).endFrom()
                .newTo()
                .setToAddress(to)
                .setToAssetsChainId(outCoin.getChainId())
                .setToAssetsId(outCoin.getAssetId())
                .setToAmount(amountOut).endTo();
        /** 计算手续费分配 */
        // 协议17: 使用新的手续费接收地址
        byte[] awardFeeSystemAddress;
        if (swapHelper.isSupportProtocol17()) {
            awardFeeSystemAddress = SwapContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0;
        } else {
            awardFeeSystemAddress = SwapContext.AWARD_FEE_SYSTEM_ADDRESS;
        }
        // 从池子中转移手续费到指定接收地址(`非`流动性提供者可奖励的交易手续费)
        for (int i = 0; i < length; i++) {
            BigInteger fee = unLiquidityAwardFees[i];
            if (fee == null || fee.equals(BigInteger.ZERO)) {
                continue;
            }
            NerveToken coin = coins[i];
            LedgerBalance feeCoinBalance = tempBalanceManager.getBalance(pairAddress, coin.getChainId(), coin.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(feeCoinBalance, fee).endFrom();
            // 系统地址的手续费奖励
            BigInteger systemAwardFee;
            // 当前交易的指定地址的手续费奖励
            BigInteger assignAddrAwardFee;
            if (feeTo == null) {
                // 交易未指定手续费奖励地址，则这部分手续费奖励分发给系统地址
                systemAwardFee = fee;
                sysDeal.newTo()
                        .setToAddress(awardFeeSystemAddress)
                        .setToAssetsChainId(coin.getChainId())
                        .setToAssetsId(coin.getAssetId())
                        .setToAmount(systemAwardFee).endTo();
            } else {
                // 其中奖励给系统的交易手续费
                systemAwardFee = fee.multiply(SwapContext.FEE_PERCENT_SYSTEM_RECEIVE_STABLE_SWAP).divide(BI_100);
                // 其中奖励给当前交易指定地址的交易手续费
                assignAddrAwardFee = fee.subtract(systemAwardFee);
                sysDeal.newTo()
                        .setToAddress(awardFeeSystemAddress)
                        .setToAssetsChainId(coin.getChainId())
                        .setToAssetsId(coin.getAssetId())
                        .setToAmount(systemAwardFee).endTo()
                        .newTo()
                        .setToAddress(feeTo)
                        .setToAssetsChainId(coin.getChainId())
                        .setToAssetsId(coin.getAssetId())
                        .setToAmount(assignAddrAwardFee).endTo();
            }
        }
    }

    public StableSwapTradeDTO getStableSwapTradeInfo(int chainId, CoinData coinData, IPairFactory iPairFactory, byte tokenOutIndex) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.isEmpty()) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
        }
        byte[] pairAddressBytes = tos.get(0).getAddress();
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        if (!stableSwapPairCache.isExist(pairAddress)) {
            throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
        }
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        // 选出用户卖出的资产
        BigInteger[] amountsIn = new BigInteger[length];
        for (CoinTo to : tos) {
            if (to.getLockTime() != 0) {
                throw new NulsException(SwapErrorCode.SWAP_TRADE_AMOUNT_LOCK_ERROR);
            }
            if (!Arrays.equals(pairAddressBytes, to.getAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
            }
            boolean exist = false;
            for (int i = 0; i < length; i++) {
                NerveToken coin = coins[i];
                if (coin.getChainId() == to.getAssetsChainId() && coin.getAssetId() == to.getAssetsId()) {
                    // 用户买进的资产不能是卖出的资产
                    if (tokenOutIndex == i) {
                        throw new NulsException(SwapErrorCode.SWAP_TRADE_RECEIVE_ERROR);
                    }
                    amountsIn[i] = to.getAmount();
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
            }
        }
        // 空值填充
        amountsIn = SwapUtils.emptyFillZero(amountsIn);
        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != tos.size()) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        byte[] _from = null;
        for (CoinFrom from : froms) {
            if (_from == null) {
                _from = from.getAddress();
            } else if (!Arrays.equals(_from, from.getAddress())) {
                throw new NulsException(SwapErrorCode.IDENTICAL_ADDRESSES);
            }
        }
        if (_from == null) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        return new StableSwapTradeDTO(_from, pairAddressBytes, amountsIn);
    }

    public StableSwapTradeDTO getStableSwapTradeInfoP21(int chainId, CoinData coinData, IPairFactory iPairFactory, byte tokenOutIndex, byte[] feeTo) throws NulsException {
        if (coinData == null) {
            return null;
        }
        boolean hasFeeTo = feeTo != null;
        if (hasFeeTo && !AddressTool.validAddress(chainId, feeTo)) {
            throw new NulsException(SwapErrorCode.FEE_RECEIVE_ADDRESS_ERROR);
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.isEmpty()) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
        }
        byte[] toAddress0 = tos.get(0).getAddress();
        byte[] pairAddressBytes;
        if (!hasFeeTo || tos.size() == 1) {
            pairAddressBytes = toAddress0;
        } else {
            pairAddressBytes = Arrays.equals(feeTo, toAddress0) ? tos.get(1).getAddress() : toAddress0;
        }
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        if (!stableSwapPairCache.isExist(pairAddress)) {
            throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
        }
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        // 选出用户卖出的资产
        BigInteger[] amountsIn = new BigInteger[length];
        //BigInteger feeAmount = null;
        CoinTo feeCoin = null;
        for (CoinTo to : tos) {
            if (to.getLockTime() != 0) {
                throw new NulsException(SwapErrorCode.SWAP_TRADE_AMOUNT_LOCK_ERROR);
            }
            if (hasFeeTo && Arrays.equals(feeTo, to.getAddress())) {
                if (feeCoin != null) {
                    // 只允许一个feeTo
                    throw new NulsException(SwapErrorCode.FEE_RECEIVE_ADDRESS_ERROR);
                }
                feeCoin = to;
                continue;
            }
            if (!Arrays.equals(pairAddressBytes, to.getAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
            }
            boolean exist = false;
            for (int i = 0; i < length; i++) {
                NerveToken coin = coins[i];
                if (coin.getChainId() == to.getAssetsChainId() && coin.getAssetId() == to.getAssetsId()) {
                    // 用户买进的资产不能是卖出的资产
                    if (tokenOutIndex == i) {
                        throw new NulsException(SwapErrorCode.SWAP_TRADE_RECEIVE_ERROR);
                    }
                    amountsIn[i] = to.getAmount();
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                throw new NulsException(SwapErrorCode.SWAP_TRADE_TOS_ERROR);
            }
        }
        boolean hasFeeCoin = feeCoin != null;
        // 空值填充
        amountsIn = SwapUtils.emptyFillZero(amountsIn);
        List<CoinFrom> froms = coinData.getFrom();
        int _fromsSize = froms.size();
        if (hasFeeCoin) {
            _fromsSize++;
        }
        if (_fromsSize != tos.size()) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        byte[] _from = null;
        for (CoinFrom from : froms) {
            if (_from == null) {
                _from = from.getAddress();
            } else if (!Arrays.equals(_from, from.getAddress())) {
                throw new NulsException(SwapErrorCode.IDENTICAL_ADDRESSES);
            }
        }
        if (_from == null) {
            throw new NulsException(SwapErrorCode.SWAP_TRADE_FROMS_ERROR);
        }
        return new StableSwapTradeDTO(_from, pairAddressBytes, amountsIn, feeCoin);
    }

    // 整合稳定币币池后，普通SWAP调用稳定币币池函数，稳定币币种1:1兑换
    public StableSwapTradeBus tradeByCombining(int chainId, IPairFactory iPairFactory, byte[] pairAddressBytes, byte[] to, LedgerTempBalanceManager tempBalanceManager, NerveToken tokenIn, BigInteger amountIn, NerveToken tokenOut, SwapSystemDealTransaction sysDeal) throws Exception {
        String pairAddress = AddressTool.getStringAddressByBytes(pairAddressBytes);
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        StableSwapPairPo pairPo = stablePair.getPair();
        NerveToken[] coins = pairPo.getCoins();
        int tokenInIndex = 0, tokenOutIndex = 0, length = coins.length;
        for (int i = 0; i < length; i++) {
            NerveToken token = coins[i];
            if (token.equals(tokenIn)) {
                tokenInIndex = i;
            } else if (token.equals(tokenOut)) {
                tokenOutIndex = i;
            }
        }
        BigInteger[] amountsIn = SwapUtils.emptyFillZero(new BigInteger[length]);
        amountsIn[tokenInIndex] = amountIn;
        StableSwapTradeBus bus = SwapUtils.calStableSwapTradeBusiness(chainId, iPairFactory, amountsIn, (byte) tokenOutIndex, pairAddressBytes, to, null);
        this.makeSystemDealTxInner(sysDeal, bus, coins, tempBalanceManager, null);
        return bus;
    }

    // 临时缓存更新，整合稳定币币池后，普通SWAP调用稳定币币池函数，稳定币币种1:1兑换
    public void updateCacheByCombining(IPairFactory iPairFactory, StableSwapTradeBus bus, long blockHeight, long blockTime) throws Exception {
        String pairAddress = AddressTool.getStringAddressByBytes(bus.getPairAddress());
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        stablePair.update(BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), blockHeight, blockTime);
    }

}
