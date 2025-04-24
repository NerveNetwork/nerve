package network.nerve.hetool.constant;

import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;

/**
 * @author: Loki
 * @date: 2018/11/12
 */
public interface Constant {

    String CMD_PATH = "network.nerve.hetool.rpc.cmd";

    /**
     * system params
     */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";
    String RPC_VERSION = "1.0";
    String BUILD_TBC_WITHDRAW_TBC = "ht_buildTbcWithdrawTBC";
    String FINISH_TBC_WITHDRAW_TBC = "ht_finishTbcWithdrawTBC";
    String BUILD_TBC_WITHDRAW_FT = "ht_buildTbcWithdrawFT";
    String FINISH_TBC_WITHDRAW_FT = "ht_finishTbcWithdrawFT";
    String FETCH_FT_PREPRETXDATA = "ht_fetchFtPrePreTxData";

    int PUB_LENGTH = 33;

    /** nonceInitial value */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    byte[] ZERO_BYTES = new byte[]{0};

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
