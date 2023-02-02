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
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.dto.*;
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
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static network.nerve.swap.constant.SwapConstant.BI_1E12;

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
    public Result<String> createFarm(String address, String stakeTokenStr, String syrupTokenStr, double syrupPerBlock, long startHeight, long lockedTime, double totalSyrupAmount, boolean modifiable, long withdrawLockTime, String password) {
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
        data.setModifiable(modifiable);
        data.setWithdrawLockTime(withdrawLockTime);
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
//        if (farmHash.equals("83b5cb94ab35fdece398686a53c42f775c70366a1440567c9b40586bdb10cf12")) {
//            return Result.getSuccess(getAllUsers(farm));
//        }
        int chainId = AddressTool.getChainIdByAddress(farm.getCreatorAddress());
        LedgerAssetDTO stakeLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());
        LedgerAssetDTO syrupLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId());

        FarmInfoDTO dto = new FarmInfoDTO();
        dto.setFarmHash(farmHash);
        dto.setSyrupPerBlock(new BigDecimal(farm.getSyrupPerBlock()).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());
        dto.setSyrupBalance(new BigDecimal(farm.getSyrupTokenBalance()).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());
        dto.setStakeBalance(new BigDecimal(farm.getStakeTokenBalance()).movePointLeft(stakeLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());

        return Result.getSuccess(dto);
    }

