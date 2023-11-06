/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter.core.context;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.util.DBUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.core.heterogeneous.register.HeterogeneousChainRegister;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.utils.ConverterDBUtil;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.converter.constant.ConverterConstant.FIRST_HETEROGENEOUS_ASSET_CHAIN_ID;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
@Component
public class HeterogeneousChainManager {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    @Autowired
    private HeterogeneousChainRegister heterogeneousChainRegister;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    private boolean isInited = false;

    private final Map<String, Integer> heterogeneousChainIdRuleMap = new HashMap<>();

    private Map<Integer, HeterogeneousChainInfo> heterogeneousChainMap = new ConcurrentHashMap<>();

    public HeterogeneousChainInfo getHeterogeneousChainByChainId(Integer chainId) {
        return heterogeneousChainMap.get(chainId);
    }

    private NulsLogger logger() {
        return chainManager.getChain(converterConfig.getChainId()).getLogger();
    }

    /**
     * 根据异构链名称获取chainId
     */
    public int getHeterogeneousChainIdByName(String heterogeneousChainName) throws NulsException {
        heterogeneousChainName = heterogeneousChainName.trim().toLowerCase();
        Integer chainId = heterogeneousChainIdRuleMap.get(heterogeneousChainName);
        if (chainId == null) {
            logger().error("error heterogeneousChainName: {}", heterogeneousChainName);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAIN_NAME_ERROR);
        }
        return chainId;
    }

    public Map<Integer, HeterogeneousChainInfo> getHeterogeneousChainMap() {
        return heterogeneousChainMap;
    }

    /**
     * 异构链多签地址变更后，调用此函数持久化更新
     * 更新异构链多签地址
     */
    public void updateMultySignAddress(int heterogeneousChainId, String multySignAddress) throws NulsException {
        logger().info("持久化更新多签合约地址, heterogeneousChainId: {}, new: {}", heterogeneousChainId, multySignAddress);
        HeterogeneousChainInfo info = heterogeneousChainInfoStorageService.getHeterogeneousChainInfo(heterogeneousChainId);
        if (info == null) {
            logger().error("error heterogeneousChainId: {}", heterogeneousChainId);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        info.setMultySignAddress(multySignAddress);
        try {
            heterogeneousChainInfoStorageService.saveHeterogeneousChainInfo(heterogeneousChainId, info);
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
        heterogeneousChainMap.get(heterogeneousChainId).setMultySignAddress(multySignAddress);
    }

    /**
     * 初始化所有异构链信息
     */
    public void initHeterogeneousChainInfo() throws Exception {
        if (!isInited) {
            Chain chain = chainManager.getChain(converterConfig.getChainId());
            isInited = true;
            List<HeterogeneousChainInfo> storageList = heterogeneousChainInfoStorageService.getAllHeterogeneousChainInfoList();
            if (storageList != null && !storageList.isEmpty()) {
                storageList.stream().forEach(info -> {
                    if (info.getChainId() != 108 || !"T".equals(info.getMultySignAddress())) {
                        heterogeneousChainMap.put(info.getChainId(), info);
                    }
                });
            }

            Collection<Object> list = SpringLiteContext.getAllBeanList();
            List<IHeterogeneousChainRegister> registerList = new ArrayList<>();
            for (Object object : list) {
                if(object instanceof IHeterogeneousChainRegister) {
                    registerList.add((IHeterogeneousChainRegister) object);
                }
            }
            // 按指定顺序执行异构链注册
            registerList.sort(new Comparator<IHeterogeneousChainRegister>() {
                @Override
                public int compare(IHeterogeneousChainRegister o1, IHeterogeneousChainRegister o2) {
                    if (o1.order() > o2.order()) {
                        return 1;
                    } else if (o1.order() < o2.order()) {
                        return -1;
                    }
                    return 0;
                }
            });
            File dir = DBUtils.loadDataPath(converterConfig.getTxDataRoot());
            String dataPath = dir.getPath();
            for (IHeterogeneousChainRegister register : registerList) {
                int chainId = register.getChainId();
                HeterogeneousCfg heterogeneousCfg = chain.getHeterogeneousCfg(chainId, 1);
                if (heterogeneousCfg == null)
                    continue;
                // 执行异构链的初始化函数
                String dbName = register.init(heterogeneousCfg, chain.getLogger());
                // add by pierre at 2023/9/5 数据库合并
                do {
                    //检查此链DB是否已合并
                    boolean hadDBMerged = heterogeneousChainInfoStorageService.hadDBMerged(chainId);
                    if (hadDBMerged) {
                        logger().info("[{}]链DB[{}]已合并至[{}]", AssetName.getEnum(chainId).name(), dbName, ConverterDBConstant.DB_HETEROGENEOUS_CHAIN);
                        converterCoreApi.setDbMergedStatus(chainId);
                        break;
                    }
                    //检查在表列表中是否存在此链DB，不存在则使用合并表，此链DB打上已合并标记
                    RocksDB table = RocksDBManager.getTable(dbName);
                    if (table == null) {
                        logger().info("[{}]链DB使用合并表[{}]", AssetName.getEnum(chainId).name(), ConverterDBConstant.DB_HETEROGENEOUS_CHAIN);
                        heterogeneousChainInfoStorageService.markMergedChainDB(chainId);
                        converterCoreApi.setDbMergedStatus(chainId);
                        break;
                    }
                    byte[] prefix = ConverterDBUtil.stringToBytes(chainId + "_");
                    //此链DB执行合并
                    RocksIterator iterator = table.newIterator();
                    int count = 0;
                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        RocksDBManager.put(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, ArraysTool.concatenate(prefix, iterator.key()), iterator.value());
                        count++;
                    }
                    //此链DB打上已合并标记
                    logger().info("[{}]链DB[{}]已合并至[{}]，数目: {}", AssetName.getEnum(chainId).name(), dbName, ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, count);
                    heterogeneousChainInfoStorageService.markMergedChainDB(chainId);
                    converterCoreApi.setDbMergedStatus(chainId);
                    RocksDBManager.destroyTable(dbName);
                    File tableFile = new File(dataPath + File.separator + dbName);
                    tableFile.delete();
                } while (false);
                // end code by pierre
                HeterogeneousChainInfo chainInfo = register.getChainInfo();
                // 保存异构链symbol和chainId的关系
                heterogeneousChainIdRuleMap.put(chainInfo.getChainName(), chainId);
                String multySignAddress = chainInfo.getMultySignAddress();
                // 持久化存储异构链基本信息
                if (StringUtils.isNotBlank(multySignAddress) && !heterogeneousChainMap.containsKey(chainId)) {
                    try {
                        heterogeneousChainInfoStorageService.saveHeterogeneousChainInfo(chainId, chainInfo);
                    } catch (Exception e) {
                        throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
                    }
                    heterogeneousChainMap.put(chainId, chainInfo);
                }
                IHeterogeneousChainDocking docking = register.getDockingImpl();
                // 执行异构链注册
                HeterogeneousChainRegisterInfo registerInfo = heterogeneousChainRegister.register(converterConfig.getChainId(), chainId, docking);
                // 向异构链组件返回注册信息
                register.registerCallBack(registerInfo);
                // 读取最新的docking contract signature version
                docking.initialSignatureVersion();
                // 设置价格Key, 通过喂价模块获取价格
                AssetName assetName = AssetName.getEnum(chainId);
                if (assetName == null) {
                    throw new RuntimeException("Empty AssetName!");
                }
                ConverterContext.priceKeyMap.put(assetName.name(), heterogeneousCfg.getPriceKey());
                // 设置协议生效数据
                int protocolVersion = heterogeneousCfg.getProtocolVersion();
                if (protocolVersion == 0) {
                    continue;
                }
                Long protocolHeight = ConverterContext.protocolHeightMap.get(protocolVersion);
                if (protocolHeight == null) {
                    throw new RuntimeException("Empty protocolHeight!");
                }
                heterogeneousDockingManager.addProtocol(chainId, protocolHeight, heterogeneousCfg.getSymbol());
            }
            //执行数据库合并操作
            //遍历注册的链DB名称
            //Map<Integer, String> chainDBNameMap = converterCoreApi.chainDBNameMap();
            ////检查此链DB是否已合并
            //Set<Map.Entry<Integer, String>> entries = chainDBNameMap.entrySet();
            //for (Map.Entry<Integer, String> entry : entries) {
            //    Integer hChainId = entry.getKey();
            //
            //}
        }

    }

    public void init2LedgerAsset() {
        try {
            // 检查主资产是否绑定异构链合约资产
            // 由于账本内的主资产记录在内存中，重启应用后，账本内的主资产的类型被还原，
            // 所以，需要检查converter模块是否已绑定异构链合约资产，是则更新账本内记录的类型
            ledgerAssetRegisterHelper.checkMainAssetBind();
            if(heterogeneousChainInfoStorageService.hadInit2LedgerAsset()) {
                logger().info("[重复执行] 已完成初始化异构链主资产到账本，不再执行");
                return;
            }
            int chainId = converterConfig.getChainId();
            int htgChainId = FIRST_HETEROGENEOUS_ASSET_CHAIN_ID;
            if (chainId != 9) {
                htgChainId = 118;
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
            HeterogeneousAssetInfo assetInfo = docking.getMainAsset();
            ledgerAssetRegisterHelper.crossChainAssetReg(chainId, FIRST_HETEROGENEOUS_ASSET_CHAIN_ID, assetInfo.getAssetId(),
                    assetInfo.getSymbol(), assetInfo.getDecimals(), assetInfo.getSymbol(), assetInfo.getContractAddress());
            heterogeneousChainInfoStorageService.init2LedgerAssetCompleted();
            logger().info("完成初始化异构链主资产到账本");
        } catch (Exception e) {
            throw new RuntimeException(e);
            //TODO pierre test
            //e.printStackTrace();
        }
    }
}
