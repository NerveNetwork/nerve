package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.ConverterTxType;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.ConverterTxService;
import io.nuls.api.model.po.ConverterTxInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static io.nuls.api.constant.DBTableConstant.CONVERTER_TX_TABLE;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-24 14:07
 * @Description: 功能描述
 */
@Component
public class MongoConverterTxServiceImpl implements ConverterTxService {

    @Autowired
    MongoDBService mongoDBService;

    @Override
    public void save(int chainId, ConverterTxInfo info) {
        Objects.requireNonNull(info.getTxHash(),"txHash can't be null");
        mongoDBService.insertOrUpdate(CONVERTER_TX_TABLE + chainId,DocumentTransferTool.toDocument(info,"txHash"));
    }

    @Override
    public List<ConverterTxInfo> queryList(int chainId, long startBlockHeight, long endBlockHeight) {
        List<Document> docList = mongoDBService.query(CONVERTER_TX_TABLE + chainId,
                Filters.and(
                        Filters.gte("blockHeight",startBlockHeight),
                        Filters.lte("blockHeight",endBlockHeight)
                )
        );
        if(docList == null || docList.isEmpty()){
            return List.of();
        }
        return DocumentTransferTool.toInfoList(docList,"txHash",ConverterTxInfo.class);
    }

    @Override
    public ConverterTxInfo getByTxHash(int chainId,String txHash) {
        Objects.requireNonNull(txHash,"txHash can't be null");
        Document doc = mongoDBService.findOne(CONVERTER_TX_TABLE + chainId,Filters.eq("_id",txHash));
        return doc == null ? null : DocumentTransferTool.toInfo(doc,"txHash",ConverterTxInfo.class);
    }

    @Override
    public Map<ConverterTxType,BigInteger> aggTotal(int chainId, int assetId) {
        Bson match = Filters.and(Filters.eq("assetChainId",chainId),Filters.eq("assetId",assetId));
        Map<ConverterTxType,BigInteger> res = new HashMap<>(2);
        mongoDBService.query(CONVERTER_TX_TABLE + chainId,match)
            .stream().forEach(doc->{
                ConverterTxInfo info = DocumentTransferTool.toInfo(doc,"txHash",ConverterTxInfo.class);
                ConverterTxType converterTxType = ConverterTxType.valueOf(info.getConverterType());
                res.compute(converterTxType,(key,old)->{
                    if(old == null){
                        return info.getAmount();
                    }else{
                        return info.getAmount().add(old);
                    }
                });
        });
        return res;
    }

    public void setMongoDBService(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }
}
