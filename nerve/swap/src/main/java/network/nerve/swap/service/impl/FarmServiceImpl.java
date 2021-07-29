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
package network.nerve.swap.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.dto.FarmInfoDTO;
import network.nerve.swap.model.dto.FarmUserInfoDTO;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.model.txdata.FarmCreateData;
import network.nerve.swap.model.txdata.FarmStakeChangeData;
import network.nerve.swap.rpc.call.AccountCall;
import network.nerve.swap.rpc.call.TransactionCall;
import network.nerve.swap.service.FarmService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.tx.v1.helpers.converter.LedgerService;
import network.nerve.swap.utils.CoinDataMaker;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
@Component
public class FarmServiceImpl implements FarmService {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmCache farmCache;
    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;
    @Autowired
    private LedgerService ledgerService;

    @Override
    public Result<String> createFarm(String address, String stakeTokenStr, String syrupTokenStr, double syrupPerBlock, long startHeight, long lockedTime, double totalSyrupAmount, String password) {
        if (syrupPerBlock <= 0 || startHeight < 0 || lockedTime < 0) {
            return Result.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(SwapErrorCode.CHAIN_NOT_EXIST);
        }
        NulsLogger logger = chain.getLogger();
        //账户验证
        String prikeyHex;
        try {
            prikeyHex = AccountCall.getAccountPrikey(chainId, address, password);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        if (StringUtils.isBlank(prikeyHex)) {
            return Result.getFailed(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }
        // 验证2种资产存在
        NerveToken stakeToken = SwapUtils.parseTokenStr(stakeTokenStr);
        if (stakeToken == null) {
            return Result.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
        }
        LedgerAssetDTO stakeAssetInfo = ledgerService.getNerveAsset(chainId, stakeToken.getChainId(), stakeToken.getAssetId());
        if (stakeAssetInfo == null) {
            logger.warn("质押资产类型不正确");
            return Result.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
        }
        NerveToken syrupToken = SwapUtils.parseTokenStr(syrupTokenStr);
        if (syrupToken == null) {
            return Result.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
        }
        LedgerAssetDTO syrupAssetInfo = ledgerService.getNerveAsset(chainId, syrupToken.getChainId(), syrupToken.getAssetId());
        if (syrupAssetInfo == null) {
            logger.warn("糖果资产类型不正确");
            return Result.getFailed(SwapErrorCode.FARM_TOKEN_ERROR);
        }
        BigInteger realSyrupPerBlock = BigDecimal.valueOf(syrupPerBlock).movePointRight(syrupAssetInfo.getDecimalPlace()).toBigInteger();
        BigInteger realTotalSyrupAmount = BigDecimal.valueOf(totalSyrupAmount).movePointRight(syrupAssetInfo.getDecimalPlace()).toBigInteger();

        // 验证每个区块奖励数额区间正确
        if (realSyrupPerBlock.compareTo(BigInteger.ZERO) <= 0) {
            logger.warn("每块奖励数量必须大于0");
            return Result.getFailed(SwapErrorCode.FARM_SYRUP_PER_BLOCK_ERROR);
        }

        if (startHeight > lockedTime) {
            logger.warn("锁定截止高度不正确");
            return Result.getFailed(SwapErrorCode.FARM_LOCK_HEIGHT_ERROR);
        }
        Transaction tx = new Transaction();
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setType(TxType.FARM_CREATE);
        FarmCreateData data = new FarmCreateData();
        data.setStakeToken(stakeToken);
        data.setSyrupPerBlock(realSyrupPerBlock);
        data.setSyrupToken(syrupToken);
        data.setLockedTime(lockedTime);
        data.setTotalSyrupAmount(realTotalSyrupAmount);
        data.setStartBlockHeight(startHeight);
        try {
            tx.setTxData(data.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }
        NonceBalance balance = null;
        try {
            balance = ledgerService.getBalanceNonce(chainId, data.getSyrupToken().getChainId(), data.getSyrupToken().getAssetId(), address);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        CoinData coinData = new CoinDataMaker()
                .addFrom(AddressTool.getAddress(address), data.getSyrupToken(), realTotalSyrupAmount, balance.getNonce(), (byte) 0)
                .addTo(SwapUtils.getFarmAddress(chainId), data.getSyrupToken(), realTotalSyrupAmount, 0)
                .getCoinData();
        try {
            tx.setCoinData(coinData.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }
        byte[] prikey = HexUtil.decode(prikeyHex);
        try {
            SwapUtils.signTx(tx, prikey);
            boolean result = TransactionCall.newTx(chainId, HexUtil.encode(tx.serialize()));
            if (result) {
                return Result.getSuccess(tx.getHash().toHex());
            }
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        return Result.getFailed(SwapErrorCode.SYS_UNKOWN_EXCEPTION);
    }


    @Override
    public Result<String> stake(String userAddress, String farmHash, double amount, String password) {
        if (amount < 0) {
            return Result.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }
        int chainId = AddressTool.getChainIdByAddress(userAddress);
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(SwapErrorCode.CHAIN_NOT_EXIST);
        }
        NulsLogger logger = chain.getLogger();
        //账户验证
        String prikeyHex;
        try {
            prikeyHex = AccountCall.getAccountPrikey(chainId, userAddress, password);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        if (StringUtils.isBlank(prikeyHex)) {
            return Result.getFailed(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }

        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }

        NonceBalance balance;
        try {
            balance = ledgerService.getBalanceNonce(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId(), userAddress);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        LedgerAssetDTO ledgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());

        BigInteger realAmount = BigDecimal.valueOf(amount).movePointRight(ledgerAssetDTO.getDecimalPlace()).toBigInteger();
        if (balance == null || realAmount.compareTo(balance.getAvailable()) > 0) {
            return Result.getFailed(SwapErrorCode.BALANCE_NOT_EMOUGH);
        }

        Transaction tx = new Transaction();
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setType(TxType.FARM_STAKE);
        FarmStakeChangeData data = new FarmStakeChangeData();
        data.setFarmHash(farm.getFarmHash());
        data.setAmount(realAmount);
        try {
            tx.setTxData(data.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }
        byte[] address = AddressTool.getAddress(userAddress);
        CoinData coinData = new CoinDataMaker()
                .addFrom(address, farm.getStakeToken(), realAmount, balance.getNonce(), (byte) 0)
                .addTo(SwapUtils.getFarmAddress(chainId), farm.getStakeToken(), realAmount, 0)
                .getCoinData();
        try {
            tx.setCoinData(coinData.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }

        byte[] prikey = HexUtil.decode(prikeyHex);
        try {
            SwapUtils.signTx(tx, prikey);
            boolean result = TransactionCall.newTx(chainId, HexUtil.encode(tx.serialize()));
            if (result) {
                return Result.getSuccess(tx.getHash().toHex());
            }
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        return Result.getFailed(SwapErrorCode.SYS_UNKOWN_EXCEPTION);
    }

    @Override
    public Result<String> withdraw(String userAddress, String farmHash, double amount, String password) {
        if (amount <= 0) {
            return Result.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }
        int chainId = AddressTool.getChainIdByAddress(userAddress);
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(SwapErrorCode.CHAIN_NOT_EXIST);
        }
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        NulsLogger logger = chain.getLogger();
        //账户验证
        String prikeyHex;
        try {
            prikeyHex = AccountCall.getAccountPrikey(chainId, userAddress, password);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }

        if (StringUtils.isBlank(prikeyHex)) {
            return Result.getFailed(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }
        byte[] address = AddressTool.getAddress(userAddress);
        FarmUserInfoPO user = this.userInfoStorageService.load(chainId, farm.getFarmHash(), address);
        if (user == null) {
            return Result.getFailed(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        LedgerAssetDTO ledgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());

        BigInteger realAmount = BigDecimal.valueOf(amount).movePointRight(ledgerAssetDTO.getDecimalPlace()).toBigInteger();
        if (realAmount.compareTo(user.getAmount()) > 0) {
            return Result.getFailed(SwapErrorCode.FARM_NERVE_WITHDRAW_ERROR);
        }

        Transaction tx = new Transaction();
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setType(TxType.FARM_WITHDRAW);
        FarmStakeChangeData data = new FarmStakeChangeData();
        data.setFarmHash(farm.getFarmHash());
        data.setAmount(realAmount);
        try {
            tx.setTxData(data.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }
        NonceBalance balance;
        try {
            balance = ledgerService.getBalanceNonce(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId(), userAddress);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        CoinData coinData = new CoinDataMaker()
                .addFrom(address, farm.getStakeToken(), BigInteger.ZERO, balance.getNonce(), (byte) 0)
                .addTo(address, farm.getStakeToken(), BigInteger.ZERO, 0)
                .getCoinData();
        try {
            tx.setCoinData(coinData.serialize());
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        }


        byte[] prikey = HexUtil.decode(prikeyHex);
        try {
            SwapUtils.signTx(tx, prikey);
            boolean result = TransactionCall.newTx(chainId, HexUtil.encode(tx.serialize()));
            if (result) {
                return Result.getSuccess(tx.getHash().toHex());
            }
        } catch (IOException e) {
            logger.error(e);
            return Result.getFailed(SwapErrorCode.IO_ERROR);
        } catch (NulsException e) {
            logger.error(e);
            return Result.getFailed(e.getErrorCode());
        }
        return Result.getFailed(SwapErrorCode.SYS_UNKOWN_EXCEPTION);
    }

    @Override
    public Result<FarmInfoDTO> farmInfo(String farmHash) {
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        int chainId = AddressTool.getChainIdByAddress(farm.getCreatorAddress());
        LedgerAssetDTO stakeLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());
        LedgerAssetDTO syrupLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId());

        FarmInfoDTO dto = new FarmInfoDTO();
        dto.setFarmHash(farmHash);
        dto.setSyrupPerBlock(new BigDecimal(farm.getSyrupPerBlock()).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2,RoundingMode.DOWN).doubleValue());
        dto.setSyrupBalance(new BigDecimal(farm.getSyrupTokenBalance()).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2,RoundingMode.DOWN).doubleValue());
        dto.setStakeBalance(new BigDecimal(farm.getStakeTokenBalance()).movePointLeft(stakeLedgerAssetDTO.getDecimalPlace()).setScale(2,RoundingMode.DOWN).doubleValue());

        return Result.getSuccess(dto);
    }

    @Override
    public Result<FarmUserInfoDTO> farmUserInfo(String farmHash, String userAddress) {
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        int chainId = AddressTool.getChainIdByAddress(userAddress);

        FarmUserInfoPO user = userInfoStorageService.load(chainId, farm.getFarmHash(), AddressTool.getAddress(userAddress));
        if (user == null) {
            return Result.getFailed(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        LedgerAssetDTO stakeLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());
        LedgerAssetDTO syrupLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId());

        Chain chain = chainManager.getChain(chainId);

        FarmUserInfoDTO dto = new FarmUserInfoDTO();
        dto.setFarmHash(farmHash);
        dto.setUserAddress(userAddress);
        dto.setAmount(new BigDecimal(user.getAmount()).movePointLeft(stakeLedgerAssetDTO.getDecimalPlace()).setScale(2,RoundingMode.DOWN).doubleValue());
        if(user.getAmount().compareTo(BigInteger.ZERO) == 0){
            dto.setReward(0);
            return Result.getSuccess(dto);
        }
        BigInteger accSyrupPerShare;
        if (null != chain.getBestHeight() ) {
            accSyrupPerShare = farm.getAccSyrupPerShare().add((farm.getSyrupPerBlock().multiply(BigInteger.valueOf(chain.getBestHeight() - farm.getLastRewardBlock()))).multiply(SwapConstant.BI_1E12).divide(farm.getStakeTokenBalance()));
        } else {
            accSyrupPerShare = farm.getAccSyrupPerShare();
        }

        BigInteger reward = user.getAmount().multiply(accSyrupPerShare).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        dto.setReward(new BigDecimal(reward).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());
        return Result.getSuccess(dto);
    }
}
