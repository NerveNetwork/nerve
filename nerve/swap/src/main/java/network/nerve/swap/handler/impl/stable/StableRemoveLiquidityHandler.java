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
import network.nerve.swap.cache.LedgerAssetCache;
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
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.stable.StableRemoveLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.stable.StableRemoveLiquidityData;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class StableRemoveLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public Integer txType() {
        return TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN;
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
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        StableRemoveLiquidityDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getStableRemoveLiquidityInfo(chainId, coinData);
            // 提取业务参数
            StableRemoveLiquidityData txData = new StableRemoveLiquidityData();
            txData.parse(tx.getTxData(), 0);

            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            if (!stableSwapPairCache.isExist(pairAddress)) {
                throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
            }
            // 销毁的LP资产
            BigInteger liquidity = dto.getLiquidity();
            byte[] indexs = txData.getIndexs();
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken tokenLP = pairPo.getTokenLP();
            NerveToken[] coins = pairPo.getCoins();

            // 整合计算数据
            StableRemoveLiquidityBus bus = SwapUtils.calStableRemoveLiquidityBusiness(chainId, iPairFactory, liquidity, indexs, dto.getPairAddress(), txData.getTo());
            //SwapContext.logger.info("[{}]handler remove bus: {}", blockHeight, bus.toString());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(chain, bus, coins, tokenLP, tx.getHash().toHex(), blockTime, tempBalanceManager);
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            stablePair.update(liquidity.negate(), SwapUtils.convertNegate(bus.getAmounts()), bus.getBalances(), blockHeight, blockTime);
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
            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            NerveToken tokenLP = stablePair.getPair().getTokenLP();
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

    private SwapResult executeP21(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        StableRemoveLiquidityDTO dto = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = this.getStableRemoveLiquidityInfo(chainId, coinData);
            // 提取业务参数
            StableRemoveLiquidityData txData = new StableRemoveLiquidityData();
            txData.parse(tx.getTxData(), 0);

            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            if (!stableSwapPairCache.isExist(pairAddress)) {
                throw new NulsException(SwapErrorCode.PAIR_NOT_EXIST);
            }
            // 销毁的LP资产
            BigInteger liquidity = dto.getLiquidity();
            byte[] indexs = txData.getIndexs();
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            NerveToken tokenLP = pairPo.getTokenLP();
            NerveToken[] coins = pairPo.getCoins();

            // 整合计算数据
            StableRemoveLiquidityBus bus = SwapUtils.calStableRemoveLiquidityBusinessP21(chainId, iPairFactory, liquidity, indexs, dto.getPairAddress(), txData.getTo());
            //SwapContext.logger.info("[{}]handler remove bus: {}", blockHeight, bus.toString());
            // 装填执行结果
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // 组装系统成交交易
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(chain, bus, coins, tokenLP, tx.getHash().toHex(), blockTime, tempBalanceManager);
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // 更新临时余额
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // 更新临时数据
            stablePair.update(liquidity.negate(), SwapUtils.convertNegate(bus.getAmounts()), bus.getBalances(), blockHeight, blockTime);
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
            String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            NerveToken tokenLP = stablePair.getPair().getTokenLP();
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

    public Transaction makeSystemDealTx(Chain chain, StableRemoveLiquidityBus bus, NerveToken[] coins, NerveToken tokenLP, String orginTxHash,
                                         long blockTime, LedgerTempBalanceManager tempBalanceManager) {
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        BigInteger[] receives = bus.getAmounts();
        byte[] pairAddress = bus.getPairAddress();
        byte[] to = bus.getTo();
        LedgerBalance ledgerBalanceLp = tempBalanceManager.getBalance(pairAddress, tokenLP.getChainId(), tokenLP.getAssetId()).getData();
        sysDeal.newFrom()
                .setFrom(ledgerBalanceLp, bus.getLiquidity()).endFrom();
        if (chain.getLatestBasicBlock().getHeight() >= SwapContext.PROTOCOL_1_15_0) {
            sysDeal.newTo()
                    .setToAddress(SwapContext.BLACKHOLE_ADDRESS)
                    .setToAssetsChainId(tokenLP.getChainId())
                    .setToAssetsId(tokenLP.getAssetId())
                    .setToAmount(bus.getLiquidity()).endTo();
        }
        if (receives != null) {
            int length = coins.length;
            for (int i = 0; i < length; i++) {
                BigInteger receive = receives[i];
                if (receive != null && receive.compareTo(BigInteger.ZERO) > 0) {
                    NerveToken coin = coins[i];
                    LedgerBalance balance = tempBalanceManager.getBalance(pairAddress, coin.getChainId(), coin.getAssetId()).getData();
                    sysDeal.newFrom()
                            .setFrom(balance, receive).endFrom()
                            .newTo()
                            .setToAddress(to)
                            .setToAssetsChainId(coin.getChainId())
                            .setToAssetsId(coin.getAssetId())
                            .setToAmount(receive).endTo();
                }
            }
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    public StableRemoveLiquidityDTO getStableRemoveLiquidityInfo(int chainId, CoinData coinData) throws NulsException {
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
        return new StableRemoveLiquidityDTO(userAddress, pairAddress, to.getAmount());
    }
}
