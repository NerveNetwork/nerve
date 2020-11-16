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
public class ChangeAgentDepositDataTest extends TestCase {
    @Test
    public void test() throws IOException {
        int[] arr = new int[]{1, 2, 5, 9};
        for (int i = 0; i < 100; i++) {
            ChangeAgentDepositData data = new ChangeAgentDepositData();
            data.setAmount(BigInteger.valueOf(200000*i));
            data.setAddress(AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]));
            data.setAgentHash(NulsHash.calcHash(UUID.randomUUID().toString().getBytes()));
            System.out.println(HexUtil.encode(data.serialize()));
        }
    }
}