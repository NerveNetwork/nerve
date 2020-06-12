package io.nuls.api.db.mongo;

import com.mongodb.client.model.*;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.constant.config.ApiConfig;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.po.*;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

import static io.nuls.api.constant.ApiConstant.SPACE;
import static io.nuls.api.constant.DBTableConstant.SYMBOL_QUOTATION_RECORD_TABLE;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 14:56
 * @Description: 功能描述
 */
@Component
public class MongoSymbolQuotationPriceServiceImpl implements SymbolQuotationPriceService {

    private static final String TABLE = DBTableConstant.SYMBOL_FINAL_QUOTATION_TABLE;

    private Map<String,Map<String, StackSymbolPriceInfo>> cache = new HashMap<>();

    @Autowired
    MongoDBService mongoDBService;

    @Autowired
    ApiConfig apiConfig;

    @Autowired
    SymbolRegService symbolRegService;

    @Override
    public void saveFinalQuotation(List<StackSymbolPriceInfo> list) {
        if(list == null || list.isEmpty()){
            return ;
        }
        save(list,TABLE);
        list.forEach(d->{
            //清掉本地缓存
            cache.put(d.getCurrency(),new HashMap<>());
        });
    }

    private void save(List<StackSymbolPriceInfo> list,String colName){
        if(list == null || list.isEmpty()){
            return ;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        list.forEach(d->{
            d.set_id(d.getTxHash() + SPACE + d.getCurrency() + SPACE + d.getSymbol());
            Optional<SymbolRegInfo> symbolInfo = symbolRegService.getFirst(d.getSymbol());
            symbolInfo.ifPresent(si->{
                d.setAssetChainId(si.getChainId());
                d.setAssetId(si.getAssetId());
                //转换成毫秒
                d.setCreateTime(d.getCreateTime());
            });
            modelList.add(new ReplaceOneModel(new Document("_id",d.get_id()),DocumentTransferTool.toDocument(d), new ReplaceOptions().upsert(true)));
        });
        mongoDBService.bulkWrite(colName,modelList);
    }

    @Override
    public void saveQuotation(List<StackSymbolPriceInfo> list) {
        save(list,SYMBOL_QUOTATION_RECORD_TABLE);
    }

    @Override
    public PageInfo<SymbolQuotationRecordInfo> queryQuotationList(String symbol, int pageIndex, int pageSize,long startTime,long endTime) {
        Bson condition = Filters.and(Filters.eq("symbol",symbol),Filters.gte("createTime",startTime),Filters.lte("createTime",endTime));
        Bson sort = Sorts.descending("blockHeight");
        Long totalCount = mongoDBService.aggReturnDoc(SYMBOL_QUOTATION_RECORD_TABLE,
                Aggregates.match(condition),
                Aggregates.group("$symbol",Accumulators.sum("count",1))
//                Aggregates.match(Filters.eq("_id",symbol))
                ).stream().map(d->Long.parseLong(d.get("count").toString())).findFirst().orElse(0L);
        List<SymbolQuotationRecordInfo> list = mongoDBService.limitQuery(SYMBOL_QUOTATION_RECORD_TABLE,condition,sort,(pageIndex - 1) * pageSize,pageSize)
                .stream().map(d->
                    DocumentTransferTool.toInfo(d,SymbolQuotationRecordInfo.class)
                ).collect(Collectors.toList());
        return new PageInfo<>(pageIndex, pageSize, totalCount, list);
    }

    @Override
    public StackSymbolPriceInfo getFreshUsdtPrice(String symbol) {
        return getFreshPrice(symbol,USDT);
    }

    @Override
    public StackSymbolPriceInfo getFreshUsdtPrice(int assetChainId, int assetId) {
        return getFreshPrice(assetChainId,assetId,USDT);
    }

    @Override
    public StackSymbolPriceInfo getFreshPrice(String symbol, String currency) {
        StackSymbolPriceInfo info = getInfoByCache(symbol,currency);
        if(info != null){
            return info;
        }
        List<Document> list = mongoDBService.limitQuery(TABLE,new Document("symbol",symbol).append("currency",currency),new Document("createTime",-1),0,1);
        return buildSymbolPriceInfo(list,symbol,currency);
    }


    @Override
    public StackSymbolPriceInfo getFreshPrice(int assetChainId, int assetId, String currency) {
        List<Document> list = mongoDBService.limitQuery(TABLE,new Document("assetChainId",assetChainId).append("assetId",assetId).append("currency",currency),new Document("createTime",1),1,1);
        return buildSymbolPriceInfo(list,null,currency);
    }

    private StackSymbolPriceInfo buildSymbolPriceInfo(List<Document> list, String symbol, String currency){
        Optional<Document> doc =  list.stream().findFirst();
        if(doc.isEmpty()){
            return StackSymbolPriceInfo.empty(symbol,currency);
        }
        StackSymbolPriceInfo info = DocumentTransferTool.toInfo(doc.get(), StackSymbolPriceInfo.class);
        Map<String, StackSymbolPriceInfo> newMap = new HashMap<>();
        newMap.put(info.getSymbol(),info);
        cache.merge(currency,newMap,(old,n1)->{
            old.put(info.getSymbol(),info);
            return old;
        });
        return info;
    }

    private StackSymbolPriceInfo getInfoByCache(String symbol, String currency){
        return cache.getOrDefault(currency,Map.of()).get(symbol);
    }

    @Override
    public List<StackSymbolPriceInfo> getAllFreshPrice(){
        List<String> symbolList = mongoDBService.aggReturnDoc(TABLE,new Document("$group",new Document("_id","$symbol")))
                .stream().map(d->d.get("_id").toString()).collect(Collectors.toList());
        return symbolList.stream().map(this::getFreshUsdtPrice).collect(Collectors.toList());
    }


    public static void main(String[] args) {
        Map<String,Map<String,Integer>> map = new HashMap<>();
        Map<String,Integer> subMap = new HashMap<>();
        subMap.put("A",1);
        map.merge("a",Map.of("A",10),(o1,o2)->{
            o1.putAll(o2);
            return o1;
        });
        System.out.println(map.get("a").get("A"));
//        map.entrySet().stream().forEach(entry->{
//            System.out.println(entry.getKey());
//
//        });
    }

    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }



}
