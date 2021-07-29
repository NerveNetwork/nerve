package network.nerve.swap.constant;

import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;

/**
 * @author: Loki
 * @date: 2018/11/12
 */
public interface SwapConstant {

    String INITIAL_STATE_ROOT = "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421";
    String SWAP_CMD_PATH = "network.nerve.swap.rpc.cmd";
    /**
     * system params
     */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String RPC_VERSION = "1.0";

    /** nonce值初始值 */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    byte[] ZERO_BYTES = new byte[]{0};
    byte PAIR_ADDRESS_TYPE = 4;
    byte FARM_ADDRESS_TYPE = 5;
    byte STABLE_PAIR_ADDRESS_TYPE = 6;

    int LP_TOKEN_DECIMALS = 18;
    BigInteger MINIMUM_LIQUIDITY = BigInteger.valueOf(1000);

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    String LINE = "_";
    // TNVTdTSQndufx1WRqrFhW9BESjsrJVKiRhcXE
    // NERVEepb7Z9BLexJY2DDxvzR7djDbU1TpWSjd2
    //String STABLE_PAIR_PUBKEY = "888888888888888888888888888888888888888888888888888888888888888888";
    String STABLE_PAIR = "STABLE_PAIR_";

    BigInteger BI_1E12 = BigInteger.valueOf(1000000000000L);
    BigInteger BI_100 = BigInteger.valueOf(100);
    BigInteger BI_1000_000 = BigInteger.valueOf(1000000);
    BigInteger BI_1000 = BigInteger.valueOf(1000);
    BigInteger BI_997 = BigInteger.valueOf(997);
    BigInteger BI_2 = BigInteger.valueOf(2);
    BigInteger BI_3 = BigInteger.valueOf(3);
    BigInteger BI_50 = BigInteger.valueOf(50);
    BigInteger BI_7 = BigInteger.valueOf(7);
    BigInteger BI_10 = BigInteger.valueOf(10);


}
