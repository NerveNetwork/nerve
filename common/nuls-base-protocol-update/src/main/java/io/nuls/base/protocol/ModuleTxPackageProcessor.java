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

package io.nuls.base.protocol;

import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;

import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2020/4/3
 */
public interface ModuleTxPackageProcessor {

    /**
     * 交易模块打包时, 如果对应交易注册时 packProduce标志是true,
     * 会把该模块下所有该标志位true的交易, 一起作为参数调用接口进行
     * 例如内部交易生成等处理, 如dex
     * 返回新产生的交易
     * @param chainId
     * @param txs
     * @param process 0:表示打包时调用 1:表示验证区块时调用
     * @param height 区块高度
     * @param blockTime 区块时间
     *
     * @return Map 返回两个列表 1.key:newlyList 新生成的交易, 2.key: rmHashList 需要删除的原始交易hash
     */
    default Map<String, List<String>> packProduce(int chainId, List<Transaction> txs, int process, long height, long blockTime) throws NulsException {
        return null;
    }

    /**
     * 模块code
     * @return
     */
    String getModuleCode();
}
