package nerve.network.converter.message;

import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.P2PHKSignature;
import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

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
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = broadcastHashSignMessage.serialize();
        BroadcastHashSignMessage newObj = ConverterUtil.getInstance(bytes, BroadcastHashSignMessage.class);
        assertNotNull(newObj);
        assertEquals(newObj.getHash(), broadcastHashSignMessage.getHash());
        assertEquals(newObj.getP2PHKSignature(), broadcastHashSignMessage.getP2PHKSignature());
    }
}