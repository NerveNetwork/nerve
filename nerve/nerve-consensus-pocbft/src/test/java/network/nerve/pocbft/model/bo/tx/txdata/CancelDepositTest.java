package network.nerve.pocbft.model.bo.tx.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

/**
 * @author Niels
 */
public class CancelDepositTest extends TestCase {
    @Test
    public void test() throws IOException {
        int[] arr = new int[]{1, 2, 5, 9};
        for (int i = 0; i < 100; i++) {
            byte[] address = AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]);
            CancelDeposit withdraw = new CancelDeposit();
            withdraw.setAddress(address);
            withdraw.setJoinTxHash(NulsHash.calcHash(UUID.randomUUID().toString().getBytes()));
            System.out.println(HexUtil.encode(withdraw.serialize()));
        }
    }
}