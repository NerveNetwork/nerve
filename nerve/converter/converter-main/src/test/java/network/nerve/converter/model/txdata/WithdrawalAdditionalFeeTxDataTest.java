package network.nerve.converter.model.txdata;


import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WithdrawalAdditionalFeeTxDataTest {

    WithdrawalAdditionalFeeTxData withdrawalAdditionalFeeTxData;

    @Before
    public void setUp() throws Exception {
        withdrawalAdditionalFeeTxData = new WithdrawalAdditionalFeeTxData();
        withdrawalAdditionalFeeTxData.setTxHash("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d");
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = withdrawalAdditionalFeeTxData.serialize();
        WithdrawalAdditionalFeeTxData newObj = ConverterUtil.getInstance(bytes, WithdrawalAdditionalFeeTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getTxHash(), withdrawalAdditionalFeeTxData.getTxHash());
    }
}