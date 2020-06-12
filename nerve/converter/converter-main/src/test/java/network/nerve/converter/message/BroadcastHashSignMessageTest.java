package network.nerve.converter.message;

import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.P2PHKSignature;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BroadcastHashSignMessageTest {

    BroadcastHashSignMessage broadcastHashSignMessage;

    String sginHex = "2102b0a6f555e548a43fb7ddb6560b7edf099b1a649be66b3cebfb3cfc3c589f502a473045022100ae6ab360460d937168a53f176a0ab2aa207d11ff4dc0c45f250f323ca7509d9a022005e2b00839860648078125348f42a86857d71bb7673c94516381bde8e05b38cf";
    @Before
    public void setUp() throws Exception {
        broadcastHashSignMessage = new BroadcastHashSignMessage();
        broadcastHashSignMessage.setHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));
        P2PHKSignature p2PHKSignature = ConverterUtil.getInstance(sginHex, P2PHKSignature.class);
        broadcastHashSignMessage.setP2PHKSignature(p2PHKSignature);
        broadcastHashSignMessage.setOriginalHash("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc34eacb");
        broadcastHashSignMessage.setType(41);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        HeterogeneousHash h1 = new HeterogeneousHash(101, "0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
        HeterogeneousHash h2 = new HeterogeneousHash(101, "0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1fb38cf");
        HeterogeneousHash h3 = new HeterogeneousHash(102, "0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f03b1f");
        heterogeneousHashList.add(h1);
        heterogeneousHashList.add(h2);
        heterogeneousHashList.add(h3);
        broadcastHashSignMessage.setHeterogeneousHashList(heterogeneousHashList);

    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = broadcastHashSignMessage.serialize();
        BroadcastHashSignMessage newObj = ConverterUtil.getInstance(bytes, BroadcastHashSignMessage.class);
        assertNotNull(newObj);
        assertEquals(newObj.getHash(), broadcastHashSignMessage.getHash());
        assertEquals(newObj.getP2PHKSignature(), broadcastHashSignMessage.getP2PHKSignature());
        assertEquals(newObj.getOriginalHash(), broadcastHashSignMessage.getOriginalHash());
        assertEquals(newObj.getType(), broadcastHashSignMessage.getType());
        assertEquals(newObj.getHeterogeneousHashList().size(), broadcastHashSignMessage.getHeterogeneousHashList().size());
    }
}