package io.nuls.api.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.model.dto.HeterogeneousAssetCollectionDTO;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.core.basic.Result;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 15:25
 * @Description: 功能描述
 */
public class SymbolRegServiceTest extends BaseTestCase {

    MongoDBService mongoDBService;

    SymbolRegService symbolRegService;
    AccountService accountService;

    @Before
    public void before() {
        super.before();
        accountService = ServiceManager.get(AccountService.class);
    }
//
////        String dbName = "nuls-api";
////        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
////        MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);
////        MongoDBService mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
////        SpringLiteContext.putBean("dbService", mongoDBService);
//
////        ApiContext.databaseUrl = "127.0.0.1";
////        ApiContext.databasePort = 27018;
////        SpringLiteContext.init("io.nuls");
//        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
//        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "admin", "password".toCharArray());
//        MongoClient mongoClient = new MongoClient(serverAddress,credential, MongoClientOptions.builder().build());
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
//        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        symbolRegService = new MongoSymbolRegServiceImpl();
//        ((MongoSymbolRegServiceImpl) symbolRegService).setMongoDBService(mongoDBService);
//    }

    @Test
    public void testSave() {
        SymbolRegInfo info = new SymbolRegInfo();
        info.setChainId(1);
        info.setAssetId(2);
        info.setSymbol("BTC");
        info.setContractAddress("abc");
        symbolRegService.save(info);
        info = symbolRegService.get(1, 1);
        Log.info("{}", info);
        List<SymbolRegInfo> list = symbolRegService.getAll();
        list.forEach(d -> {
            Log.info("{}", d);
        });
    }


    @Test
    public void testQueryBaseSymobl() {
        Result<List<HeterogeneousAssetCollectionDTO>> res = WalletRpcHandler.getAllHeterogeneousChainAssetList();
        res.getData().forEach(dto -> {
            Log.info("chainId:{}", dto.getChainId());
            dto.getAssetList().forEach(asset -> {
                Log.info("=====assset:{}", asset);
            });
        });
    }

    @Test
    public void testMutiAccount() {

//        GenerateMultiSignAccountReq req = new GenerateMultiSignAccountReq();
//        List<String> mlist = new ArrayList<>(otherPubKey);
//        mlist.add(pubKey);
//        req.setPubKeys(mlist);
//        req.setMinSigns(3);
//        io.nuls.base.api.provider.Result<String> result = accountService.createMultiSignAccount(req);
//        String mk = result.getData();
//        Log.info("{}:{}", result.getData(), entry.getValue());
    }

