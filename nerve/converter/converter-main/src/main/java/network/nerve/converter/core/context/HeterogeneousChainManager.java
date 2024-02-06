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
     * Obtain based on heterogeneous chain nameschainId
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
     * Call this function to update persistently after the address of multiple tags in the heterogeneous chain is changed
     * Update heterogeneous chain multi signature addresses
     */
    public void updateMultySignAddress(int heterogeneousChainId, String multySignAddress) throws NulsException {
        logger().info("Persistent update of multiple contract addresses, heterogeneousChainId: {}, new: {}", heterogeneousChainId, multySignAddress);
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
     * Initialize all heterogeneous chain information
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
            // Perform heterogeneous chain registration in the specified order
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
                // Execute initialization functions for heterogeneous chains
                String dbName = register.init(heterogeneousCfg, chain.getLogger());
                // add by pierre at 2023/9/5 Database merge
                do {
                    //Check this chainDBHas it been merged
                    boolean hadDBMerged = heterogeneousChainInfoStorageService.hadDBMerged(chainId);
                    if (hadDBMerged) {
                        logger().info("[{}]chainDB[{}]Merged to[{}]", AssetName.getEnum(chainId).name(), dbName, ConverterDBConstant.DB_HETEROGENEOUS_CHAIN);
                        converterCoreApi.setDbMergedStatus(chainId);
                        break;
                    }
                    //Check if this chain exists in the table listDBIf it does not exist, use a merged table, this chainDBMark as merged
                    RocksDB table = RocksDBManager.getTable(dbName);
                    if (table == null) {
                        logger().info("[{}]chainDBUsing Merge Tables[{}]", AssetName.getEnum(chainId).name(), ConverterDBConstant.DB_HETEROGENEOUS_CHAIN);
                        heterogeneousChainInfoStorageService.markMergedChainDB(chainId);
                        converterCoreApi.setDbMergedStatus(chainId);
                        break;
                    }
                    byte[] prefix = ConverterDBUtil.stringToBytes(chainId + "_");
                    //This chainDBExecute merge
                    RocksIterator iterator = table.newIterator();
                    int count = 0;
                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        RocksDBManager.put(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, ArraysTool.concatenate(prefix, iterator.key()), iterator.value());
                        count++;
                    }
                    //This chainDBMark as merged
                    logger().info("[{}]chainDB[{}]Merged to[{}]Number: {}", AssetName.getEnum(chainId).name(), dbName, ConverterDBConstant.DB_HETEROGENEOUS_CHAIN, count);
                    heterogeneousChainInfoStorageService.markMergedChainDB(chainId);
                    converterCoreApi.setDbMergedStatus(chainId);
                    RocksDBManager.destroyTable(dbName);
                    File tableFile = new File(dataPath + File.separator + dbName);
                    tableFile.delete();
                } while (false);
                // end code by pierre
                HeterogeneousChainInfo chainInfo = register.getChainInfo();
                // Save heterogeneous chainssymbolandchainIdThe relationship between
                heterogeneousChainIdRuleMap.put(chainInfo.getChainName(), chainId);
                String multySignAddress = chainInfo.getMultySignAddress();
                // Basic information of persistent storage heterogeneous chains
                if (StringUtils.isNotBlank(multySignAddress) && !heterogeneousChainMap.containsKey(chainId)) {
                    try {
                        heterogeneousChainInfoStorageService.saveHeterogeneousChainInfo(chainId, chainInfo);
                    } catch (Exception e) {
                        throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
                    }
                    heterogeneousChainMap.put(chainId, chainInfo);
                }
                if (chainId == 202) {
                    HeterogeneousChainInfo chainInfoDB = heterogeneousChainMap.get(chainId);
                    if (!chainInfoDB.getMultySignAddress().equals(chainInfo.getMultySignAddress())
                            && chainInfoDB.getMultySignAddress().equalsIgnoreCase(chainInfo.getMultySignAddress())) {
                        try {
                            heterogeneousChainInfoStorageService.saveHeterogeneousChainInfo(chainId, chainInfo);
                        } catch (Exception e) {
                            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
                        }
                        heterogeneousChainMap.put(chainId, chainInfo);
                    }
                }
                IHeterogeneousChainDocking docking = register.getDockingImpl();
                // Perform heterogeneous chain registration
                HeterogeneousChainRegisterInfo registerInfo = heterogeneousChainRegister.register(converterConfig.getChainId(), chainId, docking);
                // Return registration information to heterogeneous chain components
                register.registerCallBack(registerInfo);
                // Read the latestdocking contract signature version
                docking.initialSignatureVersion();
                // Set priceKey, Obtain prices through the pricing module
                AssetName assetName = AssetName.getEnum(chainId);
                if (assetName == null) {
                    throw new RuntimeException("Empty AssetName!");
                }
                ConverterContext.priceKeyMap.put(assetName.name(), heterogeneousCfg.getPriceKey());
                // Set effective protocol data
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
            //Perform database merge operation
            //Traverse the registered chainDBname
            //Map<Integer, String> chainDBNameMap = converterCoreApi.chainDBNameMap();
            ////Check this chainDBHas it been merged
            //Set<Map.Entry<Integer, String>> entries = chainDBNameMap.entrySet();
            //for (Map.Entry<Integer, String> entry : entries) {
            //    Integer hChainId = entry.getKey();
            //
            //}
        }

    }

    public void init2LedgerAsset() {
        try {
            // Check if the main asset is bound to heterogeneous chain contract assets
            // Due to the main asset records in the ledger being stored in memory, the type of main asset in the ledger is restored after restarting the application,
            // So, it needs to be checkedconverterHas the module been bound to heterogeneous chain contract assets? If so, update the type of record in the ledger
            ledgerAssetRegisterHelper.checkMainAssetBind();
            if(heterogeneousChainInfoStorageService.hadInit2LedgerAsset()) {
                logger().info("[Repeated execution] The initialization of heterogeneous chain master assets to the ledger has been completed and will no longer be executed");
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
            logger().info("Complete initialization of heterogeneous chain master assets to ledger");
        } catch (Exception e) {
            throw new RuntimeException(e);
            //TODO pierre test
            //e.printStackTrace();
        }
    }
}
