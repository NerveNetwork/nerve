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
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.handler.impl.stable.StableRemoveLiquidityHandler;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.business.linkswap.StableLpSwapTradeBus;
import network.nerve.swap.model.business.linkswap.SwapTradeStableRemoveLpBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.model.txdata.linkswap.SwapTradeStableRemoveLpData;
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
public class SwapTradeStableRemoveLpHandler extends SwapHandlerConstraints {

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
    private SwapHelper swapHelper;
    @Autowired
    private StableRemoveLiquidityHandler stableRemoveLiquidityHandler;
    @Autowired
    private SwapTradeHandler swapTradeHandler;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;

    @Override
    public Integer txType() {
        return TxType.SWAP_TRADE_SWAP_STABLE_REMOVE_LP;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        SwapTradeDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = swapTradeHandler.getSwapTradeInfo(chainId, coinData);
            // 提取业务参数
            SwapTradeStableRemoveLpData txData = new SwapTradeStableRemoveLpData();
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
            if (!Arrays.equals(swapTradeHandler.calcPairAddressProtocol17(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // 路径中的稳定币LP
            NerveToken stableLpToken = path[pathLength - 1];
            String stablePairAddress = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);
            // 验证最后一个token是否为stableLp的token
            if (StringUtils.isBlank(stablePairAddress)) {
                throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
            }
            byte[] stablePairAddressBytes = AddressTool.getAddress(stablePairAddress);
            byte[] swapTradeTo = stablePairAddressBytes;
            // 验证targetToken是否为当前stable池子里的token
            NerveToken targetToken = txData.getTargetToken();
            IStablePair stablePair = iPairFactory.getStablePair(stablePairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken tokenLP = pairPo.getTokenLP();
            NerveToken[] coins = pairPo.getCoins();
            int targetIndex = -1;
            for (int i = 0; i < coins.length; i++) {
                NerveToken coin = coins[i];
                if (targetToken.equals(coin)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) {
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
            // 用户卖出的资产数量
            BigInteger amountIn = dto.getAmountIn();
            // 整合计算数据
            SwapTradeBus swapTradeBus = swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, amountIn,
                    swapTradeTo, path, txData.getAmountOutMin(), txData.getFeeTo());
            List<TradePairBus> pairBuses = swapTradeBus.getTradePairBuses();
            TradePairBus lastPairBus = pairBuses.get(pairBuses.size() - 1);
            BigInteger liquidity = lastPairBus.getAmountOut();// 销毁的LP资产
            StableRemoveLiquidityBus stableRemoveLiquidityBus = SwapUtils.calStableRemoveLiquidityBusiness(swapHelper, chainId, iPairFactory, liquidity, new byte[]{(byte) targetIndex}, stablePairAddressBytes, txData.getTo());

            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            // 集成两个业务数据
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(new SwapTradeStableRemoveLpBus(swapTradeBus, stableRemoveLiquidityBus))));
            result.setSwapTradeBus(swapTradeBus);
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            // 生成swap系统交易
            Transaction sysDealTx0 = swapTradeHandler.makeSystemDealTx(chainId, iPairFactory, swapTradeBus, tx.getHash().toHex(), blockTime, tempBalanceManager, txData.getFeeTo());
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx0, blockTime);
            // 生成撤销稳定币流动性系统交易
            Transaction sysDealTx1 = stableRemoveLiquidityHandler.makeSystemDealTx(chain, stableRemoveLiquidityBus, coins, tokenLP, tx.getHash().toHex(), blockTime, tempBalanceManager);
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx1, blockTime);

            // 集成两个生成的交易
            Transaction sysDealTx = sysDealTx0;
            CoinData coinDataInstance = sysDealTx.getCoinDataInstance();
            CoinData coinData1 = sysDealTx1.getCoinDataInstance();
            coinDataInstance.getFrom().addAll(coinData1.getFrom());
            coinDataInstance.getTo().addAll(coinData1.getTo());
            sysDealTx.setCoinData(SwapUtils.nulsData2HexBytes(coinDataInstance));

            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));

            // 更新临时数据
            List<TradePairBus> busList = swapTradeBus.getTradePairBuses();
            for (TradePairBus pairBus : busList) {
                //协议17: 整合稳定币币池后，稳定币币种1:1兑换
                if (swapTradeBus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                    // 临时缓存更新应该移动到 swapTradeHandler 的 execute 函数中，防止交易验证流程污染了缓存
                    stableSwapTradeHandler.updateCacheByCombining(iPairFactory, pairBus.getStableSwapTradeBus(), blockHeight, blockTime);
                    continue;
                }
                IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeight, blockTime);
            }
            // 更新临时数据
            stablePair.update(liquidity.negate(), SwapUtils.convertNegate(stableRemoveLiquidityBus.getAmounts()), stableRemoveLiquidityBus.getBalances(), blockHeight, blockTime);
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

}
