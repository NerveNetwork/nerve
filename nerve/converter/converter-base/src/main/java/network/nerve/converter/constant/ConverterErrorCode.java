package network.nerve.converter.constant;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.rpc.model.ModuleE;

/**
 * @author: Loki
 * @date: 2018/11/12
 */
public interface ConverterErrorCode extends CommonCodeConstanst {

    ErrorCode CHAIN_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0001");
    ErrorCode AGENT_ADDRESS_NULL = ErrorCode.init(ModuleE.CV.getPrefix() + "_0002");
    ErrorCode HETEROGENEOUS_ADDRESS_NULL = ErrorCode.init(ModuleE.CV.getPrefix() + "_0003");
    ErrorCode HETEROGENEOUS_CHAINID_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0004");
    ErrorCode PROPOSAL_TYPE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0005");
    ErrorCode PROPOSAL_TX_HASH_NULL = ErrorCode.init(ModuleE.CV.getPrefix() + "_0006");
    ErrorCode REMOTE_RESPONSE_DATA_NOT_FOUND = ErrorCode.init(ModuleE.CV.getPrefix() + "_0007");
    ErrorCode INSUFFICIENT_BALANCE = ErrorCode.init(ModuleE.CV.getPrefix() + "_0008");
    ErrorCode IS_NOT_CURRENT_CHAIN_ADDRESS = ErrorCode.init(ModuleE.CV.getPrefix() + "_0009");
    ErrorCode SIGNER_NOT_CONSENSUS_AGENT = ErrorCode.init(ModuleE.CV.getPrefix() + "_0010");
    ErrorCode PROPOSAL_VOTE_INVALID = ErrorCode.init(ModuleE.CV.getPrefix() + "_0011");
    ErrorCode SIGNER_NOT_VIRTUAL_BANK_AGENT = ErrorCode.init(ModuleE.CV.getPrefix() + "_0012");


    ErrorCode RECHARGE_NOT_INCLUDE_COINFROM = ErrorCode.init(ModuleE.CV.getPrefix() + "_0013");
    ErrorCode RECHARGE_HAVE_EXACTLY_ONE_COINTO = ErrorCode.init(ModuleE.CV.getPrefix() + "_0014");
    ErrorCode HETEROGENEOUS_TX_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0015");
    ErrorCode RECHARGE_ASSETID_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0016");
    ErrorCode RECHARGE_AMOUNT_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0017");
    ErrorCode RECHARGE_ARRIVE_ADDRESS_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0018");

    ErrorCode AGENT_IS_VIRTUAL_BANK = ErrorCode.init(ModuleE.CV.getPrefix() + "_0019");
    ErrorCode AGENT_IS_NOT_VIRTUAL_BANK = ErrorCode.init(ModuleE.CV.getPrefix() + "_0020");
    ErrorCode CAN_NOT_JOIN_VIRTUAL_BANK = ErrorCode.init(ModuleE.CV.getPrefix() + "_0021");
    ErrorCode CAN_NOT_QUIT_VIRTUAL_BANK = ErrorCode.init(ModuleE.CV.getPrefix() + "_0022");

    ErrorCode AGENT_INFO_NOT_FOUND = ErrorCode.init(ModuleE.CV.getPrefix() + "_0023");
    ErrorCode WITHDRAWAL_COIN_SIZE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0024");
    ErrorCode WITHDRAWAL_FEE_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0025");
    ErrorCode WITHDRAWAL_FROM_TO_ASSET_AMOUNT_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0026");
    ErrorCode WITHDRAWAL_ARRIVE_ADDRESS_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0027");

    ErrorCode WITHDRAWAL_TX_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0028");
    ErrorCode CFM_WITHDRAWAL_ARRIVE_ADDRESS_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0029");
    ErrorCode CFM_WITHDRAWAL_HEIGHT_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0030");
    ErrorCode CFM_WITHDRAWAL_AMOUNT_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0031");
    ErrorCode VIRTUAL_BANK_OVER_MAXIMUM = ErrorCode.init(ModuleE.CV.getPrefix() + "_0032");
    ErrorCode HETEROGENEOUS_COMPONENT_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0033");

    ErrorCode CHANGE_VIRTUAL_BANK_TX_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0034");
    ErrorCode VIRTUAL_BANK_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0035");
    ErrorCode HETEROGENEOUS_TX_TIME_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0036");
    ErrorCode VIRTUAL_BANK_MULTIADDRESS_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0037");

    ErrorCode TX_SUBSIDY_FEE_ADDRESS_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0038");
    ErrorCode TX_INSUFFICIENT_SUBSIDY_FEE = ErrorCode.init(ModuleE.CV.getPrefix() + "_0039");

    ErrorCode CFM_IS_DUPLICATION = ErrorCode.init(ModuleE.CV.getPrefix() + "_0040");
    ErrorCode DISTRIBUTION_FEE_IS_DUPLICATION = ErrorCode.init(ModuleE.CV.getPrefix() + "_0041");
    ErrorCode HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY = ErrorCode.init(ModuleE.CV.getPrefix() + "_0042");
    ErrorCode DISTRIBUTION_ADDRESS_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0043");
    ErrorCode DISTRIBUTION_FEE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0044");

