package io.nuls.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.dex.model.po.TradingDealPo;

public interface TradingDealStorageService {

    void save(TradingDealPo po)throws Exception;

    TradingDealPo query(NulsHash hash)throws Exception;

    void delete(NulsHash hash)throws Exception;
}
