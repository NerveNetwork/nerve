package network.nerve.converter.heterogeneouschain.ethw.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.ethw.base.Base;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class EthwWalletApiTest extends Base {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;

    static String USDT_ETHW = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_ETHW = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_ETHW_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    protected void setErc20USDT() {
        erc20Address = USDT_ETHW;
        erc20Decimals = 6;
    }
    protected void setErc20USD18() {
        erc20Address = USD18_ETHW;
        erc20Decimals = 18;
    }
    protected void setErc20NVT() {
        erc20Address = NVT_ETHW_MINTER;
        erc20Decimals = 8;
    }

    protected void setAccount_user() {
        from = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        fromPriKey = pMap.get(from).toString();
    }

    protected void setAccount_admin() {
        from = "0x7A9a9223830e58A53F47972255a99eDBA0332617";
        fromPriKey = pMap.get(from).toString();
    }

    private static Map<String, Object> pMap;

    @BeforeClass
    public static void beforeClass() throws Exception {
        try {

            String path = "";
            String pData = IoUtils.readBytesToString(new File(path));
            pMap = JSONUtils.json2map(pData);
            System.out.println(pMap.size());
        } catch (Exception e) {

        }
    }

    protected void setLocalTest() {
        list = new ArrayList<>();
        list.add(pMap.get("0x2804A4296211Ab079AED4e12120808F1703841b3").toString());// 0x2804A4296211Ab079AED4e12120808F1703841b3
        list.add(pMap.get("0x4202726a119F7784085B04264BfF716267a51032").toString());// 0x4202726a119F7784085B04264BfF716267a51032
        list.add(pMap.get("0x4dAE32e287D43Ba6F6fE9323864e67A9c66B47e6").toString());// 0x4dAE32e287D43Ba6F6fE9323864e67A9c66B47e6
        this.multySignContractAddress = "0x50074F4Bc4bC955622b49de16Fc6E3C1c73afBcA";
        init();
    }

    public void init() {
        try {
            htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.address = Credentials.create(list.get(0)).getAddress();
        this.priKey = list.get(0);
    }

    @Test
    public void eventHashTest() {
        System.out.println(String.format("event: %s, hash: %s", HtgConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED.getName(), EventEncoder.encode(HtgConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED)));
        System.out.println(String.format("event: %s, hash: %s", HtgConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED.getName(), EventEncoder.encode(HtgConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED)));
        System.out.println(String.format("event: %s, hash: %s", HtgConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED.getName(), EventEncoder.encode(HtgConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED)));
    }

    @Test
    public void methodHashTest() {
        Function upgradeFunction = HtgUtil.getCreateOrSignUpgradeFunction("", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd", "0x");
        System.out.println(String.format("name: %s, hash: %s", upgradeFunction.getName(), FunctionEncoder.encode(upgradeFunction)));
        Function withdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction("", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd", BigInteger.ZERO, false, "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd", "0x");
        System.out.println(String.format("name: %s, hash: %s", withdrawFunction.getName(), FunctionEncoder.encode(withdrawFunction)));
        Function changeFunction = HtgUtil.getCreateOrSignManagerChangeFunction("", List.of(), List.of(), 1, "0x");
        System.out.println(String.format("name: %s, hash: %s", changeFunction.getName(), FunctionEncoder.encode(changeFunction)));
        Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd", BigInteger.ZERO, "0x7D759A3330ceC9B766Aa4c889715535eeD3c0484");
        System.out.println(String.format("name: %s, hash: %s", crossOutFunction.getName(), FunctionEncoder.encode(crossOutFunction)));
    }


    @Test
    public void erc20TotalSupplyTest() throws Exception {
        setMain();
        BigInteger totalSupply = this.erc20TotalSupply("0x54e4622DC504176b3BB432dCCAf504569699a7fF");
        System.out.println(totalSupply);
        /*BigInteger totalSupply1 = this.erc20TotalSupply("0xae7FccFF7Ec3cf126cd96678ADAE83a2b303791C");
        System.out.println(totalSupply1);
        BigInteger totalSupply2 = this.erc20TotalSupply("0x856129092C53f5E2e4d9DB7E04c961580262D0AE");
        System.out.println(totalSupply2);*/
    }

    private BigInteger erc20TotalSupply(String contract) {
        try {
            Function totalSupply = new Function(
                    "totalSupply",
                    List.of(),
                    List.of(new TypeReference<Uint256>() {}));
            List<Type> typeList = htgWalletApi.callViewFunction(contract, totalSupply);
            return new BigInteger(typeList.get(0).getValue().toString());
        } catch (Exception e) {
            Log.error("contract[{}] error[{}]", contract, e.getMessage());
            return BigInteger.ZERO;
        }
    }

    /**
     * 转入eth
     */
    @Test
    public void transferMainAsset() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        BigInteger gasPrice = htgContext.getEthGasPrice();
        // 初始化 账户
        setAccount_user();
        // MainAsset数量
        String sendAmount = "0.001";
        String to = from;
        EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(from, to, "0x", new BigDecimal(sendAmount).movePointRight(18).toBigInteger());
        System.out.println(String.format("交易类型: 转账主资产, 估算的GasLimit: %s, %s", estimateGasObj.getAmountUsed(), estimateGasObj.getError() != null ? estimateGasObj.getError().getMessage() : ""));
        String txHash = htgWalletApi.sendMainAsset(from, fromPriKey, to, new BigDecimal(sendAmount), estimateGasObj.getAmountUsed(), gasPrice);
        System.out.println(String.format("向[%s]转账%s个MainAsset, 交易hash: %s", to, sendAmount, txHash));
    }


    /**
     * 转入erc20
     */
    @Test
    public void transferERC20() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // 初始化 账户
        setAccount_admin();
        // ERC20
        //setErc20USD18();
        setErc20USDT();
        String tokenAddress = erc20Address;
        int tokenDecimals = erc20Decimals;
        String tokenAmount = "100000000";
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        EthSendTransaction token = htgWalletApi.transferERC20Token(from, to, new BigDecimal(tokenAmount).movePointRight(tokenDecimals).toBigInteger(), fromPriKey, tokenAddress);
        System.out.println(String.format("向[%s]转账%s个ERC20(USDT), 交易hash: %s", to, tokenAmount, token.getTransactionHash()));

        setErc20USD18();
        tokenAddress = erc20Address;
        tokenDecimals = erc20Decimals;
        tokenAmount = "100000000";
        to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        token = htgWalletApi.transferERC20Token(from, to, new BigDecimal(tokenAmount).movePointRight(tokenDecimals).toBigInteger(), fromPriKey, tokenAddress);
        System.out.println(String.format("向[%s]转账%s个ERC20(USD18), 交易hash: %s", to, tokenAmount, token.getTransactionHash()));
    }

    /**
     * 新方式充值eth
     */
    @Test
    public void depositMainAssetByCrossOut() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // 初始化 账户
        setAccount_user();
        // MainAsset数量
        String sendAmount = "0.02";
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal(sendAmount));
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, HtgConstant.ZERO_ADDRESS);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount, multySignContractAddress);
        System.out.println(String.format("MainAsset充值[%s], hash: %s", sendAmount, hash));
    }

    @Test
    public void balanceOf() throws Exception {
        //"swapTokenContractAddress": "0x57691c1effa1ca7bcb9de5eefd3d1b23d736148f",
        //        "baseTokenContractAddress": "0x0298c2b32eae4da002a15f36fdf7615bea3da047",
        String account = "0x57691c1effa1ca7bcb9de5eefd3d1b23d736148f";
        String contract = "0x0298c2b32eae4da002a15f36fdf7615bea3da047";
        BigInteger erc20Balance = htgWalletApi.getERC20Balance(account, contract);
        System.out.println(erc20Balance);
    }

    @Test
    public void UsdtCrossOutIITest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // 初始化 账户
        setAccount_user();
        // 费
        String mainValue = "0.02";
        // ERC20 转账数量
        String sendAmount = "2.22";
        // 初始化 ERC20 地址信息
        setErc20USDT();
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        // crossOut erc20转账
        BigInteger mainValueBi = new BigDecimal(mainValue).movePointRight(18).toBigInteger();
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount, erc20Address, null);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, mainValueBi, multySignContractAddress);

        System.out.println(String.format("CrossOutII[%s], 交易hash: %s", sendAmount, hash));
    }

    /**
     * 新方式充值erc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // 初始化 账户
        setAccount_user();
        // ERC20 转账数量
        String sendAmount = "20";// 1.123456, 1234.123456789123456789
        // 初始化 ERC20 地址信息
        //setErc20USDT();
        setErc20NVT();
        //setErc20USD18();
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {}));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20授权
            String approveAmount = "99999999";
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("erc20授权充值[%s], 授权hash: %s", approveAmount, authHash));
            while (htgWalletApi.getTxReceipt(authHash) == null) {
                System.out.println("等待8秒查询[ERC20授权]交易打包结果");
                TimeUnit.SECONDS.sleep(8);
            }
            TimeUnit.SECONDS.sleep(8);
            BigInteger tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("等待8秒查询[ERC20授权]交易额度");
                TimeUnit.SECONDS.sleep(8);
                tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            }
        }
        System.out.println("[ERC20授权]额度已满足条件");
        // crossOut erc20转账
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, erc20Address);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT);
        System.out.println(String.format("erc20充值[%s], 充值hash: %s", sendAmount, hash));
    }

    @Test
    public void getBalance() throws Exception {
        setLocalTest();
        setAccount_user();
        erc20BalancePrint("NVT", from, NVT_ETHW_MINTER, 8);
        erc20BalancePrint("USDT", from, USDT_ETHW, 6);
        balancePrint(from, 18);
        balancePrint(multySignContractAddress, 18);
    }

    protected void balancePrint(String address, int decimals) throws Exception {
        BigDecimal balance = htgWalletApi.getBalance(address);
        String result = balance.movePointLeft(decimals).toPlainString();
        System.out.println(String.format("address: %s, balance: %s", address, result));
    }

    protected void erc20BalancePrint(String desc, String address, String erc20, int decimals) throws Exception {
        BigInteger erc20Balance = htgWalletApi.getERC20Balance(address, erc20);
        String result = new BigDecimal(erc20Balance).movePointLeft(decimals).toPlainString();
        System.out.println(String.format("[%s]address: %s, erc20 balance: %s", desc, address, result));
    }

    @Test
    public void registerERC20Minter() throws Exception {
        // 正式网数据
        setMain();
        // GasPrice准备
        long gasPriceGwei = 100L;
        BigInteger gasPrice = BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9));
        // 超级账户，加载凭证，用私钥
        Credentials credentials = Credentials.create("");
        // 多签合约地址
        String contractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        // 注册的ERC20Minter合约地址
        String erc20Minter = "0x7b6F71c8B123b38aa8099e0098bEC7fbc35B8a13";

        EthGetTransactionCount transactionCount = htgWalletApi.getWeb3j().ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        //创建RawTransaction交易对象
        Function function = new Function(
                "registerMinterERC20",
                List.of(new Address(erc20Minter)),
                List.of(new TypeReference<Type>() {
                })
        );

        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(50000L),
                contractAddress, encodedFunction
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = htgWalletApi.getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();
        System.out.println(ethSendTransaction.getTransactionHash());
    }

    /**
     * 还原合约管理员
     */
    @Test
    public void resetContractManager() throws Exception {
        setLocalTest();
        // 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65,0xd29E172537A3FB133f790EBE57aCe8221CB8024F,0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
        String txKey = "bbbf300000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{};
        /*String[] removes = new String[]{
                "0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17",
                "0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65",
                "0xd29E172537A3FB133f790EBE57aCe8221CB8024F"
        };*/
        String[] removes = new String[]{
                "0x5d6a533268a230f9dc35a3702f44ebcc1bcfa389",
                "0x9d12d368cc5d3461f157ef7fe58513863844b909"
        };
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    protected void setMainData() {
        setMain();
        // "0xd87f2ad3ef011817319fd25454fc186ca71b3b56"
        // "0x0eb9e4427a0af1fa457230bef3481d028488363e"
        // "0xd6946039519bccc0b302f89493bec60f4f0b4610"
        list = new ArrayList<>();
        list.add("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b");// 公钥: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("");// 公钥: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("");// 公钥: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        this.multySignContractAddress = "0x67b3757f20DBFa114b593dfdAc2b3097Aa42133E";
        init();
    }
    /**
     * 添加 N 个管理员
     */
    @Test
    public void managerAdd() throws Exception {
        setLocalTest();
        // GasPrice准备
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        String txKey = "aaa1000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{
                "0x847772e7773061c93d0967c56f2e4b83145c8cb2", "0x6392c7ed994f7458d60528ed49c2f525dab69c9a",
                "0x4273e90fbfe5fca32704c02987d938714947a937", "0xfa27c84ec062b2ff89eb297c24aaed366079c684",
                "0x9f14432b86db285c76589d995aab7e7f88b709df", "0xc11d9943805e56b630a401d4bd9a29550353efa1",
                "0x42868061f6659e84414e0c52fb7c32c084ce2051", "0x3091e329908da52496cc72f5d5bbfba985bccb1f",
                "0x26ac58d3253cbe767ad8c14f0572d7844b7ef5af", "0x49467643f1b6459caf316866ecef9213edc4fdf2",
                "0x9dc0ec60c89be3e5530ddbd9cf73430e21237565", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"
        };
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = 3;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    @Test
    public void allContractManagerSet() throws Exception {
        setMainData();
        //setLocalTest();
        boolean queryBalance = false;
        System.out.println("查询当前合约管理员列表，请等待……");
        Set<String> all = this.allManagers(multySignContractAddress);
        System.out.println(String.format("size : %s", all.size()));
        for (String address : all) {
            if (queryBalance) {
                BigDecimal balance = htgWalletApi.getBalance(address).movePointLeft(18);
                System.out.print(String.format("address %s : %s", address, balance.toPlainString()));
            } else {
                System.out.print(String.format("address %s", address));
            }
            System.out.println();
        }
    }

    private Set<String> allManagers(String contract) throws Exception {
        Function allManagersFunction = new Function(
                "allManagers",
                List.of(),
                List.of(new TypeReference<DynamicArray<Address>>() {
                })
        );
        Function function = allManagersFunction;
        String encode = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction ethCallTransaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contract, encode);
        EthCall ethCall = htgWalletApi.getWeb3j().ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        String value = ethCall.getResult();
        List<Type> typeList = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        List<String> results = new ArrayList();
        for(Type type : typeList) {
            results.add(type.getValue().toString());
        }
        String resultStr = results.get(0).substring(1, results.get(0).length() - 1);
        String[] resultArr = resultStr.split(",");
        Set<String> resultList = new HashSet<>();
        for(String result : resultArr) {
            resultList.add(result.trim().toLowerCase());
        }
        return resultList;
    }

    /**
     * 添加10个管理员，4个签名
     */
    @Test
    public void managerAdd10By4Managers() throws Exception {
        String txKey = "aaa0000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x9f14432b86db285c76589d995aab7e7f88b709df", "0x42868061f6659e84414e0c52fb7c32c084ce2051", "0x26ac58d3253cbe767ad8c14f0572d7844b7ef5af", "0x9dc0ec60c89be3e5530ddbd9cf73430e21237565", "0x6392c7ed994f7458d60528ed49c2f525dab69c9a", "0xfa27c84ec062b2ff89eb297c24aaed366079c684", "0xc11d9943805e56b630a401d4bd9a29550353efa1", "0x3091e329908da52496cc72f5d5bbfba985bccb1f", "0x49467643f1b6459caf316866ecef9213edc4fdf2", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = 4;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    protected void setUpgradeMain() {
        setMain();
        list = new ArrayList<>();
        // 把CC的私钥放在首位
        list.add("");// 0xd6946039519bccc0b302f89493bec60f4f0b4610
        list.add("");// 0xd87f2ad3ef011817319fd25454fc186ca71b3b56
        list.add("");// 0x0eb9e4427a0af1fa457230bef3481d028488363e
        list.add("");// ???
        list.add("");// ???
        this.multySignContractAddress = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        init();
    }

    /**
     * 顶替一个管理员，10个签名
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setUpgradeMain();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        String txKey = "2755b93611fa03de342f3fe73284ad02500c6cd3531bbb93a94965214576b3cb";
        String[] adds = new String[]{"0xaff68cd458539a16b932748cf4bdd53bf196789f"};
        String[] removes = new String[]{"0xf08877ba2b11f9f7d3912bba36cc2b21447b1b42"};
        int txCount = 1;
        int signCount = 10;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * 顶替一个管理员，15个签名
     */
    @Test
    public void managerReplace1By15Managers() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        String txKey = "ccc0000000000000000000000000000000000000000000000000000000000001";
        //String[] adds = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        //String[] removes = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] adds = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        int txCount = 1;
        int signCount = 15;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * eth提现，15个签名
     */
    @Test
    public void ethWithdrawBy15Managers() throws Exception {
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 15;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("MainAsset提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * eth提现，10个签名
     */
    @Test
    public void ethWithdrawBy10Managers() throws Exception {
        String txKey = "eee0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.02";
        int signCount = 10;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("MainAsset提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * erc20提现，多个签名
     */
    @Test
    public void erc20WithdrawByManyManagers() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        setErc20USDT();
        String txKey = "fff0000000000000000000000000000000000000000000000000000000000004";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "43211234";
        String erc20 = erc20Address;
        int tokenDecimals = erc20Decimals;
        int signCount = 15;
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * erc20提现，增发的ERC20
     */
    @Test
    public void erc20WithdrawWithERC20Minter() throws Exception {
        String txKey = "ggg0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "30";
        String erc20 = "0x9c15Fc332D1AefA1c8E9EFbF11FB61a8867975F9";
        int tokenDecimals = 2;
        int signCount = 4;
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * eth提现，异常测试
     */
    @Test
    public void errorMainAssetWithdrawTest() throws Exception {
        String txKey = "h500000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 11;
        list.addAll(5, list.subList(5, 10));
        //this.VERSION = 2;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("MainAsset提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * erc20提现，异常测试
     */
    @Test
    public void errorErc20WithdrawTest() throws Exception {
        String txKey = "I000000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "30";
        String erc20 = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        int tokenDecimals = 6;
        int signCount = 15;
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * 管理员变更，异常测试
     */
    @Test
    public void errorChangeTest() throws Exception {
        String txKey = "J000000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        int txCount = 1;
        int signCount = 15;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    @Test
    public void txInputCrossOutDecoderTest() throws JsonProcessingException {
        String input = "0x0889d1f00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000bebc20000000000000000000000000025ebbac2ca9db0c1d6f0fc959bbc74985417bab00000000000000000000000000000000000000000000000000000000000000025544e565464545350526e586b446961677937656e7469314b4c37354e553541784339735141000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_CROSS_OUT);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));

    }

    @Test
    public void txInputWithdrawDecoderTest() throws JsonProcessingException {
        String input = "0xab6c2b1000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000003250dabb584f7fea1bafaff6000ffbbd2f419a1500000000000000000000000000000000000000000000000000000574fbde600000000000000000000000000000000000000000000000000000000000000000010000000000000000000000007b6f71c8b123b38aa8099e0098bec7fbc35b8a130000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000004066333561313030303464636638663432396261633562663633353665346363343562306163393332363131363130653937613334633664643334393330656336000000000000000000000000000000000000000000000000000000000000028a8dab1935f885ece286c5b779282232f4658c687aac415e27b8f0645b1dcffa294013227e9540245b96ede384d0728343570d01fee23941bf78de62368396f2de1c55e9b073b832f6744f79bf1a8a50fd1c058c1c7c08c7cfc61c7d0f251cfb47ac2b5e69335b4a36a5ca3365bfb5ae92409c8f988c71ecb1e88baeb0653173ca5b1be2ec1ec9dfcc9eef5f9c31deed9a81836a8d2ed3557355b4827f9de4bfb5d7bf6e1c2c9e69eb781cc85e0698c016a9925eb48b30fc7f91a5abb7c48a07c7304c1bab13e934d17f1a56d1fe385c1b71941fba3b1bd4f8eabfcd28b190a8ab38f69f66de77fefa68d592b3449ba6df08b8de2bdf22cd7ca2027c0c1a59479f453e821cb0fa933c9ef8ad0e4a8c47a617ef9e9a2c8ca287f7afbf16192638cd5e80c66d2bf7ac16595774531806123b75fe52677f11523430d813ebf5340e71fff05b171b6836245f2eaebe5c03a45fa4a341816aeceabf3f7f4372fc1c2c73cdff7b9a3436bf83233ee2c73b52842b22ce907b691fa41f91c9ab2245599631acd0b7b8791ca69172dd536325187f486eec65d19e0da949ddd3e25a84aa0f0a0b312c32b7fa136ae567be43733eb1976cdfb5679ae2f674d420eb7dd22a4e01d63bdc6c87cd1b8bd1554af7adc6eed10410789260e4fa4832b39e17f209064a64db9d1982910d0e84ceb6065be2fa41c03238ae31dcd72cabc623c723dffb719865a1bbdf5d451b4aa0ebe27103c19e3b44a0361676d264646531f4891f6d64e25ca27609cfbbbf09e2d88a4bff0b51dad3dfeea201eb3ae449094b1f5bd0a2d2a5efc8c12bc99d1b0067fb46208a3fb9000ab8988be48e64bde7e1545639f9c40bd99ff3e628d191426ac99e706c02da81a431ac60d3e752c0faaaf6c6981711d12af3bcab6057671c00000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));
        String signs = HexUtil.encode((byte[]) typeList.get(5));
        System.out.println(signs);
        System.out.println(String.format("签名个数: %s", signs.length() / 130));

    }

    @Test
    public void txInputFunctionDecoderTest() throws JsonProcessingException {
        String input = "0x95ca26fd0000000000000000000000000cba7a8324c9b7355cef30b4429ddc5eb45e9a0000000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, Utils.convert(
                List.of(
                        new TypeReference<Address>(){},
                        new TypeReference<DynamicBytes>(){}
                )
        ));
        System.out.println(JSONUtils.obj2PrettyJson(typeList));


    }

    @Test
    public void txInputChangeDecoderTest() throws JsonProcessingException {
        String changeInput = "0x0071922600000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000012000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000004062626237303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000008f05ae1c759b8db56ff8124a89bb1305ece17b65000000000000000000000000000000000000000000000000000000000000018622ea0e57097e0ade0f29ec30c3175e26968c44fa48a42bf4a8aae5107ef7e06b2c2d2eb4ed205888278404bd670e59d5071cbf9482e58c3b874362bc6ae923441c31b542c42c81b5f22a584893e6b0de5a5d3f56659c8e4c63241f48b6452f21082ba7f16b1c1050e92aabd95f2ba41bc329c6d23bd46a45e45e256b06e0e6964b1cf602f385625035fa67eef6bb3f210bf426377d4db11d3f4a1bd509dda49665f31e6fa6b9aaf385c5d5548d8dbfb884fc5f2b0262c343a0bfd40612b2009242311c8d24c78cc3a03e2af980a90ba27400ffe64dd52bae055dd1a4f27c76b898e5fe6343c6a8704120f350635387b8b04e84277c06c7a9131dc4077ec9482b99cca31b4285d50efa9568c60336144f063ab3d2d2c8a0cea6f3f30ab3b39bdc2994add63ffaae9e1fb07fb6d2f0e622a8eed50c9ed28673122b00a5e7a65e595bd938821c40cb87676e728e7563be0c1381ccb7a25dc9030dd9edc7235c192dde8a65637534a4f516def65a9036af04759dcf30e408788ad00724646e4939a915d233b9171b0000000000000000000000000000000000000000000000000000";
        List<Object> typeListOfChange = HtgUtil.parseInput(changeInput, HtgConstant.INPUT_CHANGE);
        System.out.println(JSONUtils.obj2PrettyJson(typeListOfChange));
    }

    @Test
    public void encoderWithdrawTest() {
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        BigInteger value = BigInteger.valueOf(10000000000000000L);
        Boolean isContractAsset = false;
        String erc20 = "0x0000000000000000000000000000000000000000";
        String hash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, value, isContractAsset, erc20, (byte) 3);
        System.out.println(String.format("hashSalt: %s", htgContext.hashSalt()));
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void encoderChangeTest() {
        String txKey = "aaa0000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x9f14432b86db285c76589d995aab7e7f88b709df", "0x42868061f6659e84414e0c52fb7c32c084ce2051", "0x26ac58d3253cbe767ad8c14f0572d7844b7ef5af", "0x9dc0ec60c89be3e5530ddbd9cf73430e21237565", "0x6392c7ed994f7458d60528ed49c2f525dab69c9a", "0xfa27c84ec062b2ff89eb297c24aaed366079c684", "0xc11d9943805e56b630a401d4bd9a29550353efa1", "0x3091e329908da52496cc72f5d5bbfba985bccb1f", "0x49467643f1b6459caf316866ecef9213edc4fdf2", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        int count = 1;
        String[] removes = new String[]{};
        String hash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, (byte) 2);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void changeEventTest() throws Exception {
        String data = "0x000000000000000000000000742e9290053f63f38270b64b1a8daf52c91e6a510000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000271000000000000000000000000080b47d949b4bbd09bb48300c81e2c30df243310c00000000000000000000000000000000000000000000000000000000000000264e4552564565706236426b3765474b776e33546e6e373534633244674b65784b4c5467325a720000000000000000000000000000000000000000000000000000";
        List<Object> eventResult = HtgUtil.parseEvent(data, HtgConstant.EVENT_CROSS_OUT_FUNDS);
        System.out.println(JSONUtils.obj2PrettyJson(eventResult));
    }

    @Test
    public void ethSign() {
        String hashStr = "0x6c6cac790fd0558d28ba8a1d0b52b958eef4387bcffbb38fa0c4663dd07cab70";
        String signaturesResult = this.ethSign(hashStr, 15);
        System.out.println(String.format("signatures: 0x%s", signaturesResult));
    }

    @Test
    public void verifySign() {
        String vHash = "0x3bd2e6b75230eef9cfee5240e3dbb656410bff53c59e08e48eb07eef62b904dd";
        String data = "bb7a28181f68a36af5b69a9778eb1e17fe8016f7dba9053054d4989e6ab4144446169ab6e04e2b55e56949b33d49683839c3be6822d4f4a9dc3f29589f9ffa1e1b6f9477428711aa35936e3909283abcf629186884d09f5dd8dff699d90aa61f337a51831ae74f9a313c147bb08136731671c8cd4ee7e44539cc79fecaf3897d9f1b328ac24121da89f46a034c17a7d4869972fb41ba3d9a5661eafab988851cfbd656e04ed25f88727a66bb203f8787f4665e89110f769cde070249879d02b566311c";
        int times = data.length() / 130;
        int k = 0;
        for (int j=0;j<times;j++) {
            String signed = data.substring(k, k + 130);
            signed = Numeric.cleanHexPrefix(signed);
            if (signed.length() != 130) {
                return;
            }
            String r = "0x" + signed.substring(0, 64);
            String s = "0x" + signed.substring(64, 128);
            ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(r), Numeric.decodeQuantity(s));
            byte[] hashBytes = Numeric.hexStringToByteArray(vHash);
            for (int i = 0; i < 4; i++) {
                BigInteger recover = Sign.recoverFromSignature(i, signature, hashBytes);
                if (recover != null) {
                    String address = "0x" + Keys.getAddress(recover);
                    System.out.println(String.format("index: %s, address: %s", i, address));
                }
            }
            k += 130;
        }

    }

    @Test
    public void test() {
        byte n = (byte) 23;
        System.out.println(String.format("%02x", n & 255));
        System.out.println(Numeric.toHexStringNoPrefix(new byte[]{n}));
        String base64 = "KOoEFTB97jAj695UEfVZJtk5oYcroD2om9MhMOa6r3VsF+aZCATTUoc6XKeczWU+pftfuixAgzYR+gZKCWvvQRtF6rhSt8x8WgVT9YC6gXjpoJHjfCy2L3QFK3MypnoQNE5FM5Zgrctcw6Nvi+wLyoD2iyekpSAM93B9swgITyQ3GyeQgsiGhxTuXi0653pUDh4QtRjdm8GuSFMuUI6DBfCSaoTnJPCuDxLQSZLw2HMCfui0vTCz0wtI+JnFq0mQ/yYcTBJoYuAAL9lhX3YaB4gwlSAoxlRR4j6+h06kTs6YAkJkSUk/Vkcv2p9GHFR69KW3QxggHz33MQZHxGZgifp2yhvH5EaWiaWLJ1Qmqs74anqUwSQLpUAvrxeUKxqX/0uGsjt1VgmWo8ZlVTCyICZ8kE5/aOKXRDgRPJBMpWE89HaaG1V1vIxchMaIlolU60U2GRpPPGSDHBbW4T8DA4fSs2J3JKh0cgq4hS+Ukmm5qPYLXwQ2qBHkpUGfbivhxX7coIQby+04KBj1mxybic079IfzzFxiqptDIyzwke/jVK37UV4oCIxLCYN9R5UZfdN9UoxZseB8a2/yDQBlzDLvKmewRhwbv+10HWnHzAY/a0V9D9p79nZtwwd6CS45dv3qBKxVfCSxQF2cdRAs+AUTWeWXHnxi6rG9/E1o7VHfKLjliYsKHCjjDCron0Z4FzkXzaZwAu25Q23oVobb/4mcSrfuw46KOCNLMvZCUpNCg6xr01mIlGvi85/tNEkrqZJc4z5Kj0Ic+ryuopO67bX6iUNCZeN57kL8Mf+n73GUi4r0s06WR2UKi0qVbuXtqOcm3raXmVG2ZJoRTKHu8pALjskWq8DU6xsQQnPFIr9YODvjV/yoKxi/aE3Aj+P4/LjsfNyQDk7DnDq8Fje0Lb3Z18nJKC4sjkN8+RqKVe9/sXT8E8mcXe52HBERERERERERERERERERERERERERERERERERERERERERERERERERERE=";
        System.out.println(Numeric.toHexString(Base64.getDecoder().decode(base64)));
    }

    @Test
    public void arrayTest() {
        String[] arr = new String[]{"aa", "bb", "cc", "dd", "ee", "ff", "gg", "hh", "ii"};
        // aa, "", cc, "", ee
        arr[1] = "";
        arr[3] = "";
        int tempIndex = 0x10;
        for (int i = 0; i < arr.length; i++) {
            String temp = arr[i];
            if ("".equals(temp)) {
                if (tempIndex == 0x10) {
                    tempIndex = i;
                }
                continue;
            } else if (tempIndex != 0x10) {
                arr[tempIndex] = arr[i];
                tempIndex++;
            }
        }
        System.out.println(Arrays.toString(arr));
        System.out.println(0x10);
    }

    @Test
    public void getHtTransaction() throws Exception {
        setMain();
        String directTxHash = "0x54a99b46b1652f90a11c43f3ed47bc0d994003e7e88d1eb64799d424f945cad5";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        System.out.println(JSONUtils.obj2PrettyJson(tx));
    }

    @Test
    public void ethDepositByCrossOutTest() throws Exception {
        String directTxHash = "0x176856463c4bf086e5f6df8c600867f5e3d39f6a293bcac188f4bcc61e019b3d";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        HtgParseTxHelper helper = new HtgParseTxHelper();
        BeanUtilTest.setBean(helper, "htgWalletApi", htgWalletApi);
        HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
        boolean crossOut = helper.validationEthDepositByCrossOut(tx, po);
        System.out.println(crossOut);
        System.out.println(po.toString());
    }

    @Test
    public void testMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.entrySet().stream().forEach(e -> e.setValue(e.getValue() + 2));
        System.out.println(map);
    }

    @Test
    public void erc20DepositByCrossOutTest() throws Exception {
        // 直接调用erc20合约
        String directTxHash = "0x6abc5a7f2f50e644bb0e75caae0a460d0f8793c19da7b272074784ebee5b8ab5";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        HtgParseTxHelper helper = new HtgParseTxHelper();
        BeanUtilTest.setBean(helper, "htgWalletApi", htgWalletApi);
        BeanUtilTest.setBean(helper, "htgERC20Helper", new MockHtgERC20Helper());
        HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
        boolean crossOut = helper.validationEthDepositByCrossOut(tx, po);
        System.out.println(crossOut);
        System.out.println(po.toString());
    }

    @Test
    public void calGasPriceTest() {
        BigDecimal nvtUsd = new BigDecimal("0.09");
        BigDecimal nvtAmount = new BigDecimal(5_00000000L);
        BigDecimal ethUsd = new BigDecimal("4317.16");
        int assetId = 2;
        BigDecimal price = HtgUtil.calcGasPriceOfWithdraw(AssetName.NVT, nvtUsd, nvtAmount, ethUsd, assetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        System.out.println(price.movePointLeft(9).toPlainString());
    }

    @Test
    public void getBlockHeaderByHeight() throws Exception {
        Long height = Long.valueOf(1560320);
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockHeight() throws Exception {
        //setMain();
        System.out.println(htgWalletApi.getBlockHeight());
    }

    @Test
    public void getChainId() throws Exception {
        setMain();
        System.out.println(htgWalletApi.getWeb3j().ethChainId().send().getChainId());
    }

    @Test
    public void getTx() throws Exception {
        // 直接调用erc20合约
        String directTxHash = "0x69fe3689e5fd3a601d85f52710f05caafa4240c89ef7529e931ae3ced3e3d3df";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        System.out.println(JSONUtils.obj2PrettyJson(tx));
    }

    @Test
    public void getTxReceipt() throws Exception {
        String directTxHash = "0x531d7ee163ef7e258036d701e730a5bc784a589999e4bfaf1d193700ac429bdf";
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt);
    }

    @Test
    public void getCurrentGasPrice() throws IOException {
        setMain();
        BigInteger gasPrice = htgWalletApi.getWeb3j().ethGasPrice().send().getGasPrice();
        System.out.println(gasPrice);
        System.out.println(new BigDecimal(gasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
    }

    @Test
    public void symboltest() throws Exception {
        setMain();
        // usdt 0xb6D685346106B697E6b2BbA09bc343caFC930cA3
        // nvt 0x8B3b22C252F431a75644E544FCAf67E390A206F4
        String contractAddress = "0xe176ebe47d621b984a73036b9da5d834411ef734";
        List<Type> symbolResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return;
        }
        String symbol = symbolResult.get(0).getValue().toString();
        System.out.println(symbol);

        List<Type> nameResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getNameERC20Function());
        if (nameResult.isEmpty()) {
            return;
        }
        String name = nameResult.get(0).getValue().toString();
        System.out.println(name);

        List<Type> decimalsResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getDecimalsERC20Function());
        if (decimalsResult.isEmpty()) {
            return;
        }
        String decimals = decimalsResult.get(0).getValue().toString();
        System.out.println(decimals);
    }

    @Test
    public void approveTest() throws Exception {
        // erc20授权
        //BigInteger approveAmount = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",16);
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        setMain();
        String from = "";
        String fromPriKey = "";
        String to = "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5";
        String erc20 = "0x54e4622DC504176b3BB432dCCAf504569699a7fF";
        BigInteger approveAmount = new BigInteger("1",16);
        Function approveFunction = this.getERC20ApproveFunction(to, approveAmount);
        String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20);
        System.out.println(String.format("erc20授权充值[%s], 授权hash: %s", approveAmount, authHash));
    }

    @Test
    public void sendETHTx() throws Exception {
        BigInteger latestNonce = htgWalletApi.getLatestNonce("0xf173805F1e3fE6239223B17F0807596Edc283012");
        System.out.println(latestNonce);
        String txHash = htgWalletApi.sendMainAssetWithNonce("0xf173805F1e3fE6239223B17F0807596Edc283012", "0xD15FDD6030AB81CEE6B519645F0C8B758D112CD322960EE910F65AD7DBB03C2B",
                "0xf173805F1e3fE6239223B17F0807596Edc283012",
                BigDecimal.ZERO,
                BigInteger.valueOf(21000),
                new BigDecimal("15.1").scaleByPowerOfTen(9).toBigInteger(),
                latestNonce);
        System.out.println(txHash);
    }

    @Test
    public void approveZeroTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // 初始化 账户
        setAccount_user();
        // 初始化 ERC20 地址信息
        setErc20USDT();
        // erc20授权
        String approveAmount = "0";
        Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
        String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
        System.out.println(String.format("erc20授权[%s], 授权hash: %s", approveAmount, authHash));
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }
}