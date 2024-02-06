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
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.dto.stable.StableAddLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
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
public class StableAddLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public Integer txType() {
        return TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN;
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
            dto = this.getStableAddLiquidityInfo(chainId, coinData, iPairFactory);
            // Extract business parameters
            StableAddLiquidityData txData = new StableAddLiquidityData();
            txData.parse(tx.getTxData(), 0);

            String pairAddress = dto.getPairAddress();
            IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
            StableSwapPairPo pairPo = stablePair.getPair();
            // Integrate computing data
            StableAddLiquidityBus bus = SwapUtils.calStableAddLiquididy(swapHelper, chainId, iPairFactory, pairAddress, dto.getFrom(), dto.getAmounts(), txData.getTo());
            //SwapContext.logger.info("[{}]handler add bus: {}", blockHeight, bus.toString());
            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // Assembly system transaction
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(bus, pairPo.getCoins(), pairPo.getTokenLP(), tx.getHash().toHex(), blockTime, tempBalanceManager);
            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // Update temporary data
            stablePair.update(bus.getLiquidity(), bus.getRealAmounts(), bus.getBalances(), blockHeight, blockTime);

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

    public Transaction makeSystemDealTx(StableAddLiquidityBus bus, NerveToken[] coins, NerveToken tokenLP, String orginTxHash, long blockTime, LedgerTempBalanceManager tempBalanceManager) {
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        sysDeal.newTo()
                .setToAddress(bus.getTo())
                .setToAssetsChainId(tokenLP.getChainId())
                .setToAssetsId(tokenLP.getAssetId())
                .setToAmount(bus.getLiquidity()).endTo();

        BigInteger[] refundAmounts = bus.getRefundAmounts();
        if (refundAmounts != null) {
            byte[] from = bus.getFrom();
            int length = coins.length;
            for (int i = 0; i < length; i++) {
                BigInteger refundAmount = refundAmounts[i];
                if (refundAmount != null && refundAmount.compareTo(BigInteger.ZERO) > 0) {
                    NerveToken coin = coins[i];
                    sysDeal.newTo()
                            .setToAddress(from)
                            .setToAssetsChainId(coin.getChainId())
                            .setToAssetsId(coin.getAssetId())
                            .setToAmount(refundAmount).endTo();
                }
            }
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }

    public StableAddLiquidityDTO getStableAddLiquidityInfo(int chainId, CoinData coinData, IPairFactory iPairFactory) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.isEmpty()) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_TOS_ERROR);
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
        BigInteger[] amounts = new BigInteger[length];
        for (CoinTo to : tos) {
            if (to.getLockTime() != 0) {
                throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_AMOUNT_LOCK_ERROR);
            }
            if (!Arrays.equals(pairAddressBytes, to.getAddress())) {
                throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
            }
            boolean exist = false;
            for (int i = 0; i < length; i++) {
                NerveToken coin = coins[i];
                if (coin.getChainId() == to.getAssetsChainId() && coin.getAssetId() == to.getAssetsId()) {
                    amounts[i] = to.getAmount();
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_TOS_ERROR);
            }
        }
        amounts = SwapUtils.emptyFillZero(amounts);
        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != tos.size()) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_FROMS_ERROR);
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
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_FROMS_ERROR);
        }
        return new StableAddLiquidityDTO(pairAddress, _from, amounts);
    }
}
