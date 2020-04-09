package io.nuls.dex.context;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;

public interface DexErrorCode extends CommonCodeConstanst {
    //币对还存在挂单委托
    ErrorCode TRADING_MORE_ORDER_EXIST = ErrorCode.init("dx_0001");
    //挂单交易的币和用户支付的币不一致
    ErrorCode ORDER_COIN_NOT_EQUAL = ErrorCode.init("dx_0002");
    //币对信息已存在
    ErrorCode COIN_TRADING_EXIST = ErrorCode.init("dx_0003");

    ErrorCode BELOW_TRADING_MIN_SIZE = ErrorCode.init("dx_0004");
    //地址错误
    ErrorCode ERROR_ADDRESS_ERROR = ErrorCode.init("dx_0005");

    ErrorCode ACCOUNT_VALID_ERROR = ErrorCode.init("dx_0006");

    ErrorCode BALANCE_NOT_ENOUGH = ErrorCode.init("dx_0007");

    ErrorCode INSUFFICIENT_FEE = ErrorCode.init("dx_0008");
}
