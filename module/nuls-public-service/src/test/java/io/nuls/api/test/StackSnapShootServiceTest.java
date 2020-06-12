package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.analysis.AnalysisHandler;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.StackSnapshootService;
import io.nuls.api.db.mongo.*;
import io.nuls.api.model.po.BlockInfo;
import io.nuls.api.model.po.StackSnapshootInfo;
import io.nuls.api.utils.DateUtil;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 15:25
 * @Description: 功能描述
 */
public class StackSnapShootServiceTest extends BaseTestCase {

    MongoDBService mongoDBService;

    MongoStackSnapshootServiceImpl stackSnapshootService;

    MongoChainServiceImpl chainService ;

    MongoSymbolQuotationPriceServiceImpl symbolPriceService = new MongoSymbolQuotationPriceServiceImpl();

    MongoDepositServiceImpl depositService = new MongoDepositServiceImpl();

    @Before
    public void before() {
        super.before();
//        String dbName = "nuls-api";
//        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);
//        MongoDBService mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        SpringLiteContext.putBean("dbService", mongoDBService);

//        ApiContext.databaseUrl = "127.0.0.1";
//        ApiContext.databasePort = 27018;
//        SpringLiteContext.init("io.nuls");
//        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
//        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "admin", "password".toCharArray());
//        MongoClient mongoClient = new MongoClient(serverAddress, credential, MongoClientOptions.builder().build());
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
//        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        stackSnapshootService = new MongoStackSnapshootServiceImpl();
//        ((MongoStackSnapshootServiceImpl) stackSnapshootService).setMongoDBService(mongoDBService);
//        mongoDBService.dropTable(DBTableConstant.STACK_SNAPSHOOT_TABLE + 1);
//        chainService = new MongoChainServiceImpl();
//        chainService.setMongoDBService(mongoDBService);
//        symbolPriceService.setMongoDBService(mongoDBService);
//        depositService.setMongoDBService(mongoDBService);
//        depositService.setSymbolPriceService(symbolPriceService);
//        stackSnapshootService.setDepositService(depositService);
//        stackSnapshootService.setSymbolPriceService(symbolPriceService);
    }

    @Test
    public void testSave() {
        StackSnapshootInfo info = new StackSnapshootInfo();
        info.setBaseInterest(BigDecimal.valueOf(100));
        info.setBlockHeight(100);
        long timestamp = System.currentTimeMillis();
        info.setDay(timestamp - timestamp % (3600 * 1000 * 24) - 3600 * 1000 * 24);
        info.setStackTotal(BigInteger.TEN);
        stackSnapshootService.save(1, info);
        info.setDay(timestamp - timestamp % (3600 * 1000 * 24));
        info.setBaseInterest(BigDecimal.valueOf(100));
        info.setBlockHeight(101);
        info.setStackTotal(BigInteger.TEN.multiply(BigInteger.TWO));
        stackSnapshootService.save(1, info);
        info = stackSnapshootService.getLastSnapshoot(1).get();
        Log.info("{}", info);
        List<StackSnapshootInfo> list = stackSnapshootService.queryList(1, 0, System.currentTimeMillis());
        Log.info("===========");
        list.forEach(d -> {
            Log.info("{}", d);
        });
    }

