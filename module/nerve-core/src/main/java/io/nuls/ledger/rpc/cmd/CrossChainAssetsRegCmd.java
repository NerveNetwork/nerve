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

import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.ledger.constant.CmdConstant;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.po.LedgerAsset;
import io.nuls.ledger.storage.CrossChainAssetRegMngRepository;
import io.nuls.ledger.utils.LoggerUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross chain asset registration
 *
 * @author: PierreLuo
 * @date: 2020-05-11
 */
@Component
@NerveCoreCmd(module = ModuleE.LG)
public class CrossChainAssetsRegCmd extends BaseLedgerCmd {
    @Autowired
    CrossChainAssetRegMngRepository crossChainAssetRegMngRepository;

    /**
     * Cross chain asset registration interface
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_REG, version = 1.0,
            description = "Cross chain asset registration interface")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Current running chainID"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID"),
            @Parameter(parameterName = "assetName", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset Name: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "initNumber", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Initial value of assets"),
            @Parameter(parameterName = "decimalPlace", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-18]", parameterDes = "The minimum number of split digits for assets"),
            @Parameter(parameterName = "assetSymbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset unit symbol: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "New asset address"),
            @Parameter(parameterName = "assetType", requestType = @TypeDescriptor(value = short.class), parameterDes = "Asset type 3-Parallel chain assets")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "successtrue,failfalse")
            })
    )
    public Response crossChainAssetReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(3);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            LoggerUtil.COMMON_LOG.debug("[register] cross chain asset params={}", JSONUtils.obj2json(params));
            params.put("chainId", assetChainId);
            LedgerAsset asset = new LedgerAsset();
            asset.map2pojo(params, Short.parseShort(params.get("assetType").toString()));
            // Check if this asset has been registered in the ledger
            if (asset.getAssetId() != 0) {
                LedgerAsset checkAsset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, asset.getAssetId());
                // Ensure that asset types are not covered when they exist
                if (checkAsset != null) {
                    asset.setAssetType(checkAsset.getAssetType());
                }
            }
            crossChainAssetRegMngRepository.saveCrossChainAsset(chainId, asset);
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            rtMap.put("value", false);
        }
        return success(rtMap);
    }

    /**
     * Cross chain asset list registration interface
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_LIST_REG, version = 1.0,
            description = "Cross chain asset list registration interface")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Current running chainID"),
            @Parameter(parameterName = "assetType", requestType = @TypeDescriptor(value = short.class), parameterDes = "Asset type 3-Parallel chain assets"),
            @Parameter(parameterName = "crossChainAssetList", parameterDes = "Cross chain asset list", requestType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "Asset ChainID"),
                    @Key(name = "assetId", valueType = int.class, description = "assetID"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Initial value of assets"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "The minimum number of split digits for assets"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset unit symbol: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
                    @Key(name = "address", valueType = String.class, description = "New asset address"),
                    @Key(name = "usable", valueType = boolean.class, description = "Is the asset available")
            })),
            @Parameter(parameterName = "height", requestType = @TypeDescriptor(value = long.class), parameterDes = "Current block height"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "successtrue,failfalse")
            })
    )
    public Response crossChainAssetListReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(3);
        try {
            LoggerUtil.COMMON_LOG.debug("[register] cross chain asset list");
            int chainId = Integer.parseInt(params.get("chainId").toString());
            long height = Long.parseLong(params.get("height").toString());
            short assetType = Short.parseShort(params.get("assetType").toString());
            List<LedgerAsset> saveAssetList = new ArrayList<>();
            List<String> deleteAssetKeyList = new ArrayList<>();
            List<Map<String, Object>> list = (List<Map<String, Object>>) params.get("crossChainAssetList");
            Collection<Map<String, Object>> dataList;
//            if (height >= LedgerConstant.PROTOCOL_1_32_0) {
            if (height >= 55644100L) {
                Map<String, Map<String, Object>> dataMap = new HashMap<>();
                for (Map<String, Object> assetMap : list) {
                    String key = assetMap.get("assetChainId").toString() + "-" + assetMap.get("assetId").toString();
                    dataMap.put(key, assetMap);
                }
                dataList = dataMap.values();
            } else {
                dataList = list;
            }
            boolean usable;
            for (Map<String, Object> assetMap : dataList) {
                int assetChainId = Integer.parseInt(assetMap.get("assetChainId").toString());
                // When an in chain asset is registered as a cross chain asset, the cross chain module will register the asset as a cross chain asset and notify the ledger. At this time, the ledger should be ignored and the asset should not be modified as a cross chain asset
                if (assetType == 3 && chainId == assetChainId) {
                    continue;
                }
                usable = (boolean) assetMap.get("usable");
                if (usable) {
                    LedgerAsset asset = new LedgerAsset();
                    assetMap.put("chainId", assetMap.get("assetChainId"));
                    asset.map2pojo(assetMap, assetType);
                    // Check if this asset has been registered in the ledger
                    if (asset.getAssetId() != 0) {
                        LedgerAsset checkAsset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, asset.getAssetId());
                        // Ensure that asset types are not covered when they exist
                        if (checkAsset != null) {
                            asset.setAssetType(checkAsset.getAssetType());
                        }
                    }
                    saveAssetList.add(asset);
                } else {
                    deleteAssetKeyList.add(assetMap.get("assetChainId").toString() + LedgerConstant.DOWN_LINE + assetMap.get("assetId").toString());
                }
            }
            crossChainAssetRegMngRepository.batchOperationCrossChainAssetList(chainId, saveAssetList, deleteAssetKeyList);
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            rtMap.put("value", false);
        }
        return success(rtMap);
    }

    /**
     * Cross chain asset contract removal interface
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_DELETE, version = 1.0,
            description = "Cross chain asset contract removal interface")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "chainId"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "successtrue,failfalse")
            })
    )
    public Response deleteCrossChainAsset(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            crossChainAssetRegMngRepository.deleteCrossChainAsset(chainId, assetChainId, assetId);
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            rtMap.put("value", false);
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_GET_CROSS_CHAIN_ASSET, version = 1.0,
            description = "Cross chain asset query")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "chainId"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "Asset Chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type"),
                    @Key(name = "assetAddress", valueType = String.class, description = "Asset address"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols")
            })
    )
    public Response getCrossChainAsset(Map params) {
        Map<String, Object> rtMap;
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            LedgerAsset asset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, assetId);
            rtMap = asset.toMap();
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * View all cross chain registered asset information
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_GET_ALL_CROSS_CHAIN_ASSET, version = 1.0,
            description = "View all cross chain registered asset information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Run ChainId,Value range[1-65535]")
    })
    @ResponseData(name = "Return value", description = "Return alistobject",
            responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "Asset Chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type"),
                    @Key(name = "assetAddress", valueType = String.class, description = "Asset address"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols")
            })
    )
    public Response getAllCrossChainAssets(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            List<LedgerAsset> ledgerAssetList = crossChainAssetRegMngRepository.getAllCrossChainAssets(chainId);
            List<Map<String, Object>> assets = ledgerAssetList.stream().map(asset -> asset.toMap()).collect(Collectors.toList());
            rtMap.put("assets", assets);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }
}
