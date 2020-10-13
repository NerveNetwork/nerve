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
package network.nerve.converter.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;

/**
 * @author: mimi
 * @date: 2020-05-29
 */
@Component
public class LedgerAssetRegisterHelper {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;

    public Boolean crossChainAssetReg(int chainId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        Integer nerveAssetId = LedgerCall.crossChainAssetReg(chainId, assetName, decimalPlace, assetSymbol, assetAddress);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        heterogeneousAssetConverterStorageService.saveAssetInfo(chainId, nerveAssetId, info);
        return true;
    }

    public Boolean deleteCrossChainAsset(int heterogeneousAssetChainId, int heterogeneousAssetId) throws Exception {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        LedgerCall.crossChainAssetDelete(nerveAssetInfo.getAssetId());
        heterogeneousAssetConverterStorageService.deleteAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    public Boolean crossChainAssetRegByExistNerveAsset(int nerveAssetChainId, int nerveAssetId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        LedgerCall.bindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetChainId, nerveAssetId);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        heterogeneousAssetConverterStorageService.saveAssetInfo(nerveAssetChainId, nerveAssetId, info);
        return true;
    }

    public Boolean deleteCrossChainAssetByExistNerveAsset(int heterogeneousAssetChainId, int heterogeneousAssetId) throws Exception {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        LedgerCall.unbindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
        heterogeneousAssetConverterStorageService.deleteAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    public HeterogeneousAssetInfo getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        return heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
    }

    public void checkMainAssetBind() throws NulsException {
        int chainId = converterConfig.getChainId();
        int assetId = converterConfig.getAssetId();
        HeterogeneousAssetInfo assetInfo = this.getHeterogeneousAssetInfo(chainId, assetId);
        if (assetInfo != null) {
            LedgerCall.bindHeterogeneousAssetReg(chainId, chainId, assetId);
        }
    }

    public NerveAssetInfo getNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        return nerveAssetInfo;
    }
}
