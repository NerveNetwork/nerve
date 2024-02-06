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
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.dto.AddLiquidityDTO;
import network.nerve.swap.model.dto.RealAddLiquidityOrderDTO;
import network.nerve.swap.model.tx.SwapSystemDealTransaction;
import network.nerve.swap.model.tx.SwapSystemRefundTransaction;
import network.nerve.swap.model.txdata.AddLiquidityData;
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
public class AddLiquidityHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired("TemporaryPairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private ChainManager chainManager;

    @Override
    public Integer txType() {
        return TxType.SWAP_ADD_LIQUIDITY;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        SwapResult result = new SwapResult();
        AddLiquidityDTO dto = null;
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            CoinData coinData = tx.getCoinDataInstance();
            dto = getAddLiquidityInfo(chainId, coinData);
            // Extract business parameters
            AddLiquidityData txData = new AddLiquidityData();
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
            BigInteger amountA, amountB;
            if (tokenA.equals(dto.getTokenX())) {
                amountA = dto.getAmountX();
                amountB = dto.getAmountY();
            } else {
                amountB = dto.getAmountX();
                amountA = dto.getAmountY();
            }
            // Calculate the actual assets injected by the user and the assets obtained by the userLPasset
            IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(dto.getPairAddress()));
            RealAddLiquidityOrderDTO orderDTO = SwapUtils.calcAddLiquidity(chainId, iPairFactory, tokenA, tokenB,
                    amountA, amountB, txData.getAmountAMin(), txData.getAmountBMin());
            BigInteger[] _realAmount;
            BigInteger[] _reserves;
            BigInteger[] _refund;
            NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
            boolean firstTokenA = tokens[0].equals(tokenA);
            if (firstTokenA) {
                _realAmount = new BigInteger[]{orderDTO.getRealAmountA(), orderDTO.getRealAmountB()};
                _reserves = new BigInteger[]{orderDTO.getReservesA(), orderDTO.getReservesB()};
                _refund = new BigInteger[]{orderDTO.getRefundA(), orderDTO.getRefundB()};
            } else {
                _realAmount = new BigInteger[]{orderDTO.getRealAmountB(), orderDTO.getRealAmountA()};
                _reserves = new BigInteger[]{orderDTO.getReservesB(), orderDTO.getReservesA()};
                _refund = new BigInteger[]{orderDTO.getRefundB(), orderDTO.getRefundA()};
            }

            // Integrate computing data
            AddLiquidityBus bus = new AddLiquidityBus(
                    _realAmount[0], _realAmount[1],
                    orderDTO.getLiquidity(),
                    _reserves[0], _reserves[1],
                    _refund[0], _refund[1]
            );
            bus.setPreBlockHeight(pair.getBlockHeightLast());
            bus.setPreBlockTime(pair.getBlockTimeLast());

            // Loading execution result
            result.setTxType(txType());
            result.setSuccess(true);
            result.setHash(tx.getHash().toHex());
            result.setTxTime(tx.getTime());
            result.setBlockHeight(blockHeight);
            result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
            // Assembly system transaction
            NerveToken tokenLP = pair.getPair().getTokenLP();
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            Transaction sysDealTx = this.makeSystemDealTx(orderDTO, dto, tx.getHash().toHex(), tokenA, tokenB, tokenLP, txData.getTo(), blockTime, tempBalanceManager);

            result.setSubTx(sysDealTx);
            result.setSubTxStr(SwapUtils.nulsData2Hex(sysDealTx));
            // Update temporary balance
            tempBalanceManager.refreshTempBalance(chainId, sysDealTx, blockTime);
            // Update temporary data
            pair.update(orderDTO.getLiquidity(), _realAmount[0].add(_reserves[0]), _realAmount[1].add(_reserves[1]), _reserves[0], _reserves[1], blockHeight, blockTime);
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
            NerveToken tokenX = dto.getTokenX();
            NerveToken tokenY = dto.getTokenY();
            SwapSystemRefundTransaction refund = new SwapSystemRefundTransaction(tx.getHash().toHex(), blockTime);
            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance balanceX = tempBalanceManager.getBalance(dto.getPairAddress(), tokenX.getChainId(), tokenX.getAssetId()).getData();
            LedgerBalance balanceY = tempBalanceManager.getBalance(dto.getPairAddress(), tokenY.getChainId(), tokenY.getAssetId()).getData();
            Transaction refundTx =
                refund.newFrom()
                        .setFrom(balanceX, dto.getAmountX()).endFrom()
                      .newFrom()
                        .setFrom(balanceY, dto.getAmountY()).endFrom()
                      .newTo()
                        .setToAddress(dto.getFromX())
                        .setToAssetsChainId(tokenX.getChainId())
                        .setToAssetsId(tokenX.getAssetId())
                        .setToAmount(dto.getAmountX()).endTo()
                      .newTo()
                        .setToAddress(dto.getFromY())
                        .setToAssetsChainId(tokenY.getChainId())
                        .setToAssetsId(tokenY.getAssetId())
                        .setToAmount(dto.getAmountY()).endTo()
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

    private Transaction makeSystemDealTx(RealAddLiquidityOrderDTO bus, AddLiquidityDTO dto, String orginTxHash, NerveToken tokenA, NerveToken tokenB, NerveToken tokenLP, byte[] to, long blockTime, LedgerTempBalanceManager tempBalanceManager) {
        SwapSystemDealTransaction sysDeal = new SwapSystemDealTransaction(orginTxHash, blockTime);
        sysDeal.newTo()
                .setToAddress(to)
                .setToAssetsChainId(tokenLP.getChainId())
                .setToAssetsId(tokenLP.getAssetId())
                .setToAmount(bus.getLiquidity()).endTo();

        if (bus.getRefundA().compareTo(BigInteger.ZERO) > 0) {
            LedgerBalance balanceA = tempBalanceManager.getBalance(dto.getPairAddress(), tokenA.getChainId(), tokenA.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(balanceA, bus.getRefundA()).endFrom();
            sysDeal.newTo()
                    .setToAddress(dto.getFromX())
                    .setToAssetsChainId(tokenA.getChainId())
                    .setToAssetsId(tokenA.getAssetId())
                    .setToAmount(bus.getRefundA()).endTo();
        }
        if (bus.getRefundB().compareTo(BigInteger.ZERO) > 0) {
            LedgerBalance balanceB = tempBalanceManager.getBalance(dto.getPairAddress(), tokenB.getChainId(), tokenB.getAssetId()).getData();
            sysDeal.newFrom()
                    .setFrom(balanceB, bus.getRefundB()).endFrom();
            sysDeal.newTo()
                    .setToAddress(dto.getFromY())
                    .setToAssetsChainId(tokenB.getChainId())
                    .setToAssetsId(tokenB.getAssetId())
                    .setToAmount(bus.getRefundB()).endTo();
        }
        Transaction sysDealTx = sysDeal.build();
        return sysDealTx;
    }


    public AddLiquidityDTO getAddLiquidityInfo(int chainId, CoinData coinData) throws NulsException {
        if (coinData == null) {
            return null;
        }
        List<CoinTo> tos = coinData.getTo();
        if (tos.size() != 2) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_TOS_ERROR);
        }
        CoinTo coinToX = tos.get(0);
        CoinTo coinToY = tos.get(1);
        if (coinToX.getLockTime() != 0 || coinToY.getLockTime() != 0) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_AMOUNT_LOCK_ERROR);
        }
        NerveToken tokenX = new NerveToken(coinToX.getAssetsChainId(), coinToX.getAssetsId());
        NerveToken tokenY = new NerveToken(coinToY.getAssetsChainId(), coinToY.getAssetsId());
        byte[] pairAddress = SwapUtils.getPairAddress(chainId, tokenX, tokenY);
        if (!Arrays.equals(coinToX.getAddress(), pairAddress) || !Arrays.equals(coinToY.getAddress(), pairAddress)) {
            throw new NulsException(SwapErrorCode.PAIR_INCONSISTENCY);
        }

        List<CoinFrom> froms = coinData.getFrom();
        if (froms.size() != 2) {
            throw new NulsException(SwapErrorCode.ADD_LIQUIDITY_FROMS_ERROR);
        }
        byte[] fromX, fromY;
        CoinFrom from0 = froms.get(0);
        CoinFrom from1 = froms.get(1);
        if (from0.getAssetsChainId() == coinToX.getAssetsChainId() && from0.getAssetsId() == coinToX.getAssetsId()) {
            fromX = from0.getAddress();
            fromY = from1.getAddress();
        } else {
            fromX = from1.getAddress();
            fromY = from0.getAddress();
        }
        return new AddLiquidityDTO(fromX, fromY, pairAddress, tokenX, tokenY, coinToX.getAmount(), coinToY.getAmount());
    }


}
