package network.nerve.pocbft.constant;

import java.math.BigInteger;

/**
 * 共识模块常量类
 * @author tag
 * 2018/11/6
 * */
public interface ConsensusConstant {

    /**
     * DB config
     * */
    String DB_CONFIG_NAME ="db_config.properties";
    String DB_DATA_PATH ="rocksdb.datapath";
    String DB_DATA_DEFAULT_PATH ="rocksdb.datapath";


    /**
     * Consensus module related table name/共识模块相关表名
     * */
    String DB_NAME_AGENT = "agent";
    String DB_NAME_APPEND_DEPOSIT = "append_agent_deposit";
    String DB_NAME_REDUCE_DEPOSIT= "reduce_agent_deposit";
    String DB_NAME_DEPOSIT = "deposit";
    String DB_NAME_PUNISH = "punish";
    String DB_NAME_CONFIG = "config";
    String DB_NAME_AWARD_SETTLE_RECORD = "award_settle_record";
    String DB_NAME_RANDOM_SEEDS = "random_seed";
    String DB_NAME_PUB_KEY = "pubKey";
    String DB_NAME_AGENT_DEPOSIT_NONCE = "agent_deposit_nonce";
    String DB_NAME_VIRTUAL_AGENT_CHANGE = "virtual_agent_change";

    byte[] EMPTY_SEED = new byte[32];
    /**
     * boot path
     * */
    String BOOT_PATH = "io.nuls,network.nerve";

    /**
     * context path
     * */
    String CONTEXT_PATH = "io.nuls.pocbft";

    /**
     * rpc file path
     * */
    String RPC_PATH = "network.nerve.pocbft.rpc";


    /**
     * system params
     * */
    String SYS_FILE_ENCODING = "file.encoding";

    /**
     * unit:round of consensus
     * 用于计算信誉值（表示只用最近这么多轮的轮次信息来计算信誉值）
     */
    int RANGE_OF_CAPACITY_COEFFICIENT = 100;

    /**
     * Penalty coefficient,greater than 4.
     */
    int CREDIT_MAGIC_NUM = 100;

    /**
     * Load the block header of the last specified number of rounds during initialization
     * 初始化时加载最近指定轮数的惩罚信息
     */
    int INIT_PUNISH_OF_ROUND_COUNT = 400;

    /**
     * 系统启动时缓存指定轮次的区块
     * Buffer a specified number of blocks at system startup
     * */
    int INIT_BLOCK_HEADER_COUNT = 110;

    /**
     * 同一个出块地址连续3轮发出两个相同高度，但不同hash的block，节点将会被红牌惩罚
     */
    byte REDPUNISH_BIFURCATION = 3;

    /**
     * 每出一个块获得的共识奖励，一年总的共识奖励金5000000，一年总出块数3154600,相除得到每一块的奖励金
     * value = 5000000/3154600
     */
    BigInteger YEAR_MILLISECOND = new BigInteger("31536000");

    /**
     * 信誉值的最小值，小于等于该值会给红牌处罚
     * */
    double RED_PUNISH_CREDIT_VAL = -1D;

    /**
     * 共识锁定时间
     * */
    long CONSENSUS_LOCK_TIME = -1;

    /**
     * lock of lockTime,(max of int48)(281474976710655L)
     */
    long LOCK_OF_LOCK_TIME = -1L ;

    /**
     * Map初始值
     * */
    int  INIT_CAPACITY_2 =2;
    int  INIT_CAPACITY_4 =4;
    int  INIT_CAPACITY_8 =8;
    int  INIT_CAPACITY_16 =16;
    int  INIT_CAPACITY_64 =64;

    /**
     * 接口版本号
     * */
    String RPC_VERSION = "1.0";

    /**
     * 轮次缓存数量
     * */
    int ROUND_CACHE_COUNT = 10;

    /**
     * 接口调用失败重试次数
     * */
    int RPC_CALL_TRY_COUNT = 5;

    byte VALUE_OF_ONE_HUNDRED =100;

    String SEPARATOR = "_";

    String SEED_NODE_SEPARATOR = ",";

    /**
     * 解锁交易允许的最大时间差(S)
     * */
    long UNLOCK_TIME_DIFFERENCE_LIMIT = 3600;

    byte VOTE_STAGE_ONE = 1;
    byte VOTE_STAGE_TWO = 2;

    byte VOTE_INIT_ROUND = 1;
    byte INIT_PACING_INDEX = 1;

    /**
     * 向其他节点获取数据超时时间（单位：S）
     * */
    int GET_DATA_TIME_OUT = 1;

    /**
     * 投票期间各阶段等待时间(ms)
     * */
    long VOTE_STAGE_ONE_WAIT_TIME = 500;
    long VOTE_STAGE_ONE_RESULT_WAIT_TIME = 800;
    /**
     * 第二阶段投票超时时间（秒）
     * */
    long VOTE_STAGE_TWO_TIME_OUT = 10 ;

    long WAIT_VOTE_RESULT_MIN_TIME = 100;
    /***
     * 区块投票轮次间隔时间
     */
    int VOTE_ROUND_INTERVAL_TIME = 2;

    byte FINAL_VOTE_ROUND_SIGN = 0;


    /**
     * 投票结果类型
     * */
    byte PREVIOUS_BLOCK = 4;
    byte PREVIOUS_ROUND = 0;
    byte CURRENT_ROUND = 1;
    byte CURRENT_BLOCK = 2;
    byte FUTURE = 3;


    long INIT_ROUND_INDEX = 2;

    /**
     * 区块投票信息缓存数
     * */
    int BLOCK_VOTE_CACHE_COUNT = 500;

    String DATE_FORMAT = "yyyyMMdd";

    String DEFALT_KEY = "NERVE_PRICE";

    int ONE_DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

    int HALF_DAY_MILLISECONDS = 12 * 60 * 60 * 1000;

    /**
     * 共识网络节点 正常出块最低比例常量60%
     * */
    int POC_NETWORK_NODE_PERCENT=66;
    short POC_CONNECT_MAX_FAIL_TIMES=20;

    String STACKING_CONFIG_FILE = "stacking-asset.json";

    /**
     * 节点重启时，等待投票轮次初始化时间(10S),如果这个时间内还未其他节点的投票信息，则初始化本地轮次
     */
    long WAIL_INIT_VOTE_ROUND_TIME = 10;
    /**
     * 节点重启时，初始化轮次间隔时间
     * */
    long INIT_CONSENSUS_ROUND_TIME = 60;
    /**
     * 节点启动需要在轮次30秒之后才计算轮次
     * */
    long INIT_CONSENSUS_ROUND_WAIT_TIME = 30;
 }