    public static void main(String[] args) throws NulsException, JsonProcessingException {
        List<String> linPubKey = new ArrayList<>();
        linPubKey.add("0344054bbda45aca3883d5f1e0f99808786aee6ea9cc061508a3d8145f3a7d1cb4");
        linPubKey.add("03924acd39414c87a338489fbc60c7567139e51bc77254d83cfe94dfa43f5d8d46");
        linPubKey.add("03f6ebfcba532f77c90d0194a09987ab8ede88d99a09783f39179dfef480f6ff85");
        linPubKey.add("03ea4cd5e58be3f048dfafe491d21ecd8ff021e3831be3e97a363ed3b992c82500");
        linPubKey.add("032658202428ece5bdfcda51363f031711dd2baa962ed3e94191a6e54b9f9b5915");
        linPubKey.add("0343aeb8889a4baf724af0153675f952e58d89f4669c7b5ed9b56d96830bb1bc98");
        linPubKey.add("039d4fa5c52510d978e0cc3502236d87ee97188c4af41b5e55fe29c13437da7745");
        linPubKey.add("03408cf4be157233867c0338bc9f84da0605725c990ce6e7facee6c69778df9aea");
        linPubKey.add("03b43537628d211f00b4c93d3db1e23079e41bd8406e86b14292af5c5b886e05cb");
        linPubKey.add("03de80653c218a69b3f89d41f82797d6847afb3c217944edad21199ee6a3f831de");
        linPubKey.add("02e06c7a398ef853cea0b91af22d99e3e3aa4cb864f7ef5a94cb2aa2ae2d0d217e");
        linPubKey.add("030431c58c879bef47f6fb5a410c8fde675e11ced2fb26cd6b84cdfa7d184e0ee4");
        linPubKey.add("0302b2e8ff18d2561f21a40fa3427d7a7fdb0fcc90f241eb1a18ca6acac5a868ff");
        linPubKey.add("03c173ea8f8dfa6093c5f592ba2677f8093730678f42db364db139cc026990f934");
        linPubKey.add("034435347b9e4c569f80ce40b4347064c317246e0d1e2e61b4959c5d5492cd4e57");
        linPubKey.add("0219406697419175d5a2f240b5a31384c556ce42045dc45b7847c07afcc4e5c92a");
        linPubKey.add("033eac0c328b83c5dd65268192358a3dcbf8b83714b7b11d8bfcb46b31b2671bd6");
        linPubKey.add("03b6ea1e63bb07951eefd333f2f85a5cb9552b5cf4f54aa1276218406ab8cd73c3");
        linPubKey.add("02a95959898a079195ac702beb9bd054f045d5bd419c5d2017fff5193dc6c21d8e");
        linPubKey.add("037c21c58417af51fb5d8e9b5a7af1b08bccb7d900d11e2c1585bcf30ae07a6ea0");
        linPubKey.add("03fb72644bb6efba8419b8302d41199f4dfc92278280a0934b4644646303b0d8e7");
        linPubKey.add("03e6de8b7f5c6ee2356413918d2a99bd1d9c850d10e9675b0e4c861a52f252f23e");
        linPubKey.add("02d5d56a1e5c478cd58890e187f878d2479a24d14f11b1001b851bd1fe935ceba7");
        linPubKey.add("031d94504ec05d2ba0f6b9b56dae0d629eead3622631d64fc23da86be87cb4695a");
        linPubKey.add("02ab09157f648a80c47aa934a243b96a4d55fd4111fa75053bdbd39dac19023868");
        linPubKey.add("03d59c3182cbaf53c65284806482ac8929345752a9397e2fd396cc9c8d26dba26a");
        linPubKey.add("02b83f315fc8d872edcbbc8494ac48ff568a084d4219b2cf51423ca6a6f9f45078");
        linPubKey.add("0263d8638edb15e70b3e76a2e4f1903e9e63c6d7af00efe745ed8aaeca58767779");
        linPubKey.add("03b724dea2a201587d6fd6cd55b8b456c0f729e1153ae7d37b36f53edb93e2f862");
        linPubKey.add("039663f6af032d05ca305fdad284b5b9c7b93ab59e0edbc9b723e3e2b643379c54");
        linPubKey.add("030278af7cb7f3fbdc331a663cf9eeee4e2b750da4659d7cbfdf5d1fa552732bab");
        linPubKey.add("03e171420cb9f6ce94fce33971b493e7808e83b7b6b848f87a875c71c1ee47641c");
        linPubKey.add("035ed397949bca80d224b0c17c8f24b6d22ca1dbc7b9a5d7795de427486567026e");
        linPubKey.add("0298b83ffb30fbe6358c9e54280cdf08b5de66e0adff770ff56c02e0ea2589b16b");
        linPubKey.add("03232081e2e9c3a0da45640863f0bfa9248a7b4dc511a93c4b0a23dcc6e1309bde");
        linPubKey.add("03c7b6fcca944d180cdaddc992ccb71c7f3b916a0db1fae863bbd599025ad195b0");
        linPubKey.add("026e99ff832bd38f07d2b22e222f01f1b55cc0f9496008707e8c3fb69f6d368286");
        linPubKey.add("02edca9389700f168d11734cf358147de50203b43c8b6e42d61eebff483111ad99");
        linPubKey.add("034d6dc78236becbfdc840748e7a12c24385ab654971d0915d2c0dfa9688d56c53");
        linPubKey.add("039a559e42e21fb5f6ab529b11d1ee5835c438d501de24fd4e1d88dbc5d8ee53ec");
        String pubKey = linPubKey.get(16 - 1);
        List<String> otherPubKey = List.of("046ff585fca5282eb9c7479967890bb6cc5ff6ea2bce667283ce97a21227be62", "03b736b7ac6cd2cfe8c221926f8fc0e2b84f8cb96c079c800be09f682d8805097e", "03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4", "021226ecc609a1a054b760d30a8e1715524ddf22274b152a7f8a98b9b7da81d833");
        Log.info("createmultisignaccount {} 3",pubKey + otherPubKey.stream().reduce("",(v1,v2)->v1 + "," + v2));

    }

    @Test
    public void testGetAssetInfo() {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, 4);
        params.put("txHash", "df9606dac0bbe4093846ed831495aadb73ad5734ef3a8428e57e66945c5ecd13");
        try {
            Map map = (Map) RpcCallUtil.request(ModuleE.LG.abbr, "getAssetRegInfoByHash", params);
            Log.info("{}",map);
        } catch (Exception e) {
            Log.error(e);
        }
        ECKey key = new ECKey();

    }


}
