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
package network.nerve.swap.help;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.stable.StableSwapPairBalancesPo;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: PierreLuo
 * @date: 2021/5/8
 */
@Component
public class StableSwapHelper {

    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;
    @Autowired
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    public boolean isLegalCoinForAddStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        SwapContext.logger.debug("chainId: {}, stablePairAddress: {}, assetChainId: {}, assetId: {},", chainId, stablePairAddress, assetChainId, assetId);
        LedgerAssetDTO asset = LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
        if (asset == null) {
            SwapContext.logger.error("资产不存在. {}-{}-{}", chainId, assetChainId, assetId);
            return false;
        }
        if (asset.getDecimalPlace() > 18) {
            SwapContext.logger.error("coin_decimal_exceeded! stablePairAddress: {}, assetChainId: {}, assetId: {}, decimal: {}", stablePairAddress, assetChainId, assetId, asset.getDecimalPlace());
            return false;
        }
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            SwapContext.logger.error("交易对不存在. {}", stablePairAddress);
            return false;
        }
        NerveToken[] coins = pairPo.getCoins();
        for (NerveToken coin : coins) {
            if (coin.getChainId() == assetChainId && coin.getAssetId() == assetId) {
                SwapContext.logger.error("资产冲突. {}, {}-{}", coin.str(), assetChainId, assetId);
                return false;
            }
        }
        return true;
    }

    public boolean isLegalStable(int chainId, String stablePairAddress) throws NulsException {
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            SwapContext.logger.error("交易对不存在. {}", stablePairAddress);
            return false;
        }
        return true;
    }

    public boolean addCoinForStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws Exception {
        byte[] address = AddressTool.getAddress(stablePairAddress);
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            return false;
        }
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        NerveToken newCoin = new NerveToken(assetChainId, assetId);
        NerveToken[] newCoins = Arrays.copyOf(coins, length + 1);
        newCoins[length] = newCoin;
        pairPo.setCoins(newCoins);
        LedgerAssetDTO newAsset = ledgerAssetCache.getLedgerAsset(chainId, newCoin);
        int[] newDecimalsOfCoins = Arrays.copyOf(pairPo.getDecimalsOfCoins(), length + 1);
        newDecimalsOfCoins[length] = newAsset.getDecimalPlace();
        pairPo.setDecimalsOfCoins(newDecimalsOfCoins);
        swapStablePairStorageService.savePair(address, pairPo);

        StableSwapPairBalancesPo pairBalancesPo = swapStablePairBalancesStorageService.getPairBalances(stablePairAddress);
        BigInteger[] balances = pairBalancesPo.getBalances();
        BigInteger[] newBalances = Arrays.copyOf(balances, length + 1);
        newBalances[length] = BigInteger.ZERO;
        pairBalancesPo.setBalances(newBalances);
        swapStablePairBalancesStorageService.savePairBalances(stablePairAddress, pairBalancesPo);

        // 更新缓存
        stableSwapPairCache.reload(stablePairAddress);
        SwapContext.stableCoinGroup.updateStableCoin(stablePairAddress, newCoins);
        return true;
    }

    public boolean addStableForSwapTrade(int chainId, String stablePairAddress) throws Exception {
        // DB持久化
        swapStablePairStorageService.savePairForSwapTrade(stablePairAddress);
        // 更新缓存
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            return false;
        }
        NerveToken[] coins = pairPo.getCoins();
        SwapContext.stableCoinGroup.add(stablePairAddress, coins);
        return true;
    }

    public boolean removeStableForSwapTrade(int chainId, String stablePairAddress) throws Exception {
        // DB持久化
        swapStablePairStorageService.delelePairForSwapTrade(stablePairAddress);
        // 更新缓存
        SwapContext.stableCoinGroup.remove(stablePairAddress);
        return true;
    }
}
