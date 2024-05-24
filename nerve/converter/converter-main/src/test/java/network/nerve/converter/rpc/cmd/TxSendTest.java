package network.nerve.converter.rpc.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
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
import network.nerve.converter.heterogeneouschain.arbitrum.context.ArbitrumContext;
import network.nerve.converter.heterogeneouschain.avax.context.AvaxContext;
import network.nerve.converter.heterogeneouschain.basechain.context.BaseChainContext;
import network.nerve.converter.heterogeneouschain.bch.context.BchContext;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.brise.context.BriseContext;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.celo.context.CeloContext;
import network.nerve.converter.heterogeneouschain.cro.context.CroContext;
import network.nerve.converter.heterogeneouschain.enuls.context.EnulsContext;
import network.nerve.converter.heterogeneouschain.eos.context.EosContext;
import network.nerve.converter.heterogeneouschain.etc.context.EtcContext;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.ethw.context.EthwContext;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.ftm.context.FtmContext;
import network.nerve.converter.heterogeneouschain.goerlieth.context.GoerliContext;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.iotx.context.IotxContext;
import network.nerve.converter.heterogeneouschain.janus.context.JanusContext;
import network.nerve.converter.heterogeneouschain.kroma.context.KromaContext;
import network.nerve.converter.heterogeneouschain.manta.context.MantaContext;
import network.nerve.converter.heterogeneouschain.kava.context.KavaContext;
import network.nerve.converter.heterogeneouschain.kcs.context.KcsContext;
import network.nerve.converter.heterogeneouschain.klay.context.KlayContext;
import network.nerve.converter.heterogeneouschain.linea.context.LineaContext;
import network.nerve.converter.heterogeneouschain.mint.context.MintContext;
import network.nerve.converter.heterogeneouschain.pulse.context.PulseContext;
import network.nerve.converter.heterogeneouschain.scroll.context.ScrollContext;
import network.nerve.converter.heterogeneouschain.matic.context.MaticContext;
import network.nerve.converter.heterogeneouschain.metis.context.MetisContext;
import network.nerve.converter.heterogeneouschain.okt.context.OktContext;
import network.nerve.converter.heterogeneouschain.one.context.OneContext;
import network.nerve.converter.heterogeneouschain.optimism.context.OptimismContext;
import network.nerve.converter.heterogeneouschain.rei.context.ReiContext;
import network.nerve.converter.heterogeneouschain.blast.context.BlastContext;
import network.nerve.converter.heterogeneouschain.merlin.context.MerlinContext;
import network.nerve.converter.heterogeneouschain.mode.context.ModeContext;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.x1.context.X1Context;
import network.nerve.converter.heterogeneouschain.zeta.context.ZetaContext;
import network.nerve.converter.heterogeneouschain.zk.context.ZkContext;
import network.nerve.converter.heterogeneouschain.zkpolygon.context.ZkpolygonContext;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.bo.NonceBalance;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.ConverterUtil;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;

