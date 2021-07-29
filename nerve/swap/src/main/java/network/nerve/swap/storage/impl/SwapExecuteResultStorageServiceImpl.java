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

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

/**
 * @author: PierreLuo
 * @date: 2021/5/7
 */
@Component
public class SwapExecuteResultStorageServiceImpl implements SwapExecuteResultStorageService {

    private final String baseArea = SwapDBConstant.DB_NAME_SWAP;
    private final String KEY_PREFIX = "RESULT-";

    @Override
    public boolean save(int chainId, NulsHash hash, SwapResult result) throws Exception {
        if (hash == null || result == null) {
            return false;
        }
        return SwapDBUtil.putModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + hash.toHex()), result);
    }

    @Override
    public SwapResult getResult(int chainId, NulsHash hash) {
        if (hash == null) {
            return null;
        }
        return SwapDBUtil.getModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + hash.toHex()), SwapResult.class);
    }

    @Override
    public boolean delete(int chainId, NulsHash hash) throws Exception {
        if (hash == null) {
            return false;
        }
        RocksDBService.delete(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + hash.toHex()));
        return true;
    }
}
