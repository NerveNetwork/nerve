package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import network.nerve.converter.utils.ConverterUtil;
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

//        List<byte[]> inAgents = new ArrayList<>();
//        inAgents.add(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
//        inAgents.add(AddressTool.getAddress("tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD"));
//        changeVirtualBankTxData.setInAgents(inAgents);
        changeVirtualBankTxData.setInAgents(null);

        List<byte[]> outAgents = new ArrayList<>();
        outAgents.add(AddressTool.getAddress("tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24"));
        outAgents.add(AddressTool.getAddress("tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD"));
        outAgents.add(AddressTool.getAddress("tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL"));
        changeVirtualBankTxData.setOutAgents(outAgents);
        changeVirtualBankTxData.setOutHeight(7564324324L);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = changeVirtualBankTxData.serialize();
        ChangeVirtualBankTxData vcbt = ConverterUtil.getInstance(bytes, ChangeVirtualBankTxData.class);
        assertNotNull(vcbt);
//        assertNotNull(vcbt.getInAgents());
        assertNotNull(vcbt.getOutAgents());
        assertEquals(vcbt.getOutHeight(), changeVirtualBankTxData.getOutHeight());
//        assertEquals(vcbt.getInAgents().size(), changeVirtualBankTxData.getInAgents().size());
        assertEquals(vcbt.getOutAgents().size(), changeVirtualBankTxData.getOutAgents().size());
//        for(int i=0;i<vcbt.getInAgents().size();i++){
//            assertArrayEquals(vcbt.getInAgents().get(i), changeVirtualBankTxData.getInAgents().get(i));
//        }
        for(int i=0;i<vcbt.getOutAgents().size();i++){
            assertArrayEquals(vcbt.getOutAgents().get(i), changeVirtualBankTxData.getOutAgents().get(i));
        }

    }



    public static void main(String[] args) throws Exception{
        String txStr = "29000401c25e00230100040001e3f0bca2174bc5b5951de002d5cb01c864c1136f0000ffffffffffffffff00692102cec353f77275953704f5c6f64b38f3f49a5b590783392c69b2718e4bf18588be46304402200f7f2c6790405332d47ffa40a45e77b98e49629e2825a7569a5141c60d3344c602201d4241b91298a8836eaed85202b1916d26a56255ec5cbefb9b820244fbf3da34";
        Transaction tx = ConverterUtil.getInstance(txStr, Transaction.class);//The last one
        System.out.println(tx.format(ChangeVirtualBankTxData.class));
    }
}
