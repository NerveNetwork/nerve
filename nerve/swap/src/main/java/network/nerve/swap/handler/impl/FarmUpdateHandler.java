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
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.cache.impl.FarmCacheImpl;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.ISwapInvoker;
import network.nerve.swap.handler.SwapHandlerConstraints;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.FarmBus;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.tx.FarmSystemTransaction;
import network.nerve.swap.model.txdata.FarmUpdateData;
import network.nerve.swap.tx.v1.helpers.FarmUpdateTxHelper;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class FarmUpdateHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmCache farmCache;
    @Autowired
    private FarmUpdateTxHelper helper;

    @Override
    public Integer txType() {
        return TxType.FARM_UPDATE;
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
            result = executeTx(chain, result, batchInfo, tx, blockHeight, blockTime);
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

    private SwapResult executeTx(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight, long blockTime) {

        try {
            // Extract business parameters
            FarmUpdateData txData = new FarmUpdateData();
            txData.parse(tx.getTxData(), 0);
            ValidaterResult validaterResult = helper.validateTxData(chain, tx, txData, batchInfo.getFarmTempManager());
            if (validaterResult.isFailed()) {
                throw new NulsException(validaterResult.getErrorCode());
            }

            FarmPoolPO farm = batchInfo.getFarmTempManager().getFarm(txData.getFarmHash().toHex());
            if (farm == null) {
                FarmPoolPO realPo = farmCache.get(txData.getFarmHash());
                farm = realPo.copy();
            }
            //handle
            if (SwapContext.PROTOCOL_1_17_0 > blockHeight) {
                executeBusiness(chain, tx, txData, farm, batchInfo, result, blockHeight, blockTime);
            } else {
                executeBusinessV17(chain, tx, txData, farm, batchInfo, result, blockHeight, blockTime);
            }

//            batchInfo.getFarmTempManager().putFarm(farm);

            // Loading execution result
            result.setSuccess(true);
            result.setBlockHeight(blockHeight);

        } catch (NulsException e) {
            chain.getLogger().error(e);
            // Execution results of failed loading
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

    private void executeBusiness(Chain chain, Transaction tx, FarmUpdateData txData, FarmPoolPO farm, BatchInfo batchInfo, SwapResult result, long blockHeight, long blockTime) throws NulsException {
        FarmBus bus = new FarmBus();
        bus.setFarmHash(txData.getFarmHash());
        bus.setAccSyrupPerShareOld(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockOld(farm.getLastRewardBlock());
        bus.setStakingBalanceOld(farm.getStakeTokenBalance());
        bus.setSyrupBalanceOld(farm.getSyrupTokenBalance());
        bus.setSyrupPerBlockOld(farm.getSyrupPerBlock());
        bus.setTotalSyrupAmountOld(farm.getTotalSyrupAmount());
        bus.setWithdrawLockTimeOld(farm.getWithdrawLockTime());
        bus.setStopHeightOld(farm.getStopHeight());
        SwapUtils.updatePool(farm, blockHeight);

        byte[] address = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        bus.setUserAddress(address);
        BigInteger oldBalance;
        BigInteger calcBalance;
        oldBalance = bus.getSyrupPerBlockOld().multiply(BigInteger.valueOf(bus.getStopHeightOld()).subtract(BigInteger.valueOf(bus.getLastRewardBlockOld())));
        calcBalance = BigInteger.ZERO;

        if (txData.getNewSyrupPerBlock() != null) {
            farm.setSyrupPerBlock(txData.getNewSyrupPerBlock());
        }
        if (txData.getChangeType() == 0) {
            farm.setTotalSyrupAmount(farm.getTotalSyrupAmount().add(txData.getChangeTotalSyrupAmount()));
            farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().add(txData.getChangeTotalSyrupAmount()));
            calcBalance = oldBalance.add(txData.getChangeTotalSyrupAmount());
        } else if (txData.getChangeType() == 1) {
            do {
                if (BigInteger.ZERO.compareTo(txData.getChangeTotalSyrupAmount()) >= 0) {
                    break;
                }
                LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
                LedgerBalance balance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chain.getChainId()), farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId()).getData();
                // farmIn the case of insufficient balance, calculate as much as can be claimed,In theory, this situation would not occur
                if (balance.getBalance().compareTo(txData.getChangeTotalSyrupAmount()) < 0) {
                    throw new NulsException(SwapErrorCode.FARM_CHANGE_ERROR);
                }

                farm.setTotalSyrupAmount(farm.getTotalSyrupAmount().subtract(txData.getChangeTotalSyrupAmount()));
                farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().subtract(txData.getChangeTotalSyrupAmount()));
                Transaction subTx = transferReward(chain, farm, address, txData.getChangeTotalSyrupAmount(), tx, blockTime, balance);
                tempBalanceManager.refreshTempBalance(chain.getChainId(), subTx, blockTime);
                result.setSubTx(subTx);
                try {
                    result.setSubTxStr(HexUtil.encode(subTx.serialize()));
                } catch (IOException e) {
                    throw new NulsException(SwapErrorCode.IO_ERROR, e);
                }
                calcBalance = oldBalance.subtract(txData.getChangeTotalSyrupAmount());
            } while (false);
        }
        if (txData.getStopHeight() > 0) {
            farm.setStopHeight(txData.getStopHeight());
        } else if (farm.getStakeTokenBalance().compareTo(BigInteger.ZERO) == 0) {
            farm.setStopHeight(0L);
        } else {
            long stopHeight = calcBalance.divide(farm.getSyrupPerBlock()).longValue() - farm.getLastRewardBlock();
            if (stopHeight <= blockHeight) {
                stopHeight = blockHeight;
            }
            farm.setStopHeight(stopHeight);
        }

        farm.setWithdrawLockTime(txData.getWithdrawLockTime());
        //Update pool information
        batchInfo.getFarmTempManager().putFarm(farm);

        bus.setAccSyrupPerShareNew(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockNew(farm.getLastRewardBlock());
        bus.setStakingBalanceNew(farm.getStakeTokenBalance());
        bus.setSyrupBalanceNew(farm.getSyrupTokenBalance());
        bus.setSyrupPerBlockNew(farm.getSyrupPerBlock());
        bus.setTotalSyrupAmountNew(farm.getTotalSyrupAmount());
        bus.setWithdrawLockTimeNew(farm.getWithdrawLockTime());
        bus.setStopHeightNew(farm.getStopHeight());

        result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));

    }

    private void executeBusinessV17(Chain chain, Transaction tx, FarmUpdateData txData, FarmPoolPO farm, BatchInfo batchInfo, SwapResult result, long blockHeight, long blockTime) throws NulsException {
        FarmBus bus = new FarmBus();
        bus.setFarmHash(txData.getFarmHash());
        bus.setAccSyrupPerShareOld(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockOld(farm.getLastRewardBlock());
        bus.setStakingBalanceOld(farm.getStakeTokenBalance());
        bus.setSyrupBalanceOld(farm.getSyrupTokenBalance());
        bus.setSyrupPerBlockOld(farm.getSyrupPerBlock());
        bus.setTotalSyrupAmountOld(farm.getTotalSyrupAmount());
        bus.setWithdrawLockTimeOld(farm.getWithdrawLockTime());
        bus.setStopHeightOld(farm.getStopHeight());
        if (SwapContext.PROTOCOL_1_29_0 <= blockHeight) {
            bus.setSyrupLockTimeOld(farm.getSyrupLockTime());
        }
        SwapUtils.updatePool(farm, blockHeight);

        byte[] address = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        bus.setUserAddress(address);

        if (txData.getNewSyrupPerBlock() != null) {
            farm.setSyrupPerBlock(txData.getNewSyrupPerBlock());
        }
        if (txData.getChangeType() == 0) {
            farm.setTotalSyrupAmount(farm.getTotalSyrupAmount().add(txData.getChangeTotalSyrupAmount()));
            farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().add(txData.getChangeTotalSyrupAmount()));
        } else if (txData.getChangeType() == 1) {
            do {
                if (BigInteger.ZERO.compareTo(txData.getChangeTotalSyrupAmount()) >= 0) {
                    break;
                }
                LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
                LedgerBalance balance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chain.getChainId()), farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId()).getData();
                // farmIn the case of insufficient balance, calculate as much as can be claimed,In theory, this situation would not occur
                if (balance.getBalance().compareTo(txData.getChangeTotalSyrupAmount()) < 0) {
                    throw new NulsException(SwapErrorCode.FARM_CHANGE_ERROR);
                }

                farm.setTotalSyrupAmount(farm.getTotalSyrupAmount().subtract(txData.getChangeTotalSyrupAmount()));
                farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().subtract(txData.getChangeTotalSyrupAmount()));
                Transaction subTx = transferReward(chain, farm, address, txData.getChangeTotalSyrupAmount(), tx, blockTime, balance);
                tempBalanceManager.refreshTempBalance(chain.getChainId(), subTx, blockTime);
                result.setSubTx(subTx);
                try {
                    result.setSubTxStr(HexUtil.encode(subTx.serialize()));
                } catch (IOException e) {
                    throw new NulsException(SwapErrorCode.IO_ERROR, e);
                }
            } while (false);
        }
        if (txData.getStopHeight() > 0) {
            farm.setStopHeight(txData.getStopHeight());
        } else if (farm.getStakeTokenBalance().compareTo(BigInteger.ZERO) == 0) {
            farm.setStopHeight(0L);
        }

        farm.setWithdrawLockTime(txData.getWithdrawLockTime());
        if (SwapContext.PROTOCOL_1_29_0 <= blockHeight) {
            farm.setSyrupLockTime(txData.getSyrupLockTime());
        }
        //Update pool information
        batchInfo.getFarmTempManager().putFarm(farm);

        bus.setAccSyrupPerShareNew(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockNew(farm.getLastRewardBlock());
        bus.setStakingBalanceNew(farm.getStakeTokenBalance());
        bus.setSyrupBalanceNew(farm.getSyrupTokenBalance());
        bus.setSyrupPerBlockNew(farm.getSyrupPerBlock());
        bus.setTotalSyrupAmountNew(farm.getTotalSyrupAmount());
        bus.setWithdrawLockTimeNew(farm.getWithdrawLockTime());
        bus.setStopHeightNew(farm.getStopHeight());
        if (SwapContext.PROTOCOL_1_29_0 <= blockHeight) {
            bus.setSyrupLockTimeNew(farm.getSyrupLockTime());
        }

        result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));

    }

    private Transaction transferReward(Chain chain, FarmPoolPO farm, byte[] address, BigInteger reward, Transaction tx, long blockTime, LedgerBalance balance) {
        FarmSystemTransaction sysTx = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);
        sysTx.setRemark("Tack back.");

        sysTx.newFrom().setFrom(balance, reward).endFrom();
        sysTx.newTo()
                .setToAddress(address)
                .setToAssetsChainId(farm.getSyrupToken().getChainId())
                .setToAssetsId(farm.getSyrupToken().getAssetId())
                .setToAmount(reward).endTo();
        return sysTx.build();
    }

    private void rollbackLedger(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight, long blockTime) {
        result.setSuccess(false);
        result.setErrorMessage("Farm update failed!");
        result.setTxType(txType());
        result.setHash(tx.getHash().toHex());
        result.setTxTime(tx.getTime());
        result.setBlockHeight(blockHeight);
        FarmSystemTransaction refund = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);
        refund.setRemark("Refund.");

        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();

        byte[] farmAddress = SwapUtils.getFarmAddress(chain.getChainId());
        int stakeChainId = 0;
        int stakeAssetId = 0;
        BigInteger amount = null;
        try {
            CoinData coinData = tx.getCoinDataInstance();
            for (CoinTo to : coinData.getTo()) {
                if (ArraysTool.arrayEquals(farmAddress, to.getAddress())) {
                    stakeChainId = to.getAssetsChainId();
                    stakeAssetId = to.getAssetsId();
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

        byte[] toAddress;
        try {
            toAddress = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return;
        }

        LedgerBalance balanceX = tempBalanceManager.getBalance(farmAddress, stakeChainId, stakeAssetId).getData();

        Transaction refundTx =
                refund.newFrom()
                        .setFrom(balanceX, amount).endFrom()
                        .newTo()
                        .setToAddress(toAddress)
                        .setToAssetsChainId(stakeChainId)
                        .setToAssetsId(stakeAssetId)
                        .setToAmount(amount).endTo()
                        .build();

        result.setSubTx(refundTx);
        String refundTxStr = SwapUtils.nulsData2Hex(refundTx);
        result.setSubTxStr(refundTxStr);
    }

    public void setHelper(FarmUpdateTxHelper farmUpdateTxHelper) {
        this.helper = farmUpdateTxHelper;
    }

    public void setChainManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public void setFarmCacher(FarmCacheImpl farmCacher) {
        this.farmCache = farmCacher;
    }

    public FarmUpdateTxHelper getHelper() {
        return helper;
    }
}
