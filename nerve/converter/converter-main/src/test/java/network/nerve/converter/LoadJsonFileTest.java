package network.nerve.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.heterogeneouschain.eth.model.EthERC20Po;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

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
}