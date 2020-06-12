package network.nerve.converter.heterogeneouschain.eth.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.eth.base.Base;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.ethereum.crypto.HashUtil;
import org.junit.Test;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.*;


public class ETHWalletApiTest extends Base {

    @Test
    public void getBlock() throws Exception {
        Long height = 7370853L;
        EthBlock.Block block = ethWalletApi.getBlockByHeight(height);
        //Block block1 = ethWalletApi.getBlock(height);
        System.out.println();
    }

    @Test
    public void getBlockWithoutTransaction() throws IOException {
        Long height = 7370853L;
        EthBlock.Block block = ethWalletApi.getWeb3j().ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(height)), false).send().getBlock();
        System.out.println();
    }

    @Test
    public void txRemarkTest() throws Exception {
        String mainEthRpcAddress = "https://mainnet.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        Web3j web3j = Web3j.build(new HttpService(mainEthRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        String txHash = "0xb1ed364e4333aae1da4a901d5231244ba6a35f9421d4607f7cb90d60bf45578a";
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        String remark = new String(Numeric.hexStringToByteArray(tx.getInput()), StandardCharsets.UTF_8);
        System.out.println(remark);
    }

    @Test
    public void getTxJson() throws Exception {
        String txHash = "0x93be98e9cb6d920a453bdc9113513585a225960dc3ad3a1efdac9839e9c0069b";
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        System.out.println(JSONUtils.obj2json(tx));
        HeterogeneousTransactionInfo info = EthUtil.newTransactionInfo(tx);
    }

    @Test
    public void withdrawTest() throws Exception {
        Set<String> set = new HashSet<>();
        set.add("0xe24973ff71d061f403cb45ddde97e034484ee7d3");
        //EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET = set;
        String txHash = "0xcdd7609b888eb261e1d6c4b0d9efe762a4e194c1291d9c0af8980cc42f6a262f";
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(txHash);
        EthParseTxHelper helper = new EthParseTxHelper();
        HeterogeneousTransactionInfo info = helper.parseWithdrawTransaction(tx, txReceipt);
        System.out.println(info.toString());
    }

    @Test
    public void getTxAndPublicKeyTest() throws Exception {
        setMain();
        String txHash = "0x855d092b64df8a6346e691a33071fb8fac197e45ee4ac8769386b4121e80da6d";
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        converPublicKey(tx);
    }

    @Test
    public void covertNerveAddressByEthTxTest() throws Exception {
        EthContext.NERVE_CHAINID = 2;
        String txHash = "0x3c39e32ffafdee0705687e2c348aab226dcd08c1226e13053ee563de077c4b69";
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        System.out.println(EthUtil.covertNerveAddressByEthTx(tx));
    }

    @Test
    public void hexTest() {
        System.out.println(HexUtil.decode("ff"));
    }


    @Test
    public void blockPublicKeyTest() throws Exception {
        Long height = 7936920L;
        EthBlock.Block block = ethWalletApi.getBlockByHeight(height);
        List<EthBlock.TransactionResult> list = block.getTransactions();
        for (EthBlock.TransactionResult txResult : list) {
            Transaction tx = (Transaction) txResult;
            converPublicKey(tx);
        }
        System.out.println();
    }

    private byte[] getRawTxHashBytes(Transaction tx) {
        String data = "";
        if (StringUtils.isNotBlank(tx.getInput()) && !"0x".equals(tx.getInput().toLowerCase())) {
            data = tx.getInput();
        }
        RawTransaction rawTx = RawTransaction.createTransaction(
                tx.getNonce(),
                tx.getGasPrice(),
                tx.getGas(),
                tx.getTo(),
                tx.getValue(),
                data);
        byte[] rawTxEncode;
        if (tx.getChainId() != null) {
            rawTxEncode = TransactionEncoder.encode(rawTx, tx.getChainId());
        } else {
            rawTxEncode = TransactionEncoder.encode(rawTx);
        }
        byte[] hashBytes = Hash.sha3(rawTxEncode);
        return hashBytes;
    }

    /**
     * 遍历每一个可能的v值，来得到正确的公钥
     */
    private void converPublicKey(Transaction tx) {
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(tx.getR()), Numeric.decodeQuantity(tx.getS()));
        byte[] hashBytes = getRawTxHashBytes(tx);

        boolean exist = false;
        boolean success = false;
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BigInteger recover = Sign.recoverFromSignature(i, signature, hashBytes);
            if (recover != null) {
                exist = true;
                String address = "0x" + Keys.getAddress(recover);
                if (tx.getFrom().toLowerCase().equals(address.toLowerCase())) {
                    success = true;
                    System.out.println("成功: " + i + ": " + address + String.format("tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
                    break;
                } else {
                    errorList.add(address);
                }
            }
        }
        if (!exist) {
            System.err.println(String.format("异常: error, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
        }
        if (!success) {
            System.err.println(String.format("失败: tx from: %s, parse from: %s, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getFrom(), errorList, tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
        }
    }

    @Test
    public void txPublicKeyTest() throws SignatureException, IOException {
        ObjectMapper objectMapper = JSONUtils.getInstance();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String txJson = "{\"hash\":\"0x855d092b64df8a6346e691a33071fb8fac197e45ee4ac8769386b4121e80da6d\",\"nonce\":6,\"blockHash\":\"0x578fcc55a574980bb44398d47ab9de98f9da75137b6da4380d730477e273a70c\",\"blockNumber\":9569417,\"transactionIndex\":60,\"from\":\"0x87e1f9c3e730cf47bc49a03a856875801e0ece50\",\"to\":\"0x9b4e2b4b13d125238aa0480dd42b4f6fc71b37cc\",\"value\":0,\"gasPrice\":7000000000,\"gas\":62090,\"input\":\"0xa9059cbb000000000000000000000000f173805f1e3fe6239223b17f0807596edc2830120000000000000000000000000000000000000000000000000de0b6b3a7640000\",\"creates\":null,\"publicKey\":null,\"raw\":null,\"r\":\"0x39950dcaa3a777889aa3fcc8664fd88fcfa132962a266af4403e126071b042ba\",\"s\":\"0x2bea3f724b531993d45aa6bfd9c20ca247179951c3720e359bbf2aa2e2e9d1cb\",\"v\":37,\"nonceRaw\":\"0x6\",\"blockNumberRaw\":\"0x920489\",\"transactionIndexRaw\":\"0x3c\",\"valueRaw\":\"0x0\",\"gasPriceRaw\":\"0x1a13b8600\",\"gasRaw\":\"0xf28a\",\"chainId\":1}";
        //String txJson = "{\"hash\":\"0xe547b2ceed87fcf22b80cfb4c01236994967ee437e84e2fe689e88c5fee2d693\",\"nonce\":21,\"blockHash\":\"0x014a0b9ef207990060ec839d01dce40d6f243be43a3a93de666a31cfa8bb3fe4\",\"blockNumber\":7419804,\"transactionIndex\":3,\"from\":\"0xf173805f1e3fe6239223b17f0807596edc283012\",\"to\":\"0xf173805f1e3fe6239223b17f0807596edc283012\",\"value\":600000000000000,\"gasPrice\":20000000000,\"gas\":21000,\"input\":\"0x\",\"creates\":null,\"publicKey\":null,\"raw\":null,\"r\":\"0xe4c001eec77fe05bf03873745ed68cf1f2f648fbd2e2c9e51b1586e7767e483c\",\"s\":\"0x7bd1c92c85c68ed11f919797320609f08796af6cccd50916858d4dbff7933ae5\",\"v\":27,\"chainId\":null,\"nonceRaw\":\"0x15\",\"blockNumberRaw\":\"0x71379c\",\"transactionIndexRaw\":\"0x3\",\"valueRaw\":\"0x221b262dd8000\",\"gasPriceRaw\":\"0x4a817c800\",\"gasRaw\":\"0x5208\"}";
        Transaction tx = JSONUtils.json2pojo(txJson, Transaction.class);

        String data = "";
        if (StringUtils.isNotBlank(tx.getInput()) && !"0x".equals(tx.getInput().toLowerCase())) {
            data = tx.getInput();
        }
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(tx.getR()), Numeric.decodeQuantity(tx.getS()));
        RawTransaction rawTx = RawTransaction.createTransaction(
                new BigInteger(tx.getNonceRaw()),
                new BigInteger(tx.getGasPriceRaw()),
                new BigInteger(tx.getGasRaw()),
                tx.getTo(),
                new BigInteger(tx.getValueRaw()),
                data);
        byte[] rawTxEncode;
        if (tx.getChainId() != null) {
            rawTxEncode = TransactionEncoder.encode(rawTx, tx.getChainId());
        } else {
            rawTxEncode = TransactionEncoder.encode(rawTx);
        }
        byte[] hashBytes = Hash.sha3(rawTxEncode);

        //String hash = "5eb0711c34fbd673be0c9112024bd9f66554d3e18dcb1acd97524168617c137d";
        boolean exist = false;
        boolean success = false;
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BigInteger recover = Sign.recoverFromSignature(i, signature, hashBytes);
            if (recover != null) {
                exist = true;
                String address = "0x" + Keys.getAddress(recover);
                if (tx.getFrom().toLowerCase().equals(address.toLowerCase())) {
                    success = true;
                    System.out.println("成功: " + i + ": " + address + String.format("tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getHash(), tx.getTransactionIndexRaw(), tx.getInput().length(), tx.getChainId()));
                    break;
                } else {
                    errorList.add(address);
                }
            }
        }
        if (!exist) {
            System.err.println(String.format("异常: error, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getHash(), tx.getTransactionIndexRaw(), tx.getInput().length(), tx.getChainId()));
        }
        if (!success) {
            System.err.println(String.format("失败: tx from: %s, parse from: %s, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getFrom(), errorList, tx.getHash(), tx.getTransactionIndexRaw(), tx.getInput().length(), tx.getChainId()));
        }
    }

    @Test
    public void hashTest() {
        String data = "f8a9068501a13b860082f28a949b4e2b4b13d125238aa0480dd42b4f6fc71b37cc80b844a9059cbb000000000000000000000000f173805f1e3fe6239223b17f0807596edc2830120000000000000000000000000000000000000000000000000de0b6b3a764000025a039950dcaa3a777889aa3fcc8664fd88fcfa132962a266af4403e126071b042baa02bea3f724b531993d45aa6bfd9c20ca247179951c3720e359bbf2aa2e2e9d1cb";
        System.out.println(Numeric.decodeQuantity("0x" + data).toString(16));
        //System.out.println(Hex.decode(HexUtil.decode(data)));
//        System.out.println(HexUtil.encode(Hex.encode(HexUtil.decode(data))));
        System.out.println(HexUtil.encode(Hash.sha3(HexUtil.decode(data))));
        System.out.println(HexUtil.encode(HashUtil.sha3(HexUtil.decode(data))));
        System.out.println(HexUtil.encode(HashUtil.sha256(HexUtil.decode(data))));
        System.out.println(HexUtil.encode(HashUtil.ripemd160(HexUtil.decode(data))));
        System.out.println(HexUtil.encode(HashUtil.doubleDigest(HexUtil.decode(data))));
    }

    @Test
    public void txDecoderTest() {
        String data = "0xf8a9068501a13b860082f28a949b4e2b4b13d125238aa0480dd42b4f6fc71b37cc80b844a9059cbb000000000000000000000000f173805f1e3fe6239223b17f0807596edc2830120000000000000000000000000000000000000000000000000de0b6b3a764000025a039950dcaa3a777889aa3fcc8664fd88fcfa132962a266af4403e126071b042baa02bea3f724b531993d45aa6bfd9c20ca247179951c3720e359bbf2aa2e2e9d1cb";
        RawTransaction decode = TransactionDecoder.decode(data);
        System.out.println();
    }

    @Test
    public void validateContractAddress() throws IOException {
        String address1 = "0xf173805F1e3fE6239223B17F0807596Edc283012";
        String address2 = "0xB058887cb5990509a3D0DD2833B2054E4a7E4a55";
        System.out.println(ethWalletApi.getWeb3j().ethGetCode(address1,
                DefaultBlockParameter.valueOf("latest")).send().getCode());
        System.out.println(ethWalletApi.getWeb3j().ethGetCode(address2,
                DefaultBlockParameter.valueOf("latest")).send().getCode());
    }

    @Test
    public void sendETHTx() throws Exception {
        String txHash = ethWalletApi.sendETH("0xf173805F1e3fE6239223B17F0807596Edc283012", "0xD15FDD6030AB81CEE6B519645F0C8B758D112CD322960EE910F65AD7DBB03C2B",
                "0xf173805F1e3fE6239223B17F0807596Edc283012",
                EthContext.getFee(),
                EthConstant.ETH_GAS_LIMIT_OF_ETH,
                EthContext.getEthGasPrice());
        System.out.println(txHash);
    }

    @Test
    public void getMainNetTxReceipt() throws Exception {
        setMain();
        // 直接调用erc20合约
        String directTxHash = "0x2ec039c38d07910f457a634c2c2048c989930c54eebac4e3363993e5d77e3fd6";
        // 合约内部调用erc20合约
        String innerCallTxHash = "0xa8fa6cdf285a317318b2a93f9c9f5b5cfb33c98aa8a4db4147da6c78ed357263";
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(directTxHash);
        System.out.println();
        //0x000000000000000000000000c11d9943805e56b630a401d4bd9a29550353efa1000000000000000000000000000000000000000000000000016345785d8a0000
    }

    @Test
    public void getTestNetTxReceipt() throws Exception {
        // 直接调用erc20合约
        String directTxHash = "0xc30747fd41072a2ee3e16e99656d0dad1c10598870dc8451526c1982a9be5581";
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt);
        //System.out.println(JSONUtils.obj2json(txReceipt));
        //EthParseTxHelper helper = new EthParseTxHelper();
        //boolean bool = helper.parseWithdrawTxReceipt(txReceipt, new HeterogeneousTransactionBaseInfo());
        //System.out.println(bool);
    }

    String privateKey3 = "59F770E9C44075DE07F67BA7A4947C65B7C3A0046B455997D1E0F854477222C8";
    String from3 = "0x09534d4692F568BC6e9bef3b4D84d48f19E52501";

    String privateKey4 = "09EF77E9D4DD02D52AB08762A0D28874B5B0F2D85E9AAB3BB011FCEB51E7EB37";
    String from4 = "0xF3c90eF58eC31805af11CE5FA6d39E395c66441f";

    String privateKey5 = "156B8A1DBEC2E2D032796464A059E569095BB4E0F0E491AC3A4B29A9EDCAB3A8";
    String from5 = "0x6afb1F9Ca069bC004DCF06C51B42992DBD90Adba";

    String privateKey6 = "43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D";
    String from6 = "0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65";

    String privateKey7 = "0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85";
    String from7 = "0xd29E172537A3FB133f790EBE57aCe8221CB8024F";

    String privateKey8 = "CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2";
    String from8 = "0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17";

    String privateKey9 = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    String from9 = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";

    private String getPrivateKey() {
        return privateKey6;
    }

    private String getFrom() {
        return from6;
    }

    /*
     添加`8`为管理员
     3: 0x6ba6da0b69a3593424c67e8b3f4b68aba62491c60af7214931336d5417eea3b7
     4: 0x033a35016431428a129009fd90b606facd53f86e7d42a821c8a7ec117cde8ad1
     5: 0x624323fe8b97e0b799e23c1ad10713040b7bc6d7a90883f078078d36d4427760
     6: 0x9b10d33660b26e568bf9fc9d224cee494657071249fadd8e14f502904f944286 (done)
     */
    @Test
    public void callContractTest() throws Exception {
        //加载凭证，用私钥
        Credentials credentials = Credentials.create(getPrivateKey());
        String contractAddress = "0x945aDdcdCB42A99aae77A10Eb220C4e04f4f7b5b";
        // args: "zxczxc",["0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17"],[]
        EthGetTransactionCount transactionCount = ethWalletApi.getWeb3j().ethGetTransactionCount(getFrom(), DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
//0xa3c1f84900000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000c11d9943805e56b630a401d4bd9a29550353efa1000000000000000000000000000000000000000000000000016345785d8a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000067765727765720000000000000000000000000000000000000000000000000000
        //创建RawTransaction交易对象
        Function function = new Function(
                "createOrSignManagerChange",
                List.of(new Utf8String("zxczxc"), new DynamicArray(Address.class, List.of(new Address("0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17"))),
                        new DynamicArray(Address.class, List.of())),
                List.of(new TypeReference<Type>() {
                })
        );

        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                EthContext.getEthGasPrice(),
                BigInteger.valueOf(300000L),
                contractAddress, encodedFunction
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = ethWalletApi.getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();
        System.out.println(ethSendTransaction.getTransactionHash());
    }

    /**
     * view/constant函数返回值解析测试
     */
    @Test
    public void viewContractTest() throws Exception {
        String contractAddress = "0xD7AF5cb5D3008D20d2F22F4612Fb5589E44699Ac";
        //创建RawTransaction交易对象
        Function allManagersFunction = new Function(
                "allManagers",
                List.of(),
                List.of(new TypeReference<DynamicArray<Address>>() {
                })
        );

        Function ifManagerFunction = new Function(
                "ifManager",
                List.of(new Address("0x54eab3868b0090e6e1a1396e0e54f788a71b2b17")),
                List.of(new TypeReference<Bool>() {
                })
        );

        Function pendingWithdrawTransactionFunction = new Function(
                "pendingWithdrawTransaction",
                List.of(new Utf8String("qweqwe")),
                List.of(
                        new TypeReference<Address>(){},
                        new TypeReference<Address>(){},
                        new TypeReference<Uint256>(){},
                        new TypeReference<Bool>(){},
                        new TypeReference<Address>(){},
                        new TypeReference<Uint8>(){}
                )
        );

        // ((DynamicArray<Address>)typeList.get(1)).getValue().get(0).getTypeAsString()
        Function pendingManagerChangeTransactionFunction = new Function(
                "pendingManagerChangeTransaction",
                List.of(new Utf8String("bnmbnm")),
                List.of(
                        new TypeReference<Address>(){},
                        new TypeReference<DynamicArray<Address>>(){},
                        new TypeReference<DynamicArray<Address>>(){},
                        new TypeReference<Uint8>(){},
                        new TypeReference<Uint8>(){}
                )
        );

        Function function = pendingManagerChangeTransactionFunction;

        String encode = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction ethCallTransaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contractAddress, encode);
        EthCall ethCall = ethWalletApi.getWeb3j().ethCall(ethCallTransaction, DefaultBlockParameterName.PENDING).sendAsync().get();
        String value = ethCall.getResult();
        List<Type> typeList = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        List results = new ArrayList();
        for(Type type : typeList) {
            results.add(type.getValue());
        }
        System.out.println(JSONUtils.obj2json(typeList));
        System.out.println(JSONUtils.obj2json(results));
    }

    /**
     * 合约事件解析测试
     */
    @Test
    public void eventTest() {
        // Event definition
        final Event MY_EVENT = new Event("MyEvent",
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Address>(true) {
                        },
                        new TypeReference<Bytes32>(true) {
                        },
                        new TypeReference<Uint8>(false) {
                        }));

        // Event definition hash
        final String MY_EVENT_HASH = EventEncoder.encode(MY_EVENT);

        TransactionReceipt txReceipt = null;
        List<org.web3j.protocol.core.methods.response.Log> logs = txReceipt.getLogs();
        org.web3j.protocol.core.methods.response.Log log = logs.get(0);
        List<String> topics = log.getTopics();
        String eventHash = topics.get(0);
        if (eventHash.equals(MY_EVENT_HASH)) { // Only MyEvent.
            // address indexed _arg1
            Address arg1 = (Address) FunctionReturnDecoder.decodeIndexedValue(log.getTopics().get(1), new TypeReference<Address>() {
            });
            // bytes32 indexed _arg2
            Bytes32 arg2 = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(log.getTopics().get(2), new TypeReference<Bytes32>() {
            });
            // uint8 _arg3
            Uint8 arg3 = (Uint8) FunctionReturnDecoder.decodeIndexedValue(log.getTopics().get(3), new TypeReference<Uint8>() {
            });
        }
    }

    /**
     * 合约事件`TransactionWithdrawCompleted`解析测试
     */
    @Test
    public void eventTransactionWithdrawCompletedTest() throws Exception {
        // Event definition
        final Event MY_EVENT = new Event("TransactionWithdrawCompleted",
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<DynamicArray<Address>>(true) {},
                        new TypeReference<Utf8String>(false) {}
                ));

        // Event definition hash
        final String MY_EVENT_HASH = EventEncoder.encode(MY_EVENT);

        String directTxHash = "0xf7b5b85886e954250e031d112a8f007384c2dda1e2f6c8ead1a330a863e1d234";
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(directTxHash);
        List<org.web3j.protocol.core.methods.response.Log> logs = txReceipt.getLogs();
        org.web3j.protocol.core.methods.response.Log log = logs.get(1);
        List<String> topics = log.getTopics();
        String eventHash = topics.get(0);
        if (eventHash.equals(MY_EVENT_HASH)) { // Only MyEvent.
            List<Type> typeList = FunctionReturnDecoder.decode(log.getData(), MY_EVENT.getParameters());
            System.out.println();
        }
    }

    @Test
    public void changeEventTest() throws Exception {
        // 0x92756cd02612b2bcbbc2b0399f6469b500d4e81fb563e1004494872c12b6dd60 / nerveHash: b5fc4cc3b236ff0bbf4890bf62ed3f032c4cb499a1d6f71a4a2d0d462f32ecfe
        // 0x024f40dc947b30693f8c037441d9b925aa87610fd9ab04c33c3dfb8648209fe9 / nerveHash: ff5914633cde854dd6bd25f915a9ca1376a64e18f16df458d28c24a193535375
        String directTxHash = "0x024f40dc947b30693f8c037441d9b925aa87610fd9ab04c33c3dfb8648209fe9";
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(directTxHash);
        List<org.web3j.protocol.core.methods.response.Log> logs = txReceipt.getLogs();
        org.web3j.protocol.core.methods.response.Log log = logs.get(0);
        List<Object> eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
        System.out.println(JSONUtils.obj2PrettyJson(eventResult));
    }

    @Test
    public void eventHashTest() {
        System.out.println(String.format("event: %s, hash: %s", EthConstant.EVENT_DEPOSIT_FUNDS.getName(), EventEncoder.encode(EthConstant.EVENT_DEPOSIT_FUNDS)));
        System.out.println(String.format("event: %s, hash: %s", EthConstant.EVENT_TRANSFER_FUNDS.getName(), EventEncoder.encode(EthConstant.EVENT_TRANSFER_FUNDS)));
        System.out.println(String.format("event: %s, hash: %s", EthConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED.getName(), EventEncoder.encode(EthConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED)));
        System.out.println(String.format("event: %s, hash: %s", EthConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED.getName(), EventEncoder.encode(EthConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED)));
    }

    @Test
    public void functionReturnDecoderTest() throws JsonProcessingException {
        Function allManagersFunction = new Function(
                "allManagers",
                List.of(),
                List.of(new TypeReference<DynamicArray<Address>>() {
                })
        );
        Function ifManagerFunction = new Function(
                "ifManager",
                List.of(new Address("0x54eab3868b0090e6e1a1396e0e54f788a71b2b17")),
                List.of(new TypeReference<Bool>() {
                })
        );
        String valueOfAllManagers = "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000600000000000000000000000009534d4692f568bc6e9bef3b4d84d48f19e52501000000000000000000000000f3c90ef58ec31805af11ce5fa6d39e395c66441f0000000000000000000000006afb1f9ca069bc004dcf06c51b42992dbd90adba0000000000000000000000008f05ae1c759b8db56ff8124a89bb1305ece17b65000000000000000000000000d29e172537a3fb133f790ebe57ace8221cb8024f00000000000000000000000054eab3868b0090e6e1a1396e0e54f788a71b2b17";
        String valueOfIfManager = "0x0000000000000000000000000000000000000000000000000000000000000001";
        List<Type> typeOfAllManager = FunctionReturnDecoder.decode(valueOfAllManagers, allManagersFunction.getOutputParameters());
        List<Type> typeOfIfManager = FunctionReturnDecoder.decode(valueOfIfManager, ifManagerFunction.getOutputParameters());
        System.out.println(JSONUtils.obj2json(typeOfAllManager));
        System.out.println(JSONUtils.obj2json(typeOfIfManager));
    }

    @Test
    public void txInputWithdrawDecoderTest() throws JsonProcessingException {
        // 46b4c37e
        String input = "0x46b4c37e00000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000df5dae2bcaf611e4219bcc3260b0ba9aa8fab75b00000000000000000000000000000000000000000000000002c68af0bb14000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004061303531396163386230333863643638396162323961323264393035326631333061363965346338363564616333353232666464666337383237626639623864";
        List<Object> typeList = EthUtil.parseInput(input, EthConstant.INPUT_WITHDRAW);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));

    }

    @Test
    public void txInputChangeDecoderTest() throws JsonProcessingException {
        String changeInput = "0xf16cb636000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000040666635393134363333636465383534646436626432356639313561396361313337366136346531386631366466343538643238633234613139333533353337350000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000018354c726a3ef2b7da89def0fce1d15d679ae16a";
        List<Object> typeListOfChange = EthUtil.parseInput(changeInput, EthConstant.INPUT_CHANGE);
        System.out.println(JSONUtils.obj2PrettyJson(typeListOfChange));
    }

    @Test
    public void ethEstimateGasTest() throws Exception {
        String contractAddress = "0xf7c9bb7e6d6b8f603f17f6af0713d99ee285bddb";
        // args: "bnmbnm",["0xc11D9943805e56b630A401D4bd9A29550353EFa1","0x11eFE2A9CF96175AB241e4A88A6b79C4f1c70389"],[]
        Function function = new Function(
                "createOrSignManagerChange",
                List.of(new Utf8String("0129833793e14165d04c4961bf1a83c1596539d5365a0a588c48a9f1e5e0172c"),
                        new DynamicArray(Address.class, List.of(new Address("0x9458de7f162a5a35faf52f6b4c715187ec2269e5"))),
                        new DynamicArray(Address.class, List.of())),
                List.of(new TypeReference<Type>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03",//364151
                null,
                BigInteger.valueOf(1_00_0000_0000_0000_0000L),
                BigInteger.valueOf(10000000L),
                contractAddress,
                null,
                encodedFunction
        );

        EthEstimateGas estimateGas = ethWalletApi.getWeb3j().ethEstimateGas(tx).send();
        if(estimateGas.getResult() != null) {
            System.out.println(String.format("gasLimit: %s, 详情: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void ethCallTest() throws Exception {
        String contractAddress = "0xf7c9bb7e6d6b8f603f17f6af0713d99ee285bddb";
        // args: "bnmbnm",["0xc11D9943805e56b630A401D4bd9A29550353EFa1","0x11eFE2A9CF96175AB241e4A88A6b79C4f1c70389"],[]
        Function function = new Function(
                "createOrSignManagerChange",
                List.of(new Utf8String("0129833793e14165d04c4961bf1a83c1596539d5365a0a588c48a9f1e5e0172c"),
                        new DynamicArray(Address.class, List.of(new Address("0x9458de7f162a5a35faf52f6b4c715187ec2269e5"))),
                        new DynamicArray(Address.class, List.of())),
                List.of(new TypeReference<Type>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03",//364151
                null,
                BigInteger.valueOf(1_00_0000_0000_0000_0000L),
                BigInteger.valueOf(10000000L),
                contractAddress,
                null,
                encodedFunction
        );

        EthCall ethCall = ethWalletApi.getWeb3j().ethCall(tx, DefaultBlockParameterName.LATEST).send();
        if(ethCall.isReverted()) {
            System.out.println(ethCall.getRevertReason());
            return;
        }
        System.out.println("验证成功");
    }

    @Test
    public void getCurrentGasPrice() throws IOException {
        //setRinkeby();
        setMain();
        BigInteger gasPrice = ethWalletApi.getWeb3j().ethGasPrice().send().getGasPrice();
        System.out.println(new BigDecimal(gasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
    }

    @Test
    public void getCurrentNonce() throws Exception {
        setMain();
        System.out.println(ethWalletApi.getNonce("0x8b969435492a4a84b51ec6bda2181fb5ed41265b"));
        System.out.println(ethWalletApi.getLatestNonce("0x8b969435492a4a84b51ec6bda2181fb5ed41265b"));
    }


    /**
     * 弃用，原因如下
     * 1. 当tx存在chainId时，转换会失败，原因是有chainId时，v变量大于了34
     * 2. 没有chainId, tx有一个错误的v值，也会造成转换失败
     * 3. 因此，使用Sign.recoverFromSignature，遍历每一个可能的v值，来得到正确的公钥
     */
    @Deprecated
    private void publicKey(Transaction tx) {
        byte[] hashBytes = getRawTxHashBytes(tx);

        //String hash = "5eb0711c34fbd673be0c9112024bd9f66554d3e18dcb1acd97524168617c137d";
        byte v = (byte) (tx.getV());
        byte[] r = Numeric.hexStringToByteArray(tx.getR());
        byte[] s = Numeric.hexStringToByteArray(tx.getS());

        Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
        try {
            BigInteger pKey = Sign.signedMessageHashToKey(hashBytes, signatureData);
            //System.out.println("p: " + Numeric.encodeQuantity(pKey));
            //BigInteger pKey = converPublicKey(tx);
            String address = "0x" + Keys.getAddress(pKey);
            if (tx.getFrom().toLowerCase().equals(address.toLowerCase())) {
                System.out.println("成功: " + v + ": " + address + String.format("tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
            } else {
                System.err.println(String.format("失败: tx from: %s, parse from: %s, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", tx.getFrom(), address, tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
            }
        } catch (Exception e) {
            System.err.println(String.format("异常: %s error, tx hash: %s, tx index: %s, tx data length: %s, tx chainId: %s", v, tx.getHash(), tx.getTransactionIndex(), tx.getInput().length(), tx.getChainId()));
        }
    }

}