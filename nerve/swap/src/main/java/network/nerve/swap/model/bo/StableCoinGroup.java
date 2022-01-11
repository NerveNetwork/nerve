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
package network.nerve.swap.model.bo;

import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.storage.SwapStablePairStorageService;

import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2021/10/28
 */
public class StableCoinGroup {

    private List<StableCoin> groupList;
    private Map<String, Integer> stableIndexMap;
    private int stableIndex;
    private LinkedHashSet<String> addressList;
    private StableSwapPairCache stableSwapPairCache;
    private SwapStablePairStorageService swapStablePairStorageService;
    private SwapConfig swapConfig;
    private boolean cacheCompleted;

    public StableCoinGroup() {
        this.groupList = new ArrayList<>();
        this.stableIndexMap = new HashMap<>();
        this.stableIndex = 0;
        this.addressList = new LinkedHashSet<>();
        this.cacheCompleted = false;
    }

    public void addAddress(String stablePairAddress, StableSwapPairCache stableSwapPairCache, SwapStablePairStorageService swapStablePairStorageService, SwapConfig swapConfig) {
        this.addressList.add(stablePairAddress);
        this.stableSwapPairCache = stableSwapPairCache;
        this.swapStablePairStorageService = swapStablePairStorageService;
        this.swapConfig = swapConfig;
    }

    public void add(String stablePairAddress, NerveToken[] coins) {
        loadCache();
        this._add(stablePairAddress, coins);
        this.addressList.add(stablePairAddress);
    }

    public void remove(String stablePairAddress) {
        loadCache();
        Integer index = stableIndexMap.get(stablePairAddress);
        if (index == null)
            return;
        groupList.get(index).clear();
        stableIndexMap.remove(stablePairAddress);
        this.addressList.remove(stablePairAddress);
    }

    public int groupIndex(NerveToken token1, NerveToken token2) {
        loadCache();
        int index = 0;
        Set<NerveToken> groupCoin;
        for (StableCoin group : groupList) {
            groupCoin = group.getGroupCoin();
            if (groupCoin.contains(token1) && groupCoin.contains(token2)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public String getAddressByIndex(int index) {
        loadCache();
        if (index < 0 || index > groupList.size() - 1)
            return null;
        String address = groupList.get(index).getAddress();
        if (StringUtils.isBlank(address)) {
            return null;
        }
        return address;
    }

    public void updateStableCoin(String stablePairAddress, NerveToken[] coins) {
        loadCache();
        Integer index = stableIndexMap.get(stablePairAddress);
        if (index == null)
            return;
        StableCoin stableCoin = groupList.get(index);
        Set<NerveToken> stableCoinGroup = new HashSet<>();
        for (NerveToken coin : coins) {
            stableCoinGroup.add(coin);
        }
        stableCoin.setGroupCoin(stableCoinGroup);
    }

    public List<StableCoin> getGroupList() {
        loadCache();
        return groupList;
    }

    private void _add(String stablePairAddress, NerveToken[] coins) {
        if (stableIndexMap.containsKey(stablePairAddress)) {
            return;
        }
        Set<NerveToken> stableCoinGroup = new HashSet<>();
        for (NerveToken coin : coins) {
            stableCoinGroup.add(coin);
        }
        groupList.add(new StableCoin(stablePairAddress, stableCoinGroup));
        stableIndexMap.put(stablePairAddress, stableIndex++);
    }

    private void loadCache() {
        if (cacheCompleted) {
            return;
        }
        // 缓存: 管理稳定币交易对-用于Swap交易
        try {
            if (addressList.size() > 0) {
                boolean initialDBDone = swapStablePairStorageService.existPairForSwapTrade(addressList.iterator().next().trim());
                if (!initialDBDone) {
                    cacheCompleted = true;
                    for (String stable : addressList) {
                        stable = stable.trim();
                        StableSwapPairDTO dto = stableSwapPairCache.get(stable);
                        if (dto == null) {
                            cacheCompleted = false;
                            continue;
                        }
                        this._add(stable, dto.getPo().getCoins());
                        swapStablePairStorageService.savePairForSwapTrade(stable);
                    }
                } else {
                    loadFromDB();
                }
            } else {
                loadFromDB();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFromDB() throws Exception {
        cacheCompleted = true;
        List<String> allForSwapTrade = swapStablePairStorageService.findAllForSwapTrade(swapConfig.getChainId());
        for (String stable : allForSwapTrade) {
            stable = stable.trim();
            StableSwapPairDTO dto = stableSwapPairCache.get(stable);
            if (dto == null) {
                continue;
            }
            this._add(stable, dto.getPo().getCoins());
        }
    }
}
