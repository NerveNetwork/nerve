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

import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapConstant;
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
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.tx.FarmSystemTransaction;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.FarmWithdrawHelper;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
@Component
public class FarmWithdrawHandler extends SwapHandlerConstraints {

    @Autowired
    private ISwapInvoker iSwapInvoker;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmWithdrawHelper helper;
    @Autowired
    private FarmCache farmCache;
    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;

    @Override
    public Integer txType() {
        return TxType.FARM_WITHDRAW;
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
            // Extract business parameters
            FarmStakeChangeData txData = new FarmStakeChangeData();
            txData.parse(tx.getTxData(), 0);
            ValidaterResult validaterResult = helper.validateTxData(chain, tx, txData, batchInfo.getFarmTempManager(), blockTime);
            if (validaterResult.isFailed()) {
                throw new NulsException(validaterResult.getErrorCode());
            }

            FarmPoolPO farm = batchInfo.getFarmTempManager().getFarm(txData.getFarmHash().toHex());
            if (farm == null) {
                FarmPoolPO realPo = farmCache.get(txData.getFarmHash());
                farm = realPo.copy();
            }
            //handle
            executeBusiness(chain, tx, txData, farm, batchInfo, result, blockHeight, blockTime);

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


    private void executeBusiness(Chain chain, Transaction tx, FarmStakeChangeData txData, FarmPoolPO farm, BatchInfo batchInfo, SwapResult result, long blockHeight, long blockTime) throws NulsException {
        FarmBus bus = new FarmBus();
        bus.setAccSyrupPerShareOld(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockOld(farm.getLastRewardBlock());
        bus.setStakingBalanceOld(farm.getStakeTokenBalance());
        bus.setSyrupBalanceOld(farm.getSyrupTokenBalance());
        bus.setFarmHash(farm.getFarmHash());
        if (blockHeight >= SwapContext.PROTOCOL_1_16_0) {
            bus.setStopHeightOld(farm.getStopHeight());
        }
        SwapUtils.updatePool(farm, blockHeight);

        byte[] address = SwapUtils.getSingleAddressFromTX(tx, chain.getChainId(), false);
        bus.setUserAddress(address);
        //Obtain user status data
        FarmUserInfoPO user = batchInfo.getFarmUserTempManager().getUserInfo(farm.getFarmHash(), address);
        if (null == user) {
            user = this.userInfoStorageService.load(chain.getChainId(), farm.getFarmHash(), address);
        }
        if (null == user) {
            throw new NulsException(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        bus.setUserAmountOld(user.getAmount());
        bus.setUserRewardDebtOld(user.getRewardDebt());
        //Generate transactions to claim rewards
        BigInteger expectedReward = user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        BigInteger realReward = expectedReward;

        if (realReward.compareTo(farm.getSyrupTokenBalance()) > 0) {
            realReward = BigInteger.ZERO.add(farm.getSyrupTokenBalance());
        }
        LedgerTempBalanceManager tempBalanceManager = batchInfo.getLedgerTempBalanceManager();
        LedgerBalance syrupBalance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chain.getChainId()), farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId()).getData();

        if (syrupBalance.getBalance().compareTo(realReward) < 0) {
            //As much as you can receive, there shouldn't be such a situation
            realReward = BigInteger.ZERO.add(syrupBalance.getBalance());
        }
        farm.setSyrupTokenBalance(farm.getSyrupTokenBalance().subtract(realReward));
        if (realReward.compareTo(BigInteger.ZERO) > 0 || blockHeight >= SwapContext.PROTOCOL_1_16_0) {
            Transaction subTx = transferReward(chain.getChainId(), farm, address, realReward, tx, blockTime, txData.getAmount(), tempBalanceManager, syrupBalance, farm.getWithdrawLockTime());
            result.setSubTx(subTx);
            try {
                result.setSubTxStr(HexUtil.encode(subTx.serialize()));
            } catch (IOException e) {
                throw new NulsException(SwapErrorCode.IO_ERROR, e);
            }
        }
        farm.setStakeTokenBalance(farm.getStakeTokenBalance().subtract(txData.getAmount()));
        //Update pool information
        batchInfo.getFarmTempManager().putFarm(farm);
        //Update user status data
        user.setAmount(user.getAmount().subtract(txData.getAmount()));

        BigInteger difference = expectedReward.subtract(realReward);
        user.setRewardDebt(user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12));
        if (difference.compareTo(BigInteger.ZERO) > 0) {
        } else if (user.getAmount().compareTo(BigInteger.ZERO) == 0) {
            user.setRewardDebt(BigInteger.ZERO);
        } else {
            BigInteger value = difference.divide(user.getAmount());
            user.setRewardDebt(user.getRewardDebt().subtract(value));
        }
        if (farm.getStakeTokenBalance().compareTo(BigInteger.ZERO) == 0) {
            farm.setStopHeight(0L);
        }

        bus.setAccSyrupPerShareNew(farm.getAccSyrupPerShare());
        bus.setLastRewardBlockNew(farm.getLastRewardBlock());
        bus.setStakingBalanceNew(farm.getStakeTokenBalance());
        bus.setSyrupBalanceNew(farm.getSyrupTokenBalance());
        bus.setUserAmountNew(user.getAmount());
        bus.setUserRewardDebtNew(user.getRewardDebt());
        if (blockHeight >= SwapContext.PROTOCOL_1_16_0) {
            bus.setStopHeightNew(farm.getStopHeight());
        }
        result.setBusiness(HexUtil.encode(SwapDBUtil.getModelSerialize(bus)));
        batchInfo.getFarmUserTempManager().putUserInfo(user);
    }

    private Transaction transferReward(int chainId, FarmPoolPO farm, byte[] address, BigInteger reward, Transaction tx, long blockTime, BigInteger withdrawAmount, LedgerTempBalanceManager tempBalanceManager, LedgerBalance syrupBalance, long lockTime) {
        if (reward.compareTo(BigInteger.ZERO) < 0) {
            reward = BigInteger.ZERO;
        }
        FarmSystemTransaction sysWithdrawTx = new FarmSystemTransaction(tx.getHash().toHex(), blockTime);
        sysWithdrawTx.setRemark("Withdraw.");
        LedgerBalance balance = tempBalanceManager.getBalance(SwapUtils.getFarmAddress(chainId), farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId()).getData();
        long toLockTime = 0;
        if (lockTime > 0) {
            toLockTime = lockTime + blockTime;
        }
        if (farm.getStakeToken().getChainId() == farm.getSyrupToken().getChainId() && farm.getStakeToken().getAssetId() == farm.getSyrupToken().getAssetId()) {
            BigInteger amount = reward.add(withdrawAmount);
            sysWithdrawTx.newFrom().setFrom(balance, amount).endFrom();


            if (lockTime == 0 || reward.compareTo(BigInteger.ZERO) == 0) {
                sysWithdrawTx.newTo()
                        .setToAddress(address)
                        .setToAssetsChainId(farm.getStakeToken().getChainId())
                        .setToAssetsId(farm.getStakeToken().getAssetId())
                        .setToAmount(amount).endTo();
            } else {
                sysWithdrawTx.newTo()
                        .setToAddress(address)
                        .setToAssetsChainId(farm.getStakeToken().getChainId())
                        .setToAssetsId(farm.getStakeToken().getAssetId())
                        .setToAmount(withdrawAmount).setToLockTime(toLockTime).endTo();
                sysWithdrawTx.newTo()
                        .setToAddress(address)
                        .setToAssetsChainId(farm.getSyrupToken().getChainId())
                        .setToAssetsId(farm.getSyrupToken().getAssetId())
                        .setToAmount(reward).endTo();
            }

            return sysWithdrawTx.build();
        }
        sysWithdrawTx.newFrom().setFrom(balance, withdrawAmount).endFrom();

        sysWithdrawTx.newTo()
                .setToAddress(address)
                .setToAssetsChainId(farm.getStakeToken().getChainId())
                .setToAssetsId(farm.getStakeToken().getAssetId())
                .setToAmount(withdrawAmount).setToLockTime(toLockTime).endTo();
        if (reward.compareTo(BigInteger.ZERO) > 0) {
            sysWithdrawTx.newFrom().setFrom(syrupBalance, reward).endFrom();
            sysWithdrawTx.newTo()
                    .setToAddress(address)
                    .setToAssetsChainId(farm.getSyrupToken().getChainId())
                    .setToAssetsId(farm.getSyrupToken().getAssetId())
                    .setToAmount(reward).endTo();
        }
        return sysWithdrawTx.build();
    }

    public ChainManager getChainManager() {
        return chainManager;
    }

    public void setChainManager(ChainManager chainManager) {
        this.chainManager = chainManager;
    }

    public FarmWithdrawHelper getHelper() {
        return helper;
    }

    public void setHelper(FarmWithdrawHelper helper) {
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
