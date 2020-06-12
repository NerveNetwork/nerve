package io.nuls.api.db;

import io.nuls.api.model.po.AssetSnapshotInfo;
import io.nuls.api.model.po.ChainStatisticalInfo;
import io.nuls.api.model.po.StatisticalInfo;
import org.bson.Document;

import java.util.List;

public interface StatisticalService {

    long getBestId(int chainId);

    void saveBestId(int chainId, long id);

    void updateBestId(int chainId, long id);

    void insert(int chainId, StatisticalInfo info);

    long calcTxCount(int chainId, long start, long end);

    List getStatisticalList(int chainId, int type, String field,int timeZoom);

    List<Document> getStatisticalList(int chainId, int type);

    List<AssetSnapshotInfo> getAssetSnapshotAggSum(int chainId, int type);

    StatisticalInfo getLastStatisticalInfo(int chainId);

    ChainStatisticalInfo getChainStatisticalInfo(int chainId);

    void saveChainStatisticalInfo(ChainStatisticalInfo statisticalInfo);

}
