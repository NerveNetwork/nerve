/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter.utils.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Niels
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RpcResult<T> {

    private String jsonrpc = "2.0";

    private String id;

    private T result;

    private RpcResultError error;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
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

    public RpcResult setResult(T result) {
        this.result = result;
        return this;
    }

    public RpcResultError getError() {
        return error;
    }

    public RpcResult setError(RpcResultError error) {
        this.error = error;
        return this;
    }

    public static <T> RpcResult success(T t) {
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(t);
        return rpcResult;
    }

    public static RpcResult failed(RpcResultError error) {
        RpcResult rpcResult = new RpcResult();
        rpcResult.setError(error);
        return rpcResult;
    }
    public static RpcResult failed(RpcErrorCode errorCode) {
        RpcResult rpcResult = new RpcResult();
        RpcResultError error = new RpcResultError(errorCode);
        rpcResult.setError(error);
        return rpcResult;
    }

    public static RpcResult failed(RpcErrorCode errorCode, String data) {
        RpcResult rpcResult = new RpcResult();
        RpcResultError error = new RpcResultError(errorCode);
        error.setData(data);
        rpcResult.setError(error);
        return rpcResult;
    }

    public static RpcResult dataNotFound() {
        RpcResult rpcResult = new RpcResult();
        RpcResultError error = new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS.getCode(), RpcErrorCode.DATA_NOT_EXISTS.getMessage(), null);
        rpcResult.setError(error);
        return rpcResult;
    }

    public static RpcResult paramError() {
        RpcResult rpcResult = new RpcResult();
        RpcResultError error = new RpcResultError(RpcErrorCode.PARAMS_ERROR.getCode(), RpcErrorCode.PARAMS_ERROR.getMessage(), null);
        rpcResult.setError(error);
        return rpcResult;
    }

    public static RpcResult paramError(String data) {
        RpcResult rpcResult = new RpcResult();
        RpcResultError error = new RpcResultError(RpcErrorCode.PARAMS_ERROR.getCode(), RpcErrorCode.PARAMS_ERROR.getMessage(), data);
        rpcResult.setError(error);
        return rpcResult;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"jsonrpc\":")
                .append('\"').append(jsonrpc).append('\"');
        sb.append(",\"id\":")
                .append(id);
        sb.append(",\"result\":")
                .append('\"').append(result).append('\"');
        sb.append(",\"error\":")
                .append(error);
        sb.append('}');
        return sb.toString();
    }
}
