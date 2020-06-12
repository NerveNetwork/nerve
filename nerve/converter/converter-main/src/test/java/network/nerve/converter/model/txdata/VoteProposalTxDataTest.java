package network.nerve.converter.model.txdata;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VoteProposalTxDataTest {


    VoteProposalTxData voteProposalTxData;
    @Before
    public void setUp() throws Exception {

        voteProposalTxData = new VoteProposalTxData();
        voteProposalTxData.setChoice((byte)0);
        voteProposalTxData.setProposalTxHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = voteProposalTxData.serialize();
        VoteProposalTxData newObj = ConverterUtil.getInstance(bytes, VoteProposalTxData.class);

        assertNotNull(newObj);
        assertEquals(newObj.getChoice(), voteProposalTxData.getChoice());
        assertEquals(newObj.getProposalTxHash(), voteProposalTxData.getProposalTxHash());
    }
}