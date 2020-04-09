/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.core.business.impl;

import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.core.business.HeterogeneousService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;

/**
 * @author: Chino
 * @date: 2020/3/18
 */
@Component
public class HeterogeneousServiceImpl implements HeterogeneousService {


    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    /**
     * 判断是否需要组装当前网络的主资产补贴异构链交易手续费
     * 异构链是合约类型,并且提现资产不是异构链主资产,才收取当前网络主资产作为手续费补贴
     * @param heterogeneousChainId
     * @param heterogeneousAssetId
     * @return
     */
    @Override
    public boolean isAssembleCurrentAssetFee(int heterogeneousChainId, int heterogeneousAssetId) throws NulsException {
        IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        return heterogeneousDocking.isSupportContractAssetByCurrentChain()
                && heterogeneousAssetId != ConverterConstant.ALL_MAIN_ASSET_ID;
    }
}
