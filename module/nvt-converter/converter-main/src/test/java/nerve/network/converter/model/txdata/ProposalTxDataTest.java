package nerve.network.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.model.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProposalTxDataTest {


    ProposalTxData proposalTxData;
    @Before
    public void setUp() throws Exception {
        proposalTxData = new ProposalTxData();
        proposalTxData.setType((byte)1);
        proposalTxData.setVoteRangeType((byte)0);
        proposalTxData.setContent("230487thurefqu/'\\[/\\//h3fpi?？‘；】【。，史蒂夫违法3421！@#￥%……&*】’ueqhpfh");

        proposalTxData.setAddress(AddressTool.getAddress("tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG"));
//        proposalTxData.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");

    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = proposalTxData.serialize();
        ProposalTxData newObj = ConverterUtil.getInstance(bytes, ProposalTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getType(), proposalTxData.getType());
        assertEquals(newObj.getVoteRangeType(), proposalTxData.getVoteRangeType());
        assertEquals(newObj.getContent(), proposalTxData.getContent());
        assertArrayEquals(newObj.getAddress(), proposalTxData.getAddress());
        if(StringUtils.isNotBlank(newObj.getHeterogeneousTxHash())
            && StringUtils.isNotBlank(proposalTxData.getHeterogeneousTxHash())){
            assertEquals(newObj.getHeterogeneousTxHash(), proposalTxData.getHeterogeneousTxHash());
        }
    }
}