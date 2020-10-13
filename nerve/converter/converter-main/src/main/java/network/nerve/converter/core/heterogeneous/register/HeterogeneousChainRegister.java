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
package network.nerve.converter.core.heterogeneous.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IHeterogeneousUpgrade;
import network.nerve.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import network.nerve.converter.core.heterogeneous.callback.management.HeterogeneousCallBackManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

/**
 * 异构链组件注册入口
 *
 * @author: Mimi
 * @date: 2020-02-17
 */
@Component
public class HeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousChainManager heterogeneousChainManager;
    @Autowired
    private HeterogeneousCallBackManager heterogeneousCallBackManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    private NulsLogger logger() {
        return chainManager.getChain(converterConfig.getChainId()).getLogger();
    }

    /**
     * @param chainName
     * @param HeterogeneousChainInterface Nerve核心接口实现实例
     */
    public HeterogeneousChainRegisterInfo register(int nerveChainId, int heterogeneousChainId, IHeterogeneousChainDocking heterogeneousChainInterface) throws NulsException {
        HeterogeneousChainRegisterInfo info = new HeterogeneousChainRegisterInfo();
        /**
         分配多签地址
         分配回调函数实例
         */
        HeterogeneousChainInfo chainInfo = heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId);
        if (chainInfo == null) {
            logger().error("error heterogeneousChainId: {}", heterogeneousChainId);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        // 管理异构链组件的接口实现实例
        heterogeneousDockingManager.registerHeterogeneousDocking(heterogeneousChainId, heterogeneousChainInterface);
        // 多签地址
        String multySignAddress = chainInfo.getMultySignAddress();
        IDepositTxSubmitter depositTxSubmitter = heterogeneousCallBackManager.createOrGetDepositTxSubmitter(nerveChainId, heterogeneousChainId);
        ITxConfirmedProcessor txConfirmedProcessor = heterogeneousCallBackManager.createOrGetTxConfirmedProcessor(nerveChainId, heterogeneousChainId);
        IHeterogeneousUpgrade heterogeneousUpgrade = heterogeneousCallBackManager.createOrGetHeterogeneousUpgrade(nerveChainId, heterogeneousChainId);
        info.setMultiSigAddress(multySignAddress);
        info.setDepositTxSubmitter(depositTxSubmitter);
        info.setTxConfirmedProcessor(txConfirmedProcessor);
        info.setHeterogeneousUpgrade(heterogeneousUpgrade);
        info.setConverterCoreApi(converterCoreApi);
        return info;
    }
}
