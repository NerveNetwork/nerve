package network.nerve.dex.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.dex.context.DexDBConstant;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

@Component
public class TradingOrderStorageServiceImpl implements TradingOrderStorageService {

    @Override
    public void save(TradingOrderPo po) throws Exception {
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_ORDER, po.getOrderHash().getBytes(), po.serialize());
    }

    @Override
    public void delete(NulsHash orderHash) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_ORDER, orderHash.getBytes());
    }

    /**
     * When the order is cancelled or the order transaction has been completed, the order data needs to be removed from the order table
     * Make backups simultaneously
     *
     * @param po
     * @throws Exception
     */
    @Override
    public void stop(TradingOrderPo po) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_ORDER, po.getOrderHash().getBytes());
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_ORDER_BACK, po.getOrderHash().getBytes(), po.serialize());
    }

    @Override
    public void rollbackStop(TradingOrderPo po) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_ORDER_BACK, po.getOrderHash().getBytes());
        RocksDBService.put(DexDBConstant.DB_NAME_TRADING_ORDER, po.getOrderHash().getBytes(), po.serialize());
    }

    @Override
    public TradingOrderPo query(byte[] orderHash) {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_TRADING_ORDER, orderHash);
        if (value == null) {
            return null;
        }
        TradingOrderPo orderPo = new TradingOrderPo();
        try {
            orderPo.parse(new NulsByteBuffer(value));
            orderPo.setOrderHash(new NulsHash(orderHash));
            return orderPo;
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return null;
        }
    }

    @Override
    public TradingOrderPo queryFromBack(byte[] orderHash) {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_TRADING_ORDER_BACK, orderHash);
        if (value == null) {
            return null;
        }
        TradingOrderPo orderPo = new TradingOrderPo();
        try {
            orderPo.parse(new NulsByteBuffer(value));
            orderPo.setOrderHash(new NulsHash(orderHash));
            return orderPo;
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return null;
        }
    }

    @Override
    public void deleteBackData(byte[] orderHash) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_TRADING_ORDER_BACK, orderHash);
    }

    @Override
    public List<TradingOrderPo> queryAll() throws NulsException {
        List<TradingOrderPo> orderPoList = new ArrayList<>();
        List<Entry<byte[], byte[]>> list = RocksDBService.entryList(DexDBConstant.DB_NAME_TRADING_ORDER);
        if (list != null && !list.isEmpty()) {
            for (Entry<byte[], byte[]> entry : list) {
                TradingOrderPo orderPo = new TradingOrderPo();
                orderPo.parse(new NulsByteBuffer(entry.getValue()));
                NulsHash hash = new NulsHash(entry.getKey());
                orderPo.setOrderHash(hash);
                orderPoList.add(orderPo);
            }
        }
        return orderPoList;
    }

    @Override
    public void saveHeight(long height){
        try{
            RocksDBService.put(DexDBConstant.DB_NAME_HEIGHT, DexDBConstant.DB_NAME_HEIGHT.getBytes(), SerializeUtils.uint64ToByteArray(height));
        }catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    @Override
    public long getHeight() {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_HEIGHT, DexDBConstant.DB_NAME_HEIGHT.getBytes());
        if (value != null) {
            long height = SerializeUtils.readUint64(value, 0);
            return height;
        }
        return 0;
    }
}
