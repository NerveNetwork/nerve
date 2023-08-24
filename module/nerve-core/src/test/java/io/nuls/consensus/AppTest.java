package io.nuls.consensus;

import static org.junit.Assert.assertTrue;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void test() throws NulsException {
        String hex = "0500dff7c164003d00e87648170000000000000000000000000000000000000000000000000000000500010e2939dd6d077bd7ad8f86b454dbe1dcbc05473d0500010000008c01170500010e2939dd6d077bd7ad8f86b454dbe1dcbc05473d0500010000e876481700000000000000000000000000000000000000000000000000000008df8c5c3503e575f90001170500010e2939dd6d077bd7ad8f86b454dbe1dcbc05473d0500010000e8764817000000000000000000000000000000000000000000000000000000ffffffffffffffff00";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(hex), 0);
        System.out.println();
    }

    @Test
    public void addressTest() throws Exception {
        List<String> list = new ArrayList();
        list.add("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        list.add("TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5");
        list.add("NERVEepb6hz6zSAPu7YkxyM68M6omUkwAJpHmt");
        list.add("NULSd6HgkvPrGrBnFAVXUBhBSTE7LqkY5u3g9");
        for (String addr : list) {
            byte[] addrBytes = AddressTool.getAddress(addr);
            System.out.println();
        }
    }
}
