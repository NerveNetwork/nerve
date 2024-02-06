package io.nuls.common;

import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.message.Request;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;

import java.util.Map;


/**
 * Message processor
 * Send message processor
 *
 * @author tag
 * 2019/2/25
 */
public class NerveCoreResponseMessageProcessor {

    public static Response requestAndResponse(String role, String cmd, Map params) throws Exception {
        return requestAndResponse(role, cmd, params, Constants.TIMEOUT_TIMEMILLIS);
    }

    public static Response requestAndResponse(String role, String cmd, Map params, long timeOut) throws Exception {
        /*String key = role + "_" + cmd;
        InvokeBean invokeBean = CommonContext.INVOKE_BEAN_MAP.get(key);
        if (invokeBean != null) {
            Response response = (Response) invokeBean.getMethod().invoke(invokeBean.getBaseCmd(), JSONUtils.byteArray2pojo(JSONUtils.obj2ByteArray(params), Map.class));
            Map<String, Object> responseData = new HashMap<>(2);
            responseData.put(cmd, response.getResponseData());
            response.setResponseData(responseData);
            return JSONUtils.byteArray2pojo(JSONUtils.obj2ByteArray(response), Response.class);
        } else {
            return ResponseMessageProcessor.requestAndResponse(role, cmd, params, timeOut);
        }*/
        return ResponseMessageProcessor.requestAndResponse(role, cmd, params, timeOut);
    }

    /**
     * sendRequest, do not accept returns
     * Send Request and wait for Response
     *
     * @param role    The role to which the remote method belongs,The role of remote method
     * @param request Command for remote methods,Command of the remote method
     * @return The return result of the remote method,Response of the remote method
     * @throws Exception request timeout（1minute）,timeout (1 minute)
     */
    public static String requestOnly(String role, Request request) throws Exception {
        /*Map<String, Object> requestMethods = request.getRequestMethods();
        Map.Entry<String, Object> next = requestMethods.entrySet().iterator().next();
        String key = role + "_" + next.getKey();
        InvokeBean invokeBean = CommonContext.INVOKE_BEAN_MAP.get(key);
        if (invokeBean != null) {
            invokeBean.getMethod().invoke(invokeBean.getBaseCmd(), JSONUtils.byteArray2pojo(JSONUtils.obj2ByteArray(next.getValue()), Map.class));
            return "1";
        } else {
            return ResponseMessageProcessor.requestOnly(role, request);
        }*/
        return ResponseMessageProcessor.requestOnly(role, request);
    }

}
