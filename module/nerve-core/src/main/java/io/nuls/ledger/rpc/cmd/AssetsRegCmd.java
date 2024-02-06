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
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.ledger.constant.CmdConstant;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.constant.LedgerErrorCode;
import io.nuls.ledger.manager.LedgerChainManager;
import io.nuls.ledger.model.po.LedgerAsset;
import io.nuls.ledger.service.AssetRegMngService;
import io.nuls.ledger.storage.AssetRegMngRepository;
import io.nuls.ledger.storage.CrossChainAssetRegMngRepository;
import io.nuls.ledger.utils.LoggerUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.ledger.constant.LedgerConstant.CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE;
import static io.nuls.ledger.constant.LedgerConstant.CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS;

/**
 * Asset registration and management interface
 *
 * @author lanjinsheng .
 * @date 2019/10/22
 */
@Component
@NerveCoreCmd(module = ModuleE.LG)
public class AssetsRegCmd extends BaseLedgerCmd {

    @Autowired
    NerveCoreConfig ledgerConfig;
    @Autowired
    AssetRegMngService assetRegMngService;
    @Autowired
    LedgerChainManager ledgerChainManager;
    @Autowired
    CrossChainAssetRegMngRepository crossChainAssetRegMngRepository;
    @Autowired
    AssetRegMngRepository assetRegMngRepository;


