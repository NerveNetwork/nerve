package nerve.network.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InitializeHeterogeneousTxDataTest {

    InitializeHeterogeneousTxData initializeHeterogeneousTxData;
    @Before
    public void setUp() throws Exception {
        initializeHeterogeneousTxData = new InitializeHeterogeneousTxData();
        initializeHeterogeneousTxData.setHeterogeneousChainId(108);
        List<byte[]> listDirector = new ArrayList<>();
        listDirector.add(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
        listDirector.add(AddressTool.getAddress("tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD"));
        listDirector.add(AddressTool.getAddress("tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24"));
        listDirector.add(AddressTool.getAddress("tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD"));
        listDirector.add(AddressTool.getAddress("tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL"));
//        initializeHeterogeneousTxData.setListDirector(listDirector);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = initializeHeterogeneousTxData.serialize();
        InitializeHeterogeneousTxData newObj = ConverterUtil.getInstance(bytes, InitializeHeterogeneousTxData.class);
        assertNotNull(newObj);
        assertEquals(initializeHeterogeneousTxData.getHeterogeneousChainId(), newObj.getHeterogeneousChainId());
//        assertEquals(initializeHeterogeneousTxData.getListDirector().size(), newObj.getListDirector().size());
    }
}
