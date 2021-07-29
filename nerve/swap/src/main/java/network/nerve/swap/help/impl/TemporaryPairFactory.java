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

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.impl.stable.TemporaryStablePair;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.SwapTempPairManager;
import network.nerve.swap.manager.stable.StableSwapTempPairManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.BatchInfo;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
@Component("TemporaryPairFactory")
public class TemporaryPairFactory implements IPairFactory {

    @Autowired
    private ChainManager chainManager;
    
    @Override
    public IPair getPair(String pairAddress) {
        int chainId = AddressTool.getChainIdByAddress(pairAddress);
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        SwapTempPairManager tempPairManager;
        if (batchInfo == null || (tempPairManager = batchInfo.getSwapTempPairManager()) == null) {
            return null;
        }
        SwapPairDTO swapPairDTO = tempPairManager.get(pairAddress);
        if (swapPairDTO == null) {
            return null;
        }
        return new TemporaryPair(swapPairDTO);
    }

    @Override
    public IStablePair getStablePair(String pairAddress) {
        int chainId = AddressTool.getChainIdByAddress(pairAddress);
        Chain chain = chainManager.getChain(chainId);
        BatchInfo batchInfo = chain.getBatchInfo();
        StableSwapTempPairManager stableSwapTempPairManager;
        if (batchInfo == null || (stableSwapTempPairManager = batchInfo.getStableSwapTempPairManager()) == null) {
            return null;
        }
        StableSwapPairDTO stableSwapPairDTO = stableSwapTempPairManager.get(pairAddress);
        if (stableSwapPairDTO == null) {
            return null;
        }
        return new TemporaryStablePair(stableSwapPairDTO);
    }
}
