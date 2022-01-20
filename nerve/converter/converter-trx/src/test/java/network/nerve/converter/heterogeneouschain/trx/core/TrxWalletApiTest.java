package network.nerve.converter.heterogeneouschain.trx.core;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.trx.base.Base;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.model.TRC20TransferEvent;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.model.TrxSendTransactionPo;
import network.nerve.converter.heterogeneouschain.trx.model.TrxTransaction;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import org.junit.Before;
import org.junit.Test;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Bool;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.api.GrpcAPI;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.crypto.SECP256K1;
import org.tron.trident.crypto.tuwenitypes.Bytes32;
import org.tron.trident.crypto.tuwenitypes.MutableBytes;
import org.tron.trident.crypto.tuwenitypes.UInt256;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Convert;
import org.tron.trident.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.*;
import static network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil.*;


public class TrxWalletApiTest extends Base {

    protected String from;
    protected String fromPriKey;
    protected String erc20Address;
    protected int erc20Decimals;
    protected SECP256K1.KeyPair keyPair;
    protected int nerveChainId = 5;

    protected void setErc20USDTMain() {
        // 0x370dd53139e0d8923f9feaf1989344ec64f6ff6d
        erc20Address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
        erc20Decimals = 6;
    }

    protected void setErc20DX() {
        // 0x370dd53139e0d8923f9feaf1989344ec64f6ff6d
        erc20Address = "TEzJjjC4NrLrYFthGFHzQon5zrErNw1JN9";
        erc20Decimals = 6;
    }

    protected void setErc20K18N() {
        // 0xe10095ee56d7181d59b49f3cbdd596945fd2d68d
        erc20Address = "TWUujuxBNCGPZNiyySfYuBJgFumrvS2iRd";
        erc20Decimals = 18;
    }

    protected void setErc20USDT() {
        erc20Address = "TXCWs4vtLW2wYFHfi7xWeiC9Kuj2jxpKqJ";
        erc20Decimals = 6;
    }

    /** FortuneCai (FCI) */
    protected void setErc20FCI() {
        // 0x404ced5e5614488129c26999627416f96fdc7fd9
        erc20Address = "TFqCQLGxG2o188eESoYkr1Ji9x85SEXBDP";
        erc20Decimals = 18;
    }

    protected void setErc20QOP() {
        setNile();
        // 0xf59beb9623a105f666698d08a8f96c2ebdc88fa2
        erc20Address = "TYMsLazzLMWCtDamLDLkYtiV6uXsTxFwdr";
        erc20Decimals = 8;
    }

    protected void setM2() {
        this.from = "TFzEXjcejyAdfLSEANordcppsxeGW9jEm2";
        this.fromPriKey = "30002e81d449f16b69bc3e06918ff6ff088863edef8a0ba3d9b06fe5d02744d7";
    }

    protected void setPM() {
        // 0x43a0eca8a75c86f30045a434114d750eb1b4b6e0
        this.from = "TG8o48ycgUCB7UJd46cSnxSJybWwTHmRpm";
        this.fromPriKey = "d8fd23d961076b3616078ff235c4018c6113f3811ed97109e925f7232986b583";
    }

    protected void setUX() {
        this.from = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        this.fromPriKey = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    }

    protected void setDev() {
        list = new ArrayList<>();
        list.add("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5");// 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
        list.add("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f");// 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
        list.add("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499");// 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe

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

        this.priKey = list.get(0);
        this.address = new KeyPair(priKey).toBase58CheckAddress();

        setM2();
        this.multySignContractAddress = "TMSQg3nMPGJmeXPQzs3aEUHrw6Jk4Gva1s";
        keyPair = createPair(fromPriKey);
    }

    protected void setBeta() {
        list = new ArrayList<>();
        list.add("978c643313a0a5473bf65da5708766dafc1cca22613a2480d0197dc99183bb09");// 0x1a9f8b818a73b0f9fde200cd88c42b626d2661cd
        list.add("6e905a55d622d43c499fa844c05db46859aed9bb525794e2451590367e202492");// 0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac
        list.add("d48b870f2cf83a739a134cd19015ed96d377f9bc9e6a41108ac82daaca5465cf");// 0x3c2ff003ff996836d39601ca22394a58ca9c473b
        this.multySignContractAddress = "TWajcnpyyZLRtLkFd6p4ZAMn5y4GpDa6MB";
        this.priKey = list.get(0);
        this.address = new KeyPair(priKey).toBase58CheckAddress();

    }

    @Before
    public void before() {
        setDev();
    }

