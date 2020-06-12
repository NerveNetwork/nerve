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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跨链资产登记
 *
 * @author: PierreLuo
 * @date: 2020-05-11
 */
@Component
public class CrossChainAssetsRegCmd extends BaseLedgerCmd {
    @Autowired
    CrossChainAssetRegMngRepository crossChainAssetRegMngRepository;

    /**
     * 跨链资产登记接口
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_REG, version = 1.0,
            description = "跨链资产登记接口")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "当前运行链ID"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID"),
            @Parameter(parameterName = "assetName", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产名称: 大、小写字母、数字、下划线（下划线不能在两端）1~20字节"),
            @Parameter(parameterName = "initNumber", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "资产初始值"),
            @Parameter(parameterName = "decimalPlace", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-18]", parameterDes = "资产最小分割位数"),
            @Parameter(parameterName = "assetSymbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产单位符号: 大、小写字母、数字、下划线（下划线不能在两端）1~20字节"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "新资产地址"),
            @Parameter(parameterName = "assetType", requestType = @TypeDescriptor(value = short.class), parameterDes = "资产类型 3-平行链资产")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "成功true,失败false")
            })
    )
    public Response crossChainAssetReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(3);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            LoggerUtil.COMMON_LOG.debug("[register] cross chain asset params={}", JSONUtils.obj2json(params));
            params.put("chainId", params.get("assetChainId"));
            LedgerAsset asset = new LedgerAsset();
            asset.map2pojo(params, Short.parseShort(params.get("assetType").toString()));
            crossChainAssetRegMngRepository.saveCrossChainAsset(chainId, asset);
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            rtMap.put("value", false);
        }
        return success(rtMap);
    }

    /**
     * 跨链资产列表登记接口
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_LIST_REG, version = 1.0,
            description = "跨链资产列表登记接口")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "当前运行链ID"),
            @Parameter(parameterName = "assetType", requestType = @TypeDescriptor(value = short.class), parameterDes = "资产类型 3-平行链资产"),
            @Parameter(parameterName = "crossChainAssetList", parameterDes = "跨链资产列表", requestType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "资产链ID"),
                    @Key(name = "assetId", valueType = int.class, description = "资产ID"),
                    @Key(name = "assetName", valueType = String.class, description = "资产名称: 大、小写字母、数字、下划线（下划线不能在两端）1~20字节"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "资产初始值"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "资产最小分割位数"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "资产单位符号: 大、小写字母、数字、下划线（下划线不能在两端）1~20字节"),
                    @Key(name = "address", valueType = String.class, description = "新资产地址"),
                    @Key(name = "usable", valueType = boolean.class, description = "资产是否可用")
            })),
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "成功true,失败false")
            })
    )
    public Response crossChainAssetListReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(3);
        try {
            LoggerUtil.COMMON_LOG.debug("[register] cross chain asset list");
            int chainId = Integer.parseInt(params.get("chainId").toString());
            short assetType = Short.parseShort(params.get("assetType").toString());
            List<LedgerAsset> saveAssetList = new ArrayList<>();
            List<String> deleteAssetKeyList = new ArrayList<>();
            List<Map<String, Object>> list = (List<Map<String, Object>>) params.get("crossChainAssetList");
            boolean usable;
            for(Map<String, Object> assetMap : list) {
                usable = (boolean) assetMap.get("usable");
                if (usable) {
                    LedgerAsset asset = new LedgerAsset();
                    assetMap.put("chainId", assetMap.get("assetChainId"));
                    asset.map2pojo(assetMap, assetType);
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
     * 跨链资产合约移除接口
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CROSS_CHAIN_ASSET_DELETE, version = 1.0,
            description = "跨链资产合约移除接口")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "链Id"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "成功true,失败false")
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
            description = "跨链资产查询")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "链Id"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "资产链id"),
                    @Key(name = "assetId", valueType = int.class, description = "资产id"),
                    @Key(name = "assetType", valueType = int.class, description = "资产类型"),
                    @Key(name = "assetAddress", valueType = String.class, description = "资产地址"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "资产初始化值"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "小数点分割位数"),
                    @Key(name = "assetName", valueType = String.class, description = "资产名"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "资产符号")
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
     * 查看所有跨链登记资产信息
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_GET_ALL_CROSS_CHAIN_ASSET, version = 1.0,
            description = "查看所有跨链登记资产信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "运行链Id,取值区间[1-65535]")
    })
    @ResponseData(name = "返回值", description = "返回一个list对象",
            responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetChainId", valueType = int.class, description = "资产链id"),
                    @Key(name = "assetId", valueType = int.class, description = "资产id"),
                    @Key(name = "assetType", valueType = int.class, description = "资产类型"),
                    @Key(name = "assetAddress", valueType = String.class, description = "资产地址"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "资产初始化值"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "小数点分割位数"),
                    @Key(name = "assetName", valueType = String.class, description = "资产名"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "资产符号")
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
