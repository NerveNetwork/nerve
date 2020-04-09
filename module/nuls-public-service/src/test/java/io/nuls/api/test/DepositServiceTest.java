package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.DepositService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.db.mongo.MongoDepositServiceImpl;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-19 14:03
 * @Description: 功能描述
 */
public class DepositServiceTest {

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
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "admin", "password".toCharArray());
        MongoClient mongoClient = new MongoClient(serverAddress,credential, MongoClientOptions.builder().build());
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
        depositService = new MongoDepositServiceImpl();
        ((MongoDepositServiceImpl) depositService).setMongoDBService(mongoDBService);
    }


    @Test
    public void testGetDepositList(){
        depositService.getDepositSumList(2).forEach(d->{
            Log.info("{}",d);
        });
    }

}