    /**
     * 主资产提现
     */
    @Test
    public void mainWithdrawBy10Managers() throws Exception {
        String txKey = "eee0000000000000000000000000000000000000000000000000000000000005";
        // 0xc11d9943805e56b630a401d4bd9a29550353efa1 ::::::::: TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX
        String toAddress = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";// TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX
        String value = "0.03";
        int signCount = 10;
        String hash = this.sendMainAssetWithdraw(txKey, toAddress, value, signCount);
        System.out.println(String.format("MainAsset提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * 授权清零
     */
    @Test
    public void cleanAllowance() throws Exception {
        setUX();
        setErc20DX();
        // 清零授权
        String approveAmount = "0";
        Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
        String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
        System.out.println(String.format("TRC20授权清零, 授权hash: %s", authHash));

    }

    @Test
    public void upgradeFunctionTest() {
        String nerveTxHash = "aaa";
        String upgradeContract = "TVAhzC3rBLy6XLceHfha3ddiVHTUpUovyY";
        String signatureData = "ccc";
        Function function = getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
        System.out.println(function);

        ByteString address0 = ApiWrapper.parseAddress("0x41d2972934f5cdcf9beef75e5884ab61ab2dfdf533");
        ByteString address1 = ApiWrapper.parseAddress("TVAhzC3rBLy6XLceHfha3ddiVHTUpUovyY");
        System.out.println(Numeric.toHexString(address0.toByteArray()));
        System.out.println(Numeric.toHexString(address1.toByteArray()));
        System.out.println();
        System.out.println(address0.toString());
        System.out.println(address1.toString());
        System.out.println();
        System.out.println(address0.toStringUtf8());
        System.out.println(address1.toStringUtf8());
    }

    /**
     * 主资产充值
     */
    @Test
    public void depositTRXByCrossOut() throws Exception {
        this.multySignContractAddress = "TQeQaQiXnBFRnW2HYxPFjE5FMkkCxaa7eL";
        setUX();
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        String value = "2.2";
        BigInteger valueBig = TrxUtil.convertTrxToSun(new BigDecimal(value));
        String erc20 = TrxConstant.ZERO_ADDRESS_TRX;
        Function function = getCrossOutFunction(to, valueBig, erc20);
        BigInteger feeLimit = TRX_100;
        TrxSendTransactionPo callContract = walletApi.callContract(from, fromPriKey, multySignContractAddress, feeLimit, function, valueBig);
        System.out.println(callContract.toString());
    }
    /**
     * TRC20充值
     */
    @Test
    public void depositERC20ByCrossOut() throws Exception {
        this.multySignContractAddress = "TQeQaQiXnBFRnW2HYxPFjE5FMkkCxaa7eL";
        setUX();
        setErc20USDT();
        //setErc20DX();
        // ERC20 转账数量
        String sendAmount = "3.3";
        // Nerve 接收地址
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";

        BigInteger convertAmount = new BigDecimal(sendAmount).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger();
        Function allowanceFunction = new Function("allowance",
                Arrays.asList(new Address(from), new Address(multySignContractAddress)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));

        BigInteger allowanceAmount = (BigInteger) walletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
        if (allowanceAmount.compareTo(convertAmount) < 0) {
            // erc20授权
            String approveAmount = "99999999";
            Function approveFunction = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(approveAmount).multiply(BigInteger.TEN.pow(erc20Decimals)));
            String authHash = this.sendTx(from, fromPriKey, approveFunction, HeterogeneousChainTxType.DEPOSIT, null, erc20Address);
            System.out.println(String.format("TRC20授权充值[%s], 授权hash: %s", approveAmount, authHash));
            while (walletApi.getTransactionReceipt(authHash) == null) {
                System.out.println("等待8秒查询[TRC20授权]交易打包结果");
                TimeUnit.SECONDS.sleep(8);
            }
            //TimeUnit.SECONDS.sleep(8);
            BigInteger tempAllowanceAmount = (BigInteger) walletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            while (tempAllowanceAmount.compareTo(convertAmount) < 0) {
                System.out.println("等待8秒查询[TRC20授权]交易额度");
                TimeUnit.SECONDS.sleep(8);
                tempAllowanceAmount = (BigInteger) walletApi.callViewFunction(erc20Address, allowanceFunction).get(0).getValue();
            }
        }
        System.out.println("[TRC20授权]额度已满足条件");
        // crossOut erc20转账
        Function crossOutFunction = TrxUtil.getCrossOutFunction(to, convertAmount, erc20Address);
        String hash = this.sendTx(from, fromPriKey, crossOutFunction, HeterogeneousChainTxType.DEPOSIT);
        System.out.println(String.format("TRC20充值[%s], 充值hash: %s", sendAmount, hash));
    }

    /**
     * TRC20提现
     */
    @Test
    public void trc20Withdraw() throws Exception {
        //setErc20DX();
        setMain();
        erc20Address = "TPZddNpQJHu8UtKPY1PYDBv2J5p5QpJ6XW";
        erc20Decimals = 6;
        String txKey = "fff0000000000000000000000000000000000000000000000000000000000000";
        String toAddress = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        String value = "20";
        int signCount = 10;
        String hash = this.sendTRC20Withdraw(txKey, toAddress, value, erc20Address, erc20Decimals, signCount);
        System.out.println(String.format("TRC20提现%s个，%s个签名，hash: %s", value, signCount, hash));
    }

    /**
     * TRX转账
     */
    @Test
    public void transferTrx() throws Exception {
        setUX();
        String to = multySignContractAddress;
        to = TrxUtil.ethAddress2trx(to);
        String value = "1.1";
        TrxSendTransactionPo trx = walletApi.transferTrx(from, to, convertTrxToSun(new BigDecimal(value)), fromPriKey);
        System.out.println(trx.getTxHash());
    }

    /**
     * TRX转账（触发callback）
     */
    @Test
    public void transferTrxForCallback() throws Exception {
        setUX();
        String to = multySignContractAddress;
        String value = "2";

        BigInteger valueBig = TrxUtil.convertTrxToSun(new BigDecimal(value));
        Function function = new Function(EMPTY_STRING, List.of(), List.of());
        BigInteger feeLimit = TRX_100;
        TrxSendTransactionPo callContract = walletApi.callContract(from, fromPriKey, to, feeLimit, function, valueBig);
        System.out.println(callContract.toString());
    }

    /**
     * TRC20转账
     */
    @Test
    public void transferTRC20() throws Exception {
        //setM2();
        setUX();

        //setErc20FCI();
        //setErc20DX();
        setErc20USDT();
        String to = "TFzEXjcejyAdfLSEANordcppsxeGW9jEm2";
        String value = "1";
        // 估算feeLimit
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(new BigInteger(value).multiply(BigInteger.TEN.pow(erc20Decimals)))),
                Arrays.asList(new TypeReference<Type>() {}));
        TrxEstimateSun estimateSun = walletApi.estimateSunUsed(from, erc20Address, function);
        if (estimateSun.isReverted()) {
            System.err.println(String.format("交易验证失败，原因: %s", estimateSun.getRevertReason()));
            return;
        }

        BigInteger feeLimit = TRX_20;
        if (estimateSun.getSunUsed() > 0) {
            feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
        }
        TrxSendTransactionPo trx = walletApi.transferTRC20Token(
                from, to, new BigInteger(value).multiply(BigInteger.TEN.pow(erc20Decimals)), fromPriKey, erc20Address, feeLimit);
        System.out.println(trx.getTxHash());
    }

