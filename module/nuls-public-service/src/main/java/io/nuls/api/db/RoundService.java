package io.nuls.api.db;

import io.nuls.api.model.po.PocRound;
import io.nuls.api.model.po.PocRoundItem;

import java.util.List;

public interface RoundService {

    PocRound getRound(int chainId, long roundIndex);

    PocRoundItem getRoundItem(int chainId, long roundIndex,int packageIndex);

    List<PocRoundItem> getRoundItemList(int chainId, long roundIndex);

    void saveRound(int chainId, PocRound round);

    long updateRound(int chainId, PocRound round);

    long updateRoundItem(int chainId, PocRoundItem item);

    void saveRoundItemList(int chainId, List<PocRoundItem> itemList);

    void removeRound(int chainId, long roundIndex);

    long getTotalCount(int chainId);

    List<PocRound> getRoundList(int chainId, int pageIndex, int pageSize);

    void setRoundItemYellow(int chainId, long roundIndex, int orderIndex, String agentHash,int count);

    void setRoundItemRed(int chainId, long roundIndex, int orderIndex, String agentHash);
}
