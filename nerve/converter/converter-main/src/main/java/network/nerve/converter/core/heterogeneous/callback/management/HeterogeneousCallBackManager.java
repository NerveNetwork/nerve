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
package network.nerve.converter.core.heterogeneous.callback.management;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.core.heterogeneous.callback.DepositTxSubmitterImpl;
import network.nerve.converter.core.heterogeneous.callback.HeterogeneousUpgradeImpl;
import network.nerve.converter.core.heterogeneous.callback.TxConfirmedProcessorImpl;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IHeterogeneousUpgrade;
import network.nerve.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import network.nerve.converter.manager.ChainManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回调函数管理器
 *
 * @author: Mimi
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousCallBackManager {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private CallBackBeanManager callBackBeanManager;

    /**
     * 管理每个异构链组件的充值交易监听回调器
     */
    private Map<Integer, IDepositTxSubmitter> depositTxSubmitterMap = new ConcurrentHashMap<>();
    /**
     * 管理每个异构链组件的异构链交易确认回调器
     */
    private Map<Integer, ITxConfirmedProcessor> txConfirmedProcessorMap = new ConcurrentHashMap<>();
    /**
     * 管理每个异构链组件的升级回调器
     */
    private Map<Integer, IHeterogeneousUpgrade> heterogeneousUpgradeMap = new ConcurrentHashMap<>();

    public IDepositTxSubmitter createOrGetDepositTxSubmitter(int nerveChainId, int heterogeneousChainId) {
        return depositTxSubmitterMap.computeIfAbsent(heterogeneousChainId, hChainId -> new DepositTxSubmitterImpl(chainManager.getChain(nerveChainId), hChainId, callBackBeanManager));
    }

    public ITxConfirmedProcessor createOrGetTxConfirmedProcessor(int nerveChainId, int heterogeneousChainId) {
        return txConfirmedProcessorMap.computeIfAbsent(heterogeneousChainId, hChainId -> new TxConfirmedProcessorImpl(chainManager.getChain(nerveChainId), hChainId, callBackBeanManager));
    }

    public IHeterogeneousUpgrade createOrGetHeterogeneousUpgrade(int nerveChainId, int heterogeneousChainId) {
        return heterogeneousUpgradeMap.computeIfAbsent(heterogeneousChainId, hChainId -> new HeterogeneousUpgradeImpl(chainManager.getChain(nerveChainId), hChainId, callBackBeanManager));
    }

    public Collection<IDepositTxSubmitter> getAllDepositTxSubmitter() {
        return depositTxSubmitterMap.values();
    }

    public Collection<ITxConfirmedProcessor> getAllTxConfirmedProcessor() {
        return txConfirmedProcessorMap.values();
    }

    public Collection<IHeterogeneousUpgrade> getAllHeterogeneousUpgrade() {
        return heterogeneousUpgradeMap.values();
    }


}
