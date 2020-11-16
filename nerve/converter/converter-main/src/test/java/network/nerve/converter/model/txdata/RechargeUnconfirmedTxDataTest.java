package network.nerve.converter.model.txdata;


import io.nuls.base.basic.AddressTool;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class RechargeUnconfirmedTxDataTest {

    RechargeUnconfirmedTxData rechargeUnconfirmedTxData;

    @Before
    public void setUp() throws Exception {
        rechargeUnconfirmedTxData = new RechargeUnconfirmedTxData();
        rechargeUnconfirmedTxData.setAssetChainId(9);
        rechargeUnconfirmedTxData.setAssetId(1);
        rechargeUnconfirmedTxData.setHeterogeneousFromAddress("0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        rechargeUnconfirmedTxData.setHeterogeneousHeight(68243L);
        rechargeUnconfirmedTxData.setOriginalTxHash(new HeterogeneousHash(101,"0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1"));
        rechargeUnconfirmedTxData.setNerveToAddress(AddressTool.getAddress("TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9"));
        rechargeUnconfirmedTxData.setAmount(new BigInteger("1235900000"));
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = rechargeUnconfirmedTxData.serialize();
        RechargeUnconfirmedTxData newObj = ConverterUtil.getInstance(bytes, RechargeUnconfirmedTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getAssetChainId(), rechargeUnconfirmedTxData.getAssetChainId());
        assertEquals(newObj.getAssetId(), rechargeUnconfirmedTxData.getAssetId());
        assertEquals(newObj.getHeterogeneousFromAddress(), rechargeUnconfirmedTxData.getHeterogeneousFromAddress());
        assertEquals(newObj.getHeterogeneousHeight(), rechargeUnconfirmedTxData.getHeterogeneousHeight());
        assertEquals(newObj.getOriginalTxHash().getHeterogeneousChainId(), rechargeUnconfirmedTxData.getOriginalTxHash().getHeterogeneousChainId());
        assertEquals(newObj.getOriginalTxHash().getHeterogeneousHash(), rechargeUnconfirmedTxData.getOriginalTxHash().getHeterogeneousChainId());
        assertTrue(newObj.getAmount().compareTo(rechargeUnconfirmedTxData.getAmount())== 0);
        assertArrayEquals(newObj.getNerveToAddress(), rechargeUnconfirmedTxData.getNerveToAddress());
    }
}