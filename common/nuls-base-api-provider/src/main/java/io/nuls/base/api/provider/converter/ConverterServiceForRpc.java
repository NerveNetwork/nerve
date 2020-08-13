/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.base.api.provider.converter;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.converter.facade.*;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.log.Log;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Charlie
 * @date: 2020/4/28
 */
@Provider(Provider.ProviderType.RPC)
public class ConverterServiceForRpc extends BaseRpcService implements ConverterService {

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> res) {
        return callRpc(ModuleE.CV.abbr, method, req, res);
    }

    @Override
    public Result<String> withdrawal(WithdrawalReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_withdrawal", req, fun);
    }

    @Override
    public Result<String> vote(VoteReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_voteProposal", req, fun);
    }

    @Override
    public Result<String> resetBank(ResetBankReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_resetVirtualBank", req, fun);
    }

    @Override
    public Result<Boolean> checkRetryParse(CheckRetryParseReq req) {
        Function<Map, Result> fun = res -> {
            Boolean rs = (Boolean) res.get("value");
            return success(rs);
        };
        return callRpc(ModuleE.CV.abbr, "cv_checkRetryParse", req, fun);
    }

    @Override
    public Result<String> registerHeterogeneousAsset(RegisterHeterogeneousAssetReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_create_heterogeneous_contract_asset_reg_pending_tx", req, fun);
    }

    @Override
    public Result<Boolean> validateRegisterHeterogeneousAsset(RegisterHeterogeneousAssetReq req) {
        Function<Map, Result> fun = res -> {
            Boolean data = Boolean.parseBoolean(res.get("value").toString());
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_validate_heterogeneous_contract_asset_reg_pending_tx", req, fun);
    }

    @Override
    public Result<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(GetHeterogeneousAssetInfoReq req) {
        return call("cv_get_heterogeneous_chain_asset_info", req, (Function<Map, Result>) res -> {
            try {
                HeterogeneousAssetInfo heterogeneousAssetInfo = MapUtils.mapToBean(res, new HeterogeneousAssetInfo());
                heterogeneousAssetInfo.setToken((Boolean) res.get("isToken"));
                return success(heterogeneousAssetInfo);
            } catch (Exception e) {
                Log.error("cv_get_heterogeneous_chain_asset_info fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<VirtualBankDirectorDTO> getVirtualBankInfo(GetVirtualBankInfoReq req) {
        return call("cv_virtualBankInfo", req, (Function<Map, Result>) res -> {
            try {
                List<Map> list = (List<Map>) res.get("list");
                return success(list.stream().map(d->{
                    Map<String,Object> map = (Map<String, Object>) d;
                    VirtualBankDirectorDTO dto = MapUtils.mapToBean(map,new VirtualBankDirectorDTO());
                    try {
                        dto.setHeterogeneousAddresses(MapUtils.mapsToObjects((List<Map<String, Object>>) map.get("heterogeneousAddresses"),HeterogeneousAddressDTO.class));
                    } catch (Exception e) {
                        Log.error("数据转换错误",e);
                    }
                    return dto;
                }).collect(Collectors.toList()));
            } catch (Exception e) {
                Log.error("cv_virtualBankInfo fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<String> getHeterogeneousAddress(GetHeterogeneousAddressReq req) {
        return null;
    }
}
