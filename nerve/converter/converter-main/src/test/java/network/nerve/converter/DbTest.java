/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.NulsSignData;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.HtgERC20StorageServiceImpl;
import network.nerve.converter.heterogeneouschain.okt.constant.OktDBConstant;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import network.nerve.converter.utils.ConverterDBUtil;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static network.nerve.converter.utils.ConverterDBUtil.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: PierreLuo
 * @date: 2021/7/21
 */
public class DbTest {

    @BeforeClass
    public static void before() {
        Log.info("init");
        RocksDBService.init("/Users/pierreluo/IdeaProjects/nerve-network/data/converter");
    }

    @Test
    public void getUTXO() {
        WithdrawalUTXOTxData txData = ConverterDBUtil.getModel(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, stringToBytes("201_WITHDRAWL_UTXO_PREFIX-885bb63426c75c0e8a25b3342d2d5b5c77b2bdc45f9c9d55d3cae05e3347f5e2"), WithdrawalUTXOTxData.class);
        System.out.println(txData);
    }

    @Test
    public void getLockedUTXO() {
        byte[] bytes = RocksDBManager.get(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, stringToBytes("201_WITHDRAWL_UTXO_LOCKED_PREFIX-8b6a4e8a771bd119270e31075f2c3c194aa7f456040f934980977142f97cd929-1"));
        System.out.println(HexUtil.encode(bytes));
    }

    @Test
    public void getHtgId() {
        String txHash = "0xae08823bbca0e4a66f3025c3084f8da332f08e94b357e8e3d7a2c4714b562160";
        HeterogeneousTransactionInfo info = ConverterDBUtil.getModel(EthDBConstant.DB_ETH, stringToBytes("BROADCAST-" + txHash), HeterogeneousTransactionInfo.class);
        System.out.println();
    }

    @Test
    public void dbData() throws Exception {
        List<Entry<byte[], byte[]>> entries = RocksDBManager.entryList(OktDBConstant.DB_OKT);
        if (entries == null) entries = Collections.EMPTY_LIST;
        System.out.println(entries.size());
        for (Entry<byte[], byte[]> entry : entries) {
            System.out.println("[old] key: " + ConverterDBUtil.bytesToString(entry.getKey()));
        }
        List<Entry<byte[], byte[]>> entriesNew = RocksDBManager.entryList(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN);
        if (entriesNew == null) entriesNew = Collections.EMPTY_LIST;
        System.out.println(entriesNew.size());
        for (Entry<byte[], byte[]> entryNew : entriesNew) {
            System.out.println("[new] key: " + ConverterDBUtil.bytesToString(entryNew.getKey()));
        }
    }

    @Test
    public void test() throws Exception {
        RocksDBService.init("/Users/pierreluo/Nuls/NERVE/cv");
        BnbContext context = new BnbContext();
        context.setLogger(Log.BASIC_LOGGER);
        HtgERC20StorageServiceImpl service = new HtgERC20StorageServiceImpl(context, BnbDBConstant.DB_BNB);
        HtgERC20Po po = service.findByAddress("0x72755f739b56ef98bda25e2622c63add229dec01");
        System.out.println(JSONUtils.obj2PrettyJson(po));
    }


    @Test
    public void testCVTableTx() throws Exception {
        //String hash = "f352fa38e8832f8647955514c349611eb2ba87e86a29a5c23b2439846e19117c";//9-625
        //String hash = "556698105bcc7e8e58887af084feda708b008185f9a8240982438d7db5a08db2";//9-627
        String hash = "a4b0a5c7d3f5a2edd4a0fabe652c1f4110167816fb7aea9d53da6cb75f9ece64";
        RocksDBService.init("/Users/pierreluo/Nuls/cvProposal");
        //byte[] bytes = RocksDBService.get("cv_proposal_exe_9", stringToBytes(hash));
        byte[] bytes = RocksDBService.get("cv_async_processed_9", stringToBytes("PROPOSAL_EXE_PREFIX_" + hash));
        System.out.println(bytesToString(bytes));
    }

