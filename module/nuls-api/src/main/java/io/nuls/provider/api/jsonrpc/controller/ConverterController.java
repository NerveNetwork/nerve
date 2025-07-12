package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.provider.api.config.Context;
import io.nuls.provider.model.dto.VirtualBankDirectorDTO;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.rpctools.ConverterTools;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.provider.utils.VerifyUtils;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class ConverterController {

    @Autowired
    ConverterTools converterTools;

    @RpcMethod("getEthAddress")
    @ApiOperation(description = "Query the corresponding Ethereum address based on the consensus node packaging address", order = 601)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Consensus node packaging address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousAddress", description = "Heterogeneous Chain Address")
    })
    )
    public RpcResult getEthAddress(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String packingAddress;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            packingAddress = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[packingAddress] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(packingAddress) || !AddressTool.validAddress(chainId, packingAddress)) {
            return RpcResult.paramError("[packingAddress] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousAddress(chainId, 101, packingAddress);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getVirtualBank")
    @ApiOperation(description = "Obtain virtual bank member information", order = 602)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    })
    @ResponseData(name = "Return value", description = "Return aListobject", responseType = @TypeDescriptor(value = List.class,
            collectionElement = VirtualBankDirectorDTO.class)
    )
    public RpcResult getVirtualBankInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<List<VirtualBankDirectorDTO>> result = converterTools.getVirtualBankInfo(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }


    @RpcMethod("getDisqualification")
    @ApiOperation(description = "Obtain a list of revoked virtual bank qualification node addresses", order = 603)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    })
    @ResponseData(name = "Return value", description = "Return aListobject", responseType = @TypeDescriptor(value = List.class,
            collectionElement = String.class)
    )
    public RpcResult getDisqualification(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<String> result = converterTools.getDisqualification(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("retryWithdrawalMsg")
    @ApiOperation(description = "Repositioning heterogeneous chain withdrawal transactionstask, Resend message", order = 604)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = int.class), parameterDes = "On chain withdrawal transactionshash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public RpcResult retryWithdrawalMsg(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String hash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            hash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[hash] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.retryWithdrawalMsg(chainId, hash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousMainAsset")
    @ApiOperation(description = "Return heterogeneous chain master assets inNERVEAsset information of the network", order = 605)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "Heterogeneous chainID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "Asset ChainID"),
            @Key(name = "assetId", valueType = int.class, description = "assetID"),
            @Key(name = "symbol", description = "assetsymbol"),
            @Key(name = "decimals", valueType = int.class, description = "Decimal places of assets")
    })
    )
    public RpcResult getHeterogeneousMainAsset(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousMainAsset(chainId);
        if (chainId == 106) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "POL");
            }
        } else
        if (chainId == 116) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "KAIA");
            }
        } else
        if (chainId == 124) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "A");
            }
        }
        return ResultUtil.getJsonRpcResult(result);
    }
    @RpcMethod("getProposalInfo")
    @ApiOperation(description = "Query proposal information（Serializing strings）", order = 606)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Proposal transactionhash")
    })
    @ResponseData(name = "Return value", description = "return network.nerve.converter.model.po.ProposalPO Serialized string of object")
    public RpcResult getProposalInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String proposalTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            proposalTxHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[proposalTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(proposalTxHash)) {
            return RpcResult.paramError("[proposalTxHash] is incorrect");
        }
        Result<String> result = converterTools.getProposalInfo(chainId, proposalTxHash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getRechargeNerveHash")
    @ApiOperation(description = "Transactions transferred across heterogeneous chainshashqueryNERVETransactionhash", order = 607)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Cross chain transfer transactions of heterogeneous chainshash")
    })
    @ResponseData(name = "Return value", description = "NERVEtransactionhash")
    public RpcResult getRechargeNerveHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String heterogeneousTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            heterogeneousTxHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(heterogeneousTxHash)) {
            return RpcResult.paramError("[heterogeneousTxHash] is incorrect");
        }
        Result<String> result = converterTools.getRechargeNerveHash(chainId, heterogeneousTxHash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("findByWithdrawalTxHash")
    @ApiOperation(description = "Based on withdrawal transactionshashObtain confirmation information", order = 608)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Withdrawal transactionshash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", description = "Heterogeneous chainID"),
            @Key(name = "heterogeneousHeight", description = "Heterogeneous chain transaction block height"),
            @Key(name = "heterogeneousTxHash", description = "Heterogeneous Chain Tradinghash"),
            @Key(name = "confirmWithdrawalTxHash", description = "NERVEConfirm transactionhash")
    }))
    public RpcResult findByWithdrawalTxHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(txHash)) {
            return RpcResult.paramError("[txHash] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.findByWithdrawalTxHash(chainId, txHash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousRegisterNetwork")
    @ApiOperation(description = "Heterogeneous chain registration network for querying assets", order = 609)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "Heterogeneous chainID"),
            @Key(name = "contractAddress", valueType = String.class, description = "Asset corresponding contract address(If there is any)")
    })
    )
    public RpcResult getHeterogeneousRegisterNetwork(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousRegisterNetwork(chainId, assetId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousAssetInfo")
    @ApiOperation(description = "Query heterogeneous chain asset information of assets", order = 610)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class)
    )
    public RpcResult getHeterogeneousAssetInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousAssetInfo(chainId, assetId);
        //9-160
        //5-34
        if ((chainId == 9 && assetId == 160) || (chainId == 5 && assetId == 34)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "POL");
            }
        } else
        if ((chainId == 9 && assetId == 448) || (chainId == 5 && assetId == 118)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "KAIA");
            }
        } else
        if ((chainId == 9 && assetId == 692) || (chainId == 5 && assetId == 148)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("symbol", "A");
            }
        }
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousAssetInfoList")
    @ApiOperation(description = "Query the heterogeneous chain asset information list of assets", order = 614)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = List.class)
    )
    public RpcResult getHeterogeneousAssetInfoList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        Map<String, Object> params1 = new HashMap<>(2);
        params1.put("chainId", chainId);
        params1.put("assetId", assetId);
        Result result = converterTools.commonRequest("cv_get_heterogeneous_chain_asset_info_list", params1);
        if ((chainId == 9 && assetId == 160) || (chainId == 5 && assetId == 34)) {
            List<Map> dataList = (List<Map>) result.getData();
            if (dataList != null) {
                for (Map data : dataList) {
                    data.put("symbol", "POL");
                }
            }
        } else
        if ((chainId == 9 && assetId == 448) || (chainId == 5 && assetId == 118)) {
            List<Map> dataList = (List<Map>) result.getData();
            if (dataList != null) {
                for (Map data : dataList) {
                    data.put("symbol", "KAIA");
                }
            }
        } else
        if ((chainId == 9 && assetId == 692) || (chainId == 5 && assetId == 148)) {
            List<Map> dataList = (List<Map>) result.getData();
            if (dataList != null) {
                for (Map data : dataList) {
                    data.put("symbol", "A");
                }
            }
        }
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("retryVirtualBank")
    @ApiOperation(description = "Re execute changes", order = 611)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Change transactionhash"),
            @Parameter(parameterName = "height", requestType = @TypeDescriptor(value = long.class), parameterDes = "Change exchange at height"),
            @Parameter(parameterName = "prepare", requestType = @TypeDescriptor(value = int.class), parameterDes = "1 - Preparation phase,2 - Unprepared, execution phase")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public RpcResult retryVirtualBank(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, prepare;
        String hash;
        long height;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            hash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }
        try {
            height = Long.parseLong(params.get(2).toString());
        } catch (Exception e) {
            return RpcResult.paramError("[height] is inValid");
        }
        try {
            prepare = Integer.parseInt(params.get(3).toString());
        } catch (Exception e) {
            return RpcResult.paramError("[prepare] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[hash] is incorrect");
        }
        Map<String, Object> params1 = new HashMap<>(8);
        params1.put("chainId", chainId);
        params1.put("hash", hash);
        params1.put("height", height);
        params1.put("prepare", prepare);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_retryVirtualBank", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("checkRetryHtgTx")
    @ApiOperation(description = "Re analyze heterogeneous chain transactions", order = 612)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous Chain Tradinghash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public RpcResult checkRetryHtgTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId,heterogeneousChainId;
        String heterogeneousTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            heterogeneousChainId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousChainId] is inValid");
        }
        try {
            heterogeneousTxHash = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(heterogeneousTxHash)) {
            return RpcResult.paramError("[heterogeneousTxHash] is incorrect");
        }
        Map<String, Object> params1 = new HashMap<>(8);
        params1.put("chainId", chainId);
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("heterogeneousTxHash", heterogeneousTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_checkRetryHtgTx", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("registerheterogeneousasset")
    @ApiOperation(description = "Register heterogeneous chain assets", order = 613)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "Decimal places of assets"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset symbols"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes", canNull = true)
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public RpcResult registerheterogeneousasset(List<Object> params) {
        VerifyUtils.verifyParams(params, 8);
        int i = 0;
        int chainId = (int) params.get(i++);
        int heterogeneousChainId = (int) params.get(i++);
        int decimals = (int) params.get(i++);
        String symbol = (String) params.get(i++);
        String contractAddress = (String) params.get(i++);
        String address = (String) params.get(i++);
        String password = (String) params.get(i++);
        String remark = (String) params.get(i++);

        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("decimals", decimals);
        params1.put("symbol", symbol);
        params1.put("contractAddress", contractAddress);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_validate_heterogeneous_contract_asset_reg_pending_tx", params1);
        if (result.isFailed()) {
            return ResultUtil.getJsonRpcResult(result);
        }
        Map<String, Object> dataMap = result.getData();
        Boolean data = Boolean.parseBoolean(dataMap.get("value").toString());
        if (!data.booleanValue()) {
            return RpcResult.failed(CommonCodeConstanst.DATA_ERROR, "validate error");
        }

        Map<String, Object> params2 = new HashMap<>();
        params2.put("chainId", chainId);
        params2.put("heterogeneousChainId", heterogeneousChainId);
        params2.put("decimals", decimals);
        params2.put("symbol", symbol);
        params2.put("contractAddress", contractAddress);
        params2.put("address", address);
        params2.put("password", password);
        params2.put("remark", remark);
        Result<Map<String, Object>> result1 = converterTools.commonRequest("cv_create_heterogeneous_contract_asset_reg_pending_tx", params2);

        return ResultUtil.getJsonRpcResult(result1);
    }

    @RpcMethod("gasLimitOfHeterogeneousChains")
    @ApiOperation(description = "What heterogeneous chains requiregasLimit", order = 614)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult gasLimitOfHeterogeneousChains(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int i = 0;
        int chainId = (int) params.get(i++);

        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_gasLimitOfHeterogeneousChains", params1);
        return ResultUtil.getJsonRpcResult(result);
    }


    @RpcMethod("getChainWithdrawalFee")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "WITHDRAWAL_FEE"),
    }))
    public RpcResult getChainWithdrawalFee(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int i = 0;
        int chainId = (int) params.get(i++);
        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", chainId);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_chainWithdrawalFee", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("hasRecordFeePayment")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "htgTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    }))
    public RpcResult hasRecordFeePayment(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        int chainId = (int) params.get(i++);
        String htgTxHash = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", chainId);
        params1.put("htgTxHash", htgTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_hasRecordFeePayment", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getWithdrawalFeeLog")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "htgTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getWithdrawalFeeLogFromDB(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        int chainId = (int) params.get(i++);
        String htgTxHash = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", chainId);
        params1.put("htgTxHash", htgTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_withdrawalFeeLog", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getMinimumFeeOfWithdrawal")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "minimumFee", description = "minimumFee"),
            @Key(name = "utxoSize", description = "utxoSize"),
            @Key(name = "feeRate", description = "feeRate")
    }))
    public RpcResult getMinimumFeeOfWithdrawal(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        int chainId = (int) params.get(i++);
        String txHash = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", chainId);
        params1.put("nerveTxHash", txHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_minimumFeeOfWithdrawal", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("recordFeePaymentNerveInner")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public RpcResult recordFeePaymentNerveInner(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int i = 0;
        int chainId = (int) params.get(i++);
        int heterogeneousChainId = (int) params.get(i++);
        String nerveTxHash = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("nerveTxHash", nerveTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_record_fee_payment_nerve_inner", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("unlockUtxo")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "nerveTxHash", parameterType = "String", parameterDes = "nerveTxHash"),
            @Parameter(parameterName = "forceUnlock", parameterType = "boolean", parameterDes = "forceUnlock"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public RpcResult unlockUtxo(List<Object> params) {
        VerifyUtils.verifyParams(params, 5);
        int i = 0;
        int chainId = (int) params.get(i++);
        String nerveTxHash = (String) params.get(i++);
        boolean forceUnlock = (boolean) params.get(i++);
        String address = (String) params.get(i++);
        String password = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("nerveTxHash", nerveTxHash);
        params1.put("forceUnlock", forceUnlock ? 1 : 0);
        params1.put("address", address);
        params1.put("password", password);
        if (params.size() > 5) {
            params1.put("htgChainId", params.get(i++));
        }
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_unlock_utxo", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("skipTx")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "nerveTxHash", parameterType = "String", parameterDes = "nerveTxHash"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public RpcResult skipTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 5);
        int i = 0;
        int chainId = (int) params.get(i++);
        String nerveTxHash = (String) params.get(i++);
        String address = (String) params.get(i++);
        String password = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("nerveTxHash", nerveTxHash);
        params1.put("address", address);
        params1.put("password", password);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_skip_tx", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getSplitGranularity")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "splitGranularity")
    }))
    public RpcResult getSplitGranularity(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int i = 0;
        int heterogeneousChainId = (int) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", heterogeneousChainId);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_getSplitGranularity", params1);
        return ResultUtil.getJsonRpcResult(result);
    }
    @RpcMethod("getUtxoCheckedInfo")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "utxoList", requestType = @TypeDescriptor(value = List.class), parameterDes = "utxoList")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "detail data")
    }))
    public RpcResult getUtxoCheckedInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        int heterogeneousChainId = (int) params.get(i++);
        List utxoList = (List) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("utxoList", utxoList);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_getUtxoCheckedInfo", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getWithdrawalUTXOInfo")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "detail data")
    }))
    public RpcResult getWithdrawalUTXOInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        int heterogeneousChainId = (int) params.get(i++);
        String nerveTxHash = (String) params.get(i++);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("nerveTxHash", nerveTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_getWithdrawalUTXOInfo", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("checkPauseOut")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public RpcResult checkPauseOut(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int i = 0;
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", params.get(i++));
        params1.put("htgChainId", params.get(i++));
        params1.put("nerveAssetChainId", params.get(i++));
        params1.put("nerveAssetId", params.get(i++));
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_checkPauseOut", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("checkPauseIn")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public RpcResult checkPauseIn(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int i = 0;
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", params.get(i++));
        params1.put("htgChainId", params.get(i++));
        params1.put("nerveAssetChainId", params.get(i++));
        params1.put("nerveAssetId", params.get(i++));
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_checkPauseIn", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getCrossOutTxFee")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    })
    )
    public RpcResult getCrossOutTxFee(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int i = 0;
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", params.get(i++));
        params1.put("txHash", params.get(i++));
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_getCrossOutTxFee", params1);
        return ResultUtil.getJsonRpcResult(result);
    }
}
