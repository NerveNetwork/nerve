package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.utils.ConverterUtil;
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


        confirmedChangeVirtualBankTxData = new ConfirmedChangeVirtualBankTxData();
        confirmedChangeVirtualBankTxData.setChangeVirtualBankTxHash(NulsHash.fromHex("25a881d8b226f4c6ffd5b02845593e571e1f9d329a852afb62b9131320f1b962"));
        HeterogeneousConfirmedVirtualBank confirmedBank = new HeterogeneousConfirmedVirtualBank();
        confirmedBank.setHeterogeneousChainId(101);
        confirmedBank.setHeterogeneousAddress("0xe24973ff71d061f403cb45ddde97e034484ee7d3");
        confirmedBank.setHeterogeneousTxHash("0xdd4ebad89dc95dabecb32add39a35c6c29196dfcc15749e515db5d0deb9f710d");
        confirmedBank.setEffectiveTime(1586506378L);
        confirmedChangeVirtualBankTxData.setListConfirmed(Arrays.asList(confirmedBank));

        List<byte[]> agentList = new ArrayList<>();
        agentList.add(AddressTool.getAddress("tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp"));
        agentList.add(AddressTool.getAddress("tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe"));
        agentList.add(AddressTool.getAddress("tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn"));
        confirmedChangeVirtualBankTxData.setListAgents(agentList);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = confirmedChangeVirtualBankTxData.serialize();
        System.out.println(HexUtil.encode(bytes));
        ConfirmedChangeVirtualBankTxData newObj = ConverterUtil.getInstance(bytes, ConfirmedChangeVirtualBankTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getChangeVirtualBankTxHash(), confirmedChangeVirtualBankTxData.getChangeVirtualBankTxHash());
        assertEquals(newObj.getListAgents().size(), confirmedChangeVirtualBankTxData.getListAgents().size());
        assertEquals(newObj.getListConfirmed().size(), confirmedChangeVirtualBankTxData.getListConfirmed().size());
    }

    @Test
    public void test1()  throws Exception {
        String str = "28008a2a905e00df25a881d8b226f4c6ffd5b02845593e571e1f9d329a852afb62b9131320f1b96203000200015f64837d090e0536482fbcab79b4f99f84c1f97e02000186a305cc8fe4b1ce3063f7fc91fcf3d931088432020001b2af285f49882f0317f4f5c140233de06aae6810010065002a307865323439373366663731643036316634303363623435646464653937653033343438346565376433423078646434656261643839646339356461626563623332616464333961333563366332393139366466636331353734396535313564623564306465623966373130648a2a905e000000692102a11ce1418bebc6e6aedbcec6b657f7b3d52c27f5da6c5e6c7035364be4f715134630440220032f4479b1aaa06a9bf4ed9b447adf6ad7285154cc2d597ba705a46d35aee587022032919678c82b9e9d61832cf5fb528d18a8f7ee5ded45860deddb9216fa8bad82";
        byte[] bytes = HexUtil.decode(str);
        Transaction tx = ConverterUtil.getInstance(bytes, Transaction.class);
        System.out.println(tx);

        String strData1 = "25a881d8b226f4c6ffd5b02845593e571e1f9d329a852afb62b9131320f1b96203000200015f64837d090e0536482fbcab79b4f99f84c1f97e02000186a305cc8fe4b1ce3063f7fc91fcf3d931088432020001b2af285f49882f0317f4f5c140233de06aae6810010065002a307865323439373366663731643036316634303363623435646464653937653033343438346565376433423078646434656261643839646339356461626563623332616464333961333563366332393139366466636331353734396535313564623564306465623966373130648a2a905e0000";
        String strData2 = "25a881d8b226f4c6ffd5b02845593e571e1f9d329a852afb62b9131320f1b96203000200015f64837d090e0536482fbcab79b4f99f84c1f97e02000186a305cc8fe4b1ce3063f7fc91fcf3d931088432020001b2af285f49882f0317f4f5c140233de06aae6810010065002a307865323439373366663731643036316634303363623435646464653937653033343438346565376433423078646434656261643839646339356461626563623332616464333961333563366332393139366466636331353734396535313564623564306465623966373130648a2a905e0000";

        byte[] bytesData = HexUtil.decode(strData1);
        ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(bytesData, ConfirmedChangeVirtualBankTxData.class);
        System.out.println(txData);

        //25a881d8b226f4c6ffd5b02845593e571e1f9d329a852afb62b9131320f1b962
    }
}