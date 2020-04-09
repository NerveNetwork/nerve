package nerve.network.converter.model.txdata;

import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RechargeTxDataTest {


    RechargeTxData rechargeTxData;

    @Before
    public void setUp() throws Exception {
        rechargeTxData = new RechargeTxData();
        rechargeTxData.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = rechargeTxData.serialize();
        RechargeTxData newObj = ConverterUtil.getInstance(bytes, RechargeTxData.class);
        assertEquals(newObj.getHeterogeneousTxHash(), rechargeTxData.getHeterogeneousTxHash());
    }
}