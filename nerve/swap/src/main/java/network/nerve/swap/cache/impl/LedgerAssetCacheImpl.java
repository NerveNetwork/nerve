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
package network.nerve.swap.cache.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.rpc.call.LedgerCall;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: PierreLuo
 * @date: 2021/4/12
 */
@Component
public class LedgerAssetCacheImpl implements LedgerAssetCache {

    //Different chain addresses will not be the same, so chains will no longer be distinguished
    //private Map<String, LedgerAssetDTO> CACHE_MAP = new HashMap<>();
    private LoadingCache<String, LedgerAssetDTO> CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, LedgerAssetDTO>() {
                @Override
                public LedgerAssetDTO load(String key) throws Exception {
                    String[] assetInfo = key.split("-");
                    int chainId = Integer.parseInt(assetInfo[0]);
                    int assetChainId = Integer.parseInt(assetInfo[1]);
                    int assetId = Integer.parseInt(assetInfo[2]);
                    return LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
                }
            });

    private LedgerAssetDTO getAssetFromCache(int chainId, int assetChainId, int assetId) {
        try {
            String key = chainId + "-" + assetChainId + "-" + assetId;
            return CACHE.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public LedgerAssetDTO getLedgerAsset(int chainId, int assetChainId, int assetId) {
        return getAssetFromCache(chainId, assetChainId, assetId);
    }

    @Override
    public LedgerAssetDTO getLedgerAsset(int chainId, NerveToken token, long height) {
        LedgerAssetDTO asset = getLedgerAsset(chainId, token);
        if (null == asset && height >= 55671490 && height <= 56671490) {
            if (token.getAssetId() == 146 && token.getChainId() == 1) {
                asset = new LedgerAssetDTO(1, 146, "NABOX", "NABOX", 18);
            } else if (token.getChainId() == 9 && token.getAssetId() == 195) {
                asset = new LedgerAssetDTO(9, 195, "USDT", "USDT", 18);
            }
        }
        return asset;
    }

    @Override
    public LedgerAssetDTO getLedgerAsset(int chainId, NerveToken token) {
        if (token == null) {
            return null;
        }
        return getLedgerAsset(chainId, token.getChainId(), token.getAssetId());
    }
}
