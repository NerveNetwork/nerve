package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.constant.config.ApiConfig;
import io.nuls.api.constant.config.SymbolBaseInfoConfig;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.HeterogeneousAssetCollectionDTO;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.api.utils.PropertyUtils;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

import static io.nuls.api.constant.DBTableConstant.SYMBOL_REG_TABLE;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 14:24
 * @Description:
 */
@Component
public class MongoSymbolRegServiceImpl implements SymbolRegService {

    @Autowired
    MongoDBService mongoDBService;

    @Autowired
    SymbolBaseInfoConfig symbolBaseInfoConfig;

    @Autowired
    ApiConfig apiConfig;

    @Override
    public void updateSymbolRegList(){
        //获取模块配置的币种信息
        List<SymbolRegInfo> configSymbolList = symbolBaseInfoConfig.getSymbolBaseInfoList();
        Result<List<SymbolRegInfo>> allSymbolList = WalletRpcHandler.getSymbolList(apiConfig.getChainId());
        if (allSymbolList.isFailed()) {
            Log.error("初始化资产信息发生异常:{}", allSymbolList.getMsg());
            System.exit(0);
        }
        List<SymbolRegInfo> list = allSymbolList.getData();
        Map<String, SymbolRegInfo> symbolInfoMap = new HashMap<>();
        configSymbolList.stream().forEach(d -> {
            d.buildId();
            symbolInfoMap.put(d.get_id(), d);
        });
        list.stream().map(d -> {
            d.buildId();
            d.setLevel(1);
            if(StringUtils.isBlank(d.getFullName())){
                d.setFullName(d.getSymbol());
            }
            return Map.of(d.get_id(), d);
        }).reduce(symbolInfoMap, (m1, m2) -> {
            m2.entrySet().forEach(entry -> {
                m1.merge(entry.getKey(), entry.getValue(), (ov, nv) -> {
                    PropertyUtils.copyNotNullProperties(nv,ov);
                    if(nv.getChainId() == apiConfig.getChainId() && nv.getAssetId() == apiConfig.getAssetId()){
                        nv.setSource(ApiConstant.SYMBOL_REG_SOURCE_NATIVE);
                    }
                    return nv;
                });
            });
            return m1;
        });
        symbolInfoMap.values().forEach(this::save);
    }

    @Override
    public void save(SymbolRegInfo info) {
        Objects.requireNonNull(info.getChainId(), "assetChainId can't be null");
        Objects.requireNonNull(info.getAssetId(), "assetId can't be null");
        Objects.requireNonNull(info.getSymbol(), "symbol can't be null");
        info.buildId();
        Document oldInfo = mongoDBService.findOne(SYMBOL_REG_TABLE, Filters.eq("_id", info.get_id()));
        if (oldInfo != null) {
            info.setIcon(info.getIcon() == null ? oldInfo.getString("icon") : info.getIcon());
        }
        mongoDBService.insertOrUpdate(SYMBOL_REG_TABLE, DocumentTransferTool.toDocument(info));
    }

    @Override
    public SymbolRegInfo get(int assetChainId, int assetId) {
        return get(assetChainId,assetId,true);
    }

    public SymbolRegInfo get(int assetChainId, int assetId,boolean first) {
        Document document = mongoDBService.findOne(DBTableConstant.SYMBOL_REG_TABLE, Filters.eq("_id", SymbolRegInfo.buildId(assetChainId, assetId)));
        if (document == null) {
            if(first){
                this.updateSymbolRegList();
                return get(assetChainId,assetId,false);
            }else{
                return null;
            }
        }
        SymbolRegInfo symbolRegInfo = DocumentTransferTool.toInfo(document, SymbolRegInfo.class);
        return symbolRegInfo;
    }

    @Override
    public List<SymbolRegInfo> get(String symbol) {
        return get(symbol,true);
    }

    public List<SymbolRegInfo> get(String symbol,boolean first) {
        List<Document> document = mongoDBService.query(DBTableConstant.SYMBOL_REG_TABLE, Filters.eq("symbol", symbol), Sorts.ascending("level"));
        if (document == null) {
            if(first){
                updateSymbolRegList();
                return get(symbol,false);
            }else{
                return List.of();
            }
        }
        return document.stream().map(d -> DocumentTransferTool.toInfo(d, SymbolRegInfo.class)).collect(Collectors.toList());
    }

    @Override
    public Optional<SymbolRegInfo> getFirst(String symbol) {
        List<SymbolRegInfo> list = get(symbol);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<SymbolRegInfo> getAll() {
        return mongoDBService.query(SYMBOL_REG_TABLE).stream().map(d -> DocumentTransferTool.toInfo(d, SymbolRegInfo.class)).collect(Collectors.toList());
    }

    @Override
    public List<SymbolRegInfo> getListBySource(Integer... source) {
        if(source.length == 0){
            return getAll();
        }
        return mongoDBService.query(SYMBOL_REG_TABLE,Filters.or(Arrays.stream(source).map(d->Filters.eq("source",d)).collect(Collectors.toList()))).stream().map(d -> DocumentTransferTool.toInfo(d, SymbolRegInfo.class)).collect(Collectors.toList());
    }


    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }
}
