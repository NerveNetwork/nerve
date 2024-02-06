package network.nerve.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.util.JsonRpcUtil;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.arbitrum.context.ArbitrumContext;
import network.nerve.converter.heterogeneouschain.avax.context.AvaxContext;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.cro.context.CroContext;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.ftm.context.FtmContext;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.kcs.context.KcsContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.matic.context.MaticContext;
import network.nerve.converter.heterogeneouschain.okt.context.OktContext;
import network.nerve.converter.heterogeneouschain.one.context.OneContext;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.model.TrxSendTransactionPo;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.TRX_100;


public class Protocol16Test {

    // Set up environment
    static boolean MAIN = true;

    @BeforeClass
    public static void beforeClass() {
        ObjectMapper objectMapper = JSONUtils.getInstance();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    Map<Integer, HtgContext> contextMap = contextMap();
    Map<Integer, String[]> multyUpgradeMap = multyUpgradeMap();
    Map<Integer, HeterogeneousCfg> cfgMap = cfgMap();
    Map<Integer, HtgWalletApi> htgWalletApiMap = new HashMap<>();
    TrxWalletApi trxWalletApi;
    String contractForCreateERC20Minter;

    @Before
    public void before() {
        try {
            if (MAIN) {
                contractForCreateERC20Minter = "0x63ae3cea2225be3390854e824a65bbbb02616bb4";
            } else {
                contractForCreateERC20Minter = "0x1EA3FfD41c3ed3e3f788830aAef553F8F691aD8C";
            }
            for (int i = 101; i <= 112; i++) {
                int htgChainId = i;
                if (htgChainId == 108) {
                    // Separate processing of wave field
                    continue;
                }
                HeterogeneousCfg cfg = cfgMap.get(htgChainId);
                HtgWalletApi htgWalletApi = new HtgWalletApi();
                Web3j web3j = Web3j.build(new HttpService(cfg.getMainRpcAddress()));
                htgWalletApi.setWeb3j(web3j);
                htgWalletApi.setEthRpcAddress(cfg.getMainRpcAddress());
                HtgContext htgContext = contextMap.get(htgChainId);
                htgContext.setLogger(Log.BASIC_LOGGER);
                htgContext.setConfig(cfg);
                Field field = htgWalletApi.getClass().getDeclaredField("htgContext");
                field.setAccessible(true);
                field.set(htgWalletApi, htgContext);
                htgWalletApiMap.put(htgChainId, htgWalletApi);
            }
            // Initialize wave fieldapi
            int trxId = 108;
            trxWalletApi = new TrxWalletApi();
            if (MAIN) {
                //ApiWrapper wrapper = new ApiWrapper("tron.nerve.network:50051", "tron.nerve.network:50061", "3333333333333333333333333333333333333333333333333333333333333333");
                ApiWrapper wrapper = ApiWrapper.ofMainnet("3333333333333333333333333333333333333333333333333333333333333333", "76f3c2b5-357a-4e6c-aced-9e1c42179717");
                trxWalletApi.setWrapper(wrapper);
                trxWalletApi.setRpcAddress("endpoint:tron.nerve.network");
            } else {
                ApiWrapper wrapper = ApiWrapper.ofShasta("3333333333333333333333333333333333333333333333333333333333333333");
                trxWalletApi.setWrapper(wrapper);
                trxWalletApi.setRpcAddress(EMPTY_STRING);
            }
            HtgContext trxContext = contextMap.get(trxId);
            HeterogeneousCfg trxCfg = cfgMap.get(trxId);
            trxContext.setLogger(Log.BASIC_LOGGER);
            trxContext.setConfig(trxCfg);
            Field field = trxWalletApi.getClass().getDeclaredField("htgContext");
            field.setAccessible(true);
            field.set(trxWalletApi, trxContext);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Read the new and old multi signed contracts for each chain
     */
    private Map<Integer, String[]> multyUpgradeMap() {
        if (MAIN) {
            return multyUpgradeMapMainnet();
        } else {
            return multyUpgradeMapTestnet();
        }
    }

    /**
     * Test Network: Configure new and old multiple contracts for each chain
     */
    private Map<Integer, String[]> multyUpgradeMapTestnet() {
        Map<Integer, String[]> map = new HashMap<>();
        // Old before new after new
        map.put(101, new String[]{"0x7D759A3330ceC9B766Aa4c889715535eeD3c0484", "0x5e1cba794aD91FCd272fDaF2cd91b6110b601ED2"});
        map.put(102, new String[]{"0xf7915d4de86b856F3e51b894134816680bf09EEE", "0xf85f03C3fAAC61ACF7B187513aeF10041029A1b2"});
        map.put(103, new String[]{"0xb339211438Dcbf3D00d7999ad009637472FC72b3", "0x19d90D3C8eb0C0B3E3093B054031fF1cA81704B8"});
        map.put(104, new String[]{"0xab34B1F41dA5a32fdE53850EfB3e54423e93483e", "0xB490F2a3eC0B90e5faa1636bE046d82AB7cdAC74"});
        map.put(105, new String[]{"0x74A163fCd791Ec7AaB2204ffAbf1A1DFb8854883", "0x0EA7cE4180E8Bc484db4be9b497d9D106a3D7781"});
        map.put(106, new String[]{"0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862", "0xFe05820BaE725fD093E9C1CB6E40AB3BDc40Def2"});
        map.put(107, new String[]{"0x74A163fCd791Ec7AaB2204ffAbf1A1DFb8854883", "0x1329d995EB0c8FD1e20fa1f9ee12e9fE4c67c60a"});
        map.put(108, new String[]{"TWajcnpyyZLRtLkFd6p4ZAMn5y4GpDa6MB", "TYVxuksybZdbyQwoR25V2YUgXYAHikcLro"});
        map.put(109, new String[]{"0xb339211438Dcbf3D00d7999ad009637472FC72b3", "0xb339211438Dcbf3D00d7999ad009637472FC72b3"});
        map.put(110, new String[]{"0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9", "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9"});
        map.put(111, new String[]{"0x830befa62501F1073ebE2A519B882e358f2a0318", "0x830befa62501F1073ebE2A519B882e358f2a0318"});
        map.put(112, new String[]{"0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9", "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9"});
        return map;
    }

    /**
     * Main network: Configure new and old multiple contracts for each chain
     */
    private Map<Integer, String[]> multyUpgradeMapMainnet() {
        /*
            0xc707e0854da2d72c90a7453f8dc224dd937d7e82  eth
            0x75ab1d50bedbd32b6113941fcf5359787a4bbef4  bsc
            0x2ead2e7a3bd88c6a90b3d464bc6938ba56f1407f  heco
            0xe096d12d6cb61e11bce3755f938b9259b386523a  oec
            0x32fae32961474e6d19b7a6346524b8a6a6fd1d9c  Harmony
            0x9ddc2fb726cf243305349587ae2a33dd7c91460e  Polygon
            0xdb442dff8ff9fd10245406da9a32528c30c10c92  kcc
            TXeFBRKUW2x8ZYKPD13RuZDTd9qHbaPGEN   TRON
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM
         */
        // Configure new and old multiple contracts for each chain
        Map<Integer, String[]> map = new HashMap<>();
        // Old before new after new
        map.put(101, new String[]{"0x6758d4c4734ac7811358395a8e0c3832ba6ac624", "0xc707e0854da2d72c90a7453f8dc224dd937d7e82"});
        map.put(102, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4"});
        map.put(103, new String[]{"0x23023c99dcede393d6d18ca7fb08541b3364fa90", "0x2ead2e7a3bd88c6a90b3d464bc6938ba56f1407f"});
        map.put(104, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0xe096d12d6cb61e11bce3755f938b9259b386523a"});
        map.put(105, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0x32fae32961474e6d19b7a6346524b8a6a6fd1d9c"});
        map.put(106, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0x9ddc2fb726cf243305349587ae2a33dd7c91460e"});
        map.put(107, new String[]{"0xf0e406c49c63abf358030a299c0e00118c4c6ba5", "0xdb442dff8ff9fd10245406da9a32528c30c10c92"});
        map.put(108, new String[]{"TYmgxoiPetfE2pVWur9xp7evW4AuZCzfBm", "TXeFBRKUW2x8ZYKPD13RuZDTd9qHbaPGEN"});
        map.put(109, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5"});
        map.put(110, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5"});
        map.put(111, new String[]{"0xf0e406c49c63abf358030a299c0e00118c4c6ba5", "0xf0e406c49c63abf358030a299c0e00118c4c6ba5"});
        map.put(112, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5"});
        return map;
    }

    /**
     * Read basic information of heterogeneous chains
     */
    private Map<Integer, HeterogeneousCfg> cfgMap() {
        try {
            String configJson;
            if (MAIN) {
                configJson = IoUtils.read("heterogeneous_mainnet.json");
            } else {
                configJson = IoUtils.read("heterogeneous_testnet.json");
            }
            List<HeterogeneousCfg> config = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
            Map<Integer, HeterogeneousCfg> cfgMap = config.stream().collect(Collectors.toMap(HeterogeneousCfg::getChainId, java.util.function.Function.identity()));
            return cfgMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map tokenAssetsMap() {
        try {
            String outFileName;
            if (MAIN) {
                outFileName = "nerve_assets_group_by_ps.json";
            } else {
                outFileName = "nerve_assets_group_by_ps_testnet.json";
            }
            String json = null;
            try {
                json = IoUtils.read(outFileName);
            } catch (Exception e) {
                // skip it
                Log.error("load file error.", e);
            }
            if (json == null) {
                return null;
            }
            Map assetsMap = JSONUtils.json2map(json);
            return assetsMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The first step is to read the data of each heterogeneous chaintokeninformation
     */
    @Test
    public void nerveAssetJsonFileFromPublicServiceTest() throws IOException {
        RpcResult request;
        if (MAIN) {
            request = JsonRpcUtil.request("https://public.nerve.network/", "getAccountLedgerList", List.of(9, "NERVEepb67fJhLqNrA5KGZXvKvjxMmJp7vJrLX"));
        } else {
            request = JsonRpcUtil.request("http://beta.public.nerve.network/", "getAccountLedgerList", List.of(5, "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA"));
        }
        List<Map> list = (List<Map>) request.getResult();
        System.out.println(list.size());

        Map<Integer, Set<AssetInfo>> result = new HashMap<>();
        for (Map info : list) {
            List<Map> htgList = (List<Map>) info.get("heterogeneousList");
            if (htgList == null || htgList.isEmpty()) continue;
            String id = info.get("assetKey").toString();
            Integer registerChainId = (Integer) info.get("registerChainId");
            for (Map htgInfo : htgList) {
                boolean bind = true;
                Boolean isToken = (Boolean) htgInfo.get("isToken");
                Integer htgChainId = (Integer) htgInfo.get("heterogeneousChainId");
                String contractAddress = (String) htgInfo.get("contractAddress");
                if (!isToken) {
                    System.out.println(String.format("Ignoring nontoken - id: %s, htg: %s, contract: %s", id, htgChainId, contractAddress));
                    continue;
                }
                if (registerChainId.intValue() > 100 && htgChainId.intValue() == registerChainId.intValue()) {
                    // Unbound assets
                    bind = false;
                }
                addAsset(id, htgChainId, contractAddress, bind, result);
            }
        }
        String resultJson = JSONUtils.obj2json(result);
        String outFileName;
        if (MAIN) {
            outFileName = "nerve_assets_group_by_ps.json";
        } else {
            outFileName = "nerve_assets_group_by_ps_testnet.json";
        }
        File out = new File(String.format("/Users/pierreluo/Nuls/%s", outFileName));
        IoUtils.writeString(out, resultJson, StandardCharsets.UTF_8.toString());
        System.out.println();
    }

    /**
     * Step 2, Traverse all heterogeneous chains, except fortrxAnd add4Chain, wave field separately processed
     */
    @Test
    public void upgradeTest() throws Exception {
        // Is it necessary to check if the permissions for binding type assets have been transferred? Checkerc20MinterWhether to register with Xinduo Sign
        boolean check = true;
        // Multiple contract creator private key
        String prikey = "???";
        upgradeS1(prikey, check);
        upgradeS2(prikey, check);
        registerMinterERC20(prikey, check);
    }

    /**
     * Step 3, Wave field processing
     */
    @Test
    public void upgradeForTronTest() throws Exception {
        // Check if the permissions for binding type assets have been transferred
        boolean check = true;
        // Multiple contract creator private key
        String prikey = "???";
        upgradeS1ForTron(prikey, check);
        upgradeS2ForTron(prikey, check);
    }

    /**
     * Step 4, make up for the rest12A virtual bank with separate wave field processing(Manual processing)
     */
    @Test
    public void managerAdd() throws Exception {
        List<String> seedList = new ArrayList<>();
        seedList.add("???");// Public key: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        seedList.add("???");// Public key: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        seedList.add("???");// Public key: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC

        for (int i = 101; i <= 112; i++) {
            int htgChainId = i;
            if (htgChainId == 108) {
                // Separate processing of wave field
                continue;
            }
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            String multySignContractAddress = newMulty;

            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            String txKey = "aaa1024000000000000000000000000000000000000000000000000000000000";
            String[] adds = new String[]{
                    "0xb12a6716624431730c3ef55f80c458371954fa52",
                    "0x1f13e90daa9548defae45cd80c135c183558db1f",
                    "0x66fb6d6df71bbbf1c247769ba955390710da40a5",
                    "0x659ec06a7aedf09b3602e48d0c23cd3ed8623a88",
                    "0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1",
                    "0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d",
                    "0xaff68cd458539a16b932748cf4bdd53bf196789f",
                    "0xc8dcc24b09eed90185dbb1a5277fd0a389855dae",
                    "0xa28035bb5082f5c00fa4d3efc4cb2e0645167444",
                    "0x10c17be7b6d3e1f424111c8bddf221c9557728b0",
                    "0x15cb37aa4d55d5a0090966bef534c89904841065",
                    "0x17e61e0176ad8a88cac5f786ca0779de87b3043b"
            };
            String[] removes = new String[]{};
            byte oldVersion = htgContext.VERSION();
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            int txCount = 1;
            int signCount = seedList.size();
            String hash = this.sendChange(htgWalletApi, htgContext, seedList, multySignContractAddress, txKey, adds, txCount, removes, signCount);
            // After execution, restore the signed version number
            htgContext.SET_VERSION(oldVersion);
            System.out.println(String.format("htgId: %s, Administrator added%sRemove%sPieces,%sSignatures,hash: %s", htgChainId, adds.length, removes.length, signCount, hash));
        }
    }

    /**
     * Step 5, replace the tools in the contractminterThe address is a newly signed address（Dynamic deploymenterc20MinterThe tools of contracts）Heterogeneous chain101~107
     */
    @Test
    public void setupMinterOnCreateERC20MinterTest() {
        // Replace the tools in the contractminterThe address is a new multi signature address, heterogeneous chain101~107
        String prikey = "???";
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            try {
                // Check if permissions have been transferred
                Function getCurrentMinter = new Function("minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                List<Type> types = htgWalletApi.callViewFunction(contractForCreateERC20Minter, getCurrentMinter);
                String minter = (String) types.get(0).getValue();
                if (minter.equalsIgnoreCase(newMulty)) {
                    // Permissions have been transferred
                    System.out.println(String.format("Permissions have been transferred[setupMinter], HtgChainId: %s, newMultyContract: %s", htgChainId, newMulty));
                    continue;
                }

                Function setupMinter = new Function("setupMinter", Arrays.asList(new Address(newMulty)), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, contractForCreateERC20Minter, setupMinter);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("Verification failed[setupMinter] - HtgChainId: %s, newMultyContract: %s, Failed to transfer, error: %s", htgChainId, newMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(setupMinter);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, contractForCreateERC20Minter, BigInteger.ZERO, data);
                //autographTransactionHere, we need to sign the transaction
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                String hexValue = Numeric.toHexString(signMessage);
                //Send transaction
                EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                if (send.hasError()) {
                    System.err.println(String.format("Broadcast failed[setupMinter] - HtgChainId: %s, multyContract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                            htgChainId, newMulty,
                            send.getError().getMessage(),
                            send.getError().getCode(),
                            send.getError().getData()
                    ));
                } else {
                    System.out.println(String.format("Broadcast successful[setupMinter] - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, newMulty, send.getTransactionHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to [setupMinter], HtgChainId: %s, multyContract: %s, error: %s", htgChainId, newMulty, e.getMessage()));
            }
        }
    }

    /**
     * Activate simultaneous recharge of main assets andtokenThe function of assets, heterogeneous chains101~112
     */
    @Test
    public void openCrossOutIITest() {
        // Activate simultaneous recharge of main assets andtokenThe function of assets, heterogeneous chains101~112
        String prikey = "???";
        // Traverse all heterogeneous chains, except fortrx
        for (int i = 101; i <= 112; i++) {
            if (i == 108) {
                continue;
            }
            Integer htgChainId = i;
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            try {
                // Check if permissions have been enabled
                Function openCrossOutII = new Function("openCrossOutII", List.of(), Arrays.asList(new TypeReference<Bool>() {}));
                List<Type> types = htgWalletApi.callViewFunction(newMulty, openCrossOutII);
                Boolean open = (Boolean) types.get(0).getValue();
                if (open) {
                    // Permission has been activated
                    System.out.println(String.format("Permission has been activated[openCrossOutII], HtgChainId: %s, newMultyContract: %s", htgChainId, newMulty));
                    continue;
                }

                Function setCrossOutII = new Function("setCrossOutII", Arrays.asList(new Bool(true)), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, newMulty, setCrossOutII);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("Verification failed[setCrossOutII] - HtgChainId: %s, newMultyContract: %s, Failed to open, error: %s", htgChainId, newMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(setCrossOutII);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, newMulty, BigInteger.ZERO, data);
                //autographTransactionHere, we need to sign the transaction
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                String hexValue = Numeric.toHexString(signMessage);
                //Send transaction
                EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                if (send.hasError()) {
                    System.err.println(String.format("Broadcast failed[setCrossOutII] - HtgChainId: %s, multyContract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                            htgChainId, newMulty,
                            send.getError().getMessage(),
                            send.getError().getCode(),
                            send.getError().getData()
                    ));
                } else {
                    System.out.println(String.format("Broadcast successful[setCrossOutII] - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, newMulty, send.getTransactionHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to [setCrossOutII], HtgChainId: %s, multyContract: %s, error: %s", htgChainId, newMulty, e.getMessage()));
            }
        }
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
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, txType.toString() + ", estimateGasLimitfail");
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



    private void addAsset(String id, Integer htgChainId, String contract, boolean bind, Map<Integer, Set<AssetInfo>> result) {
        if (contract == null) return;
        if (contract.equalsIgnoreCase("Main-Asset")) return;
        Set<AssetInfo> assetInfos = result.computeIfAbsent(htgChainId, k -> new HashSet<>());
        assetInfos.add(new AssetInfo(id, htgChainId, contract, bind));
    }

    static class AssetInfo {
        String id;
        int htgChainId;
        String contract;
        boolean bind;

        public AssetInfo(String id, int htgChainId, String contract, boolean bind) {
            this.id = id;
            this.htgChainId = htgChainId;
            this.contract = contract;
            this.bind = bind;
        }

        public boolean isBind() {
            return bind;
        }

        public void setBind(boolean bind) {
            this.bind = bind;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getHtgChainId() {
            return htgChainId;
        }

        public void setHtgChainId(int htgChainId) {
            this.htgChainId = htgChainId;
        }

        public String getContract() {
            return contract;
        }

        public void setContract(String contract) {
            this.contract = contract;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AssetInfo assetInfo = (AssetInfo) o;

            if (htgChainId != assetInfo.htgChainId) return false;
            if (contract != null ? !contract.equals(assetInfo.contract) : assetInfo.contract != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = htgChainId;
            result = 31 * result + (contract != null ? contract.hashCode() : 0);
            return result;
        }
    }

    protected void upgradeS1ForTron(String prikey, boolean check) throws Exception {
        Integer htgChainId = 108;
        String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
        String oldMulty = multyUpgrades[0];
        try {
            // Check the transfer of main assets
            if (check) {
                BigDecimal balance = trxWalletApi.getBalance(oldMulty);
                if (balance.compareTo(BigDecimal.ZERO) == 0) {
                    // The main asset has been transferred
                    System.out.println(String.format("The main asset has been transferred, HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
                    return;
                }
            }
            // call upgradeContractS1
            org.tron.trident.abi.datatypes.Function upgradeContractS1 = new org.tron.trident.abi.datatypes.Function("upgradeContractS1", List.of(), List.of());
            String from = new KeyPair(prikey).toBase58CheckAddress();
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(from, oldMulty, upgradeContractS1);
            if (estimateSun.isReverted()) {
                System.err.println(String.format("Verification failed - HtgChainId: %s, multyContract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, estimateSun.getRevertReason()));
                return;
            }
            BigInteger feeLimit = TRX_100;
            if (estimateSun.getSunUsed() > 0) {
                feeLimit = feeLimit.add(BigInteger.valueOf(estimateSun.getSunUsed()));
            }
            TrxSendTransactionPo send = trxWalletApi.callContract(from, prikey, oldMulty, feeLimit, upgradeContractS1);

            if (send == null) {
                System.err.println(String.format("Broadcast failed - HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
            } else {
                System.out.println(String.format("Broadcast successful - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, oldMulty, send.getTxHash()));
            }
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (Exception e) {
            System.err.println(String.format("Failed to transfer, HtgChainId: %s, multyContract: %s, error: %s", htgChainId, oldMulty, e.getMessage()));
            e.printStackTrace();
        }
    }

    protected void upgradeS2ForTron(String prikey, boolean check) throws Exception {
        Integer htgChainId = 108;
        String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
        String oldMulty = multyUpgrades[0];
        String newMulty = multyUpgrades[1];

        Map tokenAssetsMap = tokenAssetsMap();
        List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
        for (Map asset : list) {
            try {
                // call upgradeContractS2
                String contract = (String) asset.get("contract");
                Boolean bind = (Boolean) asset.get("bind");
                if (check) {
                    if (bind) {
                        // Binding type check if permissions have been transferred
                        org.tron.trident.abi.datatypes.Function getCurrentMinter = new org.tron.trident.abi.datatypes.Function("current_minter", List.of(), Arrays.asList(new org.tron.trident.abi.TypeReference<org.tron.trident.abi.datatypes.Address>() {}));
                        List<org.tron.trident.abi.datatypes.Type> types = trxWalletApi.callViewFunction(contract, getCurrentMinter);
                        String minter = (String) types.get(0).getValue();
                        if (minter.equalsIgnoreCase(newMulty)) {
                            // Permissions have been transferred
                            System.out.println(String.format("Permissions have been transferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                            continue;
                        }
                    }
                }
                if (!bind) {
                    // Unbound type forcibly checks whether the number of assets has been transferred
                    BigInteger erc20Balance = trxWalletApi.getERC20Balance(oldMulty, contract);
                    if (erc20Balance.compareTo(BigInteger.ZERO) == 0) {
                        // Assets have been transferred
                        System.out.println(String.format("assetTokenTransferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                        continue;
                    }
                }
                org.tron.trident.abi.datatypes.Function upgradeContractS2 = new org.tron.trident.abi.datatypes.Function("upgradeContractS2", Arrays.asList(new org.tron.trident.abi.datatypes.Address(contract)), List.of());
                String from = new KeyPair(prikey).toBase58CheckAddress();
                TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(from, oldMulty, upgradeContractS2);
                if (estimateSun.isReverted()) {
                    System.err.println(String.format("Verification failed - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, contract, estimateSun.getRevertReason()));
                    continue;
                }
                BigInteger feeLimit = TRX_100;
                if (estimateSun.getSunUsed() > 0) {
                    feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
                }
                TrxSendTransactionPo send = trxWalletApi.callContract(from, prikey, oldMulty, feeLimit, upgradeContractS2);

                if (send == null) {
                    System.err.println(String.format("Broadcast failed - HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                } else {
                    System.out.println(String.format("Broadcast successful - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, oldMulty, contract, send.getTxHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to transfer, HtgChainId: %s, multyContract: %s, contract: %s, error: %s", htgChainId, oldMulty, asset.get("contract"), e.getMessage()));
            }
        }
    }

    protected void upgradeS1(String prikey, boolean check) throws Exception {
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            // Only handle eth
            if (htgChainId != 101) {
                continue;
            }
            /*// Only handleheco
            if (htgChainId != 103) {
                continue;
            }*/
            /*// eth and hecoindividualization
            if (htgChainId == 101 || htgChainId == 103) {
                continue;
            }*/
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String oldMulty = multyUpgrades[0];
            try {
                // Check the transfer of main assets
                if (check) {
                    BigDecimal balance = htgWalletApi.getBalance(oldMulty);
                    if (balance.compareTo(BigDecimal.ZERO) == 0) {
                        // The main asset has been transferred
                        System.out.println(String.format("The main asset has been transferred, HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
                        continue;
                    }
                }
                // call upgradeContractS1
                Function upgradeContractS1 = new Function("upgradeContractS1", List.of(), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS1);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("Verification failed[Transfer of main assets] - HtgChainId: %s, multyContract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(upgradeContractS1);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, oldMulty, BigInteger.ZERO, data);
                //autographTransactionHere, we need to sign the transaction
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                String hexValue = Numeric.toHexString(signMessage);
                //Send transaction
                EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                if (send.hasError()) {
                    System.err.println(String.format("Broadcast failed[Transfer of main assets] - HtgChainId: %s, multyContract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                            htgChainId, oldMulty, send.getError().getMessage(), send.getError().getCode(), send.getError().getData()));
                } else {
                    System.out.println(String.format("Broadcast successful[Transfer of main assets] - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, oldMulty, send.getTransactionHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to [Transfer of main assets], HtgChainId: %s, multyContract: %s, error: %s", htgChainId, oldMulty, e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    protected void upgradeS2(String prikey, boolean check) throws Exception {
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            /*// Only handle eth
            if (htgChainId != 101) {
                continue;
            }*/
            /*// Only handleheco
            if (htgChainId != 103) {
                continue;
            }*/
            // Only handleoecandpolygon
            if (htgChainId != 104 && htgChainId != 106) {
                continue;
            }
            /*// eth and hecoindividualization
            if (htgChainId == 101 || htgChainId == 103) {
                continue;
            }*/
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String oldMulty = multyUpgrades[0];
            String newMulty = multyUpgrades[1];

            Map tokenAssetsMap = tokenAssetsMap();
            List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
            for (Map asset : list) {
                try {
                    // call upgradeContractS2
                    String contract = (String) asset.get("contract");
                    Boolean bind = (Boolean) asset.get("bind");
                    if (check) {
                        if (bind) {
                            // Binding typetokenCheck if permissions have been transferred
                            Function getCurrentMinter = new Function("current_minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                            List<Type> types = htgWalletApi.callViewFunction(contract, getCurrentMinter);
                            String minter = (String) types.get(0).getValue();
                            if (minter.equalsIgnoreCase(newMulty)) {
                                // Permissions have been transferred
                                System.out.println(String.format("TokenPermissions have been transferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                                continue;
                            }
                        }
                    }
                    if (!bind) {
                        // Unbound type forcibly checks whether the number of assets has been transferred
                        BigInteger erc20Balance = htgWalletApi.getERC20Balance(oldMulty, contract);
                        if (erc20Balance.compareTo(BigInteger.ZERO) == 0) {
                            // Assets have been transferred
                            System.out.println(String.format("assetTokenTransferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                            continue;
                        }
                    }
                    Function upgradeContractS2 = new Function("upgradeContractS2", Arrays.asList(new Address(contract)), List.of());
                    Credentials credentials = Credentials.create(prikey);
                    String from = credentials.getAddress();
                    EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS2);
                    if (ethEstimateGas.getError() != null) {
                        System.err.println(String.format("Verification failed[TokenAsset transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, contract, ethEstimateGas.getError().getMessage()));
                        continue;
                    }
                    BigInteger nonce = htgWalletApi.getNonce(from);
                    String data = FunctionEncoder.encode(upgradeContractS2);
                    BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                    BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                    RawTransaction rawTransaction = RawTransaction.createTransaction(
                            nonce,
                            gasPrice, gasLimit, oldMulty, BigInteger.ZERO, data);
                    //autographTransactionHere, we need to sign the transaction
                    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                    String hexValue = Numeric.toHexString(signMessage);
                    //Send transaction
                    EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                    if (send.hasError()) {
                        System.err.println(String.format("Broadcast failed[TokenAsset transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                                htgChainId, oldMulty, contract,
                                send.getError().getMessage(),
                                send.getError().getCode(),
                                send.getError().getData()
                        ));
                    } else {
                        System.out.println(String.format("Broadcast successful[TokenAsset transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, oldMulty, contract, send.getTransactionHash()));
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    System.err.println(String.format("Failed to [TokenAsset transfer], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, oldMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    protected void upgradeS2ForBind(String prikey) throws Exception {
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            // Only handleoecandpolygon
            if (htgChainId != 104 && htgChainId != 106) {
                continue;
            }
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String oldMulty = multyUpgrades[0];
            String newMulty = multyUpgrades[1];

            Map tokenAssetsMap = tokenAssetsMap();
            List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
            for (Map asset : list) {
                try {
                    // call upgradeContractS2
                    String contract = (String) asset.get("contract");
                    Boolean bind = (Boolean) asset.get("bind");
                    if (!bind) {
                        continue;
                    }
                    // Binding typetokenCheck if permissions have been transferred
                    Function getCurrentMinter = new Function("current_minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                    List<Type> types = htgWalletApi.callViewFunction(contract, getCurrentMinter);
                    String minter = (String) types.get(0).getValue();
                    if (minter.equalsIgnoreCase(newMulty)) {
                        // Permissions have been transferred
                        System.out.println(String.format("TokenPermissions have been transferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                        continue;
                    }
                    Function upgradeContractS2 = new Function("upgradeContractS2", Arrays.asList(new Address(contract)), List.of());
                    Credentials credentials = Credentials.create(prikey);
                    String from = credentials.getAddress();
                    EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS2);
                    if (ethEstimateGas.getError() != null) {
                        System.err.println(String.format("Verification failed[TokenPermission transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, contract, ethEstimateGas.getError().getMessage()));
                        continue;
                    }
                    BigInteger nonce = htgWalletApi.getNonce(from);
                    String data = FunctionEncoder.encode(upgradeContractS2);
                    BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                    BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                    RawTransaction rawTransaction = RawTransaction.createTransaction(
                            nonce,
                            gasPrice, gasLimit, oldMulty, BigInteger.ZERO, data);
                    //autographTransactionHere, we need to sign the transaction
                    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                    String hexValue = Numeric.toHexString(signMessage);
                    //Send transaction
                    EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                    if (send.hasError()) {
                        System.err.println(String.format("Broadcast failed[TokenPermission transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                                htgChainId, oldMulty, contract,
                                send.getError().getMessage(),
                                send.getError().getCode(),
                                send.getError().getData()
                        ));
                    } else {
                        System.out.println(String.format("Broadcast successful[TokenPermission transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, oldMulty, contract, send.getTransactionHash()));
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    System.err.println(String.format("Failed to [TokenPermission transfer], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, oldMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    protected void registerMinterERC20(String prikey, boolean check) throws Exception {
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            /*// Only handle eth
            if (htgChainId != 101) {
                continue;
            }*/
            /*// Only handleheco
            if (htgChainId != 103) {
                continue;
            }*/
            // Only handle oecandpolygon
            if (htgChainId != 104 && htgChainId != 106) {
                continue;
            }
            /*// eth and hecoindividualization
            if (htgChainId == 101 || htgChainId == 103) {
                continue;
            }*/
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            Map tokenAssetsMap = tokenAssetsMap();
            List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
            for (Map asset : list) {
                try {
                    // call registerMinterERC20
                    String contract = (String) asset.get("contract");
                    Boolean bind = (Boolean) asset.get("bind");
                    if (!bind) {
                        continue;
                    }
                    if (check) {
                        // Check if the binding type permission has been registered to the new multi signature
                        Function getCurrentMinter = new Function("isMinterERC20", Arrays.asList(new Address(contract)), Arrays.asList(new TypeReference<Bool>() {}));
                        List<Type> types = htgWalletApi.callViewFunction(newMulty, getCurrentMinter);
                        Boolean minter = (Boolean) types.get(0).getValue();
                        if (minter) {
                            // Registered to Xinduo Sign
                            System.out.println(String.format("Registered to Xinduo Sign, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, newMulty, contract));
                            continue;
                        }
                    }
                    Function registerMinterERC20 = new Function("registerMinterERC20", Arrays.asList(new Address(contract)), List.of());
                    Credentials credentials = Credentials.create(prikey);
                    String from = credentials.getAddress();
                    EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, newMulty, registerMinterERC20);
                    if (ethEstimateGas.getError() != null) {
                        System.err.println(String.format("Verification failed[Register to Xinduo Sign] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to register, error: %s", htgChainId, newMulty, contract, ethEstimateGas.getError().getMessage()));
                        continue;
                    }
                    BigInteger nonce = htgWalletApi.getNonce(from);
                    String data = FunctionEncoder.encode(registerMinterERC20);
                    BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                    BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                    RawTransaction rawTransaction = RawTransaction.createTransaction(
                            nonce,
                            gasPrice, gasLimit, newMulty, BigInteger.ZERO, data);
                    //autographTransactionHere, we need to sign the transaction
                    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                    String hexValue = Numeric.toHexString(signMessage);
                    //Send transaction
                    EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                    if (send.hasError()) {
                        System.err.println(String.format("Broadcast failed[Register to Xinduo Sign] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                                htgChainId, newMulty, contract,
                                send.getError().getMessage(),
                                send.getError().getCode(),
                                send.getError().getData()
                        ));
                    } else {
                        System.out.println(String.format("Broadcast successful[Register to Xinduo Sign] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, newMulty, contract, send.getTransactionHash()));
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    System.err.println(String.format("Failed to register[Register to Xinduo Sign], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, newMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<Integer, HtgContext> contextMap() {
        Map<Integer, HtgContext> map = new HashMap<>();
        map.put(101, new EthIIContext());
        map.put(102, new BnbContext());
        map.put(103, new HtContext());
        map.put(104, new OktContext());
        map.put(105, new OneContext());
        map.put(106, new MaticContext());
        map.put(107, new KcsContext());
        map.put(108, new TrxContext());
        map.put(109, new CroContext());
        map.put(110, new AvaxContext());
        map.put(111, new ArbitrumContext());
        map.put(112, new FtmContext());
        return map;
    }

    @Test
    public void upgradeForBindTest() throws Exception {
        // Is it necessary to check if the permissions for binding type assets have been transferred? Checkerc20MinterWhether to register with Xinduo Sign
        boolean check = true;
        // Multiple contract creator private key
        String prikey = "???";
        upgradeS2ForBind(prikey);
        registerMinterERC20(prikey, check);
    }

    @Test
    public void checkBindMinterToken() throws Exception {
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            // Only handle oecandpolygon
            if (htgChainId != 104 && htgChainId != 106) {
                continue;
            }
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String oldMulty = multyUpgrades[0];
            String newMulty = multyUpgrades[1];

            Map tokenAssetsMap = tokenAssetsMap();
            List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
            for (Map asset : list) {
                try {
                    // call upgradeContractS2
                    String contract = (String) asset.get("contract");
                    Boolean bind = (Boolean) asset.get("bind");
                    if (bind) {
                        // Binding typetokenCheck if permissions have been transferred
                        Function getCurrentMinter = new Function("current_minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                        List<Type> types = htgWalletApi.callViewFunction(contract, getCurrentMinter);
                        String minter = (String) types.get(0).getValue();
                        if (minter.equalsIgnoreCase(newMulty)) {
                            // Permissions have been transferred
                            //System.out.println(String.format("TokenPermissions have been transferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                            continue;
                        } else {
                            System.err.println(String.format("TokenPermission not transferred, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                        }
                    }
                } catch (Exception e) {
                    System.err.println(String.format("Failed to [TokenAsset transfer], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, oldMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void checkRegisterMinter() throws Exception {
        // Traverse all heterogeneous chains, except fortrxAnd add4Chain
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            // Only handle oecandpolygon
            if (htgChainId != 104 && htgChainId != 106) {
                continue;
            }
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            Map tokenAssetsMap = tokenAssetsMap();
            List<Map> list = (List<Map>) tokenAssetsMap.get(htgChainId.toString());
            for (Map asset : list) {
                try {
                    // call registerMinterERC20
                    String contract = (String) asset.get("contract");
                    Boolean bind = (Boolean) asset.get("bind");
                    if (!bind) {
                        continue;
                    }
                    // Check if the binding type permission has been registered to the new multi signature
                    Function getCurrentMinter = new Function("isMinterERC20", Arrays.asList(new Address(contract)), Arrays.asList(new TypeReference<Bool>() {}));
                    List<Type> types = htgWalletApi.callViewFunction(newMulty, getCurrentMinter);
                    Boolean minter = (Boolean) types.get(0).getValue();
                    if (!minter) {
                        System.err.println(String.format("Not registered to Xinduo Sign, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, newMulty, contract));
                    }
                } catch (Exception e) {
                    System.err.println(String.format("Failed to register[Register to Xinduo Sign], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, newMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    List<Object[]> list = new ArrayList<>();
    {

        list.add(new Object[]{101, "4d2d4d43dca8e2635f62401bfc4ffaf2cf24554b45a5b9c322bb887eeb79bfa47d4cba908df073795fe5e8cafaf3a6785b50480a35b76c8ed8f0d0c74de0f4ea1c1a75f71836562eee1b4043a3ecf0daff0753b1591ea935e3154bde01241ec42723fef7a93d19fd31aae8e8d905019d156963fc90324f0929fa9e8b8d634e4a801cf793c13ba49a223cd52d1c3dae2ffdcb112d9bb97e7bf2a75cd66537e218ae473bccba98ec994eb3c4f9600f8ceb9b753709480e29944144ce69f3519ef004a31ce09ed2d5bfec4ab4015f2f079b83bdff48e83f0004cebd1d20cb9d7589e3274d3a266d706351a29b8302fb7fbb15f738a7d161d68329c8d17adf32dbd53d3b961b016329d7f6222c29324c4c6d32c4392b4b441ddc2de583aff2db0b148efe41b11821c533a5acdf4b0dd38da00b8d56d88af3236d6290387ba3e6ce4b5d58f0681c177c45e1b0a7e1daf7390bdf4106c9df9652c92e13b8ace2a4b045a3381402e4386c11321849d565746329361df067686312ee94568c8bd24ab11be0748e36ad1b860307c6474292fb016b27d57af84d87b492efbfeaff59a515ea0b7bcef04ec77b6695512ce6840b9e1a53a227ea5cb80f04aa0b6150446bbfcbcb96b5759f5b1c608a87640530347e0e99c4871888927f38c6d4f027c81c804395ff9b53fe7f0e300f254278933bb6b9764ffde4432bbb3bb6125959cab34eb598050b8be146731c55ee3340b3986c34ac5afcebfbfa43deb872941f0abb543c9e3056ea9e03554909d5ad01dd10c2a799ed83b99c60b6d946acb8447ea7548eebd0ae7149f73bdd1c3de8a2b1f6a4f5184a555a5337977f28d454ff508441194b3920efb315e202866f5b9060ed69ef77cd668b65fa5f60060f45d42d9619c67be3d38d07a65026c21bee49747194622fd4be2a264b9ce70aa1c021a1e3bf32ed47c7acff8fa1fe347c1660df18b45df3c66a4b473bba629ffe87d3c3d06897c63f36b9a59a3db61f911b"});
        list.add(new Object[]{102, "c2535597640e246250b899d064cb3be946adc80a31a5f222c997167c18fe0814180eeac544cf9400f6c196d623bd2ac396b5f58f3d590ec08aef94596a719f541c04c03ec2cde110163cbb688373fb20387d5f70f88fa6347daaadfb1e4b2e5c6635b7d84e0139e68b2b80b172e776d9d1b594f3f2fd3d8e96de399a1a84895d241ca5d01668b6ab63293773ee77a1842cca7b263df92a3982048d3257b68b2c46df6befb29ed91cd5adb334abc71f80f01b3e6efed52e58f083ee3bab530e4b84261c104cdf81228079dacc6e90a4b989ebeda4875d70b7140b435e59cb8ee547407d3c8ef1b875f2182d243f0a05c5fe85f3f0bf747ef26d47b6696655b7b5e7f4191b57a76a0edf5709897b691210a20cfd32f7fa5109ea0955df07f5fba34c0b116149191a5a5c34498dbd57f2e368608ad00b7c7e6995c0c1302baf0533e3e42c1e1b05a67c6d1fed641a73257a5eb8167efc5a348e546b90419a2f4beb1805435c1c7e92610c056af81a1c13f5c40be645779ffffcc9d772d67fcd483e94d52fa4781cb493b9691a3b5e2b1f263fada0ee0600ce3decbf77138693b3b84b8e0c84a5405f8eeb464fed60396809abe1c96a5f58c3161e342660429e476c0abcdd1835a31ba862d5c448f5ce1985c55c3709fd0616938e9ec45516001502b9923f2586a3204e3fa1c8e3d0998e79ba86a9d84a891a55226a0beeac14ff1e1eb9f36f414e041c926105e9dc7690849f85c2ed7c9b238d4d1ab70ef59a86d9beaf7d83b79051d0365f7f5fab3aa452198f66e493ddeccee66e7d07973509b2c99bbc801da681f61c0fa24c823a3ed5daf32946f3b2abfc08ccc0b148eff1669627086f8c2ff5188f2e61793c11f69a1c5a99a22c74822cdac5f0d8cee743f38e46a908ca7d777e6f1bd7e4923f68cd6515b6992a58f5b9961ab1534c0711e8f9ea2fc992dce85b464361e7f718a38d303962361801ee194e893d9177a36e6017de59c23d1c1c02ae151b"});
        list.add(new Object[]{103, "4ea492dcbaf3ff9cc9b86fb968e1ce5bf14d52e7c388f1956ce9159dcd37b40869d53a54692a8e6f607366436e0368e4296dabf1fccc15c57ac498102bf4dfd11caaf2e14528594da8aefc2a8f8a757b544ce34b3e9ffbb5fd1f4ff810246b35176c64e138cec506a6c9b9973a09e21267802fc61c13b95643985b718a5e9f94751c1ba31c7fd5228e7d7c48eb6754dab4267047730fdbf321271ee5758fc0ac605c5f8c3fa2c055eee48be9c66d94e989b536a2287190a85df8f5c003f9d2b660d31b5c508c75a3b5c6010ca1ab646bd6a5ea8befe308c7a8644c271bfa2daaa8eb304484937c14c43e52bb482a373aa0b3fdb0971ba5ae4c3cbec8bef6bda868f1f11cbaa988db50a79649a75d4d5f8b4bb7b167657025f5f0090510bba605d7b67cdf46607c5929e80445d6143d20601128659bb54a5c99355e354fd7499b6f579adc1c5224e3079ca3c33ff64a869097176c07a65d5a50fd7feb9c89e27d0fe6289bd5157c15006fd52eaac6a5ac833f537984a11c97c18ee29a6c32ec100a8ea209131bba7468830389ef1f6c40b786f00471d43dd84df28ef603998c0111172a95fda12c637ec3afd58454059ea23b7001bb0231bf7035ff663042844588027b138abb1c7b1bbcdeaa7dc0fa692ebe57840b048b268c1ebf57e2885d9803d72d9d371f1861c87ff22fde762420be802801b4f5ebb48c588d9f24c1a122ecf21b14d472fa1bc108f742e1266656faac1390321c0e8347a703afeb305818a7b3754e339b756d44efc2086ef8b2048ac8ece07b145f19ea8e227a5bdbf9c1380a18ed60b851b81b677dd616c8216989a0ce93d2b1e48ad5b4a5dd46c83531429296bff68cd8863e71495c6f8e98fae786856697c67836a4843eb92ddaea1d06d64addfd6bba058e1bfd54622b50acbd35d30c4d63f512e7a170967f7fc995abc8048f36ec6cffae415202c9ea94b951399dbd6dc479e586cfcd93a1d4633c36bbc5581fdfabdbfb171c"});
        list.add(new Object[]{104, "1c5f3f8233b8d2b32212ee643d1ec72439bc43d8a9ddfe9a1d8fe6e6fb60537e56ba3f2ff611abcc395053446e9fa328b6dad9e3978b0a27bfcbe5dbb50300d71b36eac24ba44c4763d1cd3e593279ef096aa65303473a5965c16c6b5863f93f842dc7888ebb01987f302c6e796a723346d9e3d54e566a7f34f3cd5f67b0fad16c1b709993995e4cf6781c12f6f03eb42446778dd95df8e7688fe69f02246eeca72078ade4c8faf00436c6d46d38d989b58e60ced4c86f8503b68c20a47725c564a41b4c23e7985799cedd065663403363ef3dc518cb064ab3e3bbdf7afa6fc8f06ad94507b4c7f3cf0e4fd09e34d804c6705fbf575c8ffd9bc0c658f1ef112bd173d21bae58f600e70f622f3b7596c62e9fa0bc5f393a2b841267a011c54a037dcd1fc0651d8e8ff82054cc8e1cfc641bbd97235cab231b893fdcd3d377d095f39a8e381bdab89feec7fd5c8987d6a28ab7f131eec63b81071026f0cf859a0cdc9df15feb6daddf2f19290a5e544d53deaeab4a9c9953cf571a7c287ce5186601b24327e11bdd219648b68ca2c4faeff66765fb7b1212932e4458fc9d6feee4eaa270502e1b6f59d3a639f07a63a6be2100c479bf6d86ec7f2a16c86f43f1062d57629415cb1bce9d4fcce71163e2701d77db789970958a552ddab0a0f49475657701b48918366791dd9748e026c32ec98e2d81424d1026da333d4992a3c4d7c452ee765474591bea407338479fd6f670d5328fedacc98a81e4b3c7fa882eec7701919ff0e2fb6076346f79dd11ebe3c1729ad4ec28e3809c59b61d2f865f49d82186ace822848b1cf4c8b4de553ffd8989b26943a1795840e3b15bec386c14c920e6a300607a483c78c7eb49fe18ca7322e1d715ede563e87874b3f0ffe8d376c99e72ecdc53ea881c8812613004a6704e037d0d785098d2d4a0a8ff54cfda86004b48de15739212d37baddd97fc218b6cdd5ba77eaf0d45d7f1bd44a17cdbee8e06cffa3af859f3e61b"});
        list.add(new Object[]{105, "d25347a959502793f5f0715c719c36bc27985dd5e563a559c9abf9ea1cdec04c2c7b4d02d52b7b929b622949d9931671a1b0b5110deeb4dff4fc7a9beb5f16ca1b6eb9d08998a7a3d47b1e63e808e3e3476606007fa97c50a55c53cb97e8dccd5232d237ccbbfa14f5893a89aac63d2687c35cbdebc91b52eb4c552cdedce466171ced6c1fcaab2f9b23764562bc4e05eb95e071ee346a1340561d05520462e15b3b5bd97246740b238b8f9e26e04d6100fb81437961628113a98a876f47855e9a7d1cb9bc77bbcf98d9e3112e6e8317a95fb26ca69ab743ec65e61561eadf2c71c50963e28c8b13cbc459ae2c1db9e5e5120376eda616a0a5abd99835c54c66289f631beea42bb57cb0487cd5c18b8d0ead007dcaee0568b00aff293788cd7831fe2fa1687df172c2b0de98464f9b1a952e6eb79597b71fa61aa3d5235bf86d5da944921b388aa8b42522085a69f6f67d448e6ed146766849a7117d8d04b7fbd3461e02464b8bf92e9352965e3376ac8f55bb59aa751b7d1ac09349d1e38c7558046400a51bb60b5b0218a507bc8503a6b37c49c8166cee659762d1f7a4c56d55305d83e5e67942d304e5615bfd05fbb65cab179f258fad236c072008e609aa5cca0c0256901ca0d1ef9d937c016ee2eaa7450aa57cb8a6d364e8423c55388634dad6564373da08823b0fc6ec55fe0d8da7a68e01e4e0c9e854ff170422a7818914674541e9271c2af3bb60076bf41b6f3c3012cd73410851e3dd406aaceb948b830d2f12a465ca2b08274a79f8c54fd22982d896de23d3b324c6f51860f2f5e51df108ec3374741ce4c72064c66f43e3d3448e1bf897a88a1fa3298c7557babf32d5435af61441701ee55a918414025e3dd3f6cdbe8ba5d01e1dbd1dcd2c2d1b53e4fcdec59962471c375be158e0908bb8d5e582236cb01dde92a6702e1cf93ff74415e6d193b4b7480884ab50b9d7d7ba6ccd759d88a4d13eaed89a19073878e1cf7c6cd5a225bcb51c"});
        list.add(new Object[]{106, "874c6586381afa4233dc3668caa53e85febde384116d4448966916fef03c49895eaecadda4fc451231873bf0ef7b4466281248b453007e6d8cba277cc3501f0d1bdf097d608283daa5969af35ef49c7403abde23af61ea2d47f450ddf69e68599e1c59d78a6cb43eae5137794955803cbf789496b47962213d72235faec105964c1b17752d64e05e9973a8edd07c15c808c68b2849deb8d591260cf03468951cdd574b607d4cdc444134f755fe220cda81a4ad03b37643043eac9d608fbc479305701b0a8808b57e0de3fa351e4f4edeeaf741d1ff715e083529df2555430d4ec8a8372790894a7f45b4ddcf60c34611dd24e8110d352cda22a59d322aacd890f6a0fa1bb397e5a59acf413b5b543b13bf6e12b8e8ac7ebc2c163869f26a64a4af78225f750db0d7000c78aa16b1b994a520a36e913c6f3ff290063b5b33137da12662c01c881f761164b9242a7dad422f8043c68f18b114412a32dad8b5111a96f02f71bc4b480c9d22375b53ee4fefba00efd75e8d7950871983000d0f4f9c2749c63fba1b6b331de00bf389fa10fde4dceaeda5df9ea4bc8e44b6a617443c0368b4f114393a42c4598482c1305d10a58f5e45959d466f192b484c759d0c42b07041f9ce091c96b369b60248ad1e59609c7a1a2e92229ba927f8add47e24bd0a90ccd5e6d27178ff284cc55492486c925c5188d500ac0f2430ab5ba23f9b681230961fbadada1cee474b16e664a1415da17454ae0b852ff7f66c2ec744eb5048691cb556317f68476b5599a49171b68c14508a7d4596971904a2da08fc22628a2fff414e4753d61ba4faa029df1e458a3fa96bcce2cdcb3395f066f660cb0e87497d0e50590a0a954c24a3fc1e46be5e1c0bea9109bceecc6a82793ff3cf82015d3476decbac35c31b1f48565109d81c80019ac6ad36c13dc5da157343ec1a5b93bac2df8d284242ff066c448e69b0aeb1213cae658dcf961e2276aa36438c9acd6078ff39249a5d311b"});
        list.add(new Object[]{107, "58a2f50d0985a0262d7137f815a8aa8625353d50fda276cb500f4a42db581429352753d1b6c8cff940ca327e116eb31d83b37113fa6cb25347a95e9b7c1ea0a01b8534d4ec5750eca9986c1d59ce05b256bd3713cacb29fbacbc892323af8635107df5cf7653592b0a7b3a5e331da9dd3198ea93cf9ab70916db485eabe43945741bf6147da5ae635c8da674d700a4e5fcfd0b59ee6e1b52b98ad5f1b0f91e07dc255e1faffd62e26f0c079db40ee557bbefdf9d734e3eba249fada0d3f37946fda41cbc3e5030298a699e1b603a0285ab37f964a672ce6b1cf1d63db3594c134ba8b021397127e5f1a8f66874f23aec2c1ee6e677243ac9899286a2417db70a49c7e31bac466ebf0b8f70ccb829379c642b8c50953c15b7bbfcb5b4dd8d4fc7866c2eaa7e66767e66e7a7c6de10bb7f9d8c64039fa2d1a33dc5fd8c8b99b8fc26a3cfb61bd65ed5a54bb5956d105397b33b407628a3fba4b0cf303ed96e78d5e355dce2b8409a6f23d4a31a88d3a2104a4eed4b7d6424c56dc3a489aae5fee2f5e7faa7d51c161467182892dda1c638ccd2b2e34d27b9bffdadd854bf0b007f38f88e1ed08a618f6ef8f0f7762525e8b04b99e22d88d5985607ca93fc244142f6b58155d16d1b5c8d164154254fa90d02d5e4f630a9580e252dd5c2a53d7393f87f0bfc4db88f469649019ae54499291379020847d5ae5fbb3406d186bc0b78a1e35659788ba51cbc2ec3d319f4abd75d741a07029f4bda612f6495282e9ba5d00a2ecf15f4acd0163b3b25485fbdeb7191717f70c2a51f9a70f5d44f01d6be0ebe50a228da4c5c1bcf695e77c28dcef05de9c47b4b9f1697ec9d1a577242fd0d7791b9f328032ed46d239044038d97882f3f10a0078e63cbf949f2e55f6a9e75a06858016a6b13cf1b8c17a0e4895bcefd2870b64338b6c200c9a82b7fa1926c14462468591e5398a40e4bddd4aa64dae7b31cd6f4b80e5d42acf963843b80ad26e18ede22dc33fc9b1c"});
        list.add(new Object[]{108, "b73d2c696934b0f049cda6bea7200230f606bfcf78680aa221cb76605be9d46241fabd3bea866dcc0e30ce72a0db7724212718c50f9d35256233391f2c57e0111b1ac9115f7ab6ae5b8f0b74f71b53828ae650f8b62e0d18a78c2f63181e0633091d55daf677767b213f318861bc025d64d594cbbbe3cba9cb4b9ce32c88f9929b1ce94a5955f1efa07736457a5f28f2152a5ebdf27f525821b86667b17704f4ec8556e02d78705421a060b3381f470cf640a0f45b48ad48e21c4c534ac6ad7f0b091bf2eb6cd05657beae47d852d55e309250f002c4ac7db7849aa3cc56effccbcf1e52601664e16fa370aeca810375230755eef50300baac057eb404092420ce52af1c8d4b11aed3cccdfe316c644f3fe53f0a03cc02a935801e54cf8dc000ec8e72d322ffbff0f734d5660f983d6c56ba77c97bef7f1c48f5ee132d9e16b3f95228981c3f4f3e77e87e04702e0f83284e11b81007b06ffdb565fb6cc93f5a727ac49ab029184814bcd1d02f691fe26186d94f03ddc38c62929aea352e1732833e758ee81b15d30965d7b48812f4ff350b126b2d8bca7857711ff0be69e6c73c36819889c37ac3387552b942028f58874390c41504c1aa39e6d89acceb409526f17a65dc171b53346a97b9e9a36b874d827f5d42df0b5882e2a8d9efe951e301b0d953790dd805b8cf407fac695c260ca61266a0954794a6575a979447a3e67769c4960dba301bd40507005b87402ef4e71a66389483e8d976754b0ff5b0226fcaeb405e393b1919d7cc01408cff81dfad9e0cac28de661e86fda472f8519e10690635fd6a37f41c42976d3a8b43eccbdc420e52d76fdc7fbff4a39c1a43d5bccb4db7932b28adf1399feb78d66568799c2df5974d1acc8690ae461531e876986ebf11c1553106ec1b14a2d89af06ce53ea587770e46e8ebf62f8d64f4a71b76cb8b059a5a7d1051d347f3a1a99eba833a2ccb177a5e9e51bf6c22a8a7142b8fb2282c286dc38b6b251b"});
        list.add(new Object[]{109, "e9b5922ba67d1dd021728561d47d8dae8e71fb7220b8932ebe69bfcf7fd1fe1517eca05a5bb41842eb93b29c42acd3b917bd8376a668dae3e44aaf7f565ee9001c2dcdd097e169383515852f267e44911d3873662dbce58599092ad18b3eb404e545b45176e8984a595764c0dad0ff46dfce33a4894c0ac3838d618756dbc10aa81b561be789752feb6712cde83e793844d128f6d66e07bd57b78d088e4d727c3f9144d6e0447570b410a0128eac121b83e2437a4cccc7d885c8f90e58bb5c0b82c91c76c3b6d444130a494c313640d420313138a667ad7ef0a4f4e9b6609008ba03017baa0fde76aa288d2ef0ebb6b4d93e695a45b869564edc91bb8567f0cd9eefc41b033f26abf52e6e61469b41d83b0f9d226caf58fc64d8e35f2748b236046130056392c232ae590e8f8eecbd1a5dcf60010c0471a83b0ab176116bfadaaba0783b1cffd57e5fc43f9de031780b66dc81f467ffe92b96efae07e1e350b8466d869d6f027763afca0520051d69e24ba7ef91d1f859cff17c6a3d1c598b9dd27d1725681b56add28996cb91ec56468b641f1984a26165cd83a115d9530a7d079838a21e2f707698daa55918c08402e21e091ecd05011d2cddea4d88bce1de6e12c3edb8d81c31abd97f83eb129b86b27b5bfb9a1917a9719e27119230eb6fc1a68597b1dcb60e53ffddd6d2705bb0e0105ccb23f3fd3ef11ef8574a29b6127e1a56cb5b11ac1c935bafea7625612150976cdddb06ef6cf199d9ef73a87b119d3730e383e2d1b5123639ddc3b5e4f6f3de6d1a7cfd3635a613e957a707846ef177b39a63b51ef81cfa07a64ed35234661d4fd86640cf247401b9c81f42b1abde3958757641c72901266fdd6121ddba3e65c8cd11cc519539516079b3340d38d851a5a13dbd1e64011cbf19c50f2c0da230ab6dc5d85d174bb69f4648d23c6834675d03477153362dc60daf64b2d19c2d6d0da5d73d8ba263600eb1e0818304412da17590b476ab6d941b"});
        list.add(new Object[]{110, "d3103bf2cd49c769873a9ea1ca858c4e0d56a556d48d070d6f76862f9fed96013a34ee7671818b52ca6b91fb1025f051c6d2b7341fc172ed8aa755e2ac365bbe1c12f5ad5cba23f5b61167b138952463128729079f706bb5e7cb7e19e9e1ad07be3499a42bf67be99c1ae1d51dc734357569672fadf9257b8afebc6a04bd81c65d1ba137e08e81d620c85b35a6939e313e327916d99df28bda595a4da987445e0fd9742f0e2d210dc1b1c9f7f7e0a1697d56f8d47a3a2971d28140818b9db5c036da1ccd52fe1f0fdeef626e00759a2dd2fbcd7fdd25372ff2ef33f7cf8b2f2b8a80351deaef17dc1aa2e42f28cb19fd07e7e757a5e5fb6fb94cce4addcfb43c092a481b79fc183df02afafb0b3748ab80a8f786430124617341a55b774f0d693e68416b467c1a6e28fffafe9aebb4dd48a88d8cc89447d0b6a7c74dc1b93071bf3e33231b296feca14150240b890e662aef90f13b9da61ed1bd75bd4c3190d65f6d0509e5108bc8988f740a643062bdc1489bda1c1256e7d7aeaa692fefec140320c683b41b2463d45f8ef624e9f152c2d13b91b60bbd0eae94353ba442533adeed7412c9ca18dde0b0efe3e64b3ba2df1e01f3425fe4bd91482b32217b904b9ce752acf7511bbc3456cf6a3bb564d161040e2015414ffd3a3d849362b6cdca117f21fdbbb401022f396a539d676a489039086e9cf164aa461f293d5e0bbe03c7ebe15dec2ad01b492333819b30955b6bea2f78996341f045afcf3e859902e06884404821d229296d9f99d6092532a1b63f35cef5d238810941b38cd883d4d9ccd15f8f1f6c808b1bd243221770d1e53f75390e1db75ae194bf0f38348933e6cccee2643dfd04abd879f00cf585e10b131ac6961742f4e44df24878efb1af5b74fa9cfbc4613598241c2272c38e54fe234574cd3ed71fdf4ad8361802b4a496f982a3727c0ff72361002090c41e831466f7726188abf21192500f333be4c9fd43985501cc7ec88f9da31b"});
        list.add(new Object[]{111, "8711ae334ede52bc4237d1a5a38e5d6709af6897e9f4faa92b3545003c5f2d3b3c51e24245951d63bc2c8fbc8dd6ef34edfb0012f63993aefef2f4df06c6e8471c7389ed83b4bf74e4012029d233f0e90b4a707546a82dce1f7b8e2d3114352a01214da18a73a0d79a69f8aeb17fb96b29efa705918429b27ede3d27d51918a78d1c58c87068cd46ecfb39d43c11dbe74934a5c8416d513f196a2b3a45344c59624a23b2b4cd97c6a083c6921959a964364c1d0d51f2a93310a88eb8e327af1c1b351c070c37f8fec38e4321dae90441ff99ef01dc5b0bd9edf57391125676986f0cd429c51a0a4eca70c9be333f7ee3cb927d7e5a000a6cfc7ae091ecc40623a1869e1b24586d6849725316dd9bb693c14c5f7e178f925d68ead8e534ff29a3cd9812447dae970f88d186878acf7788b9acc40a30aec14c4afc2a35cdade3c26f1559351b3cdde93467f213ae98e1eb12b52c67bf1c1bfaa7913a093c354736c2206497d750169d2fc9a947618862884dfd454e7a94ca7597ef8add616db5c6b37c5d3b3b1b99320886ce2ae7ee46e0bff48a5de8c3875554f8c8c1c89d0790a3a2791327e37c89bad025039ae3d719251744b371085d8911c1353f5f8a8e68536edf6c23431ccb2d77efbf90edd110d8329f573ada514356241b4b6ac8e7be0252b280e055680be91dddd9f4565a63f28f78ca91dfb44a6cff08cfddfc99a3c789bfb91662761bda56bd30a94b4f19167b94a8d8844eff43d16c980ba0308a29d211d3a94207430875643f2fabee7322e1b8ecd5a6730c532ae325a51f114aa966b743e5004d781c024b6177f6194f3d2891813f04d7ab5d544e953f3c49b2e18afd37aa080eb86c578a68229271c61e3000b813f407708b68c754e3b353f221bb26dd4656dbc25c1cccefcfc8c8c3b7f5537c3acdaca268829a02448ec120dfe961918297566dffcb061ad4e65da78de979670f4699e91f337f94526dd009fbe65b378001fc50f88f1b"});
        list.add(new Object[]{112, "04ea12c1c4e5c8740ae8c3927dc159ce7d4c96195d23271416fb857cd758977323e7e3ccf1caf120cad854e2536a6a7a358efb4f5bbf2f9ea9fda8ad2e47c52b1b8c6135998d6aa7f46b8a1b89f32b759c4bdfa53530eee37550fa63c459915ad07df58107722fc9ce9e684d1d10c6a33cbecd12f0677ec4b0e5bcdc158955b6901b18faecd99582dc73797f6c36623e037c217200d15c48221b445e51c47e51b8b62042b64375e377917a5b7157c4a9b3b3aee23f1db91dfbe66257a654e80195771b67db2e4a1cc0e315a869074ec7489b3ff602905b595b1cd6e0e8652126aabccd002f8709ea67546fe9dcff3156e04dcd138c843373fca30c0676e08e771af6e11bfeed2f429b397b44eb660499e13e1ec67dd55040198e96695edeab92d6e700cb3a2b9e73f27f427965bacfbcafcbfa066a7cef11a813ae7b2fc94b13f89855641b84ae33dc3b2a275a6c281606fe0a6285b49868ad5432c2bd2f7fd51eb643feed6e4e32fd754e52bed9e4a004132ea024a3bbd958db27c0fb768ad2bb01be16d11ced549b271c14f337ce596b674d65edcfabce1ee820db444584aa0a55e521d3f545b8c82397f822b9173484ad0bdd9f935ebc5e78404eac3a72fddaed63a1bae71cf690d7289adc2f931c3b5cd7d979da96edf38af572a4e8b27f9d256e78491ec50adf3f490cdf362c8393e2ba89adb578e50a726d6bcad3018285b0d5c95fb9a41b6aeb57d9b578c0a0c1734c9cfbdce0424ed565c5f2f6d670aa797cdc2205b8fe177b8919e4c06500b8834ed8e8548f8fea758c25e001fc7d0dea5427865d6b2f1cc7e387827c040b22db6de61479e017c442f0f68b75fd98a61b663b8dafd625b03d220d8314778861dc6991731d741928f1ac7fcbf6ea80227316493c1ae3d8951c78a2da905cb1c52f70a9b0927df606c40ecd79031de546039399e20f7bc4e48c435b22474d6eafdab4b85f875f9fdc99fab1f0c3e534400b94a4a5ee83b437151b"});

        //add: [0x8255a0e99456f45f8b85246ef6a9b1895c784c9f], remove: [0x78c30fa073f6cbe9e544f1997b91dd616d66c590]
        //add: [TMrMRaEqAJycTGVvjv71nvxwLXBV4pgFH6], remove: [TLyjmA5vqqHESxEocsdEJHCsNegmax8FGA]
        //4e7864181e774271a799c2ae99ee6bfd17547edeaa5b370daf7991d45cc8d41e

    }

    @Test
    public void signChangeTest() {
        String priKeyOfBB = "???";

        String nerveTxHash = "4e7864181e774271a799c2ae99ee6bfd17547edeaa5b370daf7991d45cc8d41e";
        String[] addAddresses = new String[]{"0x8255a0e99456f45f8b85246ef6a9b1895c784c9f"};
        String[] removeAddresses = new String[]{"0x78c30fa073f6cbe9e544f1997b91dd616d66c590"};
        String[] addAddressesForTrx = new String[]{"TMrMRaEqAJycTGVvjv71nvxwLXBV4pgFH6"};
        String[] removeAddressesForTrx = new String[]{"TLyjmA5vqqHESxEocsdEJHCsNegmax8FGA"};
        int base = 101;
        for (int i = 0; i < 12; i++) {
            int htgChainId = base + i;
            if (htgChainId == 108) {
                // Separate processing of wave field
                continue;
            }
            HtgContext htgContext = contextMap.get(htgChainId);
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            try {
                String vHash = HtgUtil.encoderChange(htgContext, nerveTxHash, addAddresses, 1, removeAddresses, htgContext.VERSION());
                //System.out.println(String.format("Change the signature of the transactionvHash: %s, nerveTxHash: %s", vHash, nerveTxHash));
                String dataSign = HtgUtil.dataSign(vHash, priKeyOfBB);
                System.out.println(String.format("list.add(new Object[]{%s, \"%s%s\"});", htgChainId, list.get(i)[1], dataSign));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(String.format("adds: %s, removes: %s", addAddresses != null ? Arrays.toString(addAddresses) : "[]", removeAddresses != null ? Arrays.toString(removeAddresses) : "[]"));
            }
        }

        // Wave field
        HtgContext htgContext = contextMap.get(108);
        byte newVersion = 3;
        // Set a new signature version number
        htgContext.SET_VERSION(newVersion);
        try {
            String vHash = TrxUtil.encoderChange(htgContext, nerveTxHash, addAddressesForTrx, 1, removeAddressesForTrx, htgContext.VERSION());
            String dataSign = TrxUtil.dataSign(vHash, priKeyOfBB);
            System.out.println(String.format("list.add(new Object[]{%s, \"%s%s\"});", 108, list.get(7)[1], dataSign));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("adds: %s, removes: %s", addAddressesForTrx != null ? Arrays.toString(addAddressesForTrx) : "[]", removeAddressesForTrx != null ? Arrays.toString(removeAddressesForTrx) : "[]"));
        }

    }

    @Test
    public void signValidationTest() {
        // Administrator account
        String fromAddress = "0xd87f2ad3ef011817319fd25454fc186ca71b3b56";
        String fromAddressForTrx = "TVhwJEU8vZ1xxV87Uja17tdZ7y6EpXTTYh";
        String nerveTxHash = "4e7864181e774271a799c2ae99ee6bfd17547edeaa5b370daf7991d45cc8d41e";
        String[] addAddresses = new String[]{"0x8255a0e99456f45f8b85246ef6a9b1895c784c9f"};
        String[] removeAddresses = new String[]{"0x78c30fa073f6cbe9e544f1997b91dd616d66c590"};
        String[] addAddressesForTrx = new String[]{"TMrMRaEqAJycTGVvjv71nvxwLXBV4pgFH6"};
        String[] removeAddressesForTrx = new String[]{"TLyjmA5vqqHESxEocsdEJHCsNegmax8FGA"};

        int base = 101;
        for (int i = 0; i < 12; i++) {
            int htgChainId = base + i;
            if (htgChainId == 108) {
                // Separate processing of wave field
                continue;
            }
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            String signatureData = list.get(i)[1].toString();
            System.out.println(String.format("validate%sOnline virtual banking change transactions,nerveTxHash: %s, signatureData: %s", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData));
            try {
                Set<String> addSet = new HashSet<>();
                List<Address> addList = new ArrayList<>();
                for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
                    String add = addAddresses[a];
                    add = add.toLowerCase();
                    addAddresses[a] = add;
                    if (!addSet.add(add)) {
                        System.err.println("Duplicate list of addresses to be added");
                        return;
                    }
                    addList.add(new Address(add));
                }
                Set<String> removeSet = new HashSet<>();
                List<Address> removeList = new ArrayList<>();
                for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
                    String remove = removeAddresses[r];
                    remove = remove.toLowerCase();
                    removeAddresses[r] = remove;
                    if (!removeSet.add(remove)) {
                        System.err.println("Duplicate list of pending exits");
                        return;
                    }
                    removeList.add(new Address(remove));
                }
                Function txFunction = HtgUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, 1, signatureData);
                // Verify the legality of contract transactions
                EthCall ethCall = htgWalletApi.validateContractCall(fromAddress, newMulty, txFunction);
                if (ethCall.isReverted()) {
                    System.err.println(String.format("[%s]Transaction verification failed, reason: %s", htgChainId, ethCall.getRevertReason()));
                    return;
                }
                System.out.println(String.format("htgChainId: %s Transaction verification successful!", htgChainId));

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(String.format("adds: %s, removes: %s", addAddresses != null ? Arrays.toString(addAddresses) : "[]", removeAddresses != null ? Arrays.toString(removeAddresses) : "[]"));
            }
        }

        // Wave field
        try {
            HtgContext htgContext = contextMap.get(108);
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            String[] multyUpgrades = multyUpgradeMap.get(108);
            String newMulty = multyUpgrades[1];
            List<org.tron.trident.abi.datatypes.Address> addList = Arrays.stream(addAddressesForTrx).map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
            List<org.tron.trident.abi.datatypes.Address> removeList = Arrays.stream(removeAddressesForTrx).map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
            org.tron.trident.abi.datatypes.Function txFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, 1, list.get(7)[1].toString());
            // Verify the legality of contract transactions
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddressForTrx, newMulty, txFunction);
            if (estimateSun.isReverted()) {
                System.err.println(String.format("[%s]Transaction verification failed, reason: %s", 108, estimateSun.getRevertReason()));
            }
            System.out.println(String.format("htgChainId: 108 Transaction verification successful!"));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("[%s]Transaction verification failed, reason: %s", 108, e.getMessage()));
        }

    }

    @Test
    public void managerFix() throws Exception {
        String priKeyOfAA = "???";
        // Administrator account
        String fromAddress = "0xd87f2ad3ef011817319fd25454fc186ca71b3b56";
        String fromAddressForTrx = "TVhwJEU8vZ1xxV87Uja17tdZ7y6EpXTTYh";
        String nerveTxHash = "4e7864181e774271a799c2ae99ee6bfd17547edeaa5b370daf7991d45cc8d41e";
        String[] addAddresses = new String[]{"0x8255a0e99456f45f8b85246ef6a9b1895c784c9f"};
        String[] removeAddresses = new String[]{"0x78c30fa073f6cbe9e544f1997b91dd616d66c590"};
        String[] addAddressesForTrx = new String[]{"TMrMRaEqAJycTGVvjv71nvxwLXBV4pgFH6"};
        String[] removeAddressesForTrx = new String[]{"TLyjmA5vqqHESxEocsdEJHCsNegmax8FGA"};


        int base = 101;
        for (int i = 0; i < 12; i++) {
            int htgChainId = base + i;
            if (htgChainId == 108) {
                // Separate processing of wave field
                continue;
            }
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];
            String multySignContractAddress = newMulty;
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            List<Address> addList = Arrays.asList(addAddresses).stream().map(a -> new Address(a)).collect(Collectors.toList());
            List<Address> removeList = Arrays.asList(removeAddresses).stream().map(r -> new Address(r)).collect(Collectors.toList());
            Function function = HtgUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, 1, list.get(i)[1].toString());
            String address = Credentials.create(priKeyOfAA).getAddress();
            String hash = this.sendTx(htgWalletApi, multySignContractAddress, address, priKeyOfAA, function, HeterogeneousChainTxType.CHANGE);
            System.out.println(String.format("htgId: %s, Administrator added%sRemove%sPieces,hash: %s", htgChainId, addAddresses.length, removeAddresses.length, hash));
        }

        // Wave field
        try {
            HtgContext htgContext = contextMap.get(108);
            byte newVersion = 3;
            // Set a new signature version number
            htgContext.SET_VERSION(newVersion);
            String[] multyUpgrades = multyUpgradeMap.get(108);
            String newMulty = multyUpgrades[1];
            List<org.tron.trident.abi.datatypes.Address> addList = Arrays.stream(addAddressesForTrx).map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
            List<org.tron.trident.abi.datatypes.Address> removeList = Arrays.stream(removeAddressesForTrx).map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
            org.tron.trident.abi.datatypes.Function txFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, 1, list.get(7)[1].toString());
            // estimatefeeLimit
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddressForTrx, newMulty, txFunction);
            if (estimateSun.isReverted()) {
                System.err.println(String.format("[%s]Transaction verification failed, reason: %s", 108, estimateSun.getRevertReason()));
                return;
            }
            // feeLimitchoice
            BigInteger feeLimit = htgContext.GAS_LIMIT_OF_CHANGE();
            if (estimateSun.getSunUsed() > 0) {
                // Zoom in to1.3times
                feeLimit = BigDecimal.valueOf(estimateSun.getSunUsed()).multiply(TrxConstant.NUMBER_1_DOT_3).toBigInteger();
            }
            TrxSendTransactionPo trxSendTransactionPo = trxWalletApi.callContract(fromAddressForTrx, priKeyOfAA, newMulty, feeLimit, txFunction, BigInteger.ZERO);
            String htTxHash = trxSendTransactionPo.getTxHash();
            System.out.println(String.format("htgId: %s, Administrator added%sRemove%sPieces,hash: %s", 108, addAddressesForTrx.length, removeAddressesForTrx.length, htTxHash));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("[%s]Transaction verification failed, reason: %s", 108, e.getMessage()));
        }
    }


}
