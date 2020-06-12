package network.nerve.dex.storage;

import io.nuls.core.exception.NulsException;
import network.nerve.dex.model.po.NonceOrderPo;

public interface NonceOrderStorageService {

    NonceOrderPo query(String key) throws NulsException;

    void save(String key, NonceOrderPo orderPo) throws Exception;

    void delete(String key) throws Exception;
}
