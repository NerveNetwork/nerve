package network.nerve.converter.message.heterogeneous;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ComponentSignMessageTest {

    ComponentSignMessage msg;
    @Before
    public void setUp() throws Exception {
        msg = new ComponentSignMessage();
        msg.setHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));


        HeterogeneousSign sign = new HeterogeneousSign();
        HeterogeneousAddress address1 = new HeterogeneousAddress(108, "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        sign.setHeterogeneousAddress(address1);
        List<HeterogeneousSign> list = new ArrayList<>();
        list.add(sign);
        msg.setListSign(list);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = msg.serialize();
        ComponentSignMessage nwMsg = ConverterUtil.getInstance(bytes, ComponentSignMessage.class);
        assertEquals(msg.getHash(), nwMsg.getHash());
        assertEquals(msg.getListSign().size(), nwMsg.getListSign().size());
        assertEquals(msg.getListSign().get(0).getHeterogeneousAddress(), nwMsg.getListSign().get(0).getHeterogeneousAddress());
    }

}
