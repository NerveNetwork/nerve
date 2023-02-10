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
                chain.getLogger().info("[异构链基本信息查询]忽略同步区块模式");
                return;
            }
            // 每隔150个区块更新一次，计算是否达到更新条件
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
                chain.getLogger().info("[异构链基本信息查询]不满足执行条件, latestHeight: {}, currentFlag: {}, flag: {}", latestHeight, currentFlag, flag);
                return;
            } while (false);
            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().debug("非虚拟银行成员，跳过异构链RPC检视的任务");
                        break;
                    }
                    chain.getLogger().info("按照高度来更新异构链RPC检视信息, 每隔150个区块统计一次, 当前网络高度: {}", latestHeight);
                    String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi"));
                    if (StringUtils.isNotBlank(result)) {
                        List<Map> list = JSONUtils.json2list(result, Map.class);
                        Map<Long, Map> map = list.stream().collect(Collectors.toMap(m -> Long.valueOf(m.get("nativeId").toString()), Function.identity()));
                        ConverterContext.HTG_RPC_CHECK_MAP = map;
                    }
                } while (false);
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            try {
                do {
                    if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                        chain.getLogger().debug("非虚拟银行成员，跳过异构链余额缓存的任务");
                        break;
                    }
                    chain.getLogger().info("按照高度来更新缓存的虚拟银行异构链网络余额, 每隔150个区块统计一次, 当前网络高度: {}", latestHeight);
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
                    // 并行查询异构链余额
                    VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager, this.logPrint, converterCoreApi);
                } while (false);
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            try {
                chain.getLogger().info("按照高度来更新缓存的资产的注册链, 每隔150个区块统计一次, 当前网络高度: {}", latestHeight);
                // 计算资产注册链
                List<Map> assetList = LedgerCall.ledgerAssetQueryAll(chain.getChainId());
                chain.getLogger().info("需要查询的资产数量: {}", assetList == null ? 0 : assetList.size());
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
            } catch (Exception e) {
                chain.getLogger().error(e.getMessage(), e);
            }

            this.logPrint++;
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    private HeterogeneousAssetInfo registerNetwork(Chain chain, int assetChainId, int assetId) {
        try {
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, 查询注册链", assetChainId, assetId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, 查询注册链", assetChainId, assetId);
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
                    } catch (Exception e) {
                        //skip it
                    }
                }
            }
            if (resultChainId == 0) {
                return null;
            }
            if (this.logPrint % 10 == 0) {
                chain.getLogger().info("Asset: {}-{}, 查询到注册链: {}", assetChainId, assetId, resultChainId);
            } else {
                chain.getLogger().debug("Asset: {}-{}, 查询到注册链: {}", assetChainId, assetId, resultChainId);
            }
            return resultAssetInfo;
        } catch (Exception e) {
            chain.getLogger().error("Asset: {}-{}, 查询注册链错误: {}", assetChainId, assetId, e.getMessage());
            return null;
        }
    }
}
