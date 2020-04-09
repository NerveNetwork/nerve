package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import io.nuls.api.constant.DBTableConstant;
import io.nuls.api.db.BlockTimeService;
import io.nuls.api.model.po.BlockTimeInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.bson.Document;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-04 16:20
 * @Description: 功能描述
 */
@Component
public class MongoBlockTimeServiceImpl implements BlockTimeService {

    @Autowired
    private MongoDBService mongoDBService;

    @Override
    public void save(int chainId, BlockTimeInfo info) {
        if(info == null){
            return ;
        }
        Document document = mongoDBService.findOne(DBTableConstant.BLOCK_TIME_TABLE,Filters.eq("_id",chainId));
        if(document == null){
            mongoDBService.insertOne(DBTableConstant.BLOCK_TIME_TABLE,DocumentTransferTool.toDocument(info,"chainId"));
        }else{
            mongoDBService.update(DBTableConstant.BLOCK_TIME_TABLE,Filters.eq("_id",chainId),DocumentTransferTool.toDocument(info,"chainID"));
        }
    }

    @Override
    public BlockTimeInfo get(int chainId) {
        Document document = mongoDBService.findOne(DBTableConstant.BLOCK_TIME_TABLE,Filters.eq("_id",chainId));
        if (document == null) {
            return null;
        }
        BlockTimeInfo blockTimeInfo = DocumentTransferTool.toInfo(document, "chainId", BlockTimeInfo.class);
        return blockTimeInfo;
    }
}
