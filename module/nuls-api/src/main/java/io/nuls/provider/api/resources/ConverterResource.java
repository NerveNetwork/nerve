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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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
}
