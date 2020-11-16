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

import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;

import java.util.List;

/**
 * @date: 2020-05-29
 */
public interface HeterogeneousAssetConverterStorageService {
    /**
     * 保存异构链资产信息
     */
    int saveAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception;

    /**
     * 移除异构链资产信息
     */
    int deleteAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * 保存绑定类型的异构链资产信息
     */
    int saveBindAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception;

    /**
     * 移除绑定类型的异构链资产信息
     */
    int deleteBindAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * 是否绑定类型的异构链资产
     */
    boolean isBoundHeterogeneousAsset(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * 获取异构链资产信息集合
     */
    List<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId);

    /**
     * 获取异构链资产信息
     */
    HeterogeneousAssetInfo getHeterogeneousAssetInfo(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId);

    /**
     * 获取Nerve资产ID
     */
    NerveAssetInfo getNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId);



}
