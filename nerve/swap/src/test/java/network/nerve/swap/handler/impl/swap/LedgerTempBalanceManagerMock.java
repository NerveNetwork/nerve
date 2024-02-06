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
package network.nerve.swap.handler.impl.swap;

import io.nuls.base.data.Address;
import io.nuls.core.basic.Result;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.bo.LedgerBalance;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * @author: PierreLuo
 * @date: 2021/5/12
 */
public class LedgerTempBalanceManagerMock extends LedgerTempBalanceManager {

    public static LedgerTempBalanceManager newInstance(int chainId) {
        LedgerTempBalanceManagerMock temp = new LedgerTempBalanceManagerMock();
        temp.chainId = chainId;
        temp.tempBalanceMap = new HashMap<>();
        return temp;
    }

    @Override
    public Result<LedgerBalance> getBalance(byte[] address, int assetChainId, int assetId) {
        if (address == null || address.length != Address.ADDRESS_LENGTH) {
            return Result.getFailed(SwapErrorCode.PARAMETER_ERROR);
        }

        String addressKey = balanceKey(address, assetChainId,  assetId);
        LedgerBalance balance = tempBalanceMap.get(addressKey);
        // If there is no balance in the temporary balance area, take the value from the actual balance
        if (balance == null) {
            // Initialize temporary balance area
            balance = LedgerBalance.newInstance();
            balance.setBalance(BigInteger.ZERO);
            balance.setNonce(SwapConstant.DEFAULT_NONCE);
            balance.setFreeze(BigInteger.ZERO);
            balance.setAddress(address);
            balance.setAssetsChainId(assetChainId);
            balance.setAssetsId(assetId);
            tempBalanceMap.put(addressKey, balance);
        }
        return Result.getSuccess(SwapErrorCode.SUCCESS).setData(balance);
    }
}