    /**
     * 估算feeLimit
     */
    @Test
    public void estimateSun() throws Exception {
        //setNile();
        //setM2();
        //setErc20FCI();
        //setErc20DX();
        setMain();
        from = "TYmgxoiPetfE2pVWur9xp7evW4AuZCzfBm";
        setErc20USDTMain();
        String to = "THmbMWg4XrFpPWQKUojF1Hh9KjVmjXQTNX";
        String value = "1324.98";

        Function function = new Function("transfer", Arrays.asList(new Address(to), new Uint256(new BigDecimal(value).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger())), Arrays.asList(new TypeReference<Type>() {}));

        //Function function = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(value).multiply(BigInteger.TEN.pow(erc20Decimals)));

        Response.TransactionExtention call = wrapper.constantCall(from, erc20Address, function);
        System.out.println(call.toString());
        System.out.println("====================================================================================");
        String resultValue = Numeric.toHexString(call.getConstantResult(0).toByteArray());
        System.out.println(TrxUtil.getRevertReason(resultValue));
        System.out.println("====================================================================================");
        System.out.println(call.getResult());
        long energyUsed = 0;
        do {
            Map<Integer, UnknownFieldSet.Field> fieldMap = call.getUnknownFields().asMap();
            if (fieldMap == null || fieldMap.size() == 0) {
                break;
            }
            UnknownFieldSet.Field field = fieldMap.get(5);
            if (field == null) {
                break;
            }
            List<Long> longList = field.getVarintList();
            if (longList == null || longList.isEmpty()) {
                break;
            }
            energyUsed = longList.get(0);
        } while (false);
        System.out.println(String.format("energyUsed: %s", energyUsed));
    }

    /**
     * 估算feeLimit
     */
    @Test
    public void viewFunctionCall() throws Exception {
        setMain();
        from = "TMLMSuyygN1fL5HpUt1oQp3RjvdEsHZffG";
        String erc20Address = "TPZddNpQJHu8UtKPY1PYDBv2J5p5QpJ6XW";
        int erc20Decimals = 6;
        String to = "TMZBDFxu5WE8VwYSj2p3vVuBxxKMSqZDc8";
        String value = "0.018";

        Function function = new Function("transfer",
                Arrays.asList(new Address(to), new Uint256(new BigDecimal(value).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger())),
                Arrays.asList(new TypeReference<Bool>() {}));

        List<Type> valueTypes = walletApi.callViewFunction(erc20Address, function);
        System.out.println(valueTypes.get(0).getValue().toString());
        //System.out.println(String.format("valueTypes: %s", Arrays.deepToString(valueTypes.toArray())));
    }

