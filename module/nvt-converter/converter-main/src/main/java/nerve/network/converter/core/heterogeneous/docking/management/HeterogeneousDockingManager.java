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
package nerve.network.converter.core.heterogeneous.docking.management;

import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Chino
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousDockingManager {

    /**
     * 管理每个异构链组件的接口实现实例
     */
    private Map<Integer, IHeterogeneousChainDocking> heterogeneousDockingMap = new ConcurrentHashMap<>();

    public void registerHeterogeneousDocking(int heterogeneousChainId, IHeterogeneousChainDocking docking) {
        heterogeneousDockingMap.put(heterogeneousChainId, docking);
    }

    public IHeterogeneousChainDocking getHeterogeneousDocking(int heterogeneousChainId) throws NulsException {
        IHeterogeneousChainDocking docking = heterogeneousDockingMap.get(heterogeneousChainId);
        if (docking == null) {
            throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }
        return docking;
    }

    public Collection<IHeterogeneousChainDocking> getAllHeterogeneousDocking() {
        return heterogeneousDockingMap.values();
    }
}
