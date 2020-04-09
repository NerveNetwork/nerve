package io.nuls.api.test;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.mongo.MongoBlockServiceImpl;
import io.nuls.api.db.mongo.MongoContractServiceImpl;
import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.model.po.BlockHeaderInfo;
import io.nuls.api.model.po.ContractInfo;
import io.nuls.api.model.po.DepositInfo;
import io.nuls.api.model.po.StackSnapshootInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import javax.print.Doc;
import java.math.BigInteger;
import java.util.*;

public class MongoDBTest {

    MongoDBService mongoDBService;

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

    }


    @Test
    public void testDBSaveBlock() {
        MongoBlockServiceImpl mongoBlockServiceImpl = SpringLiteContext.getBean(MongoBlockServiceImpl.class);
        long time1, time2;
        time1 = System.currentTimeMillis();

        for (int i = 1; i < 2; i++) {
            BlockHeaderInfo headerInfo = new BlockHeaderInfo();
            headerInfo.setHeight(i);
            mongoBlockServiceImpl.saveBLockHeaderInfo(2, headerInfo);
            if (i % 100000 == 0) {
                time2 = System.currentTimeMillis();

                System.out.println("i:" + i + ",--------------------time:" + (time2 - time1));
                time1 = time2;
            }
        }


        BlockHeaderInfo headerInfo = mongoBlockServiceImpl.getBlockHeader(2, 0);
        System.out.println(headerInfo.getHeight());
    }

    @Test
    public void testSaveBlocks() {
        MongoBlockServiceImpl mongoBlockServiceImpl = SpringLiteContext.getBean(MongoBlockServiceImpl.class);
        BlockHeaderInfo a = mongoBlockServiceImpl.getBlockHeader(2, 0);

        List<BlockHeaderInfo> blockHeaderInfos = new ArrayList<>();
        for (int i = 1; i < 1000000; i++) {
            BlockHeaderInfo headerInfo = new BlockHeaderInfo();
            headerInfo.setHeight(i);
//            headerInfo.setSize(100);
//            headerInfo.setTotalFee(new BigInteger("100"));
//            headerInfo.setAgentVersion(1);
//            headerInfo.setScriptSign("abcdkeagaadsf");
//            headerInfo.setPackingAddress("dfadsfadsf");
//            headerInfo.setAgentHash("fasdfsdafasd");
//            headerInfo.setMerkleHash("fdsafsdfsdf");
//            headerInfo.setPreHash("fdsafadsfsd");
//            headerInfo.setHash("fdsafasdfd");
//            headerInfo.setTxHashList(List.of("abd","fsdf","Fdsf"));
//            headerInfo.setAgentAlias("dsfsd");
//            headerInfo.setAgentId("dfsdf");
//            headerInfo.setCreateTime(System.nanoTime());
//            headerInfo.setPackingIndexOfRound(3);
//            headerInfo.setReward(new BigInteger("2990"));
//            headerInfo.setTxCount(6);
//            headerInfo.setRoundIndex(4);
//            headerInfo.setRoundStartTime(10100);
//            headerInfo.setSeedPacked(false);
            blockHeaderInfos.add(headerInfo);
        }
//           mongoBlockServiceImpl.saveBulkList(2, blockHeaderInfos);
        mongoBlockServiceImpl.saveList(2, blockHeaderInfos);
    }

    public static class TestInfo {
        String agentHash;
        BigInteger amount;
        String time;

        public String getAgentHash() {
            return agentHash;
        }

        public void setAgentHash(String agentHash) {
            this.agentHash = agentHash;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }


    @Test
    public void testAgg() {
//        MongoCollection<Document> col = mongoDBService.getCollection("deposit_table_1");
//        AggregateIterable<Document> res = col.aggregate(List.of());
//        res.iterator().forEachRemaining(d->{
//            Log.info("{}",DocumentTransferTool.toInfo(d,TestInfo.class));
//        });
        mongoDBService.aggReturnDoc(DBTableConstant.STACK_SNAPSHOOT_TABLE + 2,
//                new Document("$project", new Document("time", new Document("$dateToString",
////                        new Document("format", "%Y-%m-%d %H:%M:%S").append("date", new Document("$add",
////                                List.of(new Date(0),"$time",8*3600*1000))))))
//                ,
                        new Document("$group", new Document("_id", 0).append("amount", new Document("$sum", "$stackTotal"))),
                        new Document("$project",new Document("_id",0).append("amount",1))
        )
                        .forEach(d -> Log.info("{}", d.get("amount")));

    }

    @Test
    public void testDBGet() {
        MongoBlockServiceImpl mongoBlockServiceImpl = SpringLiteContext.getBean(MongoBlockServiceImpl.class);
        BlockHeaderInfo headerInfo = mongoBlockServiceImpl.getBlockHeader(2, 0L);
        System.out.println();
    }


    @Test
    public void testGetContract() {
        MongoContractServiceImpl mongoContractServiceImpl = SpringLiteContext.getBean(MongoContractServiceImpl.class);
        ContractInfo contractInfo = mongoContractServiceImpl.getContractInfo(2, "tNULSeBaN32a2hucBYT9o4BnNoHQcM8WDmpPgq");
    }

    @Test
    public void testTransferUseTime() {
        BlockHeaderInfo blockHeaderInfo = new BlockHeaderInfo();
        blockHeaderInfo.setHash("abcdefg");
        blockHeaderInfo.setHeight(1L);
        blockHeaderInfo.setAgentAlias("alias");
        blockHeaderInfo.setAgentHash("bbbbbb");
        blockHeaderInfo.setAgentId("dfsaf");
        blockHeaderInfo.setAgentVersion(2);
        blockHeaderInfo.setCreateTime(131313L);
        blockHeaderInfo.setMerkleHash("dfsdfsdfsd");
        blockHeaderInfo.setPackingAddress("dfasdfdasf");
        blockHeaderInfo.setPackingIndexOfRound(1);
        blockHeaderInfo.setPreHash("fdsfsdf");
        blockHeaderInfo.setReward(new BigInteger("26"));
        blockHeaderInfo.setRoundIndex(1L);
        blockHeaderInfo.setRoundStartTime(131L);
        blockHeaderInfo.setScriptSign("fdsfsdf");
        blockHeaderInfo.setTotalFee(new BigInteger("26"));
        blockHeaderInfo.setTxCount(1);
        blockHeaderInfo.setTxHashList(List.of("abcd", "aset", "sdfsd", "eeee"));

        long time1 = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            Document document = new Document();
            document.put("hash", blockHeaderInfo.getHash());
            document.put("height", blockHeaderInfo.getHeight());
            document.put("preHash", blockHeaderInfo.getPreHash());
            document.put("merkleHash", blockHeaderInfo.getMerkleHash());
            document.put("createTime", blockHeaderInfo.getCreateTime());
            document.put("agentHash", blockHeaderInfo.getAgentHash());
            document.put("agentId", blockHeaderInfo.getAgentId());
            document.put("packingAddress", blockHeaderInfo.getPackingAddress());
            document.put("agentAlias", blockHeaderInfo.getAgentAlias());
            document.put("txCount", blockHeaderInfo.getTxCount());
            document.put("roundIndex", blockHeaderInfo.getRoundIndex());
            document.put("totalFee", blockHeaderInfo.getTotalFee());
            document.put("reward", blockHeaderInfo.getReward());
            document.put("size", blockHeaderInfo.getSize());
            document.put("packingIndexOfRound", blockHeaderInfo.getPackingIndexOfRound());
            document.put("scriptSign", blockHeaderInfo.getScriptSign());
            document.put("txHashList", blockHeaderInfo.getTxHashList());
            document.put("isSeedPacked", blockHeaderInfo.isSeedPacked());
            document.put("roundStartTime", blockHeaderInfo.getRoundStartTime());
            document.put("agentVersion", blockHeaderInfo.getAgentVersion());
        }

        System.out.println("-------------time;" + (System.currentTimeMillis() - time1));

        time1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            Document document = DocumentTransferTool.toDocument(blockHeaderInfo);
        }
        System.out.println("-------------time;" + (System.currentTimeMillis() - time1));
    }


}
