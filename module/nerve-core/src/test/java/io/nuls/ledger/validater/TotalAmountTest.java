package io.nuls.ledger.validater;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;

/**
 * @author Niels
 */
public class TotalAmountTest {

    @Test
    public void test() throws Exception {
        RocksDBManager.init("/Users/niels/workspace/nerve-network/data/ledger");
        List<Entry<byte[], byte[]>> list = RocksDBManager.entryList("account_9");
        BigInteger total = list.stream().map(d -> {
            try {
                String key = new String(d.getKey(), "UTF8");
                String[] keyAry = key.split("-");
                byte[] address = AddressTool.getAddress("NULSd" + keyAry[0]);
                if (AddressTool.getChainIdByAddress(address) != 9) {
                    return BigInteger.ZERO;
                }
                if ("1".equals(keyAry[1]) && "1".equals(keyAry[2]) && !"pb63T1M8JgQ26jwZpZXYL8ZMLdUAK31L".equals(keyAry[0])) {
                    io.nuls.ledger.model.po.AccountState accountState = new io.nuls.ledger.model.po.AccountState();
                    accountState.parse(d.getValue(), 0);
                    return accountState.getTotalAmount();
                } else {
                    return BigInteger.ZERO;
                }
            } catch (UnsupportedEncodingException | NulsException e) {
                return BigInteger.ZERO;
            }
        }).reduce(BigInteger::add).orElse(BigInteger.ZERO);
        Log.info("total::::{}", total.toString());
    }
}
