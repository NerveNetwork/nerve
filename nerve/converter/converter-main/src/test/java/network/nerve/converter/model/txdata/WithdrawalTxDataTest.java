package network.nerve.converter.model.txdata;

import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WithdrawalTxDataTest {

    WithdrawalTxData withdrawalTxData;

    @Before
    public void setUp() throws Exception {
        withdrawalTxData = new WithdrawalTxData();
        //withdrawalTxData.setChainId(3);
        withdrawalTxData.setHeterogeneousAddress("0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = withdrawalTxData.serialize();
        WithdrawalTxData newObj = ConverterUtil.getInstance(bytes, WithdrawalTxData.class);
        assertNotNull(newObj);
        //assertEquals(newObj.getChainId(), withdrawalTxData.getChainId());
        assertEquals(newObj.getHeterogeneousAddress(), withdrawalTxData.getHeterogeneousAddress());
    }
}