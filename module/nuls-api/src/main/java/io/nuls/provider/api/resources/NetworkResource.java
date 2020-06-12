package io.nuls.provider.api.resources;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.network.NetworkProvider;
import io.nuls.base.api.provider.network.facade.NetworkInfo;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-03 17:40
 * @Description: 功能描述
 */
@Path("/api/network")
@Component
@Api
public class NetworkResource {

    NetworkProvider networkProvider = ServiceManager.get(NetworkProvider.class);

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public RpcResult info() {
        Result<NetworkInfo> result = networkProvider.getInfo();
        if (result.isFailed()) {
            return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
        }
        return RpcResult.success(result.getData());
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public RpcResult nodes() {
        Result<String> result = networkProvider.getNodes();
        if (result.isFailed()) {
            return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
        }
        return RpcResult.success(result.getList());
    }

    @GET
    @Path("/ip")
    @Produces(MediaType.APPLICATION_JSON)
    public RpcResult ip() {
        Result<String> result = networkProvider.getNodeExtranetIp();
        if (result.isFailed()) {
            return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
        }
        return RpcResult.success(result.getData());
    }

}
