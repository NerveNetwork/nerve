package network.nerve.converter.heterogeneouschain.okt.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.okt.base.Base;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
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


public class OktWalletApiTest extends Base {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;

    static String USDT_OKT = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    //static String USDX_OKT = "0x74A163fCd791Ec7AaB2204ffAbf1A1DFb8854883";
    static String USDX_OKT = "0x21dd9f2f535a0d9bc4d667b55041650d773ea9c9";
    static String NVT_OKT_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String NULS_OKT_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String ETH_OKT_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String USDX_OKT_MINTER = "0x64fBEf777594Bd2f763101abde343AF72CF1f8d3";

    static String OKUSD_OKT_8 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String HUSD_HT_18 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String BUSD_BNB_18 = "0x02e1aFEeF2a25eAbD0362C4Ba2DC6d20cA638151";

    protected void setErc20USDT() {
        erc20Address = USDT_OKT;
        erc20Decimals = 6;
    }
    protected void setErc20USDX() {
        erc20Address = USDX_OKT;
        erc20Decimals = 6;
    }
    protected void setErc20OKUSD() {
        erc20Address = OKUSD_OKT_8;
        erc20Decimals = 8;
    }
    protected void setErc20NVT() {
        erc20Address = NVT_OKT_MINTER;
        erc20Decimals = 8;
    }
    protected void setErc20NULS() {
        erc20Address = NULS_OKT_MINTER;
        erc20Decimals = 8;
    }

    protected void setErc20EthMinter() {
        erc20Address = ETH_OKT_MINTER;
        erc20Decimals = 18;
    }

    protected void setErc20USDXMinter() {
        erc20Address = USDX_OKT_MINTER;
        erc20Decimals = 6;
    }

    protected void setAccount_c684() {
        from = "0xfa27c84eC062b2fF89EB297C24aaEd366079c684";
        fromPriKey = "b36097415f57fe0ac1665858e3d007ba066a7c022ec712928d2372b27e8513ff";
    }
    protected void setAccount_EFa1() {
        from = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        fromPriKey = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    }

    protected void setAccount_5996() {
        from = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        fromPriKey = pMap.get(from.toLowerCase()).toString();
    }