    @Test
    public void withdrawEstimateSun() throws Exception {
        //setNile();
        //setM2();
        //setErc20FCI();
        //setErc20DX();
        setMain();
        from = "TVXoMStg9Cmh2gMU4eBcZyS2Cr8Py99Wiz";
        //setErc20USDTMain();
        //String to = "THmbMWg4XrFpPWQKUojF1Hh9KjVmjXQTNX";
        //String value = "1324.98";

        //Function function = new Function("transfer", Arrays.asList(new Address(to), new Uint256(new BigDecimal(value).multiply(BigDecimal.TEN.pow(erc20Decimals)).toBigInteger())), Arrays.asList(new TypeReference<Type>() {}));

        Function function = TrxUtil.getCreateOrSignWithdrawFunction(
                "b0a3f4e0f7f28b6d55ced8f333e63f0844c25f061b8a843f8c49c6a0612ccd8d",
                "THmbMWg4XrFpPWQKUojF1Hh9KjVmjXQTNX",
                new BigInteger("1324980000"),
                true,
                "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                "44121a15536b8263820367883a3d96db2b1786e4de0987b501fa0ae1f60e2f4810c85d069be066f2816947a0134ce3cefdf1b4745fa1639c247532e11ab619fa1cb4f57f7a9fdf21ac45b74da672c2a8c774ac3fc34f76cabbd2a20ba3a69d3c9969187ae20f0e70004b206b86425d2594d83981e4b46eb28a32ce0d0bb8ddb8b51cdb270b34c92e2ac68c448d317db325202f222a7540ac59457893fcc725b0e65a4cdc23a8411c320faf59225e0ee83dda1af056ad075c7e1a5fafd9a89d30a5231b221b2850d3b8bf5c2f2c6f8efd4e6b2818c2840d84d704f3fa115b9d9d7f64306c66e85dba76272f90691db548da39b1e9b60b5090da4c48b916dc683f2b85991b1052d2954d0a7d40507ec1e530cd7a50e0834ac821f58b5e7d3e9e7f26df99bc65e1e479141b13c526d59b60a41c8205a74c1a29c58adbbcb4aa13de1313ccd01c02c57e6bc78a3830e9e11604474b6b1cb052c556610ada6ac2c961564202d7a74d51a5f0e81cd0e190b0e9eaa916850cfd43a8cdb76b014803888120a375a8351bfbaaace456e1e2a39c8b65f06ad5c8202f0fc50f03f7a243abbc9a1ad10af34d1ae14eb7ebdd0b3e6a1044c0394bfdff4261610b468eb5805ea39311a4070e1c1b0b1475fa000496a9cd5203119860b5bc9e70df850a75e9f43bdbeaa4d7c0e24b39fccee6e31d4ce4274c1d5161796a13abde394f0194817b6264bde59e09757f1c609d2ff527c7979df1d6129e183c38dee5ca950a2a62718b26f4468f55c5d64275b310ee6ae586a5883170f6e4cb1c4d9e65f7758055897f8290a79ff96e99881cef896c030af54afed95dbe111effd44408fa597d06eb674889972b07058743a76e3d48a0410fcf50e4757bd78fbc9e20f2d8b628bc01b97de77213f98a8036631b"
        );
        //Function function = this.getERC20ApproveFunction(multySignContractAddress, new BigInteger(value).multiply(BigInteger.TEN.pow(erc20Decimals)));

        Response.TransactionExtention call = wrapper.constantCall(from, "TYmgxoiPetfE2pVWur9xp7evW4AuZCzfBm", function);
        System.out.println(call.toString());
        System.out.println("====================================================================================");
        String resultValue = Numeric.toHexString(call.getConstantResult(0).toByteArray());
        System.out.println(TrxUtil.getRevertReason(resultValue));
        System.out.println("====================================================================================");
        System.out.println(call.getResult());
        long energyUsed = 0;
        do {
            Map<Integer, UnknownFieldSet.Field> fieldMap = call.getUnknownFields().asMap();
            if (fieldMap == null || fieldMap.size() == 0) {
                break;
            }
            UnknownFieldSet.Field field = fieldMap.get(5);
            if (field == null) {
                break;
            }
            List<Long> longList = field.getVarintList();
            if (longList == null || longList.isEmpty()) {
                break;
            }
            energyUsed = longList.get(0);
        } while (false);
        System.out.println(String.format("energyUsed: %s", energyUsed));
    }

