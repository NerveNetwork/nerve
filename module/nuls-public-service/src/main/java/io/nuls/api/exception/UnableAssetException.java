package io.nuls.api.exception;

import io.nuls.api.constant.ApiErrorCode;
import io.nuls.core.exception.NulsException;

/**
 * @Author: zhoulijun
 * @Time: 2020/7/20 17:22
 * @Description: 功能描述
 */
public class UnableAssetException extends NulsException {

    int assetChainId;

    int assetId;

    public UnableAssetException(int assetChainId, int assetId){
        super(ApiErrorCode.UNABLE_ASSET,"unable asset " + assetChainId + "-" + assetId);
        this.assetChainId = assetChainId;
        this.assetId = assetId;
    }

}
