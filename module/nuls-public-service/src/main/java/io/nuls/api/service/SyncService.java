package io.nuls.api.service;


import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.ApiErrorCode;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.*;
import io.nuls.api.db.mongo.MongoAccountServiceImpl;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.*;
import io.nuls.api.model.po.mini.CancelDepositInfo;
import io.nuls.api.model.rpc.BalanceInfo;
import io.nuls.api.utils.DBUtil;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.crypto.Sha512Hash;
import io.nuls.core.exception.NulsRuntimeException;
import org.apache.commons.codec.digest.Md5Crypt;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.jcajce.provider.digest.SHA3;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static io.nuls.api.constant.ApiConstant.*;

@Component
public class SyncService {

    @Autowired
    private ChainService chainService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountLedgerService ledgerService;
    @Autowired
    private TransactionService txService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AliasService aliasService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private PunishService punishService;
    @Autowired
    private RoundManager roundManager;

    @Autowired
    RoundService roundService;

    @Autowired
    BlockTimeService blockTimeService;

    @Autowired
    SymbolQuotationPriceService symbolUsdtPriceService;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    StackSnapshootService stackSnapshootService;

    @Autowired
    ConverterTxService converterTxService;

    @Autowired
    LedgerRegAssetService ledgerRegAssetService;

    //记录每个区块打包交易涉及到的账户的余额变动
    private Map<String, AccountInfo> accountInfoMap = new HashMap<>();
    //记录每个账户的资产变动
    private Map<String, AccountLedgerInfo> accountLedgerInfoMap = new HashMap<>();
    //记录每个区块代理节点的变化
    private List<AgentInfo> agentInfoList = new ArrayList<>();
    //记录每个区块交易和账户地址的关系
    private Set<TxRelationInfo> txRelationInfoSet = new HashSet<>();
    //记录每个区块设置别名信息
    private List<AliasInfo> aliasInfoList = new ArrayList<>();
    //记录每个区块委托共识的信息
    private List<DepositInfo> depositInfoList = new ArrayList<>();
    //记录每个区块的红黄牌信息
    private List<PunishLogInfo> punishLogList = new ArrayList<>();

    //记录链信息
    private List<ChainInfo> chainInfoList = new ArrayList<>();

    //记录链内资产注册信息
    private List<LedgerRegAssetInfo> ledgerRegAssetInfoList = new ArrayList<>();

    /**
     * 最终喂价记录
     */
    private List<StackSymbolPriceInfo> symbolPriceInfoList = new ArrayList<>();

    /**
     * 喂价提交记录
     */
    private List<StackSymbolPriceInfo> symbolQuotationRecordInfoList = new ArrayList<>();

    /**
     * 区块出块耗时情况
     */
    private Map<Integer, BlockTimeInfo> blockTimeInfoMap = new HashMap<>();

    /**
     * stack快照
     */
    private Optional<StackSnapshootInfo> stackSnapshootInfo = Optional.empty();

    /**
     * 跨链交易扩展数据
     */
    private List<ConverterTxInfo> converterTxInfoList = new ArrayList<>();

    //处理每个交易时，过滤交易中的重复地址
    Set<String> addressSet = new HashSet<>();

    public SyncInfo getSyncInfo(int chainId) {
        return chainService.getSyncInfo(chainId);
    }

    public BlockHeaderInfo getBestBlockHeader(int chainId) {
        return blockService.getBestBlockHeader(chainId);
    }


    public boolean syncNewBlock(int chainId, BlockInfo blockInfo) {
        clear(chainId);
        long time1, time2;
        time1 = System.currentTimeMillis();
        findAddProcessAgentOfBlock(chainId, blockInfo);
        //更新出块时间消耗统计
        processBlockTime(chainId, blockInfo);
        //处理交易
        processTxs(chainId, blockInfo.getTxList());
        //stacking 快照
        //保存数据
        save(chainId, blockInfo);

        ApiCache apiCache = CacheManager.getCache(chainId);
        apiCache.setBestHeader(blockInfo.getHeader());

        time2 = System.currentTimeMillis();
        LoggerUtil.commonLog.info("-----height finish:" + blockInfo.getHeader().getHeight() + "-----txCount:" + blockInfo.getHeader().getTxCount() + "-----use:" + (time2 - time1) + "-----");
        return true;
    }

    /**
     * 更新出块时间消耗统计
     *
     * @param chainId
     */
    private void processBlockTime(int chainId, BlockInfo blockInfo) {
        BlockTimeInfo blockTimeInfo = blockTimeInfoMap.get(chainId);
        if (blockTimeInfo == null) {
            blockTimeInfo = blockTimeService.get(chainId);
        }
        if (blockTimeInfo == null) {
            blockTimeInfo = new BlockTimeInfo();
            blockTimeInfo.setBlockHeight(blockInfo.getHeader().getHeight());
            blockTimeInfo.setChainId(chainId);
            blockTimeInfo.setLastBlockTimeStamp(blockInfo.getHeader().getCreateTime());
            blockTimeInfo.setAvgConsumeTime(0);
        } else {
            if (blockInfo.getHeader().getHeight() == 0) {
                return;
            }
            double avgConsumeTime;
            long lastConsumeTime;
            if (blockInfo.getHeader().getHeight() == 1) {
                lastConsumeTime = 2;
                avgConsumeTime = lastConsumeTime;
            } else {
                lastConsumeTime = blockInfo.getHeader().getCreateTime() - blockTimeInfo.getLastBlockTimeStamp();
                avgConsumeTime = ((blockTimeInfo.getAvgConsumeTime() * (blockInfo.getHeader().getHeight() - 1)) + lastConsumeTime) / blockInfo.getHeader().getHeight();
            }
            blockTimeInfo.setLastBlockTimeStamp(blockInfo.getHeader().getCreateTime());
            blockTimeInfo.setBlockHeight(blockInfo.getHeader().getHeight());
            blockTimeInfo.setAvgConsumeTime(avgConsumeTime == 0 ? lastConsumeTime : avgConsumeTime);
            blockTimeInfo.setLastConsumeTime(lastConsumeTime);
        }
        this.blockTimeInfoMap.put(chainId, blockTimeInfo);
    }


