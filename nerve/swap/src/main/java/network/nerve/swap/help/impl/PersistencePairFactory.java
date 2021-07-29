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
package network.nerve.swap.help.impl;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.impl.stable.PersistenceStablePair;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.storage.SwapStablePairBalancesStorageService;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
@Component("PersistencePairFactory")
public class PersistencePairFactory implements IPairFactory {

    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private SwapPairReservesStorageService swapPairReservesStorageService;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapStablePairBalancesStorageService swapStablePairBalancesStorageService;

    @Override
    public IPair getPair(String pairAddress) {
        SwapPairDTO pairDTO = swapPairCache.get(pairAddress);
        return new PersistencePair(pairDTO, swapPairReservesStorageService, swapPairCache);
    }

    @Override
    public IStablePair getStablePair(String pairAddress) {
        StableSwapPairDTO pairDTO = stableSwapPairCache.get(pairAddress);
        return new PersistenceStablePair(pairDTO, swapStablePairBalancesStorageService, stableSwapPairCache);
    }
}
