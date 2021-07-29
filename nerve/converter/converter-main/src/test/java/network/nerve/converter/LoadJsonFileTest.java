package network.nerve.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.base64.Base64Decoder;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.v2.SDKContext;
import io.nuls.v2.model.dto.RestFulResult;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.util.HttpClientUtil;
import io.nuls.v2.util.JsonRpcUtil;
import io.nuls.v2.util.RestFulUtil;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.heterogeneouschain.eth.model.EthERC20Po;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.txdata.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.config.ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ETH_ERC20_STANDARD_FILE;


public class LoadJsonFileTest {

    @BeforeClass
    public static void beforeClass() {
        ObjectMapper objectMapper = JSONUtils.getInstance();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void erc20FileTest() throws IOException {
        String json = null;
        try {
            json = IoUtils.read(ETH_ERC20_STANDARD_FILE);
        } catch (Exception e) {
            // skip it
            Log.error("init ERC20Standard error.", e);
        }
        if (json == null) {
            return;
        }
        List<EthERC20Po> ethERC20Pos = JSONUtils.json2list(json, EthERC20Po.class);
        System.out.println(ethERC20Pos.size());
    }

    @Test
    public void heterogeneousFiletest() throws Exception {
        String configJson = IoUtils.read(ConverterConstant.HETEROGENEOUS_TESTNET_CONFIG);
        List<HeterogeneousCfg> list = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
        System.out.println(list.size());
    }

    @Test
    public void desTransaction() throws NulsException, JsonProcessingException {
        //LATEST_BLOCK_HEIGHT = -1;
        //WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = -1;
        //String hex = "02006ae9526002313117008f404078e5e3a7de8502dc4544805bc0eebccc6e1001d00117050001d5e0c14c896ccbcf3709ff73b8a91b78638502e905000100402bb43500000000000000000000000000000000000000000000000000000000080d4eb0880e33a1a600021705000129cfc6376255a78451eeb4b129ed8eacffa2feef0500010000a3e111000000000000000000000000000000000000000000000000000000000000000000000000170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e050001004088d2230000000000000000000000000000000000000000000000000000000000000000000000006921022fb21df00d78dd85d4700a10da7021b3a84d2d9f5998f3eb3ac0e9e76d98246c46304402206d249b02919ac1d3cdd66f144a374b5a27fba8cd0f471b5ba1d7136108ee61f1022078cf90d32cc7cde28a4bf6d27035acd786e5ca0a7908e200fbe375894a325dd6";
        //Transaction tx = new Transaction();
        //tx.parse(HexUtil.decode(hex), 0);
        //System.out.println(tx.format(WithdrawalTxData.class));
        //System.out.println(HexUtil.encode(tx.getTxData()));
        //
        //WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData();
        //txData.parse(tx.getTxData(), 0);
        //System.out.println();
        //System.out.println(tx.format(WithdrawalTxData.class));

        // 0xC6e431fDe25CcBA2520491e29AD5B2e3134d7989
        WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = 2;
        String txDataHex = "4032303131353166623461616362306666653931386432386330643361333161373638363734346463356334626665656563353565346465626463303863656531423078303062336132626631333265633563366563616432316463653835366634393462343464356336316539303335323063366333613139363566343363653134316700";
        WithdrawalHeterogeneousSendTxData txData1 = new WithdrawalHeterogeneousSendTxData();
        txData1.parse(HexUtil.decode(txDataHex), 0);
        System.out.println(JSONUtils.obj2PrettyJson(txData1));
        System.out.println();
        //
        txDataHex = "67009f4738000000000042307864653236633433393432633466326162653263333536343435353831376333636365306161383631303933346535366161393335323432396433626633376565eb0f03d729c100e3f5ff6ed0b65c0ccdcdb75bdcce94dd9741927753abfe394e010067002a307864363934363033393531396263636330623330326638393439336265633630663466306234363130";
        ConfirmWithdrawalTxData txData2 = new ConfirmWithdrawalTxData();
        txData2.parse(HexUtil.decode(txDataHex), 0);
        System.out.println(txData2.toString());
        System.out.println();
    }

    @Test
    public void test() throws NulsException {
        SDKContext.wallet_url = "";
        List<String> hashes = new ArrayList<>();
        hashes.add("b4454af43a91962ed1b1b4794da82bce015a96cfb4b454e967e5fd45794efe9a");
        hashes.add("af91318ae16f45ca110a93a5351adebe143cbff92972061be0dcae3d0df378e5");
        hashes.add("6dfeab103bab8e92e7286374712d11ad4555db877e935caf7d0068f8a9d67c8f");

        hashes.add("1b4df690f7562f079bef2eeca6f334f633f0d61e03dc23536183170580bccb4b");
        hashes.add("b24043d8490c345a3524558313b0456d64cf780657a46a7d13107cc8a91cc81e");
        hashes.add("960461894934914bacb9050d8456fe08250c02a548b8a89b3c4deeddf2251ec9");
        hashes.add("c89c52b4b660f47b5fa9f79689be29b1ee472967ed71b6a8e211d253c05664bc");
        hashes.add("b216ab48e0d73b38a7377dbfa561434cb45ff1d077fe0a0f38330986a8a34592");
        hashes.add("9da38ed5c144a586dafff55c96a44268430a08206b25bc3c4e2fd5ff2e6b511f");


        hashes.add("ce5a9d3533c1afe2a7cef23206bb94017574af305f1b56287f7f83605ccb7b54");
        hashes.add("ff71a72df4607e5d44e3f97da79f96bf302ef316296d0a5c3e076515f0239fdc");
        hashes.add("8c793038dbbbdb2d67a9e0d1665313124ab7b1486e892839b26c02e700bf6235");
        hashes.add("8065097e07f94dbec7921242709abe66fff116c836bb47c773392aafc0790eff");
        hashes.add("243a45fc15957b12690b837e6991d6b4ab1282987f4f41f67150a475b52cf1c0");
        hashes.add("31af0529285d16df09815173c81a409348caa9b451b8c0d29b85679f5919ca15");


        for (String hash : hashes) {
            RestFulResult<Map<String, Object>> result = RestFulUtil.get(String.format("https://api.nerve.network/api/tx/%s", hash));
            Map<String, Object> dataMap = result.getData();
            String txDataHex = dataMap.get("txDataHex").toString();
            ConfirmWithdrawalTxData txData2 = new ConfirmWithdrawalTxData();
            txData2.parse(HexUtil.decode(txDataHex), 0);
            if (txData2.getHeterogeneousChainId() != 101) {
                System.out.println("不是 Ethereum 交易.");
                continue;
            }
            System.out.println(hash + ":\n " + txData2.toString());
            System.out.println("-------\n");
        }

    }

    @Test
    public void test1() throws NulsException {
        SDKContext.wallet_url = "";
        List<String> hashes = new ArrayList<>();
        Map<String, String> hashTime = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int basePageSize = 3;
        for (int i=0;i<6;i++,basePageSize++) {
            RpcResult rpcResult = JsonRpcUtil.request("https://scan.nerve.network/api/", "getTxList", List.of(9, basePageSize, 50, 55, false));
            Map result = (Map) rpcResult.getResult();
            List<Map> list = (List<Map>) result.get("list");
            for (Map dataMap : list) {
                long time = Long.valueOf(dataMap.get("createTime").toString() + "000");
                int format = Integer.parseInt(sdf.format(new Date(time)));
                String format1 = sdf1.format(new Date(time));
                if (format != 20210409) {
                    System.out.println(String.format("时间不在范围: %s", format1));
                    continue;
                }
                String _hash = dataMap.get("hash").toString();
                hashes.add(_hash);
                hashTime.put(_hash, format1);

            }
        }

        for (String hash : hashes) {
            RestFulResult<Map<String, Object>> result = RestFulUtil.get(String.format("https://api.nerve.network/api/tx/%s", hash));
            Map<String, Object> dataMap = result.getData();
            String txDataHex = dataMap.get("txDataHex").toString();
            //long time = Long.valueOf(dataMap.get("timestamp").toString() + "000");
            WithdrawalHeterogeneousSendTxData txData2 = new WithdrawalHeterogeneousSendTxData();
            txData2.parse(HexUtil.decode(txDataHex), 0);
            if (txData2.getHeterogeneousChainId() != 101) {
                System.out.println("不是 Ethereum 交易.");
                continue;
            }
            System.out.println(String.format("hash: %s, time: %s, data:\n %s", hash, hashTime.get(hash), txData2.toString()));
            System.out.println("-------\n");
        }

    }

    @Test
    public void desTxData() throws Exception {
        //LATEST_BLOCK_HEIGHT = -1;
        WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = -1;
        String hex = "6700423078303064666539353338613363653432383335393839313661373138363635653661663166393236373135653564663734333638306330303037636133343832318d233500000000002a3078656266366361663631646166353364633932623661396230333063663634326664346331313033630900014155b8703d76325f79c5c2c6ee40b50909edd1ff09000100a1e850891d000000000000000000000000000000000000000000000000000000";
        //
        RechargeUnconfirmedTxData txData = new RechargeUnconfirmedTxData();
        txData.parse(HexUtil.decode(hex), 0);
        System.out.println(txData.toString());
        System.out.println(JSONUtils.obj2PrettyJson(txData));

        //WithdrawalTxData txData = new WithdrawalTxData();
        //txData.parse(HexUtil.decode(hex), 0);
        //System.out.println();
        //System.out.println(JSONUtils.obj2PrettyJson(txData));
        //System.out.println(new String(HexUtil.decode("307831463035313841314631313139356534353138463136313539343846643837423466343333454531"), "utf8"));

        // 0xC6e431fDe25CcBA2520491e29AD5B2e3134d7989
        //WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = 2;
        //String txDataHex = "4065323865323161613634663861303933366264316238346664326531656339376361646437396562663131333539616136643761616463643565353230316232423078636438653363386364363935636662613762343066626536303866613336663538613938643838356365393333363663313436363536333437396139383439616600";
        //WithdrawalHeterogeneousSendTxData txData1 = new WithdrawalHeterogeneousSendTxData();
        //txData1.parse(HexUtil.decode(txDataHex), 0);
        //System.out.println(JSONUtils.obj2PrettyJson(txData1));
        //System.out.println();
        //
        //txDataHex = "6500a561ad000000000042307863646436333131333935326331396466636336613866323132356539333030363461363066643666373837663961353162366439393530666132333062326363885c43f08c06d26789173410803dffcce7770ea6edcdcb8b8ae12e155255aece010065002a307862313261363731363632343433313733306333656635356638306334353833373139353466613532";
        //ConfirmWithdrawalTxData txData2 = new ConfirmWithdrawalTxData();
        //txData2.parse(HexUtil.decode(txDataHex), 0);
        //System.out.println(JSONUtils.obj2PrettyJson(txData2));
        //System.out.println();
    }

    @Test
    public void testSign() throws NulsException {
        String sign = "2103d77af848c812639fe177158083c1df98b7b70c9c0f8023742b0890edaed6c778483046022100d1eb71d7b85ae0ea55b87fa6d2c6ad1d19f98c90773b1980f1607914d1b8c05b022100d9cda63aa266006dac80fdc4e8d753a00f9888bd62ba4353fd9d210378e01e91";
        TransactionSignature s = new TransactionSignature();
        s.parse(HexUtil.decode(sign), 0);

        System.out.println("NERVEe" + AddressTool.getStringAddressNoPrefix(AddressTool.getAddress(s.getP2PHKSignatures().get(0).getPublicKey(), 9)));
        System.out.println("TNVTd" + AddressTool.getStringAddressNoPrefix(AddressTool.getAddress(s.getP2PHKSignatures().get(0).getPublicKey(), 5)));

    }

    @Test
    public void parseJson() throws IOException {
        String json = "[{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAH4jZOlLtx0N9peKXfSdoHw+x5rqwIAAQAAlDV3AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAXBQAB+I2TpS7cdDfaXil30naB8Psea6sFAAEAoCnjEQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wIAAQAAlDV3AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAACj4REAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4ZjdGM0ExM2I1ZDQxMzlkNTFBN0ZmOTQzN0FmM2VFMkYxOUU3ZjYyNGUA\",\"time\":1611042256,\"transactionSignature\":\"IQJf/DMD/fDkMrRsN6nBjl5/7vANaP73ACk5nBfco08RfUcwRQIhAPMqHMNusO5Z4E/abvykR/B92lqU7Ox+/+IV90ay4L/TAiBX0njBTbhn3h/x7tj0kL0ctJR3HUUW+cJ3uLGsX45pjQ==\",\"remark\":null,\"hash\":{\"bytes\":\"oqZqcfv0nmRew+4w5zITjB289KV8TVZ8YtSmAk5bL/M=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":441,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB+I2TpS7cdDfaXil30naB8Psea6s=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":2000000000,\"nonce\":\"AAAAAAAAAAA=\",\"locked\":0},{\"address\":\"BQAB+I2TpS7cdDfaXil30naB8Psea6s=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":300100000,\"nonce\":\"AAAAAAAAAAA=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":2000000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":300000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPVf5iJ4f42B48B95kY5rWzSUAcbv19\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"CrBJNJTWr7B+QNmi0iJ3XAhzfcJj89qruomLsr4h1mk=\",\"blank\":false},\"preHash\":{\"bytes\":\"iY93eSrWZE8lDaH40o4CFeXKPegOww+FMXoftWLo01I=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"bBwULpkeDdskuC8j2yaauddYZF6CcSwiWj1z8uduLPE=\",\"blank\":false},\"time\":1611042300,\"height\":7163,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEUCIQDEr3T58gdcXQcg/It0Z4SSX6gU/noWNqppVc/82p9r+AIgKt1oe9rCVpnkcnuAryPNCEgq2BXRdhfeh9mIWPhX9IA=\"},\"publicKey\":\"A+ICnd+MAVDYpolGUiPNypSgyEzbWB45rBPKQdJ5wk/1\"},\"extend\":\"sdhRAAQA9o0GYAQAAQABADxkAAA=\",\"extendsData\":{\"roundIndex\":5363889,\"consensusMemberCount\":4,\"roundStartTime\":1611042294,\"packingIndexOfRound\":4,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":null,\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false},{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAH4jZOlLtx0N9peKXfSdoHw+x5rqwIAAQAANxeJAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAjUP8CwfFQN5QAXBQAB+I2TpS7cdDfaXil30naB8Psea6sFAAEAoCnjEQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI1D/AsHxUDeUAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wIAAQAANxeJAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAACj4REAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4ZjdGM0ExM2I1ZDQxMzlkNTFBN0ZmOTQzN0FmM2VFMkYxOUU3ZjYyNGUA\",\"time\":1611045331,\"transactionSignature\":\"IQJf/DMD/fDkMrRsN6nBjl5/7vANaP73ACk5nBfco08RfUcwRQIgEbHeffOSBO0NbdWL9y7aonZVoU/lab63pV7FjZfRBE8CIQDXQEnoXq12BHkw/ZnxPHm915iLi1G1KhEbDRaZu5Q2xw==\",\"remark\":null,\"hash\":{\"bytes\":\"9mMQUunSIpnxKdSflmoNXxsD56E2lafWSLMXXmLJYuM=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":441,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB+I2TpS7cdDfaXil30naB8Psea6s=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":2300000000,\"nonce\":\"1D/AsHxUDeU=\",\"locked\":0},{\"address\":\"BQAB+I2TpS7cdDfaXil30naB8Psea6s=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":300100000,\"nonce\":\"1D/AsHxUDeU=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":2,\"assetsId\":1,\"amount\":2300000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":300000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPVf5iJ4f42B48B95kY5rWzSUAcbv19\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"HNPz80QEDNCtrGbcJNB0BHH9HDylcXCTGozAGaFc4s0=\",\"blank\":false},\"preHash\":{\"bytes\":\"Is5tU0D9THufMg5Bn/WUjz9ph0LtHeKojfyE3GDd5Qc=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"IhaTIi/RBRHTOsdSLdm0/7XiCel6HY1jED14OQe80RM=\",\"blank\":false},\"time\":1611045361,\"height\":8600,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEUCIQCl4RHiovhz04h/n9r/9g3Q1g7WAu3qSef8tQ9zCNMjdwIgOQ/eym21bD7Yw8YfIvIWo5bE3Cxr+P0UfihIjTABrZQ=\"},\"publicKey\":\"Awh4Tj1K/2iiSWSWiHeznSJElZbBx4kTak4l4tt4GYJg\"},\"extend\":\"LNpRAAQA65kGYAQAAQABADxkAAA=\",\"extendsData\":{\"roundIndex\":5364268,\"consensusMemberCount\":4,\"roundStartTime\":1611045355,\"packingIndexOfRound\":4,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":null,\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false},{\"tx\":{\"type\":43,\"coinData\":\"ARcFAAHV4MFMiWzLzzcJ/3O4qRt4Y4UC6QUAAQCgd1VlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAACFwUAASnPxjdiVaeEUe60sSntjqz/ov7vBQABAADKmjsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXBQABjsTPPuFgsFTgq7b1yBd7nuVvpR4FAAEAACe5KQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"txData\":\"KjB4OGY0MDQwNzhFNUUzQTdkZTg1MDJkYzQ1NDQ4MDViQzBlRWJjY2M2RWUA\",\"time\":1611125087,\"transactionSignature\":\"IQIvsh3wDXjdhdRwChDacCGzqE0tn1mY8+s6wOnnbZgkbEcwRQIhAN64l4jwIE/JRGh85u7naOM0RRkZFY9K8nPzLMcSu3UgAiBeoUYnKUrTTpRXKJv0hUzlMEnDMKTrLs1jwGH9TVFwTQ==\",\"remark\":null,\"hash\":{\"bytes\":\"z4JPinhPBj5J//I9wIXwm5w0OK0IhOrzWp1k5CLjs4w=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":369,\"coinDataInstance\":{\"from\":[{\"address\":\"BQAB1eDBTIlsy883Cf9zuKkbeGOFAuk=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":1700100000,\"nonce\":\"AAAAAAAAAAA=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":1000000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":700000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPTXQudD2FBSefpQRkXTyhhtSjyEVAF\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"PhbjFoAUXkeEFsCkE+gYyPXqWyHNdQea2Rsp8wbQ3so=\",\"blank\":false},\"preHash\":{\"bytes\":\"D5etv0dr9c0mPFIDzNfXIXKmVm3b29ts4m/n+p1r1dQ=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"rKr5RkcUXDW2USkAtchHse2KsQKEL9e3iP+XxPTRjjk=\",\"blank\":false},\"time\":1611125089,\"height\":48464,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIGLKUQKs+zOws19rZsGjvPYKWLbPLHy1IE+FCaZjRkr0AiB/Bs3JOHqMcTQlRGZk/Yz7SGF3QlF5sAe5Lrcukq8cFg==\"},\"publicKey\":\"AyAL2onkEWOSqluTnXOebJNYYAwPjBeQ3U8oRZGyhd5w\"},\"extend\":\"GgFSAAQAW9EHYAQAAQABADxkAAA=\",\"extendsData\":{\"roundIndex\":5374234,\"consensusMemberCount\":4,\"roundStartTime\":1611125083,\"packingIndexOfRound\":4,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":null,\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false},{\"tx\":{\"type\":43,\"coinData\":\"AhcFAAEXpNBrOInuNgQEA7ss+kPe1xloTwUAAgAAAIpdeEVjAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAXBQABF6TQaziJ7jYEBAO7LPpD3tcZaE8FAAEAoPN9TQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIIJ+gfsGwwWkAAhcFAAEpz8Y3YlWnhFHutLEp7Y6s/6L+7wUAAgAAAIpdeEVjAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFwUAAY7Ezz7hYLBU4Ku29cgXe57lb6UeBQABAABtfE0AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\",\"txData\":\"KjB4QWEyRTFmMEM5NDE5Q2IzOTI3NzdDRTZCQzNkZjEwOUZGZGJBZTgwN2UA\",\"time\":1611210166,\"transactionSignature\":\"IQIvR9YmWfh9+OdMp9OsLGeozFOJfDNVFKHYMtXWCmN650gwRgIhAOifDplKXkdgAbC0lOdZ3pq3ZTf0YQOyL0QySu7m0pUWAiEA+pqP4fRPBIamjHuCo1jAsVa1uAZTJVo9ByB8JMwXh8o=\",\"remark\":null,\"hash\":{\"bytes\":\"Pn9hbi+PaUs68bPfLNo3x4Nng0m6paqcCUM8x4XqXJo=\",\"blank\":false},\"blockHeight\":-1,\"status\":\"UNCONFIRM\",\"size\":442,\"coinDataInstance\":{\"from\":[{\"address\":\"BQABF6TQaziJ7jYEBAO7LPpD3tcZaE8=\",\"assetsChainId\":5,\"assetsId\":2,\"amount\":100000000000000000,\"nonce\":\"AAAAAAAAAAA=\",\"locked\":0},{\"address\":\"BQABF6TQaziJ7jYEBAO7LPpD3tcZaE8=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":1300100000,\"nonce\":\"IJ+gfsGwwWk=\",\"locked\":0}],\"to\":[{\"address\":\"BQABKc/GN2JVp4RR7rSxKe2OrP+i/u8=\",\"assetsChainId\":5,\"assetsId\":2,\"amount\":100000000000000000,\"lockTime\":0},{\"address\":\"BQABjsTPPuFgsFTgq7b1yBd7nuVvpR4=\",\"assetsChainId\":5,\"assetsId\":1,\"amount\":1300000000,\"lockTime\":0}],\"fromAddressList\":[\"TNVTdTSPFpwXiHkkPNANBwEwUw4KLTGzVTJbx\"],\"fromAddressCount\":1},\"inBlockIndex\":0,\"fee\":100000,\"multiSignTx\":false},\"listInDirector\":[],\"listOutDirector\":[],\"isConfirmedVerifyCount\":0,\"blockHeader\":{\"hash\":{\"bytes\":\"0PYNpjCYj7sPgvS5J5xYorgxgf7dkrOP6ttshcZzF5A=\",\"blank\":false},\"preHash\":{\"bytes\":\"3IKcMr3MDbkwER89LTl+J6UgkJ/+SE8B5HFCeR9h7KI=\",\"blank\":false},\"merkleHash\":{\"bytes\":\"sapYdg148aOGo9cIRalj9+V8SgGuAKraMFZKQ1sWwVQ=\",\"blank\":false},\"time\":1611210167,\"height\":91003,\"txCount\":2,\"blockSignature\":{\"signData\":{\"signBytes\":\"MEQCIFtUrftvGvLBSXTvwUl9b2uTmwb8DaqMO+XO1b/ym9MsAiAoNrgJMCNrYGIL41PLLOlzOF/7gCxVnffStMK5YTrLgQ==\"},\"publicKey\":\"AyAL2onkEWOSqluTnXOebJNYYAwPjBeQ3U8oRZGyhd5w\"},\"extend\":\"pSpSAAQAsx0JYAMAAQABADxkAAA=\",\"extendsData\":{\"roundIndex\":5384869,\"consensusMemberCount\":4,\"roundStartTime\":1611210163,\"packingIndexOfRound\":3,\"mainVersion\":1,\"blockVersion\":1,\"effectiveRatio\":60,\"continuousIntervalCount\":100,\"stateRoot\":null,\"seed\":null,\"nextSeedHash\":null},\"stateRoot\":null},\"syncStatusEnum\":\"RUNNING\",\"currentDirector\":true,\"currentJoin\":false,\"currentQuit\":false,\"currentQuitDirector\":null,\"currenVirtualBankTotal\":3,\"retry\":false}]";
        List<HashMap> list = JSONUtils.json2list(json, HashMap.class);
        System.out.println(list.size());
        for (Map map : list) {
            String hashBase64 = (String) ((Map) ((Map) map.get("tx")).get("hash")).get("bytes");
            System.out.println(HexUtil.encode(Base64.getDecoder().decode(hashBase64)));
        }
    }
}