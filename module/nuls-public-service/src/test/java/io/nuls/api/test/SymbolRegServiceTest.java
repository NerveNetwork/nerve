package io.nuls.api.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.constant.config.SymbolPriceProviderSourceConfig;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.db.mongo.MongoSymbolRegServiceImpl;
import io.nuls.api.model.dto.HeterogeneousAssetCollectionDTO;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.base.api.provider.account.facade.GenerateMultiSignAccountReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

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
        Map<String,String> linPubKey = new HashMap<>();
        linPubKey.put("0344054bbda45aca3883d5f1e0f99808786aee6ea9cc061508a3d8145f3a7d1cb4","NULSd6HhCmoEttGb3TiP7DngpUGC9hJWacfdm");
        linPubKey.put("03924acd39414c87a338489fbc60c7567139e51bc77254d83cfe94dfa43f5d8d46","NULSd6Hh7NySC3JKFj4bZnDcg2auVZaeTnvwS");
        linPubKey.put("03f6ebfcba532f77c90d0194a09987ab8ede88d99a09783f39179dfef480f6ff85","NULSd6HhFkmQRcuKgz4WYxGNoaAiPvGajzXnx");
        linPubKey.put("03ea4cd5e58be3f048dfafe491d21ecd8ff021e3831be3e97a363ed3b992c82500","NULSd6HhBTkh7W9QWcghRbZgS7oHXnWiMabNB");
        linPubKey.put("032658202428ece5bdfcda51363f031711dd2baa962ed3e94191a6e54b9f9b5915","NULSd6HhA3cJftK4YZhRGM4DfsgZhUkFKkF4j");
        linPubKey.put("0343aeb8889a4baf724af0153675f952e58d89f4669c7b5ed9b56d96830bb1bc98","NULSd6HhFz49edZFa72LyE7zTDMUdz7HqNfRA");
        linPubKey.put("039d4fa5c52510d978e0cc3502236d87ee97188c4af41b5e55fe29c13437da7745","NULSd6HhDvFitV83wVT49g6xXnWBwAT1urxBf");
        linPubKey.put("03408cf4be157233867c0338bc9f84da0605725c990ce6e7facee6c69778df9aea","NULSd6Hh39s82LAL1et7zDSSgWRCFtVvGeLyY");
        linPubKey.put("03b43537628d211f00b4c93d3db1e23079e41bd8406e86b14292af5c5b886e05cb","NULSd6Hh2HoiqNEuGn3X6d6BUMFuoBFiHtyCg");
        linPubKey.put("03de80653c218a69b3f89d41f82797d6847afb3c217944edad21199ee6a3f831de","NULSd6Hh9XJBDY1oxHp5Egq4fmVjGLaSo1EuM");
        linPubKey.put("02e06c7a398ef853cea0b91af22d99e3e3aa4cb864f7ef5a94cb2aa2ae2d0d217e","NULSd6Hh4d84tQi4GGmkjDnzLsrGMJiFNnGYa");
        linPubKey.put("030431c58c879bef47f6fb5a410c8fde675e11ced2fb26cd6b84cdfa7d184e0ee4","NULSd6HhDarMgtz1cVPR7D9UWsmdJyReezHCU");
        linPubKey.put("0302b2e8ff18d2561f21a40fa3427d7a7fdb0fcc90f241eb1a18ca6acac5a868ff","NULSd6HhAYRyGHBgPhD4iV1jUtXMo6Wt3nEjF");
        linPubKey.put("03c173ea8f8dfa6093c5f592ba2677f8093730678f42db364db139cc026990f934","NULSd6HhGeaL5rsoEtmewAyKqrbNuDBagAVEE");
        linPubKey.put("034435347b9e4c569f80ce40b4347064c317246e0d1e2e61b4959c5d5492cd4e57","NULSd6HhBdHf1EqCbh4FVJp5KchRQ6DXUN7sa");
        linPubKey.put("0219406697419175d5a2f240b5a31384c556ce42045dc45b7847c07afcc4e5c92a","NULSd6HhDvgqVCcWVjeU9EFRUXYvn1NuuYS6z");
        linPubKey.put("033eac0c328b83c5dd65268192358a3dcbf8b83714b7b11d8bfcb46b31b2671bd6","NULSd6HhFZNBx6VwkHCg7fzw2SSCYYmRCjqJB");
        linPubKey.put("03b6ea1e63bb07951eefd333f2f85a5cb9552b5cf4f54aa1276218406ab8cd73c3","NULSd6Hh3TZAQvy3XTdtF3judBns35chmBPhw");
        linPubKey.put("02a95959898a079195ac702beb9bd054f045d5bd419c5d2017fff5193dc6c21d8e","NULSd6Hh9nTQTnfDRW85sxQapLEw19y7Zrf5s");
        linPubKey.put("037c21c58417af51fb5d8e9b5a7af1b08bccb7d900d11e2c1585bcf30ae07a6ea0","NULSd6HhCbTZFWHB6H91N5cQBXZEjNexQaZQR");
        linPubKey.put("03fb72644bb6efba8419b8302d41199f4dfc92278280a0934b4644646303b0d8e7","NULSd6Hh4fyCHvVyHGCAWHzHdtARy1EDzf3Kd");
        linPubKey.put("03e6de8b7f5c6ee2356413918d2a99bd1d9c850d10e9675b0e4c861a52f252f23e","NULSd6HhDt1VjLb4toyxuS2rBfVqGzpvoSvqy");
        linPubKey.put("02d5d56a1e5c478cd58890e187f878d2479a24d14f11b1001b851bd1fe935ceba7","NULSd6Hh3qpJFpojfMKu1w1JaniCiH6UrNKYe");
        linPubKey.put("031d94504ec05d2ba0f6b9b56dae0d629eead3622631d64fc23da86be87cb4695a","NULSd6HhANLPK1hk6eJ7BnvsvAmsEfABPpRL6");
        linPubKey.put("02ab09157f648a80c47aa934a243b96a4d55fd4111fa75053bdbd39dac19023868","NULSd6Hh3f6MkaQtQ48SPQRg8RMG7yfDgFYrT");
        linPubKey.put("03d59c3182cbaf53c65284806482ac8929345752a9397e2fd396cc9c8d26dba26a","NULSd6Hh2eeNufWiQkKZsyv5k2jPwMMuWB5BQ");
        linPubKey.put("02b83f315fc8d872edcbbc8494ac48ff568a084d4219b2cf51423ca6a6f9f45078","NULSd6HhFUEJ3QmZqk1yS2jvMpXXvbjkL7hAo");
        linPubKey.put("0263d8638edb15e70b3e76a2e4f1903e9e63c6d7af00efe745ed8aaeca58767779","NULSd6Hh34HEHi4N4t6118x73Dqb3dLNWevT8");
        linPubKey.put("03b724dea2a201587d6fd6cd55b8b456c0f729e1153ae7d37b36f53edb93e2f862","NULSd6Hh3SYfQoA3Exv8d2bYYa8stLTr3eXxC");
        linPubKey.put("039663f6af032d05ca305fdad284b5b9c7b93ab59e0edbc9b723e3e2b643379c54","NULSd6HhG7eoqfbtw6aFFJPJPHFNonRwPJMk9");
        linPubKey.put("030278af7cb7f3fbdc331a663cf9eeee4e2b750da4659d7cbfdf5d1fa552732bab","NULSd6HhDpfT5GGyq1wJE7hV644YomnUXMx5h");
        linPubKey.put("03e171420cb9f6ce94fce33971b493e7808e83b7b6b848f87a875c71c1ee47641c","NULSd6HhAsur8mfjCVqhqB8PCkWLHUxyU3TTb");
        linPubKey.put("035ed397949bca80d224b0c17c8f24b6d22ca1dbc7b9a5d7795de427486567026e","NULSd6HhEgsueGXWZSEWUZUyw7pBwAX8BtQgR");
        linPubKey.put("0298b83ffb30fbe6358c9e54280cdf08b5de66e0adff770ff56c02e0ea2589b16b","NULSd6HhBqB5kAe9Qvv9fqTYKM5stpyPoMmqk");
        linPubKey.put("03232081e2e9c3a0da45640863f0bfa9248a7b4dc511a93c4b0a23dcc6e1309bde","NULSd6Hh98w9WXDYmryoMf1EGu52nbLBSQ5t6");
        linPubKey.put("03c7b6fcca944d180cdaddc992ccb71c7f3b916a0db1fae863bbd599025ad195b0","NULSd6HhAMUSRQ86Nr4F6d8b1XCsuAphUytL3");
        linPubKey.put("026e99ff832bd38f07d2b22e222f01f1b55cc0f9496008707e8c3fb69f6d368286","NULSd6Hh6E1LQkU2Y9vPFsyBJnxYKoCaEAxeA");
        linPubKey.put("02edca9389700f168d11734cf358147de50203b43c8b6e42d61eebff483111ad99","NULSd6HhFjij9m3r5UEFu5NJ7SUNafh9WwPPN");
        linPubKey.put("034d6dc78236becbfdc840748e7a12c24385ab654971d0915d2c0dfa9688d56c53","NULSd6Hh57y2h1rTfHgJ2NXFGbhV4JVFHaPbw");
        linPubKey.put("039a559e42e21fb5f6ab529b11d1ee5835c438d501de24fd4e1d88dbc5d8ee53ec","NULSd6HhEVtdN3X4CPzp8aBiyxzk3iRh34NS7");
        List<String> otherPubKey = List.of("046ff585fca5282eb9c7479967890bb6cc5ff6ea2bce667283ce97a21227be62", "03b736b7ac6cd2cfe8c221926f8fc0e2b84f8cb96c079c800be09f682d8805097e", "03a1f65c80936606df6185fe9bd808d7dd5201e1e88f2a475f6b2a70d81f7f52e4", "021226ecc609a1a054b760d30a8e1715524ddf22274b152a7f8a98b9b7da81d833");
        linPubKey.entrySet().forEach(entry->{
            GenerateMultiSignAccountReq req = new GenerateMultiSignAccountReq();
            List<String> mlist = new ArrayList<>(otherPubKey);
            mlist.add(entry.getKey());
            req.setPubKeys(mlist);
            req.setMinSigns(3);
            io.nuls.base.api.provider.Result<String> result = accountService.createMultiSignAccount(req);
            String mk = result.getData();
            if(!mk.equals(entry.getValue())){
                throw new RuntimeException("不相同");
            }
            Log.info("{}:{}",result.getData(),entry.getValue());
        });
    }

    public static void main(String[] args) throws NulsException, JsonProcessingException {
        String tx = "02009f32845e066e6f6e6f6e6f008c01170100017c3286ba1b3edcc237a3a9b13080f1bc7e99904001000100a096a6d4e8000000000000000000000000000000000000000000000000000000080000000000000000000117010001d5250c029d54bdefb3504d3b1912681627592a1f010001000010a5d4e800000000000000000000000000000000000000000000000000000000000000000000006921026e80e0b3816b92be5eea85f9c8b8c2f99ea8ef3ef96346ef49ddd40c08a2e207463044022052b211bd932126b0028f1eaa792efd116d5684f153fc177d727526963d9e7bea02201657e55aee9c044cc77d3d8729b06955db68f2c1245a2d6a5c7c1a9402295980";
        NulsByteBuffer byteBuffer = new NulsByteBuffer(HexUtil.decode(tx));
        Transaction transaction = new Transaction();
        transaction.parse(byteBuffer);
        transaction.getCoinDataInstance();
        System.out.println("time:" + transaction.getTime());
        System.out.println("type:" + transaction.getType());
        System.out.println("remark:" + new String(transaction.getRemark()));
        System.out.println("coindata: " + JSONUtils.obj2json(transaction.getCoinDataInstance()));
        System.out.println(AddressTool.getStringAddressByBytes(transaction.getCoinDataInstance().getFrom().get(0).getAddress()));
        System.out.println("txHash:" + transaction.getHash().toHex());
    }

}