    protected void setMainTest() {
        list = new ArrayList<>();
        list.add("???");// 公钥: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        list.add("???");// 公钥: 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        list.add("???");// 公钥: 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC

        this.multySignContractAddress = "TXeFBRKUW2x8ZYKPD13RuZDTd9qHbaPGEN";
        this.priKey = list.get(0);
        this.address = new KeyPair(priKey).toBase58CheckAddress();
    }
    /**
     * 管理员变更
     */
    @Test
    public void managerChange() throws Exception {
        setMain();
        setMainTest();
        String txKey = "aaa1024000000000000000000000000000000000000000000000000000000000";
        String[] adds = new String[]{
                "0xb12a6716624431730c3ef55f80c458371954fa52",
                "0x1f13e90daa9548defae45cd80c135c183558db1f",
                "0x66fb6d6df71bbbf1c247769ba955390710da40a5",
                "0x659ec06a7aedf09b3602e48d0c23cd3ed8623a88",
                "0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1",
                "0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d",
                "0xaff68cd458539a16b932748cf4bdd53bf196789f",
                "0xc8dcc24b09eed90185dbb1a5277fd0a389855dae",
                "0xa28035bb5082f5c00fa4d3efc4cb2e0645167444",
                "0x10c17be7b6d3e1f424111c8bddf221c9557728b0",
                "0x15cb37aa4d55d5a0090966bef534c89904841065",
                "0x17e61e0176ad8a88cac5f786ca0779de87b3043b"
        };
        String[] removes = new String[]{};
        int txCount = 1;
        int signCount = list.size();
        context.SET_VERSION((byte) 3);
        String hash = this.sendChange(txKey, adds, txCount, removes, signCount);
        System.out.println(String.format("管理员添加%s个，移除%s个，%s个签名，hash: %s", adds.length, removes.length, signCount, hash));
    }

    /**
     * 查询多签合约的所有管理员地址
     */
    @Test
    public void allManagers() {
        setMain();
        setMainTest();
        Function function = getAllManagersFunction();
        Response.TransactionExtention call = wrapper.constantCall(from, multySignContractAddress, function);
        byte[] bytes = call.getConstantResult(0).toByteArray();
        List<Type> list = FunctionReturnDecoder.decode(Numeric.toHexString(bytes), function.getOutputParameters());
        List<Address> addressList = (List<Address>) list.get(0).getValue();
        for (Address address : addressList) {
            String strAddress = address.toString();
            BigDecimal accountBalance = BigDecimal.valueOf(wrapper.getAccountBalance(strAddress));
            System.out.println(String.format("ethFormatAddress: %s, address: %s, balance: %s", TrxUtil.trxAddress2eth(strAddress), strAddress, Convert.fromSun(accountBalance, Convert.Unit.TRX)));
        }
    }

    @Test
    public void getTxAndParseInput() throws Exception {
        String txHash = "d1958721f6c9fb379866760542b2dfff4fb0897aeb03888790f7f1e037b912a5";
        Chain.Transaction tx = wrapper.getTransactionById(txHash);
        System.out.println(tx.getRawData().getContractCount());
        System.out.println(tx.toString());
        Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
        Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
        String input = Numeric.toHexString(tg.getData().toByteArray());
        List<Object> typeList = TrxUtil.parseInput(input, TrxConstant.INPUT_WITHDRAW);
        System.out.println(Arrays.deepToString(typeList.toArray()));
    }

