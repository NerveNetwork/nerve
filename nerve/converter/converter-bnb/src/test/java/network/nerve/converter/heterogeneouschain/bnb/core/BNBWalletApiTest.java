package network.nerve.converter.heterogeneouschain.bnb.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
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

    static String TRX_TRON_BNB_MINTER = "0x3fc005d5552a5a8236f366fB6Cca94527889Ec35";
    static String USDT_TRON_BNB_MINTER = "0xB8aAE3a961b9Fd45302c20e5346441ADB4cB0d28";

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

    protected void setErc20USDTOfTRON() {
        erc20Address = USDT_TRON_BNB_MINTER;
        erc20Decimals = 6;
    }

    protected void setErc20TrxOfTRON() {
        erc20Address = TRX_TRON_BNB_MINTER;
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
        //list.add("c98cf686d26af4ec8e8cc8d8529a2494d9a3f1b9cce4b19bacca603734419244");//
        //list.add("493a2f626838b137583a96a5ffd3379463a2b15460fa67727c2a0af4f8966a05");//
        //list.add("4ec4a3df0f4ef0db2010d21d081a1d75bbd0a7746d5a83ba46d790070af6ecae");// 0x5d6a533268a230f9dc35a3702f44ebcc1bcfa389
        this.multySignContractAddress = "0xc9Ad179aDbF72F2DcB157D11043D5511D349a44b";
        init();
        erc20Address = HtgConstant.ZERO_ADDRESS;
        erc20Decimals = 0;
    }

    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        list.add("7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b");// 0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        // 7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b::::::::::0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        this.multySignContractAddress = "0xf85f03C3fAAC61ACF7B187513aeF10041029A1b2";
        init();
    }

    public void init() {
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        this.address = Credentials.create(list.get(0)).getAddress();
        //this.address = "0xd87f2ad3ef011817319fd25454fc186ca71b3b56";
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
     * Transfer to multiple signed contractsethanderc20（Used for testing withdrawals）
     */
    @Test
    public void transferBNBAndERC20() throws Exception {
        BigInteger gasPrice = htgContext.getEthGasPrice();
        // initialization account
        setAccount_EFa1();
        // BNBquantity
        String sendAmount = "0.1";
        String txHash = htgWalletApi.sendMainAssetForTestCase(from, fromPriKey, multySignContractAddress, new BigDecimal(sendAmount), BigInteger.valueOf(81000L), gasPrice);
        System.out.println(String.format("towards[%s]Transfer%sindividualBNB, transactionhash: %s", multySignContractAddress, sendAmount, txHash));
        // ERC20
        String tokenAddress = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
        String tokenAmount = "100";
        int tokenDecimals = 6;
        EthSendTransaction token = htgWalletApi.transferERC20TokenForTestCase(from, multySignContractAddress, new BigInteger(tokenAmount).multiply(BigInteger.TEN.pow(tokenDecimals)), fromPriKey, tokenAddress);
        System.out.println(String.format("towards[%s]Transfer%sindividualERC20(USDI), transactionhash: %s", multySignContractAddress, tokenAmount, token.getTransactionHash()));
    }

    /**
     * New way of rechargingeth
     */
    @Test
    public void depositBNBByCrossOut() throws Exception {
        setLocalTest();
        this.multySignContractAddress = "0xc9Ad179aDbF72F2DcB157D11043D5511D349a44b";
        // initialization account
        setAccount_EFa1();
        // BNBquantity
        String sendAmount = "0.11";
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal(sendAmount));
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, HtgConstant.ZERO_ADDRESS);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount, multySignContractAddress);
        System.out.println(String.format("bnbRecharge[%s], hash: %s", sendAmount, hash));
    }

    /**
     * New way of rechargingerc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        //setAccount_2501();
        //setAccount_024F();
        //setAccount_7B65();
        setAccount_EFa1();
        // ERC20 Transfer quantity
        String sendAmount = "1.2";
        // initialization ERC20 Address information
        //setErc20BUG();
        //setErc20EthMinter();
        //setErc20UsdiMinter();

        //setErc20BUG();
        setErc20USDX();
        //setErc20BUSD();
        //setErc20DXA();
        //setErc20GOAT();
        //setErc20SAFEMOON();
        //setErc20USDTOfTRON();


        //setErc20NVT();
        //setErc20NULS();
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20authorization
            BigInteger approveAmount = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, approveAmount);
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("erc20Authorization recharge[%s], authorizationhash: %s", approveAmount, authHash));
            while (htgWalletApi.getTxReceipt(authHash) == null) {
                System.out.println("wait for3Second query[ERC20authorization]Transaction packaging results");
                TimeUnit.SECONDS.sleep(3);
            }
            TimeUnit.SECONDS.sleep(3);
            BigInteger tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("wait for3Second query[ERC20authorization]Transaction limit");
                TimeUnit.SECONDS.sleep(3);
                tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            }
        }
        System.out.println("[ERC20authorization]The limit has met the conditions");
        // crossOut erc20Transfer
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, erc20Address);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT);
        System.out.println(String.format("erc20Recharge[%s], Rechargehash: %s", sendAmount, hash));
    }

    /**
     * One click cross chainUSDX
     */
    @Test
    public void oneClickCrossChainUSDXTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        int desChainId = 104;
        String desToAddress = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // One click cross chain handling fee
        String feeAmount = "0.000000001";
        // ERC20 Transfer quantity
        String sendAmount = "22.22";
        // initialization ERC20 Address information
        setErc20BUSD();
        // Nerve Receiving address
        String to = "0x0000000000000000000000000000000000000000";
        // tipping
        String tippingAddress = "0xd16634629c638efd8ed90bb096c216e7aec01a91";
        String tipping = "2.222";
        BigInteger convertTipping = new BigDecimal(tipping).movePointRight(erc20Decimals).toBigInteger();

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        // crossOut erc20Transfer
        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(feeCrossChain, desChainId, desToAddress, convertTipping, tippingAddress, null);
        String data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount.add(convertTipping), erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, feeCrossChain, multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendAmount, hash));
    }

    /**
     * One click cross chainUSDT(tron)To the wave field
     */
    @Test
    public void oneClickCrossChainUSDTOfTronTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        int desChainId = 108;
        String desToAddress = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // One click cross chain handling fee
        String feeAmount = "0.007";
        // ERC20 Transfer quantity
        String sendAmount = "0.4";
        // initialization ERC20 Address information
        setErc20USDTOfTRON();
        // Nerve Receiving address
        String to = "TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY";
        // tipping
        String tippingAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
        String tipping = "0.03";
        BigInteger convertTipping = new BigDecimal(tipping).movePointRight(erc20Decimals).toBigInteger();

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        // crossOut erc20Transfer
        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(feeCrossChain, desChainId, desToAddress, convertTipping, tippingAddress, null);
        String data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount, erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, feeCrossChain, multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendAmount, hash));
    }

    /**
     * One click cross chainTRX(tron)To the wave field
     */
    @Test
    public void oneClickCrossChainTrxOfTronTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        int desChainId = 108;
        String desToAddress = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // One click cross chain handling fee
        String feeAmount = "0.001";
        // ERC20 Transfer quantity
        String sendAmount = "0.25";
        // initialization ERC20 Address information
        setErc20TrxOfTRON();
        // Nerve Receiving address
        String to = "TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY";
        // tipping
        String tippingAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
        String tipping = "0.02";
        BigInteger convertTipping = new BigDecimal(tipping).movePointRight(erc20Decimals).toBigInteger();

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        // crossOut erc20Transfer
        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(feeCrossChain, desChainId, desToAddress, convertTipping, tippingAddress, null);
        String data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount, erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, feeCrossChain, multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendAmount, hash));
    }

    /**
     * One click cross chainNVT
     */
    @Test
    public void oneClickCrossChainNVTTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        int desChainId = 108;
        String desToAddress = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // One click cross chain handling fee
        String feeAmount = "0.000005";
        // ERC20 Transfer quantity
        String sendAmount = "2.5";
        // initialization ERC20 Address information
        setErc20NVT();
        // Nerve Receiving address
        String to = "TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY";
        // tipping
        String tippingAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
        String tipping = "0.2";
        BigInteger convertTipping = new BigDecimal(tipping).movePointRight(erc20Decimals).toBigInteger();

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        // crossOut erc20Transfer
        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(feeCrossChain, desChainId, desToAddress, convertTipping, tippingAddress, null);
        String data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount, erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, feeCrossChain, multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendAmount, hash));
    }

    /**
     * Cross chain additional handling fees
     */
    @Test
    public void addFeeCrossChainNVTTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        // One click cross chain handling fee
        String feeAmount = "0.000001217768718802";
        // Nerve Receiving address
        String to = "0x0000000000000000000000000000000000000000";
        // Cross chain transactions with additional transaction feeshash
        String nerveTxHash = "d0505fe1776249f6a3a8f8368f9547807164fa8b7a0d420ca85213eabe825bda";

        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function addFeeCrossChainFunction = HtgUtil.getAddFeeCrossChainFunction(nerveTxHash, null);
        String data = FunctionEncoder.encode(addFeeCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, BigInteger.ZERO, erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, feeCrossChain, multySignContractAddress);

        System.out.println(String.format("Cross chain additional handling fees[%s], transactionhash: %s", feeAmount, hash));
    }

    /**
     * One click cross chainBNB
     */
    @Test
    public void oneClickCrossChainMainAssetTest() throws Exception {
        setBeta();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        // BNB Transfer quantity
        String sendAmount = "0.01";
        // One click cross chain handling fee
        String feeAmount = "0.002";
        // Nerve Receiving address
        String to = "TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY";
        // tipping
        String tippingAddress = "";
        String tipping = "0.000000";
        BigInteger convertTipping = new BigDecimal(tipping).movePointRight(18).toBigInteger();

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(18).toBigInteger();
        BigInteger feeCrossChain = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(feeCrossChain, 101, "0xc11D9943805e56b630A401D4bd9A29550353EFa1", convertTipping, tippingAddress, null);
        String data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, BigInteger.ZERO, HtgConstant.ZERO_ADDRESS, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount.add(feeCrossChain), multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendAmount, hash));
    }

    /**
     * One click cross chain Trial and error
     */
    @Test
    public void oneClickCrossChainErrorTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        // initialization account
        setAccount_EFa1();
        String feeAmount = "0.000005";// One click cross chain handling fee
        setErc20USDX();// initialization ERC20 Address information
        String sendErc20Amount = "2.6";// ERC20 Transfer quantity
        String sendMainAmount = "0";// Main assets Transfer quantity
        int desChainId = 104;
        String desToAddress = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // Nerve Receiving address
        String to = "TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY";
        //String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertErc20Amount = new BigDecimal(sendErc20Amount).movePointRight(erc20Decimals).toBigInteger();
        BigInteger convertMainAmount = new BigDecimal(sendMainAmount).movePointRight(18).toBigInteger();
        // tipping
        String tippingAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";// TNVTdTSPGwjgRMtHqjmg8yKeMLnpBpVN5ZuuY or TNVTdTSPP9oSLvdtVSVFiUYCvXJdj1ZA1nyQU
        String tipping = "0.26";
        BigInteger convertTipping;
        if (convertErc20Amount.compareTo(BigInteger.ZERO) <= 0) {
            convertTipping = new BigDecimal(tipping).movePointRight(18).toBigInteger();
        } else {
            convertTipping = new BigDecimal(tipping).movePointRight(erc20Decimals).toBigInteger();
        }
        // crossOut erc20Transfer
        BigInteger convertFeeAmount = new BigDecimal(feeAmount).movePointRight(18).toBigInteger();
        String data = "0x";
        Function oneClickCrossChainFunction = HtgUtil.getOneClickCrossChainFunction(convertFeeAmount, desChainId, desToAddress, convertTipping, tippingAddress, null);
        data = FunctionEncoder.encode(oneClickCrossChainFunction);
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertErc20Amount, erc20Address, data);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertFeeAmount.add(convertMainAmount), multySignContractAddress);

        System.out.println(String.format("erc20One click cross chain[%s], transactionhash: %s", sendErc20Amount, hash));
    }

    /**
     * Contract upgrade testing
     */
    @Test
    public void upgradeContractTest() throws Exception {
        // environmental data
        setLocalTest();
        list.clear();
        list.add("e750c0c681a34449110787e234c125157f191449b5df27a10f6075f883b66f00");
        list.add("e84d680293e130f23068bc6bf0f16546bdb5f04816ec6d6338a9a14f0770e53a");
        list.add("b76bfc28683863b797a88749c1083819e2ec4e6358d71d34e0069da7b92a10f7");
        init();
        // GasPriceprepare
        htgContext.setEthGasPrice(BigInteger.valueOf(10L).multiply(BigInteger.TEN.pow(9)));
        htgContext.SET_VERSION((byte) 3);
        String txKey = "aaa3000000000000000000000000000000000000000000000000000000000000";
        int signCount = list.size();
        this.multySignContractAddress = "0x7293e234D14150A108f02eD822C280604Ee76583";
        String newContract = "0xA5666f880D56EC3E845e38f7A2c661e83e79f3C3";
        String hash = this.sendUpgrade(txKey, newContract, signCount);
        System.out.println(String.format("Contract upgrade testing: %s,newContract: %s, hash: %s", multySignContractAddress, newContract, hash));
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
                // dormancy15Block, send next transaction
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
        // Official website data
        setMain();
        // GasPriceprepare
        long gasPriceGwei = 100L;
        BigInteger gasPrice = BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9));
        // Super account, load credentials, use private key
        Credentials credentials = Credentials.create("");
        // Multiple contract addresses
        String contractAddress = "0x6758d4C4734Ac7811358395A8E0c3832BA6Ac624";
        // RegisteredERC20MinterContract address
        String erc20Minter = "0x7b6F71c8B123b38aa8099e0098bEC7fbc35B8a13";

        EthGetTransactionCount transactionCount = htgWalletApi.getWeb3j().ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        //establishRawTransactionTrading partner
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
        //autographTransactionHere, we need to sign the transaction
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //Send transaction
        EthSendTransaction ethSendTransaction = htgWalletApi.getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();
        System.out.println(ethSendTransaction.getTransactionHash());
    }

    /**
     * Restore Contract Administrator
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
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    protected void setBnbMainTest() {
        // "0xd87f2ad3ef011817319fd25454fc186ca71b3b56"
        // "0x0eb9e4427a0af1fa457230bef3481d028488363e"
        // "0xd6946039519bccc0b302f89493bec60f4f0b4610"
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// Public key: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("");// Public key: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("");// Public key: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        this.multySignContractAddress = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        init();
    }

    protected void setMainData() {
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
        setMain();
        list = new ArrayList<>();
        // To haveBNBPut the private key of the balance first
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        this.multySignContractAddress = "0x75Ab1d50BEDBd32b6113941fcF5359787a4bBEf4";
        init();
    }

    @Test
    public void signDataForMainAssetWithdrawTest() throws Exception {
        setMainData();
        String txKey = "6661024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x4aA04Ac1c34989cE6c36Fe7BEf9B1a2167bF59F5";
        // Mint quantity
        String value = "200";
        int signCount = 5;
        String signData = this.signDataForMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("BNB Withdrawal %s Pieces, %s Signatures, signData: %s", value, signCount, signData));
    }

    @Test
    public void sendMainAssetWithdrawBySignDataTest() throws Exception {
        setMainData();
        //setMainProxy();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        String txKey = "6661024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x4aA04Ac1c34989cE6c36Fe7BEf9B1a2167bF59F5";
        // quantity
        String value = "200";
        String signData = "cb61cc6a824fa9d322e4ff5dfb23a48733070c0b61958ba23a52d9561fb7c2af5c9825db54b6402a380e133703baae5f598d5a7ec668ff35aebf3a7b905425c11ba096547f9b3d0b44b1454506f155cb3aeaca7970144bbffdab4ef87a4e1a025f2aec9196c2b6a4eea5b7e1b6e4aa5279e67c86ac4e9357f08e708adfeaec0eac1bd8f2bca1f1463bdb7ca69fb57e23b827885f7148d7e4175bb8ba4ea83f7be6a116885b8ea01280eddea193eb27698eef72dc6beb44a00d8f8856e40ca4db970e1b281da6c076e5b69e2db0fedb5fc405de71e2500c0e968562e0b0934b59cd39ed3d1166f6a5e78e93ecb70ed969ccf624cf7b968c2a551f5752235bba4604d9881cb05a15c4378e7368479dd3897019c701e550270c5fe4b268fdb1bea4aed1cb9a01749b1bd9ef6f7899412d2c30e1585b62e97e94cf2579026ac2ba6f22bff3bf1c2a9919fbdccc741d47fae2e2249c0d96bdd949d6d4adb6306b1f3882ad7a3eec21ca2695336501624ae71fbd6c806b86d01c76bba2c52955e27d265ae234727e1bd5a7bf069cbe129bd85410c6adea0393f90e869ef5349e89768086573062ba343651fd747e908c36d1a5ba9434fd1f0884a94258546878c17f0563b7c36387d51bb5c87400795c27445fd71841bb2998db6eb7548d3c29ae70a229430b805d91fc0c7231ce6294eebd6b3ded8fa0f127eef9142e0bab0d75d8529080bee2c31bbc1b8e62bb1867acf134ec12f87c5ce4a4ac4063f0f7dcb257968ce987d906ce3fb01a4ffaa7d1a24d768d64346fbd28e8f20de9a864d6f212838549c342436c25db1b83f96b02d1f75987074db806137f31123c2304bcc28bb6114ae519ab603a97c617964009084275a223b01efd9a6c32e643ddc4372f028f99540c2cf1785948611b";

        String hash = this.sendMainAssetWithdrawBySignData(txKey, toAddress, value, signData);
        System.out.println(String.format("Withdrawal %s Pieces, hash: %s", value, hash));
    }

    /**
     * 5Signatures
     */
    @Test
    public void signDataForERC20WithdrawTest() throws Exception {
        setMainData();
        String txKey = "8881024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x4aA04Ac1c34989cE6c36Fe7BEf9B1a2167bF59F5";
        // Mint quantity
        String value = "2204052.340227488871564541";
        // NEST tokencontract
        String erc20 = "0xcd6926193308d3B371FdD6A6219067E550000000";
        int tokenDecimals = 18;
        int signCount = 5;
        String signData = this.signDataForERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20Withdrawal%sPieces,%sSignatures,signData: %s", value, signCount, signData));
    }

    /**
     * Based on existing signature data Send transaction - erc20Withdrawal
     */
    @Test
    public void sendERC20WithdrawBySignDataTest() throws Exception {
        setMainData();
        String txKey = "8881024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x4aA04Ac1c34989cE6c36Fe7BEf9B1a2167bF59F5";
        // Mint quantity
        String value = "2204052.340227488871564541";
        // NEST tokencontract
        String erc20 = "0xcd6926193308d3B371FdD6A6219067E550000000";
        int tokenDecimals = 18;
        String signData = "ad03147c18e9f89c6163dbf2a797ae117d53a51bb90aee1c3cd81743ddcf945521a5002f17f776b4513e23e7e7c418260d7a93ac99df5761ef9e4868fcf717931b260021098c49b2997404aa9c10adcb0733e9e14b20742e20aaa319f7efc28aac649bed9930aba054a1d16a9a9876b7623557f907a36447966b6fda8727d115741cfb16bd206ad342303ed65f3513e4843e06ea100bb2d6302b844107d080233560285b07598f4c3967af66b55245b54dda94f1e33d4f8b632fc78944a701e7c9731c7861deaf6538a058ddf873f3291358ec1aa98429e48154986677bdcc857f503254f75623954b674bd9d0b792d354641cc98f8d2da7eac5ca55f6b043061d942b1b9162f19afce93f19d1a1f47a4093311c1e9f9fd45f908a75a8ab4239fb66656a2a80aab499d32dd5178d05e854913fa7c6f14b9a40d7a79652214f29b8cceff11b200bb78cc2ad43c7ede5239a5b2a0f080d393564eaf5508df40a8681fb2f4d6a17706202c697a781f135328c23637a51de0419ed0eb5fadd85d549fc3932b5f41bd33504c2b455352b43d6edd2158702ffae02b7b5de90eec7e3542b2465c242a97e086f1d209561c46360b046577879240f555c86dc5098261fa8a2dfdd6106e61cf5dda6cb523e0ed41671170c8a86762c54509395c4356af69ac9ec881d34ac8012548616c417cf28553ed69cc2f88b5e17687db3731f50a667265d2ed38d0bae1b0e614b7abf1f1a070d6a0cc0faa09cb6ca215e4e2e79c1673bc1baf1f1148aa710743751f22e4ddd359e12fabeeeaef0083b8fc9f084b234a7a38e90e9a8cebd1c5b4570506329c6a544fc8312071308b65ccc2abfdbcaec644e48df10a88e35ce1ba9a63cd2fcb064d672ca0da7a9b5fa1108bb0256cab0d21ab9b126d502987a1b";

        String hash = this.sendERC20WithdrawBySignData(txKey, toAddress, value, erc20, tokenDecimals, signData);
        System.out.println(String.format("ERC20Withdrawal%sPieces,hash: %s", value, hash));
    }

    /**
     * Add N Administrators
     */
    @Test
    public void managerAdd() throws Exception {
        // Official network environment data
        //setMainData();
        setLocalTest();
        // GasPriceprepare
        long gasPriceGwei = 10L;
        htgContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        String txKey = "aaa5000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{
        };
        String[] removes = new String[]{
                "0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65"};
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * Add10Administrators,4Signatures
     */
    @Test
    public void managerAdd10By4Managers() throws Exception {
        String txKey = "aaa0000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x9f14432b86db285c76589d995aab7e7f88b709df", "0x42868061f6659e84414e0c52fb7c32c084ce2051", "0x26ac58d3253cbe767ad8c14f0572d7844b7ef5af", "0x9dc0ec60c89be3e5530ddbd9cf73430e21237565", "0x6392c7ed994f7458d60528ed49c2f525dab69c9a", "0xfa27c84ec062b2ff89eb297c24aaed366079c684", "0xc11d9943805e56b630a401d4bd9a29550353efa1", "0x3091e329908da52496cc72f5d5bbfba985bccb1f", "0x49467643f1b6459caf316866ecef9213edc4fdf2", "0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = 4;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * Replace an administrator,10Signatures
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setMainData();
        // GasPriceprepare
        long gasPriceGwei = 20L;
        htgContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        String txKey = "2755b93611fa03de342f3fe73284ad02500c6cd3531bbb93a94965214576b3cb";
        String[] adds = new String[]{"0xaff68cd458539a16b932748cf4bdd53bf196789f"};
        String[] removes = new String[]{"0xf08877ba2b11f9f7d3912bba36cc2b21447b1b42"};
        int txCount = 1;
        int signCount = 10;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * Replace an administrator,15Signatures
     */
    @Test
    public void managerReplace1By15Managers() throws Exception {
        String txKey = "ccc0000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        int txCount = 1;
        int signCount = 15;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * ethWithdrawal,15Signatures
     */
    @Test
    public void ethWithdrawBy15Managers() throws Exception {
        String txKey = "ddd0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 15;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("BNBWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * ethWithdrawal,10Signatures
     */
    @Test
    public void ethWithdrawBy10Managers() throws Exception {
        String txKey = "eee0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.02";
        int signCount = 10;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("BNBWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * erc20Withdrawal,10Signatures
     */
    @Test
    public void erc20WithdrawBy10Managers() throws Exception {
        setBeta();
        String txKey = "fff0000000000000000000000000000000000000000000000000000000000002";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "233228.604321626";
        String erc20 = "0x7BE69eb38443D3A632cB972df840013D667365e6";
        int tokenDecimals = 9;
        int signCount = 3;
        htgContext.SET_VERSION((byte) 2);
        String hash = this.sendERC20Withdraw(txKey, toAddress, value, erc20, tokenDecimals, signCount);
        System.out.println(String.format("ERC20Withdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * erc20Withdrawal,15Signatures
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
        System.out.println(String.format("ERC20Withdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * erc20Withdrawal and issuance of additional sharesERC20
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
        System.out.println(String.format("ERC20Withdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * ethWithdrawal, anomaly testing
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
        System.out.println(String.format("BNBWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * erc20Withdrawal, anomaly testing
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
        System.out.println(String.format("ERC20Withdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * Administrator change, abnormal testing
     */
    @Test
    public void errorChangeTest() throws Exception {
        String txKey = "J000000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
        String[] removes = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        int txCount = 1;
        int signCount = 15;
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
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
        String input = "38615bb000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000005f5e100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000264e4552564565706236356d466f78655866514e354b67654b7852546675664b656b42416e33430000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002c63623939313934642d336233302d346434362d623336352d6666613163663232376162642d2d2d4e41424f580000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_CROSS_OUT_II);
        //System.out.println(JSONUtils.obj2PrettyJson(typeList));
        for (Object obj : typeList) {
            if (obj instanceof byte[]) {
                System.out.println(Numeric.toHexStringNoPrefix((byte[]) obj));
                continue;
            }
            System.out.println(obj.toString());
        }

    }

    @Test
    public void txInputOneClickCrossChainDecoderTest() throws JsonProcessingException {
        //0x0000000000000000000000000000000000000000
        //1000000000000000000
        //0x0edc79e030b8077bc768542a252ecb0c7beb513f
        String input = "0x7d02ce340000000000000000000000000000000000000000000000000000b5e620f48000000000000000000000000000000000000000000000000000000000000000006800000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000128277a747a00000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000002a30786331314439393433383035653536623633304134303144346264394132393535303335334546613100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002a307864313636333436323963363338656664386564393062623039366332313665376165633031613931000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_ONE_CLICK_CROSS_CHAIN);
        //System.out.println(JSONUtils.obj2PrettyJson(typeList));
        for (Object obj : typeList) {
            if (obj instanceof byte[]) {
                System.out.println(Numeric.toHexStringNoPrefix((byte[]) obj));
                continue;
            }
            System.out.println(obj.toString());
        }

    }

    @Test
    public void txInputWithdrawDecoderTest() throws JsonProcessingException {
        String input = "0xab6c2b1000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000062616bb4f919dd601445f63f6d1add393a683e0a0000000000000000000000000000000000000000000000000000000002490e0d00000000000000000000000000000000000000000000000000000000000000010000000000000000000000003c2b8be99c50593081eaa2a724f0b8285f5aba8f0000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000004066313861366261366131623339383733393236663963663033653463373634323230313230336335653434643236366431653663633265326264383437636664000000000000000000000000000000000000000000000000000000000000028a7d6d781b31120853c18b7fec4c2b7d2b289e8ad1b3dc98355ff8fcf0f1cce56b235b53f11084d2b8031e1117830648cf06c632aa2e81109021afa5ef2aa1da931c432e86ed67c4fa9f3632d0e06d0684bf49a858f9fad528ee690298202cd11c4c2fa7e22eaa9cd6be29df85410ab8ae6f33e25f8498895f8ed7272263f7b26fc01c59d4b4f5de070e06547c86120b6ed2b382324cafb4c0a9c39cc5c0447da8244a4adf90f600dcbd38166c60d247df8774d2c721562d846dfffb4445453bdf14701bf9cb1eed9f7de3589fe8f4e59940919214d428d1436e90a1901f3a90e7b7e8f87bc39bdd8135fd946198d6249750d461a5e30cfdaa24efb6f67f25d84ab307fd1b09e0e7ce7f5a6afed1a21ff4954e499e09effa0fdab7ba34452600c69295b7e90026055445c57c797425513b6ed0a37f755c45ef08c7a34a4843bcf6475616b71c8b99b40b211cea7fc69a4ca01d22c967607ed4b7bfb361a5b71a0be7b0fa940e00dad23f46ac528c63b5a6845fb8209da72739c8b3b2d82d292f4339183bc0751c5eafc534c6e6da1694f43edcd1be464b3c9999c6334bcb38a3130181105f0a8c4d2718c96aeca3c7fdb8f7450b51dbfae5b95f5831e96fa96db72d2f378e23bf1c7903f85b3656320946e3f4513102d4d9d53b2b6bed464cf5fb38edd960489ca27e222390c96050d9e8ae767444f5a1653f70530a513e36780430228ddf10f8401b14dc3f5a282c7e03ec5a15036be261f0e0156488fbf35366b006324ee75e78200c5fa913c736783094ae6b5ffb83a8275de6740243a39a0965a18c2671cfb5101c3558e232a60148018968d4f7cfcaba40c8a8cb61c806ce852dc4e76dd598a45a365775bf7cc9cae22f4dbed1c4ea57845c52e15e1eff23dac39e45a1103f41b61b00000000000000000000000000000000000000000000";
        List<Object> typeList = HtgUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW);
        System.out.println(JSONUtils.obj2PrettyJson(typeList));
        String signs = HexUtil.encode((byte[]) typeList.get(5));
        System.out.println(signs);
        System.out.println(String.format("Number of signatures: %s", signs.length() / 130));


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
        String txKey = "0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03_0";
        String[] adds = new String[]{"0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03"};
        int count = 1;
        String[] removes = new String[]{};
        htgContext.config.setChainIdOnHtgNetwork(5);
        String hash = HtgUtil.encoderChange(htgContext, txKey, adds, count, removes, (byte) 3);
        System.out.println(String.format("hash: %s", hash));
    }

    @Test
    public void encoderUpgradeTest() {
        String txKey = "7570677261646561000000000000000000000000000000000000000000000000";
        String newContract = "0x36633F41BF6c578fDA44aE17A2E7BF40640f7Fb6";
        String hash = HtgUtil.encoderUpgrade(htgContext, txKey, newContract, (byte) 3);
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
        // Directly callingerc20contract
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
        setMain();// 1692719916
        Long height = Long.valueOf(31079341);
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(height);
        System.out.println(block.getHash());
    }

    @Test
    public void getBlockHeight() throws Exception {
        //setMain();
        System.out.println(htgWalletApi.getBlockHeight());
    }

    public static void main(String[] args) {
        // lock10Day, every day240Blocks
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
    public void getTxReceipt() throws Exception {
        setMain();
        // Directly callingerc20contract
        String directTxHash = "0xb4992925f9b5f534ec6a26ec89fd07220b3f258ff51de6efecb6cd2142cbfef9";
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt);
    }

    @Test
    public void getCurrentGasPrice() throws IOException {
        //setMain();
        setTestProxy();
        EthGasPrice send = htgWalletApi.getWeb3j().ethGasPrice().send();
        
        BigInteger gasPrice = send.getGasPrice();
        System.out.println(gasPrice);
        System.out.println(new BigDecimal(gasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
    }

    /**
     * Transfer ineth
     */
    @Test
    public void transferMainAsset() throws Exception {
        List<String> tos = new ArrayList<>();
        tos.add("0x5b7715efcbe4f2e9c474a246669f0eec77e271eb");
        tos.add("0xa43cb8b34e3684146c7d7c40ec875e419ddd6ab5");
        tos.add("0x993ceca520eaa925800686c4c4871f1dd78d0e19");
        tos.add("0xb6a385bd1f16b830ea95df776d30649635dd98e3");
        tos.add("0x2f865579a9fb015fe6cf4ce83ec0572ebb567078");
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        BigInteger gasPrice = htgContext.getEthGasPrice();
        // initialization account
        setAccount_EFa1();
        // MainAssetquantity
        String sendAmount = "0.4";
        for (String to : tos) {
            String txHash = htgWalletApi.sendMainAssetForTestCase(from, fromPriKey, to, new BigDecimal(sendAmount), BigInteger.valueOf(100000L), gasPrice);
            System.out.println(String.format("towards[%s]Transfer%sindividualMainAsset, transactionhash: %s", to, sendAmount, txHash));
        }
    }

    @Test
    public void allContractManagerSet() throws Exception {
        //localdev();
        //localdevII();
        //setMain();
        setLocalTest();
        multySignContractAddress = "0xdC6B95B2032f4445a3ee4154E0Fa005814B447d1";
        //mainnetII();
        System.out.println("Please wait for the current list of contract administrators to be queried……");
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
            System.out.println(String.format("gasLimit: %s, details: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
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
        //// estimateGasLimit
        //EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, contract, txFunction, value);
        //BigInteger estimateGas = estimateGasObj.getAmountUsed();
    }

    @Test
    public void erc20TransferEstimateGasTest() throws Exception {
        // 0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5", "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4"
        // // Recipient Address
        //        String toAddress = "0x7d4565d769dd39227e1ea36dbb3a02075cc0f623";
        //        // Mint quantity
        //        String value = "852.409090437364882556";
        //        // tokencontract
        //        String erc20 = "0x5d7f9c9f3f901f2c1b576b8d81bd4165647855a4";
        //        int tokenDecimals = 18;
        setMainProxy();
        String contractAddress = "0x0b6d7735e0430d48675cba2955e87ccb0cd754cf";
        BigInteger convertAmount = new BigDecimal("993929.538461538461538461").movePointRight(18).toBigInteger();
        //BigInteger convertAmount = new BigDecimal("1").toBigInteger();
        String from = "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4";
        String to = "0x3d24072c93c051e1241e682b05156915f9a6dd08";
        //String to = "0x0000000000000000000000000000000000000000";

        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(convertAmount)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger value = null;
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                from,
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
            System.out.println(String.format("gasLimit: %s, details: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void erc20TransferFromEstimateGasTest() throws Exception {
        setMain();
        //String sender = "0x10ED43C718714eb63d5aA57B78B54704E256024E";
        String sender = "0xcF0feBd3f17CEf5b47b0cD257aCf6025c5BFf3b7";
        //String contractAddress = "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56";
        String contractAddress = "0x755f34709e369d37c6fa52808ae84a32007d1155";
        BigInteger convertAmount = new BigDecimal("1").movePointRight(18).toBigInteger();
        String from = "0xfa84267c6441C53ec5fc1e710879c42A7E064fa5";
        String to = "0x29b4abb0f8734EA672a0e82FA47998F710B6A07a";

        Function function = new Function(
                "transferFrom",
                Arrays.asList(new Address(from),new Address(to), new Uint256(convertAmount)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger value = null;
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                sender,
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
            System.out.println(String.format("gasLimit: %s, details: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void txEstimateGasTest() throws Exception {
        setMain();
        String sender = "0x0Fdb956B85630912f56d1cf7BE8aC2c923e407f7";
        String contractAddress = "0x51187757342914E7d94FFFD95cCCa4f440FE0E06";
        BigInteger value = new BigDecimal("0.002003").movePointRight(18).toBigInteger();

        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                sender,
                null,
                null,
                null,
                contractAddress,
                value,
                "0x14d9e096000000000000000000000000000000000000000000000000000000000000001700000000000000000000000000000000000000000000000000038d7ea4c680000000000000000000000000000fdb956b85630912f56d1cf7be8ac2c923e407f7"
        );
        EthEstimateGas estimateGas = htgWalletApi.getWeb3j().ethEstimateGas(tx).send();
        if(estimateGas.getResult() != null) {
            System.out.println(String.format("gasLimit: %s, details: %s", estimateGas.getResult(), JSONUtils.obj2PrettyJson(estimateGas)));
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(estimateGas.getError()));
        }
    }

    @Test
    public void symboltest() throws Exception {
        setMain();
        String contractAddress = "0x9f5C37e0fd9bF729b1F0a6F39CE57bE5e9Bfd435";
        List<Type> symbolResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return;
        }
        String symbol = symbolResult.get(0).getValue().toString();
        System.out.println("|" + symbol + "|");

        List<Type> nameResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getNameERC20Function());
        if (nameResult.isEmpty()) {
            return;
        }
        String name = nameResult.get(0).getValue().toString();
        System.out.println( "|" + name + "|");

        List<Type> decimalsResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getDecimalsERC20Function());
        if (decimalsResult.isEmpty()) {
            return;
        }
        String decimals = decimalsResult.get(0).getValue().toString();
        System.out.println("|" + decimals + "|");
    }

    @Test
    public void lpRewardTest() throws Exception {
        setMain();
        // User address
        String userAddress = "";
        // Total user investmentnulsquantity
        String totalUserNuls = "123";
        // Total user investmentusdquantity
        String totalUserBusd = "234";
        // currentnulsThe price of(rightUSD)
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
        System.out.println(String.format("currentNULSprice: %s USDT/NULS", nulsPrice.toPlainString()));


        List<Type> tsResult = htgWalletApi.callViewFunction(lpContract, new Function(
                "totalSupply",
                List.of(),
                List.of(new TypeReference<Uint256>() {
                })));
        if (tsResult.isEmpty()) {
            return;
        }
        BigDecimal totalSupply = new BigDecimal(tsResult.get(0).getValue().toString());
        System.out.println(String.format("LPTotal flux: %s", totalSupply.movePointLeft(18).toPlainString()));
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
        System.out.println(String.format("Flow poolNULS: %s", new BigDecimal(_nuls).movePointLeft(8).toPlainString()));
        System.out.println(String.format("Flow poolBUSD: %s", new BigDecimal(_busd).movePointLeft(18).toPlainString()));
        System.out.println();
        System.out.println(String.format("I providedNULS: %s", myNuls.toPlainString()));
        System.out.println(String.format("I providedBUSD: %s", myBusd.toPlainString()));
        System.out.println();

        BigDecimal nulsValue = new BigDecimal(new BigDecimal(_nuls).multiply(yz).toBigInteger()).movePointLeft(8);
        BigDecimal busdValue = new BigDecimal(new BigDecimal(_busd).multiply(yz).toBigInteger()).movePointLeft(18);
        System.out.println(String.format("What I can exchangeNULS: %s", nulsValue.toPlainString()));
        System.out.println(String.format("What I can exchangeBUSD: %s", busdValue.toPlainString()));
        System.out.println();

        BigDecimal dNuls = nulsValue.subtract(myNuls);
        System.out.println(String.format("NULS %sNow: %s, converted toBUSD: %s", dNuls.compareTo(BigDecimal.ZERO) > 0 ? "many" : "less", dNuls.abs().toPlainString(), dNuls.multiply(nulsPrice).toPlainString()));
        BigDecimal dBusd = busdValue.subtract(myBusd);
        System.out.println(String.format("BUSD %sNow: %s", dBusd.compareTo(BigDecimal.ZERO) > 0 ? "many" : "less", dBusd.abs().toPlainString()));
        System.out.println();

        BigDecimal finalShouYi = dBusd.add(dNuls.multiply(nulsPrice));
        System.out.println(String.format("Impermanent lossorLiquidity gains: %s", finalShouYi.toPlainString()));
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
    public void newParseWithdrawTxReceiptSinceProtocol21Test() throws Exception {
        setMainData();
        htgContext.SET_MULTY_SIGN_ADDRESS(multySignContractAddress);
        List<String> list = new ArrayList<>();
        list.add("0x7a55a728421c6c47ec0753fdd122fa5f4014644185d39664a62545b81674bcf4");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", htgContext);
            Method method = helper.getClass().getDeclaredMethod("newParseWithdrawTxReceipt", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class);
            method.setAccessible(true);
            Object invoke = method.invoke(helper, tx, txReceipt, po);
            System.out.println(String.format("newParseWithdrawTxReceipt return: %s", invoke));
            BigInteger newValue = po.getValue();
            System.out.println(String.format("newParseWithdrawTxReceipt Withdraw Amount: %s", newValue));

            Method method1 = helper.getClass().getDeclaredMethod("newParseWithdrawTxReceiptSinceProtocol21", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionBaseInfo.class);
            method1.setAccessible(true);
            Object invoke1 = method1.invoke(helper, tx, txReceipt, po);
            System.out.println(String.format("newParseWithdrawTxReceiptSinceProtocol21 return: %s", invoke1));
            BigInteger value1 = po.getValue();
            System.out.println(String.format("newParseWithdrawTxReceiptSinceProtocol21 Withdraw Amount: %s", value1));
            System.out.println();
        }
    }

    @Test
    public void newValidationEthDepositByCrossOutTest() throws Exception {
        setMain();
        List<String> list = new ArrayList<>();
        list.add("0xb3c31761e479484f56196f0cdc984b94fa5817abf46b186406477ed1b2252a51");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", new MockBnbContext());
            BeanUtilTest.setBean(helper, "htgERC20Helper", new MockHtgERC20Helper());
            Method method = helper.getClass().getDeclaredMethod("newValidationEthDepositByCrossOutProtocol22", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionInfo.class);
            method.setAccessible(true);
            boolean invoke = (boolean) method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke ? "true, amount: " + po.getValue() : "false");

            method = helper.getClass().getDeclaredMethod("newValidationEthDepositByCrossOut", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionInfo.class);
            method.setAccessible(true);
            invoke = (boolean) method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke ? "true, amount: " + po.getValue() : "false");
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
        // Directly callingerc20contract
        String directTxHash = "0xb4992925f9b5f534ec6a26ec89fd07220b3f258ff51de6efecb6cd2142cbfef9";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(tx.getBlockNumberRaw() + "---" + txReceipt.getBlockNumberRaw());
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println(JSONUtils.obj2PrettyJson(tx));
        long s = System.currentTimeMillis();
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        tx = htgWalletApi.getTransactionByHash(directTxHash);
        txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt.getBlockNumber().longValue());
        System.out.println(tx.getBlockNumber().longValue());
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        long e = System.currentTimeMillis();
        System.out.println(e - s);
    }

    @Test
    public void oneClickCrossChainDataFunctionTest() {
        BigInteger feeAmount = new BigDecimal("0.012").movePointRight(18).toBigInteger();
        Function function = HtgUtil.getOneClickCrossChainFunction(feeAmount, 102, "0x830befa62501F1073ebE2A519B882e358f2a0318", null);
        String data = FunctionEncoder.encode(function);
        System.out.println(data);
        data = HtgConstant.HEX_PREFIX + data.substring(10);
        List<Type> typeList = FunctionReturnDecoder.decode(data, HtgConstant.INPUT_ONE_CLICK_CROSS_CHAIN);
        String subExtend = Numeric.toHexString((byte[]) typeList.get(typeList.size() - 1).getValue());
        System.out.println(subExtend);
    }

    @Test
    public void addFeeCrossChainDataFunctionTest() {
        String nerveTxHash = "b1ddb98a5263d921e4394b17e5cb824bac587ce67420ad0f72943f37c9c26be6";
        Function function = HtgUtil.getAddFeeCrossChainFunction(nerveTxHash, null);
        String extend = FunctionEncoder.encode(function);
        System.out.println(FunctionEncoder.encode(function));
        extend = HtgConstant.HEX_PREFIX + extend.substring(10);
        List<Type> typeList = FunctionReturnDecoder.decode(extend, HtgConstant.INPUT_ADD_FEE_CROSS_CHAIN);
        String subExtend = Numeric.toHexString((byte[]) typeList.get(typeList.size() - 1).getValue());
        System.out.println(typeList.get(0).getValue());
        System.out.println(subExtend);
    }

    @Test
    public void xtmcUpgradeTest() throws Exception {
        setMain();
        int htgChainId = 102;
        String from = "0x3250dABB584f7FEA1BAFAFf6000FFBBD2F419A15";
        String oldMulty = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        String erc20Contract = "0x822f73c2ba95080490579e631434061edbd00215";
        Function upgradeContractS2 = new Function("upgradeContractS2", Arrays.asList(new Address(erc20Contract)), List.of());
        EthEstimateGas ethEstimateGas = htgWalletApi.ethEstimateGas(from, oldMulty, upgradeContractS2);
        if (ethEstimateGas.getError() != null) {
            System.err.println(String.format("Verification failed[TokenAsset transfer] - HtgChainId: %s, multyContract: %s, erc20Contract: %s, Failed to transfer, error: %s", htgChainId, oldMulty, erc20Contract, ethEstimateGas.getError().getMessage()));
        } else {
            System.out.println(String.format("Verification successful, result: %s", ethEstimateGas.getAmountUsed()));
        }

    }

    @Test
    public void dodoTx() throws Exception {
        // Official website data
        setMain();
        // GasPriceprepare
        long gasPriceGwei = 5L;
        BigInteger gasPrice = BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9));
        // Wallet account, loading credentials, using private key
        Credentials credentials = Credentials.create("");
        // Contract address
        String contractAddress = "0x23dcfd1edf572204c7aee9680a9d853ec2c993ae";
        String from = credentials.getAddress();

        EthGetTransactionCount transactionCount = htgWalletApi.getWeb3j().ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        //establishRawTransactionTrading partner
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
        //autographTransactionHere, we need to sign the transaction
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //Send transaction
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
    static class MockBnbContext extends BnbContext {
        @Override
        public NulsLogger logger() {
            return Log.BASIC_LOGGER;
        }

        @Override
        public String MULTY_SIGN_ADDRESS() {
            return "0x75ab1d50bedbd32b6113941fcf5359787a4bbef4";
        }

        @Override
        public byte VERSION() {
            return 3;
        }
    }

}
