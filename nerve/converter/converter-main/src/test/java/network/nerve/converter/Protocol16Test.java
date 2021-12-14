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
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.model.TrxSendTransactionPo;
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
    // 设置环境
    boolean MAIN = false;

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
                ApiWrapper wrapper = new ApiWrapper("tron.nerve.network:50051", "tron.nerve.network:50061", "3333333333333333333333333333333333333333333333333333333333333333");
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
        // 配置每个链的新旧多签合约
        Map<Integer, String[]> map = new HashMap<>();
        // 前旧后新
        map.put(101, new String[]{"0x6758d4c4734ac7811358395a8e0c3832ba6ac624", "0xxx"});
        map.put(102, new String[]{"0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0xxx"});
        map.put(103, new String[]{"0x23023c99dcede393d6d18ca7fb08541b3364fa90", "0xxx"});
        map.put(104, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0xxx"});
        map.put(105, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0xxx"});
        map.put(106, new String[]{"0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5", "0xxx"});
        map.put(107, new String[]{"0xf0e406c49c63abf358030a299c0e00118c4c6ba5", "0xxx"});
        map.put(108, new String[]{"TYmgxoiPetfE2pVWur9xp7evW4AuZCzfBm", "Txxx"});
        map.put(109, new String[]{"0xxxx", "0xxx"});
        map.put(110, new String[]{"0xxxx", "0xxx"});
        map.put(111, new String[]{"0xxxx", "0xxx"});
        map.put(112, new String[]{"0xxxx", "0xxx"});
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

    private Map tokenAssetsMap() {
        try {
            String outFileName;
            if (MAIN) {
                outFileName = "nerve_assets_group_by_ps.json";
            } else {
                outFileName = "nerve_assets_group_by_ps_testnet.json";
            }
            String path = String.format("/Users/pierreluo/Nuls/%s", outFileName);
            String json = null;
            try {
                json = IoUtils.readBytesToString(new File(path), StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                // skip it
                Log.error("init ERC20Standard error.", e);
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
     * 第一步，读取每个异构链的token信息
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
            boolean bind = true;
            for (Map htgInfo : htgList) {
                Boolean isToken = (Boolean) htgInfo.get("isToken");
                Integer htgChainId = (Integer) htgInfo.get("heterogeneousChainId");
                String contractAddress = (String) htgInfo.get("contractAddress");
                if (!isToken) {
                    System.out.println(String.format("忽略非token - id: %s, htg: %s, contract: %s", id, htgChainId, contractAddress));
                    continue;
                }
                if (registerChainId.intValue() > 100 && htgChainId.intValue() == registerChainId.intValue()) {
                    // 非绑定资产
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
     * 第二步, 遍历所有异构链，除了trx和新增4条链，波场单独处理
     */
    @Test
    public void upgradeTest() throws Exception {
        // 是否检查绑定类型资产的权限已转移，检查erc20Minter是否注册到新多签
        boolean check = true;
        // 多签合约创建者私钥
        String prikey = "???";
        upgradeS1(prikey, check);
        upgradeS2(prikey, check);
        registerMinterERC20(prikey, check);
    }

    /**
     * 第三步, 波场处理
     */
    @Test
    public void upgradeForTronTest() throws Exception {
        // 是否检查绑定类型资产的权限已转移
        boolean check = true;
        // 多签合约创建者私钥
        String prikey = "???";
        upgradeS1ForTron(prikey, check);
        upgradeS2ForTron(prikey, check);
    }


    /**
     * 第四步，补齐其他12个虚拟银行，波场单独处理(手动处理)
     */
    @Test
    public void managerAdd() throws Exception {
        List<String> seedList = new ArrayList<>();
        seedList.add("???");// 公钥: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        seedList.add("???");// 公钥: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        seedList.add("???");// 公钥: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC

        for (int i = 101; i <= 112; i++) {
            int htgChainId = i;
            if (htgChainId == 108) {
                // 波场单独处理
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
                    "0x595d5364e5eb77e3707ce2710215db97a835a82d"
            };
            String[] removes = new String[]{};
            byte oldVersion = htgContext.VERSION();
            byte newVersion = 3;
            // 设置新的签名版本号
            htgContext.SET_VERSION(newVersion);
            int txCount = 1;
            int signCount = seedList.size();
            String hash = this.sendChange(htgWalletApi, htgContext, seedList, multySignContractAddress, txKey, adds, txCount, removes, signCount);
            // 执行结束后，还原签名版本号
            htgContext.SET_VERSION(oldVersion);
            System.out.println(String.format("htgId: %s, 管理员添加%s个，移除%s个，%s个签名，hash: %s", htgChainId, adds.length, removes.length, signCount, hash));
        }
    }

    /**
     * 第五步，替换工具合约中的minter地址为新多签地址（动态部署erc20Minter合约的工具），异构链101~107
     */
    @Test
    public void setupMinterOnCreateERC20MinterTest() {
        // 替换工具合约中的minter地址为新多签地址，异构链101~107
        String prikey = "???";
        // 遍历所有异构链，除了trx和新增4条链
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String newMulty = multyUpgrades[1];

            try {
                // 检查权限是否已转移
                Function getCurrentMinter = new Function("minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                List<Type> types = htgWalletApi.callViewFunction(contractForCreateERC20Minter, getCurrentMinter);
                String minter = (String) types.get(0).getValue();
                if (minter.equalsIgnoreCase(newMulty)) {
                    // 权限已转移
                    System.out.println(String.format("权限已转移[setupMinter], HtgChainId: %s, newMultyContract: %s", htgChainId, newMulty));
                    continue;
                }

                Function setupMinter = new Function("setupMinter", Arrays.asList(new Address(newMulty)), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, contractForCreateERC20Minter, setupMinter);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("验证失败[setupMinter] - HtgChainId: %s, newMultyContract: %s, Failed to transfer, error: %s", htgChainId, newMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(setupMinter);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, contractForCreateERC20Minter, BigInteger.ZERO, data);
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
        BigInteger gasLimit = estimateGas;
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
            // 检查主资产转移
            if (check) {
                BigDecimal balance = trxWalletApi.getBalance(oldMulty);
                if (balance.compareTo(BigDecimal.ZERO) == 0) {
                    // 主资产已转移
                    System.out.println(String.format("主资产已转移, HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
                    return;
                }
            }
            // call upgradeContractS1
            org.tron.trident.abi.datatypes.Function upgradeContractS1 = new org.tron.trident.abi.datatypes.Function("upgradeContractS1", List.of(), List.of());
            String from = new KeyPair(prikey).toBase58CheckAddress();
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(from, oldMulty, upgradeContractS1);
            if (estimateSun.isReverted()) {
                System.err.println(String.format("验证失败 - HtgChainId: %s, multyContract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, estimateSun.getRevertReason()));
                return;
            }
            BigInteger feeLimit = TRX_100;
            if (estimateSun.getSunUsed() > 0) {
                feeLimit = feeLimit.add(BigInteger.valueOf(estimateSun.getSunUsed()));
            }
            TrxSendTransactionPo send = trxWalletApi.callContract(from, prikey, oldMulty, feeLimit, upgradeContractS1);

            if (send == null) {
                System.err.println(String.format("广播失败 - HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
            } else {
                System.out.println(String.format("广播成功 - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, oldMulty, send.getTxHash()));
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
                        // 绑定类型检查权限是否已转移
                        org.tron.trident.abi.datatypes.Function getCurrentMinter = new org.tron.trident.abi.datatypes.Function("current_minter", List.of(), Arrays.asList(new org.tron.trident.abi.TypeReference<org.tron.trident.abi.datatypes.Address>() {}));
                        List<org.tron.trident.abi.datatypes.Type> types = trxWalletApi.callViewFunction(contract, getCurrentMinter);
                        String minter = (String) types.get(0).getValue();
                        if (minter.equalsIgnoreCase(newMulty)) {
                            // 权限已转移
                            System.out.println(String.format("权限已转移, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                            continue;
                        }
                    }
                }
                if (!bind) {
                    // 非绑定类型强行检查资产数量是否已转移
                    BigInteger erc20Balance = trxWalletApi.getERC20Balance(oldMulty, contract);
                    if (erc20Balance.compareTo(BigInteger.ZERO) == 0) {
                        // 资产已转移
                        System.out.println(String.format("资产Token已转移, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                        continue;
                    }
                }
                org.tron.trident.abi.datatypes.Function upgradeContractS2 = new org.tron.trident.abi.datatypes.Function("upgradeContractS2", Arrays.asList(new org.tron.trident.abi.datatypes.Address(contract)), List.of());
                String from = new KeyPair(prikey).toBase58CheckAddress();
                TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(from, oldMulty, upgradeContractS2);
                if (estimateSun.isReverted()) {
                    System.err.println(String.format("验证失败 - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, contract, estimateSun.getRevertReason()));
                    continue;
                }
                BigInteger feeLimit = TRX_100;
                if (estimateSun.getSunUsed() > 0) {
                    feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
                }
                TrxSendTransactionPo send = trxWalletApi.callContract(from, prikey, oldMulty, feeLimit, upgradeContractS2);

                if (send == null) {
                    System.err.println(String.format("广播失败 - HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                } else {
                    System.out.println(String.format("广播成功 - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, oldMulty, contract, send.getTxHash()));
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
            HeterogeneousCfg cfg = cfgMap.get(htgChainId);
            long networkChainId = cfg.getChainIdOnHtgNetwork();
            HtgWalletApi htgWalletApi = htgWalletApiMap.get(htgChainId);
            String[] multyUpgrades = multyUpgradeMap.get(htgChainId);
            String oldMulty = multyUpgrades[0];
            try {
                // 检查主资产转移
                if (check) {
                    BigDecimal balance = htgWalletApi.getBalance(oldMulty);
                    if (balance.compareTo(BigDecimal.ZERO) == 0) {
                        // 主资产已转移
                        System.out.println(String.format("主资产已转移, HtgChainId: %s, multyContract: %s", htgChainId, oldMulty));
                        continue;
                    }
                }
                // call upgradeContractS1
                Function upgradeContractS1 = new Function("upgradeContractS1", List.of(), List.of());
                Credentials credentials = Credentials.create(prikey);
                String from = credentials.getAddress();
                EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS1);
                if (ethEstimateGas.getError() != null) {
                    System.err.println(String.format("验证失败[主资产转移] - HtgChainId: %s, multyContract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, ethEstimateGas.getError().getMessage()));
                    continue;
                }
                BigInteger nonce = htgWalletApi.getNonce(from);
                String data = FunctionEncoder.encode(upgradeContractS1);
                BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice, gasLimit, oldMulty, BigInteger.ZERO, data);
                //签名Transaction，这里要对交易做签名
                byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                String hexValue = Numeric.toHexString(signMessage);
                //发送交易
                EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                if (send.hasError()) {
                    System.err.println(String.format("广播失败[主资产转移] - HtgChainId: %s, multyContract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                            htgChainId, oldMulty, send.getError().getMessage(), send.getError().getCode(), send.getError().getData()));
                } else {
                    System.out.println(String.format("广播成功[主资产转移] - HtgChainId: %s, multyContract: %s, hash: %s", htgChainId, oldMulty, send.getTransactionHash()));
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (Exception e) {
                System.err.println(String.format("Failed to [主资产转移], HtgChainId: %s, multyContract: %s, error: %s", htgChainId, oldMulty, e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    protected void upgradeS2(String prikey, boolean check) throws Exception {
        // 遍历所有异构链，除了trx和新增4条链
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
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
                            // 绑定类型的token检查权限是否已转移
                            Function getCurrentMinter = new Function("current_minter", List.of(), Arrays.asList(new TypeReference<Address>() {}));
                            List<Type> types = htgWalletApi.callViewFunction(contract, getCurrentMinter);
                            String minter = (String) types.get(0).getValue();
                            if (minter.equalsIgnoreCase(newMulty)) {
                                // 权限已转移
                                System.out.println(String.format("Token权限已转移, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                                continue;
                            }
                        }
                    }
                    if (!bind) {
                        // 非绑定类型强行检查资产数量是否已转移
                        BigInteger erc20Balance = htgWalletApi.getERC20Balance(oldMulty, contract);
                        if (erc20Balance.compareTo(BigInteger.ZERO) == 0) {
                            // 资产已转移
                            System.out.println(String.format("资产Token已转移, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, oldMulty, contract));
                            continue;
                        }
                    }
                    Function upgradeContractS2 = new Function("upgradeContractS2", Arrays.asList(new Address(contract)), List.of());
                    Credentials credentials = Credentials.create(prikey);
                    String from = credentials.getAddress();
                    EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS2);
                    if (ethEstimateGas.getError() != null) {
                        System.err.println(String.format("验证失败[Token资产转移] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, contract, ethEstimateGas.getError().getMessage()));
                        continue;
                    }
                    BigInteger nonce = htgWalletApi.getNonce(from);
                    String data = FunctionEncoder.encode(upgradeContractS2);
                    BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                    BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                    RawTransaction rawTransaction = RawTransaction.createTransaction(
                            nonce,
                            gasPrice, gasLimit, oldMulty, BigInteger.ZERO, data);
                    //签名Transaction，这里要对交易做签名
                    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                    String hexValue = Numeric.toHexString(signMessage);
                    //发送交易
                    EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                    if (send.hasError()) {
                        System.err.println(String.format("广播失败[Token资产转移] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                                htgChainId, oldMulty, contract,
                                send.getError().getMessage(),
                                send.getError().getCode(),
                                send.getError().getData()
                        ));
                    } else {
                        System.out.println(String.format("广播成功[Token资产转移] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, oldMulty, contract, send.getTransactionHash()));
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    System.err.println(String.format("Failed to [Token资产转移], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, oldMulty, asset.get("contract"), e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
    }

    protected void registerMinterERC20(String prikey, boolean check) throws Exception {
        // 遍历所有异构链，除了trx和新增4条链
        for (int i = 101; i <= 107; i++) {
            Integer htgChainId = i;
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
                        // 绑定类型检查权限是否已注册到新多签
                        Function getCurrentMinter = new Function("isMinterERC20", Arrays.asList(new Address(contract)), Arrays.asList(new TypeReference<Bool>() {}));
                        List<Type> types = htgWalletApi.callViewFunction(newMulty, getCurrentMinter);
                        Boolean minter = (Boolean) types.get(0).getValue();
                        if (minter) {
                            // 已注册到新多签
                            System.out.println(String.format("已注册到新多签, HtgChainId: %s, multyContract: %s, erc20Contract: %s", htgChainId, newMulty, contract));
                            continue;
                        }
                    }
                    Function registerMinterERC20 = new Function("registerMinterERC20", Arrays.asList(new Address(contract)), List.of());
                    Credentials credentials = Credentials.create(prikey);
                    String from = credentials.getAddress();
                    EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, newMulty, registerMinterERC20);
                    if (ethEstimateGas.getError() != null) {
                        System.err.println(String.format("验证失败[注册到新多签] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to register, error: %s", htgChainId, newMulty, contract, ethEstimateGas.getError().getMessage()));
                        continue;
                    }
                    BigInteger nonce = htgWalletApi.getNonce(from);
                    String data = FunctionEncoder.encode(registerMinterERC20);
                    BigInteger gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(10000L));
                    BigInteger gasPrice = htgWalletApi.getCurrentGasPrice();
                    RawTransaction rawTransaction = RawTransaction.createTransaction(
                            nonce,
                            gasPrice, gasLimit, newMulty, BigInteger.ZERO, data);
                    //签名Transaction，这里要对交易做签名
                    byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, networkChainId, credentials);
                    String hexValue = Numeric.toHexString(signMessage);
                    //发送交易
                    EthSendTransaction send = htgWalletApi.ethSendRawTransaction(hexValue);
                    if (send.hasError()) {
                        System.err.println(String.format("广播失败[注册到新多签] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, errorMsg: %s, errorCode: %s, errorData: %s",
                                htgChainId, newMulty, contract,
                                send.getError().getMessage(),
                                send.getError().getCode(),
                                send.getError().getData()
                        ));
                    } else {
                        System.out.println(String.format("广播成功[注册到新多签] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, hash: %s", htgChainId, newMulty, contract, send.getTransactionHash()));
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    System.err.println(String.format("Failed to register[注册到新多签], HtgChainId: %s, multyContract: %s, erc20Contract: %s, error: %s", htgChainId, newMulty, asset.get("contract"), e.getMessage()));
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
}