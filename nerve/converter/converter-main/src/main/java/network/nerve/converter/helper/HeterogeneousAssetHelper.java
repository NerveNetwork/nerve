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
package network.nerve.converter.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;

import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-10-21
 */
@Component
public class HeterogeneousAssetHelper {

    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;

    public List<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        return heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
    }

    public HeterogeneousAssetInfo getHeterogeneousAssetInfo(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) {
        if (heterogeneousChainId == 0) {
            heterogeneousChainId = ConverterConstant.FIRST_HETEROGENEOUS_ASSET_CHAIN_ID;
        }
        return heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
    }
}
