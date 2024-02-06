package io.nuls.provider.api.resources;

import io.nuls.base.api.provider.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.model.*;
import io.nuls.provider.api.config.Config;
import io.nuls.provider.model.ErrorData;
import io.nuls.provider.model.RpcClientResult;
import io.nuls.provider.model.dto.VirtualBankDirectorDTO;
import io.nuls.provider.rpctools.ConverterTools;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Path("/api/converter")
@Component
@Api
public class ConverterResource {

    @Autowired
    Config config;
    @Autowired
    ConverterTools converterTools;


    @GET
    @Path("/address/eth/{packingAddress}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Query the corresponding Ethereum address based on the consensus node packaging address", order = 601)
    @Parameters({
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Consensus node packaging address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "ethAddress", description = "Ethereum address")
    }))
    public RpcClientResult getEthAddress(@PathParam("packingAddress") String packingAddress) {
        if (packingAddress == null) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "packingAddress is empty"));
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousAddress(config.getChainId(), 101, packingAddress);
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    @GET
    @Path("/bank")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Obtain virtual bank member information", order = 602)
    @ResponseData(name = "Return value", description = "Return aListobject", responseType = @TypeDescriptor(value = List.class,
            collectionElement = VirtualBankDirectorDTO.class))
    public RpcClientResult getVirtualBankInfo() {
        Result<List<VirtualBankDirectorDTO>> result = converterTools.getVirtualBankInfo(config.getChainId());
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    @GET
    @Path("/disqualification")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Obtain a list of revoked virtual bank qualification node addresses", order = 603)
    @ResponseData(name = "Return value", description = "Return aListobject", responseType = @TypeDescriptor(value = List.class,
            collectionElement = String.class))
    public RpcClientResult getDisqualification() {
        Result<String> result = converterTools.getDisqualification(config.getChainId());
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    @POST
    @Path("/proposal")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Application proposal",order = 604)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = byte.class), parameterDes = "Proposal type"),
            @Parameter(parameterName = "content", requestType = @TypeDescriptor(value = String.class), parameterDes = "Proposal content"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous Chain Tradinghash"),
            @Parameter(parameterName = "businessAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "address（account、Node address, etc）"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "On chain transactionshash"),
            @Parameter(parameterName = "voteRangeType", requestType = @TypeDescriptor(value = byte.class), parameterDes = "Voting scope type"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Remarks"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    }))
    public RpcClientResult createProposal(Map params) {
        Result<String> proposal = converterTools.proposal(params);
        return ResultUtil.getRpcClientResult(proposal);
    }

    @POST
    @Path("/fee")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Additional withdrawal handling fee/Proposal for returning the original route",order = 605)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Original transactionhash"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Additional handling fee amount"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    }))
    public RpcClientResult withdrawalAdditionalFee(Map params) {
        Result<String> proposal = converterTools.withdrawalAdditionalFee(params);
        return ResultUtil.getRpcClientResult(proposal);
    }

    @GET
    @Path("/heterogeneous/mainasset/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Return heterogeneous chain master assets inNERVEAsset information of the network", order = 606)
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
    public RpcClientResult getHeterogeneousMainAsset(@PathParam("chainId") Integer chainId) {
        if (chainId == null) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "chainId is empty"));
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousMainAsset(chainId);
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    /*@GET
    @Path("/heterogeneous/test/{params}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "test", order = 607)
    @Parameters({
            @Parameter(parameterName = "params", requestType = @TypeDescriptor(value = String.class), parameterDes = "test")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class)
    )
    public RpcClientResult test(@PathParam("params") String params) {
        Map map = new HashMap();
        map.put("params", params);
        Result result = converterTools.commonRequest("cv_test", map);
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }*/

}
