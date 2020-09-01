package io.nuls.transaction.constant;

import io.nuls.core.crypto.HexUtil;

/**
 * @author: Charlie
 * @date: 2020/06/25
 */
public interface TxConstant {

    String TX_CMD_PATH = "io.nuls.transaction.rpc.cmd";

    /** system params */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String RPC_VERSION = "1.0";

    /** 新交易线程名称*/
    String TX_THREAD = "newNetTxThread";
    /** 孤儿交易处理线程名称*/
    String TX_ORPHAN_THREAD = "orphanTxThread";
    /** 未确认交易清理机制线程名称 */
    String TX_CLEAN_THREAD = "cleanTxThread";
    /** 验证交易签名线程 */
    String VERIFY_TX_SIGN_THREAD = "verifyTxSignThread";

    /** 孤儿交易处理task, 初始延迟值(秒) */
    int TX_ORPHAN_TASK_INITIALDELAY = 1;
    /** 孤儿交易处理task, 运行周期间隔(秒) */
    int TX_ORPHAN_TASK_PERIOD = 3;

    /** 未确认交易清理机制task,初始延迟值 */
    int TX_CLEAN_TASK_INITIALDELAY = 60;
    /** 未确认交易清理机制task, 运行周期间隔(秒) */
    int TX_CLEAN_TASK_PERIOD = 10;

    /** 打包时孤儿交易返回待打包队列重新处理的最大次数，超过该次数则不再处理该孤儿交易(丢弃) */
    int PACKAGE_ORPHAN_MAXCOUNT = 3;
    int PACKAGE_ORPHAN_MAP_MAXCOUNT = 1000;
    /** 处理网络新交易时，一次从待处理集合中获取新交易的最大值 */
    int NET_TX_PROCESS_NUMBER_ONCE = 1000;

    /** 接收网络新交易队列的最大容量 未处理的交易队列**/
    int TX_UNVERIFIED_QUEUE_SIZE = 10000;


    /** Map初始值 */
    int INIT_CAPACITY_32 = 32;
    int INIT_CAPACITY_16 = 16;
    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /** nonce值初始值 */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    int CACHED_SIZE = 5000;

    /** 待打包队列存储交易的map 所有交易size 最大限制 (B)*/
    int PACKABLE_TX_MAP_STRESS_DATA_SIZE = 40000 * 300;
    int PACKABLE_TX_MAP_HEAVY_DATA_SIZE = 50000 * 300;
    int PACKABLE_TX_MAP_MAX_DATA_SIZE = 70000 * 300;

    int ORPHAN_LIST_MAX_DATA_SIZE = 5000 * 300;

    long REQUEST_TIME_OUT = 600 * 1000L;

    /** 基础打包参数配置 不包含智能合约**/
    /** 打包的最大交易数 **/
    int BASIC_PACKAGE_TX_MAX_COUNT = 1200;
    /** 打包时模块验证等预留时间 **/
    long BASIC_PACKAGE_RESERVE_TIME = 400L;
    /** 一个区块中最大允许跨链模块交易的数量*/
    int BASIC_PACKAGE_CROSS_TX_MAX_COUNT = 50;
    /** 打包时,账本验证一批次的数量*/
    int BASIC_PACKAGE_VERIFY_COINDATA_BATCH = 800;
    /**rpc预留时间 **/
    long BASIC_PACKAGE_RPC_RESERVE_TIME = 20L;

    String BASIC_VERIFY_TX_SIGN_THREAD = "basicVerifyTxSignThread";

    int PACKING = 0;

    int VERIFY = 1;


    String TX_PROTOCOL_FILE = "tx-cfg-";

}
