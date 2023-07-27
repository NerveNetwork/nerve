package io.nuls.consensus.v1.message;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Eva
 */
public class GetVoteResultMessageTest extends TestCase {
    @Test
    public void test() throws IOException, NulsException {
        GetVoteResultMessage message = new GetVoteResultMessage(NulsHash.calcHash("ahahasdf".getBytes()));
        System.out.println(message.getBlockHash().toHex());

        byte[] bytes = message.serialize();

        GetVoteResultMessage msg = new GetVoteResultMessage();
        msg.parse(bytes,0);
        System.out.println(msg.getBlockHash().toHex());

    }


}