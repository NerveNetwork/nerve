/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
     * Save heterogeneous chain asset information
     */
    int saveAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception;

    /**
     * Remove heterogeneous chain asset information
     */
    int deleteAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * Save heterogeneous chain asset information for binding types
     */
    int saveBindAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception;

    /**
     * Remove heterogeneous chain asset information for binding types
     */
    int deleteBindAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * Whether to bind heterogeneous chain assets of type
     */
    boolean isBoundHeterogeneousAsset(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

    /**
     * Obtain a collection of heterogeneous chain asset information
     */
    List<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId);

    /**
     * Obtain heterogeneous chain asset information
     */
    HeterogeneousAssetInfo getHeterogeneousAssetInfo(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId);

    /**
     * obtainNerveassetID
     */
    NerveAssetInfo getNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId);

    int pauseInAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;
    int resumeInAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;
    boolean isPauseInHeterogeneousAsset(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;
    int pauseOutAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;
    int resumeOutAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;
    boolean isPauseOutHeterogeneousAsset(int heterogeneousChainId, int heterogeneousAssetId) throws Exception;

}
