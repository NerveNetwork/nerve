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
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
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
import java.util.*;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.TRX_100;


public class RetryVirtualBankTest {

    // 设置环境
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
    ApiWrapper trxWrapper;
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
                    // 波场单独处理
                    continue;
                }
                HeterogeneousCfg cfg = cfgMap.get(htgChainId);
                HtgWalletApi htgWalletApi = new HtgWalletApi();
                Web3j web3j = Web3j.build(new HttpService(cfg.getMainRpcAddress()));
                htgWalletApi.setWeb3j(web3j);
                htgWalletApi.setEthRpcAddress(cfg.getMainRpcAddress());
                HtgContext htgContext = contextMap.get(htgChainId);
                // 设置新的签名版本号
                byte newVersion = 3;
                htgContext.SET_VERSION(newVersion);
                htgContext.setLogger(Log.BASIC_LOGGER);
                htgContext.setConfig(cfg);
                Field field = htgWalletApi.getClass().getDeclaredField("htgContext");
                field.setAccessible(true);
                field.set(htgWalletApi, htgContext);
                htgWalletApiMap.put(htgChainId, htgWalletApi);
            }
            // 初始化波场api
            int trxId = 108;
            trxWalletApi = new TrxWalletApi();
            if (MAIN) {
                trxWrapper = new ApiWrapper("tron.nerve.network:50051", "tron.nerve.network:50061", "3333333333333333333333333333333333333333333333333333333333333333");
                trxWalletApi.setWrapper(trxWrapper);
                trxWalletApi.setRpcAddress("endpoint:tron.nerve.network");
            } else {
                trxWrapper = ApiWrapper.ofShasta("3333333333333333333333333333333333333333333333333333333333333333");
                trxWalletApi.setWrapper(trxWrapper);
                trxWalletApi.setRpcAddress(EMPTY_STRING);
            }
            HtgContext trxContext = contextMap.get(trxId);
            HeterogeneousCfg trxCfg = cfgMap.get(trxId);
            // 设置新的签名版本号
            byte newVersion = 3;
            trxContext.SET_VERSION(newVersion);
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
     * 读取每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyUpgradeMap() {
        if (MAIN) {
            return multyUpgradeMapMainnet();
        } else {
            return multyUpgradeMapTestnet();
        }
    }

    /**
     * 测试网: 配置每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyUpgradeMapTestnet() {
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
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
     * 主网: 配置每个链的新旧多签合约
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
        // 配置每个链的新旧多签合约
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
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
     * 读取异构链基本信息
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


    /**
     * 签名交易
     */
    @Test
    public void managerChangeSignData() throws Exception {
        List<String> seedList = new ArrayList<>();
        seedList.add("???");
        seedList.add("???");
        seedList.add("???");
        seedList.add("???");
        seedList.add("???");

        String txKey = "83f2cd331161e8050de471ac85b25fb48b3e6246864a34635abf9cda8f520e7b";
        String[] adds = new String[]{"0x5C44E5113242Fc3Fe34A255Fb6bDd881538E2Ad1"};
        String[] removes = new String[]{"0x5FBf7793196efBF7066d99fa29dc64DC23052451"};
        int txCount = 1;
        Map<String, String> signMap = new HashMap<>();
        for (int i = 101; i <= 107; i++) {
            int htgChainId = i;
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            String signData = this.changeSignData(htgContext, seedList, txKey, adds, txCount, removes);
            signMap.put(htgChainId + "", signData);
        }
        // 处理波场
        HtgContext htgContext = contextMap.get(108);
        String signDataForTron = this.changeSignDataForTron(htgContext, seedList, txKey, adds, txCount, removes);
        signMap.put("108", signDataForTron);

        System.out.println("签名数据: ");
        System.out.println(JSONUtils.obj2json(signMap));
        System.out.println("--------------\n");
    }

    /**
     * 补发交易
     */
    @Test
    public void managerChangeSendTx() throws Exception {
        String signDataJson0 = "";
        String signDataJson1 = "";
        String priKeyOfSeedAA = "";
        Map<String, Object> signDataMap0 = JSONUtils.json2map(signDataJson0);
        Map<String, Object> signDataMap1 = JSONUtils.json2map(signDataJson1);
        String txKey = "83f2cd331161e8050de471ac85b25fb48b3e6246864a34635abf9cda8f520e7b";
        String[] adds = new String[]{"0x5C44E5113242Fc3Fe34A255Fb6bDd881538E2Ad1"};
        String[] removes = new String[]{"0x5FBf7793196efBF7066d99fa29dc64DC23052451"};
        int txCount = 1;
        for (int i = 101; i <= 107; i++) {
            int htgChainId = i;
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];
            String multySignContractAddress = newMulty;
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            String signKey = htgChainId + "";
            String hash = this.sendChangeWithSignData(priKeyOfSeedAA, htgWalletApi, signDataMap0.get(signKey).toString() + signDataMap1.get(signKey).toString(), multySignContractAddress, txKey, adds, txCount, removes);
            System.out.println(String.format("htgId: %s, 管理员添加%s个，移除%s个，hash: %s", htgChainId, adds.length, removes.length, hash));
        }
        String signKey = "108";
        String hash = this.sendChangeWithSignDataForTron(priKeyOfSeedAA, signDataMap0.get(signKey).toString() + signDataMap1.get(signKey).toString(), "TXeFBRKUW2x8ZYKPD13RuZDTd9qHbaPGEN", txKey, adds, txCount, removes);
        System.out.println(String.format("htgId: 108, 管理员添加%s个，移除%s个，hash: %s", adds.length, removes.length, hash));
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

    protected String ethSignForTron(List<String> seedList, String hashStr, int signCount) {
        String result = "";
        for (int i = 0; i < signCount; i++) {
            String prikey = seedList.get(i);
            String signedHex = TrxUtil.dataSign(hashStr, prikey);
            result += signedHex;
        }
        return result;
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
        // 估算GasLimit
        EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        BigInteger estimateGas = estimateGasObj.getAmountUsed();

        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            if (estimateGasObj.getError() != null) {
                Log.error("Failed to estimate gas, error: {}", estimateGasObj.getError().getMessage());
            } else {
                Log.error("Failed to estimate gas");
            }
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, txType.toString() + ", 估算GasLimit失败");
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

    protected String changeSignDataForTron(HtgContext htgContext, List<String> seedList, String txKey, String[] adds, int count, String[] removes) throws Exception {
        String vHash = TrxUtil.encoderChange(htgContext, txKey, adds, count, removes, htgContext.VERSION());
        String signData = this.ethSignForTron(seedList, vHash, seedList.size());
        return signData;
    }

    protected String sendChangeWithSignDataForTron(String priKey, String signData, String multySignContractAddress, String txKey, String[] adds, int count, String[] removes) throws Exception {
        List<org.tron.trident.abi.datatypes.Address> addList = Arrays.asList(adds).stream().map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
        List<org.tron.trident.abi.datatypes.Address> removeList = Arrays.asList(removes).stream().map(r -> new org.tron.trident.abi.datatypes.Address(r)).collect(Collectors.toList());
        org.tron.trident.abi.datatypes.Function function = TrxUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        String from = new KeyPair(priKey).toBase58CheckAddress();
        return this.sendTxForTron(from, priKey, multySignContractAddress, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendChangeForTron(HtgContext htgContext, List<String> seedList, String multySignContractAddress, String txKey, String[] adds, int count, String[] removes, int signCount) throws Exception {
        String vHash = TrxUtil.encoderChange(htgContext, txKey, adds, count, removes, htgContext.VERSION());
        String signData = this.ethSignForTron(seedList, vHash, signCount);
        List<org.tron.trident.abi.datatypes.Address> addList = Arrays.asList(adds).stream().map(a -> new org.tron.trident.abi.datatypes.Address(a)).collect(Collectors.toList());
        List<org.tron.trident.abi.datatypes.Address> removeList = Arrays.asList(removes).stream().map(r -> new org.tron.trident.abi.datatypes.Address(r)).collect(Collectors.toList());
        org.tron.trident.abi.datatypes.Function function = TrxUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        String priKey = seedList.get(0);
        String from = new KeyPair(priKey).toBase58CheckAddress();
        return this.sendTxForTron(from, priKey, multySignContractAddress, function, HeterogeneousChainTxType.CHANGE);
    }

    protected String sendTxForTron(String fromAddress, String priKey, String multySignContractAddress, org.tron.trident.abi.datatypes.Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.sendTxForTron(fromAddress, priKey, txFunction, txType, null, multySignContractAddress);
    }

    protected String sendTxForTron(String fromAddress, String priKey, org.tron.trident.abi.datatypes.Function txFunction, HeterogeneousChainTxType txType, BigInteger value, String contract) throws Exception {
        // 估算feeLimit
        TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddress, contract, txFunction, value);
        if (estimateSun.isReverted()) {
            System.err.println(String.format("[%s]交易验证失败，原因: %s", txType, estimateSun.getRevertReason()));
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, estimateSun.getRevertReason());
        }
        BigInteger feeLimit = TRX_100;
        if (estimateSun.getSunUsed() > 0) {
            feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
        }
        System.out.println(String.format("交易类型: %s, 估算的feeLimit: %s", txType, TrxUtil.convertSunToTrx(feeLimit).toPlainString()));
        value = value == null ? BigInteger.ZERO : value;
        String encodedHex = FunctionEncoder.encode(txFunction);
        Contract.TriggerSmartContract trigger =
                Contract.TriggerSmartContract.newBuilder()
                        .setOwnerAddress(ApiWrapper.parseAddress(fromAddress))
                        .setContractAddress(ApiWrapper.parseAddress(contract))
                        .setData(ApiWrapper.parseHex(encodedHex))
                        .setCallValue(value.longValue())
                        .build();

        Response.TransactionExtention txnExt = trxWrapper.blockingStub.triggerContract(trigger);
        TransactionBuilder builder = new TransactionBuilder(txnExt.getTransaction());
        builder.setFeeLimit(feeLimit.longValue());

        Chain.Transaction signedTxn = trxWrapper.signTransaction(builder.build(), new KeyPair(priKey));
        String txHash = TrxUtil.calcTxHash(signedTxn);
        System.out.println("txHash => " + txHash);
        Response.TransactionReturn ret = trxWrapper.blockingStub.broadcastTransaction(signedTxn);
        System.out.println(String.format("[%s]======== Result: %s ", txType, ret.toString()));
        return txHash;
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

}