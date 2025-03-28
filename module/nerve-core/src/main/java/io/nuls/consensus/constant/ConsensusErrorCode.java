package io.nuls.consensus.constant;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;

/**
 * Consensus module error code corresponding class
 * @author tag
 * 2018/11/12
 * */
public interface ConsensusErrorCode extends CommonCodeConstanst {
    ErrorCode DATA_ERROR = ErrorCode.init("cs_0001");
    ErrorCode TX_NOT_EXIST = ErrorCode.init("cs_0002");
    ErrorCode AGENT_NOT_EXIST = ErrorCode.init("cs_0003");
    ErrorCode DATA_NOT_EXIST = ErrorCode.init("cs_0004");
    ErrorCode ADDRESS_ERROR = ErrorCode.init("cs_0005");
    ErrorCode PARAM_ERROR = ErrorCode.init("cs_0006");
    ErrorCode AGENTADDR_AND_PACKING_SAME = ErrorCode.init("cs_0007");
    ErrorCode REWARDADDR_AND_PACKING_SAME = ErrorCode.init("cs_0008");
    ErrorCode COMMISSION_RATE_OUT_OF_RANGE = ErrorCode.init("cs_0009");
    ErrorCode DEPOSIT_OUT_OF_RANGE = ErrorCode.init("cs_0010");
    ErrorCode DEPOSIT_ERROR = ErrorCode.init("cs_0011");
    ErrorCode TX_DATA_VALIDATION_ERROR = ErrorCode.init("cs_0012");
    ErrorCode AGENT_EXIST = ErrorCode.init("cs_0013");
    ErrorCode AGENT_PACKING_EXIST = ErrorCode.init("cs_0014");
    ErrorCode LACK_OF_CREDIT = ErrorCode.init("cs_0015");
    ErrorCode TRANSACTION_REPEATED = ErrorCode.init("cs_0016");
    ErrorCode DEPOSIT_OVER_AMOUNT= ErrorCode.init("cs_0017");
    ErrorCode DEPOSIT_NOT_ENOUGH= ErrorCode.init("cs_0018");
    ErrorCode SAVE_FAILED= ErrorCode.init("cs_0019");
    ErrorCode ROLLBACK_FAILED= ErrorCode.init("cs_0020");
    ErrorCode MERKEL_HASH_ERROR= ErrorCode.init("cs_0021");
    ErrorCode BLOCK_ROUND_VALIDATE_ERROR= ErrorCode.init("cs_0022");
    ErrorCode BANANCE_NOT_ENNOUGH= ErrorCode.init("cs_0023");
    ErrorCode CHAIN_NOT_EXIST= ErrorCode.init("cs_0024");
    ErrorCode BLOCK_PUNISH_VALID_ERROR = ErrorCode.init("cs_0025");
    ErrorCode BLOCK_SIGNATURE_ERROR = ErrorCode.init("cs_0026");
    ErrorCode BLOCK_RED_PUNISH_ERROR = ErrorCode.init("cs_0027");
    ErrorCode BLOCK_COINBASE_VALID_ERROR = ErrorCode.init("cs_0028");
    ErrorCode TRANSACTION_LIST_IS_NULL = ErrorCode.init("cs_0029");
    ErrorCode INTERFACE_CALL_FAILED = ErrorCode.init("cs_0030");
    ErrorCode ACCOUNT_VALID_ERROR = ErrorCode.init("cs_0031");
    ErrorCode TX_SIGNTURE_ERROR = ErrorCode.init("cs_0032");
    ErrorCode COIN_DATA_VALID_ERROR = ErrorCode.init("cs_0033");
    ErrorCode DEPOSIT_WAS_CANCELED = ErrorCode.init("cs_0034");
    ErrorCode DEPOSIT_NEVER_CANCELED = ErrorCode.init("cs_0035");
    ErrorCode CONFLICT_ERROR = ErrorCode.init("cs_0039");
    ErrorCode FEE_NOT_ENOUGH = ErrorCode.init("cs_0040");
    ErrorCode ERROR_UNLOCK_TIME = ErrorCode.init("cs_0041");

    ErrorCode ACCOUNT_IS_NOT_CREATOR = ErrorCode.init("cs_0042");
    ErrorCode APPEND_DEPOSIT_OUT_OF_RANGE = ErrorCode.init("cs_0043");
    ErrorCode REDUCE_DEPOSIT_OUT_OF_RANGE  = ErrorCode.init("cs_0044");
    ErrorCode REDUCE_DEPOSIT_LOCK_TIME_ERROR  = ErrorCode.init("cs_0045");
    ErrorCode AGENT_DEPOSIT_DATA_ERROR= ErrorCode.init("cs_0046");
    ErrorCode DEPOSIT_NOT_DUE= ErrorCode.init("cs_0047");
    ErrorCode REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST= ErrorCode.init("cs_0048");
    ErrorCode ASSET_NOT_SUPPORT_STACKING= ErrorCode.init("cs_0049");
    ErrorCode ASSET_NO_OFFER_YET= ErrorCode.init("cs_0050");
    ErrorCode DEPOSIT_ASSET_ERROR = ErrorCode.init("cs_0051");
    ErrorCode ASSET_NOT_SUPPORT_REGULAR_STACKING= ErrorCode.init("cs_0052");
    ErrorCode LOCKED_TX_NOT_FOUND= ErrorCode.init("cs_0053");
    ErrorCode UNLOCK_TX_NOT_FOUND= ErrorCode.init("cs_0054");
}
