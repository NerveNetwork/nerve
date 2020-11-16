package network.nerve.converter.model.txdata;


import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WithdrawalHeterogeneousSendTxDataTest {

    WithdrawalHeterogeneousSendTxData withdrawalHeterogeneousSendTxData;

    @Before
    public void setUp() throws Exception {
        withdrawalHeterogeneousSendTxData = new WithdrawalHeterogeneousSendTxData();
        withdrawalHeterogeneousSendTxData.setNerveTxHash("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d");
        withdrawalHeterogeneousSendTxData.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = withdrawalHeterogeneousSendTxData.serialize();
        WithdrawalHeterogeneousSendTxData newObj = ConverterUtil.getInstance(bytes, WithdrawalHeterogeneousSendTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getNerveTxHash(), withdrawalHeterogeneousSendTxData.getNerveTxHash());
        assertEquals(newObj.getHeterogeneousTxHash(), withdrawalHeterogeneousSendTxData.getHeterogeneousTxHash());
    }
}