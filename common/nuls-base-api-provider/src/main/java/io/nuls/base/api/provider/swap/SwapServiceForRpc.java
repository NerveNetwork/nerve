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

package io.nuls.base.api.provider.swap;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.swap.facade.StableAddCoinReq;
import io.nuls.base.api.provider.swap.facade.StableManagePairReq;
import io.nuls.core.rpc.model.ModuleE;

import java.util.Map;
import java.util.function.Function;

/**
 * @author: PierreLuo
 * @date: 2021/7/1
 */
@Provider(Provider.ProviderType.RPC)
public class SwapServiceForRpc extends BaseRpcService implements SwapService {

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> res) {
        return callRpc(ModuleE.CV.abbr, method, req, res);
    }


    @Override
    public Result<String> stableAddCoin(StableAddCoinReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_proposal", req, fun);
    }

    @Override
    public Result<String> stableManagePair(StableManagePairReq req) {
        Function<Map, Result> fun = res -> {
            String data = (String) res.get("value");
            return success(data);
        };
        return callRpc(ModuleE.CV.abbr, "cv_proposal", req, fun);
    }
}
