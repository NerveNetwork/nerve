/*
 * MIT License
 *
 * Copyright (c) 2019-2022 nerve.network
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package network.nerve.converter.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.data.NulsHash;
import io.nuls.base.protocol.ProtocolLoader;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.task.HtgConfirmTxTask;
import network.nerve.converter.core.heterogeneous.task.HtgRpcAvailableHandlerTask;
import network.nerve.converter.core.heterogeneous.task.HtgWaitingTxInvokeDataHandlerTask;
import network.nerve.converter.core.thread.handler.SignMessageByzantineHandler;
import network.nerve.converter.core.thread.task.CfmTxSubsequentProcessTask;
import network.nerve.converter.core.thread.task.ExeProposalProcessTask;
import network.nerve.converter.core.thread.task.TxCheckAndCreateProcessTask;
import network.nerve.converter.core.thread.task.VirtualBankDirectorBalanceTask;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ChainLoggerUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;


/**
 * Chain management,Responsible for initializing each chain,working,start-up,Parameter maintenance, etc
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author qinyifeng
 * @date 2018/12/11
 */
@Component
public class ChainManager {

    @Autowired
    private ConfigStorageService configStorageService;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private ProposalVotingStorageService proposalVotingStorageService;
    @Autowired
    private ExeProposalStorageService exeProposalStorageService;
    @Autowired
    private PersistentCacheStroageService persistentCacheStroageService;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    private Map<Integer, Chain> chainMap = new ConcurrentHashMap<>();

