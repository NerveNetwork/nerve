package network.nerve.dex.context;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;

public interface DexErrorCode extends CommonCodeConstanst {
    //There is still a commission for placing orders on the currency pair
    ErrorCode TRADING_MORE_ORDER_EXIST = ErrorCode.init("dx_0001");
    //The currency used for the order transaction is inconsistent with the currency paid by the user
    ErrorCode ORDER_COIN_NOT_EQUAL = ErrorCode.init("dx_0002");
    //Coin pair information already exists
    ErrorCode COIN_TRADING_EXIST = ErrorCode.init("dx_0003");

    ErrorCode BELOW_TRADING_MIN_SIZE = ErrorCode.init("dx_0004");
    //Address error
    ErrorCode ERROR_ADDRESS_ERROR = ErrorCode.init("dx_0005");

    ErrorCode ACCOUNT_VALID_ERROR = ErrorCode.init("dx_0006");

    ErrorCode BALANCE_NOT_ENOUGH = ErrorCode.init("dx_0007");

    ErrorCode INSUFFICIENT_FEE = ErrorCode.init("dx_0008");

    ErrorCode SYNC_BATCH_VALIDATE_ERROR = ErrorCode.init("dx_0009");

    ErrorCode CREATE_DEAL_TX_COIN_DATA_ERROR = ErrorCode.init("dx_0010");
}
