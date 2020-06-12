package network.nerve.dex.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.dex.context.DexDBConstant;
import network.nerve.dex.model.txData.CancelDeal;
import network.nerve.dex.storage.TradingOrderCancelStorageService;
import network.nerve.dex.util.LoggerUtil;

@Component
public class TradingOrderCancelStorageServiceImpl implements TradingOrderCancelStorageService {

    @Override
    public void save(CancelDeal po) throws Exception {
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_ORDER_CANCEL, po.getOrderHash(), po.serialize());
    }

    @Override
    public void delete(byte[] hash) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_ORDER_CANCEL, hash);
    }

    @Override
    public CancelDeal query(byte[] hash) {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_TRADING_ORDER, hash);
        if (value == null) {
            return null;
        }
        CancelDeal cancelDeal = new CancelDeal();
        try {
            cancelDeal.parse(new NulsByteBuffer(value));
            return cancelDeal;
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return null;
        }
    }
}
