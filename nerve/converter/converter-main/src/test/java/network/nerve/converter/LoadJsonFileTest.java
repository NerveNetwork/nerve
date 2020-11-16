package network.nerve.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.heterogeneouschain.eth.model.EthERC20Po;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.txdata.WithdrawalAdditionalFeeTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_ERC20_STANDARD_FILE;


public class LoadJsonFileTest {

    @BeforeClass
    public static void beforeClass() {
        ObjectMapper objectMapper = JSONUtils.getInstance();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void erc20FileTest() throws IOException {
        String json = null;
        try {
            json = IoUtils.read(ETH_ERC20_STANDARD_FILE);
        } catch (Exception e) {
            // skip it
            Log.error("init ERC20Standard error.", e);
        }
        if (json == null) {
            return;
        }
        List<EthERC20Po> ethERC20Pos = JSONUtils.json2list(json, EthERC20Po.class);
        System.out.println(ethERC20Pos.size());
    }

    @Test
    public void heterogeneousFiletest() throws Exception {
        String configJson = IoUtils.read(ConverterConstant.HETEROGENEOUS_TESTNET_CONFIG);
        List<HeterogeneousCfg> list = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
        System.out.println(list.size());
    }

    @Test
    public void desTransaction() throws NulsException {
        LATEST_BLOCK_HEIGHT = 1;
        String hex = "38008c899b5f00204e9d8c687b7e6d53c371241092afa22980b01fae067c5aeb9ec16a6fd223c9518c0117050001c2a8859453b67a1fd71bb91bc5712da9d5c3cad005000100a06c71dd03000000000000000000000000000000000000000000000000000000089ec16a6fd223c9510001170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e0500010000e66fdd0300000000000000000000000000000000000000000000000000000000000000000000006a2103ab1436c5e7caead12ff7413d8029c94b1f06fc19754ba947ffa0a39043da95a94730450220092e2c8a56ebf15c7f18cb655f691270fcaecd5a56b9022c19fd0c523686b263022100e908212aaa12eabdb51b77c757bc9c65a16305981aadf804e1f22a092dadfff9";
        Transaction tx = new Transaction();
        tx.parse(HexUtil.decode(hex), 0);
        System.out.println(HexUtil.encode(tx.getTxData()));

        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData();
        txData.parse(tx.getTxData(), 0);
        System.out.println();
        //System.out.println(tx.format(WithdrawalTxData.class));
    }
}