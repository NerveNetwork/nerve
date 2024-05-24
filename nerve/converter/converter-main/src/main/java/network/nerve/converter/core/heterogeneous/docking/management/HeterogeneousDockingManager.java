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
package network.nerve.converter.core.heterogeneous.docking.management;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.rpc.call.AccountCall;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousDockingManager {

    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    /**
     * Manage interface implementation instances for each heterogeneous chain component
     */
    private Map<Integer, IHeterogeneousChainDocking> heterogeneousDockingMap = new ConcurrentHashMap<>();

    public void registerHeterogeneousDocking(int heterogeneousChainId, IHeterogeneousChainDocking docking) {
        heterogeneousDockingMap.put(heterogeneousChainId, docking);
    }

    public boolean existHeterogeneousDocking(int heterogeneousChainId) {
        return heterogeneousDockingMap.containsKey(heterogeneousChainId);
    }

    public IHeterogeneousChainDocking removeHeterogeneousDocking(int heterogeneousChainId) {
        return heterogeneousDockingMap.remove(heterogeneousChainId);
    }

    public IHeterogeneousChainDocking getHeterogeneousDocking(int heterogeneousChainId) throws NulsException {
        IHeterogeneousChainDocking docking = heterogeneousDockingMap.get(heterogeneousChainId);
        if (docking == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR, String.format("error heterogeneousChainId: %s", heterogeneousChainId));
        }
        return docking;
    }

    public IHeterogeneousChainDocking getHeterogeneousDockingSmoothly(int heterogeneousChainId) throws NulsException {
        IHeterogeneousChainDocking docking = heterogeneousDockingMap.get(heterogeneousChainId);
        return docking;
    }

    // Load the effective protocol data for adding new heterogeneous chains
    private Map<Integer, DockingProtocolInfo> protocolEffectiveInformation = new LinkedHashMap<>();

    public void addProtocol(Integer heterogeneousChainId, Long effectiveHeight, String symbol) {
        protocolEffectiveInformation.put(heterogeneousChainId, new DockingProtocolInfo(effectiveHeight, false, symbol));
    }

    public Collection<IHeterogeneousChainDocking> getAllHeterogeneousDocking() {
        Map<Integer, IHeterogeneousChainDocking> result = new HashMap<>();
        result.putAll(heterogeneousDockingMap);

        protocolEffectiveInformation.entrySet().stream()
                .filter(e -> !heterogeneousChainInfoStorageService.hadClosed(e.getKey()))
                .forEach(e -> {
            long crossChainHeight = e.getValue().getEffectiveHeight();
            int htgChainId = e.getKey();
            if (LATEST_BLOCK_HEIGHT < crossChainHeight && heterogeneousDockingMap.containsKey(htgChainId)) {
                result.remove(htgChainId);
            }
        });
        return result.values();
    }

    public void checkAccountImportedInDocking(Chain chain, SignAccountDTO signAccountDTO) {
        protocolEffectiveInformation.entrySet().forEach(e -> {
            int htgChainId = e.getKey();
            DockingProtocolInfo protocolInfo = e.getValue();
            long crossChainHeight = protocolInfo.getEffectiveHeight();
            boolean crossChainAvailable = protocolInfo.isCrossChainAvailable();
            if (!crossChainAvailable && LATEST_BLOCK_HEIGHT >= crossChainHeight) {
                // towardsHTGHeterogeneous Chain Components,Registration address signature information
                this.registerAccount(protocolInfo.getSymbol(), chain, signAccountDTO, htgChainId);
                protocolInfo.setCrossChainAvailable(true);
            }
        });
    }

    private void registerAccount(String symbol, Chain chain, SignAccountDTO signAccountDTO, int htgChainId) {
        if (symbol == null)
            return;
        try {
            // If this node is a consensus node, And if it is a virtual bank member, registration will be executed
            if (null != signAccountDTO) {
                String priKey = AccountCall.getPriKey(signAccountDTO.getAddress(), signAccountDTO.getPassword());
                // Import accounts to heterogeneous chain cross chain components
                IHeterogeneousChainDocking dock = this.getHeterogeneousDocking(htgChainId);
                if (dock != null && dock.getCurrentSignAddress() == null) {
                    dock.importAccountByPriKey(priKey, signAccountDTO.getPassword());
                    chain.getLogger().info("[initialization]This node is a virtual banking node,To heterogeneous chain components[{}]Register signature account information..", dock.getChainSymbol());
                }
            }
        } catch (NulsException e) {
            chain.getLogger().warn("To heterogeneous chain components[{}]Abnormal signature information of registered address, error: {}", symbol, e.format());
        }
    }
}
