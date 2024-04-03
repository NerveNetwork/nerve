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
package network.nerve.swap.handler.impl.linkswap;

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
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.handler.impl.SwapTradeHandler;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.business.linkswap.StableLpSwapTradeBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.dto.stable.StableAddLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.linkswap.StableLpSwapTradeData;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static network.nerve.swap.constant.SwapErrorCode.INVALID_PATH;
import static network.nerve.swap.constant.SwapErrorCode.PAIR_NOT_EXIST;

/**
 * @author: PierreLuo
 * @date: 2021/12/30
 */
@Component
public class StableLpSwapTradeHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableAddLiquidityHandler stableAddLiquidityHandler;
    @Autowired
    private SwapTradeHandler swapTradeHandler;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public Integer txType() {
        return TxType.SWAP_STABLE_LP_SWAP_TRADE;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        StableAddLiquidityDTO dto = null;
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, coinData, iPairFactory);
            List<CoinTo> tos = coinData.getTo();
            if (tos.size() > 1) {
                throw new NulsException(SwapErrorCode.INVALID_TO);
            }
            // Extract business parameters
            StableLpSwapTradeData txData = new StableLpSwapTradeData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            // Check transaction path
            NerveToken[] path = txData.getPath();
            int pathLength = path.length;
            if (pathLength < 3) {
                throw new NulsException(INVALID_PATH);
            }
            NerveToken firstToken = path[0];
            NerveToken stableLpToken = path[1];
            // Stable coins in the path
            CoinTo coinTo = coinData.getTo().get(0);
            if (coinTo.getAssetsChainId() != firstToken.getChainId() || coinTo.getAssetsId() != firstToken.getAssetId()) {
                throw new NulsException(SwapErrorCode.INVALID_TO);
            }
            // Stable coins in the pathLP
            String pairAddressByTokenLP = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);
            if (!dto.getPairAddress().equals(pairAddressByTokenLP)) {
                throw new NulsException(INVALID_PATH);
            }

            // The first ordinaryswapThe transaction is addressed by path[1], path[2] Transaction pairs composed of
            byte[] firstSwapPair = SwapUtils.getPairAddress(chainId, path[1], path[2]);
            if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(firstSwapPair))) {
                throw new NulsException(PAIR_NOT_EXIST);
            }
            String pairAddress = dto.getPairAddress();
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            // Integrate computing data
            StableAddLiquidityBus stableAddLiquidityBus = SwapUtils.calStableAddLiquididy(swapHelper, chainId, iPairFactory, pairAddress, dto.getFrom(), dto.getAmounts(), firstSwapPair);
            NerveToken[] swapTradePath = new NerveToken[path.length - 1];
            System.arraycopy(path, 1, swapTradePath, 0, path.length - 1);
            SwapTradeBus swapTradeBus = swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, stableAddLiquidityBus.getLiquidity(), txData.getTo(), swapTradePath, txData.getAmountOutMin(), txData.getFeeTo());


            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            // Integrate two business data
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(new StableLpSwapTradeBus(stableAddLiquidityBus, swapTradeBus))));
            // Assembly system transaction
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx0 = stableAddLiquidityHandler.makeSystemDealTx(stableAddLiquidityBus, pairPo.getCoins(), pairPo.getTokenLP(), tx.getHash().toHex(), blockTime, tempBalanceManager);
            // generateswapSystem transactions
            Transaction sysDealTx1 = swapTradeHandler.makeSystemDealTx(chainId, iPairFactory, swapTradeBus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo(), dto.getFrom());

            // Integrate two generated transactions
            Transaction sysDealTx = sysDealTx0;
            CoinData coinDataInstance = sysDealTx.getCoinDataInstance();
            CoinData coinData1 = sysDealTx1.getCoinDataInstance();
            coinDataInstance.getFrom().addAll(coinData1.getFrom());
            coinDataInstance.getTo().addAll(coinData1.getTo());
            sysDealTx.setCoinData(SwapUtils.nulsData2HexBytes(coinDataInstance));

            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // update StableAddLiquidity Temporary data
            stablePair.update(stableAddLiquidityBus.getLiquidity(), stableAddLiquidityBus.getRealAmounts(), stableAddLiquidityBus.getBalances(), blockHeight, blockTime);
            // update SwapTrade Temporary data
            List<TradePairBus> busList = swapTradeBus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
                if (swapTradeBus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
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
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            String pairAddress = dto.getPairAddress();
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken[] coins = pairPo.getCoins();
            BigInteger[] amounts = dto.getAmounts();
            byte[] from = dto.getFrom();
            int length = coins.length;
            for (int i = 0; i < length; i++) {
                BigInteger amount = amounts[i];
                if (amount.equals(BigInteger.ZERO)) {
                    continue;
                }
                NerveToken coin = coins[i];
                LedgerBalance balance = tempBalanceManager.getBalance(AddressTool.getAddress(pairAddress), coin.getChainId(), coin.getAssetId()).getData();
                refund.newFrom()
                        .setFrom(balance, amount).endFrom()
                        .newTo()
                        .setToAddress(from)
                        .setToAssetsChainId(coin.getChainId())
                        .setToAssetsId(coin.getAssetId())
                        .setToAmount(amount).endTo();
            }
            Transaction refundTx = refund.build();
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

}
