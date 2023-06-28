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
import network.nerve.converter.heterogeneouschain.enuls.context.EnulsContext;
import network.nerve.converter.heterogeneouschain.ethw.context.EthwContext;
import network.nerve.converter.heterogeneouschain.goerlieth.context.GoerliContext;
import network.nerve.converter.heterogeneouschain.kava.context.KavaContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.rei.context.ReiContext;
import network.nerve.converter.heterogeneouschain.zk.context.ZkContext;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Protocol24Test {

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
        map.put(122, new ReiContext());
        map.put(123, new ZkContext());
        return map;
    }

    @Before
    public void before() {
        try {
            for (int i = 122; i <= 123; i++) {
                int htgChainId = i;
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 读取每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyMap() {
        return multyMapMainnet();
    }


    /**
     * 主网: 配置每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyMapMainnet() {
        /*
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH,
            119 ENULS, 120 KAVA, 121 ETHW, 122 REI, 123 ZK
         */
        // 配置每个链的新旧多签合约
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
        map.put(122, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5"});
        map.put(123, new String[]{"0x54C4A99Ee277eFF14b378405b6600405790d5045", "0x54C4A99Ee277eFF14b378405b6600405790d5045"});
        return map;
    }

    /**
     * 读取异构链基本信息
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
     注册跨链网络
     主网:
     registerheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 122
     registerheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 123
     */

    /**
     * 补齐管理员
     */
    @Test
    public void managerChangeSignData() throws Exception {
        List<String> seedList = new ArrayList<>();
        seedList.add("???");
        seedList.add("???");
        seedList.add("???");

        String txKey = "aaa1000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{
                "0xb12a6716624431730c3ef55f80c458371954fa52", "0x1f13e90daa9548defae45cd80c135c183558db1f",
                "0x66fb6d6df71bbbf1c247769ba955390710da40a5", "0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d",
                "0xaff68cd458539a16b932748cf4bdd53bf196789f", "0xc8dcc24b09eed90185dbb1a5277fd0a389855dae",
                "0xa28035bb5082f5c00fa4d3efc4cb2e0645167444", "0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1",
                "0x8255a0e99456f45f8b85246ef6a9b1895c784c9f", "0x25955965648cd5c017d6d4644bf65830645ef2f2",
                "0x5fbf7793196efbf7066d99fa29dc64dc23052451", "0x7c4b783a0101359590e6153df3b58c7fe24ea468"
        };
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = seedList.size();
        for (int i = 122; i <= 123; i++) {
            int htgChainId = i;
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            HtgContext htgContext = contextMap.get(htgChainId);
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
            String[] multyArray = multyMap.get(htgChainId);
            String newMulty = multyArray[1];
            String multySignContractAddress = newMulty;

            String hash = this.sendChange(htgWalletApi, htgContext, seedList, multySignContractAddress, txKey, adds, txCount, removes, signCount);
            System.out.println(String.format("htgId: %s, 管理员添加%s个，移除%s个，hash: %s", htgChainId, adds.length, removes.length, hash));
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

}