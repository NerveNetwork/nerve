package io.nuls.api.exception;

import io.nuls.api.constant.ApiErrorCode;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-26 11:18
 * @Description: 功能描述
 */
public class SyncException extends NulsException {

    public long height;

    public SyncException(ErrorCode message, long height) {
        super(message);
        this.height = height;
    }


    public SyncException(long height,Throwable e) {
        super(ApiErrorCode.FAILED,e);
        this.height = height;
    }

}
