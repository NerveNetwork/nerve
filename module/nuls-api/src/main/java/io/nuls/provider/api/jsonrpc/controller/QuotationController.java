package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.rpctools.CommonTools;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class QuotationController {

    @Autowired
    private CommonTools commonTools;

    @RpcMethod("getFinalQuotation")
    public RpcResult getHeterogeneousChainAssetInfo(List<Object> params) {
        int chainId;
        String key, date;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            key = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[key] is inValid");
        }
        try {
            date = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[date] is inValid");
        }

        Map<String, Object> params1 = new HashMap<>(8);
        params1.put("chainId", chainId);
        params1.put("key", key);
        params1.put("date", date);
        Result<Map<String, Object>> result = commonTools.commonRequest(ModuleE.QU.abbr, "qu_final_quotation", params1);
        return ResultUtil.getJsonRpcResult(result);
    }
}
