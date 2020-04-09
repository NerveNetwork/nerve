package io.nuls.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.dex.model.po.TradingOrderPo;

import java.util.List;

public interface TradingOrderStorageService {

    void save(TradingOrderPo po) throws Exception;

    void delete(NulsHash orderHash) throws Exception;

    void stop(TradingOrderPo po) throws Exception;

    void rollbackStop(TradingOrderPo po) throws Exception;

    TradingOrderPo query(byte[] orderHash);

    TradingOrderPo queryFromBack(byte[] orderHash);

    List<TradingOrderPo> queryAll() throws NulsException;
}
