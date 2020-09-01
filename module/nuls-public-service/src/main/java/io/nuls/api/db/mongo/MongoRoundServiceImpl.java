package io.nuls.api.db.mongo;

import com.mongodb.client.model.*;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.db.RoundService;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.CurrentRound;
import io.nuls.api.model.po.PocRound;
import io.nuls.api.model.po.PocRoundItem;
import io.nuls.api.utils.DocumentTransferTool;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static io.nuls.api.constant.DBTableConstant.ROUND_ITEM_TABLE;
import static io.nuls.api.constant.DBTableConstant.ROUND_TABLE;

@Component
public class MongoRoundServiceImpl implements RoundService {

    @Autowired
    private MongoDBService mongoDBService;

    public PocRound getRound(int chainId, long roundIndex) {
        Document document = this.mongoDBService.findOne(ROUND_TABLE + chainId, eq("_id", roundIndex));
        if (null == document) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "index", PocRound.class);
    }

    @Override
    public PocRoundItem getRoundItem(int chainId, long roundIndex, int packageIndex) {
        Document document = this.mongoDBService.findOne(ROUND_ITEM_TABLE + chainId, eq("_id", roundIndex + "_" + packageIndex));
        if (null == document) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "id", PocRoundItem.class);
    }

    public List<PocRoundItem> getRoundItemList(int chainId, long roundIndex) {
        List<Document> list = this.mongoDBService.query(ROUND_ITEM_TABLE + chainId, eq("roundIndex", roundIndex), Sorts.ascending("order"));
        List<PocRoundItem> itemList = new ArrayList<>();
        for (Document document : list) {
            itemList.add(DocumentTransferTool.toInfo(document, "id", PocRoundItem.class));
        }
        return itemList;
    }

    public void saveRound(int chainId, PocRound round) {
        Document document = DocumentTransferTool.toDocument(round, "index");
        this.mongoDBService.insertOrUpdate(ROUND_TABLE + chainId, document);
    }

    public long updateRound(int chainId, PocRound round) {
        Document document = DocumentTransferTool.toDocument(round, "index");
        return this.mongoDBService.updateOne(ROUND_TABLE + chainId, eq("_id", round.getIndex()), document);
    }

    public long updateRoundItem(int chainId, PocRoundItem item) {
        Document document = DocumentTransferTool.toDocument(item, "id");
        return this.mongoDBService.updateOne(ROUND_ITEM_TABLE + chainId, eq("_id", item.getId()), document);
    }

    public void saveRoundItemList(int chainId, List<PocRoundItem> itemList) {
        List<Document> docsList = new ArrayList<>();
        for (PocRoundItem item : itemList) {
            Document document = DocumentTransferTool.toDocument(item, "id");
            docsList.add(document);
        }
        try {
            this.mongoDBService.insertOrUpdate(ROUND_ITEM_TABLE + chainId, docsList);
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
    }

    public void removeRound(int chainId, long roundIndex) {
        this.mongoDBService.delete(ROUND_TABLE + chainId, eq("_id", roundIndex));
        this.mongoDBService.delete(ROUND_ITEM_TABLE + chainId, eq("roundIndex", roundIndex));
    }

    public long getTotalCount(int chainId) {
        return this.mongoDBService.getCount(ROUND_TABLE + chainId);
    }

    public List<PocRound> getRoundList(int chainId, int pageIndex, int pageSize) {
        List<Document> list = this.mongoDBService.pageQuery(ROUND_TABLE + chainId, Sorts.descending("_id"), pageIndex, pageSize);
        List<PocRound> roundList = new ArrayList<>();
        for (Document document : list) {
            roundList.add(DocumentTransferTool.toInfo(document, "index", PocRound.class));
        }
        return roundList;
    }

    @Override
    public void setRoundItemYellow(int chainId, long roundIndex, int orderIndex, String agentHash,int count) {
        Bson bson = Filters.and(
                Filters.eq("agentHash", agentHash),
                Filters.eq("yellow", false),
                Filters.eq("blockHeight", 0));
        Bson sort = Sorts.descending("roundIndex");
        List<Document> list = mongoDBService.pageQuery(ROUND_ITEM_TABLE + chainId, bson,sort,0,count);
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        list.forEach(d -> {
            d.put("yellow", true);
            long modifyCount = mongoDBService.update(ROUND_ITEM_TABLE + chainId, Filters.eq("_id", d.get("_id")), d);
            if (modifyCount > 0) {
                PocRound round = getRound(chainId, (Long) d.get("roundIndex"));
                ApiCache apiCache = CacheManager.getCache(chainId);
                CurrentRound currentRound = apiCache.getCurrentRound();
                if(currentRound != null && currentRound.getIndex() == round.getIndex()){
                    currentRound.setYellowCardCount((int) (currentRound.getYellowCardCount() + modifyCount));
                }
                int yellowCardCount = (int) (round.getYellowCardCount() + modifyCount);
                round.setYellowCardCount(yellowCardCount);
                saveRound(chainId, round);
            }
        });
    }


    @Override
    public void setRoundItemRed(int chainId, long roundIndex, int orderIndex, String agentHash) {
        Bson bson = Filters.and(
                Filters.eq("agentHash", agentHash),
                Filters.eq("yellow", false),
                Filters.eq("blockHeight", 0));
        Bson sort = Sorts.descending("roundIndex");
        Optional<Document> item = mongoDBService.query(ROUND_ITEM_TABLE + chainId, bson,sort).stream().findFirst();
        item.ifPresent(d -> {
            PocRound round = getRound(chainId, (Long) d.get("roundIndex"));
            int redCardCount = (round.getRedCardCount() + 1);
            round.setRedCardCount(redCardCount);
            saveRound(chainId, round);
        });
    }


}
