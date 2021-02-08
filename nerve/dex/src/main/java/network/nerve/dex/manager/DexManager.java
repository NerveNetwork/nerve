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
 * Dex模块管理器
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
     * 存放本链已注册的所有资产信息
     * 和已注册跨链的其他链资产信息
     */
    private Map<String, AssetInfo> assetInfoMap = new ConcurrentHashMap<>();

    /**
     * 交易对容器缓存
     * 缓存所有交易对信息，以及交易对的盘口信息
     * key: coinTradingPo.hash
     */
    private Map<String, TradingContainer> tradingContainerMap = new ConcurrentHashMap<>();

    /**
     * 存放所有交易对key，可通过币对id找到交易对的hash
     * 再通过tradingContainerMap找到对应的TradingContainer
     * key: 币对id
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
     * 通过币对hash，找到币对容器
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
     * 添加一个盘口
     *
     * @param container
     */
    public void addContainer(TradingContainer container) {
        CoinTradingPo tradingPo = container.getCoinTrading();
        String hash = container.getCoinTrading().getHash().toHex();
        tradingContainerMap.put(hash, container);
        //缓存币对id
        tradingKeyMap.put(DexUtil.getCoinTradingKey1(tradingPo), hash);
    }

    /**
     * 添加一个币对信息，并创建盘口缓存容器
     *
     * @param tradingPo
     */
    public void addCoinTrading(CoinTradingPo tradingPo) {
        TradingContainer container = new TradingContainer(tradingPo);
        String hash = container.getCoinTrading().getHash().toHex();
        tradingContainerMap.put(hash, container);
        //缓存币对id
        tradingKeyMap.put(DexUtil.getCoinTradingKey1(tradingPo), hash);
    }

    /**
     * 删除币对缓存容器
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
     * 通过币对id找到交易对的hash
     *
     * @param key
     * @return
     */
//    public String getCoinTradingHash(String key) {
//        return tradingKeyMap.get(key);
//    }

    /**
     * 根据币对id或者币对hash查询币对是否存在
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
     * 添加挂单信息到对应的币对盘口
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
     * 删除挂单
     *
     * @param orderPo
     * @throws NulsException
     */
    public void removeTradingOrder(TradingOrderPo orderPo) throws NulsException {
        if (orderPo == null) {
            return;
        }
        TradingContainer container = tradingContainerMap.get(orderPo.getTradingHash().toHex());
        if (container == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "CoinTrading not exist");
        }
        container.removeTradingOrder(orderPo);
    }

    /**
     * 初始化Dex模块管理器
     */
    public void init() throws NulsException {
        //查询所有交易对信息，创建对应盘口
        List<CoinTradingPo> tradingList = tradingStorageService.queryAll();
        for (CoinTradingPo tradingPo : tradingList) {
            this.addCoinTrading(tradingPo);
        }
        //查询所有挂单信息，缓存到盘口
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