    @Test
    public void getTxReceiptAndParseEvent() throws Exception {
        String txHash = "d1958721f6c9fb379866760542b2dfff4fb0897aeb03888790f7f1e037b912a5";
        Response.TransactionInfo txInfo = wrapper.getTransactionInfoById(txHash);
        List<Response.TransactionInfo.Log> logs = txInfo.getLogList();
        for (Response.TransactionInfo.Log log : logs) {
            String address = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
            String eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
            System.out.println(String.format("address: %s, topics: %s", address, eventHash));
            String data = Numeric.toHexString(log.getData().toByteArray());
            List<Object> objects = null;
            if (TrxConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED.equals(eventHash)) {
                objects = parseEvent(data, TrxConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
            } else if (TrxConstant.EVENT_HASH_TRANSFERFUNDS.equals(eventHash)) {
                objects = parseEvent(data, TrxConstant.EVENT_TRANSFER_FUNDS);
            }
            if (objects != null) {
                System.out.println(Arrays.deepToString(objects.toArray()));
            }

        }
    }

    @Test
    public void getTx() throws Exception {
        setMain();
        String txHash = "12c56780bb77f11ac7299e28dc6514a55eb02a2be22303b49a44bef6664c8e2f";
        Chain.Transaction tx = wrapper.getTransactionById(txHash);
        System.out.println(tx.getRet(0).getContractRet());
        System.out.println(tx.toString());
        System.out.println(TrxUtil.calcTxHash(tx));
        System.out.println(tx.getRet(0).getContractRet() == Chain.Transaction.Result.contractResult.SUCCESS);

        TrxTransaction txInfo = TrxUtil.generateTxInfo(tx);
        String trxTxHash = txInfo.getHash();
        String from = txInfo.getFrom(), to = txInfo.getTo(), input = txInfo.getInput();
        BigInteger value = txInfo.getValue();
        Chain.Transaction.Contract.ContractType type = txInfo.getType();
        if(StringUtils.isBlank(input)) {
            return;
        }
        input = Numeric.cleanHexPrefix(input);
        if (input.length() < 8) {
            return;
        }
        String methodHash;
        if ((methodHash = HEX_PREFIX + input.substring(0, 8)).equals(TrxConstant.METHOD_HASH_TRANSFER)) {
            try {
                List<Object> objects = TrxUtil.parseTRC20TransferInput(input);
                if (objects.isEmpty() || objects.size() != 2)
                    return;
                String toAddress = objects.get(0).toString();
                System.out.println(String.format("input: %s", input));
                // 接收地址不是监听的多签地址
                System.out.println(Arrays.toString(TrxUtil.parseInput(input, INPUT_TRC20_TRANSFER).toArray()));
                System.out.println(Arrays.toString(objects.toArray()));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(String.format("hash: %s, data: %s, old", trxTxHash, "error"));
                System.out.println(String.format("hash: %s, data: %s, new", trxTxHash, Arrays.toString(TrxUtil.parseTRC20TransferInput(input).toArray())));
                System.out.println();
            }

        }
    }

    @Test
    public void getTxReceiptAndRevertReason() throws Exception {
        String txHash = "dc1a63a5762a85befa8de4d243bb46cce42d41fcfb735413f2dbc0ead6405d3f";
        Response.TransactionInfo txInfo = wrapper.getTransactionInfoById(txHash);
        String s = Numeric.toHexString(txInfo.getContractResult(0).toByteArray());
        System.out.println(TrxUtil.getRevertReason(s));
    }

    @Test
    public void getTRC20TxAndParseInput() throws Exception {
        String txHash = "501c346a6c15885a6972f350aa522fd2f55f19b8302f860995b93770476d31a5";
        Chain.Transaction tx = wrapper.getTransactionById(txHash);
        System.out.println(tx.getRawData().getContractCount());
        System.out.println(tx.toString());
        Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
        Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
        String input = Numeric.toHexString(tg.getData().toByteArray());
        List<Object> typeList = TrxUtil.parseInput(input, TrxConstant.INPUT_TRC20_TRANSFER);
        System.out.println(Arrays.deepToString(typeList.toArray()));
    }

    @Test
    public void getTRC20TxReceiptAndParseEvent() throws Exception {
        String txHash = "501c346a6c15885a6972f350aa522fd2f55f19b8302f860995b93770476d31a5";
        Response.TransactionInfo txInfo = wrapper.getTransactionInfoById(txHash);
        List<Response.TransactionInfo.Log> logs = txInfo.getLogList();
        for (Response.TransactionInfo.Log log : logs) {
            String address = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
            String eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
            System.out.println(String.format("address: %s, topics: %s", address, eventHash));
            TRC20TransferEvent trc20Event = null;
            if (TrxConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                trc20Event = parseTRC20Event(log);
            } else {
                System.err.println("未知事件");
            }
            if (trc20Event != null) {
                System.out.println(trc20Event.toString());
            }

        }
    }

    @Test
    public void blockAnalysisTest() throws Exception {
        setMain();
        // 34882992L 34903358L
        Response.BlockExtention block = walletApi.getBlockByHeight(34903591L);
        List<Response.TransactionExtention> list = block.getTransactionsList();
        Chain.BlockHeader.rawOrBuilder header = block.getBlockHeader().getRawDataOrBuilder();
        long blockHeight = header.getNumber();
        int size;
        if (list != null && (size = list.size()) > 0) {
            String trxTxHash;
            long txTime = header.getTimestamp();
            for (int i = 0; i < size; i++) {
                Response.TransactionExtention txe = list.get(i);
                Chain.Transaction tx = txe.getTransaction();
                Chain.Transaction.raw txRawData = tx.getRawData();
                if (txRawData.getContractCount() == 0) {
                    continue;
                }

                TrxTransaction txInfo = TrxUtil.generateTxInfo(tx);
                // 过滤 非TRX转账和调用合约交易
                if (txInfo == null) {
                    continue;
                }
                trxTxHash = txInfo.getHash();
                String from = txInfo.getFrom(), to = txInfo.getTo(), input = txInfo.getInput();
                BigInteger value = txInfo.getValue();
                Chain.Transaction.Contract.ContractType type = txInfo.getType();
                //if ("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t".equals(to)) {
                    if(StringUtils.isBlank(input)) {
                        continue;
                    }
                    input = Numeric.cleanHexPrefix(input);
                    if (input.length() < 8) {
                        continue;
                    }
                    String methodHash;
                    if ((methodHash = HEX_PREFIX + input.substring(0, 8)).equals(TrxConstant.METHOD_HASH_TRANSFER)) {
                        try {
                            List<Object> objects = TrxUtil.parseTRC20TransferInput(input);
                            if (objects.isEmpty() || objects.size() != 2)
                                continue;
                            String toAddress = objects.get(0).toString();
                            BigInteger newValue = (BigInteger) objects.get(1);
                            List<Object> oldObjs = parseInput(input, INPUT_TRC20_TRANSFER);
                            BigInteger oldValue = (BigInteger) oldObjs.get(1);
                            System.out.println(String.format("hash: %s, contract: %s, data: %s, old", trxTxHash, to, Arrays.toString(oldObjs.toArray())));
                            System.out.println(String.format("hash: %s, contract: %s, data: %s, new", trxTxHash, to, Arrays.toString(objects.toArray())));
                            if (newValue.compareTo(oldValue) == 0) {
                                System.out.println("success");
                            } else {
                                System.err.println("===============================error===============================");
                            }
                            System.out.println();
                        } catch (Exception e) {
                            //e.printStackTrace();
                            System.err.println(String.format("hash: %s, contract: %s, data: %s, old", trxTxHash, to, "error"));
                            System.out.println(String.format("hash: %s, contract: %s, data: %s, new", trxTxHash, to, Arrays.toString(TrxUtil.parseTRC20TransferInput(input).toArray())));
                            System.out.println();
                        }

                    }
                if (methodHash.equals(TrxConstant.METHOD_HASH_TRANSFER_FROM)) {
                    try {
                        List<Object> objects = TrxUtil.parseTRC20TransferFromInput(input);
                        if (objects.isEmpty() || objects.size() != 3)
                            continue;
                        BigInteger newValue = (BigInteger) objects.get(2);
                        List<Object> oldObjs = parseInput(input, INPUT_TRC20_TRANSFER_FROM);
                        BigInteger oldValue = (BigInteger) oldObjs.get(2);
                        System.out.println(String.format("[transferFrom] hash: %s, contract: %s, data: %s, old", trxTxHash, to, Arrays.toString(oldObjs.toArray())));
                        System.out.println(String.format("[transferFrom] hash: %s, contract: %s, data: %s, new", trxTxHash, to, Arrays.toString(objects.toArray())));
                        if (newValue.compareTo(oldValue) == 0) {
                            System.out.println("success");
                        } else {
                            System.err.println("===============================error===============================");
                        }
                        System.out.println();
                    } catch (Exception e) {
                        System.err.println(String.format("[transferFrom] hash: %s, contract: %s, data: %s, old", trxTxHash, to, "error"));
                        System.out.println(String.format("[transferFrom] hash: %s, contract: %s, data: %s, new", trxTxHash, to, Arrays.toString(TrxUtil.parseTRC20TransferFromInput(input).toArray())));
                        System.out.println();
                    }


                }
                //}
            }
        }
    }

    @Test
    public void getBlock() throws Exception {
        setMain();
        Chain.Block block = wrapper.getBlockByNum(34882992L);
        System.out.println(block.getTransactionsCount());
        /**
         SHA256.Digest digest = new SHA256.Digest();
         digest.update(txn.getRawData().toByteArray());
         byte[] txid = digest.digest();
         */
        List<Chain.Transaction> list = block.getTransactionsList();
        for (Chain.Transaction tx : list) {
            String txHash = TrxUtil.calcTxHash(tx);
            System.out.println(txHash);
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            // 转账
            if (Chain.Transaction.Contract.ContractType.TransferContract == type) {
                Contract.TransferContract tc = Contract.TransferContract.parseFrom(contract.getParameter().getValue());
                System.out.println(String.format("[转账] from: %s, to: %s, amount: %s", TrxUtil.ethAddress2trx(tc.getOwnerAddress().toByteArray()),
                        TrxUtil.ethAddress2trx(tc.getToAddress().toByteArray()),
                        tc.getAmount()));
            }
            // 调用合约
            if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type) {
                Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
                System.out.println(String.format("[调用合约] from: %s, contract: %s, callValue: %s, callTokenValue: %s, data: %s",
                        TrxUtil.ethAddress2trx(tg.getOwnerAddress().toByteArray()),
                        TrxUtil.ethAddress2trx(tg.getContractAddress().toByteArray()),
                        tg.getCallValue(),
                        tg.getCallTokenValue(),
                        Numeric.toHexString(tg.getData().toByteArray())

                ));
            }
        }
    }

