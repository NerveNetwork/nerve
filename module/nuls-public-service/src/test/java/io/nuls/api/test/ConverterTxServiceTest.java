package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.mongo.MongoChainServiceImpl;
import io.nuls.api.db.mongo.MongoConverterTxServiceImpl;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.db.mongo.MongoStackSnapshootServiceImpl;
import io.nuls.api.model.po.ConverterTxInfo;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.nuls.api.constant.DBTableConstant.CONVERTER_TX_TABLE;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-24 14:40
 * @Description: 功能描述
 */
public class ConverterTxServiceTest {

    MongoConverterTxServiceImpl converterTxService;


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
//        mongoDatabase.createCollection(CONVERTER_TX_TABLE + 2);
        MongoDBService mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
        converterTxService = new MongoConverterTxServiceImpl();
        converterTxService.setMongoDBService(mongoDBService);
//        mongoDBService.dropTable(DBTableConstant.CONVERTER_TX_TABLE + 2);
    }

    @Test
    public void testQueryList(){
        List<ConverterTxInfo> list = converterTxService.queryList(2,0,1000);
        list.forEach(d->{
            Log.info("{}",d);
        });
    }


}
