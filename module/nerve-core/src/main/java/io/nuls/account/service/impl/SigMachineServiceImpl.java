package io.nuls.account.service.impl;

import io.nuls.account.model.dto.JsonrpcParam;
import io.nuls.account.model.dto.JsonrpcResult;
import io.nuls.account.service.SigMachineService;
import io.nuls.account.util.HttpClientUtil;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;

import java.util.Map;

@Component
public class SigMachineServiceImpl implements SigMachineService {

    @Autowired
    protected NerveCoreConfig config;

    @Override
    public String request(Map<String, Object> params) throws Exception {
        JsonrpcParam param = new JsonrpcParam();
        String method = (String) params.remove("method");
        param.setMethod(method);
        param.setId(StringUtils.newJsonrpcId());
        param.setParams(params);

        String response = HttpClientUtil.postJsonrpc(config.getSigMacUrl(), config.getSigMacApiKey(), param);
        JsonrpcResult<String> result = JSONUtils.json2pojo(response, JsonrpcResult.class);
        return result.getResult();
    }

}