    @Test
    public void isCompletedTx() {
        String nerveTxHash = "";
        Function function = getIsCompletedFunction(nerveTxHash);
        wrapper.constantCall(from, multySignContractAddress, function);
    }

    @Test
    public void accountTest() {
        for (String key : list) {
            KeyPair pair = new KeyPair(key);
            System.out.println(pair.toHexAddress() + ", " + pair.toBase58CheckAddress());
        }
    }

    @Test
    public void signTest() {
        String hash = "b754df4d6cfe869501f9d0f1a40ab6644072524aa4edd3ac4aaef929e06ca0bf";
        KeyPair pair = new KeyPair(list.get(0));
        SECP256K1.Signature sig = SECP256K1.sign(Bytes32.wrap(Numeric.hexStringToByteArray(hash)), pair.getRawPair());
        MutableBytes bytes = MutableBytes.create(65);
        UInt256.valueOf(sig.getR()).toBytes().copyTo(bytes, 0);
        UInt256.valueOf(sig.getS()).toBytes().copyTo(bytes, 32);
        byte recId = sig.getRecId();
        if (recId <= 1) {
            recId += 27;
        }
        bytes.set(64, recId);
        byte[] result = bytes.toArray();
        System.out.println(Numeric.toHexString(result));
    }

    @Test
    public void verifySign() {
        String signAddress = "TWAKmvmmcCgSrKXD94isjn2HiEzm5v8gj3";
        String vHash = "0xfe77b02ee937fd6debba9f8c40947dd8ce9e3c0b7a93605592052cbfeba435a2";
        String signed = "b98f5bc79223d2394cdeacdaa1b6289fc4b7478bbea7b300ea4d6fe821ea7f990e0d0a6363e950ba077e20e171e0532d961a2c4d186f51c8e037a266be431bdc1c";
        Boolean sign = TrxUtil.verifySign(signAddress, vHash, signed);
        System.out.println(sign);
    }