    /**
     * On chain heterogeneous chain asset registration interface
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_HETEROGENEOUS_REG, version = 1.0,
            description = "On chain heterogeneous chain asset registration interface")
    @Parameters(value = {
            @Parameter(parameterName = "assetName", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset Name: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "initNumber", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Initial value of assets"),
            @Parameter(parameterName = "decimalPlace", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-18]", parameterDes = "The minimum number of split digits for assets"),
            @Parameter(parameterName = "assetSymbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset unit symbol: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "New asset address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "chainId", valueType = int.class, description = "chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid")
            })
    )
    public Response chainAssetHeterogeneousReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(4);
        try {
            LoggerUtil.COMMON_LOG.debug("Heterogeneous asset register params={}", JSONUtils.obj2json(params));
            params.put("chainId", ledgerConfig.getChainId());
            LedgerAsset asset = new LedgerAsset();
            asset.map2pojo(params, LedgerConstant.HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE);
            int assetId = assetRegMngService.registerHeterogeneousAsset(asset.getChainId(), asset);
            rtMap.put("assetId", assetId);
            rtMap.put("chainId", asset.getChainId());
            LoggerUtil.COMMON_LOG.debug("return={}", JSONUtils.obj2json(rtMap));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * In chain heterogeneous chain asset registration rollback
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_HETEROGENEOUS_ROLLBACK, version = 1.0,
            description = "In chain heterogeneous chain asset registration rollback")
    @Parameters(value = {
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetId"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "successtrue,failfalse")
            })
    )
    public Response chainAssetHeterogeneousRollBack(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            assetRegMngService.rollBackHeterogeneousAsset(ledgerConfig.getChainId(), Integer.parseInt(params.get("assetId").toString()));
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_BIND_HETEROGENEOUS_ASSET_REG, version = 1.0,
            description = "NERVEAsset binding heterogeneous chain assets")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "chainId"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return the bound asset type", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetType", valueType = int.class, description = "Asset type [5-On chain ordinary assets bound to heterogeneous chain assets 6-Parallel chain assets bound to heterogeneous chain assets 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets]"),
            })
    )
    public Response bindHeterogeneousAssetReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(4);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            short assetType = 0;
            LedgerAsset ledgerAsset = null;
            // Obtain registered in chain assets
            if(chainId == assetChainId) {
                if(assetId == ledgerConfig.getAssetId()) {
                    Map<String, Object> defaultAsset = ledgerChainManager.getLocalChainDefaultAsset();
                    Short _assetType = CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.get(defaultAsset.get("assetType"));
                    if (_assetType != null) {
                        defaultAsset.put("assetType", _assetType);
                        assetType = _assetType;
                        rtMap.put("assetType", assetType);
                        rtMap.put("decimalPlace", defaultAsset.get("decimalPlace"));
                        return success(rtMap);
                    }
                } else {
                    ledgerAsset = assetRegMngRepository.getLedgerAssetByAssetId(assetChainId, assetId);
                }
            } else {
                // Obtain registered cross chain assets
                ledgerAsset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, assetId);
            }
            if (ledgerAsset == null || StringUtils.isBlank(ledgerAsset.getSymbol())) {
                return failed(LedgerErrorCode.DATA_NOT_FOUND);
            }
            Short _assetType = CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.get(ledgerAsset.getAssetType());
            if (_assetType != null) {
                ledgerAsset.setAssetType(_assetType);
                assetType = _assetType;
            }
            if(chainId == assetChainId) {
                assetRegMngRepository.saveLedgerAssetReg(chainId, ledgerAsset);
            } else {
                crossChainAssetRegMngRepository.saveCrossChainAsset(chainId, ledgerAsset);
            }
            rtMap.put("assetType", assetType);
            rtMap.put("decimalPlace", ledgerAsset.getDecimalPlace());
            LoggerUtil.COMMON_LOG.debug("return={}", JSONUtils.obj2json(rtMap));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = CmdConstant.CMD_UNBIND_HETEROGENEOUS_ASSET_REG, version = 1.0,
            description = "NERVEUnbind assets to heterogeneous chain assets")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "chainId"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return the unbound asset type", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetType", valueType = int.class, description = "Asset type [1-On chain ordinary assets 3-Parallel chain assets 4-Heterogeneous chain assets]"),
            })
    )
    public Response unbindHeterogeneousAssetReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(4);
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetChainId = Integer.parseInt(params.get("assetChainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            short assetType = 0;
            LedgerAsset ledgerAsset = null;
            // Obtain registered in chain assets
            if(chainId == assetChainId) {
                if(assetId == ledgerConfig.getAssetId()) {
                    Map<String, Object> defaultAsset = ledgerChainManager.getLocalChainDefaultAsset();
                    Short _assetType = CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.get(defaultAsset.get("assetType"));
                    if (_assetType != null) {
                        defaultAsset.put("assetType", _assetType);
                        assetType = _assetType;
                        rtMap.put("assetType", assetType);
                        return success(rtMap);
                    }
                } else {
                    ledgerAsset = assetRegMngRepository.getLedgerAssetByAssetId(assetChainId, assetId);
                }
            } else {
                // Obtain registered cross chain assets
                ledgerAsset = crossChainAssetRegMngRepository.getCrossChainAsset(chainId, assetChainId, assetId);
            }
            if (ledgerAsset == null || StringUtils.isBlank(ledgerAsset.getSymbol())) {
                return failed(LedgerErrorCode.DATA_NOT_FOUND);
            }
            Short _assetType = CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.get(ledgerAsset.getAssetType());
            if (_assetType != null) {
                ledgerAsset.setAssetType(_assetType);
                assetType = _assetType;
            }
            if(chainId == assetChainId) {
                assetRegMngRepository.saveLedgerAssetReg(chainId, ledgerAsset);
            } else {
                crossChainAssetRegMngRepository.saveCrossChainAsset(chainId, ledgerAsset);
            }
            rtMap.put("assetType", assetType);
            LoggerUtil.COMMON_LOG.debug("return={}", JSONUtils.obj2json(rtMap));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * In chainSwap-LPAsset registration interface
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_SWAP_LIQUIDITY_POOL_REG, version = 1.0,
            description = "In chainSwap-LPAsset registration interface")
    @Parameters(value = {
            @Parameter(parameterName = "assetName", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset Name: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "initNumber", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Initial value of assets"),
            @Parameter(parameterName = "decimalPlace", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-18]", parameterDes = "The minimum number of split digits for assets"),
            @Parameter(parameterName = "assetSymbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset unit symbol: large、Lowercase letters、number、Underline（The underline cannot be at both ends）1~20byte"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "New asset address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "chainId", valueType = int.class, description = "chainid"),
                    @Key(name = "assetId", valueType = int.class, description = "assetid")
            })
    )
    public Response chainAssetSwapLiquidityPoolReg(Map params) {
        Map<String, Object> rtMap = new HashMap<>(4);
        try {
            LoggerUtil.COMMON_LOG.debug("Swap Liquidity Pool asset register params={}", JSONUtils.obj2json(params));
            params.put("chainId", ledgerConfig.getChainId());
            LedgerAsset asset = new LedgerAsset();
            asset.map2pojo(params, LedgerConstant.SWAP_LIQUIDITY_POOL_CROSS_CHAIN_ASSET_TYPE);
            int assetId = assetRegMngService.registerSwapLiquidityPoolAsset(asset.getChainId(), asset);
            rtMap.put("assetId", assetId);
            rtMap.put("chainId", asset.getChainId());
            LoggerUtil.COMMON_LOG.debug("return={}", JSONUtils.obj2json(rtMap));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * In chainSwap-LPAsset registration rollback
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_SWAP_LIQUIDITY_POOL_ROLLBACK, version = 1.0,
            description = "In chainSwap-LPAsset registration rollback")
    @Parameters(value = {
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetId"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "value", valueType = boolean.class, description = "successtrue,failfalse")
            })
    )
    public Response chainAssetSwapLiquidityPoolRollBack(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            assetRegMngService.rollBackSwapLiquidityPoolAsset(ledgerConfig.getChainId(), Integer.parseInt(params.get("assetId").toString()));
            rtMap.put("value", true);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * View registered asset information within the chain
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_REG_INFO, version = 1.0,
            description = "View registered asset information within the chain")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Run ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "assetType", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset type")

    })
    @ResponseData(name = "Return value", description = "Return alistobject",
            responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type"),
                    @Key(name = "assetOwnerAddress", valueType = String.class, description = "Address of asset owner"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols"),
                    @Key(name = "txHash", valueType = String.class, description = "transactionhashvalue")
            })
    )
    public Response getAssetRegInfo(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            if (null == params.get("assetType")) {
                params.put("assetType", "0");
            }
            List<Map<String, Object>> assets = assetRegMngService.getLedgerRegAssets(Integer.valueOf(params.get("chainId").toString()), Integer.valueOf(params.get("assetType").toString()));
            rtMap.put("assets", assets);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    /**
     * View registered asset information within the chain-Through assetsid
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = CmdConstant.CMD_CHAIN_ASSET_REG_INFO_BY_ASSETID, version = 1.0,
            description = "Through assetsidView registered asset information within the chain")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterValidRange = "[1-65535]", parameterDes = "Run ChainId,Value range[1-65535]"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = String.class), parameterValidRange = "[1-65535]", parameterDes = "assetid")

    })
    @ResponseData(name = "Return value", description = "Return aMapobject",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "assetId", valueType = int.class, description = "assetid"),
                    @Key(name = "assetType", valueType = int.class, description = "Asset type"),
                    @Key(name = "assetOwnerAddress", valueType = String.class, description = "Address of asset owner"),
                    @Key(name = "initNumber", valueType = BigInteger.class, description = "Asset initialization value"),
                    @Key(name = "decimalPlace", valueType = int.class, description = "Decimal Division"),
                    @Key(name = "assetName", valueType = String.class, description = "Asset Name"),
                    @Key(name = "assetSymbol", valueType = String.class, description = "Asset symbols"),
                    @Key(name = "txHash", valueType = String.class, description = "transactionhashvalue")
            })
    )
    public Response getAssetRegInfoByAssetId(Map params) {
        Map<String, Object> rtMap = new HashMap<>(1);
        try {
            rtMap = assetRegMngService.getLedgerRegAsset(Integer.valueOf(params.get("chainId").toString()), Integer.valueOf(params.get("assetId").toString()));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }
}
