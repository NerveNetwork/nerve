/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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

package network.nerve.swap.rpc.call;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.swap.utils.LoggerUtil;

import java.util.Map;


/**
 * @author: Loki
 * @date: 2020-03-02
 */
public class BaseCall {

    public static Object requestAndResponse(String moduleCode, String cmd, Map params) throws NulsException {
        return requestAndResponse(moduleCode, cmd, params, null);
    }
    /**
     * Call other module interfaces
     * Call other module interfaces
     */
    public static Object requestAndResponse(String moduleCode, String cmd, Map params, Long timeout) throws NulsException {
        try {
            Response response = null;
            try {
                if(null == timeout) {
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params);
                }else{
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params, timeout);
                }
            } catch (Exception e) {
                LoggerUtil.LOG.error(e);
                throw new NulsException(CommonCodeConstanst.RPC_REQUEST_FAILD);
            }
            if (!response.isSuccess()) {
                String errorCode = response.getResponseErrorCode();
                LoggerUtil.LOG.error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{}", cmd, errorCode, response.getResponseComment());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            Map data = (Map)response.getResponseData();
            return data.get(cmd);
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(CommonCodeConstanst.RPC_REQUEST_FAILD);
        }
    }
}
