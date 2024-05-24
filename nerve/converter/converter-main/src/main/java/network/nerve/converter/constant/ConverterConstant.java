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
     * according to2One block per second about1Number of blocks produced per day
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

    /** nonceInitial value */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /**
     * The number of retries to query whether the transaction waiting to call the component is confirmed
     */
    int CONFIRMED_VERIFY_COUNT = 3;

    /**
     * The processor for processing queues in heterogeneous chains
     */
    String CV_PENDING_THREAD = "cv_pending_thread";
    long CV_TASK_INITIALDELAY = 120;
    long CV_TASK_PERIOD = 3;
    String CV_PENDING_PROPOSAL_THREAD = "cv_pending_proposal_thread";
    /**
     * Collect transaction signature queue processor
     */
    String CV_SIGN_THREAD = "cv_sign_thread";
    long CV_SIGN_TASK_INITIALDELAY = 120;
    long CV_SIGN_TASK_PERIOD = 3;

    /**
     * Collect transaction signature queue processor
     */
    String CV_CHECK_THREAD = "cv_check_thread";
    long CV_CHECK_TASK_INITIALDELAY = 120;
    long CV_CHECK_TASK_PERIOD = 3;
    /**
     * Query heterogeneous chain balance queue processor
     */
    String CV_HTG_BALANCE_THREAD = "cv_htg_balance_thread";
    long CV_HTG_BALANCE_TASK_INITIALDELAY = 120;
    long CV_HTG_BALANCE_TASK_PERIOD = 20;

    /** Unify the main assets of all chainsid */
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
     * Obtain prices through the pricing module
     */
    String ORACLE_KEY_NVT_PRICE = "NERVE_PRICE";

    int FIRST_HETEROGENEOUS_ASSET_CHAIN_ID = 101;
    byte[] ZERO_BYTES = new byte[]{0};
    long MINUTES_5 = 5 * 60 * 1000L;
    String IN = "in";
    String OUT = "out";
    int SIG_MODE_LOCAL = 0;
    int SIG_MODE_MACHINE = 1;
    int PUB_LENGTH = 33;

    long BTC_DUST_AMOUNT = 546;
    String BTC_WITHDRAW_FEE_TX_HASH_PREFIX = "888888888888888888";
    String BTC_ADDING_FEE_WITHDRAW_REBUILD_MARK = "020000";
    String BTC_ADDING_FEE_CHANGE_REBUILD_MARK = "020001";
}