    @Test
    public void testSnapshoot() throws Exception {

//        chainService.initCache();
        MongoStackSnapshootServiceImpl stackSnapshootService = (MongoStackSnapshootServiceImpl) SpringLiteContext.getBean(StackSnapshootService.class);
        BlockInfo blockInfo = AnalysisHandler.toBlockInfo("1675c0418972982b4ad958143270334aca329f26a7a746e2a378c92f0189881d2bc96101d0f642a8f94dcae58eb6597776d0784f3e6e76b5868e12d3978bd2d280daa05ecb6200000400000014e4130000050078daa05e0500010001003c6400002102cec353f77275953704f5c6f64b38f3f49a5b590783392c69b2718e4bf18588be473045022100e67371b5c6886afafc3b8dcb14a553b23873c744e667feff0473f6f42386893902203e7dff5a5a4c3a9af8162a5a96182f2a67c18ae2e9c8b0f999fd7f88e9607182010080daa05e0000460001170400014caf4dc0435bce7593580bc56f1f46d8c1338e3b04000100400d030000000000000000000000000000000000000000000000000000000000000000000000000000e5007edaa05e0061cca4cc79728cc70e4585a22e0af3194924b4c52f4b3ecdfb69509e0d3670f7bc02802cf102000000000000000000000000000000000000000000000000000000002329021b000000000000000000000000000000000000000000000000000000008c01170400017fe9a685e43b3124e00fd9c8e4e59158baea63450400010020b3f2020000000000000000000000000000000000000000000000000000000008520dd6e0543c748b0001170400017fe9a685e43b3124e00fd9c8e4e59158baea634504000100802cf10200000000000000000000000000000000000000000000000000000000feffffffffffffff69210318f683066b45e7a5225779061512e270044cc40a45c924afcf78bb7587758ca0463044022038549e55b6c408b420e3105595b144628db509bfcb544e1c6d97ac778f3dd13702200931de6fba04f5983d064cb3121a50c7a116e3a05b43f5a1fad7a9c7fe3da9b6e5007edaa05e0061cca4cc79728cc70e4585a22e0af3194924b4c52f4b3ecdfb69509e0d3670f7bc01204bbc00000000000000000000000000000000000000000000000000000000002329021b00000000000000000000000000000000000000000000000000000000d2021704000102dc3a715ee3d1faa7f81cdea0687292d40c189d040002001b3555030000000000000000000000000000000000000000000000000000000008f0e1d4235a545010001704000102dc3a715ee3d1faa7f81cdea0687292d40c189d04000100a08601000000000000000000000000000000000000000000000000000000000008f0e1d4235a54501000011704000102dc3a715ee3d1faa7f81cdea0687292d40c189d040002001b35550300000000000000000000000000000000000000000000000000000000feffffffffffffff692103509c32f2dd34e0a5aca9dab8b5dd23cae5b53cf147a423c09a7cbc4dede3edd746304402202a115b832964ec4cbdbf13b8dafee68d09f3e84daa2a44cd01db92d8ec2cc08302201e6d606ad482580ff94c3ccd201cb01e5ad959f9a7ed59ef6a3688207e7776fee7007fdaa05e00c1cca4cc79728cc70e4585a22e0af3194924b4c52f4b3ecdfb69509e0d3670f7bcf3942e26ce1484645a085bcdc03b8d1416b6f714d24b43516a2610db5cab99ad3162fba00f1453d36eb59986978dbc4786e655fbe7a86f972406f57143d5152fae14750d00000000000000000000000000000000000000000000000000000000802cf10200000000000000000000000000000000000000000000000000000000ca7f431b0000000000000000000000000000000000000000000000000000000002fde201021704000102dc3a715ee3d1faa7f81cdea0687292d40c189d0400020033e48c1700000000000000000000000000000000000000000000000000000000086a2610db5cab99adff170400017fe9a685e43b3124e00fd9c8e4e59158baea634504000100802cf10200000000000000000000000000000000000000000000000000000000082406f57143d5152fff05170400017fe9a685e43b3124e00fd9c8e4e59158baea6345040002007cbc740d0000000000000000000000000000000000000000000000000000000000000000000000001704000102dc3a715ee3d1faa7f81cdea0687292d40c189d040001003819f1020000000000000000000000000000000000000000000000000000000000000000000000001704000186f7bbf2b3480d872a8996d37e40d72fe0bbf6e104000200325800000000000000000000000000000000000000000000000000000000000000000000000000001704000186f7bbf2b3480d872a8996d37e40d72fe0bbf6e104000100481300000000000000000000000000000000000000000000000000000000000000000000000000001704000102dc3a715ee3d1faa7f81cdea0687292d40c189d0400020085cf170a00000000000000000000000000000000000000000000000000000000feffffffffffffff00",4);
        stackSnapshootService.setLastSnapshootTime(DateUtil.getDayStartTimestamp() - DateUtil.ONE_DAY_MILLISECOND);
        blockInfo.getTxList().forEach(tx->{
            if(tx.getType() == TxType.COIN_BASE){
                Optional<StackSnapshootInfo> stackSnapshootInfo = stackSnapshootService.buildSnapshoot(4,blockInfo.getHeader());
                stackSnapshootInfo.ifPresent(d->{
                    Log.info("{}",d);
                    stackSnapshootService.save(4,d);
                });
            }
        });
    }

}
