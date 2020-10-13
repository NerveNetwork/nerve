package network.nerve.converter.model.bo;


import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HeterogeneousSignTest {

    HeterogeneousSign sign;
    @Before
    public void setUp() throws Exception {
        sign = new HeterogeneousSign();
        HeterogeneousAddress address1 = new HeterogeneousAddress(108, "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        sign.setHeterogeneousAddress(address1);

    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = sign.serialize();
        HeterogeneousSign nwSign = ConverterUtil.getInstance(bytes, HeterogeneousSign.class);
        assertEquals(sign.getHeterogeneousAddress(), nwSign.getHeterogeneousAddress());
        assertArrayEquals(sign.getSignature(), nwSign.getSignature());

    }
}
