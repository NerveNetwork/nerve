package network.nerve.dex.manager;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.bean.AssetInfo;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.DexUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DexModule Manager
 */
@Component
public class DexManager {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private CoinTradingStorageService tradingStorageService;
    @Autowired
    private TradingOrderStorageService orderStorageService;

    /**
     * Store all registered asset information in this chain
     * Information on registered cross chain assets and other chain assets
     */
    private Map<String, AssetInfo> assetInfoMap = new ConcurrentHashMap<>();

    /**
     * Transaction pair container cache
     * Cache all transaction pair information, as well as the position information of transaction pairs
     * key: coinTradingPo.hash
     */
    private Map<String, TradingContainer> tradingContainerMap = new ConcurrentHashMap<>();

    /**
     * Store all transaction pairskey, can be achieved through currency pairsidFind the right transactionhash
     * Re passtradingContainerMapFind the correspondingTradingContainer
     * key: Coin pairsid
     * value: coinTradingPo.hash
     */
    private Map<String, String> tradingKeyMap = new ConcurrentHashMap<>();

    public void addAssetInfo(AssetInfo assetInfo) {
        assetInfoMap.put(assetInfo.getKey(), assetInfo);
    }

    public AssetInfo getAssetInfo(String assetKey) {
        return assetInfoMap.get(assetKey);
    }

    /**
     * Through currency pairshash, find the coin pair container
     *
     * @param hash
     * @return
     */
    public TradingContainer getTradingContainer(String hash) {
        return tradingContainerMap.get(hash);
    }


    public Map<String, TradingContainer> getAllContainer() {
        return tradingContainerMap;
    }

    /**
     * Add a disk slot
     *
     * @param container
     */
    public void addContainer(TradingContainer container) {
        CoinTradingPo tradingPo = container.getCoinTrading();
        String hash = container.getCoinTrading().getHash().toHex();
        tradingContainerMap.put(hash, container);
        //Cache coin pairsid
        tradingKeyMap.put(DexUtil.getCoinTradingKey1(tradingPo), hash);
    }

    /**
     * Add a coin pair information and create a disk cache container
     *
     * @param tradingPo
     */
    public void addCoinTrading(CoinTradingPo tradingPo) {
        TradingContainer container = new TradingContainer(tradingPo);
        String hash = container.getCoinTrading().getHash().toHex();
        tradingContainerMap.put(hash, container);
        //Cache coin pairsid
        tradingKeyMap.put(DexUtil.getCoinTradingKey1(tradingPo), hash);
    }

    /**
     * Delete coin pair cache container
     *
     * @param tradingPo
     */
    public void deleteCoinTrading(CoinTradingPo tradingPo) throws NulsException {
        String hash = tradingPo.getHash().toHex();
        TradingContainer container = getTradingContainer(hash);
        if (container == null) {
            return;
        }
        if (!container.getBuyOrderList().isEmpty() || !container.getSellOrderList().isEmpty()) {
            throw new NulsException(DexErrorCode.TRADING_MORE_ORDER_EXIST);
        }
        tradingKeyMap.remove(DexUtil.getCoinTradingKey1(tradingPo));
        tradingContainerMap.remove(hash);
    }

    /**
     * Through currency pairsidFind the right transactionhash
     *
     * @param key
     * @return
     */
//    public String getCoinTradingHash(String key) {
//        return tradingKeyMap.get(key);
//    }

    /**
     * Based on currency pairsidOr currency pairshashCheck if the currency pair exists
     *
     * @param key
     * @return
     */
    public boolean containsCoinTrading(String key) {
        if (tradingContainerMap.containsKey(key)) {
            return true;
        }
        return tradingKeyMap.containsKey(key);
    }

    /**
     * Add order information to the corresponding currency pair opening
     *
     * @param orderPo
     */
    public void addTradingOrder(TradingOrderPo orderPo) throws NulsException {
        TradingContainer container = tradingContainerMap.get(orderPo.getTradingHash().toHex());
        if (container == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "CoinTrading not exist");
        }
        container.addTradingOrder(orderPo);
    }

    /**
     * Delete pending orders
     *
     * @param orderPo
     * @throws NulsException
     */
    public void removeTradingOrder(TradingOrderPo orderPo) throws NulsException {
        TradingContainer container = tradingContainerMap.get(orderPo.getTradingHash().toHex());
        if (container == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "CoinTrading not exist");
        }
        container.removeTradingOrder(orderPo);
    }

    /**
     * initializationDexModule Manager
     */
    public void init() throws NulsException {
        //Query all transaction pair information and create corresponding positions
        List<CoinTradingPo> tradingList = tradingStorageService.queryAll();
        for (CoinTradingPo tradingPo : tradingList) {
            this.addCoinTrading(tradingPo);
        }
        //Query all order information and cache it to the disk port
        List<TradingOrderPo> orderList = orderStorageService.queryAll();
        Collections.sort(orderList);
        for (TradingOrderPo orderPo : orderList) {
            this.addTradingOrder(orderPo);
        }
    }

    public void clear() {
        assetInfoMap.clear();
        tradingContainerMap.clear();
        tradingKeyMap.clear();
    }

}
