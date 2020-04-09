package nerve.network.converter.model.txdata;

import io.nuls.base.data.NulsHash;
import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfirmWithdrawalTxDataTest {


    ConfirmWithdrawalTxData confirmWithdrawalTxData;

    @Before
    public void setUp() throws Exception {
        confirmWithdrawalTxData = new ConfirmWithdrawalTxData();
        confirmWithdrawalTxData.setHeterogeneousHeight(3904857L);
        confirmWithdrawalTxData.setHeterogeneousTxHash("0x1e2910a262b1008d0616a0beb24c1a491d78771baa54a33e66065e03b1f46bc1");
        confirmWithdrawalTxData.setWithdrawalTxHash(NulsHash.fromHex("7c91f96cb4f069a61985710c08f6e773ee52c3632db6c4d5ab9028d7cc30151d"));

        HeterogeneousAddress address1 = new HeterogeneousAddress(108, "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
        HeterogeneousAddress address2 = new HeterogeneousAddress(108, "0xfa27cC24aaEd366079c68484eC062b2fF89EB297");
        List<HeterogeneousAddress> list = new ArrayList<>();
        list.add(address1);
        list.add(address2);
        confirmWithdrawalTxData.setListDistributionFee(list);
    }

    @Test
    public void serializeAndParse() throws Exception {
        byte[] bytes = confirmWithdrawalTxData.serialize();
        ConfirmWithdrawalTxData newObj = ConverterUtil.getInstance(bytes, ConfirmWithdrawalTxData.class);
        assertNotNull(newObj);
        assertEquals(newObj.getHeterogeneousHeight(), confirmWithdrawalTxData.getHeterogeneousHeight());
        assertEquals(newObj.getHeterogeneousTxHash(), confirmWithdrawalTxData.getHeterogeneousTxHash());
        assertEquals(newObj.getWithdrawalTxHash(), confirmWithdrawalTxData.getWithdrawalTxHash());
        assertEquals(newObj.getListDistributionFee().get(0).getAddress(), confirmWithdrawalTxData.getListDistributionFee().get(0).getAddress());

    }
}