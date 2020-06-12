package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SubmitHeterogeneousAddressTxDataTest {

    SubmitHeterogeneousAddressTxData submitHeterogeneousAddressTxData;

    @Before
    public void setUp() throws Exception {

        HeterogeneousAddress h1 = new HeterogeneousAddress();
        h1.setChainId(3);
        h1.setAddress("0xfa27c84eC062b2fF89EB297C24aaEd366079c684");

        HeterogeneousAddress h2 = new HeterogeneousAddress();
        h2.setChainId(4);
        h2.setAddress("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2");
        List<HeterogeneousAddress> list = new ArrayList<>();
        list.add(h1);
        list.add(h2);

        submitHeterogeneousAddressTxData = new SubmitHeterogeneousAddressTxData();
        submitHeterogeneousAddressTxData.setAgentAddress(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
        submitHeterogeneousAddressTxData.setHeterogeneousAddressList(list);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = submitHeterogeneousAddressTxData.serialize();
        SubmitHeterogeneousAddressTxData newObj = ConverterUtil.getInstance(bytes, SubmitHeterogeneousAddressTxData.class);

        assertNotNull(newObj);
        assertArrayEquals(newObj.getAgentAddress(), submitHeterogeneousAddressTxData.getAgentAddress());
        assertNotNull(submitHeterogeneousAddressTxData.getHeterogeneousAddressList());
        assertEquals(submitHeterogeneousAddressTxData.getHeterogeneousAddressList().size(), newObj.getHeterogeneousAddressList().size());
        for(int i=0;i<newObj.getHeterogeneousAddressList().size();i++){
            HeterogeneousAddress newAddress = newObj.getHeterogeneousAddressList().get(i);
            HeterogeneousAddress oldAddress = submitHeterogeneousAddressTxData.getHeterogeneousAddressList().get(i);
            assertEquals(newAddress, oldAddress);
        }
    }
}