    /**
     * 查找当前出块节点并处理相关信息
     * Find the current outbound node and process related information
     *
     * @return
     */
    private void findAddProcessAgentOfBlock(int chainId, BlockInfo blockInfo) {
        BlockHeaderInfo headerInfo = blockInfo.getHeader();
        if (headerInfo.isSeedPacked()) {
            //如果是种子节点打包的区块，则创建一个新的AgentInfo对象，临时使用
            //If it is a block packed by the seed node, create a new AgentInfo object for temporary use.
            headerInfo.setAgentId(headerInfo.getPackingAddress());
        } else {
            //根据区块头的打包地址，查询打包节点的节点信息，修改相关统计数据
            //According to the packed address of the block header, query the node information of the packed node, and modify related statistics.
            if (blockInfo.getTxList() != null && !blockInfo.getTxList().isEmpty()) {
                calcCommissionReward(chainId,headerInfo, blockInfo.getTxList());
            }
        }
    }

    /**
     * 出块节点增加总打包数量
     * 如果有coinbase，切收益地址是出块地址的收益地址，增加节点的总收益和更新收益最后发放时间
     * @param chainId
     * @param headerInfo
     * @param txs
     */
    private void calcCommissionReward(int chainId,BlockHeaderInfo headerInfo,List<TransactionInfo> txs) {
        CacheManager.getCache(chainId).getAgentMap().values().stream().filter(d->d.getPackingAddress().equals(headerInfo.getPackingAddress())).findFirst()
                .ifPresent(packingAgentInfo->{
                    packingAgentInfo.setTotalPackingCount(packingAgentInfo.getTotalPackingCount() + 1);
                    packingAgentInfo.setVersion(headerInfo.getAgentVersion());
                    headerInfo.setByAgentInfo(packingAgentInfo);
                });
        txs.stream().filter(d->d.getType()==TxType.COIN_BASE).findFirst().ifPresent(tx->{
            List<CoinToInfo> list = tx.getCoinTos();
            if (null == list || list.isEmpty()) {
                return;
            }
            AssetInfo assetInfo = CacheManager.getCacheChain(chainId).getDefaultAsset();
            for (CoinToInfo output : list) {
                //找到奖励地址为转入地址的节点
                AgentInfo agentInfo = queryAgentInfo(chainId,output.getAddress(),3);
                if(agentInfo != null){
                    if (output.getChainId() == assetInfo.getChainId() && output.getAssetsId() == assetInfo.getAssetId()) {
                        agentInfo.setReward(agentInfo.getReward().add(output.getAmount()));
                    }
                    agentInfo.setLastRewardHeight(headerInfo.getHeight());
                }
            }
        });
    }

