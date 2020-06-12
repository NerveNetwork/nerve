package io.nuls.api.constant;

import io.nuls.api.model.po.StackSymbolPriceInfo;
import io.nuls.core.constant.TxType;

public interface DBTableConstant {

//    String DATABASE_NAME = "nuls-api";

    String DATABASE_NAME = "nuls-api";

    String TEST_TABLE = "test-table";

    String CHAIN_INFO_TABLE = "chain_info_table";

    String CHAIN_CONFIG_TABLE = "chain_config_table";

    String SYNC_INFO_TABLE = "sync_info_table";
    //区块信息表
    String BLOCK_HEADER_TABLE = "block_header_table_";

    String BLOCK_HEX_TABLE = "block_hex_table_";
    //账户信息表
    String ACCOUNT_TABLE = "account_table_";
    //资产信息表
    String ACCOUNT_LEDGER_TABLE = "account_ledger_table_";

    /**
     * 链上资产注册信息表
     * 用于记录通过链上注册交易({@link TxType}.LEDGER_ASSET_REG_TRANSFER)
     */
    String LEDGER_REG_ASSET_TABLE = "ledger_reg_aaset_table_";

    //共识节点信息表
    String AGENT_TABLE = "agent_table_";
    //别名信息表
    String ALIAS_TABLE = "alias_table_";
    //委托记录表
    String DEPOSIT_TABLE = "deposit_table_";
    //交易表
    String TX_TABLE = "tx_table_";
    //交易关系记录表
    String TX_RELATION_TABLE = "tx_relation_table_";
    //交易关系记录表
    String TX_UNCONFIRM_RELATION_TABLE = "tx_unconfirm_relation_table_";
    //交易表
    String TX_UNCONFIRM_TABLE = "tx_UNCONFIRM_table_";
    //coinData记录
    String COINDATA_TABLE = "coin_data_table_";
    //红黄牌记录表
    String PUNISH_TABLE = "punish_table_";

    String ROUND_TABLE = "round_table_";

    String ROUND_ITEM_TABLE = "round_item_table_";
    //账户token信息表
    String ACCOUNT_TOKEN_TABLE = "account_token_table_";
    //智能合约信息表
    String CONTRACT_TABLE = "contract_table_";
    //智能合约交易记录表
    String CONTRACT_TX_TABLE = "contract_tx_table_";
    //智能合约token转账记录表
    String TOKEN_TRANSFER_TABLE = "token_transfer_table_";
    //智能合约结果记录
    String CONTRACT_RESULT_TABLE = "contract_result_table_";
    //统计表
    String STATISTICAL_TABLE = "statistical_table_";

    /**
     * 跨链交易扩展表
     * 对tx_table表的扩展，用于存储跨链相关交易
     * @see io.nuls.api.model.po.ConverterTxInfo
     */
    String CONVERTER_TX_TABLE = "converter_tx_table_";

    /**
     * stack快照表
     * @see io.nuls.api.model.po.StackSnapshootInfo
     */
    String STACK_SNAPSHOOT_TABLE = "stack_snapshoot_table_";

    String CHAIN_STATISTICAL_TABLE = "chain_statistical_table";

    /**
     * 出块消耗时间统计表
     * @see io.nuls.api.model.po.BlockTimeInfo
     */
    String BLOCK_TIME_TABLE = "block_time_table";

    /**
     * 喂价最终确认表
     * @see StackSymbolPriceInfo
     */
    String SYMBOL_FINAL_QUOTATION_TABLE = "symbol_final_quotation_table";

    /**
     * 喂价提交记录表
     * @see io.nuls.api.model.po.SymbolQuotationRecordInfo
     */
    String SYMBOL_QUOTATION_RECORD_TABLE = "symbol_quotation_record_table";

    /**
     * 币种信息注册表
     * @see io.nuls.api.model.po.SymbolRegInfo
     */
    String SYMBOL_REG_TABLE = "symbol_reg_table";

    //---------------------------------字段(field)------------------------------
    //new_info表，最新统计的时间点
    String LAST_STATISTICAL_TIME = "last_statistical_time";

    String TX_COUNT = "txCount";
    String ANNUALIZE_REWARD = "annualizedReward";
    String CONSENSUS_LOCKED = "consensusLocked";

    //交易关系表分片数量
    int TX_RELATION_SHARDING_COUNT = 128;



}
