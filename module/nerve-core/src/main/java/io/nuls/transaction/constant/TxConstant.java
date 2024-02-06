package io.nuls.transaction.constant;

import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;

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

    /** New transaction thread name*/
    String TX_THREAD = "newNetTxThread";
    /** Orphan transaction processing thread name*/
    String TX_ORPHAN_THREAD = "orphanTxThread";
    /** Unconfirmed transaction cleaning mechanism thread name */
    String TX_CLEAN_THREAD = "cleanTxThread";
    /** Verify transaction signature thread */
    String VERIFY_TX_SIGN_THREAD = "verifyTxSignThread";

    /** Orphan transaction processingtask, Initial delay value(second) */
    int TX_ORPHAN_TASK_INITIALDELAY = 1;
    /** Orphan transaction processingtask, Run cycle interval(second) */
    int TX_ORPHAN_TASK_PERIOD = 3;

    /** Unconfirmed transaction clearance mechanismtask,Initial delay value */
    int TX_CLEAN_TASK_INITIALDELAY = 60;
    /** Unconfirmed transaction clearance mechanismtask, Run cycle interval(second) */
    int TX_CLEAN_TASK_PERIOD = 10;

    /** The maximum number of times an orphan transaction can be reprocessed in the waiting queue during packaging. If this number is exceeded, the orphan transaction will no longer be processed(discard) */
    int PACKAGE_ORPHAN_MAXCOUNT = 3;
    int PACKAGE_ORPHAN_MAP_MAXCOUNT = 1000;
    /** When processing new transactions on the network, retrieve the maximum value of the new transaction from the set to be processed at once */
    int NET_TX_PROCESS_NUMBER_ONCE = 1000;

    /** The maximum capacity of receiving new transaction queues in the network Unprocessed transaction queue**/
    int TX_UNVERIFIED_QUEUE_SIZE = 10000;


    /** MapInitial value */
    int INIT_CAPACITY_32 = 32;
    int INIT_CAPACITY_16 = 16;
    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /** nonceInitial value */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    int CACHED_SIZE = 5000;

    /** Store transactions in the queue to be packagedmap All transactionssize Maximum limit (B)*/
    int PACKABLE_TX_MAP_STRESS_DATA_SIZE = 40000 * 300;
    int PACKABLE_TX_MAP_HEAVY_DATA_SIZE = 50000 * 300;
    int PACKABLE_TX_MAP_MAX_DATA_SIZE = 70000 * 300;

    int ORPHAN_LIST_MAX_DATA_SIZE = 5000 * 300;

    long REQUEST_TIME_OUT = 600 * 1000L;

    /** Basic packaging parameter configuration Excluding smart contracts**/
    /** Maximum number of transactions packaged **/
    int BASIC_PACKAGE_TX_MAX_COUNT = 1200;
    /** Reserve time for module validation during packaging **/
    long BASIC_PACKAGE_RESERVE_TIME = 400L;
    /** The maximum number of allowed cross chain module transactions in a block*/
    int BASIC_PACKAGE_CROSS_TX_MAX_COUNT = 50;
    /** When packaging,Verify the quantity of a batch in the ledger*/
    int BASIC_PACKAGE_VERIFY_COINDATA_BATCH = 800;
    /**rpcReserve time **/
    long BASIC_PACKAGE_RPC_RESERVE_TIME = 20L;

    String BASIC_VERIFY_TX_SIGN_THREAD = "basicVerifyTxSignThread";

    int PACKING = 0;

    int VERIFY = 1;


    String TX_PROTOCOL_FILE = "tx-cfg-";

    BigInteger MAX_SUPPORT_AMOUNT = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

}
