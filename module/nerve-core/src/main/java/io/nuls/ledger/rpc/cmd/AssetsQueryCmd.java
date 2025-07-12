/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.rpc.cmd;

import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.ledger.constant.CmdConstant;
import io.nuls.ledger.manager.LedgerChainManager;
import io.nuls.ledger.model.po.LedgerAsset;
import io.nuls.ledger.storage.AssetRegMngRepository;
import io.nuls.ledger.storage.CrossChainAssetRegMngRepository;
import io.nuls.ledger.utils.LoggerUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross chain asset registration
 *
 * @author: PierreLuo
 * @date: 2020-05-11
 */
@Component
@NerveCoreCmd(module = ModuleE.LG)
public class AssetsQueryCmd extends BaseLedgerCmd {
    @Autowired
    NerveCoreConfig ledgerConfig;
    @Autowired
    CrossChainAssetRegMngRepository crossChainAssetRegMngRepository;
    @Autowired
    AssetRegMngRepository assetRegMngRepository;
    @Autowired
    LedgerChainManager ledgerChainManager;


    @CmdAnnotation(cmd = CmdConstant.CMD_GET_ASSET, version = 1.0,
            description = "Asset inquiry")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "chainId"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "Asset Chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type [1-On chain ordinary assets 2-On chain contract assets 3-Parallel chain assets 4-Heterogeneous chain assets 5-On chain ordinary assets bound to heterogeneous chain assets 6-Parallel chain assets bound to heterogeneous chain assets 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets]"),
                    @Key(name = "assetAddress", valueType = String.class, description = "Asset address"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols")
            })
    )
    public Response getAsset(Map params) {
        Map<String, Object> rtMap;
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            LedgerAsset asset;
            // Obtain registered in chain assets
            if(chainId == assetChainId) {
                if(assetId == ledgerConfig.getAssetId()) {
                    rtMap = ledgerChainManager.getLocalChainDefaultAsset();
                    return success(rtMap);
                }
                asset = assetRegMngRepository.getLedgerAssetByAssetId(chainId, assetId);
            } else {
                // Obtain registered cross chain assets
                asset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, assetId);
            }
            ChainContext context = ContextManager.getContext(chainId);
            if (context == null) {
                return failed("error chain ID");
            }
            if ((chainId == 9 && context.getLatestHeight() >= 64024518) || (chainId == 5 && context.getLatestHeight() >= 49708120)) {
                if ((assetChainId == 9 && assetId == 160) || (assetChainId == 5 && assetId == 34)) {
                    asset.setSymbol("POL");
                }
            } else
            if ((chainId == 9 && context.getLatestHeight() >= 66566655) || (chainId == 5 && context.getLatestHeight() >= 52289715)) {
                if ((assetChainId == 9 && assetId == 448) || (assetChainId == 5 && assetId == 118)) {
                    asset.setSymbol("KAIA");
                }
            } else
            if ((chainId == 9 && context.getLatestHeight() >= 75365514) || (chainId == 5 && context.getLatestHeight() >= 58629654)) {
                if ((assetChainId == 9 && assetId == 692) || (assetChainId == 5 && assetId == 148)) {
                    asset.setSymbol("A");
                }
            }
            rtMap = asset.toMap();
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * View all registered asset information
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_GET_ALL_ASSET, version = 1.0,
            description = "View all registered asset information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Run ChainId,Value range[1-65535]")
    })
    @ResponseData(name = "Return value", description = "Return alistobject",
            responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "Asset Chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type [1-On chain ordinary assets 2-On chain contract assets 3-Parallel chain assets 4-Heterogeneous chain assets 5-On chain ordinary assets bound to heterogeneous chain assets 6-Parallel chain assets bound to heterogeneous chain assets 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets]"),
                    @Key(name = "assetAddress", valueType = String.class, description = "Asset address"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols")
            })
    )
    public Response getAllCrossChainAssets(Map params) {
        Map<String, Object> rtMap = new HashMap<>(2);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            // Obtain all registered in chain assets
            List<LedgerAsset> localAssetList = assetRegMngRepository.getAllRegLedgerAssets(chainId);
            List<Map<String, Object>> localAssets = localAssetList.stream().map(asset -> asset.toMap()).collect(Collectors.toList());
            localAssets.add(ledgerChainManager.getLocalChainDefaultAsset());
            // Obtain all registered cross chain assets
            List<LedgerAsset> ledgerAssetList = crossChainAssetRegMngRepository.getAllCrossChainAssets(chainId);
            List<Map<String, Object>> assets = ledgerAssetList.stream().map(asset -> asset.toMap()).collect(Collectors.toList());
            // Merge Collection
            assets.addAll(localAssets);
            rtMap.put("assets", assets);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }


}
