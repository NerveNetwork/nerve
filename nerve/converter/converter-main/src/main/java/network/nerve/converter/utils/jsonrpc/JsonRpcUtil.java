package network.nerve.converter.utils.jsonrpc;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * JSON-RPC 请求工具
 * @author: PierreLuo
 * @date: 2019-07-01
 */
public class JsonRpcUtil {

    private static final String ID = "id";
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String DEFAULT_ID = "1";
    private static final String JSONRPC_VERSION = "2.0";

    public static RpcResult request(String requestURL, String method, List<Object> params) {
        return request(requestURL, method, params, null);
    }

    public static RpcResult request(String requestURL, String method, List<Object> params, List<BasicHeader> headers) {
        RpcResult rpcResult;
        try {
            Map<String, Object> map = new HashMap<>(8);
            map.put(ID, DEFAULT_ID);
            map.put(JSONRPC, JSONRPC_VERSION);
            map.put(METHOD, method);
            map.put(PARAMS, params);
            String resultStr = HttpClientUtil.post(requestURL, map, headers);
            rpcResult = JSONUtils.json2pojo(resultStr, RpcResult.class);
        } catch (Exception e) {
            rpcResult = RpcResult.failed(new RpcResultError(CommonCodeConstanst.DATA_ERROR.getCode(), e.getMessage(), null));
        }
        return rpcResult;
    }

    public static List<RpcResult> batchRequest(String requestURL, List<String> methods, List<List<Object>> params) {
        return batchRequest(requestURL, methods, params, null);
    }

    public static List<RpcResult> batchRequest(String requestURL, List<String> methods, List<List<Object>> params, List<BasicHeader> headers) {
        try {
            List<Map> p = new ArrayList<>();
            for (int i = 0, length = methods.size(); i < length; i++) {
                Map<String, Object> map = new HashMap<>(8);
                map.put(ID, i);
                map.put(JSONRPC, JSONRPC_VERSION);
                map.put(METHOD, methods.get(i));
                map.put(PARAMS, params.get(i));
                p.add(map);
            }
            String resultStr = HttpClientUtil.post(requestURL, p, headers);
            List<RpcResult> rpcResults;
            if (methods.size() == 1 && !resultStr.startsWith("[")) {
                rpcResults = List.of(JSONUtils.json2pojo(resultStr, RpcResult.class));
            } else {
                rpcResults = JSONUtils.json2list(resultStr, RpcResult.class);
            }
            return rpcResults;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
