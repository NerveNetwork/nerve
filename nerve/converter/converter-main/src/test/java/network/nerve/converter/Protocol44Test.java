package network.nerve.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.akc.context.AkcContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.core.WalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

import static okhttp3.ConnectionSpec.CLEARTEXT;


public class Protocol44Test {

    @BeforeClass
    public static void beforeClass() {
        ObjectMapper objectMapper = JSONUtils.getInstance();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    Map<Integer, HtgContext> contextMap = contextMap();
    Map<Integer, String[]> multyMap = multyMap();
    Map<Integer, HeterogeneousCfg> cfgMap = cfgMap();
    Map<Integer, HtgWalletApi> htgWalletApiMap = new HashMap<>();

    private Map<Integer, HtgContext> contextMap() {
        Map<Integer, HtgContext> map = new HashMap<>();
        map.put(144, new AkcContext());
        return map;
    }
    @Before
    public void before() {
        try {
            for (HtgContext htgContext : contextMap.values()) {
                int htgChainId = htgContext.HTG_CHAIN_ID();
                HeterogeneousCfg cfg = cfgMap.get(htgChainId);
                HtgWalletApi htgWalletApi = new HtgWalletApi();
                //Web3j web3j = Web3j.build(new HttpService(cfg.getMainRpcAddress()));
                //htgWalletApi.setWeb3j(web3j);
                final OkHttpClient.Builder builder =
                        new OkHttpClient.Builder().connectionSpecs(Arrays.asList(WalletApi.INFURA_CIPHER_SUITE_SPEC, CLEARTEXT));
                String os = System.getProperty("os.name");
                if ("Mac OS X".equals(os)) {
                    builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080)));
                }
                OkHttpClient okHttpClient = builder.build();
                Web3j web3j = Web3j.build(new HttpService(cfg.getMainRpcAddress(), okHttpClient));
                htgWalletApi.setWeb3j(web3j);

                htgWalletApi.setEthRpcAddress(cfg.getMainRpcAddress());
                // Set a new signature version number
                byte newVersion = 3;
                htgContext.SET_VERSION(newVersion);
                htgContext.setLogger(Log.BASIC_LOGGER);
                htgContext.setConfig(cfg);
                Field field = htgWalletApi.getClass().getDeclaredField("htgContext");
                field.setAccessible(true);
                field.set(htgWalletApi, htgContext);
                htgWalletApiMap.put(htgChainId, htgWalletApi);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Read the new and old multi signed contracts for each chain
     */
    private Map<Integer, String[]> multyMap() {
        return multyMapMainnet();
    }


    /**
     * Main network: Configure new and old multiple contracts for each chain
     */
    private Map<Integer, String[]> multyMapMainnet() {
        /*
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH,
            119 ENULS, 120 KAVA, 121 ETHW, 122 REI, 123 ZK, 124 EOS, 125 ZKpolygon, 126 Linea,
            127 Celo, 128 ETC, 129 Base, 130 Scroll, 131 Brise, 132 Janus, 133 Manta, 134 XLayer(OKB), 135 ZETA,
            138 Mode-ETH, 139 Blast-ETH, 140 Merlin-BTC, 141 PLS, 143 AKC
            201 BTC, 202 FCH, 203 BCH, 204 TBC
         */
        // Configure new and old multiple contracts for each chain
        Map<Integer, String[]> map = new HashMap<>();
        // Old before new after new
        map.put(144, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5"});
        return map;
    }

    /**
     * Read basic information of heterogeneous chains
     */
    private Map<Integer, HeterogeneousCfg> cfgMap() {
        try {
            String configJson;
            configJson = IoUtils.read("heterogeneous_mainnet.json");
            List<HeterogeneousCfg> config = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
            Map<Integer, HeterogeneousCfg> cfgMap = config.stream().collect(Collectors.toMap(HeterogeneousCfg::getChainId, java.util.function.Function.identity()));
            return cfgMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     Register for Cross Chain Network
     Main network:
     registerheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 144
     */

    /**
     * Complete administrator
     */
    @Test
    public void managerChangeSignData() throws Exception {
        List<String> seedList = new ArrayList<>();
        seedList.add("???");
        seedList.add("???");
        seedList.add("???");

        String txKey = "aaa2000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{
                "0xb12a6716624431730c3Ef55f80C458371954fA52",
                "0x1F13E90daa9548DEfae45cd80C135C183558db1f",
                "0x6C9783CC9C9fF9C0F1280E4608AfAaDF08cFb43D",
                "0xaff68CD458539A16b932748Cf4BdD53bf196789F",
                "0xC8DCC24B09EeD90185dbB1A5277Fd0A389855dAE",
                "0xA28035Bb5082f5c00fa4d3EFc4CB2e0645167444",
                "0x33d8CA8E9129Dd1c4C9C5EceFaaF362B057b9e74",
                "0x48Ee8759515e0C703691140e63725F39ACD3f7d5",
                "0x5FBf7793196efBF7066d99fa29dc64DC23052451",
                "0x7C4b783A0101359590E6153dF3b58c7fE24eA468",
                "0x1e2fD2912Bd017824e0179d74C3C28F22EE49ADD"
        };
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = seedList.size();
        for (HtgContext htgContext : contextMap.values()) {
            int htgChainId = htgContext.HTG_CHAIN_ID();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            String[] multyArray = multyMap.get(htgChainId);
            String newMulty = multyArray[1];
            String multySignContractAddress = newMulty;

            String hash = this.sendChange(htgWalletApi, htgContext, seedList, multySignContractAddress, txKey, adds, txCount, removes, signCount);
            System.out.println(String.format("htgId: %s, Administrator added %s Remove %s Pieces, hash: %s", htgChainId, adds.length, removes.length, hash));
        }
    }

    protected String changeSignData(HtgContext htgContext, List<String> seedList, String txKey, String[] adds, int count, String[] removes) throws Exception {
        String vHash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, htgContext.VERSION());
        String signData = this.ethSign(seedList, vHash, seedList.size());
        return signData;
    }

    protected String sendChangeWithSignData(String priKey, HtgWalletApi htgWalletApi, String signData, String multySignContractAddress, String txKey, String[] adds, int count, String[] removes) throws Exception {
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        String address = Credentials.create(priKey).getAddress();
        return this.sendTx(htgWalletApi, multySignContractAddress, address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendChange(HtgWalletApi htgWalletApi, HtgContext htgContext, List<String> seedList, String multySignContractAddress, String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, htgContext.VERSION());
        String signData = this.ethSign(seedList, vHash, signCount);
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        String priKey = seedList.get(0);
        String address = Credentials.create(priKey).getAddress();
        return this.sendTx(htgWalletApi, multySignContractAddress, address, priKey, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendTx(HtgWalletApi htgWalletApi, String multySignContractAddress, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.sendTx(htgWalletApi, fromAddress, priKey, txFunction, txType, null, multySignContractAddress);
    }

    protected String sendTx(HtgWalletApi htgWalletApi, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger value, String contract) throws Exception {
        // estimateGasLimit
        EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        BigInteger estimateGas = estimateGasObj.getAmountUsed();

        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            if (estimateGasObj.getError() != null) {
                Log.error("Failed to estimate gas, error: {}", estimateGasObj.getError().getMessage());
            } else {
                Log.error("Failed to estimate gas");
            }
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, txType.toString() + ", estimate Gas Limit fail");
        }
        BigInteger gasLimit = estimateGas.add(BigInteger.valueOf(50000L));
        HtgSendTransactionPo htSendTransactionPo = htgWalletApi.callContract(fromAddress, priKey, contract, gasLimit, txFunction, value, null, null);
        String ethTxHash = htSendTransactionPo.getTxHash();
        return ethTxHash;
    }

    protected String ethSign(List<String> seedList, String hashStr, int signCount) {
        String result = "";
        List<String> addressList = new ArrayList<>();
        byte[] hash = Numeric.hexStringToByteArray(hashStr);
        for (int i = 0; i < signCount; i++) {
            String prikey = seedList.get(i);
            Credentials credentials = Credentials.create(prikey);
            String address = credentials.getAddress();
            Sign.SignatureData signMessage = Sign.signMessage(hash, credentials.getEcKeyPair(), false);
            byte[] signed = new byte[65];
            System.arraycopy(signMessage.getR(), 0, signed, 0, 32);
            System.arraycopy(signMessage.getS(), 0, signed, 32, 32);
            System.arraycopy(signMessage.getV(), 0, signed, 64, 1);
            String signedHex = Numeric.toHexStringNoPrefix(signed);
            result += signedHex;
            addressList.add(address);
        }
        return result;
    }

}
