package io.nuls.core.constant;

/**
 * Transaction type
 *
 * @author captain
 * @version 1.0
 * @date 2019/5/24 18:47
 */
public class TxType {

    /**
     * coinBaseReward for block output
     */
    public static final int COIN_BASE = 1;
    /**
     * Transfer
     * the type of the transfer transaction
     */
    public static final int TRANSFER = 2;

    /**
     * Set account alias
     * Set the transaction type of account alias.
     */
    public static final int ACCOUNT_ALIAS = 3;
    /**
     * Create a new consensus node`
     */
    public static final int REGISTER_AGENT = 4;
    /**
     * Entrusting participation in consensus
     */
    public static final int DEPOSIT = 5;
    /**
     * Cancel delegation
     */
    public static final int CANCEL_DEPOSIT = 6;
    /**
     * Yellow card
     */
    public static final int YELLOW_PUNISH = 7;
    /**
     * Red card
     */
    public static final int RED_PUNISH = 8;
    /**
     * Unregister consensus node
     */
    public static final int STOP_AGENT = 9;
    /**
     * Cross chain transfer
     */
    public static final int CROSS_CHAIN = 10;

    /**
     * Registration Chain
     */
    public static final int REGISTER_CHAIN_AND_ASSET = 11;
    /**
     * Unregister Chain
     */
    public static final int DESTROY_CHAIN_AND_ASSET = 12;
    /**
     * Add an asset to the chain
     */
    public static final int ADD_ASSET_TO_CHAIN = 13;
    /**
     * Delete on chain assets
     */
    public static final int REMOVE_ASSET_FROM_CHAIN = 14;
    /**
     * Create a smart contract
     */
    public static final int CREATE_CONTRACT = 15;
    /**
     * Calling smart contracts
     */
    public static final int CALL_CONTRACT = 16;
    /**
     * Delete smart contract
     */
    public static final int DELETE_CONTRACT = 17;
    /**
     * Internal transfer of contract
     * contract transfer tx
     */
    public static final int CONTRACT_TRANSFER = 18;
    /**
     * Contract execution fee refund
     * contract return gas tx
     */
    public static final int CONTRACT_RETURN_GAS = 19;
    /**
     * Contract New Consensus Node
     * contract create agent tx
     */
    public static final int CONTRACT_CREATE_AGENT = 20;

    /**
     * Contract Entrustment Participation Consensus
     * contract deposit tx
     */
    public static final int CONTRACT_DEPOSIT = 21;

    /**
     * Contract cancellation commission consensus
     * contract withdraw tx
     */
    public static final int CONTRACT_CANCEL_DEPOSIT = 22;

    /**
     * Contract Cancellation Consensus Node
     * contract stop agent tx
     */
    public static final int CONTRACT_STOP_AGENT = 23;

    /**
     * Verifier change
     * Verifier Change
     */
    public static final int VERIFIER_CHANGE = 24;

    /**
     * Verifier initialization
     * Verifier init
     */
    public static final int VERIFIER_INIT = 25;

    /**
     * contracttokenCross chain transfer
     * contract token cross transfer tx
     */
    public static final int CONTRACT_TOKEN_CROSS_TRANSFER = 26;
    /**
     * Registration of assets within the ledger chain
     */
    public static final int LEDGER_ASSET_REG_TRANSFER = 27;


    /**
     * Additional node margin
     * Additional agent margin
     */
    public static final int APPEND_AGENT_DEPOSIT = 28;

    /**
     * Revoke node deposit
     * Cancel agent deposit
     */
    public static final int REDUCE_AGENT_DEPOSIT = 29;

    /**
     * Feed trading
     */
    public static final int QUOTATION = 30;

    /**
     * Final feed price transaction
     */
    public static final int FINAL_QUOTATION = 31;

    /**
     * Batch exitstakingtransaction
     */
    public static final int BATCH_WITHDRAW = 32;

    /**
     * Merge current accountsstakingrecord
     */
    public static final int BATCH_STAKING_MERGE = 33;

    /**
     * Create transaction pairs
     */
    public static final int COIN_TRADING = 228;

    /**
     * Order commission
     */
    public static final int TRADING_ORDER = 229;

    /**
     * Order cancellation
     */
    public static final int TRADING_ORDER_CANCEL = 230;

    /**
     * Listing transaction
     */
    public static final int TRADING_DEAL = 231;

    /**
     * Modify transaction pairs
     */
    public static final int EDIT_COIN_TRADING = 232;

    /**
     * Confirmation of cancellation transaction
     */
    public static final int ORDER_CANCEL_CONFIRM = 233;

    /**
     * confirm Virtual Bank Change Transaction
     */
    public static final int CONFIRM_CHANGE_VIRTUAL_BANK = 40;

    /**
     * Virtual Bank Change Transaction
     */
    public static final int CHANGE_VIRTUAL_BANK = 41;

    /**
     * On chain recharge transactions
     */
    public static final int RECHARGE = 42;

    /**
     * Withdrawal transactions
     */
    public static final int WITHDRAWAL = 43;

    /**
     * Confirm successful withdrawal status transaction
     */
    public static final int CONFIRM_WITHDRAWAL = 44;

