package network.nerve.converter.heterogeneouschain.ethII.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.ethII.base.BaseII;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
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
        EthContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // 初始化 账户
        setAccount_EFa1();
        // ERC20 转账数量
        String sendAmount = "130";
        // 初始化 ERC20 地址信息
        setErc20USDI();
        //setErc20USDX();
        //setErc20NVT();
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
        //setUpgradeMain();
        setLocalTest();
        //setBeta();
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
        //setMain();
        long gasPriceGwei = 96L;
        EthContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        this.multySignContractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";

        String fromAddress = "";
        String priKey = "";

        String txKey = "c0e3d91ff1d8066a82b8b09f38b80cb047a2fde88b59f4e42dff5df402ea1982";
        String[] adds = {"0x196c4b2b6e947b57b366967a1822f3fb7d9be1a8"};
        String[] removes = {"0x10c17be7b6d3e1f424111c8bddf221c9557728b0"};
        int count = 1;
        String signData = "0c074974757d9c57fba3e4a82a4c85493abb5107023fcf61ef585d94d5e7b0b75ed9f76a251ba3db8b44028298b5db478053a518ca51ea533b98633e6e5209831ca3367fd298de4967c1e014a9277010ef571ebc2bbc7c8129c377e89efcb2794a0be9c6d63a83f2ae2d5afd9c311bd23785afb9e8cc9fc0f80e57f625b8ace8241c9a6007c4f5d4b5d89478f6afbca0a15496852cc8764b876c7ebcd15d030794ba7dde3063aeb48eec192d53a1705cd5b794004b57437172c76b468e1c7d61dbf81c85963bb8ece815dc5a36bc1c42d463104ea441bdac3c7aca178fb791f424063c26acd3f35ebc891964fd7fa62ea9ff0ab37e2b4bb7e914a8aa515e06713525f61baa27c67e2841649073e12842627003244bfa7653f391f2be2b70ada85f7dcca8016dbb2b251a49a5d39510b39f2c119bf7b14fc4f1c2de2943213fb8f31c11691b24ca74c00ba90d1824692bae4e1cc4739caaf94bbef96d93e2fe0be3575a171c7e6843f5cb6a88992f37baa1a60567703a82f95aa384f92cc4587584484d6a4a1cab182430ed0ea0ae64728e8b27a8687a5aee43774b2f933117cecadea34b1e605cedd4bcbefcfa4e704fd1e4301dc3b428f1aca5ea6de028fc13c394cb0764ba1be5106d7bc1975c1430eff5ad07745e4cdc02c8ed75b6aa7a77c7fbbba7dcb92169bacfb0705700b1cb5a6a7609af2de7c52ef4c4dca042227672c340430c74161bd75ad46a8ba421d8f24d5020f55649110bcf477a35a6f7cdf7682ec7b9d0b23d5e70666840f847435e2c4592b4522cd036330a1df452fef046fe83d911b06fe31b1eaf12755a62e965e17914e162b172573147fa8eef5fb90ed3a4447420f7f9244a77a32dc97b7beb008e7b6b2e6250175c774be147e157d936699927a14239e91b";
        List<Address> addList = Arrays.asList(adds).stream().map(a -> new Address(a)).collect(Collectors.toList());
        List<Address> removeList = Arrays.asList(removes).stream().map(r -> new Address(r)).collect(Collectors.toList());
        Function function = HtgUtil.getCreateOrSignManagerChangeFunction(txKey, addList, removeList, count, signData);
        System.out.println(FunctionEncoder.encode(function));
        //String hash = this.sendTx(fromAddress, priKey, function, HeterogeneousChainTxType.CHANGE);
        //System.out.println(String.format("hash: %s", hash));
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

    protected void setUpgradeMain() {
        setMain();
        list = new ArrayList<>();
        // 把CC的私钥放在首位
        list.add("");// 0xd6946039519bccc0b302f89493bec60f4f0b4610
        list.add("");// 0xd87f2ad3ef011817319fd25454fc186ca71b3b56
        list.add("");// 0x0eb9e4427a0af1fa457230bef3481d028488363e
        list.add("");// ???
        list.add("");// ???
        this.multySignContractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        init();
    }

    /**
     * 顶替一个管理员，10个签名
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setUpgradeMain();
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
     * erc20提现，10个签名
     */
    @Test
    public void erc20WithdrawBy10Managers() throws Exception {
        String txKey = "fff0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "20";
        String erc20 = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        int tokenDecimals = 6;
        int signCount = 10;
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
        String input = "0xab6c2b1000000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000e0d7a4fb97eb822a62a4be1824a4c83c5e50828d000000000000000000000000000000000000000000000000000001f2b9262c0000000000000000000000000000000000000000000000000000000000000000010000000000000000000000007b6f71c8b123b38aa8099e0098bec7fbc35b8a130000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000004031373366333765386539363133633165356631373663646662323764313965613632633262653031306437316637666434373239303061373938323932613338000000000000000000000000000000000000000000000000000000000000028ae2b83d3d2b29f71cdee3981b8d477818fb913d7de720ef3444289f50ea46fdb93c1e5b19f7ffda43698b506536b5c82256a2eb608352ff7564d697fcbd63c9121c8bf080476d7f8fb2637b842bee9673c210746272f2420a15c7b7201001fb3fc972192c4347d1478bd2fffa82458d77abdc2a9b3a647a3aafd3508811699ad9391bbc46b8c87279b55bfbfc7484136118da80fa01f98a219f7fffaf3b3e6a76907a706c76227604d977951c35b82bd6934892b70a673ce73adefcaff9cd8bf73c701b6d315aa70ac70face17e1281620f080d53550310f0c1883487b1689c3a65328716a28db94930adc42079f03c0abd07094678a3ae90e27b2ae08855a9ad8a27471bf28e78fb94c494464a3c0b499cd24dd4c84e443b6bb4b000725c3367179f51f86a5fa7e5077b76a33c103bec8a49adb7b61a73c7569188cddc0be91eeb9fe9831c64c28e6bfb35b711a404a3446216180378a2b495561ae99254119610a08dcf174ffd46ff049c82756bb9838996630a6d1790a4597bddc8a9e174ad4085a078091bb0cabb1dbb7646544c88d94bba2ee9f77dfe097ab68da861112e7a631567b387416d0cf1d511ac3c6afb9d27774d2c2f38948fb2cf4ac8c8cdba1d96b84f52331b7cdb95c6bc5fb08f87873bd5ed13ca56b32157fd5d7c66f34e4d07874bb53d8d066f992ca968a585b6f5be0b8b4429756441aadd4c8e228befe3744080a7e32d1cff912e61076031282662e633050e8f79e10d027492f7a41183993eac7fb82682009a88fdfb293cd2332275e1554af22bbb9dabd39ff38a62dd3c7a58e6c7ee021c44fbcc9b7b0420d3ae9b442fd56ca9f55ba7e1b2bd357e2617465ad1a1a4e5314c472c437ded0fad927814cbb81765fb9e0fd3cc5a58b5b8180a3cdf4b21d31c1c00000000000000000000000000000000000000000000";
        List<Object> typeList = EthUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));
        String signs = HexUtil.encode((byte[]) typeList.get(5));
        System.out.println(signs);
        System.out.println(String.format("签名个数: %s", signs.length() / 130));


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
        String hash = this.encoderWithdraw(txKey, toAddress, value, isContractAsset, erc20, (byte) 2);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void encoderChangeTest() {
        String txKey = "c0e3d91ff1d8066a82b8b09f38b80cb047a2fde88b59f4e42dff5df402ea1982";
        String[] adds = new String[]{"0x196c4b2b6e947b57b366967a1822f3fb7d9be1a8"};
        String[] removes = new String[]{"0x10c17be7b6d3e1f424111c8bddf221c9557728b0"};
        int count = 1;
        String hash = this.encoderChange(txKey, adds, count, removes, (byte) 2);
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
        BigDecimal price = HtgUtil.calGasPriceOfWithdraw(nvtUsd, nvtAmount, ethUsd, assetId);
        System.out.println("price: " + price.movePointLeft(9).toPlainString());

        BigDecimal needPrice = new BigDecimal("31.789081289").movePointRight(9);
        BigDecimal nvtAmountCalc = HtgUtil.calNVTOfWithdraw(nvtUsd, needPrice, ethUsd, assetId);
        System.out.println("newNvtAmount: " + nvtAmountCalc.movePointLeft(8).toPlainString());

        BigDecimal newPrice = HtgUtil.calGasPriceOfWithdraw(nvtUsd, nvtAmountCalc, ethUsd, assetId);
        System.out.println("newPrice: " + newPrice.movePointLeft(9).toPlainString());
    }

    @Test
    public void getBlockHeaderByHeight() throws Exception {
        Long height = Long.valueOf(70437);
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockHeight() throws Exception {
        setMain();
        System.out.println(htgWalletApi.getBlockHeight());
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
        String contractAddress = "0x7d759a3330cec9b766aa4c889715535eed3c0484";
        //BigInteger convertAmount = htgWalletApi.convertEthToWei(new BigDecimal("0.01"));
        //Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", convertAmount, EthConstant.ZERO_ADDRESS);
        BigInteger convertAmount = new BigInteger("2" + "000000000000000000");
        Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", convertAmount, "0x5cCEffCFd3E2fE4AaCBF57123B6d42DDDc231990");
        //Function crossOutFunction = HtgUtil.getCrossOutFunction("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", convertAmount, "0x0000000000000000000000000000000000000000");

        String encodedFunction = FunctionEncoder.encode(crossOutFunction);

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xc11D9943805e56b630A401D4bd9A29550353EFa1",//364151
                null,
                BigInteger.ONE,
                BigInteger.valueOf(1000000L),
                contractAddress,
                null,
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
    public void allowanceTest() throws Exception {
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address("0xc11D9943805e56b630A401D4bd9A29550353EFa1"), new Address("0x7d759a3330cec9b766aa4c889715535eed3c0484")),
                Arrays.asList(new TypeReference<Uint256>() {}));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction("0x5cCEffCFd3E2fE4AaCBF57123B6d42DDDc231990", allowanceFunction).get(0).getValue();
        System.out.println(allowanceAmount);
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }
}