package network.nerve.converter.model.txdata;


import io.nuls.base.data.NulsHash;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProposalExeBusinessDataTest {

    ProposalExeBusinessData businessData;

    @Before
    public void setUp() throws Exception {
        businessData = new ProposalExeBusinessData();
        businessData.setHeterogeneousChainId(101);
        businessData.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
        businessData.setProposalTxHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));
//        businessData.setAddress(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
        businessData.setProposalExeHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));

        HeterogeneousAddress address1 = new HeterogeneousAddress(108, "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        HeterogeneousAddress address2 = new HeterogeneousAddress(108, "0xfa27cC24aaEd366079c68484eC062b2fF89EB297");
        List<HeterogeneousAddress> list = new ArrayList<>();
        list.add(address1);
        list.add(address2);
        businessData.setListDistributionFee(list);

    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = businessData.serialize();
        ProposalExeBusinessData newObj = ConverterUtil.getInstance(bytes, ProposalExeBusinessData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getHeterogeneousChainId(), businessData.getHeterogeneousChainId());
//        assertEquals(newObj.getHeterogeneousTxHash(), businessData.getHeterogeneousTxHash());
        assertEquals(newObj.getProposalTxHash(), businessData.getProposalTxHash());
        assertArrayEquals(newObj.getAddress(), businessData.getAddress());
        assertEquals(newObj.getProposalExeHash(), businessData.getProposalExeHash());

    }
}
