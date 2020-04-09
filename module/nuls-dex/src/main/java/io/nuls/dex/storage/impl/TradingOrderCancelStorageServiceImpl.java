package io.nuls.dex.storage.impl;

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.dex.context.DexDBConstant;
import io.nuls.dex.model.po.TradingOrderCancelPo;
import io.nuls.dex.storage.TradingOrderCancelStorageService;

@Component
public class TradingOrderCancelStorageServiceImpl implements TradingOrderCancelStorageService {

    @Override
    public void save(TradingOrderCancelPo po) throws Exception {
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_ORDER_CANCEL, po.getHash().getBytes(), po.serialize());
    }

    @Override
    public void delete(NulsHash hash) throws Exception {

    }

    @Override
    public TradingOrderCancelPo query(byte[] hash) {
        return null;
    }
}
