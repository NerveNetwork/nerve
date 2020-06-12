package io.nuls.ledger.test.constant;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.sub.FreezeLockTimeState;
import io.nuls.ledger.utils.LedgerUtil;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TestConfig {
    public static int chainId = 1;
    public static int assetChainId = 2;
    public static int assetId = 1;


    @Test
    public void testParseAccountState() {

        AccountState accountState = new AccountState();
        Map<String, FreezeLockTimeState> map = new HashMap<>();

        FreezeLockTimeState state1 = new FreezeLockTimeState();
        state1.setTxHash("e9f3b5d90bf3249af74ade93d9075b3530e4596dbb0682d3ca910592add91af5");
        state1.setNonce(LedgerUtil.getNonceDecodeByTxHash(state1.getTxHash()));
        state1.setAmount(BigInteger.valueOf(12121212));
        state1.setLockTime(-2);
        state1.setCreateTime(123456);
        map.put(HexUtil.encode(state1.getNonce()), state1);

        FreezeLockTimeState state2 = new FreezeLockTimeState();
        state2.setTxHash("158b7ecd5fc2ee6e8da602b8b419f1986a063cc0fc2f6d9d7739837820053cb2");
        state2.setNonce(LedgerUtil.getNonceDecodeByTxHash(state2.getTxHash()));
        state2.setAmount(BigInteger.valueOf(656565));
        state2.setLockTime(-2);
        state2.setCreateTime(1234567890);
        map.put(HexUtil.encode(state2.getNonce()), state2);
        accountState.setPermanentLockMap(map);

        AccountState accountState1 = new AccountState();
        try {
            byte[] bytes = accountState.serialize();
            accountState1.parse(new NulsByteBuffer(bytes));
            System.out.println(accountState1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NulsException e) {
            e.printStackTrace();
        }
    }
}
