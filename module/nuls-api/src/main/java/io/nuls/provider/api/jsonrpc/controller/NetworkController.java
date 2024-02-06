package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.network.NetworkProvider;
import io.nuls.base.api.provider.network.facade.NetworkInfo;
import io.nuls.base.api.provider.protocol.ProtocolProvider;
import io.nuls.base.api.provider.protocol.facade.GetVersionReq;
import io.nuls.base.api.provider.protocol.facade.VersionInfo;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.parse.MapUtils;
import io.nuls.provider.api.config.Config;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-03 17:40
 * @Description: Function Description
 */
@Controller
@Api(type = ApiType.JSONRPC)
public class NetworkController {

    NetworkProvider networkProvider = ServiceManager.get(NetworkProvider.class);

    ProtocolProvider protocolService = ServiceManager.get(ProtocolProvider.class);

    @Autowired
    Config config;

    @RpcMethod("network")
    @ApiOperation(description = "network", order = 101, detailDesc = "")
    public RpcResult createAccount(List<Object> params) {
        if (params == null || params.isEmpty()) {
            Result<String> result = networkProvider.getNodeExtranetIp();
            if (result.isFailed()) {
                return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
            }
            return RpcResult.success(result.getData());
        }
        String cmd = (String) params.get(0);
        if ("info".equals(cmd)) {
            Result<NetworkInfo> result = networkProvider.getInfo();
            if (result.isFailed()) {
                return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
            }
            return RpcResult.success(result.getData());
        } else  {
            Result<String> result = networkProvider.getNodes();
            if (result.isFailed()) {
                return RpcResult.failed(CommonCodeConstanst.FAILED, result.getMessage());
            }
            return RpcResult.success(result.getList());
        }
    }

    @RpcMethod("version")
    public RpcResult getVersion(List<Object> params){
        Result<VersionInfo> res = protocolService.getVersion(new GetVersionReq());
        if(config.getPackageVersion() != null){
            Map<String,Object> m = MapUtils.beanToLinkedMap(res.getData());
            m.put("clientVersion",config.getPackageVersion());
            return RpcResult.success(new Result(m));
        }else{
            return RpcResult.success(res);
        }
    }

}
