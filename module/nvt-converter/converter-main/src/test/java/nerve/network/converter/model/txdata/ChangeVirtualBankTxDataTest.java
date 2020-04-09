package nerve.network.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ChangeVirtualBankTxDataTest {

    ChangeVirtualBankTxData changeVirtualBankTxData;

    @Before
    public void setUp() throws Exception {
        changeVirtualBankTxData = new ChangeVirtualBankTxData();

        List<byte[]> inAgents = new ArrayList<>();
        inAgents.add(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
        inAgents.add(AddressTool.getAddress("tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD"));
        changeVirtualBankTxData.setInAgents(inAgents);

        List<byte[]> outAgents = new ArrayList<>();
        outAgents.add(AddressTool.getAddress("tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24"));
        outAgents.add(AddressTool.getAddress("tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD"));
        outAgents.add(AddressTool.getAddress("tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL"));
        changeVirtualBankTxData.setOutAgents(outAgents);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = changeVirtualBankTxData.serialize();
        ChangeVirtualBankTxData vcbt = ConverterUtil.getInstance(bytes, ChangeVirtualBankTxData.class);
        assertNotNull(vcbt);
        assertNotNull(vcbt.getInAgents());
        assertNotNull(vcbt.getOutAgents());
        assertEquals(vcbt.getInAgents().size(), changeVirtualBankTxData.getInAgents().size());
        assertEquals(vcbt.getOutAgents().size(), changeVirtualBankTxData.getOutAgents().size());
        for(int i=0;i<vcbt.getInAgents().size();i++){
            assertArrayEquals(vcbt.getInAgents().get(i), changeVirtualBankTxData.getInAgents().get(i));
        }
        for(int i=0;i<vcbt.getOutAgents().size();i++){
            assertArrayEquals(vcbt.getOutAgents().get(i), changeVirtualBankTxData.getOutAgents().get(i));
        }

    }

}