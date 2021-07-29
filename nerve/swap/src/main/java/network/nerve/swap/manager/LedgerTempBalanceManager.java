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

package network.nerve.swap.manager;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.rpc.call.LedgerCall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.swap.utils.SwapUtils.asStringByBase64;

/**
 * @author: PierreLuo
 * @date: 2021/4/6
 */
public class LedgerTempBalanceManager {

    protected int chainId;
    protected Map<String, LedgerBalance> tempBalanceMap;

    public static LedgerTempBalanceManager newInstance(int chainId) {
        LedgerTempBalanceManager temp = new LedgerTempBalanceManager();
        temp.chainId = chainId;
        temp.tempBalanceMap = new HashMap<>();
        return temp;
    }

    public Result<LedgerBalance> getBalance(byte[] address, int assetChainId, int assetId) {
        try {
            if (address == null || address.length != Address.ADDRESS_LENGTH) {
                return Result.getFailed(SwapErrorCode.PARAMETER_ERROR);
            }

            String addressKey = balanceKey(address, assetChainId,  assetId);
            LedgerBalance balance = tempBalanceMap.get(addressKey);
            // 临时余额区没有余额，则从真实余额中取值
            if (balance == null) {
                // 初始化临时余额区
                NonceBalance balanceNonce = LedgerCall.getBalanceNonce(chainId, assetChainId, assetId, AddressTool.getStringAddressByBytes(address));
                balance = LedgerBalance.newInstance();
                balance.setBalance(balanceNonce.getAvailable());
                balance.setNonce(balanceNonce.getNonce());
                balance.setFreeze(balanceNonce.getFreeze());
                balance.setAddress(address);
                balance.setAssetsChainId(assetChainId);
                balance.setAssetsId(assetId);
                tempBalanceMap.put(addressKey, balance);
            }
            return Result.getSuccess(SwapErrorCode.SUCCESS).setData(balance);
        } catch (NulsException e) {
            SwapContext.logger.error(e.format());
            return Result.getFailed(e.getErrorCode());
        }

    }


    public void addTempBalanceForTest(byte[] address, BigInteger amount, int assetChainId, int assetId) {
        LedgerBalance ledgerBalance = LedgerBalance.newInstance();
        ledgerBalance.setBalance(amount);
        ledgerBalance.setFreeze(BigInteger.ZERO);
        ledgerBalance.setAddress(address);
        ledgerBalance.setAssetsId(assetId);
        ledgerBalance.setAssetsChainId(assetChainId);
        ledgerBalance.setNonce(new byte[8]);
        ledgerBalance.setPreNonce(new byte[8]);
        this.tempBalanceMap.put(balanceKey(address,assetChainId,assetId),ledgerBalance);
    }

    public void addTempBalance(byte[] address, BigInteger amount, int assetChainId, int assetId) {
        LedgerBalance ledgerBalance = tempBalanceMap.get(balanceKey(address, assetChainId,  assetId));
        if (ledgerBalance != null) {
            ledgerBalance.addTemp(amount);
        }
    }

    public void minusTempBalance(byte[] address, BigInteger amount, int assetChainId, int assetId) {
        LedgerBalance ledgerBalance = tempBalanceMap.get(balanceKey(address, assetChainId,  assetId));
        if (ledgerBalance != null) {
            ledgerBalance.minusTemp(amount);
        }
    }

    public void addLockedTempBalance(byte[] address, BigInteger amount, int assetChainId, int assetId) {
        LedgerBalance ledgerBalance = tempBalanceMap.get(balanceKey(address, assetChainId,  assetId));
        if (ledgerBalance != null) {
            ledgerBalance.addLockedTemp(amount);
        }
    }

    public void minusLockedTempBalance(byte[] address, BigInteger amount, int assetChainId, int assetId) {
        LedgerBalance ledgerBalance = tempBalanceMap.get(balanceKey(address, assetChainId,  assetId));
        if (ledgerBalance != null) {
            ledgerBalance.minusLockedTemp(amount);
        }
    }

    public void refreshTempBalance(int chainId, Transaction tx, long blockTime) {
        NulsHash hash = tx.getHash();
        byte[] hashBytes = hash.getBytes();
        byte[] currentNonceBytes = Arrays.copyOfRange(hashBytes, hashBytes.length - 8, hashBytes.length);
        CoinData coinData;
        try {
            coinData = tx.getCoinDataInstance();
        } catch (NulsException e) {
            throw new NulsRuntimeException(e);
        }
        List<CoinFrom> froms = coinData.getFrom();
        List<CoinTo> tos = coinData.getTo();
        byte[] address;
        int assetChainId, assetId;
        for (CoinFrom from : froms) {
            address = from.getAddress();
            assetChainId = from.getAssetsChainId();
            assetId = from.getAssetsId();
            Result<LedgerBalance> balanceResult = getBalance(address, assetChainId, assetId);
            if (balanceResult.isFailed()) {
                throw new NulsRuntimeException(balanceResult.getErrorCode());
            }
            LedgerBalance ledgerBalance = balanceResult.getData();
            ledgerBalance.setNonce(currentNonceBytes);
            if (isLockedAmount(blockTime, from.getLocked())) {
                ledgerBalance.minusLockedTemp(from.getAmount());
            } else {
                ledgerBalance.minusTemp(from.getAmount());
            }
        }
        for (CoinTo to : tos) {
            address = to.getAddress();
            assetChainId = to.getAssetsChainId();
            assetId = to.getAssetsId();
            Result<LedgerBalance> balanceResult = getBalance(address, assetChainId, assetId);
            if (balanceResult.isFailed()) {
                throw new NulsRuntimeException(balanceResult.getErrorCode());
            }
            LedgerBalance ledgerBalance = balanceResult.getData();
            if (isLockedAmount(blockTime, to.getLockTime())) {
                ledgerBalance.addLockedTemp(to.getAmount());
            } else {
                ledgerBalance.addTemp(to.getAmount());
            }
        }
    }

    protected String balanceKey(byte[] address, int assetChainId, int assetId) {
        return new StringBuilder(chainId).append(asStringByBase64(address)).append(SwapConstant.LINE).append(assetChainId).append(SwapConstant.LINE).append(assetId).toString();
    }

    protected boolean isLockedAmount(long blockTime, long lockTime) {
        if(lockTime < 0) {
            return true;
        }
        if(blockTime < lockTime) {
            return true;
        }
        return false;
    }
}