    /**
     * Initiate proposal transactions
     */
    public static final int PROPOSAL = 45;

    /**
     * Vote on proposals for trading
     */
    public static final int VOTE_PROPOSAL = 46;

    /**
     * Subsidy for transaction fees for heterogeneous chain transactions
     */
    public static final int DISTRIBUTION_FEE = 47;

    /**
     * Virtual Bank Initialize Heterogeneous Chain
     */
    public static final int INITIALIZE_HETEROGENEOUS = 48;
    /**
     * Heterogeneous chain contract asset registration waiting
     */
    public static final int HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING = 49;
    /**
     * Heterogeneous chain contract asset registration completed
     */
    public static final int HETEROGENEOUS_CONTRACT_ASSET_REG_COMPLETE = 50;
    /**
     * Confirm proposal execution transaction
     */
    public static final int CONFIRM_PROPOSAL = 51;
    /**
     * Reset heterogeneous chains(contract)Virtual banking
     */
    public static final int RESET_HETEROGENEOUS_VIRTUAL_BANK = 52;
    /**
     * Confirm resetting heterogeneous chains(contract)Virtual banking
     */
    public static final int CONFIRM_HETEROGENEOUS_RESET_VIRTUAL_BANK = 53;
    /**
     * Heterogeneous chain recharge pending confirmation transaction
     */
    public static final int RECHARGE_UNCONFIRMED = 54;
    /**
     * Heterogeneous chain lifting has been released to heterogeneous chain networks
     */
    public static final int WITHDRAWAL_HETEROGENEOUS_SEND = 55;
    /**
     * Additional withdrawal handling fee
     */
    public static final int WITHDRAWAL_ADDITIONAL_FEE = 56;
    /**
     * Heterogeneous Chain Master Asset Registration
     */
    public static final int HETEROGENEOUS_MAIN_ASSET_REG = 57;
    /**
     * Registered cross chain chain information change
     */
    public static final int REGISTERED_CHAIN_CHANGE = 60;

    /**
     * establishswapTransaction pairs
     */
    public static final int CREATE_SWAP_PAIR = 61;

    /**
     * Creating a mining pool
     */
    public static final int FARM_CREATE = 62;

    /**
     * swaptransaction
     */
    public static final int SWAP_TRADE = 63;

    /**
     * Add liquidity
     */
    public static final int SWAP_ADD_LIQUIDITY = 64;

    /**
     * Revoke liquidity
     */
    public static final int SWAP_REMOVE_LIQUIDITY = 65;

    /**
     * Pledge mining
     */
    public static final int FARM_STAKE = 66;

    /**
     * Withdrawal of pledge
     */
    public static final int FARM_WITHDRAW = 67;
    /**
     * SWAPSystem transaction
     */
    public static final int SWAP_SYSTEM_DEAL = 68;
    /**
     * SWAPSystem refund transaction
     */
    public static final int SWAP_SYSTEM_REFUND = 69;

    /**
     * FARMSystem transactions
     */
    public static final int FARM_SYSTEM_TX = 70;

    /**
     * Create stablecoins pair
     */
    public static final int CREATE_SWAP_PAIR_STABLE_COIN = 71;

    /**
     * swapStablecoin trading
     */
    public static final int SWAP_TRADE_STABLE_COIN = 72;

    /**
     * Add stablecoin liquidity
     */
    public static final int SWAP_ADD_LIQUIDITY_STABLE_COIN = 73;

    /**
     * Revoke the liquidity of stablecoins
     */
    public static final int SWAP_REMOVE_LIQUIDITY_STABLE_COIN = 74;

    /**
     * farmInformation updates
     */
    public static final int FARM_UPDATE = 75;
    /**
     * Reset Chain Information
     */
    public static final int RESET_CHAIN_INFO = 76;
    /**
     * Aggregation of stablecoin liquidity assetsswaptransaction
     */
    public static final int SWAP_STABLE_LP_SWAP_TRADE = 77;
    /**
     * Lock account
     */
    public static final int BLOCK_ACCOUNT = 78;
    /**
     * Unlock account
     */
    public static final int UNBLOCK_ACCOUNT = 79;
    /**
     * One click cross chain pending confirmation transaction
     */
    public static final int ONE_CLICK_CROSS_CHAIN_UNCONFIRMED = 80;
    /**
     * One click cross chain
     */
    public static final int ONE_CLICK_CROSS_CHAIN = 81;
    /**
     * Cross chain additional cross chain handling fee transaction
     */
    public static final int ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN = 82;
    /**
     * swapTransaction aggregation stablecoin revoking liquidity transactions
     */
    public static final int SWAP_TRADE_SWAP_STABLE_REMOVE_LP = 83;
    /**
     * Heterogeneous Chain Master Asset BindingNERVEAsset trading
     */
    public static final int HETEROGENEOUS_MAIN_ASSET_BIND = 84;
    public static final int WITHDRAWAL_UTXO = 85;
    public static final int WITHDRAWAL_UTXO_FEE_PAYMENT = 86;
    public static final int GENERAL_BUS = 87;
    public static final int UNLOCK_TRANSFER = 88;

}
