package io.nuls.api.test;

import io.nuls.api.db.DepositService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-19 14:03
 * @Description: 功能描述
 */
public class DepositServiceTest extends BaseTestCase {

    MongoDBService mongoDBService;

    DepositService depositService;

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
        super.before();
//        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
//        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "nuls-api", "nuls123456".toCharArray());
//        MongoClient mongoClient = new MongoClient(serverAddress,credential, MongoClientOptions.builder().build());
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
//        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        depositService = new MongoDepositServiceImpl();
//        ((MongoDepositServiceImpl) depositService).setMongoDBService(mongoDBService);
    }


    @Test
    public void testGetDepositList(){
        DepositService depositService = SpringLiteContext.getBean(DepositService.class);
        Log.info("{}",depositService.getAgentDepositTotal(4,25291));
//        Log.info("{}",depositService.getStackingTotalAndTransferNVT(4,25291));

    }

}
