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

package io.nuls.base.api.provider.swap;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.swap.facade.StableAddCoinReq;
import io.nuls.base.api.provider.swap.facade.StableAddPairReq;

/**
 * @author: PierreLuo
 * @date: 2021/7/1
 */
public interface SwapService {

    /**
     * 稳定币添加币种
     * @param req
     * @return
     */
    Result<String> stableAddCoin(StableAddCoinReq req);

    /**
     * 添加稳定币交易对，用于swap路径结合
     * @param req
     * @return
     */
    Result<String> stableAddPair(StableAddPairReq req);

}