    /**
     * 处理各种交易
     * 
     * @param txs
     */
    private void processTxs(int chainId, List<TransactionInfo> txs) {
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
//            CoinDataInfo coinDataInfo = new CoinDataInfo(tx.getHash(), tx.getCoinFroms(), tx.getCoinTos());
//            coinDataList.add(coinDataInfo);
            if (tx.getType() == TxType.COIN_BASE) {
                processCoinBaseTx(chainId, tx);
            } else if (tx.getType() == TxType.TRANSFER) {
                processTransferTx(chainId, tx);
            } else if (tx.getType() == TxType.ACCOUNT_ALIAS) {
                processAliasTx(chainId, tx);
            } else if (tx.getType() == TxType.REGISTER_AGENT) {
                processCreateAgentTx(chainId, tx);
            } else if (tx.getType() == TxType.DEPOSIT) {
                processDepositTx(chainId, tx);
            } else if (tx.getType() == TxType.CANCEL_DEPOSIT) {
                processCancelDepositTx(chainId, tx);
            } else if (tx.getType() == TxType.STOP_AGENT) {
                processStopAgentTx(chainId, tx);
            } else if (tx.getType() == TxType.YELLOW_PUNISH) {
                processYellowPunishTx(chainId, tx);
            } else if (tx.getType() == TxType.RED_PUNISH) {
                processRedPunishTx(chainId, tx);
            } else if (tx.getType() == TxType.CROSS_CHAIN) {
                processCrossTransferTx(chainId, tx);
            } else if (tx.getType() == TxType.LEDGER_ASSET_REG_TRANSFER) {
                processLedgerAssetRegTransferTx(chainId, tx);
            } else if (tx.getType() == TxType.APPEND_AGENT_DEPOSIT || tx.getType() == TxType.REDUCE_AGENT_DEPOSIT) {
                processChangeAgentDeposit(chainId, tx);
            } else if (tx.getType() == TxType.FINAL_QUOTATION) {
                //保存最终喂价
                processSymbolFinalQuotation(chainId, tx);
            } else if (tx.getType() == TxType.QUOTATION) {
                //保存喂价提交流水
                processSymbolQuotation(chainId, tx);
            } else if (tx.getType() == TxType.CONFIRM_WITHDRAWAL || tx.getType() == TxType.RECHARGE){
                processConverterTx(chainId,tx);
            }
            else if (tx.getType() == TxType.TRADING_ORDER || tx.getType() == TxType.ORDER_CANCEL_CONFIRM || tx.getType() == TxType.TRADING_DEAL ){
                processDexTx(chainId,tx);
            } else if (tx.getType() == TxType.CHANGE_VIRTUAL_BANK) {
                processChangeVirtualBankTx(chainId,tx);
            }
            else {
                processTransferTx(chainId, tx);
            } 

        }
    }

    private void processChangeVirtualBankTx(int chainId, TransactionInfo tx) {
        ChangeVirtualBankInfo info = (ChangeVirtualBankInfo) tx.getTxData();
        info.getOutAgents().forEach(agentAddress->{
            AgentInfo agentInfo = queryAgentInfo(chainId,agentAddress,2);
            if(agentInfo == null){
                return ;
            }
            agentInfo.setBankNode(false);
            agentInfoList.add(agentInfo);
        });
        info.getInAgents().forEach(agentAddress->{
            AgentInfo agentInfo = queryAgentInfo(chainId,agentAddress,2);
            Objects.requireNonNull(agentInfo,"data error,can't found agent " + agentAddress);
            agentInfo.setBankNode(true);
            agentInfoList.add(agentInfo);
        });
    }

    /**
     * 处理dex交易
     * 挂单交易和测单确认是属于解锁交易，只保存form里面的交易
     * @param chainId
     * @param tx
     */
    private void processDexTx(int chainId, TransactionInfo tx) {
        addressSet.clear();
        int index = 0;
        //存储各个地址的净转入转出，from减，to加 ，结果为负数时属于净转出，反之为净转入。为0时属于锁定，不计入总的转出转入
        Map<String,BigInteger> txBalance = new HashMap<>();
        //1.计算账户余额
        /*
            from locked 为0时，减少可用余额
            from locked 为-1时，减少锁定余额
            to   locked 为0时，增加可用余额
            to   locked 为-2时，增加锁定余额
         */
        //2.保存交易用户关系表
        if(tx.getCoinFroms() != null){
            for (CoinFromInfo input : tx.getCoinFroms()) {
                txBalance.compute(input.getAddress(),(key,oldVal)->{
                    if(oldVal == null){
                        return input.getAmount().negate();
                    }else{
                        return oldVal.subtract(input.getAmount());
                    }
                });
                AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
                AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, input.getAddress(), input.getChainId(), input.getAssetsId());
                switch (input.getLocked()){
                    case 0:
                        accountInfo.setTotalBalance(accountInfo.getTotalBalance().subtract(input.getAmount()));
                        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().subtract(input.getAmount()));
                        break;
                    case -1:
                        accountInfo.setConsensusLock(accountInfo.getConsensusLock().subtract(input.getAmount()));
                        accountInfo.setTotalBalance(accountInfo.getTotalBalance().subtract(input.getAmount()));
                        ledgerInfo.setConsensusLock(ledgerInfo.getConsensusLock().subtract(input.getAmount()));
                        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().subtract(input.getAmount()));
                }
                txRelationInfoSet.add(new TxRelationInfo(input, tx, ledgerInfo.getTotalBalance(),index++));
            };
        }
        if(tx.getCoinTos() != null){
            for (CoinToInfo out : tx.getCoinTos()) {
                txBalance.compute(out.getAddress(),(key,oldVal)->{
                    if(oldVal == null){
                        return out.getAmount();
                    }else{
                        return oldVal.add(out.getAmount());
                    }
                });
                AccountInfo accountInfo = queryAccountInfo(chainId, out.getAddress());
                AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, out.getAddress(), out.getChainId(), out.getAssetsId());
                switch ((int) out.getLockTime()){
                    case 0:
                        accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(out.getAmount()));
                        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(out.getAmount()));
                        break;
                    case -2:
                        accountInfo.setConsensusLock(accountInfo.getConsensusLock().add(out.getAmount()));
                        ledgerInfo.setConsensusLock(ledgerInfo.getConsensusLock().add(out.getAmount()));
                        accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(out.getAmount()));
                        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(out.getAmount()));
                }
                txRelationInfoSet.add(new TxRelationInfo(out, tx, ledgerInfo.getTotalBalance(),index++));
            };
        }
        //3.计算账户总转出和总转入，from - to 负数为净转出，正数为净转入
        txBalance.entrySet().forEach(entry->{
            AccountInfo accountInfo = queryAccountInfo(chainId, entry.getKey());
            if(entry.getValue().compareTo(BigInteger.ZERO) < 0){
                accountInfo.setTotalOut(accountInfo.getTotalOut().add(entry.getValue()));
            } else if (entry.getValue().compareTo(BigInteger.ZERO) > 0){
                accountInfo.setTotalIn(accountInfo.getTotalIn().add(entry.getValue()));
            }
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            addressSet.add(entry.getKey());
        });
    }

    private void processCoinBaseTx(int chainId, TransactionInfo tx) {
        if (tx.getCoinTos() == null || tx.getCoinTos().isEmpty()) {
            return;
        }
        AssetInfo assetInfo = CacheManager.getCacheChain(chainId).getDefaultAsset();
        addressSet.clear();
        for (CoinToInfo output : tx.getCoinTos()) {
            addressSet.add(output.getAddress());
            calcBalance(chainId, output);
            //创世块的数据和合约返还不计算共识奖励
            if (tx.getHeight() == 0) {
                continue;
            }
            //奖励是本链主资产的时候，累计奖励金额
            if (output.getChainId() == assetInfo.getChainId() && output.getAssetsId() == assetInfo.getAssetId()) {
                if (output.getAmount().equals(BigInteger.ZERO)) {
                    continue;
                }
                AccountInfo accountInfo = queryAccountInfo(chainId, output.getAddress());
                accountInfo.setTotalReward(accountInfo.getTotalReward().add(output.getAmount()));
                accountInfo.setLastReward(output.getAmount());
                //修改账本数据
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, assetInfo.getChainId(), assetInfo.getAssetId(), accountInfo, BigInteger.ZERO);
                //保存交易流水
                TxRelationInfo txRelationInfo = new TxRelationInfo(output, tx, output.getAmount(), ledgerInfo.getTotalBalance());
                txRelationInfo.setFee(new FeeInfo(output.getChainId(), output.getAssetsId(), output.getSymbol()));
                txRelationInfoSet.add(txRelationInfo);
            }
        }
        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        }
    }


    private void processTransferTx(int chainId, TransactionInfo tx) {
        addressSet.clear();
        int index = 0;
        if (tx.getCoinFroms() != null) {
            for (CoinFromInfo input : tx.getCoinFroms()) {
                addressSet.add(input.getAddress());
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, input);
                txRelationInfoSet.add(new TxRelationInfo(input, tx, ledgerInfo.getTotalBalance(),index++));
            }
        }

        if (tx.getCoinTos() != null) {
            for (CoinToInfo output : tx.getCoinTos()) {
                addressSet.add(output.getAddress());
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, output);
                txRelationInfoSet.add(new TxRelationInfo(output, tx, ledgerInfo.getTotalBalance(),index++));
            }
        }

        if(tx.getType() == TxType.LEDGER_ASSET_REG_TRANSFER){
            LedgerRegAssetInfo info = (LedgerRegAssetInfo) tx.getTxData();
            addressSet.add(info.getAddress());
            AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId,info.getAddress(),info.getAssetChainId(),info.getAssetId());
            ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(info.getInitNumber()));
            txRelationInfoSet.add(new TxRelationInfo(info,tx,info.getInitNumber(),BigInteger.ZERO,index++));
        }

        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        }
    }

    private void processCrossTransferTx(int chainId, TransactionInfo tx) {
        addressSet.clear();
        int index = 0;
        if (tx.getCoinFroms() != null) {
            for (CoinFromInfo input : tx.getCoinFroms()) {
                //如果地址不是本链的地址，不参与计算与存储
                if (chainId != AddressTool.getChainIdByAddress(input.getAddress())) {
                    continue;
                }
                addressSet.add(input.getAddress());
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, input);
                txRelationInfoSet.add(new TxRelationInfo(input, tx, ledgerInfo.getTotalBalance(),index++));
            }
        }

        if (tx.getCoinTos() != null) {
            for (CoinToInfo output : tx.getCoinTos()) {
                //如果地址不是本链的地址，不参与计算与存储
                if (chainId != AddressTool.getChainIdByAddress(output.getAddress())) {
                    continue;
                }
                addressSet.add(output.getAddress());
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, output);
                txRelationInfoSet.add(new TxRelationInfo(output, tx, ledgerInfo.getTotalBalance(),index++));
                index++;

            }
        }
        //存储跨链扩展信息
        tx.getTxDataList().forEach(txDataInfo -> this.converterTxInfoList.add((ConverterTxInfo) txDataInfo));
        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        }
    }

    private void processCrossTransferTxForNRC20TransferOut(int chainId, TransactionInfo tx) {
        addressSet.clear();
        int index=0;
        if (tx.getCoinFroms() != null) {
            for (CoinFromInfo input : tx.getCoinFroms()) {
                //如果地址不是本链的地址，不参与计算与存储
                if (chainId != AddressTool.getChainIdByAddress(input.getAddress())) {
                    continue;
                }
                addressSet.add(input.getAddress());
                TxRelationInfo txRelationInfo;
                if (input.getAssetsId() == ApiContext.defaultAssetId) {
                    AccountLedgerInfo ledgerInfo = calcBalance(chainId, input);
                    txRelationInfo = new TxRelationInfo(input, tx, ledgerInfo.getTotalBalance(),index++);
                } else {
                    txRelationInfo = new TxRelationInfo(input, tx, BigInteger.ZERO,index++);
                }
                txRelationInfoSet.add(txRelationInfo);
            }
        }

        if (tx.getCoinTos() != null) {
            for (CoinToInfo output : tx.getCoinTos()) {
                //如果地址不是本链的地址，不参与计算与存储
                if (chainId != AddressTool.getChainIdByAddress(output.getAddress())) {
                    continue;
                }
                addressSet.add(output.getAddress());
                AccountLedgerInfo ledgerInfo = calcBalance(chainId, output);
                txRelationInfoSet.add(new TxRelationInfo(output, tx, ledgerInfo.getTotalBalance(),index++));
            }
        }

        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        }
    }

    private void processLedgerAssetRegTransferTx(int chainId, TransactionInfo tx) {
        processTransferTx(chainId, tx);
        LedgerRegAssetInfo info = (LedgerRegAssetInfo) tx.getTxData();
        ledgerRegAssetInfoList.add(info);
    }

    private void processAliasTx(int chainId, TransactionInfo tx) {
        AliasInfo aliasInfo = (AliasInfo) tx.getTxData();
        AccountInfo accountInfo = queryAccountInfo(chainId, aliasInfo.getAddress());
        accountInfo.setAlias(aliasInfo.getAlias());
        aliasInfoList.add(aliasInfo);
        if (tx.getCoinFroms() == null) {
            return;
        }
        CoinFromInfo input = tx.getCoinFroms().get(0);
        AccountLedgerInfo ledgerInfo = calcBalance(chainId, input);
        txRelationInfoSet.add(new TxRelationInfo(input, tx, ledgerInfo.getTotalBalance()));
        accountInfo = queryAccountInfo(chainId, input.getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);

        CoinToInfo output = tx.getCoinTos().get(0);
        ledgerInfo = calcBalance(chainId, output);
        txRelationInfoSet.add(new TxRelationInfo(output, tx, ledgerInfo.getTotalBalance()));
        accountInfo = queryAccountInfo(chainId, input.getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);

        //更新节点别名
        AgentInfo agentInfo = queryAgentInfo(chainId, aliasInfo.getAddress(), 2);
        if(agentInfo != null){
            agentInfo.setAgentAlias(aliasInfo.getAlias());
            agentInfo.setNew(false);
        }
    }

    /**
     * 处理创建节点交易
     *
     * @param chainId
     * @param tx
     */
    private void processCreateAgentTx(int chainId, TransactionInfo tx) {
        //构建节点数据
        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        agentInfo.setNew(true);
        //查询agent节点是否设置过别名
        AliasInfo aliasInfo = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
        if (aliasInfo != null) {
            agentInfo.setAgentAlias(aliasInfo.getAlias());
        }
        agentInfoList.add(agentInfo);

        //将创建节点的初始押金写入抵押流水中
        DepositInfo depositInfo = DepositInfo.buildByAgent(agentInfo.getDeposit(), agentInfo, tx, DepositInfoType.CREATE_AGENT);
        depositInfoList.add(depositInfo);
        changeAccountLockBalance(chainId, tx, depositInfo);
    }

    /**
     * 处理注销节点交易
     *
     * @param chainId
     * @param tx
     */
    private void processStopAgentTx(int chainId, TransactionInfo tx) {
        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        agentInfo = queryAgentInfo(chainId, agentInfo.getTxHash(), 1);

        //生成委托流水
        DepositInfo depositInfo = DepositInfo.buildByAgent(agentInfo.getDeposit().negate(), agentInfo, tx, DepositInfoType.STOP_AGENT);
        depositInfo.setBlockHeight(tx.getHeight());
        depositInfoList.add(depositInfo);
        //修改账户委托锁定并保存交易流水
        changeAccountLockBalance(chainId, tx, depositInfo);
        cleanAgent(agentInfo, tx);
    }

    /**
     * 节点停止，更新节点信息
     *
     * @param agentInfo
     * @param tx
     */
    private void cleanAgent(AgentInfo agentInfo, TransactionInfo tx) {
        //修改节点信息 以下代码不能与生成委托流水代码交换位置
        agentInfo.setDeleteHash(tx.getHash());
        agentInfo.setDeleteHeight(tx.getHeight());
        agentInfo.setStatus(ApiConstant.STOP_AGENT);
        agentInfo.setDeposit(BigInteger.ZERO);
        agentInfo.setNew(false);
    }

    private void processChangeAgentDeposit(int chainId, TransactionInfo tx) {
        DepositInfo depositInfo = (DepositInfo) tx.getTxData();
        if (tx.getType() == TxType.REDUCE_AGENT_DEPOSIT) {
            //减少记为负数
            depositInfo.setAmount(depositInfo.getAmount().negate());
            depositInfo.setType(DepositInfoType.REDUCE_AGENT_DEPOSIT);
        } else {
            depositInfo.setType(DepositInfoType.APPEND_AGENT_DEPOSIT);
        }
        depositInfo.setDeleteHeight(BigInteger.ONE.negate().longValue());
        depositInfo.setAssetId(ApiContext.defaultAssetId);
        depositInfo.setAssetChainId(ApiContext.defaultChainId);
        depositInfo.setSymbol(ApiContext.defaultSymbol);
        depositInfo.setFixedType(DepositFixedType.NONE.name());
        processDepositTx(chainId, depositInfo, tx);
        AgentInfo agentInfo = queryAgentInfo(chainId, depositInfo.getAgentHash(), 1);
        agentInfo.setDeposit(agentInfo.getDeposit().add(depositInfo.getAmount()));
        agentInfo.setNew(false);
    }

    private void processDepositTx(int chainId, TransactionInfo tx) {
        DepositInfo depositInfo = (DepositInfo) tx.getTxData();
        depositInfo.setType(DepositInfoType.STACKING);
        processDepositTx(chainId, depositInfo, tx);
    }

    private void processDepositTx(int chainId, DepositInfo depositInfo, TransactionInfo tx) {
        depositInfo.setKey(DBUtil.getDepositKey(depositInfo.getTxHash(), depositInfo.getAddress()));
        depositInfo.setNew(true);
        depositInfo.setDeleteHeight(-1);
        depositInfoList.add(depositInfo);
        changeAccountLockBalance(chainId, tx, depositInfo);
    }

    /**
     * 保存最终喂价
     *
     * @param chainId
     * @param tx
     */
    private void processSymbolFinalQuotation(int chainId, TransactionInfo tx) {
        symbolPriceInfoList = tx.getTxDataList().stream().map(d -> (StackSymbolPriceInfo) d).collect(Collectors.toList());
    }

    /**
     * 保存喂价提交流水
     *
     * @param chainId
     * @param tx
     */
    private void processSymbolQuotation(int chainId, TransactionInfo tx) {
        symbolQuotationRecordInfoList = tx.getTxDataList().stream().map(d -> {
                    SymbolQuotationRecordInfo info = (SymbolQuotationRecordInfo) d;
                    AgentInfo agentInfo = agentService.getAgentByPackingAddress(chainId, info.getAddress());
                    if (agentInfo != null) {
                        info.setAlias(agentInfo.getAgentAlias());
                    }
                    return info;
                }
        ).collect(Collectors.toList());
    }


    /**
     * 改变账户委托锁定
     *
     * @param chainId
     * @param tx
     * @param depositInfo
     */
    private void changeAccountLockBalance(int chainId, TransactionInfo tx, DepositInfo depositInfo) {
        AccountInfo accountInfo = queryAccountInfo(chainId, depositInfo.getAddress());
        //累加账户交易总数
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        //改变账户共识锁定数量
        if(depositInfo.getAssetChainId() == ApiContext.defaultChainId && depositInfo.getAssetId() == ApiContext.defaultAssetId){
            accountInfo.setConsensusLock(accountInfo.getConsensusLock().add(depositInfo.getAmount()));
        }
        //修改账本数据
        AccountLedgerInfo ledgerInfo = calcBalance(chainId, depositInfo.getAssetChainId(), depositInfo.getAssetId(), accountInfo, tx.getFee().getValue());
        ledgerInfo.setConsensusLock(ledgerInfo.getConsensusLock().add(depositInfo.getAmount()));
        //保存交易流水
        SymbolRegInfo symbolRegInfo = symbolRegService.get(depositInfo.getAssetChainId(),depositInfo.getAssetId());
        depositInfo.setSymbol(symbolRegInfo.getSymbol());
        depositInfo.setDecimal(symbolRegInfo.getDecimals());
        txRelationInfoSet.add(new TxRelationInfo(tx, depositInfo, ledgerInfo.getTotalBalance()));
    }

    private void processCancelDepositTx(int chainId, TransactionInfo tx) {

        //查询委托记录，生成对应的取消委托信息
        CancelDepositInfo cancelInfo = (CancelDepositInfo) tx.getTxData();
        DepositInfo depositInfo = depositService.getDepositInfoByKey(chainId, DBUtil.getDepositKey(cancelInfo.getJoinTxHash(), cancelInfo.getAddress()));

        DepositInfo newDeposit = new DepositInfo();
        newDeposit.setType(DepositInfoType.CANCEL_STACKING);
        newDeposit.setFixedType(DepositFixedType.NONE.name());
        newDeposit.setAssetId(depositInfo.getAssetId());
        newDeposit.setSymbol(depositInfo.getSymbol());
        newDeposit.setAssetChainId(depositInfo.getAssetChainId());
        //取消委托 数量记为负数
        newDeposit.setAmount(depositInfo.getAmount().negate());

        newDeposit.setCreateTime(cancelInfo.getCreateTime());
        newDeposit.setTxHash(cancelInfo.getTxHash());
        newDeposit.setBlockHeight(cancelInfo.getBlockHeight());
        newDeposit.setFee(cancelInfo.getFee());
        newDeposit.setAddress(cancelInfo.getAddress());

        newDeposit.setKey(DBUtil.getDepositKey(tx.getHash(), depositInfo.getKey()));
        newDeposit.setDeleteKey(depositInfo.getKey());
        newDeposit.setNew(true);
        //保存取消委托的流水
        depositInfoList.add(newDeposit);
        changeAccountLockBalance(chainId, tx, newDeposit);

        //标记对应的委托记录为已删除
        depositInfo.setDeleteHeight(cancelInfo.getBlockHeight());
        depositInfo.setDeleteKey(newDeposit.getKey());
        depositInfo.setNew(false);
        depositInfoList.add(depositInfo);
    }

    public void processYellowPunishTx(int chainId, TransactionInfo tx) {
        addressSet.clear();

        for (TxDataInfo txData : tx.getTxDataList()) {
            PunishLogInfo punishLog = (PunishLogInfo) txData;
            punishLogList.add(punishLog);
            addressSet.add(punishLog.getAddress());
        }

        ChainConfigInfo configInfo = CacheManager.getCache(chainId).getConfigInfo();
        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            AssetInfo assetInfo = CacheManager.getRegisteredAsset(DBUtil.getAssetKey(configInfo.getChainId(), configInfo.getAwardAssetId()));
            txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, assetInfo, BigInteger.ZERO, TRANSFER_TO_TYPE, accountInfo.getTotalBalance()));
        }
    }

    public void processConverterTx(int chainId, TransactionInfo tx) {
        converterTxInfoList.add((ConverterTxInfo) tx.getTxData());
        processTransferTx(chainId,tx);
    }

    public void processRedPunishTx(int chainId, TransactionInfo tx) {
        PunishLogInfo redPunish = (PunishLogInfo) tx.getTxData();
        punishLogList.add(redPunish);
        AgentInfo agentInfo = queryAgentInfo(chainId, redPunish.getAddress(), 2);
        //构建取消抵押的对象
        DepositInfo cancelDeposit = DepositInfo.buildByAgent(agentInfo.getDeposit().negate(), agentInfo, tx, DepositInfoType.STOP_AGENT);
        depositInfoList.add(cancelDeposit);
        //找到所有委托记录，设置删除状态
        //根据节点找到委托列表
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(chainId, agentInfo.getTxHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo depositInfo : depositInfos) {
                depositInfo.setDeleteKey(cancelDeposit.getKey());
                depositInfo.setDeleteHeight(tx.getHeight());
                depositInfoList.add(depositInfo);
            }
        }
        //红牌惩罚的账户余额和锁定金额的处理和停止共识节点类似
        changeAccountLockBalance(chainId, tx, cancelDeposit);
        //根据红牌找到被惩罚的节点
        cleanAgent(agentInfo, tx);