    @Test
    public void needFeeTest() {
        BigDecimal nvt = calcOtherMainAssetOfWithdraw(AssetName.NVT, new BigDecimal("0.0304"), new BigDecimal("0.07"));
        System.out.println(nvt);
    }

    @Test
    public void getHeight() {
        setMain();
        Response.BlockExtention block = wrapper.blockingStub.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println(block.getBlockHeader());
    }

    @Test
    public void covertNerveAddressByTx() throws Exception {
        String txHash = "0255066adc99ab923a5fb955bb075788fab7a8942b24f493c8962062b99f3878";
        Chain.Transaction tx = wrapper.getTransactionById(txHash);
        System.out.println(TrxUtil.covertNerveAddressByTx(tx, nerveChainId));
    }

    @Test
    public void addressTest() {
        List<String> list = new ArrayList<>();
        //list.add("TSbGkbX6ZjQZM9eco3JLzfFCR1yxbKxwsy");
        //list.add("TVAhzC3rBLy6XLceHfha3ddiVHTUpUovyY");
        list.add("0x595d5364e5eb77e3707ce2710215db97a835a82d");
        //list.add("0x6c2039b5fdae068bad4931e8cc0b8e3a542937ac");
        //list.add("0x3c2ff003ff996836d39601ca22394a58ca9c473b");
        //list.add("0xae00574bdc6bbd40612ec024e2536cc0784f73e4");
        for (String address : list) {
            System.out.println("-------");
            System.out.println(TrxUtil.ethAddress2trx(address));
            System.out.println(TrxUtil.trxAddress2eth(address));
            System.out.println("-------");
        }
    }

    @Test
    public void isMinterERC20() throws Exception {
        //setMain();
        //String multy = "TYmgxoiPetfE2pVWur9xp7evW4AuZCzfBm";
        String multy = "0x41d2972934f5cdcf9beef75e5884ab61ab2dfdf533";
        String erc20 = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";//USDT
        Function isMinterERC20Function = TrxUtil.getIsMinterERC20Function(erc20);
        List<Type> valueTypes = walletApi.callViewFunction(multy, isMinterERC20Function);
        boolean isMinterERC20 = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        System.out.println(isMinterERC20);
    }

    public static SECP256K1.KeyPair createPair(String prikey) {
        return new KeyPair(prikey).getRawPair();
    }

    protected Function getERC20TransferFromFunction(String sender, String recipient, BigInteger value) {
        return new Function(
                "transferFrom",
                List.of(new Address(sender),
                        new Address(recipient),
                        new Uint256(value)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

    protected Function getERC20ApproveFunction(String spender, BigInteger value) {
        return new Function(
                "approve",
                List.of(new Address(spender),
                        new Uint256(value)),
                List.of(new TypeReference<Type>() {
                })
        );
    }

}