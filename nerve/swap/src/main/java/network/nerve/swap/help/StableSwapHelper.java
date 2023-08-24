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

    public boolean isLegalCoinForRemoveStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        SwapContext.logger.debug("chainId: {}, stablePairAddress: {}, assetChainId: {}, assetId: {},", chainId, stablePairAddress, assetChainId, assetId);
        LedgerAssetDTO asset = LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
        if (asset == null) {
            SwapContext.logger.error("资产不存在. {}-{}-{}", chainId, assetChainId, assetId);
            return false;
        }
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            SwapContext.logger.error("交易对不存在. {}", stablePairAddress);
            return false;
        }
        // 检查移除资产逻辑，存在于池中，并且资产数量为0
        boolean isExist = false;
        NerveToken[] coins = pairPo.getCoins();
        NerveToken coin;
        int index = 0;
        for (int i = 0, len = coins.length; i < len; i++) {
            coin = coins[i];
            if (coin.getChainId() == assetChainId && coin.getAssetId() == assetId) {
                isExist = true;
                index = i;
                break;
            }
        }
        if (!isExist) {
            SwapContext.logger.error("资产不存在于交易对中. {}-{}-{}, stablePairAddress: {}", chainId, assetChainId, assetId, stablePairAddress);
            return false;
        }
        StableSwapPairBalancesPo pairBalances = swapStablePairBalancesStorageService.getPairBalances(stablePairAddress);
        BigInteger[] balances = pairBalances.getBalances();
        if (balances[index].compareTo(BigInteger.ZERO) > 0) {
            SwapContext.logger.error("交易对中此资产金额不为0. amount: {}, stablePairAddress: {}", balances[index], stablePairAddress);
            return false;
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
        boolean[] removes = pairPo.getRemoves();
        if (removes == null) {
            removes = new boolean[length + 1];
        } else {
            removes = Arrays.copyOf(removes, length + 1);
        }
        removes[length] = false;
        pairPo.setRemoves(removes);
        swapStablePairStorageService.savePair(address, pairPo);

        StableSwapPairBalancesPo pairBalancesPo = swapStablePairBalancesStorageService.getPairBalances(stablePairAddress);
        BigInteger[] balances = pairBalancesPo.getBalances();
        BigInteger[] newBalances = Arrays.copyOf(balances, length + 1);
        newBalances[length] = BigInteger.ZERO;
        pairBalancesPo.setBalances(newBalances);
        swapStablePairBalancesStorageService.savePairBalances(stablePairAddress, pairBalancesPo);

        // 更新缓存
        stableSwapPairCache.reload(stablePairAddress);
        SwapContext.stableCoinGroup.updateStableCoin(stablePairAddress, newCoins, removes);
        SwapContext.logger.info("[Commit AddCoinForStable] stablePairAddress: {}, new coin: {}", stablePairAddress, newCoin.str());
        return true;
    }

    /**
     * 设置移除状态
     */
    public boolean removeCoinForStableV2(int chainId, String stablePairAddress, int assetChainId, int assetId, String status) throws Exception {
        byte[] address = AddressTool.getAddress(stablePairAddress);
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            return false;
        }
        // 移除多链路由池的币种
        NerveToken removeCoin = new NerveToken(assetChainId, assetId);
        NerveToken[] coins = pairPo.getCoins();
        int length = coins.length;
        boolean[] removes = pairPo.getRemoves();
        if (removes == null) {
            removes = new boolean[length];
        }

        int removeIndex = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            if (coins[i].equals(removeCoin)) {
                removeIndex = i;
                break;
            }
        }
        if ("recovery".equalsIgnoreCase(status)) {
            removes[removeIndex] = false;
        } else {
            removes[removeIndex] = true;
        }
        pairPo.setRemoves(removes);
        swapStablePairStorageService.savePair(address, pairPo);
        // 更新缓存
        stableSwapPairCache.reload(stablePairAddress);
        SwapContext.stableCoinGroup.updateStableCoin(stablePairAddress, coins, removes);
        SwapContext.logger.info("[Commit [{}]CoinForStable] stablePairAddress: {}, {} coin: {}", status, stablePairAddress, status, removeCoin.str());
        return true;
    }

    /**
     * 数组缩短，index被改变，造成未知影响，弃用，保留代码
     */
    private boolean removeCoinForStableV1(int chainId, String stablePairAddress, int assetChainId, int assetId) throws Exception {
        byte[] address = AddressTool.getAddress(stablePairAddress);
        StableSwapPairPo pairPo = swapStablePairStorageService.getPair(stablePairAddress);
        if (pairPo == null) {
            return false;
        }
        // 移除多链路由池的币种
        NerveToken removeCoin = new NerveToken(assetChainId, assetId);
        NerveToken[] coins = pairPo.getCoins();
        int[] decimalsOfCoins = pairPo.getDecimalsOfCoins();
        StableSwapPairBalancesPo pairBalancesPo = swapStablePairBalancesStorageService.getPairBalances(stablePairAddress);
        BigInteger[] balances = pairBalancesPo.getBalances();

        int length = coins.length;
        NerveToken[] newCoins = new NerveToken[length - 1];
        int[] newDecimalsOfCoins = new int[length - 1];
        BigInteger[] newBalances = new BigInteger[length - 1];
        int newIndex = 0;
        NerveToken coin;
        for (int i = 0; i < length; i++) {
            coin = coins[i];
            if (coin.equals(removeCoin)) {
                continue;
            }
            newCoins[newIndex] = coin;
            newDecimalsOfCoins[newIndex] = decimalsOfCoins[i];
            newBalances[newIndex] = balances[i];
            newIndex++;
        }
        pairPo.setCoins(newCoins);
        pairPo.setDecimalsOfCoins(newDecimalsOfCoins);
        pairBalancesPo.setBalances(newBalances);
        swapStablePairStorageService.savePair(address, pairPo);
        swapStablePairBalancesStorageService.savePairBalances(stablePairAddress, pairBalancesPo);

        // 更新缓存
        stableSwapPairCache.reload(stablePairAddress);
        SwapContext.stableCoinGroup.updateStableCoin(stablePairAddress, newCoins, null);
        SwapContext.logger.info("[Commit RemoveCoinForStable] stablePairAddress: {}, remove coin: {}", stablePairAddress, removeCoin.str());
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
        SwapContext.stableCoinGroup.add(stablePairAddress, coins, pairPo.getRemoves());
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
