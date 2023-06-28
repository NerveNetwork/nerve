package network.nerve.converter.constant;

import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;

/**
 * @author: Loki
 * @date: 2018/11/12
 */
public interface ConverterConstant {

    String CONVERTER_CMD_PATH = "network.nerve.converter.rpc.cmd";

    /**
     * 按2秒一个块 大约1天的出块数量
     */
    long DAY_BLOCKS = 30L * 60L * 24L;
    /**
     * system params
     */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String HETEROGENEOUS_MAINNET_CONFIG = "heterogeneous_mainnet.json";
    String HETEROGENEOUS_TESTNET_CONFIG = "heterogeneous_testnet.json";

    String RPC_VERSION = "1.0";

    /** nonce值初始值 */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /**
     * 查询等待调用组件的交易是否确认的重试次数
     */
    int CONFIRMED_VERIFY_COUNT = 3;

    /**
     * 异构链待处理队列的处理器
     */
    String CV_PENDING_THREAD = "cv_pending_thread";
    long CV_TASK_INITIALDELAY = 120;
    long CV_TASK_PERIOD = 3;
    String CV_PENDING_PROPOSAL_THREAD = "cv_pending_proposal_thread";
    /**
     * 收集交易签名队列处理器
     */
    String CV_SIGN_THREAD = "cv_sign_thread";
    long CV_SIGN_TASK_INITIALDELAY = 120;
    long CV_SIGN_TASK_PERIOD = 3;

    /**
     * 收集交易签名队列处理器
     */
    String CV_CHECK_THREAD = "cv_check_thread";
    long CV_CHECK_TASK_INITIALDELAY = 120;
    long CV_CHECK_TASK_PERIOD = 3;
    /**
     * 查询异构链余额队列处理器
     */
    String CV_HTG_BALANCE_THREAD = "cv_htg_balance_thread";
    long CV_HTG_BALANCE_TASK_INITIALDELAY = 120;
    long CV_HTG_BALANCE_TASK_PERIOD = 20;

    /** 统一所有链的主资产id */
    int ALL_MAIN_ASSET_ID = 1;

    int MAGIC_NUM_100 = 100;

    int MAX_CHECK_TIMES = 10;

    BigInteger DISTRIBUTION_FEE_10 = new BigInteger("1000000000");//10

    BigInteger DISTRIBUTION_FEE_100 = new BigInteger("10000000000");//100


    String CV_PROTOCOL_FILE = "cv-cfg-";

    int HETEROGENEOUS_VERSION_1 = 1;
    int HETEROGENEOUS_VERSION_2 = 2;

    String SEED_PUBKEY_SEPARATOR = ",";

    /**
     * 通过喂价模块获取价格
     */
    String ORACLE_KEY_NVT_PRICE = "NERVE_PRICE";

    int FIRST_HETEROGENEOUS_ASSET_CHAIN_ID = 101;
    byte[] ZERO_BYTES = new byte[]{0};
    long MINUTES_5 = 5 * 60 * 1000L;
    String IN = "in";
    String OUT = "out";
    int SIG_MODE_LOCAL = 0;
    int SIG_MODE_MACHINE = 1;
}
