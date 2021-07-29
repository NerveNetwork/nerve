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
package network.nerve.swap.storage.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.model.po.SwapPairReservesPO;
import network.nerve.swap.storage.SwapPairReservesStorageService;
import network.nerve.swap.utils.SwapDBUtil;

/**
 * @author: PierreLuo
 * @date: 2021/5/7
 */
@Component
public class SwapPairReservesStorageServiceImpl implements SwapPairReservesStorageService {

    private final String baseArea = SwapDBConstant.DB_NAME_SWAP;
    private final String KEY_PREFIX = "PAIRR-";

    @Override
    public boolean savePairReserves(String address, SwapPairReservesPO dto) throws Exception {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        return SwapDBUtil.putModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address), dto);
    }

    @Override
    public SwapPairReservesPO getPairReserves(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        return SwapDBUtil.getModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address), SwapPairReservesPO.class);
    }

    @Override
    public boolean delelePairReserves(String address) throws Exception {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        return RocksDBService.delete(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address));
    }
}
