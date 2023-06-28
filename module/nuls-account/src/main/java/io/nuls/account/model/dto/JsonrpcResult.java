package io.nuls.account.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonrpcResult<T> {

    private final String jsonrpc = "2.0";
    private String id;
    private T result;
    private JsonrpcError error;

    public JsonrpcResult() {
    }

    public JsonrpcResult(boolean success, JsonrpcError errMsg, T result) {
        this.error = errMsg;
        this.result = result;
    }

    public static <T> JsonrpcResult<T> success(T result) {
        return new JsonrpcResult<>(true, null, result);
    }

    public static <T> JsonrpcResult<T> error(String errCode, String errMsg) {
        return error(errCode, errMsg, null);
    }

    public static <T> JsonrpcResult<T> error(String errCode, String errMsg, String data) {
        return new JsonrpcResult<>(false, new JsonrpcError(errCode, errMsg, data), null);
    }

    public static JsonrpcResult PARAM_ERROR(String field) {
        return error("32003","Paramether is not right: "+field);
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public JsonrpcError getError() {
        return error;
    }

    public void setError(JsonrpcError error) {
        this.error = error;
    }
}
