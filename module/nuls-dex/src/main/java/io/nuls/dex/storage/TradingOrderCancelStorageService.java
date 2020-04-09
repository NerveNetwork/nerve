package io.nuls.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.dex.model.po.TradingOrderCancelPo;

public interface TradingOrderCancelStorageService {

    void save(TradingOrderCancelPo po) throws Exception;

    void delete(NulsHash hash) throws Exception;

    TradingOrderCancelPo query(byte[] hash);
}
