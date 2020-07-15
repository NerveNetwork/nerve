package network.nerve.pocbft.v1.message;

import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eva
 */
public class VoteResultMessageTest extends TestCase {
    @Test
    public void test() throws IOException, NulsException {
        VoteResultMessage message = new VoteResultMessage(null,getList());

        System.out.println(HexUtil.encode(message.getSignList().get(1)));

        byte[] bytes = message.serialize();

        VoteResultMessage msg = new VoteResultMessage();
        msg.parse(bytes, 0);
        System.out.println(HexUtil.encode(msg.getSignList().get(5)));

    }

    private List<VoteMessage> getList() {
        List<VoteMessage> list = new ArrayList<VoteMessage>();
        VoteMessage message = new VoteMessage();
        message.setRoundStartTime(1000000);
        message.setSign("asdfasdfasdf".getBytes());
        message.setVoteStage((byte) 1);
        message.setPackingIndexOfRound(2);
        message.setRoundIndex(234);
        message.setHeight(1234);
        message.setBlockHash(NulsHash.calcHash("aswdfasdf".getBytes()));
        message.setVoteRoundIndex(2);
        list.add(message);
        list.add(message);
        list.add(message);
        list.add(message);
        list.add(message);
        list.add(message);
        return list;
    }
}