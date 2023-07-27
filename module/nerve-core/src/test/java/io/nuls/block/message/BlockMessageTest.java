package io.nuls.block.message;

import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import org.junit.Test;

import java.io.IOException;

public class BlockMessageTest {

    @Test
    public void name() throws IOException {
        BlockMessage message = new BlockMessage();
        message.setBlock(null);
        message.setSyn(false);
        message.setRequestHash(NulsHash.fromHex("e26f981b73de6348d8f884570787d099b2e28ae95c8f4d994d5212fba89e251a"));

        System.out.println(HexUtil.encode(message.serialize()));
    }
}