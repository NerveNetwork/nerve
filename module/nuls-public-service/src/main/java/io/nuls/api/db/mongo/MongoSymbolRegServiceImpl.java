package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.constant.config.SymbolBaseInfoConfig;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.HeterogeneousAssetCollectionDTO;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
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

    @Override
    public void init() {
        //获取模块配置的币种信息
        List<SymbolRegInfo> configSymbolList = symbolBaseInfoConfig.getSymbolBaseInfoList();
        List<HeterogeneousAssetCollectionDTO> converterSymbolList = WalletRpcHandler.getAllHeterogeneousChainAssetList().getData();
        Map<String,SymbolRegInfo> symbolInfoMap = new HashMap<>();
        configSymbolList.stream().forEach(d-> {
            d.buildId();
            symbolInfoMap.put(d.get_id(),d);
        });
        converterSymbolList.forEach(dto->{
            dto.getAssetList().stream().map(d->{
                d.buildId();
                return Map.of(d.get_id(),d);
            }).reduce(symbolInfoMap,(m1,m2)->{
                m2.entrySet().forEach(entry->{
                    m1.merge(entry.getKey(),entry.getValue(),(ov,nv)->{
                        ov.setSymbol(ov.getSymbol() == null ? nv.getSymbol() : ov.getSymbol());
                        ov.setSource(ov.getSource() == null ? ApiConstant.SYMBOL_REG_SOURCE_CONVERTER : ov.getSource());
                        ov.setDecimals(ov.getDecimals() == null ? nv.getDecimals() : ov.getDecimals());
                        return ov;
                    });
                });
                return m1;
            });
        });
        symbolInfoMap.values().forEach(symbolRegInfo -> {
            this.save(symbolRegInfo);
        });
    }

    @Override
    public void save(SymbolRegInfo info) {
        Objects.requireNonNull(info.getChainId(),"assetChainId can't be null");
        Objects.requireNonNull(info.getAssetId(),"assetId can't be null");
        Objects.requireNonNull(info.getSymbol(),"symbol can't be null");
        info.buildId();
        Document oldInfo = mongoDBService.findOne(SYMBOL_REG_TABLE,Filters.eq("_id",info.get_id()));
        if(oldInfo != null){
            info.setIcon(info.getIcon() == null ? oldInfo.getString("icon") : info.getIcon());
        }
        mongoDBService.insertOrUpdate(SYMBOL_REG_TABLE, DocumentTransferTool.toDocument(info));
    }

    @Override
    public SymbolRegInfo get(int assetChainId, int assetId) {
        Document document = mongoDBService.findOne(DBTableConstant.SYMBOL_REG_TABLE, Filters.eq("_id",SymbolRegInfo.buildId(assetChainId,assetId)));
        if (document == null) {
            return null;
        }
        SymbolRegInfo symbolRegInfo = DocumentTransferTool.toInfo(document, SymbolRegInfo.class);
        return symbolRegInfo;
    }

    @Override
    public List<SymbolRegInfo> get(String symbol) {
        List<Document> document = mongoDBService.query(DBTableConstant.SYMBOL_REG_TABLE, Filters.eq("symbol",symbol), Sorts.ascending("level"));
        if (document == null) {
            return List.of();
        }
        return document.stream().map(d->DocumentTransferTool.toInfo(d, SymbolRegInfo.class)).collect(Collectors.toList());
    }

    @Override
    public Optional<SymbolRegInfo> getFirst(String symbol) {
        List<SymbolRegInfo> list = get(symbol);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<SymbolRegInfo> getAll() {
        return mongoDBService.query(SYMBOL_REG_TABLE).stream().map(d-> DocumentTransferTool.toInfo(d,SymbolRegInfo.class)).collect(Collectors.toList());
    }


    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }
}
