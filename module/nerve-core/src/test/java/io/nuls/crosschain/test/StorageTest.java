package io.nuls.crosschain.test;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import org.junit.Test;

public class StorageTest {
    static String DB_PATH = "/Users/niels/workspace/nerve-network/data-main/cross-chain";
    static String DB_PATH2 = "/Users/niels/workspace/nerve-network/data/cross-chain";

    static final String TABLE = "registered_chain";

    @Test
    public  void readRegisterChain() throws NulsException {
        RocksDBService.init(DB_PATH);
        byte[] b = RocksDBService.get(TABLE, TABLE.getBytes());
        RegisteredChainMessage registeredChainMessage = new RegisteredChainMessage();
        registeredChainMessage.parse(b,0);
        Log.info("{}",registeredChainMessage);
    }

    @Test
    public void readRegisterChain2() throws NulsException {
        RocksDBService.init(DB_PATH2);
        byte[] b = RocksDBService.get(TABLE, TABLE.getBytes());
        RegisteredChainMessage registeredChainMessage = new RegisteredChainMessage();
        registeredChainMessage.parse(b, 0);
        Log.info("{}", registeredChainMessage);
    }
}
