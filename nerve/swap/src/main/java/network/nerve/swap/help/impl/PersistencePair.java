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
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.storage.SwapPairReservesStorageService;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
public class PersistencePair extends AbstractPair {

    private SwapPairDTO swapPairDTO;
    private SwapPairReservesStorageService swapPairReservesStorageService;
    private SwapPairCache swapPairCache;

    public PersistencePair(SwapPairDTO swapPairDTO, SwapPairReservesStorageService swapPairReservesStorageService, SwapPairCache swapPairCache) {
        this.swapPairDTO = swapPairDTO;
        this.swapPairReservesStorageService = swapPairReservesStorageService;
        this.swapPairCache = swapPairCache;
    }


    @Override
    protected SwapPairDTO getSwapPairDTO() {
        return swapPairDTO;
    }

    @Override
    public void _update(BigInteger liquidityChange, BigInteger balance0, BigInteger balance1, BigInteger reserve0, BigInteger reserve1, long blockHeight, long blockTime) throws Exception {
        swapPairDTO.setTotalLP(swapPairDTO.getTotalLP().add(liquidityChange));
        swapPairDTO.setReserve0(balance0);
        swapPairDTO.setReserve1(balance1);
        // pierre reserve0, reserve1 是否需要价格预言机
        //......

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        SwapPairReservesPO po = new SwapPairReservesPO(getPair().getAddress(), balance0, balance1, swapPairDTO.getTotalLP(), blockTime, blockHeight);
        swapPairReservesStorageService.savePairReserves(pairAddress, po);
        // 更新缓存
        swapPairCache.reload(pairAddress);
    }

    @Override
    public void _rollback(BigInteger liquidityChange, BigInteger reserve0, BigInteger reserve1, long blockHeight, long blockTime) throws Exception {
        swapPairDTO.setTotalLP(swapPairDTO.getTotalLP().subtract(liquidityChange));
        swapPairDTO.setReserve0(reserve0);
        swapPairDTO.setReserve1(reserve1);
        // pierre reserve0, reserve1 是否需要价格预言机
        //......

        String pairAddress = AddressTool.getStringAddressByBytes(getPair().getAddress());
        SwapPairReservesPO po = new SwapPairReservesPO(getPair().getAddress(), reserve0, reserve1, swapPairDTO.getTotalLP(), blockTime, blockHeight);
        swapPairReservesStorageService.savePairReserves(pairAddress, po);
        // 更新缓存
        swapPairCache.reload(pairAddress);
    }
}