    ErrorCode HETEROGENEOUS_SIGNER_LIST_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0045");
    ErrorCode HETEROGENEOUS_INIT_DUPLICATION = ErrorCode.init(ModuleE.CV.getPrefix() + "_0046");
    ErrorCode HETEROGENEOUS_HAS_BEEN_INITIALIZED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0047");
    ErrorCode TX_DUPLICATION = ErrorCode.init(ModuleE.CV.getPrefix() + "_0048");

    ErrorCode PASSWORD_IS_WRONG = ErrorCode.init(ModuleE.CV.getPrefix() + "_0049");
    ErrorCode PASSWORD_FORMAT_WRONG = ErrorCode.init(ModuleE.CV.getPrefix() + "_0050");
    ErrorCode ASSET_ID_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0051");
    ErrorCode ACCOUNT_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0052");
    ErrorCode HETEROGENEOUS_TRANSACTION_COMPLETED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0053");
    ErrorCode HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY = ErrorCode.init(ModuleE.CV.getPrefix() + "_0054");
    ErrorCode HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0055");
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_1 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0056");//待加入中存在地址-已经是管理员
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_2 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0057");//重复的待加入地址列表
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_3 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0058");//待退出中存在地址-不是管理员
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_4 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0059");//重复的待退出地址列表
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_5 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0060");//Maximum 15 managers
    ErrorCode HETEROGENEOUS_MANAGER_CHANGE_ERROR_6 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0061");//退出的管理员不能参与管理员变更交易
    ErrorCode ASSET_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0062");
    ErrorCode REG_ASSET_INFO_INCONSISTENCY = ErrorCode.init(ModuleE.CV.getPrefix() + "_0063");
    ErrorCode HETEROGENEOUS_CHAIN_NAME_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0064");
    ErrorCode HETEROGENEOUS_INVOK_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0065");
    ErrorCode SIGNATURE_BYZANTINE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0066");
    ErrorCode COINDATA_CANNOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0067");
    ErrorCode PROPOSAL_VOTE_RANGE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0068");
    ErrorCode PROPOSAL_HETEROGENEOUS_TX_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0069");
    ErrorCode PROPOSAL_HETEROGENEOUS_TX_AMOUNT_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0070");
    ErrorCode ADDRESS_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0071");
    ErrorCode PROPOSAL_CONTENT_EMPTY = ErrorCode.init(ModuleE.CV.getPrefix() + "_0072");
    ErrorCode PROPOSAL_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0073");
    ErrorCode VOTE_CHOICE_ERROR = ErrorCode.init(ModuleE.CV.getPrefix() + "_0074");
    ErrorCode VOTING_STOPPED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0075");
    ErrorCode VOTER_SIGNER_MISMATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0076");
    ErrorCode DUPLICATE_VOTE = ErrorCode.init(ModuleE.CV.getPrefix() + "_0077");
    ErrorCode NO_VOTING_RIGHTS = ErrorCode.init(ModuleE.CV.getPrefix() + "_0078");

    ErrorCode PAUSE_NEWTX = ErrorCode.init(ModuleE.CV.getPrefix() + "_0080");

    ErrorCode PROPOSAL_REJECTED= ErrorCode.init(ModuleE.CV.getPrefix() + "_0081");
    ErrorCode ADDRESS_LOCKED= ErrorCode.init(ModuleE.CV.getPrefix() + "_0082");
    ErrorCode ADDRESS_UNLOCKED= ErrorCode.init(ModuleE.CV.getPrefix() + "_0083");
    ErrorCode DISQUALIFICATION_FAILED= ErrorCode.init(ModuleE.CV.getPrefix() + "_0084");

    ErrorCode PROPOSAL_EXECUTIVE_FAILED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0084");
    ErrorCode SIGNER_NOT_SEED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0085");
    ErrorCode RESET_TX_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0086");
    ErrorCode HETEROGENEOUS_ASSET_NOT_FOUND = ErrorCode.init(ModuleE.CV.getPrefix() + "_0087");
    ErrorCode WITHDRAWAL_CONFIRMED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0088");
    ErrorCode AGENT_IS_NOT_SEED_VIRTUAL_BANK = ErrorCode.init(ModuleE.CV.getPrefix() + "_0089");

    ErrorCode NODE_NOT_IN_RUNNING = ErrorCode.init(ModuleE.CV.getPrefix() + "_0090");

    ErrorCode NO_LONGER_SUPPORTED = ErrorCode.init(ModuleE.CV.getPrefix() + "_0091");
    ErrorCode DUPLICATE_BIND = ErrorCode.init(ModuleE.CV.getPrefix() + "_0092");
    ErrorCode ASSET_ID_NOT_EXIST = ErrorCode.init(ModuleE.CV.getPrefix() + "_0093");
    ErrorCode HETEROGENEOUS_INFO_NOT_MATCH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0094");
    ErrorCode OVERRIDE_BIND_ASSET_NOT_FOUND = ErrorCode.init(ModuleE.CV.getPrefix() + "_0095");
    ErrorCode HIGH_GAS_PRICE_OF_ETH = ErrorCode.init(ModuleE.CV.getPrefix() + "_0096");

}
