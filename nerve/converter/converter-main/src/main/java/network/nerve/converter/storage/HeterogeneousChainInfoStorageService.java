/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.storage;

import network.nerve.converter.model.bo.HeterogeneousChainInfo;

import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public interface HeterogeneousChainInfoStorageService {
    /**
     * 保存异构链基本信息
     */
    int saveHeterogeneousChainInfo(int heterogeneousChainId, HeterogeneousChainInfo info) throws Exception;

    /**
     * 获取异构链基本信息
     */
    HeterogeneousChainInfo getHeterogeneousChainInfo(int heterogeneousChainId);

    /**
     * 删除异构链基本信息
     */
    void deleteHeterogeneousChainInfo(int heterogeneousChainId) throws Exception;

    /**
     * 根据异构链chainId检查是否存在这个异构链基本信息
     */
    boolean isExistHeterogeneousChainInfo(int heterogeneousChainId);

    /**
     * 获取所有异构链基本信息
     *
     * @return
     */
    List<HeterogeneousChainInfo> getAllHeterogeneousChainInfoList();

    /**
     * 查询向账本初始化异构链资产是否完成
     */
    boolean hadInit2LedgerAsset();

    /**
     * 完成向账本初始化异构链资产
     * @throws Exception
     */
    void init2LedgerAssetCompleted() throws Exception;

}
