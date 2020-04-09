package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.db.mongo.MongoSymbolQuotationPriceServiceImpl;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.StackSymbolPriceInfo;
import io.nuls.api.model.po.SymbolQuotationRecordInfo;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.nuls.api.db.SymbolQuotationPriceService.USDT;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 16:23
 * @Description: 功能描述
 */
public class SymbolPriceServiceTest  {

    MongoDBService mongoDBService;

    SymbolQuotationPriceService mongoSymbolPriceService;

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
        mongoSymbolPriceService = new MongoSymbolQuotationPriceServiceImpl();
        ((MongoSymbolQuotationPriceServiceImpl) mongoSymbolPriceService).setMongoDBService(mongoDBService);

    }

    @Test
    public void testSave() throws InterruptedException {
        mongoDBService.dropTable(DBTableConstant.SYMBOL_FINAL_QUOTATION_TABLE);
        StackSymbolPriceInfo info = mongoSymbolPriceService.getFreshUsdtPrice("BTC");
        Log.info("BTC PRICE111:{}",info);
        info = new StackSymbolPriceInfo();
        info.setPrice(BigDecimal.TEN);
        info.setCurrency(USDT);
        info.setSymbol("BTC");
        info.setTxHash("86aee05a3e31f80376fc167bd4e0b4cd83c2085b3bd62f865501abf036edd40d");
        info.setCreateTime(System.currentTimeMillis());
        info.setBlockHeight(100);
        List<StackSymbolPriceInfo> list = List.of(info);
        mongoSymbolPriceService.saveFinalQuotation(list);
        Log.info("BTC PRICE:{}",info);
        Log.info("get By cache");
        info = mongoSymbolPriceService.getFreshUsdtPrice("BTC");
        Log.info("BTC PRICE:{}",info);
        Thread.sleep(1000L);
        info.setPrice(BigDecimal.valueOf(2000));
        info.setCreateTime(System.currentTimeMillis());
        mongoSymbolPriceService.saveFinalQuotation(List.of(info));
        info = mongoSymbolPriceService.getFreshUsdtPrice("BTC");

        Log.info("BTC PRICE:{}",info);
    }

    @Test
    public void testGet(){
        StackSymbolPriceInfo btc = mongoSymbolPriceService.getFreshUsdtPrice("BTC");
        StackSymbolPriceInfo eth = mongoSymbolPriceService.getFreshUsdtPrice("ETH");
        System.out.println(btc.transfer(eth,BigDecimal.valueOf(1000)));
    }

    @Test
    public void testQueryList(){
        int pageIndex = 1,pageSize = 2;
        String symbol = "BTC";
        PageInfo<SymbolQuotationRecordInfo> res = mongoSymbolPriceService.queryQuotationList(symbol,pageIndex,pageSize,0,System.currentTimeMillis());
        System.out.println(res.getTotalCount());
        res.getList().forEach(d->{
            Log.info("{}",d);
        });
    }

}