//        AccountInfo accountInfo;
//        AccountLedgerInfo ledgerInfo;
//        CoinToInfo output = null;
//        BigInteger amount = BigInteger.ZERO;
//        addressSet.clear();
//        for (int i = 0; i < tx.getCoinTos().size(); i++) {
//            output = tx.getCoinTos().get(i);
//            accountInfo = queryAccountInfo(chainId, output.getAddress());
//            if (!addressSet.contains(output.getAddress())) {
//                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
//            }
//            //lockTime > 0 这条output的金额就是节点的保证金
//            if (output.getLockTime() > 0) {
//                accountInfo.setConsensusLock(accountInfo.getConsensusLock().subtract(agentInfo.getDeposit()));
//                ledgerInfo = calcBalance(chainId, output.getChainId(), output.getAssetsId(), accountInfo, tx.getFee().getValue());
//                TxRelationInfo relationInfo = new TxRelationInfo(output, tx, tx.getFee().getValue(), ledgerInfo.getTotalBalance());
//                relationInfo.setTransferType(TRANSFER_FROM_TYPE);
//                txRelationInfoSet.add(relationInfo);
//            } else {
//                accountInfo.setConsensusLock(accountInfo.getConsensusLock().subtract(output.getAmount()));
//                if (!output.getAddress().equals(agentInfo.getAgentAddress())) {
//                    ledgerInfo = queryLedgerInfo(chainId, output.getAddress(), output.getChainId(), output.getAssetsId());
//                    txRelationInfoSet.add(new TxRelationInfo(output, tx, BigInteger.ZERO, ledgerInfo.getTotalBalance()));
//                }
//            }
//            addressSet.add(output.getAddress());
//        }
//        //最后这条记录是创建节点的地址
//        ledgerInfo = queryLedgerInfo(chainId, agentInfo.getAgentAddress(), output.getChainId(), output.getAssetsId());
//        txRelationInfoSet.add(new TxRelationInfo(output, tx, BigInteger.ZERO, ledgerInfo.getTotalBalance()));

