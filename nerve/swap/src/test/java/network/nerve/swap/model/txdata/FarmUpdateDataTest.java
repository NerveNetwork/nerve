package network.nerve.swap.model.txdata;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import junit.framework.TestCase;

import java.math.BigInteger;

public class FarmUpdateDataTest extends TestCase {

    public void testParse() throws Exception {

        FarmUpdateData data = new FarmUpdateData();
        data.setFarmHash(NulsHash.calcHash("aaa".getBytes()));
        data.setChangeTotalSyrupAmount(BigInteger.ZERO);
        data.setStopHeight(10000);
        data.setChangeType((short) 0);
        data.setWithdrawLockTime(1111);
        data.setNewSyrupPerBlock(BigInteger.ONE);
        data.setSyrupLockTime(1000);
        byte[] bytes = data.serialize();
        FarmUpdateData data2 = new FarmUpdateData();
        data2.parse(bytes, 0);
        System.out.println(data2.getSyrupLockTime());
    }
}