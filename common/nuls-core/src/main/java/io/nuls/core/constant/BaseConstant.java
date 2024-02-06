package io.nuls.core.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tag
 */
public class BaseConstant {
    /**
     * Default for main and test networkschainID
     */
    public static final short MAINNET_CHAIN_ID = 1;
    public static final String MAINNET_DEFAULT_ADDRESS_PREFIX = "NULS";

    public static final short TESTNET_CHAIN_ID = 2;
    public static final String TESTNET_DEFAULT_ADDRESS_PREFIX = "tNULS";

    public static final short NERVE_MAINNET_CHAIN_ID = 9;
    public static final String NERVE_MAINNET_DEFAULT_ADDRESS_PREFIX = "NERVE";

    public static final short NERVE_TESTNET_CHAIN_ID = 5;
    public static final String NERVE_TESTNET_DEFAULT_ADDRESS_PREFIX = "TNVT";

    /**
     * hash length
     */
    public static final int ADDRESS_LENGTH = 23;

    /**
     * The default address type, a chain can contain several address types, which are included in the address
     * The default address type, a chain can contain several address types, and the address type is contained in the address.
     */
    public static byte DEFAULT_ADDRESS_TYPE = 1;

    /**
     * Smart contract address type
     * contract address type
     */
    public static byte CONTRACT_ADDRESS_TYPE = 2;

    /**
     * Multi signature address
     * contract address type
     */
    public static byte P2SH_ADDRESS_TYPE = 3;

    /**
     * SWAPTransaction to address
     */
    public static byte PAIR_ADDRESS_TYPE = 4;
    /**
     * SWAPPledge Pool Address
     */
    public static byte FARM_ADDRESS_TYPE = 5;
    /**
     * STABLE-SWAPTransaction to address
     */
    public static byte STABLE_PAIR_ADDRESS_TYPE = 6;

    /**
     * The running version of the main network, default to1Will make modifications based on the latest version of the block updated to the wallet
     */
    public static volatile Integer MAIN_NET_VERSION = 1;

    /**
     * utxoLock Time Boundary Value
     * Less than this value indicates locking according to height
     * A value greater than this indicates locking according to time
     */
    public static long BlOCKHEIGHT_TIME_DIVIDE = 1000000000000L;

    /**
     * Block output interval time（second）
     * Block interval time.
     * unit:second
     */
    public static long BLOCK_TIME_INTERVAL_SECOND = 10;
    /**
     * Module Unified Message ProcessorRPCinterface
     */
    public static final String MSG_PROCESS = "msgProcess";

    /**
     * Module Unified Transaction VerifierRPCinterface
     */
    public static final String TX_VALIDATOR = "txValidator";

    /**
     * Module Unified Transaction SubmissionRPCinterface
     */
    public static final String TX_COMMIT = "txCommit";

    /**
     * Module Unified Transaction RollbackRPCinterface
     */
    public static final String TX_ROLLBACK = "txRollback";

    /**
     * Module Unified Transaction Packaging Processing Interface
     */
    public static final String TX_PACKPRODUCE = "txPackProduce";

    public static final List<String> NERVE_CORE_DOMAINS = new ArrayList<>();
}
