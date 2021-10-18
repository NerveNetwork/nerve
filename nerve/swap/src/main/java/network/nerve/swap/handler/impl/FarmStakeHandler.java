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
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
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
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.tx.FarmSystemTransaction;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.FarmStakeHelper;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class FarmStakeHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmStakeHelper helper;
    @Autowired
    private FarmCache farmCache;
    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;

    @Override
    public Integer txType() {
        return TxType.FARM_STAKE;
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

    private void rollbackLedger(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight, long blockTime) {
        result.setSuccess(false);
        result.setErrorMessage("Farm stake failed!");
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

    public SwapResult executeTx(Chain chain, SwapResult result, BatchInfo batchInfo, Transaction tx, long blockHeight, long blockTime) {

        try {
            // 提取业务参数
            FarmStakeChangeData txData = new FarmStakeChangeData();
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
            //处理
            executeBusiness(chain, tx, txData, farm, batchInfo, result, blockHeight, blockTime);

//            batchInfo.getFarmTempManager().putFarm(farm);

            // 装填执行结果
            result.setSuccess(true);
            result.setBlockHeight(blockHeight);

        } catch (NulsException e) {
            chain.getLogger().error(e);
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

    private void executeBusiness(Chain chain, Transaction tx, FarmStakeChangeData txData, FarmPoolPO farm, BatchInfo batchInfo, SwapResult result, long blockHeight, long blockTime) throws NulsException {
        FarmBus bus = new FarmBus();
        bus.setFarmHash(txData.getFarmHash());

        bus.setAccSyrupPerShareOld(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockOld(farm.getLastRewardBlock());
        bus.setStakingBalanceOld(farm.getStakeTokenBalance());
        bus.setSyrupBalanceOld(farm.getSyrupTokenBalance());
        SwapUtils.updatePool(farm, blockHeight);

        byte[] address = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        bus.setUserAddress(address);
        //获取用户状态数据
        FarmUserInfoPO user = batchInfo.getFarmUserTempManager().getUserInfo(farm.getFarmHash(), address);
        if (null == user) {
            user = this.userInfoStorageService.load(chain.getChainId(), farm.getFarmHash(), address);
        }
        if (null == user) {
            user = new FarmUserInfoPO();
            user.setFarmHash(farm.getFarmHash());
            user.setAmount(BigInteger.ZERO);
            user.setUserAddress(address);
            user.setRewardDebt(BigInteger.ZERO);
        }
        bus.setUserAmountOld(user.getAmount());
        bus.setUserRewardDebtOld(user.getRewardDebt());
        //生成领取奖励的交易
//        uint256 pending = user.amount.mul(pool.accSushiPerShare).div(1e12).sub(user.rewardDebt);
        BigInteger expectedReward = user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        BigInteger realReward = expectedReward;
        do {
            if (realReward.compareTo(BigInteger.ZERO) <= 0) {
                break;
            }
            // 糖果余额不足的情况，剩多少领多少
            if (farm.getSyrupTokenBalance().compareTo(realReward) < 0) {
                realReward = BigInteger.ZERO.add(farm.getSyrupTokenBalance());
            }
            if (realReward.compareTo(BigInteger.ZERO) <= 0) {
                break;
            }

            LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
            LedgerBalance balance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chain.getChainId()), farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId()).getData();
            // farm余额不足的情况，能领多少算多少,理论上不会出现这种情况
            if (balance.getBalance().compareTo(realReward) < 0) {
                realReward = BigInteger.ZERO.add(balance.getBalance());
            }
            farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().subtract(realReward));
            if (realReward.compareTo(BigInteger.ZERO) == 0) {
                break;
            }
            Transaction subTx = transferReward(chain, farm, address, realReward, tx, blockTime, balance);
            tempBalanceManager.refreshTempBalance(chain.getChainId(), subTx, blockTime);
            result.setSubTx(subTx);
            try {
                result.setSubTxStr(HexUtil.encode(subTx.serialize()));
            } catch (IOException e) {
                throw new NulsException(SwapErrorCode.IO_ERROR, e);
            }
        } while (false);

        farm.setStakeTokenBalance(farm.getStakeTokenBalance().add(txData.getAmount()));
        //更新池子信息
        batchInfo.getFarmTempManager().putFarm(farm);
        //更新用户状态数据
        user.setAmount(user.getAmount().add(txData.getAmount()));

        BigInteger difference = expectedReward.subtract(realReward);
        user.setRewardDebt(user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12));
        if (difference.compareTo(BigInteger.ZERO) > 0) {
        } else if (user.getAmount().compareTo(BigInteger.ZERO) == 0) {
            user.setRewardDebt(BigInteger.ZERO);
        } else {
            BigInteger value = difference.divide(user.getAmount());
            user.setRewardDebt(user.getRewardDebt().subtract(value));
        }

        bus.setAccSyrupPerShareNew(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockNew(farm.getLastRewardBlock());
        bus.setStakingBalanceNew(farm.getStakeTokenBalance());
        bus.setSyrupBalanceNew(farm.getSyrupTokenBalance());
        bus.setUserAmountNew(user.getAmount());
        bus.setUserRewardDebtNew(user.getRewardDebt());
        result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
        batchInfo.getFarmUserTempManager().putUserInfo(user);
    }

    private Transaction transferReward(Chain chain, FarmPoolPO farm, byte[] address, BigInteger reward, Transaction tx, long blockTime, LedgerBalance balance) {
        FarmSystemTransaction sysTx = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);
        sysTx.setRemark("Reward.");

        sysTx.newFrom().setFrom(balance, reward).endFrom();
        sysTx.newTo()
                .setToAddress(address)
                .setToAssetsChainId(farm.getSyrupToken().getChainId())
                .setToAssetsId(farm.getSyrupToken().getAssetId())
                .setToAmount(reward).endTo();
        return sysTx.build();
    }

    public ChainManager getChainManager() {
        return chainManager;
    }

    public void setChainManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public FarmStakeHelper getHelper() {
        return helper;
    }

    public void setHelper(FarmStakeHelper helper) {
        this.helper = helper;
    }

    public FarmCache getFarmCacher() {
        return farmCache;
    }

    public void setFarmCacher(FarmCache farmCache) {
        this.farmCache = farmCache;
    }

    public FarmUserInfoStorageService getUserInfoStorageService() {
        return userInfoStorageService;
    }

    public void setUserInfoStorageService(FarmUserInfoStorageService userInfoStorageService) {
        this.userInfoStorageService = userInfoStorageService;
    }
}
