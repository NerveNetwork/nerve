package network.nerve.dex.storage;

import network.nerve.dex.model.txData.CancelDeal;

public interface TradingOrderCancelStorageService {

    void save(CancelDeal po) throws Exception;

    void delete(byte[] hash) throws Exception;

    CancelDeal query(byte[] hash);
}