    /**
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void initChain() throws Exception {
        Map<Integer, ConfigBean> configMap = configChain();
        if (configMap == null || configMap.size() == 0) {
            return;
        }
        for (Map.Entry<Integer, ConfigBean> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            chain.setConfig(entry.getValue());
            initLogger(chain);
            initTable(chain);
            loadChainCacheData(chain);
            loadHeterogeneousCfgJson(chain);
            createScheduler(chain);
            chainMap.put(chainId, chain);
            chain.getLogger().debug("Chain:{} init success..", chainId);
            ProtocolLoader.load(chainId);
            if(chainId == converterConfig.getChainId()) {
                converterCoreApi.setNerveChain(chain);
                //TODO pierre test
                //converterCoreApi.setCurrentHeterogeneousVersionII();
            }
        }
    }

    private void initTable(Chain chain) {
        int chainId = chain.getConfig().getChainId();
        try {
            // Virtual Bank Member Table、Virtual Bank Change Record Object
            RocksDBService.createTable(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chainId);
            RocksDBService.createTable(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chainId);
            // Transaction storage
            RocksDBService.createTable(ConverterDBConstant.DB_TX_PREFIX + chainId);
            // Confirm virtual bank change transaction Business Storage Table
            RocksDBService.createTable(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chainId);
            // Confirmation of withdrawal transaction status business data table
            RocksDBService.createTable(ConverterDBConstant.DB_CONFIRM_WITHDRAWAL_PREFIX + chainId);
            // Successfully called transaction for heterogeneous chain component/Implemented proposals, etc Persistent Table prevent2Secondary call
            RocksDBService.createTable(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chainId);
            // Waiting for the data table of the calling component
            RocksDBService.createTable(ConverterDBConstant.DB_PENDING_PREFIX + chainId);
            // Virtual bank calls heterogeneous chains, When merging transactions, keyCorrespondence with each transaction
            RocksDBService.createTable(ConverterDBConstant.DB_MERGE_COMPONENT_PREFIX + chainId);
            // Confirm subsidy handling fee transaction business data sheet
            RocksDBService.createTable(ConverterDBConstant.DB_DISTRIBUTION_FEE_PREFIX + chainId);
            // Proposal Storage Table
            RocksDBService.createTable(ConverterDBConstant.DB_PROPOSAL_PREFIX + chainId);
            //Proposals in voting
            RocksDBService.createTable(ConverterDBConstant.DB_PROPOSAL_VOTING_PREFIX + chainId);
            // Proposal Function Voting Information Table
            RocksDBService.createTable(ConverterDBConstant.DB_VOTE_PREFIX + chainId);
            // Address list for disqualification from banking
            RocksDBService.createTable(ConverterDBConstant.DB_DISQUALIFICATION_PREFIX + chainId);
            // Recharge transaction business data
            RocksDBService.createTable(ConverterDBConstant.DB_RECHARGE_PREFIX + chainId);
            // Transaction for executing proposalshash and Correspondence of proposals
            RocksDBService.createTable(ConverterDBConstant.DB_PROPOSAL_EXE + chainId);
            // Proposal awaiting execution
            RocksDBService.createTable(ConverterDBConstant.DB_EXE_PROPOSAL_PENDING_PREFIX + chainId);
            // Reset Virtual Bank Heterogeneous Chain
            RocksDBService.createTable(ConverterDBConstant.DB_RESET_BANK_PREFIX + chainId);
            // Heterogeneous Chain Address Signature Message Storage Table
            RocksDBService.createTable(ConverterDBConstant.DB_COMPONENT_SIGN + chainId);

        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                chain.getLogger().error(e);
            }
        }
    }

    /**
     * Retrieve and load database data to cache
     *
     * @param chain
     */
    private void loadChainCacheData(Chain chain) {
        // Load virtual bank members
        Map<String, VirtualBankDirector> mapVirtualBank = virtualBankStorageService.findAll(chain);
        if (null != mapVirtualBank && !mapVirtualBank.isEmpty()) {
            VirtualBankUtil.sortDirectorMap(mapVirtualBank);
            chain.getMapVirtualBank().putAll(mapVirtualBank);
        }
        try {
            chain.getLogger().info("MapVirtualBank : {}", JSONUtils.obj2json(chain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("MapVirtualBank log print error ");
        }

        // Load transactions of heterogeneous components to be called
        List<TxSubsequentProcessPO> listPending = txSubsequentProcessStorageService.findAll(chain);
        if (null != listPending && !listPending.isEmpty()) {
            chain.getPendingTxQueue().addAll(listPending);
        }
        try {
            chain.getLogger().info("PendingTxQueue : {}", JSONUtils.obj2json(chain.getPendingTxQueue()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("PendingTxQueue log print error ");
        }

        // Load proposals from voting
        Map<NulsHash, ProposalPO> votingMap= proposalVotingStorageService.findAll(chain);
        if (null != votingMap && !votingMap.isEmpty()) {
            chain.getVotingProposalMap().putAll(votingMap);
        }
        try {
            chain.getLogger().info("VotingProposals : {}", JSONUtils.obj2json(chain.getVotingProposalMap().keySet()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("VotingProposals log print error ");
        }

        // Load pending proposals
        List<ExeProposalPO> exeProposalPOList = exeProposalStorageService.findAll(chain);
        chain.getExeProposalQueue().addAll(exeProposalPOList);
        chain.getLogger().info("exeProposalPOList size : {}", exeProposalPOList.size());


        Integer changeBank = persistentCacheStroageService.getCacheState(chain, ConverterDBConstant.EXE_HETEROGENEOUS_CHANGE_BANK_KEY);
        if(null != changeBank){
            chain.getHeterogeneousChangeBankExecuting().set(changeBank == 0 ? false : true);
            chain.getLogger().info("HeterogeneousChangeBankExecuting : {}", chain.getHeterogeneousChangeBankExecuting().get());
        }
        Integer disqualifyBank = persistentCacheStroageService.getCacheState(chain, ConverterDBConstant.EXE_DISQUALIFY_BANK_PROPOSAL_KEY);
        if(null != disqualifyBank){
            chain.getExeDisqualifyBankProposal().set(disqualifyBank == 0 ? false : true);
            chain.getLogger().info("ExeDisqualifyBankProposal : {}", chain.getExeDisqualifyBankProposal().get());
        }
        Integer resetBank = persistentCacheStroageService.getCacheState(chain, ConverterDBConstant.RESET_VIRTUALBANK_KEY);
        if(null != resetBank){
            chain.getResetVirtualBank().set(resetBank == 0 ? false : true);
            chain.getLogger().info("ResetVirtualBank : {}", chain.getResetVirtualBank().get());
        }
    }


    private void loadHeterogeneousCfgJson(Chain chain) throws Exception {
        String configJson;
        if (converterConfig.isHeterogeneousMainNet()) {
            configJson = IoUtils.read(ConverterConstant.HETEROGENEOUS_MAINNET_CONFIG);
        } else {
            configJson = IoUtils.read(ConverterConstant.HETEROGENEOUS_TESTNET_CONFIG);
        }
        List<HeterogeneousCfg> list = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
        String multySignAddressSet = converterConfig.getMultySignAddressSet();
        if(StringUtils.isBlank(multySignAddressSet)) {
            throw new Exception("empty data of multySignAddress set");
        }
        String[] multySignAddressArray = multySignAddressSet.split(",");
        for(String multySignAddressTemp : multySignAddressArray) {
            String[] addressInfo = multySignAddressTemp.split(":");
            int chainId = Integer.parseInt(addressInfo[0]);
            String address = addressInfo[1];
            for(HeterogeneousCfg cfg : list) {
                if(cfg.getChainId() == chainId && cfg.getType() == 1) {
                    cfg.setMultySignAddress(addressToLowerCase(address));
                    break;
                }
            }
        }
        chain.setListHeterogeneous(list);
        try {
            chain.getLogger().info("HeterogeneousCfg : {}", JSONUtils.obj2json(list));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("HeterogeneousCfg log print error ");
        }
    }

    /**
     * Stop a chain
     * Delete a chain
     *
     * @param chainId chainID/chain id
     */
    public void stopChain(int chainId) {

    }

    /**
     * Read configuration file to create and initialize chain
     * Read the configuration file to create and initialize the chain
     */
    private Map<Integer, ConfigBean> configChain() {
        try {
            /*
            Read database chain information configuration/Read database chain information configuration
             */
            Map<Integer, ConfigBean> configMap = configStorageService.getList();
            /*
            If the system is running for the first time and there is no storage chain information in the local database, it is necessary to read the main chain configuration information from the configuration file
            If the system is running for the first time, the local database does not have chain information,
            and the main chain configuration information needs to be read from the configuration file at this time.
            */
            if (configMap == null || configMap.size() == 0) {
                ConfigBean configBean = converterConfig;
                if (configBean == null) {
                    return null;
                }
                configStorageService.save(configBean, configBean.getChainId());
                configMap.put(configBean.getChainId(), configBean);
            }
            return configMap;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }


    public void createScheduler(Chain chain) {
        ScheduledThreadPoolExecutor collectorExecutor = ThreadUtils.createScheduledThreadPool(1,
                new NulsThreadFactory(ConverterConstant.CV_PENDING_THREAD));
        collectorExecutor.scheduleWithFixedDelay(new CfmTxSubsequentProcessTask(chain),
                ConverterConstant.CV_TASK_INITIALDELAY, ConverterConstant.CV_TASK_PERIOD, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor exeProposalExecutor = ThreadUtils.createScheduledThreadPool(1,
                new NulsThreadFactory(ConverterConstant.CV_PENDING_PROPOSAL_THREAD));
        exeProposalExecutor.scheduleWithFixedDelay(new ExeProposalProcessTask(chain),
                ConverterConstant.CV_TASK_INITIALDELAY, ConverterConstant.CV_TASK_PERIOD, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor calculatorExecutor = ThreadUtils.createScheduledThreadPool(1,
                new NulsThreadFactory(ConverterConstant.CV_SIGN_THREAD));
        calculatorExecutor.scheduleAtFixedRate(new SignMessageByzantineHandler(chain),
                ConverterConstant.CV_SIGN_TASK_INITIALDELAY, ConverterConstant.CV_SIGN_TASK_PERIOD, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor checkExecutor = ThreadUtils.createScheduledThreadPool(1,
                new NulsThreadFactory(ConverterConstant.CV_CHECK_THREAD));
        checkExecutor.scheduleAtFixedRate(new TxCheckAndCreateProcessTask(chain),
                ConverterConstant.CV_CHECK_TASK_INITIALDELAY, ConverterConstant.CV_CHECK_TASK_PERIOD, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor htgBalanceExecutor = ThreadUtils.createScheduledThreadPool(1,
                new NulsThreadFactory(ConverterConstant.CV_HTG_BALANCE_THREAD));
        htgBalanceExecutor.scheduleAtFixedRate(new VirtualBankDirectorBalanceTask(chain),
                ConverterConstant.CV_HTG_BALANCE_TASK_INITIALDELAY, ConverterConstant.CV_HTG_BALANCE_TASK_PERIOD, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("htg-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay(new HtgConfirmTxTask(converterCoreApi), 60, 10, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("htg-waiting-tx"));
        waitingTxExecutor.scheduleWithFixedDelay(new HtgWaitingTxInvokeDataHandlerTask(converterCoreApi), 60, 10, TimeUnit.SECONDS);

        ScheduledThreadPoolExecutor rpcAvailableExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("htg-rpcavailable-tx"));
        rpcAvailableExecutor.scheduleWithFixedDelay(new HtgRpcAvailableHandlerTask(converterCoreApi), 60, 10, TimeUnit.SECONDS);
    }


    private void initLogger(Chain chain) {
        ChainLoggerUtil.init(chain);
    }

    public Map<Integer, Chain> getChainMap() {
        return chainMap;
    }

    public void setChainMap(Map<Integer, Chain> chainMap) {
        this.chainMap = chainMap;
    }

    public boolean containsKey(int key) {
        return this.chainMap.containsKey(key);
    }

    public Chain getChain(int key) {
        return this.chainMap.get(key);
    }


}
