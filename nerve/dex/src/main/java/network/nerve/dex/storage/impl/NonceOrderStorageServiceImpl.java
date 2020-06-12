package network.nerve.dex.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.dex.context.DexDBConstant;
import network.nerve.dex.model.po.NonceOrderPo;
import network.nerve.dex.storage.NonceOrderStorageService;

@Component
public class NonceOrderStorageServiceImpl implements NonceOrderStorageService {
    @Override
    public NonceOrderPo query(String key) throws NulsException {
        byte[] value = RocksDBService.get(DexDBConstant.DB_NAME_NONCE_ORDER, key.getBytes());
        if (value == null) {
            return null;
        }
        NonceOrderPo orderPo = new NonceOrderPo();
        orderPo.parse(new NulsByteBuffer(value));
        return orderPo;
    }

    @Override
    public void save(String key, NonceOrderPo orderPo) throws Exception {
        RocksDBService.put(DexDBConstant.DB_NAME_NONCE_ORDER, key.getBytes(), orderPo.serialize());
    }

    @Override
    public void delete(String key) throws Exception {
        RocksDBService.delete(DexDBConstant.DB_NAME_NONCE_ORDER, key.getBytes());
    }
}
