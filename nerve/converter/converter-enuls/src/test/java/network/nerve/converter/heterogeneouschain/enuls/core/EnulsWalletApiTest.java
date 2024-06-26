package network.nerve.converter.heterogeneouschain.enuls.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.enuls.base.Base;
import network.nerve.converter.heterogeneouschain.enuls.context.EnulsContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.ethereum.core.BlockHeader;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class EnulsWalletApiTest extends Base {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;

    static String USDT_ENULS = "0x5045b6a04AC33f8D16d47E46b971C848141eE270";
    static String USD18_ENULS = "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9";
    static String NVT_ENULS_MINTER = "0x03Cf96223BD413eb7777AFE3cdb689e7E851CB32";

    protected void setErc20USDT() {
        erc20Address = USDT_ENULS;
        erc20Decimals = 6;
    }
    protected void setErc20USD18() {
        erc20Address = USD18_ENULS;
        erc20Decimals = 18;
    }
    protected void setErc20NVT() {
        erc20Address = NVT_ENULS_MINTER;
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
            String path = new File(EnulsWalletApiTest.class.getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
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
        this.multySignContractAddress = "0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69";
        init();
    }

    protected void setDev() {
        // ["0xbe7fbb51979e0b0b70c48284e895e228290d9f73", "0xcb1fa0c0b7b4d57848bddaa4276ce0776a3215d2", "0x54103606d9fcdb40539d06344c8f8c6367ffc9b8"]
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

        this.multySignContractAddress = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
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
        this.multySignContractAddress = "0xE1F5Ae8aC67878B0D07315BDE36F004494351d67";
        // spare: 0xB29A26df2702B10BFbCf8cd52914Ad1fc99A4540
        init();
    }
    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        this.multySignContractAddress = "0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69";
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
        BigInteger totalSupply = this.erc20TotalSupply("0xb13bB925D62Adc0ea0DA95f70e7f7a09EFFD4f9E");
        System.out.println(totalSupply);
        BigInteger decimals = this.erc20Decimals("0xb13bB925D62Adc0ea0DA95f70e7f7a09EFFD4f9E");
        System.out.println(decimals);
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

    private BigInteger erc20Decimals(String contract) {
        try {
            Function decimals = new Function(
                    "decimals",
                    List.of(),
                    List.of(new TypeReference<Uint8>() {}));
            List<Type> typeList = htgWalletApi.callViewFunction(contract, decimals);
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
        String sendAmount = "1";
        for (String to : tos) {
            String txHash = htgWalletApi.sendMainAssetForTestCase(from, fromPriKey, to, new BigDecimal(sendAmount), BigInteger.valueOf(100000L), gasPrice);
            System.out.println(String.format("towards[%s]Transfer%sindividualMainAsset, transactionhash: %s", to, sendAmount, txHash));
        }
    }

    protected void setAccount_3012() {
        from = "0xf173805F1e3fE6239223B17F0807596Edc283012";
        fromPriKey = "d15fdd6030ab81cee6b519645f0c8b758d112cd322960ee910f65ad7dbb03c2b";
    }
    /**
     * Transfer inerc20
     */
    @Test
    public void transferERC20() throws Exception {
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_3012();
        // ERC20
        //setErc20USD18();
        setErc20USDT();
        String tokenAddress = erc20Address;
        int tokenDecimals = erc20Decimals;
        String tokenAmount = "100000000";
        String to = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        EthSendTransaction token = htgWalletApi.transferERC20TokenForTestCase(from, to, new BigDecimal(tokenAmount).movePointRight(tokenDecimals).toBigInteger(), fromPriKey, tokenAddress);
        System.out.println(String.format("towards[%s]Transfer%sindividualERC20(USDT), transactionhash: %s", to, tokenAmount, token.getTransactionHash()));

        setErc20USD18();
        tokenAddress = erc20Address;
        tokenDecimals = erc20Decimals;
        tokenAmount = "100000000";
        to = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        token = htgWalletApi.transferERC20TokenForTestCase(from, to, new BigDecimal(tokenAmount).movePointRight(tokenDecimals).toBigInteger(), fromPriKey, tokenAddress);
        System.out.println(String.format("towards[%s]Transfer%sindividualERC20(USDT), transactionhash: %s", to, tokenAmount, token.getTransactionHash()));
    }

    /**
     * New way of rechargingeth
     */
    @Test
    public void depositMainAssetByCrossOut() throws Exception {
        //setMainData();
        setLocalTest();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        //fromPriKey = "???";
        //from = Credentials.create(fromPriKey).getAddress();
        // MainAssetquantity
        String sendAmount = "0.123456";
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
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
    /**
     * New way of rechargingerc20
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        setLocalNewTest();
        //setBeta();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_5996();
        // ERC20 Transfer quantity
        String sendAmount = "20000000";// 1.123456, 1234.123456789123456789
        // initialization ERC20 Address information
        //setErc20USDT();
        //setErc20NVT();
        setErc20USD18();
        erc20Address = "0x50074F4Bc4bC955622b49de16Fc6E3C1c73afBcA";
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {}));

        BigInteger allowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20authorization
            String approveAmount = "99999999";
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("erc20Authorization recharge[%s], authorizationhash: %s", approveAmount, authHash));
            while (htgWalletApi.getTxReceipt(authHash) == null) {
                System.out.println("wait for 8 Second query[ERC20authorization]Transaction packaging results");
                TimeUnit.SECONDS.sleep(8);
            }
            TimeUnit.SECONDS.sleep(8);
            BigInteger tempAllowanceAmount = (BigInteger) htgWalletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("wait for 8 Second query[ERC20authorization]Transaction limit");
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

    @Test
    public void depositByCrossOutIITest() throws Exception {
        //setLocalTest();
        setBeta();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        // fee
        String mainValue = "0.15";
        // ERC20 Transfer quantity
        String sendAmount = "2.15";
        // initialization ERC20 Address information
        setErc20NVT();erc20Address="0x6D948B9Dd21f4bEB14edDd19aa8Fa94d32F02D15";
        // Nerve Receiving address
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).movePointRight(erc20Decimals).toBigInteger();
        BigInteger mainValueBi = new BigDecimal(mainValue).movePointRight(18).toBigInteger();
        Function crossOutFunction = HtgUtil.getCrossOutIIFunction(to, convertAmount, erc20Address, null);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT, mainValueBi, multySignContractAddress);

        System.out.println(String.format("CrossOutII[%s], transactionhash: %s", sendAmount, hash));
    }

    @Test
    public void crossOutIIEventTest() throws Exception {
        String data = "0x0000000000000000000000003083f7ed267dca41338de3401c4e054db2a1cd2f00000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000003b0233800000000000000000000000006d948b9dd21f4beb14eddd19aa8fa94d32f02d1500000000000000000000000000000000000000000000000131fd12c7cba2000000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000025544e56546454535046506f76327842414d52536e6e664577586a4544545641415346456836000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002465396465623031342d653336382d346662382d396437312d32366431346364363730633500000000000000000000000000000000000000000000000000000000";
        List<Object> eventResult = HtgUtil.parseEvent(data, HtgConstant.EVENT_CROSS_OUT_II_FUNDS);
        System.out.println(JSONUtils.obj2PrettyJson(eventResult));
        System.out.println(Numeric.toHexString((byte[]) eventResult.get(5)));
        System.out.println(new String((byte[]) eventResult.get(5), "UTF8"));
    }

    @Test
    public void getBalance() throws Exception {
        setLocalTest();
        setAccount_EFa1();
        //balancePrint(from, 18);
        //balancePrint(multySignContractAddress, 18);
        balancePrint("0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69", 18);
        balancePrint("0x72DD54f620C62A44032B8A81A182e5a16ab3465d", 18);
        //erc20BalancePrint("NVT", from, NVT_ENULS_MINTER, 8);
        //erc20BalancePrint("USDT", from, USDT_ENULS, 6);
    }

    protected void balancePrint(String address, int decimals) throws Exception {
        BigDecimal balance = htgWalletApi.getBalance(address);
        String result = balance.movePointLeft(decimals).stripTrailingZeros().toPlainString();
        System.out.println(String.format("address: %s, balance: %s", address, result));
    }

    protected void erc20BalancePrint(String desc, String address, String erc20, int decimals) throws Exception {
        BigInteger erc20Balance = htgWalletApi.getERC20Balance(address, erc20);
        String result = new BigDecimal(erc20Balance).movePointLeft(decimals).stripTrailingZeros().toPlainString();
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
    public void removeContractManager() throws Exception {
        setLocalTest();
        // 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65,0xd29E172537A3FB133f790EBE57aCe8221CB8024F,0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
        String txKey = "bbbf300000000000000000000000000000000000000000000000000000000001";
        String[] adds = new String[]{};
        String[] removes = new String[]{
                "0xc753d679f58b6111cc7df52b57217a0d81ff725c"
        };
        int txCount = 1;
        int signCount = list.size();
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    protected void setMainData() {
        setMain();
        // "0xd87f2ad3ef011817319fd25454fc186ca71b3b56"
        // "0x0eb9e4427a0af1fa457230bef3481d028488363e"
        // "0xd6946039519bccc0b302f89493bec60f4f0b4610"
        list = new ArrayList<>();
        list.add("???");// Public key: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("");// Public key: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("");// Public key: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        this.multySignContractAddress = "0x3758AA66caD9F2606F1F501c9CB31b94b713A6d5";
        init();
    }

    @Test
    public void sendChangeBySignDataTest() throws Exception {
        setMainData();
        String txKey = "34658b00d940b1a0bf9b9ab131405296d60b95a6556b69d79bdea4962dec6585";
        String[] adds = new String[]{"0x7c4b783a0101359590e6153df3b58c7fe24ea468"};
        String[] removes = new String[]{"0x10c17be7b6d3e1f424111c8bddf221c9557728b0"};
        int txCount = 1;
        String signData = "d9affac4d232c5933c55bbac9e5b2af32ba40c184ee1daf0a6276b78c07badb64cc40e30ca0dedc27f3c2940f76d149b765884688b82bfc9b1d10d6d6e431de11bc9eeda019d3229c785333cf0e4afb146ad967d806f2f144181e24269ad2e17ea4272e537292a7bd80c9f7a0f1288358e01ebe87e48ce8d39570b6b40c3ef94b51bedf9b60ab51515133b804ad22cb8ce999f40eebf4d5830bfe7939eb5b08173a12b6f5a7f7f0ef1d775f2eba808fd8a47b2db72e71ab8777ef74ee770e4e530e41ce4d5ab7eae33689a0e91fb9f15bbdcd5f74d0ebbad43e2aaad5145e3f9af53100274ded435fb4f4b4f603b12226f1785847d75382be4402ec5ba5a38888760811be5d369220c91b6e7e3b197bd0d53436421fee710f292636aca2cd12a9259fe4676aa4e44d159984078b90e613b9dff93d7547d9249a722e27c7cc898e2c3a5991b50978938cbf9478fa0b58bb6adabbdc50f8e318d483ef66f2f1589f376233f913d3ba2dcf0e0a2fe17c09d354e8532dafdaefe519b4cb8c4f4890e31c567b9a01bb2b4be6e00b11d6aefc804321073dc8f4cf5fbe29f4b4d28a677a844840682bf35c9491c4fde278843dfa670183b13cd9a172050684d2923b4fd3abc7a377b111c185878726eaee87da6d3fda81639f14f74a54cec481c89cf27b60f22605a2f0574383777536ad72c8209a00b5722ea5906bdac42feee4ea49ea4d7c57bcdf81d1bf3e3735fbadc6566ad71ddd5cc7ad126aca2cc423928b9e7c2d595173229b80217cee3358ab259878e66b5cc9bce768d208cbbf8404dd32df25c044323f250551b36e257d5b28200f37de5a221e5afa28018ad6589c61ac2169d21f8e25e9842cf5e781996592eb917de15dd6606a4849305c117d0bce459066e0fae89612e8e901b";
        String hash = this.sendChangeWithSignData(txKey, adds, txCount, removes, signData);

        System.out.println(String.format("Administrator added%sRemove%sPieces,hash: %s", adds.length, removes.length, hash));
    }

    /**
     * Add N Administrators
     */
    @Test
    public void managerAdd() throws Exception {
        setDev();
        // GasPriceprepare
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
        System.out.println(String.format("Administrator added%sRemove%sPieces,%sSignatures,hash: %s", adds.length, removes.length, signCount, hash));
    }

    @Test
    public void allContractManagerSet() throws Exception {
        //setBeta();
        setMainData();
        //setLocalTest();
        boolean queryBalance = false;
        System.out.println("Please wait for the current list of contract administrators to be queried……");
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

    @Test
    public void upgradeContractTest() throws Exception {
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
        String newContract = "0x4f99f4Dd49B615b924DBD34E8FCdff190c6BeF46";
        String hash = this.sendUpgrade(txKey, newContract, signCount);
        System.out.println(String.format("Contract upgrade: %s,%sSignatures,hash: %s", newContract, signCount, hash));
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
        setDev();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        String txKey = "ccc0000000000000000000000000000000000000000000000000000000000001";
        //String[] adds = new String[]{"0x7dc432b48d813b2579a118e5a0d2fee744ac8e02"};
        //String[] removes = new String[]{"0x5e57d62ab168cd69e0808a73813fbf64622b3dfd"};
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
     * erc20Withdrawal, multiple signatures
     */
    @Test
    public void erc20WithdrawByManyManagers() throws Exception {
        setDev();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        setErc20USDT();
        String txKey = "fff0000000000000000000000000000000000000000000000000000000000004";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "43211234";
        String erc20 = erc20Address;
        int tokenDecimals = erc20Decimals;
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
        setMainProxy();
        //0x131fd12c7cba20000
        // Directly callingerc20contract
        String directTxHash = "0x5025cbc3038fa84066935fb5e244b4291999ace2da2c2d48318265c55ae789ba";
        Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
        System.out.println(JSONUtils.obj2PrettyJson(tx));
    }

    @Test
    public void getTxReceipt() throws Exception {
        setMainProxy();
        String directTxHash = "0x5025cbc3038fa84066935fb5e244b4291999ace2da2c2d48318265c55ae789ba";
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
        System.out.println(txReceipt);
    }

    @Test
    public void getNonce() throws Exception {
        setMainProxy();
        System.out.println(htgWalletApi.getLatestNonce("0xDCA09A08e961Ca8268bB08051E8AAB075e2861aF"));
    }

    @Test
    public void getCurrentGasPrice() throws IOException {
        setMainProxy();
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
    public void transferOverride() throws Exception {
        BigInteger _nonce = null, _gasPrice = null, _gasLimit = null, value = BigInteger.ZERO;
        String _privateKey = null;
        String rpcUrl = "";
        Credentials credentials = Credentials.create(_privateKey);
        String _toAddress = credentials.getAddress();
        RawTransaction etherTransaction = RawTransaction.createEtherTransaction(_nonce, _gasPrice, _gasLimit, _toAddress, value);
        //Transaction signature
        byte[] signedMessage = TransactionEncoder.signMessage(etherTransaction, htgContext.getConfig().getChainIdOnHtgNetwork(), credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        web3j.shutdown();
        if (ethSendTransaction == null || ethSendTransaction.getResult() == null) {
            if (ethSendTransaction != null && ethSendTransaction.getError() != null) {
                System.out.println(String.format("Failed to transfer, error: %s", ethSendTransaction.getError().getMessage()));
            } else {
                System.out.println("Failed to transfer");
            }
            return;
        }
        String hash = ethSendTransaction.getTransactionHash();
        System.out.println(hash);
    }

    @Test
    public void approveZeroTest() throws Exception {
        setBeta();
        htgContext.setEthGasPrice(htgWalletApi.getCurrentGasPrice());
        // initialization account
        setAccount_EFa1();
        // initialization ERC20 Address information
        setErc20USDT();
        // erc20authorization
        String approveAmount = "0";
        Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
        String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
        System.out.println(String.format("erc20authorization[%s], authorizationhash: %s", approveAmount, authHash));
    }

    @Test
    public void newValidationEthDepositByCrossOutTest() throws Exception {
        setLocalTest();
        List<String> list = new ArrayList<>();
        list.add("0xf63d0a95eda1fe09531784cd11037c43c41af7a92cb72689dcd5e4f4a18228d0");
        for (String directTxHash : list) {
            Transaction tx = htgWalletApi.getTransactionByHash(directTxHash);
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(directTxHash);
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            HtgParseTxHelper helper = new HtgParseTxHelper();
            BeanUtilTest.setBean(helper, "htgContext", new MockEnulsContext());
            BeanUtilTest.setBean(helper, "htgERC20Helper", new MockHtgERC20Helper());
            Method method = helper.getClass().getDeclaredMethod("validationEthDepositByCrossOutII", Transaction.class, TransactionReceipt.class, HeterogeneousTransactionInfo.class);
            method.setAccessible(true);
            boolean invoke = (boolean) method.invoke(helper, tx, txReceipt, po);
            System.out.println(invoke ? "true, amount: " + po.getValue() : "false");
        }
    }

    @Test
    public void crossOutEstimateGasTest() throws Exception {
        setMain();
        String contractAddress = "0x72ceb9f24c70ce436918e1f058b9d576354a2958";
        String encodedFunction = "0xd98eea60000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000006427269646765000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034252470000000000000000000000000000000000000000000000000000000000";

        BigInteger value = null;
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0xe752ab620d04357cff84c74028da4155521e0500",
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
        String sender = "0xcC81d3B057c16DFfe778D2d342CfF40d33bD69A7";
        String contractAddress = "0x214Ce4BF95c894aac9b991f4378bDaF8DEd28065";
        BigInteger convertAmount = new BigDecimal("0.01").movePointRight(2).toBigInteger();
        String from = "0x267569C6893EA7eFF9Da3ac120859c49a1EE485A";
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
        String contractAddress = "0x72743a08215dd9b8be0e9d0933c4f55176ace254";
        String encodedFunction = "0x6a62784200000000000000000000000017bf829ca1a476144b8c30e84e29d7bc08a7675c";

        BigInteger value = new BigDecimal("1").movePointRight(18).toBigInteger();
        org.web3j.protocol.core.methods.request.Transaction tx = new org.web3j.protocol.core.methods.request.Transaction(
                "0x17bf829ca1a476144b8c30e84e29d7bc08a7675c",
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
    public void verifyBlockSign() throws Exception {
        setMain();
        EthBlock.Block _header = htgWalletApi.getBlockHeaderByHeight(3031716l);
        String minter = takeBlockMinter(_header);
        System.out.println(String.format("minter address: %s", minter));
    }

    public String takeBlockMinter(EthBlock.Block _header) {
        List<RlpType> result = new ArrayList<>();
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getParentHash())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getSha3Uncles())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getMiner())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getStateRoot())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getTransactionsRoot())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getReceiptsRoot())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getLogsBloom())));
        result.add(RlpString.create(_header.getDifficulty()));
        result.add(RlpString.create(_header.getNumber()));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getGasLimitRaw())));
        result.add(RlpString.create(_header.getGasUsed()));
        result.add(RlpString.create(_header.getTimestamp()));
        String subExtraData = _header.getExtraData().substring(0, _header.getExtraData().length() - 130);
        result.add(RlpString.create(Numeric.hexStringToByteArray(subExtraData)));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getMixHash())));
        result.add(RlpString.create(Numeric.hexStringToByteArray(_header.getNonceRaw())));
        RlpList rlpList = new RlpList(result);
        byte[] encode = RlpEncoder.encode(rlpList);
        byte[] hashBytes = Hash.sha3(encode);
        String sign = _header.getExtraData().substring(_header.getExtraData().length() - 130);
        String r = "0x" + sign.substring(0, 64);
        String s = "0x" + sign.substring(64, 128);
        long v = new BigInteger(sign.substring(128), 16).longValue();
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity(r), Numeric.decodeQuantity(s));
        BigInteger publicKey = Sign.recoverFromSignature((int) v, signature, hashBytes);
        if (publicKey != null) {
            return "0x" + Keys.getAddress(publicKey);
        }
        return null;
    }

    static class MockHtgERC20Helper extends HtgERC20Helper {
        @Override
        public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
            return true;
        }
    }

    static class MockEnulsContext extends EnulsContext {

        HeterogeneousCfg cfg;

        MockEnulsContext() {
            cfg = new HeterogeneousCfg();
            cfg.setDecimals(18);
        }

        @Override
        public NulsLogger logger() {
            return Log.BASIC_LOGGER;
        }

        @Override
        public String MULTY_SIGN_ADDRESS() {
            return "0x56f175d48211e7d018dda7f0a0b51bcfb405ae69";
        }

        @Override
        public HeterogeneousCfg getConfig() {
            return cfg;
        }

        @Override
        public byte VERSION() {
            return 3;
        }
    }
}
