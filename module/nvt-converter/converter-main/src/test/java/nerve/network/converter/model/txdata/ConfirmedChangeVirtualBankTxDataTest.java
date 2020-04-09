package nerve.network.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import nerve.network.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfirmedChangeVirtualBankTxDataTest {

    ConfirmedChangeVirtualBankTxData confirmedChangeVirtualBankTxData;

    @Before
    public void setUp() throws Exception {

        confirmedChangeVirtualBankTxData = new ConfirmedChangeVirtualBankTxData();
        List<byte[]> agents = new ArrayList<>();
        agents.add(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
        agents.add(AddressTool.getAddress("tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD"));
        confirmedChangeVirtualBankTxData.setListAgents(agents);
        confirmedChangeVirtualBankTxData.setChangeVirtualBankTxHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));

        HeterogeneousConfirmedVirtualBank confirmed = new HeterogeneousConfirmedVirtualBank();
        confirmed.setEffectiveTime(123456712345L);
        confirmed.setHeterogeneousAddress("0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        confirmed.setHeterogeneousChainId(109);
        confirmed.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
        confirmedChangeVirtualBankTxData.setListConfirmed(Arrays.asList(confirmed));
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = confirmedChangeVirtualBankTxData.serialize();
        ConfirmedChangeVirtualBankTxData newObj = ConverterUtil.getInstance(bytes, ConfirmedChangeVirtualBankTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getChangeVirtualBankTxHash(), confirmedChangeVirtualBankTxData.getChangeVirtualBankTxHash());
        assertEquals(newObj.getListAgents().size(), confirmedChangeVirtualBankTxData.getListAgents().size());
        assertEquals(newObj.getListConfirmed().size(), confirmedChangeVirtualBankTxData.getListConfirmed().size());
    }
}