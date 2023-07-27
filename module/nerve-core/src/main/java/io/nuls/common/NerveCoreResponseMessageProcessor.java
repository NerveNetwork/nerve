package io.nuls.common;

import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.message.Request;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;

import java.util.Map;


/**
 * 消息处理器
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
     * 发送Request，不接收返回
     * Send Request and wait for Response
     *
     * @param role    远程方法所属的角色，The role of remote method
     * @param request 远程方法的命令，Command of the remote method
     * @return 远程方法的返回结果，Response of the remote method
     * @throws Exception 请求超时（1分钟），timeout (1 minute)
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
