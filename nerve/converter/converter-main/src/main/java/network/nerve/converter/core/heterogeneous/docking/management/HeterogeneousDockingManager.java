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
package network.nerve.converter.core.heterogeneous.docking.management;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.kcs.context.KcsContext;
import network.nerve.converter.heterogeneouschain.matic.context.MaticContext;
import network.nerve.converter.heterogeneouschain.okt.context.OktContext;
import network.nerve.converter.heterogeneouschain.one.context.OneContext;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.rpc.call.AccountCall;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.converter.config.ConverterContext.*;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousDockingManager {

    /**
     * 管理每个异构链组件的接口实现实例
     */
    private Map<Integer, IHeterogeneousChainDocking> heterogeneousDockingMap = new ConcurrentHashMap<>();

    private boolean huobiCrossChainAvailable = false;
    private boolean oktCrossChainAvailable = false;
    private boolean oneCrossChainAvailable = false;
    private boolean polygonCrossChainAvailable = false;
    private boolean kucoinCrossChainAvailable = false;

    public void registerHeterogeneousDocking(int heterogeneousChainId, IHeterogeneousChainDocking docking) {
        heterogeneousDockingMap.put(heterogeneousChainId, docking);
    }

    public IHeterogeneousChainDocking getHeterogeneousDocking(int heterogeneousChainId) throws NulsException {
        // 增加HT跨链的生效高度
        /*if (LATEST_BLOCK_HEIGHT < HUOBI_CROSS_CHAIN_HEIGHT && heterogeneousChainId == HtConstant.HT_CHAIN_ID) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }*/
        IHeterogeneousChainDocking docking = heterogeneousDockingMap.get(heterogeneousChainId);
        if (docking == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }
        return docking;
    }

    public Collection<IHeterogeneousChainDocking> getAllHeterogeneousDocking() {
        Map<Integer, IHeterogeneousChainDocking> result = new HashMap<>();
        result.putAll(heterogeneousDockingMap);
        // 增加HT跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < HUOBI_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(HtContext.HTG_CHAIN_ID)) {
            result.remove(HtContext.HTG_CHAIN_ID);
        }
        // 增加OKT跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < OKT_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(OktContext.HTG_CHAIN_ID)) {
            result.remove(OktContext.HTG_CHAIN_ID);
        }
        // 增加ONE跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < ONE_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(OneContext.HTG_CHAIN_ID)) {
            result.remove(OneContext.HTG_CHAIN_ID);
        }
        // 增加POLYGON跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < POLYGON_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(MaticContext.HTG_CHAIN_ID)) {
            result.remove(MaticContext.HTG_CHAIN_ID);
        }
        // 增加KUCOIN跨链的生效高度
        if (LATEST_BLOCK_HEIGHT < KUCOIN_CROSS_CHAIN_HEIGHT && heterogeneousDockingMap.containsKey(KcsContext.HTG_CHAIN_ID)) {
            result.remove(KcsContext.HTG_CHAIN_ID);
        }
        return result.values();
    }

    public void checkAccountImportedInDocking(Chain chain, SignAccountDTO signAccountDTO) {
        if (!huobiCrossChainAvailable && LATEST_BLOCK_HEIGHT >= HUOBI_CROSS_CHAIN_HEIGHT) {
            // 向HT异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                this.registerAccount(chain, signAccountDTO, HtContext.HTG_CHAIN_ID);
                huobiCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件[HT]注册地址签名信息异常, 错误: {}", e.format());
            }
        }
        if (!oktCrossChainAvailable && LATEST_BLOCK_HEIGHT >= OKT_CROSS_CHAIN_HEIGHT) {
            // 向OKT异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                this.registerAccount(chain, signAccountDTO, OktContext.HTG_CHAIN_ID);
                oktCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件[OKT]注册地址签名信息异常, 错误: {}", e.format());
            }
        }
        if (!oneCrossChainAvailable && LATEST_BLOCK_HEIGHT >= ONE_CROSS_CHAIN_HEIGHT) {
            // 向ONE异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                this.registerAccount(chain, signAccountDTO, OneContext.HTG_CHAIN_ID);
                oneCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件[ONE]注册地址签名信息异常, 错误: {}", e.format());
            }
        }
        if (!polygonCrossChainAvailable && LATEST_BLOCK_HEIGHT >= POLYGON_CROSS_CHAIN_HEIGHT) {
            // 向MATIC异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                this.registerAccount(chain, signAccountDTO, MaticContext.HTG_CHAIN_ID);
                polygonCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件[MATIC]注册地址签名信息异常, 错误: {}", e.format());
            }
        }
        if (!kucoinCrossChainAvailable && LATEST_BLOCK_HEIGHT >= KUCOIN_CROSS_CHAIN_HEIGHT) {
            // 向KCS异构链组件,注册地址签名信息
            try {
                // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
                this.registerAccount(chain, signAccountDTO, KcsContext.HTG_CHAIN_ID);
                kucoinCrossChainAvailable = true;
            } catch (NulsException e) {
                chain.getLogger().warn("向异构链组件[KCS]注册地址签名信息异常, 错误: {}", e.format());
            }
        }
    }

    private void registerAccount(Chain chain, SignAccountDTO signAccountDTO, int htgChainId) throws NulsException {
        // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
        if (null != signAccountDTO) {
            String priKey = AccountCall.getPriKey(signAccountDTO.getAddress(), signAccountDTO.getPassword());
            // 向异构链跨链组件导入账户
            IHeterogeneousChainDocking dock = this.getHeterogeneousDocking(htgChainId);
            if (dock != null && dock.getCurrentSignAddress() == null) {
                dock.importAccountByPriKey(priKey, signAccountDTO.getPassword());
                chain.getLogger().info("[初始化]本节点是虚拟银行节点,向异构链组件[{}]注册签名账户信息..", dock.getChainSymbol());
            }
        }
    }
}