    @Test
    public void testHeterogeneousConfirmedChangeVBTableTx() throws Exception {
        RocksDBService.init("/Users/pierreluo/Nuls/cv1012v2");
        String baseArea = ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO;
        String KEY_PREFIX = "CONFIRMED_CHANGE_VB-";
        String txHash = "a051a43ff30e9ce9693512688a9a9a95bd13c3e2244e6556ea128df3aad859e5";
        HeterogeneousConfirmedChangeVBPo model = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + txHash), HeterogeneousConfirmedChangeVBPo.class);
        System.out.println(model);
        byte[] ALL_KEY = stringToBytes("CONFIRMED_CHANGE_VB-ALL");
        byte[] bytes = RocksDBService.get(baseArea, ALL_KEY);
        StringSetPo model1 = ConverterDBUtil.getModel(bytes, StringSetPo.class);
        System.out.println(model1.getCollection());
    }

    @Test
    public void testVirtualBankTableTx() throws Exception {
        RocksDBService.init("/Users/pierreluo/Nuls/cv0623");
        List<Entry<byte[], byte[]>> listEntry = RocksDBService.entryList(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + 9);
        if (null == listEntry) {
            return;
        }
        Map<String, VirtualBankDirector> map = new HashMap<>();
        for (Entry<byte[], byte[]> entry : listEntry) {
            VirtualBankDirector vbd = ConverterDBUtil.getModel(entry.getValue(), VirtualBankDirector.class);
            map.put(vbd.getSignAddress(), vbd);
        }
        System.out.println(map.size());
    }

    @Test
    public void testSignTableTx() throws Exception {
        String hash = "4e7864181e774271a799c2ae99ee6bfd17547edeaa5b370daf7991d45cc8d41e";
        RocksDBService.init("/Users/pierreluo/Nuls/NERVE/data_converter_aa_0331");
        byte[] bytes = RocksDBService.get("cv_component_sign_9", ConverterDBUtil.stringToBytes(hash));
        ComponentSignByzantinePO po = ConverterDBUtil.getModel(bytes, ComponentSignByzantinePO.class);
        List<ComponentCallParm> callParms = po.getCallParms();
        ComponentCallParm c = null;
        ComponentCallParm trxParm = null;
        for (ComponentCallParm p : callParms) {
            if (p.getHeterogeneousId() == 108) {
                trxParm = p;
            } else {
                c = p;
            }
            System.out.println(String.format("list.add(new Object[]{%s, \"%s\"});", p.getHeterogeneousId(), p.getSigned()));
        }
        System.out.println(String.format("add: %s, remove: %s", Arrays.toString(c.getInAddress()), Arrays.toString(c.getOutAddress())));
        System.out.println(String.format("add: %s, remove: %s", Arrays.toString(trxParm.getInAddress()), Arrays.toString(trxParm.getOutAddress())));
        System.out.println(po.getHash().toHex());
    }

    @Test
    public void txTest() throws Exception {
        RocksDBService.init("/Users/pierreluo/Nuls/cv1012v2");
        String hash = "799bd5f4b45595923134b9eacddb930034f55ea110c0d2ea10929634c61a6fa3";
        byte[] txBytes = RocksDBService.get(ConverterDBConstant.DB_TX_PREFIX + 9, HexUtil.decode(hash));
        TransactionPO tx = null;
        if (null != txBytes) {
            try {
                tx = ConverterUtil.getInstance(txBytes, TransactionPO.class);
                byte[] signature = tx.getTx().getTransactionSignature();
                TransactionSignature ts = new TransactionSignature();
                ts.parse(signature, 0);
                List<P2PHKSignature> p2PHKSignatures = ts.getP2PHKSignatures();
                p2PHKSignatures.forEach(p -> System.out.println(String.format("pub: %s, addr: %s, signData: %s", HexUtil.encode(p.getPublicKey()), AddressTool.getAddressString(p.getPublicKey(), 9), HexUtil.encode(p.getSignData().getSignBytes()))));
                //pub: 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b, addr: NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA, signData: 30440220166759234d6613d72b51364a8da25ce45b0f760554de7bda9bfeb328d9d0d465022050e669a4dea156c7b89abee5d05867c1a72428d935b1d432c4a3a3ea12e876af
                //pub: 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03, addr: NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9, signData: 304402203170fb3d9f131004d5f84549658ab6d85e45493473a21de936870d4a1d2a2da502206da7f0628db01ec5c0995aef1d479d78f9ded16ac220fdee9699ef9669d668c2
                //pub: 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9, addr: NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE, signData: 3045022100a30e4f0b533ce593d6ded5092b7c213914424d80b686c21c1b358260a7a55bd3022029a1d2584bbab82c92fd156af228e3186343771ebffb4cdf70d4360d1c08114d

                System.out.println(tx.getTx().getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void signHashTest() {
        List<ECKey> list = new ArrayList<>();
        list.add(ECKey.fromPrivate(HexUtil.decode("aaaaaaaaaaa")));
        list.add(ECKey.fromPrivate(HexUtil.decode("aaaaaaaaaaa")));
        list.add(ECKey.fromPrivate(HexUtil.decode("aaaaaaaaaaa")));
        list.add(ECKey.fromPrivate(HexUtil.decode("aaaaaaaaaaa")));
        list.add(ECKey.fromPrivate(HexUtil.decode("aaaaaaaaaaa")));

        byte[] hash = HexUtil.decode("799bd5f4b45595923134b9eacddb930034f55ea110c0d2ea10929634c61a6fa3");
        for (ECKey key : list) {
            byte[] sign = key.sign(hash);
            System.out.println(String.format("pub: %s, addr: %s, signData: %s",
                    HexUtil.encode(key.getPubKey()),
                    AddressTool.getAddressString(key.getPubKey(), 9),
                    HexUtil.encode(sign)));
        }
    }

    @Test
    public void confirmedChangeTxSendTest() throws Exception {
        Transaction tx = new Transaction();
        tx.setTime(1728633179);
        tx.setType(40);
        ConfirmedChangeVirtualBankTxData txData = new ConfirmedChangeVirtualBankTxData();
        txData.setChangeVirtualBankTxHash(NulsHash.fromHex("a051a43ff30e9ce9693512688a9a9a95bd13c3e2244e6556ea128df3aad859e5"));
        List<byte[]> listAgents = new ArrayList<>();
        listAgents.add(AddressTool.getAddress("NERVEepb6kENgUgVNdaJj9d5e947pmrZ19pmwk"));
        listAgents.add(AddressTool.getAddress("NERVEepb6gVC8TBCioYHK6PPLKYk9aoXH8eGbV"));
        listAgents.add(AddressTool.getAddress("NERVEepb6exLKu4eJkHCekXicn7YpDa3hzHGvt"));
        listAgents.add(AddressTool.getAddress("NERVEepb6eKxoK6ZmKhhDK4oZkuXnUMxZfCta4"));
        listAgents.add(AddressTool.getAddress("NERVEepb6bt22V1LgaWavLNPEyTKbayCD58xo4"));
        listAgents.add(AddressTool.getAddress("NERVEepb6bT6dAfEtY9Z4c38Khawdr4LKZNais"));
        listAgents.add(AddressTool.getAddress("NERVEepb6FQX5fGD34zpbXWD2YRRRr5QXRmapK"));
        listAgents.add(AddressTool.getAddress("NERVEepb6Doc1HeS13ntknsSDsJusDktdTBagN"));
        listAgents.add(AddressTool.getAddress("NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC"));
        listAgents.add(AddressTool.getAddress("NERVEepb6BJjKQi5crP1s97HvMZNUJiopwzfUq"));
        listAgents.add(AddressTool.getAddress("NERVEepb6BFXG13yVCSYR3LDdP8fPathYoKzZw"));
        listAgents.add(AddressTool.getAddress("NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA"));
        listAgents.add(AddressTool.getAddress("NERVEepb692UnBKbUCp99XamFhVJ6vXFNYHUeC"));
        listAgents.add(AddressTool.getAddress("NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB"));
        listAgents.add(AddressTool.getAddress("NERVEepb62CKawndBxG7nqb4n34ckarb7LNQki"));
        txData.setListAgents(listAgents);

        List<HeterogeneousConfirmedVirtualBank> parsed = parseConfirmedData(confirmedData);
        txData.setListConfirmed(parsed);
        tx.setTxData(txData.serialize());

        TransactionSignature ts = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        ts.setP2PHKSignatures(p2PHKSignatures);

        List<String> signList = new ArrayList<>();
        //signList.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049,30440220628891d846de60c626a901727ffa8851517190cd8264728dbeb169e54b846acb02207574165fd4a68c7b1388ad9282f99d67144d0542cf436645487a486b4703c6a8");
        signList.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b,30440220166759234d6613d72b51364a8da25ce45b0f760554de7bda9bfeb328d9d0d465022050e669a4dea156c7b89abee5d05867c1a72428d935b1d432c4a3a3ea12e876af");
        signList.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03,304402203170fb3d9f131004d5f84549658ab6d85e45493473a21de936870d4a1d2a2da502206da7f0628db01ec5c0995aef1d479d78f9ded16ac220fdee9699ef9669d668c2");
        signList.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4,304402205f51cfc92e12f09b6a1c15dd2102d98e82444cee66d714383c417da30734ec8d02205fc266757bd2f837fc030932ec899b365f2524c9386c49450f317daf6201716c");
        signList.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4,3045022100eb83efd4fbc9490fb2e1364c61ed5261ea1562ecf633bf980036ff331f44b059022021a8417bbe6cef67918a9196dce270f61502e81810856d60bad98ce3e335c896");
        signList.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9,3045022100a30e4f0b533ce593d6ded5092b7c213914424d80b686c21c1b358260a7a55bd3022029a1d2584bbab82c92fd156af228e3186343771ebffb4cdf70d4360d1c08114d");
        signList.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d,3044022001b45194adf60f97d5744e459692a365142392a70cede1e9bc008f83ea4f36ca022052c34196094ad0cdda6f6943aaf4a01023b9cc60bfdf4b0206e6e5fddc1f6859");
        signList.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd,3044022079e5c11ad59511f3f7bc07d8911560e4a8096d85f0ad374d50a5deb440d09a5202204b4ae33f52ffb9cb5716e2db7118a558aadc0c86a3c84e23a0865d59c98dc124");
        signList.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292,304402201de03ca95147bfbac993aec87675c7d95345fa923d48c0d1208d3476e469cd4e02204561e30f86832791eb10bf985340d6dc2e291097a3d6d9c542866a581f2bd177");
        signList.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0,30450221008b4c9d8391c8b1d141c8f961ad8e0cd70a8d8b1c7267a41508bf2232d80adf7d022068683b67dcbed2cf2464767b2ee36461af36125f398f7f311dc4b35d2c202a1a");
        signList.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7,3045022100c276d3e2d79f8158a4a533545369ada9fbfba7a8d0e153e6ff66a84c0159ec9d02204ec6d9803693a19005d70a3766d354bf344bd89017dc95a11b0aaf91cfc85e95");

        for (String signData : signList) {
            String[] split = signData.split(",");
            String pub = split[0];
            String sign = split[1];
            NulsSignData nulsSignData = new NulsSignData();
            nulsSignData.setSignBytes(HexUtil.decode(sign));
            P2PHKSignature p2PHKSignature = new P2PHKSignature();
            p2PHKSignature.setPublicKey(HexUtil.decode(pub));
            p2PHKSignature.setSignData(nulsSignData);
            p2PHKSignatures.add(p2PHKSignature);
        }
        tx.setTransactionSignature(ts.serialize());
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, 9);
        boolean signture = SignatureUtil.validateTransactionSignture(9, tx);
        System.out.println(signture);
        addressSet.forEach(a -> System.out.println(a));
        System.out.println(tx.getHash().toHex());
        System.out.println(HexUtil.encode(tx.serialize()));

    }

    @Test
    public void parseConfirmedDataTest() {
        List<HeterogeneousConfirmedVirtualBank> parsed = parseConfirmedData("");
        System.out.println(parsed.size());
    }
    private List<HeterogeneousConfirmedVirtualBank> parseConfirmedData(String data) {
        String[] split = data.split("\n");
        System.out.println();
        split[6].replaceAll("\t", "");

        List<HeterogeneousConfirmedVirtualBank> listConfirmed = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                if (s.contains("banks-")) {
                    i++;
                    HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank();
                    listConfirmed.add(bank);
                    bank.setHeterogeneousChainId(Integer.parseInt(split[i++].split(":")[1].trim()));
                    if (bank.getHeterogeneousChainId() == 203) {
                        String[] split1 = split[i++].split(":");
                        bank.setHeterogeneousAddress(split1[1].trim() + ":" + split1[2].trim());
                    } else {
                        bank.setHeterogeneousAddress(split[i++].split(":")[1].trim());
                    }
                    bank.setHeterogeneousTxHash(split[i++].split(":")[1].trim());
                    bank.setEffectiveTime(Long.parseLong(split[i++].split(":")[1].trim()));
                    if (bank.getHeterogeneousChainId() > 200) {
                        continue;
                    }
                    i++;
                    List<HeterogeneousAddress> signAddressList = new ArrayList<>();
                    bank.setSignedHeterogeneousAddress(signAddressList);
                    HeterogeneousAddress address = new HeterogeneousAddress();
                    signAddressList.add(address);
                    address.setChainId(bank.getHeterogeneousChainId());
                    address.setAddress(split[i].replaceAll("\t", ""));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println();
        return listConfirmed;
    }

    @Test
    public void testBigDecimal() {
        BigDecimal big = new BigDecimal("13068492433458328880192656953000000000000000000").movePointLeft(18);
        BigDecimal token = new BigDecimal("13068492433458328880192656953").movePointLeft(18);
        System.out.println(big);
        System.out.println(token);
        System.out.println(big.subtract(token));
        try {
            new DbTest().testSignTableTx();
        } catch (Exception e) {
            e.printStackTrace();
            StackTraceElement[] stackTrace0 = e.getStackTrace();
            if (stackTrace0 != null) {
                for (StackTraceElement s : stackTrace0) {
                    System.out.println(String.format("---pierre test0----, %s", s));
                }
            }
            if (e.getCause() != null) {
                StackTraceElement[] stackTrace = e.getCause().getStackTrace();
                if (stackTrace != null) {
                    for (StackTraceElement s : stackTrace) {
                        System.out.println(String.format("---pierre test----, %s", s));
                    }
                }
                if (e.getCause().getCause() != null) {
                    StackTraceElement[] stackTrace1 = e.getCause().getCause().getStackTrace();
                    if (stackTrace1 != null) {
                        for (StackTraceElement s : stackTrace1) {
                            System.out.println(String.format("---pierre test1----, %s", s));
                        }
                    }
                }
            }
        }
    }

    String confirmedData = "banks-0:\n" +
            "\t\t\theterogeneousChainId: 101\n" +
            "\t\t\theterogeneousAddress: 0xc707e0854da2d72c90a7453f8dc224dd937d7e82\n" +
            "\t\t\theterogeneousTxHash: 0xdd8045d01f8b17a83d3f874877d373b8d0be616d5edbeaaea20398322b8c5140\n" +
            "\t\t\teffectiveTime: 1728633179\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633179\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-1:\n" +
            "\t\t\theterogeneousChainId: 102\n" +
            "\t\t\theterogeneousAddress: 0x75ab1d50bedbd32b6113941fcf5359787a4bbef4\n" +
            "\t\t\theterogeneousTxHash: 0x50fd30d2a05cc90e298889fc097fb80fc749a52de516abf6e6315bf431aa5aac\n" +
            "\t\t\teffectiveTime: 1728633169\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633169\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-2:\n" +
            "\t\t\theterogeneousChainId: 103\n" +
            "\t\t\theterogeneousAddress: 0x2ead2e7a3bd88c6a90b3d464bc6938ba56f1407f\n" +
            "\t\t\theterogeneousTxHash: 0x0a6fe092a16789aa3e5bf6a7beba3a9bf79fa619dd8d94477929ebc7590fb4c2\n" +
            "\t\t\teffectiveTime: 1728633159\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633159\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-3:\n" +
            "\t\t\theterogeneousChainId: 104\n" +
            "\t\t\theterogeneousAddress: 0xe096d12d6cb61e11bce3755f938b9259b386523a\n" +
            "\t\t\theterogeneousTxHash: 0x48854a468abd427c84fb702f2660c46f3a3ce3119eae37507803f8dcbe3eb298\n" +
            "\t\t\teffectiveTime: 1728633156\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633156\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-4:\n" +
            "\t\t\theterogeneousChainId: 105\n" +
            "\t\t\theterogeneousAddress: 0x32fae32961474e6d19b7a6346524b8a6a6fd1d9c\n" +
            "\t\t\theterogeneousTxHash: 0x0c60aa5eabe1bff69f4e32f579e2c096b9f54cd4d7b8d1a067e321997bafd901\n" +
            "\t\t\teffectiveTime: 1728633171\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633171\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-5:\n" +
            "\t\t\theterogeneousChainId: 106\n" +
            "\t\t\theterogeneousAddress: 0x9ddc2fb726cf243305349587ae2a33dd7c91460e\n" +
            "\t\t\theterogeneousTxHash: 0x2acf080a41ecb40a97c22b8c67a72fffc43c6858ffabfba02e543db13fe7b6ca\n" +
            "\t\t\teffectiveTime: 1728633173\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633173\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-6:\n" +
            "\t\t\theterogeneousChainId: 107\n" +
            "\t\t\theterogeneousAddress: 0xdb442dff8ff9fd10245406da9a32528c30c10c92\n" +
            "\t\t\theterogeneousTxHash: 0x5ab6c94565e94985fbc22b818ceba97919aa11f64e6caf29590f4584f5114f67\n" +
            "\t\t\teffectiveTime: 1728633175\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633175\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-7:\n" +
            "\t\t\theterogeneousChainId: 108\n" +
            "\t\t\theterogeneousAddress: TXeFBRKUW2x8ZYKPD13RuZDTd9qHbaPGEN\n" +
            "\t\t\theterogeneousTxHash: 0x2d54657ba3aeccfed76e67026030ffb644c9e1c52111208706c332b9b5328f4c\n" +
            "\t\t\teffectiveTime: 1728633171\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633171\n" +
            "\t\t\t\tTVhwJEU8vZ1xxV87Uja17tdZ7y6EpXTTYh\n" +
            "\n" +
            "\t\tbanks-8:\n" +
            "\t\t\theterogeneousChainId: 109\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0xb15fb437ccfc7fb9169a36cbf5010272fc2bc4e85f33bcc2702afd7806061263\n" +
            "\t\t\teffectiveTime: 1728633167\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633167\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-9:\n" +
            "\t\t\theterogeneousChainId: 110\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x8938aa8280967cfbd64b2651228b893ca05e74d451eea5ce83c54fb8b908d391\n" +
            "\t\t\teffectiveTime: 1728633172\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633172\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-10:\n" +
            "\t\t\theterogeneousChainId: 111\n" +
            "\t\t\theterogeneousAddress: 0xf0e406c49c63abf358030a299c0e00118c4c6ba5\n" +
            "\t\t\theterogeneousTxHash: 0xb7b681b2be1f087490875d8c57a47c441d5237ae5b3f77489d691b5430a46940\n" +
            "\t\t\teffectiveTime: 1728633174\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633174\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-11:\n" +
            "\t\t\theterogeneousChainId: 112\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x5ebb93bdb0f5c2b482acf802da157442d8728205ed2609d03df347bf6300099a\n" +
            "\t\t\teffectiveTime: 1728633173\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633173\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-12:\n" +
            "\t\t\theterogeneousChainId: 113\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0xeeed5142ee91a6400ddd7d96596e72e69f139516f9a7d66dac4a07b41918d631\n" +
            "\t\t\teffectiveTime: 1728633178\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633178\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-13:\n" +
            "\t\t\theterogeneousChainId: 114\n" +
            "\t\t\theterogeneousAddress: 0xf0e406c49c63abf358030a299c0e00118c4c6ba5\n" +
            "\t\t\theterogeneousTxHash: 0x4d873604986ba842651ae98591561f51c10ab0606eadd05ffc8972a18463727a\n" +
            "\t\t\teffectiveTime: 1728633180\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633180\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-14:\n" +
            "\t\t\theterogeneousChainId: 115\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x2e869800d5038713ce01dda4d563153a0b75366e8ce1bfa09578ca8c618277f0\n" +
            "\t\t\teffectiveTime: 1728633177\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633177\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-15:\n" +
            "\t\t\theterogeneousChainId: 116\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x41c2699d57e5178b26405e85b0b329a7fa5f08e6780dd14d0ebab19a30e3f2f1\n" +
            "\t\t\teffectiveTime: 1728633176\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633176\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-16:\n" +
            "\t\t\theterogeneousChainId: 117\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x6eeecce694219218ebb404d411b5455be7f6e9c6e9bde6b87abfc954ab63c59c\n" +
            "\t\t\teffectiveTime: 1728633175\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633175\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-17:\n" +
            "\t\t\theterogeneousChainId: 119\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0xd20a6e846b0b7ed112bfe3eb786e115b116ca8a35678c3379508a942853ac546\n" +
            "\t\t\teffectiveTime: 1728633159\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633159\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-18:\n" +
            "\t\t\theterogeneousChainId: 120\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x96f3ae434807e3a8f70017e315a5761f872ebafb919d67994fd5595133c67661\n" +
            "\t\t\teffectiveTime: 1728633174\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633174\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-19:\n" +
            "\t\t\theterogeneousChainId: 121\n" +
            "\t\t\theterogeneousAddress: 0x67b3757f20dbfa114b593dfdac2b3097aa42133e\n" +
            "\t\t\theterogeneousTxHash: 0xa433e1a2cb8d994c726bd585b021f0bac6f1e5403d895e9adef34ec34374dda0\n" +
            "\t\t\teffectiveTime: 1728633166\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633166\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-20:\n" +
            "\t\t\theterogeneousChainId: 122\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x7feac2ca6e0dd4f42cd008454b2212ebec38457ca903b829b8b8fe5b38fead1b\n" +
            "\t\t\teffectiveTime: 1728636839\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728636839\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-21:\n" +
            "\t\t\theterogeneousChainId: 123\n" +
            "\t\t\theterogeneousAddress: 0x54c4a99ee277eff14b378405b6600405790d5045\n" +
            "\t\t\theterogeneousTxHash: 0x784544654d19328c70f6bc1b943d8553f839f45fd817c393dd36a6f9bd46f2ff\n" +
            "\t\t\teffectiveTime: 1728633413\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633413\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-22:\n" +
            "\t\t\theterogeneousChainId: 124\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x3aede1b0b9fa48ce84f03912498f8a595a2c0066a6ed687581e7641e435b2d42\n" +
            "\t\t\teffectiveTime: 1728636839\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728636839\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-23:\n" +
            "\t\t\theterogeneousChainId: 125\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x07ebbb9612e7e2c656df2fb2b1478865a9dd7cc4de4196ddc183b9ddca2aafea\n" +
            "\t\t\teffectiveTime: 1728636870\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728636870\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-24:\n" +
            "\t\t\theterogeneousChainId: 126\n" +
            "\t\t\theterogeneousAddress: 0x8cd6e29d3686d24d3c2018cee54621ea0f89313b\n" +
            "\t\t\theterogeneousTxHash: 0xdeea3bcb0e6ad5a699d0fb677dc33620373bc4e114b5c5f0ea1c5e0507d3f919\n" +
            "\t\t\teffectiveTime: 1728633165\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633165\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-25:\n" +
            "\t\t\theterogeneousChainId: 127\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x7905b9e5f2bfac95b7adc4df304b77da96349b6162858bcafdcb8085b992d879\n" +
            "\t\t\teffectiveTime: 1728633419\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633419\n" +
            "\t\t\t\t0x0eb9e4427a0af1fa457230bef3481d028488363e\n" +
            "\n" +
            "\t\tbanks-26:\n" +
            "\t\t\theterogeneousChainId: 128\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x890e5537bcad4bce3de91b5a3c3eac38717dd2a5a78a0d5ffc1c05612cae4872\n" +
            "\t\t\teffectiveTime: 1728633139\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633139\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-27:\n" +
            "\t\t\theterogeneousChainId: 129\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x2dc1462550294017600c2a2eecae495760bfe60862478ccd12fb099584649286\n" +
            "\t\t\teffectiveTime: 1728633157\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633157\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-28:\n" +
            "\t\t\theterogeneousChainId: 130\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x967b37408333c13234ca35b92b82770b5683e44d30140d0ff9dcc89b5b9e63eb\n" +
            "\t\t\teffectiveTime: 1728633377\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633377\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-29:\n" +
            "\t\t\theterogeneousChainId: 131\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x07552311875c7e222cb77eb6ec0380ec631f4ab373ede5b8b6e4aca01dc9d27d\n" +
            "\t\t\teffectiveTime: 1728633163\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633163\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-30:\n" +
            "\t\t\theterogeneousChainId: 133\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x0094f386c223e7a38faf1e25ad4fbdc284e1c1d703b3622029aeb033d9dc62c6\n" +
            "\t\t\teffectiveTime: 1728633169\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633169\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-31:\n" +
            "\t\t\theterogeneousChainId: 134\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x749dac3a28e50930c8aeee9a8e3a43ed66b2a70bf9e20a3bd92b1862981a38da\n" +
            "\t\t\teffectiveTime: 1728633160\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633160\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-32:\n" +
            "\t\t\theterogeneousChainId: 135\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0xd00cfac38a9928c2823f937bf082b4256b7711b3d918139aaa0427513f564dba\n" +
            "\t\t\teffectiveTime: 1728633158\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633158\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-33:\n" +
            "\t\t\theterogeneousChainId: 138\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x91bb2d5a7a0b0d347fd0ca68fb4831329af3fbbac790244ffa9e3cff52eb54ff\n" +
            "\t\t\teffectiveTime: 1728633165\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633165\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-34:\n" +
            "\t\t\theterogeneousChainId: 139\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x2b93e2d0fa1e92bb9551c8ee96ee54ddd1fbc996ec819adedd4a05c24bb530ab\n" +
            "\t\t\teffectiveTime: 1728633163\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633163\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-35:\n" +
            "\t\t\theterogeneousChainId: 140\n" +
            "\t\t\theterogeneousAddress: 0x3758aa66cad9f2606f1f501c9cb31b94b713a6d5\n" +
            "\t\t\theterogeneousTxHash: 0x098bcf4806221202bc66cc3eb95d422ae7413e30f3c5704fd6a436a4493f62a6\n" +
            "\t\t\teffectiveTime: 1728633165\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633165\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-36:\n" +
            "\t\t\theterogeneousChainId: 141\n" +
            "\t\t\theterogeneousAddress: 0x0035cca7ff94156aefcdd109bfd0c25083c1d89b\n" +
            "\t\t\theterogeneousTxHash: 0x73126601724750a94119af4623b8e8b0f958ffb58f0106b8d265f09dac8c6106\n" +
            "\t\t\teffectiveTime: 1728633185\n" +
            "\t\t\tsignedHeterogeneousAddress: 1728633185\n" +
            "\t\t\t\t0xd87f2ad3ef011817319fd25454fc186ca71b3b56\n" +
            "\n" +
            "\t\tbanks-37:\n" +
            "\t\t\theterogeneousChainId: 201\n" +
            "\t\t\theterogeneousAddress: bc1q7l4q8kqekyur4ak3tf4s2rr9rp4nhz6axejxjwrc3f28ywm4tl8smz5dpd\n" +
            "\t\t\theterogeneousTxHash: \n" +
            "\t\t\teffectiveTime: 1728633127\n" +
            "\n" +
            "\t\tbanks-38:\n" +
            "\t\t\theterogeneousChainId: 202\n" +
            "\t\t\theterogeneousAddress: 35nAXxa7CtTk1dRZGYga3cBfn7mHRB4qS8\n" +
            "\t\t\theterogeneousTxHash: \n" +
            "\t\t\teffectiveTime: 1728633127\n" +
            "\n" +
            "\t\tbanks-39:\n" +
            "\t\t\theterogeneousChainId: 203\n" +
            "\t\t\theterogeneousAddress: bitcoincash:pp54cej9hyllw9qyvca3u6rt6csnyz46kuhlfqn0r9\n" +
            "\t\t\theterogeneousTxHash: \n" +
            "\t\t\teffectiveTime: 1728633127";

    @Test
    public void airdropDataTest() {
        List<String> list = new ArrayList<>();
        list.add("0x00000000008c4fb1c916e0c88fd4cc402d935e7d,0.000000000000000002");
        list.add("0x000000005736775feb0c8568e7dee77222a26880,0.000000000000000004");
        list.add("0x0000000099cb7fc48a935bceb9f05bbae54e8987,0.000000000000000001");
        list.add("0x00000000a1f2d3063ed639d19a6a56be87e25b1a,0.000000000000000001");
        list.add("0x0093e5f2a850268c0ca3093c7ea53731296487eb,0.438334411942757000");
        list.add("0x00ed6c13d9827cb6aec192941f17c811249dc12d,279831.067679478000000000");
        list.add("0x0132fda667ffe62e292acb2d87721d1a7b5eafba,1875000.000000000000000000");
        list.add("0x01871be1443e33c7a9177b44a4c332114bf006bb,75149432.268554200000000000");
        list.add("0x01b189ea88872d2743e99a31e46a8589116e7578,3217982.842897650000000000");
        list.add("0x01c51957a32420012cc395dfbbea33e8008c3f4b,6573333.000000000000000000");
        list.add("0x027ed2f9674fba4edbb1740d8b4b7dbedaaa49b8,6910175.688303880000000000");
        list.add("0x0328375788f9813105655a6336c13ca97d76650e,9536242.221586350000000000");
        list.add("0x0342bf4281d0742f45e97b931c7f3ef33c2db53d,1125000.000000000000000000");
        list.add("0x03476b674fcbc2001e84146092da5752034bca76,1648546.719999060000000000");
        list.add("0x034e617d90eba96b47a555d1d9b1bedbff196044,464000.027564842000000000");
        list.add("0x03b17ba0aefaee94d3be8857155c8eaa25faa9ae,1556970.878261400000000000");
        list.add("0x03e304011dae397c6890ad42360ac9c74648fe0d,262918.663104375000000000");
        list.add("0x040a77ff15a56e453a8b1e7bb631cb861166de30,9198528.000000000000000000");
        list.add("0x047c6e19c1f984bbb41f5b8fc6db3dde4eeaf594,8222871.617443040000000000");
        list.add("0x04d0ffc694bf7f49e9c213f251ef928dcfa33acb,100000.000000000000000000");
        list.add("0x05a4602070ed8746a759a854ab2499f523292cda,150000.000000000000000000");
        list.add("0x067cf965e50e0988893eac7dfdc9ee3d86f77b57,794972.771070217000000000");
        list.add("0x06b5d88862f0d90ae4d03570b6249a5f34e0e10e,1078818.705260210000000000");
        list.add("0x089a048e7e7f6667d0a1816f9d59c0d6ed908d22,1080609.369914590000000000");
        list.add("0x089a4ba7e4d3ce2b1c18777896a1a07465c58f0f,32517962.315457600000000000");
        list.add("0x099f5ab2e59661d284939d6be2621ff59d4e4d86,2000000.000000000000000000");
        list.add("0x0a2f81ebffa334117bbdb67b94ff75beefe34aff,4163659.000000000000000000");
        list.add("0x0a6c98bf0ae4490fe547ced2b8d7867c0ae872ec,3978423.582587650000000000");
        list.add("0x0a96534f248c9b98330e377f1091712ce50a9206,2804504.767709780000000000");
        list.add("0x0af033752b157b7ccfd0cb94e12b1f636a7e668a,20000000.210559200000000000");
        list.add("0x0b871d41b1fa5abae024213606bcf94e6f754ba6,30888800.000000000000000000");
        list.add("0x0ba494ed27fc87b6bd5528a0e59af4c987ab3b24,675354.863701925000000000");
        list.add("0x0bf43783c9afb65e9cd21ca4274460129431aa36,786666.000000000000000000");
        list.add("0x0cc00f8752a3ba21629e639a3f01abfc9ef48c8c,10597331.529471500000000000");
        list.add("0x0d4ba7836430f148f0a18e783211e2d49f8c655f,774628.067359791000000000");
        list.add("0x0dac0a6921efdddc731e45702a5ec377cfde5661,10828207.423946300000000000");
        list.add("0x0eb30a20a8f077404f9833ac97442acaf7fe0ab0,12628124.000000000000000000");
        list.add("0x0f35b6ef1eff18b7ade09131467d1c78f840f2cc,881381.353727484000000000");
        list.add("0x0f802e7949c4f5ff65fe9378a887b2cb6aacd3a4,1431163.872162100000000000");
        list.add("0x0f96d7177989e85cafd73a4dfbc58daf2b96f147,251539.400999947000000000");
        list.add("0x10468ce60e281bab9d5dc29f2a9f3b436d4bbf1a,31988900.473962800000000000");
        list.add("0x10626c828d66e15495c43b5d82eaced4a59a83fd,4882109.303000000000000000");
        list.add("0x10775170e4f16e2cca113c89642c55db7bc54dcf,3286666.000000000000000000");
        list.add("0x116e48c8b17a2d66c828615054aadb181e196d4d,3498246.783922660000000000");
        list.add("0x11d59233a7fe739a3d5218cef5c19eaa68fe7e77,35864211.255192700000000000");
        list.add("0x11ededebf63bef0ea2d2d071bdf88f71543ec6fb,536949.307365866000000000");
        list.add("0x120051a72966950b8ce12eb5496b5d1eeec1541b,6854716.303045770000000000");
        list.add("0x1210c68c517301dad5a7eb15761c5ef6888f85de,18532800.000000000000000000");
        list.add("0x1293117ccac79ba623a2530b5667566a03495f2e,2196287.000000000000000000");
        list.add("0x13703dc1957beb73c8561a5965b96f41b3d9664d,597774.180250005000000000");
        list.add("0x13ac8114ea9fd5e25a1977980476b174ea8bb9e6,897516.797064851000000000");
        list.add("0x13c23cd78c3b91c8b3e9b32c3a440f38ddec3db6,2241199.262310120000000000");
        list.add("0x13c665dc30a7f45a29c271ef92821afe7a042a92,354289.164664449000000000");
        list.add("0x13e1079e238b42be9d38c1d9740d88eebad4f289,0.000000266098206349");
        list.add("0x14e25f21fa7a34786b4f1ccf3d31fb25245cd5a7,11906076.606751900000000000");
        list.add("0x14f84fa150fa8a50cb2c3588c39a822d6b76bfb2,0.991370918266266000");
        list.add("0x15287e83d202982c67e190155225f38f1bf22846,7180611.667593920000000000");
        list.add("0x161d7cb8a9ad62c34332908c87e80782964c8fbd,100000000.000000000000000000");
        list.add("0x1670420b2ea905a87762fddf055b68405b189685,375103.711819127000000000");
        list.add("0x16960f7ca3f2be2244c0b6828610a4e7cf914ede,4123506.560504740000000000");
        list.add("0x178ab90a4b98e76ac6aecb2636cb1f3ae0326159,18502296.689273300000000000");
        list.add("0x17d1d0980aaed406293134c0924509c248277fbc,1706775.878710000000000000");
        list.add("0x180e7d6bc7add39e716e9f77235e4ce5960ba73a,3099804.257356340000000000");
        list.add("0x1844b727a901363b680660db28ec4b25055200f4,992652.256901007000000000");
        list.add("0x18dd2b58bcfb7565add41e81f676230718bd7148,4720000.000000000000000000");
        list.add("0x194e9ef98ea15cc39547af183b27e4405cc2a7a5,254013.489524155000000000");
        list.add("0x1a40fa0b86c77e08735f7abaedd0d6ee5ecbafb2,2322830.236304970000000000");
        list.add("0x1a670a18909535b8f216bc901c65124885c4bcfb,150000000.000000000000000000");
        list.add("0x1b466b80875bb914cb48d51370e411403e1fe0d5,1000000.000000000000000000");
        list.add("0x1b720da826f5ec6ab8e9fbf64433ae5f3f235a80,1307516.786537260000000000");
        list.add("0x1b9e9ad115b6f243b24651a14a93fa8d8104ec06,50657134.101989100000000000");
        list.add("0x1bbd93f18cee38cd2e9ddcc1b3750c5b4b6a00c9,600.000000000000000000");
        list.add("0x1cbcef4a446d3a576aab3cc96b539f0a8f56a1bf,2140000.000000000000000000");
        list.add("0x1ce8f8d65d4dc25ce78edc33d203b1b8b5e08a21,1766543.726902040000000000");
        list.add("0x1cfcb0c3492c919170b4698406f56e4a303d0ecd,232440.788534600000000000");
        list.add("0x1d95a2cd586b1dc9b142df734aefa9e8341abf17,11015584.000000000000000000");
        list.add("0x1da2aedc160e5267f4b913fea52c1bf9772af7e7,7500000.000000000000000000");
        list.add("0x1dd57989eb645aef82e69a3198aa133346aef90c,5000000.000000000000000000");
        list.add("0x1e6ec70a4a6ca3c204767297263c040065e2cf75,43703430.151596000000000000");
        list.add("0x1eaf201b01f837267377ac729ed7f3d2f6ea030f,627697.132242930000000000");
        list.add("0x1f1b85d652185bbf231f3f62d46c1cacdcfb28db,2028798.003974200000000000");
        list.add("0x1f33ed2ac608851ada7c15e07466d10cbd9cc57c,2140000.000000000000000000");
        list.add("0x1f47f88d05be0cea429ee4f0c2e7f95575974bbd,5831237.189605120000000000");
        list.add("0x1fd67accf6f24b936373f715aaccda6ab647a35e,5000000.000000000000000000");
        list.add("0x1fe06bb2fe7d5d1e70e2fa5d063c1151779d04e7,4700006.570531290000000000");
        list.add("0x1ff02c55149fb61a5993d7f66aecca44a145e78d,3963753.994369590000000000");
        list.add("0x205a303b7b6a315fc6095883747431131d10f8d0,30111166.000000000000000000");
        list.add("0x205ce7367f6aec715d4bdd277e1721d7d8d7b17a,6882884.250562580000000000");
        list.add("0x2069f5116c6ccbd1169751ad8df38f67704484eb,3750000.000000000000000000");
        list.add("0x2085e25ed9a6f5af02e366f107bf9a539aa0f59a,2284684.958135670000000000");
        list.add("0x20b8a090cfdd17e9abb554fb12612b976184a974,1125000.000000000000000000");
        list.add("0x2157c45fc569dbc3c34439b0223d08ac3e9b4783,2867018.901104790000000000");
        list.add("0x218f638b8861f07555f79242a46efed2e60aa93c,9795529.915670350000000000");
        list.add("0x224897274c25dc1fdc3feda7a1cdc61159bf6451,1772180.468439020000000000");
        list.add("0x22909e39fb6ba392922a56020156aa8a334cc4f7,63349963.000000000000000000");
        list.add("0x22c1ecef2d6f026837d91b770deac6821c163273,6028037.948694930000000000");
        list.add("0x232a7c673fe733dce2d48fd5cb902f1072dd1942,13146666.000000000000000000");
        list.add("0x2331e95e1059dd589c2d1f1a0b7a6c1755cd4750,14010430.017088100000000000");
        list.add("0x23f794515696a029d9d08c425ca8dcff08b9a5a3,1875000.000000000000000000");
        list.add("0x23fd930ed240cc43a2351cf1f0bbfd6e48b446ba,2441868.045222700000000000");
        list.add("0x246356be380c95e2f7bb45c16c6467e630d50637,1923794.764750820000000000");
        list.add("0x2530bb4e3631f95a8874802a66c07429a9b7035f,1649505.459624120000000000");
        list.add("0x264522be26eb65ba5715f70062b3650b3201cc61,34572403.951715500000000000");
        list.add("0x26ebf95b32b23e6b4e057b6d1cdfa7b15e35b0be,15000000.000000000000000000");
        list.add("0x26fadb5b6a363e3b392e4f81c87f46237607018a,4720000.000000000000000000");
        list.add("0x27bdd7ccff208defc4c6678f3caf829fcf84253f,19549701.980000000000000000");
        list.add("0x28dbbb8af5f9e465fed538f69ce2605547e7d93b,342468.611932824000000000");
        list.add("0x29472251c851a91da990968df2af894d41846dda,678553.260655929000000000");
        list.add("0x294744870e46cc57d542537544c1852f91da1e12,1192710.340845710000000000");
        list.add("0x294c9a5e1351cb43b8bd6a5e9b7bc82870da088b,4556556.457429010000000000");
        list.add("0x2952b7fe6755dcd8789f4429a96b63d97845f51a,69497427.182515800000000000");
        list.add("0x2a67881d02dc63c2e206aef02c8abf4231c60929,1285431.323129780000000000");
        list.add("0x2a9d78f54ccca2f760358c7187a87d1376fb7ae1,944000000.000000000000000000");
        list.add("0x2b14a8e66ff21cc5e0426f42ab8abb56dd38bb9e,758105.639549302000000000");
        list.add("0x2b5547971d2dc6574b730dc3be74bf1758221cfb,2140000.000000000000000000");
        list.add("0x2b56c09a4aab2dc957741899c5916a64596dc4e6,400000.000000000000000000");
        list.add("0x2c0380a0ecf6265e73d461d6a7bc907e4d60170c,0.100000000000000000");
        list.add("0x2c1c58eb50dd6778ea65c02385c1ce7d59626d0d,44667590.363412700000000000");
        list.add("0x2c9adda64015522e12f8202351b2753fe825576a,753932.231195188000000000");
        list.add("0x2d2eead1b36fbb847d8c3eb40b9c9e08da10a464,864598.000000000000000000");
        list.add("0x2d70bd16644abcc94d7780139fb74c9b92b6a4bc,100000.000000000000000000");
        list.add("0x2da3c9ba80495d3d3a187286d7f006933dc23971,464309.830281814000000000");
        list.add("0x2de8f4a76488c78455194a52e1a4b95346898ad8,3332461.324196470000000000");
        list.add("0x2e0edcf5c0bbd9fcd5a6c9dbc7b5f39207f60e8a,3000000.000000000000000000");
        list.add("0x2e4c003a210cb204ea74cfda8777558f6beec863,3959797.725432720000000000");
        list.add("0x2e92f1ea4d756ec95ce05fee03f7395bc68b2100,393925.921023591000000000");
        list.add("0x30dffcc6e85e6c6cb715ef54839de5fb49fa9230,1.000000000000000000");
        list.add("0x31fd2edac02912441c0ea69548395443e17c5a27,541516.235495608000000000");
        list.add("0x32acaaa604d1fcdfec4b88499803bb889bdcaed4,4280000.000000000000000000");
        list.add("0x330088977036e99d8e2ed1b5de8e14ce58be9b43,579532.799543890000000000");
        list.add("0x333a871ee6c923a8317f9ed25d840d634719e200,2742504.516076680000000000");
        list.add("0x347042f3fbbc9040c640f7cc4146e05a82e238bb,1987140.772336750000000000");
        list.add("0x3489b1e99537432acae2deadd3c289408401d893,5000000.000000000000000000");
        list.add("0x34e413733beb0fdd740cc701a15af3754afaf8ce,1063343.144566640000000000");
        list.add("0x351af1631aa5ea1ca62ad8a4e3cd87128d4d9108,0.363506174657224000");
        list.add("0x35206f7498a5ddc47b4b41dce346e5088b4ac4e1,3300315.551724210000000000");
        list.add("0x35a6daf11702b3edb62a285d6b4e44e2a9b026fa,450000.000000000000000000");
        list.add("0x35bb4b96762ccadcb1026b1c3a5ac47e76e5915f,25186113.723966400000000000");
        list.add("0x35ee02fb833c90656cb058a7e14689cccfe1056d,2000000.000000000000000000");
        list.add("0x36019e705e0c998c4d925ce4171c1481ed461d97,0.000036568637933264");
        list.add("0x36980b042d655edaa45d91515a20b3a4eac4e133,1539179.682622910000000000");
        list.add("0x36ec4bea46797ac5a7904f747c089106089b7452,6631453.183497330000000000");
        list.add("0x370824957aa5a482da3af012c508792b1851ac76,1038555.581041620000000000");
        list.add("0x37d5ac135ecb0d5658b8a468179eb6f9ce72929c,2899043.444341360000000000");
        list.add("0x3824bcfb419ff17d66a059a5abc798f569981082,615347.422038693000000000");
        list.add("0x382ffce2287252f930e1c8dc9328dac5bf282ba1,176325.692974660000000000");
        list.add("0x38b3882abc9baad642110c172f4086d92c855ce6,58235637.000000000000000000");
        list.add("0x395361541cf7ed11e5aa04cd93dcbc299a90859a,2140000.000000000000000000");
        list.add("0x397626c790056bf8a55f9ce2abb9590e90c7c804,36660000.000000000000000000");
        list.add("0xd39a074db3566ced7d68c54afbdbbfc61c140d14,2409316933.743190000000000000");
        list.add("0x3a8e62dacb6362efc8916c11e66c83bb57e08f3d,546595.982408380000000000");
        list.add("0x3b0bcf23fe50b37789cc702681e8afd3369b8a42,2541062.227422890000000000");
        list.add("0x3ba692641ec15c303edbd3dba35490ce4f88354d,1906895.515067230000000000");
        list.add("0x3bb6f768763e3798b83a4370f3587af549d9b37d,240015.803523946000000000");
        list.add("0x3c4506bd25da292cd287aa1128f69bee0e1b42cf,4109787.640931990000000000");
        list.add("0x3dde47442b21f6f71aabd7d7fbe82f2aa7c701b4,205223.119422604000000000");
        list.add("0x3e4bf64db6feac51cf962c01e7e247d2d0a94d72,70000005.000000000000000000");
        list.add("0x3f2d1a3470444adb0bd12cb65ec407851072ca42,382753.554791487000000000");
        list.add("0x3f992e135b28ad933a154dc23741e86f4f7d6f89,1000749.696978840000000000");
        list.add("0x3fa7bbda3af4a99a08c0da210dd048c5b233eb8b,167063855.000000000000000000");
        list.add("0x40aeb1b18f5c50c0f5eb74b07aa8bcccc1cee45c,1875000.000000000000000000");
        list.add("0x40bfbcf21e4ff6e693787e0e34ec4a1c135fae19,316869.662903322000000000");
        list.add("0x416f8028c051514f5e95299c62656b686fb31b91,1249345.514459550000000000");
        list.add("0x4175403c387707c64598da59c6c5d457b31fd769,2490096.661999970000000000");
        list.add("0x424ae84d6a894443956ba8c7b59c277dad6b35af,0.182950788567239000");
        list.add("0x4270eb37f84f5d9b6f3551f7913dee50fc61f719,5152668.504481720000000000");
        list.add("0x42b0e1adcdbfe2abbbb83c2b4e34a82636f515c7,2858083.485215520000000000");
        list.add("0x42ca4e7520d99fe6dec5984006ace9b931793929,560105.246598524000000000");
        list.add("0x43ad2fcdb9f8aae225babb60ef5506d50306d97a,5825380.379755270000000000");
        list.add("0x44daa60bea7ae144f74761d0c8e8d94a92eb9bf1,5844011.087097530000000000");
        list.add("0x450feca6be15b9ce2cff2c7a2895009e01c5f569,5441974.376682100000000000");
        list.add("0x4513d02a3cd0aea786ab527243473a2e798b9d31,2766812.000000000000000000");
        list.add("0x459f21340f005b9ba2a93db36d67c23b39ae4c4a,21714887.000000000000000000");
        list.add("0x45b05163963fdae6c180a11dfa6e42e39058de3c,1252392.991098820000000000");
        list.add("0x47ee04908e1dec41df719b2c8cc5f437e1a498f3,1875000.000000000000000000");
        list.add("0x4a3f41083d4615313626f8aff4e244c9798e897e,5600000.000000000000000000");
        list.add("0x4ae26e87e97374f44fbf25eab31461256840520f,250000.509230000000000000");
        list.add("0x4b2848ac2b42891dfe12b3adc4668135b725e8ee,758265.978652702000000000");
        list.add("0x4b5a82053de30a9bfdc75a6cf8c0b5d74dc9ad05,3653153.359790960000000000");
        list.add("0x4b94e3bb0bec004d6711586aad2869b10996348c,6695795.883754580000000000");
        list.add("0x4be1559167ced04f92b1a7dd6abac674f7c8d932,3750000.000000000000000000");
        list.add("0x4cf34de88b5ff01ce56216581a117908205bb889,13463228.992405500000000000");
        list.add("0x4d1144aa8b03a7a899ef279a2b5176ea302fa678,1203632.019140110000000000");
        list.add("0x4d7081186dc4bc0b8e3f3a268b1876659e8fc84b,1702843.000000000000000000");
        list.add("0x4d8f9dd4011dc9e805faacbd53afa6afa244542c,1316589.765745290000000000");
        list.add("0x4e48d65b0962bf0c2fa9a6c487435d4ea54552b6,3595158.929405140000000000");
        list.add("0x4ee6236636ab9a70796b1ef55a02a103ee2ab3e7,1972815.919302320000000000");
        list.add("0x506c0eac5603ad37985d5d68dab3d22e0a2b8ae3,229644.018581931000000000");
        list.add("0x50bec6f02aa38577955e3d595137b335fcf1a822,386000000.000000000000000000");
        list.add("0x50d4a15ee772bac5734e2403aa22057cd59b099b,0.455723734155615000");
        list.add("0x50d76fa664b9d7c44b3a105c22b48a788669e625,1168853.333431880000000000");
        list.add("0x51188f0a9a25fbf09bf90bcba9ca241353af0bf0,28770597.312387200000000000");
        list.add("0x519ea7518da2bd6b6eac6e4e403493bc8f7e615a,7879381.445246150000000000");
        list.add("0x51a33136360e53ae83260e4bf1820ad329d671a6,800000.000000000000000000");
        list.add("0x51b304f96e7b4d482f95524f452ddcbf0ba15b6e,2593551.874911110000000000");
        list.add("0x51c24b5921a23b457673b6d86cab778e5e9ba974,1140691.270200660000000000");
        list.add("0x52a64ff6ed9c7e5dbc97166be3e896293579935c,10234514.782267900000000000");
        list.add("0x536d18ebbb26da78c5b51a9f2ba9be423ab41b13,1039521.520866530000000000");
        list.add("0x549d8ad25e3a1f98de284bfecec9dc579fd72f7c,2817788.297952470000000000");
        list.add("0x5604fe64d6e8a17adf7f0d293151eba0af5b0dad,1138057.353619200000000000");
        list.add("0x56126b084a5b9ed659d2d845b15f5345620f6f1d,7639531.000000000000000000");
        list.add("0x564e645557434713b9bf18c21dc86f9164ff2902,50197216.000000000000000000");
        list.add("0x574ba8760b130b4efcf11c95308a88048363c4a4,14000000.000000000000000000");
        list.add("0x5821fc32083920ab9841e74b61ce5da42dde26ba,1906338.125948600000000000");
        list.add("0x58c04ba439d4671cd92b014bb84e1fb1f6d0f7e1,830327.540171361000000000");
        list.add("0x591148cfb7641828d1d1ea18dce20e8506445f61,882007.490424050000000000");
        list.add("0x593f735f40f90bffc51a48696164a464820e70ea,100000.000000000000000000");
        list.add("0x598fd2172fa5d5185ca71633adc799338ae3fd0e,2878203.996563610000000000");
        list.add("0x5a447f1e945684ba800966d844972c7e87c49282,2199524.742779350000000000");
        list.add("0x5b22681e6f8799a910b473677ff899b20d462672,3022467.967187680000000000");
        list.add("0x5bb5323d524537abd40cd2fab64a7defb5781b44,6735344.580742430000000000");
        list.add("0x5c5481deff6f0bc281adf99d043de9109a7c1257,2839906.467337620000000000");
        list.add("0x5c841929608d2328533c0c6fa4a2a2631d1db074,1105886.210735670000000000");
        list.add("0x5d566b40c8aaaec8ca38f67ed7fcddbc631126cd,37281060.000000000000000000");
        list.add("0x5de0d4539acd5c60bf4febeb0bff03c63e525455,0.753680041512089000");
        list.add("0x5fd5be85da7fc379d32cd343d3d5cccbdebeef2d,1036867.467857890000000000");
        list.add("0x6044a40d327e43426e01586374e123141117a63c,390594.443564390000000000");
        list.add("0x60cde01749e0b083d848bb6c897b31c753d65f2d,5554974.056181330000000000");
        list.add("0x60d4f414ced406de84e2ee9f83c18cdaf6de98b4,5851119.754842110000000000");
        list.add("0x612aacab2b1af08ae45fd0fa4e3cf24868c8d072,102838804.000000000000000000");
        list.add("0x61958ed6419a176d6cc3ced00a42c9f536e170ad,7116971.114422840000000000");
        list.add("0x622a48f9b1acb663f828e9efb6fba99087019173,2865445.000000000000000000");
        list.add("0x627faaa13ca3d25bf959325d44543e04c99ccec8,20823145.789562900000000000");
        list.add("0x62e7e2fd9d1b317db37f6c36e0042e24fb7b3f48,1687759.448755990000000000");
        list.add("0x630bc7dd0abfc2d196289ce09db947dd2cafae7c,19585000.000000000000000000");
        list.add("0x631965dcee2ff968632d3a2fb6b56e4516aea082,10743550.306724700000000000");
        list.add("0x64945302884ef37887739223718b6517c0208bb7,10785558.754164300000000000");
        list.add("0x6495eeb0374c0f741c93d59ce1d343f8103331f2,950000.000000000000000000");
        list.add("0x64cc4897de70f3156bd7e4047785808a06cea489,3183699.184322150000000000");
        list.add("0x64e00c4af8e1927ec43c6b72aaa5c6ac260ed2cf,2584187.424698860000000000");
        list.add("0x654468e020d7f8ad67c092a607785f3e3becb6b3,2360000.000000000000000000");
        list.add("0x65e385715878503ba53e0d3f6a8b5a2ff0c77614,4089772.222305510000000000");
        list.add("0x66082a9b0a65c957f6f794222e5949c917ea8303,1000000.000000000000000000");
        list.add("0x661b18a76733ec0c4cb202b86b1a871fc58c258e,3625058.000000000000000000");
        list.add("0x66b1a06970f7217575b9987aff87d91f6a7ee661,2079520.401577400000000000");
        list.add("0x6702d04fe34570f092a29b6c0f8dfafab68cbe36,5054337.484283710000000000");
        list.add("0x67a529b810e5d89a7c7e399b9982587ae71951e9,4508157.976943350000000000");
        list.add("0x6816ccdcc13aacd8eabefb109ee1816e041d558f,1106314.117837330000000000");
        list.add("0x6841f85301fc8e12e00137717d684c9e50ce4beb,4947599.000000000000000000");
        list.add("0x68c67117a0de39f3aa3b67d4141c052eef101dde,6030790.000000000000000000");
        list.add("0x6995870d9890e10979afcbdea5d8d249e969822d,658819.659989906000000000");
        list.add("0x6b25a2eaca3e3446d163977c9eb4ff3e8640cf0c,99135.398799603000000000");
        list.add("0x6b943dff8733ca834524c691531905d7f19e649f,3286666.000000000000000000");
        list.add("0x6bb69382271f8514fccaa4e92cbf8024e9d7daaf,1875000.000000000000000000");
        list.add("0x6e15d645b2977bb90866ae27c7b75a4a67bf75f0,1825513.052724970000000000");
        list.add("0x6e3f8993370774e4cd0f0f12741e8e7e38ba3c8d,15681482.933077700000000000");
        list.add("0x6e77be3c33894e8163425a97aa97b7f901d6e936,6851666.000000000000000000");
        list.add("0x6f04a432b28230cbba7a0bef4cbf0c70ff0a2cc0,6677345.419682220000000000");
        list.add("0x6fa50bcd7d5c0f4ac32498c4d42368a1b880e6da,168866.334872999000000000");
        list.add("0x6ffe4f6124fb49412b54c028fe77d51833ecf64f,10000.000000000000000000");
        list.add("0x70694cf532b1b95f3902ae84d168dfc7f6b32362,4312310.792666130000000000");
        list.add("0x70868b1edfb5c024257a24776c9806626200dff7,700000.000000000000000000");
        list.add("0x71218dcfdbdb0bbda99488b640792e26a4720e43,1.346021088340760000");
        list.add("0x71270ae5ba1b91a307257eb288bc5786ff228c3e,1141536.085035710000000000");
        list.add("0x71c60b00dc25f3ed2e0ccb6ea5249f0b7798717f,1496744.758944190000000000");
        list.add("0x727f115d36ee44d62e81ab9611c1130484e403b3,1676714.176723040000000000");
        list.add("0x7280f6dd56b1fe324adec580602e46da2abaf784,598338.717950695000000000");
        list.add("0x729b18a3f6ec3cd5360b51ad82a7e91c344d48fb,720000.000000000000000000");
        list.add("0x729f0d8835618820ef5f4fe0221c7c6913260532,23000000.000000000000000000");
        list.add("0x7365ae343befd65d672a55707d9c898f1c4de92d,1214175.031634710000000000");
        list.add("0x7390662934ceaa7d019ba51cb819acc2afcd6a9a,1415953.995748250000000000");
        list.add("0x7400e7d9290b7b8cca6e7d915c3b0623dd7f372f,3750000.000000000000000000");
        list.add("0x74055ba6adb44f86bed5df0f8e4d7053d2769ce4,7668584.659129520000000000");
        list.add("0x742b3eb91dede87fb5e30653965d77fd6544a73f,30815078.787809400000000000");
        list.add("0x74abcf7f6824ad37c82476ed63c32d43e3bc3d08,1080689.248507040000000000");
        list.add("0x75682a6f58a36eb33a7811d5d0d69593a4983194,0.000000000251334540");
        list.add("0x75ac11e4ea4e57e6875545271bbcba33c80b7fce,74449594.000000000000000000");
        list.add("0x75e105dffd855995652567c07bf0e0cbd58e1c7e,1069663.608305090000000000");
        list.add("0x75e27b34d511bf049c4a70f5673a5a0aa54d98af,1743826.855035770000000000");
        list.add("0x7622ab97980567d8ef1022dd8dcea81e13d2c259,723022.832900000000000000");
        list.add("0x7678e98f9055e72bd4864fb03fc6749777c3ca90,504774.162449922000000000");
        list.add("0x77381ca85c2d970eacace0319d9e94e2c8895619,37504047.512352700000000000");
        list.add("0x7774571ce94a8dc1b90cb9b8dccc8b78527dd9f7,222258.005522496000000000");
        list.add("0x77de62f0efa475bbcc0b5cbb79726c9999f61b1f,4221002.382430780000000000");
        list.add("0x77e03a09e69e0f1eb8c585e6ab67a28f98e4562f,36457.836407399900000000");
        list.add("0x7882456e69eca751d24337c623dfa4540078db15,653723.631991274000000000");
        list.add("0x78b2eba44fd10e29ced32bf99edc69ede16f8917,11909298.399616700000000000");
        list.add("0x798a209836eb25839f8133b07dac28464174d0ad,4530626.655499040000000000");
        list.add("0x7a06fb41528e5b13c1e4fdabfbd2d8a260830ad5,4139351.731718760000000000");
        list.add("0x7a343a64fbe43b1208660c7fd573769f1a56018a,39942.000000000000000000");
        list.add("0x7a4359a8bd3d495dce6a4b53603d9253fa8189c3,11100000.000000000000000000");
        list.add("0x7aec2694fe1bfd2ad743047854cae07b0621c3b9,39011332.412192100000000000");
        list.add("0x7b07de0dac7128016aa46d798dd05f84e8121ef6,500000.000000000000000000");
        list.add("0x7b2dd8aa195c683669ba3846da5512d880bbf8c3,7731425.946476970000000000");
        list.add("0x7b6bd4070e111160cdcd6b6d32f90719cbbc3650,30000.000000000000000000");
        list.add("0x7c32a22c824d2db82b00a30ea4ff973a632b1659,55996.633195822000000000");
        list.add("0x7c56d95e481df5f2ca5defea5d0f7538a5dde9c8,13521928.517118900000000000");
        list.add("0x7c8ceb08fca2cb1ff72dd76ee2d91031f0cec504,1271274.949055720000000000");
        list.add("0x7c8f44bbab1ca4b49e1ea2bfcb4b1a302f50e20b,11.828604651283700000");
        list.add("0x7cc78773cf63b47cbbc5aad573ace669d3ab4feb,10218383.017585900000000000");
        list.add("0x7cf09d7a9a74f746edcb06949b9d64bcd9d1604f,0.000000000000000001");
        list.add("0x7d2b3f5cc6d7a8d6f1a73d54cf2c79bef4934152,27234480.235142200000000000");
        list.add("0x7d2fa2c18e0f26d906ef3ea111eb993e7bc76101,615213.949072277000000000");
        list.add("0x7eff59fe997fe7dbeb91bf636547cb1059076a60,5636355.741080510000000000");
        list.add("0x7fb0efee69d454cbcdef619ce16a9e441e8e8b41,1880000.000000000000000000");
        list.add("0x8040c869287d5994b1523118f5df6bf54cfaae0c,656615.342545841000000000");
        list.add("0x81a124a637b1d465c07441e1618c4f1c255e7b82,2101969.854509840000000000");
        list.add("0x82329ea3c9cea457cf3a6596dfba77eae636f40c,1040563.608910240000000000");
        list.add("0x82976d81ec405a2eab7e00b5d34055fcd85c2970,2888944.648663060000000000");
        list.add("0x830f72836ba3a4252e12916d6e467aa73a9713cf,1875000.000000000000000000");
        list.add("0x8337bbbafa168d682af20bbba4ac99db3607d9fd,334348.401323845000000000");
        list.add("0x83f170a29dcc7947f6aa0de625f0a2b9f756f9d9,6177975.297207530000000000");
        list.add("0x84e356a5af981e8e91513396cb75078f0f638bb6,139604617.864165000000000000");
        list.add("0x85920645852335d756c8695553672bef788cf2d5,2119905.465237920000000000");
        list.add("0x85d9a835a2e57c339dd5727e299637017b94c8e9,9800000.000000000000000000");
        list.add("0x866d991a0fcff2fc1f39829172d00dc22a543252,4199640.660000000000000000");
        list.add("0x868d63cc31a4eee661fc93bb5d15227a44328ae7,886952.000000000000000000");
        list.add("0x87105810e27ea15d7a2755a03dc14bc522ce74ba,0.434947373782845000");
        list.add("0x8735b430dcf67ea17ded8e3a3824535b807f0375,54446371.000000000000000000");
        list.add("0x87eddcb296bfd7b756fa16c0b4046d68bf9ec3cd,3034835.989664400000000000");
        list.add("0x87efd7240b7f7d387d278ee80b3a8304b20a960a,0.740985759322377000");
        list.add("0x88a520856df657dbb84a884774248453c2efb99b,990.000000000000000000");
        list.add("0x893d288b3e7409f6b4f710d11854d932b54389f9,196206.793817298000000000");
        list.add("0x8970873535ebd2dc5d5817a8194f01c58d7a95fa,4111111.000000000000000000");
        list.add("0x8a18169c1ff7508906c49cc9b3bf6e451da959cf,1432071.629670700000000000");
        list.add("0x8a5d335f9b2789f7db40d8f79cb59a401b268b33,1875000.000000000000000000");
        list.add("0x8a91a269966f6ef8af08f97f89fc2741e49436ae,3010272.014550790000000000");
        list.add("0x8ad34542d4d1bbd462802c00674ee59145c26652,1875000.000000000000000000");
        list.add("0x8b6ea1b0cf9859fee05b089f91d2ac79a28f7adf,10302069.919969200000000000");
        list.add("0x8c46353b878673c1cfc861905005a1638fb1b465,3924104.700750430000000000");
        list.add("0x8c8b40cd5d7ac009c00f83df289323feb1f24d11,2982528.706625810000000000");
        list.add("0x8cbe028302daac3c6c96f73517febf7f902ba575,4378109.988524430000000000");
        list.add("0x8d8e6daa90afd08fb52a7ec4f1ced8da05347263,755066.262685731000000000");
        list.add("0x8d8ed2a5a9dba8d780d541a585fcabbb2a71af7c,13341078.895919700000000000");
        list.add("0x8e52796da0ae7c42da00a17a25131778dea9a122,4111111.000000000000000000");
        list.add("0x8e909f987c9eebc29c2733aad68eca4b4889d005,677502.468224274000000000");
        list.add("0x8f8071cda2594d8891e440fd05b911c854094188,461355.831031261000000000");
        list.add("0x8faf7d4dd5110dc3d3c17d7b0d14640c9fd21239,1380775.976370410000000000");
        list.add("0x90c05622efe59da62d8bb41e36c577173af35fd0,691584.244363104000000000");
        list.add("0x912e7c0e889b1b8f42807011d587d075e3f7c507,33866172.611178600000000000");
        list.add("0x916de917e4194dbb936e1574dd4f80f73c086a4d,2096666.161736990000000000");
        list.add("0x91cbe28c18b2217597deee6962c0c105673dab7e,109685716.844813000000000000");
        list.add("0x927797eb84ae27727c7c61833ae166168225181f,2120500.000000000000000000");
        list.add("0x92f2a3e91b3e9f093cf9aa3d2c89a2b0b4522b8f,2266541.036889190000000000");
        list.add("0x9351ef6cacd3a5e546040aceea94047a9d4de68b,4060981.629754500000000000");
        list.add("0x93813db1dff2747726ef3b697d3e70ce36c22e0a,12737236.860619100000000000");
        list.add("0x93b24bd7c65ede4e7620f16a48e215318d1ad996,0.000000000000003529");
        list.add("0x93c2355672118297b9c55b2e9b301de489d955a2,2759192.000000000000000000");
        list.add("0x9471a0a9c91fadb918f425299a8c9b012e52ca31,2337749.093487810000000000");
        list.add("0x94d3dfeca8bac903f4f22882369662ad1ca29d0c,5396645.843558150000000000");
        list.add("0x94f79b18513760db24d275cabff96d30caa5d2e5,515666.093630587000000000");
        list.add("0x95a9749dcf4e3e474d50506f702bf7ad151f90c7,13195661.541653700000000000");
        list.add("0x95c318a011e381095dac7224599d128167a7841c,0.796859676623186000");
        list.add("0x95daa0b7ef78270d14971fc79a54bd093a52c7ba,81813534.394508200000000000");
        list.add("0x960b1104a078c8f6ed75cc69b39ce94e274e7794,3000000.000000000000000000");
        list.add("0x960ea5e44ff9fe0962852d39e78ed3810c537893,3080592.928921150000000000");
        list.add("0x96712ef6c8d1948010bf5ad897db8378b1563a52,986116.371418356000000000");
        list.add("0x9684a34c4a51ab32403fe09162f088dd334792e9,4508184.652000000000000000");
        list.add("0x96c195f6643a3d797cb90cb6ba0ae2776d51b5f3,1286.526674320770000000");
        list.add("0x974adffafd50ecfffdacfef9bce6a186f9373dbd,2245218.388929710000000000");
        list.add("0x980310a0d2bfbcfd5994c821a296a522e56d5753,1470294.380007750000000000");
        list.add("0x9842d3d8082d504b28031e180979efc1f1e42247,2853677.656800640000000000");
        list.add("0x9945e2e84efcc429a89471b82a1ead871f2c03b1,11449350.027725000000000000");
        list.add("0x99691ab9b554e468ce910a091f361c96efa5b775,1125207.123178580000000000");
        list.add("0x9a2cee07330d0a8a3d0079f2f68d6be693a1c097,3921562.166163620000000000");
        list.add("0x9b1204189d7281de8e753b8d41ac7cc9de41f433,5619887.986953480000000000");
        list.add("0x9b29ffb0cf323285ae59876d0f5c96d067c0f3f9,3750000.000000000000000000");
        list.add("0x9b525c464d4e51b13df900bd5f47b0a0a1b00f2b,3750000.000000000000000000");
        list.add("0x9c9964733479a6e0758d97a7b89dce81c20b19d7,1186048.445361190000000000");
        list.add("0x9d5e3c411cc0fa1cd977b58c36b5a7afe07f19ef,3500000.000000000000000000");
        list.add("0x9d8609fc9ac8ce94cb0029942594f2f7b8481066,517274.484431345000000000");
        list.add("0x9e7ba38f4e2c637ff1a45769a70c0b55353ef68a,39400000.000000000000000000");
        list.add("0x9ebd335d42e39eadf1a615076e4bde2663bff7fd,2458914.327032370000000000");
        list.add("0x9f379f3e251a6a58000c8321a32f60a62218c178,246156791.495485000000000000");
        list.add("0x9f3f40ee07bbf5b1722ef7c1095dadd000982e9b,3999415.008539820000000000");
        list.add("0x9f825fee9cd977a79c9fc56c0d08bb349c26a89b,6996041.778685950000000000");
        list.add("0x9f8ff0c2c2dfef77dbbb050d0376eba8468489ee,1500000.000000000000000000");
        list.add("0x9fdc2094459f383f6b1c32e21005b765d342f097,14160000.000000000000000000");
        list.add("0xa00f069b4f3c7667598b091d84c4a26f99e38380,1000000.000000000000000000");
        list.add("0xa0811ef6d50d511ceebe129f4fce112d342f3a04,11428180.000000000000000000");
        list.add("0xa0b84518551e1102db4f55f18bd350866569125f,4384849.051232880000000000");
        list.add("0xa133e5ec1a4edf8b6da9f515c52882b7d736b542,1875000.000000000000000000");
        list.add("0xa152b6dd19b39a46011482073a5b2d3edb5b8806,2872397.828976180000000000");
        list.add("0xa186d0aca8172a53d263467885a38af8a40e7926,2500000.000000000000000000");
        list.add("0xa1bb332752c5ebd21823779e5876b746f7c6926d,1045844.745286660000000000");
        list.add("0xa1d4cebe28bb29405cf65c08c967252da3be899c,2026470.029193600000000000");
        list.add("0xa1e0c6a294ddb91fbb99ed8f60f3a86357a24e81,55245766.051671600000000000");
        list.add("0xa24a671a47e0b248d1ee3c5e6591288fb9f45513,4424020.123682540000000000");
        list.add("0xa350c0889aa7a2d9ba72ff8ab28abdd85f4d940b,11598.531650481300000000");
        list.add("0xa4dc2788bcc832095bba2b148a5d98d0dca270a6,7910486.980000000000000000");
        list.add("0xa4e59a3b437e29a490e8cb1e8ee971ad6d822572,5900000.000000000000000000");
        list.add("0xa4e5cffb8c96ddbc2e46574c678fc121180abb99,983088.661651339000000000");
        list.add("0xa6702494889294abc123fb0a2b3fcce31757b3ee,9006661.980718350000000000");
        list.add("0xa67729dab3f1d0c5b8f7e71a7386ba0445f8209a,506394.567324191000000000");
        list.add("0xa7b6018299f3abcf26389ae8f7f33e05c08fe97b,1701999.412529520000000000");
        list.add("0xa826cf3689996d341bcc047fd2881f6aff075ea4,140000000.000000000000000000");
        list.add("0xa8a99ca7fd656330ae9794212923503533dcfe03,1186568.000000000000000000");
        list.add("0xa935433de1e70538269c417a01543b3da8478a48,0.778728233815505000");
        list.add("0xa9deb0243ce7e0637231d114ce809af4a28a646d,3000000.000000000000000000");
        list.add("0xabeab23981d2f29d9bf032ebf4532f30bcc7f92c,0.952988499903844000");
        list.add("0xabf1d62405bf9c4d83d85e82c86bf4874a13ca69,1584293.358341470000000000");
        list.add("0xad002d200d3447a4fb59e4115c281b9586372961,12822461.637201300000000000");
        list.add("0xad3d0280bb6829045f66751270301e75f265977f,138762.647746069000000000");
        list.add("0xad4208d913c016f93d1fb8081b9094b885ebe91a,11207646.047490100000000000");
        list.add("0xada13fc7089745118d55468d8b384f2697c33e14,7098242.607008030000000000");
        list.add("0xada33052993c788241877c73816126e12694cce4,2016857.485332110000000000");
        list.add("0xae00f8d9ad23f5602bd2a9847db3c09521c7df79,886460.651250426000000000");
        list.add("0xae11e1c23405a125819561c79cfc25f1298d223c,0.221932913291957000");
        list.add("0xae301164bbffdd69e50d91742866148cdbcfee96,6020370.196902400000000000");
        list.add("0xaebdfa603001303fccbffe97cd5f8209dfd35ddc,3931310.000000000000000000");
        list.add("0xaf501b1a327d4542beea42616e67ee463849cbc5,3095478.847911470000000000");
        list.add("0xb01a9a7ef5779fa8680898827cc2b1aed638ee2d,568128.400000000000000000");
        list.add("0xb239aaf8c44a786f305b78604de2d19af17944d4,2178614.810766310000000000");
        list.add("0xb2c7f20c52799c55f78087ec976644c4fb00583e,3936942.940968440000000000");
        list.add("0xb3067d84035dcb8747a95cdf3161d520b4f61171,354000.000000000000000000");
        list.add("0xb58469d49571317fa8bed6f239caf02509dab833,472996.763005659000000000");
        list.add("0xb5c9768b0a2a396ff2bdb052a14cdfe61f62bd5d,100000.000000000000000000");
        list.add("0xb5ffa3204e9454043976702fd75e9aaaf0ea69c9,145243.643460629000000000");
        list.add("0xb83a67645167326292f80f53eb35622f2410f8b1,2000000.000000000000000000");
        list.add("0xb84f1cfc7ec8ba82d5b10f4f4bb583981ecad5e7,3951473.026655640000000000");
        list.add("0xb8cdca1978436c777df741eda44606b2d4231fc9,2888551.861435900000000000");
        list.add("0xb9d0ef0d7fcd65ab699115fa23160ea955254e02,3828770.746622840000000000");
        list.add("0xbaaa0c765fe6e387d1e749a8c9d144039f601311,44436106.000000000000000000");
        list.add("0xbb1eb3f5041ad84e8a54d1134bd29b80b1a2ecff,0.463538959068712000");
        list.add("0xbc147973709a9f8f25b5f45021cab1ea030d3885,2454.020575411680000000");
        list.add("0xbc3b9b106a497a5b2956b8d520f20b0afb8c14bb,5065561.051768000000000000");
        list.add("0xbc4d53af44d143303713461b5ba9ba0bf6cb1cc3,503571.526010396000000000");
        list.add("0xbc5897a6700ac48fe4a5c0fe4d4b1e9f613c9eb4,3965238.670189280000000000");
        list.add("0xbc80814146c329325c88bba6cf900406b443157f,1111.000000000000000000");
        list.add("0xbce5aca2dbb1057d8c4a76fc85706713d4f0bd7e,2577636.689157040000000000");
        list.add("0xbdd75b461106896f626385b63ae65e6bed0003d9,13000.000000000000000000");
        list.add("0xbe05b4310c06253aefd4f36d9bedbf51c39ad0cf,773452.997968215000000000");
        list.add("0xbe236e23a986c06ac7a3a369ec55351b57214b99,0.570000000000000000");
        list.add("0xbe37d0c3a1e056727c22c845017c677a35ba6a37,545795.627122784000000000");
        list.add("0xbe6437c73e538727613198efab2d3b2d8c64ee74,6695503.120527150000000000");
        list.add("0xbefe469b093863fdd52f0d0a845ca6682a5130f8,604868.867941383000000000");
        list.add("0xbeff1734a1f74a4e7ee6bc18acdada0dfc6eb6ec,11800000.000000000000000000");
        list.add("0xbf41c68f6261b31df61c3f9c1902fbcfdaca62e2,530413.000000000000000000");
        list.add("0xbf75a79403906139bd56c6c5df73aadd9588083b,4280000.000000000000000000");
        list.add("0xc04405fc2dd1b5d9457be1ac5b31d45a2205460b,11619800.000000000000000000");
        list.add("0xc0677f6b10a1eca66c65ca044cda6ee7e87d5bba,45149473.334867600000000000");
        list.add("0xc081abcb7cb788bd3af2ed64d87fbe3c95b589f5,2000000.000000000000000000");
        list.add("0xc0979464ad8b240725ddfc344922a14bd5babba9,4023031.106772970000000000");
        list.add("0xc0a904783c1d87b8375b6f67539cb23378988de7,26222200.000000000000000000");
        list.add("0xc0e2533af49b2c1cf616d9352a11f4417440ab96,439469.000000000000000000");
        list.add("0xc0efb827cb55b072f173f7aff8deb2095b05b991,122.000000000000000000");
        list.add("0xc0f4401a650d1ca1d404e9c978f89e81aee5ea77,11968640.023516800000000000");
        list.add("0xc16f0800f7628efd3109fce1b6606a6041bd7663,924038.256877589000000000");
        list.add("0xc16f8144f823cc54fa9caee38335db514f733800,639933.986389200000000000");
        list.add("0xc1e060e33d776af85280a3371455120215016065,3500000.000000000000000000");
        list.add("0xc1ea39fee77cdf59cdea493d489e635b3b8cbb85,3958818.837955440000000000");
        list.add("0xc240c90520b25b0eabb181a73c9cd671c71da907,1695382.773510490000000000");
        list.add("0xc241b740ce5503ca745f0313d1474bcee286ea54,1100000.990000000000000000");
        list.add("0xc3681665d0617005c0d9d4d325fbdf9a1555b3fd,1875000.000000000000000000");
        list.add("0xc38101c3634dbafd860b1b59d8ea5c4ee11d5420,3363766.802577930000000000");
        list.add("0xc381d5abe4a1e7624035e6ea1fc30daf5005ce7e,2512022.287527780000000000");
        list.add("0xc441564582b7f86b00dc22b3d6ca18c37669faa7,2855625.957320040000000000");
        list.add("0xc4627cfbef633c852b034faabd4ab6988b959075,4474028.911085070000000000");
        list.add("0xc4906f8d62fbf7b109da39218850808d0423e736,1328937.912050910000000000");
        list.add("0xc50406db4770d7d8c98db9de97a072cd2207b20b,5000000.000000000000000000");
        list.add("0xc6764b8ae98f0d59d3c853ac6e956025622a7d00,134136.680900558000000000");
        list.add("0xc67d16de07e9d329b73bd007faf87dca7e7d1d17,16180716.053219200000000000");
        list.add("0xc6a0d1c8152509299ef7edf7b0dd81dedd3f089c,3160654.956493580000000000");
        list.add("0xc6c58d510cf90eacd8cd79b52f8a3a7caf343204,5483478.077479530000000000");
        list.add("0xc9401c120b70c8a1f759bbe01b2f3fcbc564d5e7,10457235.810346500000000000");
        list.add("0xc9b22861e6acf0fc2316eb7a7608e8d2be4847da,11000000.000000000000000000");
        list.add("0xc9b93b198d4b97ba9829a52f85eba561f387aeb2,2889359.126592730000000000");
        list.add("0xcb9956ae8f6a02bb77267b928443c25ec0c317c6,586647.321540595000000000");
        list.add("0xcbe67891d8ff389a2173718a4bea89465250db9c,7357278.473484220000000000");
        list.add("0xcbf7823569102fbcb8d363c40ad4d41455f93ca3,2360000.000000000000000000");
        list.add("0xcc4005fd7538f25f6ca395859f5d6c6c0ee9fd58,358713.358723715000000000");
        list.add("0xcc96fcc096b1d157e8aab061bdaf771fc3f6ce00,1125000.000000000000000000");
        list.add("0xccf73497efa861159273b4fe9159f03ded24afd4,1000000.000000000000000000");
        list.add("0xcdd3408c19e218074ebf2b41b17321ca4b24d3e4,3625000.000000000000000000");
        list.add("0xcf9aee3ad11242158a415020ebceaa8cfea5a5c0,9077774.232220410000000000");
        list.add("0xcff2b6f2801bde8d03fd2d825e88448d0188cc15,1097979.476099930000000000");
        list.add("0xd023ae85b36028579f89eb3f31e8db39b2a5fef5,1747364.661166970000000000");
        list.add("0xd03c3b9fd2ea6b95f9d3a93c5a055b6c1e4cc091,375000.000000000000000000");
        list.add("0xd081bd06ebc199012814dc3383fb18d997e224c7,806298.040866140000000000");
        list.add("0xd0e11654e4fcdd76221346d3e483a9f43cac5b8c,89974629.575665500000000000");
        list.add("0xd0ead065e5a79e2647496b239f6667fbf8af3544,63920146.202206500000000000");
        list.add("0xd10e5882ececdd3cb519ab344586833ac8193608,670221.659087489000000000");
        list.add("0xd11616e66b128c0b756b91cc13466defaae67d07,1225659478.622560000000000000");
        list.add("0xd123404c90812f278388616666be667d84befb9e,7500000.000000000000000000");
        list.add("0xd15f971cb555200d6bed4ab8a065dac0ef7a6d2a,3139454.537837310000000000");
        list.add("0xd26a0a35321c6793e47eb602f1676c915caf111c,0.011426606341675300");
        list.add("0xd39a074db3566ced7d68c54afbdbbfc61c140d14,1139969437.405440000000000000");
        list.add("0xd3ed83f2950c303842debd1348ed189e395d4f65,0.765243547958430000");
        list.add("0xd42a58a002d35c083c2da38cdfefbe3e5ada7890,4893429.454649300000000000");
        list.add("0xd460b4bd0141f07527991bcad1c0385a59d4a74e,1204657.759874260000000000");
        list.add("0xd4e65bcd448edb640529e685f6ac4ad824c3bb88,9391071.359529430000000000");
        list.add("0xd5eb9d04c9e92cc9a21530ec825aca0fc88e27b3,365303123.786644000000000000");
        list.add("0xd5f5df396dceaf9b3fe8356594d77c176c1b8e32,0.000629794172156621");
        list.add("0xd6b49bc222a4fa3c36cc9daeff040f92cb7fd344,5000000.000000000000000000");
        list.add("0xd7c031973ee976dc84079ce71f6fb13ff6165af5,216238.389632127000000000");
        list.add("0xd8369d1ee9ae93b5a5f2d927029174ad3cc65e3d,2355867.107485460000000000");
        list.add("0xd8d43dab63bc03b5abd3106a4eb36e2e8c343846,1279991.535558290000000000");
        list.add("0xd9c551f3ad5f9d020edcb88e5b771341b1da4ff4,2693428.846779540000000000");
        list.add("0xda05aae342fe286f7b9cd1c332bb060fb85d130b,32605881.919592000000000000");
        list.add("0xda294e62a32725a192f5fd9af8911966b15e20e1,0.090715075587241200");
        list.add("0xda4156c17fdd9ea0ad617a821b9ac19dbaaf82b1,5339700.000000000000000000");
        list.add("0xdac183120b590eed127051a5dee535e614ac0cbc,954910.185906624000000000");
        list.add("0xdd9f24efc84d93deef3c8745c837ab63e80abd27,30593.720296956200000000");
        list.add("0xde61d0e647a0ea825210b3c27d47eb01bcffd4bc,1300000.000000000000000000");
        list.add("0xde834d48336d605459d97a1d82a2961ec2e59cdc,80000.000000000000000000");
        list.add("0xdf9127ad22902af6a1a2527e2c0d87e910ed6d5f,7250000.000000000000000000");
        list.add("0xdf946674f892df8459442fac07bd5596a7aadd63,1875000.000000000000000000");
        list.add("0xdfa90aaaa03826319df70d3eec051e6c20928b41,46319824.969597900000000000");
        list.add("0xe04f35ce6e75be4742a86491e49009f6173d40e6,127513.000000000000000000");
        list.add("0xe08487309e3105411b2520ee80cc74ca760f7a6f,930079.227266688000000000");
        list.add("0xe092d03e77ba917263f273bba06d7bf7d6e22ebd,650000.000000000000000000");
        list.add("0xe0c27ab52919e801a9179b61efb2a5e871a0e685,295503.587671617000000000");
        list.add("0xe0e242fb606b17e3af135c9709f5ea62c585595f,1776874.000000000000000000");
        list.add("0xe1827d434538752ca08abd2c75d44e3522c772a5,2000.000000000000000000");
        list.add("0xe298b42cebe000826ae7fb655153caac0d6a8711,10.000000000000000000");
        list.add("0xe2ae4c2bc219b8531226d0a6af687e3978f7313a,58127093.323720500000000000");
        list.add("0xe2ae59f83a2062c19f359bb06358f4f289e5ca78,3555599.000000000000000000");
        list.add("0xe3547f396b41c3403faf9cad7b1ee15f986b66e8,3067125.350876580000000000");
        list.add("0xe4e223416defee5f7c0bbaab65b9d681f39c7c78,4983223.670175830000000000");
        list.add("0xe509c61a6857303c43e950f4fac8ca30a8728031,22939166.248827700000000000");
        list.add("0xe72b455a5951ac9fe230591fbd7f7815fce16523,8695027.703566430000000000");
        list.add("0xe748fd1b4a86cae5248538c36daa319817a20255,7719802.552191890000000000");
        list.add("0xe763c71b65b8aa15e502ae709096d03e5f92bd7b,4111111.000000000000000000");
        list.add("0xe8c8d94a7ade37ef500bbfa9bf68ed9cd22dc80e,3011347.000000000000000000");
        list.add("0xe94f5570067d2af1d9c20604639216970dbfe1d8,32439988.333053100000000000");
        list.add("0xe989a2cb496a1672c2b5d95b90e1e3536eb2df0d,3185875.403006820000000000");
        list.add("0xe9b84ae4f3cc21ee6495203327ae49ccc183ac46,15334712.593187700000000000");
        list.add("0xe9fa3452a567ba5f44d5ee83a8501e7e74532232,0.000009006200612141");
        list.add("0xea1c00512038b245eb0cb2e44dbf1151cd9f5ee5,6552262.702842670000000000");
        list.add("0xea6cd5f5a500b81f00020a42d115b8479bf8ace5,7045827.982275390000000000");
        list.add("0xeab4b551d1cd3a509be1eb5093e8915aa98b6bbe,52904366.000000000000000000");
        list.add("0xec48dc30ee66b6030f9cb5656eae5734b9eb5bc7,1657824.361226010000000000");
        list.add("0xedab498397be67407862251073305b38ffbfe537,1115257330.537860000000000000");
        list.add("0xee0232e292ee48203656e18c8b1d9e7fe5ede212,100000.000000000000000000");
        list.add("0xee48cd47e048ac376195e1b63fd1b01695e1aba8,500000.000000000000000000");
        list.add("0xee5894bb440342457eea42e46d6af75bedb14805,4720000.000000000000000000");
        list.add("0xee6fa8a3c1c260bf3b21bdd9e541b49dea716791,1010756.000000000000000000");
        list.add("0xef37713728c8632595168a04705685700f9cc593,1583028.424839090000000000");
        list.add("0xef7992722d675bbf67a1c178cb42cbec7dfc8a4b,0.636196938945988000");
        list.add("0xefded2d3fad4cd27485ea90842c245afa26d6a7a,154450000.000000000000000000");
        list.add("0xf0b6d05280cb913046090e3f2d75bf8d8d0bacb8,1415380.853203980000000000");
        list.add("0xf13b766bb479dc44993c21dccbaaf0e7e4177b9a,16484243.663680500000000000");
        list.add("0xf15ce577f11a3d0ca186fbb8e7f8119408da247c,2297522.288922100000000000");
        list.add("0xf1eb3c363e77949e7318415e834bd5db7391852b,2920530.075619890000000000");
        list.add("0xf21996a30a7b37672429cdb8c917cb410bd6009d,129.275217932615000000");
        list.add("0xf2351a66852b07498fe97ee74860ff7693b84f37,8820892.555675560000000000");
        list.add("0xf23c4d13aeb82e7675c6f0600d135842902917e5,7240000.000000000000000000");
        list.add("0xf2a5807c0978b709c53c531c72cd704e8cbe55fd,20940045.196408300000000000");
        list.add("0xf37cbe64c8e9486a8af90e31818a671a95394b39,577121.295456460000000000");
        list.add("0xf394bb008057448135427febf9f5db054bc3bd09,2351531.833155310000000000");
        list.add("0xf3e36ad56aa85abdacc18c02d19509ae4f7d5899,15453230.568856700000000000");
        list.add("0xf45279e284b62c08f08fd05f24ff698375066a13,2870169.736089540000000000");
        list.add("0xf4bac79bbaf44fa11abbf29643ed4e6da027cac4,5000000.000000000000000000");
        list.add("0xf4c3803514445bf9ed5568fb07a2c9ca43459346,2933173.353891590000000000");
        list.add("0xf4fa5b7cfc11c467420f5b09f3eca6c35f1f242e,9959064.746980010000000000");
        list.add("0xf516652f3dc04b8f5deee1d43355b0f8dcd2f3ef,18501653.682382900000000000");
        list.add("0xf5324c6c7dd4eddb68498fc5eb439b5f4ddcff35,130657.829406926000000000");
        list.add("0xf59be805f5b33eceff3a70043fbdc955be257c9a,17502510.927934200000000000");
        list.add("0xf5b8537c526df134639cb3c11f2b4a0ca07827bb,3051929.473067760000000000");
        list.add("0xf6757ababc9d169a47a297c08b38ef674ecc61fc,1208842.691417030000000000");
        list.add("0xf6e8f5b7fcc21926de78d26a1b74791eda230c54,1000000.000000000000000000");
        list.add("0xf767c24432148fd99d971f9c6d0483fc9c040c03,2803156.730237500000000000");
        list.add("0xf7a44beab8005cd69ca10c75892cb5c877d20761,1104190.837367460000000000");
        list.add("0xf7f073e85ee8f630a67614beed0da537791dd9d8,15085352.844220600000000000");
        list.add("0xf7f54cad9d66045da56ec0277f9e9cf0398f19ec,1875000.000000000000000000");
        list.add("0xf826c15040dc601c4f2bf8d18a1217524f0101cd,1875000.000000000000000000");
        list.add("0xf873b6f72d243c73596c6b615411452174c94f5b,70000.000000000000000000");
        list.add("0xf973f61b09adbeefb8061a2359ec9339f3539f0a,1.000000000000000000");
        list.add("0xf99e928d8223aab8c97a3c1e84c678dbc0e3249e,1230339.229051900000000000");
        list.add("0xf9e4113f40fba9c3503dbb62326f405142b97ff5,30990594.000000000000000000");
        list.add("0xfb26f3fae0839f0634c4b2b45089a491d4c11284,22983962.728900300000000000");
        list.add("0xfbe1095150e751463ed595604c93c51ed9a89b8a,4227645.890175410000000000");
        list.add("0xfcda02694ea9393f08f7f7317e7e9c599a15af06,395059.846668549000000000");
        list.add("0xfdd294cd1df24f3c1d0fe8d0d83fa729f9d26c8b,6138202.319628140000000000");
        list.add("0xfe64b05b71d5458ef747f10e72fa262537c48954,535594.774785584000000000");
        list.add("0xfebdad50a50610a2e83bb8566e326c1f69eb4783,348065.568596016000000000");
        list.add("0xfee1d3b4f47fd804472c4e4acafd6a7245d45104,3750000.000000000000000000");
        list.add("0xfef56500fdac1035267bc210c689a75ca46cccdd,1235531.576034360000000000");
        list.add("0xfff317cadd10dac62c1699d4dc858c0aa62e71d9,581162.634941610000000000");
        list.add("0xc3254c3130dd25a744eb4ab8e14bfe108142efed,500000000");


        StringBuilder addrBider = new StringBuilder("[");
        StringBuilder amountBider = new StringBuilder("[");
        BigInteger calcTotal = BigInteger.ZERO;
        for (String str : list) {
            String[] split = str.split(",");
            String address = split[0].trim();
            BigInteger amount = new BigDecimal(split[1].trim()).movePointRight(18).toBigInteger();
            calcTotal = calcTotal.add(amount);
            addrBider.append("\"").append(address).append("\",");
            amountBider.append("\"").append(amount.toString()).append("\",");
        }
        addrBider.deleteCharAt(addrBider.length() - 1);
        amountBider.deleteCharAt(amountBider.length() - 1);
        addrBider.append("]");
        amountBider.append("]");
        System.out.println(calcTotal);
        System.out.println(addrBider.toString());
        System.out.println(amountBider.toString());
    }
}
