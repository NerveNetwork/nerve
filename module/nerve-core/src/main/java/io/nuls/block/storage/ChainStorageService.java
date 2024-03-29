/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.storage;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.util.Deque;
import java.util.List;

/**
 * Chain storage services
 *
 * @author captain
 * @version 1.0
 * @date 18-11-14 morning10:08
 */
public interface ChainStorageService {

    /**
     * Store a chain
     *
     * @param chainId chainId/chain id
     * @param blocks
     * @return
     * @throws Exception
     */
    boolean save(int chainId, List<Block> blocks);

    /**
     * Store a block
     *
     * @param chainId chainId/chain id
     * @param block
     * @return
     * @throws Exception
     */
    boolean save(int chainId, Block block);

    /**
     * Query a block
     *
     * @param chainId chainId/chain id
     * @param hash
     * @return
     * @throws NulsException
     */
    Block query(int chainId, NulsHash hash);

    /**
     * Query a chain
     *
     * @param chainId chainId/chain id
     * @param hashList
     * @return
     * @throws NulsException
     */
    List<Block> query(int chainId, Deque<NulsHash> hashList);

    /**
     * Remove a chain
     *
     * @param chainId chainId/chain id
     * @param hashList
     * @return
     * @throws Exception
     */
    boolean remove(int chainId, Deque<NulsHash> hashList);

    /**
     * Remove a block
     *
     * @param chainId chainId/chain id
     * @param hash
     * @return
     * @throws Exception
     */
    boolean remove(int chainId, NulsHash hash);

    /**
     * Destroy Chain Storage
     *
     * @param chainId chainId/chain id
     * @return
     * @throws Exception
     */
    boolean destroy(int chainId);

}