    Map<String, Object> pMap;
    String packageAddressPrivateKeyZP;
    String packageAddressPrivateKeyNE;
    String packageAddressPrivateKeyHF;
    @Before
    public void before() {
        try {
            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            String packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
            String packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
            String packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
            packageAddressPrivateKeyZP = pMap.get(packageAddressZP).toString();
            packageAddressPrivateKeyNE = pMap.get(packageAddressNE).toString();
            packageAddressPrivateKeyHF = pMap.get(packageAddressHF).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setLocalNewTest() {
        list = new ArrayList<>();
        list.add(packageAddressPrivateKeyZP);// 0x2804A4296211Ab079AED4e12120808F1703841b3
        list.add(packageAddressPrivateKeyNE);// 0x4202726a119F7784085B04264BfF716267a51032
        list.add(packageAddressPrivateKeyHF);// 0x4dAE32e287D43Ba6F6fE9323864e67A9c66B47e6
        this.multySignContractAddress = "0xf85f03C3fAAC61ACF7B187513aeF10041029A1b2";
        init();
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
        //list.add("43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D");// 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65
        //list.add("0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85");// 0xd29E172537A3FB133f790EBE57aCe8221CB8024F
        //list.add("CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2");// 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
        //list.add("c98cf686d26af4ec8e8cc8d8529a2494d9a3f1b9cce4b19bacca603734419244");//
        //list.add("493a2f626838b137583a96a5ffd3379463a2b15460fa67727c2a0af4f8966a05");//
        //list.add("4ec4a3df0f4ef0db2010d21d081a1d75bbd0a7746d5a83ba46d790070af6ecae");// 0x5d6a533268a230f9dc35a3702f44ebcc1bcfa389
        this.multySignContractAddress = "0xf85f03C3fAAC61ACF7B187513aeF10041029A1b2";// 0x4B3230f6d76B9DE6B41c1E484eD5401f0F0458a9
        // spare: 0xB29A26df2702B10BFbCf8cd52914Ad1fc99A4540
        init();
    }
    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        list.add("7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b");// 0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        // 7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b::::::::::0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        this.multySignContractAddress = "0xab34B1F41dA5a32fdE53850EfB3e54423e93483e";
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
        String sendAmount = "0.5";
        for (String to : tos) {
            String txHash = htgWalletApi.sendMainAssetForTestCase(from, fromPriKey, to, new BigDecimal(sendAmount), BigInteger.valueOf(100000L), gasPrice);
            System.out.println(String.format("towards[%s]Transfer%sindividualMainAsset, transactionhash: %s", to, sendAmount, txHash));
        }
    }

    /**
     * Transfer to multiple signed contractsethanderc20（Used for testing withdrawals）
     */
    @Test
    public void transferMainAssetAndERC20() throws Exception {
        BigInteger gasPrice = htgContext.getEthGasPrice();
        // initialization account
        setAccount_EFa1();
        // MainAssetquantity
        String sendAmount = "0.1";
        String txHash = htgWalletApi.sendMainAssetForTestCase(from, fromPriKey, multySignContractAddress, new BigDecimal(sendAmount), BigInteger.valueOf(81000L), gasPrice);
        System.out.println(String.format("towards[%s]Transfer%sindividualMainAsset, transactionhash: %s", multySignContractAddress, sendAmount, txHash));
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
    public void depositMainAssetByCrossOut() throws Exception {
        setLocalNewTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_5996();
        // MainAssetquantity
        String sendAmount = "0.001";
        // Nerve Receiving address
        String to = "TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad";
        BigInteger convertAmount = htgWalletApi.convertMainAssetToWei(new BigDecimal(sendAmount));
        Function crossOutFunction = HtgUtil.getCrossOutFunction(to, convertAmount, HtgConstant.ZERO_ADDRESS);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, convertAmount, multySignContractAddress);
        System.out.println(String.format("MainAssetRecharge[%s], hash: %s", sendAmount, hash));
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
    public void balanceOfMain() throws Exception {
        setMain();
        // 0xd87F2ad3EF011817319FD25454FC186CA71B3B56 0x0Eb9e4427a0Af1Fa457230bEF3481d028488363E
        String account = "0xd87F2ad3EF011817319FD25454FC186CA71B3B56";
        BigDecimal balance = htgWalletApi.getBalance(account);
        System.out.println(balance.movePointLeft(18).toPlainString());
    }
    /**
     * New way of rechargingerc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        // ERC20 Transfer quantity
        String sendAmount = "1.12";
        // initialization ERC20 Address information
        //setErc20EthMinter();
        //setErc20UsdiMinter();
        //setErc20DXA();
        //setErc20USDX();
        //setErc20OKUSD();
        setErc20USDT();
        //setErc20NVT();
        //setErc20NULS();
        //setErc20USDXMinter();
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        //erc20Address = "0x3a202151ad3f67cfe1647f7aeb9b8f60c8b257dd";
        //multySignContractAddress = "0x7293e234D14150A108f02eD822C280604Ee76583";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {}));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20authorization
            BigInteger approveAmount = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
            //new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals))
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, approveAmount);
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("erc20Authorization recharge[%s], authorizationhash: %s", approveAmount, authHash));
            while (htgWalletApi.getTxReceipt(authHash) == null) {
                System.out.println("wait for8Second query[ERC20authorization]Transaction packaging results");
                TimeUnit.SECONDS.sleep(8);
            }
            TimeUnit.SECONDS.sleep(8);
            BigInteger tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("wait for8Second query[ERC20authorization]Transaction limit");
                TimeUnit.SECONDS.sleep(8);
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
     * One click cross chainUSDT
     */
    @Test
    public void oneClickCrossChainUSDTTest() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        int desChainId = 102;
        String desToAddress = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // One click cross chain handling fee
        String feeAmount = "0.0001";
        // ERC20 Transfer quantity
        String sendAmount = "3.22";
        // initialization ERC20 Address information
        setErc20USDT();
        // Nerve Receiving address
        String to = "0x0000000000000000000000000000000000000000";
        // tipping
        String tippingAddress = "0xd16634629c638efd8ed90bb096c216e7aec01a91";
        String tipping = "0.2321";
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
        String[] removes = new String[]{
                "0xc753d679f58b6111cc7df52b57217a0d81ff725c"
        };
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    /*protected void setMainData() {
        setMain();
        // "0xd87f2ad3ef011817319fd25454fc186ca71b3b56"
        // "0x0eb9e4427a0af1fa457230bef3481d028488363e"
        // "0xd6946039519bccc0b302f89493bec60f4f0b4610"
        list = new ArrayList<>();
        list.add("");// Public key: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("");// Public key: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("");// Public key: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        this.multySignContractAddress = "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5";
        init();
    }*/
    /**
     * Add N Administrators
     */
    @Test
    public void managerAdd() throws Exception {
        // Official network environment data
        //setMainData();
        setLocalTest();
        // GasPriceprepare
        long gasPriceGwei = 1L;
        htgContext.setEthGasPrice(BigInteger.valueOf(gasPriceGwei).multiply(BigInteger.TEN.pow(9)));
        String txKey = "aaa4000000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{};
        String[] removes = new String[]{
                "0x8f05ae1c759b8db56ff8124a89bb1305ece17b65"
        };
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
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
        String newContract = "0x5478f79fce38762a4E5e9e7f3A9a748B0D06f8F0";
        String hash = this.sendUpgrade(txKey, newContract, signCount);
        System.out.println(String.format("Contract upgrade testing: %s,newContract: %s, hash: %s", multySignContractAddress, newContract, hash));
    }

    @Test
    public void allContractManagerSet() throws Exception {
        setBeta();
        multySignContractAddress = "0x4B3230f6d76B9DE6B41c1E484eD5401f0F0458a9";
        System.out.println("Please wait for the current list of contract administrators to be queried……");
        Set<String> all = this.allManagers(multySignContractAddress);
        System.out.println(String.format("size : %s", all.size()));
        for (String address : all) {
            BigDecimal balance = htgWalletApi.getBalance(address).movePointLeft(18);
            System.out.print(String.format("address %s : %s", address, balance.toPlainString()));
            System.out.println();
        }
    }

    private LinkedHashSet<String> allManagers(String contract) throws Exception {
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
        for(Type type : typeList) {
            results.add(type.getValue().toString());
        }
        String resultStr = results.get(0).substring(1, results.get(0).length() - 1);
        String[] resultArr = resultStr.split(",");
        LinkedHashSet<String> resultList = new LinkedHashSet<>();
        for(String result : resultArr) {
            resultList.add(result.trim().toLowerCase());
        }
        return resultList;
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

    protected void setUpgradeMain() {
        setMain();
        list = new ArrayList<>();
        // holdCCPut the private key first
        list.add("");// 0xd6946039519bccc0b302f89493bec60f4f0b4610
        list.add("");// 0xd87f2ad3ef011817319fd25454fc186ca71b3b56
        list.add("");// 0x0eb9e4427a0af1fa457230bef3481d028488363e
        list.add("");// ???
        list.add("");// ???
        this.multySignContractAddress = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        init();
    }

    /**
     * Replace an administrator,10Signatures
     */
    @Test
    public void managerReplace1By10Managers() throws Exception {
        setUpgradeMain();
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

    protected void setMainData() {
        setMain();
        list = new ArrayList<>();
        // To haveOKTPut the private key of the balance first
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        this.multySignContractAddress = "0xe096d12d6cb61e11bce3755f938b9259b386523a";
        init();
    }

    /**
     * 5Signatures
     */
    @Test
    public void signDataForERC20WithdrawTest() throws Exception {
        setMainData();
        String txKey = "bbb1024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x351af1631aa5ea1ca62ad8a4e3cd87128d4d9108";
        // Withdrawal quantity
        String value = "4209.8239259093025";
        // cc
        String erc20 = "0xe93707eda8bd9690e7a0ed754be9d064ed6984e4";
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
        String txKey = "bbb1024000000000000000000000000000000000000000000000000000000000";
        // Recipient Address
        String toAddress = "0x351af1631aa5ea1ca62ad8a4e3cd87128d4d9108";
        // Withdrawal quantity
        String value = "4209.8239259093025";
        // cc
        String erc20 = "0xe93707eda8bd9690e7a0ed754be9d064ed6984e4";
        int tokenDecimals = 18;
        String signData = "???";
        String hash = this.sendERC20WithdrawBySignData(txKey, toAddress, value, erc20, tokenDecimals, signData);
        System.out.println(String.format("ERC20Withdrawal%sPieces,hash: %s", value, hash));
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
        System.out.println(String.format("MainAssetWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
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
        System.out.println(String.format("MainAssetWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
    }

    /**
     * erc20Withdrawal,10Signatures
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
    public void errorMainAssetWithdrawTest() throws Exception {
        String txKey = "h500000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "0.01";
        int signCount = 11;
        list.addAll(5, list.subList(5, 10));
        //this.VERSION = 2;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("MainAssetWithdrawal%sPieces,%sSignatures,hash: %s", value, signCount, hash));
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
        // Directly callingerc20contract
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
        BigDecimal nvtUsd = new BigDecimal("0.16");
        BigDecimal nvtAmount = new BigDecimal(21_00000000L);
        BigDecimal ethUsd = new BigDecimal("360.16");
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
        setMain();
        System.out.println(htgWalletApi.getBlockHeight());
    }

    @Test
    public void getTx() throws Exception {
        //setMain();
        String directTxHash = "0xf169a140a8f44d37ad8b950029eca030e540695069fcf9528961a72f51e2f05d";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        System.out.println(JSONUtils.obj2PrettyJson(tx));
    }

    @Test
    public void getTestNetTxReceipt() throws Exception {
        // 0x7f054ce8e7a3c2aaa1939275ef7f5246f0372f095d2a26b01dc723b74c495f39
        // 0xdb046601d0431c217e2a6e1a40bd17a7fff55364c57b8f20090b5db6be4a7cc1
        //setMainProxy();
        //setMain();
        // Directly callingerc20contract
        String directTxHash = "0xf169a140a8f44d37ad8b950029eca030e540695069fcf9528961a72f51e2f05d";
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
        String contractAddress = "0x54e4622DC504176b3BB432dCCAf504569699a7fF";
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
        // erc20authorization
        //BigInteger approveAmount = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",16);
        htgContext.setEthGasPrice(new BigDecimal("0.1").scaleByPowerOfTen(9).toBigInteger());
        setMain();
        String from = "";
        String fromPriKey = "";
        String to = "0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5";
        String erc20 = "0x54e4622DC504176b3BB432dCCAf504569699a7fF";
        BigInteger approveAmount = new BigInteger("1",16);
        Function approveFunction = this.getERC20ApproveFunction(to, approveAmount);
        String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20);
        System.out.println(String.format("erc20Authorization recharge[%s], authorizationhash: %s", approveAmount, authHash));
    }

    @Test
    public void isMinterERC20() throws Exception {
        String multy = "0xab34b1f41da5a32fde53850efb3e54423e93483e";
        //String erc20 = "0x2785f6458c3bab956ccb1542f602c69d1188b28f";//TRX
        String erc20 = "0xd8eb69948e214da7fd8da6815c9945f175a4fce7";//NULS
        Function isMinterERC20Function = HtgUtil.getIsMinterERC20Function(erc20);
        List<Type> valueTypes = htgWalletApi.callViewFunction(multy, isMinterERC20Function, true);
        boolean isMinterERC20 = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        System.out.println(isMinterERC20);
    }

    @Test
    public void broadcastTest() throws Exception {
        String hexValue = "0xf8ab198504a817c8008301044494d8eb69948e214da7fd8da6815c9945f175a4fce780b844095ea7b3000000000000000000000000b490f2a3ec0b90e5faa1636be046d82ab7cdac74ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff81a5a00b18f7ea819578ff55b25b8d29d9fe49e2404f515e9021cc5ef54a6a4de0cb41a0064f4731345d1262e2d3b5e4005c67e0c74a07f25f4bbbbf36ef464fc2014836";
        EthSendTransaction send = htgWalletApi.getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();
        if (send == null) {
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD, String.format("%s request error", "OKT"));
        }
        if (send.hasError()) {
            System.err.println(send.getError().getMessage());
            return;
        }
        System.out.println(send.getTransactionHash());
    }

    @Test
    public void nonceTest() throws Exception {
        System.out.println(htgWalletApi.getLatestNonce("0x3083f7ed267dca41338de3401c4e054db2a1cd2f"));
    }

    @Test
    public void broadcastTxTest() throws Exception {
        String _privateKey = "7ce617815b0e2f570d0c7eb77339d85fbdaf132f389ee5a2d1f9a30c05861b45";
        String _toAddress = "0xd8eb69948e214da7fd8da6815c9945f175a4fce7";
        BigInteger _nonce = new BigInteger("25");
        BigInteger _gasPrice = new BigInteger("20000000000");
        BigInteger _gasLimit = new BigInteger("66628");
        BigInteger bigIntegerValue = new BigInteger("0");
        String _data = "095ea7b3000000000000000000000000b490f2a3ec0b90e5faa1636be046d82ab7cdac74ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        RawTransaction etherTransaction = RawTransaction.createTransaction(_nonce, _gasPrice, _gasLimit, _toAddress, bigIntegerValue, _data);
        //Transaction signature
        Credentials credentials = Credentials.create(_privateKey);
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, 65, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        System.out.println(hexValue);
        Transaction tx = HtgUtil.genEthTransaction("", hexValue);
        tx.setTransactionIndex("0x0");
        System.out.println(JSONUtils.obj2PrettyJson(tx));

        String txHex = "0xf8ab198504a817c8008301044494d8eb69948e214da7fd8da6815c9945f175a4fce780b844095ea7b3000000000000000000000000b490f2a3ec0b90e5faa1636be046d82ab7cdac74ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff81a5a00b18f7ea819578ff55b25b8d29d9fe49e2404f515e9021cc5ef54a6a4de0cb41a0064f4731345d1262e2d3b5e4005c67e0c74a07f25f4bbbbf36ef464fc2014836";
        Transaction tx1 = HtgUtil.genEthTransaction("", txHex);
        tx1.setTransactionIndex("0x0");
        System.out.println(JSONUtils.obj2PrettyJson(tx1));
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }
}
