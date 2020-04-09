package io.nuls.api.db.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.DepositService;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.manager.AssetManager;
import io.nuls.api.model.dto.AssetBaseInfo;
import io.nuls.api.model.po.DepositInfo;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.nuls.api.constant.DBTableConstant.DEPOSIT_TABLE;

@Component
public class MongoDepositServiceImpl implements DepositService {

    @Autowired
    private MongoDBService mongoDBService;

    @Autowired
    SymbolQuotationPriceService symbolPriceService;

    public DepositInfo getDepositInfoByKey(int chainId, String key) {
        Document document = mongoDBService.findOne(DEPOSIT_TABLE + chainId, Filters.eq("_id", key));
        if (document == null) {
            return null;
        }
        DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
        return depositInfo;
    }

    public DepositInfo getDepositInfoByHash(int chainId, String hash) {
        Document document = mongoDBService.findOne(DEPOSIT_TABLE + chainId, Filters.eq("txHash", hash));
        if (document == null) {
            return null;
        }
        DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
        return depositInfo;
    }

    public List<DepositInfo> getDepositListByAgentHash(int chainId, String hash) {
        List<DepositInfo> depositInfos = new ArrayList<>();
        Bson bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("deleteKey", null));
        List<Document> documentList = mongoDBService.query(DEPOSIT_TABLE + chainId, bson);
        if (documentList == null && documentList.isEmpty()) {
            return depositInfos;
        }
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        return depositInfos;
    }

    public PageInfo<DepositInfo> getDepositListByAgentHash(int chainID, String hash, int pageIndex, int pageSize) {
        Bson bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("deleteKey", null));
        List<Document> documentList = mongoDBService.pageQuery(DEPOSIT_TABLE + chainID, bson, Sorts.descending("createTime"), pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(DEPOSIT_TABLE + chainID, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        PageInfo<DepositInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, depositInfos);
        return pageInfo;
    }

    public List<DepositInfo> getDepositListByHash(int chainID, String hash) {
        Bson bson = Filters.and(Filters.eq("txHash", hash));
        List<Document> documentList = mongoDBService.query(DEPOSIT_TABLE + chainID, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        return depositInfos;
    }

    public void rollbackDeposit(int chainId, List<DepositInfo> depositInfoList) {
        if (depositInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (DepositInfo depositInfo : depositInfoList) {

            if (depositInfo.isNew()) {
                modelList.add(new DeleteOneModel<>(Filters.eq("_id", depositInfo.getKey())));
            } else {
                Document document = DocumentTransferTool.toDocument(depositInfo);
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", depositInfo.getKey()), document));
            }
        }
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        mongoDBService.bulkWrite(DEPOSIT_TABLE + chainId, modelList, options);
    }


    public void saveDepositList(int chainId, List<DepositInfo> depositInfoList) {
        if (depositInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (DepositInfo depositInfo : depositInfoList) {
            AssetBaseInfo assetBaseInfo = AssetManager.getAssetBaseInfo(depositInfo.getAssetChainId(),depositInfo.getAssetId());
            if(assetBaseInfo == null){
                Log.error("未获取到资产{}的基础信息",depositInfo.getAssetChainId() + "-" + depositInfo.getAssetId(),new RuntimeException());
                System.exit(0);
            }
            depositInfo.setDecimal(assetBaseInfo.getDecimals());
            depositInfo.setSymbol(assetBaseInfo.getSymbol());
            Document document = DocumentTransferTool.toDocument(depositInfo, "key");
            if (depositInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", depositInfo.getKey()), document));
            }
        }
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        mongoDBService.bulkWrite(DEPOSIT_TABLE + chainId, modelList, options);
    }

    @Override
    public List<DepositInfo> getDepositList(int chainId, long startHeight) {
//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.eq("type", 0), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//
//        List<Document> list = this.mongoDBService.query(DEPOSIT_TABLE + chainId, bson);
//        List<DepositInfo> resultList = new ArrayList<>();
//        for (Document document : list) {
//            resultList.add(DocumentTransferTool.toInfo(document, "key", DepositInfo.class));
//        }

        return getDepositList(chainId,startHeight,0);
    }

    /**
     * 获取高度范围类指定类型的抵押列表
     * @param chainId
     * @param startHeight
     * @param types
     * @return
     */
    @Override
    public List<DepositInfo> getDepositList(int chainId, long startHeight, int... types) {

        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
        List<Bson> typeFileer = new ArrayList<>();
        for (int i = 0;i<types.length;i++) {
            typeFileer.add(Filters.eq("type",types[i]));
        }
        bson = Filters.and(bson,Filters.or(typeFileer.toArray(new Bson[typeFileer.size()])));
        List<Document> list = this.mongoDBService.query(DEPOSIT_TABLE + chainId, bson);
        List<DepositInfo> resultList = new ArrayList<>();
        for (Document document : list) {
            resultList.add(DocumentTransferTool.toInfo(document, "key", DepositInfo.class));
        }
        return resultList;
    }




    @Override
    public BigInteger getDepositAmount(int chainId, String address, String agentHash) {
        Bson filter;
        if (StringUtils.isBlank(agentHash)) {
            filter = Filters.eq("address", address);
        } else {
            filter = Filters.and(Filters.eq("address", address), Filters.eq("agentHash", agentHash));
        }
        final BigInteger[] total = {BigInteger.ZERO};
        Consumer<Document> listBlocker = new Consumer<>() {
            @Override
            public void accept(final Document document) {
                BigInteger value = new BigInteger(document.getString("amount"));
                total[0] = total[0].add(value);
            }
        };
        MongoCollection<Document> collection = mongoDBService.getCollection(DEPOSIT_TABLE + chainId);
        collection.find(filter).projection(new BasicDBObject().append("amount", 1)).forEach(listBlocker);

        return total[0];
    }

    public PageInfo<DepositInfo> getCancelDepositListByAgentHash(int chainId, String hash, int type, int pageIndex, int pageSize) {
        Bson bson;
        if (type != 2) {
            bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("type", type));
        } else {
            bson = Filters.eq("agentHash", hash);
        }
        List<Document> documentList = mongoDBService.pageQuery(DEPOSIT_TABLE + chainId, bson, Sorts.descending("createTime"), pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(DEPOSIT_TABLE + chainId, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        PageInfo<DepositInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, depositInfos);
        return pageInfo;
    }

    public List<String> getAgentHashList(int chainId, String address) {
        Bson bson = Filters.and(Filters.eq("address", address), Filters.eq("type", 0), Filters.eq("deleteHeight", 0));
        DistinctIterable<String> iterable = mongoDBService.getCollection(DEPOSIT_TABLE + chainId).distinct("agentHash", bson, String.class);
        List<String> list = new ArrayList<>();
        MongoCursor<String> mongoCursor = iterable.iterator();
        while (mongoCursor.hasNext()) {
            list.add(mongoCursor.next());
        }
        return list;
    }

    public PageInfo<DepositInfo> getDepositListByAddress(int chainId, String agentHash, String address, int pageIndex, int pageSize) {
        Bson bson;
        if (StringUtils.isBlank(agentHash)) {
            bson = Filters.and(Filters.eq("address", address), Filters.eq("type", 0), Filters.eq("deleteHeight", 0));
        } else {
            bson = Filters.and(Filters.eq("address", address), Filters.eq("agentHash", agentHash), Filters.eq("type", 0), Filters.eq("deleteHeight", 0));
        }
        long totalCount = mongoDBService.getCount(DEPOSIT_TABLE + chainId, bson);
        List<Document> documentList = mongoDBService.pageQuery(DEPOSIT_TABLE + chainId, bson, Sorts.descending("createTime"), pageIndex, pageSize);
        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        PageInfo<DepositInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, depositInfos);
        return pageInfo;
    }

    @Override
    public BigInteger getAgentDepositTotal(int chainId,long height) {
        List<DepositInfo> depositList = this.getDepositList(chainId, height, DepositInfoType.CREATE_AGENT,DepositInfoType.APPEND_AGENT_DEPOSIT,DepositInfoType.REDUCE_AGENT_DEPOSIT,DepositInfoType.STOP_AGENT);
        return depositList.stream().map(d->d.getAmount()).reduce(BigInteger.ZERO,(d1,d2)->d1.add(d2));
    }

    @Override
    public BigInteger getStackingTotalByNVT(int chainId,long height) {
        List<DepositInfo> depositList = this.getDepositList(chainId, height, DepositInfoType.STACKING);
        SymbolPrice nvtUsdtPrice = symbolPriceService.getFreshUsdtPrice(ApiContext.defaultChainId,ApiContext.defaultAssetId);

        BigInteger stackingTotal = depositList.stream().map(d->{
            //抵押的是NVT直接返回数量
            if(d.getAssetChainId() == ApiContext.defaultChainId && d.getAssetId() == ApiContext.defaultAssetId){
                return d.getAmount();
            }
            //需要将异构资产换算成NVT
            //获取当前抵押的资产与USDT的汇率
            SymbolPrice symbolPrice = symbolPriceService.getFreshUsdtPrice(d.getAssetChainId(),d.getAssetId());
            //将当前抵押资产转换成NVT
            BigDecimal amount = nvtUsdtPrice.transfer(symbolPrice,new BigDecimal(d.getAmount()).movePointLeft(d.getDecimal()));
            return amount.movePointRight(ApiContext.defaultDecimals).toBigInteger();
        }).reduce(BigInteger.ZERO,(d1,d2)->d1.add(d2));
        return stackingTotal;
    }

    @Override
    public List<DepositInfo> getDepositSumList(int chainId){
        Bson id = new Document()
                .append("assetChainId","$assetChainId")
                .append("assetId","$assetId")
                .append("decimal","$decimal")
                .append("symbol","$symbol");
        Bson sum = new Document("$sum","$amount");
        Bson group = new Document().append("_id",id).append("sum",sum);
        List<Document> list = mongoDBService.aggReturnDoc(DEPOSIT_TABLE + chainId,new Document("$group",group));
        return list.stream().map(d->{
            Document _id = (Document) d.get("_id");
            Long amount = d.getLong("sum");
            int assetChainId = _id.getInteger("assetChainId");
            int assetId = _id.getInteger("assetId");
            String symbol = _id.getString("symbol");
            int decimal = _id.getInteger("decimal");
            DepositInfo di = new DepositInfo();
            di.setSymbol(symbol);
            di.setAmount(amount);
            di.setAssetChainId(assetChainId);
            di.setAssetId(assetId);
            di.setDecimal(decimal);
            return di;
        }).collect(Collectors.toList());
    }

    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    public void setSymbolPriceService(SymbolQuotationPriceService symbolPriceService) {
        this.symbolPriceService = symbolPriceService;
    }
}
