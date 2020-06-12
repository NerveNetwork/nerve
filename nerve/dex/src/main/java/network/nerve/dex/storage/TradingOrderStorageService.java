package network.nerve.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.model.po.TradingOrderPo;

import java.util.List;

public interface TradingOrderStorageService {

    void save(TradingOrderPo po) throws Exception;

    void delete(NulsHash orderHash) throws Exception;

    void stop(TradingOrderPo po) throws Exception;

    void rollbackStop(TradingOrderPo po) throws Exception;

    TradingOrderPo query(byte[] orderHash);

    TradingOrderPo queryFromBack(byte[] orderHash);

    void deleteBackData(byte[] orderHash) throws Exception;

    List<TradingOrderPo> queryAll() throws NulsException;
}
