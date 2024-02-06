package network.nerve.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.model.po.CoinTradingEditInfoPo;
import network.nerve.dex.model.po.CoinTradingPo;

import java.util.List;

/**
 * Coin to coin storage services
 */
public interface CoinTradingStorageService {

    /**
     * Store a coin pair information
     *
     * @param tradingPo
     * @return
     */
    void save(CoinTradingPo tradingPo) throws Exception;

    /**
     * Based on the transaction of creating currency pairshashQuery a transaction pair information
     *
     * @param hash
     * @return
     */
    CoinTradingPo query(NulsHash hash) throws NulsException;

    /**
     * Based on currency pairsidQuery currency pair information
     *
     * @param coinTradingKey
     * @return
     */
    CoinTradingPo query(String coinTradingKey);

    /**
     * Query all currency pair information
     *
     * @return
     */
    List<CoinTradingPo> queryAll() throws NulsException;


    void delete(CoinTradingPo tradingPo) throws Exception;


    void saveEditInfo(NulsHash tradingHash, CoinTradingEditInfoPo editInfoPo) throws Exception;

    CoinTradingEditInfoPo queryEditInfoPo(NulsHash tradingHash) throws Exception;

    void deleteEditInfo(NulsHash tradingHash) throws Exception;
}