//    private ErrorCode getAllUsers(FarmPoolPO farm) {
//
//        List<Entry<byte[], byte[]>> list = RocksDBService.entryList(SwapDBConstant.DB_NAME_FARM_USER + 9);
//        BigInteger total = BigInteger.ZERO;
//        SwapUtils.updatePool(farm,22438000);
//        for (Entry<byte[], byte[]> entry : list) {
//            byte[] key = entry.getKey();
//            byte[] farmHash = new byte[32];
//            System.arraycopy(key, 0, farmHash, 0, 32);
//            if (!ArraysTool.arrayEquals(farm.getFarmHash().getBytes(), farmHash)) {
//                continue;
//            }
//            byte[] address = new byte[23];
//            System.arraycopy(key, 32, address, 0, 23);
//            String addr = AddressTool.getStringAddressByBytes(address);
//
//            FarmUserInfoPO user = SwapDBUtil.getModel(entry.getValue(), FarmUserInfoPO.class);
//
//            BigInteger expectedReward = user.getAmount().multiply(farm.getAccSyrupPerShare()).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
//            System.out.println(addr + " :: " + expectedReward.toString());
//            total = total.add(expectedReward);
//        }
//        System.out.println(new BigDecimal(total,18).toString());
//        return null;
//    }

    @Override
    public Result<FarmUserInfoDTO> farmUserInfo(String farmHash, String userAddress) {
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        int chainId = AddressTool.getChainIdByAddress(userAddress);

        Chain chain = chainManager.getChain(chainId);
        FarmUserInfoDTO dto = new FarmUserInfoDTO();
        dto.setFarmHash(farmHash);
        dto.setUserAddress(userAddress);


        FarmUserInfoPO user = userInfoStorageService.load(chainId, farm.getFarmHash(), AddressTool.getAddress(userAddress));
        if (user == null) {
            return Result.getFailed(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        LedgerAssetDTO stakeLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getStakeToken().getChainId(), farm.getStakeToken().getAssetId());
        LedgerAssetDTO syrupLedgerAssetDTO = ledgerService.getNerveAsset(chainId, farm.getSyrupToken().getChainId(), farm.getSyrupToken().getAssetId());
        dto.setAmount(new BigDecimal(user.getAmount()).movePointLeft(stakeLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());

        if (farm.getStartBlockHeight() - chain.getBestHeight() >= 0) {
            dto.setReward(0);
            return Result.getSuccess(dto);
        }

        if (user.getAmount().compareTo(BigInteger.ZERO) == 0) {
            dto.setReward(0);
            return Result.getSuccess(dto);
        }

        BigInteger accSyrupPerShare;
        if (null != chain.getBestHeight()) {
            long realEnd = chain.getBestHeight();
            if (chain.getBestHeight() > SwapContext.PROTOCOL_1_16_0) {
                if (farm.getStopHeight() != null && farm.getStopHeight() > 0 && farm.getStopHeight() < chain.getBestHeight()) {
                    realEnd = farm.getStopHeight();
                }
            }
            accSyrupPerShare = farm.getAccSyrupPerShare().add((farm.getSyrupPerBlock().multiply(BigInteger.valueOf(realEnd - farm.getLastRewardBlock()))).multiply(SwapConstant.BI_1E12).divide(farm.getStakeTokenBalance()));
        } else {
            accSyrupPerShare = farm.getAccSyrupPerShare();
        }

        BigInteger reward = user.getAmount().multiply(accSyrupPerShare).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        dto.setReward(new BigDecimal(reward).movePointLeft(syrupLedgerAssetDTO.getDecimalPlace()).setScale(2, RoundingMode.DOWN).doubleValue());
        if (dto.getReward() < 0) {
            dto.setReward(0d);
        }
        return Result.getSuccess(dto);
    }

    @Override
    public Result<List<FarmInfoVO>> getFarmList(int chainId) {
        Collection<FarmPoolPO> list = farmCache.getList();
        List<FarmInfoVO> volist = new ArrayList<>();
        for (FarmPoolPO po : list) {
            volist.add(poToVo(chainId, po));
        }
        return Result.getSuccess(volist);
    }

    private FarmInfoVO poToVo(int chainId, FarmPoolPO po) {
        FarmInfoVO vo = new FarmInfoVO();
        vo.setFarmHash(po.getFarmHash().toHex());
        vo.setAccSyrupPerShare(po.getAccSyrupPerShare().toString());
        vo.setCreatorAddress(AddressTool.getAddressString(po.getCreatorAddress(), chainId));
        vo.setLockedTime(po.getLockedTime());
        vo.setStakeTokenAssetId(po.getStakeToken().getAssetId());
        vo.setStakeTokenChainId(po.getStakeToken().getChainId());
        vo.setStakeTokenBalance(po.getStakeTokenBalance().toString());
        vo.setModifiable(po.isModifiable());
        vo.setSyrupTokenBalance(po.getSyrupTokenBalance().toString());
        vo.setSyrupTokenAssetId(po.getSyrupToken().getAssetId());
        vo.setSyrupTokenChainId(po.getSyrupToken().getChainId());
        vo.setSyrupPerBlock(po.getSyrupPerBlock().toString());
        vo.setStartBlockHeight(po.getStartBlockHeight());
        vo.setTotalSyrupAmount(po.getTotalSyrupAmount().toString());
        vo.setWithdrawLockTime(po.getWithdrawLockTime());
        if (null != po.getStopHeight()) {
            vo.setStopHeight(po.getStopHeight());
        }
        return vo;
    }

    @Override
    public Result<FarmInfoVO> farmDetail(int chainId, String farmHash) {
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }

        return Result.getSuccess(poToVo(chainId, farm));
    }

    @Override
    public Result<FarmUserInfoVO> farmUserDetail(int chainId, String farmHash, String userAddress) {
        FarmPoolPO farm = farmCache.get(NulsHash.fromHex(farmHash));
        if (null == farm) {
            return Result.getFailed(SwapErrorCode.FARM_NOT_EXIST);
        }
        FarmUserInfoPO user = userInfoStorageService.load(chainId, farm.getFarmHash(), AddressTool.getAddress(userAddress));
        if (user == null) {
            return Result.getFailed(SwapErrorCode.FARM_NERVE_STAKE_ERROR);
        }
        Chain chain = chainManager.getChain(chainId);

        FarmUserInfoVO dto = new FarmUserInfoVO();
        dto.setFarmHash(farmHash);
        dto.setUserAddress(userAddress);
        dto.setAmount(user.getAmount().toString());
        BigInteger accSyrupPerShare;
        if (farm.getStartBlockHeight() - chain.getBestHeight() >= 0) {
            dto.setReward("0");
            return Result.getSuccess(dto);
        }

        if (user.getAmount().compareTo(BigInteger.ZERO) == 0) {
            dto.setReward("0");
            return Result.getSuccess(dto);
        }

        if (null != chain.getBestHeight()) {
            long realEnd = chain.getBestHeight();
            if (chain.getBestHeight() > SwapContext.PROTOCOL_1_16_0) {
                if (farm.getStopHeight() != null && farm.getStopHeight() > 0 && farm.getStopHeight() < chain.getBestHeight()) {
                    realEnd = farm.getStopHeight();
                }
            }
            accSyrupPerShare = farm.getAccSyrupPerShare().add((farm.getSyrupPerBlock().multiply(BigInteger.valueOf(realEnd - farm.getLastRewardBlock()))).multiply(SwapConstant.BI_1E12).divide(farm.getStakeTokenBalance()));
        } else {
            accSyrupPerShare = farm.getAccSyrupPerShare();
        }
        BigInteger reward = user.getAmount().multiply(accSyrupPerShare).divide(SwapConstant.BI_1E12).subtract(user.getRewardDebt());
        dto.setReward(reward.toString());

        return Result.getSuccess(dto);
    }
}
