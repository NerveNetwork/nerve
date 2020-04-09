package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.analysis.AnalysisHandler;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.mongo.*;
import io.nuls.api.model.po.BlockInfo;
import io.nuls.api.model.po.StackSnapshootInfo;
import io.nuls.api.utils.DateUtil;
import io.nuls.core.constant.TxType;
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
public class StackSnapShootServiceTest {

    MongoDBService mongoDBService;

    MongoStackSnapshootServiceImpl stackSnapshootService;

    MongoChainServiceImpl chainService ;

    MongoSymbolQuotationPriceServiceImpl symbolPriceService = new MongoSymbolQuotationPriceServiceImpl();

    MongoDepositServiceImpl depositService = new MongoDepositServiceImpl();

    @Before
    public void before() {

//        String dbName = "nuls-api";
//        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);
//        MongoDBService mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        SpringLiteContext.putBean("dbService", mongoDBService);

//        ApiContext.databaseUrl = "127.0.0.1";
//        ApiContext.databasePort = 27018;
//        SpringLiteContext.init("io.nuls");
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "admin", "password".toCharArray());
        MongoClient mongoClient = new MongoClient(serverAddress, credential, MongoClientOptions.builder().build());
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
        stackSnapshootService = new MongoStackSnapshootServiceImpl();
        ((MongoStackSnapshootServiceImpl) stackSnapshootService).setMongoDBService(mongoDBService);
        mongoDBService.dropTable(DBTableConstant.STACK_SNAPSHOOT_TABLE + 1);
        chainService = new MongoChainServiceImpl();
        chainService.setMongoDBService(mongoDBService);
        symbolPriceService.setMongoDBService(mongoDBService);
        depositService.setMongoDBService(mongoDBService);
        depositService.setSymbolPriceService(symbolPriceService);
        stackSnapshootService.setDepositService(depositService);
        stackSnapshootService.setSymbolPriceService(symbolPriceService);
    }

    @Test
    public void testSave() {
        StackSnapshootInfo info = new StackSnapshootInfo();
        info.setBaseInterest(BigInteger.valueOf(100));
        info.setBlockHeight(100);
        long timestamp = System.currentTimeMillis();
        info.setDay(timestamp - timestamp % (3600 * 1000 * 24) - 3600 * 1000 * 24);
        info.setStackTotal(BigInteger.TEN);
        stackSnapshootService.save(1, info);
        info.setDay(timestamp - timestamp % (3600 * 1000 * 24));
        info.setBaseInterest(BigInteger.valueOf(100));
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
        chainService.initCache();
        BlockInfo blockInfo = AnalysisHandler.toBlockInfo("c6aba96cad7083dc64aa4dc3cdbbe6d5c7e7c3ca3a33e26a3b18fd11affb26d2c46fad23dbb78d0b1e18ed098266093d4b5bb857120d7debae7a85505a42e5ae809d685ecf23000002000000140210000003007e9d685e0200010001005064000021037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a086346304402204dae8459f02ae6cb41e71828fa53c54e2759243863143bc7f79cd3a2f5b840f402204eb42321c310efe16307d2761a3a46653f2d025ef1442f81110caf9144aaf9e30100809d685e0000460001170200015f64837d090e0536482fbcab79b4f99f84c1f97e02000100a08601000000000000000000000000000000000000000000000000000000000000000000000000000002007e9d685e00008c01170200017fe9a685e43b3124e00fd9c8e4e59158baea634502000100a026744e1809000000000000000000000000000000000000000000000000000008fdacc3ceee0d3efb00011702000145f9cb67a8573840daffa166fcab2a93d9b467fe0200010000a0724e1809000000000000000000000000000000000000000000000000000000000000000000006a210318f683066b45e7a5225779061512e270044cc40a45c924afcf78bb7587758ca0473045022100fbab0194bed8c775c7b60b64d72cdcdfce509e3efaca225197234a540e4fb3d00220618b61a381b46f34caca459695c28ac438bcda30c7738c95fed5fbbc3a21148d",2);
        stackSnapshootService.setLastSnapshootTime(DateUtil.getDayStartTimestamp() - DateUtil.ONE_DAY_MILLISECOND);
        blockInfo.getTxList().forEach(tx->{
            if(tx.getType() == TxType.COIN_BASE){
                Optional<StackSnapshootInfo> stackSnapshootInfo = stackSnapshootService.buildSnapshoot(2,tx);
                stackSnapshootInfo.ifPresent(d->{
                    Log.info("{}",d);
                    stackSnapshootService.save(2,d);
                });
            }
        });
    }

}
