package network.nerve.converter.model.po;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProposalPOTest {

    ProposalPO proposalPO;
    @Before
    public void setUp() throws Exception {
        proposalPO = new ProposalPO();
        proposalPO.setType((byte)1);
        proposalPO.setContent("The development of the latent period of Astifen is reasonable. Just now, it has been discussed with");
        proposalPO.setHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));
        proposalPO.setAddress(AddressTool.getAddress("tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24"));
        proposalPO.setHeterogeneousChainId(101);
//        proposalPO.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
        proposalPO.setVoteEndHeight(79865);
        proposalPO.setVoteRangeType((byte)0);
        proposalPO.setStatus((byte)0);


        List<String> againstList = new ArrayList<>();
        againstList.add("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        againstList.add("tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD");


        List<String> favorList = new ArrayList<>();
        favorList.add("tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp");
        favorList.add("tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe");
        favorList.add("tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn");

        proposalPO.setFavorList(favorList);
        proposalPO.setFavorNumber(favorList.size());
        proposalPO.setAgainstList(againstList);
        proposalPO.setAgainstNumber(againstList.size());
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = proposalPO.serialize();
        ProposalPO newObj = ConverterUtil.getInstance(bytes, ProposalPO.class);
        assertNotNull(newObj);
        assertEquals(proposalPO.getHash(), newObj.getHash());
        assertEquals(proposalPO.getStatus(), newObj.getStatus());
        assertEquals(proposalPO.getType(), newObj.getType());
        assertArrayEquals(proposalPO.getAddress(), newObj.getAddress());
        assertEquals(proposalPO.getHeterogeneousChainId(), newObj.getHeterogeneousChainId());
//        assertEquals(proposalPO.getHeterogeneousTxHash(), newObj.getHeterogeneousTxHash());
        assertEquals(proposalPO.getVoteEndHeight(), newObj.getVoteEndHeight());
        assertEquals(proposalPO.getVoteRangeType(), newObj.getVoteRangeType());
        assertEquals(proposalPO.getAgainstNumber(), newObj.getAgainstNumber());
        assertEquals(proposalPO.getFavorNumber(), newObj.getFavorNumber());
        assertEquals(proposalPO.getAgainstList().size(), newObj.getAgainstList().size());
        assertEquals(proposalPO.getFavorList().size(), newObj.getFavorList().size());
    }
}
