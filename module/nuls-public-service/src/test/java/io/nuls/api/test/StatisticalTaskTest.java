package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.PublicServiceBootstrap;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.db.mongo.MongoSymbolRegServiceImpl;
import io.nuls.api.model.po.AssetSnapshotInfo;
import io.nuls.api.rpc.controller.ChainController;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.service.impl.SymbolUsdtPriceProviderServiceImpl;
import io.nuls.api.task.NewStatisticalTask;
import io.nuls.api.task.QueryChainInfoTask;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-23 15:53
 * @Description: 功能描述
 */
public class StatisticalTaskTest extends BaseTestCase{


    MongoDBService mongoDBService;

    NewStatisticalTask newStatisticalTask;



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
//        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
//        MongoCredential credential = MongoCredential.createScramSha1Credential("nuls", "admin", "password".toCharArray());
//        MongoClient mongoClient = new MongoClient(serverAddress,credential, MongoClientOptions.builder().build());
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(DBTableConstant.DATABASE_NAME);
//        mongoDBService = new MongoDBService(mongoClient, mongoDatabase);
//        SymbolRegService symbolRegService = new MongoSymbolRegServiceImpl();
//        ((MongoSymbolRegServiceImpl) symbolRegService).setMongoDBService(mongoDBService);
//        SymbolUsdtPriceProviderService symbolUsdtPriceProviderService = new SymbolUsdtPriceProviderServiceImpl();
//        newStatisticalTask = new NewStatisticalTask();
//        newStatisticalTask.setSymbolUsdtPriceProviderService(symbolUsdtPriceProviderService);
//        newStatisticalTask.setSymbolRegService(symbolRegService);
//        PublicServiceBootstrap.main(new String[]{});
//        PublicServiceBootstrap publicServiceBootstrap = SpringLiteContext.getBean(PublicServiceBootstrap.class);
//        while (publicServiceBootstrap.getState() != RpcModuleState.Running){
//            try {
//                Thread.sleep(1000L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        super.before();
        new QueryChainInfoTask(2).run();
        newStatisticalTask = new NewStatisticalTask(2);
    }

    @Test
    public void testBuildTransactionSnapshoot(){
        List<AssetSnapshotInfo> list = newStatisticalTask.buildAsssetSnapshoot(1,860);
        list.forEach(d->{
            Log.info("{}",d);
        });
    }

    @Test
    public void testSaveStatistical(){
//        ChainController chainController = SpringLiteContext.getBean(ChainController.class);
//        chainController.symbolReport(List.of(2));
        newStatisticalTask.run();
    }

    @Test
    public void testSymbolReport(){
        ChainController chainController = SpringLiteContext.getBean(ChainController.class);
        Log.info("{}",chainController.symbolReport(List.of(2)));

    }

}
