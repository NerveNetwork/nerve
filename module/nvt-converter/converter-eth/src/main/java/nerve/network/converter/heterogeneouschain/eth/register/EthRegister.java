/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.register;

import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import nerve.network.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import nerve.network.converter.heterogeneouschain.eth.constant.EthConstant;
import nerve.network.converter.heterogeneouschain.eth.constant.EthDBConstant;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.docking.EthDocking;
import nerve.network.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import nerve.network.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import nerve.network.converter.heterogeneouschain.eth.listener.EthListener;
import nerve.network.converter.heterogeneouschain.eth.model.EthERC20Po;
import nerve.network.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import nerve.network.converter.heterogeneouschain.eth.storage.*;
import nerve.network.converter.heterogeneouschain.eth.syncblock.EthBlockScheduled;
import nerve.network.converter.heterogeneouschain.eth.syncblock.EthConfirmTxScheduled;
import nerve.network.converter.model.bo.HeterogeneousCfg;
import nerve.network.converter.model.bo.HeterogeneousChainInfo;
import nerve.network.converter.model.bo.HeterogeneousChainRegisterInfo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static nerve.network.converter.heterogeneouschain.eth.constant.EthConstant.ETH_ERC20_STANDARD_FILE;
import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * Eth组件向Nerve核心注册
 *
 * @author: Chino
 * @date: 2020-02-20
 */
@Component("ethRegister")
public class EthRegister implements IHeterogeneousChainRegister {

    @Autowired
    private EthERC20StorageService ethERC20StorageService;

    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthListener ethListener;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthAccountStorageService ethAccountStorageService;
    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private EthBlockScheduled ethBlockScheduled;
    @Autowired
    private EthConfirmTxScheduled ethConfirmTxScheduled;
    @Autowired
    private EthMultiSignAddressHistoryStorageService ethMultiSignAddressHistoryStorageService;
    @Autowired
    private EthTxStorageService ethTxStorageService;
    @Autowired
    private EthParseTxHelper ethParseTxHelper;

    private boolean isInitial = false;

    @Override
    public int getChainId() {
        return EthConstant.ETH_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            EthContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            EthContext.setConfig(config);
            ethWalletApi.init();
            // 存放nerveChainId
            EthContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(EthDBConstant.DB_ETH);
            initERC20();
            initUnconfirmedTxQueue();
            initScheduled();
        }
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(EthConstant.ETH_CHAIN_ID);
        info.setChainName(EthConstant.ETH_SYMBOL);
        info.setMultySignAddress(EthContext.getConfig().getMultySignAddress());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        EthDocking ethDocking = EthDocking.getInstance();
        ethDocking.setEthWalletApi(ethWalletApi);
        ethDocking.setEthListener(ethListener);
        ethDocking.setConverterConfig(converterConfig);
        ethDocking.setEthTxRelationStorageService(ethTxRelationStorageService);
        ethDocking.setEthUnconfirmedTxStorageService(ethUnconfirmedTxStorageService);
        ethDocking.setEthAccountStorageService(ethAccountStorageService);
        ethDocking.setEthERC20Helper(ethERC20Helper);
        ethDocking.setEthTxStorageService(ethTxStorageService);
        ethDocking.setEthParseTxHelper(ethParseTxHelper);
        return ethDocking;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
        // 监听多签地址交易
        ethListener.addListeningAddress(multiSigAddress);
        // 管理回调函数实例
        ethCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
        ethCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
        // 更新多签地址
        EthContext.MULTY_SIGN_ADDRESS = multiSigAddress;
        // 保存当前多签地址到多签地址历史列表中
        ethMultiSignAddressHistoryStorageService.save(multiSigAddress);
        EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET = ethMultiSignAddressHistoryStorageService.findAll();
        logger().info("ETH注册完成.");
    }

    private void initScheduled() {
        ScheduledThreadPoolExecutor blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("eth-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay(ethBlockScheduled, 60, 20, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("eth-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay(ethConfirmTxScheduled, 60, 20, TimeUnit.SECONDS);
    }

    private void initUnconfirmedTxQueue() {
        List<EthUnconfirmedTxPo> list = ethUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                // 初始化缓存列表
                EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
                // 把待确认的交易加入到监听交易hash列表中
                ethListener.addListeningTx(po.getTxHash());
            });
        }
        EthContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initERC20() throws Exception {
        if (ethERC20StorageService.hadInitDB()) {
            return;
        }
        String json = null;
        try {
            json = IoUtils.read(ETH_ERC20_STANDARD_FILE);
        } catch (Exception e) {
            // skip it
            logger().error("init ERC20Standard error.", e);
        }
        if (json == null) {
            return;
        }
        List<EthERC20Po> ethERC20Pos = JSONUtils.json2list(json, EthERC20Po.class);
        int maxAssetId = 1;
        for (EthERC20Po po : ethERC20Pos) {
            po.setAssetId(++maxAssetId);
            po.setAddress(po.getAddress().toLowerCase());
            ethERC20StorageService.save(po);
        }
        ethERC20StorageService.saveMaxAssetId(maxAssetId);
        ethERC20StorageService.initDBCompleted(maxAssetId);
    }
}
