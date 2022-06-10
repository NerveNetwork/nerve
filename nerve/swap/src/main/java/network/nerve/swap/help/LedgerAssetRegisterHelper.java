/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.rpc.call.LedgerCall;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;
import network.nerve.swap.utils.SwapUtils;

import static network.nerve.swap.constant.SwapConstant.LP_TOKEN_DECIMALS;

/**
 * @author: mimi
 * @date: 2020-05-29
 */
@Component
public class LedgerAssetRegisterHelper {

    @Autowired
    private SwapPairStorageService swapPairStorageService;
    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    public LedgerAssetDTO lpAssetReg(int chainId, NerveToken tokenA, NerveToken tokenB) throws Exception {
        String assetSymbol = lpTokenSymbol(chainId, tokenA, tokenB);
        String assetAddress = SwapUtils.getStringPairAddress(chainId, tokenA, tokenB);
        Integer lpAssetId = LedgerCall.lpAssetReg(chainId, assetSymbol, LP_TOKEN_DECIMALS, assetSymbol, assetAddress);
        byte[] assetAddressBytes = AddressTool.getAddress(assetAddress);
        SwapPairPO po = new SwapPairPO(assetAddressBytes);
        NerveToken tokenLP = new NerveToken(chainId, lpAssetId);
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        po.setToken0(tokens[0]);
        po.setToken1(tokens[1]);
        po.setTokenLP(tokenLP);
        swapPairStorageService.savePair(assetAddressBytes, po);
        return new LedgerAssetDTO(chainId, lpAssetId, assetSymbol, assetSymbol, LP_TOKEN_DECIMALS);
    }

    public SwapPairPO deleteLpAsset(int chainId, NerveToken tokenA, NerveToken tokenB) throws Exception {
        String assetAddress = SwapUtils.getStringPairAddress(chainId, tokenA, tokenB);
        SwapPairPO pair = swapPairStorageService.getPair(assetAddress);
        LedgerCall.lpAssetDelete(pair.getTokenLP().getAssetId());
        swapPairStorageService.delelePair(assetAddress);
        return pair;
    }

    public LedgerAssetDTO lpAssetRegForStable(int chainId, String pairAddress, NerveToken[] coins, String symbol) throws Exception {
        LedgerAssetDTO ledgerAsset0 = ledgerAssetCache.getLedgerAsset(chainId, coins[0]);
        String assetSymbol;
        if (StringUtils.isNotBlank(symbol)) {
            assetSymbol = symbol;
        } else {
            assetSymbol = SwapConstant.STABLE_PAIR + ledgerAsset0.getAssetSymbol();
        }
        String assetAddress = pairAddress;
        Integer lpAssetId = LedgerCall.lpAssetReg(chainId, assetSymbol, LP_TOKEN_DECIMALS, assetSymbol, assetAddress);
        byte[] assetAddressBytes = AddressTool.getAddress(assetAddress);
        StableSwapPairPo po = new StableSwapPairPo(assetAddressBytes);
        NerveToken tokenLP = new NerveToken(chainId, lpAssetId);
        po.setTokenLP(tokenLP);
        po.setCoins(coins);
        int length = coins.length;
        int[] decimalsOfCoins = new int[length];
        decimalsOfCoins[0] = ledgerAsset0.getDecimalPlace();
        for (int i = 1; i < length; i++) {
            decimalsOfCoins[i] = ledgerAssetCache.getLedgerAsset(chainId, coins[i]).getDecimalPlace();
        }
        po.setDecimalsOfCoins(decimalsOfCoins);
        swapStablePairStorageService.savePair(assetAddressBytes, po);
        return new LedgerAssetDTO(chainId, lpAssetId, assetSymbol, assetSymbol, LP_TOKEN_DECIMALS);
    }

    public StableSwapPairPo deleteLpAssetForStable(int chainId, String pairAddress) throws Exception {
        String assetAddress = pairAddress;
        StableSwapPairPo pair = swapStablePairStorageService.getPair(assetAddress);
        LedgerCall.lpAssetDelete(pair.getTokenLP().getAssetId());
        swapStablePairStorageService.delelePair(assetAddress);
        return pair;
    }

    private String lpTokenSymbol(int chainId, NerveToken tokenA, NerveToken tokenB) {
        NerveToken[] tokens = SwapUtils.tokenSort(tokenA, tokenB);
        NerveToken token0 = tokens[0];
        NerveToken token1 = tokens[1];
        LedgerAssetDTO ledgerAsset0 = ledgerAssetCache.getLedgerAsset(chainId, token0);
        LedgerAssetDTO ledgerAsset1 = ledgerAssetCache.getLedgerAsset(chainId, token1);
        String symbol0 = token0.str();
        String symbol1 = token1.str();
        if (ledgerAsset0 != null && StringUtils.isNotBlank(ledgerAsset0.getAssetSymbol())) {
            symbol0 = ledgerAsset0.getAssetSymbol();
        }
        if (ledgerAsset1 != null && StringUtils.isNotBlank(ledgerAsset1.getAssetSymbol())) {
            symbol1 = ledgerAsset1.getAssetSymbol();
        }
        return new StringBuilder(symbol0).append("_").append(symbol1).append("_LP").toString();
    }


}
