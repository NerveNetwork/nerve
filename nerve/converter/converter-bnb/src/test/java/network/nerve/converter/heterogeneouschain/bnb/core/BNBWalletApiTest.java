package network.nerve.converter.heterogeneouschain.bnb.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.base.Base;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class BnbWalletApiTest extends Base {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;

    static String USDX_BNB_MAIN_TEST = "0x7dce26DFad3bb82B0605073480352B6FECEa169a";
    static String USDX_BNB = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String BUG_BNB_18 = "0x90C89b9f9c4605a887540FaD286E02B71f03D70d";

    static String DXA_BNB_8 = "0x3139dbe1bf7feb917cf8e978b72b6ead764b0e6c";
    static String GOAT_BNB_9 = "0xba0147e9c99b0467efe7a9c51a2db140f1881db5";
    static String SAFEMOON_BNB_9 = "0x7be69eb38443d3a632cb972df840013d667365e6";
    static String OKUSD_OKT_8 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String HUSD_HT_18 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String BUSD_BNB_18 = "0x02e1aFEeF2a25eAbD0362C4Ba2DC6d20cA638151";

    static String NVT_BNB_MINTER = "0x3F1f3D17619E916C4F04707BA57d8E0b9e994fB0";
    static String NULS_BNB_MINTER = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
    static String DXA_BNB_MINTER = "0x3139dBe1Bf7FEb917CF8e978b72b6eAd764b0e6C";
    static String USDI_BNB_MINTER = "0xF3B4771813f27C390B11703450F5E188b83829F9";
    static String ETH_BNB_MINTER = "0x9296D0AF7DA81AAD9ae273118Ba377403db6691a";

    protected void setErc20USDI() {
        erc20Address = "0xxxx";
        erc20Decimals = 6;
    }

    protected void setErc20DXA() {
        erc20Address = DXA_BNB_8;
        erc20Decimals = 8;
    }
    protected void setErc20GOAT() {
        erc20Address = GOAT_BNB_9;
        erc20Decimals = 9;
    }
    protected void setErc20SAFEMOON() {
        erc20Address = SAFEMOON_BNB_9;
        erc20Decimals = 9;
    }

    protected void setErc20BUSD() {
        erc20Address = BUSD_BNB_18;
        erc20Decimals = 18;
    }

    protected void setErc20USDX() {
        erc20Address = USDX_BNB;
        erc20Decimals = 6;
    }

    protected void setErc20BUG() {
        erc20Address = BUG_BNB_18;
        erc20Decimals = 18;
    }

    protected void setErc20USDXMainTest() {
        erc20Address = USDX_BNB_MAIN_TEST;
        erc20Decimals = 6;
    }

    protected void setErc20NVT() {
        erc20Address = NVT_BNB_MINTER;
        erc20Decimals = 8;
    }

    protected void setErc20NULS() {
        erc20Address = NULS_BNB_MINTER;
        erc20Decimals = 8;
    }

    protected void setErc20UsdiMinter() {
        erc20Address = USDI_BNB_MINTER;
        erc20Decimals = 6;
    }

    protected void setErc20EthMinter() {
        erc20Address = ETH_BNB_MINTER;
        erc20Decimals = 18;
    }

    protected void setAccount_c684() {
        from = "0xfa27c84eC062b2fF89EB297C24aaEd366079c684";
        fromPriKey = "b36097415f57fe0ac1665858e3d007ba066a7c022ec712928d2372b27e8513ff";
    }

    protected void setAccount_024F() {
        from = "0xd29E172537A3FB133f790EBE57aCe8221CB8024F";
        fromPriKey = "0935e3d8c87c2ea5c90e3e3a0509d06eb8496655db63745fae4ff01eb2467e85";
    }

    protected void setAccount_EFa1() {
        from = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        fromPriKey = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    }

    protected void setAccount_2501() {
        from = "0x09534d4692F568BC6e9bef3b4D84d48f19E52501";
        fromPriKey = "59f770e9c44075de07f67ba7a4947c65b7c3a0046b455997d1e0f854477222c8";
    }

    protected void setAccount_7B65() {
        from = "0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65";
        fromPriKey = "43da7c269917207a3cbb564b692cd57e9c72f9fcfdb17ef2190dd15546c4ed9d";
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
        this.multySignContractAddress = "0xdd35003eD2118D997F3404C9C17eb20dfea0f767";
        init();
    }

    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        list.add("7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b");// 0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        // 7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b::::::::::0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        this.multySignContractAddress = "0xf7915d4de86b856F3e51b894134816680bf09EEE";
        init();
    }

    public void init() {
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
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
                    List.of(new TypeReference<Uint256>() {
                    }));
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
    public void transferBNBAndERC20() throws Exception {
        BigInteger gasPrice = htgContext.getEthGasPrice();
        // 初始化 账户
        setAccount_EFa1();
        // BNB数量
        String sendAmount = "0.1";
        String txHash = htgWalletApi.sendMainAsset(from, fromPriKey, multySignContractAddress, new BigDecimal(sendAmount), BigInteger.valueOf(81000L), gasPrice);
        System.out.println(String.format("向[%s]转账%s个BNB, 交易hash: %s", multySignContractAddress, sendAmount, txHash));
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
    public void depositBNBByCrossOut() throws Exception {
        setLocalTest();
        this.multySignContractAddress = "0xc9Ad179aDbF72F2DcB157D11043D5511D349a44b";
        // 初始化 账户
        setAccount_EFa1();
        // BNB数量
        String sendAmount = "0.11";
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal(sendAmount));
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, HtgConstant.ZERO_ADDRESS);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount, multySignContractAddress);
        System.out.println(String.format("bnb充值[%s], hash: %s", sendAmount, hash));
    }

    /**
     * 新方式充值erc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        this.multySignContractAddress = "0xc9Ad179aDbF72F2DcB157D11043D5511D349a44b";
        // 初始化 账户
        //setAccount_2501();
        setAccount_024F();
        //setAccount_7B65();
        //setAccount_EFa1();
        // ERC20 转账数量
        String sendAmount = "2000";
        // 初始化 ERC20 地址信息
        setErc20BUG();
        //setErc20EthMinter();
        //setErc20UsdiMinter();

        //setErc20BUG();
        //setErc20USDX();
        //setErc20DXA();
        //setErc20GOAT();
        //setErc20SAFEMOON();
        setErc20BUSD();


        //setErc20NVT();
        //setErc20NULS();
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20授权
            String approveAmount = "99999999";
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("erc20授权充值[%s], 授权hash: %s", approveAmount, authHash));
            while (htgWalletApi.getTxReceipt(authHash) == null) {
                System.out.println("等待3秒查询[ERC20授权]交易打包结果");
                TimeUnit.SECONDS.sleep(3);
            }
            TimeUnit.SECONDS.sleep(3);
            BigInteger tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("等待3秒查询[ERC20授权]交易额度");
                TimeUnit.SECONDS.sleep(3);
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
    public void depositPoolTest() throws Exception {
        setLocalTest();
        setAccount_EFa1();
        setErc20BUSD();
        BigInteger gasLimit = BigInteger.valueOf(250000L);
        String poolContract = "0x6912762E5F1281F54B4248fF78D6dbe50A6511c9";
        long poolId = 0;
        BigInteger value = new BigDecimal("1230").movePointRight(erc20Decimals).toBigInteger();
        Function depositFunction = new Function("deposit",
                Arrays.asList(new Uint256(poolId), new Uint256(value)),
                Arrays.asList(new TypeReference<Type>() {}));
        for (int i = 0; i < 30; i++) {
            try {
                HtgSendTransactionPo htSendTransactionPo = htgWalletApi.callContract(from, fromPriKey, poolContract, gasLimit, depositFunction, null, null, null);
                String ethTxHash = htSendTransactionPo.getTxHash();
                System.out.println(ethTxHash);
                // 休眠15个区块，发下一个交易
                TimeUnit.SECONDS.sleep(45);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void getBalance() throws Exception {
        setLocalTest();
        setAccount_EFa1();
        erc20BalancePrint("NVT", from, NVT_BNB_MINTER, 8);
        erc20BalancePrint("BUSD", from, BUSD_BNB_18, 18);
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

    protected void setBnbMainTest() {
        // "0xd87f2ad3ef011817319fd25454fc186ca71b3b56"
        // "0x0eb9e4427a0af1fa457230bef3481d028488363e"
        // "0xd6946039519bccc0b302f89493bec60f4f0b4610"
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 公钥: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("");// 公钥: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("");// 公钥: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        this.multySignContractAddress = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        init();
    }

    protected void setMainData() {
        setMain();
        list = new ArrayList<>();
        // 把有BNB余额的私钥放在首位
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        this.multySignContractAddress = "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4";
        init();
    }

    /**
     * 5个签名
     */
    @Test
    public void signDataForERC20WithdrawTest() throws Exception {
        /*
         0xd87f2ad3ef011817319fd25454fc186ca71b3b56, 0x0eb9e4427a0af1fa457230bef3481d028488363e, 0xd6946039519bccc0b302f89493bec60f4f0b4610,
         0xb12a6716624431730c3ef55f80c458371954fa52, 0x1f13e90daa9548defae45cd80c135c183558db1f, 0x66fb6d6df71bbbf1c247769ba955390710da40a5,
         0x659ec06a7aedf09b3602e48d0c23cd3ed8623a88, 0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1, 0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d,
         0xaff68cd458539a16b932748cf4bdd53bf196789f, 0xc8dcc24b09eed90185dbb1a5277fd0a389855dae, 0xa28035bb5082f5c00fa4d3efc4cb2e0645167444,
         0x10c17be7b6d3e1f424111c8bddf221c9557728b0, 0x15cb37aa4d55d5a0090966bef534c89904841065, 0x17e61e0176ad8a88cac5f786ca0779de87b3043b address[]


         0xaff68cd458539a16b932748cf4bdd53bf196789f, 0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d, 0xa28035bb5082f5c00fa4d3efc4cb2e0645167444,
         0x1f13e90daa9548defae45cd80c135c183558db1f, 0xb12a6716624431730c3ef55f80c458371954fa52
         0xd87f2ad3ef011817319fd25454fc186ca71b3b56, 0x0eb9e4427a0af1fa457230bef3481d028488363e, 0xd6946039519bccc0b302f89493bec60f4f0b4610,
         0x17e61e0176ad8a88cac5f786ca0779de87b3043b, 0x659ec06a7aedf09b3602e48d0c23cd3ed8623a88
         */
        setMainData();
        String txKey = "ddd1024000000000000000000000000000000000000000000000000000000000";
        // 接收者地址
        String toAddress = "0x3250dABB584f7FEA1BAFAFf6000FFBBD2F419A15";
        // 造币数量
        String value = "53334857.539598";
        // FISU token合约
        String erc20 = "0x0d12197EeD5f4C867Af1528ABbE1837C5c40902e";
        int tokenDecimals = 18;
        int signCount = 5;
        String signData = this.signDataForERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20提现%s个，%s个签名，signData: %s", value, signCount, signData));
    }

    /**
     * 根据已有的签名数据 发送交易 - erc20提现
     */
    @Test
    public void sendERC20WithdrawBySignDataTest() throws Exception {
        setMainData();
        String txKey = "ddd1024000000000000000000000000000000000000000000000000000000000";
        // 接收者地址
        String toAddress = "0x3250dABB584f7FEA1BAFAFf6000FFBBD2F419A15";
        // 造币数量
        String value = "53334857.539598";
        // FISU token合约
        String erc20 = "0x0d12197EeD5f4C867Af1528ABbE1837C5c40902e";
        int tokenDecimals = 18;
        String signData = "";

        String hash = this.sendERC20WithdrawBySignData(txKey, toAddress, value, erc20, tokenDecimals, signData);
        System.out.println(String.format("ERC20提现%s个，hash: %s", value, hash));
    }

    /**
     * 添加 N 个管理员
     */
    @Test
    public void managerAdd() throws Exception {
        // 正式网环境数据
        setMainData();
        // GasPrice准备
        long gasPriceGwei = 20L;
        htgContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        String txKey = "aaa4000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{};
        String[] removes = new String[]{"0x018fc24ec7a4a69c83884d93b3b8f87b670c0ef5"};
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
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

    /**
     * 顶替一个管理员，10个签名
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setMainData();
        // GasPrice准备
        long gasPriceGwei = 20L;
        htgContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
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
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 15;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("BNB提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
        System.out.println(String.format("BNB提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
    public void errorBNBWithdrawTest() throws Exception {
        String txKey = "h500000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 11;
        list.addAll(5, list.subList(5, 10));
        //this.VERSION = 2;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("BNB提现%s个，%s个签名，hash: %s", value, signCount, hash));
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
    public void txInputErc20TransferDecoderTest() throws JsonProcessingException {
        String input = "0xa9059cbb00000000000000000000000021decdab7af693437e77936e081c2f4d4391094a0000000000000000000000000000000000000000000000000de0b6b3a7640000";
        List<Object> typeList = HtgUtil.parseInput(input, Utils.convert(
                List.of(
                        new TypeReference<Address>(){},
                        new TypeReference<Uint256>(){}
                )
        ));
        System.out.println(JSONUtils.obj2PrettyJson(typeList));

    }

    @Test
    public void txInputCrossOutDecoderTest() throws JsonProcessingException {
        String input = "0x0889d1f00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000bebc20000000000000000000000000025ebbac2ca9db0c1d6f0fc959bbc74985417bab00000000000000000000000000000000000000000000000000000000000000025544e565464545350526e586b446961677937656e7469314b4c37354e553541784339735141000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_CROSS_OUT);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));

    }

    @Test
    public void txInputCrossOutIIDecoderTest() throws JsonProcessingException {
        String input = "0x38615bb000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000001312d00000000000000000000000000b6d685346106b697e6b2bba09bc343cafc930ca300000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000025544e565464545350526e586b446961677937656e7469314b4c37354e5535417843397351410000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_CROSS_OUT_II);
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
        String hash = HtgUtil.encoderWithdraw(htgContext, txKey, toAddress, value, isContractAsset, erc20, (byte) 2);
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
    public void crossOutEventTest() throws Exception {
        String data = "0x000000000000000000000000742e9290053f63f38270b64b1a8daf52c91e6a510000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000271000000000000000000000000080b47d949b4bbd09bb48300c81e2c30df243310c00000000000000000000000000000000000000000000000000000000000000264e4552564565706236426b3765474b776e33546e6e373534633244674b65784b4c5467325a720000000000000000000000000000000000000000000000000000";
        List<Object> eventResult = HtgUtil.parseEvent(data, HtgConstant.EVENT_CROSS_OUT_FUNDS);
        System.out.println(JSONUtils.obj2PrettyJson(eventResult));
    }

    @Test
    public void crossOutIIEventTest() throws Exception {
        String txHash = "0xf79ab90e54716d2201246008321121e014047c0430194290028ec8974571a978";
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(txHash);
        org.web3j.protocol.core.methods.response.Log log = txReceipt.getLogs().get(1);
        String data = log.getData();
        List<Object> eventResult = HtgUtil.parseEvent(data, HtgConstant.EVENT_CROSS_OUT_II_FUNDS);
        System.out.println(JSONUtils.obj2PrettyJson(eventResult));
        System.out.println(Numeric.toHexString((byte[]) eventResult.get(5)));
        System.out.println(new String((byte[]) eventResult.get(5), "UTF8"));
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
        for (int j = 0; j < times; j++) {
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
    public void getBnbTransaction() throws Exception {
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
        BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
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
        BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
        boolean crossOut = helper.validationEthDepositByCrossOut(tx, po);
        System.out.println(crossOut);
        System.out.println(po.toString());
    }

    @Test
    public void calGasPriceTest() {
        BigDecimal nvtUsd = new BigDecimal("0.09");
        BigDecimal nvtAmount = new BigDecimal(8_00000000L);
        BigDecimal ethUsd = new BigDecimal("606.16");
        int assetId = 2;
        BigDecimal price = HtgUtil.calcGasPriceOfWithdraw(AssetName.NVT, nvtUsd, nvtAmount, ethUsd, assetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        System.out.println(price.movePointLeft(9).toPlainString());
    }

    @Test
    public void getBlockHeaderByHeight() throws Exception {
        Long height = Long.valueOf(70437);
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockHeight() throws Exception {
        //setMain();
        System.out.println(htgWalletApi.getBlockHeight());
    }

    public static void main(String[] args) {
        // 锁10天，每天240个区块
        long lockBlockPerDay = 4 * 60;
        calcLockDay(lockBlockPerDay, 262);
        calcLockDay(lockBlockPerDay, 282);
        calcLockDay(lockBlockPerDay, 382);
        calcLockDay(lockBlockPerDay, 392);
        calcLockDay(lockBlockPerDay, 412);
        calcLockDay(lockBlockPerDay, 456);
        calcLockDay(lockBlockPerDay, 556);
        calcLockDay(lockBlockPerDay, 656);
        calcLockDay(lockBlockPerDay, 756);
        calcLockDay(lockBlockPerDay, 856);
        calcLockDay(lockBlockPerDay, 999);
        calcLockDay(lockBlockPerDay, 1999);
    }

    private static void calcLockDay(long lockBlockPerDay, long currentBlock) {
        long realUnlockBlock = currentBlock + lockBlockPerDay * 10;
        long unlockBlock = realUnlockBlock / lockBlockPerDay * lockBlockPerDay;
        System.out.println(String.format("number: %s, realUnlockBlock: %s, unlockBlock: %s, currentBlock: %s",
                realUnlockBlock / lockBlockPerDay,
                realUnlockBlock,
                unlockBlock,
                currentBlock));
    }

    @Test
    public void getTestNetTxReceipt() throws Exception {
        // 直接调用erc20合约
        String directTxHash = "0xf79ab90e54716d2201246008321121e014047c0430194290028ec8974571a978";
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
    public void allContractManagerSet() throws Exception {
        //localdev();
        //localdevII();
        //setMain();
        setLocalTest();
        //mainnetII();
        System.out.println("查询当前合约管理员列表，请等待……");
        Set<String> all = this.allManagers(multySignContractAddress);
        System.out.println(String.format("size : %s", all.size()));
        for (String address : all) {
            BigDecimal balance = htgWalletApi.getBalance(address).movePointLeft(18);
            System.out.print(String.format("address %s : %s", address, balance.toPlainString()));
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
        EthCall ethCall = htgWalletApi.getWeb3j().ethCall(ethCallTransaction, DefaultBlockParameterName.PENDING).sendAsync().get();
        String value = ethCall.getResult();
        List<Type> typeList = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        List<String> results = new ArrayList();
        for (Type type : typeList) {
            results.add(type.getValue().toString());
        }
        String resultStr = results.get(0).substring(1, results.get(0).length() - 1);
        String[] resultArr = resultStr.split(",");
        Set<String> resultList = new HashSet<>();
        for (String result : resultArr) {
            resultList.add(result.trim().toLowerCase());
        }
        return resultList;
    }

    @Test
    public void crossOutEstimateGasTest() throws Exception {
        setMain();
        // 0x42981d0bfbaf196529376ee702f2a9eb9092fcb5
        String contractAddress = "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4";
        BigInteger convertAmount = new BigDecimal("30000").movePointRight(9).toBigInteger();
        Function function = HtgUtil.getCrossOutFunction("NERVEepb67fJhLqNrA5KGZXvKvjxMmJp7vJrLX", convertAmount, "0x42981d0bfbaf196529376ee702f2a9eb9092fcb5");
        String encodedFunction = FunctionEncoder.encode(function);
        //String encodedFunction = "0x95ca26fd000000000000000000000000d248e509c1aacbfe79ac161c3f531643ecbdc34600000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000000";

        BigInteger value = null;
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xa803fc1c1e83d6389865e1248dc924ed4c6953de",
                null,
                null,
                null,
                contractAddress,
                value,
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
        String txKey = "f79ab90e54716d2201246008321121e014047c0430194290028ec8974571a978";
        String toAddress = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        BigInteger value = new BigDecimal("30000").movePointRight(9).toBigInteger();
        String erc20 = "0x42981d0bfbaf196529376ee702f2a9eb9092fcb5";
        //Function function =  HtgUtil.getCreateOrSignWithdrawFunction(txKey, toAddress, value, true, erc20, signData);
        //// 估算GasLimit
        //EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        //BigInteger estimateGas = estimateGasObj.getAmountUsed();
    }

    @Test
    public void erc20TransferEstimateGasTest() throws Exception {
        // 0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4"
        setMain();
        String contractAddress = "0x42981d0bfbaf196529376ee702f2a9eb9092fcb5";
        BigInteger convertAmount = new BigDecimal("30000").movePointRight(9).toBigInteger();
        String to = "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4";

        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(convertAmount)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger value = null;
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xa803fc1c1e83d6389865e1248dc924ed4c6953de",
                null,
                null,
                null,
                contractAddress,
                value,
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
    public void symboltest() throws Exception {
        //setMain();
        String contractAddress = "0x75363e9a5e6fb87eb6197ef6b633961061636348";
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
    public void lpRewardTest() throws Exception {
        setMain();
        // 用户地址
        String userAddress = "";
        // 用户总投入的nuls数量
        String totalUserNuls = "123";
        // 用户总投入的usd数量
        String totalUserBusd = "234";
        // 当前nuls的价格(对USD)
        String _nulsPrice = "0.47";

        String farmsContract = "0x73feaa1eE314F8c655E354234017bE2193C9E24E";
        String lpContract = "0x853784b7bde87d858555715c0123374242db7943";
        long NULS_BUSD_ID = 319;
        List<Type> uiResult = htgWalletApi.callViewFunction(farmsContract, new Function(
                "userInfo",
                List.of(new Uint256(BigInteger.valueOf(NULS_BUSD_ID)),
                        new Address(userAddress)),
                List.of(new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {})));
        if (uiResult.isEmpty()) {
            return;
        }
        System.out.println();

        BigDecimal myLp = new BigDecimal(uiResult.get(0).getValue().toString()).movePointLeft(18);
        BigDecimal myNuls = new BigDecimal(totalUserNuls);
        BigDecimal myBusd = new BigDecimal(totalUserBusd);
        BigDecimal nulsPrice = new BigDecimal(_nulsPrice);
        System.out.println(String.format("当前NULS价格: %s USDT/NULS", nulsPrice.toPlainString()));


        List<Type> tsResult = htgWalletApi.callViewFunction(lpContract, new Function(
                "totalSupply",
                List.of(),
                List.of(new TypeReference<Uint256>() {
                })));
        if (tsResult.isEmpty()) {
            return;
        }
        BigDecimal totalSupply = new BigDecimal(tsResult.get(0).getValue().toString());
        System.out.println(String.format("LP总流通量: %s", totalSupply.movePointLeft(18).toPlainString()));
        totalSupply = totalSupply.movePointLeft(18);
        BigDecimal yz = myLp.divide(totalSupply, 18, RoundingMode.UP);
        System.out.println(String.format("yz: %s", yz.toPlainString()));

        List<Type> grResult = htgWalletApi.callViewFunction(lpContract, new Function(
                "getReserves",
                List.of(),
                List.of(new TypeReference<Uint112>() {
                }, new TypeReference<Uint112>() {
                }, new TypeReference<Uint32>() {
                })));
        System.out.println();
        BigInteger _nuls = (BigInteger) grResult.get(0).getValue();
        BigInteger _busd = (BigInteger) grResult.get(1).getValue();
        System.out.println(String.format("流动池的NULS: %s", new BigDecimal(_nuls).movePointLeft(8).toPlainString()));
        System.out.println(String.format("流动池的BUSD: %s", new BigDecimal(_busd).movePointLeft(18).toPlainString()));
        System.out.println();
        System.out.println(String.format("我提供了NULS: %s", myNuls.toPlainString()));
        System.out.println(String.format("我提供了BUSD: %s", myBusd.toPlainString()));
        System.out.println();

        BigDecimal nulsValue = new BigDecimal(new BigDecimal(_nuls).multiply(yz).toBigInteger()).movePointLeft(8);
        BigDecimal busdValue = new BigDecimal(new BigDecimal(_busd).multiply(yz).toBigInteger()).movePointLeft(18);
        System.out.println(String.format("我能换出的NULS: %s", nulsValue.toPlainString()));
        System.out.println(String.format("我能换出的BUSD: %s", busdValue.toPlainString()));
        System.out.println();

        BigDecimal dNuls = nulsValue.subtract(myNuls);
        System.out.println(String.format("NULS %s了: %s，折算成BUSD: %s", dNuls.compareTo(BigDecimal.ZERO) > 0 ? "多" : "少", dNuls.abs().toPlainString(), dNuls.multiply(nulsPrice).toPlainString()));
        BigDecimal dBusd = busdValue.subtract(myBusd);
        System.out.println(String.format("BUSD %s了: %s", dBusd.compareTo(BigDecimal.ZERO) > 0 ? "多" : "少", dBusd.abs().toPlainString()));
        System.out.println();

        BigDecimal finalShouYi = dBusd.add(dNuls.multiply(nulsPrice));
        System.out.println(String.format("无常损失or流动性收益: %s", finalShouYi.toPlainString()));
    }

    @Test
    public void newParseWithdrawTxReceiptTest() throws Exception {
        setBeta();
        htgContext.SET_MULTY_SIGN_ADDRESS(multySignContractAddress);
        List<String> list = new ArrayList<>();
        list.add("0x0e8fd08fe94371d7cf15db7cbd1695cb36b46724566fb9d3ab0527231398ffec");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", htgContext);
            Method method = helper.getClass().getDeclaredMethod("newParseWithdrawTxReceipt", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class);
            method.setAccessible(true);
            Object invoke = method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke);
            BigInteger newValue = po.getValue();
            System.out.println(newValue);
            newValue = newValue == null ? BigInteger.ZERO : newValue;

            Method method1 = helper.getClass().getDeclaredMethod("parseWithdrawTxReceipt", TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class);
            method1.setAccessible(true);
            Object invoke1 = method1.invoke(helper, txReceipt, po);
            System.out.println(invoke1);
            BigInteger oldValue = po.getValue();
            System.out.println(oldValue);
            oldValue = oldValue == null ? BigInteger.ZERO : oldValue;
            System.out.println(newValue.compareTo(oldValue) == 0);
            System.out.println();
        }
    }

    @Test
    public void newValidationEthDepositByCrossOutTest() throws Exception {
        setMain();
        List<String> list = new ArrayList<>();
        list.add("0xf33d7958967d36331eb20eafb23e49dcfcbddb5f925b6b23608cb5fd74a1433c");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", new BnbContext());
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

    @Test
    public void hasERC20WithListeningAddressTest() throws Exception {
        setMain();
        Predicate<String> predicate = toAddress -> "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5".equals(toAddress);
        List<String> list = new ArrayList<>();
        list.add("0xf33d7958967d36331eb20eafb23e49dcfcbddb5f925b6b23608cb5fd74a1433c");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgERC20Helper helper = new HtgERC20Helper();
            BeanUtilTest.setBean(helper, "htgContext", new BnbContext());
            Method method = helper.getClass().getDeclaredMethod("hasERC20WithListeningAddressNew", TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class, Predicate.class);
            method.setAccessible(true);
            Object invoke = method.invoke(helper, txReceipt, po, predicate);
            System.out.println(invoke);

            method = helper.getClass().getDeclaredMethod("hasERC20WithListeningAddressOld", TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class, Predicate.class);
            method.setAccessible(true);
            invoke = method.invoke(helper, txReceipt, po, predicate);
            System.out.println(invoke);
        }
    }

    @Test
    public void deployERC20MinterDecoderTest() throws JsonProcessingException {
        String input = "0xd98eea60000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000005546f6b656e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003544b4e0000000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, Utils.convert(
                List.of(
                        new TypeReference<Utf8String>(){},
                        new TypeReference<Utf8String>(){},
                        new TypeReference<Uint8>(){},
                        new TypeReference<Uint8>(){}
                )
        ));
        System.out.println(JSONUtils.obj2PrettyJson(typeList));
    }

    @Test
    public void encodeTest() throws JsonProcessingException {
        String from = "0x0cba7a8324c9b7355cef30b4429ddc5eb45e9a00";
        Function function = new Function("bidderClaim",
                Arrays.asList(new Address(from), new DynamicBytes(new byte[]{})),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        System.out.println(FunctionEncoder.encode(function));
    }

    @Test
    public void getTx() throws Exception {
        setMain();
        // 直接调用erc20合约
        String directTxHash = "0x03f27803ecccebcd647e4e28579004ad9ee01b01c43476c9beef467babff453a";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        System.out.println(JSONUtils.obj2PrettyJson(tx));
    }

    @Test
    public void dodoTx() throws Exception {
        // 正式网数据
        setMain();
        // GasPrice准备
        long gasPriceGwei = 5L;
        BigInteger gasPrice = BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9));
        // 钱包账户，加载凭证，用私钥
        Credentials credentials = Credentials.create("");
        // 合约地址
        String contractAddress = "0x23dcfd1edf572204c7aee9680a9d853ec2c993ae";
        String from = credentials.getAddress();

        EthGetTransactionCount transactionCount = htgWalletApi.getWeb3j().ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        //创建RawTransaction交易对象
        Function function = new Function("bidderClaim",
                Arrays.asList(new Address(from), new DynamicBytes(new byte[]{})),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(200000L),
                contractAddress, encodedFunction
        );
        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = htgWalletApi.getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();
        if (ethSendTransaction == null || ethSendTransaction.getResult() == null) {
            if (ethSendTransaction != null && ethSendTransaction.getError() != null) {
                System.err.println(String.format("Failed, error: %s", ethSendTransaction.getError().getMessage()));
            } else {
                System.err.println("Failed");
            }
            return;
        }
        System.out.println(ethSendTransaction.getTransactionHash());
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }

}