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
    @ApiOperation(description = "根据共识节点打包地址查询相应的以太坊地址", order = 601)
    @Parameters({
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "共识节点打包地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "ethAddress", description = "以太坊地址")
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
    @ApiOperation(description = "获取虚拟银行成员信息", order = 602)
    @ResponseData(name = "返回值", description = "返回一个List对象", responseType = @TypeDescriptor(value = List.class,
            collectionElement = VirtualBankDirectorDTO.class))
    public RpcClientResult getVirtualBankInfo() {
        Result<List<VirtualBankDirectorDTO>> result = converterTools.getVirtualBankInfo(config.getChainId());
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    @GET
    @Path("/disqualification")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "获取已撤销虚拟银行资格节点地址列表", order = 603)
    @ResponseData(name = "返回值", description = "返回一个List对象", responseType = @TypeDescriptor(value = List.class,
            collectionElement = String.class))
    public RpcClientResult getDisqualification() {
        Result<String> result = converterTools.getDisqualification(config.getChainId());
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        return clientResult;
    }

    @POST
    @Path("/proposal")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "申请提案",order = 604)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = byte.class), parameterDes = "提案类型"),
            @Parameter(parameterName = "content", requestType = @TypeDescriptor(value = String.class), parameterDes = "提案类容"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链交易hash"),
            @Parameter(parameterName = "businessAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "地址（账户、节点地址等）"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "链内交易hash"),
            @Parameter(parameterName = "voteRangeType", requestType = @TypeDescriptor(value = byte.class), parameterDes = "投票范围类型"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    }))
    public RpcClientResult createProposal(Map params) {
        Result<String> proposal = converterTools.proposal(params);
        return ResultUtil.getRpcClientResult(proposal);
    }

    @POST
    @Path("/fee")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "追加提现手续费/原路退回的提案",order = 605)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "原始交易hash"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "追加的手续费金额"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    }))
    public RpcClientResult withdrawalAdditionalFee(Map params) {
        Result<String> proposal = converterTools.withdrawalAdditionalFee(params);
        return ResultUtil.getRpcClientResult(proposal);
    }

    @GET
    @Path("/heterogeneous/mainasset/{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "返回异构链主资产在NERVE网络的资产信息", order = 606)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "异构链ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "资产链ID"),
            @Key(name = "assetId", valueType = int.class, description = "资产ID"),
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数")
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

}