//        //根据节点找到委托列表
//        List<DepositInfo> depositInfos = depositService.pageDepositListByAgentHash(chainId, agentInfo.getTxHash());
//        if (!depositInfos.isEmpty()) {
//            for (DepositInfo depositInfo : depositInfos) {
//                DepositInfo cancelDeposit = new DepositInfo();
//                cancelDeposit.setNew(true);
//                cancelDeposit.setType(ApiConstant.CANCEL_CONSENSUS);
//                cancelDeposit.copyInfoWithDeposit(depositInfo);
//                cancelDeposit.setKey(DBUtil.getDepositKey(tx.getHash(), depositInfo.getKey()));
//                cancelDeposit.setTxHash(tx.getHash());
//                cancelDeposit.setBlockHeight(tx.getHeight());
//                cancelDeposit.setDeleteKey(depositInfo.getKey());
//                cancelDeposit.setFee(BigInteger.ZERO);
//                cancelDeposit.setCreateTime(tx.getCreateTime());
//
//                depositInfo.setDeleteKey(cancelDeposit.getKey());
//                depositInfo.setDeleteHeight(tx.getHeight());
//                depositInfoList.add(depositInfo);
//                depositInfoList.add(cancelDeposit);
//
//                agentInfo.setDeposit(agentInfo.getDeposit().subtract(depositInfo.getAmount()));
//            }
//        }
    }

    private void processRegChainTx(int chainId, TransactionInfo tx) {
        CoinFromInfo input = tx.getCoinFroms().get(0);
        AccountInfo accountInfo;
        AccountLedgerInfo ledgerInfo;
        for (CoinToInfo to : tx.getCoinTos()) {
            if (to.getAddress().equals(input.getAddress())) {
                accountInfo = queryAccountInfo(chainId, input.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
                ledgerInfo = calcBalance(chainId, input.getChainId(), input.getAssetsId(), accountInfo, input.getAmount().subtract(to.getAmount()));
                txRelationInfoSet.add(new TxRelationInfo(input, tx, input.getAmount().subtract(to.getAmount()), ledgerInfo.getTotalBalance()));
                break;
            } else {
                accountInfo = queryAccountInfo(chainId, to.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
                ledgerInfo = calcBalance(chainId, to);
                txRelationInfoSet.add(new TxRelationInfo(to, tx, ledgerInfo.getTotalBalance()));
            }
        }

        chainInfoList.add((ChainInfo) tx.getTxData());
    }


    private AccountLedgerInfo calcBalance(int chainId, CoinToInfo output) {
        ChainInfo chainInfo = CacheManager.getCacheChain(chainId);
        if (output.getChainId() == chainInfo.getChainId() && output.getAssetsId() == chainInfo.getDefaultAsset().getAssetId()) {
            AccountInfo accountInfo = queryAccountInfo(chainId, output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn().add(output.getAmount()));
            accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(output.getAmount()));
        }

        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, output.getAddress(), output.getChainId(), output.getAssetsId());
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(output.getAmount()));
        return ledgerInfo;
    }

    private AccountLedgerInfo calcBalance(int chainId, CoinFromInfo input) {
        ChainInfo chainInfo = CacheManager.getCacheChain(chainId);
        if (input.getChainId() == chainInfo.getChainId() && input.getAssetsId() == chainInfo.getDefaultAsset().getAssetId()) {
            AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
            accountInfo.setTotalOut(accountInfo.getTotalOut().add(input.getAmount()));
            accountInfo.setTotalBalance(accountInfo.getTotalBalance().subtract(input.getAmount()));
            if (accountInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
                throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "account[" + accountInfo.getAddress() + "] totalBalance < 0");
            }
        }
        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, input.getAddress(), input.getChainId(), input.getAssetsId());
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().subtract(input.getAmount()));
        if (ledgerInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
            //  throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "accountLedger[" + DBUtil.getAccountAssetKey(ledgerInfo.getAddress(), ledgerInfo.getChainId(), ledgerInfo.getAssetId()) + "] totalBalance < 0");
        }
        return ledgerInfo;
    }

    private AccountLedgerInfo calcBalance(int chainId, int assetChainId, int assetId, AccountInfo accountInfo, BigInteger fee) {
        if (chainId == assetChainId) {
            accountInfo.setTotalOut(accountInfo.getTotalOut().add(fee));
            accountInfo.setTotalBalance(accountInfo.getTotalBalance().subtract(fee));
            if (accountInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
                throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "account[" + accountInfo.getAddress() + "] totalBalance < 0");
            }
        }
        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, accountInfo.getAddress(), ApiContext.defaultChainId, assetId);
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().subtract(fee));
//        if (ledgerInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
//            throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "accountLedger[" + DBUtil.getAccountAssetKey(ledgerInfo.getAddress(), ledgerInfo.getChainId(), ledgerInfo.getAssetId()) + "] totalBalance < 0");
//        }
        return ledgerInfo;
    }


    /**
     * 解析区块和所有交易后，将数据存储到数据库中
     * Store entity in the database after parsing the block and all transactions
     */
    public void  save(int chainId, BlockInfo blockInfo) {
        long height = blockInfo.getHeader().getHeight();

        long time1, time2;

        //以下处理为幂等操作，可以重复调用
        //处理轮次
        SyncInfo syncInfo = chainService.saveNewSyncInfo(chainId, height, blockInfo.getHeader().getAgentVersion());
        roundManager.process(chainId, blockInfo);

        ApiContext.protocolVersion = syncInfo.getVersion();
        //存储区块头信息
        time1 = System.currentTimeMillis();
        blockService.saveBLockHeaderInfo(chainId, blockInfo.getHeader());
//        //存区块序列化完整信息
//        blockService.saveBlockHexInfo(chainId, blockInfo.getBlockHexInfo());

        //存储出块时间信息
        blockTimeService.save(chainId, blockTimeInfoMap.get(chainId));
        //存储交易记录
        txService.saveTxList(chainId, blockInfo.getTxList().stream()
                //过滤掉没有奖励的coin base交易
                .filter(t -> {
                    if(t.getType() != TxType.COIN_BASE) {
                        return true;
                    }
                    if (t.getCoinTos().isEmpty() && t.getCoinFroms().isEmpty()) {
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList()));
        //存储交易和地址关系记录
        txService.saveTxRelationList(chainId, txRelationInfoSet);

        //存储别名记录
        aliasService.saveAliasList(chainId, aliasInfoList);

        //存储红黄牌惩罚记录
        savePunishList(chainId, punishLogList);

        //存储链信息
        chainService.saveChainList(chainInfoList);

        switch (syncInfo.getStep()){
            case 0 :
                //存储共识节点列表
                agentService.saveAgentList(chainId, agentInfoList);
                syncInfo.setStep(1);
                chainService.updateStep(syncInfo);
            case 1 :
                //存储委托/取消委托记录
                depositService.saveDepositList(chainId, depositInfoList);
                syncInfo.setStep(2);
                chainService.updateStep(syncInfo);
            case 2 :
                //存储账户信息表
                accountService.saveAccounts(chainId, accountInfoMap);
                syncInfo.setStep(3);
                chainService.updateStep(syncInfo);
            case 3 :
                ledgerService.saveLedgerList(chainId, accountLedgerInfoMap);
                syncInfo.setStep(4);
                chainService.updateStep(syncInfo);
            case 4 :
                //存储最终喂价数据
                symbolUsdtPriceService.saveFinalQuotation(symbolPriceInfoList);
                syncInfo.setStep(5);
                chainService.updateStep(syncInfo);
            case 5 :
                //存储各个节点喂价记录
                symbolUsdtPriceService.saveQuotation(symbolQuotationRecordInfoList);
                syncInfo.setStep(6);
                chainService.updateStep(syncInfo);
            case 6 :
                //存储stacking每日快照
                stackSnapshootService.buildSnapshoot(chainId,blockInfo.getHeader()).ifPresent(d -> stackSnapshootService.save(chainId, d));
                syncInfo.setStep(7);
                chainService.updateStep(syncInfo);
            case 7 :
                //存储跨链交易扩展信息
                converterTxInfoList.forEach(d -> converterTxService.save(chainId, d));
                syncInfo.setStep(8);
                chainService.updateStep(syncInfo);
            case 8 :
                //存储链内资产注册信息
                ledgerRegAssetInfoList.forEach(ledgerRegAssetService::save);
                syncInfo.setStep(9);
                chainService.updateStep(syncInfo);
        }

        //完成解析
        syncInfo.setStep(100);
        chainService.updateStep(syncInfo);

    }

    private void savePunishList(int chainId, List<PunishLogInfo> punishLogList) {
        this.punishService.savePunishList(chainId,punishLogList);
        punishLogList.forEach(d->{
            AgentInfo agentInfo = agentService.getAgentByAgentAddress(chainId,d.getAddress());
            if(d.getType() == PUBLISH_YELLOW){
                roundService.setRoundItemYellow(chainId,d.getRoundIndex(),d.getPackageIndex(),agentInfo.getTxHash());
            }else{
                roundService.setRoundItemRed(chainId,d.getRoundIndex(),d.getPackageIndex(),agentInfo.getTxHash());
            }
        });
    }

    private AccountInfo queryAccountInfo(int chainId, String address) {
        AccountInfo accountInfo = accountInfoMap.get(address);
        if (accountInfo == null) {
            accountInfo = accountService.getAccountInfo(chainId, address);
            if (accountInfo == null) {
                accountInfo = new AccountInfo(address);
            }
            accountInfoMap.put(address, accountInfo);
        }
        return accountInfo;
    }

    private AccountLedgerInfo queryLedgerInfo(int chainId, String address, int assetChainId, int assetId) {
        String key = DBUtil.getAccountAssetKey(address, assetChainId, assetId);
        AccountLedgerInfo ledgerInfo = accountLedgerInfoMap.get(key);
        if (ledgerInfo == null) {
            ledgerInfo = ledgerService.getAccountLedgerInfo(chainId, key);
            if (ledgerInfo == null) {
                ledgerInfo = new AccountLedgerInfo(address, assetChainId, assetId);
            }
            accountLedgerInfoMap.put(key, ledgerInfo);
        }
        return ledgerInfo;
    }

    private AgentInfo queryAgentInfo(int chainId, String key, int type) {
        AgentInfo agentInfo;
        for (int i = 0; i < agentInfoList.size(); i++) {
            agentInfo = agentInfoList.get(i);

            if (type == 1 && agentInfo.getTxHash().equals(key)) {
                return agentInfo;
            } else if (type == 2 && agentInfo.getAgentAddress().equals(key)) {
                return agentInfo;
            } else if (type == 3 && agentInfo.getRewardAddress().equals(key)) {
                return agentInfo;
            }
        }
        if (type == 1) {
            agentInfo = agentService.getAgentByHash(chainId, key);
        } else if (type == 2) {
            agentInfo = agentService.getAgentByAgentAddress(chainId, key);
        } else {
            agentInfo = agentService.getAgentByRewardAddress(chainId, key);
        }
        if (agentInfo != null) {
            agentInfoList.add(agentInfo);
        }
        return agentInfo;
    }

    private void clear(int chainId) {
        accountInfoMap.clear();
        accountLedgerInfoMap.clear();
        agentInfoList.clear();
        txRelationInfoSet.clear();
        aliasInfoList.clear();
        depositInfoList.clear();
        punishLogList.clear();
        chainInfoList.clear();
        symbolPriceInfoList.clear();
        symbolPriceInfoList.clear();
        stackSnapshootInfo = Optional.empty();
        converterTxInfoList.clear();
        ledgerRegAssetInfoList.clear();
        symbolQuotationRecordInfoList.clear();
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache.getAccountMap().size() > MongoAccountServiceImpl.cacheSize * 2) {
            Set<String> keySet = apiCache.getAccountMap().keySet();
            int i = 0;
            for (String key : keySet) {
                apiCache.getAccountMap().remove(key);
                i++;
                if (i >= MongoAccountServiceImpl.cacheSize) {
                    break;
                }
            }
        }
    }



}
