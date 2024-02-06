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

import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.model.dto.SwapPairDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2021/4/6
 */
public class SwapTempPairManager {

    private int chainId;
    /**
     * Used for verification
     */
    private Set<String> tempCreatePairs = new HashSet<>();
    private SwapPairCache swapPairCache;
    private Map<String, SwapPairDTO> pairs = new HashMap<>();

    private SwapTempPairManager() {
        this.swapPairCache = SpringLiteContext.getBean(SwapPairCache.class);
    }

    public static SwapTempPairManager newInstance(int chainId) {
        SwapTempPairManager swapTempPairManager = new SwapTempPairManager();
        swapTempPairManager.chainId = chainId;
        return swapTempPairManager;
    }

    public SwapPairDTO get(String address) {
        SwapPairDTO swapPairDTO = pairs.get(address);
        if (swapPairDTO == null) {
            swapPairDTO = swapPairCache.get(address);
            if (swapPairDTO == null) {
                return null;
            }
            swapPairDTO = swapPairDTO.clone();
            pairs.put(address, swapPairDTO);
        }
        return swapPairDTO;
    }

    public boolean isExist(String address) {
        return swapPairCache.isExist(address) || tempCreatePairs.contains(address);
    }

    public void add(String address) {
        tempCreatePairs.add(address);
    }
}
