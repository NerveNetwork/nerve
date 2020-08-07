package io.nuls.api.db.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Sorts;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.db.PunishService;
import io.nuls.api.db.RoundService;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.PocRoundItem;
import io.nuls.api.model.po.PunishLogInfo;
import io.nuls.api.model.po.TxDataInfo;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static io.nuls.api.constant.DBTableConstant.PUNISH_TABLE;

@Component
public class MongoPunishServiceImpl implements PunishService {

    @Autowired
    private MongoDBService mongoDBService;

    @Autowired
    RoundService roundService;

    public void savePunishList(int chainId, List<PunishLogInfo> punishLogList) {
        if (punishLogList.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (PunishLogInfo punishLog : punishLogList) {
            punishLog.set_id(punishLog.getTxHash() + "_" + punishLog.getAddress() + "_" + punishLog.getRoundIndex());
            documentList.add(DocumentTransferTool.toDocument(punishLog));
        }
        mongoDBService.insertOrUpdate(PUNISH_TABLE + chainId,documentList);
    }

    public List<TxDataInfo> getYellowPunishLog(int chainId, String txHash) {
        List<Document> documentList = mongoDBService.query(PUNISH_TABLE + chainId, Filters.and(Filters.eq("txHash", txHash),Filters.eq("type", ApiConstant.PUBLISH_YELLOW)));
        List<TxDataInfo> punishLogs = new ArrayList<>();
        for (Document document : documentList) {
            PunishLogInfo punishLog = DocumentTransferTool.toInfo(document,PunishLogInfo.class);
            punishLogs.add(punishLog);
        }
        return punishLogs;
    }


    public PunishLogInfo getRedPunishLog(int chainId, String txHash) {
        Document document = mongoDBService.findOne(PUNISH_TABLE + chainId, Filters.and(Filters.eq("txHash", txHash),Filters.eq("type", ApiConstant.PUBLISH_RED)));
        if (document == null) {
            return null;
        }
        PunishLogInfo punishLog = DocumentTransferTool.toInfo(document, PunishLogInfo.class);
        return punishLog;
    }

    public long getYellowCount(int chainId, String agentAddress) {
        Bson filter = and(eq("type", ApiConstant.PUBLISH_YELLOW), eq("address", agentAddress));
        long count = mongoDBService.getCount(PUNISH_TABLE + chainId, filter);
        return count;
    }

    public PageInfo<PunishLogInfo> getPunishLogList(int chainId, int type, String address, int pageIndex, int pageSize) {
        Bson filter = null;

        if (type == 0 && !StringUtils.isBlank(address)) {
            filter = Filters.eq("address", address);
        } else if (type > 0 && StringUtils.isBlank(address)) {
            filter = Filters.eq("type", type);
        } else if (type > 0 && !StringUtils.isBlank(address)) {
            filter = Filters.and(eq("type", type), eq("address", address));
        }

        long totalCount = mongoDBService.getCount(PUNISH_TABLE + chainId, filter);
        List<Document> documentList = mongoDBService.pageQuery(PUNISH_TABLE + chainId, filter, Sorts.descending("time"), pageIndex, pageSize);
        List<PunishLogInfo> punishLogList = new ArrayList<>();
        for (Document document : documentList) {
            punishLogList.add(DocumentTransferTool.toInfo(document, PunishLogInfo.class));
        }
        PageInfo<PunishLogInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, punishLogList);
        return pageInfo;
    }


    public void rollbackPunishLog(int chainID, List<String> txHashs, long height) {
        if (txHashs.isEmpty()) {
            return;
        }
        mongoDBService.delete(PUNISH_TABLE + chainID, Filters.eq("blockHeight", height));
    }
}
