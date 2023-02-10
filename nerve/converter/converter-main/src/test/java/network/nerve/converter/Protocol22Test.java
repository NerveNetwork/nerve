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
import network.nerve.converter.heterogeneouschain.bch.context.BchContext;
import network.nerve.converter.heterogeneouschain.enuls.context.EnulsContext;
import network.nerve.converter.heterogeneouschain.ethw.context.EthwContext;
import network.nerve.converter.heterogeneouschain.goerlieth.context.GoerliContext;
import network.nerve.converter.heterogeneouschain.iotx.context.IotxContext;
import network.nerve.converter.heterogeneouschain.kava.context.KavaContext;
import network.nerve.converter.heterogeneouschain.klay.context.KlayContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.metis.context.MetisContext;
import network.nerve.converter.heterogeneouschain.optimism.context.OptimismContext;
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


public class Protocol22Test {

    // 设置环境
    static boolean MAIN = true;

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
    String contractForCreateERC20Minter;

    private Map<Integer, HtgContext> contextMap() {
        Map<Integer, HtgContext> map = new HashMap<>();
        map.put(118, new GoerliContext());
        map.put(119, new EnulsContext());
        map.put(120, new KavaContext());
        map.put(121, new EthwContext());
        return map;
    }

    @Before
    public void before() {
        try {
            if (MAIN) {
                contractForCreateERC20Minter = "0x63ae3cea2225be3390854e824a65bbbb02616bb4";
            } else {
                contractForCreateERC20Minter = "0x1EA3FfD41c3ed3e3f788830aAef553F8F691aD8C";
            }
            for (int i = 118; i <= 121; i++) {
                int htgChainId = i;
                if (htgChainId == 118 && MAIN) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 读取每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyMap() {
        if (MAIN) {
            return multyMapMainnet();
        } else {
            return multyMapTestnet();
        }
    }

    /**
     * 测试网: 配置每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyMapTestnet() {
        /*
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH,
            118 GoerliETH, 119 ENULS, 120 KAVA, 122 LGCY
         */
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
        map.put(118, new String[]{"0x898d47a9C97B6b17409FaFC982514A8e89251fED", "0x898d47a9C97B6b17409FaFC982514A8e89251fED"});
        map.put(119, new String[]{"0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69", "0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69"});
        map.put(120, new String[]{"0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9", "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9"});
        return map;
    }

    /**
     * 主网: 配置每个链的新旧多签合约
     */
    private Map<Integer, String[]> multyMapMainnet() {
        /*
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH,
            119 ENULS, 120 KAVA, 121 ETHW, 122 LGCY
         */
        // 配置每个链的新旧多签合约
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
        map.put(119, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5"});
        map.put(120, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5"});
        map.put(121, new String[]{"0x67b3757f20DBFa114b593dfdAc2b3097Aa42133E", "0x67b3757f20DBFa114b593dfdAc2b3097Aa42133E"});
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
     注册跨链网络
     测试网:
     bindheterogeneousmainasset TNVTdTSPLGfeN8cS9tLBnYnjYjk4MrMabDgcK 119 2 1
     registerheterogeneousmainasset TNVTdTSPLGfeN8cS9tLBnYnjYjk4MrMabDgcK 120


     主网:
     bindheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 119 1 1
     registerheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 120
     registerheterogeneousmainasset NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA 121
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
                "0xa28035bb5082f5c00fa4d3efc4cb2e0645167444", "0x10c17be7b6d3e1f424111c8bddf221c9557728b0",
                "0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1", "0x8255a0e99456f45f8b85246ef6a9b1895c784c9f",
                "0x25955965648cd5c017d6d4644bf65830645ef2f2", "0x5fbf7793196efbf7066d99fa29dc64dc23052451"
        };
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = seedList.size();
        for (int i = 119; i <= 121; i++) {
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

    @Test
    public void setupMinterOnCreateERC20MinterTest() {
        // 更新工具合约中的minter地址为新多签地址
        String prikey = "???";
        // 遍历所有异构链
        for (int i = 118; i <= 121; i++) {
            Integer htgChainId = i;
            if (htgChainId == 118 && MAIN) {
                continue;
            }
            String _contractForCreateERC20Minter = contractForCreateERC20Minter;
            if (htgChainId == 121 && MAIN) {
                _contractForCreateERC20Minter = "0xBa536D073c38614057D8C14C2F28144e84c1cde6";
            }

            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyArray = multyMap.get(htgChainId);
            String newMulty = multyArray[1];

            try {
                Function setupMinter = new Function("setupMinter", Arrays.asList(new Address(newMulty)), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, _contractForCreateERC20Minter, setupMinter);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("验证失败[setupMinter] - HtgChainId: %s, newMultyContract: %s, Failed to setupMinter, error: %s", htgChainId, newMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(setupMinter);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, _contractForCreateERC20Minter, BigInteger.ZERO, data);
                //签名Transaction，这里要对交易做签名
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                String hexValue = Numeric.toHexString(signMessage);
                //发送交易
                EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                if (send.hasError()) {
                    System.err.println(String.format("广播失败[setupMinter] - HtgChainId: %s, multyContract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                            htgChainId, newMulty,
                            send.getError().getMessage(),
                            send.getError().getCode(),
                            send.getError().getData()
                    ));
                } else {
                    System.out.println(String.format("广播成功[setupMinter] - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, newMulty, send.getTransactionHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to [setupMinter], HtgChainId: %s, multyContract: %s, error: %s", htgChainId, newMulty, e.getMessage()));
            }
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