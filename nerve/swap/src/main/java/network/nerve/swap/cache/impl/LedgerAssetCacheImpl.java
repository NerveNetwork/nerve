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

import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.rpc.call.LedgerCall;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021/4/12
 */
@Component
public class LedgerAssetCacheImpl implements LedgerAssetCache {

    //Different chain addresses will not be the same, so chains will no longer be distinguished
    private Map<String, LedgerAssetDTO> CACHE_MAP = new HashMap<>();

    @Override
    public LedgerAssetDTO getLedgerAsset(int chainId, int assetChainId, int assetId) {
        String key = assetChainId + "_" + assetId;
        LedgerAssetDTO dto = CACHE_MAP.get(key);
        if (dto == null) {
            dto = LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
            if (dto == null) {
                return null;
            }
            CACHE_MAP.put(key, dto);
        }
        return dto;
    }

    @Override
    public LedgerAssetDTO getLedgerAsset(int chainId, NerveToken token) {
        if (token == null) {
            return null;
        }
        return getLedgerAsset(chainId, token.getChainId(), token.getAssetId());
    }
}
