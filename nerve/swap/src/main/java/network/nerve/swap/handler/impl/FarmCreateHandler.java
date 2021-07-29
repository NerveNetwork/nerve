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

import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.tx.FarmSystemTransaction;
import network.nerve.swap.model.txdata.FarmCreateData;
import network.nerve.swap.tx.v1.helpers.FarmCreateTxHelper;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class FarmCreateHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private FarmCreateTxHelper helper;

    @Override
    public Integer txType() {
        return TxType.FARM_CREATE;
    }

    @Override
    protected ISwapInvoker swapInvoker() {
        return iSwapInvoker;
    }

    @Override
    public SwapResult execute(int chainId, Transaction tx, long blockHeight, long blockTime) {
        Chain chain = chainManager.getChain(chainId);
        SwapResult result = new SwapResult();
        BatchInfo batchInfo = chainManager.getChain(chainId).getBatchInfo();
        try {
            result = executeTx(chain, result, batchInfo, tx, blockHeight);
        } catch (Exception e) {
            Log.error(tx.getHash().toHex(), e);
            result.setSuccess(false);
        }
        if (!result.isSuccess()) {
            try {
                rollbackLedger(chain, result, batchInfo, tx, blockHeight, blockTime);
            } catch (Exception e) {
                Log.error(tx.getHash().toHex(), e);
                result.setSuccess(false);
            }
        }
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }

    private void rollbackLedger(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight, long blockTime) {
        result.setSuccess(false);
        result.setErrorMessage("Farm create failed!");
        result.setTxType(txType());
        result.setHash(tx.getHash().toHex());
        result.setTxTime(tx.getTime());
        result.setBlockHeight(blockHeight);
        FarmSystemTransaction refund = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);
        refund.setRemark("Refund.");

        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        byte[] farmAddress = SwapUtils.getFarmAddress(chain.getChainId());
        int syrupChainId = 0;
        int syrupAssetId = 0;
        BigInteger amount = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            for (CoinTo to : coinData.getTo()) {
                if (ArraysTool.arrayEquals(farmAddress, to.getAddress())) {
                    syrupChainId = to.getAssetsChainId();
                    syrupAssetId = to.getAssetsId();
                    amount = to.getAmount();
                }
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return;
        }
        if (amount == null) {
            return;
        }

        byte[] toAddress ;
        try {
            toAddress = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return;
        }

        LedgerBalance balanceX = tempBalanceManager.getBalance(farmAddress, syrupChainId, syrupAssetId).getData();

        Transaction refundTx =
                refund.newFrom()
                        .setFrom(balanceX, amount).endFrom()
                        .newTo()
                        .setToAddress(toAddress)
                        .setToAssetsChainId(syrupChainId)
                        .setToAssetsId(syrupAssetId)
                        .setToAmount(amount).endTo()
                        .build();

        result.setSubTx(refundTx);
        String refundTxStr = SwapUtils.nulsData2Hex(refundTx);
        result.setSubTxStr(refundTxStr);
    }

    public SwapResult executeTx(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight) {
        try {
            // 提取业务参数
            FarmCreateData txData = new FarmCreateData();
            txData.parse(tx.getTxData(), 0);
            ValidaterResult validaterResult = helper.validateTxData(chain, tx, txData);
            if (validaterResult.isFailed()) {
                throw new NulsException(validaterResult.getErrorCode());
            }

            FarmPoolPO po = helper.getBean(chain.getChainId(), tx, txData);
            batchInfo.getFarmTempManager().putFarm(po);

            // 装填执行结果
            result.setSuccess(true);
            result.setBlockHeight(blockHeight);

        } catch (NulsException e) {
            Log.error(tx.getHash().toHex(), e);
            // 装填失败的执行结果
            result.setSuccess(false);
            result.setErrorMessage(e.format());
        }
        result.setTxType(txType());
        result.setHash(tx.getHash().toHex());
        result.setTxTime(tx.getTime());
        result.setBlockHeight(blockHeight);
        batchInfo.getSwapResultMap().put(tx.getHash().toHex(), result);
        return result;
    }

    public FarmCreateTxHelper getHelper() {
        return helper;
    }

    public void setHelper(FarmCreateTxHelper helper) {
        this.helper = helper;
    }

    public void setChainManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }
}
