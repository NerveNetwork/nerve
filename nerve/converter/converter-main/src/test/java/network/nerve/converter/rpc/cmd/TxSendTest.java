package network.nerve.converter.rpc.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.bo.NonceBalance;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.ConverterUtil;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TxSendTest {
    /*
    static String address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";
    static String address30 = "tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ";
    */

    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";
    static String address30 = "TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9";
    /**
     * 0xc11D9943805e56b630A401D4bd9A29550353EFa1 [Account 9]
     */
    /*static String address31 = "tNULSeBaMrQaVh1V7LLvbKa5QSN54bS4sdbXaF";
    String packageAddressZP = "tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp";
    String packageAddressNE = "tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe";
    String packageAddressHF = "tNULSeBaMmShSTVwbU4rHkZjpD98JgFgg6rmhF";// 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
    String packageAddress6  = "tNULSeBaMfmpwBtUSHyLCGHq4WqYY5A4Dxak91";
    String packageAddress7  = "tNULSeBaMjqtMNhWWyUKZUsGhWaRd88RMrSU6J";
    String packageAddress8  = "tNULSeBaMrmiuHZg9c2JVAbLQydAxjNvuKRgFj";*/

    static String agentAddress;
    static String packageAddress;
    static String packageAddressPrivateKey;

    static String address31 = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
    static String address32 = "TNVTdTSPQj7T5LiVmL974o2YRWa1YPJJzJHhn";
    static String address33 = "TNVTdTSPKy4iLwK6XC52VNqVSnk1vncF5Z2mu";
    static String address34 = "TNVTdTSPRgkZKoNaeAUj6H3UWH29D5ftv7LNN";
    static String address35 = "TNVTdTSPVhoYssF5cgMVGWRYsdai9KLs9rotk";
    static String address36 = "TNVTdTSPN9pNrMMEmhZsNYVn9Lcyu3cxSUbAL";
    static String address37 = "TNVTdTSPLnyxJ4gWi3L4mr6sSQrcfqLqPbCkP";
    static String address38 = "TNVTdTSPJiFqnqW2sGNVZ1do2C6tFoLv7DBgE";
    static String address39 = "TNVTdTSPTgPV9AjgQKFvdT1eviWisVMG7Naah";
    static String address40 = "TNVTdTSPGvLeBDxQiWRH3jZTcrYKwSF2axCfy";
    static String address41 = "TNVTdTSPPyoYQNDgfbF83P3kWJz9bvrNej1RW";
    static String address42 = "TNVTdTSPTNWUw7YiRLuwpFiPiUcpYQbzRU8LT";
    static String address43 = "TNVTdTSPSxopb3jVAdDEhx49S6iaA2CiPa3oa";
    static String address44 = "TNVTdTSPNc8YhE5h7Msd8R9Vebd5DG9W38Hd6";
    static String address45 = "TNVTdTSPPDMJA6eFRAb47vC2Lzx662nj3VVhg";
    static String address46 = "TNVTdTSPMXhkD6FJ9htA9H3aDEVVg8DNoriur";
    static String address47 = "TNVTdTSPPENjnLifQrJ4EK6tWp1HaDhnW5h7y";
    static String address48 = "TNVTdTSPLmeuz7aVsdb2WTcGXKFmcKowTfk46";
    static String address49 = "TNVTdTSPF1mBVywX7BR674SZbaHBn3JoPhyJi";
    static String address50 = "TNVTdTSPHR7jCTZwtEB6FS1BZuBe7RVjshEsB";
    static String address51 = "TNVTdTSPRrYndMR8JZ4wJovLDbRp2o4gGWDAp";

    String packageAddressZP = "TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd";
    String packageAddressNE = "TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT";
    String packageAddressHF = "TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx";// 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
    String packageAddress6 = "TNVTdTSPF9nBiba1vk4PqRkyQaYqwoAJX95xn";// 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65
    String packageAddress7 = "TNVTdTSPKDqbuQc6cF3m41CcQKRvzmXSQzouy";// 0xd29E172537A3FB133f790EBE57aCe8221CB8024F
    String packageAddress8 = "TNVTdTSPS9g9pGmjEo2gjjGKsNBGc22ysz25a";// 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17
    String packageAddressPrivateKeyZP = "b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5";
    String packageAddressPrivateKeyNE = "188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f";
    String packageAddressPrivateKeyHF = "fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499";
    String packageAddressPrivateKey6 = "43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D";
    String packageAddressPrivateKey7 = "0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85";
    String packageAddressPrivateKey8 = "CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2";

    static String USDT_HT = "0x04f535663110a392a6504839beed34e019fdb4e0";
    static String USDX_HT = "0x03Cf96223BD413eb7777AFE3cdb689e7E851CB32";
    static String USDI_HT = "0x3F1f3D17619E916C4F04707BA57d8E0b9e994fB0";
    static String DEX_HT_18 = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
    static String NVT_HT_MINTER = "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9";
    static String NULS_HT_MINTER = "0x5045b6a04AC33f8D16d47E46b971C848141eE270";
    static String ETH_HT_MINTER = "0x830befa62501F1073ebE2A519B882e358f2a0318";

    static String USDX_BNB = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String NVT_BNB_MINTER = "0x3F1f3D17619E916C4F04707BA57d8E0b9e994fB0";
    static String NULS_BNB_MINTER = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
    static String DXA_BNB_MINTER = "0x3139dBe1Bf7FEb917CF8e978b72b6eAd764b0e6C";
    static String USDI_BNB_MINTER = "0xF3B4771813f27C390B11703450F5E188b83829F9";
    static String ETH_BNB_MINTER = "0x9296D0AF7DA81AAD9ae273118Ba377403db6691a";


    static String USDX = "0xB058887cb5990509a3D0DD2833B2054E4a7E4a55";
    static String USDI = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
    static String ENVT = "0x53be0d78b686f68643c38dfcc4f141a0c2785a08";
    // ERC20Minter
    static String NVT_ETH_MINTER = "0x25EbbAC2CA9DB0c1d6F0fc959BbC74985417BaB0";
    static String NVT18 = "0x4860dfB7b6a66344ea2dCF18E9fE7CDa5a7c0724";
    static String NULS_ETH_MINTER = "0x79D7c11CC945a1734d21Ef41e631EFaE894Af2C3";
    static String NULS18 = "0x7E6B3AcC5AE8Ecbe008aca3C11E22ff12B77d17f";
    static String DXA_ETH_MINTER = "0xf71f9DAcf995895a1ae10482F3b7806e22D1Ea03";

    static String DAI = "0xad6d458402f60fd3bd25163575031acdce07538d";
    static String FAU = "0xfab46e002bbf0b4509813474841e0716e6730136";
    static String MT = "0x9b4e2b4b13d125238aa0480dd42b4f6fc71b37cc";
    //symbol: DAI, decimals: 18, address: 0xad6d458402f60fd3bd25163575031acdce07538d
    //symbol: FAU, decimals: 18, address: 0xfab46e002bbf0b4509813474841e0716e6730136

    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;
    static int ethChainId = 101;
    static int bnbChainId = 102;
    static int htChainId = 103;
    static int heterogeneousAssetId = 1;

    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";


    @Before
    public void before() throws Exception {
        AddressTool.addPrefix(5, "TNVT");
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId, "UTF-8"));
        // 设置共识节点地址和出块地址
        packageZP();
    }


    // 0x09534d4692F568BC6e9bef3b4D84d48f19E52501 [Account3]
    // 0xF3c90eF58eC31805af11CE5FA6d39E395c66441f [Account4]
    // 0x6afb1F9Ca069bC004DCF06C51B42992DBD90Adba [Account5]
    // 私钥: 43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D / 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65 [Account 6] / tNULSeBaMfmpwBtUSHyLCGHq4WqYY5A4Dxak91
    // 私钥: 0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85 / 0xd29E172537A3FB133f790EBE57aCe8221CB8024F [Account 7] / tNULSeBaMjqtMNhWWyUKZUsGhWaRd88RMrSU6J
    // 私钥: CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2 / 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17 [Account 8] / tNULSeBaMrmiuHZg9c2JVAbLQydAxjNvuKRgFj
    @Test
    public void importPriKeyTest() {
        //公钥: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        //importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp, 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
        //公钥: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
        //importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe, 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn
        importPriKey("B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF", password);//30 ETH 测试网地址 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
        importPriKey("4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39", password);//31 测试网地址 tNULSeBaMrQaVh1V7LLvbKa5QSN54bS4sdbXaF, 0xc11D9943805e56b630A401D4bd9A29550353EFa1 [Account 9]
        importPriKey(packageAddressPrivateKey, password);
        //=================================================================//
        importPriKey("e70ea2ebe146d900bf84bc7a96a02f4802546869da44a23c29f599c7e42001da", password);//32 TNVTdTSPQj7T5LiVmL974o2YRWa1YPJJzJHhn
        importPriKey("4c6b4c5d9b07e364d6b306d1afe0f2c37e15c64ac5151a395a4c570f00ce867d", password);//33 TNVTdTSPKy4iLwK6XC52VNqVSnk1vncF5Z2mu
        importPriKey("2fea28f438a104062e4dcd79427282573053a6b762e68b942055221462c46f02", password);//34 TNVTdTSPRgkZKoNaeAUj6H3UWH29D5ftv7LNN
        importPriKey("08407198c196c950afffd326a00321a5ea563b3beaf640d462f3a274319b753d", password);//35 TNVTdTSPVhoYssF5cgMVGWRYsdai9KLs9rotk
        importPriKey("be9dd9fb419ede7188b45451445525669d5e9d256bd3f938ecae177194867aa1", password);//36 TNVTdTSPN9pNrMMEmhZsNYVn9Lcyu3cxSUbAL
        importPriKey("9769cdc13af8da759ba985156876072986e9e10deb5fe41fe8406567071b0a71", password);//37 TNVTdTSPLnyxJ4gWi3L4mr6sSQrcfqLqPbCkP
        importPriKey("9887b7e02b098a421b406223c3ec5b92889d294f4ed84a0d53018dced35cff41", password);//38 TNVTdTSPJiFqnqW2sGNVZ1do2C6tFoLv7DBgE
        importPriKey("7ec6ae2f4da0b80c0120ea96e8ce8973623ccaed36f5c2145032ac453dc006f0", password);//39 TNVTdTSPTgPV9AjgQKFvdT1eviWisVMG7Naah
        importPriKey("bd08d1cd9a1f319a0c0439b29029f7e46584c56126fd30f02c0b6fb5fb8e4144", password);//40 TNVTdTSPGvLeBDxQiWRH3jZTcrYKwSF2axCfy
        importPriKey("48348fff812b049024efcd2b3481ada1cfdeb3deabb56e4ba9d84c2ebb3a8a1f", password);//41 TNVTdTSPPyoYQNDgfbF83P3kWJz9bvrNej1RW
        importPriKey("f13495414167ffc3254ef93a0fc47102f721a556d1fb595f5cc130021cbcc67a", password);//42 TNVTdTSPTNWUw7YiRLuwpFiPiUcpYQbzRU8LT
        importPriKey("2c30389340122e20b9462a418979dcced200549c4aa7e42f189425ecedb18b2a", password);//43 TNVTdTSPSxopb3jVAdDEhx49S6iaA2CiPa3oa
        importPriKey("19397598342ea2adf2162c0dc9a00381bdaa28a3a481ba9f6fa70afa3040625d", password);//44 TNVTdTSPNc8YhE5h7Msd8R9Vebd5DG9W38Hd6
        importPriKey("8e81aab76c78c07d3304d0c03e7790423b3e28b9851756d0b1ac962ac1acb504", password);//45 TNVTdTSPPDMJA6eFRAb47vC2Lzx662nj3VVhg
        importPriKey("9d1f84b2b3c1f53498abb436d87a32af793978d22bc76fc2b6fa1971b117ff63", password);//46 TNVTdTSPMXhkD6FJ9htA9H3aDEVVg8DNoriur
        importPriKey("0bd10e2fe13ca8d6d91b43e0518d5ad06adaad9a78520a39d8db00ed62d45dd4", password);//47 TNVTdTSPPENjnLifQrJ4EK6tWp1HaDhnW5h7y
        importPriKey("01f72a7d50655939b60c4f79ea6dd2661b435d49ce81467d91f5b85f4a26c112", password);//48 TNVTdTSPLmeuz7aVsdb2WTcGXKFmcKowTfk46
        importPriKey("c0102d3f66edf0fd8939fb149bbe5a5f6503e8a7bf41b80b8b5a0312c6ced3a7", password);//49 TNVTdTSPF1mBVywX7BR674SZbaHBn3JoPhyJi
        importPriKey("d92a08dafcec90ba2e08cc825c6f74c41058b9bc325f61ffa1fddaf27a358f3b", password);//50 TNVTdTSPHR7jCTZwtEB6FS1BZuBe7RVjshEsB
        importPriKey("efc10e6831a87ba71dad9c3769b07875a0eb9b8ced5139125f05a58d0f0c599f", password);//51 TNVTdTSPRrYndMR8JZ4wJovLDbRp2o4gGWDAp

        //importPriKey("a0282c3f197bae3345938595aba2296affae60fbafa7e2723910248466718858", password);//
    }

    private String privateKey20 = "9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b";
    private String privateKey21 = "477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75";
    private String privateKey22 = "8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78";
    private String privateKey23 = "4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530";
    private String privateKey24 = "bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7";
    private String privateKey25 = "ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200";
    private String privateKey26 = "4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a";
    private String privateKey27 = "3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1";
    private String privateKey28 = "27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e";
    private String privateKey29 = "76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b";
    private String privateKey30 = "B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF";
    private String privateKey31 = "4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39";
    private String privateKey32 = "e70ea2ebe146d900bf84bc7a96a02f4802546869da44a23c29f599c7e42001da";
    private String privateKey33 = "4c6b4c5d9b07e364d6b306d1afe0f2c37e15c64ac5151a395a4c570f00ce867d";
    private String privateKey34 = "2fea28f438a104062e4dcd79427282573053a6b762e68b942055221462c46f02";
    private String privateKey35 = "08407198c196c950afffd326a00321a5ea563b3beaf640d462f3a274319b753d";
    private String privateKey36 = "be9dd9fb419ede7188b45451445525669d5e9d256bd3f938ecae177194867aa1";
    private String privateKey37 = "9769cdc13af8da759ba985156876072986e9e10deb5fe41fe8406567071b0a71";
    private String privateKey38 = "9887b7e02b098a421b406223c3ec5b92889d294f4ed84a0d53018dced35cff41";
    private String privateKey39 = "7ec6ae2f4da0b80c0120ea96e8ce8973623ccaed36f5c2145032ac453dc006f0";
    private String privateKey40 = "bd08d1cd9a1f319a0c0439b29029f7e46584c56126fd30f02c0b6fb5fb8e4144";
    private String privateKey41 = "48348fff812b049024efcd2b3481ada1cfdeb3deabb56e4ba9d84c2ebb3a8a1f";
    private String privateKey42 = "f13495414167ffc3254ef93a0fc47102f721a556d1fb595f5cc130021cbcc67a";
    private String privateKey43 = "2c30389340122e20b9462a418979dcced200549c4aa7e42f189425ecedb18b2a";
    private String privateKey44 = "19397598342ea2adf2162c0dc9a00381bdaa28a3a481ba9f6fa70afa3040625d";
    private String privateKey45 = "8e81aab76c78c07d3304d0c03e7790423b3e28b9851756d0b1ac962ac1acb504";
    private String privateKey46 = "9d1f84b2b3c1f53498abb436d87a32af793978d22bc76fc2b6fa1971b117ff63";
    private String privateKey47 = "0bd10e2fe13ca8d6d91b43e0518d5ad06adaad9a78520a39d8db00ed62d45dd4";
    private String privateKey48 = "01f72a7d50655939b60c4f79ea6dd2661b435d49ce81467d91f5b85f4a26c112";
    private String privateKey49 = "c0102d3f66edf0fd8939fb149bbe5a5f6503e8a7bf41b80b8b5a0312c6ced3a7";
    private String privateKey50 = "d92a08dafcec90ba2e08cc825c6f74c41058b9bc325f61ffa1fddaf27a358f3b";
    private String privateKey51 = "efc10e6831a87ba71dad9c3769b07875a0eb9b8ced5139125f05a58d0f0c599f";

    public static void importPriKey(String priKey, String pwd) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getBalance() throws Exception {
        getBalanceByAddress(address31);
        //getBalanceByAddress(address32);
    }

    protected void getBalanceByAddress(String address) throws Exception {
        System.out.println();
        System.out.println(String.format("address: %s", address));
        BigInteger balance2 = LedgerCall.getBalance(chain, chainId, assetId, address);
        System.out.println(String.format("　主资产NVT %s-%s: %s", chainId, assetId, balance2));

        Integer nulsAssetChainId = 1;
        Integer nulsAssetId = 1;
        BigInteger balance7 = LedgerCall.getBalance(chain, nulsAssetChainId, nulsAssetId, address);
        System.out.println(String.format("平行资产NULS %s-%s: %s", nulsAssetChainId, nulsAssetId, balance7));

        NerveAssetInfo bnbAssetId = this.findAssetIdByHeterogeneousId(bnbChainId, heterogeneousAssetId);
        BigInteger bnbBalance = LedgerCall.getBalance(chain, bnbAssetId.getAssetChainId(), bnbAssetId.getAssetId(), address);
        System.out.println(String.format("币安资产BNB %s-%s: %s", bnbAssetId.getAssetChainId(), bnbAssetId.getAssetId(), bnbBalance));

        NerveAssetInfo bnbUsdxAssetId = this.findAssetIdByAddress(bnbChainId, USDX_BNB);
        BigInteger balanceBnbUsdx = LedgerCall.getBalance(chain, bnbUsdxAssetId.getAssetChainId(), bnbUsdxAssetId.getAssetId(), address);
        System.out.println(String.format("币安资产USDX %s-%s: %s", bnbUsdxAssetId.getAssetChainId(), bnbUsdxAssetId.getAssetId(), balanceBnbUsdx));

        NerveAssetInfo ethAssetId = this.findAssetIdByHeterogeneousId(EthConstant.ETH_CHAIN_ID, EthConstant.ETH_ASSET_ID);
        BigInteger balance = LedgerCall.getBalance(chain, ethAssetId.getAssetChainId(), ethAssetId.getAssetId(), address);
        System.out.println(String.format("以太资产ETH %s-%s: %s", ethAssetId.getAssetChainId(), ethAssetId.getAssetId(), balance));

        NerveAssetInfo usdxAssetId = this.findAssetIdByAddress(ethChainId, USDX);
        BigInteger balance4 = LedgerCall.getBalance(chain, usdxAssetId.getAssetChainId(), usdxAssetId.getAssetId(), address);
        System.out.println(String.format("以太资产USDX %s-%s: %s", usdxAssetId.getAssetChainId(), usdxAssetId.getAssetId(), balance4));

        NerveAssetInfo envtAssetId = this.findAssetIdByAddress(ethChainId, ENVT);
        BigInteger balance5 = LedgerCall.getBalance(chain, envtAssetId.getAssetChainId(), envtAssetId.getAssetId(), address);
        System.out.println(String.format("以太资产ENVT %s-%s: %s", envtAssetId.getAssetChainId(), envtAssetId.getAssetId(), balance5));

        NerveAssetInfo usdiAssetId = this.findAssetIdByAddress(ethChainId, USDI);
        BigInteger balance3 = LedgerCall.getBalance(chain, usdiAssetId.getAssetChainId(), usdiAssetId.getAssetId(), address);
        System.out.println(String.format("以太资产USDI %s-%s: %s", usdiAssetId.getAssetChainId(), usdiAssetId.getAssetId(), balance3));

        NerveAssetInfo htAssetId = this.findAssetIdByHeterogeneousId(htChainId, heterogeneousAssetId);
        BigInteger htBalance = LedgerCall.getBalance(chain, htAssetId.getAssetChainId(), htAssetId.getAssetId(), address);
        System.out.println(String.format("火币资产HT %s-%s: %s", htAssetId.getAssetChainId(), htAssetId.getAssetId(), htBalance));

        NerveAssetInfo htUsdtAssetId = this.findAssetIdByAddress(htChainId, USDT_HT);
        BigInteger balanceHtUsdt = LedgerCall.getBalance(chain, htUsdtAssetId.getAssetChainId(), htUsdtAssetId.getAssetId(), address);
        System.out.println(String.format("火币资产USDT %s-%s: %s", htUsdtAssetId.getAssetChainId(), htUsdtAssetId.getAssetId(), balanceHtUsdt));

        NerveAssetInfo nvtAssetId = this.findAssetIdByAddress(ethChainId, NVT_ETH_MINTER);
        if (nvtAssetId.isEmpty()) {
            nvtAssetId = this.findAssetIdByAddress(bnbChainId, NVT_BNB_MINTER);
        }
        BigInteger balance8 = LedgerCall.getBalance(chain, nvtAssetId.getAssetChainId(), nvtAssetId.getAssetId(), address);
        System.out.println(String.format("绑定资产NVT %s-%s: %s", nvtAssetId.getAssetChainId(), nvtAssetId.getAssetId(), balance8));

        NerveAssetInfo nulseAssetId = this.findAssetIdByAddress(ethChainId, NULS_ETH_MINTER);
        if (nulseAssetId.isEmpty()) {
            nulseAssetId = this.findAssetIdByAddress(bnbChainId, NULS_BNB_MINTER);
        }
        BigInteger balance6 = LedgerCall.getBalance(chain, nulseAssetId.getAssetChainId(), nulseAssetId.getAssetId(), address);
        System.out.println(String.format("绑定资产NULS %s-%s: %s", nulseAssetId.getAssetChainId(), nulseAssetId.getAssetId(), balance6));

        NerveAssetInfo dxaAssetId = this.findAssetIdByAddress(bnbChainId, DXA_BNB_MINTER);
        if (dxaAssetId.isEmpty()) {
            dxaAssetId = this.findAssetIdByAddress(ethChainId, DXA_ETH_MINTER);
        }
        BigInteger balanceDxa = LedgerCall.getBalance(chain, dxaAssetId.getAssetChainId(), dxaAssetId.getAssetId(), address);
        System.out.println(String.format("绑定资产DXA %s-%s: %s", dxaAssetId.getAssetChainId(), dxaAssetId.getAssetId(), balanceDxa));

        NerveAssetInfo usdiBnbAssetId = this.findAssetIdByAddress(bnbChainId, USDI_BNB_MINTER);
        BigInteger balanceUsdiBnb = LedgerCall.getBalance(chain, usdiBnbAssetId.getAssetChainId(), usdiBnbAssetId.getAssetId(), address);
        System.out.println(String.format("绑定资产USDI_BNB %s-%s: %s", usdiBnbAssetId.getAssetChainId(), usdiBnbAssetId.getAssetId(), balanceUsdiBnb));


    }

    @Test
    public void getNonceAndBalance() throws Exception {
        NonceBalance b = LedgerCall.getBalanceNonce(chain, chainId, assetId, agentAddress);
        System.out.println(b.getAvailable());
        BigInteger balance2 = LedgerCall.getBalance(chain, chainId, assetId, agentAddress);
        System.out.println(balance2);
    }

    protected Map<String, Object> getTxCfmClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug(JSONUtils.obj2PrettyJson(record));
        return (Map)record.get("tx_getConfirmedTxClient");
    }

    @Test
    public void getTx() throws Exception {
        String txStr = (String)(getTxCfmClient("3039d958fab9359427691c444478ebb78a5fce2e99bd1bf4d773c612b346dd2d").get("tx"));
        System.out.println(txStr);
        Transaction tx = ConverterUtil.getInstance(txStr, Transaction.class);//最后一条
        //System.out.println(tx.format(WithdrawalHeterogeneousSendTxData.class));
        //System.out.println(tx.format(WithdrawalTxData.class));
        System.out.println(tx.format(ConfirmWithdrawalTxData.class));
        //System.out.println(tx.format(RechargeUnconfirmedTxData.class));
        //System.out.println(tx.format(RechargeTxData.class));
    }

    @Test
    public void withdrawalHT() throws Exception {
        int htgChainId = htChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.01个HT
        BigInteger value = new BigInteger(Long.valueOf(1_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBNB() throws Exception {
        int htgChainId = bnbChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.01个BNB
        BigInteger value = new BigInteger(Long.valueOf(4_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalETH() throws Exception {
        int htgChainId = ethChainId;
        //int htgChainId = bnbChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.019个ETH
        BigInteger value = new BigInteger(Long.valueOf(1_9000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(ethChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDX() throws Exception {
        int htgChainId = bnbChainId;
        String contract = USDX_BNB;
        //int htgChainId = ethChainId;
        //String contract = USDX;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 1个USDX
        BigInteger value = new BigInteger(Long.valueOf(1_000000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDT() throws Exception {
        int htgChainId = htChainId;
        String contract = USDT_HT;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.2个USDT
        BigInteger value = new BigInteger(Long.valueOf(200000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDI() throws Exception {
        int htgChainId = ethChainId;
        String contract = USDI;
        //int htgChainId = bnbChainId;
        //String contract = USDI_BNB_MINTER;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 2个USDI
        BigInteger value = new BigInteger(Long.valueOf(2_000000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalENVT() throws Exception {
        int htgChainId = ethChainId;
        String contract = ENVT;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 90万个ENVT
        BigInteger value = new BigInteger("900000000000000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalNVT() throws Exception {
        int htgChainId = ethChainId;
        String contract = NVT_ETH_MINTER;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 2个NVT
        BigInteger value = new BigInteger("20000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalDXA() throws Exception {
        int htgChainId = ethChainId;
        String contract = DXA_ETH_MINTER;
        String from = address22;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 2000千万个DXA
        BigInteger value = new BigInteger("2000000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalNULS() throws Exception {
        int htgChainId = ethChainId;
        String contract = NULS_ETH_MINTER;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 19万个NULS
        BigInteger value = new BigInteger("19000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    protected void withdrawalByParams(String from, String to, BigInteger value, BigInteger fee, int heterogeneousChainId, NerveAssetInfo assetInfo) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("assetChainId", assetInfo.getAssetChainId());
        params.put("assetId", assetInfo.getAssetId());
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAddress", to);
        params.put("amount", value);
        params.put("distributionFee", fee);
        params.put("remark", "提现");
        params.put("address", from);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
        if (cmdResp.isSuccess()) {
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.info("hash:{}", hash);
            Log.info("txHex:{}", txHex);
        } else {
            System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        }
    }

    @Test
    public void withdrawalAdditionalFee() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        // 追加nvt手续费
        // 提现金额ETH
        String amount = "1";
        BigDecimal am = new BigDecimal(amount).movePointRight(8);
        amount = am.toPlainString();

        params.put("txHash", "e18512529814c2d40852b8e122258d0f6f67246f9ef9d0aa4e3b9a28244ea4fd");
        params.put("amount", amount);
        params.put("remark", "追加手续费");
        params.put("address", address31);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal_additional_fee", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void contractAssetReg() throws Exception {
        // 0x1c78958403625aeA4b0D5a0B527A27969703a270 USDI in ropsten
        // 0xAb58ee8e62178693265a1418D109b70dB4595586 USDK in rinkeby
        regERC20(htChainId, "USDT", USDT_HT, 6);
        regERC20(bnbChainId, "USDX", USDX_BNB, 6);
        regERC20(ethChainId, "USDX", USDX, 6);
        regERC20(ethChainId, "USDI", USDI, 6);
        //regERC20(ethChainId, "ENVT", ENVT, 18);
        //regERC20(ethChainId, "DAI", DAI, 18);
        //regERC20("FAU", FAU, 18);
        //regERC20("MT", MT, 18); // mainnet
    }

    @Test
    public void mainAssetReg() throws Exception {
        // BNB
        regHeterogeneousMainAsset(BnbConstant.BNB_CHAIN_ID);
        // HT
        regHeterogeneousMainAsset(HtConstant.HT_CHAIN_ID);
    }

    @Test
    public void bindContractAssetReg() throws Exception {
        bindERC20(ethChainId, "NVT", NVT_ETH_MINTER, 8, 5, 1);
        bindERC20(bnbChainId, "NVT", NVT_BNB_MINTER, 8, 5, 1);
        bindERC20(htChainId, "NVT", NVT_HT_MINTER, 8, 5, 1);

        bindERC20(bnbChainId, "ETH", ETH_BNB_MINTER, 18, 5, 2);
        bindERC20(htChainId, "ETH", ETH_HT_MINTER, 18, 5, 2);
        //bindERC20(bnbChainId, "USDI", USDI_BNB_MINTER, 6, 5, 6);
        //bindERC20(bnbChainId, "DXA_ETH_MINTER", DXA_BNB_MINTER, 8, 5, 7);
        //bindERC20(ethChainId, "DXA_ETH_MINTER", DXA_ETH_MINTER, 8, 5, 7);
        //bindERC20(ethChainId, "NVT_ETH_MINTER", NVT_ETH_MINTER, 8, 5, 1);
    }

    @Test
    public void unbindContractAssetReg() throws Exception {
        //unbindERC20(htChainId, 5, 2);
        unbindERC20(ethChainId, 5, 1);
    }

    @Test
    public void overrideBindContractAssetReg() throws Exception {
        overrideBindERC20(ethChainId, NVT_ETH_MINTER, 5, 6);
    }

    private void regERC20(int heterogeneousChainId, String symbol, String contract, int decimal) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("decimals", decimal);
        params.put("symbol", symbol);
        params.put("contractAddress", contract);
        params.put("remark", "ropsten合约资产注册");
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CREATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void regHeterogeneousMainAsset(int heterogeneousChainId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("remark", "异构链主资产注册");
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void bindERC20(int heterogeneousChainId, String symbol, String contract, int decimal, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("decimals", decimal);
        params.put("symbol", symbol);
        params.put("contractAddress", contract);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void overrideBindERC20(int heterogeneousChainId, String contract, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("contractAddress", contract);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.OVERRIDE_BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void unbindERC20(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.UNBIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void chainAssetTxRegisterTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("assetSymbol","DXA_ETH_MINTER");
        params.put("assetName","DXA_ETH_MINTER");
        params.put("initNumber",100000000);
        params.put("decimalPlace",8);
        params.put("txCreatorAddress", address22);
        params.put("assetOwnerAddress", address22);
        params.put("password","nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        Log.debug("response {}", JSONUtils.obj2PrettyJson(response));
    }

    @Test
    public void getAssetInfoByAddress() throws Exception {
        this.findAssetIdByAddress(ethChainId, NVT_ETH_MINTER, true);
    }

    private NerveAssetInfo findAssetIdByAddress(int heterogeneousChainId, String contractAddress) throws Exception {
        return this.findAssetIdByAddress(heterogeneousChainId, contractAddress, false);
    }

    private NerveAssetInfo findAssetIdByAddress(int heterogeneousChainId, String contractAddress, boolean debug) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("contractAddress", contractAddress);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ADDRESS, params);
        Map responseData = (Map) cmdResp.getResponseData();
        Map result = (Map) responseData.get(ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ADDRESS);
        if (result == null) {
            return NerveAssetInfo.emptyInstance();
        }
        Integer chainId = (Integer) result.get("chainId");
        Integer assetId = (Integer) result.get("assetId");
        if (debug) {
            System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        }
        return new NerveAssetInfo(chainId, assetId);
    }

    private NerveAssetInfo findAssetIdByHeterogeneousId(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAssetId", heterogeneousAssetId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID, params);
        Map responseData = (Map) cmdResp.getResponseData();
        Map result = (Map) responseData.get(ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID);
        if (result == null) {
            return NerveAssetInfo.emptyInstance();
        }
        Integer chainId = (Integer) result.get("chainId");
        Integer assetId = (Integer) result.get("assetId");
        return new NerveAssetInfo(chainId, assetId);
    }

    @Test
    public void findAssetIdByHeId() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("heterogeneousChainId", 101);
        params.put("heterogeneousAssetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void findAssetInfoByAssetId() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("assetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_LIST, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void gatPriceTest() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("assetId", 110);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_test", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void retryWithdrawalMsg() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("hash", "2b8c3bb2893804f534ecd5e3489ce6d13a941cf3c03830ebff32d4a46810f63d");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_retry_withdrawal", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }


    @Test
    public void transfer() throws Exception {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();

        outputs.add(new CoinDTO(address22, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address23, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address24, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address25, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address26, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address27, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address28, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address29, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address30, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address31, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));

        outputs.add(new CoinDTO(address32, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address33, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address34, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address35, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address36, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address37, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address38, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address39, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address40, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(address41, chainId, 1, BigInteger.valueOf(1510000_0000_0000L), password, 0));

        outputs.add(new CoinDTO(packageAddressZP, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(packageAddressNE, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));
        outputs.add(new CoinDTO(packageAddressHF, chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));

        BigInteger inAmount = BigInteger.valueOf(10_0000L);
        for (CoinDTO dto : outputs) {
            inAmount = inAmount.add(dto.getAmount());
        }
        inputs.add(new CoinDTO(address21, chainId, 1, inAmount, password, 0));

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);

        //调用接口
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }


    @Test
    public void batchCreateAgent() throws Exception {
        List<String> agentList = new ArrayList<>();
        agentList.add(address32);
        agentList.add(address33);
        agentList.add(address34);
        agentList.add(address35);
        agentList.add(address36);
        agentList.add(address37);
        agentList.add(address38);
        agentList.add(address39);
        agentList.add(address40);
        agentList.add(address41);

        /* SEED
         TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd
         TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT
         TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx
         TNVTdTSPPqgPMwVhq1xAHX24b1FUtm1EMKhJV
         TNVTdTSPUr48Mp8MTfhtGMT8Q9XdAiWZ9536e
         */
        List<String> packageList = new ArrayList<>();
        packageList.add("TNVTdTSPPqppC8czizjmncqTLNxBfXpBcNh8R");
        packageList.add("TNVTdTSPFg5pk4Qg9UNaMwJwjmwKyTKTQdU8i");
        packageList.add("TNVTdTSPR5cUiU8iLoPdZGU4zshmXBFq1UmCj");
        packageList.add("TNVTdTSPH6fQP6kTAEAWWbo2UTtLSFCawut3p");
        packageList.add("TNVTdTSPK2nrUKvNNXcnzyfGzeZK8GKYZTKaR");
        packageList.add("TNVTdTSPQD2JMBpAb9GNBBxFpiaQhszcx7DtM");
        packageList.add("TNVTdTSPR4QSKNXWfKy9QMUzwV6ioeZ9LcYew");
        packageList.add("TNVTdTSPFhSkwpuyumqp4R2H1oxcwpcHqQ2r5");
        packageList.add("TNVTdTSPVCVuAcXDpjs6khV6tmEvo9C5XKNhY");
        packageList.add("TNVTdTSPQW27Lu4Hk2ZjGVHAKbNn4WNg43Fm5");
        System.out.println(packageList.size());
        int i = 0;
        for (String packageAddress : packageList) {
            String agentAddr = agentList.get(i++);
            Map<String, Object> params = new HashMap<>();
            params.put("agentAddress", agentAddr);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("deposit", "25000000000000"); // 50W
            params.put("packingAddress", packageAddress);
            params.put("password", password);
            params.put("rewardAddress", agentAddr);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
            System.out.println(cmdResp.getResponseData());
        }
    }

    @Test
    public void batchStopAgent() throws Exception {
        List<String> agentList = new ArrayList<>();
        agentList.add(address32);
        agentList.add(address33);
        agentList.add(address34);
        agentList.add(address35);
        agentList.add(address36);
        agentList.add(address37);
        agentList.add(address38);
        agentList.add(address39);
        agentList.add(address40);
        agentList.add(address41);
        System.out.println(agentList.size());
        for (String agent : agentList) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", agent);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", params);
            System.out.println(cmdResp.getResponseData());
        }
    }

    @Test
    public void createAgent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", agentAddress);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("deposit", "27000000000000"); // 27W
        params.put("packingAddress", packageAddress);
        params.put("password", password);
        params.put("rewardAddress", agentAddress);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
        System.out.println(cmdResp.getResponseData());
    }


    @Test
    public void stopAgent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", agentAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", params);
        System.out.println(cmdResp.getResponseData());
    }

    /**
     * 追加保证金
     */
    @Test
    public void appendAgentDeposit() throws Exception {
        String hash = appendAgent(agentAddress, "10000000000000");// 10W
        System.out.println(hash);
    }

    /**
     * 减少保证金
     */
    @Test
    public void reduceAgent() throws Exception {
        String hash = reduceAgent(agentAddress, "6000000000000");// 6W
        System.out.println(hash);
    }

    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < 2000; i++) {
            int oddEven = RandomUtils.nextInt(1, 3);
            // [32, 44]
            int nextInt = RandomUtils.nextInt(32, 45);
            if (nextInt == 42) {
                nextInt = 28;
            } else if (nextInt == 43) {
                nextInt = 27;
            } else if (nextInt == 44) {
                nextInt = 26;
            }
            String behavior = "";
            String hash;
            String address = fieldValue("address" + nextInt);
            // 测试管理员变更
            if (oddEven % 2 == 0) {
                try {
                    behavior = "减少保证金";
                    hash = reduceAgent(address, "2000000000000");// 2w
                } catch (Exception e) {
                    System.err.println(String.format("%s失败, address-%s[%s], msg: %s", behavior, nextInt, address, e.getMessage()));
                    behavior = "追加保证金";
                    hash = appendAgent(address, "2000000000000");// 2w
                }
            } else {
                try {
                    behavior = "追加保证金";
                    hash = appendAgent(address, "2000000000000");// 2w
                } catch (Exception e) {
                    System.err.println(String.format("%s失败, address-%s[%s], msg: %s", behavior, nextInt, address, e.getMessage()));
                    behavior = "减少保证金";
                    hash = reduceAgent(address, "2000000000000");// 2w
                }
            }
            System.out.println(String.format("address-%s[%s] %s 2w, hash: %s", nextInt, address, behavior, hash));
            TimeUnit.SECONDS.sleep(5);
        }

    }

    int bigTotal = 0;
    int depositTotal = 0;
    int withdrawTotal = 0;
    @Test
    public void randomDepositOrWithdrawalTest() throws Exception {
        System.out.println(String.format("开始前，总账户余额: %s, 多签合约余额: %s",
                    ethWalletApi.getERC20Balance("0xf173805F1e3fE6239223B17F0807596Edc283012", USDI),
                    ethWalletApi.getERC20Balance("0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", USDI)
                ));
        for (int i = 0; i < 2000; i++) {
            int oddEven = RandomUtils.nextInt(1, 3);
            // [32, 44]
            int nextInt = RandomUtils.nextInt(32, 45);
            if (nextInt == 42) {
                nextInt = 28;
            } else if (nextInt == 43) {
                nextInt = 27;
            } else if (nextInt == 44) {
                nextInt = 26;
            }
            String behavior;
            String hash;
            String address = fieldValue("address" + nextInt);
            // 测试充值提现
            if (oddEven % 2 == 0) {
                behavior = "eth网络充值";
                hash = dealDepositUSDI(nextInt, address);
            } else {
                behavior = "nerve网络提现";
                hash = dealWithdrawalUSDI(nextInt, address);
            }
            System.out.println(String.format("address-%s[%s] %s, hash: %s", nextInt, address, behavior, hash));
            if (hash == null) {
                continue;
            }
            TimeUnit.SECONDS.sleep(2);
        }
        System.out.println(String.format("结束后，总账户余额: %s, 多签合约余额: %s, 总账户支出: %s, 总充值: %s, 总提现: %s",
                ethWalletApi.getERC20Balance("0xf173805F1e3fE6239223B17F0807596Edc283012", USDI),
                ethWalletApi.getERC20Balance("0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", USDI),
                bigTotal,
                depositTotal,
                withdrawTotal
        ));
    }

    protected ETHWalletApi ethWalletApi;

    @Before
    public void setUp() throws Exception {
        String ethRpcAddress = "https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        ethWalletApi = new ETHWalletApi();
        EthContext.setLogger(Log.BASIC_LOGGER);
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        ethWalletApi.setWeb3j(web3j);
        ethWalletApi.setEthRpcAddress(ethRpcAddress);
    }

    private String dealDepositUSDI(int random, String account) throws Exception {
        /*
        find eth account
        check usdi balance in eth network
        if (no)
            account1 to ethAccount
            return
        ethAccount to 0xce0fb0b8075f8f54517d939cb4887ba42d97a23a
        done
        */
        String account1 = "0xf173805F1e3fE6239223B17F0807596Edc283012";
        String account1PrivateKey = "d15fdd6030ab81cee6b519645f0c8b758d112cd322960ee910f65ad7dbb03c2b";
        String ethAccountPrivateKey = fieldValue("privateKey" + random);
        Credentials credentials = Credentials.create(ethAccountPrivateKey);
        String ethAccount = credentials.getAddress();
        BigInteger USDIBalance = ethWalletApi.getERC20Balance(ethAccount, USDI);
        if (USDIBalance.compareTo(BigInteger.valueOf(1_000000L)) < 0) {
            EthSendTransaction tx = ethWalletApi.transferERC20Token(account1, ethAccount, BigInteger.valueOf(10_000000L), account1PrivateKey, USDI);
            if (StringUtils.isNotBlank(tx.getTransactionHash())) {
                bigTotal += 10;
                System.out.println(String.format("总账户支出10个USDI[%s], ethTxHash: %s, 累积支出: %s", ethAccount, tx.getTransactionHash(), bigTotal));
            }
            return null;
        }
        EthSendTransaction info = ethWalletApi.transferERC20Token(ethAccount, "0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", BigInteger.valueOf(1_000000L), ethAccountPrivateKey, USDI);
        if (StringUtils.isNotBlank(info.getTransactionHash())) {
            depositTotal += 1;
            System.out.println(String.format("    [%s]充值1个USDI, ethTxHash: %s, 累积充值: %s", ethAccount, info.getTransactionHash(), depositTotal));
        }
        System.out.println();
        return info.getTransactionHash();
    }

    private String dealWithdrawalUSDI(int random, String account) throws Exception {
        /*
         check usdi balance in nerve network
         if (no)
            return
         execute
         */
        NerveAssetInfo usdiAssetId = this.findAssetIdByAddress(ethChainId, USDI);
        BigInteger balance = LedgerCall.getBalance(chain, usdiAssetId.getAssetChainId(), usdiAssetId.getAssetId(), account);
        if (balance.compareTo(BigInteger.valueOf(1_000000L)) < 0) {
            return null;
        }
        Credentials credentials = Credentials.create(fieldValue("privateKey" + random));
        return withdrawalToken(ethChainId, USDI, account, credentials.getAddress());
    }

    private String withdrawalToken(int heterogeneousChainId, String token, String account, String ethAccount) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("assetChainId", chainId);
        params.put("assetId", findAssetIdByAddress(heterogeneousChainId, token));
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAddress", ethAccount);
        // 1个token
        params.put("amount", Long.valueOf(1_000000L));
        params.put("remark", "提现");
        params.put("address", "0xf173805F1e3fE6239223B17F0807596Edc283012");
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
        if (cmdResp.getResponseData() == null || ((Map) cmdResp.getResponseData()).get("cv_withdrawal") == null) {
            throw new Exception(String.format("error code: %s, msg: %s", cmdResp.getResponseErrorCode(), cmdResp.getResponseComment()));
        }
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
        String hash = (String) result.get("value");
        withdrawTotal += 1;
        System.out.println(String.format("    [%s]提现1个USDI, nerveTxHash: %s, 累积提现: %s", ethAccount, hash, withdrawTotal));
        return hash;
    }

    protected String fieldValue(String fieldName) throws Exception {
        return this.getClass().getDeclaredField(fieldName).get(this).toString();
    }

    private String appendAgent(String agentAddress, String amount) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", agentAddress);
        params.put("password", password);
        params.put("amount", amount);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_appendAgentDeposit", params);
        if (cmdResp.getResponseData() == null || ((Map) cmdResp.getResponseData()).get("cs_appendAgentDeposit") == null) {
            throw new Exception(String.format("error code: %s, msg: %s", cmdResp.getResponseErrorCode(), cmdResp.getResponseComment()));
        }
        return (String) ((Map) ((Map) cmdResp.getResponseData()).get("cs_appendAgentDeposit")).get("txHash");
    }

    private String reduceAgent(String agentAddress, String amount) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", agentAddress);
        params.put("password", password);
        params.put("amount", amount);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_reduceAgentDeposit", params);
        if (cmdResp.getResponseData() == null || ((Map) cmdResp.getResponseData()).get("cs_reduceAgentDeposit") == null) {
            throw new Exception(String.format("error code: %s, msg: %s", cmdResp.getResponseErrorCode(), cmdResp.getResponseComment()));
        }
        return (String) ((Map) ((Map) cmdResp.getResponseData()).get("cs_reduceAgentDeposit")).get("txHash");
    }

    @Test
    public void ledgerAssetQueryOne() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, 5);

        params.put("assetChainId", 5);
        params.put("assetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "lg_get_asset", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetQueryAll() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "lg_get_all_asset", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetInChainQuery() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", chainId);
        params.put("assetId", 2);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfoByAssetId", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetInChainQueryWhole() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfo", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void proposal() throws Exception {
        //账户已存在则覆盖 If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.REFUND.value());
        params.put("content", "这是提案的内容……");
        params.put("heterogeneousChainId", htChainId);
        params.put("heterogeneousTxHash", "0x80b3129e056623b130eb52fc7d187f23aecfd58fc8c4b82b064198a46a5c64fb");
        params.put("businessAddress", "");
        params.put("hash", "");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "提案");
        params.put("address", agentAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_proposal", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_proposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);

    }

    @Test
    public void voteProposal() throws Exception {
        //账户已存在则覆盖 If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("proposalTxHash", "f627ec78542aab0eadf59b5c3e290d22da655b81d90491a2ce3bc1dcdb043664");
        params.put("choice", ProposalVoteChoiceEnum.FAVOR.value());
        params.put("remark", "投票remark");
        params.put("address", agentAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_voteProposal", params);
        Log.info(JSONUtils.obj2PrettyJson(cmdResp));
        /*HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_voteProposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);*/

    }

    /**
     * 删除账户
     */
    @Test
    public void removeAccountTest() throws Exception {
        removeAccount("TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd", password);
    }

    protected void removeAccount(String address, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_removeAccount", params);
        Log.debug("{}", JSONUtils.obj2json(cmdResp.getResponseData()));
    }

    private void packageZP() {
        agentAddress = packageAddressZP;
        packageAddress = packageAddressZP;
        packageAddressPrivateKey = packageAddressPrivateKeyZP;
    }

    private void packageNE() {
        agentAddress = packageAddressNE;
        packageAddress = packageAddressNE;
        packageAddressPrivateKey = packageAddressPrivateKeyNE;
    }

    private void packageHF() {
        agentAddress = packageAddressHF;
        packageAddress = packageAddressHF;
        packageAddressPrivateKey = packageAddressPrivateKeyHF;
    }

    private void package6() {
        agentAddress = address26;
        packageAddress = packageAddress6;
        packageAddressPrivateKey = packageAddressPrivateKey6;
    }

    private void package7() {
        agentAddress = address27;
        packageAddress = packageAddress7;
        packageAddressPrivateKey = packageAddressPrivateKey7;
    }

    private void package8() {
        agentAddress = address28;
        packageAddress = packageAddress8;
        packageAddressPrivateKey = packageAddressPrivateKey8;
    }

    static class CoinDTO {
        private String address;
        private Integer assetsChainId;
        private Integer assetsId;
        private BigInteger amount;
        private String password;
        private long lockTime;

        public CoinDTO(String address, Integer assetsChainId, Integer assetsId, BigInteger amount, String password, long lockTime) {
            this.address = address;
            this.assetsChainId = assetsChainId;
            this.assetsId = assetsId;
            this.amount = amount;
            this.password = password;
            this.lockTime = lockTime;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setAssetsChainId(Integer assetsChainId) {
            this.assetsChainId = assetsChainId;
        }

        public void setAssetsId(Integer assetsId) {
            this.assetsId = assetsId;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setLockTime(long lockTime) {
            this.lockTime = lockTime;
        }

        public String getAddress() {
            return address;
        }

        public Integer getAssetsChainId() {
            return assetsChainId;
        }

        public Integer getAssetsId() {
            return assetsId;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public String getPassword() {
            return password;
        }

        public long getLockTime() {
            return lockTime;
        }
    }
}
