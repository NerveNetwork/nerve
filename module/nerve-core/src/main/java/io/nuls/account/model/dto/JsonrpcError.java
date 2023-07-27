package io.nuls.account.model.dto;

public class JsonrpcError<T> {

    private String code;

    private String message;

    private Object data;

    public JsonrpcError() {

    }

    public JsonrpcError(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public JsonrpcError setCode(String code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public JsonrpcError setMessage(String message) {
        this.message = message;
        return this;
    }

    public Object getData() {
        return data;
    }

    public JsonrpcError setData(Object data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"code\":")
                .append(code);
        sb.append(",\"message\":")
                .append('\"').append(message).append('\"');
        sb.append(",\"entity\":")
                .append('\"').append(data).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
