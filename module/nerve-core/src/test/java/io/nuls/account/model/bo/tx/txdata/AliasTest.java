package io.nuls.account.model.bo.tx.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Niels
 */
public class AliasTest extends TestCase {

    public void test() throws IOException {
        int[] arr = new int[]{1, 2, 5, 9};
        for (int i = 0; i < 100; i++) {
            byte[] address = AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]);
            Alias alias = new Alias(address, "aliasdddd");
            System.out.println(HexUtil.encode(alias.serialize()));
        }
    }
}