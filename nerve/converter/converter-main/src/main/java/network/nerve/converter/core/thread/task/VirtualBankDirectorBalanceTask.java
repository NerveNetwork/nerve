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

package network.nerve.converter.core.thread.task;

import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.LatestBasicBlock;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.HeterogeneousAddressDTO;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2021-01-12
 */
public class VirtualBankDirectorBalanceTask implements Runnable {

    private Chain chain;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private ConverterCoreApi converterCoreApi;
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    private long flag;
    private int logPrint;

    public VirtualBankDirectorBalanceTask(Chain chain) {
        this.chain = chain;
        this.heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
        this.converterCoreApi = SpringLiteContext.getBean(ConverterCoreApi.class);
        this.heterogeneousAssetHelper = SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
        this.flag = -1;
    }

    @Override
    public void run() {
        try {
            if (!converterCoreApi.isRunning()) {
                chain.getLogger().info("[Basic information query of heterogeneous chains]Ignoring synchronous block mode");
                return;
            }
            // every other150Update each block once, calculate if the update conditions are met
            LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
            long latestHeight = latestBasicBlock.getHeight();
            long currentFlag = latestHeight / 150;
            do {
                if (flag == -1) {
                    flag = currentFlag;
                    break;
                }
                if (currentFlag > flag) {
                    flag = currentFlag;
                    break;
                }
                chain.getLogger().info("[Basic information query of heterogeneous chains] Not meeting the execution conditions, latestHeight: {}, currentFlag: {}, flag: {}", latestHeight, currentFlag, flag);
                return;
            } while (false);
            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().info("Non virtual bank members, skipping heterogeneous chains RPC The task of inspection");
                        break;
                    }
                    chain.getLogger().info("Update heterogeneous chains by height RPC Viewing Information, every other 150 Count each block once, Current network height: {}", latestHeight);
                    String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi"));
                    if (StringUtils.isNotBlank(result)) {
                        List<Map> list = JSONUtils.json2list(result, Map.class);
                        Map<Long, Map> map = list.stream().collect(Collectors.toMap(m -> Long.valueOf(m.get("nativeId").toString()), Function.identity()));
                        ConverterContext.HTG_RPC_CHECK_MAP = map;
                    }
                } while (false);
            } catch (Throwable e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            /*try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().info("Non virtual bank members, skip the task of heterogeneous chain balance caching");
                        break;
                    }
                    chain.getLogger().info("Update cached virtual bank heterogeneous chain network balances by height, every other 150 Count each block once, Current network height: {}", latestHeight);
                    Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
                    List<VirtualBankDirectorDTO> list = new ArrayList<>();
                    for (VirtualBankDirector director : mapVirtualBank.values()) {
                        VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                        for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                            if (chain.getChainId() == 5 && addr.getChainId() == 101) {
                                continue;
                            }
                            IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                            String chainSymbol = heterogeneousDocking.getChainSymbol();
                            addr.setSymbol(chainSymbol);
                        }
                        list.add(directorDTO);
                    }
                    ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST = list;
                    // Parallel query of heterogeneous chain balances
                    VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager, this.logPrint, converterCoreApi);
                } while (false);
            } catch (Throwable e) {
                chain.getLogger().error(e.getMessage(), e);
            }*/
            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().info("Non virtual bank members, skip the task of heterogeneous chain balance caching");
                        break;
                    }
                    chain.getLogger().info("Update cached virtual bank heterogeneous chain network balances by height, every other150Count each block once, Current network height: {}", latestHeight);
                    Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
                    List<VirtualBankDirectorDTO> list;
                    if (ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST == null || ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.isEmpty()) {
                        list = new ArrayList<>();
                        for (VirtualBankDirector director : mapVirtualBank.values()) {
                            VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                            for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                                if (chain.getChainId() == 5 && addr.getChainId() == 101) {
                                    continue;
                                }
                                IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                                String chainSymbol = heterogeneousDocking.getChainSymbol();
                                addr.setSymbol(chainSymbol);
                            }
                            list.add(directorDTO);
                        }
                        ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST = list;
                    } else {
                        Map<String, VirtualBankDirectorDTO> directorDTOMap = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.stream().collect(Collectors.toMap(VirtualBankDirectorDTO::getSignAddress, Function.identity()));
                        for (VirtualBankDirector director : mapVirtualBank.values()) {
                            if (!directorDTOMap.containsKey(director.getSignAddress())) {
                                VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                                for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                                    if (chain.getChainId() == 5 && addr.getChainId() == 101) {
                                        continue;
                                    }
                                    IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                                    String chainSymbol = heterogeneousDocking.getChainSymbol();
                                    addr.setSymbol(chainSymbol);
                                }
                                directorDTOMap.put(director.getSignAddress(), directorDTO);
                            }
                        }
                        for (VirtualBankDirectorDTO dto : ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST) {
                            if (!mapVirtualBank.containsKey(dto.getSignAddress())) {
                                directorDTOMap.remove(dto.getSignAddress());
                            }
                        }
                        list = new ArrayList<>(directorDTOMap.values());
                    }

                    // Parallel query of heterogeneous chain balances
                    VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager, this.logPrint, converterCoreApi);
                } while (false);
            } catch (Throwable e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            try {
                chain.getLogger().info("Update the registration chain of cached assets by height, every other 150 Count each block once, Current network height: {}", latestHeight);
                // Calculate asset registration chain
                List<Map> assetList = LedgerCall.ledgerAssetQueryAll(chain.getChainId());
                chain.getLogger().info("Number of assets to be queried: {}", assetList == null ? 0 : assetList.size());
                if (!assetList.isEmpty()) {
                    for (Map asset : assetList) {
                        Integer assetChainId = Integer.parseInt(asset.get("assetChainId").toString());
                        Integer assetId = Integer.parseInt(asset.get("assetId").toString());
                        HeterogeneousAssetInfo info = this.registerNetwork(chain, assetChainId, assetId);
                        if (info != null) {
                            ConverterContext.assetRegisterNetwork.put(assetChainId + "_" + assetId, info);
                        }
                    }
                }
            } catch (Throwable e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            this.logPrint++;
        } catch (Throwable e) {
            chain.getLogger().error(e);
        }
    }

    private HeterogeneousAssetInfo registerNetwork(Chain chain, int assetChainId, int assetId) {
        try {
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, Query registration chain", assetChainId, assetId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, Query registration chain", assetChainId, assetId);
            }
            HeterogeneousAssetInfo resultAssetInfo = null;
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(assetChainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return null;
            }
            int resultChainId = 0;
            for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                if (StringUtils.isBlank(assetInfo.getContractAddress())) {
                    resultChainId = assetInfo.getChainId();
                    resultAssetInfo = assetInfo;
                    break;
                }
            }
            if (resultChainId == 0) {
                for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                    if (!converterCoreApi.checkNetworkRunning(assetInfo.getChainId())) {
                        return null;
                    }
                    if (chain.getChainId() == 5 && assetInfo.getChainId() == 101) {
                        return null;
                    }
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
                    if (docking == null) {
                        return null;
                    }
                    try {
                        if (!docking.isMinterERC20(assetInfo.getContractAddress())) {
                            resultChainId = docking.getChainId();
                            resultAssetInfo = assetInfo;
                            break;
                        }
                    } catch (Throwable e) {
                        //skip it
                    }
                }
            }
            if (resultChainId == 0) {
                return null;
            }
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, Found registration chain: {}", assetChainId, assetId, resultChainId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, Found registration chain: {}", assetChainId, assetId, resultChainId);
            }
            return resultAssetInfo;
        } catch (Throwable e) {
            chain.getLogger().error("Asset: {}-{}, Query registration chain error: {}", assetChainId, assetId, e.getMessage());
            return null;
        }
    }
}