import java.io.File;
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

    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";// 9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";// 477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";// 8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";// 4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";// bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";// ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";// 4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";// 3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";// 27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";// 76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b
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

    String packageAddressZP = "TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd";// 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
    String packageAddressNE = "TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT";// 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
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

    Map<String, Object> pMap;

    /** Mint testnet */
    static String USDT_MINT = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_MINT = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_MINT_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Pulse testnet */
    static String USDT_PULSE = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_PULSE = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_PULSE_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Merlin testnet */
    static String USDT_MERLIN = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_MERLIN = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_MERLIN_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Blast testnet */
    static String USDT_BLAST = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_BLAST = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_BLAST_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Mode testnet */
    static String USDT_MODE = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_MODE = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_MODE_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Kroma testnet */
    static String USDT_KROMA = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String USD18_KROMA = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";
    static String NVT_KROMA_MINTER = "0x348371cFc7782d336C890B733d792258E1809216";

    /** ZETA testnet */
    static String USDT_ZETA = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_ZETA = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_ZETA_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** X1 testnet */
    static String USDT_X1 = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_X1 = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_X1_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Manta testnet */
    static String USDT_MANTA = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_MANTA = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_MANTA_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** Janus testnet */
    static String USDT_JANUS = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_JANUS = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_JANUS_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** brise testnet */
    static String USDT_BRISE = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_BRISE = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_BRISE_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** scroll testnet */
    static String USDT_SCROLL = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_SCROLL = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_SCROLL_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** base chain testnet */
    static String USDT_BASECHAIN = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_BASECHAIN = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_BASECHAIN_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** etc testnet */
    static String USDT_ETC = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_ETC = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_ETC_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** celo testnet */
    static String USDT_CELO = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_CELO = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_CELO_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** linea testnet */
    static String USDT_LINEA = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";
    static String USD18_LINEA = "0x348371cFc7782d336C890B733d792258E1809216";
    static String NVT_LINEA_MINTER = "0x7C2439d104Ee2C459C76C3E0415C2aaFc2CE018B";

    /** zkpolygon testnet */
    static String USDT_ZKP = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_ZKP = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_ZKP_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** EOS testnet */
    static String USDT_EOS = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_EOS = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_EOS_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** ZK testnet */
    //static String USDT_ZK = "0x3ac3a8Ca2745b5724b966Cd9Adb5D6aC39FC1EF9";
    //static String USD18_ZK = "0x095A09d3617fc01447427897EA9635c676B944df";
    //static String NVT_ZK_MINTER = "0xFF49575bc46A86d238f4bC708fe28e7C6443592D";
    /** ZK mainnet */
    static String USDT_ZK = "0x80d1437b27Fc18dFf50Cf4cB918d78d38B9e4c30";
    static String USD18_ZK = "0x3ac3a8Ca2745b5724b966Cd9Adb5D6aC39FC1EF9";
    static String NVT_ZK_MINTER = "0xFF49575bc46A86d238f4bC708fe28e7C6443592D";

    /** REI */
    static String USDT_REI = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_REI = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_REI_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** ETHW */
    static String USDT_ETHW = "0xF2e1C076eede6F0B8d82eE78fa12112DDEfb5f06";
    static String USD18_ETHW = "0x21615a84D8D834ca598a59D8E5C810128403DfaE";
    static String NVT_ETHW_MINTER = "0xE6b360C49A316fcc71d55B3074160ee043a7BD8B";

    /** KAVA */
    static String USDT_KAVA = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_KAVA = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_KAVA_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** ENULS */
    static String USDT_ENULS = "0x5045b6a04AC33f8D16d47E46b971C848141eE270";
    static String USD18_ENULS = "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9";
    static String NVT_ENULS_MINTER = "0x03Cf96223BD413eb7777AFE3cdb689e7E851CB32";

    /** GOERLI */
    static String USDT_GOERLI = "0x74A163fCd791Ec7AaB2204ffAbf1A1DFb8854883";
    static String USD18_GOERLI = "0x56F175D48211e7D018ddA7f0A0B51bcfB405AE69";
    static String NVT_GOERLI_MINTER = "0xab34B1F41dA5a32fdE53850EfB3e54423e93483e";

    /** BCH */
    static String USDT_BCH = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_BCH = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_BCH_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** KLAY */
    static String USDT_KLAY = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_KLAY = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_KLAY_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** OPTIMISM */
    static String USDT_OPTIMISM = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_OPTIMISM = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_OPTIMISM_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** IOTX */
    static String USDT_IOTX = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String USD18_IOTX = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String NVT_IOTX_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";

    /** METIS */
    static String USDT_METIS = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String USD18_METIS = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String NVT_METIS_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";

    /** FTM */
    static String USDT_FTM = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_FTM = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String NVT_FTM_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";

    /** Arbitrum ETH */
    static String USDT_ARBI = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_ARBI = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String NVT_ARBI_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";

    /** AVAX */
    static String USDT_AVAX = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String USD18_AVAX = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String NVT_AVAX_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";

    /** CRO */
    static String USDT_CRO = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String USD18_CRO = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NVT_CRO_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** TRX */
    static String DX_TRX_6 = "TEzJjjC4NrLrYFthGFHzQon5zrErNw1JN9";
    static String FCI_TRX_18 = "TFqCQLGxG2o188eESoYkr1Ji9x85SEXBDP";
    static String USDT_TRX_6 = "TXCWs4vtLW2wYFHfi7xWeiC9Kuj2jxpKqJ";
    static String NVT_TRX_MINTER = "TJa51xhiz6eLo2jtf8pxPGJ1AjXNvqPC49";

    /** Kcs */
    static String USDT_KCS = "0xbBF80744c94C85d65B08290860df6ff04089F050";
    static String NVT_KCS_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String NULS_KCS_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";

    /** Matic */
    static String USDT_MATIC = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String NVT_MATIC_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String NULS_MATIC_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** One */
    static String USDT_ONE = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String NVT_ONE_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    static String NULS_ONE_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** Okt */
    static String OKUSD_OKT_18 = "0x031012aDC3aa572357df286F11CDA27893B008B1";
    static String OKUSD_OKT_8 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String USDT_OKT = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String USDX_OKT = "0x74A163fCd791Ec7AaB2204ffAbf1A1DFb8854883";
    static String USDX_OKT_MINTER = "0x64fBEf777594Bd2f763101abde343AF72CF1f8d3";
    static String NVT_OKT_MINTER = "0x8B3b22C252F431a75644E544FCAf67E390A206F4";
    static String BNB_OKT_MINTER = "0xd136b3DbB573e9CC3f52CDB46e097D495A7dF80A";
    //static String NULS_OKT_MINTER = "0xf3392C7b8d47694c10564C85736d6B0643b1761E";
    //static String ETH_OKT_MINTER = "0xbBF80744c94C85d65B08290860df6ff04089F050";

    /** Huobi */
    static String HUSD_HT_8 = "0x91a1C7e0eCC46326fF3d916bBB73416D95097a5D";
    static String HUSD_HT_18 = "0x10B382863647C4610050A69fBc1E582aC29fE58A";
    static String USDT_HT = "0x04f535663110a392a6504839beed34e019fdb4e0";
    static String GOAT_HT = "0x81793d5d366a3f8884aceac9ea0dea012e31809e";//decimals=9
    static String USDX_HT = "0x03Cf96223BD413eb7777AFE3cdb689e7E851CB32";
    static String USDI_HT = "0x3F1f3D17619E916C4F04707BA57d8E0b9e994fB0";
    static String DEX_HT_18 = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
    static String NVT_HT_MINTER = "0x8999d8738CC9B2E1fb1D01E1af732421D53Cb2A9";
    //static String NULS_HT_MINTER = "0x5045b6a04AC33f8D16d47E46b971C848141eE270";
    //static String ETH_HT_MINTER = "0x830befa62501F1073ebE2A519B882e358f2a0318";

    /** Binance */
    static String BUSD_BNB_18 = "0x02e1aFEeF2a25eAbD0362C4Ba2DC6d20cA638151";
    static String USDX_BNB = "0xb6D685346106B697E6b2BbA09bc343caFC930cA3";
    static String DXA_BNB_8 = "0x3139dbe1bf7feb917cf8e978b72b6ead764b0e6c";
    static String GOAT_BNB_9 = "0xba0147e9c99b0467efe7a9c51a2db140f1881db5";
    static String SAFEMOON_BNB_9 = "0x7be69eb38443d3a632cb972df840013d667365e6";
    static String NVT_BNB_MINTER = "0x3F1f3D17619E916C4F04707BA57d8E0b9e994fB0";
    static String TRX_BNB_MINTER = "0x3fc005d5552a5a8236f366fB6Cca94527889Ec35";
    static String USDT_BNB_MINTER = "0xB8aAE3a961b9Fd45302c20e5346441ADB4cB0d28";
    //static String NULS_BNB_MINTER = "0x2eDCf5f18D949c51776AFc42CDad667cDA2cF862";
    //static String DXA_BNB_MINTER = "0x3139dBe1Bf7FEb917CF8e978b72b6eAd764b0e6C";
    //static String USDI_BNB_MINTER = "0xF3B4771813f27C390B11703450F5E188b83829F9";
    //static String ETH_BNB_MINTER = "0x9296D0AF7DA81AAD9ae273118Ba377403db6691a";
    static String BUG_BNB_18 = "0x90C89b9f9c4605a887540FaD286E02B71f03D70d";


    /** Ethereum */
    static String USDX = "0xB058887cb5990509a3D0DD2833B2054E4a7E4a55";
    static String USDI = "0x1c78958403625aeA4b0D5a0B527A27969703a270";
    static String ENVT = "0x53be0d78b686f68643c38dfcc4f141a0c2785a08";
    // ERC20Minter
    static String NVT_ETH_MINTER = "0x25EbbAC2CA9DB0c1d6F0fc959BbC74985417BaB0";
    static String NVT18 = "0x4860dfB7b6a66344ea2dCF18E9fE7CDa5a7c0724";
    static String NULS18 = "0x7E6B3AcC5AE8Ecbe008aca3C11E22ff12B77d17f";
    //static String NULS_ETH_MINTER = "0x79D7c11CC945a1734d21Ef41e631EFaE894Af2C3";
    //static String DXA_ETH_MINTER = "0xf71f9DAcf995895a1ae10482F3b7806e22D1Ea03";

    static String DAI = "0xad6d458402f60fd3bd25163575031acdce07538d";
    static String FAU = "0xfab46e002bbf0b4509813474841e0716e6730136";
    static String MT = "0x9b4e2b4b13d125238aa0480dd42b4f6fc71b37cc";
    //symbol: DAI, decimals: 18, address: 0xad6d458402f60fd3bd25163575031acdce07538d
    //symbol: FAU, decimals: 18, address: 0xfab46e002bbf0b4509813474841e0716e6730136

    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;
    EthIIContext ethIIContext = new EthIIContext();
    BnbContext bnbContext = new BnbContext();
    HtContext htContext = new HtContext();
    OktContext oktContext = new OktContext();
    OneContext oneContext = new OneContext();
    MaticContext maticContext = new MaticContext();
    KcsContext kcsContext = new KcsContext();
    TrxContext trxContext = new TrxContext();
    CroContext croContext = new CroContext();
    AvaxContext avaxContext = new AvaxContext();
    ArbitrumContext arbitrumContext = new ArbitrumContext();
    FtmContext ftmContext = new FtmContext();
    MetisContext metisContext = new MetisContext();
    IotxContext iotxContext = new IotxContext();
    OptimismContext optimismContext = new OptimismContext();
    KlayContext klayContext = new KlayContext();
    BchContext bchContext = new BchContext();
    GoerliContext goerliContext = new GoerliContext();
    EnulsContext enulsContext = new EnulsContext();
    KavaContext kavaContext = new KavaContext();
    EthwContext ethwContext = new EthwContext();
    ReiContext reiContext = new ReiContext();
    ZkContext zkContext = new ZkContext();
    EosContext eosContext = new EosContext();
    ZkpolygonContext zkpolygonContext = new ZkpolygonContext();
    LineaContext lineaContext = new LineaContext();
    CeloContext celoContext = new CeloContext();
    EtcContext etcContext = new EtcContext();
    BaseChainContext baseChainContext = new BaseChainContext();
    ScrollContext scrollContext = new ScrollContext();
    BriseContext briseContext = new BriseContext();
    JanusContext janusContext = new JanusContext();
    MantaContext mantaContext = new MantaContext();
    FchContext fchContext = new FchContext();
    BtcContext btcContext = new BtcContext();
    X1Context x1Context = new X1Context();
    ZetaContext zetaContext = new ZetaContext();
    KromaContext kromaContext = new KromaContext();
    ModeContext modeContext = new ModeContext();
    BlastContext blastContext = new BlastContext();
    MerlinContext merlinContext = new MerlinContext();
    PulseContext pulseContext = new PulseContext();
    MintContext mintContext = new MintContext();

    static int ethChainId = 101;
    static int bnbChainId = 102;
    static int htChainId = 103;
    static int oktChainId = 104;
    static int oneChainId = 105;
    static int maticChainId = 106;
    static int kcsChainId = 107;
    static int trxChainId = 108;
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

        try {
            String path = new File(TxSendTest.class.getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
            packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
            packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
            packageAddressPrivateKeyZP = pMap.get(packageAddressZP).toString();
            packageAddressPrivateKeyNE = pMap.get(packageAddressNE).toString();
            packageAddressPrivateKeyHF = pMap.get(packageAddressHF).toString();
            address31 = "TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad";
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Set consensus node address and block output address
        packageZP();
    }

    // 0x09534d4692F568BC6e9bef3b4D84d48f19E52501 [Account3]
    // 0xF3c90eF58eC31805af11CE5FA6d39E395c66441f [Account4]
    // 0x6afb1F9Ca069bC004DCF06C51B42992DBD90Adba [Account5]
    // Private key: 43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D / 0x8F05AE1C759b8dB56ff8124A89bb1305ECe17B65 [Account 6] / tNULSeBaMfmpwBtUSHyLCGHq4WqYY5A4Dxak91
    // Private key: 0935E3D8C87C2EA5C90E3E3A0509D06EB8496655DB63745FAE4FF01EB2467E85 / 0xd29E172537A3FB133f790EBE57aCe8221CB8024F [Account 7] / tNULSeBaMjqtMNhWWyUKZUsGhWaRd88RMrSU6J
    // Private key: CCF560337BA3DE2A76C1D08825212073B299B115474B65DE4B38B587605FF7F2 / 0x54eAB3868B0090E6e1a1396E0e54F788a71B2b17 [Account 8] / tNULSeBaMrmiuHZg9c2JVAbLQydAxjNvuKRgFj
    @Test
    public void importPriKeyTest() {
        // HF: 0x16534991E80117Ca16c724C991aad9EAbd1D7ebe
        //Public key: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        //importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//Seed block address tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp, 0xdd7CBEdDe731e78e8b8E4b2c212bC42fA7C09D03
        //Public key: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
        //importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//Seed block address tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe, 0xD16634629C638EFd8eD90bB096C216e7aEc01A91
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
        importPriKey("B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF", password);//30 ETH Test network address tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
        importPriKey("4594348E3482B751AA235B8E580EFEF69DB465B3A291C5662CEDA6459ED12E39", password);//31 Test network address tNULSeBaMrQaVh1V7LLvbKa5QSN54bS4sdbXaF, 0xc11D9943805e56b630A401D4bd9A29550353EFa1 [Account 9]
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
        importPriKey(pMap.get("0xc9afb4fa1d7e2b7d324b7cb1178417ff705f5996").toString(), password);//52 TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad 0xc9afb4fa1d7e2b7d324b7cb1178417ff705f5996

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
            //Overwrite if account already exists If the account exists, it covers.
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
        //getBalanceByAddress("QA-User address", "TNVTdTSPSShZokMfXRo82TP2Kq6Fc2nhMNmF7");
        getBalanceByAddress("address31-User address", address31);
        //getBalanceByAddress("packageAddressZP-User address", packageAddressZP);
        //getBalanceByAddress("address32-User address", address32);
    }

    protected void getBalanceByAddress(String title, String address) throws Exception {
        System.out.println();
        System.out.println(String.format("%s address: %s", title, address));

        this.balanceInfoPrint("　Main asset NVT", new NerveAssetInfo(chainId, assetId), address, 8);
        //this.balanceInfoPrint("asset USDTN", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset NVT_USDTN_LP", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset BNB", new NerveAssetInfo(5, 2), address, 18);
        //this.balanceInfoPrint("asset OKB", new NerveAssetInfo(5, 2), address, 18);
        //this.balanceInfoPrint("asset PLS", new NerveAssetInfo(5, 3), address, 18);
        this.balanceInfoPrint("asset Mint-ETH", new NerveAssetInfo(5, 2), address, 18);
        this.balanceInfoPrint("asset FCH", new NerveAssetInfo(5, 3), address, 8);
        this.balanceInfoPrint("asset BTC", new NerveAssetInfo(5, 4), address, 8);
        //this.balanceInfoPrint("asset Mode-ETH", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset Blast-ETH", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset Merlin-BTC", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset BTC", new NerveAssetInfo(5, 4), address, 8);
        //this.balanceInfoPrint("asset FCH", new NerveAssetInfo(5, 3), address, 8);
        //this.balanceInfoPrint("asset MANTA-ETH", new NerveAssetInfo(5, 2), address, 18);
        this.balanceInfoPrint("asset USDT(mint)", new NerveAssetInfo(5, 5), address, 6);
        this.balanceInfoPrint("asset USD18(mint)", new NerveAssetInfo(5, 6), address, 18);
        //this.balanceInfoPrint("asset USDT(pls)", new NerveAssetInfo(5, 6), address, 6);
        //this.balanceInfoPrint("asset USD18(pls)", new NerveAssetInfo(5, 7), address, 18);
        //this.balanceInfoPrint("asset U1D", new NerveAssetInfo(5, 8), address, 18);
        //this.balanceInfoPrint("asset U2D", new NerveAssetInfo(5, 9), address, 18);
        //this.balanceInfoPrint("asset U3D", new NerveAssetInfo(5, 10), address, 18);
        //this.balanceInfoPrint("asset U4D", new NerveAssetInfo(5, 11), address, 18);
        //this.balanceInfoPrint("asset USDT(mode)", new NerveAssetInfo(5, 6), address, 6);
        //this.balanceInfoPrint("asset USD18(mode)", new NerveAssetInfo(5, 7), address, 18);
        //this.balanceInfoPrint("asset USDT(blast)", new NerveAssetInfo(5, 10), address, 6);
        //this.balanceInfoPrint("asset USD18(blast)", new NerveAssetInfo(5, 11), address, 18);
        //this.balanceInfoPrint("asset USDT(merlin)", new NerveAssetInfo(5, 12), address, 6);
        //this.balanceInfoPrint("asset USD18(merlin)", new NerveAssetInfo(5, 13), address, 18);
        //this.balanceInfoPrint("asset USDT(manta)", new NerveAssetInfo(5, 8), address, 6);
        //this.balanceInfoPrint("asset USD18(manta)", new NerveAssetInfo(5, 9), address, 18);
        //this.balanceInfoPrint("asset JANUS", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset BRISE", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset EOS", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset CELO", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset ETC", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset Base-ETH", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset Scroll-ETH", new NerveAssetInfo(5, 6), address, 18);
        //this.balanceInfoPrint("asset LINEA-ETH", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset EOS", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset POLYGON-ETH", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset TRX", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset METIS", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset IOTX", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset OETH", new NerveAssetInfo(5, 10), address, 18);
        //this.balanceInfoPrint("asset KLAY", new NerveAssetInfo(5, 13), address, 18);
        //this.balanceInfoPrint("asset BCH", new NerveAssetInfo(5, 16), address, 18);
        //this.balanceInfoPrint("asset goerliETH", new NerveAssetInfo(5, 2), address, 18);
        //this.balanceInfoPrint("asset ENULS", new NerveAssetInfo(5, 3), address, 8);
        //this.balanceInfoPrint("asset KAVA", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset ETHW", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset REI", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset ZK-ETH", new NerveAssetInfo(5, 3), address, 18);
        //this.balanceInfoPrint("asset USDX(bsc)", new NerveAssetInfo(5, 9), address, 6);
        //this.balanceInfoPrint("asset BUSD(bsc)", new NerveAssetInfo(5, 10), address, 18);
        //this.balanceInfoPrint("asset USDX(okt)", new NerveAssetInfo(5, 5), address, 6);
        //this.balanceInfoPrint("asset USDT(okt)", new NerveAssetInfo(5, 6), address, 6);
        //this.balanceInfoPrint("asset USDT(trx)", new NerveAssetInfo(5, 10), address, 6);
        //this.balanceInfoPrint("asset USDT(iotx)", new NerveAssetInfo(5, 8), address, 6);
        //this.balanceInfoPrint("asset USD18(iotx)", new NerveAssetInfo(5, 9), address, 18);
        //this.balanceInfoPrint("asset USDT(optimism)", new NerveAssetInfo(5, 11), address, 6);
        //this.balanceInfoPrint("asset USD18(optimism)", new NerveAssetInfo(5, 12), address, 18);
        //this.balanceInfoPrint("asset USDT(klay)", new NerveAssetInfo(5, 14), address, 6);
        //this.balanceInfoPrint("asset USD18(klay)", new NerveAssetInfo(5, 15), address, 18);
        //this.balanceInfoPrint("asset USDT(bch)", new NerveAssetInfo(5, 17), address, 6);
        //this.balanceInfoPrint("asset USD18(bch)", new NerveAssetInfo(5, 18), address, 18);
        //this.balanceInfoPrint("asset USDT(goerli)", new NerveAssetInfo(5, 4), address, 6);
        //this.balanceInfoPrint("asset USD18(goerli)", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset USDT(enuls)", new NerveAssetInfo(5, 4), address, 6);
        //this.balanceInfoPrint("asset USD18(enuls)", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset USDT(kava)", new NerveAssetInfo(5, 4), address, 6);
        //this.balanceInfoPrint("asset USD18(kava)", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset USDT(ethw)", new NerveAssetInfo(5, 4), address, 6);
        //this.balanceInfoPrint("asset USD18(ethw)", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset USDT(zk)", new NerveAssetInfo(5, 4), address, 6);
        //this.balanceInfoPrint("asset USD18(zk)", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset USDT(eos)", new NerveAssetInfo(5, 10), address, 6);
        //this.balanceInfoPrint("asset USD18(eos)", new NerveAssetInfo(5, 11), address, 18);
        //this.balanceInfoPrint("asset USDT(zkpolygon)", new NerveAssetInfo(5, 9), address, 6);
        //this.balanceInfoPrint("asset USD18(zkpolygon)", new NerveAssetInfo(5, 10), address, 18);
        //this.balanceInfoPrint("asset USDT(celo)", new NerveAssetInfo(5, 12), address, 6);
        //this.balanceInfoPrint("asset USD18(celo)", new NerveAssetInfo(5, 13), address, 18);
        //this.balanceInfoPrint("asset USDT(etc)", new NerveAssetInfo(5, 11), address, 6);
        //this.balanceInfoPrint("asset USD18(etc)", new NerveAssetInfo(5, 12), address, 18);
        //this.balanceInfoPrint("asset USDT(base)", new NerveAssetInfo(5, 5), address, 6);
        //this.balanceInfoPrint("asset USD18(base)", new NerveAssetInfo(5, 6), address, 18);
        //this.balanceInfoPrint("asset USDT(scroll)", new NerveAssetInfo(5, 15), address, 6);
        //this.balanceInfoPrint("asset USD18(scroll)", new NerveAssetInfo(5, 16), address, 18);
        //this.balanceInfoPrint("asset USDT(brise)", new NerveAssetInfo(5, 7), address, 6);
        //this.balanceInfoPrint("asset USD18(brise)", new NerveAssetInfo(5, 8), address, 18);
        //this.balanceInfoPrint("asset USDT(janus)", new NerveAssetInfo(5, 7), address, 6);
        //this.balanceInfoPrint("asset USD18(janus)", new NerveAssetInfo(5, 8), address, 18);

        //this.balanceInfoPrint("Tron-assetTRX", this.findAssetIdByHeterogeneousId(trxChainId, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Tron-assetDX", this.findAssetIdByAddress(trxChainId, DX_TRX_6), address);
        //this.balanceInfoPrint("Tron-assetUSDT", this.findAssetIdByAddress(trxChainId, USDT_TRX_6), address);

        //this.balanceInfoPrint("Harmony-assetONE", this.findAssetIdByHeterogeneousId(oneChainId, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Harmony-assetUSDT", this.findAssetIdByAddress(oneChainId, USDT_ONE), address);
        //this.balanceInfoPrint("Polygon-assetMATIC", this.findAssetIdByHeterogeneousId(maticChainId, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Polygon-assetUSDT", this.findAssetIdByAddress(maticChainId, USDT_MATIC), address);
        //this.balanceInfoPrint("Kucoin-assetKCS", this.findAssetIdByHeterogeneousId(kcsChainId, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Kucoin-assetUSDT", this.findAssetIdByAddress(kcsChainId, USDT_KCS), address);

        //this.balanceInfoPrint("Cro-assetCRO", this.findAssetIdByHeterogeneousId(croContext.HTG_CHAIN_ID, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Cro-assetUSDT", this.findAssetIdByAddress(croContext.HTG_CHAIN_ID, USDT_CRO), address);
        //this.balanceInfoPrint("Cro-assetUSD18", this.findAssetIdByAddress(croContext.HTG_CHAIN_ID, USD18_CRO), address);
        //
        //this.balanceInfoPrint("Avax-assetAVAX", this.findAssetIdByHeterogeneousId(avaxContext.HTG_CHAIN_ID, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Avax-assetUSDT", this.findAssetIdByAddress(avaxContext.HTG_CHAIN_ID, USDT_AVAX), address);
        //this.balanceInfoPrint("Avax-assetUSD18", this.findAssetIdByAddress(avaxContext.HTG_CHAIN_ID, USD18_AVAX), address);
        //
        //this.balanceInfoPrint("Arbitrum-assetAETH", this.findAssetIdByHeterogeneousId(arbitrumContext.HTG_CHAIN_ID, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Arbitrum-assetUSDT", this.findAssetIdByAddress(arbitrumContext.HTG_CHAIN_ID, USDT_ARBI), address);
        //this.balanceInfoPrint("Arbitrum-assetUSD18", this.findAssetIdByAddress(arbitrumContext.HTG_CHAIN_ID, USD18_ARBI), address);
        //
        //this.balanceInfoPrint("Ftm-assetFTM", this.findAssetIdByHeterogeneousId(ftmContext.HTG_CHAIN_ID, heterogeneousAssetId), address);
        //this.balanceInfoPrint("Ftm-assetUSDT", this.findAssetIdByAddress(ftmContext.HTG_CHAIN_ID, USDT_FTM), address);
        //this.balanceInfoPrint("Ftm-assetUSD18", this.findAssetIdByAddress(ftmContext.HTG_CHAIN_ID, USD18_FTM), address);

        //this.balanceInfoPrint("Ethereum-assetETH", this.findAssetIdByHeterogeneousId(EthConstant.ETH_CHAIN_ID, EthConstant.ETH_ASSET_ID), address);
        //this.balanceInfoPrint("Ethereum-assetUSDX", this.findAssetIdByAddress(ethChainId, USDX), address, 6);
        //this.balanceInfoPrint("Ethereum-assetUSDI", this.findAssetIdByAddress(ethChainId, USDI), address);
        //this.balanceInfoPrint("BSC-assetBNB", this.findAssetIdByHeterogeneousId(bnbChainId, heterogeneousAssetId), address);
        //this.balanceInfoPrint("BSC-assetUSDX", this.findAssetIdByAddress(bnbChainId, USDX_BNB), address);
        //this.balanceInfoPrint("BSC-assetBUG", this.findAssetIdByAddress(bnbChainId, BUG_BNB_18), address);
        //this.balanceInfoPrint("BSC-assetBUSD", this.findAssetIdByAddress(bnbChainId, BUSD_BNB_18), address, 18);
        //this.balanceInfoPrint("HT-assetHT", this.findAssetIdByHeterogeneousId(htChainId, heterogeneousAssetId), address, 18);
        //this.balanceInfoPrint("HT-assetUSDX", this.findAssetIdByAddress(htChainId, USDX_HT), address, 6);
        //this.balanceInfoPrint("HT-assetHUSD", this.findAssetIdByAddress(htChainId, HUSD_HT_18), address, 18);
        //this.balanceInfoPrint("OKT-assetOKT", this.findAssetIdByHeterogeneousId(oktChainId, heterogeneousAssetId), address, 18);
        //this.balanceInfoPrint("OKT-assetUSDX", this.findAssetIdByAddress(oktChainId, USDX_OKT), address, 6);
        //this.balanceInfoPrint("OKT-assetUSDT", this.findAssetIdByAddress(oktChainId, USDT_OKT), address, 6);
        //this.balanceInfoPrint("OKT-assetOKUSD", this.findAssetIdByAddress(oktChainId, OKUSD_OKT_8), address, 8);
        //
        //this.balanceInfoPrint("Parallel assetsNULS", new NerveAssetInfo(1, 1), address);
        //this.balanceInfoPrint("asset USDX6", new NerveAssetInfo(5, 3), address, 6);
        //this.balanceInfoPrint("asset BUSD18", new NerveAssetInfo(5, 4), address, 18);
        //this.balanceInfoPrint("asset HUSD18", new NerveAssetInfo(5, 5), address, 18);
        //this.balanceInfoPrint("asset OKUSD8", new NerveAssetInfo(5, 6), address, 8);
        //this.balanceInfoPrint("asset KXT", new NerveAssetInfo(5, 7), address, 8);
        //this.balanceInfoPrint("asset KBT", new NerveAssetInfo(5, 8), address, 8);
        //this.balanceInfoPrint("asset KHT", new NerveAssetInfo(5, 9), address, 8);
        //this.balanceInfoPrint("asset KOT", new NerveAssetInfo(5, 10), address, 8);

    }

    private void balanceInfoPrint(String desc, NerveAssetInfo token, String address) {
        this.balanceInfoPrint(desc, token, address, -1);
    }
    private void balanceInfoPrint(String desc, NerveAssetInfo token, String address, int decimals) {
        BigInteger balance = LedgerCall.getBalance(chain, token.getAssetChainId(), token.getAssetId(), address);
        String balanceStr;
        if (decimals >= 0) {
            balanceStr = new BigDecimal(balance).movePointLeft(decimals).stripTrailingZeros().toPlainString();
        } else {
            balanceStr = balance.toString();
        }
        System.out.println(String.format("%s %s-%s: %s", desc, token.getAssetChainId(), token.getAssetId(), balanceStr));
    }

    @Test
    public void getNonceAndBalance() throws Exception {
        NonceBalance b = LedgerCall.getBalanceNonce(chain, chainId, assetId, "TNVTdTSPyT1GGPrbahr9qo7S87dMBatx9NHtP");
        System.out.println(b.getAvailable());
        System.out.println(HexUtil.encode(b.getNonce()));
        //BigInteger balance2 = LedgerCall.getBalance(chain, chainId, assetId, agentAddress);
        //System.out.println(balance2);
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
        String txStr = (String)(getTxCfmClient("24d3ae873ab659213079f2da20f2911c522222a73d5159dafff663ae0216452d").get("tx"));
        System.out.println(txStr);
        Transaction tx = ConverterUtil.getInstance(txStr, Transaction.class);//The last one
        System.out.println(tx.format());
        //System.out.println(tx.format(WithdrawalHeterogeneousSendTxData.class));
        //System.out.println(tx.format(WithdrawalTxData.class));
        //System.out.println(tx.format(ConfirmWithdrawalTxData.class));
        //System.out.println(tx.format(RechargeUnconfirmedTxData.class));
        //System.out.println(tx.format(RechargeTxData.class));
    }

    @Test
    public void withdrawalMintETH() throws Exception {
        int htgChainId = mintContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.00002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalPLS() throws Exception {
        int htgChainId = pulseContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalMerlinBTC() throws Exception {
        int htgChainId = merlinContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0000001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(2000_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBlastETH() throws Exception {
        int htgChainId = blastContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0000001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalModeETH() throws Exception {
        int htgChainId = modeContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0000001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalKroma() throws Exception {
        int htgChainId = kromaContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.00002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(60_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalZeta() throws Exception {
        int htgChainId = zetaContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.00002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(60_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBTC() throws Exception {
        int htgChainId = btcContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "mmLahgkWGHQSKszCDcZXPooWoRuYhQPpCF";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.00001129").movePointRight(8).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(29_1000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalOKB() throws Exception {
        int htgChainId = x1Context.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(600_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalManta() throws Exception {
        int htgChainId = mantaContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(5_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalJanus() throws Exception {
        int htgChainId = janusContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBrise() throws Exception {
        int htgChainId = briseContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalScrollETH() throws Exception {
        int htgChainId = scrollContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }
    @Test
    public void withdrawalBaseETH() throws Exception {
        int htgChainId = baseChainContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalETC() throws Exception {
        int htgChainId = etcContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalCELO() throws Exception {
        int htgChainId = celoContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.0002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalLineaETH() throws Exception {
        int htgChainId = lineaContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.002").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(500_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalZKPolygon() throws Exception {
        int htgChainId = zkpolygonContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(15_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalEOS() throws Exception {
        int htgChainId = eosContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalZKETH() throws Exception {
        int htgChainId = zkContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.006").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1100_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalREI() throws Exception {
        int htgChainId = reiContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalETHW() throws Exception {
        int htgChainId = ethwContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalKAVA() throws Exception {
        int htgChainId = kavaContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalENULS() throws Exception {
        int htgChainId = enulsContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("5").movePointRight(8).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalOKT() throws Exception {
        int htgChainId = oktChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.08individualHT
        BigInteger value = new BigInteger(Long.valueOf(8_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalONE() throws Exception {
        int htgChainId = oneChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.01individualONE
        BigInteger value = new BigInteger(Long.valueOf(1_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalTRX() throws Exception {
        int feeChainId = chainId;
        int htgChainId = trxChainId;
        String from = address31;
        String to = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // TRXquantity
        BigInteger value = new BigDecimal("0.2").movePointRight(6).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(151_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, feeChainId, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalIOTX() throws Exception {
        int htgChainId = iotxContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // IOTX quantity
        BigInteger value = new BigDecimal("0.74").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalMETIS() throws Exception {
        int htgChainId = metisContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // IOTX quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalOETH() throws Exception {
        int htgChainId = optimismContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalKLAY() throws Exception {
        int htgChainId = klayContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(2_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBCH() throws Exception {
        int htgChainId = bchContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // Main assets quantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalFTM() throws Exception {
        int htgChainId = ftmContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // FTMquantity
        BigInteger value = new BigDecimal("1.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalARBI_ETH() throws Exception {
        int htgChainId = arbitrumContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // AETHquantity
        BigInteger value = new BigDecimal("0.001").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalAVAX() throws Exception {
        int htgChainId = avaxContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // AVAXquantity
        BigInteger value = new BigDecimal("1.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(25_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalCRO() throws Exception {
        int htgChainId = croContext.HTG_CHAIN_ID;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // CROquantity
        BigInteger value = new BigDecimal("1.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(35_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalKCS() throws Exception {
        int htgChainId = kcsChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.01individualKCS
        BigInteger value = new BigInteger(Long.valueOf(1_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalMatic() throws Exception {
        int htgChainId = maticChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 0.01individualMATIC
        BigInteger value = new BigInteger(Long.valueOf(1_0000_0000_0000_0000L).toString());
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalHT() throws Exception {
        int htgChainId = htChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // HTquantity
        BigInteger value = new BigDecimal("0.11").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBNB() throws Exception {
        int feeChainId = chainId;
        int htgChainId = bnbChainId;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // BNBquantity
        BigInteger value = new BigDecimal("0.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(50_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, feeChainId, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalETH() throws Exception {
        int htgChainId = goerliContext.HTG_CHAIN_ID();
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // ETHquantity
        BigInteger value = new BigDecimal("0.02").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = this.findAssetIdByHeterogeneousId(htgChainId, heterogeneousAssetId);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalDX() throws Exception {
        int htgChainId = trxChainId;
        String contract = DX_TRX_6;
        String from = address31;
        String to = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // DXNumber of items
        String value = "1.1";
        BigInteger valueBig = new BigDecimal(value).movePointRight(6).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(151_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, valueBig, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDTOnTrx() throws Exception {
        int htgChainId = trxChainId;
        String contract = USDT_TRX_6;
        String from = address31;
        String to = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // USDTNumber of items
        String value = "10.5";
        BigInteger valueBig = new BigDecimal(value).movePointRight(6).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(141_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, valueBig, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalNVTOnTrx() throws Exception {
        int htgChainId = trxChainId;
        String contract = NVT_TRX_MINTER;
        String from = address31;
        String to = "TTaJsdnYPsBjLLM1u2qMw1e9fLLoVKnNUX";
        // NVTNumber of items
        String value = "10500";
        BigInteger valueBig = new BigDecimal(value).movePointRight(8).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(141_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, valueBig, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDX() throws Exception {
        int feeChainId = chainId;
        int htgChainId = bnbContext.HTG_CHAIN_ID();
        String contract = USDX_BNB;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // USDXquantity
        BigInteger value = new BigDecimal("0.3").movePointRight(6).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(55_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, feeChainId, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalBUG() throws Exception {
        int feeChainId = chainId;
        int htgChainId = bnbChainId;
        String contract = BUG_BNB_18;
        //int htgChainId = ethChainId;
        //String contract = USDX;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // tokenquantity
        BigInteger value = new BigDecimal("1.1").movePointRight(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(50_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, feeChainId, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSDT() throws Exception {
        int htgChainId = mintContext.HTG_CHAIN_ID();
        String contract = USDT_MINT;
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        //String to = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // USDTquantity 1.123456
        BigInteger value = new BigDecimal("0.823456").scaleByPowerOfTen(6).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalUSD18() throws Exception {
        int htgChainId = mintContext.HTG_CHAIN_ID();
        String contract = USD18_MINT;
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        //String to = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // USD18quantity
        BigInteger value = new BigDecimal("1230.123456789123456789").scaleByPowerOfTen(18).toBigInteger();
        //BigInteger value = new BigDecimal("13").scaleByPowerOfTen(18).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalGOAT() throws Exception {
        int htgChainId = htChainId;
        String contract = GOAT_HT;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 8individualGOAT
        BigInteger value = new BigDecimal("8").scaleByPowerOfTen(9).toBigInteger();
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
        // 0.2individualUSDI
        BigInteger value = new BigInteger(Long.valueOf(2_00000L).toString());
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
        // 90Ten thousandENVT
        BigInteger value = new BigInteger("900000000000000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalNVT() throws Exception {
        int feeChainId = chainId;
        int htgChainId = mintContext.HTG_CHAIN_ID();
        String contract = "0x67Ce1821eFa30478e459ABFC5966d4Bc82Dbc17f";
        String from = address31;
        String to = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
        //String to = "0xc11D9943805e56b630A401D4bd9A29550353EFa1";
        // NVT quantity
        BigInteger value = new BigDecimal("200").movePointRight(8).toBigInteger();
        BigInteger fee = new BigInteger(Long.valueOf(20_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, feeChainId, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalDXA() throws Exception {
        int htgChainId = ethChainId;
        String contract = NVT18;
        String from = address22;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 2000MillionsDXA
        BigInteger value = new BigInteger("2000000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(1_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    @Test
    public void withdrawalNULS() throws Exception {
        int htgChainId = ethChainId;
        String contract = NULS18;
        String from = address31;
        String to = "0xc11d9943805e56b630a401d4bd9a29550353efa1";
        // 19Ten thousandNULS
        BigInteger value = new BigInteger("19000000000000");
        BigInteger fee = new BigInteger(Long.valueOf(10_0000_0000L).toString());
        NerveAssetInfo assetInfo = findAssetIdByAddress(htgChainId, contract);
        this.withdrawalByParams(from, to, value, fee, htgChainId, assetInfo);
    }

    protected void withdrawalByParams(String from, String to, BigInteger value, BigInteger fee, int heterogeneousChainId, NerveAssetInfo assetInfo) throws Exception {
        this.withdrawalByParams(from, to, value, fee, chainId, heterogeneousChainId, assetInfo);
    }

    protected void withdrawalByParams(String from, String to, BigInteger value, BigInteger fee, int feeChainId, int heterogeneousChainId, NerveAssetInfo assetInfo) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("assetChainId", assetInfo.getAssetChainId());
        params.put("assetId", assetInfo.getAssetId());
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAddress", to);
        params.put("amount", value);
        params.put("distributionFee", fee);
        params.put("feeChainId", feeChainId);
        params.put("remark", "Withdrawal");
        params.put("address", from);
        password = "nuls123456";
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

        Integer feeChainId = chainId;
        // Additional handling fees
        String amount = "1245";
        BigDecimal am = new BigDecimal(amount).movePointRight(8);
        amount = am.toPlainString();
        //params.put("rebuild", true);
        //params.put("htgChainId", 201);

        params.put("txHash", "d4fb8db455aaf782fe2a9f09a6d926245ed920d7ed2f8e71988a9db07d9b5a49");
        params.put("amount", amount);
        params.put("feeChainId", feeChainId);
        params.put("remark", "Additional handling fees");
        params.put("address", address31);
        password = "nuls123456";
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal_additional_fee", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void contractAssetReg() throws Exception {
        // 0x1c78958403625aeA4b0D5a0B527A27969703a270 USDI in ropsten
        // 0xAb58ee8e62178693265a1418D109b70dB4595586 USDK in rinkeby

        //regERC20(kromaContext.HTG_CHAIN_ID(), "USDT", USDT_KROMA, 6);
        //regERC20(kromaContext.HTG_CHAIN_ID(), "USD18", USD18_KROMA, 18);
        //regERC20(zetaContext.HTG_CHAIN_ID(), "USDT", USDT_ZETA, 6);
        //regERC20(zetaContext.HTG_CHAIN_ID(), "USD18", USD18_ZETA, 18);
        //regERC20(x1Context.HTG_CHAIN_ID(), "USDT", USDT_X1, 6);
        //regERC20(x1Context.HTG_CHAIN_ID(), "USD18", USD18_X1, 18);
        //regERC20(pulseContext.HTG_CHAIN_ID(), "USDT", USDT_PULSE, 6);
        //regERC20(pulseContext.HTG_CHAIN_ID(), "USD18", USD18_PULSE, 18);
        regERC20(mintContext.HTG_CHAIN_ID(), "USDT", USDT_MINT, 6);
        regERC20(mintContext.HTG_CHAIN_ID(), "USD18", USD18_MINT, 18);
        //regERC20(mantaContext.HTG_CHAIN_ID(), "USDT", USDT_MANTA, 6);
        //regERC20(mantaContext.HTG_CHAIN_ID(), "USD18", USD18_MANTA, 18);
        //regERC20(bnbChainId, "USDX", USDX_BNB, 6);
        //regERC20(bnbChainId, "BUSD", BUSD_BNB_18, 18);
        //regERC20(trxChainId, "USDT", USDT_TRX_6, 6);
        //regERC20(oktChainId, "USDX", USDX_OKT, 6);
        //regERC20(oktChainId, "USDT", USDT_OKT, 6);
        //regERC20(htChainId, "USDX", USDX_HT, 6);
        //regERC20(htChainId, "GOAT", GOAT_HT, 9);
        //regERC20(maticChainId, "USDT", USDT_MATIC, 6);

        //regERC20(bnbChainId, "BUG", BUG_BNB_18, 18);
        //regERC20(ethChainId, "USDX", USDX, 6);

        //regERC20(ethChainId, "USDI", USDI, 6);
        //regERC20(bnbChainId, "DXA", DXA_BNB_8, 8);
        //regERC20(bnbChainId, "SAFEMOON", SAFEMOON_BNB_9, 9);
        //regERC20(bnbChainId, "GOAT", GOAT_BNB_9, 9);
        //regERC20(htChainId, "HUSD", HUSD_HT_18, 18);
        //regERC20(oktChainId, "OKUSD", OKUSD_OKT_8, 8);

        //regERC20(oneChainId, "USDT", USDT_ONE, 6);
        //regERC20(maticChainId, "USDT", USDT_MATIC, 6);
        //regERC20(kcsChainId, "USDT", USDT_KCS, 6);

        //regERC20(trxChainId, "DX", DX_TRX_6, 6);
        //regERC20(croContext.HTG_CHAIN_ID, "USDT", USDT_CRO, 6);
        //regERC20(croContext.HTG_CHAIN_ID, "USD18", USD18_CRO, 18);
        //regERC20(avaxContext.HTG_CHAIN_ID, "USDT", USDT_AVAX, 6);
        //regERC20(avaxContext.HTG_CHAIN_ID, "USD18", USD18_AVAX, 18);
        //regERC20(arbitrumContext.HTG_CHAIN_ID, "USDT", USDT_ARBI, 6);
        //regERC20(arbitrumContext.HTG_CHAIN_ID, "USD18", USD18_ARBI, 18);
        //regERC20(ftmContext.HTG_CHAIN_ID, "USDT", USDT_FTM, 6);
        //regERC20(ftmContext.HTG_CHAIN_ID, "USD18", USD18_FTM, 18);
        //regERC20(metisContext.HTG_CHAIN_ID, "USDT", USDT_METIS, 6);
        //regERC20(metisContext.HTG_CHAIN_ID, "USD18", USD18_METIS, 18);
        //regERC20(iotxContext.HTG_CHAIN_ID, "USDT", USDT_IOTX, 6);
        //regERC20(iotxContext.HTG_CHAIN_ID, "USD18", USD18_IOTX, 18);
        //regERC20(optimismContext.HTG_CHAIN_ID, "USDT", USDT_OPTIMISM, 6);
        //regERC20(optimismContext.HTG_CHAIN_ID, "USD18", USD18_OPTIMISM, 18);
        //regERC20(klayContext.HTG_CHAIN_ID, "USDT", USDT_KLAY, 6);
        //regERC20(klayContext.HTG_CHAIN_ID, "USD18", USD18_KLAY, 18);
        //regERC20(bchContext.HTG_CHAIN_ID, "USDT", USDT_BCH, 6);
        //regERC20(bchContext.HTG_CHAIN_ID, "USD18", USD18_BCH, 18);
        //regERC20(goerliContext.HTG_CHAIN_ID(), "USDT", USDT_GOERLI, 6);
        //regERC20(goerliContext.HTG_CHAIN_ID(), "USD18", USD18_GOERLI, 18);
        //regERC20(enulsContext.HTG_CHAIN_ID(), "USDT", USDT_ENULS, 6);
        //regERC20(enulsContext.HTG_CHAIN_ID(), "USD18", USD18_ENULS, 18);
        //regERC20(kavaContext.HTG_CHAIN_ID(), "USDT", USDT_KAVA, 6);
        //regERC20(kavaContext.HTG_CHAIN_ID(), "USD18", USD18_KAVA, 18);
        //regERC20(ethwContext.HTG_CHAIN_ID(), "USDT", USDT_ETHW, 6);
        //regERC20(ethwContext.HTG_CHAIN_ID(), "USD18", USD18_ETHW, 18);
        //regERC20(reiContext.HTG_CHAIN_ID(), "USDT", USDT_REI, 6);
        //regERC20(reiContext.HTG_CHAIN_ID(), "USD18", USD18_REI, 18);
        //regERC20(zkContext.HTG_CHAIN_ID(), "USDT", USDT_ZK, 6);
        //regERC20(zkContext.HTG_CHAIN_ID(), "USD18", USD18_ZK, 18);
        //regERC20(eosContext.HTG_CHAIN_ID(), "USDT", USDT_EOS, 6);
        //regERC20(eosContext.HTG_CHAIN_ID(), "USD18", USD18_EOS, 18);
        //regERC20(zkpolygonContext.HTG_CHAIN_ID(), "USDT", USDT_ZKP, 6);
        //regERC20(zkpolygonContext.HTG_CHAIN_ID(), "USD18", USD18_ZKP, 18);
        //regERC20(lineaContext.HTG_CHAIN_ID(), "USDT", USDT_LINEA, 6);
        //regERC20(lineaContext.HTG_CHAIN_ID(), "USD18", USD18_LINEA, 18);
        //regERC20(celoContext.HTG_CHAIN_ID(), "USDT", USDT_CELO, 6);
        //regERC20(celoContext.HTG_CHAIN_ID(), "USD18", USD18_CELO, 18);
        //regERC20(etcContext.HTG_CHAIN_ID(), "USDT", USDT_ETC, 6);
        //regERC20(etcContext.HTG_CHAIN_ID(), "USD18", USD18_ETC, 18);
        //regERC20(baseChainContext.HTG_CHAIN_ID(), "USDT", USDT_BASECHAIN, 6);
        //regERC20(baseChainContext.HTG_CHAIN_ID(), "USD18", USD18_BASECHAIN, 18);
        //regERC20(scrollContext.HTG_CHAIN_ID(), "USDT", USDT_SCROLL, 6);
        //regERC20(scrollContext.HTG_CHAIN_ID(), "USD18", USD18_SCROLL, 18);
        //regERC20(briseContext.HTG_CHAIN_ID(), "USDT", USDT_BRISE, 6);
        //regERC20(briseContext.HTG_CHAIN_ID(), "USD18", USD18_BRISE, 18);
        //regERC20(janusContext.HTG_CHAIN_ID(), "USDT", USDT_JANUS, 6);
        //regERC20(janusContext.HTG_CHAIN_ID(), "USD18", USD18_JANUS, 18);

        //regERC20(ethChainId, "ENVT", ENVT, 18);
        //regERC20(ethChainId, "DAI", DAI, 18);
        //regERC20("FAU", FAU, 18);
        //regERC20("MT", MT, 18); // mainnet
        //regERC20(modeContext.HTG_CHAIN_ID(), "USDT", USDT_MODE, 6);
        //regERC20(modeContext.HTG_CHAIN_ID(), "USD18", USD18_MODE, 18);
        //regERC20(blastContext.HTG_CHAIN_ID(), "USDT", USDT_BLAST, 6);
        //regERC20(blastContext.HTG_CHAIN_ID(), "USD18", USD18_BLAST, 18);
        //regERC20(merlinContext.HTG_CHAIN_ID(), "USDT", USDT_MERLIN, 6);
        //regERC20(merlinContext.HTG_CHAIN_ID(), "USD18", USD18_MERLIN, 18);
    }

    @Test
    public void unRegisterContractAssetReg() throws Exception {
        unRegister(bnbChainId, 5, 1);
    }

    // Recharge pause
    @Test
    public void pauseInContractAssetReg() throws Exception {
        pauseIn(oktChainId, 5, 5);
    }

    // Recharge unlocking
    @Test
    public void resumeInContractAssetReg() throws Exception {
        resumeIn(oktChainId, 5, 5);
    }

    // Withdrawal pause
    @Test
    public void pauseOutContractAssetReg() throws Exception {
        pauseOut(htChainId, 5, 7);
    }

    // Withdrawal unlocking
    @Test
    public void resumeOutContractAssetReg() throws Exception {
        resumeOut(htChainId, 5, 7);
    }

    @Test
    public void mainAssetBind() throws Exception {
        //bindHeterogeneousMainAsset(goerliContext.HTG_CHAIN_ID(), 5, 2);
        bindHeterogeneousMainAsset(enulsContext.HTG_CHAIN_ID(), 5, 3);
    }

    @Test
    public void mainAssetReg() throws Exception {
        //password = "25eb1cb9-d19c-43d4-8899-32bef8b9b006";
        //regHeterogeneousMainAsset(kromaContext.HTG_CHAIN_ID());
        //regHeterogeneousMainAsset(zetaContext.HTG_CHAIN_ID());
        //regHeterogeneousMainAsset(x1Context.HTG_CHAIN_ID());
        regHeterogeneousMainAsset(pulseContext.HTG_CHAIN_ID());
        regHeterogeneousMainAsset(mintContext.HTG_CHAIN_ID());
        // BNB
        regHeterogeneousMainAsset(bnbContext.HTG_CHAIN_ID);
        // HT
        //regHeterogeneousMainAsset(htContext.HTG_CHAIN_ID);
        // OKT
        //regHeterogeneousMainAsset(oktContext.HTG_CHAIN_ID);
        //// ONE
        //regHeterogeneousMainAsset(oneContext.HTG_CHAIN_ID);
        //// MATIC
        //regHeterogeneousMainAsset(maticContext.HTG_CHAIN_ID);
        //// KCS
        //regHeterogeneousMainAsset(kcsContext.HTG_CHAIN_ID);
        //// TRX
        //regHeterogeneousMainAsset(trxContext.HTG_CHAIN_ID);
        //// CRO
        //regHeterogeneousMainAsset(croContext.HTG_CHAIN_ID);
        //// AVAX
        //regHeterogeneousMainAsset(avaxContext.HTG_CHAIN_ID);
        //// ARB_ETH
        //regHeterogeneousMainAsset(arbitrumContext.HTG_CHAIN_ID);
        //// FTM
        //regHeterogeneousMainAsset(ftmContext.HTG_CHAIN_ID);
        // METIS
        //regHeterogeneousMainAsset(metisContext.HTG_CHAIN_ID);
        // IOTX
        //regHeterogeneousMainAsset(iotxContext.HTG_CHAIN_ID);
        // OPTIMISM
        //regHeterogeneousMainAsset(optimismContext.HTG_CHAIN_ID);
        // KLAY
        //regHeterogeneousMainAsset(klayContext.HTG_CHAIN_ID);
        // BCH
        //regHeterogeneousMainAsset(bchContext.HTG_CHAIN_ID);
        // KAVA
        //regHeterogeneousMainAsset(kavaContext.HTG_CHAIN_ID());
        // ETHW
        //regHeterogeneousMainAsset(ethwContext.HTG_CHAIN_ID());
        // REI
        //regHeterogeneousMainAsset(reiContext.HTG_CHAIN_ID());
        // ZK
        //regHeterogeneousMainAsset(zkContext.HTG_CHAIN_ID());
        // EOS
        //regHeterogeneousMainAsset(eosContext.HTG_CHAIN_ID());
        // ZKPOLYGON
        //regHeterogeneousMainAsset(zkpolygonContext.HTG_CHAIN_ID());
        // LINEA
        //regHeterogeneousMainAsset(lineaContext.HTG_CHAIN_ID());
        // CELO
        //regHeterogeneousMainAsset(celoContext.HTG_CHAIN_ID());
        // ETC
        //regHeterogeneousMainAsset(etcContext.HTG_CHAIN_ID());
        // BASE
        //regHeterogeneousMainAsset(baseChainContext.HTG_CHAIN_ID());
        // SCROLL
        //regHeterogeneousMainAsset(scrollContext.HTG_CHAIN_ID());
        // BRISE
        //regHeterogeneousMainAsset(briseContext.HTG_CHAIN_ID());
        // JANUS
        //regHeterogeneousMainAsset(janusContext.HTG_CHAIN_ID());
        // MANTA
        //regHeterogeneousMainAsset(mantaContext.HTG_CHAIN_ID());
        // FCH
        regHeterogeneousMainAsset(fchContext.HTG_CHAIN_ID());
        // BTC
        //regHeterogeneousMainAsset(modeContext.HTG_CHAIN_ID());
        //regHeterogeneousMainAsset(blastContext.HTG_CHAIN_ID());
        regHeterogeneousMainAsset(merlinContext.HTG_CHAIN_ID());
        regHeterogeneousMainAsset(btcContext.HTG_CHAIN_ID());
    }

    @Test
    public void bindContractAssetReg() throws Exception {

        //bindERC20(kromaContext.HTG_CHAIN_ID(), "NVT", NVT_KROMA_MINTER, 8, 5, 1);
        //bindERC20(zetaContext.HTG_CHAIN_ID(), "NVT", NVT_ZETA_MINTER, 8, 5, 1);
        //bindERC20(x1Context.HTG_CHAIN_ID(), "NVT", NVT_X1_MINTER, 8, 5, 1);
        //bindERC20(pulseContext.HTG_CHAIN_ID(), "NVT", NVT_PULSE_MINTER, 8, 5, 1);
        bindERC20(mintContext.HTG_CHAIN_ID(), "NVTTest", "0x67Ce1821eFa30478e459ABFC5966d4Bc82Dbc17f", 8, 5, 1);
        //bindERC20(mintContext.HTG_CHAIN_ID(), "NVT", NVT_MINT_MINTER, 8, 5, 1);
        //bindERC20(mantaContext.HTG_CHAIN_ID(), "NVT", NVT_MANTA_MINTER, 8, 5, 1);
        //bindERC20(bnbChainId, "NVT", NVT_BNB_MINTER, 8, 5, 1);
        //bindERC20(oktChainId, "NVT", NVT_OKT_MINTER, 8, 5, 1);
        //bindERC20(oktChainId, "USDX", USDX_OKT_MINTER, 6, 5, 7);
        //bindERC20(oktChainId, "BNB", BNB_OKT_MINTER, 18, 5, 3);
        //bindERC20(trxChainId, "NVT", NVT_TRX_MINTER, 8, 5, 1);
        //bindERC20(bnbChainId, "TRX", TRX_BNB_MINTER, 6, 5, 7);
        //bindERC20(bnbChainId, "USDT", USDT_BNB_MINTER, 6, 5, 8);
        //bindERC20(ethChainId, "NVT", NVT_ETH_MINTER, 8, 5, 1);
        //bindERC20(bnbChainId, "NVT", NVT_BNB_MINTER, 8, 5, 1);
        //bindERC20(htChainId, "NVT", NVT_HT_MINTER, 8, 5, 1);
        //bindERC20(oktChainId, "NVT", NVT_OKT_MINTER, 8, 5, 1);
        //bindERC20(oneChainId, "NVT", NVT_ONE_MINTER, 8, 5, 1);
        //bindERC20(maticChainId, "NVT", NVT_MATIC_MINTER, 8, 5, 1);
        //bindERC20(kcsChainId, "NVT", NVT_KCS_MINTER, 8, 5, 1);
        //bindERC20(trxChainId, "NVT", NVT_TRX_MINTER, 8, 5, 1);
        //bindERC20(croContext.HTG_CHAIN_ID, "NVT", NVT_CRO_MINTER, 8, 5, 1);
        //bindERC20(avaxContext.HTG_CHAIN_ID, "NVT", NVT_AVAX_MINTER, 8, 5, 1);
        //bindERC20(arbitrumContext.HTG_CHAIN_ID, "NVT", NVT_ARBI_MINTER, 8, 5, 1);
        //bindERC20(ftmContext.HTG_CHAIN_ID, "NVT", NVT_FTM_MINTER, 8, 5, 1);
        //bindERC20(metisContext.HTG_CHAIN_ID, "NVT", NVT_METIS_MINTER, 8, 5, 1);
        //bindERC20(iotxContext.HTG_CHAIN_ID, "NVT", NVT_IOTX_MINTER, 8, 5, 1);
        //bindERC20(optimismContext.HTG_CHAIN_ID, "NVT", NVT_OPTIMISM_MINTER, 8, 5, 1);
        //bindERC20(klayContext.HTG_CHAIN_ID, "NVT", NVT_KLAY_MINTER, 8, 5, 1);
        //bindERC20(bchContext.HTG_CHAIN_ID, "NVT", NVT_BCH_MINTER, 8, 5, 1);
        //bindERC20(goerliContext.HTG_CHAIN_ID(), "NVT", NVT_GOERLI_MINTER, 8, 5, 1);
        //bindERC20(enulsContext.HTG_CHAIN_ID(), "NVT", NVT_ENULS_MINTER, 8, 5, 1);
        //bindERC20(kavaContext.HTG_CHAIN_ID(), "NVT", NVT_KAVA_MINTER, 8, 5, 1);
        //bindERC20(ethwContext.HTG_CHAIN_ID(), "NVT", NVT_ETHW_MINTER, 8, 5, 1);
        //bindERC20(reiContext.HTG_CHAIN_ID(), "NVT", NVT_REI_MINTER, 8, 5, 1);
        //bindERC20(zkContext.HTG_CHAIN_ID(), "NVT", NVT_ZK_MINTER, 8, 5, 1);
        //bindERC20(eosContext.HTG_CHAIN_ID(), "NVT", NVT_EOS_MINTER, 8, 5, 1);
        //bindERC20(zkpolygonContext.HTG_CHAIN_ID(), "NVT", NVT_ZKP_MINTER, 8, 5, 1);
        //bindERC20(lineaContext.HTG_CHAIN_ID(), "NVT", NVT_LINEA_MINTER, 8, 5, 1);
        //bindERC20(celoContext.HTG_CHAIN_ID(), "NVT", NVT_CELO_MINTER, 8, 5, 1);
        //bindERC20(etcContext.HTG_CHAIN_ID(), "NVT", NVT_ETC_MINTER, 8, 5, 1);
        //bindERC20(baseChainContext.HTG_CHAIN_ID(), "NVT", NVT_BASECHAIN_MINTER, 8, 5, 1);
        //bindERC20(scrollContext.HTG_CHAIN_ID(), "NVT", NVT_SCROLL_MINTER, 8, 5, 1);
        //bindERC20(briseContext.HTG_CHAIN_ID(), "NVT", NVT_BRISE_MINTER, 8, 5, 1);
        //bindERC20(janusContext.HTG_CHAIN_ID(), "NVT", NVT_JANUS_MINTER, 8, 5, 1);

        //bindERC20(bnbChainId, "ETH", ETH_BNB_MINTER, 18, 5, 2);
        //bindERC20(htChainId, "ETH", ETH_HT_MINTER, 18, 5, 2);
        //bindERC20(oktChainId, "ETH", ETH_OKT_MINTER, 18, 5, 2);
        //bindERC20(modeContext.HTG_CHAIN_ID(), "NVT", NVT_MODE_MINTER, 8, 5, 1);
        //bindERC20(blastContext.HTG_CHAIN_ID(), "NVT", NVT_BLAST_MINTER, 8, 5, 1);
        //bindERC20(merlinContext.HTG_CHAIN_ID(), "NVT", NVT_MERLIN_MINTER, 8, 5, 1);
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
        params.put("remark", "ropstenContract asset registration");
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
        params.put("remark", "Heterogeneous Chain Master Asset Registration");
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void bindHeterogeneousMainAsset(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("remark", "Heterogeneous Chain Master Asset Binding");
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_BIND_TX, params);
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

    private void unRegister(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.UNREGISTER_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void pauseIn(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.PAUSE_IN_HETEROGENEOUS_CONTRACT_TOKEN_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void pauseOut(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.PAUSE_OUT_HETEROGENEOUS_CONTRACT_TOKEN_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void resumeIn(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.RESUME_IN_HETEROGENEOUS_CONTRACT_TOKEN_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    private void resumeOut(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("nerveAssetChainId", nerveAssetChainId);
        params.put("nerveAssetId", nerveAssetId);
        params.put("address", packageAddress);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.RESUME_OUT_HETEROGENEOUS_CONTRACT_TOKEN_TX, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void combiningDataInit() throws Exception {
        chainAssetTxRegister("USDX6", 6);
        chainAssetTxRegister("BUSD18", 18);
        chainAssetTxRegister("HUSD18", 18);
        chainAssetTxRegister("OKUSD8", 8);
        chainAssetTxRegister("KXT", 8);
        chainAssetTxRegister("KBT", 8);
        chainAssetTxRegister("KHT", 8);
        chainAssetTxRegister("KOT", 8);
    }

    @Test
    public void chainAssetTxRegisterTest() throws Exception {
        //chainAssetTxRegister("U1D", 18);
        //chainAssetTxRegister("U2D", 18);
        //chainAssetTxRegister("U3D", 18);
        //chainAssetTxRegister("U4D", 18);
        chainAssetTxRegister("U5D", 6);
        chainAssetTxRegister("U6D", 8);
        //chainAssetTxRegister("NULS", 8);
        //chainAssetTxRegister("USDTN", 18);
    }

    private void chainAssetTxRegister(String asset, int decimals) throws Exception {
        // Build params map
        Map<String, Object> params = new HashMap<>();
        params.put("assetSymbol", asset);
        params.put("assetName", asset);
        params.put("initNumber", 100000000);
        params.put("decimalPlace", decimals);
        params.put("txCreatorAddress", address31);
        params.put("assetOwnerAddress", address31);
        params.put("password", "nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        if (!response.isSuccess()) {
            Log.error("asset[{}-{}]Creation failed, error: {}", asset, decimals, JSONUtils.obj2PrettyJson(response));
        } else {
            Map chainAssetTxReg = (Map)((Map) response.getResponseData()).get("chainAssetTxReg");
            String txHash = (String) chainAssetTxReg.get("txHash");
            Log.info("asset[{}-{}]Created successfully, txHash: {}", asset, decimals, txHash);
            TimeUnit.MILLISECONDS.sleep(6000);
        }
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
        params.put("heterogeneousChainId", 119);
        params.put("heterogeneousAssetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void getVirtualBankInfo() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("balance", true);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_virtualBankInfo", params, Constants.TIMEOUT_TIMEMILLIS * 10L);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void findAssetInfoListByAssetId() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("assetId", 1);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_LIST, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void getRegisterNetwork() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("assetId", 2);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_HETEROGENEOUS_REGISTER_NETWORK, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void getProposalInfo() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("proposalTxHash", "3355e95d8f16f45347f97917134089d52a1d390ab9fe29d52c92ed47b822ec58");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.GET_PROPOSAL_INFO, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
        Map responseData = (Map) cmdResp.getResponseData();
        String result = (String) responseData.get(ConverterCmdConstant.GET_PROPOSAL_INFO);
        ProposalPO po = new ProposalPO();
        po.parse(HexUtil.decode(result), 0);
        System.out.println(JSONUtils.obj2PrettyJson(po));
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
    public void checkRetryParse() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("heterogeneousChainId", 201);
        params.put("heterogeneousTxHash", "cc0866e50e135f1daccdfe50d6df432c860ad08c5088c21eb4536f0b821969fc");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CHECK_RETRY_PARSE, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void checkRetryHtgTx() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 5);
        params.put("heterogeneousChainId", 201);
        params.put("heterogeneousTxHash", "ef942ebc15f875b280d24282be2b6c7a5ec84eec6e2eb0551a787be5e808fa27");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.CHECK_RETRY_HTG_TX, params);
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
        //outputs.add(new CoinDTO("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad", chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));
        //outputs.add(new CoinDTO("TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA", chainId, 1, BigInteger.valueOf(10000_0000_0000L), password, 0));

        BigInteger inAmount = BigInteger.valueOf(10_0000L);
        for (CoinDTO dto : outputs) {
            inAmount = inAmount.add(dto.getAmount());
        }
        inputs.add(new CoinDTO(address21, chainId, 1, inAmount, password, 0));

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);

        //Calling interfaces
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void transferOne() throws Exception {
        String from = address31;
        String to = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
        String value = "100";
        int assetChainId = 5;
        int assetId = 8;
        int decimals = 18;

        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "abc");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();
        outputs.add(new CoinDTO(to, assetChainId, assetId, new BigDecimal(value).movePointRight(decimals).toBigInteger(), password, 0));
        BigInteger inAmount = BigInteger.ZERO;
        if (assetChainId == chainId && assetId == 1) {
            inAmount = BigInteger.valueOf(10_0000L);
        } else {
            inputs.add(new CoinDTO(from, chainId, 1, BigInteger.valueOf(10_0000L), password, 0));
        }
        for (CoinDTO dto : outputs) {
            inAmount = inAmount.add(dto.getAmount());
        }
        inputs.add(new CoinDTO(from, assetChainId, assetId, inAmount, password, 0));
        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);
        //Calling interfaces
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
        package6();
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", agentAddress);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("deposit", "21000000000000"); // 21W
        params.put("packingAddress", packageAddress);
        password = "nuls123456";
        params.put("password", password);
        params.put("rewardAddress", agentAddress);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
        System.out.println(cmdResp.getResponseData());
    }


    @Test
    public void stopAgent() throws Exception {
        package6();
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", agentAddress);
        password = "nuls123456";
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", params);
        System.out.println(cmdResp.getResponseData());
    }

    /**
     * Additional margin
     */
    @Test
    public void appendAgentDeposit() throws Exception {
        String hash = appendAgent(agentAddress, "10000000000000");// 10W
        System.out.println(hash);
    }

    /**
     * Reduce margin
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
            // Test administrator change
            if (oddEven % 2 == 0) {
                try {
                    behavior = "Reduce margin";
                    hash = reduceAgent(address, "2000000000000");// 2w
                } catch (Exception e) {
                    System.err.println(String.format("%sfail, address-%s[%s], msg: %s", behavior, nextInt, address, e.getMessage()));
                    behavior = "Additional margin";
                    hash = appendAgent(address, "2000000000000");// 2w
                }
            } else {
                try {
                    behavior = "Additional margin";
                    hash = appendAgent(address, "2000000000000");// 2w
                } catch (Exception e) {
                    System.err.println(String.format("%sfail, address-%s[%s], msg: %s", behavior, nextInt, address, e.getMessage()));
                    behavior = "Reduce margin";
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
        System.out.println(String.format("Before starting, the total account balance: %s, Balance of multiple signed contracts: %s",
                    htgWalletApi.getERC20Balance("0xf173805F1e3fE6239223B17F0807596Edc283012", USDI),
                    htgWalletApi.getERC20Balance("0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", USDI)
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
            // Test recharge withdrawal
            if (oddEven % 2 == 0) {
                behavior = "ethNetwork recharge";
                hash = dealDepositUSDI(nextInt, address);
            } else {
                behavior = "nerveOnline withdrawal";
                hash = dealWithdrawalUSDI(nextInt, address);
            }
            System.out.println(String.format("address-%s[%s] %s, hash: %s", nextInt, address, behavior, hash));
            if (hash == null) {
                continue;
            }
            TimeUnit.SECONDS.sleep(2);
        }
        System.out.println(String.format("After completion, the total account balance: %s, Balance of multiple signed contracts: %s, Total account expenses: %s, Total recharge: %s, Total withdrawal: %s",
                htgWalletApi.getERC20Balance("0xf173805F1e3fE6239223B17F0807596Edc283012", USDI),
                htgWalletApi.getERC20Balance("0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", USDI),
                bigTotal,
                depositTotal,
                withdrawTotal
        ));
    }

    protected ETHWalletApi htgWalletApi;

    @Before
    public void setUp() throws Exception {
        String ethRpcAddress = "https://ropsten.infura.io/v3/e51e9f10a4f647af81d5f083873f27a5";
        htgWalletApi = new ETHWalletApi();
        EthContext.setLogger(Log.BASIC_LOGGER);
        Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
        htgWalletApi.setWeb3j(web3j);
        htgWalletApi.setEthRpcAddress(ethRpcAddress);
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
        BigInteger USDIBalance = htgWalletApi.getERC20Balance(ethAccount, USDI);
        if (USDIBalance.compareTo(BigInteger.valueOf(1_000000L)) < 0) {
            EthSendTransaction tx = htgWalletApi.transferERC20Token(account1, ethAccount, BigInteger.valueOf(10_000000L), account1PrivateKey, USDI);
            if (StringUtils.isNotBlank(tx.getTransactionHash())) {
                bigTotal += 10;
                System.out.println(String.format("Total account expenses10individualUSDI[%s], ethTxHash: %s, Accumulated expenses: %s", ethAccount, tx.getTransactionHash(), bigTotal));
            }
            return null;
        }
        EthSendTransaction info = htgWalletApi.transferERC20Token(ethAccount, "0xce0fb0b8075f8f54517d939cb4887ba42d97a23a", BigInteger.valueOf(1_000000L), ethAccountPrivateKey, USDI);
        if (StringUtils.isNotBlank(info.getTransactionHash())) {
            depositTotal += 1;
            System.out.println(String.format("    [%s]Recharge1individualUSDI, ethTxHash: %s, Accumulated recharge: %s", ethAccount, info.getTransactionHash(), depositTotal));
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
        // 1individualtoken
        params.put("amount", Long.valueOf(1_000000L));
        params.put("remark", "Withdrawal");
        params.put("address", "0xf173805F1e3fE6239223B17F0807596Edc283012");
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
        if (cmdResp.getResponseData() == null || ((Map) cmdResp.getResponseData()).get("cv_withdrawal") == null) {
            throw new Exception(String.format("error code: %s, msg: %s", cmdResp.getResponseErrorCode(), cmdResp.getResponseComment()));
        }
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
        String hash = (String) result.get("value");
        withdrawTotal += 1;
        System.out.println(String.format("    [%s]Withdrawal1individualUSDI, nerveTxHash: %s, Accumulated withdrawal: %s", ethAccount, hash, withdrawTotal));
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
        if (!cmdResp.isSuccess()) {
            System.err.println(JSONUtils.obj2PrettyJson(cmdResp));
        } else {
            Map map = (Map) (((Map) cmdResp.getResponseData()).get("lg_get_all_asset"));
            List<Map> assets = (List<Map>) map.get("assets");
            for (Map asset : assets) {
                System.out.println(
                        String.format("%s[%s-%s] decimals: %s, initNumber: %s, type: %s",
                                asset.get("assetSymbol"),
                                asset.get("assetChainId"),
                                asset.get("assetId"),
                                asset.get("decimalPlace"),
                                asset.get("initNumber"),
                                asset.get("assetType")));
            }
        }
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

    /** Freeze account */
    @Test
    public void proposalLOCK() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.LOCK.value());
        params.put("content", "This is the content of the proposal……Freeze account");
        params.put("businessAddress", address32);
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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

    /** Unfreezing accounts */
    @Test
    public void proposalUNLOCK() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.UNLOCK.value());
        params.put("content", "This is the content of the proposal……Unfreezing accounts");
        params.put("businessAddress", address32);
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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

    /** Refund funds */
    @Test
    public void proposalREFUND() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.REFUND.value());
        params.put("content", "This is the content of the proposal……Refund funds");
        params.put("heterogeneousChainId", bnbContext.HTG_CHAIN_ID());
        params.put("heterogeneousTxHash", "0x7a7795899765b2cf7be0e9a936dad2c240362c8979bf26de264d041998fd0031");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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

    /** Revocation of bank qualification */
    @Test
    public void proposalEXPELLED() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.EXPELLED.value());
        params.put("content", "This is the content of the proposal……Revocation of bank qualification");
        params.put("businessAddress", address26);
        params.put("hash", "");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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
    public void proposalUpgrade() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.UPGRADE.value());
        params.put("content", "2-3");
        params.put("heterogeneousChainId", htContext.HTG_CHAIN_ID);
        params.put("businessAddress", "0xF7c9BB7e6D6B8F603F17f6af0713D99EE285BDDb");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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
    public void proposalTransfer2OtherNerveAddress() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.TRANSFER.value());
        params.put("content", "Transfer to another address");
        params.put("heterogeneousChainId", btcContext.HTG_CHAIN_ID());
        params.put("heterogeneousTxHash", "cc0866e50e135f1daccdfe50d6df432c860ad08c5088c21eb4536f0b821969fc");
        params.put("businessAddress", "TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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
    public void proposalCloseHtgChain() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.CLOSE_HTG_CHAIN.value());
        params.put("content", "close htg chain");
        params.put("heterogeneousChainId", 140);
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("proposalTxHash", "88ee5e5272772203cb3a6223b3a2e4f14ea48b722e821efa798732a3276b9a32");
        params.put("choice", ProposalVoteChoiceEnum.FAVOR.value());
        params.put("remark", "voteremark");
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

    @Test
    public void proposal() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.REFUND.value());
        params.put("content", "This is the content of the proposal……");
        params.put("heterogeneousChainId", trxChainId);
        params.put("heterogeneousTxHash", "0x3db1462ef1988747db5d3bfddf6a8615e026af4b6749eae15eb7bdd9a9ea5026");
        params.put("businessAddress", "");
        params.put("hash", "");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
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
    /**
     * Delete account
     */
    @Test
    public void removeAccountTest() throws Exception {
        removeAccount("TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg", password);
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
