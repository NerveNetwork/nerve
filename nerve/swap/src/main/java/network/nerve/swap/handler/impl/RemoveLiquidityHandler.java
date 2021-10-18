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
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.RemoveLiquidityDTO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.RemoveLiquidityData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class RemoveLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    @Override
    public Integer txType() {
        return TxType.SWAP_REMOVE_LIQUIDITY;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        RemoveLiquidityDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = getRemoveLiquidityInfo(chainId, coinData);
            // 提取业务参数
            RemoveLiquidityData txData = new RemoveLiquidityData();
            txData.parse(tx.getTxData(), 0);
            long deadline = txData.getDeadline();
            if (blockTime > deadline) {
                throw new NulsException(SwapErrorCode.EXPIRED);
            }
            if (!AddressTool.validAddress(chainId, txData.getTo())) {
                throw new NulsException(SwapErrorCode.RECEIVE_ADDRESS_ERROR);
            }
            NerveToken tokenA = txData.getTokenA();
            NerveToken tokenB = txData.getTokenB();
            // 检查tokenA,B是否存在，pair地址是否合法
            LedgerAssetDTO assetA = ledgerAssetCache.getLedgerAsset(chainId, tokenA);
            LedgerAssetDTO assetB = ledgerAssetCache.getLedgerAsset(chainId, tokenB);
            if (assetA == null || assetB == null) {
                throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
            }
            if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(dto.getPairAddress()))) {
                throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
            }
            if (!Arrays.equals(SwapUtils.getPairAddress(chainId, tokenA, tokenB), dto.getPairAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
            }
            // 销毁的LP资产
            BigInteger liquidity = dto.getLiquidity();

            // 整合计算数据
            RemoveLiquidityBus bus = SwapUtils.calRemoveLiquidityBusiness(chainId, iPairFactory, dto.getPairAddress(), liquidity,
                    tokenA, tokenB, txData.getAmountAMin(), txData.getAmountBMin());

            IPair pair = bus.getPair();
            BigInteger amount0 = bus.getAmount0();
            BigInteger amount1 = bus.getAmount1();

            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            NerveToken tokenLP = pair.getPair().getTokenLP();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(chain, bus, dto, tokenLP, txData.getTo(), tx.getHash().toHex(), blockTime, tempBalanceManager);
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            BigInteger balance0 = bus.getReserve0().subtract(amount0);
            BigInteger balance1 = bus.getReserve1().subtract(amount1);
            pair.update(liquidity.negate(), balance0, balance1, bus.getReserve0(), bus.getReserve1(), blockHeight, blockTime);
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
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(dto.getPairAddress()));
            NerveToken tokenLP = pair.getPair().getTokenLP();
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance ledgerBalanceLp = tempBalanceManager.getBalance(dto.getPairAddress(), tokenLP.getChainId(), tokenLP.getAssetId()).getData();
            Transaction refundTx =
                refund.newFrom()
                        .setFrom(ledgerBalanceLp, dto.getLiquidity()).endFrom()
                      .newTo()
                        .setToAddress(dto.getUserAddress())
                        .setToAssetsChainId(tokenLP.getChainId())
                        .setToAssetsId(tokenLP.getAssetId())
                        .setToAmount(dto.getLiquidity()).endTo()
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

    private Transaction makeSystemDealTx(Chain chain, RemoveLiquidityBus bus, RemoveLiquidityDTO dto, NerveToken tokenLP, byte[] to, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager) {
        NerveToken token0 = bus.getToken0();
        NerveToken token1 = bus.getToken1();
        BigInteger amount0 = bus.getAmount0();
        BigInteger amount1 = bus.getAmount1();
        LedgerBalance ledgerBalance0 = tempBalanceManager.getBalance(dto.getPairAddress(), token0.getChainId(), token0.getAssetId()).getData();
        LedgerBalance ledgerBalance1 = tempBalanceManager.getBalance(dto.getPairAddress(), token1.getChainId(), token1.getAssetId()).getData();
        LedgerBalance ledgerBalanceLp = tempBalanceManager.getBalance(dto.getPairAddress(), tokenLP.getChainId(), tokenLP.getAssetId()).getData();

        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        sysDeal.newFrom()
                .setFrom(ledgerBalance0, amount0).endFrom()
                .newFrom()
                .setFrom(ledgerBalance1, amount1).endFrom()
                .newFrom()
                .setFrom(ledgerBalanceLp, dto.getLiquidity()).endFrom()
                .newTo()
                .setToAddress(to)
                .setToAssetsChainId(token0.getChainId())
                .setToAssetsId(token0.getAssetId())
                .setToAmount(amount0).endTo()
                .newTo()
                .setToAddress(to)
                .setToAssetsChainId(token1.getChainId())
                .setToAssetsId(token1.getAssetId())
                .setToAmount(amount1).endTo();
        if (chain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_15_0) {
            sysDeal.newTo()
                    .setToAddress(SwapContext.BLACKHOLE_ADDRESS)
                    .setToAssetsChainId(tokenLP.getChainId())
                    .setToAssetsId(tokenLP.getAssetId())
                    .setToAmount(dto.getLiquidity()).endTo();
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    public RemoveLiquidityDTO getRemoveLiquidityInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 1) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_TOS_ERROR);
        }
        CoinTo to = tos.get(0);
        if (to.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_AMOUNT_LOCK_ERROR);
        }
        byte[] pairAddress = to.getAddress();

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 1) {
            throw new NulsException(SwapErrorCode.REMOVE_LIQUIDITY_FROMS_ERROR);
        }
        CoinFrom from = froms.get(0);
        if (!from.getAmount().equals(to.getAmount())) {
            throw new NulsException(SwapErrorCode.INVALID_AMOUNTS);
        }
        byte[] userAddress = from.getAddress();
        return new RemoveLiquidityDTO(userAddress, pairAddress, to.getAmount());
    }

}
