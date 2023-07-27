package io.nuls.account.model.dto;

public class JsonrpcParam {

    private String id;
    private String jsonrpc;
    private String method;
    private Object params;

    public JsonrpcParam() {
    }

    public JsonrpcParam(String id, String jsonrpc, String method, Object params) {
        this.id = id;
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
