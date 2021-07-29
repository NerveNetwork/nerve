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

package network.nerve.swap.manager.stable;

import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2021/4/6
 */
public class StableSwapTempPairManager {

    private int chainId;
    /**
     * 用于校验
     */
    private Set<String> tempCreatePairs = new HashSet<>();
    private StableSwapPairCache stableSwapPairCache;
    private Map<String, StableSwapPairDTO> pairs = new HashMap<>();

    private StableSwapTempPairManager() {
        this.stableSwapPairCache = SpringLiteContext.getBean(StableSwapPairCache.class);
    }

    public static StableSwapTempPairManager newInstance(int chainId) {
        StableSwapTempPairManager stableSwapTempPairManager = new StableSwapTempPairManager();
        stableSwapTempPairManager.chainId = chainId;
        return stableSwapTempPairManager;
    }

    public StableSwapPairDTO get(String address) {
        StableSwapPairDTO swapPairDTO = pairs.get(address);
        if (swapPairDTO == null) {
            swapPairDTO = stableSwapPairCache.get(address);
            if (swapPairDTO == null) {
                return null;
            }
            swapPairDTO = swapPairDTO.clone();
            pairs.put(address, swapPairDTO);
        }
        return swapPairDTO;
    }

    public boolean isExist(String address) {
        return stableSwapPairCache.isExist(address) || tempCreatePairs.contains(address);
    }

    public void add(String address) {
        tempCreatePairs.add(address);
    }
}
