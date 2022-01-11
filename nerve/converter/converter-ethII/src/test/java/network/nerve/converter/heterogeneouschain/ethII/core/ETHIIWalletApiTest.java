package network.nerve.converter.heterogeneouschain.ethII.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.ethII.base.BaseII;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ETHIIWalletApiTest extends BaseII {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;

    protected void setErc20USDI() {
        erc20Address = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        erc20Decimals = 6;
    }
    protected void setErc20USDX() {
        erc20Address = "0xB058887cb5990509a3D0DD2833B2054E4a7E4a55";
        erc20Decimals = 6;
    }
    protected void setErc20NVT() {
        erc20Address = "0x25EbbAC2CA9DB0c1d6F0fc959BbC74985417BaB0";
        erc20Decimals = 8;
    }
    protected void setErc20NULS() {
        erc20Address = "0x79D7c11CC945a1734d21Ef41e631EFaE894Af2C3";
        erc20Decimals = 8;
    }
    protected void setErc20GOAT() {
        erc20Address = "0xfeD0D0E316aC3E1EA8e46771fEd27Cf18f883122";
        erc20Decimals = 9;
    }
    protected void setAccount_c684() {
        from = "0xfa27c84eC062b2fF89EB297C24aaEd366079c684";
        fromPriKey = "b36097415f57fe0ac1665858e3d007ba066a7c022ec712928d2372b27e8513ff";
    }
    protected void setAccount_EFa1() {
        from = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        fromPriKey = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    }

    @Before
    public void before() {

    }

    protected void setDev() {
        list = new ArrayList<>();
        list.add("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b");// 0xbe7fbb51979e0b0b70c48284e895e228290d9f73
        list.add("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75");// 0xcb1fa0c0b7b4d57848bddaa4276ce0776a3215d2
        list.add("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78");// 0x54103606d9fcdb40539d06344c8f8c6367ffc9b8
        list.add("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530");// 0x847772e7773061c93d0967c56f2e4b83145c8cb2
        list.add("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7");// 0x4273e90fbfe5fca32704c02987d938714947a937

        list.add("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200");// 0x9f14432b86db285c76589d995aab7e7f88b709df
        list.add("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a");// 0x42868061f6659e84414e0c52fb7c32c084ce2051
        list.add("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1");// 0x26ac58d3253cbe767ad8c14f0572d7844b7ef5af
        list.add("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e");// 0x9dc0ec60c89be3e5530ddbd9cf73430e21237565
        list.add("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b");// 0x6392c7ed994f7458d60528ed49c2f525dab69c9a

        list.add("B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF");// 0xfa27c84ec062b2ff89eb297c24aaed366079c684
        list.add("4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39");// 0xc11d9943805e56b630a401d4bd9a29550353efa1
        list.add("e70ea2ebe146d900bf84bc7a96a02f4802546869da44a23c29f599c7e42001da");// 0x3091e329908da52496cc72f5d5bbfba985bccb1f
        list.add("4c6b4c5d9b07e364d6b306d1afe0f2c37e15c64ac5151a395a4c570f00ce867d");// 0x49467643f1b6459caf316866ecef9213edc4fdf2
        list.add("2fea28f438a104062e4dcd79427282573053a6b762e68b942055221462c46f02");// 0x5e57d62ab168cd69e0808a73813fbf64622b3dfd

        list.add("08407198c196c950afffd326a00321a5ea563b3beaf640d462f3a274319b753d");// 0x7dc432b48d813b2579a118e5a0d2fee744ac8e02 16
        this.multySignContractAddress = "";
        init();
    }
    protected void setLocalTest() {
        list = new ArrayList<>();
        list.add("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5");// 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
        list.add("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f");// 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
        list.add("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499");// 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
        list.add("43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D");// 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65
        list.add("0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85");// 0xd29E172537A3FB133f790EBE57aCe8221CB8024F
        list.add("CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2");// 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
        list.add("c98cf686d26af4ec8e8cc8d8529a2494d9a3f1b9cce4b19bacca603734419244");//
        list.add("493a2f626838b137583a96a5ffd3379463a2b15460fa67727c2a0af4f8966a05");//
        list.add("4ec4a3df0f4ef0db2010d21d081a1d75bbd0a7746d5a83ba46d790070af6ecae");// 0x5d6a533268a230f9dc35a3702f44ebcc1bcfa389
        this.multySignContractAddress = "0xdcb777E7491f03D69cD10c1FeE335C9D560eb5A2";
        init();
    }
    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        list.add("7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b");// 0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        list.add("");// TNVTdTSPTtz11cQnFMDUBXwPNzKEg9GUkbKgn 0xfe3199255e9a20d334032c4e08717ddcee4051a9
        list.add("");// TNVTdTSPG3P5L6YBdu6dWW3ntVqLX7an9cPvg 0xcf6928aaec226dbec5ef6aa3c972716a7a2a4a10
        list.add("");// TNVTdTSPS3D8f4vQTupY9nTTbj1KvDAji5vqM 0x55c1db80c6498ef42bff065c7be98644e67f0b23
        this.multySignContractAddress = "0x7D759A3330ceC9B766Aa4c889715535eeD3c0484";
        init();
    }

    public void init() {
        EthContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
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
        BigInteger totalSupply = this.erc20TotalSupply("0x2cC112629954377620A20CE4fD730df8D977E6fE");
        System.out.println(totalSupply);
        BigInteger totalSupply1 = this.erc20TotalSupply("0xae7FccFF7Ec3cf126cd96678ADAE83a2b303791C");
        System.out.println(totalSupply1);
        BigInteger totalSupply2 = this.erc20TotalSupply("0x856129092C53f5E2e4d9DB7E04c961580262D0AE");
        System.out.println(totalSupply2);
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
     * 给多签合约转入eth和erc20（用于测试提现）
     */
    @Test
    public void transferETHAndERC20() throws Exception {
        BigInteger gasPrice = EthContext.getEthGasPrice();
        // 初始化 账户
        setAccount_EFa1();
        // ETH数量
        String sendAmount = "0.1";
        String txHash = htgWalletApi.sendMainAsset(from, fromPriKey, multySignContractAddress, new BigDecimal(sendAmount), BigInteger.valueOf(81000L), gasPrice);
        System.out.println(String.format("向[%s]转账%s个ETH, 交易hash: %s", multySignContractAddress, sendAmount, txHash));
        // ERC20
        String tokenAddress = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        String tokenAmount = "100";
        int tokenDecimals = 6;
        EthSendTransaction token = htgWalletApi.transferERC20Token(from, multySignContractAddress, new BigInteger(tokenAmount).multiply(BigInteger.TEN.pow(tokenDecimals)), fromPriKey, tokenAddress);
        System.out.println(String.format("向[%s]转账%s个ERC20(USDI), 交易hash: %s", multySignContractAddress, tokenAmount, token.getTransactionHash()));
    }

    /**
     * 新方式充值eth
     */
    @Test
    public void depositETHByCrossOut() throws Exception {
        setLocalTest();
        this.multySignContractAddress = "0xBEe53Bf6C5bFaf07Af2aF5c48077B4DD60396653";
        // 初始化 账户
        setAccount_EFa1();
        // ETH数量
        String sendAmount = "0.1";
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal(sendAmount));
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, EthConstant.ZERO_ADDRESS);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount, multySignContractAddress);
        System.out.println(String.format("eth充值[%s], hash: %s", sendAmount, hash));
    }
    /**
     * 新方式充值erc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalTest();
        this.multySignContractAddress = "0xBEe53Bf6C5bFaf07Af2aF5c48077B4DD60396653";
        //setBeta();
        EthContext.setEthGasPrice(BigInteger.valueOf(3L).multiply(BigInteger.TEN.pow(9)));
        // 初始化 账户
        setAccount_EFa1();
        // ERC20 转账数量
        String sendAmount = "200";
        // 初始化 ERC20 地址信息
        //setErc20USDI();
        //setErc20USDX();
        setErc20NVT();
        //setErc20NULS();
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
        String txKey = "bbbf400000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{};
        /*String[] removes = new String[]{
                "0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17",
                "0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65",
                "0xd29E172537A3FB133f790EBE57aCe8221CB8024F"
        };*/
        // 0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4, 0x8c2cada1927087f6233405788e5d66d23b9378d4
        String[] removes = new String[]{
                "0x5d6a533268a230f9dc35a3702f44ebcc1bcfa389",
                "0x9d12d368cc5d3461f157ef7fe58513863844b909"
        };
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * 添加 N 个管理员
     */
    @Test
    public void managerAdd() throws Exception {
        // 正式网环境数据
        setMainData();
        // GasPrice准备
        long gasPriceGwei = 26L;
        EthContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        String txKey = "aaa6000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{};
        String[] removes = new String[]{"0x018fc24ec7a4a69c83884d93b3b8f87b670c0ef5"};
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    @Test
    public void sendChange() throws Exception {
        setMain();
        long gasPriceGwei = 96L;
        EthContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        this.multySignContractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";

        String fromAddress = "";
        String priKey = "";

        String txKey = "bae357f174a98c9e4142cffed1cf746074d8e7c7fe4c30d24f693ade8f774953";
        String[] adds = {"0x17e61e0176ad8a88cac5f786ca0779de87b3043b"};
        String[] removes = {"0x196c4b2b6e947b57b366967a1822f3fb7d9be1a8"};
        int count = 1;
        String signData = "00c4b328f1e7b6c4e4c4f6a55d063bfaacc0cf0e0900f30a3f715be5e03f9bff142bd5a2728130f3e63c47383401eedd66e2b26a6d303efa8390ab313a1d277d1ca4fa8cb4a306fa7256b79e7cf49d855ff1a390efbfdfd3ca29b7f6ec4e1491086708439a6002191086b235055a327dcda3dacc905ee9910be6ad46ff57eb85271c69b3f2ebb524fa6514685fcd9d578305f336edac868d55eee8aec7f909b50dc93729c8d868802d625f75306f64fcc8345c26c59319bb5c8bab68143a920d963c1b05709851545e2cc58cc65f7c28078bd0130e07176dc4ac93f122b0b9cef1607551c1dca88325519306ac514ba15acb4c0298f4f87d5f798a91b5c842ef1e63141b2f60dc18b13c7b042372fc1f002a9e05046f9b6e62297d93f93ade55c8494b694e0defa27d75e72a67a6b5716cba71a73b31ada7f375452baa97cfec2be42e211b59f5a2e5eac2a793780753fcc587f5f6f9474968837e7c26732f8bae3ac8be0626168be94263e4eaf0daeb5f4edced952977baa4df9e3a84ef6b2c59b8df4fa61b402fc60190319c7798ac163abb3bf6a7672e909890b3d2065b44b51b051a8b3c709c06a5ed7a8e203815e04d51191f4ab4a6c7a5a5dd28520b3376f5257647f01c14ce1133f252312b244ddcba2b0f2d62f11495a54e5eb727ffe66b1247c86a454cd79da6c2723d330288163ac1107631d2aeef57fa1a3f34e87832191e0200a91cdcd64696b7e15dd78da409fa0c3b70ac10a953800faeeb38b15db7edd96c413f657b3a92f129c257639903f524db5675247cc8c94e0689381131530c4ab0740e1b230d23e497ae9ca421db3ddb43b60117559441042bab8ef33aafcf57d91376094e6fa612d144dc8ac50eee84426877fcc6b808802f2b1a2ba52686d1256c59a31b";
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        System.out.println(FunctionEncoder.encode(function));
        String hash = this.sendTx(fromAddress, priKey, function, HeterogeneousChainTxType.CHANGE);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void sendWithdraw() throws Exception {
        setMain();
        long gasPriceGwei = 80L;
        EthContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        this.multySignContractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";

        String fromAddress = "";
        String priKey = "";

        /*String txKey = "6d30f99e589d5a92b6065df0a26a8697c742f14f2b828ef8bd3750beea13656a";
        String toAddress = "0x8caF3998f48f14898CDF25Dc2300bE4f094b9aEA";
        String signData = "0xbbb65fb506594c82cacb5d1853f3354fb53649738df3a1509d5a208349da85550afa5f27d787ed6cae96335b8814e7bfbd2ad39e91d3713d20479876801cb67a1cfe7af0da7872595acd61933fd1f55f60308a5c899d41465eb5b0f4cbe265e3945b14fdcdb4c194b6caca75f3ac043c0283406c2baa1534e35bdd9c8a57c451081c22d4327849b8b59f97dd4a8d2ffcaaca57f51019d4b30339664623ee4c935509768bc7143109e18fc578811f3067f0a1013211165fabc4e683b2fd393c3440bd1c74e153ee92ba764e1ef81e730213338202bb5d2a4f8769527443c321235e7d88614f1438bdeb03342849b6d3d94f08f6a60959de2694add0a7cc97605a8206c51b70374be07b44d5830247419540d80f000621d62c67702940738362d424e5a89c4144a72af6c2e477928f4e9eba788eb7a4a2c0d55bc6e265ed1fc1945b9159f71cc3d0ed2e97c1709db6f8a68972f7e1222432e35808c0a60076ebe7846a9edd9f3f4e4e39cf33f7562525fa5974ee101103fbbf0e65b996193ccad9cb42cb3bcc1c5810066933ed91fad4563c956e8812ac37502654bdc4e8b4b4c3cdbb647456dd343fc5314cd01f394c0dc0318dc89fd9a77b89a153bd92ee5350b13da682d8091b4daf3fd431691900208ba3f73e4b5724fe64f35d192633f0333f743a53d6d07261b0b01d755eb422ba58752a89837ac9fc432bafbe7c633104beaa6cf254c8ee1bfe4959ba9714c3953e5cbb8ba2adf5a3dc0dc4f15b2e7e215a37e03f69744ba27438b4856f3174495daf705cffec9f881245b6f828dfdd618fc1560b1aa1a3c01bca1f1fc049097502be82149186e7f3ec96717e4b3988f30d423d6139025f64a020cc5183e1153df603cbecb71157aafdb96936b5ab2cce443bf1632d34de8b691c";
        BigInteger bValue = new BigInteger("6040000000000000000");
        Function function = HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, false, EthConstant.ZERO_ADDRESS, signData);

        String hash = this.sendTx(fromAddress, priKey, function, HeterogeneousChainTxType.WITHDRAW);
        System.out.println(String.format("hash: %s", hash));*/

        String txKey = "6af3a8256d7309d4c9b9d4b4ab5fd173708a766374cd60d7254957b628472ed3";
        String toAddress = "0xE0d7A4fB97eB822A62a4Be1824A4c83C5e50828D";
        String signData = "b652ef3b02008214d3c0a60018f573490ad95e702f9793f13917f86342c2dc84210a8f43b179f4a3494d07d4acfffc49f77b4566538a61f908681e291aa4b4f91c32fa2a6e9a29f1d3af2649494c1ee7e308929592f95c37a322cf5eefbf20e6876792eec66269149e9bafd722991534e804abc2a01f5e573d940b76c6d1f5f54b1baf84a2896c8e2cc586c36627a54b288f8aa4330bd5bc29dde4a3a2e094a4f74154b590449ca2da93daa37beb9d3556fc7908ae0e76a0f4db86a5eba9a8ca86a41b0b67d43c7ff9332746e353fcff5164278433ad18fd10e0f2541f071d950a5af74a8e5f4b396eeda756b0a9f787cb3dbcc34137da90b0ec7084706ce811ef4e2d1b46894218bbd58573b2fd81f961e153a0d67be2dabb0c664f8a8a94caf8b2477a7d7d2f01b0c2ccca4f39b25f253ff0710cadb46f41d8a6f95485bdebab1c5e621c60dc8c3ddbbe32545fab70d629bc841e50681748462a777005199fae0c1e69df2b866869181f94c6bcb8ead1a37e6c3415b5dc256ef5b8657323745f301090be1b906f82764e372eabfdb1fcbaf7e5a64341169bc80306a1f65c272164c9f4ea6c69beeeafeed8aa71170b19fc32bf40c2fda0275fe75414f878210afb8f02f3fb1bd4bef8d70d18da21a07da4e8eb48011b0d5cc8ac888641d48345124629f14c80051575b2e1745342b3d4e7edabb2d0a22aa16a6baf76205d17344ba2e87a65221b188c3c566758db17d88e434fdb96860eea4fbdbe735b1fcda844735795df6aab64d4149f60e8b1ef14f6dd415b969e5f389a279d26a719f30eedc41e49ef920b1bb02cb234d7c389af1811b1a652d2b2ed663ac36152941e256bd6a9d9a925bee53b7618df4b6203bca40f3be45d71942e37f8ff934cfcc01805fe2297abe9307b1b";
        BigInteger bValue = new BigInteger("1505000000000");
        Function function = HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, bValue, true, "0x7b6f71c8b123b38aa8099e0098bec7fbc35b8a13", signData);

        String hash = this.sendTx(fromAddress, priKey, function, HeterogeneousChainTxType.WITHDRAW);
        System.out.println(String.format("hash: %s", hash));

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

    protected void setMainData() {
        setMain();
        list = new ArrayList<>();
        // 十个虚拟银行的私钥，并把AA的私钥放在首位
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        this.multySignContractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        init();
    }


    /**
     * 5个签名
     */
    @Test
    public void signDataForERC20WithdrawTest() throws Exception {
        setMainData();
        String txKey = "bbb1024000000000000000000000000000000000000000000000000000000000";
        // 接收者地址
        String toAddress = "0x64CE6baa1144e307C68aF1a1fB2ecFe35A058052";
        // 造币数量
        String value = "13068492433.458328880192656953";
        // 新的以太坊网络上的nabox合约
        String erc20 = "0x03d1e72765545729a035e909edd9371a405f77fb";
        int tokenDecimals = 18;
        int signCount = 5;
        String signData = this.signDataForERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，signData: %s", new BigDecimal(value).movePointLeft(tokenDecimals).toPlainString(), signCount, signData));
    }

    /**
     * 根据已有的签名数据 发送交易 - erc20提现
     */
    @Test
    public void sendERC20WithdrawBySignDataTest() throws Exception {
        setMainData();
        String txKey = "bbb1024000000000000000000000000000000000000000000000000000000000";
        // 接收者地址
        String toAddress = "0x64CE6baa1144e307C68aF1a1fB2ecFe35A058052";
        // 造币数量
        String value = "13068492433.458328880192656953";
        // 新的以太坊网络上的nabox合约
        String erc20 = "0x03d1e72765545729a035e909edd9371a405f77fb";
        int tokenDecimals = 18;
        String signData = "???";
        String hash = this.sendERC20WithdrawBySignData(txKey, toAddress, value, erc20, tokenDecimals, signData);
        System.out.println(String.format("ERC20提现%s个，hash: %s", new BigDecimal(value).movePointLeft(tokenDecimals).toPlainString(), hash));
    }

    /**
     * erc20提现，10个签名
     */
    @Test
    public void erc20WithdrawBy10Managers() throws Exception {
        setMainData();
        String txKey = "bbb1024000000000000000000000000000000000000000000000000000000000";
        // 接收者地址
        String toAddress = "0x64CE6baa1144e307C68aF1a1fB2ecFe35A058052";
        // 造币数量
        String value = "13068492433.458328880192656953";
        // 新的以太坊网络上的nabox合约
        String erc20 = "0x03d1e72765545729a035e909edd9371a405f77fb";
        int tokenDecimals = 18;
        int signCount = 10;
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * 顶替一个管理员，10个签名
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setMainData();
        // GasPrice准备
        long gasPriceGwei = 20L;
        EthContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
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
        String txKey = "ccc0000000000000000000000000000000000000000000000000000000000000";
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
        setBeta();
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        String value = "50";
        int signCount = list.size();
        String hash = this.sendETHWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("ETH提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
        String hash = this.sendETHWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("ETH提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * erc20提现，15个签名
     */
    @Test
    public void erc20WithdrawBy15Managers() throws Exception {
        String txKey = "ggg0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "30";
        String erc20 = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        int tokenDecimals = 6;
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
    public void errorETHWithdrawTest() throws Exception {
        String txKey = "h500000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 11;
        list.addAll(5, list.subList(5, 10));
        //this.VERSION = 2;
        String hash = this.sendETHWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("ETH提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
        String input = "0x0889d1f00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000001c8591a900000000000000000000000000037611b28aca5673744161dc337128cfdd2657f6900000000000000000000000000000000000000000000000000000000000000264e45525645657062364332754454533238336d4459326a387a383443557961654141794d6b470000000000000000000000000000000000000000000000000000";
        List<Object> typeList = EthUtil.parseInput(input, HtgConstant.INPUT_CROSS_OUT);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));

    }

    @Test
    public void txInputWithdrawDecoderTest() throws JsonProcessingException {
        String input = "0xab6c2b1000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000001f610776e0d7111dcea613efc9ed41cc7e5dc052000000000000000000000000000000000000000000000000002386f26fc10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000004030353561626565613662666438333765353863333665336164313435663239323633623261313835623532373339393261393736663663313733623536343366000000000000000000000000000000000000000000000000000000000000028aa60e705ca63ec5ff8fbdc2d7c90b730421615f1fdfc0ef70a0cc138c51969375528c8f60111f6f01ebc05d150c9cb093e2df7022ee40475ad5ff869fa180ec9c1bc2213e2340c55024242a9aa4fbc143c86324a31a69d7bf007e5d6192c3c6fd4d33697c087240a2cf70f7becbb6728701743d732e030afbb147286c720c73b97c1c85046012421d44166ac43d558d6d4a49ade3f3d7268d9e3061e353c1007998cc31830855a20d5385fd9646eff43e461b3ad74ae008a84f083e69b8d66b302cb81b2fcb07bb9f4f698ac83429f1a1cfa75edd0315bd3b4d0226fed11718fa0136006609d7c07951a146d4a874d6b9cf94161caf8f86a4c3832121c5ce9d44b588861b98e85379acf8c593e6d4b8db52d7d17594bd40e7d9a4a46af29aead4917cfaf3416e9450ef4f5ba4b145c0092b95899d2e02cabcb9c8d2a2dae9bf9856b194b81cba6f556b119849cc7267538437743bfabfe88a81f2386810ebe64bfc34f2c36f35c03394f3215f2faf29311eefb03ecac32e94fdf01db6878229bfdbcea498fb1b372938ba577739e55844d677819813d1be6b1dc0c66793643564e78ccc4b52171063b818cd4cf0970a1dbaf1965cc8eab474896a34c47ac7f2edf9823312a2921b01d30f8ed68271adaa1447ad324ef4b797249df5ed3766503a8166b94f5ace9c6b6b652078ed0faf7a3029077263a4b4cf88073b43d373c24362e28ed24287b41b64d464a2a2ed80e80b8b869386b8e6032f8c672c67aeb594870347c72961815d4c3acb94b2365cd8e5f80f50ae4ece3f8ae9aaf414dd51f2eda53bbf58fa57241c3348bdebca500be98ba4eef0f5173bf99a621ee9e667d58bb1215365a049aaa82021c213dc176052feceaeddc5271647f912d33af1665b075d6fe2347bba61821c00000000000000000000000000000000000000000000";
        List<Object> typeList = EthUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));
        String signs = HexUtil.encode((byte[]) typeList.get(5));
        System.out.println(signs);
        System.out.println(String.format("签名个数: %s", signs.length() / 130));


    }

    @Test
    public void getWithdrawDecoderTest() throws Exception {
        setMain();
        List<String> errorList = new ArrayList<>();
        errorList.add("0x9ae49e043ff68277b14001b46ea77bcbaeb7e5a9a5823cfe925c4277360cc82b");
        errorList.add("0xe32a27462f006c2ea1bfaf22df74ee5c0e9aa99de9aeaef1c3b9364a06aed2c6");
        errorList.add("0x1437ee52b1293dd821c3e910f728b494e28005edfac44bd06fb8e44aaf0c15e2");
        errorList.add("0xe9cad66845122f9a2d9901cdc1badfe415477e0e4ea3cedd21b134721f8711fe");
        errorList.add("0x6f4bf72d15ed0a9f59e17693602c7e90d04c65855b2ce33166f4399e8f6aac5d");
        errorList.add("0x9ab16ecdb63987eb40ae1465f82e82fbb3f2b27728d1603ddd856efc3025dffa");
        errorList.add("0xcdb36dd4e92e4b11e77f3dcfd6103a291e7b821c84a37c542aef4933639fce6c");
        errorList.add("0x0addb7e2d11936f64810c548d4b77b642c449d2fe576e1a8d59c0afde717fdac");
        errorList.add("0x38934dba8a36cf9c1372e170745af26a0d8c65e456d30694c79365589fb03a52");
        errorList.add("0x3f987320e0f9c3b417c5d08e6cfe8150180d929f12cb9cf341f15df9667a4f3d");
        errorList.add("0x51f82ee99a5fcde16fd42aae545280cb559e7fd73d9732ee3d9a7c59b79a08e2");
        errorList.add("0x9740248e8a9f049a2a7060a066a2c2995d4d3c8d43cfb771b6a7d811228739c7");
        errorList.add("0x2906cb745f056792f92b502c1d39870b2176174e273baebc4623ea2bae3fb03a");
        errorList.add("0x5a2e28695822358477517ba9dd75e2eef631db787eea818d4766c0047a5167b0");
        errorList.add("0x0e814fd721a03c24b69bc15ebf09b110d051e303143c964354dd4eab98b53ba2");
        errorList.add("0x21ce9c9bc128e2c8a0657b8b7b551b9ac94f7d7cced7fffcc86f44fb8269dba1");
        errorList.add("0xd9b5dff5bb24fb1f5d63caa863b1a1879ee9d0ab5d4e402da4b374ce40f7d949");
        errorList.add("0x9a0e1513bfe4cf3a8886dd75d11502c21edb7c1dd4cb6cd9e49f3338862f5747");
        errorList.add("0x5b3a9681d9aea786efbe5104db5012d6c0259c03d76a2cb8fa05cf3eabeeee63");
        errorList.add("0x967b0f5fda1d34817bdeed4a4eb2a00678261846b9bd6f5bbbd0310f6563c358");
        errorList.add("0xff79d042bd7b81e04c2d0c6be7f10b8e4c25b8972d31f1f3d56a9bcade6714af");
        errorList.add("0x7225b55846e181fcb2f1afe3e1ed5ed63b5a0e3432f593fc61f99dd80d98b9bb");
        errorList.add("0x0f81db6dc78d60e9f42c2e7b7afca67222a75c754e8a7b60720e1a13b5bdea72");
        errorList.add("0x1300c33e4f87569f8dfabe58a9a25251670ba7837efe514357084e1dd96ae5d0");
        errorList.add("0x768176b885bdd04b1562292d6124607a5e7ad52f5baf25224e891862f975da1f");
        errorList.add("0xce8c6289b6aea85256ce6249b39f56fe78a9e1f584733af744752178525d7718");
        errorList.add("0x14f5a8f1221c6aa7e257b3e848126a03e4ecd161ef60128ce2609e136176867d");
        errorList.add("0x6fa5a6ae9e257f132e273a7830b7c24bb92ed6f37bdc3a633964b8e5d756d011");
        errorList.add("0xc1e87ba8a39e77788f7ce4e92b6d62c1d6a651d0e511ee2d993e1169e7c0706b");
        errorList.add("0xd2a737b2ef510f44ec614e6b415912c1dcee86e92cd88f6fcf8af9f306cfaee9");
        errorList.add("0x51a7e2347ca81e7db7dc876d2003d986ce78bfbf19011630362c1c7d4091bf03");
        errorList.add("0x9c623ebf26b42237b2545f5b2205b127b228f8b7a34417ae971ce05f4cee5e9e");
        errorList.add("0xc53bf2992e31a6c63f1980b746c7b5f6e5ede88976a498900ff8ff61012ef0cb");
        errorList.add("0x075accaaab20136fe27d11c5b6254c377bb8cb7b32f6886813239a243f781ce8");
        errorList.add("0x4caeacc147eb729e54f028c46483e3d83636c2c9dae09d2d0e56b0050645005e");
        errorList.add("0x85e900115da050502114141414858eac50ca8f4e9808e34de3abc19b23d76b51");
        errorList.add("0x962e3e42e275a006d5c20d9f51c22a31a3250e2a594065597c8d90861a9b2612");
        errorList.add("0x8c9765fe383b8472b2af544aba31936fa1d226a33fd64a2ae62aa110f36ec11b");
        errorList.add("0x0c4b47ed034e6e9130348e743a23a6fba015313ef60675ff10e06d38b06c17ae");
        errorList.add("0x91d111bb56aea94ed992af3236161c2820397f3e2f7d0db74937233e327f6163");
        errorList.add("0x4430abe7a18db2861f90d1ac7fe78f0eec81344f2482abf85c70b152dd0d4d12");
        errorList.add("0x0a5b7b77810c220a400065aebd0f4fd1ff1d6c6342b25eecb23f0184003a133e");
        errorList.add("0xdd644ba1ab564ad0ae357a4aca2fa7cd61902c1148a51673ac46d3b204a97bc5");
        errorList.add("0x9983ee194d565128b69790ab1f4089515aedf80df81bdf8964b99e234cb52463");
        errorList.add("0x46e5b9c0627e2278d9c75986869f45c3833f5dc3b2601ca0d3a884e5d607a8e4");
        errorList.add("0x84f9ba30a7154c636f0120d7892c3f7ee47e777c288bdd6a4eae95cce9898c3e");
        errorList.add("0x346db316366d35616f6afd5772d3a9ee4bf80f6dfabc141d30771c9522c77ae8");
        errorList.add("0x4f9a207ae3307b2fdd6e02ff9e8026a945229d0329995d6ee785cc74ba55f198");
        errorList.add("0xe51e13ba3da8c21cba5a800588669203077f20939ab3766f36c29ade9b4d0b41");
        errorList.add("0x1a01ef960098f99b14562792847af1618c166782b14fe70e36785a526ba9368f");
        errorList.add("0xbbcd679e398ecfa01c3e8eb9e10ff0c828910c4753ced66f79e5dbf8afd0a821");
        errorList.add("0x5c08d3ce69143d34c41e745333ebe2d04e55e35916dc7228212e814dc19d7057");
        errorList.add("0x39c7374b73cc50e90724e8ccda6431e24573bc20ceeab14442a889a52bdaf8fc");
        errorList.add("0x05cf2ac00809124edd0d47b453ef08c17cb36580bbdd059303fab2af359bc7cc");
        errorList.add("0x6df7d831391f89686ad8920c07404dad4b5ffb44778fd227d00d39a202e81eca");
        errorList.add("0xf559f609c46f44069df4483acdaab7639361780d1243c4b731f56330dadf9bab");
        errorList.add("0xc3b0818a4b724217e6642a0200e74264b1d2f6db58725579da4cd295741db3a1");
        errorList.add("0x48aa3f2014c21313091ce4055a4d8ebdc23ddaddb6abcb54b4fbbcaa810f675a");
        errorList.add("0x2b4853a87b98ce0da2e1acb44b0b42f224df4d0aa1b0d64315e4bb764638f6f6");
        errorList.add("0x02aab08ef1eb788f6033e23836a5127a8a2e0da853032a7ebb57d4cec08cbd9e");
        errorList.add("0x5f3dad37d8a8874d5fffe9d2635c248c58667aa96e2e3ba33ada783c4bf7cc4f");
        errorList.add("0x17ee8d400f2fc783f1d44626a6c8a7d4fd7f42a7f8cca23e31e0981548388570");
        errorList.add("0x7871e20d8bbc6e70ac7ae3fad50975450b723a9e9126dd6b36f243284deebc09");
        errorList.add("0x4e0656b8f9f5a09352cfb908fc42248bcdbe6dc9ea0bfc5080e3d198df52247e");
        errorList.add("0x12771fa1db0c9c82887b2561bd7803362d55d07b275e7031b9e04f4f228ce3fc");
        errorList.add("0x7f9f7cb6abf887e90ca130aead5823db9397e7c7f5a7ec6a2290faa0e45d2bae");
        errorList.add("0x54ff182911f5dbce431c7c4b5c195c19d68ee80f15cfdd88ee9575e3f5d62dcd");
        errorList.add("0xa1a8602c6d328ebd65421fae425136103b0b15a7dd076650f38543d415ed6d3e");
        errorList.add("0x310986488b1d9306f6c4094bd993fcb37948380923d40d90d7aea1439eb4e295");
        errorList.add("0xa14f635597870096d62b4848d290de8c40676950a62f94c3c305efed8ecf0f80");
        errorList.add("0x0e53aa6085c3482e6d88155785bac7b85131b545a9cebfe86d24deca8d1a8866");
        errorList.add("0x62ec5a5bef98130ebe23d46629aa7d9fde5a565c74ce4f853b90588a5ee7784d");
        for (String directTxHash : errorList) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            String input = tx.getInput();
            List<Object> typeList = EthUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW);
            System.out.println(directTxHash + "\t" + typeList.get(0).toString() + "\t" + new BigDecimal(tx.getGasPrice()).movePointLeft(9).toPlainString());
            //String signs = HexUtil.encode((byte[]) typeList.get(5));
            //System.out.println(signs);
            //System.out.println(String.format("签名个数: %s", signs.length() / 130));
        }


    }

    @Test
    public void txInputChangeDecoderTest() throws JsonProcessingException {
        String changeInput = "0x0071922600000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000140000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000040633065336439316666316438303636613832623862303966333862383063623034376132666465383862353966346534326466663564663430326561313938320000000000000000000000000000000000000000000000000000000000000001000000000000000000000000196c4b2b6e947b57b366967a1822f3fb7d9be1a8000000000000000000000000000000000000000000000000000000000000000100000000000000000000000010c17be7b6d3e1f424111c8bddf221c9557728b0000000000000000000000000000000000000000000000000000000000000028a0c074974757d9c57fba3e4a82a4c85493abb5107023fcf61ef585d94d5e7b0b75ed9f76a251ba3db8b44028298b5db478053a518ca51ea533b98633e6e5209831ca3367fd298de4967c1e014a9277010ef571ebc2bbc7c8129c377e89efcb2794a0be9c6d63a83f2ae2d5afd9c311bd23785afb9e8cc9fc0f80e57f625b8ace8241c9a6007c4f5d4b5d89478f6afbca0a15496852cc8764b876c7ebcd15d030794ba7dde3063aeb48eec192d53a1705cd5b794004b57437172c76b468e1c7d61dbf81c85963bb8ece815dc5a36bc1c42d463104ea441bdac3c7aca178fb791f424063c26acd3f35ebc891964fd7fa62ea9ff0ab37e2b4bb7e914a8aa515e06713525f61baa27c67e2841649073e12842627003244bfa7653f391f2be2b70ada85f7dcca8016dbb2b251a49a5d39510b39f2c119bf7b14fc4f1c2de2943213fb8f31c11691b24ca74c00ba90d1824692bae4e1cc4739caaf94bbef96d93e2fe0be3575a171c7e6843f5cb6a88992f37baa1a60567703a82f95aa384f92cc4587584484d6a4a1cab182430ed0ea0ae64728e8b27a8687a5aee43774b2f933117cecadea34b1e605cedd4bcbefcfa4e704fd1e4301dc3b428f1aca5ea6de028fc13c394cb0764ba1be5106d7bc1975c1430eff5ad07745e4cdc02c8ed75b6aa7a77c7fbbba7dcb92169bacfb0705700b1cb5a6a7609af2de7c52ef4c4dca042227672c340430c74161bd75ad46a8ba421d8f24d5020f55649110bcf477a35a6f7cdf7682ec7b9d0b23d5e70666840f847435e2c4592b4522cd036330a1df452fef046fe83d911b06fe31b1eaf12755a62e965e17914e162b172573147fa8eef5fb90ed3a4447420f7f9244a77a32dc97b7beb008e7b6b2e6250175c774be147e157d936699927a14239e91b00000000000000000000000000000000000000000000";
        List<Object> typeListOfChange = EthUtil.parseInput(changeInput, HtgConstant.INPUT_CHANGE);
        System.out.println(JSONUtils.obj2PrettyJson(typeListOfChange));
    }

    @Test
    public void encoderWithdrawTest() {
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        BigInteger value = BigInteger.valueOf(10000000000000000L);
        Boolean isContractAsset = false;
        String erc20 = "0x0000000000000000000000000000000000000000";
        String hash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, value, isContractAsset, erc20, (byte) 2);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void encoderChangeTest() {
        String txKey = "c0e3d91ff1d8066a82b8b09f38b80cb047a2fde88b59f4e42dff5df402ea1982";
        String[] adds = new String[]{"0x196c4b2b6e947b57b366967a1822f3fb7d9be1a8"};
        String[] removes = new String[]{"0x10c17be7b6d3e1f424111c8bddf221c9557728b0"};
        int count = 1;
        String hash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, (byte) 2);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void changeEventTest() throws Exception {
        String data = "0x000000000000000000000000742e9290053f63f38270b64b1a8daf52c91e6a510000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000271000000000000000000000000080b47d949b4bbd09bb48300c81e2c30df243310c00000000000000000000000000000000000000000000000000000000000000264e4552564565706236426b3765474b776e33546e6e373534633244674b65784b4c5467325a720000000000000000000000000000000000000000000000000000";
        List<Object> eventResult = EthUtil.parseEvent(data, HtgConstant.EVENT_CROSS_OUT_FUNDS);
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
    public void ethDepositByCrossOutTest() throws Exception {
        String directTxHash = "0x176856463c4bf086e5f6df8c600867f5e3d39f6a293bcac188f4bcc61e019b3d";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        HtgParseTxHelper helper = new HtgParseTxHelper();
        BeanUtilTest.setBean(helper, "htgWalletApi", htgWalletApi);
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
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
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        boolean crossOut = helper.validationEthDepositByCrossOut(tx, po);
        System.out.println(crossOut);
        System.out.println(po.toString());
    }

    @Test
    public void calGasPriceTest() {
        BigDecimal nvtUsd = new BigDecimal("0.15");
        BigDecimal nvtAmount = new BigDecimal(10_00000000L);
        System.out.println("nvtAmount: " + nvtAmount.movePointLeft(8).toPlainString());
        BigDecimal ethUsd = new BigDecimal("380.16");
        int assetId = 2;
        BigDecimal price = HtgUtil.calcGasPriceOfWithdraw(AssetName.NVT, nvtUsd, nvtAmount, ethUsd, assetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        System.out.println("price: " + price.movePointLeft(9).toPlainString());

        BigDecimal needPrice = new BigDecimal("31.789081289").movePointRight(9);
        BigDecimal nvtAmountCalc = HtgUtil.calcOtherMainAssetOfWithdraw(AssetName.NVT, nvtUsd, needPrice, ethUsd, assetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        System.out.println("newNvtAmount: " + nvtAmountCalc.movePointLeft(8).toPlainString());

        BigDecimal newPrice = HtgUtil.calcGasPriceOfWithdraw(AssetName.NVT, nvtUsd, nvtAmountCalc, ethUsd, assetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        System.out.println("newPrice: " + newPrice.movePointLeft(9).toPlainString());
    }

    @Test
    public void getBlockHeaderByHeight() throws Exception {
        Long height = Long.valueOf(70437);
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockByHeight() throws Exception {
        setMain();
        // 13950563 13950568
        Long height = Long.valueOf(13950568);
        EthBlock.Block block = htgWalletApi.getBlockByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockHeight() throws Exception {
        setMain();
        System.out.println(htgWalletApi.getBlockHeight());
    }

    @Test
    public void getBlockChainId() throws Exception {
        //setMain();
        System.out.println(htgWalletApi.getWeb3j().ethChainId().send().getChainId());
    }

    @Test
    public void getTestNetTxReceipt() throws Exception {
        // 直接调用erc20合约
        String directTxHash = "0x466dd4be78d49664d24dce9564a0ff58758e31280d0ff897d8a65bd2cc7f80e2";
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt);
    }

    @Test
    public void getCurrentGasPrice() throws IOException {
        //setMain();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("org.web3j.protocol.http.HttpService");
        logger.setLevel(Level.INFO);

        BigInteger gasPrice = htgWalletApi.getWeb3j().ethGasPrice().send().getGasPrice();
        System.out.println(gasPrice);
        System.out.println(new BigDecimal(gasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
        System.out.println();
    }

    @Test
    public void crossOutEncoderTest() {
        Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", new BigDecimal("0.1").scaleByPowerOfTen(18).toBigInteger(), "0x0000000000000000000000000000000000000000");
        System.out.println(FunctionEncoder.encode(crossOutFunction));
    }

    @Test
    public void crossOutEstimateGasTest() throws Exception {
        setMain();
        String contractAddress = "0x6758d4c4734ac7811358395a8e0c3832ba6ac624";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal("0.01"));
        Function function = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", convertAmount, EthConstant.ZERO_ADDRESS);

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xaff68CD458539A16b932748Cf4BdD53bf196789F",//364151
                null,
                null,
                null,
                contractAddress,
                convertAmount,
                encodedFunction
        );
        System.out.println(String.format("encodedFunction: %s", encodedFunction));
        EthEstimateGas estimateGas = htgWalletApi.getWeb3j().ethEstimateGas(tx).send();
        if(estimateGas.getResult() != null) {
            System.out.println(String.format("gasLimit: %s, 详情: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void withdrawEstimateGasTest() throws Exception {
        setMain();
        Function withdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction(
                "7511c6447e32f817bafb717352f7d92719959097ad2e9e086ab13c1a87c03b68",
                "0xaf308156c2A172747592111dD4c01FD1c739D9aB",
                BigInteger.valueOf(96950000000000000L),
                false,
                "0x0000000000000000000000000000000000000000",
                "0x2ad1faf5c32cb2703d7e4eb846168a7903bd4d928277e9f751bd076f341e11ee0c62bb3f7890097ba14cc87ea3a43a6f812a9609e6e829e81934cd87161b21491c38dbbb1490b6cfc07183c804026cdb053767156651c9f0907ba531b70bed0ce3420e290b6dd64df233f08b0a85c5d45f14b1076adf281133619a44a3bd30d3241c23835455deca849849a172050d8c128b50447a97219058eca6564e3d2d978e476d4bb249134790c7fbf95f4f4d46f044279a77a4ab98f930fc8581270ad6fe341cd3d136954aa84927973003bee9ec913f382259e822562bcbced5ebdab3ac66d76496482262933f01fc813c40fdfde0c094ef16382e7bf086ecf76eb5c78bde621bae38831bbbf5a765b4ec84476f5f5f312e891c27dbcd5e485f188739a46e909716f224beeeb6484e294efe654e4b0abee1f6a338bfc4803ee0dad2c28fcada721b251f0ee236cd461c059143eb09727039f82c32b28459233eb07746587f106db3662a1e4786a2ca9d9853114792d50e522c982376491519a046ce1ace001183641c134cafdc1a4f67ca9e0da2fc41d3cd737e77611b7e45a4652ad2091d8d2c508e229b00b6bf0ed329f8ba72025db51edfc3e21ef84aedd79fbfc6a7aa4ae998cd1b7ba9c81d62b57398ca4ac9439490b2a209f22da7aff9ae0c41645c09ae6b4fde7ed79623a229650567cd760bfba6a0b67caff6253eea8d47d66c3588f27750b61b56b63d21306d082afe12f064d60fbd11ba5636fe664231d0bacebaadac8c692e61ad28a99dc1f4a94cc8b51136dbbb0a761adffc222cf217ecf14ff497906d381beb3f8382fb2fae47a3f60302a9428ea377ddbfb245b85146776ef5ddd759e47007f55968ad072d8fea5a432ddc3d430ffd22f15798272404c23368395e36da9d1b7a7bd38f38a2cd57ec20d51999af8410d9a9bedc0b83b9a06eb134134b43b8b8661c74ff15828e9cc690a78bb33f917b63bd5d1b353cdb0ca02cf1258671518a1b1d842a98c7be596145f8d03b7783daff88ff03908b13300b6bbb4dcbeb871a5d5d030c26cd3dbd646b3bf644d985719029f2b8b259b8b7b3c5efd7511b7587db1b103b542f489a695ed45c2ce18db795299a8680151369264fdad6c2af49e3315351fcdea17ea6e56bcfe0547fc49f67f5da00ce7369d1b9efc560d0d2166cd9ca1b1e5d905f9dcf6d0d3284c9391347cf18f5a8672815bf103e975a2bd74d1c23a8394ace1926ace1ac82b24bf766f9aaac55ff9c92af11d068f700168ba7b332181b83dcc3a4686f023fe5e370142861fb952298467667909e125e148745e9795aa23cd066d5cee90f5e46a6aae874a5e0dcf3a39d168b825ecd5e68bc3c8a2bfdf71b"
        );

        String encodedFunction = FunctionEncoder.encode(withdrawFunction);

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xaff68cd458539a16b932748cf4bdd53bf196789f",//364151
                null,
                null,
                null,
                "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624",
                BigInteger.ZERO,
                encodedFunction
        );
        System.out.println(String.format("encodedFunction: %s", encodedFunction));
        EthEstimateGas estimateGas = htgWalletApi.getWeb3j().ethEstimateGas(tx).send();
        if(estimateGas.getResult() != null) {
            System.out.println(String.format("gasLimit: %s, 详情: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void validate() throws Exception {
        setMain();
        // 1096853502d76fad290b411231312cbc3d2323b6896a0d750c427556b1565440
        BigInteger value = BigInteger.ZERO;
        Function withdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction("1096853502d76fad290b411231312cbc3d2323b6896a0d750c427556b1565440", "0x42129b75a285863d9850feefd11af4a00ebecef8",
                BigInteger.valueOf(33000000), true, "0xb058887cb5990509a3d0dd2833b2054e4a7e4a55", "803e83a17be1c86cef63dc992cd56d7a0d0eaf3439224c874810b37467d559f1017ebc3df3008e2a1a62487c5723e73d071e59eeb6ab954cf1f0e26fe0ee4a281b87ddecf03386f4bf2985f65d92ebcff6e074db13c6944313ab909d84efd9d99f4f7f843689cdd2d749918e643abe0361bf61687ceefd9caf4c8e6abb45670a001cdffc85303cafce1e3880f9a57251bf750870d8c2a131b0da1706280f42033165373a3d735f2605a9866eec3f3de2d9a42f32cd0a2a63f2c0ead513158b4419991c");
        Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", (value = BigInteger.valueOf(33000000)),  EthConstant.ZERO_ADDRESS);
        String fromAddress = "0x3c2ff003fF996836d39601cA22394A58ca9c473b";
        String multyAddress = "0x7D759A3330ceC9B766Aa4c889715535eeD3c0484";
        EthCall ethCall = htgWalletApi.validateContractCall(fromAddress, multyAddress, crossOutFunction, value);
        System.out.println(JSONUtils.obj2PrettyJson(ethCall));
    }

    @Test
    public void crossOutValidateMain() throws Exception {
        setMain();
        BigInteger value = BigInteger.ZERO;
        Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", (value = BigInteger.valueOf(33000000)),  EthConstant.ZERO_ADDRESS);
        String fromAddress = "0xaff68cd458539a16b932748cf4bdd53bf196789f";
        String multyAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        EthCall ethCall = htgWalletApi.validateContractCall(fromAddress, multyAddress, crossOutFunction, value);
        System.out.println(JSONUtils.obj2PrettyJson(ethCall));
    }



    @Test
    public void allowanceTest() throws Exception {
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address("0xc11D9943805e56b630A401D4bd9A29550353EFa1"), new Address("0x7d759a3330cec9b766aa4c889715535eed3c0484")),
                Arrays.asList(new TypeReference<Uint256>() {}));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction("0x5cCEffCFd3E2fE4AaCBF57123B6d42DDDc231990", allowanceFunction).get(0).getValue();
        System.out.println(allowanceAmount);
    }
    /*public static void main(String[] args) throws Exception {
        HtgWalletApi htgWalletApi = new HtgWalletApi();
        htgWalletApi.init("https://mainnet.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5");
        HtgParseTxHelper helper = new HtgParseTxHelper();
        helper.htgWalletApi = htgWalletApi;
        String directTxHash = "0x7c74f2c95808bbf92533198ebcf75552f18c59c339c1d8c3637e9ae94f748062";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
        boolean out = helper.newValidationEthDepositByCrossOut(tx, txReceipt, po);
        System.out.println(out);

        BeanMap beanMap = new BeanMap();
        beanMap.add(HtgBlockAnalysisHelper.class);
        beanMap.add(HtgContext.class, (bnbContext = new BnbContext()));

        BeanUtilTest.setBean(htgWalletApi, "htgContext", new EthIIContext());
        EthIIContext context = new EthIIContext();
        HeterogeneousCfg cfg = new HeterogeneousCfg();
        cfg.setChainIdOnHtgNetwork(3);
        EthContext.setConfig(cfg);
        BeanUtilTest.setBean(htgWalletApi, "htgContext", context);

    }*/
    @Test
    public void newValidationEthDepositByCrossOutTest() throws Exception {
        setMain();
        List<String> list = new ArrayList<>();
        //list.add("0x7c74f2c95808bbf92533198ebcf75552f18c59c339c1d8c3637e9ae94f748062");
        //list.add("0xc2c5cd34d49813cc0c5438a272fa3902e8eee20bf992d8562035703c2467ec7f");
        //list.add("0x5530db02a1e2768621fff41c2e5827092b9b5b10538f7a99b1d188b657f06447");
        //list.add("0x8fc500075949643f78eb2ba9ed605792f551c325162af4db65577586421e9dc9");
        //list.add("0x8310276c339c816107406cda3ad3e3a1862a270651eafb7cb995c03b18bc83a5");
        //list.add("0x06ced804faea7b64ad04751f6b19eddb650f88ede385993117f25dcce07c4abe");
        //list.add("0x8b2695b5802545a3a6505f92b3f389cb4e22d8af20f38047e7f6f4d810759a2b");
        //list.add("0xaf8997a68242369fa6f0bc57a29150d3ffda9934e2858ca52deaef5981ef6cd5");
        //list.add("0x3966201a9e553ba8cb8fc24023541aff1b7cfb544a01857614fdb79097d7273b");
        //list.add("0x0aaf4b69440eb9e8ea296497c96a81ef84a7389d5c1f646d9d2b9b0809e438a2");
        //list.add("0xebb5a7887125c68d205e7ab6d81afb5f63e74bd2935b6219f586a26aadb2508f");
        //list.add("0xe86cce5d0c36a38e6d70b719228cd00033921c75472eba2f796748b0e1e47a77");
        list.add("0xf284372659597dc42cb2b86a7b55bdb08b110b79fa93884af00cd8f47fbf378a");
        list.add("0xdfb7d251794395dfc536fbbaec9fd1457c787138043b5dac741ce05b482e34ef");
        list.add("0x8e226808f4f65b49bf814c508de98671a20008f314409211ed3fe502a1cbcb00");
        list.add("0x182e45e30df234734031e009dd7accd297c110510cd6f9a576dc6dca86be6d32");
        list.add("0x6f504331ff4ea25d273bc146db13a211c128589053ba5299bf47009cf601a8ab");
        list.add("0x9ff1c35e620efda7e1ab9a7b6d56b4ed36423898ff23f13bc20630c27168b838");
        list.add("0xd6b37946fce975a78c74949461a1c58abd9c594eec1eef6270723e420fcfb36b");
        list.add("0x7027e81b7a252b47ee5e253c5f6832733ec45cfdf27a39c5df1fe53ed6f0cd10");
        list.add("0x038d07919bb77e3ed33d74979698ef797b777e4ea43b69601fa331065cbe779a");
        list.add("0xf1705cc51e0da7d5574a17d8707a72165a9472cacb0e1922c82f62749b15ef08");
        list.add("0xbf133b5dfbbfa6889ff40e4040c870dd7d9cabef75f0c8f3b3350ca0c72c225a");
        list.add("0x67975e35a46c965f2f2492052677d2ba0e9b20b2e04679168181f84ad620b7e9");
        list.add("0xdff3b51747a1a81aa5d2751cd230f74874a20e7f60604952e3247196fbbe4738");
        list.add("0xc8f2ffe4972778e74ab92c3e022011c136ccaa6f3545d0731ca89291cacb5ed3");
        list.add("0x2f0ef2a48fc40f5cfc3175df819177c1ee41c0822e16f8b1cd98dcfa088caeec");
        list.add("0xff6aa9083658f3b41097fd0963191bd48e99523d19d7ba5ef18658c3f4a08910");
        list.add("0xd1f744dcbc56170504df1436b4d8132e4fb6817ebae8b524369db4d90c9da1a7");
        list.add("0xc046907e69f89c8d51cd8395b34a523a2163731a381c3e288a2d47d11c090966");
        list.add("0x4804816782a8540eb7cb78e316b347ac9b8dc44e1aac866ccb2375349103830d");
        list.add("0x1247285e97cae152a7aca3aeacf12d7de709c69054ae288c27e72e06bd8a2207");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", new EthIIContext());
            Method method = helper.getClass().getDeclaredMethod("newValidationEthDepositByCrossOut", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionInfo.class);
            method.setAccessible(true);
            Object invoke = method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke);

            method = helper.getClass().getDeclaredMethod("_validationEthDepositByCrossOut", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionInfo.class);
            method.setAccessible(true);
            invoke = method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke);
        }
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }
}