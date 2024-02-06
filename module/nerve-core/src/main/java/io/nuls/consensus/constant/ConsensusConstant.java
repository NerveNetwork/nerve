package io.nuls.consensus.constant;

import java.math.BigInteger;

/**
 * Consensus module constant class
 *
 * @author tag
 * 2018/11/6
 */
public interface ConsensusConstant {

    /**
     * DB config
     */
    String DB_CONFIG_NAME = "db_config.properties";
    String DB_DATA_PATH = "rocksdb.datapath";
    String DB_DATA_DEFAULT_PATH = "rocksdb.datapath";


    /**
     * Consensus module related table name/Consensus module related table names
     */
    String DB_NAME_AGENT = "agent";
    String DB_NAME_APPEND_DEPOSIT = "append_agent_deposit";
    String DB_NAME_REDUCE_DEPOSIT = "reduce_agent_deposit";
    String DB_NAME_DEPOSIT = "deposit";
    String DB_NAME_PUNISH = "punish";
    String DB_NAME_CONFIG = "config";
    String DB_NAME_STAKING_LIMIT = "staking_limit";
    String DB_NAME_AWARD_SETTLE_RECORD = "award_settle_record";
    String DB_NAME_RANDOM_SEEDS = "random_seed";
    String DB_NAME_PUB_KEY = "pubKey";
    String DB_NAME_AGENT_DEPOSIT_NONCE = "agent_deposit_nonce";
    String DB_NAME_VIRTUAL_AGENT_CHANGE = "virtual_agent_change";

    byte[] EMPTY_SEED = new byte[32];
    /**
     * boot path
     */
    String BOOT_PATH = "io.nuls,network.nerve";

    /**
     * context path
     */
    String CONTEXT_PATH = "io.nuls.pocbft";

    /**
     * rpc file path
     */
    String RPC_PATH = "network.nerve.pocbft.rpc";


    /**
     * system params
     */
    String SYS_FILE_ENCODING = "file.encoding";

    /**
     * Load the block header of the last specified number of rounds during initialization
     * Load penalty information for the most recent specified number of rounds during initialization
     */
    int INIT_PUNISH_OF_ROUND_COUNT = 2100;

    /**
     * Cache block headers for specified rounds during system startup
     * Buffer a specified number of blocks at system startup
     */
    int INIT_BLOCK_HEADER_COUNT = 2100;

    /**
     * Same outbound address1000Presence within the wheel3The wheel emits two identical heights, but differenthashofblockNodes will be penalized with red cards
     */
    byte REDPUNISH_BIFURCATION = 3;

    /**
     * The minimum value of reputation value, less than or equal to this value will result in a red card penalty
     */
    double RED_PUNISH_CREDIT_VAL = -1D;

    /**
     * Consensus locking time
     */
    long CONSENSUS_LOCK_TIME = -1;

    /**
     * MapInitial value
     */
    int INIT_CAPACITY_2 = 2;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_16 = 16;
    int INIT_CAPACITY_32 = 32;
    int INIT_CAPACITY_64 = 64;

    /**
     * Interface version number
     */
    String RPC_VERSION = "1.0";

    /**
     * Interface call failure retry count
     */
    int RPC_CALL_TRY_COUNT = 5;

    byte VALUE_OF_ONE_HUNDRED = 100;

    String SEPARATOR = "_";

    String SEED_NODE_SEPARATOR = ",";

    /**
     * Maximum time difference allowed for unlocking transactions(S)
     */
    long UNLOCK_TIME_DIFFERENCE_LIMIT = 600;

    byte VOTE_STAGE_ONE = 1;
    byte VOTE_STAGE_TWO = 2;

    /**
     * index
     */
    long INIT_ROUND_INDEX = 2;

    String DATE_FORMAT = "yyyyMMdd";

    String DEFALT_KEY = "NERVE_PRICE";

    int ONE_DAY_SECONDS = 24 * 60 * 60;
    int ONE_DAY_MILLISECONDS = ONE_DAY_SECONDS * 1000;
    int HALF_DAY_MILLISECONDS = 12 * 60 * 60 * 1000;

    /**
     * Consensus Network Node Minimum proportion constant for normal block output60%
     */
    int POC_NETWORK_NODE_PERCENT = 20;
    short POC_CONNECT_MAX_FAIL_TIMES = 100;

    String STACKING_CONFIG_FILE = "staking-asset";

}
