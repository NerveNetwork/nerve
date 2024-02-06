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
package network.nerve.swap.help.impl.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
public class PersistenceStablePair extends AbstractStablePair {

    private StableSwapPairDTO stableSwapPairDTO;
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;
    private StableSwapPairCache stableSwapPairCache;

    public PersistenceStablePair(StableSwapPairDTO stableSwapPairDTO,
                                 SwapStablePairBalancesStorageService swapStablePairBalancesStorageService,
                                 StableSwapPairCache stableSwapPairCache) {
        this.stableSwapPairDTO = stableSwapPairDTO;
        this.swapStablePairBalancesStorageService = swapStablePairBalancesStorageService;
        this.stableSwapPairCache = stableSwapPairCache;
    }

    @Override
    protected StableSwapPairDTO getStableSwapPairDTO() {
        return stableSwapPairDTO;
    }

    @Override
    public void _update(BigInteger liquidityChange, BigInteger[] newBalances, long blockHeight, long blockTime) throws Exception {
        stableSwapPairDTO.setTotalLP(stableSwapPairDTO.getTotalLP().add(liquidityChange));
        stableSwapPairDTO.setBalances(newBalances);

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        this.savePairBalances(pairAddress, newBalances, stableSwapPairDTO.getTotalLP(), blockTime, blockHeight);
        // Update cache
        stableSwapPairCache.reload(pairAddress);
    }

    @Override
    public void _rollback(BigInteger liquidityChange, BigInteger[] balances, long blockHeight, long blockTime) throws Exception {
        stableSwapPairDTO.setTotalLP(stableSwapPairDTO.getTotalLP().subtract(liquidityChange));
        stableSwapPairDTO.setBalances(balances);

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        this.savePairBalances(pairAddress, balances, stableSwapPairDTO.getTotalLP(), blockTime, blockHeight);
        // Update cache
        stableSwapPairCache.reload(pairAddress);
    }

    private void savePairBalances(String pairAddress, BigInteger[] balancesCurrent, BigInteger totalLP, Long blockTime, Long blockHeight) throws Exception {
        int lengthCurrent = balancesCurrent.length;
        StableSwapPairBalancesPo pairBalancesPo = swapStablePairBalancesStorageService.getPairBalances(pairAddress);
        if (pairBalancesPo == null) {
            pairBalancesPo = new StableSwapPairBalancesPo(AddressTool.getAddress(pairAddress), lengthCurrent);
        }
        BigInteger[] balancesFromDB = pairBalancesPo.getBalances();
        int lengthFromDB = balancesFromDB.length;
        // If the current length is less thanDBThe length in indicates that if a currency has been added to the transaction pair, it cannot overwrite the updatePO
        if (lengthCurrent < lengthFromDB) {
            for (int i = 0; i < lengthCurrent; i++) {
                balancesFromDB[i] = balancesCurrent[i];
            }
            pairBalancesPo.setBalances(balancesFromDB);
        } else if (lengthCurrent == lengthFromDB) {
            pairBalancesPo.setBalances(balancesCurrent);
        } else {
            // The function of removing currency is not provided, and this scenario is abnormal when it occurs
            throw new NulsException(SwapErrorCode.COIN_LENGTH_ERROR);
        }
        pairBalancesPo.setTotalLP(totalLP);
        pairBalancesPo.setBlockTimeLast(blockTime);
        pairBalancesPo.setBlockHeightLast(blockHeight);
        //SwapContext.logger.info("[{}]pairAddress: {}, stable info: {}", blockHeight, pairAddress, pairBalancesPo.toString());
        swapStablePairBalancesStorageService.savePairBalances(pairAddress, pairBalancesPo);
        //SwapContext.logger.info("[{}]pairAddress: {}, save done: {}", blockHeight, pairAddress, bool);
    }

}
