package io.nuls.dex.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.dex.context.DexDBConstant;
import io.nuls.dex.model.po.TradingDealPo;
import io.nuls.dex.storage.TradingDealStorageService;

@Component
public class TradingDealStorageServiceImpl implements TradingDealStorageService {
    @Override
    public void save(TradingDealPo po) throws Exception {
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_DEAL, po.getDealHash().getBytes(), po.serialize());
    }

    @Override
    public TradingDealPo query(NulsHash hash) throws Exception {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_TRADING_DEAL, hash.getBytes());
        if (value == null) {
            return null;
        }
        TradingDealPo po = new TradingDealPo();
        po.parse(new NulsByteBuffer(value));
        po.setTradingHash(hash);
        return po;
    }

    @Override
    public void delete(NulsHash hash) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_DEAL, hash.getBytes());
    }
}
