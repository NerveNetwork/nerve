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
package nerve.network.converter.core.heterogeneous.register;

import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.context.HeterogeneousChainManager;
import nerve.network.converter.core.heterogeneous.callback.interfaces.IDepositTxSubmitter;
import nerve.network.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import nerve.network.converter.core.heterogeneous.callback.management.HeterogeneousCallBackManager;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.HeterogeneousChainInfo;
import nerve.network.converter.model.bo.HeterogeneousChainRegisterInfo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

/**
 * 异构链组件注册入口
 *
 * @author: Chino
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
        // 多签地址
        String multySignAddress = chainInfo.getMultySignAddress();
        IDepositTxSubmitter depositTxSubmitter = heterogeneousCallBackManager.createOrGetDepositTxSubmitter(nerveChainId, heterogeneousChainId);
        ITxConfirmedProcessor txConfirmedProcessor = heterogeneousCallBackManager.createOrGetTxConfirmedProcessor(nerveChainId, heterogeneousChainId);
        info.setMultiSigAddress(multySignAddress);
        info.setDepositTxSubmitter(depositTxSubmitter);
        info.setTxConfirmedProcessor(txConfirmedProcessor);
        // 管理异构链组件的接口实现实例
        heterogeneousDockingManager.registerHeterogeneousDocking(heterogeneousChainId, heterogeneousChainInterface);
        return info;
    }
}
